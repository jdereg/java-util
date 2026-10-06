package com.cedarsoftware.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NavigableSet;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The null-safe concurrent collections serialize, as the JDK collections they stand in for do: ConcurrentHashMap,
 * ConcurrentSkipListMap and ConcurrentSkipListSet. They threw NotSerializableException, so a ConcurrentHashMapNullSafe
 * in an HTTP session, a cache configured for Java serialization or an RMI call failed where a ConcurrentHashMap
 * worked. The entries are written with real nulls (the placeholders that stand for null inside are never written),
 * with the comparator, and read back through the public constructors.
 */
class NullSafeConcurrentSerializationTest {

    @SuppressWarnings("unchecked")
    private static <T> T roundTrip(T object) throws IOException, ClassNotFoundException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(bytes)) {
            out.writeObject(object);
        }
        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
            return (T) in.readObject();
        }
    }

    @Test
    void concurrentHashMapNullSafe() throws Exception {
        ConcurrentHashMapNullSafe<String, String> map = new ConcurrentHashMapNullSafe<>();
        map.put("a", "1");
        map.put(null, "null key");
        map.put("b", null);
        ConcurrentHashMapNullSafe<String, String> back = roundTrip(map);
        assertEquals(map, back);
        assertEquals("null key", back.get(null));
        assertTrue(back.containsKey("b"));
        assertNull(back.get("b"));
        assertEquals(3, back.size());
        back.put("c", "3");                                     // a working map
        assertEquals("3", back.get("c"));
        assertEquals(new ConcurrentHashMapNullSafe<String, String>(), roundTrip(new ConcurrentHashMapNullSafe<>()));
    }

    @Test
    void concurrentNavigableMapNullSafe() throws Exception {
        ConcurrentNavigableMapNullSafe<String, String> natural = new ConcurrentNavigableMapNullSafe<>();
        natural.put("b", "2");
        natural.put(null, "null key");
        natural.put("a", null);
        ConcurrentNavigableMapNullSafe<String, String> back = roundTrip(natural);
        assertEquals(natural, back);
        assertEquals(new ArrayList<>(natural.keySet()), new ArrayList<>(back.keySet()));
        assertNull(back.comparator());

        ConcurrentNavigableMapNullSafe<String, String> caseless =
                new ConcurrentNavigableMapNullSafe<>(String.CASE_INSENSITIVE_ORDER);
        caseless.put("Banana", "y");
        caseless.put("apple", "x");
        ConcurrentNavigableMapNullSafe<String, String> caselessBack = roundTrip(caseless);
        assertSame(String.CASE_INSENSITIVE_ORDER, caselessBack.comparator());
        assertEquals("x", caselessBack.get("APPLE"));
        assertEquals(Arrays.asList("apple", "Banana"), new ArrayList<>(caselessBack.keySet()));

        // a descending view comes back descending
        NavigableMap<String, String> descending = roundTrip(caseless.descendingMap());
        assertEquals(Arrays.asList("Banana", "apple"), new ArrayList<>(descending.keySet()));
        descending.put("cherry", "z");
        assertEquals("cherry", descending.firstKey());

        assertEquals(new ConcurrentNavigableMapNullSafe<String, String>(),
                roundTrip(new ConcurrentNavigableMapNullSafe<String, String>()));
    }

    @Test
    void concurrentNavigableSetNullSafe() throws Exception {
        ConcurrentNavigableSetNullSafe<String> set = new ConcurrentNavigableSetNullSafe<>();
        set.addAll(Arrays.asList("b", null, "a"));
        ConcurrentNavigableSetNullSafe<String> back = roundTrip(set);
        assertEquals(set, back);
        assertEquals(new ArrayList<>(set), new ArrayList<>(back));
        assertTrue(back.contains(null));

        ConcurrentNavigableSetNullSafe<String> caseless =
                new ConcurrentNavigableSetNullSafe<>(String.CASE_INSENSITIVE_ORDER);
        caseless.addAll(Arrays.asList("Banana", "apple"));
        ConcurrentNavigableSetNullSafe<String> caselessBack = roundTrip(caseless);
        assertSame(String.CASE_INSENSITIVE_ORDER, caselessBack.comparator());
        assertTrue(caselessBack.contains("APPLE"));

        NavigableSet<String> descending = roundTrip(caseless.descendingSet());
        assertEquals(Arrays.asList("Banana", "apple"), new ArrayList<>(descending));
        descending.add("cherry");
        assertEquals("cherry", descending.first());

        NavigableSet<String> descendingNatural = roundTrip(set.descendingSet());
        assertEquals(new ArrayList<>(set.descendingSet()), new ArrayList<>(descendingNatural));

        assertEquals(new ConcurrentNavigableSetNullSafe<String>(), roundTrip(new ConcurrentNavigableSetNullSafe<>()));
    }

    /** As for TreeMap: a comparator that cannot be serialized cannot be written. */
    @Test
    void aComparatorThatIsNotSerializableIsRefused() {
        Comparator<String> lambda = (a, b) -> a.compareTo(b);
        ConcurrentNavigableMapNullSafe<String, String> map = new ConcurrentNavigableMapNullSafe<>(lambda);
        map.put("a", "1");
        assertThrows(NotSerializableException.class, () -> roundTrip(map));
        ConcurrentNavigableSetNullSafe<String> set = new ConcurrentNavigableSetNullSafe<>(lambda);
        set.add("a");
        assertThrows(NotSerializableException.class, () -> roundTrip(set));
    }

    /** Serialized inside another object, as an HTTP session would hold one. */
    @Test
    void nestedInAnotherSerializedObject() throws Exception {
        Map<String, Object> session = new LinkedHashMap<>();
        ConcurrentHashMapNullSafe<String, String> map = new ConcurrentHashMapNullSafe<>();
        map.put(null, null);
        List<Object> holder = new ArrayList<>(Arrays.asList(map, map));
        session.put("data", holder);
        Map<String, Object> back = roundTrip(session);
        @SuppressWarnings("unchecked")
        List<Object> list = (List<Object>) back.get("data");
        assertSame(list.get(0), list.get(1));                   // one object, written once
        assertEquals(map, list.get(0));
    }
}
