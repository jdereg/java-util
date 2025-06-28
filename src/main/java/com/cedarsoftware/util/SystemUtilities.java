package com.cedarsoftware.util;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Method;
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
import java.util.logging.Level;
import java.util.logging.Logger;
import com.cedarsoftware.util.LoggingConfig;
import java.util.stream.Collectors;
import java.util.Set;
import java.util.HashSet;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

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
    private static final Logger LOG = Logger.getLogger(SystemUtilities.class.getName());
    static { LoggingConfig.init(); }
    
    // Security: Sensitive variable patterns that should not be exposed
    private static final Set<String> SENSITIVE_VARIABLE_PATTERNS = Collections.unmodifiableSet(
        new HashSet<>(Arrays.asList(
            "PASSWORD", "PASSWD", "PASS", "SECRET", "KEY", "TOKEN", "CREDENTIAL", 
            "AUTH", "APIKEY", "API_KEY", "PRIVATE", "CERT", "CERTIFICATE",
            "DATABASE_URL", "DB_URL", "CONNECTION_STRING", "DSN",
            "AWS_SECRET", "AZURE_CLIENT_SECRET", "GCP_SERVICE_ACCOUNT"
        ))
    );
    
    // Security: Resource limits for system operations
    private static final AtomicInteger SHUTDOWN_HOOK_COUNT = new AtomicInteger(0);
    private static final int MAX_SHUTDOWN_HOOKS = 100;

    private SystemUtilities() {
    }

    /**
     * Fetch value from environment variable and if not set, then fetch from
     * System properties. If neither available, return null.
     * 
     * <p><strong>Security Note:</strong> This method filters out potentially sensitive 
     * variables such as passwords, tokens, and credentials to prevent information disclosure.
     * Use {@link #getExternalVariableUnsafe(String)} if you need access to sensitive variables
     * and have verified the security requirements.</p>
     * 
     * @param var String key of variable to return
     * @return variable value or null if not found or filtered for security
     */
    public static String getExternalVariable(String var)
    {
        if (StringUtilities.isEmpty(var)) {
            return null;
        }
        
        // Security: Check if this is a sensitive variable that should be filtered
        if (isSensitiveVariable(var)) {
            LOG.log(Level.FINE, "Access to sensitive variable blocked: " + sanitizeVariableName(var));
            return null;
        }

        String value = System.getProperty(var);
        if (StringUtilities.isEmpty(value)) {
            value = System.getenv(var);
        }
        return StringUtilities.isEmpty(value) ? null : value;
    }
    
    /**
     * Fetch value from environment variable and if not set, then fetch from
     * System properties, without security filtering.
     * 
     * <p><strong>Security Warning:</strong> This method bypasses security filtering
     * and may return sensitive information such as passwords or tokens. Use with extreme
     * caution and ensure proper access controls are in place.</p>
     * 
     * @param var String key of variable to return
     * @return variable value or null if not found
     */
    public static String getExternalVariableUnsafe(String var)
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
     * Checks if a variable name matches patterns for sensitive information.
     * 
     * @param varName the variable name to check
     * @return true if the variable name suggests sensitive content
     */
    private static boolean isSensitiveVariable(String varName) {
        if (varName == null) {
            return false;
        }
        
        String upperVar = varName.toUpperCase();
        return SENSITIVE_VARIABLE_PATTERNS.stream().anyMatch(upperVar::contains);
    }
    
    /**
     * Sanitizes variable names for safe logging.
     * 
     * @param varName the variable name to sanitize
     * @return sanitized variable name safe for logging
     */
    private static String sanitizeVariableName(String varName) {
        if (varName == null) {
            return "[null]";
        }
        
        if (varName.length() <= 3) {
            return "[var:" + varName.length() + "-chars]";
        }
        
        return varName.substring(0, 2) + StringUtilities.repeat("*", varName.length() - 4) + varName.substring(varName.length() - 2);
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
        int[] version = parseJavaVersionNumbers();
        int majorVersion = version[0];
        int minorVersion = version[1];
        return majorVersion > major || (majorVersion == major && minorVersion >= minor);
    }

    /**
     * @return current JDK major version
     */
    public static int currentJdkMajorVersion() {
        try {
            // Security: Check SecurityManager permissions for reflection
            checkReflectionPermission();
            
            Method versionMethod = ReflectionUtils.getMethod(Runtime.class, "version");
            Object v = versionMethod.invoke(Runtime.getRuntime());
            Method major = ReflectionUtils.getMethod(v.getClass(), "major");
            return (Integer) major.invoke(v);
        } catch (Exception ignored) {
            String spec = System.getProperty("java.specification.version");
            return spec.startsWith("1.") ? Integer.parseInt(spec.substring(2)) : Integer.parseInt(spec);
        }
    }

    private static int[] parseJavaVersionNumbers() {
        try {
            // Security: Check SecurityManager permissions for reflection
            checkReflectionPermission();
            
            Method versionMethod = ReflectionUtils.getMethod(Runtime.class, "version");
            Object v = versionMethod.invoke(Runtime.getRuntime());
            Method majorMethod = ReflectionUtils.getMethod(v.getClass(), "major");
            Method minorMethod = ReflectionUtils.getMethod(v.getClass(), "minor");
            int major = (Integer) majorMethod.invoke(v);
            int minor = (Integer) minorMethod.invoke(v);
            return new int[]{major, minor};
        } catch (Exception ignored) {
            String[] parts = JAVA_VERSION.split("\\.");
            int major = Integer.parseInt(parts[0]);
            int minor = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;
            return new int[]{major, minor};
        }
    }
    
    /**
     * Checks security manager permissions for reflection operations.
     * 
     * @throws SecurityException if reflection is not permitted
     */
    private static void checkReflectionPermission() {
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            sm.checkPermission(new RuntimePermission("accessDeclaredMembers"));
        }
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
     * Create temporary directory that will be deleted on JVM exit.
     * 
     * <p><strong>Security Note:</strong> The prefix parameter is validated to prevent 
     * path traversal attacks and ensure safe directory creation.</p>
     *
     * @param prefix the prefix for the temporary directory name
     * @return the created temporary directory
     * @throws IllegalArgumentException if the prefix contains invalid characters
     * @throws IOException if the directory cannot be created (thrown as unchecked)
     */
    public static File createTempDirectory(String prefix) {
        // Security: Validate prefix to prevent path traversal and injection
        validateTempDirectoryPrefix(prefix);
        
        try {
            File tempDir = Files.createTempDirectory(prefix).toFile();
            tempDir.deleteOnExit();
            return tempDir.getCanonicalFile();
        } catch (IOException e) {
            ExceptionUtilities.uncheckedThrow(e);
            return null; // unreachable
        }
    }
    
    /**
     * Validates the prefix for temporary directory creation.
     * 
     * @param prefix the prefix to validate
     * @throws IllegalArgumentException if the prefix is invalid
     */
    private static void validateTempDirectoryPrefix(String prefix) {
        if (prefix == null) {
            throw new IllegalArgumentException("Temporary directory prefix cannot be null");
        }
        
        if (prefix.isEmpty()) {
            throw new IllegalArgumentException("Temporary directory prefix cannot be empty");
        }
        
        // Check for path traversal attempts
        if (prefix.contains("..") || prefix.contains("/") || prefix.contains("\\")) {
            throw new IllegalArgumentException("Temporary directory prefix contains invalid path characters: " + prefix);
        }
        
        // Check for null bytes and control characters
        if (prefix.contains("\0")) {
            throw new IllegalArgumentException("Temporary directory prefix contains null byte");
        }
        
        // Check for other dangerous characters
        if (prefix.matches(".*[<>:\"|?*].*")) {
            throw new IllegalArgumentException("Temporary directory prefix contains invalid characters: " + prefix);
        }
        
        // Limit length to prevent excessive resource usage
        if (prefix.length() > 100) {
            throw new IllegalArgumentException("Temporary directory prefix too long (max 100 characters): " + prefix.length());
        }
    }

    /**
     * Get system timezone, considering various sources
     */
    public static TimeZone getSystemTimeZone() {
        String tzEnv = System.getenv("TZ");
        if (tzEnv != null && !tzEnv.isEmpty()) {
            return TimeZone.getTimeZone(tzEnv);
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
     * Get all environment variables with optional filtering and security protection.
     * 
     * <p><strong>Security Note:</strong> This method automatically filters out sensitive
     * variables such as passwords, tokens, and credentials to prevent information disclosure.
     * Use {@link #getEnvironmentVariablesUnsafe(Predicate)} if you need access to sensitive
     * variables and have verified the security requirements.</p>
     * 
     * @param filter optional predicate to further filter variables (applied after security filtering)
     * @return map of non-sensitive environment variables
     */
    public static Map<String, String> getEnvironmentVariables(Predicate<String> filter) {
        return System.getenv().entrySet().stream()
                .filter(e -> !isSensitiveVariable(e.getKey())) // Security: Filter sensitive variables
                .filter(e -> filter == null || filter.test(e.getKey()))
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (v1, v2) -> v1,
                        LinkedHashMap::new
                ));
    }
    
    /**
     * Get all environment variables with optional filtering, without security protection.
     * 
     * <p><strong>Security Warning:</strong> This method bypasses security filtering
     * and may return sensitive information such as passwords or tokens. Use with extreme
     * caution and ensure proper access controls are in place.</p>
     * 
     * @param filter optional predicate to filter variables
     * @return map of all environment variables matching the filter
     */
    public static Map<String, String> getEnvironmentVariablesUnsafe(Predicate<String> filter) {
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
    public static List<NetworkInfo> getNetworkInterfaces() {
        List<NetworkInfo> interfaces = new ArrayList<>();
        Enumeration<NetworkInterface> en = null;
        try {
            en = NetworkInterface.getNetworkInterfaces();
        } catch (SocketException e) {
            ExceptionUtilities.uncheckedThrow(e);
        }

        while (en.hasMoreElements()) {
            NetworkInterface ni = en.nextElement();
            try {
                if (ni.isUp()) {
                    List<InetAddress> addresses = Collections.list(ni.getInetAddresses());
                    interfaces.add(new NetworkInfo(
                            ni.getName(),
                            ni.getDisplayName(),
                            addresses,
                            ni.isLoopback()
                    ));
                }
            } catch (SocketException e) {
                LOG.log(Level.WARNING, "Failed to inspect network interface " + ni.getName(), e);
            }
        }
        return interfaces;
    }

    /**
     * Add shutdown hook with safe execution and resource limits.
     * 
     * <p><strong>Security Note:</strong> This method enforces a limit on the number of 
     * shutdown hooks to prevent resource exhaustion attacks. The current limit is 
     * {@value #MAX_SHUTDOWN_HOOKS} hooks.</p>
     * 
     * @param hook the runnable to execute during shutdown
     * @throws IllegalStateException if the maximum number of shutdown hooks is exceeded
     * @throws IllegalArgumentException if hook is null
     */
    public static void addShutdownHook(Runnable hook) {
        if (hook == null) {
            throw new IllegalArgumentException("Shutdown hook cannot be null");
        }
        
        // Security: Enforce limit on shutdown hooks to prevent resource exhaustion
        int currentCount = SHUTDOWN_HOOK_COUNT.incrementAndGet();
        if (currentCount > MAX_SHUTDOWN_HOOKS) {
            SHUTDOWN_HOOK_COUNT.decrementAndGet();
            throw new IllegalStateException("Maximum number of shutdown hooks exceeded: " + MAX_SHUTDOWN_HOOKS);
        }
        
        try {
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    hook.run();
                } catch (Exception e) {
                    LOG.log(Level.SEVERE, "Shutdown hook threw exception", e);
                } finally {
                    SHUTDOWN_HOOK_COUNT.decrementAndGet();
                }
            }));
        } catch (Exception e) {
            // If adding the hook fails, decrement the counter
            SHUTDOWN_HOOK_COUNT.decrementAndGet();
            throw e;
        }
    }
    
    /**
     * Get the current number of registered shutdown hooks.
     * 
     * @return the number of shutdown hooks currently registered
     */
    public static int getShutdownHookCount() {
        return SHUTDOWN_HOOK_COUNT.get();
    }

    // Support classes

    /**
     * Simple container class describing the JVM memory usage at a given point
     * in time.
     */
    public static class MemoryInfo {
        private final long totalMemory;
        private final long freeMemory;
        private final long maxMemory;

        /**
         * Create an instance holding the supplied memory metrics.
         *
         * @param totalMemory total memory currently allocated to the JVM
         * @param freeMemory  amount of memory that is unused
         * @param maxMemory   maximum memory the JVM will attempt to use
         */
        public MemoryInfo(long totalMemory, long freeMemory, long maxMemory) {
            this.totalMemory = totalMemory;
            this.freeMemory = freeMemory;
            this.maxMemory = maxMemory;
        }

        /**
         * @return the total memory currently allocated to the JVM
         */
        public long getTotalMemory() {
            return totalMemory;
        }

        /**
         * @return the amount of unused memory
         */
        public long getFreeMemory() {
            return freeMemory;
        }

        /**
         * @return the maximum memory the JVM can utilize
         */
        public long getMaxMemory() {
            return maxMemory;
        }
    }

    /**
     * Describes a network interface present on the host system.
     */
    public static class NetworkInfo {
        private final String name;
        private final String displayName;
        private final List<InetAddress> addresses;
        private final boolean loopback;

        /**
         * Construct a new {@code NetworkInfo} instance.
         *
         * @param name        the interface name
         * @param displayName the human readable display name
         * @param addresses   all addresses bound to the interface
         * @param loopback    whether this interface represents the loopback device
         */
        public NetworkInfo(String name, String displayName, List<InetAddress> addresses, boolean loopback) {
            this.name = name;
            this.displayName = displayName;
            List<InetAddress> safe = addresses == null ? Collections.emptyList() : new ArrayList<>(addresses);
            this.addresses = Collections.unmodifiableList(safe);
            this.loopback = loopback;
        }

        /**
         * @return the interface name
         */
        public String getName() {
            return name;
        }

        /**
         * @return the user friendly display name
         */
        public String getDisplayName() {
            return displayName;
        }

        /**
         * @return all addresses associated with the interface
         */
        public List<InetAddress> getAddresses() {
            return addresses;
        }

        /**
         * @return {@code true} if this interface is a loopback interface
         */
        public boolean isLoopback() {
            return loopback;
        }
    }

    /**
     * Captures the results of executing an operating system process.
     */
    public static class ProcessResult {
        private final int exitCode;
        private final String output;
        private final String error;

        /**
         * Create a new result.
         *
         * @param exitCode the exit value returned by the process
         * @param output   text captured from standard out
         * @param error    text captured from standard error
         */
        public ProcessResult(int exitCode, String output, String error) {
            this.exitCode = exitCode;
            this.output = output;
            this.error = error;
        }

        /**
         * @return the exit value of the process
         */
        public int getExitCode() {
            return exitCode;
        }

        /**
         * @return the contents of the standard output stream
         */
        public String getOutput() {
            return output;
        }

        /**
         * @return the contents of the standard error stream
         */
        public String getError() {
            return error;
        }
    }
}
