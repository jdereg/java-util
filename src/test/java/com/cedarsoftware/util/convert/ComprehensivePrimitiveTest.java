package com.cedarsoftware.util.convert;

import java.util.logging.Logger;

import com.cedarsoftware.util.LoggingConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Comprehensive test of primitive conversions with the new addFactoryConversion approach
 */
class ComprehensivePrimitiveTest {
    private static final Logger LOG = Logger.getLogger(ComprehensivePrimitiveTest.class.getName());
    static {
        LoggingConfig.initForTests();
    }

    @Test
    void testAllBasicPrimitiveConversions() {
        Converter converter = new Converter(new DefaultConverterOptions());
        
        // String to primitives (should now work via addFactoryConversion)
        byte b = converter.convert("42", byte.class);
        assertEquals(42, b);
        
        short s = converter.convert("123", short.class);
        assertEquals(123, s);
        
        int i = converter.convert("456", int.class);
        assertEquals(456, i);
        
        long l = converter.convert("789", long.class);
        assertEquals(789L, l);
        
        float f = converter.convert("3.14", float.class);
        assertEquals(3.14f, f, 0.001f);
        
        double d = converter.convert("2.718", double.class);
        assertEquals(2.718, d, 0.001);
        
        boolean bool = converter.convert("true", boolean.class);
        assertTrue(bool);
        
        char c = converter.convert("X", char.class);
        assertEquals('X', c);
        
        // Wrapper to primitives (should work via addFactoryConversion + UniversalConversions)
        int fromInteger = converter.convert(Integer.valueOf(99), int.class);
        assertEquals(99, fromInteger);
        
        long fromLong = converter.convert(Long.valueOf(888L), long.class);
        assertEquals(888L, fromLong);
        
        LOG.info("✓ All comprehensive primitive conversions work");
    }
}