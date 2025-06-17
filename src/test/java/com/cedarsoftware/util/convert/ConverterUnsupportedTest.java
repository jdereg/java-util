package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ConverterUnsupportedTest {

    @Test
    void cachedUnsupportedReturnsNull() {
        Converter converter1 = new Converter(new DefaultConverterOptions());
        assertThrows(IllegalArgumentException.class,
                () -> converter1.convert(new HashMap<>(), Map.class));

        Converter converter2 = new Converter(new DefaultConverterOptions());
        assertFalse(converter2.isSimpleTypeConversionSupported(Map.class, Map.class));
        Map<?, ?> result = converter2.convert(new HashMap<>(), Map.class);
        assertNull(result);
    }
}
