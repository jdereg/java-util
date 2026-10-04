package com.cedarsoftware.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.RandomAccess;
import java.util.Set;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A class's hierarchy is kept in two arrays (its supertypes and their distances), and getDistanceMap() and the
 * supertypes are read-only views of them. It was a set and a map of the same types, about 1,500 bytes per class; now
 * about 840. The views keep the Map and Set contracts, and list the types breadth-first -- the map's order was a
 * HashMap's, which varied from run to run.
 */
class ClassUtilitiesHierarchyViewsTest {

    /** Implemented by nothing. */
    interface Unrelated {
    }

    private static final Class<?>[] TYPES = {ArrayList.class, String.class, Integer.class, int.class, Object.class,
            Runnable.class, String[].class, int[][].class, java.util.HashMap.class, Thread.class};

    @Test
    void theDistanceMapKeepsTheMapContract() {
        for (Class<?> type : TYPES) {
            Map<Class<?>, Integer> map = ClassUtilities.getClassHierarchyInfo(type).getDistanceMap();
            Map<Class<?>, Integer> copy = new HashMap<>(map);
            assertEquals(copy, map, type.getName());
            assertEquals(map, copy, type.getName());
            assertEquals(copy.hashCode(), map.hashCode(), type.getName());
            assertEquals(copy.size(), map.size());
            for (Map.Entry<Class<?>, Integer> e : copy.entrySet()) {
                assertEquals(e.getValue(), map.get(e.getKey()));
                assertTrue(map.containsKey(e.getKey()));
            }
            assertEquals(Integer.valueOf(0), map.get(type));
            assertNull(map.get(Unrelated.class));
            assertFalse(map.containsKey(Unrelated.class));
            assertNull(map.get("not a class"));
            assertFalse(map.containsKey("not a class"));
        }
    }

    @Test
    void theTypesComeBreadthFirstTheClassItselfFirst() {
        for (Class<?> type : TYPES) {
            ClassUtilities.ClassHierarchyInfo info = ClassUtilities.getClassHierarchyInfo(type);
            List<Class<?>> keys = new ArrayList<>(info.getDistanceMap().keySet());
            assertEquals(type, keys.get(0));
            int last = 0;
            for (Class<?> key : keys) {
                if (key.isPrimitive()) {
                    continue;       // a wrapper's primitive is listed last, one step away
                }
                int d = info.getDistanceMap().get(key);
                assertTrue(d >= last, type.getName() + ": " + keys);
                last = d;
            }
            assertEquals(keys, new ArrayList<>(info.getAllSupertypes()), "the supertypes list the same, in order");
        }
        assertEquals(Arrays.asList(ArrayList.class),
                new ArrayList<>(ClassUtilities.getClassHierarchyInfo(ArrayList.class)
                        .getDistanceMap().keySet()).subList(0, 1));
    }

    @Test
    void theSupertypesKeepTheSetContract() {
        Set<Class<?>> supertypes = ClassUtilities.getClassHierarchyInfo(ArrayList.class).getAllSupertypes();
        Set<Class<?>> copy = new LinkedHashSet<>(supertypes);
        assertEquals(copy, supertypes);
        assertEquals(supertypes, copy);
        assertEquals(copy.hashCode(), supertypes.hashCode());
        assertTrue(supertypes.contains(RandomAccess.class));
        assertTrue(supertypes.contains(Object.class));
        assertFalse(supertypes.contains(Map.class));
        assertFalse(supertypes.contains("not a class"));
    }

    @Test
    void theViewsAreReadOnly() {
        ClassUtilities.ClassHierarchyInfo info = ClassUtilities.getClassHierarchyInfo(ArrayList.class);
        Map<Class<?>, Integer> map = info.getDistanceMap();
        assertThrows(UnsupportedOperationException.class, () -> map.put(Map.class, 1));
        assertThrows(UnsupportedOperationException.class, () -> map.remove(ArrayList.class));
        assertThrows(UnsupportedOperationException.class, map::clear);
        Iterator<Map.Entry<Class<?>, Integer>> entries = map.entrySet().iterator();
        entries.next();
        assertThrows(UnsupportedOperationException.class, entries::remove);
        assertThrows(UnsupportedOperationException.class, () -> entries.next().setValue(7));
        Set<Class<?>> supertypes = info.getAllSupertypes();
        assertThrows(UnsupportedOperationException.class, () -> supertypes.add(Map.class));
        assertThrows(UnsupportedOperationException.class, () -> supertypes.remove(Object.class));
        assertEquals(Integer.valueOf(0), map.get(ArrayList.class));
    }
}
