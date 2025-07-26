package com.cedarsoftware.util;

import org.junit.jupiter.api.Test;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test to verify that generic array processing (reflection-based fallback) works correctly
 * for uncommon array types that don't have specific optimized handlers.
 * This covers the process1DGenericArray method's uncovered lines.
 */
public class MultiKeyMapGenericArrayProcessingTest {
    
    @Test
    void testSingleElementGenericArrayWithNullElement() {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        
        // Single element Float[] array containing null - should return NULL_SENTINEL
        Float[] singleNullFloat = {null};
        map.put(singleNullFloat, "null_float_value");
        
        // Should be accessible via null lookup due to single-element optimization
        assertEquals("null_float_value", map.get((Object) null));
        assertTrue(map.containsKey((Object) null));
        
        // Single element Short[] array containing null - should return NULL_SENTINEL  
        Short[] singleNullShort = {null};
        map.put(singleNullShort, "null_short_value"); // Should overwrite due to null normalization
        
        assertEquals("null_short_value", map.get((Object) null));
        
        // Single element Object[] array containing null
        Object[] singleNullObject = {null};
        map.put(singleNullObject, "null_object_value"); // Should overwrite due to null normalization
        
        assertEquals("null_object_value", map.get((Object) null));
        
        // All null single-element arrays should be equivalent (normalized to null)
        assertEquals(1, map.size());
    }
    
    @Test
    void testSingleElementGenericArrayWithNonNullElement() {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        
        // Single element Float[] array with non-null element - should return the element directly
        Float[] singleFloat = {3.14f};
        map.put(singleFloat, "single_float_value");
        
        // Should be accessible via the unwrapped element due to single-element optimization
        assertEquals("single_float_value", map.get(3.14f));
        assertTrue(map.containsKey(3.14f));
        
        // Single element Short[] array with non-null element
        Short[] singleShort = {(short) 42};
        map.put(singleShort, "single_short_value");
        
        assertEquals("single_short_value", map.get((short) 42));
        assertTrue(map.containsKey((short) 42));
        
        // Single element Character[] array with non-null element
        Character[] singleChar = {'A'};
        map.put(singleChar, "single_char_value");
        
        assertEquals("single_char_value", map.get('A'));
        assertTrue(map.containsKey('A'));
        
        // Single element Byte[] array with non-null element
        Byte[] singleByte = {(byte) 123};
        map.put(singleByte, "single_byte_value");
        
        assertEquals("single_byte_value", map.get((byte) 123));
        assertTrue(map.containsKey((byte) 123));
        
        // Each should be a separate key
        assertEquals(4, map.size());
    }
    
    @Test
    void testMultiDimensionalGenericArrayExpansion() {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        
        // Float[][] - 2D array should trigger expandWithHash path
        Float[][] float2D = {{1.0f, 2.0f}, {3.0f, 4.0f}};
        map.put(float2D, "float_2d_value");
        
        // Should work with equivalent 2D array
        Float[][] lookupFloat2D = {{1.0f, 2.0f}, {3.0f, 4.0f}};
        assertEquals("float_2d_value", map.get(lookupFloat2D));
        assertTrue(map.containsKey(lookupFloat2D));
        
        // Short[][] - 2D array should trigger expandWithHash path
        Short[][] short2D = {{(short) 1, (short) 2}, {(short) 3, (short) 4}};
        map.put(short2D, "short_2d_value");
        
        // Should work with equivalent 2D array
        Short[][] lookupShort2D = {{(short) 1, (short) 2}, {(short) 3, (short) 4}};
        assertEquals("short_2d_value", map.get(lookupShort2D));
        assertTrue(map.containsKey(lookupShort2D));
        
        // Object[] containing arrays/collections should trigger expandWithHash
        Object[] mixedArray = {new int[]{1, 2}, new ArrayList<>(Arrays.asList("a", "b"))};
        map.put(mixedArray, "mixed_array_value");
        
        Object[] lookupMixedArray = {new int[]{1, 2}, new ArrayList<>(Arrays.asList("a", "b"))};
        assertEquals("mixed_array_value", map.get(lookupMixedArray));
        assertTrue(map.containsKey(lookupMixedArray));
        
        assertEquals(3, map.size());
    }
    
    @Test
    void testGenericArrayContainingCollections() {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        
        // Object[] containing collections should not be considered 1D and should expand
        List<String> list1 = Arrays.asList("a", "b");
        List<String> list2 = Arrays.asList("c", "d");
        Object[] arrayWithCollections = {list1, list2, "plain_string"};
        
        map.put(arrayWithCollections, "array_with_collections_value");
        
        // Should work with equivalent structure
        List<String> lookupList1 = Arrays.asList("a", "b");
        List<String> lookupList2 = Arrays.asList("c", "d");
        Object[] lookupArrayWithCollections = {lookupList1, lookupList2, "plain_string"};
        
        assertEquals("array_with_collections_value", map.get(lookupArrayWithCollections));
        assertTrue(map.containsKey(lookupArrayWithCollections));
        
        assertEquals(1, map.size());
    }
    
    @Test
    void testEmptyGenericArrays() {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        
        // Empty generic arrays should all be equivalent (same as empty common arrays)
        Float[] emptyFloat = {};
        Short[] emptyShort = {};
        Character[] emptyChar = {};
        Byte[] emptyByte = {};
        
        map.put(emptyFloat, "empty_float");
        map.put(emptyShort, "empty_short");  // Should overwrite empty_float
        map.put(emptyChar, "empty_char");    // Should overwrite empty_short
        map.put(emptyByte, "empty_byte");    // Should overwrite empty_char
        
        // All empty arrays should be equivalent
        assertEquals("empty_byte", map.get(emptyFloat));
        assertEquals("empty_byte", map.get(emptyShort));
        assertEquals("empty_byte", map.get(emptyChar));
        assertEquals("empty_byte", map.get(emptyByte));
        
        // Should have only 1 key (all empty arrays are equivalent)
        assertEquals(1, map.size());
    }
    
    @Test
    void testGenericArraysMultipleElements1D() {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        
        // Multi-element 1D generic arrays (no nested arrays/collections)
        Float[] floatArray = {1.0f, 2.0f, 3.0f};
        Short[] shortArray = {(short) 10, (short) 20, (short) 30};
        Character[] charArray = {'X', 'Y', 'Z'};
        Byte[] byteArray = {(byte) 100, (byte) 101, (byte) 102};
        
        map.put(floatArray, "float_array_value");
        map.put(shortArray, "short_array_value");
        map.put(charArray, "char_array_value");
        map.put(byteArray, "byte_array_value");
        
        // Should work with equivalent arrays
        assertEquals("float_array_value", map.get(new Float[]{1.0f, 2.0f, 3.0f}));
        assertEquals("short_array_value", map.get(new Short[]{(short) 10, (short) 20, (short) 30}));
        assertEquals("char_array_value", map.get(new Character[]{'X', 'Y', 'Z'}));
        assertEquals("byte_array_value", map.get(new Byte[]{(byte) 100, (byte) 101, (byte) 102}));
        
        assertTrue(map.containsKey(new Float[]{1.0f, 2.0f, 3.0f}));
        assertTrue(map.containsKey(new Short[]{(short) 10, (short) 20, (short) 30}));
        assertTrue(map.containsKey(new Character[]{'X', 'Y', 'Z'}));
        assertTrue(map.containsKey(new Byte[]{(byte) 100, (byte) 101, (byte) 102}));
        
        assertEquals(4, map.size());
    }
    
    @Test
    void testGenericArrayWithNullElements() {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        
        // Generic arrays with null elements mixed with non-null (still 1D)
        Float[] floatWithNulls = {1.0f, null, 3.0f};
        Short[] shortWithNulls = {(short) 10, null, (short) 30};
        Object[] objectWithNulls = {"first", null, "third"};
        
        map.put(floatWithNulls, "float_nulls_value");
        map.put(shortWithNulls, "short_nulls_value");
        map.put(objectWithNulls, "object_nulls_value");
        
        // Should work with equivalent arrays
        assertEquals("float_nulls_value", map.get(new Float[]{1.0f, null, 3.0f}));
        assertEquals("short_nulls_value", map.get(new Short[]{(short) 10, null, (short) 30}));
        assertEquals("object_nulls_value", map.get(new Object[]{"first", null, "third"}));
        
        assertEquals(3, map.size());
    }
}