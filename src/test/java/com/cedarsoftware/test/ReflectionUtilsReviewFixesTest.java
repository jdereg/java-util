package com.cedarsoftware.test;

import java.lang.reflect.Field;
import java.util.Currency;
import java.util.function.Predicate;

import com.cedarsoftware.util.ReflectionUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Pins the fixes from the 4.106.0 ReflectionUtils review. This test lives in
 * {@code com.cedarsoftware.test} (NOT {@code com.cedarsoftware.util}) on purpose: the
 * dangerous-class block is only enforced against callers OUTSIDE the java-util package
 * ({@code isTrustedCaller()}), so the assertion below is only meaningful from here.
 * <ul>
 *   <li>The dangerous-class security block still fires when the check runs inside the THREADED
 *       cache strategy's {@code computeIfAbsent} — whose mapping function is invoked from a lambda
 *       inside {@code ThreadedLRUCacheStrategy} / {@code AbstractConcurrentNullSafeMap}. Those
 *       intermediary {@code com.cedarsoftware.util.*} lambda frames must be recognized as cache
 *       infrastructure, not mis-read as a trusted java-util caller (which would bypass the block).
 *       Regression guard for the LOCKING→THREADED reflection-cache switch.</li>
 *   <li>{@code getClassNameFromByteCode} throws {@code IllegalStateException} (its documented
 *       contract) rather than {@code NegativeArraySizeException} on a malformed
 *       {@code constant_pool_count} of 0.</li>
 * </ul>
 *
 * @author John DeRegnaucourt (jdereg@gmail.com)
 */
class ReflectionUtilsReviewFixesTest {

    @AfterEach
    void clearSecurityProps() {
        System.clearProperty("reflectionutils.security.enabled");
        System.clearProperty("reflectionutils.dangerous.class.validation.enabled");
        System.clearProperty("reflectionutils.dangerous.class.patterns");
    }

    @Test
    void dangerousClassBlockSurvivesThreadedCacheComputeIfAbsent() {
        System.setProperty("reflectionutils.security.enabled", "true");
        System.setProperty("reflectionutils.dangerous.class.validation.enabled", "true");
        System.setProperty("reflectionutils.dangerous.class.patterns", "java.util.Currency");

        // A unique filter instance forces a cache miss, so the security check runs INSIDE the
        // THREADED cache's computeIfAbsent mapping function (the path whose intermediary lambda
        // frames must be treated as infrastructure, not as a trusted java-util caller).
        Predicate<Field> uniqueFilter = f -> true;
        assertThatThrownBy(() -> ReflectionUtils.getDeclaredFields(Currency.class, uniqueFilter))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("Access denied");
    }

    @Test
    void getClassNameFromByteCodeStillWorksOnValidClassFile() throws Exception {
        byte[] bytes = readClassBytes(ReflectionUtilsReviewFixesTest.class);
        assertEquals(ReflectionUtilsReviewFixesTest.class.getName(),
                ReflectionUtils.getClassNameFromByteCode(bytes));
    }

    @Test
    void getClassNameFromByteCodeThrowsIllegalStateOnZeroConstantPoolCount() {
        // Minimal header with constant_pool_count = 0 (invalid; the JVM spec requires >= 1):
        // magic (4) + minor (2) + major (2) + constant_pool_count (2 == 0x0000).
        byte[] malformed = {
                (byte) 0xCA, (byte) 0xFE, (byte) 0xBA, (byte) 0xBE, // magic
                0x00, 0x00,                                          // minor
                0x00, 0x34,                                          // major (52)
                0x00, 0x00                                           // constant_pool_count = 0 (bad)
        };
        assertThatThrownBy(() -> ReflectionUtils.getClassNameFromByteCode(malformed))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("constant pool count");
    }

    private static byte[] readClassBytes(Class<?> c) throws Exception {
        String resource = c.getName().replace('.', '/') + ".class";
        try (java.io.InputStream in = c.getClassLoader().getResourceAsStream(resource)) {
            assertThat(in).isNotNull();
            java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) != -1) {
                out.write(buf, 0, n);
            }
            return out.toByteArray();
        }
    }
}
