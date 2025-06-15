package com.cedarsoftware.util;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

@SuppressWarnings("ResultOfMethodCallIgnored")
public class TrackingMapTest
{
    @Test
    public void getFree() {
        TrackingMap<String, Object> map = new TrackingMap<>(new CaseInsensitiveMap<String, Object>());
        map.put("first", "value");
        map.put("second", "value");
        map.expungeUnused();
        assertEquals(0, map.size());
        assertTrue(map.isEmpty());
    }

    @Test
    public void getOne() {
        TrackingMap<String, Object> map = new TrackingMap<>(new CaseInsensitiveMap<String, Object>());
        map.put("first", "firstValue");
        map.put("second", "value");
        map.get("first");
        map.expungeUnused();
        assertEquals(1, map.size());
        assertEquals(map.get("first"), "firstValue");
        assertFalse(map.isEmpty());
    }

    @Test
    public void getOneCaseInsensitive() {
        TrackingMap<String, Object> map = new TrackingMap<>(new CaseInsensitiveMap<String, Object>());
        map.put("first", "firstValue");
        map.put("second", "value");
        map.get("FiRsT");
        map.expungeUnused();
        assertEquals(1, map.size());
        assertEquals(map.get("first"), "firstValue");
        assertFalse(map.isEmpty());
    }

    @Test
    public void getOneMultiple() {
        TrackingMap<String, Object> map = new TrackingMap<>(new CaseInsensitiveMap<String, Object>());
        map.put("first", "firstValue");
        map.put("second", "value");
        map.get("FiRsT");
        map.get("FIRST");
        map.get("First");
        map.expungeUnused();
        assertEquals(1, map.size());
        assertEquals(map.get("first"), "firstValue");
        assertFalse(map.isEmpty());
    }

    @Test
    public void containsKeyCounts() {
        TrackingMap<String, Object> map = new TrackingMap<>(new CaseInsensitiveMap<String, Object>());
        map.put("first", "firstValue");
        map.put("second", "value");
        map.containsKey("first");
        map.expungeUnused();
        assertEquals(1, map.size());
        assertEquals(map.get("first"), "firstValue");
        assertFalse(map.isEmpty());
    }

    @Test
    public void containsValueDoesNotCount() {
        TrackingMap<String, Object> map = new TrackingMap<>(new CaseInsensitiveMap<String, Object>());
        map.put("first", "firstValue");
        map.put("second", "value");
        map.containsValue("firstValue");
        map.expungeUnused();
        assertEquals(0, map.size());
        assertTrue(map.isEmpty());
    }

    @Test
    public void sameBackingMapsAreEqual() {
        CaseInsensitiveMap<String, Object> backingMap = new CaseInsensitiveMap<>();
        TrackingMap<String, Object> map1 = new TrackingMap<>(backingMap);
        TrackingMap<String, Object> map2 = new TrackingMap<>(backingMap);
        assertEquals(map1, map2);
    }

    @Test
    public void equalBackingMapsAreEqual() {
        Map<Character, Object> map1 = new TrackingMap<>(new HashMap<>());
        Map<Character, Object> map2 = new TrackingMap<>(new HashMap<>());
        assertEquals(map1, map2);

        map1.put('a', 65);
        map1.put('b', 66);
        map2 = new TrackingMap<>(new HashMap<>());
        map2.put('a', 65);
        map2.put('b', 66);
        assertEquals(map1, map2);
    }

    @Test
    public void unequalBackingMapsAreNotEqual()
    {
        Map<Character, Object> map1 = new TrackingMap<>(new HashMap<>());
        Map<Character, Object> map2 = new TrackingMap<>(new HashMap<>());
        assertEquals(map1, map2);

        map1.put('a', 65);
        map1.put('b', 66);
        map2 = new TrackingMap<>(new HashMap<>());
        map2.put('a', 65);
        map2.put('b', 66);
        map2.put('c', 67);
        assertNotEquals(map1, map2);
    }

    @Test
    public void testDifferentClassIsEqual()
    {
        CaseInsensitiveMap<String, Object> backingMap = new CaseInsensitiveMap<>();
        backingMap.put("a", "alpha");
        backingMap.put("b", "bravo");

        // Identity check
        Map<String, Object> map1 = new TrackingMap<>(backingMap);
        assert map1.equals(backingMap);

        // Equivalence check
        Map<String, Object> map2 = new LinkedHashMap<>();
        map2.put("b", "bravo");
        map2.put("a", "alpha");

        assert map1.equals(map2);
    }

    @Test
    public void testGet() {
        Map<String, Object> ciMap = new CaseInsensitiveMap<>();
        ciMap.put("foo", "bar");
        Map<String, Object> map = new TrackingMap<>(ciMap);
        assert map.get("Foo").equals("bar");
    }

    @Test
    public void testPut() {
        Map<String, Object> ciMap = new CaseInsensitiveMap<>();
        ciMap.put("foo", "bar");
        Map<String, Object> map = new TrackingMap<>(ciMap);
        map.put("Foo", "baz");
        assert map.get("foo").equals("baz");
        assert ciMap.get("foo").equals("baz");
        assert map.size() == 1;
    }

    @Test
    public void testContainsKey() {
        Map<String, Object> ciMap = new CaseInsensitiveMap<>();
        ciMap.put("foo", "bar");
        Map<String, Object> map = new TrackingMap<>(ciMap);
        map.containsKey("FOO");
    }

    @Test
    public void testPutAll() {
        Map<String, Object> ciMap = new CaseInsensitiveMap<>();
        ciMap.put("foo", "bar");
        Map<String, Object> map = new TrackingMap<>(ciMap);
        Map<String, Object> additionalEntries = new HashMap<>();
        additionalEntries.put("animal", "aardvaark");
        additionalEntries.put("ballast", "bubbles");
        additionalEntries.put("tricky", additionalEntries);
        map.putAll(additionalEntries);
        assert ciMap.get("ballast").equals("bubbles");
        assert ciMap.size() == 4;
    }

    @Test
    public void testRemove() throws Exception {
        TrackingMap<String, Object> map = new TrackingMap<>(new CaseInsensitiveMap<String, Object>());
        map.put("first", "firstValue");
        map.put("second", "secondValue");
        map.put("third", "thirdValue");
        map.get("FiRsT");
        map.get("ThirD");
        map.remove("first");
        map.expungeUnused();
        assertEquals(1, map.size());
        assertEquals(map.get("thiRd"), "thirdValue");
        assertFalse(map.isEmpty());
    }

    @Test
    public void testHashCode() throws Exception {
        Map<String, Object> map1 = new TrackingMap<>(new CaseInsensitiveMap<String, Object>());
        map1.put("f", "foxtrot");
        map1.put("o", "oscar");

        Map<String, Object> map2 = new LinkedHashMap<>();
        map2.put("o", "foxtrot");
        map2.put("f", "oscar");

        Map<String, Object> map3 = new TrackingMap<>(new CaseInsensitiveMap<>());
        map3.put("F", "foxtrot");
        map3.put("O", "oscar");

        assert map1.hashCode() == map2.hashCode();
        assert map2.hashCode() == map3.hashCode();
    }

    @Test
    public void testToString() {
        Map<String, Object> ciMap = new CaseInsensitiveMap<>();
        ciMap.put("foo", "bar");
        TrackingMap<String, Object> map = new TrackingMap<>(ciMap);
        assertNotNull(map.toString());
    }

    @Test
    public void testClear() throws Exception {
        Map<String, Object> map = new TrackingMap<>(new CaseInsensitiveMap<String, Object>());
        map.put("first", "firstValue");
        map.put("second", "secondValue");
        map.put("third", "thirdValue");
        map.get("FiRsT");
        map.get("ThirD");
        map.clear();
        assertEquals(0, map.size());
        assertTrue(map.isEmpty());
    }

    @Test
    public void testValues() throws Exception {
        Map<String, Object> map = new TrackingMap<>(new CaseInsensitiveMap<String, Object>());
        map.put("first", "firstValue");
        map.put("second", "secondValue");
        map.put("third", "thirdValue");
        Collection<Object> values = map.values();
        assertNotNull(values);
        assertEquals(3, map.size());
        assertTrue(values.contains("firstValue"));
        assertTrue(values.contains("secondValue"));
        assertTrue(values.contains("thirdValue"));
    }

    @Test
    public void testKeySet() throws Exception {
        Map<String, Object> map = new TrackingMap<>(new CaseInsensitiveMap<String, Object>());
        map.put("first", "firstValue");
        map.put("second", "secondValue");
        map.put("third", "thirdValue");
        Collection<String> keys = map.keySet();
        assertNotNull(keys);
        assertEquals(3, map.size());
        assertTrue(keys.contains("first"));
        assertTrue(keys.contains("second"));
        assertTrue(keys.contains("third"));
    }

    @Test
    public void testEntrySet() throws Exception {
        CaseInsensitiveMap<String, Object> backingMap = new CaseInsensitiveMap<>();
        Map<String, Object> map = new TrackingMap<>(backingMap);
        map.put("first", "firstValue");
        map.put("second", "secondValue");
        map.put("third", "thirdValue");
        Set<Map.Entry<String, Object>> keys = map.entrySet();
        assertNotNull(keys);
        assertEquals(3, keys.size());
        assertEquals(backingMap.entrySet(), map.entrySet());
    }

    @Test
    public void testInformAdditionalUsage() throws Exception {
        TrackingMap<String, Object> map = new TrackingMap<>(new CaseInsensitiveMap<String, Object>());
        map.put("first", "firstValue");
        map.put("second", "secondValue");
        map.put("third", "thirdValue");
        Collection<String> additionalUsage = new HashSet<>();
        additionalUsage.add("FiRsT");
        additionalUsage.add("ThirD");
        map.informAdditionalUsage(additionalUsage);
        map.remove("first");
        map.expungeUnused();
        assertEquals(1, map.size());
        assertEquals(map.get("thiRd"), "thirdValue");
        assertFalse(map.isEmpty());
    }

    @Test
    public void testInformAdditionalUsage1() throws Exception {
        TrackingMap<String, Object> map = new TrackingMap<>(new CaseInsensitiveMap<String, Object>());
        map.put("first", "firstValue");
        map.put("second", "secondValue");
        map.put("third", "thirdValue");
        TrackingMap<String, Object> additionalUsage = new TrackingMap<>(map);
        additionalUsage.get("FiRsT");
        additionalUsage.get("ThirD");
        map.informAdditionalUsage(additionalUsage);
        map.remove("first");
        map.expungeUnused();
        assertEquals(1, map.size());
        assertEquals(map.get("thiRd"), "thirdValue");
        assertFalse(map.isEmpty());
    }

    @Test
    public void testConstructWithNull()
    {
        try
        {
            new TrackingMap<>(null);
            fail();
        }
        catch (IllegalArgumentException ignored)
        { }
    }

    @Test
    public void testPuDoesNotCountAsAccess()
    {
        TrackingMap<String, Object> trackMap = new TrackingMap<>(new CaseInsensitiveMap<>());
        trackMap.put("k", "kite");
        trackMap.put("u", "uniform");

        assert trackMap.keysUsed().isEmpty();

        trackMap.put("K", "kilo");
        assert trackMap.keysUsed().isEmpty();
        assert trackMap.size() == 2;
    }

    @Test
    public void testContainsKeyNotCoundOnNonExistentKey()
    {
        TrackingMap<String, String> trackMap = new TrackingMap<>(new CaseInsensitiveMap<>());
        trackMap.put("y", "yankee");
        trackMap.put("z", "zulu");

        trackMap.containsKey("f");

        assert trackMap.keysUsed().size() == 1;
        assert trackMap.keysUsed().contains("f");
    }

    @Test
    public void testGetNotCoundOnNonExistentKey()
    {
        TrackingMap<String, String> trackMap = new TrackingMap<>(new CaseInsensitiveMap<>());
        trackMap.put("y", "yankee");
        trackMap.put("z", "zulu");

        trackMap.get("f");

        assert trackMap.keysUsed().size() == 1;
        assert trackMap.keysUsed().contains("f");
    }

    @Test
    public void testGetOfNullValueCountsAsAccess()
    {
        TrackingMap<String, String> trackMap = new TrackingMap<>(new CaseInsensitiveMap<>());

        trackMap.put("y", null);
        trackMap.put("z", "zulu");

        trackMap.get("y");

        assert trackMap.keysUsed().size() == 1;
    }

    @Test
    public void testFetchInternalMap()
    {
        TrackingMap<String, Object> trackMap = new TrackingMap<>(new CaseInsensitiveMap<>());
        assert trackMap.getWrappedMap() instanceof CaseInsensitiveMap;
        trackMap = new TrackingMap<>(new HashMap<>());
        assert trackMap.getWrappedMap() instanceof HashMap;
    }

    @Test
    public void testReplaceContentsMaintainsInstanceAndResetsState()
    {
        CaseInsensitiveMap<String, String> original = new CaseInsensitiveMap<>();
        original.put("a", "alpha");
        original.put("b", "bravo");
        TrackingMap<String, String> tracking = new TrackingMap<>(original);
        tracking.get("a");

        Map<String, String> replacement = new HashMap<>();
        replacement.put("c", "charlie");
        replacement.put("d", "delta");

        Map<String, String> before = tracking.getWrappedMap();
        tracking.replaceContents(replacement);

        assertSame(before, tracking.getWrappedMap());
        assertEquals(2, tracking.size());
        assertTrue(tracking.containsKey("c"));
        assertTrue(tracking.containsKey("d"));
        assertFalse(tracking.containsKey("a"));
        assertTrue(tracking.keysUsed().isEmpty());
    }

    @Test
    public void testReplaceContentsWithNullThrows()
    {
        TrackingMap<String, String> tracking = new TrackingMap<>(new HashMap<>());
        try
        {
            tracking.replaceContents(null);
            fail();
        }
        catch (IllegalArgumentException ignored)
        { }
    }

    @Test
    public void testSetWrappedMapDelegatesToReplaceContents()
    {
        Map<String, String> base = new HashMap<>();
        base.put("x", "xray");
        TrackingMap<String, String> tracking = new TrackingMap<>(base);

        Map<String, String> newContents = new HashMap<>();
        newContents.put("y", "yankee");
        Map<String, String> before = tracking.getWrappedMap();

        tracking.setWrappedMap(newContents);

        assertSame(before, tracking.getWrappedMap());
        assertEquals(1, tracking.size());
        assertTrue(tracking.containsKey("y"));
        assertTrue(tracking.keysUsed().isEmpty());
    }

    @Test
    public void testSetWrappedMapNullThrows()
    {
        TrackingMap<String, String> tracking = new TrackingMap<>(new HashMap<>());
        try
        {
            tracking.setWrappedMap(null);
            fail();
        }
        catch (IllegalArgumentException ignored)
        { }
    }
}
