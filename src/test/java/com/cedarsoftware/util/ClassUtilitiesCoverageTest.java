package com.cedarsoftware.util;

import com.cedarsoftware.util.convert.Converter;
import com.cedarsoftware.util.convert.DefaultConverterOptions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ClassUtilitiesCoverageTest {

    enum OuterEnum { A; static class Inner {} }

    static class FailingCtor {
        private FailingCtor() { throw new IllegalStateException("fail"); }
    }

    private Converter converter;

    @BeforeEach
    void setup() {
        converter = new Converter(new DefaultConverterOptions());
        ClassUtilities.setUseUnsafe(false);
    }

    @AfterEach
    void tearDown() {
        ClassUtilities.setUseUnsafe(false);
    }

    @Test
    void testDoesOneWrapTheOther() {
        assertTrue(ClassUtilities.doesOneWrapTheOther(Integer.class, int.class));
        assertTrue(ClassUtilities.doesOneWrapTheOther(int.class, Integer.class));
        assertFalse(ClassUtilities.doesOneWrapTheOther(Integer.class, long.class));
        assertFalse(ClassUtilities.doesOneWrapTheOther(null, Integer.class));
    }

    @Test
    void testClassHierarchyInfoDepthAndDistances() {
        ClassUtilities.ClassHierarchyInfo info1 = ClassUtilities.getClassHierarchyInfo(ArrayList.class);
        ClassUtilities.ClassHierarchyInfo info2 = ClassUtilities.getClassHierarchyInfo(ArrayList.class);
        assertSame(info1, info2);
        assertEquals(3, info1.getDepth());
        Map<Class<?>, Integer> map = info1.getDistanceMap();
        assertEquals(0, map.get(ArrayList.class));
        assertEquals(1, map.get(AbstractList.class));
        assertEquals(1, map.get(List.class));
        assertEquals(3, map.get(Object.class));
        assertFalse(map.containsKey(Map.class));
    }

    @Test
    void testGetPrimitiveFromWrapper() {
        assertEquals(int.class, ClassUtilities.getPrimitiveFromWrapper(Integer.class));
        assertNull(ClassUtilities.getPrimitiveFromWrapper(String.class));
        assertThrows(IllegalArgumentException.class, () -> ClassUtilities.getPrimitiveFromWrapper(null));
    }

    @Test
    void testIndexOfSmallestValue() {
        assertEquals(1, ClassUtilities.indexOfSmallestValue(new int[]{5, 1, 3}));
        assertEquals(-1, ClassUtilities.indexOfSmallestValue(new int[]{}));
        assertEquals(-1, ClassUtilities.indexOfSmallestValue(null));
    }

    @Test
    void testGetClassIfEnum() {
        assertEquals(OuterEnum.class, ClassUtilities.getClassIfEnum(OuterEnum.class));
        assertEquals(OuterEnum.class, ClassUtilities.getClassIfEnum(OuterEnum.Inner.class));
        assertNull(ClassUtilities.getClassIfEnum(String.class));
    }

    @Test
    void testSecurityChecks() {
        assertTrue(ClassUtilities.SecurityChecker.isSecurityBlocked(Process.class));
        assertFalse(ClassUtilities.SecurityChecker.isSecurityBlocked(String.class));
        assertTrue(ClassUtilities.SecurityChecker.isSecurityBlockedName("java.lang.ProcessImpl"));
        assertFalse(ClassUtilities.SecurityChecker.isSecurityBlockedName("java.lang.String"));
        assertThrows(SecurityException.class,
                () -> ClassUtilities.SecurityChecker.verifyClass(System.class));
        assertDoesNotThrow(() -> ClassUtilities.SecurityChecker.verifyClass(String.class));
    }

    static class MapClsLoader extends ClassLoader {
        private final String name;
        private final byte[] data;
        MapClsLoader(String name, byte[] data) {
            super(null);
            this.name = name;
            this.data = data;
        }
        @Override
        public java.io.InputStream getResourceAsStream(String res) {
            if (name.equals(res)) {
                return new java.io.ByteArrayInputStream(data);
            }
            return null;
        }
    }

    @Test
    void testLoadResourceAsString() {
        String resName = "resource.txt";
        byte[] bytes = "hello".getBytes(StandardCharsets.UTF_8);
        ClassLoader prev = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(new MapClsLoader(resName, bytes));
            String out = ClassUtilities.loadResourceAsString(resName);
            assertEquals("hello", out);
        } finally {
            Thread.currentThread().setContextClassLoader(prev);
        }
    }

    @Test
    void testSetUseUnsafe() {
        ClassUtilities.setUseUnsafe(false);
        assertThrows(IllegalArgumentException.class,
                () -> ClassUtilities.newInstance(converter, FailingCtor.class, null));

        ClassUtilities.setUseUnsafe(true);
        Object obj = ClassUtilities.newInstance(converter, FailingCtor.class, null);
        assertNotNull(obj);
    }
}

