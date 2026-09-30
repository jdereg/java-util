package com.cedarsoftware.util;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * DeepEquals compares, hashes and reports an object's instance fields other than synthetic and transient ones, from a
 * list ReflectionUtils filters once per class. An inner class's reference to its outer object is synthetic, and a
 * transient field is by declaration not part of the state: neither may make two objects unequal, change their hash,
 * or appear in a difference report.
 */
class DeepEqualsComparedFieldsTest {

    static class Outer {
        final int tag;

        Outer(int tag) {
            this.tag = tag;
        }

        class Inner {
            final Object value;

            Inner(Object value) {
                this.value = value;
            }

            int outerTag() {
                return Outer.this.tag;     // keeps the synthetic reference to the outer object
            }
        }
    }

    static class WithTransient {
        static int shared = 7;
        final Object value;
        transient int cachedTotal;

        WithTransient(Object value, int cachedTotal) {
            this.value = value;
            this.cachedTotal = cachedTotal;
        }
    }

    @Test
    void anInnerObjectIsNotComparedByItsOuterObject() {
        Object a = new Outer(1).new Inner("x");
        Object b = new Outer(2).new Inner("x");
        assertTrue(DeepEquals.deepEquals(a, b));
        assertEquals(DeepEquals.deepHashCode(a), DeepEquals.deepHashCode(b));
        assertFalse(DeepEquals.deepEquals(new Outer(1).new Inner("x"), new Outer(1).new Inner("y")));
    }

    @Test
    void aTransientFieldIsNotCompared() {
        WithTransient a = new WithTransient("v", 1);
        WithTransient b = new WithTransient("v", 2);
        assertTrue(DeepEquals.deepEquals(a, b));
        assertEquals(DeepEquals.deepHashCode(a), DeepEquals.deepHashCode(b));
        assertFalse(DeepEquals.deepEquals(new WithTransient("v", 1), new WithTransient("w", 1)));
    }

    @Test
    void aDifferenceReportShowsOnlyTheComparedFields() {
        Map<String, Object> options = new HashMap<>();
        Object a = Collections.singletonList(new Outer(1).new Inner(new WithTransient("v", 1)));
        Object b = Collections.singletonList(new Outer(2).new Inner(new WithTransient("w", 2)));
        assertFalse(DeepEquals.deepEquals(a, b, options));
        String diff = (String) options.get(DeepEquals.DIFF);
        assertTrue(diff.contains("value"), diff);
        assertFalse(diff.contains("this$0"), diff);
        assertFalse(diff.contains("cachedTotal"), diff);
        assertFalse(diff.contains("shared"), diff);
    }
}
