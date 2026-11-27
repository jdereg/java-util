package com.cedarsoftware.util;

import javax.crypto.Cipher;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.function.Supplier;

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
 *   <li><code>encryptionutilities.legacy.decrypt.enabled=true</code> &mdash; Whether {@code decrypt()} also reads
 *       the unauthenticated AES-CBC format of java-util 3.3.2 and earlier. Not part of the master switch: it is
 *       on unless set to false. Set it to false where a ciphertext can come from someone else (see
 *       {@link #decrypt(String, String)})</li>
 * </ul>
 *
 * <h3>Security Features</h3>
 * <ul>
 *   <li><b>File Size Validation:</b> Bounds the time spent hashing an oversized file (hashing reads through a
 *       64 KB buffer, so memory is not at stake). The limit is checked against the reported length and again as the
 *       file is read, so a device, a FIFO or a growing file is held to it too</li>
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
 *   <li>AES implementation uses GCM mode with authentication; {@code decrypt()} also reads the unauthenticated
 *       format of java-util 3.3.2 and earlier unless {@code encryptionutilities.legacy.decrypt.enabled=false}</li>
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
    private static final String AES_GCM_ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_TAG_BIT_LENGTH = 128;
    private static final int GCM_TAG_BYTE_LENGTH = GCM_TAG_BIT_LENGTH / 8;
    private static final byte VERSION_1 = 1;
    private static final int VERSION_OFFSET = 0;
    private static final int SALT_OFFSET = VERSION_OFFSET + 1;
    private static final int IV_OFFSET = SALT_OFFSET + STANDARD_SALT_SIZE;
    private static final int CIPHER_TEXT_OFFSET = IV_OFFSET + STANDARD_IV_SIZE;
    private static final int MIN_VERSIONED_PAYLOAD_SIZE = CIPHER_TEXT_OFFSET + GCM_TAG_BYTE_LENGTH;
    private static final int LEGACY_AES_BLOCK_SIZE = 16;

    // Cached SecureRandom instance (thread-safe)
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    // Cached SecretKeyFactory instance (thread-safe)
    private static final SecretKeyFactory PBKDF2_FACTORY;

    static {
        try {
            PBKDF2_FACTORY = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("PBKDF2WithHmacSHA256 algorithm not available", e);
        }
    }

    /**
     * An untouched digest of one algorithm, cloned for each caller. MessageDigest.getInstance() searches the installed
     * security providers on every call, which costs more than hashing a short input. The prototype is never updated,
     * so cloning it from many threads at once is safe. As with {@link #PBKDF2_FACTORY}, the provider is the one
     * getInstance() chose the first time the algorithm was asked for. A digest its provider cannot clone is made by
     * getInstance() each time, as before.
     */
    private static final class DigestPrototype {
        private final String algorithm;
        private volatile MessageDigest prototype;

        DigestPrototype(String algorithm) {
            this.algorithm = algorithm;
        }

        MessageDigest newDigest() {
            MessageDigest p = prototype;
            if (p == null) {
                p = getDigest(algorithm);       // IllegalArgumentException if the algorithm is not available
                prototype = p;
            }
            try {
                return (MessageDigest) p.clone();
            } catch (CloneNotSupportedException e) {
                return getDigest(algorithm);
            }
        }
    }

    private static final DigestPrototype MD5_PROTOTYPE = new DigestPrototype("MD5");
    private static final DigestPrototype SHA1_PROTOTYPE = new DigestPrototype("SHA-1");
    private static final DigestPrototype SHA256_PROTOTYPE = new DigestPrototype("SHA-256");
    private static final DigestPrototype SHA384_PROTOTYPE = new DigestPrototype("SHA-384");
    private static final DigestPrototype SHA512_PROTOTYPE = new DigestPrototype("SHA-512");
    private static final DigestPrototype SHA3_256_PROTOTYPE = new DigestPrototype("SHA3-256");
    private static final DigestPrototype SHA3_512_PROTOTYPE = new DigestPrototype("SHA3-512");

    // Security configuration methods
    
    private static final String LEGACY_DECRYPT_PROPERTY = "encryptionutilities.legacy.decrypt.enabled";

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

    private static int getMinSaltSize() {
        String minSaltSizeProp = System.getProperty("encryptionutilities.min.salt.size");
        if (minSaltSizeProp != null) {
            try {
                return Math.max(1, Integer.parseInt(minSaltSizeProp));
            } catch (NumberFormatException e) {
                // Fall through to default
            }
        }
        return DEFAULT_MIN_SALT_SIZE;
    }

    private static int getMaxSaltSize() {
        String maxSaltSizeProp = System.getProperty("encryptionutilities.max.salt.size");
        if (maxSaltSizeProp != null) {
            try {
                return Integer.parseInt(maxSaltSizeProp);
            } catch (NumberFormatException e) {
                // Fall through to default
            }
        }
        return DEFAULT_MAX_SALT_SIZE;
    }

    private static int getMinIvSize() {
        String minIvSizeProp = System.getProperty("encryptionutilities.min.iv.size");
        if (minIvSizeProp != null) {
            try {
                return Math.max(1, Integer.parseInt(minIvSizeProp));
            } catch (NumberFormatException e) {
                // Fall through to default
            }
        }
        return DEFAULT_MIN_IV_SIZE;
    }

    private static int getMaxIvSize() {
        String maxIvSizeProp = System.getProperty("encryptionutilities.max.iv.size");
        if (maxIvSizeProp != null) {
            try {
                return Integer.parseInt(maxIvSizeProp);
            } catch (NumberFormatException e) {
                // Fall through to default
            }
        }
        return DEFAULT_MAX_IV_SIZE;
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
     *   <li>Optional file size validation to prevent resource exhaustion</li>
     * </ul>
     *
     * @param file the file to hash
     * @return hexadecimal string of the MD5 hash, or null if the file cannot be read
     * @throws SecurityException if security validation is enabled and file exceeds size limits
     */
    public static String fastMD5(File file) {
        return hashFile(file, EncryptionUtilities::getMD5Digest);
    }

    /**
     * The body of every {@code fast*} hash: the file read through its channel into the digest {@code digest}
     * supplies, held to the file-size limit when that validation is on. The limit is checked against the length the
     * file system reports, before the file is opened, and again as the bytes are read: a device or a FIFO reports
     * length 0 and then streams without end, and a file can grow after the check.
     *
     * @return the hash in hexadecimal, or null if the file cannot be opened
     */
    private static String hashFile(File file, Supplier<MessageDigest> digest) {
        validateFileSize(file);
        long limit = isSecurityEnabled() && isFileSizeValidationEnabled() ? getMaxFileSize() : Long.MAX_VALUE;

        try (FileInputStream fis = new FileInputStream(file)) {
            return calculateFileHash(fis.getChannel(), digest.get(), limit);
        } catch (FileNotFoundException e) {
            return null;
        } catch (IOException e) {
            ExceptionUtilities.uncheckedThrow(e);
            return null; // unreachable
        }
    }

    /**
     * Calculates a SHA-1 hash of a file using optimized I/O operations.
     * <p>
     * This implementation uses:
     * <ul>
     *   <li>Heap ByteBuffer for efficient memory use</li>
     *   <li>FileChannel for optimal file access</li>
     * </ul>
     *
     * @param file the file to hash
     * @return hexadecimal string of the SHA-1 hash, or null if the file cannot be read
     */
    public static String fastSHA1(File file) {
        return hashFile(file, EncryptionUtilities::getSHA1Digest);
    }

    /**
     * Calculates a SHA-256 hash of a file using optimized I/O operations.
     * <p>
     * This implementation uses:
     * <ul>
     *   <li>Heap ByteBuffer for efficient memory use</li>
     *   <li>FileChannel for optimal file access</li>
     * </ul>
     *
     * @param file the file to hash
     * @return hexadecimal string of the SHA-256 hash, or null if the file cannot be read
     */
    public static String fastSHA256(File file) {
        return hashFile(file, EncryptionUtilities::getSHA256Digest);
    }

    /**
     * Calculates a SHA-384 hash of a file using optimized I/O operations.
     *
     * @param file the file to hash
     * @return hexadecimal string of the SHA-384 hash, or null if the file cannot be read
     */
    public static String fastSHA384(File file) {
        return hashFile(file, EncryptionUtilities::getSHA384Digest);
    }

    /**
     * Calculates a SHA-512 hash of a file using optimized I/O operations.
     * <p>
     * This implementation uses:
     * <ul>
     *   <li>Heap ByteBuffer for efficient memory use</li>
     *   <li>FileChannel for optimal file access</li>
     * </ul>
     *
     * @param file the file to hash
     * @return hexadecimal string of the SHA-512 hash, or null if the file cannot be read
     */
    public static String fastSHA512(File file) {
        return hashFile(file, EncryptionUtilities::getSHA512Digest);
    }

    /**
     * Calculates a SHA3-256 hash of a file using optimized I/O operations.
     *
     * @param file the file to hash
     * @return hexadecimal string of the SHA3-256 hash, or null if the file cannot be read
     */
    public static String fastSHA3_256(File file) {
        return hashFile(file, EncryptionUtilities::getSHA3_256Digest);
    }

    /**
     * Calculates a SHA3-512 hash of a file using optimized I/O operations.
     *
     * @param file the file to hash
     * @return hexadecimal string of the SHA3-512 hash, or null if the file cannot be read
     */
    public static String fastSHA3_512(File file) {
        return hashFile(file, EncryptionUtilities::getSHA3_512Digest);
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
        return calculateFileHash(channel, digest, Long.MAX_VALUE);
    }

    /** As {@link #calculateFileHash(FileChannel, MessageDigest)}, refusing to read more than {@code limit} bytes. */
    private static String calculateFileHash(FileChannel channel, MessageDigest digest, long limit) {
        // Buffer size - configurable for security and performance:
        // Default 64KB optimal for modern OS/disk operations
        // Matches common SSD page sizes and OS buffer sizes  
        final int BUFFER_SIZE = getValidatedBufferSize(STANDARD_BUFFER_SIZE);

        // Heap buffer avoids expensive native allocations
        // Reuse buffer to reduce garbage creation
        ByteBuffer buffer = ByteBuffer.allocate(BUFFER_SIZE);

        // Read until EOF
        try {
            long total = 0;
            int read;
            while ((read = channel.read(buffer)) != -1) {
                if ((total += read) > limit) {
                    throw new SecurityException("File size too large (max " + limit + " bytes): more was read");
                }
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
     * Each call returns a new instance of its own (a copy of a cached, untouched one).
     *
     * @return MessageDigest configured for MD5
     * @throws IllegalArgumentException if MD5 algorithm is not available
     */
    public static MessageDigest getMD5Digest() {
        return MD5_PROTOTYPE.newDigest();
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
     * Each call returns a new instance of its own (a copy of a cached, untouched one).
     *
     * @return MessageDigest configured for SHA-1
     * @throws IllegalArgumentException if SHA-1 algorithm is not available
     */
    public static MessageDigest getSHA1Digest() {
        return SHA1_PROTOTYPE.newDigest();
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
     * Each call returns a new instance of its own (a copy of a cached, untouched one).
     *
     * @return MessageDigest configured for SHA-256
     * @throws IllegalArgumentException if SHA-256 algorithm is not available
     */
    public static MessageDigest getSHA256Digest() {
        return SHA256_PROTOTYPE.newDigest();
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
     * Each call returns a new instance of its own (a copy of a cached, untouched one).
     *
     * @return MessageDigest configured for SHA-384
     * @throws IllegalArgumentException if SHA-384 algorithm is not available
     */
    public static MessageDigest getSHA384Digest() {
        return SHA384_PROTOTYPE.newDigest();
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
     * Each call returns a new instance of its own (a copy of a cached, untouched one).
     *
     * @return MessageDigest configured for SHA-512
     * @throws IllegalArgumentException if SHA-512 algorithm is not available
     */
    public static MessageDigest getSHA512Digest() {
        return SHA512_PROTOTYPE.newDigest();
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
     * Each call returns a new instance of its own (a copy of a cached, untouched one).
     *
     * @return MessageDigest configured for SHA3-256
     * @throws IllegalArgumentException if SHA3-256 algorithm is not available
     */
    public static MessageDigest getSHA3_256Digest() {
        return SHA3_256_PROTOTYPE.newDigest();
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
     * Each call returns a new instance of its own (a copy of a cached, untouched one).
     *
     * @return MessageDigest configured for SHA3-512
     * @throws IllegalArgumentException if SHA3-512 algorithm is not available
     */
    public static MessageDigest getSHA3_512Digest() {
        return SHA3_512_PROTOTYPE.newDigest();
    }

    /**
     * Derives an AES key from a password and salt using PBKDF2.
     * <p>
     * Security: The iteration count can be validated when security features are enabled
     * to ensure it meets minimum security standards and prevent resource exhaustion attacks.
     * The iteration count is 65,536, fixed by the ciphertext format. The iteration limits of the security
     * configuration can refuse that count; they cannot change it.
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
        validateCryptoParameterSize(salt.length, "Salt", getMinSaltSize(), getMaxSaltSize());

        PBEKeySpec spec = null;
        try {
            spec = new PBEKeySpec(password.toCharArray(), salt, iterations, bitsNeeded);
            return PBKDF2_FACTORY.generateSecret(spec).getEncoded();
        } catch (Exception e) {
            throw new IllegalStateException("Unable to derive key", e);
        } finally {
            // Clear password from memory for security
            if (spec != null) {
                spec.clearPassword();
            }
        }
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
            // Security: Use configurable salt and IV sizes with validation
            int saltSize = STANDARD_SALT_SIZE;
            int ivSize = STANDARD_IV_SIZE;
            validateCryptoParameterSize(saltSize, "Salt", getMinSaltSize(), getMaxSaltSize());
            validateCryptoParameterSize(ivSize, "IV", getMinIvSize(), getMaxIvSize());

            byte[] salt = new byte[saltSize];
            SECURE_RANDOM.nextBytes(salt);
            byte[] iv = new byte[ivSize];
            SECURE_RANDOM.nextBytes(iv);

            SecretKeySpec sKey = new SecretKeySpec(deriveKey(key, salt, 128), "AES");
            Cipher cipher = Cipher.getInstance(AES_GCM_ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, sKey, new GCMParameterSpec(GCM_TAG_BIT_LENGTH, iv));

            byte[] encrypted = cipher.doFinal(content.getBytes(StandardCharsets.UTF_8));

            // Use ByteBuffer for cleaner assembly of output
            ByteBuffer buffer = ByteBuffer.allocate(1 + salt.length + iv.length + encrypted.length);
            buffer.put(VERSION_1);  // version
            buffer.put(salt);
            buffer.put(iv);
            buffer.put(encrypted);
            return ByteUtilities.encode(buffer.array());
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
            // Security: Use configurable salt and IV sizes with validation
            int saltSize = STANDARD_SALT_SIZE;
            int ivSize = STANDARD_IV_SIZE;
            validateCryptoParameterSize(saltSize, "Salt", getMinSaltSize(), getMaxSaltSize());
            validateCryptoParameterSize(ivSize, "IV", getMinIvSize(), getMaxIvSize());

            byte[] salt = new byte[saltSize];
            SECURE_RANDOM.nextBytes(salt);
            byte[] iv = new byte[ivSize];
            SECURE_RANDOM.nextBytes(iv);

            SecretKeySpec sKey = new SecretKeySpec(deriveKey(key, salt, 128), "AES");
            Cipher cipher = Cipher.getInstance(AES_GCM_ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, sKey, new GCMParameterSpec(GCM_TAG_BIT_LENGTH, iv));
            byte[] encrypted = cipher.doFinal(content);

            // Use ByteBuffer for cleaner assembly of output
            ByteBuffer buffer = ByteBuffer.allocate(1 + salt.length + iv.length + encrypted.length);
            buffer.put(VERSION_1);  // version
            buffer.put(salt);
            buffer.put(iv);
            buffer.put(encrypted);
            return ByteUtilities.encode(buffer.array());
        } catch (Exception e) {
            throw new IllegalStateException("Error occurred encrypting data", e);
        }
    }

    private static boolean isVersion1Payload(byte[] data) {
        return data.length >= MIN_VERSIONED_PAYLOAD_SIZE && data[VERSION_OFFSET] == VERSION_1;
    }

    private static boolean isLegacyCiphertextCandidate(byte[] data) {
        return data.length > 0 && data.length % LEGACY_AES_BLOCK_SIZE == 0;
    }

    /**
     * Reads the format java-util 3.3.2 and earlier wrote, byte for byte as it was written: AES/CBC/PKCS5Padding with
     * no authentication; the key is the first 16 hex digits of the unsalted MD5 of the password (64 bits), and the
     * IV is the MD5 of those key bytes, the same for every message under one password.
     */
    private static byte[] decryptLegacyPayload(String key, byte[] data) throws Exception {
        byte[] keyBytes = calculateMD5Hash(key.getBytes(StandardCharsets.UTF_8)).substring(0, 16)
                .getBytes(StandardCharsets.UTF_8);
        MessageDigest md5 = getMD5Digest();
        md5.update(keyBytes);
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(keyBytes, "AES"), new IvParameterSpec(md5.digest()));
        return cipher.doFinal(data);
    }

    private static byte[] decryptVersion1Payload(String key, byte[] data) throws Exception {
        byte[] salt = new byte[STANDARD_SALT_SIZE];
        System.arraycopy(data, SALT_OFFSET, salt, 0, STANDARD_SALT_SIZE);

        SecretKeySpec sKey = new SecretKeySpec(deriveKey(key, salt, 128), "AES");
        Cipher cipher = Cipher.getInstance(AES_GCM_ALGORITHM);
        GCMParameterSpec parameterSpec =
                new GCMParameterSpec(GCM_TAG_BIT_LENGTH, data, IV_OFFSET, STANDARD_IV_SIZE);
        cipher.init(Cipher.DECRYPT_MODE, sKey, parameterSpec);
        return cipher.doFinal(data, CIPHER_TEXT_OFFSET, data.length - CIPHER_TEXT_OFFSET);
    }

    /**
     * Whether decrypt() also reads the format of java-util 3.3.2 and earlier, AES-CBC with no authentication.
     * Yes unless the application turns it off: {@code encryptionutilities.legacy.decrypt.enabled=false}. Read on
     * each call. Any value other than "false" (ignoring case) leaves it on.
     */
    private static boolean isLegacyDecryptEnabled() {
        return !"false".equalsIgnoreCase(System.getProperty(LEGACY_DECRYPT_PROPERTY, "true").trim());
    }

    /**
     * With the old format turned off, an input that is not what encrypt() writes is refused here, before any cipher
     * sees it and with one answer for every such input. Decrypting it as the old format answers "decrypted" or "bad
     * padding", and that answer, asked a few thousand times, is enough to build a ciphertext the application
     * accepts, without the password.
     */
    private static void requireReadableFormat(byte[] data) {
        if (!isVersion1Payload(data) && !isLegacyDecryptEnabled()) {
            throw new IllegalStateException("Not data written by encrypt(): it is not an AES-GCM payload. Data "
                    + "written by java-util 3.3.2 or earlier (AES-CBC, not authenticated) is not read while "
                    + LEGACY_DECRYPT_PROPERTY + "=false");
        }
    }

    private static byte[] decryptToBytes(String key, byte[] data) throws Exception {
        if (!isLegacyDecryptEnabled()) {
            // Turned off: what encrypt() writes, and nothing else. A failed tag check is a failure: it is not retried
            // as the old format, where a chosen change to the ciphertext passes the padding check and returns noise.
            return decryptVersion1Payload(key, data);
        }
        // The default: AES-GCM first, then the old format as a second try, as through 4.118.0. Old data whose first
        // byte happens to be the version byte fails the tag check and is read by the second try.
        if (isVersion1Payload(data)) {
            try {
                return decryptVersion1Payload(key, data);
            } catch (Exception versionedError) {
                if (!isLegacyCiphertextCandidate(data)) {
                    throw versionedError;
                }
                try {
                    return decryptLegacyPayload(key, data);
                } catch (Exception legacyError) {
                    legacyError.addSuppressed(versionedError);
                    throw legacyError;
                }
            }
        }
        return decryptLegacyPayload(key, data);
    }

    /**
     * Decrypts a hexadecimal string of encrypted data to its original string form.
     * <p>
     * It reads what {@link #encrypt(String, String)} writes, AES-GCM, which detects a changed ciphertext or a wrong
     * password and fails. If that fails, it tries the format of java-util 3.3.2 and earlier as a second try.
     * </p>
     * <p>
     * <b>Data written by java-util 3.3.2 or earlier</b> is in another format: AES/CBC/PKCS5Padding with no
     * authentication, a 64-bit key from an unsalted MD5 of the password, and one IV for every message under a
     * password. It is read by default, so data kept from those versions keeps working. Set the system property
     * {@code encryptionutilities.legacy.decrypt.enabled=false} to read AES-GCM only. Do that where a ciphertext can
     * come from someone else: with the old format on, a party who can submit inputs and see whether each one fails
     * can build a ciphertext this method accepts, without the password, and a changed ciphertext (or about 1 wrong
     * password in 256) can come back as noise instead of failing. To leave the old format behind, decrypt each kept
     * value and save it again with {@link #encrypt(String, String)}, then turn it off.
     * </p>
     *
     * @param key    decryption key
     * @param hexStr hexadecimal string of encrypted data
     * @return decrypted string
     * @throws IllegalStateException if decryption fails, or the input is not an AES-GCM payload and the old
     *                               format is turned off
     */
    public static String decrypt(String key, String hexStr) {
        if (key == null || hexStr == null) {
            throw new IllegalArgumentException("key and hexStr cannot be null");
        }
        byte[] data = ByteUtilities.decode(hexStr);
        if (data == null || data.length == 0) {
            throw new IllegalArgumentException("Invalid hexadecimal input");
        }
        requireReadableFormat(data);
        try {
            return new String(decryptToBytes(key, data), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("Error occurred decrypting data", e);
        }
    }

    /**
     * Decrypts a hexadecimal string of encrypted data to its original byte array form. As
     * {@link #decrypt(String, String)}: what {@link #encryptBytes(String, byte[])} writes, then the format of
     * java-util 3.3.2 and earlier as a second try, unless {@code encryptionutilities.legacy.decrypt.enabled=false}.
     *
     * @param key    decryption key
     * @param hexStr hexadecimal string of encrypted data
     * @return decrypted byte array
     * @throws IllegalStateException if decryption fails, or the input is not an AES-GCM payload and the old
     *                               format is turned off
     */
    public static byte[] decryptBytes(String key, String hexStr) {
        if (key == null || hexStr == null) {
            throw new IllegalArgumentException("key and hexStr cannot be null");
        }
        byte[] data = ByteUtilities.decode(hexStr);
        if (data == null || data.length == 0) {
            throw new IllegalArgumentException("Invalid hexadecimal input");
        }
        requireReadableFormat(data);
        try {
            return decryptToBytes(key, data);
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
     *     return EncryptionUtilities.finalizeHash(result);
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
