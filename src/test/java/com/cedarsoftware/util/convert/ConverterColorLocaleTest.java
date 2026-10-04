package com.cedarsoftware.util.convert;

import java.util.Locale;

import com.cedarsoftware.util.geom.Color;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * String to Color reads color names whatever the JVM's default locale.
 */
class ConverterColorLocaleTest {

    /**
     * Under a Turkish default locale, "WHITE".toLowerCase() is "whıte" (dotless i), which no name matched, so "WHITE",
     * "PINK" and "LIGHT_GRAY" failed to convert. The names are now lowered in Locale.ROOT.
     */
    @Test
    void colorNamesReadUnderATurkishDefaultLocale() {
        Locale saved = Locale.getDefault();
        try {
            Locale.setDefault(new Locale("tr", "TR"));
            Converter converter = new Converter(new DefaultConverterOptions());
            assertEquals(Color.WHITE, converter.convert("WHITE", Color.class));
            assertEquals(Color.PINK, converter.convert("PINK", Color.class));
            assertEquals(Color.LIGHT_GRAY, converter.convert("LIGHT_GRAY", Color.class));
            assertEquals(Color.RED, converter.convert("RED", Color.class));
            assertEquals(new Color(10, 20, 30), converter.convert("RGB(10, 20, 30)", Color.class));
        } finally {
            Locale.setDefault(saved);
        }
    }
}
