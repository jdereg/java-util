package com.cedarsoftware.util;

import com.cedarsoftware.util.convert.Convert;
import com.cedarsoftware.util.convert.DefaultConverterOptions;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class ConverterStaticMethodsTest {

    // Use an interface to ensure no built-in conversion exists
    interface UnconvertibleType {
        String getValue();
    }
    
    private static class CustomType implements UnconvertibleType {
        final String value;
        CustomType(String value) { this.value = value; }
        
        @Override
        public String getValue() { return value; }
    }

    @Test
    void conversionSupportDelegatesToInstance() {
        assertTrue(Converter.isConversionSupportedFor(String.class, Integer.class));
        assertFalse(Converter.isConversionSupportedFor(Map.class, List.class));
    }

    @Test
    void getSupportedConversionsListsKnownTypes() {
        Map<String, Set<String>> conversions = Converter.getSupportedConversions();
        assertThat(conversions).isNotEmpty();
        assertTrue(conversions.get("String").contains("Integer"));
        assertEquals(Converter.allSupportedConversions().size(), conversions.size());
    }

    @Test
    void customConversionsWorkWithConverterInstance() {
        // Demonstrates the new pattern for adding custom conversions
        Convert<CustomType> fn1 = (from, conv) -> new CustomType((String) from);
        Convert<CustomType> fn2 = (from, conv) -> new CustomType(((String) from).toUpperCase());
        
        // Create a Converter instance for custom conversions
        DefaultConverterOptions options = new DefaultConverterOptions();
        com.cedarsoftware.util.convert.Converter converter = new com.cedarsoftware.util.convert.Converter(options);
        
        // Add first conversion using new signature
        Convert<?> prev = converter.addConversion(fn1, String.class, CustomType.class);
        assertNull(prev);
        CustomType result = converter.convert("abc", CustomType.class);
        assertEquals("abc", result.value);

        // Replace with second conversion
        prev = converter.addConversion(fn2, String.class, CustomType.class);
        assertSame(fn1, prev);
        result = converter.convert("abc", CustomType.class);
        assertEquals("ABC", result.value);
        
        // Static converter should not have the custom conversion 
        // Test with interface type to ensure no built-in conversion exists
        try {
            Converter.convert("abc", UnconvertibleType.class);
            fail("Expected conversion to fail on static Converter without custom conversion");
        } catch (IllegalArgumentException e) {
            // Expected - static converter doesn't have our custom conversion
            assertThat(e.getMessage()).contains("Unsupported conversion");
        }
    }
}
