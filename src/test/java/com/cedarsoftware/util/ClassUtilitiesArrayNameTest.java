package com.cedarsoftware.util;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * forName() accepts three spellings of an array class: the JVM's own ({@code "[Ljava.lang.String;"}), the
 * JVM's internal form with '/' separators ({@code "[Ljava/lang/String;"}), and the Java form
 * ({@code "java.lang.String[]"}). Its parser was loose: it ignored anything after a primitive's letter
 * ({@code "[Ixyz"} was int[]), accepted a mix of the forms ({@code "[I[]"}, {@code "[Ljava.lang.String;[]"})
 * and of the separators ({@code "[Ljava/lang.String;"}) -- none of which the JDK accepts -- and cached each
 * spelling for good. So one class could fill the name cache without bound: 8 classes, in array dimensions and
 * separator mixes, left 17,340 entries that were never freed, and the junk-suffix family has no end at all.
 * Malformed names are now refused, which leaves at most three spellings per array class.
 */
class ClassUtilitiesArrayNameTest {

    private static final ClassLoader LOADER = ClassUtilitiesArrayNameTest.class.getClassLoader();

    @BeforeEach
    void clearCaches() {
        ClassUtilities.clearCaches();
    }

    private static void assertRefused(String name) {
        assertNull(ClassUtilities.forName(name, null), name + " (default loader)");
        assertNull(ClassUtilities.forName(name, LOADER), name + " (explicit loader)");
    }

    private static void assertResolves(Class<?> expected, String name) {
        assertEquals(expected, ClassUtilities.forName(name, null), name + " (default loader)");
        assertEquals(expected, ClassUtilities.forName(name, LOADER), name + " (explicit loader)");
    }

    private static String brackets(int n) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < n; i++) {
            sb.append('[');
        }
        return sb.toString();
    }

    private static String javaDims(int n) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < n; i++) {
            sb.append("[]");
        }
        return sb.toString();
    }

    @Test
    void anythingAfterAPrimitiveLetterIsRefused() {
        assertRefused("[Ixyz");
        assertRefused("[I;");
        assertRefused("[[Jfoo");
        assertRefused("[ZZ");
        for (int i = 0; i < 1000; i++) {
            assertRefused("[I" + i);
        }
    }

    @Test
    void mixedArrayFormsAreRefused() {
        assertRefused("[I[]");
        assertRefused("[Ljava.lang.String;[]");
        assertRefused("[[Ljava/lang/String;[][]");
        assertRefused("java.lang.String[]]");
        assertRefused("java.lang.String[[]");
        assertRefused("java/lang/String[]");
        assertRefused("[]");
    }

    @Test
    void malformedObjectDescriptorsAreRefused() {
        assertRefused("[Ljava/lang.String;");
        assertRefused("[Ljava.lang/String;");
        assertRefused("[L;");
        assertRefused("[L[I;");
        assertRefused("[Ljava.lang.String;;");
        assertRefused("[Ljava.lang.String;x");
        assertRefused("[Lint;");
    }

    @Test
    void theJvmsDimensionLimitIsKept() {
        assertResolves(arrayClass(int.class, 255), brackets(255) + "I");
        assertResolves(arrayClass(String.class, 255), "java.lang.String" + javaDims(255));
        assertRefused(brackets(256) + "I");
        assertRefused(brackets(256) + "Ljava.lang.String;");
        assertRefused("int" + javaDims(256));
    }

    @Test
    void theThreeSpellingsStillResolve() {
        assertResolves(int[].class, "[I");
        assertResolves(int[][].class, "int[][]");
        assertResolves(boolean[].class, "boolean[]");
        assertResolves(String[].class, "[Ljava.lang.String;");
        assertResolves(String[].class, "[Ljava/lang/String;");
        assertResolves(String[].class, "java.lang.String[]");
        assertResolves(String[][][].class, "[[[Ljava.lang.String;");
        assertResolves(String[][][].class, "java.lang.String[][][]");
        assertResolves(Map.Entry[].class, "[Ljava.util.Map$Entry;");
        assertResolves(Map.Entry[].class, "[Ljava/util/Map$Entry;");
        assertResolves(Map.Entry[][].class, "java.util.Map$Entry[][]");
        // Repeated lookups come from the cache and agree
        assertResolves(String[].class, "[Ljava/lang/String;");
        assertResolves(String[].class, "java.lang.String[]");
    }

    @Test
    void aMissingElementIsNotFound() {
        assertRefused("[Lcom.example.nope.Missing;");
        assertRefused("com.example.nope.Missing[]");
    }

    private static Class<?> arrayClass(Class<?> element, int dims) {
        Class<?> c = element;
        for (int i = 0; i < dims; i++) {
            c = java.lang.reflect.Array.newInstance(c, 0).getClass();
        }
        return c;
    }
}
