package com.cedarsoftware.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test cases for boxing support in computeInheritanceDistance.
 * Verifies that primitive types can have valid paths to reference types
 * through boxing to their wrapper classes.
 */
class ClassUtilitiesBoxingDistanceTest {
    
    @Test
    @DisplayName("Primitive int to Number should box through Integer")
    void testPrimitiveIntToNumber() {
        // int → Integer (boxing) → Number
        // Should return 1 (Integer to Number)
        int distance = ClassUtilities.computeInheritanceDistance(int.class, Number.class);
        assertEquals(1, distance, "int should box to Integer then inherit to Number");
    }
    
    @Test
    @DisplayName("Primitive double to Number should box through Double")
    void testPrimitiveDoubleToNumber() {
        // double → Double (boxing) → Number
        int distance = ClassUtilities.computeInheritanceDistance(double.class, Number.class);
        assertEquals(1, distance, "double should box to Double then inherit to Number");
    }
    
    @Test
    @DisplayName("Primitive byte to Object should box through Byte")
    void testPrimitiveByteToObject() {
        // byte → Byte (boxing) → Number → Object
        int distance = ClassUtilities.computeInheritanceDistance(byte.class, Object.class);
        assertEquals(2, distance, "byte should box to Byte then inherit through Number to Object");
    }
    
    @Test
    @DisplayName("Primitive boolean to Object should box through Boolean")
    void testPrimitiveBooleanToObject() {
        // boolean → Boolean (boxing) → Object
        int distance = ClassUtilities.computeInheritanceDistance(boolean.class, Object.class);
        assertEquals(1, distance, "boolean should box to Boolean then inherit to Object");
    }
    
    @Test
    @DisplayName("Primitive char to Object should box through Character")
    void testPrimitiveCharToObject() {
        // char → Character (boxing) → Object
        int distance = ClassUtilities.computeInheritanceDistance(char.class, Object.class);
        assertEquals(1, distance, "char should box to Character then inherit to Object");
    }
    
    @Test
    @DisplayName("Wrapper Integer to Number still works")
    void testWrapperIntegerToNumber() {
        // Integer → Number (direct inheritance)
        int distance = ClassUtilities.computeInheritanceDistance(Integer.class, Number.class);
        assertEquals(1, distance, "Integer should directly inherit from Number");
    }
    
    @Test
    @DisplayName("Primitive to unrelated reference type returns -1")
    void testPrimitiveToUnrelatedReferenceType() {
        // int cannot reach String even through boxing
        int distance = ClassUtilities.computeInheritanceDistance(int.class, String.class);
        assertEquals(-1, distance, "int cannot reach String even through boxing");
    }
    
    @Test
    @DisplayName("Primitive to Comparable should work through boxing")
    void testPrimitiveToComparable() {
        // int → Integer (boxing) → Comparable
        // Integer implements Comparable<Integer>
        int distance = ClassUtilities.computeInheritanceDistance(int.class, Comparable.class);
        assertEquals(1, distance, "int should box to Integer which implements Comparable");
    }
    
    @Test
    @DisplayName("Primitive to Serializable should work through boxing")
    void testPrimitiveToSerializable() {
        // int → Integer (boxing) → Number → Serializable
        // Number implements Serializable, Integer extends Number
        int distance = ClassUtilities.computeInheritanceDistance(int.class, java.io.Serializable.class);
        assertEquals(2, distance, "int should box to Integer, which extends Number that implements Serializable");
    }
    
    @Test
    @DisplayName("All numeric primitives to Number")
    void testAllNumericPrimitivesToNumber() {
        assertEquals(1, ClassUtilities.computeInheritanceDistance(byte.class, Number.class));
        assertEquals(1, ClassUtilities.computeInheritanceDistance(short.class, Number.class));
        assertEquals(1, ClassUtilities.computeInheritanceDistance(int.class, Number.class));
        assertEquals(1, ClassUtilities.computeInheritanceDistance(long.class, Number.class));
        assertEquals(1, ClassUtilities.computeInheritanceDistance(float.class, Number.class));
        assertEquals(1, ClassUtilities.computeInheritanceDistance(double.class, Number.class));
    }
    
    @Test
    @DisplayName("Primitive widening still works independently")
    void testPrimitiveWideningStillWorks() {
        // Verify that primitive widening still works as before
        assertEquals(1, ClassUtilities.computeInheritanceDistance(int.class, long.class));
        assertEquals(2, ClassUtilities.computeInheritanceDistance(byte.class, int.class));
        assertEquals(1, ClassUtilities.computeInheritanceDistance(float.class, double.class));
    }
    
    @Test
    @DisplayName("Mixed: primitive to wrapper's superclass vs widening")
    void testMixedPrimitiveToWrapperSuperclassVsWidening() {
        // int → long (widening) should be distance 1
        assertEquals(1, ClassUtilities.computeInheritanceDistance(int.class, long.class));
        
        // int → Number (boxing to Integer then to Number) should be distance 1
        assertEquals(1, ClassUtilities.computeInheritanceDistance(int.class, Number.class));
        
        // int → Long: This actually works through widening!
        // int → long (widening, distance 1), then long wraps to Long (distance 0)
        // So int → Long is distance 1 through widening + autoboxing
        assertEquals(1, ClassUtilities.computeInheritanceDistance(int.class, Long.class));
        
        // int → Short should be -1 (no widening path from int to short)
        assertEquals(-1, ClassUtilities.computeInheritanceDistance(int.class, Short.class));
    }
}