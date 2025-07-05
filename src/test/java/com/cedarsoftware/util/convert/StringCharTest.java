package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Test String to char conversion with addFactoryConversion
 */
class StringCharTest {
    
    @Test
    void testStringToChar() {
        Converter converter = new Converter(new DefaultConverterOptions());
        
        // Test String to char (should now work with addFactoryConversion)
        char charVal = converter.convert("A", char.class);
        assertEquals('A', charVal);
        
        // Test String to Character (should still work)
        Character charObj = converter.convert("B", Character.class);
        assertEquals('B', charObj.charValue());
        
        System.out.println("✓ String to char/Character conversions work with addFactoryConversion");
    }
}