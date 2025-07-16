package com.cedarsoftware.util;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.Random;
import java.util.Set;
import java.util.SortedMap;
import java.util.logging.Logger;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListMap;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.fail;

/**
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
class CaseInsensitiveMapTest
{
    private static final Logger LOG = Logger.getLogger(CaseInsensitiveMapTest.class.getName());
    @AfterEach
    public void cleanup() {
        // Reset to default for other tests
        CaseInsensitiveMap.setMaxCacheLengthString(100);
        // Restore the default CaseInsensitiveString cache to avoid
        // interference between tests that modify the global cache.
        CaseInsensitiveMap.replaceCache(new LRUCache<>(5000, LRUCache.StrategyType.THREADED));
    }

    @Test
    void testMapStraightUp()
    {
        CaseInsensitiveMap<String, Object> stringMap = createSimpleMap();

        assertEquals("Two", stringMap.get("one"));
        assertEquals("Two", stringMap.get("One"));
        assertEquals("Two", stringMap.get("oNe"));
        assertEquals("Two", stringMap.get("onE"));
        assertEquals("Two", stringMap.get("ONe"));
        assertEquals("Two", stringMap.get("oNE"));
        assertEquals("Two", stringMap.get("ONE"));

        assertNotEquals("two", stringMap.get("one"));

        assertEquals("Four", stringMap.get("three"));
        assertEquals("Six", stringMap.get("fIvE"));
    }

    @Test
    void testWithNonStringKeys()
    {
        CaseInsensitiveMap<Object, Object> stringMap = new CaseInsensitiveMap<>();
        assert stringMap.isEmpty();

        stringMap.put(97, "eight");
        stringMap.put(19, "nineteen");
        stringMap.put("a", "two");
        stringMap.put("three", "four");
        stringMap.put(null, "null");

        assertEquals("two", stringMap.get("a"));
        assertEquals("four", stringMap.get("three"));
        assertNull(stringMap.get(8L));
        assertEquals("nineteen", stringMap.get(19));
        assertEquals("null", stringMap.get(null));
    }

    @Test
    void testOverwrite()
    {
        CaseInsensitiveMap<String, Object> stringMap = createSimpleMap();

        assertEquals("Four", stringMap.get("three"));

        stringMap.put("thRee", "Thirty");

        assertNotEquals("Four", stringMap.get("three"));
        assertEquals("Thirty", stringMap.get("three"));
        assertEquals("Thirty", stringMap.get("THREE"));
    }

    @Test
    void testKeySetWithOverwriteAttempt()
    {
        CaseInsensitiveMap<String, Object> stringMap = createSimpleMap();

        stringMap.put("thREe", "Four");

        Set<String> keySet = stringMap.keySet();
        assertNotNull(keySet);
        assertEquals(3, keySet.size());

        boolean foundOne = false, foundThree = false, foundFive = false;
        for (String key : keySet)
        {
            if (key.equals("One"))
            {
                foundOne = true;
            }
            if (key.equals("Three"))
            {
                foundThree = true;
            }
            if (key.equals("Five"))
            {
                foundFive = true;
            }
        }
        assertTrue(foundOne);
        assertTrue(foundThree);
        assertTrue(foundFive);
    }

    @Test
    void testEntrySetWithOverwriteAttempt()
    {
        CaseInsensitiveMap<String, Object> stringMap = createSimpleMap();

        stringMap.put("thREe", "four");

        Set<Map.Entry<String, Object>> entrySet = stringMap.entrySet();
        assertNotNull(entrySet);
        assertEquals(3, entrySet.size());

        boolean foundOne = false, foundThree = false, foundFive = false;
        for (Map.Entry<String, Object> entry : entrySet)
        {
            String key = entry.getKey();
            Object value = entry.getValue();
            if (key.equals("One") && value.equals("Two"))
            {
                foundOne = true;
            }
            if (key.equals("Three") && value.equals("four"))
            {
                foundThree = true;
            }
            if (key.equals("Five") && value.equals("Six"))
            {
                foundFive = true;
            }
        }
        assertTrue(foundOne);
        assertTrue(foundThree);
        assertTrue(foundFive);
    }

    @Test
    void testPutAll()
    {
        CaseInsensitiveMap<String, Object> stringMap = createSimpleMap();
        CaseInsensitiveMap<String, Object> newMap = new CaseInsensitiveMap<>(2);
        newMap.put("thREe", "four");
        newMap.put("Seven", "Eight");

        stringMap.putAll(newMap);

        assertEquals(4, stringMap.size());
        assertNotEquals("two", stringMap.get("one"));
        assertEquals("Six", stringMap.get("fIvE"));
        assertEquals("four", stringMap.get("three"));
        assertEquals("Eight", stringMap.get("seven"));

        Map<String, Object> a = createSimpleMap();
        assertThrows(NullPointerException.class, () -> a.putAll(null));     // Ensure NPE happening per Map contract
    }

    /**
     * Test putting all entries from an empty map into the CaseInsensitiveMap.
     * Verifies that no exception is thrown and the map remains unchanged.
     */
    @Test
    void testPutAllWithEmptyMap() {
        // Initialize the CaseInsensitiveMap with some entries
        CaseInsensitiveMap<String, String> ciMap = new CaseInsensitiveMap<>();
        ciMap.put("One", "1");
        ciMap.put("Two", "2");

        // Capture the initial state of the map
        int initialSize = ciMap.size();
        Map<String, String> initialEntries = new HashMap<>(ciMap);

        // Create an empty map
        Map<String, String> emptyMap = new HashMap<>();

        // Call putAll with the empty map and ensure no exception is thrown
        assertDoesNotThrow(() -> ciMap.putAll(emptyMap), "putAll with empty map should not throw an exception");

        // Verify that the map remains unchanged
        assertEquals(initialSize, ciMap.size(), "Map size should remain unchanged after putAll with empty map");
        assertEquals(initialEntries, ciMap, "Map entries should remain unchanged after putAll with empty map");
    }

    /**
     * Additional Test: Test putting all entries from a non-empty map into the CaseInsensitiveMap.
     * Verifies that the entries are added correctly.
     */
    @Test
    void testPutAllWithNonEmptyMap() {
        // Initialize the CaseInsensitiveMap with some entries
        CaseInsensitiveMap<String, String> ciMap = new CaseInsensitiveMap<>();
        ciMap.put("One", "1");

        // Create a map with entries to add
        Map<String, String> additionalEntries = new HashMap<>();
        additionalEntries.put("Two", "2");
        additionalEntries.put("Three", "3");

        // Call putAll with the additional entries
        assertDoesNotThrow(() -> ciMap.putAll(additionalEntries), "putAll with non-empty map should not throw an exception");

        // Verify that the new entries are added
        assertEquals(3, ciMap.size(), "Map size should be 3 after putAll");
        assertEquals("1", ciMap.get("one"));
        assertEquals("2", ciMap.get("TWO"));
        assertEquals("3", ciMap.get("three"));
    }
    
    @Test
    void testContainsKey()
    {
        CaseInsensitiveMap<String, Object> stringMap = createSimpleMap();

        assertTrue(stringMap.containsKey("one"));
        assertTrue(stringMap.containsKey("One"));
        assertTrue(stringMap.containsKey("oNe"));
        assertTrue(stringMap.containsKey("onE"));
        assertTrue(stringMap.containsKey("ONe"));
        assertTrue(stringMap.containsKey("oNE"));
        assertTrue(stringMap.containsKey("ONE"));
    }

    @Test
    void testRemove()
    {
        CaseInsensitiveMap<String, Object> stringMap = createSimpleMap();

        assertEquals("Two", stringMap.remove("one"));
        assertNull(stringMap.get("one"));
    }

    @Test
    void testNulls()
    {
        CaseInsensitiveMap<String, Object> stringMap = createSimpleMap();

        stringMap.put(null, "Something");
        assertEquals("Something", stringMap.get(null));
    }

    @Test
    void testRemoveIterator()
    {
        Map<String, Object> map = new CaseInsensitiveMap<>();
        map.put("One", null);
        map.put("Two", null);
        map.put("Three", null);

        int count = 0;
        Iterator<String> i = map.keySet().iterator();
        while (i.hasNext())
        {
            i.next();
            count++;
        }

        assertEquals(3, count);

        i = map.keySet().iterator();
        while (i.hasNext())
        {
            Object elem = i.next();
            if (elem.equals("One"))
            {
                i.remove();
            }
        }

        assertEquals(2, map.size());
        assertFalse(map.containsKey("one"));
        assertTrue(map.containsKey("two"));
        assertTrue(map.containsKey("three"));
    }

    @Test
    void testEquals()
    {
        Map<String, Object> a = createSimpleMap();
        Map<String, Object> b = createSimpleMap();
        assertEquals(a, b);
        Map<String, Object> c = new HashMap<>();
        assertNotEquals(a, c);

        Map<String, Object> other = new LinkedHashMap<>();
        other.put("one", "Two");
        other.put("THREe", "Four");
        other.put("five", "Six");

        assertEquals(a, other);
        assertEquals(other, a);

        other.clear();
        other.put("one", "Two");
        other.put("Three-x", "Four");
        other.put("five", "Six");
        assertNotEquals(a, other);

        other.clear();
        other.put("One", "Two");
        other.put("Three", "Four");
        other.put("Five", "six");   // lowercase six
        assertNotEquals(a, other);

        assertNotEquals("Foo", a);

        other.put("FIVE", null);
        assertNotEquals(a, other);

        a = createSimpleMap();
        b = createSimpleMap();
        a.put("Five", null);
        assertNotEquals(a, b);
    }

    @Test
    void testEquals1()
    {
        Map<String, Object> map1 = new CaseInsensitiveMap<>();
        Map<String, Object> map2 = new CaseInsensitiveMap<>();
        assert map1.equals(map2);
    }

    @Test
    void testEqualsShortCircuits() {
        CaseInsensitiveMap<String, String> map = new CaseInsensitiveMap<>();
        map.put("One", "1");
        map.put("Two", "2");

        // Test the first short-circuit: (other == this)
        assertTrue(map.equals(map), "equals() should return true when comparing the map to itself");

        // Test the second short-circuit: (!(other instanceof Map))
        String notAMap = "This is not a map";
        assertFalse(map.equals(notAMap), "equals() should return false when 'other' is not a Map");
    }
    
    @Test
    void testHashCode()
    {
        Map<String, Object> a = createSimpleMap();
        Map<String, Object> b = new CaseInsensitiveMap<>(a);
        assertEquals(a.hashCode(), b.hashCode());

        b = new CaseInsensitiveMap<>();
        b.put("ONE", "Two");
        b.put("THREE", "Four");
        b.put("FIVE", "Six");
        assertEquals(a.hashCode(), b.hashCode());

        b = new CaseInsensitiveMap<>();
        b.put("One", "Two");
        b.put("THREE", "FOUR");
        b.put("Five", "Six");
        assertNotEquals(a.hashCode(), b.hashCode());  // value FOUR is different than Four
    }

    @Test
    void testHashcodeWithNullInKeys()
    {
        Map<String, String> map = new CaseInsensitiveMap<>();
        map.put("foo", "bar");
        map.put("baz", "qux");
        map.put(null, "quux");

        assert map.keySet().hashCode() != 0;
    }

    @Test
    void testToString()
    {
        assertNotNull(createSimpleMap().toString());
    }

    @Test
    void testClear()
    {
        Map<String, Object> a = createSimpleMap();
        a.clear();
        assertEquals(0, a.size());
    }

    @Test
    void testContainsValue()
    {
        Map<String, Object> a = createSimpleMap();
        assertTrue(a.containsValue("Two"));
        assertFalse(a.containsValue("TWO"));
    }

    @Test
    void testValues()
    {
        Map<String, Object> a = createSimpleMap();
        Collection<Object> col = a.values();
        assertEquals(3, col.size());
        assertTrue(col.contains("Two"));
        assertTrue(col.contains("Four"));
        assertTrue(col.contains("Six"));
        assertFalse(col.contains("TWO"));

        a.remove("one");
        assert col.size() == 2;
    }

    @Test
    void testNullKey()
    {
        Map<String, Object> a = createSimpleMap();
        a.put(null, "foo");
        String b = (String) a.get(null);
        int x = b.hashCode();
        assertEquals("foo", b);
    }

    @Test
    void testConstructors()
    {
        Map<String, Object> map = new CaseInsensitiveMap<>();
        map.put("BTC", "Bitcoin");
        map.put("LTC", "Litecoin");

        assertEquals(2, map.size());
        assertEquals("Bitcoin", map.get("btc"));
        assertEquals("Litecoin", map.get("ltc"));

        map = new CaseInsensitiveMap<>(20);
        map.put("BTC", "Bitcoin");
        map.put("LTC", "Litecoin");

        assertEquals(2, map.size());
        assertEquals("Bitcoin", map.get("btc"));
        assertEquals("Litecoin", map.get("ltc"));

        map = new CaseInsensitiveMap<>(20, 0.85f);
        map.put("BTC", "Bitcoin");
        map.put("LTC", "Litecoin");

        assertEquals(2, map.size());
        assertEquals("Bitcoin", map.get("btc"));
        assertEquals("Litecoin", map.get("ltc"));

        Map<String, Object> map1 = new HashMap<>();
        map1.put("BTC", "Bitcoin");
        map1.put("LTC", "Litecoin");

        map = new CaseInsensitiveMap<>(map1);
        assertEquals(2, map.size());
        assertEquals("Bitcoin", map.get("btc"));
        assertEquals("Litecoin", map.get("ltc"));
    }

    @Test
    void testEqualsAndHashCode()
    {
        Map<Object, Object> map1 = new HashMap<>();
        map1.put("BTC", "Bitcoin");
        map1.put("LTC", "Litecoin");
        map1.put(16, 16);
        map1.put(null, null);

        Map<Object, Object> map2 = new CaseInsensitiveMap<>();
        map2.put("BTC", "Bitcoin");
        map2.put("LTC", "Litecoin");
        map2.put(16, 16);
        map2.put(null, null);

        Map<Object, Object> map3 = new CaseInsensitiveMap<>();
        map3.put("btc", "Bitcoin");
        map3.put("ltc", "Litecoin");
        map3.put(16, 16);
        map3.put(null, null);

        assertTrue(map1.hashCode() != map2.hashCode());    // By design: case sensitive maps will [rightly] compute hash of ABC and abc differently
        assertTrue(map1.hashCode() != map3.hashCode());    // By design: case sensitive maps will [rightly] compute hash of ABC and abc differently
        assertEquals(map2.hashCode(), map3.hashCode());

        assertEquals(map1, map2);
        assertEquals(map1, map3);
        assertEquals(map3, map1);
        assertEquals(map2, map3);
    }

    // --------- Test returned keySet() operations ---------

    @Test
    void testKeySetContains()
    {
        Map<String, Object> m = createSimpleMap();
        Set<String> s = m.keySet();
        assertTrue(s.contains("oNe"));
        assertTrue(s.contains("thRee"));
        assertTrue(s.contains("fiVe"));
        assertFalse(s.contains("dog"));
    }

    @Test
    void testKeySetContainsAll()
    {
        Map<String, Object> m = createSimpleMap();
        Set<String> s = m.keySet();
        Set<String> items = new HashSet<>();
        items.add("one");
        items.add("five");
        assertTrue(s.containsAll(items));
        items.add("dog");
        assertFalse(s.containsAll(items));
    }

    @Test
    void testKeySetRemove()
    {
        Map<String, Object> m = createSimpleMap();
        Set<String> s = m.keySet();

        s.remove("Dog");
        assertEquals(3, m.size());
        assertEquals(3, s.size());

        assertTrue(s.remove("oNe"));
        assertTrue(s.remove("thRee"));
        assertTrue(s.remove("fiVe"));
        assertEquals(0, m.size());
        assertEquals(0, s.size());
    }

    @Test
    void testKeySetRemoveAll()
    {
        Map<String, Object> m = createSimpleMap();
        Set<String> s = m.keySet();
        Set<String> items = new HashSet<>();
        items.add("one");
        items.add("five");
        assertTrue(s.removeAll(items));
        assertEquals(1, m.size());
        assertEquals(1, s.size());
        assertTrue(s.contains("three"));
        assertTrue(m.containsKey("three"));

        items.clear();
        items.add("dog");
        s.removeAll(items);
        assertEquals(1, m.size());
        assertEquals(1, s.size());
        assertTrue(s.contains("three"));
        assertTrue(m.containsKey("three"));
    }

    @Test
    void testKeySetRetainAll()
    {
        Map<String, Object> m = createSimpleMap();
        Set<String> s = m.keySet();
        Set<String> items = new HashSet<>();
        items.add("three");
        assertTrue(s.retainAll(items));
        assertEquals(1, m.size());
        assertEquals(1, s.size());
        assertTrue(s.contains("three"));
        assertTrue(m.containsKey("three"));

        m = createSimpleMap();
        s = m.keySet();
        items.clear();
        items.add("dog");
        items.add("one");
        assertTrue(s.retainAll(items));
        assertEquals(1, m.size());
        assertEquals(1, s.size());
        assertTrue(s.contains("one"));
        assertTrue(m.containsKey("one"));
    }

    @Test
    void testEntrySetRetainAllEmpty() {
        CaseInsensitiveMap<String, Object> map = new CaseInsensitiveMap<>();
        map.put("One", "Two");
        map.put("Three", "Four");
        map.put("Five", "Six");

        Set<Map.Entry<String, Object>> entries = map.entrySet();
        assertEquals(3, entries.size());
        assertEquals(3, map.size());

        // Retain nothing (empty collection)
        boolean changed = entries.retainAll(Collections.emptySet());

        assertTrue(changed, "Map should report it was changed");
        assertTrue(entries.isEmpty(), "EntrySet should be empty");
        assertTrue(map.isEmpty(), "Map should be empty");

        // Test retainAll with empty collection on already empty map
        changed = entries.retainAll(Collections.emptySet());
        assertFalse(changed, "Empty map should report no change");
        assertTrue(entries.isEmpty(), "EntrySet should still be empty");
        assertTrue(map.isEmpty(), "Map should still be empty");
    }

    @Test
    void testEntrySetRetainAllEntryChecking() {
        CaseInsensitiveMap<String, Object> map = new CaseInsensitiveMap<>();
        map.put("One", "Two");
        map.put("Three", "Four");
        map.put("Five", "Six");

        Set<Map.Entry<String, Object>> entries = map.entrySet();
        assertEquals(3, entries.size());

        // Create a collection with both Map.Entry objects and non-Entry objects
        Collection<Object> mixedCollection = new ArrayList<>();
        // Add a real entry that exists in the map
        mixedCollection.add(new AbstractMap.SimpleEntry<>("ONE", "Two"));
        // Add a non-Entry object (should be ignored)
        mixedCollection.add("Not an entry");
        // Add another entry with different case but wrong value (should not be retained)
        mixedCollection.add(new AbstractMap.SimpleEntry<>("three", "Wrong Value"));
        // Add a non-existent entry
        mixedCollection.add(new AbstractMap.SimpleEntry<>("NonExistent", "Value"));

        boolean changed = entries.retainAll(mixedCollection);

        assertTrue(changed, "Map should be changed");
        assertEquals(1, map.size(), "Should retain only the matching entry");
        assertTrue(map.containsKey("One"), "Should retain entry with case-insensitive match and matching value");
        assertEquals("Two", map.get("One"), "Should retain correct value");
        assertFalse(map.containsKey("Three"), "Should not retain entry with non-matching value");
        assertFalse(map.containsKey("NonExistent"), "Should not retain non-existent entry");
    }
    
    @Test
    void testKeySetToObjectArray()
    {
        Map<String, Object> m = createSimpleMap();
        Set<String> s = m.keySet();
        Object[] array = s.toArray();
        assertEquals(array[0], "One");
        assertEquals(array[1], "Three");
        assertEquals(array[2], "Five");
    }

    @Test
    void testKeySetToTypedArray()
    {
        Map<String, Object> m = createSimpleMap();
        Set<String> s = m.keySet();
        String[] array = s.toArray(new String[]{});
        assertEquals(array[0], "One");
        assertEquals(array[1], "Three");
        assertEquals(array[2], "Five");

        array = (String[]) s.toArray(new String[4]);
        assertEquals(array[0], "One");
        assertEquals(array[1], "Three");
        assertEquals(array[2], "Five");
        assertNull(array[3]);
        assertEquals(4, array.length);

        array = (String[]) s.toArray(new String[]{"","",""});
        assertEquals(array[0], "One");
        assertEquals(array[1], "Three");
        assertEquals(array[2], "Five");
        assertEquals(3, array.length);
    }

    @Test
    void testKeySetToArrayDifferentKeyTypes()
    {
        Map<Object, Object> map = new CaseInsensitiveMap<>();
        map.put("foo", "bar");
        map.put(1.0d, 0.0d);
        map.put(true, false);
        map.put(Boolean.FALSE, Boolean.TRUE);
        Object[] keys = map.keySet().toArray();
        assert keys[0] == "foo";
        assert keys[1] instanceof Double;
        assert 1.0d == (double)keys[1];
        assert keys[2] instanceof Boolean;
        assert (boolean) keys[2];
        assert keys[3] instanceof Boolean;
        assert Boolean.FALSE == keys[3];
    }

    @Test
    void testKeySetClear()
    {
        Map<String, Object> m = createSimpleMap();
        Set<String> s = m.keySet();
        s.clear();
        assertEquals(0, m.size());
        assertEquals(0, s.size());
    }

    @Test
    void testKeySetHashCode()
    {
        Map<String, Object> m = createSimpleMap();
        Set<String> s = m.keySet();
        int h = s.hashCode();
        Set<String> s2 = new HashSet<>();
        s2.add("One");
        s2.add("Three");
        s2.add("Five");
        assertNotEquals(h, s2.hashCode());

        s2 = new CaseInsensitiveSet<>();
        s2.add("One");
        s2.add("Three");
        s2.add("Five");
        assertEquals(h, s2.hashCode());
    }

    @Test
    void testKeySetIteratorActions()
    {
        Map<String, Object> m = createSimpleMap();
        Set<String> s = m.keySet();
        Iterator<String> i = s.iterator();
        Object o = i.next();
        assertTrue(o instanceof String);
        i.remove();
        assertEquals(2, m.size());
        assertEquals(2, s.size());

        o = i.next();
        assertTrue(o instanceof String);
        i.remove();
        assertEquals(1, m.size());
        assertEquals(1, s.size());

        o = i.next();
        assertTrue(o instanceof String);
        i.remove();
        assertEquals(0, m.size());
        assertEquals(0, s.size());
    }

    @Test
    void testKeySetEquals()
    {
        Map<String, Object> m = createSimpleMap();
        Set<String> s = m.keySet();

        Set<String> s2 = new HashSet<>();
        s2.add("One");
        s2.add("Three");
        s2.add("Five");
        assertEquals(s2, s);
        assertEquals(s, s2);

        Set<String> s3 = new HashSet<>();
        s3.add("one");
        s3.add("three");
        s3.add("five");
        assertNotEquals(s3, s);
        assertEquals(s, s3);

        Set<String> s4 = new CaseInsensitiveSet<>();
        s4.add("one");
        s4.add("three");
        s4.add("five");
        assertEquals(s4, s);
        assertEquals(s, s4);
    }

    @Test
    void testKeySetAddNotSupported()
    {
        Map<String, Object> m = createSimpleMap();
        Set<String> s = m.keySet();
        try
        {
            s.add("Bitcoin");
            fail("should not make it here");
        }
        catch (UnsupportedOperationException ignored)
        { }

        Set<String> items = new HashSet<>();
        items.add("Food");
        items.add("Water");

        try
        {
            s.addAll(items);
            fail("should not make it here");
        }
        catch (UnsupportedOperationException ignored)
        { }
    }

    // ---------------- returned Entry Set tests ---------

    @Test
    void testEntrySetContains()
    {
        Map<String, Object> m = createSimpleMap();
        Set<Map.Entry<String, Object>> s = m.entrySet();
        assertTrue(s.contains(getEntry("one", "Two")));
        assertTrue(s.contains(getEntry("tHree", "Four")));
        assertFalse(s.contains(getEntry("one", "two")));    // Value side is case-sensitive (needs 'Two' not 'two')

        assertFalse(s.contains("Not an entry"));
    }

    @Test
    void testEntrySetContainsAll()
    {
        Map<String, Object> m = createSimpleMap();
        Set<Map.Entry<String, Object>> s = m.entrySet();
        Set<Map.Entry<String, Object>> items = new HashSet<>();
        items.add(getEntry("one", "Two"));
        items.add(getEntry("thRee", "Four"));
        assertTrue(s.containsAll(items));

        items = new HashSet<>();
        items.add(getEntry("one", "two"));
        items.add(getEntry("thRee", "Four"));
        assertFalse(s.containsAll(items));
    }

    @Test
    void testEntrySetRemove()
    {
        Map<String, Object> m = createSimpleMap();
        Set<Map.Entry<String, Object>> s = m.entrySet();

        assertFalse(s.remove(getEntry("Cat", "Six")));
        assertEquals(3, m.size());
        assertEquals(3, s.size());

        assertTrue(s.remove(getEntry("oNe", "Two")));
        assertTrue(s.remove(getEntry("thRee", "Four")));

        assertFalse(s.remove(getEntry("Dog", "Two")));
        assertEquals(1, m.size());
        assertEquals(1, s.size());

        assertTrue(s.remove(getEntry("fiVe", "Six")));
        assertEquals(0, m.size());
        assertEquals(0, s.size());
    }

    @Test
    void testEntrySetRemoveAllPaths() {
        CaseInsensitiveMap<String, Object> map = new CaseInsensitiveMap<>();
        map.put("One", "Two");
        map.put("Three", "Four");
        map.put("Five", "Six");

        Set<Map.Entry<String, Object>> entries = map.entrySet();
        assertEquals(3, entries.size());

        // Create collection with mixed content to test both paths
        Collection<Object> mixedCollection = new ArrayList<>();
        // Entry object matching a map entry
        mixedCollection.add(new AbstractMap.SimpleEntry<>("ONE", "Two"));
        // Non-Entry object (should hit else branch)
        mixedCollection.add("Not an entry");
        // Add an Entry that will cause ClassCastException when cast to Entry<K,V>
        mixedCollection.add(new AbstractMap.SimpleEntry<Integer, Integer>(1, 1));
        // Entry object matching another map entry (different case)
        mixedCollection.add(new AbstractMap.SimpleEntry<>("three", "Four"));

        boolean changed = entries.removeAll(mixedCollection);

        assertTrue(changed, "Map should be changed");
        assertEquals(1, map.size(), "Should have removed matching entries");
        assertTrue(map.containsKey("Five"), "Should retain non-matching entry");
        assertFalse(map.containsKey("One"), "Should remove case-insensitive match");
        assertFalse(map.containsKey("Three"), "Should remove case-insensitive match");

        // Test removeAll with non-matching collection
        Collection<Object> nonMatching = new ArrayList<>();
        nonMatching.add("Still not an entry");
        nonMatching.add(new AbstractMap.SimpleEntry<>("NonExistent", "Value"));

        changed = entries.removeAll(nonMatching);
        assertFalse(changed, "Map should not be changed when no entries match");
        assertEquals(1, map.size(), "Map size should remain the same");
    }
    
    @Test
    void testEntrySetRemoveAll()
    {
        // Pure JDK test that fails
//        LinkedHashMap<String, Object> mm = new LinkedHashMap<>();
//        mm.put("One", "Two");
//        mm.put("Three", "Four");
//        mm.put("Five", "Six");
//        Set ss = mm.entrySet();
//        Set itemz = new HashSet();
//        itemz.add(getEntry("One", "Two"));
//        itemz.add(getEntry("Five", "Six"));
//        ss.removeAll(itemz);
//
//        itemz.clear();
//        itemz.add(getEntry("dog", "Two"));
//        assertFalse(ss.removeAll(itemz));
//        assertEquals(1, mm.size());
//        assertEquals(1, ss.size());
//        assertTrue(ss.contains(getEntry("Three", "Four")));
//        assertTrue(mm.containsKey("Three"));
//
//        itemz.clear();
//        itemz.add(getEntry("Three", "Four"));
//        assertTrue(ss.removeAll(itemz));  // fails - bug in JDK (Watching to see if this gets fixed)
//        assertEquals(0, mm.size());
//        assertEquals(0, ss.size());

        // Cedar Software code handles removeAll from entrySet perfectly
        Map<String, Object> m = createSimpleMap();
        Set<Map.Entry<String, Object>> s = m.entrySet();
        Set<Map.Entry<String, Object>> items = new HashSet<>();
        items.add(getEntry("one", "Two"));
        items.add(getEntry("five", "Six"));
        assertTrue(s.removeAll(items));
        assertEquals(1, m.size());
        assertEquals(1, s.size());
        assertTrue(s.contains(getEntry("three", "Four")));
        assertTrue(m.containsKey("three"));

        items.clear();
        items.add(getEntry("dog", "Two"));
        assertFalse(s.removeAll(items));
        assertEquals(1, m.size());
        assertEquals(1, s.size());
        assertTrue(s.contains(getEntry("three", "Four")));
        assertTrue(m.containsKey("three"));

        items.clear();
        items.add(getEntry("three", "Four"));
        assertTrue(s.removeAll(items));
        assertEquals(0, m.size());
        assertEquals(0, s.size());
    }

    @Test
    void testEntrySetRemovePaths() {
        CaseInsensitiveMap<String, Object> map = new CaseInsensitiveMap<>();
        map.put("One", "Two");
        map.put("Three", "Four");

        Set<Map.Entry<String, Object>> entries = map.entrySet();
        assertEquals(2, entries.size());

        // Test non-Entry path (should hit if-statement and return false)
        boolean result = entries.remove("Not an entry object");
        assertFalse(result, "Remove should return false for non-Entry object");
        assertEquals(2, map.size(), "Map size should not change");

        // Test Entry path
        result = entries.remove(new AbstractMap.SimpleEntry<>("ONE", "Two"));
        assertTrue(result, "Remove should return true when entry was removed");
        assertEquals(1, map.size(), "Map size should decrease");
        assertFalse(map.containsKey("One"), "Entry should be removed");
        assertTrue(map.containsKey("Three"), "Other entry should remain");
    }
    
    @Test
    void testEntrySetRetainAll()
    {
        Map<String, Object> m = createSimpleMap();
        Set<Map.Entry<String, Object>> s = m.entrySet();
        Set<Map.Entry<String, Object>> items = new HashSet<>();
        items.add(getEntry("three", "Four"));
        assertTrue(s.retainAll(items));
        assertEquals(1, m.size());
        assertEquals(1, s.size());
        assertTrue(s.contains(getEntry("three", "Four")));
        assertTrue(m.containsKey("three"));

        items.clear();
        items.add(getEntry("dog", "canine"));
        assertTrue(s.retainAll(items));
        assertEquals(0, m.size());
        assertEquals(0, s.size());
    }

    @Test
    void testEntrySetRetainAll2()
    {
        Map<String, Object> m = createSimpleMap();
        Set<Map.Entry<String, Object>> s = m.entrySet();
        Set<Map.Entry<String, Object>> items = new HashSet<>();
        items.add(getEntry("three", null));
        assertTrue(s.retainAll(items));
        assertEquals(0, m.size());
        assertEquals(0, s.size());

        m = createSimpleMap();
        s = m.entrySet();
        items.clear();
        items.add(getEntry("three", 16));
        assertTrue(s.retainAll(items));
        assertEquals(0, m.size());
        assertEquals(0, s.size());
    }

    @Test
    void testEntrySetRetainAll3()
    {
        Map<String, Object> map1 = new CaseInsensitiveMap<>();
        Map<String, Object> map2 = new CaseInsensitiveMap<>();

        map1.put("foo", "bar");
        map1.put("baz", "qux");
        map2.putAll(map1);

        assert !map1.entrySet().retainAll(map2.entrySet());
        assert map1.equals(map2);
    }

    @SuppressWarnings("unchecked")
    @Test
    void testEntrySetToObjectArray()
    {
        Map<String, Object> m = createSimpleMap();
        Set<Map.Entry<String, Object>> s = m.entrySet();
        Object[] array = s.toArray();
        assertEquals(3, array.length);

        Map.Entry<String, Object> entry = (Map.Entry<String, Object>)array[0];
        assertEquals("One", entry.getKey());
        assertEquals("Two", entry.getValue());

        entry = (Map.Entry<String, Object>) array[1];
        assertEquals("Three", entry.getKey());
        assertEquals("Four", entry.getValue());

        entry = (Map.Entry<String, Object>) array[2];
        assertEquals("Five", entry.getKey());
        assertEquals("Six", entry.getValue());
    }

    @Test
    void testEntrySetToTypedArray()
    {
        Map<String, Object> m = createSimpleMap();
        Set<Map.Entry<String, Object>> s = m.entrySet();
        Object[] array = s.toArray(new Object[]{});
        assertEquals(array[0], getEntry("One", "Two"));
        assertEquals(array[1], getEntry("Three", "Four"));
        assertEquals(array[2], getEntry("Five", "Six"));

        s = m.entrySet();   // Should not need to do this (JDK has same issue)
        array = s.toArray(new Map.Entry[4]);
        assertEquals(array[0], getEntry("One", "Two"));
        assertEquals(array[1], getEntry("Three", "Four"));
        assertEquals(array[2], getEntry("Five", "Six"));
        assertNull(array[3]);
        assertEquals(4, array.length);

        s = m.entrySet();
        array = s.toArray(new Object[]{getEntry("1", 1), getEntry("2", 2), getEntry("3", 3)});
        assertEquals(array[0], getEntry("One", "Two"));
        assertEquals(array[1], getEntry("Three", "Four"));
        assertEquals(array[2], getEntry("Five", "Six"));
        assertEquals(3, array.length);
    }

    @Test
    void testEntrySetClear()
    {
        Map<String, Object> m = createSimpleMap();
        Set<Map.Entry<String, Object>> s = m.entrySet();
        s.clear();
        assertEquals(0, m.size());
        assertEquals(0, s.size());
    }

    @Test
    void testEntrySetHashCode()
    {
        Map<String, Object> m = createSimpleMap();
        Map<String, Object> m2 = new CaseInsensitiveMap<>();
        m2.put("one", "Two");
        m2.put("three", "Four");
        m2.put("five", "Six");
        assertEquals(m.hashCode(), m2.hashCode());

        Map<String, Object> m3 = new LinkedHashMap<>();
        m3.put("One", "Two");
        m3.put("Three", "Four");
        m3.put("Five", "Six");
        assertNotEquals(m.hashCode(), m3.hashCode());
    }

    @Test
    void testEntrySetIteratorActions()
    {
        Map<String, Object> m = createSimpleMap();
        Set s = m.entrySet();
        Iterator i = s.iterator();
        Object o = i.next();
        assertTrue(o instanceof Map.Entry);
        i.remove();
        assertEquals(2, m.size());
        assertEquals(2, s.size());

        o = i.next();
        assertTrue(o instanceof Map.Entry);
        i.remove();
        assertEquals(1, m.size());
        assertEquals(1, s.size());

        o = i.next();
        assertTrue(o instanceof Map.Entry);
        i.remove();
        assertEquals(0, m.size());
        assertEquals(0, s.size());
    }

    @Test
    void testEntrySetEquals()
    {
        Map<String, Object> m = createSimpleMap();
        Set<Map.Entry<String, Object>> s = m.entrySet();

        Set<Map.Entry<String, Object>> s2 = new HashSet<>();
        s2.add(getEntry("One", "Two"));
        s2.add(getEntry("Three", "Four"));
        s2.add(getEntry("Five", "Six"));
        assertEquals(s, s2);

        s2.clear();
        s2.add(getEntry("One", "Two"));
        s2.add(getEntry("Three", "Four"));
        s2.add(getEntry("Five", "six"));    // lowercase six
        assertNotEquals(s, s2);

        s2.clear();
        s2.add(getEntry("One", "Two"));
        s2.add(getEntry("Thre", "Four"));   // missing 'e' on three
        s2.add(getEntry("Five", "Six"));
        assertNotEquals(s, s2);

        Set<Map.Entry<String, Object>> s3 = new HashSet<>();
        s3.add(getEntry("one", "Two"));
        s3.add(getEntry("three", "Four"));
        s3.add(getEntry("five","Six"));
        assertEquals(s, s3);

        Set<Map.Entry<String, Object>> s4 = new CaseInsensitiveSet<>();
        s4.add(getEntry("one", "Two"));
        s4.add(getEntry("three", "Four"));
        s4.add(getEntry("five","Six"));
        assertEquals(s, s4);

        CaseInsensitiveMap<String, Object> secondStringMap = createSimpleMap();
        assertNotEquals("one", s);

        assertEquals(s, secondStringMap.entrySet());
        // case-insensitive
        secondStringMap.put("five", "Six");
        assertEquals(s, secondStringMap.entrySet());
        secondStringMap.put("six", "sixty");
        assertNotEquals(s, secondStringMap.entrySet());
        secondStringMap.remove("five");
        assertNotEquals(s, secondStringMap.entrySet());
        secondStringMap.put("five", null);
        secondStringMap.remove("six");
        assertNotEquals(s, secondStringMap.entrySet());
        m.put("five", null);
        assertEquals(m.entrySet(), secondStringMap.entrySet());
    }

    @SuppressWarnings("unchecked")
    @Test
    void testEntrySetAddNotSupport()
    {
        Map<String, Object> m = createSimpleMap();
        Set<Map.Entry<String, Object>> s = m.entrySet();

        try
        {
            s.add(getEntry("10", 10));
            fail("should not make it here");
        }
        catch (UnsupportedOperationException ignored)
        { }

        Set<String> s2 = new HashSet<>();
        s2.add("food");
        s2.add("water");

        try
        {
            s.addAll((Set)s2);
            fail("should not make it here");
        }
        catch (UnsupportedOperationException ignored)
        { }
    }

    @Test
    void testEntrySetKeyInsensitive()
    {
        Map<String, Object> m = createSimpleMap();
        int one = 0;
        int three = 0;
        int five = 0;
        for (Map.Entry<String, Object> entry : m.entrySet())
        {
            if (entry.equals(new AbstractMap.SimpleEntry<String, Object>("one", "Two")))
            {
                one++;
            }
            if (entry.equals(new AbstractMap.SimpleEntry<String, Object>("thrEe", "Four")))
            {
                three++;
            }
            if (entry.equals(new AbstractMap.SimpleEntry<String, Object>("FIVE", "Six")))
            {
                five++;
            }
        }

        assertEquals(1, one);
        assertEquals(1, three);
        assertEquals(1, five);
    }

    @Test
    void testRetainAll2()
    {
        Map<String, String> oldMap = new CaseInsensitiveMap<>();
        Map<String, String> newMap = new CaseInsensitiveMap<>();

        oldMap.put("foo", null);
        oldMap.put("bar", null);
        newMap.put("foo", null);
        newMap.put("bar", null);
        newMap.put("qux", null);
        Set<String> oldKeys = oldMap.keySet();
        Set<String> newKeys = newMap.keySet();
        assertTrue(newKeys.retainAll(oldKeys));
    }

    @Test
    void testRetainAll3()
    {
        Map<String, String> oldMap = new CaseInsensitiveMap<>();
        Map<String, String> newMap = new CaseInsensitiveMap<>();

        oldMap.put("foo", null);
        oldMap.put("bar", null);
        newMap.put("foo", null);
        newMap.put("bar", null);
        Set<String> oldKeys = oldMap.keySet();
        Set<String> newKeys = newMap.keySet();
        assertFalse(newKeys.retainAll(oldKeys));
    }

    @Test
    void testRemoveAll2() {
        Map<String, String> oldMap = new CaseInsensitiveMap<>();
        Map<String, String> newMap = new CaseInsensitiveMap<>();

        oldMap.put("bart", null);
        oldMap.put("qux", null);
        newMap.put("foo", null);
        newMap.put("bar", null);
        newMap.put("qux", null);
        Set<String> oldKeys = oldMap.keySet();
        Set<String> newKeys = newMap.keySet();
        boolean ret = newKeys.removeAll(oldKeys);
        assertTrue(ret);
    }

    @Test
    void testAgainstUnmodifiableMap()
    {
        Map<String, String> oldMeta = new CaseInsensitiveMap<>();
        oldMeta.put("foo", "baz");
        oldMeta = Collections.unmodifiableMap(oldMeta);
        oldMeta.keySet();
        Map<String, String> newMeta = new CaseInsensitiveMap<>();
        newMeta.put("foo", "baz");
        newMeta.put("bar", "qux");
        newMeta = Collections.unmodifiableMap(newMeta);

        Set<String> oldKeys = new CaseInsensitiveSet<>(oldMeta.keySet());
        Set<String> sameKeys = new CaseInsensitiveSet<>(newMeta.keySet());
        sameKeys.retainAll(oldKeys);
    }

    @Test
    void testSetValueApiOnEntrySet()
    {
        Map<String, String> map = new CaseInsensitiveMap<>();
        map.put("One", "Two");
        map.put("Three", "Four");
        map.put("Five", "Six");
        for (Map.Entry<String, String> entry : map.entrySet())
        {
            if ("Three".equals(entry.getKey()))
            {   // Make sure this 'writes thru' to the underlying map's value.
                entry.setValue("~3");
            }
        }
        assertEquals("~3", map.get("Three"));
    }

    @Test
    void testWrappedTreeMap()
    {
        CaseInsensitiveMap<String, Object> map = new CaseInsensitiveMap<>(new TreeMap<>());
        map.put("z", "zulu");
        map.put("J", "juliet");
        map.put("a", "alpha");
        assert map.size() == 3;
        Iterator<String> i = map.keySet().iterator();
        assert "a".equals(i.next());
        assert "J".equals(i.next());
        assert "z".equals(i.next());
        assert map.containsKey("A");
        assert map.containsKey("j");
        assert map.containsKey("Z");

        assert map.getWrappedMap() instanceof TreeMap;
    }

    @Test
    void testWrappedTreeMapNotAllowsNull()
    {
        try
        {
            Map<String, Object> map = new CaseInsensitiveMap<>(new TreeMap<>());
            map.put(null, "not allowed");
            fail();
        }
        catch (NullPointerException ignored)
        { }
    }

    @Test
    void testWrappedConcurrentHashMap()
    {
        Map<String, Object> map = new CaseInsensitiveMap<>(new ConcurrentHashMap<>());
        map.put("z", "zulu");
        map.put("J", "juliet");
        map.put("a", "alpha");
        assert map.size() == 3;
        assert map.containsKey("A");
        assert map.containsKey("j");
        assert map.containsKey("Z");

        assert ((CaseInsensitiveMap)map).getWrappedMap() instanceof ConcurrentHashMap;
    }

    @Test
    void testWrappedConcurrentMapNotAllowsNull()
    {
        try
        {
            Map<String, Object> map = new CaseInsensitiveMap<>(new ConcurrentHashMap<>());
            map.put(null, "not allowed");
            fail();
        }
        catch (NullPointerException ignored)
        { }
    }

    @Test
    void testWrappedMapKeyTypes()
    {
        CaseInsensitiveMap<String, Object> map = new CaseInsensitiveMap<>();
        map.put("Alpha", 1);
        map.put("alpha", 2);
        map.put("alPHA", 3);

        assert map.size() == 1;
        assert map.containsKey("Alpha");
        assert map.containsKey("alpha");
        assert map.containsKey("alPHA");

        Map check = map.getWrappedMap();
        assert check.keySet().size() == 1;
        assert check.keySet().iterator().next() instanceof CaseInsensitiveMap.CaseInsensitiveString;
    }

    @Test
    void testUnmodifiableMap()
    {
        Map<String, Object> junkMap = new ConcurrentHashMap<>();
        junkMap.put("z", "zulu");
        junkMap.put("J", "juliet");
        junkMap.put("a", "alpha");
        Map<String, Object> map = new CaseInsensitiveMap<>(Collections.unmodifiableMap(junkMap));
        assert map.size() == 3;
        assert map.containsKey("A");
        assert map.containsKey("j");
        assert map.containsKey("Z");
        map.put("h", "hotel");      // modifiable allowed on the CaseInsensitiveMap
    }

    @Test
    void testWeakHashMap()
    {
        Map<String, Object> map = new CaseInsensitiveMap<>(new WeakHashMap<>());
        map.put("z", "zulu");
        map.put("J", "juliet");
        map.put("a", "alpha");
        assert map.size() == 3;
        assert map.containsKey("A");
        assert map.containsKey("j");
        assert map.containsKey("Z");

        assert ((CaseInsensitiveMap)map).getWrappedMap() instanceof WeakHashMap;
    }

    @Test
    void testWrappedMap()
    {
        Map<String, Object> linked = new LinkedHashMap<>();
        linked.put("key1", 1);
        linked.put("key2", 2);
        linked.put("key3", 3);
        CaseInsensitiveMap<String, Object> caseInsensitive = new CaseInsensitiveMap<>(linked);
        Set<String> newKeys = new LinkedHashSet<>();
        newKeys.add("key4");
        newKeys.add("key5");
        int newValue = 4;

        for (String key : newKeys)
        {
            caseInsensitive.put(key, newValue++);
        }

        Iterator<String> i = caseInsensitive.keySet().iterator();
        assertEquals(i.next(), "key1");
        assertEquals(i.next(), "key2");
        assertEquals(i.next(), "key3");
        assertEquals(i.next(), "key4");
        assertEquals(i.next(), "key5");
    }

    @Test
    void testNotRecreatingCaseInsensitiveStrings()
    {
        Map<String, Object> map = new CaseInsensitiveMap<>();
        map.put("true", "eddie");

        // copy 1st map
        Map<String, Object> newMap = new CaseInsensitiveMap<>(map);

        CaseInsensitiveMap<String, Object>.CaseInsensitiveEntry entry1 = (CaseInsensitiveMap<String, Object>.CaseInsensitiveEntry) map.entrySet().iterator().next();
        CaseInsensitiveMap<String, Object>.CaseInsensitiveEntry entry2 = (CaseInsensitiveMap<String, Object>.CaseInsensitiveEntry) newMap.entrySet().iterator().next();

        assertSame(entry1.getOriginalKey(), entry2.getOriginalKey());
    }

    @Test
    void testPutAllOfNonCaseInsensitiveMap()
    {
        Map<String, Object> nonCi = new HashMap<>();
        nonCi.put("Foo", "bar");
        nonCi.put("baz", "qux");

        Map<String, Object> ci = new CaseInsensitiveMap<>();
        ci.putAll(nonCi);

        assertTrue(ci.containsKey("foo"));
        assertTrue(ci.containsKey("Baz"));
    }

    @Test
    void testNotRecreatingCaseInsensitiveStringsUsingTrackingMap()
    {
        Map<String, Object> map = new CaseInsensitiveMap<>();
        map.put("dog", "eddie");
        map = new TrackingMap<>(map);

        // copy 1st map
        Map<String, Object> newMap = new CaseInsensitiveMap<>(map);

        CaseInsensitiveMap<String, Object>.CaseInsensitiveEntry entry1 = (CaseInsensitiveMap<String, Object>.CaseInsensitiveEntry) map.entrySet().iterator().next();
        CaseInsensitiveMap<String, Object>.CaseInsensitiveEntry entry2 = (CaseInsensitiveMap<String, Object>.CaseInsensitiveEntry) newMap.entrySet().iterator().next();

        assertSame(entry1.getOriginalKey(), entry2.getOriginalKey());
    }

    @Test
    void testEntrySetIsEmpty()
    {
        Map<String, Object> map = createSimpleMap();
        Set<Map.Entry<String, Object>> entries = map.entrySet();
        assert !entries.isEmpty();
    }
    
    @Test
    void testPutObject()
    {
        CaseInsensitiveMap<Object, Object> map = new CaseInsensitiveMap<>();
        map.put(1L, 1L);
        map.put("hi", "ho");
        Object x = map.put("hi", "hi");
        assert x == "ho";
        map.put(Boolean.TRUE, Boolean.TRUE);
        String str = "hello";
        CaseInsensitiveMap.CaseInsensitiveString ciStr = new CaseInsensitiveMap.CaseInsensitiveString(str);
        map.put(ciStr, str);
        assert map.get(str) == str;
        assert 1L == ((Number)map.get(1L)).longValue();
        assert Boolean.TRUE == map.get(true);
    }

    @Test
    void testTwoMapConstructor()
    {
        Map<String, Object> real = new HashMap<>();
        real.put("z", 26);
        real.put("y", 25);
        real.put("m", 13);
        real.put("d", 4);
        real.put("c", 3);
        real.put("b", 2);
        real.put("a", 1);

        Map<String, Object> backingMap = new TreeMap<>();
        CaseInsensitiveMap<String, Object> ciMap = new CaseInsensitiveMap<>(real, backingMap);
        assert ciMap.size() == real.size();
        assert ciMap.containsKey("Z");
        assert ciMap.containsKey("A");
        assert ciMap.getWrappedMap() instanceof TreeMap;
        assert ciMap.getWrappedMap() == backingMap;
    }

    @Test
    void testCaseInsensitiveStringConstructor()
    {
        CaseInsensitiveMap.CaseInsensitiveString ciString = new CaseInsensitiveMap.CaseInsensitiveString("John");
        assert ciString.equals("JOHN");
        assert ciString.equals("john");
        assert ciString.hashCode() == "John".toLowerCase().hashCode();
        assert ciString.compareTo("JOHN") == 0;
        assert ciString.compareTo("john") == 0;
        assert ciString.compareTo("alpha") > 0;
        assert ciString.compareTo("ALPHA") > 0;
        assert ciString.compareTo("theta") < 0;
        assert ciString.compareTo("THETA") < 0;
        assert ciString.toString().equals("John");
    }

    @Test
    void testHeterogeneousMap()
    {
        Map<Object, Object> ciMap = new CaseInsensitiveMap<>();
        ciMap.put(1.0d, "foo");
        ciMap.put("Key", "bar");
        ciMap.put(true, "baz");

        assert ciMap.get(1.0d) == "foo";
        assert ciMap.get("Key") == "bar";
        assert ciMap.get(true) == "baz";

        assert ciMap.remove(true) == "baz";
        assert ciMap.size() == 2;
        assert ciMap.remove(1.0d) == "foo";
        assert ciMap.size() == 1;
        assert ciMap.remove("Key") == "bar";
        assert ciMap.size() == 0;
    }

    @Test
    void testCaseInsensitiveString()
    {
        CaseInsensitiveMap.CaseInsensitiveString ciString = new CaseInsensitiveMap.CaseInsensitiveString("foo");
        assert ciString.equals(ciString);
        assert ciString.compareTo(1.5d) < 0;

        CaseInsensitiveMap.CaseInsensitiveString ciString2 = new CaseInsensitiveMap.CaseInsensitiveString("bar");
        assert !ciString.equals(ciString2);
    }

    @Test
    void testCaseInsensitiveStringHashcodeCollision()
    {
        CaseInsensitiveMap.CaseInsensitiveString ciString = new CaseInsensitiveMap.CaseInsensitiveString("f608607");
        CaseInsensitiveMap.CaseInsensitiveString ciString2 = new CaseInsensitiveMap.CaseInsensitiveString("f16010070");
        assert ciString.hashCode() == ciString2.hashCode();
        assert !ciString.equals(ciString2);
    }

    private String current = "0";
    String getNext() {
        int length = current.length();
        StringBuilder next = new StringBuilder(current);
        boolean carry = true;

        for (int i = length - 1; i >= 0 && carry; i--) {
            char ch = next.charAt(i);
            if (ch == 'j') {
                next.setCharAt(i, '0');
            } else {
                if (ch == '9') {
                    next.setCharAt(i, 'a');
                } else {
                    next.setCharAt(i, (char) (ch + 1));
                }
                carry = false;
            }
        }

        // If carry is still true, all digits were 'f', append '1' at the beginning
        if (carry) {
            next.insert(0, '1');
        }

        current = next.toString();
        return current;
    }

    @EnabledIfSystemProperty(named = "performRelease", matches = "true")
    @Test
    void testGenHash() {
        HashMap<Integer, CaseInsensitiveMap.CaseInsensitiveString> hs = new HashMap<>();
        long t1 = System.currentTimeMillis();
        int dupe = 0;

        while (true) {
            String hash = getNext();
            CaseInsensitiveMap.CaseInsensitiveString key = new CaseInsensitiveMap.CaseInsensitiveString(hash);
            if (hs.containsKey(key.hashCode())) {
                dupe++;
                continue;
            } else {
                hs.put(key.hashCode(), key);
            }

            if (System.currentTimeMillis() - t1 > 250) {
                break;
            }
        }
        LOG.info("Done, ran " + (System.currentTimeMillis() - t1) + " ms, " + dupe + " dupes, CaseInsensitiveMap.size: " + hs.size());
    }

    @Test
    void testConcurrentSkipListMap()
    {
        ConcurrentMap<String, Object> map = new ConcurrentSkipListMap<>();
        map.put("key1", "foo");
        map.put("key2", "bar");
        map.put("key3", "baz");
        map.put("key4", "qux");
        CaseInsensitiveMap<String, Object> ciMap = new CaseInsensitiveMap<>(map);
        assert ciMap.get("KEY1") == "foo";
        assert ciMap.get("KEY2") == "bar";
        assert ciMap.get("KEY3") == "baz";
        assert ciMap.get("KEY4") == "qux";
    }

    @EnabledIfSystemProperty(named = "performRelease", matches = "true")
    @Test
    void testPerformance()
    {
        Map<String, String> map = new CaseInsensitiveMap<>();
        Random random = new Random();

        long start = System.nanoTime();

        for (int i=0; i < 10000; i++)
        {
            String key = StringUtilities.getRandomString(random, 1, 10);
            String value = StringUtilities.getRandomString(random, 1, 10);
            map.put(key, value);
        }

        long stop = System.nanoTime();
        LOG.info("load CI map with 10,000: " + (stop - start) / 1000000);

        start = System.nanoTime();

        for (int i=0; i < 100000; i++)
        {
            Map<String, String> copy = new CaseInsensitiveMap<>(map);
        }

        stop = System.nanoTime();

        LOG.info("dupe CI map 100,000 times: " + (stop - start) / 1000000);
    }

    @EnabledIfSystemProperty(named = "performRelease", matches = "true")
    @Test
    void testPerformance2()
    {
        Map<String, String> map = new LinkedHashMap<>();
        Random random = new Random();

        long start = System.nanoTime();

        for (int i=0; i < 10000; i++)
        {
            String key = StringUtilities.getRandomString(random, 1, 10);
            String value = StringUtilities.getRandomString(random, 1, 10);
            map.put(key, value);
        }

        long stop = System.nanoTime();
        LOG.info("load linked map with 10,000: " + (stop - start) / 1000000);

        start = System.nanoTime();

        for (int i=0; i < 100000; i++)
        {
            Map<String, String> copy = new LinkedHashMap<>(map);
        }

        stop = System.nanoTime();

        LOG.info("dupe linked map 100,000 times: " + (stop - start) / 1000000);
    }

    @Test
void testComputeIfAbsent() {
    CaseInsensitiveMap<String, String> map = new CaseInsensitiveMap<>();
    map.put("One", "Two");
    map.put("Three", "Four");

    // Key present, should not overwrite
    map.computeIfAbsent("oNe", k -> "NotUsed");
    assertEquals("Two", map.get("one"));

    // Key absent, should add
    map.computeIfAbsent("fIvE", k -> "Six");
    assertEquals("Six", map.get("five"));
}

    @Test
    void testComputeIfPresent() {
        CaseInsensitiveMap<String, String> map = new CaseInsensitiveMap<>();
        map.put("One", "Two");
        map.put("Three", "Four");

        // Key present, apply function
        map.computeIfPresent("thRee", (k, v) -> v.toUpperCase());
        assertEquals("FOUR", map.get("Three"));

        // Key absent, no change
        map.computeIfPresent("sEvEn", (k, v) -> "???");
        assertNull(map.get("SEVEN"));
    }

    @Test
    void testCompute() {
        CaseInsensitiveMap<String, String> map = new CaseInsensitiveMap<>();
        map.put("One", "Two");

        // Key present, modify value
        map.compute("oNe", (k, v) -> v + "-Modified");
        assertEquals("Two-Modified", map.get("ONE"));

        // Key absent, insert new value
        map.compute("EiGhT", (k, v) -> v == null ? "8" : v);
        assertEquals("8", map.get("eight"));
    }

    @Test
    void testMerge() {
        CaseInsensitiveMap<String, String> map = new CaseInsensitiveMap<>();
        map.put("Five", "Six");

        // Key present, merge values
        map.merge("fIvE", "SIX", (oldVal, newVal) -> oldVal + "-" + newVal);
        assertEquals("Six-SIX", map.get("five"));

        // Key absent, insert new
        map.merge("NINE", "9", (oldVal, newVal) -> oldVal + "-" + newVal);
        assertEquals("9", map.get("nine"));
    }

    @Test
    void testPutIfAbsent() {
        CaseInsensitiveMap<String, String> map = new CaseInsensitiveMap<>();
        map.put("One", "Two-Modified");

        // Key present, should not overwrite
        map.putIfAbsent("oNe", "NewTwo");
        assertEquals("Two-Modified", map.get("ONE"));

        // Key absent, add new entry
        map.putIfAbsent("Ten", "10");
        assertEquals("10", map.get("tEn"));
    }

    @Test
    void testRemoveKeyValue() {
        CaseInsensitiveMap<String, String> map = new CaseInsensitiveMap<>();
        map.put("One", "Two");
        map.put("Three", "Four");

        // Wrong value, should not remove
        assertFalse(map.remove("one", "NotTwo"));
        assertEquals("Two", map.get("ONE"));

        // Correct value, remove entry
        assertTrue(map.remove("oNe", "Two"));
        assertNull(map.get("ONE"));
    }

    @Test
    void testReplaceKeyOldValueNewValue() {
        CaseInsensitiveMap<String, String> map = new CaseInsensitiveMap<>();
        map.put("Three", "Four");

        // Old value doesn't match, no replace
        assertFalse(map.replace("three", "NoMatch", "NomatchValue"));
        assertEquals("Four", map.get("THREE"));

        // Old value matches, do replace
        // Use the exact same case as originally stored: "Four" instead of "FOUR"
        assertTrue(map.replace("thRee", "Four", "4"));
        assertEquals("4", map.get("THREE"));
    }

    @Test
    void testReplaceKeyValue() {
        CaseInsensitiveMap<String, String> map = new CaseInsensitiveMap<>();
        map.put("Five", "Six-SIX");

        // Replace unconditionally if key present
        map.replace("FiVe", "ReplacedFive");
        assertEquals("ReplacedFive", map.get("five"));
    }

    @Test
    void testAllNewApisTogether() {
        CaseInsensitiveMap<String, String> map = new CaseInsensitiveMap<>();
        map.put("One", "Two");
        map.put("Three", "Four");

        // computeIfAbsent
        map.computeIfAbsent("fIvE", k -> "Six");
        // computeIfPresent
        map.computeIfPresent("ThReE", (k, v) -> v + "-Modified");
        // compute
        map.compute("oNe", (k, v) -> v + "-Changed");
        // merge
        map.merge("fIvE", "SIX", (oldVal, newVal) -> oldVal + "-" + newVal);
        // putIfAbsent
        map.putIfAbsent("Ten", "10");
        // remove(key,value)
        map.remove("one", "Two-Changed");   // matches after compute("one",...)
        // replace(key,oldValue,newValue)
        map.replace("three", "Four-Modified", "4");
        // replace(key,value)
        map.replace("fIvE", "ReplacedFive");

        // Verify all changes
        assertNull(map.get("One"), "Should have been removed by remove(key,value) after compute changed the value");
        assertEquals("4", map.get("THREE"), "Should have replaced after matching old value");
        assertEquals("ReplacedFive", map.get("FIVE"), "Should have replaced the value");
        assertEquals("10", map.get("tEn"), "Should have put if absent");
    }

    @Test
    void testForEachSimple() {
        CaseInsensitiveMap<String, String> map = new CaseInsensitiveMap<>();
        map.put("One", "Two");
        map.put("Three", "Four");
        map.put("Five", "Six");

        // We will collect the entries visited by forEach
        Map<String, String> visited = new HashMap<>();
        map.forEach((k, v) -> visited.put(k, v));

        // Check that all entries were visited with keys in original case
        assertEquals(3, visited.size());
        assertEquals("Two", visited.get("One"));
        assertEquals("Four", visited.get("Three"));
        assertEquals("Six", visited.get("Five"));

        // Ensure that calling forEach on an empty map visits nothing
        CaseInsensitiveMap<String, String> empty = new CaseInsensitiveMap<>();
        empty.forEach((k, v) -> fail("No entries should be visited"));
    }

    @Test
    void testForEachNonStringKeys() {
        CaseInsensitiveMap<Object, Object> map = new CaseInsensitiveMap<>();
        map.put(42, "Answer");
        map.put(true, "Boolean");
        map.put("Hello", "World");

        Map<Object, Object> visited = new HashMap<>();
        map.forEach((k, v) -> visited.put(k, v));

        // Confirm all entries are visited
        assertEquals(3, visited.size());
        // Non-String keys should be unchanged
        assertEquals("Answer", visited.get(42));
        assertEquals("Boolean", visited.get(true));
        // String key should appear in original form ("Hello")
        assertEquals("World", visited.get("Hello"));
    }

    @Test
    void testForEachWithNullValues() {
        CaseInsensitiveMap<String, String> map = new CaseInsensitiveMap<>();
        map.put("NullKey", null);
        map.put("NormalKey", "NormalValue");

        Map<String, String> visited = new HashMap<>();
        map.forEach((k, v) -> visited.put(k, v));

        assertEquals(2, visited.size());
        assertTrue(visited.containsKey("NullKey"));
        assertNull(visited.get("NullKey"));
        assertEquals("NormalValue", visited.get("NormalKey"));
    }

    @Test
    void testReplaceAllSimple() {
        CaseInsensitiveMap<String, String> map = new CaseInsensitiveMap<>();
        map.put("Alpha", "a");
        map.put("Bravo", "b");
        map.put("Charlie", "c");

        // Convert all values to uppercase
        map.replaceAll((k, v) -> v.toUpperCase());

        assertEquals("A", map.get("alpha"));
        assertEquals("B", map.get("bravo"));
        assertEquals("C", map.get("CHARLIE"));
        // Keys should remain in original form within the map
        // Keys: "Alpha", "Bravo", "Charlie" unchanged
        Set<String> keys = map.keySet();
        assertTrue(keys.contains("Alpha"));
        assertTrue(keys.contains("Bravo"));
        assertTrue(keys.contains("Charlie"));
    }

    @Test
    void testReplaceAllCaseInsensitivityOnKeys() {
        CaseInsensitiveMap<String, String> map = new CaseInsensitiveMap<>();
        map.put("One", "Two");
        map.put("THREE", "Four");
        map.put("FiVe", "Six");

        // Replace all values with their length as a string
        map.replaceAll((k, v) -> String.valueOf(v.length()));

        assertEquals("3", map.get("one"));   // "Two" length is 3
        assertEquals("4", map.get("three")); // "Four" length is 4
        assertEquals("3", map.get("five"));  // "Six" length is 3

        // Ensure keys are still their original form
        assertTrue(map.keySet().contains("One"));
        assertTrue(map.keySet().contains("THREE"));
        assertTrue(map.keySet().contains("FiVe"));
    }

    @Test
    void testReplaceAllNonStringKeys() {
        CaseInsensitiveMap<Object, Object> map = new CaseInsensitiveMap<>();
        map.put("Key", "Value");
        map.put(100, 200);
        map.put(true, false);

        // Transform all values to strings prefixed with "X-"
        map.replaceAll((k, v) -> "X-" + String.valueOf(v));

        assertEquals("X-Value", map.get("key"));
        assertEquals("X-200", map.get(100));
        assertEquals("X-false", map.get(true));
    }

    @Test
    void testReplaceAllEmptyMap() {
        CaseInsensitiveMap<String, String> empty = new CaseInsensitiveMap<>();
        // Should not fail or modify anything
        empty.replaceAll((k, v) -> v + "-Modified");
        assertTrue(empty.isEmpty());
    }

    @Test
    void testReplaceAllWithNullValues() {
        CaseInsensitiveMap<String, String> map = new CaseInsensitiveMap<>();
        map.put("NullValKey", null);
        map.put("NormalKey", "Value");

        map.replaceAll((k, v) -> v == null ? "wasNull" : v + "-Appended");

        assertEquals("wasNull", map.get("NullValKey"));
        assertEquals("Value-Appended", map.get("NormalKey"));
    }

    @Test
    void testForEachAndReplaceAllTogether() {
        CaseInsensitiveMap<String, String> map = new CaseInsensitiveMap<>();
        map.put("Apple", "red");
        map.put("Banana", "yellow");
        map.put("Grape", "purple");

        // First, replaceAll colors with their uppercase form
        map.replaceAll((k, v) -> v.toUpperCase());

        // Now forEach to verify changes
        Map<String, String> visited = new HashMap<>();
        map.forEach(visited::put);

        assertEquals("RED", visited.get("Apple"));
        assertEquals("YELLOW", visited.get("Banana"));
        assertEquals("PURPLE", visited.get("Grape"));
    }

    @Test
    void testRemoveKeyValueNonStringKey() {
        // Create a map and put a non-string key
        CaseInsensitiveMap<Object, Object> map = new CaseInsensitiveMap<>();
        map.put(42, "Answer");
        map.put("One", "Two");  // A string key for comparison

        // Removing with a non-string key should hit the last statement of remove()
        // because key instanceof String will fail.
        assertTrue(map.remove(42, "Answer"), "Expected to remove entry by non-string key");

        // Verify that the entry was indeed removed
        assertFalse(map.containsKey(42));
        assertEquals("Two", map.get("one"));  // Ensure other entries are unaffected
    }

    @Test
    void testNormalizeKeyWithNonStringKey() {
        CaseInsensitiveMap<Object, Object> map = new CaseInsensitiveMap<>();
        // putIfAbsent calls normalizeKey internally
        // Because 42 is not a String, normalizeKey() should hit the 'return key;' line.
        map.putIfAbsent(42, "The Answer");

        // Verify that the entry is there and the key is intact.
        assertTrue(map.containsKey(42));
        assertEquals("The Answer", map.get(42));
    }

    @Test
    void testWrapperFunctionBothBranches() {
        CaseInsensitiveMap<Object, Object> map = new CaseInsensitiveMap<>();
        map.put("One", "Two");    // Will be wrapped as CaseInsensitiveString
        map.put(42, "Answer");    // Will remain as Integer

        // Test computeIfPresent which uses wrapBiFunctionForKey
        // First with String key (hits instanceof CaseInsensitiveString branch)
        map.computeIfPresent("oNe", (k, v) -> {
            assertTrue(k instanceof String);
            assertEquals("oNe", k);  // Should get original string, not CaseInsensitiveString
            assertEquals("Two", v);
            return "Two-Modified";
        });

        // Then with non-String key (hits else branch)
        map.computeIfPresent(42, (k, v) -> {
            assertTrue(k instanceof Integer);
            assertEquals(42, k);
            assertEquals("Answer", v);
            return "Answer-Modified";
        });

        // Test computeIfAbsent which uses wrapFunctionForKey
        // First with String key (hits instanceof CaseInsensitiveString branch)
        map.computeIfAbsent("New", k -> {
            assertTrue(k instanceof String);
            assertEquals("New", k);  // Should get original string
            return "Value";
        });

        // Then with non-String key (hits else branch)
        map.computeIfAbsent(99, k -> {
            assertTrue(k instanceof Integer);
            assertEquals(99, k);
            return "Ninety-Nine";
        });

        // Verify all operations worked correctly
        assertEquals("Two-Modified", map.get("ONE"));
        assertEquals("Answer-Modified", map.get(42));
        assertEquals("Value", map.get("NEW"));
        assertEquals("Ninety-Nine", map.get(99));
    }

    @Test
    void testComputeMethods() {
        CaseInsensitiveMap<Object, Object> map = new CaseInsensitiveMap<>();

        // Put initial values with specific case
        map.put("One", "Original");
        map.put(42, "Answer");

        // Track if lambdas are called
        boolean[] lambdaCalled = new boolean[1];

        // Test 1: computeIfAbsent when key exists (case-insensitive)
        Object result = map.computeIfAbsent("oNe", k -> {
            lambdaCalled[0] = true;
            return "Should Not Be Used";
        });
        assertFalse(lambdaCalled[0], "Lambda should not be called when key exists");
        assertEquals("Original", result, "Should return existing value");
        assertEquals("Original", map.get("one"), "Value should be unchanged");
        assertTrue(map.keySet().contains("One"), "Original case should be retained");

        // Test 2: computeIfAbsent for new key
        lambdaCalled[0] = false;
        String newKey = "NeW_KeY";
        result = map.computeIfAbsent(newKey, k -> {
            lambdaCalled[0] = true;
            assertEquals(newKey, k, "Lambda should receive key as provided");
            return "New Value";
        });
        assertTrue(lambdaCalled[0], "Lambda should be called for new key");
        assertEquals("New Value", result);
        assertEquals("New Value", map.get("new_key"));
        assertTrue(map.keySet().contains(newKey), "Should retain case of new key");

        // Test 3: computeIfAbsent with non-String key
        lambdaCalled[0] = false;
        Integer intKey = 99;
        result = map.computeIfAbsent(intKey, k -> {
            lambdaCalled[0] = true;
            assertEquals(intKey, k, "Lambda should receive non-String key unchanged");
            return "Int Value";
        });
        assertTrue(lambdaCalled[0], "Lambda should be called for new integer key");
        assertEquals("Int Value", result);
        assertEquals("Int Value", map.get(intKey));

        // Test 4: computeIfPresent when key exists
        lambdaCalled[0] = false;
        result = map.computeIfPresent("OnE", (k, v) -> {
            lambdaCalled[0] = true;
            assertEquals("OnE", k, "Should receive key as provided to method");
            assertEquals("Original", v, "Should receive existing value");
            return "Updated Value";
        });
        assertTrue(lambdaCalled[0], "Lambda should be called for existing key");
        assertEquals("Updated Value", result);
        assertEquals("Updated Value", map.get("one"));
        assertTrue(map.keySet().contains("One"), "Original case should be retained");

        // Test 5: computeIfPresent when key doesn't exist
        lambdaCalled[0] = false;
        result = map.computeIfPresent("NonExistent", (k, v) -> {
            lambdaCalled[0] = true;
            return "Should Not Be Used";
        });
        assertFalse(lambdaCalled[0], "Lambda should not be called for non-existent key");
        assertNull(result, "Should return null for non-existent key");

        // Test 6: compute (unconditional) on existing key
        lambdaCalled[0] = false;
        result = map.compute("oNe", (k, v) -> {
            lambdaCalled[0] = true;
            assertEquals("oNe", k, "Should receive key as provided");
            assertEquals("Updated Value", v, "Should receive current value");
            return "Computed Value";
        });
        assertTrue(lambdaCalled[0], "Lambda should be called");
        assertEquals("Computed Value", result);
        assertEquals("Computed Value", map.get("one"));
        assertTrue(map.keySet().contains("One"), "Original case should be retained");

        // Test 7: compute (unconditional) on non-existent key
        String newComputeKey = "CoMpUtE_KeY";
        lambdaCalled[0] = false;
        result = map.compute(newComputeKey, (k, v) -> {
            lambdaCalled[0] = true;
            assertEquals(newComputeKey, k, "Should receive key as provided");
            assertNull(v, "Should receive null for non-existent key");
            return "Brand New";
        });
        assertTrue(lambdaCalled[0], "Lambda should be called for new key");
        assertEquals("Brand New", result);
        assertEquals("Brand New", map.get("compute_key"));
        assertTrue(map.keySet().contains(newComputeKey), "Should retain case of new key");
    }

    @Test
    void testToArrayTArrayBothBranchesInsideForLoop() {
        CaseInsensitiveMap<Object, Object> map = new CaseInsensitiveMap<>();
        // Add a String key, which will be wrapped as CaseInsensitiveString internally
        map.put("One", 1);
        // Add a non-String key, which will remain as is
        map.put(42, "FortyTwo");

        // Now, when toArray() runs, we'll have one key that is a CaseInsensitiveString
        // ("One") and one key that is not (42), causing both sides of the ternary operator
        // to be executed inside the for-loop.

        Object[] result = map.keySet().toArray(new Object[0]);

        assertEquals(2, result.length);
        // We don't need a strict assertion on which keys appear first,
        // but we do know that "One" should appear as a String and 42 as an Integer.
        // The key "One" was inserted as a String, so it should come out as the original String "One".
        // The key 42 is a non-string key and should appear as-is.
        assertTrue(contains(result, "One"));
        assertTrue(contains(result, 42));
    }

    private boolean contains(Object[] arr, Object value) {
        for (Object o : arr) {
            if (o.equals(value)) {
                return true;
            }
        }
        return false;
    }

    @Test
    void testConstructFromHashtable() {
        Hashtable<String, String> source = new Hashtable<>();
        source.put("One", "1");
        CaseInsensitiveMap<String, String> ciMap = new CaseInsensitiveMap<>(source);
        assertEquals("1", ciMap.get("one"));
    }

    @Test
    void testConstructFromIdentityHashMap() {
        IdentityHashMap<String, String> source = new IdentityHashMap<>();
        source.put("One", "1");

        // Now that the constructor throws an exception for IdentityHashMap,
        // we test that behavior using assertThrows.
        assertThrows(IllegalArgumentException.class, () -> {
            new CaseInsensitiveMap<>(source);
        });
    }

    @Test
    void testConstructFromConcurrentNavigableMapNullSafe() {
        // Assuming ConcurrentNavigableMapNullSafe is available and works similarly to a ConcurrentSkipListMap
        ConcurrentNavigableMapNullSafe<String, String> source = new ConcurrentNavigableMapNullSafe<>();
        source.put("One", "1");
        CaseInsensitiveMap<String, String> ciMap = new CaseInsensitiveMap<>(source);
        assertEquals("1", ciMap.get("one"));
    }

    @Test
    void testConstructFromConcurrentHashMapNullSafe() {
        // Assuming ConcurrentHashMapNullSafe is available
        ConcurrentHashMapNullSafe<String, String> source = new ConcurrentHashMapNullSafe<>();
        source.put("One", "1");
        CaseInsensitiveMap<String, String> ciMap = new CaseInsensitiveMap<>(source);
        assertEquals("1", ciMap.get("one"));
    }

    @Test
    void testConstructFromConcurrentSkipListMap() {
        ConcurrentSkipListMap<String, String> source = new ConcurrentSkipListMap<>();
        source.put("One", "1");
        CaseInsensitiveMap<String, String> ciMap = new CaseInsensitiveMap<>(source);
        assertEquals("1", ciMap.get("one"));
    }

    @Test
    void testConstructFromNavigableMapInterface() {
        // NavigableMap is an interface; use a known implementation that is not a TreeMap or ConcurrentSkipListMap
        // But if we want to ensure just that it hits the NavigableMap branch before SortedMap:
        // If source is just a ConcurrentSkipListMap, that will match the ConcurrentNavigableMap branch first.
        // Let's use an anonymous NavigableMap wrapping a ConcurrentSkipListMap:
        NavigableMap<String, String> source = new ConcurrentSkipListMap<>();
        source.put("One", "1");
        // If we've already tested ConcurrentSkipListMap above, consider a different approach:
        // Use a NavigableMap that isn't caught by earlier conditions:
        // However, by code structure, NavigableMap check comes after ConcurrentNavigableMap checks.
        // Let's rely on the order of checks:
        // - The code checks if (source instanceof ConcurrentNavigableMapNullSafe)
        //   then if (source instanceof ConcurrentHashMapNullSafe)
        //   then if (source instanceof ConcurrentNavigableMap)
        //   then if (source instanceof ConcurrentMap)
        //   then if (source instanceof NavigableMap)
        // Since ConcurrentSkipListMap is a ConcurrentNavigableMap, it might get caught earlier.
        // To ensure we hit the NavigableMap branch, we can use a wrapper:
        NavigableMap<String, String> navigableMap = new NavigableMapWrapper<>(source);
        CaseInsensitiveMap<String, String> ciMap = new CaseInsensitiveMap<>(navigableMap);
        assertEquals("1", ciMap.get("one"));
    }

    @Test
    void testConstructFromSortedMapInterface() {
        // Create and populate a TreeMap first
        SortedMap<String, String> temp = new TreeMap<>();
        temp.put("One", "1");

        // Now wrap the populated TreeMap
        SortedMap<String, String> source = Collections.unmodifiableSortedMap(temp);

        CaseInsensitiveMap<String, String> ciMap = new CaseInsensitiveMap<>(source);
        assertEquals("1", ciMap.get("one"));
    }


    // A wrapper class to ensure we test just the NavigableMap interface branch.
    static class NavigableMapWrapper<K,V> extends AbstractMap<K,V> implements NavigableMap<K,V> {
        private final NavigableMap<K,V> delegate;

        NavigableMapWrapper(NavigableMap<K,V> delegate) {
            this.delegate = delegate;
        }

        @Override
        public Entry<K, V> lowerEntry(K key) { return delegate.lowerEntry(key); }
        @Override
        public K lowerKey(K key) { return delegate.lowerKey(key); }
        @Override
        public Entry<K, V> floorEntry(K key) { return delegate.floorEntry(key); }
        @Override
        public K floorKey(K key) { return delegate.floorKey(key); }
        @Override
        public Entry<K, V> ceilingEntry(K key) { return delegate.ceilingEntry(key); }
        @Override
        public K ceilingKey(K key) { return delegate.ceilingKey(key); }
        @Override
        public Entry<K, V> higherEntry(K key) { return delegate.higherEntry(key); }
        @Override
        public K higherKey(K key) { return delegate.higherKey(key); }
        @Override
        public Entry<K, V> firstEntry() { return delegate.firstEntry(); }
        @Override
        public Entry<K, V> lastEntry() { return delegate.lastEntry(); }
        @Override
        public Entry<K, V> pollFirstEntry() { return delegate.pollFirstEntry(); }
        @Override
        public Entry<K, V> pollLastEntry() { return delegate.pollLastEntry(); }
        @Override
        public NavigableMap<K, V> descendingMap() { return delegate.descendingMap(); }
        @Override
        public NavigableSet<K> navigableKeySet() { return delegate.navigableKeySet(); }
        @Override
        public NavigableSet<K> descendingKeySet() { return delegate.descendingKeySet(); }
        @Override
        public NavigableMap<K, V> subMap(K fromKey, boolean fromInclusive, K toKey, boolean toInclusive) {
            return delegate.subMap(fromKey, fromInclusive, toKey, toInclusive);
        }
        @Override
        public NavigableMap<K, V> headMap(K toKey, boolean inclusive) {
            return delegate.headMap(toKey, inclusive);
        }
        @Override
        public NavigableMap<K, V> tailMap(K fromKey, boolean inclusive) {
            return delegate.tailMap(fromKey, inclusive);
        }
        @Override
        public Comparator<? super K> comparator() { return delegate.comparator(); }
        @Override
        public SortedMap<K, V> subMap(K fromKey, K toKey) { return delegate.subMap(fromKey, toKey); }
        @Override
        public SortedMap<K, V> headMap(K toKey) { return delegate.headMap(toKey); }
        @Override
        public SortedMap<K, V> tailMap(K fromKey) { return delegate.tailMap(fromKey); }
        @Override
        public K firstKey() { return delegate.firstKey(); }
        @Override
        public K lastKey() { return delegate.lastKey(); }
        @Override
        public Set<Entry<K, V>> entrySet() { return delegate.entrySet(); }
    }

    @Test
    void testCopyMethodKeyInstanceofStringBothOutcomes() {
        // Create a source map with both a String key and a non-String key
        Map<Object, Object> source = new HashMap<>();
        source.put("One", 1);    // key is a String, will test 'key instanceof String' == true
        source.put(42, "FortyTwo"); // key is an Integer, will test 'key instanceof String' == false

        // Constructing a CaseInsensitiveMap from this source triggers copy()
        CaseInsensitiveMap<Object, Object> ciMap = new CaseInsensitiveMap<>(source);

        // Verify that the entries were copied correctly
        // For the String key "One", it should be case-insensitive now
        assertEquals(1, ciMap.get("one"));

        // For the non-String key 42, it should remain as is
        assertEquals("FortyTwo", ciMap.get(42));
    }

    /**
     * Test to verify the symmetry of the equals method.
     * CaseInsensitiveString.equals(String) returns true,
     * but String.equals(CaseInsensitiveString) returns false,
     * violating the equals contract.
     */
    @Test
    public void testEqualsSymmetry() {
        CaseInsensitiveMap.CaseInsensitiveString cis = new CaseInsensitiveMap.CaseInsensitiveString("Apple");
        String str = "apple";

        // cis.equals(str) should be true
        assertTrue(cis.equals(str), "CaseInsensitiveString should be equal to a String with same letters ignoring case");

        // str.equals(cis) should be false, violating symmetry
        assertFalse(str.equals(cis), "String should not be equal to CaseInsensitiveString, violating symmetry");
    }

    /**
     * Test to check if compareTo is consistent with equals.
     * According to Comparable contract, compareTo should return 0 if and only if equals returns true.
     */
    @Test
    public void testCompareToConsistencyWithEquals() {
        CaseInsensitiveMap.CaseInsensitiveString cis1 = new CaseInsensitiveMap.CaseInsensitiveString("Banana");
        CaseInsensitiveMap.CaseInsensitiveString cis2 = new CaseInsensitiveMap.CaseInsensitiveString("banana");
        String str = "BANANA";

        // cis1.equals(cis2) should be true
        assertTrue(cis1.equals(cis2), "Both CaseInsensitiveString instances should be equal ignoring case");

        // cis1.compareTo(cis2) should be 0
        assertEquals(0, cis1.compareTo(cis2), "compareTo should return 0 for equal CaseInsensitiveString instances");

        // cis1.equals(str) should be true
        assertTrue(cis1.equals(str), "CaseInsensitiveString should be equal to String ignoring case");

        // cis1.compareTo(str) should be 0
        assertEquals(0, cis1.compareTo(str), "compareTo should return 0 when comparing with equal String ignoring case");
    }

    /**
     * Test to demonstrate how CaseInsensitiveString behaves in a HashSet.
     * Since hashCode and equals are overridden, duplicates based on case-insensitive equality should not be added.
     */
    @Test
    public void testHashSetBehavior() {
        Set<CaseInsensitiveMap.CaseInsensitiveString> set = new HashSet<>();
        CaseInsensitiveMap.CaseInsensitiveString cis1 = new CaseInsensitiveMap.CaseInsensitiveString("Cherry");
        CaseInsensitiveMap.CaseInsensitiveString cis2 = new CaseInsensitiveMap.CaseInsensitiveString("cherry");
        String str = "CHERRY";

        set.add(cis1);
        set.add(cis2); // Should not be added as duplicate
        assert set.size() == 1;
        set.add(new CaseInsensitiveMap.CaseInsensitiveString("Cherry")); // Should not be added as duplicate

        // The size should be 1
        assertEquals(1, set.size(), "HashSet should contain only one unique CaseInsensitiveString entry");

        // Even adding a String with same content should not affect the set
        set.add(new CaseInsensitiveMap.CaseInsensitiveString(str));
        assertEquals(1, set.size(), "Adding equivalent CaseInsensitiveString should not increase HashSet size");
    }

    @Test
    public void testCacheReplacement() {
        // Create initial strings and verify they're cached
        CaseInsensitiveMap<String, String> map1 = new CaseInsensitiveMap<>();
        map1.put("test1", "value1");
        map1.put("test2", "value2");

        // Create a new cache with different capacity
        LRUCache<String, CaseInsensitiveMap.CaseInsensitiveString> newCache = new LRUCache<>(500);

        // Replace the cache
        CaseInsensitiveMap.replaceCache(newCache);

        // Create new map after cache replacement
        CaseInsensitiveMap<String, String> map2 = new CaseInsensitiveMap<>();
        map2.put("test3", "value3");
        map2.put("test4", "value4");

        // Verify all maps still work correctly
        assertTrue(map1.containsKey("TEST1")); // Case-insensitive check
        assertTrue(map1.containsKey("TEST2"));
        assertTrue(map2.containsKey("TEST3"));
        assertTrue(map2.containsKey("TEST4"));

        // Verify values are preserved
        assertEquals("value1", map1.get("TEST1"));
        assertEquals("value2", map1.get("TEST2"));
        assertEquals("value3", map2.get("TEST3"));
        assertEquals("value4", map2.get("TEST4"));
    }

    @Test
    public void testReplaceCacheWithNull() {
        assertThrows(NullPointerException.class, () -> CaseInsensitiveMap.replaceCache(null));
    }

    @Test
    public void testStringCachingBasedOnLength() {
        // Test string shorter than max length (should be cached)
        CaseInsensitiveMap.setMaxCacheLengthString(10);
        String shortString = "short";
        Map<String, String> map = new CaseInsensitiveMap<>();
        map.put(shortString, "value1");
        map.put(shortString.toUpperCase(), "value2");

        // Since the string is cached, both keys should reference the same CaseInsensitiveString instance
        assertTrue(map.containsKey(shortString) && map.containsKey(shortString.toUpperCase()),
                "Same short string should use cached instance");

        // Test string longer than max length (should not be cached)
        String longString = "this_is_a_very_long_string_that_exceeds_max_length";
        map.put(longString, "value3");
        map.put(longString.toUpperCase(), "value4");

        // Even though not cached, the map should still work correctly
        assertTrue(map.containsKey(longString) && map.containsKey(longString.toUpperCase()),
                "Long string should work despite not being cached");
        CaseInsensitiveMap.setMaxCacheLengthString(100);
    }

    @Test
    public void testMaxCacheLengthStringBehavior() {
        try {
            CaseInsensitiveMap<String, String> map = new CaseInsensitiveMap<>();

            // Add a key < 100 chars
            String originalKey = "TestString12";
            map.put(originalKey, "value1");

            // Get the CaseInsensitiveString wrapper
            Map<String, String> wrapped = map.getWrappedMap();
            Object originalWrapper = wrapped.keySet().iterator().next();

            // Remove using different case
            map.remove("TESTSTRING12");

            // Put back with different value
            map.put(originalKey, "value2");

            // Get new wrapper
            wrapped = map.getWrappedMap();
            Object newWrapper = wrapped.keySet().iterator().next();

            // Assert same wrapper was reused from cache
            assertSame(originalWrapper, newWrapper, "Cached CaseInsensitiveString instance should be reused");

            // Now set max length to 10 (our test string is longer than 10)
            CaseInsensitiveMap.setMaxCacheLengthString(10);

            // Clear map and repeat process
            map.clear();
            map.put(originalKey, "value3");

            Object firstWrapper = map.getWrappedMap().keySet().iterator().next();

            map.remove("TESTstring12");
            map.put(originalKey, "value4");

            Object secondWrapper = map.getWrappedMap().keySet().iterator().next();

            // Should be different instances now as string is > 10 chars
            assertNotSame(firstWrapper, secondWrapper, "Strings exceeding max length should use different instances");
        } finally {
            // Reset to default
            CaseInsensitiveMap.setMaxCacheLengthString(100);
        }
    }

    @Test
    public void testCaseInsensitiveEntryToString() {
        CaseInsensitiveMap<String, String> map = new CaseInsensitiveMap<>();
        map.put("TestKey", "TestValue");

        Set<Map.Entry<String, String>> entrySet = map.entrySet();
        Map.Entry<String, String> entry = entrySet.iterator().next();

        assertEquals("TestKey=TestValue", entry.toString(), "Entry toString() should match 'key=value' format");
    }

    @Test
    public void testCaseInsensitiveEntryEqualsWithNonEntry() {
        CaseInsensitiveMap<String, String> map = new CaseInsensitiveMap<>();
        map.put("TestKey", "TestValue");

        Map.Entry<String, String> entry = map.entrySet().iterator().next();

        // Test equals with a non-Entry object
        String notAnEntry = "not an entry";
        assertFalse(entry.equals(notAnEntry), "Entry should not be equal to non-Entry object");
    }
    
    @Test
    public void testInvalidMaxLength() {
        assertThrows(IllegalArgumentException.class, () -> CaseInsensitiveMap.setMaxCacheLengthString(9));
    }

    @Test
    public void testCaseInsensitiveStringSubSequence() {
        CaseInsensitiveMap.CaseInsensitiveString cis = new CaseInsensitiveMap.CaseInsensitiveString("Hello");
        CharSequence seq = cis.subSequence(1, 4);
        assertEquals("ell", seq.toString());
    }

    @Test
    public void testCaseInsensitiveStringChars() {
        String str = "a\uD83D\uDE00b";
        CaseInsensitiveMap.CaseInsensitiveString cis = new CaseInsensitiveMap.CaseInsensitiveString(str);
        int[] expected = str.chars().toArray();
        assertArrayEquals(expected, cis.chars().toArray());
    }

    @Test
    public void testCaseInsensitiveStringCodePoints() {
        String str = "a\uD83D\uDE00b";
        CaseInsensitiveMap.CaseInsensitiveString cis = new CaseInsensitiveMap.CaseInsensitiveString(str);
        int[] expected = str.codePoints().toArray();
        assertArrayEquals(expected, cis.codePoints().toArray());
    }

    @EnabledIfSystemProperty(named = "performRelease", matches = "true")
    @Test
    void testCaseInsensitiveMapPerformanceComparison() {
        LOG.info("Performance Test: CaseInsensitiveMap vs TreeMap with String.CASE_INSENSITIVE_ORDER");
        LOG.info("================================================================");
        
        Random random = new Random(42); // Fixed seed for reproducible results
        
        // Test 1: CaseInsensitiveMap backed by HashMap
        LOG.info("\nTest 1: CaseInsensitiveMap(HashMap) vs TreeMap(String.CASE_INSENSITIVE_ORDER)");
        testMapPerformance(new CaseInsensitiveMap<>(new HashMap<>()), 
                          new TreeMap<>(String.CASE_INSENSITIVE_ORDER), 
                          "CaseInsensitiveMap(HashMap)", 
                          "TreeMap(CASE_INSENSITIVE_ORDER)",
                          random);
        
        // Test 2: CaseInsensitiveMap backed by LinkedHashMap  
        LOG.info("\nTest 2: CaseInsensitiveMap(LinkedHashMap) vs TreeMap(String.CASE_INSENSITIVE_ORDER)");
        testMapPerformance(new CaseInsensitiveMap<>(new LinkedHashMap<>()), 
                          new TreeMap<>(String.CASE_INSENSITIVE_ORDER), 
                          "CaseInsensitiveMap(LinkedHashMap)", 
                          "TreeMap(CASE_INSENSITIVE_ORDER)",
                          random);
        
        // Test 3: CaseInsensitiveMap backed by TreeMap() vs TreeMap(String.CASE_INSENSITIVE_ORDER)
        LOG.info("\nTest 3: CaseInsensitiveMap(TreeMap) vs TreeMap(String.CASE_INSENSITIVE_ORDER)");
        testMapPerformance(new CaseInsensitiveMap<>(new TreeMap<>()), 
                          new TreeMap<>(String.CASE_INSENSITIVE_ORDER), 
                          "CaseInsensitiveMap(TreeMap)", 
                          "TreeMap(CASE_INSENSITIVE_ORDER)",
                          random);
        
        LOG.info("\n================================================================");
        LOG.info("Performance test completed");
    }
    
    private void testMapPerformance(Map<String, String> map1, Map<String, String> map2, 
                                   String map1Name, String map2Name, Random random) {
        
        // Generate test data
        String[] keys = new String[10000];
        String[] values = new String[10000];
        for (int i = 0; i < keys.length; i++) {
            keys[i] = StringUtilities.getRandomString(random, 5, 15);
            values[i] = StringUtilities.getRandomString(random, 10, 20);
        }
        
        // JIT warmup - run both maps several times to ensure fair comparison
        warmupMaps(map1, map2, keys, values, 3);
        
        // Test map1 performance
        long map1Time = timeMapOperations(map1, keys, values, 2000);
        
        // Clear and test map2 performance  
        long map2Time = timeMapOperations(map2, keys, values, 2000);
        
        // Calculate speedup

        int map1Ops = countOps(map1, keys, values, 2000);
        int map2Ops = countOps(map2, keys, values, 2000);
        
        LOG.info(String.format("%-35s: %,d operations in %,d ms%n", map1Name, map1Ops, map1Time));
        LOG.info(String.format("%-35s: %,d operations in %,d ms%n", map2Name, map2Ops, map2Time));
        
        double opsSpeedup = (double) map1Ops / map2Ops;
        LOG.info(String.format("Operations speedup: %.2fx (%s performed %.2fx more operations)%n", 
                         opsSpeedup, 
                         opsSpeedup > 1.0 ? map1Name : map2Name, 
                         opsSpeedup > 1.0 ? opsSpeedup : 1.0 / opsSpeedup));
    }
    
    private void warmupMaps(Map<String, String> map1, Map<String, String> map2, 
                           String[] keys, String[] values, int iterations) {
        // Warmup both maps alternately to ensure fair JIT compilation
        for (int i = 0; i < iterations; i++) {
            performMapOperations(map1, keys, values, 100);
            map1.clear();
            performMapOperations(map2, keys, values, 100);
            map2.clear();
        }
    }
    
    private long timeMapOperations(Map<String, String> map, String[] keys, String[] values, long durationMs) {
        map.clear();
        long startTime = System.currentTimeMillis();
        long endTime = startTime + durationMs;
        
        int i = 0;
        while (System.currentTimeMillis() < endTime) {
            String key = keys[i % keys.length];
            String value = values[i % values.length];
            
            map.put(key, value);
            map.get(key.toLowerCase()); // Test case insensitive lookup
            map.get(key.toUpperCase()); // Test case insensitive lookup
            map.containsKey(key);
            
            i++;
            if (i % 1000 == 0) {
                map.clear(); // Periodically clear to test fresh insertions
            }
        }
        
        return System.currentTimeMillis() - startTime;
    }
    
    private int countOps(Map<String, String> map, String[] keys, String[] values, long durationMs) {
        map.clear();
        long startTime = System.currentTimeMillis();
        long endTime = startTime + durationMs;
        
        int operations = 0;
        int i = 0;
        while (System.currentTimeMillis() < endTime) {
            String key = keys[i % keys.length];
            String value = values[i % values.length];
            
            map.put(key, value);
            map.get(key.toLowerCase());
            map.get(key.toUpperCase());
            map.containsKey(key);
            
            operations += 4; // 4 operations per loop
            i++;
            if (i % 1000 == 0) {
                map.clear();
            }
        }
        
        return operations;
    }
    
    private void performMapOperations(Map<String, String> map, String[] keys, String[] values, int count) {
        for (int i = 0; i < count; i++) {
            String key = keys[i % keys.length];
            String value = values[i % values.length];
            
            map.put(key, value);
            map.get(key.toLowerCase());
            map.get(key.toUpperCase());
            map.containsKey(key);
        }
    }

    // ---------------------------------------------------
    
    private CaseInsensitiveMap<String, Object> createSimpleMap()
    {
        CaseInsensitiveMap<String, Object> stringMap = new CaseInsensitiveMap<>();
        stringMap.put("One", "Two");
        stringMap.put("Three", "Four");
        stringMap.put("Five", "Six");
        return stringMap;
    }

    private Map.Entry<String, Object> getEntry(final String key, final Object value)
    {
        return new Map.Entry()
        {
            Object myValue = value;

            public String getKey()
            {
                return key;
            }

            public Object getValue()
            {
                return value;
            }

            public Object setValue(Object value)
            {
                Object save = myValue;
                myValue = value;
                return save;
            }
        };
    }

    @Test
    public void testAutoExpansionWithArrayKeys() {
        // Create CaseInsensitiveMap with MultiKeyMap backing
        @SuppressWarnings("unchecked")
        Map backing = new MultiKeyMap<String>();
        CaseInsensitiveMap<Object, String> map = new CaseInsensitiveMap<>(Collections.emptyMap(), backing);
        
        // Test with Object array - should auto-expand to multi-key
        Object[] keys1 = {"DEPT", "Engineering"};
        assertNull(map.put(keys1, "Value1"));
        assertEquals("Value1", map.get(new Object[]{"dept", "ENGINEERING"}));
        assertTrue(map.containsKey(new Object[]{"DEPT", "engineering"}));
        
        // Test with String array - should auto-expand to multi-key  
        String[] keys2 = {"dept", "Marketing"};
        assertNull(map.put(keys2, "Value2"));
        assertEquals("Value2", map.get(new String[]{"DEPT", "marketing"}));
        
        // Test removal with array
        assertEquals("Value1", map.remove(new Object[]{"dept", "Engineering"}));
        assertFalse(map.containsKey(new Object[]{"DEPT", "engineering"}));
    }

    @Test
    public void testAutoExpansionWithCollectionKeys() {
        // Create CaseInsensitiveMap with MultiKeyMap backing
        @SuppressWarnings("unchecked")
        Map backing = new MultiKeyMap<String>();
        CaseInsensitiveMap<Object, String> map = new CaseInsensitiveMap<>(Collections.emptyMap(), backing);
        
        // Test with ArrayList - should auto-expand to multi-key
        List<Object> keys1 = Arrays.asList("DEPT", "Engineering");
        assertNull(map.put(keys1, "Value1"));
        assertEquals("Value1", map.get(Arrays.asList("dept", "ENGINEERING")));
        assertTrue(map.containsKey(Arrays.asList("DEPT", "engineering")));
        
        // Test with different collection type
        Set<Object> keys2 = new LinkedHashSet<>(Arrays.asList("dept", "Marketing"));
        assertNull(map.put(keys2, "Value2"));
        assertEquals("Value2", map.get(Arrays.asList("DEPT", "marketing")));
        
        // Test removal with collection
        assertEquals("Value1", map.remove(Arrays.asList("dept", "Engineering")));
        assertFalse(map.containsKey(Arrays.asList("DEPT", "engineering")));
    }

    @Test
    public void testAutoExpansionOnlyWithMultiKeyMapBacking() {
        // Create CaseInsensitiveMap with HashMap backing (not MultiKeyMap)
        CaseInsensitiveMap<Object, String> map = new CaseInsensitiveMap<>(Collections.emptyMap(), new HashMap<>());
        
        // Test that arrays and collections are NOT auto-expanded with non-MultiKeyMap backing
        Object[] keys = {"key1", "key2"};
        assertNull(map.put(keys, "Value1"));
        
        // Should store the array itself as a key, not expand it
        assertEquals("Value1", map.get(keys)); // Same array object
        assertTrue(map.containsKey(keys)); // Same array object
        
        // Different array with same contents should not match (since it's stored as object reference)
        Object[] differentArray = {"key1", "key2"};
        assertNull(map.get(differentArray));
        assertFalse(map.containsKey(differentArray));
    }

    @Test
    public void testAutoExpansionCaseInsensitiveStringHandling() {
        // Create CaseInsensitiveMap with MultiKeyMap backing (flattenDimensions=true for auto-expansion)
        @SuppressWarnings("unchecked")
        Map backing = new MultiKeyMap<String>(true);
        CaseInsensitiveMap<Object, String> map = new CaseInsensitiveMap<>(Collections.emptyMap(), backing);
        
        // Test that String elements in arrays/collections are handled case-insensitively
        map.put(new String[]{"Dept", "Engineering"}, "Value1");
        
        // Should find with different case
        assertEquals("Value1", map.get(new String[]{"DEPT", "engineering"}));
        assertEquals("Value1", map.get(Arrays.asList("dept", "ENGINEERING")));
        assertTrue(map.containsKey(new Object[]{"DEPT", "Engineering"}));
        
        // Test with mixed types (String and non-String)
        map.put(Arrays.asList("Project", 123, "Alpha"), "Value2");
        assertEquals("Value2", map.get(new Object[]{"PROJECT", 123, "alpha"}));
        assertEquals("Value2", map.get(Arrays.asList("project", 123, "ALPHA")));
        
        // Only String keys should be case-insensitive
        assertNull(map.get(Arrays.asList("Project", 456, "Alpha"))); // Different number
    }

    @Test
    public void testAutoExpansionWithTypedArrays() {
        // Create CaseInsensitiveMap with MultiKeyMap backing
        @SuppressWarnings("unchecked")
        Map backing = new MultiKeyMap<String>();
        CaseInsensitiveMap<Object, String> map = new CaseInsensitiveMap<>(Collections.emptyMap(), backing);
        
        // Test with int array - should pass through directly (no Strings to wrap)
        // Arrays are unpacked into multi-key lookups in MultiKeyMap
        int[] intKeys = {1, 2, 3};
        assertNull(map.put(intKeys, "IntValue"));
        
        // Arrays are unpacked into multi-key lookups in MultiKeyMap
        assertEquals("IntValue", map.get(intKeys)); // Same array object
        assertTrue(map.containsKey(intKeys)); // Same array object
        
        // Different array with same contents should match (unpacked to same keys)
        int[] differentIntArray = {1, 2, 3};
        assertEquals("IntValue", map.get(differentIntArray));
        assertTrue(map.containsKey(differentIntArray));
        
        // Test with double array - should pass through directly
        double[] doubleKeys = {1.1, 2.2};
        assertNull(map.put(doubleKeys, "DoubleValue"));
        assertEquals("DoubleValue", map.get(doubleKeys)); // Same array object
        
        // Test removal
        assertEquals("IntValue", map.remove(intKeys));
        assertFalse(map.containsKey(intKeys));
    }
}
