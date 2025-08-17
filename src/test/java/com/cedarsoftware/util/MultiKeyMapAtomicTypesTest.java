package com.cedarsoftware.util;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive test for atomic type support in MultiKeyMap value-based equality.
 * Tests AtomicBoolean, AtomicInteger, and AtomicLong integration with existing numeric types.
 */
public class MultiKeyMapAtomicTypesTest {

    @Test
    void testAtomicBooleanEquality() {
        MultiKeyMap<String> map = MultiKeyMap.<String>builder().valueBasedEquality(true).build();
        
        // Put with AtomicBoolean
        map.put(new Object[]{new AtomicBoolean(true), new AtomicBoolean(false)}, "atomic-bool-value");
        
        // Should match with Boolean
        assertEquals("atomic-bool-value", map.get(new Object[]{Boolean.TRUE, Boolean.FALSE}));
        assertEquals("atomic-bool-value", map.get(new Object[]{true, false}));
        
        // Should match with other AtomicBoolean instances with same values
        assertEquals("atomic-bool-value", map.get(new Object[]{new AtomicBoolean(true), new AtomicBoolean(false)}));
        
        // Test with Collections
        assertEquals("atomic-bool-value", map.get(Arrays.asList(Boolean.TRUE, Boolean.FALSE)));
        
        // Should NOT match with different boolean values
        assertNull(map.get(new Object[]{Boolean.TRUE, Boolean.TRUE}));
        assertNull(map.get(new Object[]{new AtomicBoolean(false), new AtomicBoolean(true)}));
        
        // Should NOT match with non-boolean types
        assertNull(map.get(new Object[]{1, 0}));
        assertNull(map.get(new Object[]{"true", "false"}));
    }
    
    @Test
    void testAtomicIntegerWithAllIntegralTypes() {
        MultiKeyMap<String> map = MultiKeyMap.<String>builder().valueBasedEquality(true).build();
        
        // Put with AtomicInteger
        map.put(new Object[]{new AtomicInteger(42), new AtomicInteger(100)}, "atomic-int-value");
        
        // Should match with ALL integral types
        assertEquals("atomic-int-value", map.get(new Object[]{(byte) 42, (byte) 100}));        // byte
        assertEquals("atomic-int-value", map.get(new Object[]{(short) 42, (short) 100}));      // short
        assertEquals("atomic-int-value", map.get(new Object[]{42, 100}));                      // int
        assertEquals("atomic-int-value", map.get(new Object[]{42L, 100L}));                    // long
        assertEquals("atomic-int-value", map.get(new Object[]{new AtomicLong(42), new AtomicLong(100)})); // AtomicLong
        assertEquals("atomic-int-value", map.get(new Object[]{new BigInteger("42"), new BigInteger("100")})); // BigInteger
        
        // Should match with whole-number floating types
        assertEquals("atomic-int-value", map.get(new Object[]{42.0f, 100.0f}));                // float (whole)
        assertEquals("atomic-int-value", map.get(new Object[]{42.0, 100.0}));                  // double (whole)
        assertEquals("atomic-int-value", map.get(new Object[]{new BigDecimal("42"), new BigDecimal("100")})); // BigDecimal (whole)
        
        // Should work with Collections
        assertEquals("atomic-int-value", map.get(Arrays.asList(42, 100)));
        assertEquals("atomic-int-value", map.get(Arrays.asList(42L, 100L)));
        assertEquals("atomic-int-value", map.get(Arrays.asList(new AtomicInteger(42), new AtomicInteger(100))));
        
        // Should NOT match with fractional floating types
        assertNull(map.get(new Object[]{42.1, 100.0}));
        assertNull(map.get(new Object[]{new BigDecimal("42.1"), new BigDecimal("100")}));
    }
    
    @Test
    void testAtomicLongWithAllIntegralTypes() {
        MultiKeyMap<String> map = MultiKeyMap.<String>builder().valueBasedEquality(true).build();
        
        // Put with AtomicLong
        map.put(new Object[]{new AtomicLong(1000), new AtomicLong(2000)}, "atomic-long-value");
        
        // Should match with ALL integral types that can represent these values
        assertEquals("atomic-long-value", map.get(new Object[]{1000, 2000}));                  // int
        assertEquals("atomic-long-value", map.get(new Object[]{1000L, 2000L}));                // long
        assertEquals("atomic-long-value", map.get(new Object[]{new AtomicInteger(1000), new AtomicInteger(2000)})); // AtomicInteger
        assertEquals("atomic-long-value", map.get(new Object[]{new BigInteger("1000"), new BigInteger("2000")})); // BigInteger
        
        // Should match with whole-number floating types
        assertEquals("atomic-long-value", map.get(new Object[]{1000.0f, 2000.0f}));            // float (whole)
        assertEquals("atomic-long-value", map.get(new Object[]{1000.0, 2000.0}));              // double (whole)
        assertEquals("atomic-long-value", map.get(new Object[]{new BigDecimal("1000"), new BigDecimal("2000")})); // BigDecimal (whole)
        
        // Test with very large long values (near Long.MAX_VALUE)
        map.put(new Object[]{new AtomicLong(Long.MAX_VALUE)}, "max-long-value");
        assertEquals("max-long-value", map.get(new Object[]{Long.MAX_VALUE}));
        assertEquals("max-long-value", map.get(new Object[]{new BigInteger(String.valueOf(Long.MAX_VALUE))}));
        // Note: Float/double may lose precision for very large longs, so we don't test exact equality there
    }
    
    @Test
    void testMixedAtomicTypes() {
        MultiKeyMap<String> map = MultiKeyMap.<String>builder().valueBasedEquality(true).build();
        
        // Put with mixed atomic types
        map.put(new Object[]{new AtomicInteger(5), new AtomicLong(10), new AtomicBoolean(true)}, "mixed-atomic-value");
        
        // Should match with equivalent non-atomic types
        assertEquals("mixed-atomic-value", map.get(new Object[]{5, 10L, Boolean.TRUE}));
        assertEquals("mixed-atomic-value", map.get(new Object[]{5L, 10, true}));
        assertEquals("mixed-atomic-value", map.get(new Object[]{5.0, 10.0, Boolean.TRUE}));
        
        // Should match with Collections
        assertEquals("mixed-atomic-value", map.get(Arrays.asList(5, 10L, Boolean.TRUE)));
        
        // Should match with other atomic instances
        assertEquals("mixed-atomic-value", map.get(new Object[]{new AtomicInteger(5), new AtomicLong(10), new AtomicBoolean(true)}));
    }
    
    @Test
    void testAtomicTypesWithBigDecimalAndBigInteger() {
        MultiKeyMap<String> map = MultiKeyMap.<String>builder().valueBasedEquality(true).build();
        
        // Put with BigDecimal and BigInteger
        map.put(new Object[]{new BigDecimal("123"), new BigInteger("456")}, "big-numbers-value");
        
        // Should match with atomic types
        assertEquals("big-numbers-value", map.get(new Object[]{new AtomicInteger(123), new AtomicLong(456)}));
        assertEquals("big-numbers-value", map.get(new Object[]{123, new AtomicLong(456)}));
        assertEquals("big-numbers-value", map.get(new Object[]{new AtomicInteger(123), 456L}));
        
        // Should work both ways
        map.put(new Object[]{new AtomicInteger(789), new AtomicLong(1000)}, "atomic-to-big-value");
        assertEquals("atomic-to-big-value", map.get(new Object[]{new BigDecimal("789"), new BigInteger("1000")}));
    }
    
    @Test
    void testAtomicTypesZeroValues() {
        MultiKeyMap<String> map = MultiKeyMap.<String>builder().valueBasedEquality(true).build();
        
        // Test that different representations of zero are treated as equal
        map.put(new Object[]{new AtomicInteger(0), new AtomicLong(0)}, "atomic-zero-value");
        
        assertEquals("atomic-zero-value", map.get(new Object[]{0, 0L}));                        // primitives
        assertEquals("atomic-zero-value", map.get(new Object[]{0.0, 0.0f}));                   // floating
        assertEquals("atomic-zero-value", map.get(new Object[]{new BigDecimal("0"), new BigInteger("0")})); // big numbers
        assertEquals("atomic-zero-value", map.get(new Object[]{new AtomicInteger(0), new AtomicLong(0)})); // same atomic types
        
        // Negative zero should also equal positive zero for floating point
        assertEquals("atomic-zero-value", map.get(new Object[]{-0.0, -0.0f}));
    }
    
    @Test
    void testAtomicTypesEdgeCases() {
        MultiKeyMap<String> map = MultiKeyMap.<String>builder().valueBasedEquality(true).build();
        
        // Test maximum values
        map.put(new Object[]{new AtomicInteger(Integer.MAX_VALUE)}, "int-max-atomic");
        assertEquals("int-max-atomic", map.get(new Object[]{Integer.MAX_VALUE}));
        assertEquals("int-max-atomic", map.get(new Object[]{(long) Integer.MAX_VALUE}));
        assertEquals("int-max-atomic", map.get(new Object[]{new AtomicLong(Integer.MAX_VALUE)}));
        
        // Test minimum values
        map.put(new Object[]{new AtomicLong(Long.MIN_VALUE)}, "long-min-atomic");
        assertEquals("long-min-atomic", map.get(new Object[]{Long.MIN_VALUE}));
        assertEquals("long-min-atomic", map.get(new Object[]{new BigInteger(String.valueOf(Long.MIN_VALUE))}));
        
        // Test that atomic types work with floating point special values
        map.put(new Object[]{new AtomicInteger(1), Double.NaN}, "atomic-with-nan");
        assertEquals("atomic-with-nan", map.get(new Object[]{1, Float.NaN}));
        assertEquals("atomic-with-nan", map.get(new Object[]{1L, Double.NaN}));
    }
    
    @Test
    void testAtomicTypeBasedEqualityWhenDisabled() {
        // Test that atomic types use value-based equality even when valueBasedEquality = false
        // This is intentional design - atomic types always compare by value for intuitive behavior
        MultiKeyMap<String> map = MultiKeyMap.<String>builder()
            .valueBasedEquality(false)  // Explicitly set to false for this test
            .build();
        
        // Put with AtomicInteger
        map.put(new Object[]{new AtomicInteger(42)}, "atomic-int-value");
        
        // Should NOT match with other numeric types when value-based equality is disabled
        assertNull(map.get(new Object[]{42}));                                                  // int
        assertNull(map.get(new Object[]{42L}));                                                 // long
        assertNull(map.get(new Object[]{new AtomicLong(42)}));                                  // Different atomic type
        assertNull(map.get(new Object[]{new BigInteger("42")}));                               // BigInteger
        
        // Should match with same atomic type and value (value-based comparison for atomic types)
        assertEquals("atomic-int-value", map.get(new Object[]{new AtomicInteger(42)}));         // same type and value
        
        // Should NOT match with different values
        assertNull(map.get(new Object[]{new AtomicInteger(43)}));                              // different value
        
        // Test AtomicBoolean value-based behavior even in type-strict mode
        map.put(new Object[]{new AtomicBoolean(true)}, "atomic-bool-value");
        assertNull(map.get(new Object[]{Boolean.TRUE}));                                       // Different type (Boolean)
        assertNull(map.get(new Object[]{true}));                                               // primitive boolean
        assertEquals("atomic-bool-value", map.get(new Object[]{new AtomicBoolean(true)}));     // same type and value
        
        // Test AtomicLong value-based behavior
        map.put(new Object[]{new AtomicLong(999)}, "atomic-long-value");
        assertEquals("atomic-long-value", map.get(new Object[]{new AtomicLong(999)}));         // same type and value
        assertNull(map.get(new Object[]{new AtomicInteger(999)}));                             // Different atomic type
        assertNull(map.get(new Object[]{999L}));                                               // Different type (Long)
    }
    
    @Test
    void testAtomicTypesPerformance() {
        MultiKeyMap<String> map = MultiKeyMap.<String>builder().valueBasedEquality(true).build();
        
        // Performance test with many atomic type operations
        int count = 1000;
        
        // Insert many atomic type keys
        for (int i = 0; i < count; i++) {
            map.put(new Object[]{new AtomicInteger(i), new AtomicLong(i * 2), new AtomicBoolean(i % 2 == 0)}, "value" + i);
        }
        
        // Test lookup performance with equivalent non-atomic types
        long startTime = System.nanoTime();
        for (int i = 0; i < count; i++) {
            String result = map.get(new Object[]{i, (long) i * 2, i % 2 == 0});
            assertEquals("value" + i, result);
        }
        long endTime = System.nanoTime();
        
        // Performance should be reasonable
        long durationMs = (endTime - startTime) / 1_000_000;
        assertTrue(durationMs < 100, "Atomic type processing should be fast, took " + durationMs + "ms");
        
        assertEquals(count, map.size());
    }
}