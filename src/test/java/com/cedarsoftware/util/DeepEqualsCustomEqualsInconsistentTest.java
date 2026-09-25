package com.cedarsoftware.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * GitHub issue #362: DeepEquals threw NullPointerException (from getContainingDescription, while building
 * the diff) when a class's own equals() returned false for two instances that are nevertheless equal
 * field by field.
 * <p>
 * DeepEquals honors a custom equals() by default, and when it returns false re-compares the two objects
 * field by field to explain why. When the fields are all equal there is no field to blame, so that path
 * returned false without recording any difference, breaking the invariant every other false path keeps
 * (a descriptive node is on the stack) -- and the diff builder dereferenced the missing description.
 */
class DeepEqualsCustomEqualsInconsistentTest {

    /** The reporter's reproduction: identity equals(), equal fields. */
    static class IdentityEquals {
        final String value = "x";

        @Override
        public boolean equals(Object o) {
            return this == o;
        }

        @Override
        public int hashCode() {
            return System.identityHashCode(this);
        }
    }

    /** equals() that is simply always false for a different instance, with a nested field. */
    static class NeverEqual {
        final int id;
        final List<String> tags;

        NeverEqual(int id, String... tags) {
            this.id = id;
            this.tags = new ArrayList<>(Arrays.asList(tags));
        }

        @Override
        public boolean equals(Object o) {
            return this == o;
        }

        @Override
        public int hashCode() {
            return id;
        }
    }

    static class Holder {
        final String name;
        final Object payload;

        Holder(String name, Object payload) {
            this.name = name;
            this.payload = payload;
        }
    }

    @Test
    void reporterReproductionReturnsFalseWithoutThrowing() {
        assertFalse(DeepEquals.deepEquals(new IdentityEquals(), new IdentityEquals()));
    }

    @Test
    void diffNamesTheCustomEqualsAsTheReason() {
        Map<String, Object> options = new HashMap<>();
        assertFalse(DeepEquals.deepEquals(new IdentityEquals(), new IdentityEquals(), options));
        String diff = (String) options.get("diff");
        assertNotNull(diff);
        assertTrue(diff.contains("custom equals"), diff);
    }

    /**
     * Nested, the false return used to leave the diff pointing at whatever sibling was still pending on
     * the stack: here the unrelated field "name", with identical Expected and Found values.
     */
    @Test
    void nestedDiffPointsAtTheFieldWhoseEqualsFailed() {
        Map<String, Object> options = new HashMap<>();
        Holder h1 = new Holder("a", new NeverEqual(1, "t"));
        Holder h2 = new Holder("a", new NeverEqual(1, "t"));
        assertFalse(DeepEquals.deepEquals(h1, h2, options));
        String diff = (String) options.get("diff");
        assertNotNull(diff);
        String firstLine = diff.split("\n")[0];
        assertTrue(firstLine.endsWith(".payload"), "the path must end at the failing field: " + diff);
        assertTrue(firstLine.startsWith("[custom equals() returned false]"), diff);
        assertTrue(diff.contains("NeverEqual.equals() returned false, but every field is equal"), diff);
    }

    @Test
    void insideACollectionAMapAndAnArray() {
        Map<String, Object> options = new HashMap<>();
        assertFalse(DeepEquals.deepEquals(Collections.singletonList(new IdentityEquals()),
                Collections.singletonList(new IdentityEquals()), options));
        assertTrue(String.valueOf(options.get("diff")).contains("custom equals() returned false"), String.valueOf(options.get("diff")));

        options.clear();
        assertFalse(DeepEquals.deepEquals(Collections.singletonMap("k", new IdentityEquals()),
                Collections.singletonMap("k", new IdentityEquals()), options));
        assertTrue(String.valueOf(options.get("diff")).contains("custom equals() returned false"), String.valueOf(options.get("diff")));

        options.clear();
        assertFalse(DeepEquals.deepEquals(new Object[] {new IdentityEquals()}, new Object[] {new IdentityEquals()}, options));
        assertTrue(String.valueOf(options.get("diff")).contains("custom equals() returned false"), String.valueOf(options.get("diff")));
    }

    /** A custom-equals object inside another: the innermost one is reported. */
    @Test
    void customEqualsInsideCustomEquals() {
        Map<String, Object> options = new HashMap<>();
        Holder inner1 = new Holder("in", new IdentityEquals());
        Holder inner2 = new Holder("in", new IdentityEquals());
        assertFalse(DeepEquals.deepEquals(new NeverEqualWrapper(inner1), new NeverEqualWrapper(inner2), options));
        String diff = String.valueOf(options.get("diff"));
        assertTrue(diff.contains("IdentityEquals.equals() returned false"), diff);
    }

    static class NeverEqualWrapper {
        final Object inside;

        NeverEqualWrapper(Object inside) {
            this.inside = inside;
        }

        @Override
        public boolean equals(Object o) {
            return this == o;
        }

        @Override
        public int hashCode() {
            return 1;
        }
    }

    /** With no depth budget left to look inside, the objects are reported as unequal values, not skipped. */
    @Test
    void exhaustedDepthBudgetStillDescribesTheMismatch() {
        String key = "deepequals.max.recursion.depth";
        String prior = System.getProperty(key);
        System.setProperty(key, "1");   // the payload node sits exactly at the limit: no budget to look inside
        DeepEquals.reloadSecurityProperties();
        try {
            Map<String, Object> options = new HashMap<>();
            Holder h1 = new Holder("a", new IdentityEquals());
            Holder h2 = new Holder("a", new IdentityEquals());
            assertFalse(DeepEquals.deepEquals(h1, h2, options));
            String diff = String.valueOf(options.get("diff"));
            assertTrue(diff.split("\n")[0].endsWith(".payload"), diff);
        } finally {
            if (prior == null) {
                System.clearProperty(key);
            } else {
                System.setProperty(key, prior);
            }
            DeepEquals.reloadSecurityProperties();
        }
    }

    @Test
    void aRealFieldDifferenceIsStillReportedAsSuch() {
        Map<String, Object> options = new HashMap<>();
        assertFalse(DeepEquals.deepEquals(new NeverEqual(1, "t"), new NeverEqual(2, "t"), options));
        String diff = (String) options.get("diff");
        assertNotNull(diff);
        assertTrue(diff.contains("id"), diff);
    }

    @Test
    void sameInstanceIsEqual() {
        IdentityEquals x = new IdentityEquals();
        assertTrue(DeepEquals.deepEquals(x, x));
    }

    /** With custom equals() ignored, the field-by-field answer stands: equal. */
    @Test
    void ignoringCustomEqualsComparesFields() {
        Map<String, Object> options = new HashMap<>();
        options.put(DeepEquals.IGNORE_CUSTOM_EQUALS, Collections.emptySet());
        assertTrue(DeepEquals.deepEquals(new IdentityEquals(), new IdentityEquals(), options));

        Map<String, Object> onlyThisClass = new HashMap<>();
        onlyThisClass.put(DeepEquals.IGNORE_CUSTOM_EQUALS, Collections.singleton(IdentityEquals.class));
        assertTrue(DeepEquals.deepEquals(new IdentityEquals(), new IdentityEquals(), onlyThisClass));
    }
}
