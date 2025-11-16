package com.cedarsoftware.util;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.DataInputStream;
import java.io.File;
import java.io.Flushable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLConnection;
import java.net.HttpURLConnection;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Objects;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utility class providing robust I/O operations with built-in error handling and resource management.
 * <p>
 * This class simplifies common I/O tasks such as:
 * </p>
 * <ul>
 *   <li>Stream transfers and copying</li>
 *   <li>Resource closing and flushing</li>
 *   <li>Byte array compression/decompression</li>
 *   <li>URL connection handling</li>
 *   <li>File operations</li>
 * </ul>
 *
 * <p><strong>Key Features:</strong></p>
 * <ul>
 *   <li>Automatic buffer management for optimal performance</li>
 *   <li>GZIP and Deflate compression support</li>
 *   <li>Unchecked exception handling for close/flush operations (fail-fast, not silent)</li>
 *   <li>Progress tracking through callback mechanism</li>
 *   <li>Support for XML stream operations with unchecked exception handling</li>
 *   <li>
 *     <b>XML stream support:</b> Methods {@link #close(XMLStreamReader)}, {@link #close(XMLStreamWriter)},
 *     and {@link #flush(XMLStreamWriter)} work with {@code javax.xml.stream} classes.
 *     <b>These methods require the {@code java.xml} module to be present at runtime.</b>
 *     If you're using JPMS, add {@code requires java.xml;} to your module-info.java if using these methods.
 *     For OSGi, ensure your bundle imports the {@code javax.xml.stream} package or declare it as an optional import
 *     if XML support is not required. The rest of the library does <b>not</b> require {@code java.xml}.
 *   </li>
 * </ul>
 * <p>
 * <b>Exception Handling Philosophy:</b> All close() and flush() methods in this class throw exceptions as
 * <b>unchecked</b> via {@link ExceptionUtilities#uncheckedThrow(Throwable)}. This design choice provides:
 * <ul>
 *   <li>Cleaner code - no try-catch required in finally blocks or cleanup code</li>
 *   <li>Better diagnostics - close/flush failures are visible rather than silently swallowed</li>
 *   <li>Early problem detection - infrastructure issues surface immediately rather than hiding until later failures</li>
 *   <li>Flexibility - callers can catch these as regular exceptions higher in the call stack if desired</li>
 * </ul>
 * While close/flush exceptions are rare, they often indicate serious issues (disk full, network failures,
 * resource exhaustion) that should be diagnosed rather than hidden.
 * </p>
 *
 * <p><strong>Usage Example:</strong></p>
 * <pre>{@code
 * // Copy file to output stream
 * try (InputStream fis = Files.newInputStream(Paths.get("input.txt"))) {
 *     try (OutputStream fos = Files.newOutputStream(Paths.get("output.txt"))) {
 *         IOUtilities.transfer(fis, fos);
 *     }
 * }
 *
 * // Compress byte array
 * byte[] compressed = IOUtilities.compressBytes(originalBytes);
 * byte[] uncompressed = IOUtilities.uncompressBytes(compressed);
 * }</pre>
 *
 * <p><strong>Security and Performance Configuration:</strong></p>
 * <p>IOUtilities provides configurable security and performance options through system properties.
 * Most security features have <strong>safe defaults</strong> but can be customized as needed:</p>
 * <ul>
 *   <li><code>io.debug=false</code> &mdash; Enable debug logging</li>
 *   <li><code>io.connect.timeout=5000</code> &mdash; Connection timeout (1s-5min)</li>
 *   <li><code>io.read.timeout=30000</code> &mdash; Read timeout (1s-5min)</li>
 *   <li><code>io.max.stream.size=2147483647</code> &mdash; Stream size limit (2GB)</li>
 *   <li><code>io.max.decompression.size=2147483647</code> &mdash; Decompression size limit (2GB)</li>
 *   <li><code>io.path.validation.disabled=false</code> &mdash; Path security validation enabled</li>
 *   <li><code>io.url.protocol.validation.disabled=false</code> &mdash; URL protocol validation enabled</li>
 *   <li><code>io.allowed.protocols=http,https,file,jar</code> &mdash; Allowed URL protocols</li>
 *   <li><code>io.file.protocol.validation.disabled=false</code> &mdash; File protocol validation enabled</li>
 *   <li><code>io.debug.detailed.urls=false</code> &mdash; Detailed URL logging disabled</li>
 *   <li><code>io.debug.detailed.paths=false</code> &mdash; Detailed path logging disabled</li>
 * </ul>
 *
 * @author Ken Partlow
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
public final class IOUtilities {
    private static final int TRANSFER_BUFFER = 32768;
    private static final int DEFAULT_CONNECT_TIMEOUT = 5000;
    private static final int DEFAULT_READ_TIMEOUT = 30000;
    private static final int MIN_TIMEOUT = 1000;  // Minimum 1 second to prevent DoS
    private static final int MAX_TIMEOUT = 300000; // Maximum 5 minutes to prevent resource exhaustion
    private static final boolean DEBUG = Boolean.parseBoolean(System.getProperty("io.debug", "false"));
    private static final Logger LOG = Logger.getLogger(IOUtilities.class.getName());
    static { LoggingConfig.init(); }

    private static void debug(String msg, Exception e) {
        if (DEBUG) {
            if (e == null) {
                LOG.fine(msg);
            } else {
                LOG.log(Level.FINE, msg, e);
            }
        }
    }

    private IOUtilities() { }

    /**
     * Safely retrieves and validates timeout values from system properties.
     * Prevents system property injection attacks by enforcing strict bounds and validation.
     * 
     * @param propertyName the system property name to read
     * @param defaultValue the default value to use if property is invalid or missing
     * @param propertyType description of the property for logging (e.g., "connect timeout")
     * @return validated timeout value within safe bounds
     */
    private static int getValidatedTimeout(String propertyName, int defaultValue, String propertyType) {
        try {
            String propertyValue = System.getProperty(propertyName);
            if (propertyValue == null || propertyValue.trim().isEmpty()) {
                return defaultValue;
            }
            
            // Additional validation to prevent injection attacks
            if (!propertyValue.matches("^-?\\d+$")) {
                debug("Invalid " + propertyType + " format, using default", null);
                return defaultValue;
            }
            
            int timeout = Integer.parseInt(propertyValue.trim());
            
            // Enforce reasonable bounds to prevent DoS attacks
            if (timeout < MIN_TIMEOUT) {
                debug("Configured " + propertyType + " too low, using minimum value", null);
                return MIN_TIMEOUT;
            }
            
            if (timeout > MAX_TIMEOUT) {
                debug("Configured " + propertyType + " too high, using maximum value", null);
                return MAX_TIMEOUT;
            }
            
            return timeout;
            
        } catch (NumberFormatException e) {
            debug("Invalid " + propertyType + " configuration detected, using defaults", null);
            return defaultValue;
        } catch (SecurityException e) {
            debug("Security restriction accessing " + propertyType + " property, using defaults", null);
            return defaultValue;
        }
    }

    /**
     * Safely retrieves and validates size limit values from system properties.
     * Prevents system property injection attacks by enforcing strict bounds and validation.
     * 
     * @param propertyName the system property name to read
     * @param defaultValue the default value to use if property is invalid or missing
     * @param propertyType description of the property for logging (e.g., "max stream size")
     * @return validated size value within safe bounds
     */
    private static int getValidatedSizeProperty(String propertyName, int defaultValue, String propertyType) {
        try {
            String propertyValue = System.getProperty(propertyName);
            if (propertyValue == null || propertyValue.trim().isEmpty()) {
                return defaultValue;
            }
            
            // Additional validation to prevent injection attacks
            if (!propertyValue.matches("^-?\\d+$")) {
                debug("Invalid " + propertyType + " format, using default", null);
                return defaultValue;
            }
            
            long size = Long.parseLong(propertyValue.trim());
            
            // Enforce reasonable bounds to prevent resource exhaustion
            if (size <= 0) {
                debug("Configured " + propertyType + " must be positive, using default", null);
                return defaultValue;
            }
            
            // Prevent overflow and extremely large values
            if (size > Integer.MAX_VALUE) {
                debug("Configured " + propertyType + " too large, using maximum safe value", null);
                return Integer.MAX_VALUE;
            }
            
            return (int) size;
            
        } catch (NumberFormatException e) {
            debug("Invalid " + propertyType + " configuration detected, using defaults", null);
            return defaultValue;
        } catch (SecurityException e) {
            debug("Security restriction accessing " + propertyType + " property, using defaults", null);
            return defaultValue;
        }
    }

    /**
     * Gets the default maximum stream size for security purposes.
     * Can be configured via system property 'io.max.stream.size'.
     * Defaults to 2GB if not configured. Uses secure validation to prevent injection.
     *
     * @return the maximum allowed stream size in bytes
     */
    private static int getDefaultMaxStreamSize() {
        return getValidatedSizeProperty("io.max.stream.size", 2147483647, "max stream size");
    }

    /**
     * Gets the default maximum decompression size for security purposes.
     * Can be configured via system property 'io.max.decompression.size'.
     * Defaults to 2GB if not configured. Uses secure validation to prevent injection.
     *
     * @return the maximum allowed decompressed data size in bytes
     */
    private static int getDefaultMaxDecompressionSize() {
        return getValidatedSizeProperty("io.max.decompression.size", 2147483647, "max decompression size");
    }


    /**
     * Validates that a file path is secure and does not contain path traversal attempts or other security violations.
     * Can be disabled via system property 'io.path.validation.disabled=true'.
     * 
     * @param file the file to validate
     * @throws IllegalArgumentException if file is null
     * @throws SecurityException if path contains traversal attempts or other security violations
     */
    private static void validateFilePath(File file) {
        Convention.throwIfNull(file, "File cannot be null");
        
        // Allow disabling path validation via system property for compatibility
        if (Boolean.parseBoolean(System.getProperty("io.path.validation.disabled", "false"))) {
            return;
        }
        
        String filePath = file.getPath();
        
        // Fast checks first - no filesystem operations needed
        // Check for obvious path traversal attempts
        if (filePath.contains("../") || filePath.contains("..\\") || 
            filePath.contains("/..") || filePath.contains("\\..")) {
            throw new SecurityException("Path traversal attempt detected: " + sanitizePathForLogging(filePath));
        }
        
        // Check for null bytes which can be used to bypass filters
        if (filePath.indexOf('\0') != -1) {
            throw new SecurityException("Null byte in file path: " + sanitizePathForLogging(filePath));
        }
        
        // Check for suspicious characters that might indicate injection attempts
        if (filePath.contains("|") || filePath.contains(";") || filePath.contains("&") || 
            filePath.contains("`") || filePath.contains("$")) {
            throw new SecurityException("Suspicious characters detected in file path: " + sanitizePathForLogging(filePath));
        }
        
        // Perform comprehensive security validation including symlink detection
        validateFileSystemSecurity(file, filePath);
    }
    
    /**
     * Performs comprehensive file system security validation including symlink detection,
     * special file checks, and canonical path verification.
     * 
     * @param file the file to validate
     * @param filePath the file path string for logging
     * @throws SecurityException if security violations are detected
     */
    private static void validateFileSystemSecurity(File file, String filePath) {
        try {
            // Get canonical path to resolve all symbolic links and relative references
            String canonicalPath = file.getCanonicalPath();
            String absolutePath = file.getAbsolutePath();
            
            // Detect symbolic link attacks by comparing canonical and absolute paths
            if (!canonicalPath.equals(absolutePath)) {
                // On Windows, case differences might be normal, so normalize case for comparison
                if (System.getProperty("os.name", "").toLowerCase().contains("windows")) {
                    if (!canonicalPath.equalsIgnoreCase(absolutePath)) {
                        debug("Potential symlink or case manipulation detected in file access", null);
                    }
                } else {
                    debug("Potential symlink detected in file access", null);
                }
            }
            
            // Check for attempts to access system directories (Unix/Linux specific)
            String lowerCanonical = canonicalPath.toLowerCase();
            if (lowerCanonical.startsWith("/proc/") || lowerCanonical.startsWith("/sys/") || 
                lowerCanonical.startsWith("/dev/") || lowerCanonical.equals("/etc/passwd") ||
                lowerCanonical.equals("/etc/shadow") || lowerCanonical.startsWith("/etc/ssh/")) {
                throw new SecurityException("Access to system directory/file denied: " + sanitizePathForLogging(canonicalPath));
            }
            
            // Check for Windows system file access attempts
            if (System.getProperty("os.name", "").toLowerCase().contains("windows")) {
                String lowerPath = canonicalPath.toLowerCase();
                if (lowerPath.contains("\\windows\\system32\\") || lowerPath.contains("\\windows\\syswow64\\") ||
                    lowerPath.endsWith("\\sam") || lowerPath.endsWith("\\system") || lowerPath.endsWith("\\security")) {
                    throw new SecurityException("Access to Windows system directory/file denied: " + sanitizePathForLogging(canonicalPath));
                }
            }
            
            // Validate against overly long paths that might cause buffer overflows
            if (canonicalPath.length() > 4096) {
                throw new SecurityException("File path too long (potential buffer overflow): " + sanitizePathForLogging(canonicalPath));
            }
            
            // Check for path elements that indicate potential security issues
            validatePathElements(canonicalPath);
            
        } catch (IOException e) {
            throw new SecurityException("Unable to validate file path security: " + sanitizePathForLogging(filePath), e);
        }
    }
    
    /**
     * Validates individual path elements for security issues.
     * 
     * @param canonicalPath the canonical file path to validate
     * @throws SecurityException if security violations are detected
     */
    private static void validatePathElements(String canonicalPath) {
        String[] pathElements = canonicalPath.split("[/\\\\]");
        
        for (String element : pathElements) {
            if (element.isEmpty()) continue;
            
            // Check for hidden system files or directories that shouldn't be accessed
            if (element.startsWith(".") && (element.equals(".ssh") || element.equals(".gnupg") || 
                element.equals(".aws") || element.equals(".docker"))) {
                throw new SecurityException("Access to sensitive hidden directory denied: " + sanitizePathForLogging(element));
            }
            
            // Check for backup or temporary files that might contain sensitive data
            if (element.endsWith(".bak") || element.endsWith(".tmp") || element.endsWith(".old") ||
                element.endsWith("~") || element.startsWith("core.")) {
                debug("Accessing potentially sensitive file type detected", null);
            }
            
            // Check for path elements with unusual characters
            if (element.contains("\t") || element.contains("\n") || element.contains("\r")) {
                throw new SecurityException("Invalid characters in path element: " + sanitizePathForLogging(element));
            }
        }
    }

    /**
     * Validates that the URLConnection's protocol is safe and prevents SSRF attacks.
     * Only allows HTTP and HTTPS protocols by default, with configurable overrides.
     * 
     * @param connection the URLConnection to validate
     * @throws SecurityException if the protocol is not allowed
     */
    private static void validateUrlProtocol(URLConnection connection) {
        if (connection == null || connection.getURL() == null) {
            return; // Already handled by null checks
        }
        
        String protocol = connection.getURL().getProtocol();
        if (protocol == null) {
            throw new SecurityException("URL protocol cannot be null");
        }
        
        protocol = protocol.toLowerCase();
        
        // Check if protocol validation is disabled (for testing or specific use cases)
        if (Boolean.parseBoolean(System.getProperty("io.url.protocol.validation.disabled", "false"))) {
            debug("URL protocol validation disabled via system property", null);
            return;
        }
        
        // Get allowed protocols from system property or use secure defaults
        // Note: file and jar are included for legitimate resource access but have additional validation
        String allowedProtocolsProperty = System.getProperty("io.allowed.protocols", "http,https,file,jar");
        String[] allowedProtocols = allowedProtocolsProperty.toLowerCase().split(",");
        
        // Trim whitespace from protocols
        for (int i = 0; i < allowedProtocols.length; i++) {
            allowedProtocols[i] = allowedProtocols[i].trim();
        }
        
        // Check if the protocol is allowed
        boolean isAllowed = false;
        for (String allowedProtocol : allowedProtocols) {
            if (protocol.equals(allowedProtocol)) {
                isAllowed = true;
                break;
            }
        }
        
        if (!isAllowed) {
            String sanitizedUrl = sanitizeUrlForLogging(connection.getURL().toString());
            debug("Blocked dangerous URL protocol: " + sanitizedUrl, null);
            throw new SecurityException("URL protocol '" + protocol + "' is not allowed. Allowed protocols: " + allowedProtocolsProperty);
        }
        
        // Additional validation for dangerous protocol patterns (only if not explicitly allowed)
        validateAgainstDangerousProtocols(protocol, allowedProtocols);
        
        // Additional validation for file and jar protocols
        if (protocol.equals("file") || protocol.equals("jar")) {
            validateFileProtocolSafety(connection);
        }
        
        debug("URL protocol validation passed for: " + protocol, null);
    }
    
    /**
     * Validates against known dangerous protocol patterns that should never be allowed
     * unless explicitly configured in allowed protocols.
     * 
     * @param protocol the protocol to validate
     * @param allowedProtocols array of explicitly allowed protocols
     * @throws SecurityException if a dangerous protocol pattern is detected
     */
    private static void validateAgainstDangerousProtocols(String protocol, String[] allowedProtocols) {
        // Critical protocols that should never be allowed even if explicitly configured
        String[] criticallyDangerousProtocols = {
            "javascript", "data", "vbscript"
        };
        
        for (String dangerous : criticallyDangerousProtocols) {
            if (protocol.equals(dangerous)) {
                throw new SecurityException("Critically dangerous protocol '" + protocol + "' is never allowed");
            }
        }
        
        // Other potentially dangerous protocols - only forbidden if not explicitly allowed
        String[] potentiallyDangerousProtocols = {
            "netdoc", "mailto", "gopher", "ldap", "dict", "sftp", "tftp"
        };
        
        // Check if this protocol is explicitly allowed
        boolean explicitlyAllowed = false;
        for (String allowed : allowedProtocols) {
            if (protocol.equals(allowed)) {
                explicitlyAllowed = true;
                break;
            }
        }
        
        // If not explicitly allowed, check if it's in the dangerous list
        if (!explicitlyAllowed) {
            for (String dangerous : potentiallyDangerousProtocols) {
                if (protocol.equals(dangerous)) {
                    throw new SecurityException("Dangerous protocol '" + protocol + "' is forbidden unless explicitly allowed");
                }
            }
        }
        
        // Check for protocol injection attempts
        if (protocol.contains(":") || protocol.contains("/") || protocol.contains("\\") || 
            protocol.contains(" ") || protocol.contains("\t") || protocol.contains("\n") || 
            protocol.contains("\r")) {
            throw new SecurityException("Invalid characters detected in protocol: " + protocol);
        }
    }
    
    /**
     * Validates file and jar protocol URLs for safety.
     * Allows legitimate resource access while blocking dangerous file system access.
     * 
     * @param connection the URLConnection with file or jar protocol
     * @throws SecurityException if the file URL is deemed unsafe
     */
    private static void validateFileProtocolSafety(URLConnection connection) {
        String urlString = connection.getURL().toString();
        String protocol = connection.getURL().getProtocol();
        
        // Check if file protocol validation is disabled for testing
        if (Boolean.parseBoolean(System.getProperty("io.file.protocol.validation.disabled", "false"))) {
            debug("File protocol validation disabled via system property", null);
            return;
        }
        
        // Jar protocols are generally safer as they access files within archives
        if ("jar".equals(protocol)) {
            // Basic validation for jar URLs
            if (urlString.contains("..") || urlString.contains("\0")) {
                throw new SecurityException("Dangerous path patterns detected in jar URL");
            }
            return; // Allow jar protocols with basic validation
        }
        
        // For file protocols, apply more strict validation
        if ("file".equals(protocol)) {
            String path = connection.getURL().getPath();
            if (path == null) {
                throw new SecurityException("File URL path cannot be null");
            }
            
            // Allow only if it's clearly a resource within the application's domain
            // Common patterns for legitimate resources:
            // - ClassLoader.getResource() typically produces paths in target/classes or jar files
            // - Should not allow access to sensitive system paths
            
            if (isSystemPath(path)) {
                throw new SecurityException("File URL accesses system path: " + sanitizeUrlForLogging(urlString));
            }
            
            if (path.contains("..") || path.contains("\0")) {
                throw new SecurityException("Dangerous path patterns detected in file URL");
            }
            
            // Additional check for suspicious paths
            if (isSuspiciousPath(path)) {
                throw new SecurityException("Suspicious file path detected: " + sanitizeUrlForLogging(urlString));
            }
            
            debug("File protocol validation passed for resource path", null);
        }
    }
    
    /**
     * Checks if a path accesses system directories that should be protected.
     * 
     * @param path the file path to check
     * @return true if the path accesses system directories
     */
    private static boolean isSystemPath(String path) {
        if (path == null) return false;
        
        String lowerPath = path.toLowerCase();
        
        // Unix/Linux system paths
        if (lowerPath.startsWith("/etc/") || lowerPath.startsWith("/proc/") || 
            lowerPath.startsWith("/sys/") || lowerPath.startsWith("/dev/")) {
            return true;
        }
        
        // Windows system paths
        if (lowerPath.contains("system32") || lowerPath.contains("syswow64") ||
            lowerPath.contains("\\windows\\") || lowerPath.contains("/windows/")) {
            return true;
        }
        
        return false;
    }
    
    /**
     * Checks if a path contains suspicious patterns that might indicate an attack.
     * 
     * @param path the file path to check
     * @return true if suspicious patterns are detected
     */
    private static boolean isSuspiciousPath(String path) {
        if (path == null) return false;
        
        // Check for hidden directories that might contain sensitive files
        if (path.contains("/.ssh/") || path.contains("/.gnupg/") || 
            path.contains("/.aws/") || path.contains("/.docker/")) {
            return true;
        }
        
        // Check for passwd, shadow files, and other sensitive files
        if (path.endsWith("/passwd") || path.endsWith("/shadow") || 
            path.contains("id_rsa") || path.contains("private")) {
            return true;
        }
        
        return false;
    }
    
    /**
     * Sanitizes URLs for safe logging by masking sensitive parts.
     * 
     * @param url the URL to sanitize
     * @return sanitized URL safe for logging
     */
    private static String sanitizeUrlForLogging(String url) {
        if (url == null) return "[null]";
        
        // Check if detailed logging is explicitly enabled
        boolean allowDetailedLogging = Boolean.parseBoolean(System.getProperty("io.debug.detailed.urls", "false"));
        if (!allowDetailedLogging) {
            // Only show protocol and length for security
            try {
                java.net.URL urlObj = new java.net.URL(url);
                return "[" + urlObj.getProtocol() + "://...:" + url.length() + "-chars]";
            } catch (Exception e) {
                return "[malformed-url:" + url.length() + "-chars]";
            }
        }
        
        // Detailed logging when explicitly enabled - still sanitize credentials
        String sanitized = url.replaceAll("://[^@/]*@", "://[credentials]@");
        if (sanitized.length() > 200) {
            sanitized = sanitized.substring(0, 200) + "...[truncated]";
        }
        return sanitized;
    }

    /**
     * Sanitizes file paths for safe logging by limiting length and removing sensitive information.
     * This method prevents information disclosure through log files by masking potentially
     * sensitive path information while preserving enough detail for security analysis.
     * 
     * @param path the file path to sanitize
     * @return sanitized path safe for logging
     */
    private static String sanitizePathForLogging(String path) {
        if (path == null) return "[null]";
        
        // Check if detailed logging is explicitly enabled (for debugging only)
        boolean allowDetailedLogging = Boolean.parseBoolean(System.getProperty("io.debug.detailed.paths", "false"));
        if (!allowDetailedLogging) {
            // Minimal logging - only show basic pattern information to prevent information disclosure
            if (path.contains("..")) {
                return "[path-with-traversal-pattern]";
            }
            if (path.contains("\0")) {
                return "[path-with-null-byte]";
            }
            if (path.toLowerCase().contains("system32") || path.toLowerCase().contains("syswow64")) {
                return "[windows-system-path]";
            }
            if (path.startsWith("/proc/") || path.startsWith("/sys/") || path.startsWith("/dev/") || path.startsWith("/etc/")) {
                return "[unix-system-path]";
            }
            if (path.contains("/.")) {
                return "[hidden-directory-path]";
            }
            // Generic path indicator without exposing structure
            return "[file-path:" + path.length() + "-chars]";
        }
        
        // Detailed logging only when explicitly enabled (for debugging)
        // Limit length and mask potentially sensitive parts
        if (path.length() > 100) {
            path = path.substring(0, 100) + "...[truncated]";
        }
        // Remove any remaining control characters for log safety
        return path.replaceAll("[\\x00-\\x1F\\x7F]", "?");
    }

    /**
     * Gets an appropriate InputStream from a URLConnection, handling compression if necessary.
     * <p>
     * This method automatically detects and handles various compression encodings
     * and optimizes connection performance with appropriate buffer sizing and connection parameters.
     * </p>
     * <ul>
     *   <li>GZIP ("gzip" or "x-gzip")</li>
     *   <li>DEFLATE ("deflate")</li>
     * </ul>
     *
     * @param c the URLConnection to get the input stream from
     * @return a buffered InputStream, potentially wrapped with a decompressing stream
     * @throws IOException if an I/O error occurs (thrown as unchecked)
     */
    public static InputStream getInputStream(URLConnection c) {
        Convention.throwIfNull(c, "URLConnection cannot be null");
        
        // Validate URL protocol to prevent SSRF and local file access attacks
        validateUrlProtocol(c);

        // Optimize connection parameters before getting the stream
        optimizeConnection(c);

        // Cache content encoding before opening the stream to avoid additional HTTP header lookups
        String enc = c.getContentEncoding();

        // Get the input stream - this is the slow operation
        InputStream is;
        try {
            is = c.getInputStream();
        } catch (IOException e) {
            ExceptionUtilities.uncheckedThrow(e);
            return null; // unreachable
        }

        // Apply decompression based on encoding
        if (enc != null) {
            if ("gzip".equalsIgnoreCase(enc) || "x-gzip".equalsIgnoreCase(enc)) {
                try {
                    is = new GZIPInputStream(is, TRANSFER_BUFFER);
                } catch (IOException e) {
                    ExceptionUtilities.uncheckedThrow(e);
                    return null; // unreachable
                }
            } else if ("deflate".equalsIgnoreCase(enc)) {
                is = new InflaterInputStream(is, new Inflater(), TRANSFER_BUFFER);
            }
        }

        return new BufferedInputStream(is, TRANSFER_BUFFER);
    }

    /**
     * Optimizes a URLConnection for faster input stream access.
     *
     * @param c the URLConnection to optimize
     */
    private static void optimizeConnection(URLConnection c) {
        // Only apply HTTP-specific optimizations to HttpURLConnection
        if (c instanceof HttpURLConnection) {
            HttpURLConnection http = (HttpURLConnection) c;

            // Set to true to allow HTTP redirects
            http.setInstanceFollowRedirects(true);

            // Disable caching to avoid disk operations
            http.setUseCaches(false);
            
            // Use secure timeout validation to prevent injection attacks
            int connectTimeout = getValidatedTimeout("io.connect.timeout", DEFAULT_CONNECT_TIMEOUT, "connect timeout");
            int readTimeout = getValidatedTimeout("io.read.timeout", DEFAULT_READ_TIMEOUT, "read timeout");
            
            http.setConnectTimeout(connectTimeout);
            http.setReadTimeout(readTimeout);

            // Apply general URLConnection optimizations
            c.setRequestProperty("Accept-Encoding", "gzip, x-gzip, deflate");
        }
    }

    /**
     * Transfers the contents of a File to a URLConnection's output stream.
     * <p>
     * Progress can be monitored and the transfer can be cancelled through the callback interface.
     * </p>
     *
     * @param f  the source File to transfer
     * @param c  the destination URLConnection
     * @param cb optional callback for progress monitoring and cancellation (may be null)
     * @return the number of bytes transferred
     * @throws IOException if an I/O error occurs during the transfer (thrown as unchecked)
     */
    public static long transfer(File f, URLConnection c, TransferCallback cb) {
        Convention.throwIfNull(f, "File cannot be null");
        Convention.throwIfNull(c, "URLConnection cannot be null");
        validateFilePath(f);
        try (InputStream in = new BufferedInputStream(Files.newInputStream(f.toPath()));
             OutputStream out = new BufferedOutputStream(c.getOutputStream())) {
            return transfer(in, out, cb);
        } catch (IOException e) {
            ExceptionUtilities.uncheckedThrow(e);
            return 0; // unreachable
        }
    }

    /**
     * Transfers the contents of a URLConnection's input stream to a File.
     * <p>
     * Progress can be monitored and the transfer can be cancelled through the callback interface.
     * Automatically handles compressed streams.
     * </p>
     *
     * @param c  the source URLConnection
     * @param f  the destination File
     * @param cb optional callback for progress monitoring and cancellation (may be null)
     * @return the number of bytes transferred
     * @throws IOException if an I/O error occurs during the transfer (thrown as unchecked)
     */
    public static long transfer(URLConnection c, File f, TransferCallback cb) {
        Convention.throwIfNull(c, "URLConnection cannot be null");
        Convention.throwIfNull(f, "File cannot be null");
        validateFilePath(f);
        try (InputStream in = getInputStream(c)) {
            return transfer(in, f, cb);
        } catch (IOException e) {
            ExceptionUtilities.uncheckedThrow(e);
            return 0; // unreachable
        }
    }

    /**
     * Transfers the contents of an InputStream to a File.
     * <p>
     * Progress can be monitored and the transfer can be cancelled through the callback interface.
     * The output stream is automatically buffered for optimal performance.
     * </p>
     *
     * @param s  the source InputStream
     * @param f  the destination File
     * @param cb optional callback for progress monitoring and cancellation (may be null)
     * @return the number of bytes transferred
     * @throws IOException if an I/O error occurs during the transfer (thrown as unchecked)
     */
    public static long transfer(InputStream s, File f, TransferCallback cb) {
        Convention.throwIfNull(s, "InputStream cannot be null");
        Convention.throwIfNull(f, "File cannot be null");
        validateFilePath(f);
        try (OutputStream out = new BufferedOutputStream(Files.newOutputStream(f.toPath()))) {
            return transfer(s, out, cb);
        } catch (IOException e) {
            ExceptionUtilities.uncheckedThrow(e);
            return 0; // unreachable
        }
    }

    /**
     * Creates a safe defensive copy of the transfer buffer for callback use.
     * This prevents race conditions where the callback might modify the buffer
     * while it's still being used for transfer operations, or where multiple
     * callbacks might access the same buffer concurrently.
     * 
     * @param buffer the original transfer buffer
     * @param count the number of valid bytes in the buffer
     * @return a defensive copy containing only the valid data
     */
    private static byte[] createSafeCallbackBuffer(byte[] buffer, int count) {
        if (count <= 0) {
            return new byte[0];
        }
        
        // Create a defensive copy with only the valid data to prevent:
        // 1. Buffer corruption if callback modifies the array
        // 2. Race conditions with concurrent buffer access
        // 3. Information leakage of unused buffer portions
        byte[] callbackBuffer = new byte[count];
        System.arraycopy(buffer, 0, callbackBuffer, 0, count);
        return callbackBuffer;
    }

    /**
     * Transfers bytes from an input stream to an output stream with optional progress monitoring.
     * <p>
     * This method does not close the streams; that responsibility remains with the caller.
     * Progress can be monitored and the transfer can be cancelled through the callback interface.
     * The callback receives a defensive copy of the buffer to prevent race conditions and data corruption.
     * </p>
     *
     * @param in  the source InputStream
     * @param out the destination OutputStream
     * @param cb  optional callback for progress monitoring and cancellation (may be null)
     * @return the number of bytes transferred
     * @throws IOException if an I/O error occurs during transfer (thrown as unchecked)
     */
    public static long transfer(InputStream in, OutputStream out, TransferCallback cb) {
        Convention.throwIfNull(in, "InputStream cannot be null");
        Convention.throwIfNull(out, "OutputStream cannot be null");
        try {
            byte[] buffer = new byte[TRANSFER_BUFFER];
            int count;
            long total = 0;
            while ((count = in.read(buffer)) != -1) {
                out.write(buffer, 0, count);
                total += count;
                if (cb != null) {
                    // Create a defensive copy to prevent race conditions and buffer corruption
                    byte[] callbackBuffer = createSafeCallbackBuffer(buffer, count);
                    cb.bytesTransferred(callbackBuffer, count);
                    if (cb.isCancelled()) {
                        break;
                    }
                }
            }
            return total;
        } catch (IOException e) {
            ExceptionUtilities.uncheckedThrow(e);
            return 0; // unreachable
        }
    }

    /**
     * Reads exactly the specified number of bytes from an InputStream into a byte array.
     * <p>
     * This method will continue reading until either the byte array is full or the end of the stream is reached.
     * Uses DataInputStream.readFully for a simpler implementation.
     * </p>
     *
     * @param in    the InputStream to read from
     * @param bytes the byte array to fill
     * @return the number of bytes transferred (always bytes.length if successful)
     * @throws IOException if the stream ends before the byte array is filled or if any other I/O error occurs (thrown as unchecked)
     */
    public static int transfer(InputStream in, byte[] bytes) {
        Convention.throwIfNull(in, "InputStream cannot be null");
        Convention.throwIfNull(bytes, "byte array cannot be null");
        try {
            new DataInputStream(in).readFully(bytes);
            return bytes.length;
        } catch (IOException e) {
            ExceptionUtilities.uncheckedThrow(e);
            return 0; // unreachable
        }
    }

    /**
     * Transfers all bytes from an input stream to an output stream.
     * <p>
     * This method does not close the streams; that responsibility remains with the caller.
     * Uses an internal buffer for efficient transfer.
     * </p>
     *
     * @param in  the source InputStream
     * @param out the destination OutputStream
     * @return the number of bytes transferred
     * @throws IOException if an I/O error occurs during transfer (thrown as unchecked)
     */
    public static long transfer(InputStream in, OutputStream out) {
        Convention.throwIfNull(in, "InputStream cannot be null");
        Convention.throwIfNull(out, "OutputStream cannot be null");
        try {
            byte[] buffer = new byte[TRANSFER_BUFFER];
            int count;
            long total = 0;
            while ((count = in.read(buffer)) != -1) {
                out.write(buffer, 0, count);
                total += count;
            }
            out.flush();
            return total;
        } catch (IOException e) {
            ExceptionUtilities.uncheckedThrow(e);
            return 0; // unreachable
        }
    }

    /**
     * Transfers the contents of a File to an OutputStream.
     * <p>
     * The input is automatically buffered for optimal performance.
     * The output stream is flushed after the transfer but not closed.
     * </p>
     *
     * @param file the source File
     * @param out  the destination OutputStream
     * @return the number of bytes transferred
     * @throws IOException if an I/O error occurs during transfer (thrown as unchecked)
     */
    public static long transfer(File file, OutputStream out) {
        Convention.throwIfNull(file, "File cannot be null");
        Convention.throwIfNull(out, "OutputStream cannot be null");
        validateFilePath(file);
        try (InputStream in = new BufferedInputStream(Files.newInputStream(file.toPath()), TRANSFER_BUFFER)) {
            return transfer(in, out);
        } catch (IOException e) {
            ExceptionUtilities.uncheckedThrow(e);
            return 0; // unreachable
        } finally {
            flush(out);
        }
    }

    /**
     * Closes an XMLStreamReader, throwing any exceptions as unchecked.
     * <p>
     * This method can be safely used in finally blocks without requiring a try-catch,
     * as {@link XMLStreamException} will be thrown unchecked via {@link ExceptionUtilities#uncheckedThrow(Throwable)}.
     * This provides cleaner code while ensuring close failures are visible rather than silently swallowed.
     * </p>
     * <p>
     * Close exceptions are rare but important - they often indicate serious issues like network failures,
     * resource exhaustion, or data corruption. Making them visible helps diagnose problems earlier.
     * </p>
     *
     * @param reader the XMLStreamReader to close (may be null)
     * @throws XMLStreamException if close fails (thrown as unchecked)
     */
    public static void close(XMLStreamReader reader) {
        if (reader != null) {
            try {
                reader.close();
            } catch (XMLStreamException e) {
                ExceptionUtilities.uncheckedThrow(e);
            }
        }
    }

    /**
     * Closes an XMLStreamWriter, throwing any exceptions as unchecked.
     * <p>
     * This method can be safely used in finally blocks without requiring a try-catch,
     * as {@link XMLStreamException} will be thrown unchecked via {@link ExceptionUtilities#uncheckedThrow(Throwable)}.
     * This provides cleaner code while ensuring close failures are visible rather than silently swallowed.
     * </p>
     *
     * @param writer the XMLStreamWriter to close (may be null)
     * @throws XMLStreamException if close fails (thrown as unchecked)
     */
    public static void close(XMLStreamWriter writer) {
        if (writer != null) {
            try {
                writer.close();
            } catch (XMLStreamException e) {
                ExceptionUtilities.uncheckedThrow(e);
            }
        }
    }

    /**
     * Closes any Closeable resource, throwing any exceptions as unchecked.
     * <p>
     * This method can be safely used in finally blocks or cleanup code without requiring a try-catch,
     * as {@link IOException} will be thrown unchecked via {@link ExceptionUtilities#uncheckedThrow(Throwable)}.
     * This provides cleaner code while ensuring close failures are visible rather than silently swallowed.
     * </p>
     * <p>
     * <b>Why close exceptions matter:</b> While rare, close failures often indicate serious issues:
     * <ul>
     *   <li>File streams: Disk full, permission denied, filesystem corruption</li>
     *   <li>Network streams: Connection lost, timeout, broken pipe</li>
     *   <li>Database connections: Transaction rollback failures, connection pool issues</li>
     *   <li>Compressed streams: Incomplete data, corruption, checksum failures</li>
     * </ul>
     * Making these exceptions visible helps diagnose infrastructure problems early rather than
     * hiding them until they cause more serious failures downstream.
     * </p>
     *
     * @param c the Closeable resource to close (may be null)
     * @throws IOException if close fails (thrown as unchecked)
     */
    public static void close(Closeable c) {
        if (c != null) {
            try {
                c.close();
            } catch (IOException e) {
                ExceptionUtilities.uncheckedThrow(e);
            }
        }
    }

    /**
     * Flushes any Flushable resource, throwing any exceptions as unchecked.
     * <p>
     * This method can be safely used without requiring a try-catch, as {@link IOException}
     * will be thrown unchecked via {@link ExceptionUtilities#uncheckedThrow(Throwable)}.
     * Flush failures often indicate buffer overflow, disk full, or network issues that
     * should be made visible rather than silently ignored.
     * </p>
     *
     * @param f the Flushable resource to flush (may be null)
     * @throws IOException if flush fails (thrown as unchecked)
     */
    public static void flush(Flushable f) {
        if (f != null) {
            try {
                f.flush();
            } catch (IOException e) {
                ExceptionUtilities.uncheckedThrow(e);
            }
        }
    }

    /**
     * Flushes an XMLStreamWriter, throwing any exceptions as unchecked.
     * <p>
     * This method can be safely used without requiring a try-catch, as {@link XMLStreamException}
     * will be thrown unchecked via {@link ExceptionUtilities#uncheckedThrow(Throwable)}.
     * Flush failures often indicate buffer or output stream issues that should be made visible.
     * </p>
     *
     * @param writer the XMLStreamWriter to flush (may be null)
     * @throws XMLStreamException if flush fails (thrown as unchecked)
     */
    public static void flush(XMLStreamWriter writer) {
        if (writer != null) {
            try {
                writer.flush();
            } catch (XMLStreamException e) {
                ExceptionUtilities.uncheckedThrow(e);
            }
        }
    }

    /**
     * Converts an InputStream's contents to a byte array.
     * <p>
     * This method loads the entire stream into memory, so use with appropriate consideration for memory usage.
     * Uses a default maximum size limit (2GB) to prevent memory exhaustion attacks while allowing reasonable
     * data transfer operations. For custom limits, use {@link #inputStreamToBytes(InputStream, int)}.
     * </p>
     *
     * @param in the InputStream to read from
     * @return the byte array containing the stream's contents
     * @throws IOException if an I/O error occurs or the stream exceeds the default size limit (thrown as unchecked)
     */
    public static byte[] inputStreamToBytes(InputStream in) {
        return inputStreamToBytes(in, getDefaultMaxStreamSize());
    }

    /**
     * Converts an InputStream's contents to a byte array with a maximum size limit.
     *
     * @param in      the InputStream to read from
     * @param maxSize the maximum number of bytes to read
     * @return the byte array containing the stream's contents
     * @throws IOException if an I/O error occurs or the stream exceeds maxSize (thrown as unchecked)
     */
    public static byte[] inputStreamToBytes(InputStream in, int maxSize) {
        Convention.throwIfNull(in, "Inputstream cannot be null");
        if (maxSize <= 0) {
            throw new IllegalArgumentException("maxSize must be > 0");
        }
        try (FastByteArrayOutputStream out = new FastByteArrayOutputStream(Math.min(16384, maxSize))) {
            byte[] buffer = new byte[Math.min(TRANSFER_BUFFER, maxSize)];
            int total = 0;
            int count;
            while (total < maxSize && (count = in.read(buffer, 0, Math.min(buffer.length, maxSize - total))) != -1) {
                if (total + count > maxSize) {
                    throw new IOException("Stream exceeds maximum allowed size: " + maxSize);
                }
                total += count;
                out.write(buffer, 0, count);
            }
            // Check if there's more data after reaching the limit
            if (total >= maxSize && in.read() != -1) {
                throw new IOException("Stream exceeds maximum allowed size: " + maxSize);
            }
            return out.toByteArray();
        } catch (IOException e) {
            ExceptionUtilities.uncheckedThrow(e);
            return null; // unreachable
        }
    }

    /**
     * Transfers a byte array to a URLConnection's output stream.
     * <p>
     * The output stream is automatically buffered for optimal performance and properly closed after transfer.
     * </p>
     *
     * @param c     the URLConnection to write to
     * @param bytes the byte array to transfer
     * @return the number of bytes transferred
     * @throws IOException if an I/O error occurs during transfer (thrown as unchecked)
     */
    public static int transfer(URLConnection c, byte[] bytes) {
        Convention.throwIfNull(c, "URLConnection cannot be null");
        Convention.throwIfNull(bytes, "byte array cannot be null");
        try (OutputStream out = new BufferedOutputStream(c.getOutputStream())) {
            out.write(bytes);
            return bytes.length;
        } catch (IOException e) {
            ExceptionUtilities.uncheckedThrow(e);
            return 0; // unreachable
        }
    }

    /**
     * Compresses the contents of one ByteArrayOutputStream into another using GZIP compression.
     * <p>
     * Uses BEST_SPEED compression level for optimal performance.
     * </p>
     *
     * @param original   the ByteArrayOutputStream containing the data to compress
     * @param compressed the ByteArrayOutputStream to receive the compressed data
     * @throws IOException if an I/O error occurs during compression (thrown as unchecked)
     */
    public static void compressBytes(ByteArrayOutputStream original, ByteArrayOutputStream compressed) {
        Convention.throwIfNull(original, "Original ByteArrayOutputStream cannot be null");
        Convention.throwIfNull(compressed, "Compressed ByteArrayOutputStream cannot be null");
        try (DeflaterOutputStream gzipStream = new AdjustableGZIPOutputStream(compressed, Deflater.BEST_SPEED)) {
            original.writeTo(gzipStream);
            gzipStream.flush();
        } catch (IOException e) {
            ExceptionUtilities.uncheckedThrow(e);
        }
    }

    /**
     * Compresses the contents of one FastByteArrayOutputStream into another using GZIP compression.
     * <p>
     * Uses BEST_SPEED compression level for optimal performance.
     * </p>
     *
     * @param original   the FastByteArrayOutputStream containing the data to compress
     * @param compressed the FastByteArrayOutputStream to receive the compressed data
     * @throws IOException if an I/O error occurs during compression (thrown as unchecked)
     */
    public static void compressBytes(FastByteArrayOutputStream original, FastByteArrayOutputStream compressed) {
        Convention.throwIfNull(original, "Original FastByteArrayOutputStream cannot be null");
        Convention.throwIfNull(compressed, "Compressed FastByteArrayOutputStream cannot be null");
        try (DeflaterOutputStream gzipStream = new AdjustableGZIPOutputStream(compressed, Deflater.BEST_SPEED)) {
            gzipStream.write(original.toByteArray(), 0, original.size());
            gzipStream.flush();
        } catch (IOException e) {
            ExceptionUtilities.uncheckedThrow(e);
        }
    }

    /**
     * Compresses a byte array using GZIP compression.
     *
     * @param bytes the byte array to compress
     * @return a new byte array containing the compressed data
     * @throws RuntimeException if compression fails
     */
    public static byte[] compressBytes(byte[] bytes) {
        return compressBytes(bytes, 0, bytes.length);
    }

    /**
     * Compresses a portion of a byte array using GZIP compression.
     *
     * @param bytes  the source byte array
     * @param offset the starting position in the source array
     * @param len    the number of bytes to compress
     * @return a new byte array containing the compressed data
     * @throws RuntimeException if compression fails
     */
    public static byte[] compressBytes(byte[] bytes, int offset, int len) {
        Convention.throwIfNull(bytes, "Byte array cannot be null");
        try (FastByteArrayOutputStream byteStream = new FastByteArrayOutputStream()) {
            try (DeflaterOutputStream gzipStream = new AdjustableGZIPOutputStream(byteStream, Deflater.BEST_SPEED)) {
                gzipStream.write(bytes, offset, len);
                gzipStream.flush();
            }
            return Arrays.copyOf(byteStream.toByteArray(), byteStream.size());
        } catch (Exception e) {
            throw new RuntimeException("Error compressing bytes.", e);
        }
    }

    /**
     * Uncompresses a GZIP-compressed byte array with default size limits.
     * <p>
     * If the input is not GZIP-compressed, returns the original array unchanged.
     * Uses a default maximum decompressed size (2GB) to prevent zip bomb attacks.
     * </p>
     *
     * @param bytes the compressed byte array
     * @return the uncompressed byte array, or the original array if not compressed
     * @throws RuntimeException if decompression fails or exceeds size limits
     */
    public static byte[] uncompressBytes(byte[] bytes) {
        return uncompressBytes(bytes, 0, bytes.length, getDefaultMaxDecompressionSize());
    }

    /**
     * Uncompresses a portion of a GZIP-compressed byte array with default size limits.
     * <p>
     * If the input is not GZIP-compressed, returns the original array unchanged.
     * Uses a default maximum decompressed size (2GB) to prevent zip bomb attacks.
     * </p>
     *
     * @param bytes  the compressed byte array
     * @param offset the starting position in the source array
     * @param len    the number of bytes to uncompress
     * @return the uncompressed byte array, or the original array if not compressed
     * @throws RuntimeException if decompression fails or exceeds size limits
     */
    public static byte[] uncompressBytes(byte[] bytes, int offset, int len) {
        return uncompressBytes(bytes, offset, len, getDefaultMaxDecompressionSize());
    }

    /**
     * Uncompresses a portion of a GZIP-compressed byte array with specified size limit.
     * <p>
     * If the input is not GZIP-compressed, returns the original array unchanged.
     * </p>
     *
     * @param bytes    the compressed byte array
     * @param offset   the starting position in the source array
     * @param len      the number of bytes to uncompress
     * @param maxSize  the maximum allowed decompressed size in bytes
     * @return the uncompressed byte array, or the original array if not compressed
     * @throws RuntimeException if decompression fails or exceeds size limits
     */
    public static byte[] uncompressBytes(byte[] bytes, int offset, int len, int maxSize) {
        Objects.requireNonNull(bytes, "Byte array cannot be null");
        if (maxSize <= 0) {
            throw new IllegalArgumentException("maxSize must be > 0");
        }
        
        if (ByteUtilities.isGzipped(bytes, offset)) {
            try (ByteArrayInputStream byteStream = new ByteArrayInputStream(bytes, offset, len);
                 GZIPInputStream gzipStream = new GZIPInputStream(byteStream, TRANSFER_BUFFER)) {
                return inputStreamToBytes(gzipStream, maxSize);
            } catch (IOException e) {
                throw new RuntimeException("Error uncompressing bytes", e);
            }
        }
        return Arrays.copyOfRange(bytes, offset, offset + len);
    }

    /**
     * Callback interface for monitoring and controlling byte transfers.
     * <p>
     * The callback receives a defensive copy of the transfer buffer to ensure thread safety
     * and prevent race conditions. Implementations can safely modify the provided buffer
     * without affecting the ongoing transfer operation.
     * </p>
     */
    @FunctionalInterface
    public interface TransferCallback {
        /**
         * Called when bytes are transferred during an operation.
         * <p>
         * The provided buffer is a defensive copy containing only the transferred bytes.
         * It is safe to modify this buffer without affecting the transfer operation.
         * </p>
         *
         * @param bytes the buffer containing the transferred bytes (defensive copy)
         * @param count the number of bytes actually transferred (equals bytes.length)
         */
        void bytesTransferred(byte[] bytes, int count);

        /**
         * Checks if the transfer operation should be cancelled.
         * Default implementation returns false.
         *
         * @return true if the transfer should be cancelled, false to continue
         */
        default boolean isCancelled() {
            return false;
        }
    }
}
