package com.cedarsoftware.util;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

class CaseInsensitiveMapConstructorTest {

    @Test
    void testNullSourceMap() {
        assertThrows(NullPointerException.class, () -> new CaseInsensitiveMap<>(null, new HashMap<>()));
    }

    @Test
    void testNullMapInstance() {
        Map<String, String> source = new HashMap<>();
        assertThrows(NullPointerException.class, () -> new CaseInsensitiveMap<>(source, null));
    }

    @Test
    void testNonEmptyMapInstance() {
        Map<String, String> source = new HashMap<>();
        Map<String, String> dest = new HashMap<>();
        dest.put("one", "1");
        assertThrows(IllegalArgumentException.class, () -> new CaseInsensitiveMap<>(source, dest));
    }
}
