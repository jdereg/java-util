package com.cedarsoftware.util;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.*;

import static junit.framework.TestCase.assertFalse;
import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;

@SuppressWarnings("ResultOfMethodCallIgnored")
@PrepareForTest({UsageTrackingMap.class, Map.class})
@RunWith(PowerMockRunner.class)
public class UsageTrackingMapTest {
    @Mock
    public Map<String, Object> mockedBackingMap;

    @Mock
    public Map<String, Object> anotherMockedBackingMap;


    @Test
    public void getFree() {
        UsageTrackingMap<String, Object> map = new UsageTrackingMap<>(new CaseInsensitiveMap<String, Object>());
        map.put("first", "value");
        map.put("second", "value");
        map.expungeUnused();
        assertEquals(0, map.size());
        assertTrue(map.isEmpty());
    }

    @Test
    public void getOne() {
        UsageTrackingMap<String, Object> map = new UsageTrackingMap<>(new CaseInsensitiveMap<String, Object>());
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
        UsageTrackingMap<String, Object> map = new UsageTrackingMap<>(new CaseInsensitiveMap<String, Object>());
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
        UsageTrackingMap<String, Object> map = new UsageTrackingMap<>(new CaseInsensitiveMap<String, Object>());
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
        UsageTrackingMap<String, Object> map = new UsageTrackingMap<>(new CaseInsensitiveMap<String, Object>());
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
        UsageTrackingMap<String, Object> map = new UsageTrackingMap<>(new CaseInsensitiveMap<String, Object>());
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
        UsageTrackingMap<String, Object> map1 = new UsageTrackingMap<>(backingMap);
        UsageTrackingMap<String, Object> map2 = new UsageTrackingMap<>(backingMap);
        assertEquals(map1, map2);
    }

    @Test
    public void equalBackingMapsAreEqual() {
        UsageTrackingMap<String, Object> map1 = new UsageTrackingMap<>(mockedBackingMap);
        UsageTrackingMap<String, Object> map2 = new UsageTrackingMap<>(anotherMockedBackingMap);
        PowerMockito.when(mockedBackingMap.equals(anotherMockedBackingMap)).thenReturn(true);
        assertEquals(map1, map2);
        verify(mockedBackingMap).equals(anotherMockedBackingMap);
    }

    @Test
    public void unequalBackingMapsAreNotEqual() {
        UsageTrackingMap<String, Object> map1 = new UsageTrackingMap<>(mockedBackingMap);
        UsageTrackingMap<String, Object> map2 = new UsageTrackingMap<>(anotherMockedBackingMap);
        PowerMockito.when(mockedBackingMap.equals(any())).thenReturn(false);
        assertNotEquals(map1, map2);
        verify(mockedBackingMap).equals(anotherMockedBackingMap);
    }

    @Test
    public void differentClassIsNeverEqual() {
        CaseInsensitiveMap<String, Object> backingMap = new CaseInsensitiveMap<>();
        UsageTrackingMap<String, Object> map1 = new UsageTrackingMap<>(backingMap);
        PowerMockito.when(mockedBackingMap.equals(any())).thenReturn(true);
        assertNotEquals(map1, backingMap);
    }

    @Test
    public void testGet() throws Exception {
        UsageTrackingMap<String, Object> map = new UsageTrackingMap<>(mockedBackingMap);
        map.get("key");
        verify(mockedBackingMap).get("key");
    }

    @Test
    public void testPut() throws Exception {
        UsageTrackingMap<String, Object> map = new UsageTrackingMap<>(mockedBackingMap);
        map.put("key", "value");
        verify(mockedBackingMap).put("key", "value");
    }

    @Test
    public void testContainsKey() throws Exception {
        UsageTrackingMap<String, Object> map = new UsageTrackingMap<>(mockedBackingMap);
        map.containsKey("key");
        verify(mockedBackingMap).containsKey("key");
    }

    @Test
    public void testPutAll() throws Exception {
        UsageTrackingMap<String, Object> map = new UsageTrackingMap<>(mockedBackingMap);
        Map additionalEntries = new HashMap();
        additionalEntries.put("animal", "aardvaark");
        additionalEntries.put("ballast", "bubbles");
        additionalEntries.put("tricky", additionalEntries);
        map.putAll(additionalEntries);
        verify(mockedBackingMap).putAll(additionalEntries);
    }

    @Test
    public void testRemove() throws Exception {
        UsageTrackingMap<String, Object> map = new UsageTrackingMap<>(new CaseInsensitiveMap<String, Object>());
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

    }

    @Test
    public void testToString() throws Exception {
        UsageTrackingMap<String, Object> map = new UsageTrackingMap<>(mockedBackingMap);
        assertNotNull(map.toString());
    }

    @Test
    public void testClear() throws Exception {
        UsageTrackingMap<String, Object> map = new UsageTrackingMap<>(new CaseInsensitiveMap<String, Object>());
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
        UsageTrackingMap<String, Object> map = new UsageTrackingMap<>(new CaseInsensitiveMap<String, Object>());
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
        UsageTrackingMap<String, Object> map = new UsageTrackingMap<>(new CaseInsensitiveMap<String, Object>());
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
        UsageTrackingMap<String, Object> map = new UsageTrackingMap<>(backingMap);
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
        UsageTrackingMap<String, Object> map = new UsageTrackingMap<>(new CaseInsensitiveMap<String, Object>());
        map.put("first", "firstValue");
        map.put("second", "secondValue");
        map.put("third", "thirdValue");
        Set<String> additionalUsage = new HashSet<>();
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
        UsageTrackingMap<String, Object> map = new UsageTrackingMap<>(new CaseInsensitiveMap<String, Object>());
        map.put("first", "firstValue");
        map.put("second", "secondValue");
        map.put("third", "thirdValue");
        UsageTrackingMap<String, Object> additionalUsage = new UsageTrackingMap<>(map);
        additionalUsage.get("FiRsT");
        additionalUsage.get("ThirD");
        map.informAdditionalUsage(additionalUsage);
        map.remove("first");
        map.expungeUnused();
        assertEquals(1, map.size());
        assertEquals(map.get("thiRd"), "thirdValue");
        assertFalse(map.isEmpty());
    }
}