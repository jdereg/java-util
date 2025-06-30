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
        assertTrue(Converter.isContainerConversionSupported(List.class, Day.class));
    }

    @Test
    void enumSetSourceSupportedToArray() {
        assertTrue(Converter.isContainerConversionSupported(EnumSet.class, String[].class));
    }

    @Test
    void collectionSourceSupportedToCollection() {
        assertTrue(Converter.isContainerConversionSupported(List.class, Set.class));
    }

    @Test
    void arrayToArrayWhenTargetNotCollection() {
        assertTrue(Converter.isContainerConversionSupported(String[].class, Integer[].class));
    }

    @Test
    void unsupportedTypesReturnFalse() {
        assertFalse(Converter.isContainerConversionSupported(String.class, Integer.class));
    }
}
