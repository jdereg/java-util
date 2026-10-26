package com.cedarsoftware.util;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import com.cedarsoftware.io.JsonIo;
import com.cedarsoftware.util.convert.Converter;
import com.cedarsoftware.util.convert.DefaultConverterOptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * newInstance() caches, per class and per argument SHAPE (the classes of the supplied values), which
 * constructor worked -- or that none did. A constructor body that rejects a VALUE ("must not be negative")
 * made the outcome depend on the values, yet it was cached against the types: one rejected value then
 * decided every later call with the same shape. A valid value either failed with "Unable to instantiate
 * (cached)" or -- through the Map path json-io uses -- was silently replaced by a default-constructed
 * object; and a fallback constructor chosen only because the preferred one rejected a value was replayed
 * for good values too.
 * <p>
 * Map keys deliberately differ from the constructor's parameter name, which drives the Map path into the
 * positional matching that classes compiled without {@code -parameters} (Maven's default) always take.
 */
class ClassUtilitiesConstructorPlanCacheTest {

    private final Converter converter = new Converter(new DefaultConverterOptions());

    /** A constructor that validates its argument. */
    static class Positive {
        final int x;

        Positive(int x) {
            if (x < 0) {
                throw new IllegalArgumentException("negative: " + x);
            }
            this.x = x;
        }
    }

    /** A validating constructor plus a no-arg fallback. */
    static class PositiveOrDefault {
        final int x;
        final boolean viaDefault;

        PositiveOrDefault(int x) {
            if (x < 0) {
                throw new IllegalArgumentException("negative: " + x);
            }
            this.x = x;
            this.viaDefault = false;
        }

        PositiveOrDefault() {
            this.x = -99;
            this.viaDefault = true;
        }
    }

    /** Derived state computed in the constructor; transient, so no serializer restores it. */
    public static class Account {
        int amount;
        transient int doubled;

        public Account(int a) {
            if (a < 0) {
                throw new IllegalArgumentException("negative: " + a);
            }
            this.amount = a;
            this.doubled = 2 * a;
        }
    }

    private static Map<String, Object> amount(Object v) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("amount", v);
        return m;
    }

    @BeforeEach
    void clearCaches() {
        ClassUtilities.clearCaches();
    }

    @Test
    void validValueWorksAfterARejectedValue() {
        IllegalArgumentException first = assertThrows(IllegalArgumentException.class,
                () -> ClassUtilities.newInstance(converter, Positive.class, Collections.singletonList(-1)));
        assertTrue(first.getMessage().contains("negative"), first.getMessage());

        Positive p = (Positive) ClassUtilities.newInstance(converter, Positive.class, Collections.singletonList(5));
        assertEquals(5, p.x);
    }

    @Test
    void aRepeatedRejectionStillReportsTheConstructorsReason() {
        assertThrows(IllegalArgumentException.class,
                () -> ClassUtilities.newInstance(converter, Positive.class, Collections.singletonList(-1)));
        IllegalArgumentException second = assertThrows(IllegalArgumentException.class,
                () -> ClassUtilities.newInstance(converter, Positive.class, Collections.singletonList(-2)));
        assertFalse(second.getMessage().contains("(cached)"), second.getMessage());
        assertTrue(second.getMessage().contains("negative: -2"), second.getMessage());
    }

    @Test
    void aFallbackConstructorChosenBecauseOfAValueIsNotReplayed() {
        PositiveOrDefault rejected = (PositiveOrDefault) ClassUtilities.newInstance(
                converter, PositiveOrDefault.class, Collections.singletonList(-1));
        assertTrue(rejected.viaDefault, "-1 is rejected, so the no-arg constructor is the fallback");

        PositiveOrDefault ok = (PositiveOrDefault) ClassUtilities.newInstance(
                converter, PositiveOrDefault.class, Collections.singletonList(5));
        assertFalse(ok.viaDefault, "5 is valid: the int constructor must be used");
        assertEquals(5, ok.x);
    }

    @Test
    void mapPathDoesNotTurnLaterValidValuesIntoDefaults() {
        Account rejected = (Account) ClassUtilities.newInstance(converter, Account.class, amount(-1));
        assertEquals(0, rejected.amount, "rejected value: default-constructed, as designed for field injection");

        for (int v : Arrays.asList(5, 7)) {
            Account a = (Account) ClassUtilities.newInstance(converter, Account.class, amount(v));
            assertEquals(v, a.amount);
            assertEquals(2 * v, a.doubled, "the constructor must have run with " + v);
        }
    }

    @Test
    void jsonIoReadIsNotPoisonedByAnEarlierDocument() {
        String type = Account.class.getName();
        JsonIo.toJava("{\"@type\":\"" + type + "\",\"amount\":-1}", null).asClass(Account.class);

        Account a = JsonIo.toJava("{\"@type\":\"" + type + "\",\"amount\":5}", null).asClass(Account.class);
        assertEquals(5, a.amount);
        assertEquals(10, a.doubled, "the constructor must have run with 5");
    }

    @Test
    void aValidFirstCallIsStillCachedAndReused() {
        for (int v = 0; v < 3; v++) {
            Positive p = (Positive) ClassUtilities.newInstance(converter, Positive.class, Collections.singletonList(v));
            assertEquals(v, p.x);
        }
        assertThrows(IllegalArgumentException.class,
                () -> ClassUtilities.newInstance(converter, Positive.class, Collections.singletonList(-1)));
        Positive after = (Positive) ClassUtilities.newInstance(converter, Positive.class, Collections.singletonList(9));
        assertEquals(9, after.x);
    }

    /** Its only constructor refuses the defaults a call without arguments gives it. */
    static class Refuses {
        static final java.util.concurrent.atomic.AtomicInteger RUNS = new java.util.concurrent.atomic.AtomicInteger();
        final String number;

        Refuses(String number) {
            RUNS.incrementAndGet();
            if (number == null || !number.startsWith("P-")) {
                throw new IllegalArgumentException("number must start with P-");
            }
            this.number = number;
        }
    }

    /**
     * A call whose outcome depends on no value (no arguments: the constructors get defaults) is remembered as failing,
     * and a later one says why, as the first did. It was "Unable to instantiate (cached): ..", the constructors' own
     * errors gone -- json-io's error for a class it could not build read so after the first read of that class.
     */
    @Test
    void aRememberedFailureSaysWhy() {
        IllegalArgumentException first = assertThrows(IllegalArgumentException.class,
                () -> ClassUtilities.newInstance(converter, Refuses.class, Collections.emptyList()));
        assertTrue(first.getMessage().contains("must start with P-"), first.getMessage());
        int runs = Refuses.RUNS.get();
        IllegalArgumentException second = assertThrows(IllegalArgumentException.class,
                () -> ClassUtilities.newInstance(converter, Refuses.class, Collections.emptyList()));
        assertEquals(first.getMessage(), second.getMessage());
        assertFalse(second.getMessage().contains("(cached)"), second.getMessage());
        assertEquals(runs, Refuses.RUNS.get());     // remembered: the constructor is not run again
    }
}
