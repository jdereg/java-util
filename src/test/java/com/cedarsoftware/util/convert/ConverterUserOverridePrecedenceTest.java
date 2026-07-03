package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pins the precedence contract between instance-specific user conversions and the shared
 * (static) resolution cache: user conversions are consulted FIRST, so an entry written to the
 * shared cache by a plain Converter instance can never mask another instance's overrides.
 * <p>
 * Regression background: the shared FULL_CONVERSION_CACHE was read before USER_DB, so
 * (a) an override supplied via ConverterOptions was silently ignored for any (source, target)
 * pair previously converted by ANY plain instance in the JVM, and (b) an override registered
 * via addConversion() worked only until any plain instance converted the same pair and
 * repopulated the shared cache — after which the override instance silently reverted to the
 * built-in conversion. Both were order-dependent and silent.
 * <p>
 * Each test uses its own private types where isolation from the JVM-wide shared cache matters.
 *
 * @author John DeRegnaucourt (jdereg@gmail.com)
 */
class ConverterUserOverridePrecedenceTest {

    @Test
    void constructorSuppliedOverrideNotMaskedBySharedCache() {
        // A plain instance converts the pair first, populating the shared cache with the built-in.
        Converter plain = new Converter(new DefaultConverterOptions());
        assertEquals(5, plain.convert("5", Integer.class));

        // An instance constructed WITH an override for the same pair must use its override.
        DefaultConverterOptions options = new DefaultConverterOptions();
        options.getConverterOverrides().put(
                Converter.pair(String.class, Integer.class), (from, c) -> 42);
        Converter withOverride = new Converter(options);
        assertEquals(42, withOverride.convert("5", Integer.class),
                "ConverterOptions override must not be masked by the shared conversion cache");

        // And the plain instance must be unaffected by the other instance's override.
        assertEquals(7, plain.convert("7", Integer.class));
    }

    @Test
    void addConversionOverrideSurvivesCrossInstanceCacheRepopulation() {
        Converter plain = new Converter(new DefaultConverterOptions());
        Converter custom = new Converter(new DefaultConverterOptions());

        custom.addConversion((from, c) -> 99L, String.class, Long.class);
        assertEquals(99L, custom.convert("5", Long.class));

        // Another (plain) instance converts the same pair, repopulating the shared cache
        // with the built-in conversion...
        assertEquals(5L, plain.convert("5", Long.class));

        // ...which must NOT re-poison the override instance.
        assertEquals(99L, custom.convert("5", Long.class),
                "addConversion override must survive shared-cache repopulation by other instances");
    }

    @Test
    void constructorOverrideRegisteredWithPrimitivesFiresForWrapperLookups() {
        // Override registered under the PRIMITIVE target type...
        DefaultConverterOptions options = new DefaultConverterOptions();
        options.getConverterOverrides().put(
                Converter.pair(String.class, long.class), (from, c) -> 777L);
        Converter converter = new Converter(options);

        // ...must fire for both the primitive and the wrapper variants of the lookup,
        // matching addConversion()'s primitive/wrapper variation expansion.
        assertEquals(777L, (Long) converter.convert("5", long.class));
        assertEquals(777L, converter.convert("5", Long.class),
                "ConverterOptions override keyed with a primitive must also fire for the wrapper");
    }

    // Private types so shared-cache state created here cannot collide with other tests.
    static class SupportSource { }
    static class SupportTarget { }

    @Test
    void supportChecksAreInstanceCorrectAndNegativesAreShareable() {
        Converter plain = new Converter(new DefaultConverterOptions());

        // Negative answer (repeated — second call may be served from the shared negative cache).
        assertFalse(plain.isConversionSupportedFor(SupportSource.class, SupportTarget.class));
        assertFalse(plain.isConversionSupportedFor(SupportSource.class, SupportTarget.class));

        // A different instance that registers the conversion must see it as supported...
        Converter custom = new Converter(new DefaultConverterOptions());
        custom.addConversion((from, c) -> new SupportTarget(), SupportSource.class, SupportTarget.class);
        assertTrue(custom.isConversionSupportedFor(SupportSource.class, SupportTarget.class));

        // ...while the plain instance still (correctly) reports unsupported.
        assertFalse(plain.isConversionSupportedFor(SupportSource.class, SupportTarget.class));
    }

    // Distinct hierarchy for the inheritance-level scenario.
    static class ParentSource { }
    static class ChildSource extends ParentSource { }
    static class InheritTarget { }

    @Test
    void inheritanceLevelUserConversionBeatsSharedNegative() {
        // Plain instance resolves (ChildSource -> InheritTarget) as unsupported (cached negative).
        Converter plain = new Converter(new DefaultConverterOptions());
        assertFalse(plain.isConversionSupportedFor(ChildSource.class, InheritTarget.class));

        // An instance with a PARENT-level user conversion must resolve the child pair through
        // inheritance, even though the shared cache holds a negative for the exact pair.
        Converter custom = new Converter(new DefaultConverterOptions());
        custom.addConversion((from, c) -> new InheritTarget(), ParentSource.class, InheritTarget.class);
        assertTrue(custom.isConversionSupportedFor(ChildSource.class, InheritTarget.class),
                "inheritance-level user conversion must not be masked by a shared negative entry");
        assertTrue(custom.convert(new ChildSource(), InheritTarget.class) instanceof InheritTarget);
    }
}
