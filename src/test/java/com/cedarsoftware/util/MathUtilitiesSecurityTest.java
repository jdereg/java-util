package com.cedarsoftware.util;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Security tests for MathUtilities class.
 * Tests configurable security controls to prevent resource exhaustion attacks.
 * 
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
public class MathUtilitiesSecurityTest {
    
    private String originalSecurityEnabled;
    private String originalMaxArraySize;
    private String originalMaxStringLength;
    private String originalMaxPermutationSize;
    
    @BeforeEach
    void setUp() {
        // Save original system property values
        originalSecurityEnabled = System.getProperty("mathutilities.security.enabled");
        originalMaxArraySize = System.getProperty("mathutilities.max.array.size");
        originalMaxStringLength = System.getProperty("mathutilities.max.string.length");
        originalMaxPermutationSize = System.getProperty("mathutilities.max.permutation.size");
    }
    
    @AfterEach
    void tearDown() {
        // Restore original system property values
        restoreProperty("mathutilities.security.enabled", originalSecurityEnabled);
        restoreProperty("mathutilities.max.array.size", originalMaxArraySize);
        restoreProperty("mathutilities.max.string.length", originalMaxStringLength);
        restoreProperty("mathutilities.max.permutation.size", originalMaxPermutationSize);
    }
    
    private void restoreProperty(String key, String value) {
        if (value == null) {
            System.clearProperty(key);
        } else {
            System.setProperty(key, value);
        }
    }
    
    @Test
    void testSecurityDisabledByDefault() {
        // Clear all security properties to test default behavior
        System.clearProperty("mathutilities.security.enabled");
        System.clearProperty("mathutilities.max.array.size");
        System.clearProperty("mathutilities.max.string.length");
        System.clearProperty("mathutilities.max.permutation.size");
        
        // Should work without throwing SecurityException when security disabled
        assertDoesNotThrow(() -> {
            // Create large arrays that would exceed default limits if security was enabled
            long[] largeArray = new long[200000]; // Larger than 100K default
            for (int i = 0; i < largeArray.length; i++) {
                largeArray[i] = i;
            }
            long min = MathUtilities.minimum(largeArray);
            assertEquals(0, min);
        }, "MathUtilities should work without security limits by default");
        
        assertDoesNotThrow(() -> {
            // Create large string that would exceed default limits if security was enabled
            StringBuilder largeNumber = new StringBuilder();
            for (int i = 0; i < 200000; i++) { // Larger than 100K default
                largeNumber.append("1");
            }
            Number result = MathUtilities.parseToMinimalNumericType(largeNumber.toString());
            assertNotNull(result);
        }, "MathUtilities should work without security limits by default");
    }
    
    @Test
    void testArraySizeLimiting() {
        // Enable security with array size limit
        System.setProperty("mathutilities.security.enabled", "true");
        System.setProperty("mathutilities.max.array.size", "100");
        
        // Create array that exceeds the limit
        long[] largeArray = new long[150]; // 150 > 100 limit
        for (int i = 0; i < largeArray.length; i++) {
            largeArray[i] = i;
        }
        
        // Should throw SecurityException for oversized array
        SecurityException e = assertThrows(SecurityException.class, () -> {
            MathUtilities.minimum(largeArray);
        }, "Should throw SecurityException when array size exceeded");
        
        assertTrue(e.getMessage().contains("Array size exceeds maximum allowed"));
        assertTrue(e.getMessage().contains("100"));
    }
    
    @Test
    void testStringLengthLimiting() {
        // Enable security with string length limit
        System.setProperty("mathutilities.security.enabled", "true");
        System.setProperty("mathutilities.max.string.length", "50");
        
        // Create string that exceeds the limit
        StringBuilder longNumber = new StringBuilder();
        for (int i = 0; i < 60; i++) { // 60 characters > 50 limit
            longNumber.append("1");
        }
        
        // Should throw SecurityException for oversized string
        SecurityException e = assertThrows(SecurityException.class, () -> {
            MathUtilities.parseToMinimalNumericType(longNumber.toString());
        }, "Should throw SecurityException when string length exceeded");
        
        assertTrue(e.getMessage().contains("String length exceeds maximum allowed"));
        assertTrue(e.getMessage().contains("50"));
    }
    
    @Test
    void testPermutationSizeLimiting() {
        // Enable security with permutation size limit
        System.setProperty("mathutilities.security.enabled", "true");
        System.setProperty("mathutilities.max.permutation.size", "5");
        
        // Create list that exceeds the limit
        List<Integer> largeList = new ArrayList<>();
        for (int i = 0; i < 7; i++) { // 7 elements > 5 limit
            largeList.add(i);
        }
        
        // Should throw SecurityException for oversized list
        SecurityException e = assertThrows(SecurityException.class, () -> {
            MathUtilities.nextPermutation(largeList);
        }, "Should throw SecurityException when permutation size exceeded");
        
        assertTrue(e.getMessage().contains("List size exceeds maximum allowed for permutation"));
        assertTrue(e.getMessage().contains("5"));
    }
    
    @Test
    void testSecurityEnabledWithDefaultLimits() {
        // Enable security without specifying custom limits (should use defaults)
        System.setProperty("mathutilities.security.enabled", "true");
        System.clearProperty("mathutilities.max.array.size");
        System.clearProperty("mathutilities.max.string.length");
        System.clearProperty("mathutilities.max.permutation.size");
        
        // Test reasonable sizes that should work with default limits
        long[] reasonableArray = {1, 2, 3, 4, 5}; // Well under 100K default
        String reasonableNumber = "12345"; // Well under 100K default
        List<Integer> reasonableList = Arrays.asList(1, 2, 3); // Well under 10 default
        
        // Should work fine with reasonable sizes
        assertDoesNotThrow(() -> {
            long min = MathUtilities.minimum(reasonableArray);
            assertEquals(1, min);
        }, "Reasonable arrays should work with default limits");
        
        assertDoesNotThrow(() -> {
            Number result = MathUtilities.parseToMinimalNumericType(reasonableNumber);
            assertEquals(12345L, result);
        }, "Reasonable strings should work with default limits");
        
        assertDoesNotThrow(() -> {
            List<Integer> testList = new ArrayList<>(reasonableList);
            boolean hasNext = MathUtilities.nextPermutation(testList);
            assertTrue(hasNext);
        }, "Reasonable lists should work with default limits");
    }
    
    @Test
    void testZeroLimitsDisableChecks() {
        // Enable security but set limits to 0 (disabled)
        System.setProperty("mathutilities.security.enabled", "true");
        System.setProperty("mathutilities.max.array.size", "0");
        System.setProperty("mathutilities.max.string.length", "0");
        System.setProperty("mathutilities.max.permutation.size", "0");
        
        // Create large structures that would normally trigger limits
        long[] largeArray = new long[1000];
        for (int i = 0; i < largeArray.length; i++) {
            largeArray[i] = i;
        }
        
        StringBuilder largeNumber = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            largeNumber.append("1");
        }
        
        List<Integer> largeList = new ArrayList<>();
        for (int i = 0; i < 15; i++) {
            largeList.add(i);
        }
        
        // Should NOT throw SecurityException when limits set to 0
        assertDoesNotThrow(() -> {
            MathUtilities.minimum(largeArray);
        }, "Should not enforce limits when set to 0");
        
        assertDoesNotThrow(() -> {
            MathUtilities.parseToMinimalNumericType(largeNumber.toString());
        }, "Should not enforce limits when set to 0");
        
        assertDoesNotThrow(() -> {
            MathUtilities.nextPermutation(largeList);
        }, "Should not enforce limits when set to 0");
    }
    
    @Test
    void testNegativeLimitsDisableChecks() {
        // Enable security but set limits to negative values (disabled)
        System.setProperty("mathutilities.security.enabled", "true");
        System.setProperty("mathutilities.max.array.size", "-1");
        System.setProperty("mathutilities.max.string.length", "-5");
        System.setProperty("mathutilities.max.permutation.size", "-10");
        
        // Create structures that would trigger positive limits
        long[] array = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10};
        String number = "123456789012345";
        List<Integer> list = Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12);
        
        // Should NOT throw SecurityException when limits are negative
        assertDoesNotThrow(() -> {
            MathUtilities.minimum(array);
        }, "Should not enforce negative limits");
        
        assertDoesNotThrow(() -> {
            MathUtilities.parseToMinimalNumericType(number);
        }, "Should not enforce negative limits");
        
        assertDoesNotThrow(() -> {
            List<Integer> testList = new ArrayList<>(list);
            MathUtilities.nextPermutation(testList);
        }, "Should not enforce negative limits");
    }
    
    @Test
    void testInvalidLimitValuesUseDefaults() {
        // Enable security with invalid limit values
        System.setProperty("mathutilities.security.enabled", "true");
        System.setProperty("mathutilities.max.array.size", "invalid");
        System.setProperty("mathutilities.max.string.length", "not_a_number");
        System.setProperty("mathutilities.max.permutation.size", "");
        
        // Should use default values when parsing fails
        // Test with structures that are small and should work with defaults
        long[] smallArray = {1, 2, 3, 4, 5};
        String smallNumber = "12345";
        List<Integer> smallList = Arrays.asList(1, 2, 3);
        
        // Should work normally (using default values when invalid limits provided)
        assertDoesNotThrow(() -> {
            long min = MathUtilities.minimum(smallArray);
            assertEquals(1, min);
        }, "Should use default values when invalid property values provided");
        
        assertDoesNotThrow(() -> {
            Number result = MathUtilities.parseToMinimalNumericType(smallNumber);
            assertEquals(12345L, result);
        }, "Should use default values when invalid property values provided");
        
        assertDoesNotThrow(() -> {
            List<Integer> testList = new ArrayList<>(smallList);
            boolean hasNext = MathUtilities.nextPermutation(testList);
            assertTrue(hasNext);
        }, "Should use default values when invalid property values provided");
    }
    
    @Test
    void testSecurityDisabledIgnoresLimits() {
        // Disable security but set strict limits
        System.setProperty("mathutilities.security.enabled", "false");
        System.setProperty("mathutilities.max.array.size", "3");
        System.setProperty("mathutilities.max.string.length", "5");
        System.setProperty("mathutilities.max.permutation.size", "2");
        
        // Create structures that would exceed the limits if security was enabled
        long[] largeArray = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10}; // 10 elements > 3 limit
        String longNumber = "1234567890"; // 10 characters > 5 limit
        List<Integer> largeList = Arrays.asList(1, 2, 3, 4, 5); // 5 elements > 2 limit
        
        // Should work normally when security is disabled regardless of limit settings
        assertDoesNotThrow(() -> {
            MathUtilities.minimum(largeArray);
        }, "Should ignore limits when security disabled");
        
        assertDoesNotThrow(() -> {
            MathUtilities.parseToMinimalNumericType(longNumber);
        }, "Should ignore limits when security disabled");
        
        assertDoesNotThrow(() -> {
            List<Integer> testList = new ArrayList<>(largeList);
            MathUtilities.nextPermutation(testList);
        }, "Should ignore limits when security disabled");
    }
    
    @Test
    void testAllNumericTypesWithArrayLimits() {
        // Test that array size limits work for all numeric types
        System.setProperty("mathutilities.security.enabled", "true");
        System.setProperty("mathutilities.max.array.size", "5");
        
        long[] longArray = {1, 2, 3, 4, 5, 6, 7}; // 7 > 5 limit
        double[] doubleArray = {1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0}; // 7 > 5 limit
        BigInteger[] bigIntArray = {
            BigInteger.valueOf(1), BigInteger.valueOf(2), BigInteger.valueOf(3),
            BigInteger.valueOf(4), BigInteger.valueOf(5), BigInteger.valueOf(6), BigInteger.valueOf(7)
        }; // 7 > 5 limit
        BigDecimal[] bigDecArray = {
            BigDecimal.valueOf(1.0), BigDecimal.valueOf(2.0), BigDecimal.valueOf(3.0),
            BigDecimal.valueOf(4.0), BigDecimal.valueOf(5.0), BigDecimal.valueOf(6.0), BigDecimal.valueOf(7.0)
        }; // 7 > 5 limit
        
        // All should throw SecurityException
        assertThrows(SecurityException.class, () -> MathUtilities.minimum(longArray));
        assertThrows(SecurityException.class, () -> MathUtilities.maximum(longArray));
        assertThrows(SecurityException.class, () -> MathUtilities.minimum(doubleArray));
        assertThrows(SecurityException.class, () -> MathUtilities.maximum(doubleArray));
        assertThrows(SecurityException.class, () -> MathUtilities.minimum(bigIntArray));
        assertThrows(SecurityException.class, () -> MathUtilities.maximum(bigIntArray));
        assertThrows(SecurityException.class, () -> MathUtilities.minimum(bigDecArray));
        assertThrows(SecurityException.class, () -> MathUtilities.maximum(bigDecArray));
    }
    
    @Test
    void testSmallStructuresWithinLimits() {
        // Enable security with reasonable limits
        System.setProperty("mathutilities.security.enabled", "true");
        System.setProperty("mathutilities.max.array.size", "1000");
        System.setProperty("mathutilities.max.string.length", "100");
        System.setProperty("mathutilities.max.permutation.size", "8");
        
        // Create small structures that are well within limits
        long[] smallArray = {1, 2, 3, 4, 5}; // 5 < 1000 limit
        String smallNumber = "12345"; // 5 characters < 100 limit
        List<Integer> smallList = Arrays.asList(1, 2, 3, 4); // 4 elements < 8 limit
        
        // Should work normally for structures within limits
        assertDoesNotThrow(() -> {
            long min = MathUtilities.minimum(smallArray);
            assertEquals(1, min);
            long max = MathUtilities.maximum(smallArray);
            assertEquals(5, max);
        }, "Should work normally for structures within limits");
        
        assertDoesNotThrow(() -> {
            Number result = MathUtilities.parseToMinimalNumericType(smallNumber);
            assertEquals(12345L, result);
        }, "Should work normally for structures within limits");
        
        assertDoesNotThrow(() -> {
            List<Integer> testList = new ArrayList<>(smallList);
            boolean hasNext = MathUtilities.nextPermutation(testList);
            assertTrue(hasNext);
        }, "Should work normally for structures within limits");
    }
    
    @Test
    void testBackwardCompatibilityPreserved() {
        // Clear all security properties to test default behavior
        System.clearProperty("mathutilities.security.enabled");
        System.clearProperty("mathutilities.max.array.size");
        System.clearProperty("mathutilities.max.string.length");
        System.clearProperty("mathutilities.max.permutation.size");
        
        // Test normal functionality still works
        long[] testArray = {5, 2, 8, 1, 9};
        String testNumber = "12345";
        List<Integer> testList = new ArrayList<>(Arrays.asList(1, 2, 3));
        
        // Should work normally without any security restrictions
        assertDoesNotThrow(() -> {
            long min = MathUtilities.minimum(testArray);
            assertEquals(1, min);
            long max = MathUtilities.maximum(testArray);
            assertEquals(9, max);
            
            Number result = MathUtilities.parseToMinimalNumericType(testNumber);
            assertEquals(12345L, result);
            
            boolean hasNext = MathUtilities.nextPermutation(testList);
            assertTrue(hasNext);
            assertEquals(Arrays.asList(1, 3, 2), testList);
        }, "Should preserve backward compatibility");
    }
}