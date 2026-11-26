package com.cedarsoftware.util;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The 4.119.0 once-over of ReflectionUtils: an enum's own fields named like java.lang.Enum's; one choice among
 * overloads on every JVM; {@code call()} choosing among the methods the class has, as Java does, remembering its
 * choices without holding on to the arguments' classes, and reaching methods of JDK classes that cannot be opened;
 * a bridge method not counted as an overload; and the most specific of two interfaces' declarations.
 */
class ReflectionUtilsOnceOverTest {

    // ---------------------------------------------------------------------------------------------------------
    // fields
    // ---------------------------------------------------------------------------------------------------------

    enum Digest {
        SHA256("e3b0c442", 3);

        private final String hash;
        private final int ordinal;
        private final String label = "sha";

        Digest(String hash, int ordinal) {
            this.hash = hash;
            this.ordinal = ordinal;
        }
    }

    /**
     * The default filter leaves out java.lang.Enum's own "hash" and "ordinal". It left out any enum's: tested by
     * "is the declaring class an Enum", which every enum is, so json-io wrote this enum without two of its fields.
     */
    @Test
    void anEnumsOwnFieldsNamedHashAndOrdinalAreItsFields() throws Exception {
        List<String> own = new ArrayList<>();
        for (Field field : ReflectionUtils.getDeclaredFields(Digest.class)) {
            own.add(field.getName());
        }
        assertEquals(Arrays.asList("hash", "ordinal", "label"), own);

        List<String> all = new ArrayList<>();
        for (Field field : ReflectionUtils.getAllDeclaredFields(Digest.class)) {
            all.add(field.getDeclaringClass().getSimpleName() + "." + field.getName());
        }
        assertTrue(all.containsAll(Arrays.asList("Digest.hash", "Digest.ordinal", "Digest.label", "Enum.name")),
                all.toString());
        assertFalse(all.contains("Enum.ordinal") || all.contains("Enum.hash"), "java.lang.Enum's own: " + all);
        assertEquals("e3b0c442", ReflectionUtils.getField(Digest.class, "hash").get(Digest.SHA256));
        assertEquals(3, ReflectionUtils.getField(Digest.class, "ordinal").get(Digest.SHA256));
    }

    // ---------------------------------------------------------------------------------------------------------
    // which method
    // ---------------------------------------------------------------------------------------------------------

    public static class Service {
        public String process(String value) {
            return "String";
        }

        public String process(Object value) {
            return "Object";
        }

        public String process(Integer value) {
            return "Integer";
        }
    }

    /**
     * getMethod(instance, name, count) among overloads of one class: the same one on every JVM. It was the last in
     * the JVM's own order of the class's methods -- process(String) on JDK 8, process(Integer) on JDK 25 -- because
     * the comparison called each "less specific" than the other.
     */
    @Test
    void theOverloadChosenByArgumentCountIsTheSameOnEveryJvm() {
        Method chosen = ReflectionUtils.getMethod(new Service(), "process", 1);
        assertEquals("[class java.lang.Integer]", Arrays.toString(chosen.getParameterTypes()),
                "first by the names of the parameter types");
        // and call() still follows the arguments where they decide
        assertEquals("String", ReflectionUtils.call(new Service(), "process", "text"));
        assertEquals("Integer", ReflectionUtils.call(new Service(), "process", 7));
        assertEquals("Object", ReflectionUtils.call(new Service(), "process", 7.5));
    }

    public static class Base {
        private String log(String message) {
            return "Base.log(String), private";
        }

        static String describe(Integer number) {
            return "Base.describe(Integer), static";
        }
    }

    public interface Shape {
        static String area(String unit) {
            return "Shape.area(String), static";
        }

        default String name(Object anything) {
            return "Shape.name(Object), default";
        }
    }

    public static class Child extends Base implements Shape {
        public String log(Object message) {
            return "Child.log(Object)";
        }

        public String area(Object unit) {
            return "Child.area(Object)";
        }

        private String secret(Long number) {
            return "Child.secret(Long), private";
        }
    }

    /**
     * call(object, name, args) chooses among the methods the object's class has. A superclass's private method and
     * an interface's static method are not among them, and Java never calls them through the class; from 4.113.0
     * they were candidates, and being more specific they won.
     */
    @Test
    void callChoosesAmongTheMethodsTheClassHas() {
        Child child = new Child();
        assertEquals(child.log("x"), ReflectionUtils.call(child, "log", "x"));
        assertEquals("Child.log(Object)", ReflectionUtils.call(child, "log", "x"));
        assertEquals(child.area("cm"), ReflectionUtils.call(child, "area", "cm"));
        assertEquals("Child.area(Object)", ReflectionUtils.call(child, "area", "cm"));

        // its own private method, an inherited static and an inherited default are its members
        assertEquals("Child.secret(Long), private", ReflectionUtils.call(child, "secret", 5L));
        assertEquals("Base.describe(Integer), static", ReflectionUtils.call(child, "describe", 5));
        assertEquals("Shape.name(Object), default", ReflectionUtils.call(child, "name", "n"));
        // and a private method of a superclass can still be asked for by name where the class has none of its own
        assertEquals("Base.log(String), private", ReflectionUtils.call(new Base(), "log", "x"));
    }

    public static class Register {
        public String add(Object anything) {
            return "Object";
        }

        public String add(String text) {
            return "String";
        }

        public String add(Integer number) {
            return "Integer";
        }
    }

    /** The choice is remembered for more than the last list of argument classes, and still follows each. */
    @Test
    void callFollowsTheArgumentsWhenTheyAlternate() {
        Register register = new Register();
        Object[] arguments = {"s", 1, 2.5, null, 'c', "t", 3, new ArrayList<>(), 4L, new StringBuilder(), true, "u", 5};
        String[] expected = {"String", "Integer", "Object", null, "Object", "String", "Integer", "Object", "Object",
                "Object", "Object", "String", "Integer"};
        for (int round = 0; round < 50; round++) {
            for (int i = 0; i < arguments.length; i++) {
                if (arguments[i] != null) {
                    assertEquals(expected[i], ReflectionUtils.call(register, "add", arguments[i]), "argument " + i);
                }
            }
        }
    }

    /** What a throwaway loader defines: an argument's class that must not be held on to. */
    public static class Visitor {
        @Override
        public String toString() {
            return "visitor";
        }
    }

    /**
     * call() remembers its choice with the classes of the arguments. Held strongly, on a class that lives as long
     * as the JVM (ArrayList, or an application class called with a plugin's object), an argument's class kept its
     * class loader, and all that loader had loaded, in memory. They are held weakly.
     */
    @Test
    void theClassOfAnArgumentIsNotKeptAlive() throws Exception {
        URL testClasses = ReflectionUtilsOnceOverTest.class.getProtectionDomain().getCodeSource().getLocation();
        ClassLoader jdkOnly = ClassLoader.getSystemClassLoader().getParent();
        URLClassLoader loader = new URLClassLoader(new URL[]{testClasses}, jdkOnly);
        Object visitor = loader.loadClass(Visitor.class.getName()).getConstructor().newInstance();
        assertTrue(visitor.getClass() != Visitor.class && visitor.getClass().getClassLoader() == loader);

        assertEquals(false, ReflectionUtils.call(new ArrayList<>(Arrays.asList("a", "b")), "remove", visitor));
        assertEquals("Object", ReflectionUtils.call(new Register(), "add", visitor));

        WeakReference<ClassLoader> released = new WeakReference<>(loader);
        visitor = null;
        loader.close();
        loader = null;
        for (int i = 0; i < 50 && released.get() != null; i++) {
            System.gc();
            Thread.sleep(40);
        }
        assertNull(released.get(), "the argument's class loader is still reachable");
        // the choices for other arguments are still made: an Integer is an Object to remove(), as it is to javac
        assertEquals(false, ReflectionUtils.call(new ArrayList<>(Arrays.asList("a", "b")), "remove", 1));
        assertEquals(true, ReflectionUtils.call(new ArrayList<>(Arrays.asList("a", "b")), "remove", "a"));
    }

    /**
     * A method of a class the JDK does not open cannot be made accessible on JDK 9+, and invoking it was an
     * IllegalAccessException: the list Arrays.asList() returns, a HashMap's entry, an unmodifiable view. Java calls
     * them through List, Map.Entry and Collection, and so does call() now.
     */
    @Test
    void methodsOfJdkClassesThatCannotBeOpenedAreCalledThroughTheirPublicDeclaration() {
        assertEquals(2, ReflectionUtils.call(Arrays.asList("a", "b"), "size"));
        Map.Entry<String, Integer> entry = new HashMap<>(Collections.singletonMap("k", 1)).entrySet().iterator().next();
        assertEquals("k", ReflectionUtils.call(entry, "getKey"));
        assertEquals(false, ReflectionUtils.call(Collections.unmodifiableList(Arrays.asList(1)), "isEmpty"));
        assertEquals(true, ReflectionUtils.call(Collections.unmodifiableList(Arrays.asList(1, 2)), "contains", 2));
        assertEquals("b", ReflectionUtils.call(Arrays.asList("a", "b"), "get", 1));

        Method size = ReflectionUtils.getMethod(Arrays.asList("a"), "size", 0);
        assertNotNull(size);
        assertEquals(1, ReflectionUtils.call(Arrays.asList("a"), size));
    }

    // ---------------------------------------------------------------------------------------------------------
    // bridges and diamonds
    // ---------------------------------------------------------------------------------------------------------

    public static class Maker implements Supplier<String> {
        @Override
        public String get() {
            return "made";
        }
    }

    /** One get() in the source is two in the class file (the compiler's bridge returns Object): not an overload. */
    @Test
    void aBridgeMethodDoesNotMakeAMethodOverloaded() throws Exception {
        Method get = ReflectionUtils.getNonOverloadedMethod(Maker.class, "get");
        assertEquals(String.class, get.getReturnType());
        assertEquals("made", get.invoke(new Maker()));
    }

    @Retention(RetentionPolicy.RUNTIME)
    @interface Marked {
    }

    public interface Named {
        default Object id() {
            return "Named";
        }
    }

    public interface Special extends Named {
        @Marked
        @Override
        default String id() {
            return "Special";
        }
    }

    public static class Impl implements Named, Special {
    }

    /**
     * Where an interface overrides another's method and a class implements both, the method is the overriding
     * one, with its return type and annotations (JLS 9.4.1; Class.getMethod() on JDK 9+). The first one met in the
     * walk was returned: Named's, an Object without the annotation.
     */
    @Test
    void ofTwoInterfacesDeclarationsTheMoreSpecificIsTheMethod() throws Exception {
        Method id = ReflectionUtils.getMethod(Impl.class, "id");
        assertEquals(Special.class, id.getDeclaringClass());
        assertEquals(String.class, id.getReturnType());
        assertNotNull(ReflectionUtils.getMethodAnnotation(id, Marked.class));
        assertEquals("Special", id.invoke(new Impl()));
        assertEquals("Special", ReflectionUtils.call(new Impl(), "id"));
    }
}
