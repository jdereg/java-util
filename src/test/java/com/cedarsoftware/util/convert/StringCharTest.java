package com.cedarsoftware.util.convert;

import java.util.logging.Logger;

import com.cedarsoftware.util.ClassUtilities;
import com.cedarsoftware.util.LoggingConfig;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Test String to char conversion with addFactoryConversion
 */
class StringCharTest {
    private static final Logger LOG = Logger.getLogger(StringCharTest.class.getName());
    static {
        LoggingConfig.initForTests();
    }

    @Test
    void testStringToChar() {
        Converter converter = new Converter(new DefaultConverterOptions());
        
        // Test String to char (should now work with addFactoryConversion)
        char charVal = converter.convert("A", char.class);
        assertEquals('A', charVal);
        
        // Test String to Character (should still work)
        Character charObj = converter.convert("B", Character.class);
        assertEquals('B', charObj.charValue());
        
        LOG.info("✓ String to char/Character conversions work with addFactoryConversion");
    }
}