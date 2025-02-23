package com.cedarsoftware.util;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ClassFinderTest {
    // Test classes for inheritance hierarchy
    interface TestInterface {}
    interface SubInterface extends TestInterface {}
    static class BaseClass {}
    static class MiddleClass extends BaseClass implements TestInterface {}
    private static class SubClass extends MiddleClass implements SubInterface {}

    @Test
    void testExactMatch() {
        Map<Class<?>, String> map = new HashMap<>();
        map.put(MiddleClass.class, "middle");
        map.put(BaseClass.class, "base");

        String result = ClassUtilities.findClosest(MiddleClass.class, map, "default");
        assertEquals("middle", result);
    }

    @Test
    void testInheritanceHierarchy() {
        Map<Class<?>, String> map = new HashMap<>();
        map.put(BaseClass.class, "base");
        map.put(TestInterface.class, "interface");

        // SubClass extends MiddleClass extends BaseClass implements TestInterface
        String result = ClassUtilities.findClosest(SubClass.class, map, "default");
        assertEquals("base", result);
    }

    @Test
    void testInterfaceMatch() {
        Map<Class<?>, String> map = new HashMap<>();
        map.put(TestInterface.class, "interface");

        String result = ClassUtilities.findClosest(MiddleClass.class, map, "default");
        assertEquals("interface", result);
    }

    @Test
    void testNoMatch() {
        Map<Class<?>, String> map = new HashMap<>();
        map.put(String.class, "string");

        String result = ClassUtilities.findClosest(Integer.class, map, "default");
        assertEquals("default", result);
    }

    @Test
    void testEmptyMap() {
        Map<Class<?>, String> map = new HashMap<>();
        String result = ClassUtilities.findClosest(BaseClass.class, map, "default");
        assertEquals("default", result);
    }

    @Test
    void testNullClass() {
        Map<Class<?>, String> map = new HashMap<>();
        assertThrows(IllegalArgumentException.class, () -> ClassUtilities.findClosest(null, map, "default"));
    }

    @Test
    void testNullMap() {
        assertThrows(IllegalArgumentException.class, () -> ClassUtilities.findClosest(BaseClass.class, null, "default"));
    }

    @Test
    void testMultipleInheritanceLevels() {
        Map<Class<?>, String> map = new HashMap<>();
        map.put(BaseClass.class, "base");
        map.put(MiddleClass.class, "middle");
        map.put(TestInterface.class, "interface");

        // Should find the closest match in the hierarchy
        String result = ClassUtilities.findClosest(SubClass.class, map, "default");
        assertEquals("middle", result);
    }

    @Test
    void testMultipleInterfaces() {
        Map<Class<?>, String> map = new HashMap<>();
        map.put(TestInterface.class, "parent-interface");
        map.put(SubInterface.class, "sub-interface");

        // Should find the closest interface
        String result = ClassUtilities.findClosest(SubClass.class, map, "default");
        assertEquals("sub-interface", result);
    }
}