package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConverterCollectionSupportTest {

    private enum Day { MONDAY, TUESDAY }

    @Test
    void enumTargetSupportedFromCollection() {
        assertTrue(Converter.isCollectionConversionSupported(List.class, Day.class));
    }

    @Test
    void enumSetSourceSupportedToArray() {
        assertTrue(Converter.isCollectionConversionSupported(EnumSet.class, String[].class));
    }

    @Test
    void collectionSourceSupportedToCollection() {
        assertTrue(Converter.isCollectionConversionSupported(List.class, Set.class));
    }

    @Test
    void arrayToArrayWhenTargetNotCollection() {
        assertTrue(Converter.isCollectionConversionSupported(String[].class, Integer[].class));
    }

    @Test
    void unsupportedTypesReturnFalse() {
        assertFalse(Converter.isCollectionConversionSupported(String.class, Integer.class));
    }
}
