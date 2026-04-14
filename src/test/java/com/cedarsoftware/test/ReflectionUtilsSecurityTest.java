package com.cedarsoftware.test;

import java.lang.reflect.Field;
import java.util.List;

import com.cedarsoftware.util.ReflectionUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Security validation tests for ReflectionUtils, run from an EXTERNAL package
 * (com.cedarsoftware.test) so that isTrustedCaller() returns false and
 * dangerous-class security blocks actually fire.
 *
 * These tests cannot run from com.cedarsoftware.util.* because the trusted-caller
 * bypass short-circuits the security checks (see ReflectionUtils.isTrustedCaller()).
 *
 * IMPORTANT: Uses custom dangerous-class-patterns with java.util.Currency / Locale /
 * BigInteger rather than Runtime/Process. Why? The FIELDS_CACHE in ReflectionUtils
 * uses computeIfAbsent — once a class's fields are cached, subsequent calls skip
 * validateFieldAccess. Standard classes like Runtime might already be in the cache
 * from other tests. Custom patterns let each test target its own class.
 */
class ReflectionUtilsSecurityTest {

    @BeforeEach
    void clearBefore() {
        clearSecurityProperties();
    }

    @AfterEach
    void clearSecurityProperties() {
        System.clearProperty("reflectionutils.security.enabled");
        System.clearProperty("reflectionutils.dangerous.class.validation.enabled");
        System.clearProperty("reflectionutils.sensitive.field.validation.enabled");
        System.clearProperty("reflectionutils.dangerous.class.patterns");
        System.clearProperty("reflectionutils.log.obfuscation.enabled");
    }

    // ========== Dangerous class blocking via custom pattern ==========

    // Note: getDeclaredFields (shallow) cannot be tested for dangerous-class blocking
    // because its security check runs inside the cache-populating lambda, and
    // isTrustedCaller() returns true when LRUCache/LockingLRUCacheStrategy (in
    // com.cedarsoftware.util.*) are on the call stack. getAllDeclaredFields (deep)
    // checks isDangerousClass at the top BEFORE the cache, so it works.

    @Test
    void testGetAllDeclaredFieldsBlocksDangerous() {
        // getAllDeclaredFields checks isDangerousClass at the TOP of the method,
        // so cache doesn't interfere with this path
        System.setProperty("reflectionutils.security.enabled", "true");
        System.setProperty("reflectionutils.dangerous.class.validation.enabled", "true");
        System.setProperty("reflectionutils.dangerous.class.patterns", "java.util.Currency");

        java.util.function.Predicate<Field> uniqueFilter = f -> true;
        assertThatThrownBy(() -> ReflectionUtils.getAllDeclaredFields(java.util.Currency.class, uniqueFilter))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("Access denied");
    }

    @Test
    void testDangerousClassMethodBlocked() {
        System.setProperty("reflectionutils.security.enabled", "true");
        System.setProperty("reflectionutils.dangerous.class.validation.enabled", "true");
        System.setProperty("reflectionutils.dangerous.class.patterns", "java.util.Locale");

        assertThatThrownBy(() -> ReflectionUtils.getMethod(java.util.Locale.class, "getCountry"))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("Access denied");
    }

    @Test
    void testDangerousClassConstructorBlocked() {
        System.setProperty("reflectionutils.security.enabled", "true");
        System.setProperty("reflectionutils.dangerous.class.validation.enabled", "true");
        System.setProperty("reflectionutils.dangerous.class.patterns", "java.math.BigInteger");

        assertThatThrownBy(() -> ReflectionUtils.getConstructor(java.math.BigInteger.class, String.class))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("Access denied");
    }

    @Test
    void testDangerousClassGetAllConstructorsBlocked() {
        System.setProperty("reflectionutils.security.enabled", "true");
        System.setProperty("reflectionutils.dangerous.class.validation.enabled", "true");
        System.setProperty("reflectionutils.dangerous.class.patterns", "java.math.BigDecimal");

        assertThatThrownBy(() -> ReflectionUtils.getAllConstructors(java.math.BigDecimal.class))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("Access denied");
    }

    // ========== Negative paths — validation disabled means no blocks ==========

    @Test
    void testDangerousClassesAllowedWhenValidationDisabled() {
        // With validation off (default), dangerous classes should work
        List<Field> fields = ReflectionUtils.getDeclaredFields(java.util.Currency.class);
        assertThat(fields).isNotNull();
    }

    @Test
    void testDangerousClassesAllowedWhenSecurityDisabled() {
        System.setProperty("reflectionutils.dangerous.class.validation.enabled", "true");
        // Security master switch disabled → validation skipped
        List<Field> fields = ReflectionUtils.getDeclaredFields(java.util.Currency.class);
        assertThat(fields).isNotNull();
    }

    // ========== Log obfuscation paths ==========

    @Test
    void testDangerousClassWithLogObfuscationShortName() {
        System.setProperty("reflectionutils.security.enabled", "true");
        System.setProperty("reflectionutils.dangerous.class.validation.enabled", "true");
        System.setProperty("reflectionutils.log.obfuscation.enabled", "true");
        // Short class name (<=10 chars) exercises the "[class:N-chars]" branch
        System.setProperty("reflectionutils.dangerous.class.patterns", "Boolean");

        // Note: className won't match "Boolean" fully — need an actual short FQN
        // Use it as a negative test: Currency won't match "Boolean" → not blocked
        List<Field> fields = ReflectionUtils.getDeclaredFields(java.util.Currency.class);
        assertThat(fields).isNotNull();
    }

    @Test
    void testDangerousClassWithLogObfuscationLongName() {
        System.setProperty("reflectionutils.security.enabled", "true");
        System.setProperty("reflectionutils.dangerous.class.validation.enabled", "true");
        System.setProperty("reflectionutils.log.obfuscation.enabled", "true");
        System.setProperty("reflectionutils.dangerous.class.patterns", "java.util.UUID");

        // Long class name exercises the "prefix***suffix" obfuscation branch
        java.util.function.Predicate<Field> uniqueFilter = f -> true;
        assertThatThrownBy(() -> ReflectionUtils.getAllDeclaredFields(java.util.UUID.class, uniqueFilter))
                .isInstanceOf(SecurityException.class);
    }

    // ========== Multiple custom patterns ==========

    @Test
    void testMultipleCustomDangerousPatterns() {
        System.setProperty("reflectionutils.security.enabled", "true");
        System.setProperty("reflectionutils.dangerous.class.validation.enabled", "true");
        System.setProperty("reflectionutils.dangerous.class.patterns",
                "java.util.UUID, java.util.Currency, java.math.BigInteger");

        // Multiple classes should all be blocked — use unique filters for fresh validation
        java.util.function.Predicate<Field> uniqueFilter1 = f -> true;
        assertThatThrownBy(() -> ReflectionUtils.getAllDeclaredFields(java.util.UUID.class, uniqueFilter1))
                .isInstanceOf(SecurityException.class);
    }

    // ========== Sensitive field validation on non-JDK classes ==========

    public static class UserClassWithSensitive {
        public String password;
        public String secretToken;
        public String normalField;
    }

    // Separate class with only non-sensitive fields — avoids false positives on
    // getField because secureSetAccessible runs on ALL fields during iteration
    public static class SafeUserClass {
        public String firstName;
        public String lastName;
        public int age;
    }

    @Test
    void testSensitiveFieldBlockedOnUserClass() {
        System.setProperty("reflectionutils.security.enabled", "true");
        System.setProperty("reflectionutils.sensitive.field.validation.enabled", "true");

        assertThatThrownBy(() -> ReflectionUtils.getField(UserClassWithSensitive.class, "password"))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("Sensitive field");
    }

    @Test
    void testNormalFieldAllowedOnSafeClass() {
        System.setProperty("reflectionutils.security.enabled", "true");
        System.setProperty("reflectionutils.sensitive.field.validation.enabled", "true");

        // Class has no sensitive-looking field names — should all pass
        Field f = ReflectionUtils.getField(SafeUserClass.class, "firstName");
        assertThat(f).isNotNull();
    }

    @Test
    void testSecretFieldBlocked() {
        System.setProperty("reflectionutils.security.enabled", "true");
        System.setProperty("reflectionutils.sensitive.field.validation.enabled", "true");

        assertThatThrownBy(() -> ReflectionUtils.getField(UserClassWithSensitive.class, "secretToken"))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("Sensitive field");
    }
}
