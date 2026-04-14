package com.cedarsoftware.test;

import java.util.ArrayList;

import com.cedarsoftware.util.ClassUtilities;
import com.cedarsoftware.util.convert.Converter;
import com.cedarsoftware.util.convert.DefaultConverterOptions;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Security validation tests for ClassUtilities, run from external package
 * (com.cedarsoftware.test) to ensure no trusted-caller bypass is possible.
 *
 * Unlike ReflectionUtils, ClassUtilities' security (SecurityChecker) does not
 * have a trusted-caller bypass — it always blocks security-sensitive classes
 * like Runtime, Process, ProcessBuilder, System, ClassLoader, etc.
 */
class ClassUtilitiesSecurityTest {

    private final Converter converter = new Converter(new DefaultConverterOptions());

    // ========== newInstance on security-blocked classes ==========

    @Test
    void testNewInstanceRuntimeBlocked() {
        assertThatThrownBy(() -> ClassUtilities.newInstance(converter, Runtime.class, new ArrayList<>()))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("security reasons");
    }

    @Test
    void testNewInstanceProcessBuilderBlocked() {
        assertThatThrownBy(() -> ClassUtilities.newInstance(converter, ProcessBuilder.class, new ArrayList<>()))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("security reasons");
    }

    @Test
    void testNewInstanceProcessBlocked() {
        assertThatThrownBy(() -> ClassUtilities.newInstance(converter, Process.class, new ArrayList<>()))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("security reasons");
    }

    @Test
    void testNewInstanceSystemBlocked() {
        assertThatThrownBy(() -> ClassUtilities.newInstance(converter, System.class, new ArrayList<>()))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("security reasons");
    }

    @Test
    void testNewInstanceClassLoaderBlocked() {
        assertThatThrownBy(() -> ClassUtilities.newInstance(converter, ClassLoader.class, new ArrayList<>()))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("security reasons");
    }

    // ========== forName on security-blocked class names ==========

    @Test
    void testForNameRuntimeBlocked() {
        assertThatThrownBy(() -> ClassUtilities.forName("java.lang.Runtime", ClassUtilities.getClassLoader()))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("cannot load");
    }

    @Test
    void testForNameProcessBuilderBlocked() {
        assertThatThrownBy(() -> ClassUtilities.forName("java.lang.ProcessBuilder", ClassUtilities.getClassLoader()))
                .isInstanceOf(SecurityException.class);
    }

    @Test
    void testForNameProcessImplBlocked() {
        assertThatThrownBy(() -> ClassUtilities.forName("java.lang.ProcessImpl", ClassUtilities.getClassLoader()))
                .isInstanceOf(SecurityException.class);
    }

    @Test
    void testForNameScriptEngineBlocked() {
        assertThatThrownBy(() -> ClassUtilities.forName("javax.script.ScriptEngineManager", ClassUtilities.getClassLoader()))
                .isInstanceOf(SecurityException.class);
    }

    @Test
    void testForNameScriptPackageBlocked() {
        // Any class under javax.script. should be blocked via package-level blocking
        assertThatThrownBy(() -> ClassUtilities.forName("javax.script.SomeClass", ClassUtilities.getClassLoader()))
                .isInstanceOf(SecurityException.class);
    }

    @Test
    void testForNameNashornPackageBlocked() {
        assertThatThrownBy(() -> ClassUtilities.forName("jdk.nashorn.api.scripting.NashornScriptEngine", ClassUtilities.getClassLoader()))
                .isInstanceOf(SecurityException.class);
    }

    @Test
    void testForNameMethodHandlesLookupBlocked() {
        assertThatThrownBy(() -> ClassUtilities.forName("java.lang.invoke.MethodHandles$Lookup", ClassUtilities.getClassLoader()))
                .isInstanceOf(SecurityException.class);
    }

    @Test
    void testForNameSystemBlocked() {
        assertThatThrownBy(() -> ClassUtilities.forName("java.lang.System", ClassUtilities.getClassLoader()))
                .isInstanceOf(SecurityException.class);
    }

    // ========== SecurityChecker public API ==========

    @Test
    void testIsSecurityBlockedForRuntime() {
        assertThat(ClassUtilities.SecurityChecker.isSecurityBlocked(Runtime.class)).isTrue();
    }

    @Test
    void testIsSecurityBlockedForProcess() {
        assertThat(ClassUtilities.SecurityChecker.isSecurityBlocked(Process.class)).isTrue();
    }

    @Test
    void testIsSecurityBlockedForProcessBuilder() {
        assertThat(ClassUtilities.SecurityChecker.isSecurityBlocked(ProcessBuilder.class)).isTrue();
    }

    @Test
    void testIsSecurityBlockedForSystem() {
        assertThat(ClassUtilities.SecurityChecker.isSecurityBlocked(System.class)).isTrue();
    }

    @Test
    void testIsSecurityBlockedForClassLoader() {
        assertThat(ClassUtilities.SecurityChecker.isSecurityBlocked(ClassLoader.class)).isTrue();
    }

    @Test
    void testIsSecurityBlockedForMethod() {
        assertThat(ClassUtilities.SecurityChecker.isSecurityBlocked(java.lang.reflect.Method.class)).isTrue();
    }

    @Test
    void testIsSecurityBlockedForField() {
        assertThat(ClassUtilities.SecurityChecker.isSecurityBlocked(java.lang.reflect.Field.class)).isTrue();
    }

    @Test
    void testIsSecurityBlockedForConstructor() {
        assertThat(ClassUtilities.SecurityChecker.isSecurityBlocked(java.lang.reflect.Constructor.class)).isTrue();
    }

    @Test
    void testIsSecurityBlockedForSafeClass() {
        assertThat(ClassUtilities.SecurityChecker.isSecurityBlocked(String.class)).isFalse();
        assertThat(ClassUtilities.SecurityChecker.isSecurityBlocked(java.util.ArrayList.class)).isFalse();
    }

    @Test
    void testVerifyClassThrowsOnBlocked() {
        assertThatThrownBy(() -> ClassUtilities.SecurityChecker.verifyClass(Runtime.class))
                .isInstanceOf(SecurityException.class);
    }

    @Test
    void testVerifyClassPassesOnSafe() {
        // Should not throw
        ClassUtilities.SecurityChecker.verifyClass(String.class);
    }

    @Test
    void testIsSecurityBlockedNameExactMatch() {
        assertThat(ClassUtilities.SecurityChecker.isSecurityBlockedName("java.lang.Runtime")).isTrue();
        assertThat(ClassUtilities.SecurityChecker.isSecurityBlockedName("java.lang.ProcessBuilder")).isTrue();
        assertThat(ClassUtilities.SecurityChecker.isSecurityBlockedName("javax.script.ScriptEngineManager")).isTrue();
    }

    @Test
    void testIsSecurityBlockedNamePackageMatch() {
        assertThat(ClassUtilities.SecurityChecker.isSecurityBlockedName("javax.script.AnyClass")).isTrue();
        assertThat(ClassUtilities.SecurityChecker.isSecurityBlockedName("jdk.nashorn.internal.Something")).isTrue();
    }

    @Test
    void testIsSecurityBlockedNameSafeClass() {
        assertThat(ClassUtilities.SecurityChecker.isSecurityBlockedName("java.lang.String")).isFalse();
        assertThat(ClassUtilities.SecurityChecker.isSecurityBlockedName("java.util.HashMap")).isFalse();
    }

    // ========== Enhanced security depth limit ==========

    @Test
    void testMaxClassLoadDepthNotExceededForNormalLoad() {
        // Normal class load should succeed (depth=1, not exceeding any reasonable limit)
        Class<?> c = ClassUtilities.forName("java.lang.String", ClassUtilities.getClassLoader());
        assertThat(c).isEqualTo(String.class);
    }
}
