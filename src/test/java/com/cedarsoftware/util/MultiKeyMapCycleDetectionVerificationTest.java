package com.cedarsoftware.util;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTimeout;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test to verify cycle detection in expandAndHash method is working correctly.
 * This test specifically targets the visited.containsKey() code path that handles circular references.
 */
public class MultiKeyMapCycleDetectionVerificationTest {
    
    @Test
    void testSimpleArrayCycle() {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        
        // Create a simple self-referencing array
        Object[] circular = new Object[1];
        circular[0] = circular; // Self-reference creates cycle
        
        // This should exercise the cycle detection without causing stack overflow
        assertDoesNotThrow(() -> {
            map.put(circular, "cycle_value");
        });
        
        // Should be able to retrieve the value  
        assertEquals("cycle_value", map.get(circular));
        assertTrue(map.containsKey(circular));
        assertEquals(1, map.size());
    }
    
    // Note: List-based cycles can cause stack overflow in ArrayList.hashCode() 
    // before reaching expandAndHash cycle detection. This is a limitation of 
    // Java's collection hashCode implementations, not our cycle detection.
    
    @Test
    void testTwoElementArrayCycle() {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        
        // Array with one normal element and one self-reference
        Object[] circular = new Object[2];
        circular[0] = "normal_element";
        circular[1] = circular; // Self-reference
        
        assertDoesNotThrow(() -> {
            map.put(circular, "two_element_cycle");
        });
        
        assertEquals("two_element_cycle", map.get(circular));
        assertTrue(map.containsKey(circular));
        assertEquals(1, map.size());
    }
    
    @Test
    void testCycleDetectionEquivalence() {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        
        // Create two different arrays with similar structure but different cycles
        Object[] circular1 = new Object[1];
        circular1[0] = circular1;
        
        Object[] circular2 = new Object[1];
        circular2[0] = circular2;
        
        map.put(circular1, "first_cycle");
        
        // Due to cycle detection, these should NOT be equivalent (different identity hash)
        // Each has its own cycle marker based on System.identityHashCode()
        assertDoesNotThrow(() -> {
            map.put(circular2, "second_cycle");
        });
        
        // They should remain separate because they have different identity hash codes
        assertEquals("first_cycle", map.get(circular1));
        assertEquals("second_cycle", map.get(circular2));
        assertEquals(2, map.size());
    }
    
    @Test
    void testCycleWithFlattenTrue() {
        // Test cycle detection when flattenDimensions = true
        MultiKeyMap<String> map = MultiKeyMap.<String>builder().flattenDimensions(true).build();
        
        Object[] circular = new Object[2];
        circular[0] = "data";
        circular[1] = circular;
        
        assertDoesNotThrow(() -> {
            map.put(circular, "flattened_cycle");
        });
        
        assertEquals("flattened_cycle", map.get(circular));
        assertEquals(1, map.size());
    }
    
    @Test
    void testCycleWithFlattenFalse() {
        // Test cycle detection when flattenDimensions = false
        MultiKeyMap<String> map = MultiKeyMap.<String>builder().flattenDimensions(false).build();
        
        Object[] circular = new Object[2];
        circular[0] = "data";
        circular[1] = circular;
        
        assertDoesNotThrow(() -> {
            map.put(circular, "structured_cycle");
        });
        
        assertEquals("structured_cycle", map.get(circular));
        assertEquals(1, map.size());
    }
    
    @Test
    void testRemoveCyclicKey() {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        
        Object[] circular = new Object[1];
        circular[0] = circular;
        
        map.put(circular, "to_remove");
        assertEquals(1, map.size());
        
        String removed = map.remove(circular);
        assertEquals("to_remove", removed);
        assertEquals(0, map.size());
        
        assertNull(map.get(circular));
        assertFalse(map.containsKey(circular));
    }
    
    @Test
    void testCyclicKeyWithNormalKeys() {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        
        // Add normal keys
        map.put("normal1", "value1");
        map.put(Arrays.asList("normal", "key"), "value2");
        
        // Add cyclic key
        Object[] circular = new Object[2];
        circular[0] = "prefix";
        circular[1] = circular;
        
        assertDoesNotThrow(() -> {
            map.put(circular, "cyclic_value");
        });
        
        // All should be accessible
        assertEquals("value1", map.get("normal1"));
        assertEquals("value2", map.get(Arrays.asList("normal", "key")));
        assertEquals("cyclic_value", map.get(circular));
        
        assertEquals(3, map.size());
    }
    
    @Test
    void testArrayContainingItself() {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        
        // Array that contains itself as an element
        Object[] selfContaining = new Object[3];
        selfContaining[0] = "start";
        selfContaining[1] = selfContaining; // Self-reference
        selfContaining[2] = "end";
        
        assertDoesNotThrow(() -> {
            map.put(selfContaining, "self_containing_value");
        });
        
        assertEquals("self_containing_value", map.get(selfContaining));
        assertEquals(1, map.size());
    }
    
    @Test
    void testCycleDetectionPreventsBadHashComputation() {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        
        // This test verifies that cycle detection prevents problems with hash computation
        // on circular structures
        Object[] circular = new Object[1];
        circular[0] = circular;
        
        // Should not cause infinite recursion or stack overflow during hash computation
        assertTimeout(java.time.Duration.ofSeconds(5), () -> {
            map.put(circular, "timeout_test");
            assertEquals("timeout_test", map.get(circular));
        });
    }
}