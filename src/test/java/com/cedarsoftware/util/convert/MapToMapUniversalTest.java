package com.cedarsoftware.util.convert;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.WeakHashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Comprehensive test for the "One Converter to Rule Them All" - testing every known Map type.
 * This verifies that our "secret sauce" Map→Map converter can handle:
 * 1. All regular JDK Map types
 * 2. All "freak" Collection wrapper types  
 * 3. JsonObject from json-io (via reflection)
 * 4. Custom Map types like CompactMap
 * 5. Edge cases and error conditions
 */
class MapToMapUniversalTest {

    private Converter converter;
    private Map<String, Object> sourceMap;

    @BeforeEach
    void setUp() {
        ConverterOptions options = new ConverterOptions() {};
        converter = new Converter(options);
        
        // Create a source map with diverse content
        sourceMap = new LinkedHashMap<>();
        sourceMap.put("string", "value");
        sourceMap.put("number", 42);
        sourceMap.put("boolean", true);
        sourceMap.put("null", null);
    }
    
    // Helper method to verify map conversion - JDK 8 compatible
    private void assertMapConversion(Map<?, ?> result, Class<?> expectedClass, Map<String, Object> expectedContent) {
        assertThat(result).isInstanceOf(expectedClass);
        assertThat(result).hasSize(expectedContent.size());
        for (Map.Entry<String, Object> entry : expectedContent.entrySet()) {
            assertThat(result.get(entry.getKey())).isEqualTo(entry.getValue());
        }
    }

    // ========================================
    // Regular JDK Map Types
    // ========================================

    @Test
    void testHashMap() {
        Map<?, ?> result = converter.convert(sourceMap, HashMap.class);
        assertMapConversion(result, HashMap.class, sourceMap);
    }

    @Test
    void testLinkedHashMap() {
        Map<?, ?> result = converter.convert(sourceMap, LinkedHashMap.class);
        assertMapConversion(result, LinkedHashMap.class, sourceMap);
    }

    @Test
    void testTreeMap() {
        // TreeMap requires String keys for natural ordering
        Map<String, Object> stringKeyMap = new HashMap<>();
        stringKeyMap.put("a", "value1");
        stringKeyMap.put("b", "value2");
        stringKeyMap.put("c", "value3");
        
        Map<?, ?> result = converter.convert(stringKeyMap, TreeMap.class);
        assertMapConversion(result, TreeMap.class, stringKeyMap);
    }

    @Test
    void testConcurrentHashMap() {
        Map<?, ?> result = (Map<?, ?>) converter.convert(sourceMap, ConcurrentHashMap.class);
        
        // ConcurrentHashMap doesn't support null keys/values, so create expected map without nulls
        Map<String, Object> expectedNonNullMap = new LinkedHashMap<>();
        expectedNonNullMap.put("string", "value");
        expectedNonNullMap.put("number", 42);
        expectedNonNullMap.put("boolean", true);
        // "null" key with null value is excluded for ConcurrentHashMap
        
        assertMapConversion(result, ConcurrentHashMap.class, expectedNonNullMap);
    }

    @Test
    void testConcurrentSkipListMap() {
        // ConcurrentSkipListMap requires String keys for natural ordering and doesn't support nulls
        Map<String, Object> stringKeyMap = new HashMap<>();
        stringKeyMap.put("a", "value1");
        stringKeyMap.put("b", "value2");
        
        Map<?, ?> result = converter.convert(stringKeyMap, ConcurrentSkipListMap.class);
        assertMapConversion(result, ConcurrentSkipListMap.class, stringKeyMap);
    }

    @Test
    void testWeakHashMap() {
        Map<?, ?> result = converter.convert(sourceMap, WeakHashMap.class);
        assertMapConversion(result, WeakHashMap.class, sourceMap);
    }

    @Test
    void testIdentityHashMap() {
        Map<?, ?> result = converter.convert(sourceMap, IdentityHashMap.class);
        assertMapConversion(result, IdentityHashMap.class, sourceMap);
    }

    // ========================================
    // "Freak" Collection Wrapper Types
    // ========================================

    @Test
    void testEmptyMap() {
        Map<String, Object> emptySource = Collections.emptyMap();
        
        // Convert to EmptyMap type
        Map<?, ?> emptyMap = Collections.emptyMap();
        Class<?> emptyMapClass = emptyMap.getClass();
        
        Map<?, ?> result = (Map<?, ?>) converter.convert(emptySource, emptyMapClass);
        assertThat(result).isInstanceOf(emptyMapClass);
        assertThat(result).isEmpty();
        assertThat(result).isSameAs(Collections.emptyMap()); // Should return singleton
    }

    @Test
    void testSingletonMap() {
        Map<String, Object> singleSource = Collections.singletonMap("key", "value");
        
        // Convert to SingletonMap type
        Map<?, ?> singletonMap = Collections.singletonMap("test", "test");
        Class<?> singletonMapClass = singletonMap.getClass();
        
        
        Map<?, ?> result = (Map<?, ?>) converter.convert(singleSource, singletonMapClass);
        assertThat(result).isInstanceOf(singletonMapClass);
        assertThat(result).hasSize(1);
        assertThat(result.get("key")).isEqualTo("value");
    }

    @Test
    void testSingletonMapWithMultipleEntries() {
        // Should throw exception when trying to convert multi-entry map to singleton
        Map<?, ?> singletonMap = Collections.singletonMap("test", "test");
        Class<?> singletonMapClass = singletonMap.getClass();
        
        assertThatThrownBy(() -> converter.convert(sourceMap, singletonMapClass))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Cannot convert Map with " + sourceMap.size() + " entries to SingletonMap");
    }

    @Test
    void testUnmodifiableMap() {
        Map<?, ?> unmodifiableMap = Collections.unmodifiableMap(new HashMap<>());
        Class<?> unmodifiableMapClass = unmodifiableMap.getClass();
        
        Map<?, ?> result = (Map<?, ?>) converter.convert(sourceMap, unmodifiableMapClass);
        assertThat(result).isInstanceOf(unmodifiableMapClass);
        assertMapConversion(result, unmodifiableMapClass, sourceMap);
        
        // Verify it's actually unmodifiable
        assertThatThrownBy(() -> {
            @SuppressWarnings("unchecked")
            Map<Object, Object> mutableResult = (Map<Object, Object>) result;
            mutableResult.put("new", "value");
        }).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void testSynchronizedMap() {
        Map<?, ?> synchronizedMap = Collections.synchronizedMap(new HashMap<>());
        Class<?> synchronizedMapClass = synchronizedMap.getClass();
        
        Map<?, ?> result = (Map<?, ?>) converter.convert(sourceMap, synchronizedMapClass);
        assertThat(result).isInstanceOf(synchronizedMapClass);
        assertMapConversion(result, synchronizedMapClass, sourceMap);
    }

    // ========================================
    // JsonObject (via reflection)
    // ========================================

    @Test
    void testJsonObject() {
        try {
            // Try to load JsonObject class via reflection
            Class<?> jsonObjectClass = Class.forName("com.cedarsoftware.io.JsonObject");
            
            Map<?, ?> result = (Map<?, ?>) converter.convert(sourceMap, jsonObjectClass);
            assertThat(result).isInstanceOf(jsonObjectClass);
            assertMapConversion(result, jsonObjectClass, sourceMap);
        } catch (ClassNotFoundException e) {
            // JsonObject not available in this environment, skip test
            System.out.println("JsonObject not available, skipping test");
        }
    }

    // ========================================
    // Custom Map Types (CompactMap, etc.)
    // ========================================

    @Test
    void testCompactMap() {
        try {
            // Try to load CompactMap class
            Class<?> compactMapClass = Class.forName("com.cedarsoftware.util.CompactMap");
            
            Map<?, ?> result = (Map<?, ?>) converter.convert(sourceMap, compactMapClass);
            assertThat(result).isInstanceOf(compactMapClass);
            assertMapConversion(result, compactMapClass, sourceMap);
        } catch (ClassNotFoundException e) {
            // CompactMap not available, skip test
            System.out.println("CompactMap not available, skipping test");
        }
    }

    // ========================================
    // Edge Cases and Error Conditions
    // ========================================

    @Test
    void testNullSource() {
        Map<?, ?> result = converter.convert(null, HashMap.class);
        assertThat(result).isNull();
    }

    @Test
    void testNonMapSource() {
        // String has its own specific converter - StringConversions::toMap
        // Only enum-like strings (uppercase with underscores) are supported
        Map<?, ?> result = converter.convert("ENUM_VALUE", HashMap.class);
        assertThat(result).isInstanceOf(HashMap.class);
        assertThat(result).isNotNull();
        // String conversion creates a name-based map structure
        assertThat(result).hasSize(1);
    }

    @Test
    void testGenericMapInterface() {
        // When target is generic Map interface, should default to LinkedHashMap
        Map<?, ?> result = converter.convert(sourceMap, Map.class);
        assertThat(result).isInstanceOf(LinkedHashMap.class); // Default fallback
        assertMapConversion(result, result.getClass(), sourceMap);
    }

    @Test
    void testNullTargetType() {
        // Should fallback to LinkedHashMap when target type is null
        Map<?, ?> result = MapConversions.mapToMapWithTarget(sourceMap, converter, null);
        assertThat(result).isInstanceOf(LinkedHashMap.class);
        assertMapConversion(result, result.getClass(), sourceMap);
    }

    // ========================================
    // Round-trip Conversions
    // ========================================

    @Test
    void testRoundTripConversions() {
        // Test converting between different Map types
        Map<?, ?> hashMap = converter.convert(sourceMap, HashMap.class);
        Map<?, ?> linkedHashMap = converter.convert(hashMap, LinkedHashMap.class);
        Map<?, ?> backToSource = converter.convert(linkedHashMap, sourceMap.getClass());
        
        // Verify all entries match
        for (Map.Entry<String, Object> entry : sourceMap.entrySet()) {
            assertThat(backToSource.get(entry.getKey())).isEqualTo(entry.getValue());
        }
        assertThat(backToSource).isInstanceOf(sourceMap.getClass());
    }

    @Test
    void testFreakToRegularConversion() {
        // Convert from unmodifiable to regular map
        Map<?, ?> unmodifiableMap = Collections.unmodifiableMap(new HashMap<>(sourceMap));
        Map<?, ?> regularMap = converter.convert(unmodifiableMap, HashMap.class);
        
        assertThat(regularMap).isInstanceOf(HashMap.class);
        // Verify all entries match
        for (Map.Entry<String, Object> entry : sourceMap.entrySet()) {
            assertThat(regularMap.get(entry.getKey())).isEqualTo(entry.getValue());
        }
        
        // Should be mutable now - cast safely
        @SuppressWarnings("unchecked")
        Map<Object, Object> mutableMap = (Map<Object, Object>) regularMap;
        mutableMap.put("new", "value");
        assertThat(regularMap.get("new")).isEqualTo("value");
    }

    // ========================================
    // Performance and Stress Tests
    // ========================================

    @Test
    void testLargeMapConversion() {
        // Create a large map
        Map<String, Integer> largeMap = new HashMap<>();
        for (int i = 0; i < 1000; i++) { // Reduce size for faster test
            largeMap.put("key" + i, i);
        }
        
        Map<?, ?> result = converter.convert(largeMap, LinkedHashMap.class);
        assertThat(result).isInstanceOf(LinkedHashMap.class);
        assertThat(result).hasSize(1000);
        // Verify a sample of entries
        assertThat(result.get("key0")).isEqualTo(0);
        assertThat(result.get("key500")).isEqualTo(500);
        assertThat(result.get("key999")).isEqualTo(999);
    }

    @Test
    void testComplexNestedContent() {
        // Create map with complex nested content (JDK 8 compatible)
        Map<String, Object> complexMap = new HashMap<>();
        Map<String, String> innerMap = new HashMap<>();
        innerMap.put("inner", "value");
        complexMap.put("nested", innerMap);
        complexMap.put("list", Arrays.asList(1, 2, 3));
        complexMap.put("array", new String[]{"a", "b", "c"});
        
        Map<?, ?> result = converter.convert(complexMap, TreeMap.class);
        assertMapConversion(result, TreeMap.class, complexMap);
    }
}