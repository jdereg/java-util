package com.cedarsoftware.util;

import com.cedarsoftware.util.convert.Converter;
import com.cedarsoftware.util.convert.DefaultConverterOptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test cases to verify that mutable buffers and arrays are not shared between calls.
 */
class ClassUtilitiesMutableBufferTest {
    
    private final Converter converter = new Converter(new DefaultConverterOptions());
    
    /**
     * Helper method to access the private getArgForType method
     */
    private Object getArgForType(Class<?> argType) throws Exception {
        Method method = ClassUtilities.class.getDeclaredMethod("getArgForType", 
                com.cedarsoftware.util.convert.Converter.class, Class.class);
        method.setAccessible(true);
        return method.invoke(null, converter, argType);
    }
    
    @Test
    @DisplayName("ByteBuffer instances should be fresh to prevent mutation issues")
    void testByteBufferFreshInstances() throws Exception {
        // Get two ByteBuffer instances via the internal mapping
        ByteBuffer buffer1 = (ByteBuffer) getArgForType(ByteBuffer.class);
        ByteBuffer buffer2 = (ByteBuffer) getArgForType(ByteBuffer.class);
        
        assertNotNull(buffer1);
        assertNotNull(buffer2);
        assertNotSame(buffer1, buffer2, "ByteBuffer instances should not be shared");
        
        // Verify mutation of one doesn't affect the other
        assertEquals(0, buffer1.position());
        assertEquals(0, buffer2.position());
        
        // The important thing is they are different instances
        assertNotSame(buffer1.array(), buffer2.array(), "ByteBuffer backing arrays should be different");
    }
    
    @Test
    @DisplayName("CharBuffer instances should be fresh to prevent mutation issues")
    void testCharBufferFreshInstances() throws Exception {
        // Get two CharBuffer instances via the internal mapping
        CharBuffer buffer1 = (CharBuffer) getArgForType(CharBuffer.class);
        CharBuffer buffer2 = (CharBuffer) getArgForType(CharBuffer.class);
        
        assertNotNull(buffer1);
        assertNotNull(buffer2);
        assertNotSame(buffer1, buffer2, "CharBuffer instances should not be shared");
        
        // Verify they are independent
        assertEquals(0, buffer1.position());
        assertEquals(0, buffer2.position());
        assertNotSame(buffer1.array(), buffer2.array(), "CharBuffer backing arrays should be different");
    }
    
    @Test
    @DisplayName("Object[] instances should be fresh to prevent mutation issues")
    void testObjectArrayFreshInstances() throws Exception {
        // Get two Object[] instances via the internal mapping
        Object[] array1 = (Object[]) getArgForType(Object[].class);
        Object[] array2 = (Object[]) getArgForType(Object[].class);
        
        assertNotNull(array1);
        assertNotNull(array2);
        assertNotSame(array1, array2, "Object[] instances should not be shared");
        
        // Both should be empty
        assertEquals(0, array1.length);
        assertEquals(0, array2.length);
    }
    
    @Test
    @DisplayName("Primitive array instances should be fresh")
    void testPrimitiveArrayFreshInstances() throws Exception {
        // Test int[]
        int[] intArray1 = (int[]) getArgForType(int[].class);
        int[] intArray2 = (int[]) getArgForType(int[].class);
        
        assertNotNull(intArray1);
        assertNotNull(intArray2);
        assertNotSame(intArray1, intArray2, "int[] instances should not be shared");
        
        // Test byte[]
        byte[] byteArray1 = (byte[]) getArgForType(byte[].class);
        byte[] byteArray2 = (byte[]) getArgForType(byte[].class);
        
        assertNotNull(byteArray1);
        assertNotNull(byteArray2);
        assertNotSame(byteArray1, byteArray2, "byte[] instances should not be shared");
    }
    
    @Test
    @DisplayName("Boxed primitive array instances should be fresh")
    void testBoxedPrimitiveArrayFreshInstances() throws Exception {
        // Test Integer[]
        Integer[] intArray1 = (Integer[]) getArgForType(Integer[].class);
        Integer[] intArray2 = (Integer[]) getArgForType(Integer[].class);
        
        assertNotNull(intArray1);
        assertNotNull(intArray2);
        assertNotSame(intArray1, intArray2, "Integer[] instances should not be shared");
        
        // Test Boolean[]
        Boolean[] boolArray1 = (Boolean[]) getArgForType(Boolean[].class);
        Boolean[] boolArray2 = (Boolean[]) getArgForType(Boolean[].class);
        
        assertNotNull(boolArray1);
        assertNotNull(boolArray2);
        assertNotSame(boolArray1, boolArray2, "Boolean[] instances should not be shared");
    }
    
}