package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class UnsupportedConversionCacheHitTest {

    @Test
    void secondCallUsesCachedUnsupported() {
        Converter converter = new Converter(new DefaultConverterOptions());
        assertFalse(converter.isSimpleTypeConversionSupported(Map.class, Map.class));

        Map<?, ?> first = converter.convert(new HashMap<>(), Map.class);
        assertNull(first);

        Map<?, ?> second = converter.convert(new HashMap<>(), Map.class);
        assertNull(second);
    }
}
