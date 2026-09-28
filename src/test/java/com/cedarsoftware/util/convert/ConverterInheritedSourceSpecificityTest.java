package com.cedarsoftware.util.convert;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * For a source class with no conversion of its own, the Converter uses the conversion of its most specific supertype
 * that has one, as Java prefers the most specific overload. Inherited conversions were ranked by inheritance distance,
 * so a supertype could come before its own subtype: an ArrayList is 2 steps from Object (through RandomAccess) and 3
 * from Collection, so with conversions added from both, the Object one was used for an ArrayList.
 */
class ConverterInheritedSourceSpecificityTest {

    static final class Marker {
        final String from;

        Marker(String from) {
            this.from = from;
        }
    }

    private static Converter converter() {
        Converter converter = new Converter(new DefaultConverterOptions());
        converter.addConversion((from, c) -> new Marker("Object"), Object.class, Marker.class);
        converter.addConversion((from, c) -> new Marker("Collection"), Collection.class, Marker.class);
        converter.addConversion((from, c) -> new Marker("CharSequence"), CharSequence.class, Marker.class);
        return converter;
    }

    @Test
    void aSubtypesConversionIsUsedBeforeItsSupertypes() {
        Converter converter = converter();
        assertEquals("Collection", converter.convert(new ArrayList<>(), Marker.class).from);
        assertEquals("Collection", converter.convert(new HashSet<>(), Marker.class).from);
        assertEquals("CharSequence", converter.convert(new StringBuilder("x"), Marker.class).from);
        assertEquals("Object", converter.convert(new Date(0), Marker.class).from);
    }
}
