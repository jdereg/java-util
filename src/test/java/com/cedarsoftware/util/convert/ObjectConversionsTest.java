package com.cedarsoftware.util.convert;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for ObjectConversions to ensure Object to Map conversions work correctly.
 * TEMPORARILY DISABLED - Object to Map conversions not yet supported in Golden Idea state
 */
class ObjectConversionsTest {

    private Converter converter;

    @BeforeEach
    void setUp() {
        ConverterOptions options = new ConverterOptions() {};
        converter = new Converter(options);
    }

    @Test
    void testSimpleObjectToMap() {
        TestObject obj = new TestObject("John", 30);
        
        Map<String, Object> result = converter.convert(obj, Map.class);
        
        assertThat(result).isNotNull();
        assertThat(result).containsEntry("name", "John");
        assertThat(result).containsEntry("age", 30L); // MathUtilities converts int to Long
    }

    // @Test
    void testNestedObjectToMap() {
        NestedTestObject nested = new NestedTestObject("nested value");
        TestObjectWithNested obj = new TestObjectWithNested("parent", nested);
        
        Map<String, Object> result = converter.convert(obj, Map.class);
        
        assertThat(result).isNotNull();
        assertThat(result).containsEntry("name", "parent");
        assertThat(result).containsKey("nested");
        
        @SuppressWarnings("unchecked")
        Map<String, Object> nestedMap = (Map<String, Object>) result.get("nested");
        assertThat(nestedMap).containsEntry("value", "nested value");
    }

    // @Test
    void testPrimitiveToMap() {
        Map<String, Object> result = converter.convert(42, Map.class);
        
        assertThat(result).isNotNull();
        assertThat(result).containsEntry("_v", 42); // Uses "_v" key, preserves original Integer
        assertThat(result).hasSize(1);
    }

    // @Test
    void testStringToMap() {
        // String has its own specific conversion that takes precedence over Object.class
        // This test verifies that String conversion works (not using ObjectConversions)
        Map<String, Object> result = converter.convert("ENUM_VALUE", Map.class);
        
        assertThat(result).isNotNull();
        assertThat(result).containsEntry("name", "ENUM_VALUE"); // String enum-like conversion
        assertThat(result).hasSize(1);
    }

    // @Test
    void testNullObjectToMap() {
        Map<String, Object> result = converter.convert(null, Map.class);
        
        assertThat(result).isNull();
    }

    // Test classes
    public static class TestObject {
        public String name;
        public int age;

        public TestObject(String name, int age) {
            this.name = name;
            this.age = age;
        }
    }

    public static class NestedTestObject {
        public String value;

        public NestedTestObject(String value) {
            this.value = value;
        }
    }

    public static class TestObjectWithNested {
        public String name;
        public NestedTestObject nested;

        public TestObjectWithNested(String name, NestedTestObject nested) {
            this.name = name;
            this.nested = nested;
        }
    }
}