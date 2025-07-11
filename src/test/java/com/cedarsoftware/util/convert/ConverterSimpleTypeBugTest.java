package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Test to demonstrate and verify the fix for the isSimpleTypeConversionSupported() bug.
 * 
 * The bug: isSimpleTypeConversionSupported() only checks inheritance but ignores 
 * custom converter overrides, causing incorrect behavior when users register
 * custom converters for types.
 */
public class ConverterSimpleTypeBugTest {

    // Test class that extends Date (should normally be considered "simple")
    public static class WeirdDate extends Date {
        public WeirdDate(long time) {
            super(time);
        }
    }

    @Test
    void testSimpleTypeCheckWithoutCustomOverrides() {
        // Without custom overrides, WeirdDate should be considered simple since it extends Date
        ConverterOptions options = new ConverterOptions() {};
        Converter converter = new Converter(options);
        
        assertTrue(converter.isSimpleTypeConversionSupported(Date.class));
        assertTrue(converter.isSimpleTypeConversionSupported(WeirdDate.class), 
                   "WeirdDate extends Date, so should be simple without custom overrides");
    }

    @Test
    void testSimpleTypeCheckWithCustomOverrides() {
        // With custom overrides, WeirdDate should NOT be considered simple anymore
        Map<Converter.ConversionPair, Convert<?>> overrides = new HashMap<>();
        
        // Register custom converters for WeirdDate
        overrides.put(Converter.pair(String.class, WeirdDate.class, 0L), 
                     (source, converter) -> new WeirdDate(System.currentTimeMillis()));
        overrides.put(Converter.pair(Map.class, WeirdDate.class, 0L),
                     (source, converter) -> new WeirdDate(System.currentTimeMillis()));
        
        ConverterOptions options = new ConverterOptions() {
            @Override
            public Map<Converter.ConversionPair, Convert<?>> getConverterOverrides() {
                return overrides;
            }
        };
        
        Converter converter = new Converter(options);
        
        // Date should still be simple (no custom overrides for it)
        assertTrue(converter.isSimpleTypeConversionSupported(Date.class));
        
        // WeirdDate should NOT be simple because user registered custom converters
        // This is the key assertion that should pass after the fix
        assertFalse(converter.isSimpleTypeConversionSupported(WeirdDate.class), 
                   "WeirdDate should not be considered simple when custom overrides exist");
    }

    @Test
    void testDifferentInstancesHaveDifferentBehavior() {
        // First converter with no custom overrides
        ConverterOptions options1 = new ConverterOptions() {};
        Converter converter1 = new Converter(options1);
        
        // Second converter with custom overrides for WeirdDate
        Map<Converter.ConversionPair, Convert<?>> overrides = new HashMap<>();
        overrides.put(Converter.pair(String.class, WeirdDate.class, 0L), 
                     (source, converter) -> new WeirdDate(System.currentTimeMillis()));
        
        ConverterOptions options2 = new ConverterOptions() {
            @Override
            public Map<Converter.ConversionPair, Convert<?>> getConverterOverrides() {
                return overrides;
            }
        };
        Converter converter2 = new Converter(options2);
        
        // Different instances should have different behavior for the same type
        assertTrue(converter1.isSimpleTypeConversionSupported(WeirdDate.class), 
                  "Converter1 should consider WeirdDate simple (no custom overrides)");
        assertFalse(converter2.isSimpleTypeConversionSupported(WeirdDate.class), 
                   "Converter2 should NOT consider WeirdDate simple (has custom overrides)");
    }

    @Test
    void testMultipleTargetOverrides() {
        // Test case where multiple conversions TO the same target type exist
        Map<Converter.ConversionPair, Convert<?>> overrides = new HashMap<>();
        
        // Multiple source types that convert TO WeirdDate
        overrides.put(Converter.pair(String.class, WeirdDate.class, 0L), 
                     (source, converter) -> new WeirdDate(System.currentTimeMillis()));
        overrides.put(Converter.pair(Long.class, WeirdDate.class, 0L),
                     (source, converter) -> new WeirdDate((Long) source));
        overrides.put(Converter.pair(Map.class, WeirdDate.class, 0L),
                     (source, converter) -> new WeirdDate(System.currentTimeMillis()));
        
        ConverterOptions options = new ConverterOptions() {
            @Override
            public Map<Converter.ConversionPair, Convert<?>> getConverterOverrides() {
                return overrides;
            }
        };
        
        Converter converter = new Converter(options);
        
        // WeirdDate should not be simple because it has custom overrides
        assertFalse(converter.isSimpleTypeConversionSupported(WeirdDate.class), 
                   "WeirdDate should not be simple when multiple custom overrides exist");
    }

    @Test
    void testTwoArgumentSimpleTypeSupportWithCustomOverrides() {
        // Test the two-argument version of isSimpleTypeConversionSupported
        Map<Converter.ConversionPair, Convert<?>> overrides = new HashMap<>();
        
        // Register custom converter from String to WeirdDate
        overrides.put(Converter.pair(String.class, WeirdDate.class, 0L), 
                     (source, converter) -> new WeirdDate(System.currentTimeMillis()));
        
        ConverterOptions options = new ConverterOptions() {
            @Override
            public Map<Converter.ConversionPair, Convert<?>> getConverterOverrides() {
                return overrides;
            }
        };
        
        Converter converter = new Converter(options);
        
        // Built-in conversion should still be simple
        assertTrue(converter.isSimpleTypeConversionSupported(String.class, Integer.class),
                  "Built-in String->Integer should still be simple");
        
        // Custom conversion should NOT be simple  
        assertFalse(converter.isSimpleTypeConversionSupported(String.class, WeirdDate.class),
                   "Custom String->WeirdDate should not be considered simple");
    }
}