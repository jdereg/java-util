package com.cedarsoftware.util.convert;

import java.util.logging.Logger;

import com.cedarsoftware.util.ClassUtilities;
import com.cedarsoftware.util.LoggingConfig;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Test String to various primitive conversions with addFactoryConversion
 */
class StringPrimitiveTest {
    private static final Logger LOG = Logger.getLogger(StringPrimitiveTest.class.getName());
    static {
        LoggingConfig.initForTests();
    }

    @Test
    void testStringToPrimitives() {
        Converter converter = new Converter(new DefaultConverterOptions());
        
        // Test String to primitive conversions (should now work with addFactoryConversion)
        byte byteVal = converter.convert("42", byte.class);
        assertEquals(42, byteVal);
        
        short shortVal = converter.convert("123", short.class);
        assertEquals(123, shortVal);
        
        int intVal = converter.convert("456", int.class);
        assertEquals(456, intVal);
        
        char charVal = converter.convert("Z", char.class);
        assertEquals('Z', charVal);
        
        // Test String to wrapper conversions (should still work)
        Byte byteObj = converter.convert("42", Byte.class);
        assertEquals(42, byteObj.byteValue());
        
        Short shortObj = converter.convert("123", Short.class);
        assertEquals(123, shortObj.shortValue());
        
        Integer intObj = converter.convert("456", Integer.class);
        assertEquals(456, intObj.intValue());
        
        Character charObj = converter.convert("Z", Character.class);
        assertEquals('Z', charObj.charValue());
        
        LOG.info("✓ String to primitive/wrapper conversions work with addFactoryConversion");
    }
}