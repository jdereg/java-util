package com.cedarsoftware.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test cases for primitive widening distance calculations in ClassUtilities.
 * Verifies that computeInheritanceDistance correctly models Java's primitive
 * widening conversions as defined in JLS 5.1.2.
 */
class ClassUtilitiesPrimitiveWideningTest {
    
    @Test
    @DisplayName("Same primitive type should have distance 0")
    void testSamePrimitiveType() {
        assertEquals(0, ClassUtilities.computeInheritanceDistance(int.class, int.class));
        assertEquals(0, ClassUtilities.computeInheritanceDistance(byte.class, byte.class));
        assertEquals(0, ClassUtilities.computeInheritanceDistance(double.class, double.class));
        assertEquals(0, ClassUtilities.computeInheritanceDistance(boolean.class, boolean.class));
    }
    
    @Test
    @DisplayName("Primitive to same wrapper should have distance 0")
    void testPrimitiveToSameWrapper() {
        assertEquals(0, ClassUtilities.computeInheritanceDistance(int.class, Integer.class));
        assertEquals(0, ClassUtilities.computeInheritanceDistance(Integer.class, int.class));
        assertEquals(0, ClassUtilities.computeInheritanceDistance(byte.class, Byte.class));
        assertEquals(0, ClassUtilities.computeInheritanceDistance(Boolean.class, boolean.class));
    }
    
    @Test
    @DisplayName("byte widening conversions")
    void testByteWidening() {
        // byte → short → int → long → float → double
        assertEquals(1, ClassUtilities.computeInheritanceDistance(byte.class, short.class));
        assertEquals(2, ClassUtilities.computeInheritanceDistance(byte.class, int.class));
        assertEquals(3, ClassUtilities.computeInheritanceDistance(byte.class, long.class));
        assertEquals(4, ClassUtilities.computeInheritanceDistance(byte.class, float.class));
        assertEquals(5, ClassUtilities.computeInheritanceDistance(byte.class, double.class));
        
        // byte cannot widen to char or boolean
        assertEquals(-1, ClassUtilities.computeInheritanceDistance(byte.class, char.class));
        assertEquals(-1, ClassUtilities.computeInheritanceDistance(byte.class, boolean.class));
    }
    
    @Test
    @DisplayName("short widening conversions")
    void testShortWidening() {
        // short → int → long → float → double
        assertEquals(1, ClassUtilities.computeInheritanceDistance(short.class, int.class));
        assertEquals(2, ClassUtilities.computeInheritanceDistance(short.class, long.class));
        assertEquals(3, ClassUtilities.computeInheritanceDistance(short.class, float.class));
        assertEquals(4, ClassUtilities.computeInheritanceDistance(short.class, double.class));
        
        // short cannot widen to byte, char, or boolean
        assertEquals(-1, ClassUtilities.computeInheritanceDistance(short.class, byte.class));
        assertEquals(-1, ClassUtilities.computeInheritanceDistance(short.class, char.class));
        assertEquals(-1, ClassUtilities.computeInheritanceDistance(short.class, boolean.class));
    }
    
    @Test
    @DisplayName("char widening conversions")
    void testCharWidening() {
        // char → int → long → float → double
        assertEquals(1, ClassUtilities.computeInheritanceDistance(char.class, int.class));
        assertEquals(2, ClassUtilities.computeInheritanceDistance(char.class, long.class));
        assertEquals(3, ClassUtilities.computeInheritanceDistance(char.class, float.class));
        assertEquals(4, ClassUtilities.computeInheritanceDistance(char.class, double.class));
        
        // char cannot widen to byte, short, or boolean
        assertEquals(-1, ClassUtilities.computeInheritanceDistance(char.class, byte.class));
        assertEquals(-1, ClassUtilities.computeInheritanceDistance(char.class, short.class));
        assertEquals(-1, ClassUtilities.computeInheritanceDistance(char.class, boolean.class));
    }
    
    @Test
    @DisplayName("int widening conversions")
    void testIntWidening() {
        // int → long → float → double
        assertEquals(1, ClassUtilities.computeInheritanceDistance(int.class, long.class));
        assertEquals(2, ClassUtilities.computeInheritanceDistance(int.class, float.class));
        assertEquals(3, ClassUtilities.computeInheritanceDistance(int.class, double.class));
        
        // int cannot widen to smaller types
        assertEquals(-1, ClassUtilities.computeInheritanceDistance(int.class, byte.class));
        assertEquals(-1, ClassUtilities.computeInheritanceDistance(int.class, short.class));
        assertEquals(-1, ClassUtilities.computeInheritanceDistance(int.class, char.class));
        assertEquals(-1, ClassUtilities.computeInheritanceDistance(int.class, boolean.class));
    }
    
    @Test
    @DisplayName("long widening conversions")
    void testLongWidening() {
        // long → float → double
        assertEquals(1, ClassUtilities.computeInheritanceDistance(long.class, float.class));
        assertEquals(2, ClassUtilities.computeInheritanceDistance(long.class, double.class));
        
        // long cannot widen to integral types
        assertEquals(-1, ClassUtilities.computeInheritanceDistance(long.class, int.class));
        assertEquals(-1, ClassUtilities.computeInheritanceDistance(long.class, short.class));
    }
    
    @Test
    @DisplayName("float widening conversions")
    void testFloatWidening() {
        // float → double
        assertEquals(1, ClassUtilities.computeInheritanceDistance(float.class, double.class));
        
        // float cannot widen to any other type
        assertEquals(-1, ClassUtilities.computeInheritanceDistance(float.class, long.class));
        assertEquals(-1, ClassUtilities.computeInheritanceDistance(float.class, int.class));
    }
    
    @Test
    @DisplayName("double has no widening conversions")
    void testDoubleNoWidening() {
        // double is the widest numeric type
        assertEquals(-1, ClassUtilities.computeInheritanceDistance(double.class, float.class));
        assertEquals(-1, ClassUtilities.computeInheritanceDistance(double.class, long.class));
        assertEquals(-1, ClassUtilities.computeInheritanceDistance(double.class, int.class));
    }
    
    @Test
    @DisplayName("boolean has no widening conversions")
    void testBooleanNoWidening() {
        // boolean doesn't participate in widening
        assertEquals(-1, ClassUtilities.computeInheritanceDistance(boolean.class, int.class));
        assertEquals(-1, ClassUtilities.computeInheritanceDistance(boolean.class, byte.class));
        assertEquals(-1, ClassUtilities.computeInheritanceDistance(int.class, boolean.class));
    }
    
    @Test
    @DisplayName("Wrapper to wrapper widening should work")
    void testWrapperToWrapperWidening() {
        // Wrappers should follow same widening rules as primitives
        assertEquals(1, ClassUtilities.computeInheritanceDistance(Byte.class, Short.class));
        assertEquals(2, ClassUtilities.computeInheritanceDistance(Byte.class, Integer.class));
        assertEquals(3, ClassUtilities.computeInheritanceDistance(Byte.class, Long.class));
        assertEquals(1, ClassUtilities.computeInheritanceDistance(Integer.class, Long.class));
        assertEquals(2, ClassUtilities.computeInheritanceDistance(Integer.class, Float.class));
    }
    
    @Test
    @DisplayName("Mixed primitive and wrapper widening")
    void testMixedPrimitiveWrapperWidening() {
        // Primitive to different wrapper
        assertEquals(1, ClassUtilities.computeInheritanceDistance(int.class, Long.class));
        assertEquals(2, ClassUtilities.computeInheritanceDistance(int.class, Float.class));
        assertEquals(3, ClassUtilities.computeInheritanceDistance(int.class, Double.class));
        
        // Wrapper to different primitive
        assertEquals(1, ClassUtilities.computeInheritanceDistance(Integer.class, long.class));
        assertEquals(1, ClassUtilities.computeInheritanceDistance(Short.class, int.class));
    }
    
    @Test
    @DisplayName("Wrapper to Number superclass")
    void testWrapperToNumberSuperclass() {
        // Wrapper classes extend Number
        assertEquals(1, ClassUtilities.computeInheritanceDistance(Integer.class, Number.class));
        assertEquals(1, ClassUtilities.computeInheritanceDistance(Double.class, Number.class));
        assertEquals(1, ClassUtilities.computeInheritanceDistance(Byte.class, Number.class));
        
        // Wrapper to Object
        assertEquals(2, ClassUtilities.computeInheritanceDistance(Integer.class, Object.class));
        
        // But primitives can't inherit from Number
        assertEquals(-1, ClassUtilities.computeInheritanceDistance(int.class, Number.class));
        assertEquals(-1, ClassUtilities.computeInheritanceDistance(double.class, Number.class));
    }
    
    @Test
    @DisplayName("No narrowing conversions")
    void testNoNarrowingConversions() {
        // Narrowing conversions should return -1
        assertEquals(-1, ClassUtilities.computeInheritanceDistance(double.class, int.class));
        assertEquals(-1, ClassUtilities.computeInheritanceDistance(long.class, int.class));
        assertEquals(-1, ClassUtilities.computeInheritanceDistance(int.class, short.class));
        assertEquals(-1, ClassUtilities.computeInheritanceDistance(short.class, byte.class));
    }
}