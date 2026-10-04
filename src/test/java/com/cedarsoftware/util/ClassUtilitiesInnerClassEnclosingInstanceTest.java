package com.cedarsoftware.util;

import java.util.Arrays;
import java.util.Collections;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * newInstance() of a non-static inner class gives it an enclosing instance on every call, and uses one supplied among
 * the arguments. The first call cached its (Outer, ...) constructor as an ordinary plan; replaying that plan matched
 * the Outer parameter like any other and passed null, so the second call's instance had no enclosing instance. And an
 * enclosing instance passed in the arguments was ignored for a newly made one.
 */
class ClassUtilitiesInnerClassEnclosingInstanceTest {

    public static class Outer {
        final String tag;

        public Outer() {
            tag = "made";
        }

        public Outer(String tag) {
            this.tag = tag;
        }

        public class Inner {
            public Outer outer() {
                return Outer.this;
            }
        }

        public class Named {
            final String name;

            public Named(String name) {
                this.name = name;
            }

            public Outer outer() {
                return Outer.this;
            }
        }
    }

    @Test
    void everyCallGetsAnEnclosingInstance() {
        for (int call = 1; call <= 3; call++) {
            Outer.Inner inner = (Outer.Inner) ClassUtilities.newInstance(Outer.Inner.class, Collections.emptyList());
            assertNotNull(inner.outer(), "call " + call);
            Outer.Named named = (Outer.Named) ClassUtilities.newInstance(Outer.Named.class, Arrays.asList("n" + call));
            assertNotNull(named.outer(), "call " + call);
            assertEquals("n" + call, named.name);
        }
    }

    @Test
    void anEnclosingInstanceAmongTheArgumentsIsTheOneUsed() {
        for (int call = 1; call <= 3; call++) {
            Outer mine = new Outer("mine" + call);
            Outer.Named named = (Outer.Named) ClassUtilities.newInstance(Outer.Named.class, Arrays.asList(mine, "x"));
            assertSame(mine, named.outer(), "call " + call);
            assertEquals("x", named.name);

            Outer.Inner inner = (Outer.Inner) ClassUtilities.newInstance(Outer.Inner.class,
                    Collections.singletonList(mine));
            assertSame(mine, inner.outer(), "call " + call);
        }
    }
}
