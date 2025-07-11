package com.cedarsoftware.util;

import org.junit.jupiter.api.Test;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for TrackingMap MultiKeyMap functionality.
 */
class TrackingMapMultiKeyTest {

    @Test
    void testMultiKeyOperationsWithMultiKeyMapBacking() {
        MultiKeyMap<String> multiKeyMap = new MultiKeyMap<>();
        TrackingMap<Object, String> trackingMap = new TrackingMap<>(multiKeyMap);
        
        // Test putMultiKey and getMultiKey
        assertNull(trackingMap.putMultiKey("Value1", "DEPT", "Engineering"));
        assertEquals("Value1", trackingMap.getMultiKey("DEPT", "Engineering"));
        
        // Verify keys were tracked
        assertTrue(trackingMap.keysUsed().contains("DEPT"));
        assertTrue(trackingMap.keysUsed().contains("Engineering"));
        
        // Test containsMultiKey
        assertTrue(trackingMap.containsMultiKey("DEPT", "Engineering"));
        assertFalse(trackingMap.containsMultiKey("DEPT", "Marketing"));
        
        // Test removeMultiKey
        assertEquals("Value1", trackingMap.removeMultiKey("DEPT", "Engineering"));
        assertNull(trackingMap.getMultiKey("DEPT", "Engineering"));
        
        // Verify keys were untracked after removal
        assertFalse(trackingMap.keysUsed().contains("DEPT"));
        assertFalse(trackingMap.keysUsed().contains("Engineering"));
    }

    @Test
    void testMultiKeyOperationsWithRegularMapBacking() {
        TrackingMap<String, String> trackingMap = new TrackingMap<>(new java.util.HashMap<>());
        
        // Multi-key operations should throw IllegalStateException with regular map
        assertThrows(IllegalStateException.class, () -> 
            trackingMap.putMultiKey("Value1", "DEPT", "Engineering"));
        assertThrows(IllegalStateException.class, () -> 
            trackingMap.getMultiKey("DEPT", "Engineering"));
        assertThrows(IllegalStateException.class, () -> 
            trackingMap.removeMultiKey("DEPT", "Engineering"));
        assertThrows(IllegalStateException.class, () -> 
            trackingMap.containsMultiKey("DEPT", "Engineering"));
    }

    @Test
    void testArrayAutoExpansionInRegularMethods() {
        MultiKeyMap<String> multiKeyMap = new MultiKeyMap<>();
        TrackingMap<Object, String> trackingMap = new TrackingMap<>(multiKeyMap);
        
        // Test Object[] array auto-expansion
        Object[] keys1 = {"DEPT", "Engineering"};
        assertEquals(null, trackingMap.put(keys1, "Value1"));
        assertEquals("Value1", trackingMap.get(keys1));
        assertTrue(trackingMap.containsKey(keys1));
        
        // Verify individual key components were tracked
        assertTrue(trackingMap.keysUsed().contains("DEPT"));
        assertTrue(trackingMap.keysUsed().contains("Engineering"));
        
        // Test String[] array auto-expansion
        String[] keys2 = {"DEPT", "Marketing"};
        assertEquals(null, trackingMap.put(keys2, "Value2"));
        assertEquals("Value2", trackingMap.get(keys2));
        
        // Test collection auto-expansion
        assertEquals(null, trackingMap.put(Arrays.asList("DEPT", "Sales"), "Value3"));
        assertEquals("Value3", trackingMap.get(Arrays.asList("DEPT", "Sales")));
        
        // Test removal with arrays
        assertEquals("Value1", trackingMap.remove(keys1));
        assertNull(trackingMap.get(keys1));
        
        // Verify keys were untracked after removal
        assertFalse(trackingMap.keysUsed().contains("DEPT"));
        assertFalse(trackingMap.keysUsed().contains("Engineering"));
    }

    @Test
    void testTypedArraysPassedThrough() {
        MultiKeyMap<String> multiKeyMap = new MultiKeyMap<>();
        TrackingMap<Object, String> trackingMap = new TrackingMap<>(multiKeyMap);
        
        // Test typed arrays (int[], double[], etc.) are passed through
        int[] intKeys = {1, 2, 3};
        assertEquals(null, trackingMap.put(intKeys, "IntValue"));
        assertEquals("IntValue", trackingMap.get(intKeys));
        
        // Verify the array itself is tracked (not individual components)
        assertTrue(trackingMap.keysUsed().contains(intKeys));
    }

    @Test
    void testArrayAutoExpansionWithRegularMapFallback() {
        TrackingMap<Object, String> trackingMap = new TrackingMap<>(new java.util.HashMap<>());
        
        // With regular HashMap backing, arrays should be treated as regular keys
        Object[] keys = {"DEPT", "Engineering"};
        assertEquals(null, trackingMap.put(keys, "Value1"));
        assertEquals("Value1", trackingMap.get(keys));
        
        // The array itself should be tracked, not individual components
        assertTrue(trackingMap.keysUsed().contains(keys));
        assertFalse(trackingMap.keysUsed().contains("DEPT"));
        assertFalse(trackingMap.keysUsed().contains("Engineering"));
    }

    @Test
    void testExpungeUnusedWithMultiKeyTracking() {
        MultiKeyMap<String> multiKeyMap = new MultiKeyMap<>();
        TrackingMap<Object, String> trackingMap = new TrackingMap<>(multiKeyMap);
        
        // Add several multi-key entries
        trackingMap.putMultiKey("Value1", "DEPT", "Engineering");
        trackingMap.putMultiKey("Value2", "DEPT", "Marketing");
        trackingMap.putMultiKey("Value3", "LOCATION", "NY");
        
        // Access only some entries
        trackingMap.getMultiKey("DEPT", "Engineering");
        trackingMap.containsMultiKey("LOCATION", "NY");
        
        // Before expunge
        assertEquals(3, trackingMap.size());
        
        // Expunge unused
        trackingMap.expungeUnused();
        
        // Only accessed entries should remain
        assertEquals(2, trackingMap.size());
        assertEquals("Value1", trackingMap.getMultiKey("DEPT", "Engineering"));
        assertEquals("Value3", trackingMap.getMultiKey("LOCATION", "NY"));
        assertNull(trackingMap.getMultiKey("DEPT", "Marketing"));
    }
}