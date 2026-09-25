package com.cedarsoftware.util;

import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.TimeZone;
import java.util.stream.Stream;

import com.cedarsoftware.io.JsonIo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * The 4.113.0 additions to the ClassUtilities block list: JDK types whose constructors write, truncate
 * or delete a caller-named file, resolve a host name, open a socket, start a thread or register a
 * shutdown hook, found by constructing every JDK constructor with caller-style arguments under a
 * recording SecurityManager (JDK 21 public constructors; JDK 8 all constructors). Plus the JDK/vendor
 * internal packages, blocked as a family with two exemptions JSON legitimately needs.
 * <p>
 * Every probe here is side-effect free: the gate runs before any constructor is chosen, and no probe
 * passes a path, host or mode, so even a regressed block cannot write, delete or connect.
 */
class ClassUtilitiesSideEffectConstructorBlockTest {

    /** Blocked directly, or through a blocked supertype (JarFile via ZipFile, FileHandler via Handler). */
    static Stream<String> blockedJdkClasses() {
        return Stream.of(
                // create or truncate the named file
                "java.io.PrintStream",
                "java.io.PrintWriter",
                "java.util.Formatter",
                "javax.imageio.stream.FileImageOutputStream",
                // delete the named file (OPEN_DELETE)
                "java.util.zip.ZipFile",
                "java.util.jar.JarFile",
                // resolve a host name / open a socket
                "java.net.InetSocketAddress",
                "java.util.logging.SocketHandler",
                // logging handlers (whole family) and LogManager
                "java.util.logging.Handler",
                "java.util.logging.FileHandler",
                "java.util.logging.StreamHandler",
                "java.util.logging.ConsoleHandler",
                "java.util.logging.MemoryHandler",
                "java.util.logging.LogManager",
                // start a thread
                "java.util.Timer",
                // blocked packages
                "java.util.prefs.Preferences",
                "java.util.prefs.AbstractPreferences",
                // dynamic proxies and the JDK handler that dispatches to arbitrary methods
                "java.lang.reflect.Proxy",
                "java.beans.EventHandler");
    }

    /** Every proxy class extends java.lang.reflect.Proxy: jdk.proxyN.$ProxyN (16+), com.sun.proxy.$ProxyN (8-15). */
    @Test
    void runtimeProxyClassIsBlocked() {
        Object proxy = java.lang.reflect.Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[] {Runnable.class}, (p, m, args) -> null);
        Class<?> proxyClass = proxy.getClass();
        assertTrue(ClassUtilities.SecurityChecker.isSecurityBlocked(proxyClass), proxyClass.getName());
        assertThrows(SecurityException.class, () -> ClassUtilities.SecurityChecker.verifyClass(proxyClass));
        assertThrows(SecurityException.class, () -> ClassUtilities.forName(proxyClass.getName(), null));
        assertThrows(SecurityException.class, () -> ClassUtilities.newInstance(proxyClass, null));
    }

    @ParameterizedTest
    @MethodSource("blockedJdkClasses")
    void blockedByName(String className) {
        assertTrue(ClassUtilities.SecurityChecker.isSecurityBlockedName(className)
                        || ClassUtilities.SecurityChecker.isSecurityBlocked(load(className)),
                className + " should be blocked");
        assertThrows(SecurityException.class, () -> ClassUtilities.forName(className, null),
                "forName must refuse " + className);
    }

    @ParameterizedTest
    @MethodSource("blockedJdkClasses")
    void blockedAsLoadedClassAndForInstantiation(String className) {
        Class<?> c = load(className);
        assertTrue(ClassUtilities.SecurityChecker.isSecurityBlocked(c), className + " should be blocked");
        assertThrows(SecurityException.class, () -> ClassUtilities.SecurityChecker.verifyClass(c));
        assertThrows(SecurityException.class, () -> ClassUtilities.newInstance(c, null),
                "newInstance must refuse " + className);
    }

    /** A caller's own subclass is caught by the supertype walk. */
    static class MyPrintStream extends PrintStream {
        MyPrintStream(OutputStream out) {
            super(out);
        }
    }

    @Test
    void subclassOfBlockedTypeIsBlocked() {
        assertTrue(ClassUtilities.SecurityChecker.isSecurityBlocked(MyPrintStream.class));
        assertThrows(SecurityException.class, () -> ClassUtilities.newInstance(MyPrintStream.class, null));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "jdk.jshell.JShell",
            "jdk.jshell.execution.LocalExecutionControl",
            "jdk.jfr.Recording",
            "jdk.jfr.consumer.RecordingStream",
            "jdk.management.jfr.RemoteRecordingStream"})
    void blockedJdkPackagesByName(String className) {
        // JShell and JFR are absent from JDK 8, so assert on the name; forName must refuse either way.
        assertTrue(ClassUtilities.SecurityChecker.isSecurityBlockedName(className));
        assertThrows(SecurityException.class, () -> ClassUtilities.forName(className, null));
    }

    // ---------------------------------------------------------------------------------------------
    // Internal packages
    // ---------------------------------------------------------------------------------------------

    @ParameterizedTest
    @ValueSource(strings = {
            "sun.nio.ch.Util",
            "sun.reflect.ReflectionFactory",
            "sun.security.provider.SecureRandom",
            "sun.util.calendar.Gregorian",
            "com.sun.proxy.$Proxy5",
            "com.sun.org.apache.xerces.internal.dom.DocumentImpl",
            "com.apple.concurrent.LibDispatchRetainedResource",
            "apple.laf.JRSUIConstants"})
    void internalPackagesBlockedByName(String className) {
        assertTrue(ClassUtilities.SecurityChecker.isSecurityBlockedName(className), className);
    }

    @Test
    void loadedInternalClassIsBlocked() throws Exception {
        // Present on JDK 8 through 26; loading a class in a non-exported package is permitted.
        Class<?> internal = Class.forName("sun.nio.ch.Util", false, ClassLoader.getSystemClassLoader());
        assertTrue(ClassUtilities.SecurityChecker.isSecurityBlocked(internal));
        assertThrows(SecurityException.class, () -> ClassUtilities.SecurityChecker.verifyClass(internal));
    }

    /** The internal-package rule matches the class itself, not a public class implementing an internal type. */
    @Test
    void publicClassImplementingInternalInterfaceIsNotBlocked() throws Exception {
        Class<?> direct = java.nio.ByteBuffer.allocateDirect(1).getClass();   // java.nio.DirectByteBuffer
        boolean implementsInternal = false;
        for (Class<?> i : direct.getInterfaces()) {
            implementsInternal |= i.getName().startsWith("sun.");
        }
        assumeTrue(implementsInternal, "DirectByteBuffer implements sun.nio.ch.DirectBuffer on this JDK");
        assertFalse(ClassUtilities.SecurityChecker.isSecurityBlocked(direct));
        assertSame(direct, ClassUtilities.forName(direct.getName(), null));
    }

    @Test
    void prefixMatchIsByPackageNotSubstring() {
        assertFalse(ClassUtilities.SecurityChecker.isSecurityBlockedName("com.example.sun.Widget"));
        assertFalse(ClassUtilities.SecurityChecker.isSecurityBlockedName("sunshine.Widget"));
        assertFalse(ClassUtilities.SecurityChecker.isSecurityBlockedName("com.sunrise.Widget"));
    }

    @Test
    void zoneInfoIsExempt() {
        Class<?> zoneInfo = TimeZone.getDefault().getClass();
        assumeTrue("sun.util.calendar.ZoneInfo".equals(zoneInfo.getName()), "JDK TimeZone implementation");
        assertFalse(ClassUtilities.SecurityChecker.isSecurityBlocked(zoneInfo));
        assertFalse(ClassUtilities.SecurityChecker.isSecurityBlockedName(zoneInfo.getName()));
        assertSame(zoneInfo, ClassUtilities.forName("sun.util.calendar.ZoneInfo", null));
    }

    @Test
    void charsetImplementationsAreExempt() {
        Class<?> utf8 = StandardCharsets.UTF_8.getClass();
        assumeTrue(utf8.getName().startsWith("sun.nio.cs."), "JDK Charset implementation");
        assertFalse(ClassUtilities.SecurityChecker.isSecurityBlocked(utf8));
        assertSame(utf8, ClassUtilities.forName(utf8.getName(), null));
    }

    @ParameterizedTest
    @ValueSource(strings = {"sun.misc.Unsafe", "com.sun.jndi.ldap.LdapCtx", "com.sun.rowset.JdbcRowSetImpl",
            "com.sun.org.apache.xalan.internal.xsltc.trax.TemplatesImpl"})
    void exemptionsNeverOverrideOtherRules(String className) {
        assertTrue(ClassUtilities.SecurityChecker.isSecurityBlockedName(className), className);
    }

    // ---------------------------------------------------------------------------------------------
    // Through json-io: the exempt internal types still round-trip; a blocked type is refused.
    // ---------------------------------------------------------------------------------------------

    public static class Holder {
        public Object zone;
        public Object path;
    }

    // (A Charset in an Object field is not round-tripped here: json-io writes it as an empty
    // sun.nio.cs.* POJO that no JDK 16+ can instantiate -- a json-io defect independent of the block list.)
    @Test
    void jsonRoundTripOfExemptInternalTypes() {
        Holder h = new Holder();
        h.zone = TimeZone.getTimeZone("America/New_York");
        h.path = java.nio.file.Paths.get("a", "b.txt");
        String json = JsonIo.toJson(h, null);
        Holder back = JsonIo.toJava(json, null).asClass(Holder.class);
        assertNotNull(back.zone);
        assertEquals("America/New_York", ((TimeZone) back.zone).getID());
        assertEquals(h.path, back.path);
    }

    @Test
    void pathImplementationIsExempt() {
        Class<?> pathClass = java.nio.file.Paths.get("x").getClass();
        assumeTrue(pathClass.getName().startsWith("sun.nio.fs."), "JDK Path implementation");
        assertFalse(ClassUtilities.SecurityChecker.isSecurityBlocked(pathClass));
        assertSame(pathClass, ClassUtilities.forName(pathClass.getName(), null));
    }

    /**
     * json-io resolves the class names in its config files through ClassUtilities.forName() while its
     * option builders initialize; a blocked name there is a SecurityException in a static initializer,
     * which takes json-io down entirely. Guard every name json-io configures that exists on this JVM.
     */
    @ParameterizedTest
    @ValueSource(strings = {"aliases.txt", "classFactory.txt", "coercedTypes.txt", "customReaders.txt",
            "customWriters.txt", "nonRefs.txt", "notCustomRead.txt", "notCustomWritten.txt"})
    void noClassJsonIoConfiguresIsBlocked(String configFile) throws Exception {
        java.net.URL url = JsonIo.class.getResource("/config/" + configFile);
        assumeTrue(url != null, "json-io config file present: " + configFile);
        String text;
        try (java.io.InputStream in = url.openStream()) {
            text = new String(IOUtilities.inputStreamToBytes(in), StandardCharsets.UTF_8);
        }
        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("\\b([a-z][a-z0-9_]*(?:\\.[a-z0-9_]+)*\\.[A-Z][A-Za-z0-9_$]*)").matcher(text);
        java.util.List<String> blocked = new java.util.ArrayList<>();
        int resolved = 0;
        while (m.find()) {
            Class<?> c;
            try {
                c = Class.forName(m.group(1), false, JsonIo.class.getClassLoader());
            } catch (ClassNotFoundException | LinkageError e) {
                continue;
            }
            resolved++;
            if (ClassUtilities.SecurityChecker.isSecurityBlocked(c)) {
                blocked.add(c.getName());
            }
        }
        assertTrue(resolved > 0 || !"aliases.txt".equals(configFile), "parsed no class names from " + configFile);
        assertTrue(blocked.isEmpty(), configFile + " names blocked classes: " + blocked);
    }

    @Test
    void jsonNamingABlockedTypeIsRefused() {
        String json = "{\"@type\":\"java.util.Timer\"}";
        Throwable t = assertThrows(Throwable.class, () -> JsonIo.toJava(json, null).asClass(Object.class));
        assertTrue(causeChainHas(t, SecurityException.class), "expected a SecurityException in " + t);
    }

    private static boolean causeChainHas(Throwable t, Class<? extends Throwable> type) {
        for (Throwable c = t; c != null; c = c.getCause() == c ? null : c.getCause()) {
            if (type.isInstance(c)) {
                return true;
            }
        }
        return false;
    }

    private static Class<?> load(String className) {
        try {
            return Class.forName(className, false, ClassLoader.getSystemClassLoader());
        } catch (ClassNotFoundException e) {
            throw new AssertionError("JDK class missing: " + className, e);
        }
    }
}
