package com.cedarsoftware.util;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * A legacy subclass backed by a SortedMap finds keys in that map by its comparator -- BigDecimal 1.0 and 1.00 are one
 * key, and so are two keys a custom comparator calls equal -- but in its array it found them by equality, and sorted
 * them for iteration by CompactMapComparator, whatever the backing map's comparator. So the same map answered
 * differently, and iterated in a different order, before and after it outgrew its array. Now the array follows the
 * backing map's comparator too, at every size; keys that comparator cannot compare are refused at once.
 */
class CompactMapLegacySortedTest {

    /** A legacy subclass of compact size 3 backed by a TreeMap with the given comparator (null: natural order). */
    static class Sorted3<K, V> extends CompactMap<K, V> {
        private final Comparator<? super K> comparator;

        Sorted3(Comparator<? super K> comparator) {
            this.comparator = comparator;
        }

        @Override
        protected int compactSize() {
            return 3;
        }

        @Override
        protected Map<K, V> getNewMap() {
            return new TreeMap<>(comparator);
        }
    }

    /** One class per comparator: the order is worked out once per class, as for any legacy subclass. */
    static class Natural<K, V> extends Sorted3<K, V> {
        Natural() {
            super(null);
        }
    }

    static class ByLength<V> extends Sorted3<String, V> {
        ByLength() {
            super(Comparator.comparingInt(String::length));
        }
    }

    static class Reversed<K, V> extends Sorted3<K, V> {
        Reversed() {
            super(Collections.reverseOrder());
        }
    }

    /** At every size from empty to past the compact size, the map answers and iterates as a TreeMap of its order. */
    private static <K> void answersAsATreeMap(java.util.function.Supplier<Map<K, Object>> make,
                                              Comparator<? super K> comparator, Function<Integer, K> stored,
                                              Function<Integer, K> probe) {
        for (int size = 0; size <= 5; size++) {
            Map<K, Object> map = make.get();
            TreeMap<K, Object> expected = new TreeMap<>(comparator);
            for (int i = 1; i <= size; i++) {
                map.put(stored.apply(i), i);
                expected.put(stored.apply(i), i);
            }
            String what = map.getClass().getSimpleName() + " size " + size;
            assertEquals(new ArrayList<>(expected.entrySet()), new ArrayList<>(map.entrySet()), what);
            for (int i = 1; i <= size + 1; i++) {
                K key = probe.apply(i);
                assertEquals(expected.get(key), map.get(key), what + " get " + key);
                assertEquals(expected.containsKey(key), map.containsKey(key), what + " containsKey " + key);
            }
            for (int i = 1; i <= size + 1; i++) {
                K key = probe.apply(i);
                assertEquals(expected.put(key, -i), map.put(key, -i), what + " put " + key);
                assertEquals(new ArrayList<>(expected.entrySet()), new ArrayList<>(map.entrySet()),
                        what + " after put " + key);
            }
            for (int i = size + 1; i >= 1; i--) {
                K key = probe.apply(i);
                assertEquals(expected.remove(key), map.remove(key), what + " remove " + key);
                assertEquals(new ArrayList<>(expected.entrySet()), new ArrayList<>(map.entrySet()),
                        what + " after remove " + key);
            }
        }
    }

    @Test
    void naturalOrderMakesNumericallyEqualBigDecimalsOneKey() {
        answersAsATreeMap(Natural::new, null, i -> new BigDecimal(i + ".0"), i -> new BigDecimal(i + ".00"));
    }

    @Test
    void aCustomComparatorDecidesWhichKeysAreOneKeyAndTheirOrder() {
        String[] stored = {null, "c", "aa", "bbb", "dddd", "eeeee", "ffffff"};
        String[] probes = {null, "x", "yy", "zzz", "wwww", "vvvvv", "uuuuuu"};
        answersAsATreeMap(ByLength::new, Comparator.comparingInt(String::length), i -> stored[7 - i], i -> probes[i]);
    }

    @Test
    void aReversedOrderIteratesInReverse() {
        answersAsATreeMap(Reversed::new, Collections.reverseOrder(), i -> "k" + i, i -> "k" + i);
    }

    @Test
    void keysTheBackingMapCouldNotCompareAreRefusedAtOnce() {
        for (int entries = 1; entries <= 3; entries++) {
            Map<Object, Object> map = new Natural<>();
            for (int i = 0; i < entries; i++) {
                map.put("k" + i, i);
            }
            assertThrows(ClassCastException.class, () -> map.put(7, "seven"));
            assertEquals(entries, map.size());
            for (int i = entries; i < 6; i++) {
                map.put("k" + i, i);                // and it still grows into its TreeMap
            }
            assertEquals(6, map.size());
        }
    }

    @Test
    void legacyMapsThatAreNotSortedStillMatchByEquality() {
        List<Map<Object, Object>> maps = Arrays.asList(new CompactMap<>(), new CompactLinkedMap<>(),
                new CompactMap<Object, Object>() {
                    @Override
                    protected int compactSize() {
                        return 3;
                    }
                });
        for (Map<Object, Object> map : maps) {
            map.put(new BigDecimal("1.0"), "a");
            assertNull(map.get(new BigDecimal("1.00")));
            map.put(new BigDecimal("1.00"), "b");
            map.put(7, "mixed types are fine here");
            assertEquals(3, map.size());
        }
    }
}
