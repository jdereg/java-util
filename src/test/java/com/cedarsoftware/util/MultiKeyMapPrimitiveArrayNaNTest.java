package com.cedarsoftware.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Test that verifies NaN handling in primitive arrays for MultiKeyMap.
 * In valueBasedEquality mode, NaN should equal NaN.
 * In type-strict mode, NaN should not equal NaN (Java standard behavior).
 */
public class MultiKeyMapPrimitiveArrayNaNTest {

    @Test
    public void testDoubleArrayNaNHandlingValueBasedMode() {
        MultiKeyMap<String> map = MultiKeyMap.<String>builder()
            .valueBasedEquality(true)
            .build();
        
        // Test double arrays with NaN
        double[] key1 = {1.0, Double.NaN, 3.0};
        double[] key2 = {1.0, Double.NaN, 3.0};
        
        map.put(key1, "value1");
        
        // In value-based mode, NaN == NaN, so key2 should find the same entry
        assertEquals("value1", map.get(key2), 
            "Value-based equality should treat NaN == NaN in double arrays");
        assertTrue(map.containsKey(key2),
            "Value-based equality should find key with NaN values");
        
        // Test with multiple NaN values
        double[] key3 = {Double.NaN, Double.NaN, Double.NaN};
        double[] key4 = {Double.NaN, Double.NaN, Double.NaN};
        
        map.put(key3, "all-nan");
        assertEquals("all-nan", map.get(key4),
            "Value-based equality should match arrays with all NaN values");
    }
    
    @Test
    public void testDoubleArrayNaNHandlingTypeStrictMode() {
        MultiKeyMap<String> map = MultiKeyMap.<String>builder()
            .valueBasedEquality(false)
            .build();
        
        // Test double arrays with NaN
        double[] key1 = {1.0, Double.NaN, 3.0};
        double[] key2 = {1.0, Double.NaN, 3.0};
        
        map.put(key1, "value1");
        
        // In type-strict mode with Arrays.equals, NaN values with same bit pattern ARE equal
        // Arrays.equals uses Double.doubleToLongBits for comparison, which treats NaN as equal
        assertEquals("value1", map.get(key2), 
            "Type-strict mode uses Arrays.equals which treats same NaN bit patterns as equal");
        assertTrue(map.containsKey(key2),
            "Type-strict mode with Arrays.equals finds keys with same NaN bit patterns");
    }
    
    @Test
    public void testFloatArrayNaNHandlingValueBasedMode() {
        MultiKeyMap<String> map = MultiKeyMap.<String>builder()
            .valueBasedEquality(true)
            .build();
        
        // Test float arrays with NaN
        float[] key1 = {1.0f, Float.NaN, 3.0f};
        float[] key2 = {1.0f, Float.NaN, 3.0f};
        
        map.put(key1, "value1");
        
        // In value-based mode, NaN == NaN, so key2 should find the same entry
        assertEquals("value1", map.get(key2), 
            "Value-based equality should treat NaN == NaN in float arrays");
        assertTrue(map.containsKey(key2),
            "Value-based equality should find key with NaN values");
        
        // Test with multiple NaN values
        float[] key3 = {Float.NaN, Float.NaN, Float.NaN};
        float[] key4 = {Float.NaN, Float.NaN, Float.NaN};
        
        map.put(key3, "all-nan-float");
        assertEquals("all-nan-float", map.get(key4),
            "Value-based equality should match float arrays with all NaN values");
    }
    
    @Test
    public void testFloatArrayNaNHandlingTypeStrictMode() {
        MultiKeyMap<String> map = MultiKeyMap.<String>builder()
            .valueBasedEquality(false)
            .build();
        
        // Test float arrays with NaN
        float[] key1 = {1.0f, Float.NaN, 3.0f};
        float[] key2 = {1.0f, Float.NaN, 3.0f};
        
        map.put(key1, "value1");
        
        // In type-strict mode with Arrays.equals, NaN values with same bit pattern ARE equal
        // Arrays.equals uses Float.floatToIntBits for comparison, which treats NaN as equal
        assertEquals("value1", map.get(key2), 
            "Type-strict mode uses Arrays.equals which treats same NaN bit patterns as equal");
        assertTrue(map.containsKey(key2),
            "Type-strict mode with Arrays.equals finds keys with same NaN bit patterns");
    }
    
    @Test
    public void testZeroHandlingInPrimitiveArrays() {
        MultiKeyMap<String> map = MultiKeyMap.<String>builder()
            .valueBasedEquality(true)
            .build();
        
        // Test that -0.0 and +0.0 are treated as equal (they compare equal with ==)
        double[] key1 = {1.0, 0.0, 3.0};
        double[] key2 = {1.0, -0.0, 3.0};
        
        map.put(key1, "with-zero");
        
        // Both +0.0 and -0.0 should find the same entry (== returns true for them)
        assertEquals("with-zero", map.get(key2),
            "Value-based equality should treat +0.0 == -0.0 in double arrays");
        
        // Same for float arrays
        float[] fkey1 = {1.0f, 0.0f, 3.0f};
        float[] fkey2 = {1.0f, -0.0f, 3.0f};
        
        map.put(fkey1, "float-zero");
        assertEquals("float-zero", map.get(fkey2),
            "Value-based equality should treat +0.0f == -0.0f in float arrays");
    }
    
    @Test
    public void testMixedNaNAndRegularValues() {
        MultiKeyMap<String> map = MultiKeyMap.<String>builder()
            .valueBasedEquality(true)
            .build();
        
        // Test arrays with mix of NaN and regular values
        double[] key1 = {Double.NaN, 2.0, Double.NaN, 4.0, Double.NaN};
        double[] key2 = {Double.NaN, 2.0, Double.NaN, 4.0, Double.NaN};
        double[] key3 = {Double.NaN, 2.0, Double.NaN, 4.0, 5.0}; // Different last element
        
        map.put(key1, "mixed-nan");
        
        assertEquals("mixed-nan", map.get(key2),
            "Should match arrays with same NaN positions and values");
        assertNull(map.get(key3),
            "Should not match arrays with different non-NaN values");
    }
    
    @Test
    public void testIntArraysUnaffectedByNaNHandling() {
        // Verify that non-floating point arrays still work correctly
        MultiKeyMap<String> map = MultiKeyMap.<String>builder()
            .valueBasedEquality(true)
            .build();
        
        int[] key1 = {1, 2, 3};
        int[] key2 = {1, 2, 3};
        int[] key3 = {1, 2, 4};
        
        map.put(key1, "int-array");
        
        assertEquals("int-array", map.get(key2),
            "Int arrays should still match correctly");
        assertNull(map.get(key3),
            "Different int arrays should not match");
    }
}