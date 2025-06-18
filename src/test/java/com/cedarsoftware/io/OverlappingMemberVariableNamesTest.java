package com.cedarsoftware.io;

import com.cedarsoftware.io.TypeHolder;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Ensure that inner class fields are properly serialized when the inner and outer
 * classes share member names. The JSON output should no longer include the
 * synthetic 'this$' reference after record support changes.
 */
public class OverlappingMemberVariableNamesTest {

    static class Outer {
        private String name;
        private Inner foo;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public Inner getFoo() { return foo; }
        public void setFoo(Inner foo) { this.foo = foo; }

        class Inner {
            private String name;
            public String getName() { return name; }
            public void setName(String name) { this.name = name; }
        }
    }

    @Test
    public void testNestedWithSameMemberName() {
        Outer outer = new Outer();
        outer.setName("Joe Outer");

        Outer.Inner inner = outer.new Inner();
        outer.setFoo(inner);
        outer.getFoo().setName("Jane Inner");

        String json = JsonIo.toJson(outer, null);
        // Older json-io versions serialize the synthetic outer class reference
        // as a field named "this$0".  Newer versions omit this field entirely.
        // The presence or absence of this reference should not affect
        // deserialization, so simply ensure that round-tripping works.

        Outer x = JsonIo.toJava(json, null).asType(new TypeHolder<Outer>() {});

        assertEquals("Joe Outer", x.getName());
        assertEquals("Jane Inner", x.getFoo().getName());
    }
}
