package com.cedarsoftware.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * ReflectionUtils.call(instance, name, args) calls the overload Java would call with those arguments (JLS 15.12.2):
 * of the methods with that name and as many parameters, those the arguments can be passed to without unboxing (else
 * with unboxing and widening), and of those the most specific. It took the method getMethod(instance, name, count)
 * chose, whatever the arguments: for an Integer it could take process(String) and fail with "argument type
 * mismatch" -- and which it took followed the order the JVM lists methods in, which varies from run to run. When
 * Java would find the call ambiguous, or no overload takes the arguments, getMethod's choice stands, as before.
 */
class ReflectionUtilsCallOverloadTest {

    public static class Base {
        public String process(Object o) {
            return "Object";
        }

        public String process(Integer i) {
            return "Integer";
        }
    }

    public static class Target extends Base {
        public String process(String s) {
            return "String";
        }

        public String process(CharSequence c) {
            return "CharSequence";
        }

        public String scale(long l) {
            return "long";
        }

        public String scale(double d) {
            return "double";
        }

        public String box(long l) {
            return "long";
        }

        public String box(Long l) {
            return "Long";
        }

        public static String stat(Number n) {
            return "Number";
        }

        public static String stat(Integer i) {
            return "Integer";
        }
    }

    public static class Override extends Target {
        @java.lang.Override
        public String process(String s) {
            return "Override.String";
        }
    }

    private static Object call(Object target, String name, Object... args) {
        return ReflectionUtils.call(target, name, args);
    }

    @Test
    void theMostSpecificOverloadTheArgumentsFit() {
        Target t = new Target();
        assertEquals("String", call(t, "process", "s"));
        assertEquals("CharSequence", call(t, "process", new StringBuilder("sb")));
        assertEquals("Integer", call(t, "process", 5));
        assertEquals("Object", call(t, "process", 2.5d));
    }

    @Test
    void unboxingOnlyWhenNoOverloadTakesTheArgumentAsItIs() {
        Target t = new Target();
        assertEquals("Long", call(t, "box", 7L));
        assertEquals("long", call(t, "scale", 5), "an int widens to long before double");
        assertEquals("double", call(t, "scale", 2.5f), "a float widens only to double");
    }

    @Test
    void anOverrideIsCalledAndStaticsToo() {
        assertEquals("Override.String", call(new Override(), "process", "s"));
        assertEquals("Integer", call(Target.class, "stat", 5));
        assertEquals("Number", call(Target.class, "stat", 5L));
    }

    @Test
    void theChoiceFollowsTheArgumentsOnEveryCall() {
        Target t = new Target();
        for (int i = 0; i < 3; i++) {
            assertEquals("Integer", call(t, "process", 5));
            assertEquals("String", call(t, "process", "s"));
            assertEquals("Object", call(t, "process", 2.5d));
        }
    }
}
