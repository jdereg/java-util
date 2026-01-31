package com.cedarsoftware.util;

import java.math.BigDecimal;
import java.math.BigInteger;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Comprehensive test to ensure ALL numeric types can cross-compare with each other
 * when value-based equality is enabled.
 */
public class MultiKeyMapComprehensiveNumericEqualityTest {

    @Test
    void testAllIntegralTypesAreEquivalent() {
        MultiKeyMap<String> map = MultiKeyMap.<String>builder().valueBasedEquality(true).build();
        
        // Put with byte
        map.put(new Object[]{(byte) 42, (byte) 100}, "integral-value");
        
        // Should match with ALL other integral types
        assertEquals("integral-value", map.get(new Object[]{(short) 42, (short) 100}));        // short
        assertEquals("integral-value", map.get(new Object[]{42, 100}));                        // int  
        assertEquals("integral-value", map.get(new Object[]{42L, 100L}));                      // long
        assertEquals("integral-value", map.get(new Object[]{new BigInteger("42"), new BigInteger("100")})); // BigInteger
        
        // And should match with whole-number floating types
        assertEquals("integral-value", map.get(new Object[]{42.0f, 100.0f}));                  // float (whole)
        assertEquals("integral-value", map.get(new Object[]{42.0, 100.0}));                    // double (whole)
        assertEquals("integral-value", map.get(new Object[]{new BigDecimal("42"), new BigDecimal("100")})); // BigDecimal (whole)
    }
    
    @Test
    void testAllFloatingTypesAreEquivalent() {
        MultiKeyMap<String> map = MultiKeyMap.<String>builder().valueBasedEquality(true).build();
        
        // Use values that are exactly representable in both float and double
        // 0.5 and 0.25 are exactly representable in IEEE 754
        map.put(new Object[]{0.5f, 0.25f}, "floating-value");
        
        // Should match with double (since 0.5f == 0.5 exactly)
        assertEquals("floating-value", map.get(new Object[]{0.5, 0.25}));                    // double
        
        // Should match with BigDecimal  
        assertEquals("floating-value", map.get(new Object[]{new BigDecimal("0.5"), new BigDecimal("0.25")})); // BigDecimal
        
        // Should NOT match with integral types (since these have fractional parts)
        assertNull(map.get(new Object[]{0, 0}));                                               // int
        assertNull(map.get(new Object[]{0L, 0L}));                                             // long
        assertNull(map.get(new Object[]{new BigInteger("0"), new BigInteger("0")}));          // BigInteger
    }
    
    @Test
    void testBigDecimalWithAllTypes() {
        MultiKeyMap<String> map = MultiKeyMap.<String>builder().valueBasedEquality(true).build();
        
        // Put with BigDecimal (whole numbers)
        map.put(new Object[]{new BigDecimal("123"), new BigDecimal("456.0")}, "bigdecimal-whole");
        
        // Should match with ALL integral types
        assertEquals("bigdecimal-whole", map.get(new Object[]{(byte) 123, (short) 456}));      // byte, short
        assertEquals("bigdecimal-whole", map.get(new Object[]{123, 456}));                     // int
        assertEquals("bigdecimal-whole", map.get(new Object[]{123L, 456L}));                   // long  
        assertEquals("bigdecimal-whole", map.get(new Object[]{new BigInteger("123"), new BigInteger("456")})); // BigInteger
        
        // Should match with floating types
        assertEquals("bigdecimal-whole", map.get(new Object[]{123.0f, 456.0f}));               // float
        assertEquals("bigdecimal-whole", map.get(new Object[]{123.0, 456.0}));                 // double
        
        // Test BigDecimal with fractional parts (use exactly representable value)
        map.put(new Object[]{new BigDecimal("123.5")}, "bigdecimal-fractional");
        
        // Should match with floating types (123.5 is exactly representable)
        assertEquals("bigdecimal-fractional", map.get(new Object[]{123.5f}));                 // float
        assertEquals("bigdecimal-fractional", map.get(new Object[]{123.5}));                  // double
        
        // Should NOT match with integral types
        assertNull(map.get(new Object[]{123}));                                                // int
        assertNull(map.get(new Object[]{new BigInteger("123")}));                             // BigInteger
    }
    
    @Test
    void testBigIntegerWithAllTypes() {
        MultiKeyMap<String> map = MultiKeyMap.<String>builder().valueBasedEquality(true).build();
        
        // Put with BigInteger 
        map.put(new Object[]{new BigInteger("789"), new BigInteger("1000")}, "biginteger-value");
        
        // Should match with integral types that can represent these values
        assertEquals("biginteger-value", map.get(new Object[]{789, 1000}));                    // int
        assertEquals("biginteger-value", map.get(new Object[]{789L, 1000L}));                  // long
        
        // Should match with whole-number floating types
        assertEquals("biginteger-value", map.get(new Object[]{789.0f, 1000.0f}));              // float (whole)
        assertEquals("biginteger-value", map.get(new Object[]{789.0, 1000.0}));                // double (whole)
        assertEquals("biginteger-value", map.get(new Object[]{new BigDecimal("789"), new BigDecimal("1000")})); // BigDecimal (whole)
        
        // Should NOT match with fractional floating types
        assertNull(map.get(new Object[]{789.1, 1000.0}));                                      // double (fractional)
        assertNull(map.get(new Object[]{new BigDecimal("789.1"), new BigDecimal("1000")}));    // BigDecimal (fractional)
    }
    
    @Test
    void testLargeNumberHandling() {
        MultiKeyMap<String> map = MultiKeyMap.<String>builder().valueBasedEquality(true).build();
        
        // Test with BigInteger that's too large for long
        BigInteger hugeBigInt = new BigInteger("99999999999999999999999999999999999999999999999999999999");
        map.put(new Object[]{hugeBigInt}, "huge-bigint");
        
        // Should match with equivalent BigDecimal
        assertEquals("huge-bigint", map.get(new Object[]{new BigDecimal(hugeBigInt)}));
        
        // Should NOT match with any primitive types (too large)
        assertNull(map.get(new Object[]{Long.MAX_VALUE}));
        assertNull(map.get(new Object[]{Double.MAX_VALUE}));
        
        // Test with BigDecimal that has high precision but is exactly representable in double
        BigDecimal preciseBigDecimal = new BigDecimal("1.25"); // 1.25 is exactly representable
        map.put(new Object[]{preciseBigDecimal}, "precise-bigdecimal");
        
        // Should match with double (since 1.25 is exactly representable)
        assertEquals("precise-bigdecimal", map.get(new Object[]{1.25}));
        assertEquals("precise-bigdecimal", map.get(new Object[]{1.25f}));
    }
    
    @Test
    void testEdgeCaseNumbers() {
        MultiKeyMap<String> map = MultiKeyMap.<String>builder().valueBasedEquality(true).build();
        
        // Test maximum values that fit in different types
        map.put(new Object[]{Byte.MAX_VALUE}, "byte-max");
        assertEquals("byte-max", map.get(new Object[]{(short) Byte.MAX_VALUE}));
        assertEquals("byte-max", map.get(new Object[]{(int) Byte.MAX_VALUE}));
        assertEquals("byte-max", map.get(new Object[]{(long) Byte.MAX_VALUE}));
        assertEquals("byte-max", map.get(new Object[]{new BigInteger(String.valueOf(Byte.MAX_VALUE))}));
        assertEquals("byte-max", map.get(new Object[]{new BigDecimal(String.valueOf(Byte.MAX_VALUE))}));
        
        // Test minimum values
        map.put(new Object[]{Byte.MIN_VALUE}, "byte-min");
        assertEquals("byte-min", map.get(new Object[]{(short) Byte.MIN_VALUE}));
        assertEquals("byte-min", map.get(new Object[]{(int) Byte.MIN_VALUE}));
        assertEquals("byte-min", map.get(new Object[]{(long) Byte.MIN_VALUE}));
        assertEquals("byte-min", map.get(new Object[]{new BigInteger(String.valueOf(Byte.MIN_VALUE))}));
        assertEquals("byte-min", map.get(new Object[]{new BigDecimal(String.valueOf(Byte.MIN_VALUE))}));
    }
    
    @Test
    void testTypeStrictStillWorksWhenDisabled() {
        MultiKeyMap<String> map = MultiKeyMap.<String>builder()
            .valueBasedEquality(false)  // Explicitly set to false for this test
            .build();
        
        // Put with int
        map.put(new Object[]{42}, "int-value");
        
        // Should NOT match with other numeric types
        assertNull(map.get(new Object[]{42L}));                                                 // long
        assertNull(map.get(new Object[]{42.0}));                                               // double  
        assertNull(map.get(new Object[]{(byte) 42}));                                          // byte
        assertNull(map.get(new Object[]{new BigInteger("42")}));                              // BigInteger
        assertNull(map.get(new Object[]{new BigDecimal("42")}));                              // BigDecimal
        
        // Should only match with exact same type
        assertEquals("int-value", map.get(new Object[]{42}));                                   // int (same)
    }
}