package com.cedarsoftware.util.convert;

import java.util.Locale;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * String to Locale reads a language tag, whether its parts are joined by "-" (BCP 47) or "_" (Locale.toString()).
 */
class ConverterLocaleTest {

    private final Converter converter = new Converter(new DefaultConverterOptions());

    /**
     * "en_US", the form Locale.toString() prints, gave the undetermined locale (""): Locale.forLanguageTag reads
     * only "-" between the parts and silently returns "und" for anything it cannot read.
     */
    @Test
    void underscoreSeparatedTagsRead() {
        assertEquals(Locale.US, converter.convert("en_US", Locale.class));
        assertEquals(Locale.FRANCE, converter.convert("fr_FR", Locale.class));
        assertEquals(Locale.forLanguageTag("zh-Hant-TW"), converter.convert("zh_Hant_TW", Locale.class));
        assertEquals(Locale.US, converter.convert("en-US", Locale.class));
        assertEquals(Locale.ENGLISH, converter.convert("en", Locale.class));
        for (Locale locale : new Locale[]{Locale.US, Locale.CANADA_FRENCH, Locale.JAPAN, Locale.GERMANY}) {
            assertEquals(locale, converter.convert(locale.toString(), Locale.class), locale.toString());
            assertEquals(locale, converter.convert(converter.convert(locale, String.class), Locale.class));
        }
    }
}
