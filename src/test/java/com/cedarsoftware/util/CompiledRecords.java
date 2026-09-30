package com.cedarsoftware.util;

import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Records for tests, compiled at run time (java-util compiles for Java 8, so a test cannot declare one): in package
 * {@code recs}, a package-private {@code PkgPoint(int x, int y)}, a public {@code PubPoint(int x, int y)}, a public
 * {@code Throwing(int x)} whose accessor throws, and, inside {@code Make}, a private {@code Priv(int x, String s)}
 * and a package-private {@code Arr(int[] values)}. {@link #make} calls Make's static factory of that name.
 */
final class CompiledRecords implements AutoCloseable {

    private final URLClassLoader loader;

    CompiledRecords(Path dir) throws Exception {
        JavaCompiler javac = ToolProvider.getSystemJavaCompiler();
        assumeTrue(javac != null, "a JDK compiler is needed to make the records");
        Path pkg = Files.createDirectories(dir.resolve("recs"));
        write(pkg.resolve("PkgPoint.java"), "package recs; record PkgPoint(int x, int y) {}");
        write(pkg.resolve("PubPoint.java"), "package recs; public record PubPoint(int x, int y) {}");
        write(pkg.resolve("Throwing.java"), "package recs; public record Throwing(int x) {"
                + " public int x() { throw new IllegalStateException(\"refused\"); } }");
        write(pkg.resolve("Make.java"), "package recs; public class Make {"
                + " private record Priv(int x, String s) {}"
                + " record Arr(int[] values) {}"
                + " public static Object pkg(int x, int y) { return new PkgPoint(x, y); }"
                + " public static Object pub(int x, int y) { return new PubPoint(x, y); }"
                + " public static Object priv(int x, String s) { return new Priv(x, s); }"
                + " public static Object arr(int... values) { return new Arr(values); }"
                + " public static Object throwing(int x) { return new Throwing(x); } }");
        int status = javac.run(null, null, null, "-d", dir.toString(),
                pkg.resolve("PkgPoint.java").toString(), pkg.resolve("PubPoint.java").toString(),
                pkg.resolve("Throwing.java").toString(), pkg.resolve("Make.java").toString());
        assertEquals(0, status, "records compile");
        loader = new URLClassLoader(new URL[]{dir.toUri().toURL()}, CompiledRecords.class.getClassLoader());
    }

    private static void write(Path file, String source) throws Exception {
        Files.write(file, source.getBytes(StandardCharsets.UTF_8));
    }

    Object make(String what, Object... args) throws Exception {
        Class<?> make = loader.loadClass("recs.Make");
        for (Method m : make.getMethods()) {
            if (m.getName().equals(what)) {
                return m.getParameterCount() == 1 && m.getParameterTypes()[0] == int[].class
                        ? m.invoke(null, (Object) args[0]) : m.invoke(null, args);
            }
        }
        throw new IllegalArgumentException(what);
    }

    @Override
    public void close() throws Exception {
        loader.close();
    }
}
