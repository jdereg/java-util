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
 *   <li>Silent exception handling for close/flush operations</li>
 *   <li>Progress tracking through callback mechanism</li>
 *   <li>Support for XML stream operations</li>
 *   <li>
 *     <b>XML stream support:</b> Some methods work with {@code javax.xml.stream.XMLStreamReader} and
 *     {@code javax.xml.stream.XMLStreamWriter}. <b>These methods require the {@code java.xml} module to be present at runtime.</b>
 *     If you're using OSGi, ensure your bundle imports the {@code javax.xml.stream} package or declare it as an optional import
 *     if XML support is not required. The rest of the library does <b>not</b> require {@code java.xml}.
 *   </li>
 * </ul>
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
     * Gets the default maximum stream size for security purposes.
     * Can be configured via system property 'io.max.stream.size'.
     * Defaults to 2GB if not configured.
     *
     * @return the maximum allowed stream size in bytes
     */
    private static int getDefaultMaxStreamSize() {
        try {
            return Integer.parseInt(System.getProperty("io.max.stream.size", "2147483647")); // 2GB default (Integer.MAX_VALUE)
        } catch (NumberFormatException e) {
            return 2147483647; // 2GB fallback
        }
    }

    /**
     * Gets the default maximum decompression size for security purposes.
     * Can be configured via system property 'io.max.decompression.size'.
     * Defaults to 2GB if not configured.
     *
     * @return the maximum allowed decompressed data size in bytes
     */
    private static int getDefaultMaxDecompressionSize() {
        try {
            return Integer.parseInt(System.getProperty("io.max.decompression.size", "2147483647")); // 2GB default
        } catch (NumberFormatException e) {
            return 2147483647; // 2GB fallback
        }
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
            int connectTimeout = DEFAULT_CONNECT_TIMEOUT;
            int readTimeout = DEFAULT_READ_TIMEOUT;
            try {
                connectTimeout = Integer.parseInt(System.getProperty("io.connect.timeout", String.valueOf(DEFAULT_CONNECT_TIMEOUT)));
                readTimeout = Integer.parseInt(System.getProperty("io.read.timeout", String.valueOf(DEFAULT_READ_TIMEOUT)));
            } catch (NumberFormatException e) {
                debug("Invalid timeout configuration detected, using defaults", null);
            }
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
     * @throws IOException if an I/O error occurs during the transfer (thrown as unchecked)
     */
    public static void transfer(File f, URLConnection c, TransferCallback cb) {
        Convention.throwIfNull(f, "File cannot be null");
        Convention.throwIfNull(c, "URLConnection cannot be null");
        validateFilePath(f);
        try (InputStream in = new BufferedInputStream(Files.newInputStream(f.toPath()));
             OutputStream out = new BufferedOutputStream(c.getOutputStream())) {
            transfer(in, out, cb);
        } catch (IOException e) {
            ExceptionUtilities.uncheckedThrow(e);
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
     * @throws IOException if an I/O error occurs during the transfer (thrown as unchecked)
     */
    public static void transfer(URLConnection c, File f, TransferCallback cb) {
        Convention.throwIfNull(c, "URLConnection cannot be null");
        Convention.throwIfNull(f, "File cannot be null");
        validateFilePath(f);
        try (InputStream in = getInputStream(c)) {
            transfer(in, f, cb);
        } catch (IOException e) {
            ExceptionUtilities.uncheckedThrow(e);
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
     * @throws IOException if an I/O error occurs during the transfer (thrown as unchecked)
     */
    public static void transfer(InputStream s, File f, TransferCallback cb) {
        Convention.throwIfNull(s, "InputStream cannot be null");
        Convention.throwIfNull(f, "File cannot be null");
        validateFilePath(f);
        try (OutputStream out = new BufferedOutputStream(Files.newOutputStream(f.toPath()))) {
            transfer(s, out, cb);
        } catch (IOException e) {
            ExceptionUtilities.uncheckedThrow(e);
        }
    }

    /**
     * Transfers bytes from an input stream to an output stream with optional progress monitoring.
     * <p>
     * This method does not close the streams; that responsibility remains with the caller.
     * Progress can be monitored and the transfer can be cancelled through the callback interface.
     * </p>
     *
     * @param in  the source InputStream
     * @param out the destination OutputStream
     * @param cb  optional callback for progress monitoring and cancellation (may be null)
     * @throws IOException if an I/O error occurs during transfer (thrown as unchecked)
     */
    public static void transfer(InputStream in, OutputStream out, TransferCallback cb) {
        Convention.throwIfNull(in, "InputStream cannot be null");
        Convention.throwIfNull(out, "OutputStream cannot be null");
        try {
            byte[] buffer = new byte[TRANSFER_BUFFER];
            int count;
            while ((count = in.read(buffer)) != -1) {
                out.write(buffer, 0, count);
                if (cb != null) {
                    cb.bytesTransferred(buffer, count);
                    if (cb.isCancelled()) {
                        break;
                    }
                }
            }
        } catch (IOException e) {
            ExceptionUtilities.uncheckedThrow(e);
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
     * @throws IOException if the stream ends before the byte array is filled or if any other I/O error occurs (thrown as unchecked)
     */
    public static void transfer(InputStream in, byte[] bytes) {
        Convention.throwIfNull(in, "InputStream cannot be null");
        Convention.throwIfNull(bytes, "byte array cannot be null");
        try {
            new DataInputStream(in).readFully(bytes);
        } catch (IOException e) {
            ExceptionUtilities.uncheckedThrow(e);
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
     * @throws IOException if an I/O error occurs during transfer (thrown as unchecked)
     */
    public static void transfer(InputStream in, OutputStream out) {
        Convention.throwIfNull(in, "InputStream cannot be null");
        Convention.throwIfNull(out, "OutputStream cannot be null");
        try {
            byte[] buffer = new byte[TRANSFER_BUFFER];
            int count;
            while ((count = in.read(buffer)) != -1) {
                out.write(buffer, 0, count);
            }
            out.flush();
        } catch (IOException e) {
            ExceptionUtilities.uncheckedThrow(e);
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
     * @throws IOException if an I/O error occurs during transfer (thrown as unchecked)
     */
    public static void transfer(File file, OutputStream out) {
        Convention.throwIfNull(file, "File cannot be null");
        Convention.throwIfNull(out, "OutputStream cannot be null");
        validateFilePath(file);
        try (InputStream in = new BufferedInputStream(Files.newInputStream(file.toPath()), TRANSFER_BUFFER)) {
            transfer(in, out);
        } catch (IOException e) {
            ExceptionUtilities.uncheckedThrow(e);
        } finally {
            flush(out);
        }
    }

    /**
     * Safely closes an XMLStreamReader, suppressing any exceptions.
     *
     * @param reader the XMLStreamReader to close (may be null)
     */
    public static void close(XMLStreamReader reader) {
        if (reader != null) {
            try {
                reader.close();
            } catch (XMLStreamException e) {
                debug("Failed to close XMLStreamReader", e);
            }
        }
    }

    /**
     * Safely closes an XMLStreamWriter, suppressing any exceptions.
     *
     * @param writer the XMLStreamWriter to close (may be null)
     */
    public static void close(XMLStreamWriter writer) {
        if (writer != null) {
            try {
                writer.close();
            } catch (XMLStreamException e) {
                debug("Failed to close XMLStreamWriter", e);
            }
        }
    }

    /**
     * Safely closes any Closeable resource, suppressing any exceptions.
     *
     * @param c the Closeable resource to close (may be null)
     */
    public static void close(Closeable c) {
        if (c != null) {
            try {
                c.close();
            } catch (IOException e) {
                debug("Failed to close Closeable", e);
            }
        }
    }

    /**
     * Safely flushes any Flushable resource, suppressing any exceptions.
     *
     * @param f the Flushable resource to flush (may be null)
     */
    public static void flush(Flushable f) {
        if (f != null) {
            try {
                f.flush();
            } catch (IOException e) {
                debug("Failed to flush", e);
            }
        }
    }

    /**
     * Safely flushes an XMLStreamWriter, suppressing any exceptions.
     *
     * @param writer the XMLStreamWriter to flush (may be null)
     */
    public static void flush(XMLStreamWriter writer) {
        if (writer != null) {
            try {
                writer.flush();
            } catch (XMLStreamException e) {
                debug("Failed to flush XMLStreamWriter", e);
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
     * @throws IOException if an I/O error occurs during transfer (thrown as unchecked)
     */
    public static void transfer(URLConnection c, byte[] bytes) {
        Convention.throwIfNull(c, "URLConnection cannot be null");
        Convention.throwIfNull(bytes, "byte array cannot be null");
        try (OutputStream out = new BufferedOutputStream(c.getOutputStream())) {
            out.write(bytes);
        } catch (IOException e) {
            ExceptionUtilities.uncheckedThrow(e);
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
     */
    @FunctionalInterface
    public interface TransferCallback {
        /**
         * Called when bytes are transferred during an operation.
         *
         * @param bytes the buffer containing the transferred bytes
         * @param count the number of bytes actually transferred
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
