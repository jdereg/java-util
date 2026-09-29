package com.cedarsoftware.util;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * MultiKeyMap.entrySet() is a snapshot held in a list; it was a LinkedHashMap's entry set, which hashed every
 * reported key for callers that mostly only iterate. It behaves as that snapshot did, which is the oracle here: a
 * LinkedHashMap filled from the same entries -- order, toString(), hashCode(), equals(), contains() by Java equality
 * of the key and then the whole entry, iterator removal, clear(), setValue(). contains() never hashes a value, so a
 * value whose graph leads back to the map is fine.
 */
class MultiKeyMapEntrySetSnapshotTest {

    private static Object randomKey(Random r) {
        switch (r.nextInt(6)) {
            case 0: return r.nextInt(30);
            case 1: return "k" + r.nextInt(30);
            case 2: return new Object[]{"a" + r.nextInt(5), r.nextInt(5)};
            case 3: return Arrays.asList(r.nextInt(5), "b" + r.nextInt(5));
            case 4: return new LinkedHashSet<>(Arrays.asList(r.nextInt(5), r.nextInt(5)));
            default: return new Object[]{"n", Arrays.asList(r.nextInt(3), r.nextInt(3))};
        }
    }

    private static List<MultiKeyMap<Integer>> maps(Random r) {
        List<MultiKeyMap<Integer>> maps = new ArrayList<>();
        maps.add(new MultiKeyMap<>());
        maps.add(MultiKeyMap.<Integer>builder().flattenDimensions(true).build());
        maps.add(MultiKeyMap.<Integer>builder().caseSensitive(false).build());
        maps.add(MultiKeyMap.<Integer>builder()
                .collectionKeyMode(MultiKeyMap.CollectionKeyMode.COLLECTIONS_NOT_EXPANDED).build());
        for (MultiKeyMap<Integer> m : maps) {
            for (int i = 0; i < 300; i++) {
                m.put(randomKey(r), i);
            }
        }
        return maps;
    }

    @Test
    void behavesAsTheLinkedHashMapSnapshotDid() {
        Random r = new Random(17);
        for (MultiKeyMap<Integer> map : maps(r)) {
            Set<Map.Entry<Object, Integer>> snapshot = map.entrySet();
            Map<Object, Integer> oracleMap = new LinkedHashMap<>();
            for (Map.Entry<Object, Integer> e : snapshot) {
                oracleMap.put(e.getKey(), e.getValue());
            }
            Set<Map.Entry<Object, Integer>> oracle = oracleMap.entrySet();

            assertEquals(oracle.size(), snapshot.size());
            assertEquals(oracle.toString(), snapshot.toString());
            assertEquals(oracle.hashCode(), snapshot.hashCode());
            assertEquals(oracle, snapshot);
            assertEquals(snapshot, oracle);
            Iterator<Map.Entry<Object, Integer>> a = snapshot.iterator();
            Iterator<Map.Entry<Object, Integer>> b = oracle.iterator();
            while (b.hasNext()) {
                Map.Entry<Object, Integer> x = a.next();
                Map.Entry<Object, Integer> y = b.next();
                assertTrue(x.getKey() == y.getKey() && x.getValue().equals(y.getValue()), "order");
            }
            assertFalse(a.hasNext());

            List<Object> probes = new ArrayList<>(oracle);
            for (Map.Entry<Object, Integer> e : oracle) {
                probes.add(new AbstractMap.SimpleEntry<>(e.getKey(), e.getValue() + 1));    // value differs
                if (e.getKey() instanceof Integer) {
                    probes.add(new AbstractMap.SimpleEntry<>(((Integer) e.getKey()).doubleValue(), e.getValue()));
                }
                if (e.getKey() instanceof List) {
                    probes.add(new AbstractMap.SimpleEntry<>(new ArrayList<>((List<?>) e.getKey()), e.getValue()));
                }
            }
            probes.add("not an entry");
            probes.add(null);
            for (Object probe : probes) {
                assertEquals(oracle.contains(probe), snapshot.contains(probe), String.valueOf(probe));
            }

            // Removal through the iterator, then contains() and size() agree again
            Iterator<Map.Entry<Object, Integer>> it = snapshot.iterator();
            Iterator<Map.Entry<Object, Integer>> ot = oracle.iterator();
            int i = 0;
            while (it.hasNext()) {
                it.next();
                ot.next();
                if (i++ % 3 == 0) {
                    it.remove();
                    ot.remove();
                }
            }
            assertEquals(oracle.size(), snapshot.size());
            for (Object probe : probes) {
                assertEquals(oracle.contains(probe), snapshot.contains(probe), "after removal: " + probe);
            }
            assertEquals(oracle, snapshot);

            // setValue() changes the snapshot's entry, not the map
            Map.Entry<Object, Integer> first = snapshot.iterator().next();
            Integer before = map.get(first.getKey());
            first.setValue(-1);
            assertEquals(-1, snapshot.iterator().next().getValue());
            assertEquals(before, map.get(first.getKey()));

            snapshot.clear();
            assertTrue(snapshot.isEmpty());
            assertFalse(snapshot.contains(first));
        }
    }

    /** contains() compares whole entries without hashing values, so a value that holds the map is fine. */
    @Test
    void containsDoesNotHashValues() {
        MultiKeyMap<Object> map = new MultiKeyMap<>();
        List<Object> holdsTheMap = new ArrayList<>();
        holdsTheMap.add(map);
        map.put("k", holdsTheMap);
        Set<Map.Entry<Object, Object>> snapshot = map.entrySet();
        assertTrue(snapshot.contains(new AbstractMap.SimpleEntry<>("k", holdsTheMap)));
        assertFalse(snapshot.contains(new AbstractMap.SimpleEntry<>("k", "other")));
        assertFalse(snapshot.contains(new AbstractMap.SimpleEntry<>("x", holdsTheMap)));
    }
}
