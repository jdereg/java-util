package com.cedarsoftware.util;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A sorted or reverse-ordered CompactMap finds keys with its comparator in its array and in its backing map, so keys
 * the comparator calls equal are one key, as in a TreeMap: BigDecimal 1.0 and 1.00. Holding one or two entries it
 * matched by equality instead: get(1.00) missed a stored 1.0, and put(1.00) left a one-entry array that printed as
 * {id=[Ljava.lang.Object;@...} and that remove(1.00) could not empty. Every size now matches as the larger ones do.
 */
class CompactMapSortedKeyMatchingTest {

    private static BigDecimal number(String text) {
        return new BigDecimal(text);
    }

    private static List<CompactMap<Object, Object>> sortedMaps() {
        return Arrays.asList(CompactMap.builder().sortedOrder().compactSize(3).build(),
                CompactMap.builder().reverseOrder().compactSize(3).build());
    }

    @Test
    void oneEntryMatchesByTheComparator() {
        for (CompactMap<Object, Object> map : sortedMaps()) {
            map.put(number("1.0"), "a");
            assertEquals("a", map.get(number("1.00")));
            assertTrue(map.containsKey(number("1.00")));
            assertEquals("a", map.put(number("1.00"), "b"));
            assertEquals(1, map.size());
            assertEquals(CompactMap.LogicalValueType.ENTRY, map.getLogicalValueType());
            assertEquals("{1.0=b}", map.toString());                 // the stored key stays, as in a TreeMap
            assertEquals("b", map.remove(number("1.00")));
            assertTrue(map.isEmpty());
        }
    }

    @Test
    void twoEntriesMatchByTheComparator() {
        for (CompactMap<Object, Object> map : sortedMaps()) {
            map.put(number("1.0"), "a");
            map.put(number("2"), "b");
            assertEquals("a", map.remove(number("1.00")));
            assertEquals(Collections.singletonMap(number("2"), "b"), new TreeMap<>(map));
            assertEquals(1, map.size());
        }
    }

    /** At every size from empty to past the compact size, the map answers as a TreeMap of the same order. */
    @Test
    void everySizeAnswersAsATreeMap() {
        for (boolean reverse : new boolean[]{false, true}) {
            for (int size = 0; size <= 5; size++) {
                CompactMap.Builder<Object, Object> builder = CompactMap.builder().compactSize(3);
                CompactMap<Object, Object> map = (reverse ? builder.reverseOrder() : builder.sortedOrder()).build();
                TreeMap<Object, Object> expected = reverse ? new TreeMap<>(Collections.reverseOrder()) : new TreeMap<>();
                for (int i = 1; i <= size; i++) {
                    map.put(number(i + ".0"), i);
                    expected.put(number(i + ".0"), i);
                }
                String what = (reverse ? "reverse" : "sorted") + " size " + size;
                for (int i = 1; i <= size + 1; i++) {
                    BigDecimal probe = number(i + ".00");
                    assertEquals(expected.get(probe), map.get(probe), what + " get " + probe);
                    assertEquals(expected.containsKey(probe), map.containsKey(probe), what + " containsKey " + probe);
                }
                for (int i = 1; i <= size + 1; i++) {
                    BigDecimal probe = number(i + ".000");
                    assertEquals(expected.put(probe, -i), map.put(probe, -i), what + " put " + probe);
                    assertEquals(expected.size(), map.size(), what + " size after put " + probe);
                    assertEquals(new ArrayList<>(expected.entrySet()), new ArrayList<>(map.entrySet()), what);
                }
                for (int i = size + 1; i >= 1; i--) {
                    BigDecimal probe = number(i + ".0000");
                    assertEquals(expected.remove(probe), map.remove(probe), what + " remove " + probe);
                    assertEquals(expected.size(), map.size(), what + " size after remove " + probe);
                }
                assertTrue(map.isEmpty(), what);
            }
        }
    }

    /** An ordering read from a document is a string made at run time, not the constant: it sorts the same. */
    @Test
    void anOrderingMadeAtRunTimeMatchesByTheComparatorToo() {
        for (String ordering : new String[]{CompactMap.SORTED, CompactMap.REVERSE}) {
            Map<String, Object> options = new HashMap<>();
            options.put(CompactMap.ORDERING, new String(ordering.toCharArray()));
            options.put(CompactMap.SINGLE_KEY, "sortedKeyMatchingTest");      // a class of its own
            CompactMap<Object, Object> map = CompactMap.newMap(options);
            assertEquals(ordering, map.getConfig().get(CompactMap.ORDERING));
            map.put(number("1.0"), "a");
            assertEquals("a", map.get(number("1.00")));
            assertEquals("a", map.put(number("1.00"), "b"));
            assertEquals(1, map.size());
        }
    }

    @Test
    void mapsThatAreNotSortedStillMatchByEquality() {
        for (Map<Object, Object> map : Arrays.<Map<Object, Object>>asList(CompactMap.builder().build(),
                CompactMap.builder().insertionOrder().build(), new CompactLinkedMap<>())) {
            map.put(number("1.0"), "a");
            assertNull(map.get(number("1.00")));
            map.put(number("1.00"), "b");
            assertEquals(2, map.size());
        }
    }

    @Test
    void aCaseInsensitiveSortedMapStillFoldsCase() {
        CompactMap<Object, Object> map = CompactMap.builder().caseSensitive(false).sortedOrder().build();
        map.put("Abc", 1);
        assertEquals(1, map.get("ABC"));
        map.put("ABC", 2);
        assertEquals("{Abc=2}", map.toString());
    }
}
