package com.cedarsoftware.test;

import com.cedarsoftware.util.ReflectionUtils;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Pins a fix from the 4.106.0 ReflectionUtils review: {@code getClassNameFromByteCode} throws
 * {@code IllegalStateException} (its documented contract) rather than {@code NegativeArraySizeException} on a
 * malformed {@code constant_pool_count} of 0. (The review's other fix was to the dangerous-class switch, which
 * 4.119.0 removed along with its test here; see {@code ReflectionUtilsSwitchesRemovedTest}.)
 *
 * @author John DeRegnaucourt (jdereg@gmail.com)
 */
class ReflectionUtilsReviewFixesTest {

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
