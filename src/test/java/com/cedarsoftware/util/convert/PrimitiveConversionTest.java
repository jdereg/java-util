package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Test basic primitive conversions that should work with ConversionTripleMap
 */
class PrimitiveConversionTest {
    
    @Test
    void testBasicPrimitiveConversions() {
        Converter converter = new Converter(new DefaultConverterOptions());
        
        // Test wrapper to primitive conversions (the main fix)
        int intVal = converter.convert(Integer.valueOf(42), int.class);
        assertEquals(42, intVal);
        
        long longVal = converter.convert(Long.valueOf(123L), long.class);
        assertEquals(123L, longVal);
        
        float floatVal = converter.convert(Float.valueOf(3.14f), float.class);
        assertEquals(3.14f, floatVal, 0.001f);
        
        double doubleVal = converter.convert(Double.valueOf(2.718), double.class);
        assertEquals(2.718, doubleVal, 0.001);
        
        System.out.println("✓ Basic wrapper-to-primitive conversions work");
    }
}