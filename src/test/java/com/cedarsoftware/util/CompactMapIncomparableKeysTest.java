package com.cedarsoftware.util;

import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * A sorted or reverse-ordered CompactMap orders keys with CompactMapComparator. Two keys of one class that is not
 * Comparable (two ArrayLists, say) have no order; the comparator ordered them by identity, so two equal keys were kept
 * as two entries and get() of an equal key answered null. As its Javadoc promised and as TreeMap does, such keys are
 * now refused with ClassCastException. Keys of different classes still order by class name, and putting the same
 * instance again replaces its value. Two classes of one name, from two class loaders, are two classes: their keys are
 * kept, ordered by identity as before.
 */
class CompactMapIncomparableKeysTest {

    private static List<Integer> list(Integer... values) {
        return new ArrayList<>(Arrays.asList(values));
    }

    @Test
    void twoKeysOfAClassThatIsNotComparableAreRefused() {
        for (CompactMap<Object, Object> map : Arrays.asList(CompactMap.builder().sortedOrder().build(),
                CompactMap.builder().reverseOrder().build())) {
            map.put("z", 0);
            List<Integer> key = list(1);
            map.put(key, "first");
            assertThrows(ClassCastException.class, () -> map.put(list(1), "second"));
            assertThrows(ClassCastException.class, () -> map.get(list(1)));
            assertEquals(2, map.size());
            map.put(key, "again");                      // the same instance: its value is replaced
            assertEquals("again", map.get(key));
            assertEquals(2, map.size());
        }
    }

    @Test
    void aMapOfOneEntryThatRefusesASecondKeyStillHoldsItsOneEntry() {
        CompactMap<Object, Object> map = CompactMap.builder().sortedOrder().build();
        List<Integer> key = list(1);
        map.put(key, "first");
        CompactMap.LogicalValueType before = map.getLogicalValueType();
        assertThrows(ClassCastException.class, () -> map.put(list(2), "second"));
        assertEquals(before, map.getLogicalValueType());
        assertEquals(1, map.size());
        assertEquals("first", map.get(key));
        map.put("a", 2);
        assertEquals(Arrays.asList("a", key), new ArrayList<>(map.keySet()));
        map.remove("a");
        assertEquals(before, map.getLogicalValueType());
    }

    @Test
    void theSameHoldsOnceTheMapHasABackingMap() {
        CompactMap<Object, Object> map = CompactMap.builder().sortedOrder().compactSize(2).build();
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
        List<Integer> key = list(7);
        map.put(key, 4);
        assertEquals(CompactMap.LogicalValueType.MAP, map.getLogicalValueType());
        assertThrows(ClassCastException.class, () -> map.put(list(7), 5));
        assertEquals(4, map.get(key));
        assertEquals(4, map.size());
    }

    @Test
    void keysOfDifferentClassesStillOrderByClassName() {
        CompactMap<Object, Object> map = CompactMap.builder().sortedOrder().build();
        map.put("b", 1);
        map.put(2, 2);
        map.put(list(3), 3);
        map.put("a", 4);
        map.put(1, 5);
        assertEquals(Arrays.asList(1, 2, "a", "b", list(3)), new ArrayList<>(map.keySet()));   // Integer, String, ArrayList
    }

    /** Not Comparable. */
    static final class Key {
    }

    /** {@code c} defined again, by a class loader of its own: a second class of the same name. */
    private static Class<?> definedAgain(Class<?> c) throws Exception {
        byte[] bytes;
        try (InputStream in = c.getClassLoader().getResourceAsStream(c.getName().replace('.', '/') + ".class")) {
            bytes = IOUtilities.inputStreamToBytes(in);
        }
        return new ClassLoader(c.getClassLoader()) {
            Class<?> define() {
                return defineClass(c.getName(), bytes, 0, bytes.length);
            }
        }.define();
    }

    @Test
    void keysOfOneClassNameFromTwoClassLoadersAreTwoKeys() throws Exception {
        Class<?> again = definedAgain(Key.class);
        assertNotSame(Key.class, again);
        assertEquals(Key.class.getName(), again.getName());
        Constructor<?> constructor = again.getDeclaredConstructor();
        constructor.setAccessible(true);
        Object first = new Key();
        Object second = constructor.newInstance();

        CompactMap<Object, Object> map = CompactMap.builder().sortedOrder().build();
        map.put(first, 1);
        map.put(second, 2);
        assertEquals(2, map.size());
        assertEquals(1, map.get(first));
        assertEquals(2, map.get(second));
    }
}
