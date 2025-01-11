package com.cedarsoftware.util;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Utility class providing common system-level operations and information gathering capabilities.
 * This class offers static methods for accessing and managing system resources, environment
 * settings, and runtime information.
 *
 * <h2>Key Features:</h2>
 * <ul>
 *     <li>System environment and property access</li>
 *     <li>Memory usage monitoring and management</li>
 *     <li>Network interface information retrieval</li>
 *     <li>Process management and identification</li>
 *     <li>Runtime environment analysis</li>
 *     <li>Temporary file management</li>
 * </ul>
 *
 * <h2>Usage Examples:</h2>
 * <pre>{@code
 * // Get system environment variable with fallback to system property
 * String configPath = SystemUtilities.getExternalVariable("CONFIG_PATH");
 *
 * // Check available system resources
 * int processors = SystemUtilities.getAvailableProcessors();
 * MemoryInfo memory = SystemUtilities.getMemoryInfo();
 *
 * // Get network configuration
 * List<NetworkInfo> networks = SystemUtilities.getNetworkInterfaces();
 * }</pre>
 *
 * <p>All methods in this class are thread-safe unless otherwise noted. The class cannot be
 * instantiated and provides only static utility methods.</p>
 *
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
 *
 * @see Runtime
 * @see System
 * @see ManagementFactory
 */
public final class SystemUtilities
{
    public static final String OS_NAME = System.getProperty("os.name").toLowerCase();
    public static final String JAVA_VERSION = System.getProperty("java.version");
    public static final String USER_HOME = System.getProperty("user.home");
    public static final String TEMP_DIR = System.getProperty("java.io.tmpdir");
    
    private SystemUtilities() {
    }

    /**
     * Fetch value from environment variable and if not set, then fetch from
     * System properties.  If neither available, return null.
     * @param var String key of variable to return
     */
    public static String getExternalVariable(String var)
    {
        if (StringUtilities.isEmpty(var)) {
            return null;
        }

        String value = System.getProperty(var);
        if (StringUtilities.isEmpty(value)) {
            value = System.getenv(var);
        }
        return StringUtilities.isEmpty(value) ? null : value;
    }

    
    /**
     * Get available processors, considering Docker container limits
     */
    public static int getAvailableProcessors() {
        return Math.max(1, Runtime.getRuntime().availableProcessors());
    }

    /**
     * Get current JVM memory usage information
     */
    public static MemoryInfo getMemoryInfo() {
        Runtime runtime = Runtime.getRuntime();
        return new MemoryInfo(
                runtime.totalMemory(),
                runtime.freeMemory(),
                runtime.maxMemory()
        );
    }

    /**
     * Get system load average over last minute
     * @return load average or -1.0 if not available
     */
    public static double getSystemLoadAverage() {
        return ManagementFactory.getOperatingSystemMXBean().getSystemLoadAverage();
    }

    /**
     * Check if running on specific Java version or higher
     */
    public static boolean isJavaVersionAtLeast(int major, int minor) {
        String[] version = JAVA_VERSION.split("\\.");
        int majorVersion = Integer.parseInt(version[0]);
        int minorVersion = version.length > 1 ? Integer.parseInt(version[1]) : 0;
        return majorVersion > major || (majorVersion == major && minorVersion >= minor);
    }

    /**
     * Get process ID of current JVM
     * @return process ID for the current Java process
     */
    public static long getCurrentProcessId() {
        String jvmName = ManagementFactory.getRuntimeMXBean().getName();
        int index = jvmName.indexOf('@');
        if (index < 1) {
            return 0;
        }
        try {
            return Long.parseLong(jvmName.substring(0, index));
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    /**
     * Create temporary directory that will be deleted on JVM exit
     */
    public static File createTempDirectory(String prefix) throws IOException {
        File tempDir = Files.createTempDirectory(prefix).toFile();
        tempDir.deleteOnExit();
        return tempDir;
    }

    /**
     * Get system timezone, considering various sources
     */
    public static TimeZone getSystemTimeZone() {
        String tzEnv = System.getenv("TZ");
        if (tzEnv != null) {
            try {
                return TimeZone.getTimeZone(tzEnv);
            } catch (Exception ignored) { }
        }
        return TimeZone.getDefault();
    }

    /**
     * Check if enough memory is available
     */
    public static boolean hasAvailableMemory(long requiredBytes) {
        MemoryInfo info = getMemoryInfo();
        return info.getFreeMemory() >= requiredBytes;
    }

    /**
     * Get all environment variables with optional filtering
     */
    public static Map<String, String> getEnvironmentVariables(Predicate<String> filter) {
        return System.getenv().entrySet().stream()
                .filter(e -> filter == null || filter.test(e.getKey()))
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (v1, v2) -> v1,
                        LinkedHashMap::new
                ));
    }

    /**
     * Get network interface information
     */
    public static List<NetworkInfo> getNetworkInterfaces() throws SocketException {
        List<NetworkInfo> interfaces = new ArrayList<>();
        for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
            NetworkInterface ni = en.nextElement();
            if (ni.isUp()) {
                List<InetAddress> addresses = Collections.list(ni.getInetAddresses());
                interfaces.add(new NetworkInfo(
                        ni.getName(),
                        ni.getDisplayName(),
                        addresses,
                        ni.isLoopback()
                ));
            }
        }
        return interfaces;
    }
    
    /**
     * Add shutdown hook with safe execution
     */
    public static void addShutdownHook(Runnable hook) {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                hook.run();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }));
    }

    // Support classes
    public static class MemoryInfo {
        private final long totalMemory;
        private final long freeMemory;
        private final long maxMemory;

        public MemoryInfo(long totalMemory, long freeMemory, long maxMemory) {
            this.totalMemory = totalMemory;
            this.freeMemory = freeMemory;
            this.maxMemory = maxMemory;
        }

        public long getTotalMemory() {
            return totalMemory;
        }

        public long getFreeMemory() {
            return freeMemory;
        }

        public long getMaxMemory() {
            return maxMemory;
        }
    }

    public static class NetworkInfo {
        private final String name;
        private final String displayName;
        private final List<InetAddress> addresses;
        private final boolean loopback;

        public NetworkInfo(String name, String displayName, List<InetAddress> addresses, boolean loopback) {
            this.name = name;
            this.displayName = displayName;
            this.addresses = addresses;
            this.loopback = loopback;
        }
        
        public String getName() {
            return name;
        }

        public String getDisplayName() {
            return displayName;
        }

        public List<InetAddress> getAddresses() {
            return addresses;
        }

        public boolean isLoopback() {
            return loopback;
        }
    }

    public static class ProcessResult {
        private final int exitCode;
        private final String output;
        private final String error;

        public ProcessResult(int exitCode, String output, String error) {
            this.exitCode = exitCode;
            this.output = output;
            this.error = error;
        }

        public int getExitCode() {
            return exitCode;
        }

        public String getOutput() {
            return output;
        }

        public String getError() {
            return error;
        }
    }
}