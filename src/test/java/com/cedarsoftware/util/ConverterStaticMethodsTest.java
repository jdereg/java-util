package com.cedarsoftware.util;

import com.cedarsoftware.util.convert.Convert;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class ConverterStaticMethodsTest {

    private static class CustomType {
        final String value;
        CustomType(String value) { this.value = value; }
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
    void addConversionAddsAndReplaces() {
        Convert<CustomType> fn1 = (from, conv) -> new CustomType((String) from);
        Convert<CustomType> fn2 = (from, conv) -> new CustomType(((String) from).toUpperCase());
        try {
            Convert<?> prev = Converter.addConversion(String.class, CustomType.class, fn1);
            assertNull(prev);
            CustomType result = Converter.convert("abc", CustomType.class);
            assertEquals("abc", result.value);

            prev = Converter.addConversion(String.class, CustomType.class, fn2);
            assertSame(fn1, prev);
            result = Converter.convert("abc", CustomType.class);
            assertEquals("ABC", result.value);
        } finally {
            Converter.addConversion(String.class, CustomType.class, new Convert<Object>() {
                @Override
                public Object convert(Object from, com.cedarsoftware.util.convert.Converter converter) {
                    return new CustomType((String)from);
                }
            });
        }
    }
}
