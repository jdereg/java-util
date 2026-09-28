package com.cedarsoftware.util;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A Map of arguments whose keys are not all Strings is handled as the documentation says, not with a raw
 * ClassCastException or NullPointerException: named matching uses the String keys it finds, an ordered Map falls
 * back to its values in order, and one that would have to be ordered by name is an IllegalArgumentException.
 */
class ClassUtilitiesMapArgumentKeysTest {

    public static class S2 {
        final String a;
        final String b;

        public S2(String a, String b) {
            this.a = a;
            this.b = b;
        }
    }

    @Test
    void anUnorderedMapWithAKeyThatIsNotAStringIsAnIllegalArgument() {
        Map<Object, Object> integerKey = new HashMap<>();
        integerKey.put(1, "v");
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> ClassUtilities.newInstance(S2.class, integerKey));
        assertTrue(e.getMessage().contains("java.lang.Integer"), e.getMessage());

        Map<Object, Object> nullKey = new HashMap<>();
        nullKey.put(null, "v");
        nullKey.put("a", "x");
        e = assertThrows(IllegalArgumentException.class, () -> ClassUtilities.newInstance(S2.class, nullKey));
        assertTrue(e.getMessage().contains("null"), e.getMessage());
    }

    @Test
    void anOrderedMapFallsBackToItsValuesInOrder() {
        Map<Object, Object> args = new LinkedHashMap<>();
        args.put(1, "x");
        args.put(2, "y");
        S2 s = (S2) ClassUtilities.newInstance(S2.class, args);
        assertEquals("x", s.a);
        assertEquals("y", s.b);
    }

    @Test
    void namedMatchingIgnoresAKeyThatNamesNoParameter() {
        Map<Object, Object> args = new HashMap<>();
        args.put("a", "x");
        args.put("b", "y");
        args.put(7, "junk");
        args.put(null, "junk");
        S2 s = (S2) ClassUtilities.newInstance(S2.class, args);
        assertEquals("x", s.a);
        assertEquals("y", s.b);
    }
}
