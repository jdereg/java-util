package com.cedarsoftware.util;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Stream;

import com.cedarsoftware.util.convert.Converter;
import com.cedarsoftware.util.convert.DefaultConverterOptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClassUtilitiesTest {
    // Example classes and interfaces for testing
    interface TestInterface {}
    interface SubInterface extends TestInterface {}
    static class TestClass {}
    private static class SubClass extends TestClass implements TestInterface {}
    private static class AnotherClass {}
    private Converter converter;

    // Test classes
    static class NoArgConstructor {
        public NoArgConstructor() {}
    }

    static class SingleArgConstructor {
        private final String value;
        public SingleArgConstructor(String value) {
            this.value = value;
        }
        public String getValue() { return value; }
    }

    static class MultiArgConstructor {
        private final String str;
        private final int num;
        public MultiArgConstructor(String str, int num) {
            this.str = str;
            this.num = num;
        }
        public String getStr() { return str; }
        public int getNum() { return num; }
    }

    static class OverloadedConstructors {
        private final String value;
        private final int number;

        public OverloadedConstructors() {
            this("default", 0);
        }

        public OverloadedConstructors(String value) {
            this(value, 0);
        }

        public OverloadedConstructors(String value, int number) {
            this.value = value;
            this.number = number;
        }

        public String getValue() { return value; }
        public int getNumber() { return number; }
    }

    static class PrivateConstructor {
        private String value;
        private PrivateConstructor(String value) {
            this.value = value;
        }
        public String getValue() { return value; }
    }

    static class PrimitiveConstructor {
        private final int intValue;
        private final boolean boolValue;

        public PrimitiveConstructor(int intValue, boolean boolValue) {
            this.intValue = intValue;
            this.boolValue = boolValue;
        }

        public int getIntValue() { return intValue; }
        public boolean getBoolValue() { return boolValue; }
    }
    
    @BeforeEach
    void setUp() {
        converter = new Converter(new DefaultConverterOptions());
    }

    @Test
    @DisplayName("Should create instance with no-arg constructor")
    void shouldCreateInstanceWithNoArgConstructor() {
        Object instance = ClassUtilities.newInstance(converter, NoArgConstructor.class, null);
        assertNotNull(instance);
        assertInstanceOf(NoArgConstructor.class, instance);
    }

    @Test
    @DisplayName("Should create instance with single argument")
    void shouldCreateInstanceWithSingleArgument() {
        List<Object> args = Collections.singletonList("test");
        Object instance = ClassUtilities.newInstance(converter, SingleArgConstructor.class, args);

        assertNotNull(instance);
        assertInstanceOf(SingleArgConstructor.class, instance);
        assertEquals("test", ((SingleArgConstructor) instance).getValue());
    }

    @Test
    @DisplayName("Should create instance with multiple arguments")
    void shouldCreateInstanceWithMultipleArguments() {
        List<Object> args = Arrays.asList("test", 42);
        Object instance = ClassUtilities.newInstance(converter, MultiArgConstructor.class, args);

        assertNotNull(instance);
        assertInstanceOf(MultiArgConstructor.class, instance);
        MultiArgConstructor mac = (MultiArgConstructor) instance;
        assertEquals("test", mac.getStr());
        assertEquals(42, mac.getNum());
    }

    @Test
    @DisplayName("Should handle private constructors")
    void shouldHandlePrivateConstructors() {
        List<Object> args = Collections.singletonList("private");
        Object instance = ClassUtilities.newInstance(converter, PrivateConstructor.class, args);

        assertNotNull(instance);
        assertInstanceOf(PrivateConstructor.class, instance);
        assertEquals("private", ((PrivateConstructor) instance).getValue());
    }

    @Test
    @DisplayName("Should handle primitive parameters with null arguments")
    void shouldHandlePrimitiveParametersWithNullArguments() {
        Object instance = ClassUtilities.newInstance(converter, PrimitiveConstructor.class, null);

        assertNotNull(instance);
        assertInstanceOf(PrimitiveConstructor.class, instance);
        PrimitiveConstructor pc = (PrimitiveConstructor) instance;
        assertEquals(0, pc.getIntValue());  // default int value
        assertFalse(pc.getBoolValue());     // default boolean value
    }

    @Test
    @DisplayName("Should choose best matching constructor with overloads")
    void shouldChooseBestMatchingConstructor() {
        List<Object> args = Arrays.asList("custom", 42);
        Object instance = ClassUtilities.newInstance(converter, OverloadedConstructors.class, args);

        assertNotNull(instance);
        assertInstanceOf(OverloadedConstructors.class, instance);
        OverloadedConstructors oc = (OverloadedConstructors) instance;
        assertEquals("custom", oc.getValue());
        assertEquals(42, oc.getNumber());
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException for security-sensitive classes")
    void shouldThrowExceptionForSecuritySensitiveClasses() {
        Class<?>[] sensitiveClasses = {
                ProcessBuilder.class,
                Process.class,
                ClassLoader.class,
                Constructor.class,
                Method.class,
                Field.class
        };

        for (Class<?> sensitiveClass : sensitiveClasses) {
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> ClassUtilities.newInstance(converter, sensitiveClass, null)
            );
            assertTrue(exception.getMessage().contains("not"));
            assertInstanceOf(IllegalArgumentException.class, exception);
        }
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException for interfaces")
    void shouldThrowExceptionForInterfaces() {
        assertThrows(IllegalArgumentException.class,
                () -> ClassUtilities.newInstance(converter, Runnable.class, null));
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException for null class")
    void shouldThrowExceptionForNullClass() {
        assertThrows(IllegalArgumentException.class,
                () -> ClassUtilities.newInstance(converter, null, null));
    }

    @ParameterizedTest
    @MethodSource("provideArgumentMatchingCases")
    @DisplayName("Should match constructor arguments correctly")
    void shouldMatchConstructorArgumentsCorrectly(Class<?> clazz, List<Object> args, Object[] expectedValues) {
        Object instance = ClassUtilities.newInstance(converter, clazz, args);
        assertNotNull(instance);
        assertArrayEquals(expectedValues, getValues(instance));
    }

    private static Stream<Arguments> provideArgumentMatchingCases() {
        return Stream.of(
                Arguments.of(
                        MultiArgConstructor.class,
                        Arrays.asList("test", 42),
                        new Object[]{"test", 42}
                ),
                Arguments.of(
                        MultiArgConstructor.class,
                        Arrays.asList(42, "test"),  // wrong order, should still match
                        new Object[]{"test", 42}
                ),
                Arguments.of(
                        MultiArgConstructor.class,
                        Collections.singletonList("test"),  // partial args
                        new Object[]{"test", 0}  // default int value
                )
        );
    }

    private Object[] getValues(Object instance) {
        if (instance instanceof MultiArgConstructor) {
            MultiArgConstructor mac = (MultiArgConstructor) instance;
            return new Object[]{mac.getStr(), mac.getNum()};
        }
        throw new IllegalArgumentException("Unsupported test class");
    }

    @Test
    void testComputeInheritanceDistanceWithNulls() {
        assertEquals(-1, ClassUtilities.computeInheritanceDistance(null, null));
        assertEquals(-1, ClassUtilities.computeInheritanceDistance(String.class, null));
        assertEquals(-1, ClassUtilities.computeInheritanceDistance(null, Object.class));
    }

    @Test
    void testComputeInheritanceDistanceWithSameClass() {
        assertEquals(0, ClassUtilities.computeInheritanceDistance(String.class, String.class));
        assertEquals(0, ClassUtilities.computeInheritanceDistance(Object.class, Object.class));
    }

    @Test
    void testComputeInheritanceDistanceWithSuperclass() {
        assertEquals(1, ClassUtilities.computeInheritanceDistance(String.class, Object.class));
        assertEquals(1, ClassUtilities.computeInheritanceDistance(Integer.class, Number.class));
    }

    @Test
    void testComputeInheritanceDistanceWithInterface() {
        assertEquals(1, ClassUtilities.computeInheritanceDistance(ArrayList.class, List.class));
        assertEquals(2, ClassUtilities.computeInheritanceDistance(HashSet.class, Collection.class));
    }

    @Test
    void testComputeInheritanceDistanceUnrelatedClasses() {
        assertEquals(-1, ClassUtilities.computeInheritanceDistance(String.class, List.class));
        assertEquals(-1, ClassUtilities.computeInheritanceDistance(HashMap.class, List.class));
    }

    @Test
    void testIsPrimitive() {
        assertTrue(ClassUtilities.isPrimitive(int.class));
        assertTrue(ClassUtilities.isPrimitive(Integer.class));
        assertFalse(ClassUtilities.isPrimitive(String.class));
    }
    
    @Test
    public void testClassToClassDirectInheritance() {
        assertEquals(1, ClassUtilities.computeInheritanceDistance(SubClass.class, TestClass.class),
                "Direct class to class inheritance should have a distance of 1.");
    }

    @Test
    public void testClassToClassNoInheritance() {
        assertEquals(-1, ClassUtilities.computeInheritanceDistance(TestClass.class, AnotherClass.class),
                "No inheritance between classes should return -1.");
    }

    @Test
    public void testClassToInterfaceDirectImplementation() {
        assertEquals(1, ClassUtilities.computeInheritanceDistance(SubClass.class, TestInterface.class),
                "Direct class to interface implementation should have a distance of 1.");
    }

    @Test
    public void testClassToInterfaceNoImplementation() {
        assertEquals(-1, ClassUtilities.computeInheritanceDistance(TestClass.class, TestInterface.class),
                "No implementation of the interface by the class should return -1.");
    }

    @Test
    public void testInterfaceToClass() {
        assertEquals(-1, ClassUtilities.computeInheritanceDistance(TestInterface.class, TestClass.class),
                "Interface to class should always return -1 as interfaces cannot inherit from classes.");
    }

    @Test
    public void testInterfaceToInterfaceDirectInheritance() {
        assertEquals(1, ClassUtilities.computeInheritanceDistance(SubInterface.class, TestInterface.class),
                "Direct interface to interface inheritance should have a distance of 1.");
    }

    @Test
    public void testInterfaceToInterfaceNoInheritance() {
        assertEquals(-1, ClassUtilities.computeInheritanceDistance(TestInterface.class, SubInterface.class),
                "No inheritance between interfaces should return -1.");
    }

    @Test
    public void testSameClass() {
        assertEquals(0, ClassUtilities.computeInheritanceDistance(TestClass.class, TestClass.class),
                "Distance from a class to itself should be 0.");
    }

    @Test
    public void testSameInterface() {
        assertEquals(0, ClassUtilities.computeInheritanceDistance(TestInterface.class, TestInterface.class),
                "Distance from an interface to itself should be 0.");
    }

    @Test
    public void testWithNullSource() {
        assertEquals(-1, ClassUtilities.computeInheritanceDistance(null, TestClass.class),
                "Should return -1 when source is null.");
    }

    @Test
    public void testWithNullDestination() {
        assertEquals(-1, ClassUtilities.computeInheritanceDistance(TestClass.class, null),
                "Should return -1 when destination is null.");
    }

    @Test
    public void testWithBothNull() {
        assertEquals(-1, ClassUtilities.computeInheritanceDistance(null, null),
                "Should return -1 when both source and destination are null.");
    }

    @Test
    public void testPrimitives() {
        assert 0 == ClassUtilities.computeInheritanceDistance(byte.class, Byte.TYPE);
        assert 0 == ClassUtilities.computeInheritanceDistance(Byte.TYPE, byte.class);
        assert 0 == ClassUtilities.computeInheritanceDistance(Byte.TYPE, Byte.class);
        assert 0 == ClassUtilities.computeInheritanceDistance(Byte.class, Byte.TYPE);
        assert 0 == ClassUtilities.computeInheritanceDistance(Byte.class, byte.class);
        assert 0 == ClassUtilities.computeInheritanceDistance(int.class, Integer.class);
        assert 0 == ClassUtilities.computeInheritanceDistance(Integer.class, int.class);

        assert -1 == ClassUtilities.computeInheritanceDistance(Byte.class, int.class);
        assert -1 == ClassUtilities.computeInheritanceDistance(int.class, Byte.class);
        assert -1 == ClassUtilities.computeInheritanceDistance(int.class, String.class);
        assert -1 == ClassUtilities.computeInheritanceDistance(int.class, String.class);
        assert -1 == ClassUtilities.computeInheritanceDistance(Short.TYPE, Integer.TYPE);
        assert -1 == ClassUtilities.computeInheritanceDistance(String.class, Integer.TYPE);

        assert -1 == ClassUtilities.computeInheritanceDistance(Date.class, java.sql.Date.class);
        assert 1 == ClassUtilities.computeInheritanceDistance(java.sql.Date.class, Date.class);
    }

    @Test
    public void testClassForName()
    {
        Class<?> testObjectClass = ClassUtilities.forName(SubClass.class.getName(), ClassUtilities.class.getClassLoader());
        assert testObjectClass instanceof Class<?>;
        assert SubClass.class.getName().equals(testObjectClass.getName());
    }

    @Test
    public void testClassForNameWithClassloader()
    {
        Class<?> testObjectClass = ClassUtilities.forName("ReallyLong", new AlternateNameClassLoader("ReallyLong", Long.class));
        assert testObjectClass instanceof Class<?>;
        assert "java.lang.Long".equals(testObjectClass.getName());
    }

    @Test
    public void testClassForNameNullClassErrorHandling()
    {
        assert null == ClassUtilities.forName(null, ClassUtilities.class.getClassLoader());
        assert null == ClassUtilities.forName("Smith&Wesson", ClassUtilities.class.getClassLoader());
    }

    @Test
    public void testClassForNameFailOnClassLoaderErrorTrue()
    {
        assert null == ClassUtilities.forName("foo.bar.baz.Qux", ClassUtilities.class.getClassLoader());
    }

    @Test
    public void testClassForNameFailOnClassLoaderErrorFalse()
    {
        Class<?> testObjectClass = ClassUtilities.forName("foo.bar.baz.Qux", ClassUtilities.class.getClassLoader());
        assert testObjectClass == null;
    }

    @Test
    public void testClassUtilitiesAliases()
    {
        ClassUtilities.addPermanentClassAlias(HashMap.class, "mapski");
        Class<?> x = ClassUtilities.forName("mapski", ClassUtilities.class.getClassLoader());
        assert HashMap.class == x;

        ClassUtilities.removePermanentClassAlias("mapski");
        x = ClassUtilities.forName("mapski", ClassUtilities.class.getClassLoader());
        assert x == null;
    }

    private static class AlternateNameClassLoader extends ClassLoader
    {
        AlternateNameClassLoader(String alternateName, Class<?> clazz)
        {
            super(AlternateNameClassLoader.class.getClassLoader());
            this.alternateName = alternateName;
            this.clazz = clazz;
        }

        public Class<?> loadClass(String className)
        {
            return findClass(className);
        }

        protected Class<?> findClass(String className)
        {
            try
            {
                return findSystemClass(className);
            }
            catch (Exception ignored)
            { }

            if (alternateName.equals(className))
            {
                return Long.class;
            }

            return null;
        }

        private final String alternateName;
        private final Class<?> clazz;
    }
}
