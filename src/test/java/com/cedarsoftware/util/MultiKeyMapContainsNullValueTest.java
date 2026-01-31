package com.cedarsoftware.util;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test to expose and verify the fix for the bug where containsKey() methods
 * incorrectly return false when a key exists but has a null value.
 * 
 * The bug is in the contains* methods which check if get*() != null,
 * but this fails when the key exists with a null value.
 */
public class MultiKeyMapContainsNullValueTest {

    @Test
    void testContainsSimpleSingleKeyWithNullValue() {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        
        // Put a key with null value
        map.put("existingKey", null);
        
        // This should return true - the key exists even though value is null
        assertTrue(map.containsKey("existingKey"), 
                "containsKey should return true for existing key with null value");
        
        // Verify the value is indeed null
        assertNull(map.get("existingKey"), 
                "get should return null for null value");
        
        // Verify key doesn't exist returns false
        assertFalse(map.containsKey("nonExistentKey"), 
                "containsKey should return false for non-existent key");
    }

    @Test
    void testContainsEmptyArrayWithNullValue() {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        
        // Put empty array with null value
        Object[] emptyArray = new Object[0];
        map.put(emptyArray, null);
        
        // This should return true - the key exists even though value is null
        assertTrue(map.containsKey(emptyArray), 
                "containsKey should return true for empty array with null value");
        assertTrue(map.containsKey(new Object[0]), 
                "containsKey should return true for equivalent empty array with null value");
        
        // Verify the value is indeed null
        assertNull(map.get(emptyArray), 
                "get should return null for null value");
    }

    @Test
    void testContainsArrayLength1WithNullValue() {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        
        // Put single-element array with null value
        Object[] singleArray = {"key1"};
        map.put(singleArray, null);
        
        // This should return true - the key exists even though value is null
        assertTrue(map.containsKey(singleArray), 
                "containsKey should return true for single-element array with null value");
        assertTrue(map.containsKey(new Object[]{"key1"}), 
                "containsKey should return true for equivalent single-element array with null value");
        
        // Verify the value is indeed null
        assertNull(map.get(singleArray), 
                "get should return null for null value");
    }

    @Test
    void testContainsArrayLength2WithNullValue() {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        
        // Put two-element array with null value
        Object[] twoArray = {"key1", "key2"};
        map.put(twoArray, null);
        
        // This should return true - the key exists even though value is null
        assertTrue(map.containsKey(twoArray), 
                "containsKey should return true for two-element array with null value");
        assertTrue(map.containsKey(new Object[]{"key1", "key2"}), 
                "containsKey should return true for equivalent two-element array with null value");
        
        // Verify the value is indeed null
        assertNull(map.get(twoArray), 
                "get should return null for null value");
    }

    @Test
    void testContainsArrayLength3WithNullValue() {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        
        // Put three-element array with null value
        Object[] threeArray = {"key1", "key2", "key3"};
        map.put(threeArray, null);
        
        // This should return true - the key exists even though value is null
        assertTrue(map.containsKey(threeArray), 
                "containsKey should return true for three-element array with null value");
        assertTrue(map.containsKey(new Object[]{"key1", "key2", "key3"}), 
                "containsKey should return true for equivalent three-element array with null value");
        
        // Verify the value is indeed null
        assertNull(map.get(threeArray), 
                "get should return null for null value");
    }

    @Test
    void testContainsArrayLength4WithNullValue() {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        
        // Put four-element array with null value
        Object[] fourArray = {"key1", "key2", "key3", "key4"};
        map.put(fourArray, null);
        
        // This should return true - the key exists even though value is null
        assertTrue(map.containsKey(fourArray), 
                "containsKey should return true for four-element array with null value");
        assertTrue(map.containsKey(new Object[]{"key1", "key2", "key3", "key4"}), 
                "containsKey should return true for equivalent four-element array with null value");
        
        // Verify the value is indeed null
        assertNull(map.get(fourArray), 
                "get should return null for null value");
    }

    @Test
    void testContainsArrayLength5WithNullValue() {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        
        // Put five-element array with null value
        Object[] fiveArray = {"key1", "key2", "key3", "key4", "key5"};
        map.put(fiveArray, null);
        
        // This should return true - the key exists even though value is null
        assertTrue(map.containsKey(fiveArray), 
                "containsKey should return true for five-element array with null value");
        assertTrue(map.containsKey(new Object[]{"key1", "key2", "key3", "key4", "key5"}), 
                "containsKey should return true for equivalent five-element array with null value");
        
        // Verify the value is indeed null
        assertNull(map.get(fiveArray), 
                "get should return null for null value");
    }

    @Test
    void testContainsCollectionLength1WithNullValue() {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        
        // Put single-element collection with null value
        List<Object> singleList = Arrays.asList("key1");
        map.put(singleList, null);
        
        // This should return true - the key exists even though value is null
        assertTrue(map.containsKey(singleList), 
                "containsKey should return true for single-element collection with null value");
        assertTrue(map.containsKey(Arrays.asList("key1")), 
                "containsKey should return true for equivalent single-element collection with null value");
        
        // Verify the value is indeed null
        assertNull(map.get(singleList), 
                "get should return null for null value");
    }

    @Test
    void testContainsCollectionLength2WithNullValue() {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        
        // Put two-element collection with null value
        List<Object> twoList = Arrays.asList("key1", "key2");
        map.put(twoList, null);
        
        // This should return true - the key exists even though value is null
        assertTrue(map.containsKey(twoList), 
                "containsKey should return true for two-element collection with null value");
        assertTrue(map.containsKey(Arrays.asList("key1", "key2")), 
                "containsKey should return true for equivalent two-element collection with null value");
        
        // Verify the value is indeed null
        assertNull(map.get(twoList), 
                "get should return null for null value");
    }

    @Test
    void testContainsCollectionLength3WithNullValue() {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        
        // Put three-element collection with null value
        List<Object> threeList = Arrays.asList("key1", "key2", "key3");
        map.put(threeList, null);
        
        // This should return true - the key exists even though value is null
        assertTrue(map.containsKey(threeList), 
                "containsKey should return true for three-element collection with null value");
        assertTrue(map.containsKey(Arrays.asList("key1", "key2", "key3")), 
                "containsKey should return true for equivalent three-element collection with null value");
        
        // Verify the value is indeed null
        assertNull(map.get(threeList), 
                "get should return null for null value");
    }

    @Test
    void testContainsCollectionLength4WithNullValue() {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        
        // Put four-element collection with null value
        List<Object> fourList = Arrays.asList("key1", "key2", "key3", "key4");
        map.put(fourList, null);
        
        // This should return true - the key exists even though value is null
        assertTrue(map.containsKey(fourList), 
                "containsKey should return true for four-element collection with null value");
        assertTrue(map.containsKey(Arrays.asList("key1", "key2", "key3", "key4")), 
                "containsKey should return true for equivalent four-element collection with null value");
        
        // Verify the value is indeed null
        assertNull(map.get(fourList), 
                "get should return null for null value");
    }

    @Test
    void testContainsCollectionLength5WithNullValue() {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        
        // Put five-element collection with null value
        List<Object> fiveList = Arrays.asList("key1", "key2", "key3", "key4", "key5");
        map.put(fiveList, null);
        
        // This should return true - the key exists even though value is null
        assertTrue(map.containsKey(fiveList), 
                "containsKey should return true for five-element collection with null value");
        assertTrue(map.containsKey(Arrays.asList("key1", "key2", "key3", "key4", "key5")), 
                "containsKey should return true for equivalent five-element collection with null value");
        
        // Verify the value is indeed null
        assertNull(map.get(fiveList), 
                "get should return null for null value");
    }

    @Test
    void testContainsLargeArrayWithNullValue() {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        
        // Put large array (6+ elements) with null value - goes through general path
        Object[] largeArray = {"key1", "key2", "key3", "key4", "key5", "key6", "key7"};
        map.put(largeArray, null);
        
        // This should return true - the key exists even though value is null
        assertTrue(map.containsKey(largeArray), 
                "containsKey should return true for large array with null value");
        assertTrue(map.containsKey(new Object[]{"key1", "key2", "key3", "key4", "key5", "key6", "key7"}), 
                "containsKey should return true for equivalent large array with null value");
        
        // Verify the value is indeed null
        assertNull(map.get(largeArray), 
                "get should return null for null value");
    }

    @Test
    void testContainsLargeCollectionWithNullValue() {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        
        // Put large collection (6+ elements) with null value - goes through general path
        List<Object> largeList = Arrays.asList("key1", "key2", "key3", "key4", "key5", "key6", "key7");
        map.put(largeList, null);
        
        // This should return true - the key exists even though value is null
        assertTrue(map.containsKey(largeList), 
                "containsKey should return true for large collection with null value");
        assertTrue(map.containsKey(Arrays.asList("key1", "key2", "key3", "key4", "key5", "key6", "key7")), 
                "containsKey should return true for equivalent large collection with null value");
        
        // Verify the value is indeed null
        assertNull(map.get(largeList), 
                "get should return null for null value");
    }

    @Test
    void testMixedNullAndNonNullValues() {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        
        // Put various keys with null and non-null values
        map.put("nullKey", null);
        map.put("nonNullKey", "value");
        map.put(Arrays.asList("nullListKey"), null);
        map.put(Arrays.asList("nonNullListKey"), "listValue");
        map.put(new Object[]{"nullArrayKey"}, null);
        map.put(new Object[]{"nonNullArrayKey"}, "arrayValue");
        
        // All keys should be found via containsKey
        assertTrue(map.containsKey("nullKey"));
        assertTrue(map.containsKey("nonNullKey"));
        assertTrue(map.containsKey(Arrays.asList("nullListKey")));
        assertTrue(map.containsKey(Arrays.asList("nonNullListKey")));
        assertTrue(map.containsKey(new Object[]{"nullArrayKey"}));
        assertTrue(map.containsKey(new Object[]{"nonNullArrayKey"}));
        
        // Verify the values are correct
        assertNull(map.get("nullKey"));
        assertEquals("value", map.get("nonNullKey"));
        assertNull(map.get(Arrays.asList("nullListKey")));
        assertEquals("listValue", map.get(Arrays.asList("nonNullListKey")));
        assertNull(map.get(new Object[]{"nullArrayKey"}));
        assertEquals("arrayValue", map.get(new Object[]{"nonNullArrayKey"}));
        
        assertEquals(6, map.size());
    }
}