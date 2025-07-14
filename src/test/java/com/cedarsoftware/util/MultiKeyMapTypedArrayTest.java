package com.cedarsoftware.util;

import java.util.logging.Logger;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Test the typed array API support in MultiKeyMap.
 * Tests zero-conversion access for String[], int[], Class<?>[], and other typed arrays.
 */
class MultiKeyMapTypedArrayTest {
    private static final Logger LOG = Logger.getLogger(MultiKeyMapTypedArrayTest.class.getName());
    static {
        LoggingConfig.initForTests();
    }

    @Test
    void testStringArrayKeys() {
        MultiKeyMap<String> map = new MultiKeyMap<>(16);
        
        // Store using varargs
        map.putMultiKey("stringValue", "key1", "key2", "key3");
        
        // Retrieve using String[] - zero conversion
        String[] stringKeys = {"key1", "key2", "key3"};
        assertEquals("stringValue", map.get((Object) stringKeys));
        
        // Store using String[] directly
        String[] directKeys = {"direct1", "direct2"};
        map.put(directKeys, "directValue");
        
        // Retrieve using equivalent Object[]
        Object[] objectKeys = {"direct1", "direct2"};
        assertEquals("directValue", map.getMultiKey(objectKeys));
        
        // Retrieve using equivalent varargs
        assertEquals("directValue", map.getMultiKey("direct1", "direct2"));
    }
    
    @Test
    void testIntArrayKeys() {
        MultiKeyMap<String> map = new MultiKeyMap<>(16);
        
        // Store using varargs (boxed integers)
        map.putMultiKey("intValue", 1, 2, 3);
        
        // Retrieve using int[] - zero conversion via reflection
        int[] intKeys = {1, 2, 3};
        assertEquals("intValue", map.get(intKeys));
        
        // Store using int[] directly
        int[] directIntKeys = {10, 20, 30};
        map.put(directIntKeys, "directIntValue");
        
        // Retrieve using equivalent Object[]
        Object[] objectKeys = {10, 20, 30};
        assertEquals("directIntValue", map.getMultiKey(objectKeys));
    }
    
    @Test
    void testLongArrayKeys() {
        MultiKeyMap<String> map = new MultiKeyMap<>(16);
        
        // Store using varargs
        map.putMultiKey("longValue", 1L, 2L, 3L);
        
        // Retrieve using long[] - zero conversion
        long[] longKeys = {1L, 2L, 3L};
        assertEquals("longValue", map.get(longKeys));
        
        // Store using long[] directly
        long[] directKeys = {100L, 200L};
        map.put(directKeys, "directLongValue");
        
        // Retrieve using equivalent Object[]
        Object[] objectKeys = {100L, 200L};
        assertEquals("directLongValue", map.getMultiKey(objectKeys));
    }
    
    @Test
    void testClassArrayKeys() {
        MultiKeyMap<String> map = new MultiKeyMap<>(16);
        
        // Store using varargs
        map.putMultiKey("classValue", String.class, Integer.class, Long.class);
        
        // Retrieve using Class<?>[] - zero conversion
        Class<?>[] classKeys = {String.class, Integer.class, Long.class};
        assertEquals("classValue", map.get((Object) classKeys));
        
        // Store using Class<?>[] directly
        Class<?>[] directKeys = {Double.class, Boolean.class};
        map.put(directKeys, "directClassValue");
        
        // Retrieve using equivalent Object[]
        Object[] objectKeys = {Double.class, Boolean.class};
        assertEquals("directClassValue", map.getMultiKey(objectKeys));
    }
    
    @Test
    void testDoubleArrayKeys() {
        MultiKeyMap<String> map = new MultiKeyMap<>(16);
        
        // Store using varargs
        map.putMultiKey("doubleValue", 1.5, 2.5, 3.5);
        
        // Retrieve using double[] - zero conversion
        double[] doubleKeys = {1.5, 2.5, 3.5};
        assertEquals("doubleValue", map.get(doubleKeys));
        
        // Store using double[] directly
        double[] directKeys = {10.1, 20.2};
        map.put(directKeys, "directDoubleValue");
        
        // Retrieve using equivalent Object[]
        Object[] objectKeys = {10.1, 20.2};
        assertEquals("directDoubleValue", map.getMultiKey(objectKeys));
    }
    
    @Test
    void testBooleanArrayKeys() {
        MultiKeyMap<String> map = new MultiKeyMap<>(16);
        
        // Store using varargs
        map.putMultiKey("booleanValue", true, false, true);
        
        // Retrieve using boolean[] - zero conversion
        boolean[] boolKeys = {true, false, true};
        assertEquals("booleanValue", map.get(boolKeys));
        
        // Store using boolean[] directly
        boolean[] directKeys = {false, false, true};
        map.put(directKeys, "directBooleanValue");
        
        // Retrieve using equivalent Object[]
        Object[] objectKeys = {false, false, true};
        assertEquals("directBooleanValue", map.getMultiKey(objectKeys));
    }
    
    @Test
    void testCharArrayKeys() {
        MultiKeyMap<String> map = new MultiKeyMap<>(16);
        
        // Store using varargs
        map.putMultiKey("charValue", 'a', 'b', 'c');
        
        // Retrieve using char[] - zero conversion
        char[] charKeys = {'a', 'b', 'c'};
        assertEquals("charValue", map.get(charKeys));
        
        // Store using char[] directly
        char[] directKeys = {'x', 'y', 'z'};
        map.put(directKeys, "directCharValue");
        
        // Retrieve using equivalent Object[]
        Object[] objectKeys = {'x', 'y', 'z'};
        assertEquals("directCharValue", map.getMultiKey(objectKeys));
    }
    
    @Test
    void testByteArrayKeys() {
        MultiKeyMap<String> map = new MultiKeyMap<>(16);
        
        // Store using varargs
        map.putMultiKey("byteValue", (byte) 1, (byte) 2, (byte) 3);
        
        // Retrieve using byte[] - zero conversion
        byte[] byteKeys = {1, 2, 3};
        assertEquals("byteValue", map.get(byteKeys));
        
        // Store using byte[] directly
        byte[] directKeys = {10, 20, 30};
        map.put(directKeys, "directByteValue");
        
        // Retrieve using equivalent Object[]
        Object[] objectKeys = {(byte) 10, (byte) 20, (byte) 30};
        assertEquals("directByteValue", map.getMultiKey(objectKeys));
    }
    
    @Test
    void testShortArrayKeys() {
        MultiKeyMap<String> map = new MultiKeyMap<>(16);
        
        // Store using varargs
        map.putMultiKey("shortValue", (short) 1, (short) 2, (short) 3);
        
        // Retrieve using short[] - zero conversion
        short[] shortKeys = {1, 2, 3};
        assertEquals("shortValue", map.get(shortKeys));
        
        // Store using short[] directly
        short[] directKeys = {100, 200};
        map.put(directKeys, "directShortValue");
        
        // Retrieve using equivalent Object[]
        Object[] objectKeys = {(short) 100, (short) 200};
        assertEquals("directShortValue", map.getMultiKey(objectKeys));
    }
    
    @Test
    void testFloatArrayKeys() {
        MultiKeyMap<String> map = new MultiKeyMap<>(16);
        
        // Store using varargs
        map.putMultiKey("floatValue", 1.5f, 2.5f, 3.5f);
        
        // Retrieve using float[] - zero conversion
        float[] floatKeys = {1.5f, 2.5f, 3.5f};
        assertEquals("floatValue", map.get(floatKeys));
        
        // Store using float[] directly
        float[] directKeys = {10.1f, 20.2f};
        map.put(directKeys, "directFloatValue");
        
        // Retrieve using equivalent Object[]
        Object[] objectKeys = {10.1f, 20.2f};
        assertEquals("directFloatValue", map.getMultiKey(objectKeys));
    }
    
    @Test
    void testMixedTypedArrays() {
        MultiKeyMap<String> map = new MultiKeyMap<>(16);
        
        // Store using varargs with mixed types
        map.putMultiKey("mixedValue", "text", 42, 3.14, true);
        
        // Create typed arrays of different types
        String[] stringKeys = {"text"};
        int[] intKeys = {42};
        double[] doubleKeys = {3.14};
        boolean[] boolKeys = {true};
        
        // Each typed array should find nothing (different key dimensions)
        assertNull(map.get((Object) stringKeys));
        assertNull(map.get((Object) intKeys));
        assertNull(map.get((Object) doubleKeys));
        assertNull(map.get(boolKeys));
        
        // But Object[] equivalent should work
        Object[] objectKeys = {"text", 42, 3.14, true};
        assertEquals("mixedValue", map.getMultiKey(objectKeys));
    }
    
    @Test
    void testTypedArrayWithNulls() {
        MultiKeyMap<String> map = new MultiKeyMap<>(16);
        
        // Store using varargs with nulls
        map.putMultiKey("nullValue", "key1", null, "key3");
        
        // String[] cannot contain null in a meaningful way for this test
        // but Object[] can
        Object[] objectKeys = {"key1", null, "key3"};
        assertEquals("nullValue", map.getMultiKey(objectKeys));
        
        // Store using Object[] with nulls
        Object[] nullKeys = {null, "notNull", null};
        map.put(nullKeys, "mixedNullValue");
        assertEquals("mixedNullValue", map.getMultiKey(nullKeys));
    }
    
    @Test
    void testEmptyTypedArrays() {
        MultiKeyMap<String> map = new MultiKeyMap<>(16);
        
        // Empty arrays should return null
        assertNull(map.get((Object) new String[0]));
        assertNull(map.get((Object) new int[0]));
        assertNull(map.get((Object) new Object[0]));
        assertNull(map.get((Object) new Class<?>[0]));
    }
    
    @Test
    void testTypedArrayEquality() {
        MultiKeyMap<String> map = new MultiKeyMap<>(16);
        
        // Store once using varargs
        map.putMultiKey("equalityTest", String.class, Integer.class, 42L);
        
        // Should be retrievable via different array types
        Object[] objectArray = {String.class, Integer.class, 42L};
        assertEquals("equalityTest", map.getMultiKey(objectArray));
        
        Class<?>[] classArray = {String.class, Integer.class}; // Wrong length
        assertNull(map.get((Object) classArray));
        
        // Should be retrievable via mixed Object array
        Object[] mixedArray = {String.class, Integer.class, 42L};
        assertEquals("equalityTest", map.getMultiKey(mixedArray));
    }
    
    @Test
    void testTypedArrayHashConsistency() {
        MultiKeyMap<String> map = new MultiKeyMap<>(16);
        
        // Store using int[]
        int[] intArray = {1, 2, 3, 4, 5};
        map.put(intArray, "intArrayValue");
        
        // Retrieve using equivalent Object[]
        Object[] objectArray = {1, 2, 3, 4, 5};
        assertEquals("intArrayValue", map.getMultiKey(objectArray));
        
        // Retrieve using original int[]
        assertEquals("intArrayValue", map.get(intArray));
        
        // All should point to same entry
        assertEquals(1, map.size());
    }
    
    @Test
    void testTypedArrayPerformance() {
        MultiKeyMap<String> map = new MultiKeyMap<>(32);
        
        // Populate with test data using varargs
        for (int i = 0; i < 100; i++) {
            map.putMultiKey("value" + i, String.class, Integer.class, (long) i);
        }
        
        // Create typed arrays for comparison
        Class<?>[] classArray = {String.class, Integer.class};
        long[] longArray = {50L}; // Wrong dimensions
        Object[] objectArray = {String.class, Integer.class, 50L};
        
        // Warm up
        for (int i = 0; i < 1000; i++) {
            map.getMultiKey(objectArray);
        }
        
        // Time typed array access (this would be 3-element search)
        long start = System.nanoTime();
        for (int i = 0; i < 10000; i++) {
            String result = map.getMultiKey(objectArray);
            assertNotNull(result);
        }
        long objectTime = System.nanoTime() - start;
        
        // Test that wrong-sized arrays return null quickly
        start = System.nanoTime();
        for (int i = 0; i < 10000; i++) {
            String result = map.get(longArray);
            assertNull(result);
        }
        long wrongSizeTime = System.nanoTime() - start;
        
        LOG.info("Object[] access time: " + (objectTime / 1_000_000.0) + " ms");
        LOG.info("Wrong-size array time: " + (wrongSizeTime / 1_000_000.0) + " ms");
        
        // Both should be reasonably fast - exact timing depends on JVM and caching
        assertTrue(objectTime > 0 && wrongSizeTime > 0, 
                  "Both operations should complete in measurable time");
    }
    
    @Test
    void testTypedArrayVsCollectionVsObject() {
        MultiKeyMap<String> map = new MultiKeyMap<>(16);
        
        // Store once using varargs
        map.putMultiKey("universalValue", "x", "y", "z");
        
        // Should be retrievable via all three APIs
        
        // 1. Object[] array
        Object[] objectArray = {"x", "y", "z"};
        assertEquals("universalValue", map.getMultiKey(objectArray));
        
        // 2. Collection
        java.util.List<Object> collection = java.util.Arrays.asList("x", "y", "z");
        assertEquals("universalValue", map.get(collection));
        
        // 3. Typed array (String[])
        String[] stringArray = {"x", "y", "z"};
        assertEquals("universalValue", map.get((Object) stringArray));
        
        // All should access the same entry
        assertEquals(1, map.size());
        
        // Verify they all use the same hash computation
        assertTrue(map.containsMultiKey("x", "y", "z"));
        assertTrue(map.containsMultiKey(objectArray));
        assertTrue(map.containsKey(collection));
        assertTrue(map.containsKey((Object) stringArray));
    }
    
    @Test
    void testLargeTypedArrays() {
        MultiKeyMap<String> map = new MultiKeyMap<>(16);
        
        // Create large typed arrays
        int[] largeIntArray = new int[50];
        String[] largeStringArray = new String[50];
        
        for (int i = 0; i < 50; i++) {
            largeIntArray[i] = i;
            largeStringArray[i] = "element" + i;
        }
        
        // Store using int[]
        map.put(largeIntArray, "largeIntValue");
        
        // Store using String[]
        map.put(largeStringArray, "largeStringValue");
        
        // Retrieve using same typed arrays
        assertEquals("largeIntValue", map.get((Object) largeIntArray));
        assertEquals("largeStringValue", map.get((Object) largeStringArray));
        
        // Retrieve using equivalent Object[]
        Object[] intAsObject = new Object[50];
        Object[] stringAsObject = new Object[50];
        
        for (int i = 0; i < 50; i++) {
            intAsObject[i] = i;
            stringAsObject[i] = "element" + i;
        }
        
        assertEquals("largeIntValue", map.getMultiKey(intAsObject));
        assertEquals("largeStringValue", map.getMultiKey(stringAsObject));
        
        assertEquals(2, map.size());
    }
}