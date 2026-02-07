package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for Converter bugs related to instance-specific user-added conversions.
 *
 * Bug 1: isConversionSupportedFor() uses hardcoded instanceId 0L in getConversionFromDBs(),
 *         missing user-added conversions. Worse, it caches UNSUPPORTED which poisons convert().
 *
 * Bug 2: hasConverterOverrideFor() extracts instanceId from options overrides instead of using
 *         this.instanceId, so it misses conversions added dynamically via addConversion().
 *
 * Bug 3: isConversionSupportedFor(Class) uses static SELF_CONVERSION_CACHE without checking
 *         for instance-specific dynamic overrides first.
 */
class ConverterInstanceBugTest {

    /** Simple custom type not in the built-in conversion database */
    public static class Widget {
        private final String name;

        public Widget(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

    // =====================================================================
    // Bug 1: isConversionSupportedFor() misses user-added conversions
    // =====================================================================

    @Test
    void testIsConversionSupportedForFindsUserAddedConversion() {
        Converter converter = new Converter(new DefaultConverterOptions());
        converter.addConversion(String.class, Widget.class, (from, conv) -> new Widget((String) from));

        // Should return true — the user registered this conversion
        assertTrue(converter.isConversionSupportedFor(String.class, Widget.class),
                "isConversionSupportedFor should detect user-added conversions");
    }

    @Test
    void testIsConversionSupportedForDoesNotPoisonConvertCache() {
        Converter converter = new Converter(new DefaultConverterOptions());
        converter.addConversion(String.class, Widget.class, (from, conv) -> new Widget((String) from));

        // Call isConversionSupportedFor FIRST — before convert()
        boolean supported = converter.isConversionSupportedFor(String.class, Widget.class);
        assertTrue(supported, "isConversionSupportedFor should return true for user-added conversion");

        // Now convert() must still work — must NOT return null from cached UNSUPPORTED
        Widget result = converter.convert("hello", Widget.class);
        assertNotNull(result, "convert() must not return null after isConversionSupportedFor() was called first");
        assertEquals("hello", result.getName());
    }

    @Test
    void testConvertWorksWithUserAddedConversionWithoutPriorQuery() {
        // Baseline: convert() works when called directly (not preceded by isConversionSupportedFor)
        Converter converter = new Converter(new DefaultConverterOptions());
        converter.addConversion(String.class, Widget.class, (from, conv) -> new Widget((String) from));

        Widget result = converter.convert("world", Widget.class);
        assertNotNull(result);
        assertEquals("world", result.getName());
    }

    // =====================================================================
    // Bug 2: hasConverterOverrideFor() misses dynamically added conversions
    // =====================================================================

    @Test
    void testIsSimpleTypeReturnsFalseAfterOverridingBuiltIn() {
        Converter converter = new Converter(new DefaultConverterOptions());

        // String→Integer is a built-in "simple type" conversion
        assertTrue(converter.isSimpleTypeConversionSupported(String.class, Integer.class),
                "String→Integer should be simple before override");

        // Override with a custom conversion
        converter.addConversion(String.class, Integer.class, (from, conv) -> 42);

        // Should now return false — user has overridden it, so it's not a "simple" type anymore
        assertFalse(converter.isSimpleTypeConversionSupported(String.class, Integer.class),
                "String→Integer should NOT be simple after user override via addConversion()");
    }

    @Test
    void testIsSimpleTypeSingleArgReturnsFalseAfterDynamicOverride() {
        Converter converter = new Converter(new DefaultConverterOptions());

        // Override a built-in conversion targeting Integer
        converter.addConversion(String.class, Integer.class, (from, conv) -> 42);

        // The single-arg form checks hasConverterOverrideFor(Integer.class)
        // which should detect the identity entry added by addConversion()
        assertFalse(converter.isSimpleTypeConversionSupported(Integer.class),
                "Integer should NOT be simple when user has overridden a conversion targeting it");
    }

    // =====================================================================
    // Bug 3: isConversionSupportedFor(Class) static cache + dynamic overrides
    // =====================================================================

    @Test
    void testIsConversionSupportedForSingleArgDetectsDynamicConversion() {
        Converter converter = new Converter(new DefaultConverterOptions());
        converter.addConversion(String.class, Widget.class, (from, conv) -> new Widget((String) from));

        // addConversion() adds identity (Widget→Widget) to USER_DB for this instance.
        // isConversionSupportedFor(Widget.class) should return true.
        assertTrue(converter.isConversionSupportedFor(Widget.class),
                "isConversionSupportedFor(Widget.class) should return true when Widget is involved in user conversions");
    }

    // =====================================================================
    // Bug 4: getInheritedConverter() called with 0L in query methods
    //         misses user conversions for parent classes + poisons cache
    // =====================================================================

    @Test
    void testIsConversionSupportedForFindsUserConversionViaInheritance() {
        Converter converter = new Converter(new DefaultConverterOptions());
        // Register conversion for a PARENT class (Object→Widget)
        converter.addConversion(Object.class, Widget.class, (from, conv) -> new Widget(from.toString()));

        // Query via CHILD class (String→Widget) — should find via inheritance walk
        assertTrue(converter.isConversionSupportedFor(String.class, Widget.class),
                "isConversionSupportedFor should find user conversion via inheritance");

        // convert() must also work — must NOT be poisoned by the query above
        Widget w = converter.convert("test", Widget.class);
        assertNotNull(w, "convert() must not return null after isConversionSupportedFor() was called");
        assertEquals("test", w.getName());
    }

    @Test
    void testConvertNotPoisonedByIsSimpleTypeViaInheritance() {
        Converter converter = new Converter(new DefaultConverterOptions());
        // Register conversion for a PARENT class (Object→Widget)
        converter.addConversion(Object.class, Widget.class, (from, conv) -> new Widget(from.toString()));

        // isSimpleTypeConversionSupported should not poison the cache for this pair
        // (it may return true or false, but must not cache UNSUPPORTED for a pair that convert() handles)
        converter.isSimpleTypeConversionSupported(String.class, Widget.class);

        // convert() must still work
        Widget w = converter.convert("hello", Widget.class);
        assertNotNull(w, "convert() must not return null after isSimpleTypeConversionSupported() was called");
        assertEquals("hello", w.getName());
    }

    @Test
    void testDifferentInstancesGetCorrectResultsForSingleArgQuery() {
        // Instance A: no user conversions
        Converter converterA = new Converter(new DefaultConverterOptions());

        // Instance B: has user conversion for Widget
        Converter converterB = new Converter(new DefaultConverterOptions());
        converterB.addConversion(String.class, Widget.class, (from, conv) -> new Widget((String) from));

        // Instance B should say Widget is supported
        assertTrue(converterB.isConversionSupportedFor(Widget.class),
                "Instance with Widget conversion should report Widget as supported");

        // Instance A should say Widget is NOT supported (it has no conversions for Widget)
        assertFalse(converterA.isConversionSupportedFor(Widget.class),
                "Instance without Widget conversion should report Widget as unsupported");
    }
}
