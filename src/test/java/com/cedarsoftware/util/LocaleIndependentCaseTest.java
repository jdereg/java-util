package com.cedarsoftware.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TestUtil's case-insensitive search and CompactMap's reading of a map type's name answer the same under any default
 * locale. Both lowered with the default locale, and Turkish lowers I to a dotless i.
 */
class LocaleIndependentCaseTest {

    /** A sorted map type whose name says it is descending, in capitals. */
    @SuppressWarnings("serial")
    public static class DESCENDINGTreeMap<K, V> extends TreeMap<K, V> {
    }

    /** Under tr-TR, "Invalid ID" lowered to "ınvalıd ıd", which contains neither "invalid" nor "id". */
    @Test
    void containsIgnoreCaseUnderATurkishDefaultLocale() {
        assertTrue(underTurkish(() -> TestUtil.checkContainsIgnoreCase("Invalid ID: 42", "invalid", "id")));
        underTurkish(() -> {
            TestUtil.assertContainsIgnoreCase("Invalid ID: 42", "INVALID", "Id", "42");
            return null;
        });
    }

    /**
     * A sorted map type whose name says "descending" gives reverse ordering; under tr-TR its name lowered to
     * "...descendıng..." and the ordering came out ascending. (The compact size is one no other test uses, so the
     * template class CompactMap generates for this configuration is made here, under the Turkish locale.)
     */
    @Test
    void aMapTypesNameIsReadTheSameUnderATurkishDefaultLocale() {
        List<String> keys = underTurkish(() -> {
            Map<String, Integer> map = CompactMap.<String, Integer>builder()
                    .mapType(DESCENDINGTreeMap.class)
                    .compactSize(37)
                    .build();
            map.put("a", 1);
            map.put("c", 3);
            map.put("b", 2);
            return new ArrayList<>(map.keySet());
        });
        assertEquals(Arrays.asList("c", "b", "a"), keys);
    }

    private static <T> T underTurkish(Supplier<T> action) {
        Locale saved = Locale.getDefault();
        try {
            Locale.setDefault(new Locale("tr", "TR"));
            return action.get();
        } finally {
            Locale.setDefault(saved);
        }
    }
}
