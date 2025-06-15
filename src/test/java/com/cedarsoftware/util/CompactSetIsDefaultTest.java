package com.cedarsoftware.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CompactSetIsDefaultTest {

    @Test
    void defaultSetIsRecognized() {
        CompactSet<String> set = new CompactSet<>();
        assertTrue(set.isDefaultCompactSet());
    }

    @Test
    void customSetIsNotRecognized() {
        CompactSet<String> set = CompactSet.<String>builder()
                .caseSensitive(false)
                .compactSize(10)
                .sortedOrder()
                .build();
        assertFalse(set.isDefaultCompactSet());
    }
}
