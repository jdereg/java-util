package com.cedarsoftware.util.convert;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for ObjectConversions bugs.
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
        assertThat(result).containsEntry("age", 30);
    }

    // ---- Bug #1: Complex nested objects in collections become toString() ----

    @Test
    void testNestedObjectInListPreservesStructure() {
        OuterWithList outer = new OuterWithList();
        Map<String, Object> result = converter.convert(outer, Map.class);

        assertThat(result).isNotNull();
        Object items = result.get("items");
        assertThat(items).isInstanceOf(List.class);
        List<?> itemList = (List<?>) items;
        assertThat(itemList).hasSize(2);
        // Each item should be a Map with structured data, NOT a toString() string
        assertThat(itemList.get(0)).isInstanceOf(Map.class);
        @SuppressWarnings("unchecked")
        Map<String, Object> firstItem = (Map<String, Object>) itemList.get(0);
        assertThat(firstItem).containsEntry("name", "hello");
        assertThat(firstItem).containsEntry("value", 42);
    }

    @Test
    void testNestedObjectInArrayPreservesStructure() {
        OuterWithArray outer = new OuterWithArray();
        Map<String, Object> result = converter.convert(outer, Map.class);

        assertThat(result).isNotNull();
        Object items = result.get("items");
        assertThat(items).isInstanceOf(List.class);
        List<?> itemList = (List<?>) items;
        assertThat(itemList).hasSize(2);
        assertThat(itemList.get(0)).isInstanceOf(Map.class);
    }

    // ---- Bug #3: Numbers round-tripped through string parsing ----

    @Test
    void testIntegerNotDowncastOrUpcast() {
        // Integer 30 should stay as Integer, not be round-tripped through
        // toString() → parseToMinimalNumericType() which returns Long
        TestObject obj = new TestObject("test", 30);
        Map<String, Object> result = converter.convert(obj, Map.class);

        Object age = result.get("age");
        assertThat(age).isInstanceOf(Integer.class);
        assertThat(age).isEqualTo(30);
    }

    @Test
    void testLongNotDowncast() {
        // Long value 100L should NOT be downcast to Integer through the string round-trip
        LongHolder holder = new LongHolder();
        Map<String, Object> result = converter.convert(holder, Map.class);

        Object value = result.get("count");
        assertThat(value).isInstanceOf(Long.class);
        assertThat(value).isEqualTo(100L);
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

    public static class Inner {
        public String name = "hello";
        public int value = 42;
    }

    public static class OuterWithList {
        public List<Inner> items = Arrays.asList(new Inner(), new Inner());
    }

    public static class OuterWithArray {
        public Inner[] items = {new Inner(), new Inner()};
    }

    public static class LongHolder {
        public long count = 100L;
    }
}