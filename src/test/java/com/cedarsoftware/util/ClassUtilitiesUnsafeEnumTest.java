package com.cedarsoftware.util;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * With unsafe instantiation on, newInstance() of an enum is refused rather than answered with a fake constant (name
 * null, ordinal 0, equal to none of the real ones). Unsafe instantiation still serves an ordinary class.
 */
class ClassUtilitiesUnsafeEnumTest {

    public enum Color { RED, GREEN }

    public enum Op {
        PLUS {
            @Override
            int apply(int a, int b) {
                return a + b;
            }
        };

        abstract int apply(int a, int b);
    }

    /** No constructor newInstance() can call: its only one throws. */
    public static class Refuses {
        final String value;

        public Refuses() {
            throw new IllegalStateException("no");
        }
    }

    @AfterEach
    void unsafeOff() {
        ClassUtilities.setUseUnsafe(false);
    }

    @Test
    void anEnumIsNeverMadeUnsafely() {
        ClassUtilities.setUseUnsafe(true);
        for (int call = 1; call <= 2; call++) {     // the second call takes the cached failure
            assertThrows(IllegalArgumentException.class, () -> ClassUtilities.newInstance(Color.class, null));
            assertThrows(IllegalArgumentException.class,
                    () -> ClassUtilities.newInstance(Op.PLUS.getClass(), null));
        }
    }

    @Test
    void anOrdinaryClassIsStillMadeUnsafely() {
        ClassUtilities.setUseUnsafe(true);
        Refuses r = (Refuses) ClassUtilities.newInstance(Refuses.class, null);
        assertNull(r.value);
        assertEquals(Refuses.class, r.getClass());
    }
}
