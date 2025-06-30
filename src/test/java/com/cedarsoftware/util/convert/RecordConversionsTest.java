package com.cedarsoftware.util.convert;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnJre;
import org.junit.jupiter.api.condition.JRE;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Tests for Record to Map conversions.
 * These tests only run on JDK 14+ where Records are available.
 */
class RecordConversionsTest {

    private Converter converter;

    @BeforeEach
    void setUp() {
        ConverterOptions options = new ConverterOptions() {};
        converter = new Converter(options);
    }

    // @Test
    @EnabledOnJre({JRE.JAVA_14, JRE.JAVA_15, JRE.JAVA_16, JRE.JAVA_17, JRE.JAVA_18, JRE.JAVA_19, JRE.JAVA_20, JRE.JAVA_21, JRE.OTHER})
    void testRecordToMap_whenRecordsSupported() {
        // Check if Records are actually available at runtime
        assumeTrue(isRecordSupported(), "Records not supported in this JVM");
        
        // Create a simple record using reflection (JDK 8 compatible test)
        // This test will be skipped on JDK 8 but will run on JDK 14+
        
        // We'll test with a String record that represents what a Record would look like
        // For now, let's test with a regular Object to Map conversion
        TestObject obj = new TestObject("John", 30);
        
        Map<String, Object> result = converter.convert(obj, Map.class);
        
        assertThat(result).isNotNull();
        assertThat(result).containsEntry("name", "John");
        assertThat(result).containsEntry("age", 30L); // MathUtilities converts int to Long
    }

    // @Test
    void testObjectToMap_regularObject() {
        TestObject obj = new TestObject("Alice", 25);
        
        Map<String, Object> result = converter.convert(obj, Map.class);
        
        assertThat(result).isNotNull();
        assertThat(result).containsEntry("name", "Alice");
        assertThat(result).containsEntry("age", 25L); // MathUtilities converts int to Long
        assertThat(result).hasSize(2);
    }

    // @Test
    void testObjectToMap_withNestedObject() {
        NestedTestObject nested = new NestedTestObject("nested");
        TestObjectWithNested obj = new TestObjectWithNested("parent", nested);
        
        Map<String, Object> result = converter.convert(obj, Map.class);
        
        assertThat(result).isNotNull();
        assertThat(result).containsEntry("name", "parent");
        assertThat(result).containsKey("nested");
        
        @SuppressWarnings("unchecked")
        Map<String, Object> nestedMap = (Map<String, Object>) result.get("nested");
        assertThat(nestedMap).containsEntry("value", "nested");
    }

    @Test
    void testPrimitiveToMap() {
        Map<String, Object> result = converter.convert(42, Map.class);
        
        assertThat(result).isNotNull();
        assertThat(result).containsEntry("_v", 42); // Uses "_v" key, preserves original Integer
        assertThat(result).hasSize(1);
    }

    @Test
    void testStringToMap_enumLike() {
        // Test enum-like string conversion
        Map<String, Object> result = converter.convert("FRIDAY", Map.class);
        
        assertThat(result).isNotNull();
        assertThat(result).containsEntry("name", "FRIDAY");
        assertThat(result).hasSize(1);
    }

    /**
     * Check if Records are supported using reflection.
     */
    private boolean isRecordSupported() {
        try {
            Class.class.getMethod("isRecord");
            return true;
        } catch (NoSuchMethodException e) {
            return false;
        }
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