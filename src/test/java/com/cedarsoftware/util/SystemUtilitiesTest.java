package com.cedarsoftware.util;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.net.InetAddress;
import java.net.SocketException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author John DeRegnaucourt (jdereg@gmail.com)
 *         <br>
 *         Copyright (c) Cedar Software LLC
 *         <br><br>
 *         Licensed under the Apache License, Version 2.0 (the "License");
 *         you may not use this file except in compliance with the License.
 *         You may obtain a copy of the License at
 *         <br><br>
 *         <a href="http://www.apache.org/licenses/LICENSE-2.0">License</a>
 *         <br><br>
 *         Unless required by applicable law or agreed to in writing, software
 *         distributed under the License is distributed on an "AS IS" BASIS,
 *         WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *         See the License for the specific language governing permissions and
 *         limitations under the License.
 */
class SystemUtilitiesTest
{
    @TempDir
    Path tempDir;  // JUnit 5 will inject a temporary directory

    private String originalTZ;

    @BeforeEach
    void setup() {
        originalTZ = System.getenv("TZ");
    }

    @Test
    void testGetExternalVariable() {
        // Test with existing system property
        String originalValue = System.getProperty("java.home");
        assertNotNull(SystemUtilities.getExternalVariable("java.home"));
        assertEquals(originalValue, SystemUtilities.getExternalVariable("java.home"));

        // Test with non-existent variable
        assertNull(SystemUtilities.getExternalVariable("NON_EXISTENT_VARIABLE"));

        // Test with empty string
        assertNull(SystemUtilities.getExternalVariable(""));

        // Test with null
        assertNull(SystemUtilities.getExternalVariable(null));
    }

    @Test
    void testGetAvailableProcessors() {
        int processors = SystemUtilities.getAvailableProcessors();
        assertTrue(processors >= 1);
        assertTrue(processors <= Runtime.getRuntime().availableProcessors());
    }

    @Test
    void testGetMemoryInfo() {
        SystemUtilities.MemoryInfo info = SystemUtilities.getMemoryInfo();

        assertTrue(info.getTotalMemory() > 0);
        assertTrue(info.getFreeMemory() >= 0);
        assertTrue(info.getMaxMemory() > 0);
        assertTrue(info.getFreeMemory() <= info.getTotalMemory());
        assertTrue(info.getTotalMemory() <= info.getMaxMemory());
    }

    @Test
    void testGetSystemLoadAverage() {
        double loadAvg = SystemUtilities.getSystemLoadAverage();
        // Load average might be -1 on some platforms if not available
        assertTrue(loadAvg >= -1.0);
    }

    @Test
    void testIsJavaVersionAtLeast() {
        // Test current JVM version
        String version = System.getProperty("java.version");
        int currentMajor = Integer.parseInt(version.split("\\.")[0]);

        // Should be true for current version
        assertTrue(SystemUtilities.isJavaVersionAtLeast(currentMajor, 0));

        // Should be false for future version
        assertFalse(SystemUtilities.isJavaVersionAtLeast(currentMajor + 1, 0));
    }

    @Test
    void testGetCurrentProcessId() {
        long pid = SystemUtilities.getCurrentProcessId();
        assertTrue(pid > 0);
    }

    @Test
    public void testCreateTempDirectory() throws Exception {
        File tempDir = SystemUtilities.createTempDirectory("test-prefix");
        try {
            assertTrue(tempDir.exists());
            assertTrue(tempDir.isDirectory());
            assertTrue(tempDir.canRead());
            assertTrue(tempDir.canWrite());
        } finally {
            if (tempDir != null && tempDir.exists()) {
                tempDir.delete();
            }
        }
    }

    @Test
    void testGetSystemTimeZone() {
        TimeZone tz = SystemUtilities.getSystemTimeZone();
        assertNotNull(tz);
    }

    @Test
    void testHasAvailableMemory() {
        assertTrue(SystemUtilities.hasAvailableMemory(1));  // 1 byte should be available
        assertFalse(SystemUtilities.hasAvailableMemory(Long.MAX_VALUE));  // More than possible memory
    }

    @Test
    void testGetEnvironmentVariables() {
        // Test without filter (note: security filtering may reduce the count)
        Map<String, String> allVars = SystemUtilities.getEnvironmentVariables(null);
        assertFalse(allVars.isEmpty());
        // Security filtering may reduce the count, so we check that it's less than or equal to system env size
        assertTrue(allVars.size() <= System.getenv().size());
        
        // Test unsafe method returns all variables
        Map<String, String> unsafeVars = SystemUtilities.getEnvironmentVariablesUnsafe(null);
        assertEquals(System.getenv().size(), unsafeVars.size());

        // Test with filter
        Map<String, String> filteredVars = SystemUtilities.getEnvironmentVariables(
                key -> key.startsWith("JAVA_")
        );
        assertTrue(filteredVars.size() <= allVars.size());
        filteredVars.keySet().forEach(key -> assertTrue(key.startsWith("JAVA_")));
    }

    @Test
    void testGetNetworkInterfaces() throws SocketException {
        List<SystemUtilities.NetworkInfo> interfaces = SystemUtilities.getNetworkInterfaces();
        assertNotNull(interfaces);

        for (SystemUtilities.NetworkInfo info : interfaces) {
            assertNotNull(info.getName());
            assertNotNull(info.getDisplayName());
            assertNotNull(info.getAddresses());
            // Don't test isLoopback() value as it depends on network configuration
        }
    }

    @Test
    void testAddShutdownHook() {
        AtomicBoolean hookCalled = new AtomicBoolean(false);
        SystemUtilities.addShutdownHook(() -> hookCalled.set(true));
        // Note: Cannot actually test if hook is called as it would require JVM shutdown
    }

    @Test
    void testMemoryInfoClass() {
        SystemUtilities.MemoryInfo info = new SystemUtilities.MemoryInfo(1000L, 500L, 2000L);
        assertEquals(1000L, info.getTotalMemory());
        assertEquals(500L, info.getFreeMemory());
        assertEquals(2000L, info.getMaxMemory());
    }

    @Test
    void testNetworkInfoClass() {
        List<InetAddress> addresses = Arrays.asList(InetAddress.getLoopbackAddress());
        SystemUtilities.NetworkInfo info = new SystemUtilities.NetworkInfo(
                "test-interface",
                "Test Interface",
                addresses,
                true
        );

        assertEquals("test-interface", info.getName());
        assertEquals("Test Interface", info.getDisplayName());
        assertEquals(addresses, info.getAddresses());
        assertTrue(info.isLoopback());
    }
    @Test
    void testProcessResultClass() {
        SystemUtilities.ProcessResult result = new SystemUtilities.ProcessResult(0, "output", "error");
        assertEquals(0, result.getExitCode());
        assertEquals("output", result.getOutput());
        assertEquals("error", result.getError());
    }

    @Test
    void testConstructorIsPrivate() throws Exception {
        Constructor con = SystemUtilities.class.getDeclaredConstructor();
        assertEquals(Modifier.PRIVATE, con.getModifiers() & Modifier.PRIVATE);
        con.setAccessible(true);

        assertNotNull(con.newInstance());
    }

    @Test
    void testGetExternalVariable2()
    {
        String win = SystemUtilities.getExternalVariable("Path");
        String nix = SystemUtilities.getExternalVariable("PATH");
        assertTrue(nix != null || win != null);
        long x = UniqueIdGenerator.getUniqueId();
        assertTrue(x > 0);
    }
}
