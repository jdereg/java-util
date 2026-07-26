package com.cedarsoftware.util;

import java.util.Random;

import org.apache.commons.collections4.keyvalue.MultiKey;
import org.apache.commons.collections4.map.MultiKeyMap;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * Tracks Cedar's {@link com.cedarsoftware.util.MultiKeyMap} against Apache Commons Collections'
 * {@code MultiKeyMap} for key counts 1-6, recording each result as a ratio in {@link PerfRatchet}.
 * <p>
 * Apache is the right yardstick here — it is the library Cedar's implementation exists to beat — and
 * expressing the result as a ratio makes it portable: a CI runner half the speed of a dev laptop slows
 * both sides equally, so the recorded number still means "Cedar costs Nx what Apache costs".
 *
 * <h2>What this replaces</h2>
 * The previous version ran a 6&times;7 matrix of key counts and data sizes up to 250,000 entries, ten
 * iterations each, for both implementations: roughly <b>105 million map operations</b>, plus a
 * {@code Thread.sleep(100)} and an explicit {@code System.gc()} per configuration (4.2 seconds of pure
 * sleeping and 42 forced collections). It took 25 seconds and contained <b>no assertions</b> — it
 * printed a 42-row table that a human had to compare against remembered numbers.
 * <p>
 * Two of the seven data sizes (100,000 and 250,000) accounted for 80% of that work while telling us
 * nothing the smaller sizes did not. The matrix collapses to one representative size per key count,
 * because key-count cost is what actually differs between the implementations; data size mostly
 * re-measures the backing table's growth policy.
 */
class MultiKeyMapPerformanceComparisonTest {

    /** Entries per trial. Large enough that a trial outlasts timer noise and reaches C2. */
    private static final int ENTRIES = 20000;

    private static final int[] KEY_COUNTS = {1, 2, 3, 4, 5, 6};

    /** Untimed passes over every key count before the first measurement. See {@link PerfRatchet#warmAll}. */
    private static final int WARM_ROUNDS = 3;

    @EnabledIfSystemProperty(named = "performRelease", matches = "true")
    @Test
    void perfPutRatioVsApache() {
        int n = KEY_COUNTS.length;
        Runnable[] cedar = new Runnable[n];
        Runnable[] apache = new Runnable[n];

        for (int k = 0; k < n; k++) {
            final int kc = KEY_COUNTS[k];
            final Object[][] keys = generateKeys(ENTRIES, kc);
            final String[] values = generateValues(ENTRIES);

            cedar[k] = () -> {
                com.cedarsoftware.util.MultiKeyMap<String> map =
                        com.cedarsoftware.util.MultiKeyMap.<String>builder()
                                .simpleKeysMode(true)
                                .capacity((int) (ENTRIES / 0.75) + 1)
                                .build();
                for (int i = 0; i < keys.length; i++) {
                    if (kc == 1) {
                        map.put(keys[i][0], values[i]);
                    } else {
                        map.putMultiKey(values[i], keys[i]);
                    }
                }
                PerfRatchet.sink = map;
            };
            apache[k] = () -> {
                MultiKeyMap<Object, String> map = new MultiKeyMap<>();
                for (int i = 0; i < keys.length; i++) {
                    if (kc == 1) {
                        map.put(new MultiKey<>(new Object[]{keys[i][0]}), values[i]);
                    } else {
                        map.put(new MultiKey<>(keys[i]), values[i]);
                    }
                }
                PerfRatchet.sink = map;
            };
        }

        // Warm EVERY key count before measuring any, so keys1 does not absorb the JIT cost for all.
        PerfRatchet.warmAll(WARM_ROUNDS, cedar);
        PerfRatchet.warmAll(WARM_ROUNDS, apache);

        for (int k = 0; k < n; k++) {
            failIfStrictRegression(PerfRatchet.compare("multikeymap.put.keys" + KEY_COUNTS[k], ENTRIES,
                    "Cedar.putMultiKey", cedar[k], "Apache.put", apache[k]));
        }
    }

    @EnabledIfSystemProperty(named = "performRelease", matches = "true")
    @Test
    void perfGetRatioVsApache() {
        int n = KEY_COUNTS.length;
        Runnable[] cedarGet = new Runnable[n];
        Runnable[] apacheGet = new Runnable[n];

        for (int k = 0; k < n; k++) {
            final int kc = KEY_COUNTS[k];
            final Object[][] keys = generateKeys(ENTRIES, kc);
            final String[] values = generateValues(ENTRIES);

            // Population happens outside the timed workload so this measures lookup, not insertion.
            final com.cedarsoftware.util.MultiKeyMap<String> cedar =
                    com.cedarsoftware.util.MultiKeyMap.<String>builder()
                            .simpleKeysMode(true)
                            .capacity((int) (ENTRIES / 0.75) + 1)
                            .build();
            final MultiKeyMap<Object, String> apache = new MultiKeyMap<>();
            for (int i = 0; i < keys.length; i++) {
                if (kc == 1) {
                    cedar.put(keys[i][0], values[i]);
                    apache.put(new MultiKey<>(new Object[]{keys[i][0]}), values[i]);
                } else {
                    cedar.putMultiKey(values[i], keys[i]);
                    apache.put(new MultiKey<>(keys[i]), values[i]);
                }
            }

            cedarGet[k] = () -> {
                for (Object[] key : keys) {
                    PerfRatchet.sink = kc == 1 ? cedar.get(key[0]) : cedar.getMultiKey(key);
                }
            };
            apacheGet[k] = () -> {
                for (Object[] key : keys) {
                    PerfRatchet.sink = kc == 1
                            ? apache.get(new MultiKey<>(new Object[]{key[0]}))
                            : apache.get(new MultiKey<>(key));
                }
            };
        }

        PerfRatchet.warmAll(WARM_ROUNDS, cedarGet);
        PerfRatchet.warmAll(WARM_ROUNDS, apacheGet);

        for (int k = 0; k < n; k++) {
            failIfStrictRegression(PerfRatchet.compare("multikeymap.get.keys" + KEY_COUNTS[k], ENTRIES,
                    "Cedar.getMultiKey", cedarGet[k], "Apache.get", apacheGet[k]));
        }
    }

    /**
     * Fixed seed, so a recorded ratio reflects a code change rather than a different key distribution.
     * Mixes String/Integer/Long/Double to exercise the hashing paths a single key type would not.
     */
    private static Object[][] generateKeys(int count, int keyCount) {
        Object[][] keys = new Object[count][keyCount];
        for (int i = 0; i < count; i++) {
            for (int j = 0; j < keyCount; j++) {
                switch (j % 4) {
                    case 0:
                        keys[i][j] = "key" + i + "_" + j;
                        break;
                    case 1:
                        keys[i][j] = i * 1000 + j;
                        break;
                    case 2:
                        keys[i][j] = i * 1000000L + j;
                        break;
                    default:
                        keys[i][j] = i + j / 10.0;
                        break;
                }
            }
        }
        return keys;
    }

    private static String[] generateValues(int count) {
        String[] values = new String[count];
        for (int i = 0; i < count; i++) {
            values[i] = "value_" + i;
        }
        return values;
    }

    /** Reported, not asserted — see {@link PerfRatchet}. Only -Dperf.strict=true fails a regression. */
    private static void failIfStrictRegression(PerfRatchet.Metric m) {
        if (PerfRatchet.isStrict() && m.getVerdict() == PerfRatchet.Verdict.REGRESSED) {
            fail("Performance regression (perf.strict enabled): ratio " + m.getRatio());
        }
    }
}
