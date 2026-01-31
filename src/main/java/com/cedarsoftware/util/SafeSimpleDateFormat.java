package com.cedarsoftware.util;

import java.math.RoundingMode;
import java.text.DateFormat;
import java.text.DateFormatSymbols;
import java.text.FieldPosition;
import java.text.NumberFormat;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Thread-safe wrapper for {@link SimpleDateFormat} with copy-on-write semantics.
 *
 * <p>Design goals:
 * <ul>
 *   <li>Source/binary compatible surface for existing users.</li>
 *   <li>Re-entrant and safe across threads (no shared mutable SDF instances).</li>
 *   <li>Performance: at most one {@code SimpleDateFormat} per thread per configuration.</li>
 *   <li>Mutators create a new immutable State; threads lazily rebuild their SDF (copy-on-write).</li>
 * </ul>
 *
 * <p>Hot path: no locks. Lookups happen in a per-thread LRU.</p>
 *
 * <p>For legacy code that used {@code SafeSimpleDateFormat.getDateFormat(pattern)}, this class
 * still provides the static accessor, returning a thread-local {@code SimpleDateFormat} keyed
 * only by pattern (same semantics as before).</p>
 *
 * @author John DeRegnaucourt (jdereg@gmail.com)
 *         <br>
 *         Copyright (c) Cedar Software LLC
 *         <br><br>
 *         Licensed under the Apache License, Version 2.0 (the "License");
 *         you may not use this file except in compliance with the License.
 *         You may obtain a copy of the License at
 *         <br><br>
 *         <a href="http://www.apache.org/licenses/LICENSE-2.0">License</a>
 *         <br><br>
 *         Unless required by applicable law or agreed to in writing, software
 *         distributed under the License is distributed on an "AS IS" BASIS,
 *         WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *         See the License for the specific language governing permissions and
 *         limitations under the License.
 */
public class SafeSimpleDateFormat extends DateFormat {
    private static final long serialVersionUID = 1L;

    // -------------------- Static legacy accessor (pattern-only) --------------------

    // Per-thread LRU for the static accessor (pattern -> SDF). Keeps original behavior.
    private static final int STATIC_PER_THREAD_LRU_CAPACITY = 16;
    private static final ThreadLocal<Map<String, SimpleDateFormat>> STATIC_TL =
            ThreadLocal.withInitial(() -> new LinkedHashMap<String, SimpleDateFormat>(24, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, SimpleDateFormat> eldest) {
                    return size() > STATIC_PER_THREAD_LRU_CAPACITY;
                }
            });

    /**
     * Legacy static accessor preserved for compatibility.
     * Returns a per-thread cached {@link SimpleDateFormat} for the given pattern.
     * <p><b>Note:</b> Mutating the returned {@code SimpleDateFormat} (e.g., setTimeZone)
     * will affect subsequent uses of this pattern in the same thread, just like the
     * original implementation.</p>
     */
    public static SimpleDateFormat getDateFormat(String pattern) {
        Objects.requireNonNull(pattern, "pattern");
        Map<String, SimpleDateFormat> m = STATIC_TL.get();
        SimpleDateFormat sdf = m.get(pattern);
        if (sdf == null) {
            // Build with defaults consistent with this class' defaults
            Locale loc = Locale.getDefault();
            TimeZone tz = TimeZone.getDefault();
            SimpleDateFormat fresh = new SimpleDateFormat(pattern, DateFormatSymbols.getInstance(loc));
            fresh.setTimeZone(tz);
            fresh.setLenient(true);
            NumberFormat nf = defaultNumberFormat();
            fresh.setNumberFormat((NumberFormat) nf.clone());
            Calendar cal = Calendar.getInstance(tz, loc);
            cal.clear();
            fresh.setCalendar(cal);
            m.put(pattern, fresh);
            sdf = fresh;
        }
        return sdf;
    }

    /** Clears the static accessor's per-thread cache. */
    public static void clearStaticThreadLocalCache() {
        STATIC_TL.remove();
    }

    // -------------------- Instance-based safe API (copy-on-write) --------------------

    // Per-thread LRU cache: State -> SimpleDateFormat (size-bounded).
    private static final int PER_THREAD_LRU_CAPACITY = 4;
    private static final ThreadLocal<Map<State, SimpleDateFormat>> TL =
            ThreadLocal.withInitial(() -> new LinkedHashMap<State, SimpleDateFormat>(8, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<State, SimpleDateFormat> eldest) {
                    return size() > PER_THREAD_LRU_CAPACITY;
                }
            });

    // Immutable snapshot of relevant config.
    private static final class State {
        final String pattern;
        final Locale locale;                // stored explicitly (Java 8 compatible)
        final TimeZone tz;                  // cloned
        final boolean lenient;
        final DateFormatSymbols symbols;    // cloned
        final NumberFormat nf;              // cloned
        final NFSig nfSig;                  // value signature for equals/hash
        final Long twoDigitYearStartEpochMs; // nullable

        State(String pattern,
              Locale locale,
              TimeZone tz,
              boolean lenient,
              NumberFormat nf,
              DateFormatSymbols symbols,
              Long twoDigitYearStartEpochMs) {

            this.pattern = Objects.requireNonNull(pattern, "pattern");
            this.locale  = Objects.requireNonNull(locale, "locale");
            this.tz      = (TimeZone) Objects.requireNonNull(tz, "tz").clone();
            this.lenient = lenient;
            this.nf      = (NumberFormat) Objects.requireNonNull(nf, "numberFormat").clone();
            this.symbols = (DateFormatSymbols) Objects.requireNonNull(symbols, "symbols").clone();
            this.twoDigitYearStartEpochMs = twoDigitYearStartEpochMs;
            this.nfSig = NFSig.of(this.nf);
        }

        SimpleDateFormat build() {
            SimpleDateFormat sdf = new SimpleDateFormat(pattern, symbols);
            sdf.setTimeZone(tz);
            sdf.setNumberFormat((NumberFormat) nf.clone()); // per-SDF copy
            if (twoDigitYearStartEpochMs != null) {
                sdf.set2DigitYearStart(new Date(twoDigitYearStartEpochMs));
            }
            Calendar cal = Calendar.getInstance(tz, locale);
            cal.clear();
            cal.setLenient(lenient);  // Set lenient on the calendar
            sdf.setCalendar(cal);
            // Set lenient after setCalendar to ensure it takes effect
            sdf.setLenient(lenient);
            return sdf;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof State)) return false;
            State s = (State) o;
            return lenient == s.lenient
                    && pattern.equals(s.pattern)
                    && locale.equals(s.locale)
                    && tzIdEquals(tz, s.tz)
                    && Objects.equals(twoDigitYearStartEpochMs, s.twoDigitYearStartEpochMs)
                    && symbols.equals(s.symbols)
                    && nfSig.equals(s.nfSig);
        }

        @Override
        public int hashCode() {
            return Objects.hash(pattern, locale, tzIdHash(tz), lenient, symbols, nfSig, twoDigitYearStartEpochMs);
        }

        private static boolean tzIdEquals(TimeZone a, TimeZone b) {
            return Objects.equals(a.getID(), b.getID());
        }
        private static int tzIdHash(TimeZone tz) {
            return Objects.hashCode(tz.getID());
        }
    }

    // Compact signature for NumberFormat equality/hash.
    private static final class NFSig {
        final Class<?> type;
        final boolean grouping;
        final boolean parseIntegerOnly;
        final int minInt, maxInt, minFrac, maxFrac;
        final RoundingMode roundingMode; // may be null

        NFSig(Class<?> type, boolean grouping, boolean parseIntegerOnly,
              int minInt, int maxInt, int minFrac, int maxFrac, RoundingMode rm) {
            this.type = type;
            this.grouping = grouping;
            this.parseIntegerOnly = parseIntegerOnly;
            this.minInt = minInt;
            this.maxInt = maxInt;
            this.minFrac = minFrac;
            this.maxFrac = maxFrac;
            this.roundingMode = rm;
        }

        static NFSig of(NumberFormat nf) {
            RoundingMode rm = null;
            try { rm = nf.getRoundingMode(); } catch (Exception ignore) {}
            return new NFSig(
                    nf.getClass(),
                    nf.isGroupingUsed(),
                    nf.isParseIntegerOnly(),
                    nf.getMinimumIntegerDigits(),
                    nf.getMaximumIntegerDigits(),
                    nf.getMinimumFractionDigits(),
                    nf.getMaximumFractionDigits(),
                    rm
            );
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof NFSig)) return false;
            NFSig n = (NFSig) o;
            return grouping == n.grouping
                    && parseIntegerOnly == n.parseIntegerOnly
                    && minInt == n.minInt
                    && maxInt == n.maxInt
                    && minFrac == n.minFrac
                    && maxFrac == n.maxFrac
                    && Objects.equals(type, n.type)
                    && Objects.equals(roundingMode, n.roundingMode);
        }
        @Override
        public int hashCode() {
            return Objects.hash(type, grouping, parseIntegerOnly, minInt, maxInt, minFrac, maxFrac, roundingMode);
        }
    }

    private static NumberFormat defaultNumberFormat() {
        NumberFormat nf = NumberFormat.getNumberInstance();
        nf.setGroupingUsed(false);
        return nf;
    }

    // Instance state (copy-on-write).
    private final AtomicReference<State> stateRef;

    public SafeSimpleDateFormat(String format) {
        Locale locale = Locale.getDefault();
        TimeZone tz = TimeZone.getDefault();
        
        // Initialize parent DateFormat fields to prevent NPEs
        this.calendar = Calendar.getInstance(tz, locale);
        this.numberFormat = defaultNumberFormat();
        
        this.stateRef = new AtomicReference<>(
                new State(format,
                        locale,
                        tz,
                        /* lenient */ true,
                        this.numberFormat,
                        DateFormatSymbols.getInstance(locale),
                        /* twoDigitYearStart */ null)
        );
    }

    private static Map<State, SimpleDateFormat> currentThreadCache() {
        return TL.get();
    }

    private SimpleDateFormat getSdf() {
        State st = stateRef.get();
        return currentThreadCache().computeIfAbsent(st, State::build);
    }

    // ----- Public API (unchanged signatures) -----

    @Override
    public StringBuffer format(Date date, StringBuffer toAppendTo, FieldPosition fieldPosition) {
        return getSdf().format(date, toAppendTo, fieldPosition);
    }

    @Override
    public Date parse(String source, ParsePosition pos) {
        return getSdf().parse(source, pos);
    }

    @Override
    public void setTimeZone(TimeZone tz) {
        update(s -> new State(s.pattern, s.locale, Objects.requireNonNull(tz, "tz"),
                s.lenient, s.nf, s.symbols, s.twoDigitYearStartEpochMs));
        // Keep parent DateFormat fields in sync
        if (this.calendar != null) {
            this.calendar.setTimeZone(tz);
        }
    }

    @Override
    public void setLenient(boolean lenient) {
        update(s -> new State(s.pattern, s.locale, s.tz,
                lenient, s.nf, s.symbols, s.twoDigitYearStartEpochMs));
        // Keep parent DateFormat fields in sync
        if (this.calendar != null) {
            this.calendar.setLenient(lenient);
        }
    }

    @Override
    public void setCalendar(Calendar cal) {
        Objects.requireNonNull(cal, "cal");
        final TimeZone tz = cal.getTimeZone();
        final boolean len = cal.isLenient();
        update(s -> new State(s.pattern, s.locale, tz,
                len, s.nf, s.symbols, s.twoDigitYearStartEpochMs));
        // Keep parent DateFormat field in sync
        this.calendar = cal;
    }

    @Override
    public void setNumberFormat(NumberFormat format) {
        Objects.requireNonNull(format, "format");
        update(s -> new State(s.pattern, s.locale, s.tz,
                s.lenient, format, s.symbols, s.twoDigitYearStartEpochMs));
        // Keep parent DateFormat field in sync
        this.numberFormat = format;
    }

    public void setDateFormatSymbols(DateFormatSymbols symbols) {
        Objects.requireNonNull(symbols, "symbols");
        update(s -> new State(s.pattern, s.locale, s.tz,
                s.lenient, s.nf, symbols, s.twoDigitYearStartEpochMs));
    }

    public void set2DigitYearStart(Date date) {
        Objects.requireNonNull(date, "date");
        final long epochMs = date.getTime();
        update(s -> new State(s.pattern, s.locale, s.tz,
                s.lenient, s.nf, s.symbols, epochMs));
    }

    @Override
    public String toString() {
        return stateRef.get().pattern;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof SafeSimpleDateFormat)) return false;
        SafeSimpleDateFormat that = (SafeSimpleDateFormat) other;
        return this.stateRef.get().equals(that.stateRef.get());
    }

    @Override
    public int hashCode() {
        return stateRef.get().hashCode();
    }

    /**
     * Copy-on-write updater. Replaces the current State with a new one and
     * prunes the old State's formatter from the current thread's cache.
     * Uses compareAndSet for thread-safety under concurrent mutations.
     */
    private void update(java.util.function.UnaryOperator<State> fn) {
        State oldSt;
        State newSt;
        do {
            oldSt = stateRef.get();
            newSt = Objects.requireNonNull(fn.apply(oldSt), "new state");
            if (oldSt.equals(newSt)) {
                return;
            }
        } while (!stateRef.compareAndSet(oldSt, newSt));
        // Prevent per-thread cache growth for this thread:
        currentThreadCache().remove(oldSt);
        // Lazy rebuild on next use; call getSdf() here if you prefer eager.
        // getSdf();
    }

    /** Clears all cached formatters for the current thread (instance-based cache). */
    public static void clearThreadLocalCache() {
        TL.remove();
    }

    /** Clears this instance’s cached formatter for the current thread. */
    public void clearThreadLocal() {
        currentThreadCache().remove(stateRef.get());
    }
}
