package com.cedarsoftware.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClassUtilitiesTest {
    // Example classes and interfaces for testing
    interface TestInterface {}
    interface SubInterface extends TestInterface {}
    static class TestClass {}
    static class SubClass extends TestClass implements TestInterface {}
    static class AnotherClass {}

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
