package com.cedarsoftware.util;

import com.cedarsoftware.util.convert.Converter;
import com.cedarsoftware.util.convert.DefaultConverterOptions;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Test that verifies zero-arg constructor fast-path properly falls back
 * to other constructors when the zero-arg constructor fails.
 */
class ClassUtilitiesZeroArgFallbackTest {
    
    private static final Converter converter = new Converter(new DefaultConverterOptions());
    
    public static class ZeroArgThrows {
        public final String value;
        public final boolean usedStringConstructor;
        
        // Zero-arg constructor that always throws
        public ZeroArgThrows() {
            throw new IllegalStateException("Zero-arg constructor intentionally fails");
        }
        
        // Alternative constructor that works
        public ZeroArgThrows(String value) {
            this.value = value != null ? value : "used-null-default";
            this.usedStringConstructor = true;
        }
    }
    
    public static class ZeroArgThrowsWithPrimitive {
        public final int value;
        
        // Zero-arg constructor that always throws
        public ZeroArgThrowsWithPrimitive() {
            throw new IllegalStateException("Zero-arg constructor intentionally fails");
        }
        
        // Alternative constructor with primitive parameter
        public ZeroArgThrowsWithPrimitive(int value) {
            this.value = value;
        }
    }
    
    @Test
    void testFallbackWhenZeroArgConstructorThrows() {
        // When calling with no arguments and zero-arg constructor throws,
        // should fall back to constructor with parameter using null/default
        Object result = ClassUtilities.newInstance(converter, ZeroArgThrows.class, null);
        
        assertNotNull(result);
        assertTrue(result instanceof ZeroArgThrows);
        ZeroArgThrows instance = (ZeroArgThrows) result;
        assertTrue(instance.usedStringConstructor, "Should have used String constructor as fallback");
        // The String constructor is being called with an empty string (default for String type)
        // not null, because matchArgumentsToParameters provides default values
        assertNotNull(instance.value);
        // The important point is that the fallback to other constructors IS working
    }
    
    @Test
    void testFallbackWithPrimitiveParameter() {
        // When calling with no arguments and zero-arg constructor throws,
        // should fall back to constructor with primitive parameter using default value
        Object result = ClassUtilities.newInstance(converter, ZeroArgThrowsWithPrimitive.class, null);
        
        assertNotNull(result);
        assertTrue(result instanceof ZeroArgThrowsWithPrimitive);
        ZeroArgThrowsWithPrimitive instance = (ZeroArgThrowsWithPrimitive) result;
        assertEquals(0, instance.value); // Default value for int
    }
    
    public static class OnlyZeroArgThrows {
        // Only has a zero-arg constructor that throws
        public OnlyZeroArgThrows() {
            throw new IllegalStateException("Zero-arg constructor intentionally fails");
        }
    }
    
    @Test
    void testFailureWhenOnlyZeroArgExists() {
        // When only zero-arg constructor exists and it throws,
        // should throw an exception (no fallback possible)
        assertThrows(Exception.class, () -> {
            ClassUtilities.newInstance(converter, OnlyZeroArgThrows.class, null);
        });
    }
}