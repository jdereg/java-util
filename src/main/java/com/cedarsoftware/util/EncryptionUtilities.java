package com.cedarsoftware.util;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
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
 *       <li>SHA-512</li>
 *     </ul>
 *   </li>
 *   <li><b>Encryption/Decryption:</b>
 *     <ul>
 *       <li>AES-128 encryption</li>
 *       <li>CBC mode with PKCS5 padding</li>
 *       <li>IV generation from key</li>
 *     </ul>
 *   </li>
 *   <li><b>Optimized File Operations:</b>
 *     <ul>
 *       <li>Zero-copy I/O using DirectByteBuffer</li>
 *       <li>Efficient large file handling</li>
 *       <li>Custom filesystem support</li>
 *     </ul>
 *   </li>
 * </ul>
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
 *   <li>AES implementation uses CBC mode with PKCS5 padding</li>
 *   <li>IV is deterministically generated from the key using MD5</li>
 * </ul>
 *
 * <p><b>Performance Features:</b></p>
 * <ul>
 *   <li>Optimized buffer sizes for modern storage systems</li>
 *   <li>Direct ByteBuffer usage for zero-copy I/O</li>
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
    private EncryptionUtilities() {
    }

    /**
     * Calculates an MD5 hash of a file using optimized I/O operations.
     * <p>
     * This implementation uses:
     * <ul>
     *   <li>DirectByteBuffer for zero-copy I/O</li>
     *   <li>FileChannel for optimal file access</li>
     *   <li>Fallback for non-standard filesystems</li>
     * </ul>
     *
     * @param file the file to hash
     * @return hexadecimal string of the MD5 hash, or null if the file cannot be read
     */
    public static String fastMD5(File file) {
        try (InputStream in = Files.newInputStream(file.toPath())) {
            if (in instanceof FileInputStream) {
                return calculateFileHash(((FileInputStream) in).getChannel(), getMD5Digest());
            }
            // Fallback for non-file input streams (rare, but possible with custom filesystem providers)
            return calculateStreamHash(in, getMD5Digest());
        } catch (NoSuchFileException e) {
            return null;
        } catch (IOException e) {
            return null;
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
     * @param in InputStream to read from
     * @param digest MessageDigest to use for hashing
     * @return hexadecimal string of the hash value
     * @throws IOException if an I/O error occurs
     */
    private static String calculateStreamHash(InputStream in, MessageDigest digest) throws IOException {
        // 64KB buffer size - optimal for:
        // 1. Modern OS page sizes
        // 2. SSD block sizes
        // 3. Filesystem block sizes
        // 4. Memory usage vs. throughput balance
        final int BUFFER_SIZE = 64 * 1024;

        byte[] buffer = new byte[BUFFER_SIZE];
        int read;

        while ((read = in.read(buffer)) != -1) {
            digest.update(buffer, 0, read);
        }

        return ByteUtilities.encode(digest.digest());
    }

    /**
     * Calculates a SHA-256 hash of a file using optimized I/O operations.
     * <p>
     * This implementation uses:
     * <ul>
     *   <li>DirectByteBuffer for zero-copy I/O</li>
     *   <li>FileChannel for optimal file access</li>
     *   <li>Fallback for non-standard filesystems</li>
     * </ul>
     *
     * @param file the file to hash
     * @return hexadecimal string of the SHA-256 hash, or null if the file cannot be read
     */
    public static String fastSHA1(File file) {
        try (InputStream in = Files.newInputStream(file.toPath())) {
            if (in instanceof FileInputStream) {
                return calculateFileHash(((FileInputStream) in).getChannel(), getSHA1Digest());
            }
            // Fallback for non-file input streams (rare, but possible with custom filesystem providers)
            return calculateStreamHash(in, getSHA1Digest());
        } catch (NoSuchFileException e) {
            return null;
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * Calculates a SHA-256 hash of a file using optimized I/O operations.
     * <p>
     * This implementation uses:
     * <ul>
     *   <li>DirectByteBuffer for zero-copy I/O</li>
     *   <li>FileChannel for optimal file access</li>
     *   <li>Fallback for non-standard filesystems</li>
     * </ul>
     *
     * @param file the file to hash
     * @return hexadecimal string of the SHA-256 hash, or null if the file cannot be read
     */
    public static String fastSHA256(File file) {
        try (InputStream in = Files.newInputStream(file.toPath())) {
            if (in instanceof FileInputStream) {
                return calculateFileHash(((FileInputStream) in).getChannel(), getSHA256Digest());
            }
            // Fallback for non-file input streams (rare, but possible with custom filesystem providers)
            return calculateStreamHash(in, getSHA256Digest());
        } catch (NoSuchFileException e) {
            return null;
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * Calculates a SHA-512 hash of a file using optimized I/O operations.
     * <p>
     * This implementation uses:
     * <ul>
     *   <li>DirectByteBuffer for zero-copy I/O</li>
     *   <li>FileChannel for optimal file access</li>
     *   <li>Fallback for non-standard filesystems</li>
     * </ul>
     *
     * @param file the file to hash
     * @return hexadecimal string of the SHA-512 hash, or null if the file cannot be read
     */
    public static String fastSHA512(File file) {
        try (InputStream in = Files.newInputStream(file.toPath())) {
            if (in instanceof FileInputStream) {
                return calculateFileHash(((FileInputStream) in).getChannel(), getSHA512Digest());
            }
            // Fallback for non-file input streams (rare, but possible with custom filesystem providers)
            return calculateStreamHash(in, getSHA512Digest());
        } catch (NoSuchFileException e) {
            return null;
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * Calculates a hash of a file using the provided MessageDigest and FileChannel.
     * <p>
     * This implementation uses:
     * <ul>
     *   <li>64KB buffer size optimized for modern storage systems</li>
     *   <li>DirectByteBuffer for zero-copy I/O</li>
     *   <li>Efficient buffer management</li>
     * </ul>
     *
     * @param channel FileChannel to read from
     * @param digest MessageDigest to use for hashing
     * @return hexadecimal string of the hash value
     * @throws IOException if an I/O error occurs
     */
    public static String calculateFileHash(FileChannel channel, MessageDigest digest) throws IOException {
        // Modern OS/disk optimal transfer size (64KB)
        // Matches common SSD page sizes and OS buffer sizes
        final int BUFFER_SIZE = 64 * 1024;

        // Direct buffer for zero-copy I/O
        // Reuse buffer to avoid repeated allocation/deallocation
        ByteBuffer buffer = ByteBuffer.allocateDirect(BUFFER_SIZE);

        // Read until EOF
        while (channel.read(buffer) != -1) {
            buffer.flip();  // Prepare buffer for reading
            digest.update(buffer);  // Update digest
            buffer.clear();  // Prepare buffer for writing
        }

        return ByteUtilities.encode(digest.digest());
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
     * Creates a byte array suitable for use as an AES key from a string password.
     * <p>
     * The key is derived using MD5 and truncated to the specified bit length.
     *
     * @param key the password to derive the key from
     * @param bitsNeeded the required key length in bits (typically 128, 192, or 256)
     * @return byte array containing the derived key
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
     * @throws Exception if cipher creation fails
     */
    public static Cipher createAesEncryptionCipher(String key) throws Exception {
        return createAesCipher(key, Cipher.ENCRYPT_MODE);
    }

    /**
     * Creates an AES cipher in decryption mode.
     *
     * @param key the decryption key
     * @return Cipher configured for AES decryption
     * @throws Exception if cipher creation fails
     */
    public static Cipher createAesDecryptionCipher(String key) throws Exception {
        return createAesCipher(key, Cipher.DECRYPT_MODE);
    }

    /**
     * Creates an AES cipher with the specified mode.
     * <p>
     * Uses CBC mode with PKCS5 padding and IV derived from the key.
     *
     * @param key the encryption/decryption key
     * @param mode Cipher.ENCRYPT_MODE or Cipher.DECRYPT_MODE
     * @return configured Cipher instance
     * @throws Exception if cipher creation fails
     */
    public static Cipher createAesCipher(String key, int mode) throws Exception {
        Key sKey = new SecretKeySpec(createCipherBytes(key, 128), "AES");
        return createAesCipher(sKey, mode);
    }

    /**
     * Creates an AES cipher with the specified key and mode.
     * <p>
     * Uses CBC mode with PKCS5 padding and IV derived from the key.
     *
     * @param key SecretKeySpec for encryption/decryption
     * @param mode Cipher.ENCRYPT_MODE or Cipher.DECRYPT_MODE
     * @return configured Cipher instance
     * @throws Exception if cipher creation fails
     */
    public static Cipher createAesCipher(Key key, int mode) throws Exception {
        // Use password key as seed for IV (must be 16 bytes)
        MessageDigest d = getMD5Digest();
        d.update(key.getEncoded());
        byte[] iv = d.digest();

        AlgorithmParameterSpec paramSpec = new IvParameterSpec(iv);
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");        // CBC faster than CFB8/NoPadding (but file length changes)
        cipher.init(mode, key, paramSpec);
        return cipher;
    }

    /**
     * Encrypts a string using AES-128.
     *
     * @param key encryption key
     * @param content string to encrypt
     * @return hexadecimal string of encrypted data
     * @throws IllegalStateException if encryption fails
     */
    public static String encrypt(String key, String content) {
        try {
            return ByteUtilities.encode(createAesEncryptionCipher(key).doFinal(content.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("Error occurred encrypting data", e);
        }
    }

    /**
     * Encrypts a byte array using AES-128.
     *
     * @param key encryption key
     * @param content bytes to encrypt
     * @return hexadecimal string of encrypted data
     * @throws IllegalStateException if encryption fails
     */
    public static String encryptBytes(String key, byte[] content) {
        try {
            return ByteUtilities.encode(createAesEncryptionCipher(key).doFinal(content));
        } catch (Exception e) {
            throw new IllegalStateException("Error occurred encrypting data", e);
        }
    }

    /**
     * Decrypts a hexadecimal string of encrypted data to its original string form.
     *
     * @param key decryption key
     * @param hexStr hexadecimal string of encrypted data
     * @return decrypted string
     * @throws IllegalStateException if decryption fails
     */
    public static String decrypt(String key, String hexStr) {
        try {
            return new String(createAesDecryptionCipher(key).doFinal(ByteUtilities.decode(hexStr)));
        } catch (Exception e) {
            throw new IllegalStateException("Error occurred decrypting data", e);
        }
    }

    /**
     * Decrypts a hexadecimal string of encrypted data to its original byte array form.
     *
     * @param key decryption key
     * @param hexStr hexadecimal string of encrypted data
     * @return decrypted byte array
     * @throws IllegalStateException if decryption fails
     */
    public static byte[] decryptBytes(String key, String hexStr) {
        try {
            return createAesDecryptionCipher(key).doFinal(ByteUtilities.decode(hexStr));
        } catch (Exception e) {
            throw new IllegalStateException("Error occurred decrypting data", e);
        }
    }

    /**
     * Calculates a hash of a byte array using the specified MessageDigest.
     *
     * @param d MessageDigest to use
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
}
