package com.cedarsoftware.util.convert;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NavigableMap;
import java.util.SortedMap;
import java.util.TreeMap;

import com.cedarsoftware.util.geom.Color;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for MapConversions bugs.
 *
 * @author John DeRegnaucourt (jdereg@gmail.com)
 *         <br>
 *         Copyright (c) Cedar Software LLC
 *         <br><br>
 *         Licensed under the Apache License, Version 2.0 (the "License");
 *         you may not use this file except in compliance with the License.
 *         You may obtain a copy of the License at
 *         <br><br>
 *         <a href="http://www.apache.org/licenses/LICENSE-2.0">License</a>
 *         <br><br>
 *         Unless required by applicable law or agreed to in writing, software
 *         distributed under the License is distributed on an "AS IS" BASIS,
 *         WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *         See the License for the specific language governing permissions and
 *         limitations under the License.
 */
class MapConversionsTest {

    private Converter converter;

    @BeforeEach
    void setUp() {
        converter = new Converter(new DefaultConverterOptions());
    }

    // ---- Bug #1: toThrowable unchecked cast of causeMessage ----

    @Test
    void toThrowable_causeMessageAsInteger_doesNotThrowClassCastException() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("class", "java.lang.RuntimeException");
        map.put("message", "outer error");
        map.put("cause", "java.lang.IllegalArgumentException");
        map.put("causeMessage", 42);  // Not a String — was throwing ClassCastException

        Throwable result = MapConversions.toThrowable(map, converter, RuntimeException.class);
        assertNotNull(result);
        assertEquals("outer error", result.getMessage());
        assertNotNull(result.getCause());
        assertEquals("42", result.getCause().getMessage());
    }

    @Test
    void toThrowable_causeMessageAsString_stillWorks() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("class", "java.lang.RuntimeException");
        map.put("message", "outer");
        map.put("cause", "java.lang.IllegalArgumentException");
        map.put("causeMessage", "inner message");

        Throwable result = MapConversions.toThrowable(map, converter, RuntimeException.class);
        assertNotNull(result);
        assertNotNull(result.getCause());
        assertEquals("inner message", result.getCause().getMessage());
    }

    // ---- Bug #2: analyzeTarget misses sorted/navigable wrapper variants ----

    @Test
    void mapToMap_unmodifiableSortedMap() {
        TreeMap<String, Integer> source = new TreeMap<>();
        source.put("a", 1);
        source.put("b", 2);
        SortedMap<String, Integer> unmodifiable = Collections.unmodifiableSortedMap(source);

        // Convert to the same unmodifiable sorted map type
        Map<?, ?> result = MapConversions.mapToMapWithTarget(source, converter, unmodifiable.getClass());
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(1, result.get("a"));
        // Must actually be unmodifiable — not a plain LinkedHashMap fallback
        @SuppressWarnings("unchecked")
        Map<String, Object> resultMap = (Map<String, Object>) result;
        assertThrows(UnsupportedOperationException.class, () -> resultMap.put("c", 3));
    }

    @Test
    void mapToMap_unmodifiableNavigableMap() {
        TreeMap<String, Integer> source = new TreeMap<>();
        source.put("x", 10);
        NavigableMap<String, Integer> unmodifiable = Collections.unmodifiableNavigableMap(source);

        Map<?, ?> result = MapConversions.mapToMapWithTarget(source, converter, unmodifiable.getClass());
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(10, result.get("x"));
        @SuppressWarnings("unchecked")
        Map<String, Object> resultMap = (Map<String, Object>) result;
        assertThrows(UnsupportedOperationException.class, () -> resultMap.put("y", 20));
    }

    @Test
    void mapToMap_synchronizedSortedMap() {
        TreeMap<String, Integer> source = new TreeMap<>();
        source.put("k", 99);
        SortedMap<String, Integer> synced = Collections.synchronizedSortedMap(source);

        Map<?, ?> result = MapConversions.mapToMapWithTarget(source, converter, synced.getClass());
        assertNotNull(result);
        assertEquals(1, result.size());
        // Synchronized maps are still mutable, but verify the type name indicates synchronized
        assertTrue(result.getClass().getName().contains("Synchronized"));
    }

    @Test
    void mapToMap_synchronizedNavigableMap() {
        TreeMap<String, Integer> source = new TreeMap<>();
        source.put("m", 7);
        NavigableMap<String, Integer> synced = Collections.synchronizedNavigableMap(source);

        Map<?, ?> result = MapConversions.mapToMapWithTarget(source, converter, synced.getClass());
        assertNotNull(result);
        assertEquals(1, result.size());
        assertTrue(result.getClass().getName().contains("Synchronized"));
    }

    // ---- Bug #3: toColor packed RGB ignores explicit alpha ----

    @Test
    void toColor_rgbWithExplicitAlpha_usesExplicitAlpha() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("rgb", 0x00FF0000);  // packed int: alpha=0, red=255
        map.put("alpha", 128);        // explicit alpha should win

        Color color = MapConversions.toColor(map, converter);
        assertEquals(255, color.getRed());
        assertEquals(0, color.getGreen());
        assertEquals(0, color.getBlue());
        assertEquals(128, color.getAlpha());  // Should be 128, not 0
    }

    @Test
    void toColor_rgbWithoutAlpha_defaultsTo255() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("rgb", 0x00FF8040);  // packed int with zero in alpha bits

        Color color = MapConversions.toColor(map, converter);
        assertEquals(255, color.getRed());
        assertEquals(128, color.getGreen());
        assertEquals(64, color.getBlue());
        assertEquals(255, color.getAlpha());  // No alpha key → default 255
    }

    // ---- Bug #4: copyEntries silently swallows exceptions ----

    @Test
    void mapToMap_treeMap_incompatibleKeyThrowsOrSkips() {
        // TreeMap with natural ordering: non-Comparable keys will throw ClassCastException
        // The copyEntries catch should only catch ClassCastException/NPE, not all exceptions
        Map<Object, Object> source = new LinkedHashMap<>();
        source.put("validKey", "value1");
        source.put(42, "value2");  // Integer key — not comparable to String in TreeMap

        // Converting to TreeMap: String key works, Integer key should be skipped (ClassCastException)
        Map<?, ?> result = MapConversions.mapToMapWithTarget(source, converter, TreeMap.class);
        assertNotNull(result);
        // At least the compatible entry should be present
        assertEquals("value1", result.get("validKey"));
    }
}
