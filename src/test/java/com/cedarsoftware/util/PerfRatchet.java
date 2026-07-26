package com.cedarsoftware.util;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.TreeMap;
import java.util.logging.Logger;

/**
 * Machine-portable performance tracking for the {@code performRelease} benchmarks.
 *
 * <h2>Why ratios instead of nanoseconds</h2>
 * Absolute nanos/op vary by 2-5x between a dev laptop, a CI runner, and a release machine, so any
 * fixed threshold either fails constantly on slow hardware or never catches a real regression on
 * fast hardware. Instead every metric is expressed as a <b>ratio against a yardstick workload
 * measured in the same JVM, in the same run, interleaved trial-for-trial</b>. Machine speed divides
 * out: hardware 3x slower slows the subject and the yardstick alike, leaving the ratio intact. What
 * the ratio measures is what we actually care about — the cost of java-util's data structure
 * relative to the JDK equivalent it replaces.
 *
 * <h2>Why nothing here fails your build</h2>
 * A perf number is a measurement, not a contract, and turning normal variance into a red build
 * teaches everyone to ignore it. Verdicts are reported, not asserted:
 * <ul>
 *   <li><b>OK</b> — within the noise band of the recorded baseline.</li>
 *   <li><b>IMPROVED</b> — faster than baseline by more than {@value #DEFAULT_IMPROVE_PCT}%; a gain
 *       available to be locked in.</li>
 *   <li><b>REGRESSED</b> — slower than baseline by more than {@value #DEFAULT_REGRESS_PCT}%. Logged
 *       at WARNING and printed in the summary block.</li>
 *   <li><b>NEW</b> — no baseline recorded yet.</li>
 * </ul>
 * Pass {@code -Dperf.strict=true} to turn REGRESSED into a genuine test failure. That is opt-in, for
 * a machine you trust, and is deliberately off in CI.
 *
 * <h2>The ratchet</h2>
 * The baseline in {@code src/test/resources/perf-baseline.properties} holds the best ratio recorded for
 * each metric, so the bar only ever moves toward faster:
 * <pre>
 * # lock in a genuine gain -- writes only credible improvements
 * mvn clean test -DperformRelease=true -Dperf.baseline.update=true
 *
 * # establish or repair baselines -- writes every metric, either direction
 * mvn clean test -DperformRelease=true -Dperf.baseline.reset=true
 * </pre>
 * Update mode never writes a regression, so a bad run cannot slacken the bar, and an improvement must
 * clear the noise floor to be written, so a lucky outlier cannot ratchet the bar out of reach. Reset
 * exists because a pure ratchet cannot repair itself: a baseline first captured from an unluckily
 * <em>fast</em> reading would report a regression forever, and update mode by definition will not raise
 * it back.
 * <p>
 * <b>Always calibrate from a full suite run.</b> Running these classes alone gives materially different
 * ratios and far lower noise than the release build — measured in isolation the metrics scattered 2-15%,
 * and alongside the other ~20,000 tests the same metrics scattered 5-68%. A baseline captured in
 * isolation reports false regressions during the release it was meant to protect.
 *
 * <h2>Measurement hygiene</h2>
 * Untimed warmup to let JIT settle, then N timed trials reduced by <b>median</b> rather than mean, so a
 * single GC pause or scheduler hiccup cannot skew a run. Subject and yardstick trials alternate, so
 * thermal drift and background load hit both. Workloads publish into {@link #sink} to keep the JIT from
 * eliminating the work being measured. When several variants share one lambda body, call
 * {@link #warmAll} first — see the note there on why per-variant warmup is not enough.
 * <p>
 * None of this makes an in-JVM microbenchmark as trustworthy as a forked JMH harness. It is calibrated
 * to be honest about that: it publishes its own noise, widens its bars to match, and declines to gate
 * what it cannot measure reproducibly.
 */
public final class PerfRatchet {
    private static final Logger LOG = Logger.getLogger(PerfRatchet.class.getName());

    private static final String BASELINE_RESOURCE = "/perf-baseline.properties";
    private static final Path BASELINE_SOURCE = Paths.get("src", "test", "resources", "perf-baseline.properties");
    private static final Path REPORT_FILE = Paths.get("target", "perf-report.tsv");

    /**
     * A regression must exceed this percentage over baseline to be called one.
     * <p>
     * Set from measured noise, not from wishful thinking. Repeating the MultiKeyMap suite back-to-back
     * on an idle machine moved most ratios 3-13% run over run, with one metric swinging 24%. That noise
     * sits <em>on top of</em> a baseline holding the best ratio ever seen, so a typical run is already
     * above the bar before any code changes. A tighter band would cry regression most runs, and a perf
     * signal that fires constantly is one everybody learns to skip.
     * <p>
     * What this band still catches is the class of regression that matters: a lost fast path, a new lock
     * on a hot path, an accidental O(1)-to-O(n) — those land at 1.5x to 10x, not 1.4x. A subtle 20%
     * regression is below the noise floor of this kind of measurement and no threshold would catch it
     * honestly; the per-run ratios in the summary block are there to be trended for that.
     */
    static final int DEFAULT_REGRESS_PCT = 40;
    /** An improvement must exceed this percentage under baseline to be lockable. */
    static final int DEFAULT_IMPROVE_PCT = 10;

    private static final int DEFAULT_WARMUP_TRIALS = 5;
    private static final int DEFAULT_TRIALS = 7;

    /**
     * Consumes workload results so the JIT cannot dead-code-eliminate the operations being timed.
     * Volatile, and written on every iteration by the benchmark bodies.
     */
    public static volatile Object sink;

    private static final List<Metric> METRICS = Collections.synchronizedList(new ArrayList<Metric>());
    private static final Properties BASELINE = loadBaseline();
    private static volatile boolean hookInstalled;

    private PerfRatchet() {
    }

    public enum Verdict { OK, IMPROVED, REGRESSED, NEW }

    /** One recorded measurement. */
    public static final class Metric {
        final String name;
        final String subjectLabel;
        final double subjectNanos;
        final String yardstickLabel;
        final double yardstickNanos;   // NaN when there is no yardstick
        final double ratio;            // NaN when there is no yardstick
        final double baseline;         // NaN when not previously recorded
        final double noisePct;         // observed spread across this run's own trials
        final Verdict verdict;

        Metric(String name, String subjectLabel, double subjectNanos, String yardstickLabel,
               double yardstickNanos, double ratio, double baseline, double noisePct, Verdict verdict) {
            this.name = name;
            this.subjectLabel = subjectLabel;
            this.subjectNanos = subjectNanos;
            this.yardstickLabel = yardstickLabel;
            this.yardstickNanos = yardstickNanos;
            this.ratio = ratio;
            this.baseline = baseline;
            this.noisePct = noisePct;
            this.verdict = verdict;
        }

        /** Spread across this run's own trials, as a percentage of the median. Higher means less trustworthy. */
        public double getNoisePct() {
            return noisePct;
        }

        public Verdict getVerdict() {
            return verdict;
        }

        public double getRatio() {
            return ratio;
        }

        /** Percent change vs baseline; positive means slower. NaN when there is no baseline. */
        double deltaPct() {
            if (Double.isNaN(baseline) || baseline <= 0 || Double.isNaN(ratio)) {
                return Double.NaN;
            }
            return (ratio - baseline) / baseline * 100.0;
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Public API
    // ---------------------------------------------------------------------------------------------

    /**
     * Times {@code subject} against {@code yardstick} and records the ratio against the baseline.
     * Both workloads must perform exactly {@code opsPerTrial} operations per invocation.
     * <p>
     * This is the primary entry point. Pick a yardstick that is the honest alternative to the subject
     * — the JDK collection it replaces, or the third-party library it competes with — because the
     * recorded number means "how many times the cost of that alternative".
     *
     * @param metric         stable dotted key for the baseline file; renaming it orphans its history
     * @param opsPerTrial    operations performed by one invocation of either workload
     * @param subjectLabel   human label for the subject, e.g. {@code "CaseInsensitiveMap.put"}
     * @param subject        the java-util workload being tracked
     * @param yardstickLabel human label for the yardstick, e.g. {@code "LinkedHashMap.put"}
     * @param yardstick      the comparison workload
     * @return the recorded metric
     */
    public static Metric compare(String metric, int opsPerTrial,
                                 String subjectLabel, Runnable subject,
                                 String yardstickLabel, Runnable yardstick) {
        installHook();
        int warmups = intProperty("perf.warmups", DEFAULT_WARMUP_TRIALS);
        int trials = intProperty("perf.trials", DEFAULT_TRIALS);

        for (int i = 0; i < warmups; i++) {
            subject.run();
            yardstick.run();
        }

        // One collection before timing, not per trial: a GC inside the timed region is exactly the
        // noise the median is there to absorb, and per-trial collection would dominate the runtime.
        System.gc();

        List<Double> subjectTimes = new ArrayList<>(trials);
        List<Double> yardstickTimes = new ArrayList<>(trials);
        for (int i = 0; i < trials; i++) {
            // Alternate so drift and background load land on both sides.
            subjectTimes.add(timeOnce(opsPerTrial, subject));
            yardstickTimes.add(timeOnce(opsPerTrial, yardstick));
        }

        double subjectNanos = median(subjectTimes);
        double yardstickNanos = median(yardstickTimes);
        double ratio = yardstickNanos > 0 ? subjectNanos / yardstickNanos : Double.NaN;

        // Noise floor for THIS run: how much the two sides' own trials disagreed. Used below so a
        // difference smaller than the measurement's own scatter is never announced as a regression.
        double noisePct = Math.max(spreadPct(subjectTimes), spreadPct(yardstickTimes));

        return record(metric, subjectLabel, subjectNanos, yardstickLabel, yardstickNanos, ratio, noisePct);
    }

    /**
     * Times a workload and reports nanos/op with no ratio and no verdict.
     * <p>
     * For workloads with no honest yardstick. These are recorded for human trending only — absolute
     * nanos are not comparable across machines, so gating on them would be meaningless.
     *
     * @param metric      stable dotted key
     * @param opsPerTrial operations performed by one invocation
     * @param label       human label
     * @param workload    the workload to time
     * @return the recorded metric
     */
    public static Metric report(String metric, int opsPerTrial, String label, Runnable workload) {
        installHook();
        int warmups = intProperty("perf.warmups", DEFAULT_WARMUP_TRIALS);
        int trials = intProperty("perf.trials", DEFAULT_TRIALS);

        for (int i = 0; i < warmups; i++) {
            workload.run();
        }
        System.gc();

        List<Double> times = new ArrayList<>(trials);
        for (int i = 0; i < trials; i++) {
            times.add(timeOnce(opsPerTrial, workload));
        }

        Metric m = new Metric(metric, label, median(times), null, Double.NaN, Double.NaN,
                Double.NaN, spreadPct(times), Verdict.OK);
        METRICS.add(m);
        LOG.info(String.format(Locale.ROOT, "perf %s: %s = %.1f ns/op", metric, label, m.subjectNanos));
        return m;
    }

    /** True when {@code -Dperf.strict=true}; REGRESSED verdicts then fail their test. */
    public static boolean isStrict() {
        return Boolean.parseBoolean(System.getProperty("perf.strict", "false"));
    }

    /**
     * Runs every workload untimed to force JIT compilation of a shared code path before any of them is
     * measured.
     * <p>
     * Needed whenever a test measures several variants through the <em>same</em> lambda body — differing
     * only by captured state, as a loop over key counts or collection sizes does. HotSpot compiles that
     * body once per call site, so without this the first variant measured absorbs the interpreter and C1
     * cost on everyone's behalf and records a permanently pessimistic, unstable baseline, while later
     * variants get a fully warm method. Per-variant warmup inside {@link #compare} cannot fix it: by
     * then the first variant has already been measured.
     *
     * @param rounds   untimed passes over the whole set
     * @param workloads every workload that will subsequently be measured
     */
    public static void warmAll(int rounds, Runnable... workloads) {
        for (int i = 0; i < rounds; i++) {
            for (Runnable w : workloads) {
                w.run();
            }
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Internals
    // ---------------------------------------------------------------------------------------------

    private static Metric record(String metric, String subjectLabel, double subjectNanos,
                                 String yardstickLabel, double yardstickNanos, double ratio,
                                 double noisePct) {
        double baseline = baselineFor(metric);

        // Both bars are the configured floor OR this run's own measured scatter, whichever is larger.
        // Self-calibrating: a metric whose trials disagreed by 35% cannot credibly report a 25% change
        // in either direction, while a metric that measured to within 3% is held to the tight floor.
        //
        // Applying it to IMPROVED matters as much as to REGRESSED, and is less obvious. The baseline
        // holds the best ratio ever recorded, so a single lucky outlier on a noisy metric would be
        // locked in as the permanent bar -- after which every honest run sits far above it and reports
        // a regression forever. Requiring an improvement to clear the noise floor before it can be
        // ratcheted in is what keeps the bar reachable.
        double noiseFloor = Math.max(0.0, noisePct);
        double regressBar = Math.max(intProperty("perf.regress.pct", DEFAULT_REGRESS_PCT), noiseFloor);
        double improveBar = Math.max(intProperty("perf.improve.pct", DEFAULT_IMPROVE_PCT), noiseFloor);

        Verdict verdict;
        if (Double.isNaN(ratio)) {
            verdict = Verdict.OK;
        } else if (Double.isNaN(baseline)) {
            verdict = Verdict.NEW;
        } else if (ratio > baseline * (1.0 + regressBar / 100.0)) {
            verdict = Verdict.REGRESSED;
        } else if (ratio < baseline * (1.0 - improveBar / 100.0)) {
            verdict = Verdict.IMPROVED;
        } else {
            verdict = Verdict.OK;
        }

        Metric m = new Metric(metric, subjectLabel, subjectNanos, yardstickLabel, yardstickNanos,
                ratio, baseline, noisePct, verdict);
        METRICS.add(m);

        String detail = String.format(Locale.ROOT,
                "%s = %.1f ns/op, %s = %.1f ns/op, ratio %.2fx",
                subjectLabel, subjectNanos, yardstickLabel, yardstickNanos, ratio);

        switch (verdict) {
            case REGRESSED:
                LOG.warning(String.format(Locale.ROOT,
                        "PERF REGRESSION %s: %s -- %.0f%% slower than baseline %.2fx (bar %.0f%%, run noise %.0f%%)",
                        metric, detail, m.deltaPct(), baseline, regressBar, noisePct));
                break;
            case IMPROVED:
                LOG.info(String.format(Locale.ROOT,
                        "PERF IMPROVED %s: %s -- %.0f%% faster than baseline %.2fx (lock in with -Dperf.baseline.update=true)",
                        metric, detail, -m.deltaPct(), baseline));
                break;
            case NEW:
                LOG.info(String.format(Locale.ROOT, "PERF NEW %s: %s (no baseline yet)", metric, detail));
                break;
            default:
                LOG.info(String.format(Locale.ROOT, "perf %s: %s (baseline %.2fx)", metric, detail, baseline));
                break;
        }
        return m;
    }

    /** Runs the workload once and returns nanoseconds per operation. */
    private static double timeOnce(int opsPerTrial, Runnable workload) {
        long start = System.nanoTime();
        workload.run();
        long elapsed = System.nanoTime() - start;
        return opsPerTrial > 0 ? (double) elapsed / opsPerTrial : elapsed;
    }

    private static double median(List<Double> values) {
        List<Double> sorted = new ArrayList<>(values);
        Collections.sort(sorted);
        int n = sorted.size();
        if (n == 0) {
            return Double.NaN;
        }
        if ((n & 1) == 1) {
            return sorted.get(n / 2);
        }
        return (sorted.get(n / 2 - 1) + sorted.get(n / 2)) / 2.0;
    }

    /**
     * Interquartile spread of the trials as a percentage of their median.
     * <p>
     * Deliberately <em>not</em> {@code (max - min) / median}: a single outlier trial — the first timed
     * one after a heap expansion, or any trial that caught a GC — makes full-range spread read 200-500%
     * even when the median is rock steady (an allocation-heavy benchmark measured a 215% range while its
     * ratio repeated within 3% across runs). Full range would therefore have inflated the
     * self-calibrating regression bar to the point that nothing could ever be flagged. The interquartile
     * range describes the scatter of the middle of the distribution, which is what the median actually
     * estimates.
     */
    private static double spreadPct(List<Double> values) {
        int n = values.size();
        if (n < 4) {
            return 0.0;
        }
        List<Double> sorted = new ArrayList<>(values);
        Collections.sort(sorted);
        double q1 = sorted.get(n / 4);
        double q3 = sorted.get(n - 1 - n / 4);
        double med = median(values);
        return med > 0 ? (q3 - q1) / med * 100.0 : 0.0;
    }

    private static double baselineFor(String metric) {
        String raw = BASELINE.getProperty(metric);
        if (raw == null) {
            return Double.NaN;
        }
        try {
            return Double.parseDouble(raw.trim());
        } catch (NumberFormatException e) {
            LOG.warning("Unparseable perf baseline for " + metric + ": " + raw);
            return Double.NaN;
        }
    }

    /**
     * Loads the baseline, preferring the source file over the classpath copy.
     * <p>
     * The classpath copy in {@code target/test-classes} is a build artifact that Maven does not prune,
     * so reading it first makes the checked-in file non-authoritative: editing or deleting
     * {@code src/test/resources/perf-baseline.properties} has no effect until a {@code clean}, and a
     * value the file no longer contains keeps being read back and re-merged on update. Since this class
     * writes to the source path, it must read from there too or the two disagree.
     */
    private static Properties loadBaseline() {
        Properties props = new Properties();
        if (Files.isReadable(BASELINE_SOURCE)) {
            try (InputStream in = Files.newInputStream(BASELINE_SOURCE)) {
                props.load(in);
                return props;
            } catch (IOException e) {
                LOG.warning("Could not read perf baseline " + BASELINE_SOURCE + ": " + e.getMessage());
            }
        }
        try (InputStream in = PerfRatchet.class.getResourceAsStream(BASELINE_RESOURCE)) {
            if (in != null) {
                props.load(in);
            }
        } catch (IOException e) {
            LOG.warning("Could not read perf baseline: " + e.getMessage());
        }
        return props;
    }

    private static int intProperty(String key, int defaultValue) {
        String raw = System.getProperty(key);
        if (raw == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private static void installHook() {
        if (hookInstalled) {
            return;
        }
        synchronized (PerfRatchet.class) {
            if (hookInstalled) {
                return;
            }
            hookInstalled = true;
            Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
                @Override
                public void run() {
                    summarize();
                }
            }, "perf-ratchet-summary"));
        }
    }

    /**
     * Prints the summary block and writes the report. Delimited by a banner so
     * {@code scripts/extract-perf-results.sh} can lift it straight out of a deploy log.
     */
    static synchronized void summarize() {
        List<Metric> metrics = new ArrayList<>(METRICS);
        if (metrics.isEmpty()) {
            return;
        }
        Collections.sort(metrics, (a, b) -> a.name.compareTo(b.name));

        StringBuilder out = new StringBuilder();
        out.append('\n').append(banner()).append('\n');
        out.append(String.format(Locale.ROOT, "%-42s %12s %12s %8s %9s %7s  %s%n",
                "METRIC", "SUBJECT", "YARDSTICK", "RATIO", "BASELINE", "NOISE", "VERDICT"));

        int ok = 0, improved = 0, regressed = 0, isNew = 0;
        for (Metric m : metrics) {
            switch (m.verdict) {
                case IMPROVED: improved++; break;
                case REGRESSED: regressed++; break;
                case NEW: isNew++; break;
                default: ok++; break;
            }
            String verdictText = m.verdict.name();
            double delta = m.deltaPct();
            if (!Double.isNaN(delta) && (m.verdict == Verdict.REGRESSED || m.verdict == Verdict.IMPROVED)) {
                verdictText += String.format(Locale.ROOT, " (%+.0f%%)", delta);
            }
            out.append(String.format(Locale.ROOT, "%-42s %9.1f ns %9.1f ns %7s %9s %6.0f%%  %s%n",
                    m.name,
                    m.subjectNanos,
                    m.yardstickNanos,
                    Double.isNaN(m.ratio) ? "-" : String.format(Locale.ROOT, "%.2fx", m.ratio),
                    Double.isNaN(m.baseline) ? "-" : String.format(Locale.ROOT, "%.2fx", m.baseline),
                    m.noisePct,
                    verdictText));
        }

        out.append(String.format(Locale.ROOT, "%n%d OK, %d IMPROVED, %d REGRESSED, %d NEW%n",
                ok, improved, regressed, isNew));

        boolean resetting = Boolean.parseBoolean(System.getProperty("perf.baseline.reset", "false"));
        boolean updating = resetting
                || Boolean.parseBoolean(System.getProperty("perf.baseline.update", "false"));
        if (updating) {
            int written = writeBaselineUpdates(metrics, resetting);
            out.append(String.format(Locale.ROOT, "Baseline %s: %d metric(s) written to %s%n",
                    resetting ? "RESET" : "UPDATED", written, BASELINE_SOURCE));
        } else if (improved > 0 || isNew > 0) {
            out.append("Lock in improvements/new metrics with: mvn test -DperformRelease=true -Dperf.baseline.update=true\n");
        }
        if (regressed > 0 && !isStrict()) {
            out.append("Regressions are reported, not failed. Use -Dperf.strict=true to fail on them.\n");
        }
        out.append(banner()).append('\n');

        System.out.print(out);
        writeReport(metrics);
    }

    private static String banner() {
        StringBuilder b = new StringBuilder("=== PERF RATCHET ");
        while (b.length() < 104) {
            b.append('=');
        }
        return b.toString();
    }

    /**
     * Writes ratios back to the baseline source file.
     * <p>
     * In the default (update) mode only NEW and IMPROVED metrics are written, so a metric's bar only ever
     * moves toward faster and a regression can never slacken it — that is the ratchet.
     * <p>
     * {@code reset} writes every metric regardless of direction. This exists because a pure ratchet has
     * no way to repair itself: if a baseline is first recorded from an unluckily <em>fast</em> outlier,
     * every honest run afterwards reports a regression forever, and update mode by definition will not
     * raise the bar to fix it. Reset is also the correct way to establish baselines in the first place,
     * and must be run under the same conditions the metrics will be checked under — a full
     * {@code -DperformRelease=true} suite, not an isolated test class, since running alongside 20,000
     * other tests changes GC and JIT state enough to move both the ratios and their noise substantially.
     *
     * @param reset true to overwrite every metric; false to write only improvements and new metrics
     * @return count of metrics written
     */
    private static int writeBaselineUpdates(List<Metric> metrics, boolean reset) {
        TreeMap<String, String> merged = new TreeMap<>();
        for (String name : BASELINE.stringPropertyNames()) {
            merged.put(name, BASELINE.getProperty(name));
        }

        int written = 0;
        for (Metric m : metrics) {
            if (Double.isNaN(m.ratio)) {
                continue;
            }
            if (reset || m.verdict == Verdict.NEW || m.verdict == Verdict.IMPROVED) {
                merged.put(m.name, String.format(Locale.ROOT, "%.4f", m.ratio));
                written++;
            }
        }

        try {
            Files.createDirectories(BASELINE_SOURCE.getParent());
            try (BufferedWriter w = Files.newBufferedWriter(BASELINE_SOURCE, StandardCharsets.UTF_8)) {
                w.write("# java-util performance baseline -- best observed ratio per metric.\n");
                w.write("#\n");
                w.write("# Each value is subject-nanos / yardstick-nanos, both measured in the same JVM run, so the\n");
                w.write("# number is portable across machines: 1.85 means \"1.85x the cost of the JDK equivalent\".\n");
                w.write("# LOWER IS BETTER and the bar only moves down -- see PerfRatchet.\n");
                w.write("#\n");
                w.write("# Lock in a genuine gain (writes only credible improvements -- the ratchet):\n");
                w.write("#   mvn clean test -DperformRelease=true -Dperf.baseline.update=true\n");
                w.write("#\n");
                w.write("# Establish or repair baselines (writes every metric, either direction):\n");
                w.write("#   mvn clean test -DperformRelease=true -Dperf.baseline.reset=true\n");
                w.write("#\n");
                w.write("# Always calibrate from a FULL suite run. Running these classes in isolation gives\n");
                w.write("# materially different ratios and much lower noise than the release build does.\n");
                w.write("\n");
                for (java.util.Map.Entry<String, String> e : merged.entrySet()) {
                    w.write(e.getKey() + "=" + e.getValue() + "\n");
                }
            }
        } catch (IOException e) {
            LOG.warning("Could not write perf baseline " + BASELINE_SOURCE + ": " + e.getMessage());
            return 0;
        }
        return written;
    }

    private static void writeReport(List<Metric> metrics) {
        try {
            Files.createDirectories(REPORT_FILE.getParent());
            try (BufferedWriter w = Files.newBufferedWriter(REPORT_FILE, StandardCharsets.UTF_8)) {
                w.write("metric\tsubject\tsubject_ns_per_op\tyardstick\tyardstick_ns_per_op\tratio\tbaseline\tnoise_pct\tverdict\n");
                for (Metric m : metrics) {
                    w.write(String.format(Locale.ROOT, "%s\t%s\t%.3f\t%s\t%.3f\t%.4f\t%.4f\t%.1f\t%s%n",
                            m.name, m.subjectLabel, m.subjectNanos,
                            m.yardstickLabel == null ? "-" : m.yardstickLabel,
                            m.yardstickNanos, m.ratio, m.baseline, m.noisePct, m.verdict));
                }
            }
        } catch (IOException e) {
            LOG.warning("Could not write perf report: " + e.getMessage());
        }
    }
}
