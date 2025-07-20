package com.cedarsoftware.util;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.util.Arrays;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.security.Key;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.spec.AlgorithmParameterSpec;

/**
 * Utility class providing cryptographic operations including hashing, encryption, and decryption.
 * <p>
 * This class offers:
 * </p>
 * <ul>
 *   <li><b>Hash Functions:</b>
 *     <ul>
 *       <li>MD5 (fast implementation)</li>
 *       <li>SHA-1 (fast implementation)</li>
 *       <li>SHA-256</li>
 *       <li>SHA-384</li>
 *       <li>SHA-512</li>
 *       <li>SHA3-256</li>
 *       <li>SHA3-512</li>
 *       <li>Other variants like SHA-224 or SHA3-384 are available via
 *           {@link java.security.MessageDigest}</li>
 *     </ul>
 *   </li>
 *   <li><b>Encryption/Decryption:</b>
 *     <ul>
 *       <li>AES-128 encryption</li>
 *       <li>GCM mode with authentication</li>
 *       <li>Random IV per encryption</li>
 *     </ul>
 *   </li>
 *   <li><b>Optimized File Operations:</b>
 *     <ul>
 *       <li>Efficient buffer management</li>
 *       <li>Large file handling</li>
 *       <li>Custom filesystem support</li>
 *     </ul>
 *   </li>
 * </ul>
 *
 * <h2>Security Configuration</h2>
 * <p>EncryptionUtilities provides configurable security controls to prevent various attack vectors including
 * resource exhaustion, cryptographic parameter manipulation, and large file processing attacks.
 * All security features are <strong>disabled by default</strong> for backward compatibility.</p>
 *
 * <p>Security controls can be enabled via system properties:</p>
 * <ul>
 *   <li><code>encryptionutilities.security.enabled=false</code> &mdash; Master switch for all security features</li>
 *   <li><code>encryptionutilities.file.size.validation.enabled=false</code> &mdash; Enable file size limits for hashing operations</li>
 *   <li><code>encryptionutilities.buffer.size.validation.enabled=false</code> &mdash; Enable buffer size validation</li>
 *   <li><code>encryptionutilities.crypto.parameters.validation.enabled=false</code> &mdash; Enable cryptographic parameter validation</li>
 *   <li><code>encryptionutilities.max.file.size=2147483647</code> &mdash; Maximum file size for hashing operations (2GB)</li>
 *   <li><code>encryptionutilities.max.buffer.size=1048576</code> &mdash; Maximum buffer size (1MB)</li>
 *   <li><code>encryptionutilities.min.pbkdf2.iterations=10000</code> &mdash; Minimum PBKDF2 iterations</li>
 *   <li><code>encryptionutilities.max.pbkdf2.iterations=1000000</code> &mdash; Maximum PBKDF2 iterations</li>
 *   <li><code>encryptionutilities.min.salt.size=8</code> &mdash; Minimum salt size in bytes</li>
 *   <li><code>encryptionutilities.max.salt.size=64</code> &mdash; Maximum salt size in bytes</li>
 *   <li><code>encryptionutilities.min.iv.size=8</code> &mdash; Minimum IV size in bytes</li>
 *   <li><code>encryptionutilities.max.iv.size=32</code> &mdash; Maximum IV size in bytes</li>
 * </ul>
 *
 * <h3>Security Features</h3>
 * <ul>
 *   <li><b>File Size Validation:</b> Prevents memory exhaustion through oversized file processing</li>
 *   <li><b>Buffer Size Validation:</b> Configurable limits on buffer sizes to prevent memory exhaustion</li>
 *   <li><b>Crypto Parameter Validation:</b> Validates cryptographic parameters to ensure security standards</li>
 *   <li><b>PBKDF2 Iteration Validation:</b> Ensures iteration counts meet minimum security requirements</li>
 * </ul>
 *
 * <h3>Usage Example</h3>
 * <pre>{@code
 * // Enable security with custom settings
 * System.setProperty("encryptionutilities.security.enabled", "true");
 * System.setProperty("encryptionutilities.file.size.validation.enabled", "true");
 * System.setProperty("encryptionutilities.max.file.size", "104857600"); // 100MB
 *
 * // These will now enforce security controls
 * String hash = EncryptionUtilities.fastMD5(smallFile); // works
 * String hash2 = EncryptionUtilities.fastMD5(hugeFile); // throws SecurityException if > 100MB
 * }</pre>
 *
 * <p><b>Hash Function Usage:</b></p>
 * <pre>{@code
 * // File hashing
 * String md5 = EncryptionUtilities.fastMD5(new File("example.txt"));
 * String sha1 = EncryptionUtilities.fastSHA1(new File("example.txt"));
 *
 * // Byte array hashing
 * String hash = EncryptionUtilities.calculateMD5Hash(bytes);
 * }</pre>
 *
 * <p><b>Encryption Usage:</b></p>
 * <pre>{@code
 * // String encryption/decryption
 * String encrypted = EncryptionUtilities.encrypt("password", "sensitive data");
 * String decrypted = EncryptionUtilities.decrypt("password", encrypted);
 *
 * // Byte array encryption/decryption
 * String encryptedHex = EncryptionUtilities.encryptBytes("password", originalBytes);
 * byte[] decryptedBytes = EncryptionUtilities.decryptBytes("password", encryptedHex);
 * }</pre>
 *
 * <p><b>Security Notes:</b></p>
 * <ul>
 *   <li>MD5 and SHA-1 are provided for legacy compatibility but are cryptographically broken</li>
 *   <li>Use SHA-256 or SHA-512 for secure hashing</li>
 *   <li>AES implementation uses GCM mode with authentication</li>
 *   <li>IV and salt are randomly generated for each encryption</li>
 * </ul>
 *
 * <p><b>Performance Features:</b></p>
 * <ul>
 *   <li>Optimized buffer sizes for modern storage systems</li>
 *   <li>Heap ByteBuffer usage for efficient memory management</li>
 *   <li>Efficient memory management</li>
 *   <li>Thread-safe implementation</li>
 * </ul>
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
 */
public class EncryptionUtilities {
    // Default security limits
    private static final long DEFAULT_MAX_FILE_SIZE = 2147483647L; // 2GB
    private static final int DEFAULT_MAX_BUFFER_SIZE = 1048576; // 1MB
    private static final int DEFAULT_MIN_PBKDF2_ITERATIONS = 10000;
    private static final int DEFAULT_MAX_PBKDF2_ITERATIONS = 1000000;
    private static final int DEFAULT_MIN_SALT_SIZE = 8;
    private static final int DEFAULT_MAX_SALT_SIZE = 64;
    private static final int DEFAULT_MIN_IV_SIZE = 8;
    private static final int DEFAULT_MAX_IV_SIZE = 32;
    
    // Standard cryptographic parameters (used when security is disabled)
    private static final int STANDARD_PBKDF2_ITERATIONS = 65536;
    private static final int STANDARD_SALT_SIZE = 16;
    private static final int STANDARD_IV_SIZE = 12;
    private static final int STANDARD_BUFFER_SIZE = 64 * 1024; // 64KB
    
    static {
        // Initialize system properties with defaults if not already set (backward compatibility)
        initializeSystemPropertyDefaults();
    }
    
    private static void initializeSystemPropertyDefaults() {
        // Set default values if not explicitly configured
        if (System.getProperty("encryptionutilities.max.file.size") == null) {
            System.setProperty("encryptionutilities.max.file.size", String.valueOf(DEFAULT_MAX_FILE_SIZE));
        }
        if (System.getProperty("encryptionutilities.max.buffer.size") == null) {
            System.setProperty("encryptionutilities.max.buffer.size", String.valueOf(DEFAULT_MAX_BUFFER_SIZE));
        }
        if (System.getProperty("encryptionutilities.min.pbkdf2.iterations") == null) {
            System.setProperty("encryptionutilities.min.pbkdf2.iterations", String.valueOf(DEFAULT_MIN_PBKDF2_ITERATIONS));
        }
        if (System.getProperty("encryptionutilities.max.pbkdf2.iterations") == null) {
            System.setProperty("encryptionutilities.max.pbkdf2.iterations", String.valueOf(DEFAULT_MAX_PBKDF2_ITERATIONS));
        }
        if (System.getProperty("encryptionutilities.min.salt.size") == null) {
            System.setProperty("encryptionutilities.min.salt.size", String.valueOf(DEFAULT_MIN_SALT_SIZE));
        }
        if (System.getProperty("encryptionutilities.max.salt.size") == null) {
            System.setProperty("encryptionutilities.max.salt.size", String.valueOf(DEFAULT_MAX_SALT_SIZE));
        }
        if (System.getProperty("encryptionutilities.min.iv.size") == null) {
            System.setProperty("encryptionutilities.min.iv.size", String.valueOf(DEFAULT_MIN_IV_SIZE));
        }
        if (System.getProperty("encryptionutilities.max.iv.size") == null) {
            System.setProperty("encryptionutilities.max.iv.size", String.valueOf(DEFAULT_MAX_IV_SIZE));
        }
    }
    
    // Security configuration methods
    
    private static boolean isSecurityEnabled() {
        return Boolean.parseBoolean(System.getProperty("encryptionutilities.security.enabled", "false"));
    }
    
    private static boolean isFileSizeValidationEnabled() {
        return Boolean.parseBoolean(System.getProperty("encryptionutilities.file.size.validation.enabled", "false"));
    }
    
    private static boolean isBufferSizeValidationEnabled() {
        return Boolean.parseBoolean(System.getProperty("encryptionutilities.buffer.size.validation.enabled", "false"));
    }
    
    private static boolean isCryptoParametersValidationEnabled() {
        return Boolean.parseBoolean(System.getProperty("encryptionutilities.crypto.parameters.validation.enabled", "false"));
    }
    
    private static long getMaxFileSize() {
        String maxFileSizeProp = System.getProperty("encryptionutilities.max.file.size");
        if (maxFileSizeProp != null) {
            try {
                return Math.max(1, Long.parseLong(maxFileSizeProp));
            } catch (NumberFormatException e) {
                // Fall through to default
            }
        }
        return isSecurityEnabled() ? DEFAULT_MAX_FILE_SIZE : Long.MAX_VALUE;
    }
    
    private static int getMaxBufferSize() {
        String maxBufferSizeProp = System.getProperty("encryptionutilities.max.buffer.size");
        if (maxBufferSizeProp != null) {
            try {
                return Math.max(1024, Integer.parseInt(maxBufferSizeProp)); // Minimum 1KB
            } catch (NumberFormatException e) {
                // Fall through to default
            }
        }
        return isSecurityEnabled() ? DEFAULT_MAX_BUFFER_SIZE : Integer.MAX_VALUE;
    }
    
    private static int getValidatedPBKDF2Iterations(int requestedIterations) {
        if (!isSecurityEnabled() || !isCryptoParametersValidationEnabled()) {
            return requestedIterations; // Use as-is when security disabled
        }
        
        int minIterations = getMinPBKDF2Iterations();
        int maxIterations = getMaxPBKDF2Iterations();
        
        if (requestedIterations < minIterations) {
            throw new SecurityException("PBKDF2 iteration count too low (min " + minIterations + "): " + requestedIterations);
        }
        if (requestedIterations > maxIterations) {
            throw new SecurityException("PBKDF2 iteration count too high (max " + maxIterations + "): " + requestedIterations);
        }
        
        return requestedIterations;
    }
    
    private static int getMinPBKDF2Iterations() {
        String minIterationsProp = System.getProperty("encryptionutilities.min.pbkdf2.iterations");
        if (minIterationsProp != null) {
            try {
                return Math.max(1000, Integer.parseInt(minIterationsProp)); // Minimum 1000 for security
            } catch (NumberFormatException e) {
                // Fall through to default
            }
        }
        return DEFAULT_MIN_PBKDF2_ITERATIONS;
    }
    
    private static int getMaxPBKDF2Iterations() {
        String maxIterationsProp = System.getProperty("encryptionutilities.max.pbkdf2.iterations");
        if (maxIterationsProp != null) {
            try {
                return Integer.parseInt(maxIterationsProp);
            } catch (NumberFormatException e) {
                // Fall through to default
            }
        }
        return DEFAULT_MAX_PBKDF2_ITERATIONS;
    }
    
    private static int getValidatedBufferSize(int requestedSize) {
        if (!isSecurityEnabled() || !isBufferSizeValidationEnabled()) {
            return requestedSize; // Use as-is when security disabled
        }
        
        int maxBufferSize = getMaxBufferSize();
        if (requestedSize > maxBufferSize) {
            throw new SecurityException("Buffer size too large (max " + maxBufferSize + "): " + requestedSize);
        }
        if (requestedSize < 1024) { // Minimum 1KB
            throw new SecurityException("Buffer size too small (min 1024): " + requestedSize);
        }
        
        return requestedSize;
    }
    
    private static void validateFileSize(File file) {
        if (!isSecurityEnabled() || !isFileSizeValidationEnabled()) {
            return; // Skip validation when security disabled
        }
        
        try {
            long fileSize = file.length();
            long maxFileSize = getMaxFileSize();
            if (fileSize > maxFileSize) {
                throw new SecurityException("File size too large (max " + maxFileSize + " bytes): " + fileSize);
            }
        } catch (SecurityException e) {
            throw e; // Re-throw security exceptions
        } catch (Exception e) {
            // If we can't determine file size, allow it to proceed (backward compatibility)
        }
    }
    
    private static void validateCryptoParameterSize(int size, String paramName, int minSize, int maxSize) {
        if (!isSecurityEnabled() || !isCryptoParametersValidationEnabled()) {
            return; // Skip validation when security disabled
        }
        
        if (size < minSize) {
            throw new SecurityException(paramName + " size too small (min " + minSize + "): " + size);
        }
        if (size > maxSize) {
            throw new SecurityException(paramName + " size too large (max " + maxSize + "): " + size);
        }
    }

    private EncryptionUtilities() {
    }

    /**
     * Calculates an MD5 hash of a file using optimized I/O operations.
     * <p>
     * This implementation uses:
     * <ul>
     *   <li>Heap ByteBuffer for efficient memory use</li>
     *   <li>FileChannel for optimal file access</li>
     *   <li>Fallback for non-standard filesystems</li>
     *   <li>Optional file size validation to prevent resource exhaustion</li>
     * </ul>
     *
     * @param file the file to hash
     * @return hexadecimal string of the MD5 hash, or null if the file cannot be read
     * @throws SecurityException if security validation is enabled and file exceeds size limits
     */
    public static String fastMD5(File file) {
        // Security: Validate file size to prevent resource exhaustion
        validateFileSize(file);
        
        try (InputStream in = Files.newInputStream(file.toPath())) {
            if (in instanceof FileInputStream) {
                return calculateFileHash(((FileInputStream) in).getChannel(), getMD5Digest());
            }
            // Fallback for non-file input streams (rare, but possible with custom filesystem providers)
            return calculateStreamHash(in, getMD5Digest());
        } catch (NoSuchFileException e) {
            return null;
        } catch (IOException e) {
            throw new java.io.UncheckedIOException(e);
        }
    }

    /**
     * Calculates a hash from an InputStream using the specified MessageDigest.
     * <p>
     * This implementation uses:
     * <ul>
     *   <li>64KB buffer optimized for modern storage systems</li>
     *   <li>Matches OS and filesystem page sizes</li>
     *   <li>Aligns with SSD block sizes</li>
     * </ul>
     *
     * @param in     InputStream to read from
     * @param digest MessageDigest to use for hashing
     * @return hexadecimal string of the hash value
     */
    private static String calculateStreamHash(InputStream in, MessageDigest digest) {
        // Buffer size - configurable for security and performance:
        // Default 64KB optimal for:
        // 1. Modern OS page sizes
        // 2. SSD block sizes
        // 3. Filesystem block sizes
        // 4. Memory usage vs. throughput balance
        final int BUFFER_SIZE = getValidatedBufferSize(STANDARD_BUFFER_SIZE);

        byte[] buffer = new byte[BUFFER_SIZE];
        int read;

        try {
            while ((read = in.read(buffer)) != -1) {
                digest.update(buffer, 0, read);
            }
        } catch (IOException e) {
            ExceptionUtilities.uncheckedThrow(e);
        }

        return ByteUtilities.encode(digest.digest());
    }

    /**
     * Calculates a SHA-1 hash of a file using optimized I/O operations.
     * <p>
     * This implementation uses:
     * <ul>
     *   <li>Heap ByteBuffer for efficient memory use</li>
     *   <li>FileChannel for optimal file access</li>
     *   <li>Fallback for non-standard filesystems</li>
     * </ul>
     *
     * @param file the file to hash
     * @return hexadecimal string of the SHA-1 hash, or null if the file cannot be read
     */
    public static String fastSHA1(File file) {
        // Security: Validate file size to prevent resource exhaustion
        validateFileSize(file);
        
        try (InputStream in = Files.newInputStream(file.toPath())) {
            if (in instanceof FileInputStream) {
                return calculateFileHash(((FileInputStream) in).getChannel(), getSHA1Digest());
            }
            // Fallback for non-file input streams (rare, but possible with custom filesystem providers)
            return calculateStreamHash(in, getSHA1Digest());
        } catch (NoSuchFileException e) {
            return null;
        } catch (IOException e) {
            throw new java.io.UncheckedIOException(e);
        }
    }

    /**
     * Calculates a SHA-256 hash of a file using optimized I/O operations.
     * <p>
     * This implementation uses:
     * <ul>
     *   <li>Heap ByteBuffer for efficient memory use</li>
     *   <li>FileChannel for optimal file access</li>
     *   <li>Fallback for non-standard filesystems</li>
     * </ul>
     *
     * @param file the file to hash
     * @return hexadecimal string of the SHA-256 hash, or null if the file cannot be read
     */
    public static String fastSHA256(File file) {
        // Security: Validate file size to prevent resource exhaustion
        validateFileSize(file);
        
        try (InputStream in = Files.newInputStream(file.toPath())) {
            if (in instanceof FileInputStream) {
                return calculateFileHash(((FileInputStream) in).getChannel(), getSHA256Digest());
            }
            // Fallback for non-file input streams (rare, but possible with custom filesystem providers)
            return calculateStreamHash(in, getSHA256Digest());
        } catch (NoSuchFileException e) {
            return null;
        } catch (IOException e) {
            throw new java.io.UncheckedIOException(e);
        }
    }

    /**
     * Calculates a SHA-384 hash of a file using optimized I/O operations.
     *
     * @param file the file to hash
     * @return hexadecimal string of the SHA-384 hash, or null if the file cannot be read
     */
    public static String fastSHA384(File file) {
        // Security: Validate file size to prevent resource exhaustion
        validateFileSize(file);
        
        try (InputStream in = Files.newInputStream(file.toPath())) {
            if (in instanceof FileInputStream) {
                return calculateFileHash(((FileInputStream) in).getChannel(), getSHA384Digest());
            }
            return calculateStreamHash(in, getSHA384Digest());
        } catch (NoSuchFileException e) {
            return null;
        } catch (IOException e) {
            throw new java.io.UncheckedIOException(e);
        }
    }

    /**
     * Calculates a SHA-512 hash of a file using optimized I/O operations.
     * <p>
     * This implementation uses:
     * <ul>
     *   <li>Heap ByteBuffer for efficient memory use</li>
     *   <li>FileChannel for optimal file access</li>
     *   <li>Fallback for non-standard filesystems</li>
     * </ul>
     *
     * @param file the file to hash
     * @return hexadecimal string of the SHA-512 hash, or null if the file cannot be read
     */
    public static String fastSHA512(File file) {
        // Security: Validate file size to prevent resource exhaustion
        validateFileSize(file);
        
        try (InputStream in = Files.newInputStream(file.toPath())) {
            if (in instanceof FileInputStream) {
                return calculateFileHash(((FileInputStream) in).getChannel(), getSHA512Digest());
            }
            // Fallback for non-file input streams (rare, but possible with custom filesystem providers)
            return calculateStreamHash(in, getSHA512Digest());
        } catch (NoSuchFileException e) {
            return null;
        } catch (IOException e) {
            throw new java.io.UncheckedIOException(e);
        }
    }

    /**
     * Calculates a SHA3-256 hash of a file using optimized I/O operations.
     *
     * @param file the file to hash
     * @return hexadecimal string of the SHA3-256 hash, or null if the file cannot be read
     */
    public static String fastSHA3_256(File file) {
        // Security: Validate file size to prevent resource exhaustion
        validateFileSize(file);
        
        try (InputStream in = Files.newInputStream(file.toPath())) {
            if (in instanceof FileInputStream) {
                return calculateFileHash(((FileInputStream) in).getChannel(), getSHA3_256Digest());
            }
            return calculateStreamHash(in, getSHA3_256Digest());
        } catch (NoSuchFileException e) {
            return null;
        } catch (IOException e) {
            throw new java.io.UncheckedIOException(e);
        }
    }

    /**
     * Calculates a SHA3-512 hash of a file using optimized I/O operations.
     *
     * @param file the file to hash
     * @return hexadecimal string of the SHA3-512 hash, or null if the file cannot be read
     */
    public static String fastSHA3_512(File file) {
        // Security: Validate file size to prevent resource exhaustion
        validateFileSize(file);
        
        try (InputStream in = Files.newInputStream(file.toPath())) {
            if (in instanceof FileInputStream) {
                return calculateFileHash(((FileInputStream) in).getChannel(), getSHA3_512Digest());
            }
            return calculateStreamHash(in, getSHA3_512Digest());
        } catch (NoSuchFileException e) {
            return null;
        } catch (IOException e) {
            throw new java.io.UncheckedIOException(e);
        }
    }

    /**
     * Calculates a hash of a file using the provided MessageDigest and FileChannel.
     * <p>
     * This implementation uses:
     * <ul>
     *   <li>64KB buffer size optimized for modern storage systems</li>
     *   <li>Heap ByteBuffer for efficient memory use</li>
     *   <li>Efficient buffer management</li>
     * </ul>
     *
     * @param channel FileChannel to read from
     * @param digest  MessageDigest to use for hashing
     * @return hexadecimal string of the hash value
     * @throws IOException if an I/O error occurs (thrown as unchecked)
     */
    public static String calculateFileHash(FileChannel channel, MessageDigest digest) {
        // Buffer size - configurable for security and performance:
        // Default 64KB optimal for modern OS/disk operations
        // Matches common SSD page sizes and OS buffer sizes  
        final int BUFFER_SIZE = getValidatedBufferSize(STANDARD_BUFFER_SIZE);

        // Heap buffer avoids expensive native allocations
        // Reuse buffer to reduce garbage creation
        ByteBuffer buffer = ByteBuffer.allocate(BUFFER_SIZE);

        // Read until EOF
        try {
            while (channel.read(buffer) != -1) {
                buffer.flip();  // Prepare buffer for reading
                digest.update(buffer);  // Update digest
                buffer.clear();  // Prepare buffer for writing
            }

            return ByteUtilities.encode(digest.digest());
        } catch (IOException e) {
            ExceptionUtilities.uncheckedThrow(e);
            return null; // unreachable
        }
    }

    /**
     * Calculates an MD5 hash of a byte array.
     *
     * @param bytes the data to hash
     * @return hexadecimal string of the MD5 hash, or null if input is null
     */
    public static String calculateMD5Hash(byte[] bytes) {
        return calculateHash(getMD5Digest(), bytes);
    }


    /**
     * Creates a MessageDigest instance for the specified algorithm.
     *
     * @param digest the name of the digest algorithm
     * @return MessageDigest instance for the specified algorithm
     * @throws IllegalArgumentException if the algorithm is not available
     */
    public static MessageDigest getDigest(String digest) {
        try {
            return MessageDigest.getInstance(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalArgumentException(String.format("The requested MessageDigest (%s) does not exist", digest), e);
        }

    }

    /**
     * Creates an MD5 MessageDigest instance.
     *
     * @return MessageDigest configured for MD5
     * @throws IllegalArgumentException if MD5 algorithm is not available
     */
    public static MessageDigest getMD5Digest() {
        return getDigest("MD5");
    }

    /**
     * Calculates a SHA-1 hash of a byte array.
     *
     * @param bytes the data to hash
     * @return hexadecimal string of the SHA-1 hash, or null if input is null
     */
    public static String calculateSHA1Hash(byte[] bytes) {
        return calculateHash(getSHA1Digest(), bytes);
    }

    /**
     * Creates a SHA-1 MessageDigest instance.
     *
     * @return MessageDigest configured for SHA-1
     * @throws IllegalArgumentException if SHA-1 algorithm is not available
     */
    public static MessageDigest getSHA1Digest() {
        return getDigest("SHA-1");
    }

    /**
     * Calculates a SHA-256 hash of a byte array.
     *
     * @param bytes the data to hash
     * @return hexadecimal string of the SHA-256 hash, or null if input is null
     */
    public static String calculateSHA256Hash(byte[] bytes) {
        return calculateHash(getSHA256Digest(), bytes);
    }

    /**
     * Creates a SHA-256 MessageDigest instance.
     *
     * @return MessageDigest configured for SHA-256
     * @throws IllegalArgumentException if SHA-256 algorithm is not available
     */
    public static MessageDigest getSHA256Digest() {
        return getDigest("SHA-256");
    }

    /**
     * Calculates a SHA-384 hash of a byte array.
     *
     * @param bytes the data to hash
     * @return hexadecimal string of the SHA-384 hash, or null if input is null
     */
    public static String calculateSHA384Hash(byte[] bytes) {
        return calculateHash(getSHA384Digest(), bytes);
    }

    /**
     * Creates a SHA-384 MessageDigest instance.
     *
     * @return MessageDigest configured for SHA-384
     * @throws IllegalArgumentException if SHA-384 algorithm is not available
     */
    public static MessageDigest getSHA384Digest() {
        return getDigest("SHA-384");
    }

    /**
     * Calculates a SHA-512 hash of a byte array.
     *
     * @param bytes the data to hash
     * @return hexadecimal string of the SHA-512 hash, or null if input is null
     */
    public static String calculateSHA512Hash(byte[] bytes) {
        return calculateHash(getSHA512Digest(), bytes);
    }

    /**
     * Creates a SHA-512 MessageDigest instance.
     *
     * @return MessageDigest configured for SHA-512
     * @throws IllegalArgumentException if SHA-512 algorithm is not available
     */
    public static MessageDigest getSHA512Digest() {
        return getDigest("SHA-512");
    }

    /**
     * Calculates a SHA3-256 hash of a byte array.
     *
     * @param bytes the data to hash
     * @return hexadecimal string of the SHA3-256 hash, or null if input is null
     */
    public static String calculateSHA3_256Hash(byte[] bytes) {
        return calculateHash(getSHA3_256Digest(), bytes);
    }

    /**
     * Creates a SHA3-256 MessageDigest instance.
     *
     * @return MessageDigest configured for SHA3-256
     * @throws IllegalArgumentException if SHA3-256 algorithm is not available
     */
    public static MessageDigest getSHA3_256Digest() {
        return getDigest("SHA3-256");
    }

    /**
     * Calculates a SHA3-512 hash of a byte array.
     *
     * @param bytes the data to hash
     * @return hexadecimal string of the SHA3-512 hash, or null if input is null
     */
    public static String calculateSHA3_512Hash(byte[] bytes) {
        return calculateHash(getSHA3_512Digest(), bytes);
    }

    /**
     * Creates a SHA3-512 MessageDigest instance.
     *
     * @return MessageDigest configured for SHA3-512
     * @throws IllegalArgumentException if SHA3-512 algorithm is not available
     */
    public static MessageDigest getSHA3_512Digest() {
        return getDigest("SHA3-512");
    }

    /**
     * Derives an AES key from a password and salt using PBKDF2.
     * <p>
     * Security: The iteration count can be validated when security features are enabled
     * to ensure it meets minimum security standards and prevent resource exhaustion attacks.
     * Default iteration count is 65536 when security validation is disabled.
     * </p>
     *
     * @param password   the password
     * @param salt       random salt bytes
     * @param bitsNeeded key length in bits
     * @return derived key bytes
     * @throws SecurityException if security validation is enabled and iteration count is outside acceptable range
     */
    public static byte[] deriveKey(String password, byte[] salt, int bitsNeeded) {
        // Security: Validate iteration count and salt size
        int iterations = getValidatedPBKDF2Iterations(STANDARD_PBKDF2_ITERATIONS);
        validateCryptoParameterSize(salt.length, "Salt", 
            Integer.parseInt(System.getProperty("encryptionutilities.min.salt.size", String.valueOf(DEFAULT_MIN_SALT_SIZE))),
            Integer.parseInt(System.getProperty("encryptionutilities.max.salt.size", String.valueOf(DEFAULT_MAX_SALT_SIZE))));
        
        try {
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            KeySpec spec = new PBEKeySpec(password.toCharArray(), salt, iterations, bitsNeeded);
            return factory.generateSecret(spec).getEncoded();
        } catch (Exception e) {
            throw new IllegalStateException("Unable to derive key", e);
        }
    }

    /**
     * Creates a byte array suitable for use as an AES key from a string password.
     * <p>
     * The key is derived using MD5 and truncated to the specified bit length.
     * This legacy method is retained for backward compatibility.
     *
     * @param key        the password to derive the key from
     * @param bitsNeeded the required key length in bits (typically 128, 192, or 256)
     * @return byte array containing the derived key
     * @deprecated Use {@link #deriveKey(String, byte[], int)} for stronger security
     */
    public static byte[] createCipherBytes(String key, int bitsNeeded) {
        String word = calculateMD5Hash(key.getBytes(StandardCharsets.UTF_8));
        return word.substring(0, bitsNeeded / 8).getBytes(StandardCharsets.UTF_8);
    }

    /**
     * Creates an AES cipher in encryption mode.
     *
     * @param key the encryption key
     * @return Cipher configured for AES encryption
     */
    @Deprecated
    public static Cipher createAesEncryptionCipher(String key) {
        return createAesCipher(key, Cipher.ENCRYPT_MODE);
    }

    /**
     * Creates an AES cipher in decryption mode.
     *
     * @param key the decryption key
     * @return Cipher configured for AES decryption
     */
    @Deprecated
    public static Cipher createAesDecryptionCipher(String key) {
        return createAesCipher(key, Cipher.DECRYPT_MODE);
    }

    /**
     * Creates an AES cipher with the specified mode.
     * <p>
     * Uses CBC mode with PKCS5 padding and IV derived from the key.
     *
     * @param key  the encryption/decryption key
     * @param mode Cipher.ENCRYPT_MODE or Cipher.DECRYPT_MODE
     * @return configured Cipher instance
     */
    @Deprecated
    public static Cipher createAesCipher(String key, int mode) {
        Key sKey = new SecretKeySpec(createCipherBytes(key, 128), "AES");
        return createAesCipher(sKey, mode);
    }

    /**
     * Creates an AES cipher with the specified key and mode.
     * <p>
     * Uses CBC mode with PKCS5 padding and IV derived from the key.
     *
     * @param key  SecretKeySpec for encryption/decryption
     * @param mode Cipher.ENCRYPT_MODE or Cipher.DECRYPT_MODE
     * @return configured Cipher instance
     */
    @Deprecated
    public static Cipher createAesCipher(Key key, int mode)  {
        // Use password key as seed for IV (must be 16 bytes)
        MessageDigest d = getMD5Digest();
        d.update(key.getEncoded());
        byte[] iv = d.digest();

        AlgorithmParameterSpec paramSpec = new IvParameterSpec(iv);
        Cipher cipher = null;        // CBC faster than CFB8/NoPadding (but file length changes)
        
        try {
            cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
            ExceptionUtilities.uncheckedThrow(e);
        }

        try {
            cipher.init(mode, key, paramSpec);
        } catch (InvalidKeyException | InvalidAlgorithmParameterException e) {
            ExceptionUtilities.uncheckedThrow(e);
        }
        return cipher;
    }

    /**
     * Encrypts a string using AES-128.
     *
     * @param key     encryption key
     * @param content string to encrypt
     * @return hexadecimal string of encrypted data
     * @throws IllegalStateException if encryption fails
     */
    public static String encrypt(String key, String content) {
        if (key == null || content == null) {
            throw new IllegalArgumentException("key and content cannot be null");
        }
        try {
            SecureRandom random = new SecureRandom();
            
            // Security: Use configurable salt and IV sizes with validation
            int saltSize = STANDARD_SALT_SIZE;
            int ivSize = STANDARD_IV_SIZE;
            validateCryptoParameterSize(saltSize, "Salt", 
                Integer.parseInt(System.getProperty("encryptionutilities.min.salt.size", String.valueOf(DEFAULT_MIN_SALT_SIZE))),
                Integer.parseInt(System.getProperty("encryptionutilities.max.salt.size", String.valueOf(DEFAULT_MAX_SALT_SIZE))));
            validateCryptoParameterSize(ivSize, "IV", 
                Integer.parseInt(System.getProperty("encryptionutilities.min.iv.size", String.valueOf(DEFAULT_MIN_IV_SIZE))),
                Integer.parseInt(System.getProperty("encryptionutilities.max.iv.size", String.valueOf(DEFAULT_MAX_IV_SIZE))));
            
            byte[] salt = new byte[saltSize];
            random.nextBytes(salt);
            byte[] iv = new byte[ivSize];
            random.nextBytes(iv);

            SecretKeySpec sKey = new SecretKeySpec(deriveKey(key, salt, 128), "AES");
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, sKey, new GCMParameterSpec(128, iv));

            byte[] encrypted = cipher.doFinal(content.getBytes(StandardCharsets.UTF_8));

            byte[] out = new byte[1 + salt.length + iv.length + encrypted.length];
            out[0] = 1; // version
            System.arraycopy(salt, 0, out, 1, salt.length);
            System.arraycopy(iv, 0, out, 1 + salt.length, iv.length);
            System.arraycopy(encrypted, 0, out, 1 + salt.length + iv.length, encrypted.length);
            return ByteUtilities.encode(out);
        } catch (Exception e) {
            throw new IllegalStateException("Error occurred encrypting data", e);
        }
    }

    /**
     * Encrypts a byte array using AES-128.
     *
     * @param key     encryption key
     * @param content bytes to encrypt
     * @return hexadecimal string of encrypted data
     * @throws IllegalStateException if encryption fails
     */
    public static String encryptBytes(String key, byte[] content) {
        if (key == null || content == null) {
            throw new IllegalArgumentException("key and content cannot be null");
        }
        try {
            SecureRandom random = new SecureRandom();
            
            // Security: Use configurable salt and IV sizes with validation
            int saltSize = STANDARD_SALT_SIZE;
            int ivSize = STANDARD_IV_SIZE;
            validateCryptoParameterSize(saltSize, "Salt", 
                Integer.parseInt(System.getProperty("encryptionutilities.min.salt.size", String.valueOf(DEFAULT_MIN_SALT_SIZE))),
                Integer.parseInt(System.getProperty("encryptionutilities.max.salt.size", String.valueOf(DEFAULT_MAX_SALT_SIZE))));
            validateCryptoParameterSize(ivSize, "IV", 
                Integer.parseInt(System.getProperty("encryptionutilities.min.iv.size", String.valueOf(DEFAULT_MIN_IV_SIZE))),
                Integer.parseInt(System.getProperty("encryptionutilities.max.iv.size", String.valueOf(DEFAULT_MAX_IV_SIZE))));
            
            byte[] salt = new byte[saltSize];
            random.nextBytes(salt);
            byte[] iv = new byte[ivSize];
            random.nextBytes(iv);

            SecretKeySpec sKey = new SecretKeySpec(deriveKey(key, salt, 128), "AES");
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, sKey, new GCMParameterSpec(128, iv));
            byte[] encrypted = cipher.doFinal(content);

            byte[] out = new byte[1 + salt.length + iv.length + encrypted.length];
            out[0] = 1;
            System.arraycopy(salt, 0, out, 1, salt.length);
            System.arraycopy(iv, 0, out, 1 + salt.length, iv.length);
            System.arraycopy(encrypted, 0, out, 1 + salt.length + iv.length, encrypted.length);
            return ByteUtilities.encode(out);
        } catch (Exception e) {
            throw new IllegalStateException("Error occurred encrypting data", e);
        }
    }

    /**
     * Decrypts a hexadecimal string of encrypted data to its original string form.
     *
     * @param key    decryption key
     * @param hexStr hexadecimal string of encrypted data
     * @return decrypted string
     * @throws IllegalStateException if decryption fails
     */
    public static String decrypt(String key, String hexStr) {
        if (key == null || hexStr == null) {
            throw new IllegalArgumentException("key and hexStr cannot be null");
        }
        byte[] data = ByteUtilities.decode(hexStr);
        if (data == null || data.length == 0) {
            throw new IllegalArgumentException("Invalid hexadecimal input");
        }
        try {
            if (data[0] == 1 && data.length > 29) {
                byte[] salt = Arrays.copyOfRange(data, 1, 17);
                byte[] iv = Arrays.copyOfRange(data, 17, 29);
                byte[] cipherText = Arrays.copyOfRange(data, 29, data.length);

                SecretKeySpec sKey = new SecretKeySpec(deriveKey(key, salt, 128), "AES");
                Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
                cipher.init(Cipher.DECRYPT_MODE, sKey, new GCMParameterSpec(128, iv));
                return new String(cipher.doFinal(cipherText), StandardCharsets.UTF_8);
            }
            return new String(createAesDecryptionCipher(key).doFinal(data), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("Error occurred decrypting data", e);
        }
    }

    /**
     * Decrypts a hexadecimal string of encrypted data to its original byte array form.
     *
     * @param key    decryption key
     * @param hexStr hexadecimal string of encrypted data
     * @return decrypted byte array
     * @throws IllegalStateException if decryption fails
     */
    public static byte[] decryptBytes(String key, String hexStr) {
        if (key == null || hexStr == null) {
            throw new IllegalArgumentException("key and hexStr cannot be null");
        }
        byte[] data = ByteUtilities.decode(hexStr);
        if (data == null || data.length == 0) {
            throw new IllegalArgumentException("Invalid hexadecimal input");
        }
        try {
            if (data[0] == 1 && data.length > 29) {
                byte[] salt = Arrays.copyOfRange(data, 1, 17);
                byte[] iv = Arrays.copyOfRange(data, 17, 29);
                byte[] cipherText = Arrays.copyOfRange(data, 29, data.length);

                SecretKeySpec sKey = new SecretKeySpec(deriveKey(key, salt, 128), "AES");
                Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
                cipher.init(Cipher.DECRYPT_MODE, sKey, new GCMParameterSpec(128, iv));
                return cipher.doFinal(cipherText);
            }
            return createAesDecryptionCipher(key).doFinal(data);
        } catch (Exception e) {
            throw new IllegalStateException("Error occurred decrypting data", e);
        }
    }

    /**
     * Calculates a hash of a byte array using the specified MessageDigest.
     *
     * @param d     MessageDigest to use
     * @param bytes data to hash
     * @return hexadecimal string of the hash value, or null if input is null
     */
    public static String calculateHash(MessageDigest d, byte[] bytes) {
        if (bytes == null) {
            return null;
        }

        d.update(bytes);
        return ByteUtilities.encode(d.digest());
    }

    /**
     * Applies MurmurHash3 finalization to improve the distribution of hash values.
     * <p>
     * This function implements the finalization step of the MurmurHash3 algorithm, which applies
     * a series of bit-mixing operations to eliminate poor distribution in the lower bits of hash values.
     * It is particularly useful for improving the quality of hashCode() implementations by reducing
     * hash collisions and improving distribution across hash table buckets.
     * </p>
     * <p>
     * <strong>Note:</strong> This is only the finalization step of MurmurHash3, not the complete
     * MurmurHash3 algorithm. It takes an existing hash value and improves its bit distribution.
     * </p>
     * <p>
     * <strong>Usage:</strong> Apply this to the result of your hashCode() computation:
     * <pre>
     * public int hashCode() {
     *     int result = Objects.hash(field1, field2, field3);
     *     return EncryptionUtilities.murmurHash3(result);
     * }
     * </pre>
     * </p>
     * <p>
     * The finalization step performs the following operations:
     * <ol>
     * <li>XOR with right-shifted bits (eliminates poor distribution)</li>
     * <li>Multiply by a carefully chosen constant</li>
     * <li>Repeat the process to maximize avalanche effect</li>
     * </ol>
     * </p>
     *
     * @param hash the input hash value to be finalized
     * @return the finalized hash value with improved bit distribution
     * @see <a href="https://github.com/aappleby/smhasher/blob/master/src/MurmurHash3.cpp">MurmurHash3 Reference Implementation</a>
     */
    public static int finalizeHash(int hash) {
        // MurmurHash3 finalization
        hash ^= (hash >>> 16);
        hash *= 0x85ebca6b;
        hash ^= (hash >>> 13);
        hash *= 0xc2b2ae35;
        hash ^= (hash >>> 16);
        return hash;
    }
}
