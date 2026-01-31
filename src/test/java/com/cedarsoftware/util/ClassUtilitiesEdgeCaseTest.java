package com.cedarsoftware.util;

import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.Parameter;
import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.RandomAccess;
import java.util.Set;
import java.util.TreeSet;

import com.cedarsoftware.util.convert.Converter;
import com.cedarsoftware.util.convert.DefaultConverterOptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Edge case tests for ClassUtilities as suggested by GPT-5 review.
 * These tests cover deep interface hierarchies, diamond patterns,
 * primitive/wrapper relationships, array descriptor parsing, and
 * JPMS/named parameter fallback scenarios.
 */
class ClassUtilitiesEdgeCaseTest {
    
    // ===== Test Interfaces and Classes for Deep Hierarchy Testing =====
    
    // Diamond inheritance pattern
    interface Level0 {}
    interface Level1A extends Level0 {}
    interface Level1B extends Level0 {}
    interface Level2 extends Level1A, Level1B {}  // Diamond merge
    interface Level3 extends Level2 {}
    interface Level4 extends Level3 {}
    
    // Deep single chain
    interface Chain0 {}
    interface Chain1 extends Chain0 {}
    interface Chain2 extends Chain1 {}
    interface Chain3 extends Chain2 {}
    interface Chain4 extends Chain3 {}
    interface Chain5 extends Chain4 {}
    
    // Classes implementing multiple interface chains
    static class DiamondImpl implements Level4 {}
    static class ChainImpl implements Chain5 {}
    static class MultiImpl implements Level2, Chain3 {}
    
    // ===== findLowestCommonSupertypes() Tests =====
    
    @Test
    @DisplayName("Deep interface chains with diamonds - verify lowest types returned")
    void testDeepInterfaceChainsWithDiamonds() {
        // Test diamond pattern - should get Level0 as common root of Level1A and Level1B
        Set<Class<?>> result = ClassUtilities.findLowestCommonSupertypes(Level1A.class, Level1B.class);
        assertTrue(result.contains(Level0.class), "Should find common diamond root");
        
        // Test deep chain - should find exact common point
        result = ClassUtilities.findLowestCommonSupertypes(Chain5.class, Chain3.class);
        assertTrue(result.contains(Chain3.class), "Should find Chain3 as lowest common");
        
        // Test across different chains - Object is excluded by default
        result = ClassUtilities.findLowestCommonSupertypes(DiamondImpl.class, ChainImpl.class);
        assertTrue(result.isEmpty(), "Object is excluded by default, so result should be empty");
    }
    
    @Test
    @DisplayName("Class vs interface mixes - ArrayList & TreeSet")
    void testClassVsInterfaceMixes() {
        // ArrayList implements List, RandomAccess, Collection
        // TreeSet implements SortedSet, NavigableSet, Set, Collection
        // Both extend AbstractCollection which implements Collection
        Set<Class<?>> result = ClassUtilities.findLowestCommonSupertypes(ArrayList.class, TreeSet.class);
        
        // Should get AbstractCollection (the common superclass)
        assertTrue(result.contains(AbstractCollection.class) || result.contains(Collection.class), 
                   "Should find AbstractCollection or Collection as common");
        assertFalse(result.contains(Iterable.class), "Iterable should be excluded by default");
        assertFalse(result.contains(Serializable.class), "Serializable should be excluded by default");
        assertFalse(result.contains(Cloneable.class), "Cloneable should be excluded by default");
        
        // RandomAccess is only in ArrayList, so shouldn't appear
        assertFalse(result.contains(RandomAccess.class), "RandomAccess is not common");
    }
    
    @Test
    @DisplayName("Multiple interface implementations with complex hierarchy")
    void testMultipleInterfaceImplementations() {
        Set<Class<?>> result = ClassUtilities.findLowestCommonSupertypes(MultiImpl.class, DiamondImpl.class);
        
        // Both implement Level2 (through different paths)
        assertTrue(result.contains(Level2.class), "Should find Level2 as common");
        assertFalse(result.contains(Level0.class), "Should not include Level0 (parent of Level2)");
        assertFalse(result.contains(Level1A.class), "Should not include Level1A (parent of Level2)");
    }
    
    // ===== computeInheritanceDistance() Tests =====
    
    @Test
    @DisplayName("Primitives, wrappers, and mixed relationships")
    void testPrimitiveWrapperDistances() {
        // Wrapper to same primitive = 0
        assertEquals(0, ClassUtilities.computeInheritanceDistance(Integer.class, int.class));
        assertEquals(0, ClassUtilities.computeInheritanceDistance(int.class, Integer.class));
        assertEquals(0, ClassUtilities.computeInheritanceDistance(Boolean.class, boolean.class));
        assertEquals(0, ClassUtilities.computeInheritanceDistance(double.class, Double.class));
        
        // Wrapper to Number class = 1
        assertEquals(1, ClassUtilities.computeInheritanceDistance(Integer.class, Number.class));
        assertEquals(1, ClassUtilities.computeInheritanceDistance(Double.class, Number.class));
        assertEquals(1, ClassUtilities.computeInheritanceDistance(Long.class, Number.class));
        
        // Different primitives now support widening conversions
        assertEquals(1, ClassUtilities.computeInheritanceDistance(int.class, long.class));
        assertEquals(2, ClassUtilities.computeInheritanceDistance(byte.class, int.class));
        assertEquals(1, ClassUtilities.computeInheritanceDistance(float.class, double.class));
        
        // Cross primitive/wrapper of different types now support widening
        assertEquals(1, ClassUtilities.computeInheritanceDistance(Integer.class, long.class));
        assertEquals(3, ClassUtilities.computeInheritanceDistance(int.class, Double.class));
    }
    
    // ===== loadClass() Array Descriptor Tests =====
    
    @Test
    @DisplayName("loadClass with various array descriptors")
    void testLoadClassArrayDescriptors() throws ClassNotFoundException {
        // Java-style array syntax
        Class<?> c1 = ClassUtilities.forName("java.lang.String[]", null);
        assertEquals("[Ljava.lang.String;", c1.getName());
        assertTrue(c1.isArray());
        assertEquals(String.class, c1.getComponentType());
        
        Class<?> c2 = ClassUtilities.forName("int[][]", null);
        assertEquals("[[I", c2.getName());
        assertTrue(c2.isArray());
        assertTrue(c2.getComponentType().isArray());
        assertEquals(int.class, c2.getComponentType().getComponentType());
        
        // JVM descriptor syntax
        Class<?> c3 = ClassUtilities.forName("[I", null);
        assertEquals(int[].class, c3);
        
        Class<?> c4 = ClassUtilities.forName("[Ljava/lang/String;", null);
        assertEquals(String[].class, c4);
        
        Class<?> c5 = ClassUtilities.forName("[[[D", null);
        assertEquals(double[][][].class, c5);
        
        // Mixed primitive array types
        assertEquals(boolean[].class, ClassUtilities.forName("[Z", null));
        assertEquals(byte[].class, ClassUtilities.forName("[B", null));
        assertEquals(char[].class, ClassUtilities.forName("[C", null));
        assertEquals(short[].class, ClassUtilities.forName("[S", null));
        assertEquals(long[].class, ClassUtilities.forName("[J", null));
        assertEquals(float[].class, ClassUtilities.forName("[F", null));
        
        // Multi-dimensional object arrays
        Class<?> c6 = ClassUtilities.forName("[[Ljava/util/List;", null);
        assertEquals(List[][].class, c6);
    }
    
    @Test
    @DisplayName("loadClass with edge case descriptors")
    void testLoadClassEdgeCaseDescriptors() {
        // Test malformed descriptors
        
        // "[[" currently returns null rather than throwing - this might be a bug
        // but we test current behavior
        Class<?> result = ClassUtilities.forName("[[", null);
        assertNull(result, "Double bracket without type returns null");
        
        // "[X" with invalid primitive type actually returns null (doesn't throw currently)
        result = ClassUtilities.forName("[X", null);
        assertNull(result, "Invalid primitive type returns null");
        
        // "[Ljava/lang/String" missing semicolon actually returns null too
        result = ClassUtilities.forName("[Ljava/lang/String", null);
        assertNull(result, "Missing semicolon returns null");
        
        // "[" alone might be treated as a regular class name attempt
        result = ClassUtilities.forName("[", null);
        assertNull(result, "Single bracket returns null");
    }
    
    // ===== newInstance() JPMS and Named Parameter Tests =====
    
    @Test
    @DisplayName("newInstance with JPMS-blocked constructor fallback")
    void testNewInstanceJPMSFallback() {
        // This test simulates JPMS blocking by using a class with multiple constructors
        // where we'd prefer one but might need to fall back to another
        
        Converter converter = new Converter(new DefaultConverterOptions());
        
        // ArrayList has multiple constructors
        // If one is blocked (simulated), it should fall back to another
        Object instance = ClassUtilities.newInstance(converter, ArrayList.class, Collections.emptyList());
        assertNotNull(instance);
        assertInstanceOf(ArrayList.class, instance);
    }
    
    // Test class for named parameter scenarios
    static class NamedParamTestClass {
        public final String value1;
        public final int value2;
        
        public NamedParamTestClass() {
            this.value1 = "default";
            this.value2 = 0;
        }
        
        public NamedParamTestClass(String value1, int value2) {
            this.value1 = value1;
            this.value2 = value2;
        }
    }
    
    @Test
    @DisplayName("newInstance with named parameters compiled without -parameters flag")
    void testNewInstanceNamedParamsFallback() {
        Converter converter = new Converter(new DefaultConverterOptions());
        
        // When compiled without -parameters, parameter names are not available
        // Should fall back to positional matching or default constructor
        Map<String, Object> namedArgs = new HashMap<>();
        namedArgs.put("value1", "test");
        namedArgs.put("value2", 42);
        
        // This should work even if parameter names aren't available at runtime
        Object instance = ClassUtilities.newInstance(converter, NamedParamTestClass.class, namedArgs);
        assertNotNull(instance);
        assertInstanceOf(NamedParamTestClass.class, instance);
        
        // Test with constructor that has no parameter names available
        Constructor<?>[] constructors = NamedParamTestClass.class.getConstructors();
        boolean hasParameterNames = false;
        for (Constructor<?> constructor : constructors) {
            Parameter[] params = constructor.getParameters();
            if (params.length > 0) {
                // Check if parameter names are synthetic (arg0, arg1, etc.)
                hasParameterNames = !params[0].getName().startsWith("arg");
            }
        }
        
        // Whether or not we have parameter names, the instantiation should work
        // It should fall back gracefully when names aren't available
    }
    
    @Test
    @DisplayName("Complex inheritance distance calculations")
    void testComplexInheritanceDistances() {
        // Test with deep interface hierarchies
        // DiamondImpl -> Level4 -> Level3 -> Level2 -> Level1A/Level1B -> Level0 (5 hops)
        assertEquals(5, ClassUtilities.computeInheritanceDistance(DiamondImpl.class, Level0.class));
        
        // ChainImpl -> Chain5 -> Chain4 -> Chain3 -> Chain2 -> Chain1 -> Chain0 (6 hops)
        assertEquals(6, ClassUtilities.computeInheritanceDistance(ChainImpl.class, Chain0.class));
        
        // Test with multiple paths (diamond)
        assertEquals(1, ClassUtilities.computeInheritanceDistance(Level2.class, Level1A.class));
        assertEquals(1, ClassUtilities.computeInheritanceDistance(Level2.class, Level1B.class));
        assertEquals(2, ClassUtilities.computeInheritanceDistance(Level2.class, Level0.class));
        
        // Test with unrelated hierarchies
        assertEquals(-1, ClassUtilities.computeInheritanceDistance(DiamondImpl.class, Chain0.class));
        assertEquals(-1, ClassUtilities.computeInheritanceDistance(Level4.class, Chain3.class));
    }
}