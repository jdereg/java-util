package com.cedarsoftware.util.convert;

import java.util.logging.Logger;

import com.cedarsoftware.util.LoggingConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Test to verify complete isolation between static and instance conversion contexts.
 * 
 * Business Rule: 
 * - Static context (instanceId 0L) only sees static conversions and factory conversions
 * - Instance context (instanceId non-zero) only sees its own conversions and factory conversions
 * - No cross-contamination between contexts
 */
class IsolationTest {
    private static final Logger LOG = Logger.getLogger(IsolationTest.class.getName());
    static {
        LoggingConfig.initForTests();
    }
    
    static class AppType {
        final String value;
        AppType(String value) { this.value = value; }
        @Override public String toString() { return "AppType(" + value + ")"; }
        @Override public boolean equals(Object obj) {
            return obj instanceof AppType && ((AppType) obj).value.equals(this.value);
        }
    }
    
    @Test
    void testCompleteIsolationBetweenStaticAndInstanceContexts() {
        // Start clean - no conversions should exist for our custom type
        assertThrows(IllegalArgumentException.class, () -> 
                com.cedarsoftware.util.Converter.convert("test", AppType.class));
        
        com.cedarsoftware.util.convert.Converter instance1 = new com.cedarsoftware.util.convert.Converter(new DefaultConverterOptions());
        com.cedarsoftware.util.convert.Converter instance2 = new com.cedarsoftware.util.convert.Converter(new DefaultConverterOptions());
        
        assertThrows(IllegalArgumentException.class, () -> 
                instance1.convert("test", AppType.class));
        assertThrows(IllegalArgumentException.class, () -> 
                instance2.convert("test", AppType.class));
        
        // Add static conversion - should only affect static context
        com.cedarsoftware.util.Converter.addConversion(String.class, AppType.class, 
                (from, converter) -> new AppType("STATIC: " + from));
        
        // Static context should now work
        AppType staticResult = com.cedarsoftware.util.Converter.convert("test", AppType.class);
        assertEquals("STATIC: test", staticResult.value);
        
        // Instance contexts should still fail - NO FALLBACK TO STATIC
        assertThrows(IllegalArgumentException.class, () -> 
                instance1.convert("test", AppType.class));
        assertThrows(IllegalArgumentException.class, () -> 
                instance2.convert("test", AppType.class));
        
        // Add instance-specific conversion to instance1
        instance1.addConversion(
                (from, converter) -> new AppType("INSTANCE1: " + from),
                String.class, 
                AppType.class);
        
        // Only instance1 should now work
        AppType instance1Result = instance1.convert("test", AppType.class);
        assertEquals("INSTANCE1: test", instance1Result.value);
        
        // Static and instance2 should be unaffected
        AppType stillStaticResult = com.cedarsoftware.util.Converter.convert("test", AppType.class);
        assertEquals("STATIC: test", stillStaticResult.value);
        
        assertThrows(IllegalArgumentException.class, () -> 
                instance2.convert("test", AppType.class));
        
        LOG.info("✓ Complete isolation verified:");
        LOG.info("  Static: " + stillStaticResult.value);
        LOG.info("  Instance1: " + instance1Result.value);
        LOG.info("  Instance2: No conversion (isolated)");
    }
    
    @Test
    void testFactoryConversionsAvailableToAll() {
        // Factory conversions (like Integer to String) should work in all contexts
        
        // Static context
        String staticResult = com.cedarsoftware.util.Converter.convert(42, String.class);
        assertEquals("42", staticResult);
        
        // Instance contexts  
        com.cedarsoftware.util.convert.Converter instance1 = new com.cedarsoftware.util.convert.Converter(new DefaultConverterOptions());
        com.cedarsoftware.util.convert.Converter instance2 = new com.cedarsoftware.util.convert.Converter(new DefaultConverterOptions());
        
        String instance1Result = instance1.convert(42, String.class);
        String instance2Result = instance2.convert(42, String.class);
        
        assertEquals("42", instance1Result);
        assertEquals("42", instance2Result);
        
        LOG.info("✓ Factory conversions work in all contexts");
    }
}