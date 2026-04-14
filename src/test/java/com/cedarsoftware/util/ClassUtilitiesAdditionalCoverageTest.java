package com.cedarsoftware.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.cedarsoftware.util.convert.Converter;
import com.cedarsoftware.util.convert.DefaultConverterOptions;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Additional coverage tests for ClassUtilities — targets JaCoCo gaps not
 * already covered by ClassUtilitiesCoverageTest and other existing tests:
 * - forName with array notation (int[], String[], int[][])
 * - forName with primitive type names
 * - computeInheritanceDistance edge cases
 * - loadResourceAsString/Bytes failure paths
 * - newInstance with various arg shapes
 * - isPrimitive for wrapper types
 * - findClosest (ClassUtilities version)
 */
class ClassUtilitiesAdditionalCoverageTest {

    private final Converter converter = new Converter(new DefaultConverterOptions());

    // ========== forName — array notation ==========

    @Test
    void testForNameIntArray() {
        Class<?> c = ClassUtilities.forName("int[]", ClassUtilities.getClassLoader());
        assertThat(c).isEqualTo(int[].class);
    }

    @Test
    void testForNameStringArray() {
        Class<?> c = ClassUtilities.forName("java.lang.String[]", ClassUtilities.getClassLoader());
        assertThat(c).isEqualTo(String[].class);
    }

    @Test
    void testForNameMultiDimIntArray() {
        Class<?> c = ClassUtilities.forName("int[][]", ClassUtilities.getClassLoader());
        assertThat(c).isEqualTo(int[][].class);
    }

    @Test
    void testForNameBooleanArray() {
        Class<?> c = ClassUtilities.forName("boolean[]", ClassUtilities.getClassLoader());
        assertThat(c).isEqualTo(boolean[].class);
    }

    @Test
    void testForNameByteArray() {
        Class<?> c = ClassUtilities.forName("byte[]", ClassUtilities.getClassLoader());
        assertThat(c).isEqualTo(byte[].class);
    }

    @Test
    void testForNameCharArray() {
        Class<?> c = ClassUtilities.forName("char[]", ClassUtilities.getClassLoader());
        assertThat(c).isEqualTo(char[].class);
    }

    @Test
    void testForNameDoubleArray() {
        Class<?> c = ClassUtilities.forName("double[]", ClassUtilities.getClassLoader());
        assertThat(c).isEqualTo(double[].class);
    }

    @Test
    void testForNameFloatArray() {
        Class<?> c = ClassUtilities.forName("float[]", ClassUtilities.getClassLoader());
        assertThat(c).isEqualTo(float[].class);
    }

    @Test
    void testForNameLongArray() {
        Class<?> c = ClassUtilities.forName("long[]", ClassUtilities.getClassLoader());
        assertThat(c).isEqualTo(long[].class);
    }

    @Test
    void testForNameShortArray() {
        Class<?> c = ClassUtilities.forName("short[]", ClassUtilities.getClassLoader());
        assertThat(c).isEqualTo(short[].class);
    }

    // ========== forName — primitives ==========

    @Test
    void testForNamePrimitiveInt() {
        assertThat(ClassUtilities.forName("int", ClassUtilities.getClassLoader())).isEqualTo(int.class);
    }

    @Test
    void testForNamePrimitiveBoolean() {
        assertThat(ClassUtilities.forName("boolean", ClassUtilities.getClassLoader())).isEqualTo(boolean.class);
    }

    @Test
    void testForNamePrimitiveVoid() {
        assertThat(ClassUtilities.forName("void", ClassUtilities.getClassLoader())).isEqualTo(void.class);
    }

    @Test
    void testForNamePrimitiveLong() {
        assertThat(ClassUtilities.forName("long", ClassUtilities.getClassLoader())).isEqualTo(long.class);
    }

    @Test
    void testForNamePrimitiveDouble() {
        assertThat(ClassUtilities.forName("double", ClassUtilities.getClassLoader())).isEqualTo(double.class);
    }

    // ========== forName — normal classes ==========

    @Test
    void testForNameStandardClass() {
        Class<?> c = ClassUtilities.forName("java.lang.String", ClassUtilities.getClassLoader());
        assertThat(c).isEqualTo(String.class);
    }

    @Test
    void testForNameNonExistent() {
        Class<?> c = ClassUtilities.forName("foo.bar.NonExistent", ClassUtilities.getClassLoader());
        assertThat(c).isNull();
    }

    @Test
    void testForNameEmpty() {
        assertThat(ClassUtilities.forName("", ClassUtilities.getClassLoader())).isNull();
    }

    @Test
    void testForNameNull() {
        assertThat(ClassUtilities.forName(null, ClassUtilities.getClassLoader())).isNull();
    }

    // ========== computeInheritanceDistance ==========

    @Test
    void testInheritanceDistanceSameClass() {
        assertThat(ClassUtilities.computeInheritanceDistance(String.class, String.class)).isEqualTo(0);
    }

    @Test
    void testInheritanceDistanceParent() {
        int distance = ClassUtilities.computeInheritanceDistance(String.class, Object.class);
        assertThat(distance).isEqualTo(1);
    }

    @Test
    void testInheritanceDistanceUnrelated() {
        int distance = ClassUtilities.computeInheritanceDistance(String.class, Integer.class);
        assertThat(distance).isEqualTo(-1);
    }

    @Test
    void testInheritanceDistanceInterface() {
        int distance = ClassUtilities.computeInheritanceDistance(ArrayList.class, List.class);
        assertThat(distance).isGreaterThan(0);
    }

    @Test
    void testInheritanceDistancePrimitiveToWrapper() {
        // int → Integer should be considered 0 distance (wrap/unwrap)
        int distance = ClassUtilities.computeInheritanceDistance(Integer.class, Number.class);
        assertThat(distance).isEqualTo(1);
    }

    // ========== getClassLoader ==========

    @Test
    void testGetClassLoaderDefault() {
        assertThat(ClassUtilities.getClassLoader()).isNotNull();
    }

    @Test
    void testGetClassLoaderForClass() {
        assertThat(ClassUtilities.getClassLoader(String.class)).isNotNull();
    }

    @Test
    void testGetClassLoaderForAnchorClass() {
        assertThat(ClassUtilities.getClassLoader(ClassUtilitiesAdditionalCoverageTest.class)).isNotNull();
    }

    // ========== loadResource ==========

    @Test
    void testLoadResourceAsStringNonexistent() {
        assertThatThrownBy(() -> ClassUtilities.loadResourceAsString("nonexistent-" + System.nanoTime() + ".txt"))
                .isInstanceOf(Exception.class);
    }

    @Test
    void testLoadResourceAsBytesNonexistent() {
        assertThatThrownBy(() -> ClassUtilities.loadResourceAsBytes("nonexistent-" + System.nanoTime() + ".txt"))
                .isInstanceOf(Exception.class);
    }

    // ========== newInstance — various shapes ==========

    static class NoArgsClass {
        public String name = "default";
    }

    static class SingleArgClass {
        public String name;
        public SingleArgClass(String name) { this.name = name; }
    }

    static class TwoArgClass {
        public String name;
        public int value;
        public TwoArgClass(String name, int value) { this.name = name; this.value = value; }
    }

    @Test
    void testNewInstanceNoArgs() {
        Object obj = ClassUtilities.newInstance(converter, NoArgsClass.class, new ArrayList<>());
        assertThat(obj).isInstanceOf(NoArgsClass.class);
        NoArgsClass instance = (NoArgsClass) obj;
        assertThat(instance.name).isEqualTo("default");
    }

    @Test
    void testNewInstanceSingleArg() {
        Object obj = ClassUtilities.newInstance(converter, SingleArgClass.class,
                Arrays.asList("hello"));
        assertThat(obj).isInstanceOf(SingleArgClass.class);
        SingleArgClass instance = (SingleArgClass) obj;
        assertThat(instance.name).isEqualTo("hello");
    }

    @Test
    void testNewInstanceTwoArgs() {
        Object obj = ClassUtilities.newInstance(converter, TwoArgClass.class,
                Arrays.asList("foo", 42));
        assertThat(obj).isInstanceOf(TwoArgClass.class);
        TwoArgClass instance = (TwoArgClass) obj;
        assertThat(instance.name).isEqualTo("foo");
        assertThat(instance.value).isEqualTo(42);
    }

    @Test
    void testNewInstanceWithMap() {
        Map<String, Object> args = new HashMap<>();
        args.put("name", "test");
        args.put("value", 100);
        Object obj = ClassUtilities.newInstance(converter, TwoArgClass.class, args);
        assertThat(obj).isInstanceOf(TwoArgClass.class);
    }

    // ========== newInstance — interface rejection ==========

    @Test
    void testNewInstanceListInterfaceThrows() {
        assertThatThrownBy(() -> ClassUtilities.newInstance(converter, List.class, new ArrayList<>()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("interface");
    }

    @Test
    void testNewInstanceMapInterfaceThrows() {
        assertThatThrownBy(() -> ClassUtilities.newInstance(converter, Map.class, new ArrayList<>()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("interface");
    }

    @Test
    void testNewInstanceArrayList() {
        // ArrayList is concrete — should work
        Object obj = ClassUtilities.newInstance(converter, ArrayList.class, new ArrayList<>());
        assertThat(obj).isInstanceOf(ArrayList.class);
    }

    @Test
    void testNewInstanceHashMap() {
        Object obj = ClassUtilities.newInstance(converter, HashMap.class, new ArrayList<>());
        assertThat(obj).isInstanceOf(HashMap.class);
    }

    // ========== isPrimitive ==========

    @Test
    void testIsPrimitiveInt() {
        assertThat(ClassUtilities.isPrimitive(int.class)).isTrue();
    }

    @Test
    void testIsPrimitiveInteger() {
        assertThat(ClassUtilities.isPrimitive(Integer.class)).isTrue();
    }

    @Test
    void testIsPrimitiveString() {
        assertThat(ClassUtilities.isPrimitive(String.class)).isFalse();
    }

    @Test
    void testIsPrimitiveBoolean() {
        assertThat(ClassUtilities.isPrimitive(boolean.class)).isTrue();
        assertThat(ClassUtilities.isPrimitive(Boolean.class)).isTrue();
    }

    @Test
    void testIsPrimitiveAllPrimitives() {
        assertThat(ClassUtilities.isPrimitive(byte.class)).isTrue();
        assertThat(ClassUtilities.isPrimitive(short.class)).isTrue();
        assertThat(ClassUtilities.isPrimitive(int.class)).isTrue();
        assertThat(ClassUtilities.isPrimitive(long.class)).isTrue();
        assertThat(ClassUtilities.isPrimitive(float.class)).isTrue();
        assertThat(ClassUtilities.isPrimitive(double.class)).isTrue();
        assertThat(ClassUtilities.isPrimitive(char.class)).isTrue();
        assertThat(ClassUtilities.isPrimitive(boolean.class)).isTrue();
    }

    @Test
    void testIsPrimitiveAllWrappers() {
        assertThat(ClassUtilities.isPrimitive(Byte.class)).isTrue();
        assertThat(ClassUtilities.isPrimitive(Short.class)).isTrue();
        assertThat(ClassUtilities.isPrimitive(Integer.class)).isTrue();
        assertThat(ClassUtilities.isPrimitive(Long.class)).isTrue();
        assertThat(ClassUtilities.isPrimitive(Float.class)).isTrue();
        assertThat(ClassUtilities.isPrimitive(Double.class)).isTrue();
        assertThat(ClassUtilities.isPrimitive(Character.class)).isTrue();
        assertThat(ClassUtilities.isPrimitive(Boolean.class)).isTrue();
    }

    // ========== getClassIfEnum ==========

    enum MyEnum { A, B }

    @Test
    void testGetClassIfEnum() {
        Class<?> enumClass = ClassUtilities.getClassIfEnum(MyEnum.class);
        assertThat(enumClass).isEqualTo(MyEnum.class);
    }

    @Test
    void testGetClassIfEnumNonEnum() {
        Class<?> enumClass = ClassUtilities.getClassIfEnum(String.class);
        assertThat(enumClass).isNull();
    }

    // ========== findClosest ==========

    @Test
    void testFindClosest() {
        Map<Class<?>, String> map = new HashMap<>();
        map.put(Number.class, "number");
        map.put(Object.class, "object");

        String result = ClassUtilities.findClosest(Integer.class, map, "default");
        assertThat(result).isEqualTo("number");
    }

    @Test
    void testFindClosestNoMatch() {
        Map<Class<?>, String> map = new HashMap<>();
        map.put(Integer.class, "integer");

        String result = ClassUtilities.findClosest(String.class, map, "default");
        assertThat(result).isEqualTo("default");
    }

    @Test
    void testFindClosestExactMatch() {
        Map<Class<?>, String> map = new HashMap<>();
        map.put(String.class, "string");
        map.put(Object.class, "object");

        String result = ClassUtilities.findClosest(String.class, map, "default");
        assertThat(result).isEqualTo("string");
    }

}
