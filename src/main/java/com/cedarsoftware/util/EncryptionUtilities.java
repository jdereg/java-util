package com.cedarsoftware.util;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.spec.AlgorithmParameterSpec;

/**
 * Useful encryption utilities that simplify tasks like getting an
 * encrypted String return value (or MD5 hash String) for String or
 * Stream input.
 * @author John DeRegnaucourt (john@cedarsoftware.com)
 *         <br>
 *         Copyright (c) Cedar Software LLC
 *         <br><br>
 *         Licensed under the Apache License, Version 2.0 (the "License");
 *         you may not use this file except in compliance with the License.
 *         You may obtain a copy of the License at
 *         <br><br>
 *         http://www.apache.org/licenses/LICENSE-2.0
 *         <br><br>
 *         Unless required by applicable law or agreed to in writing, software
 *         distributed under the License is distributed on an "AS IS" BASIS,
 *         WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *         See the License for the specific language governing permissions and
 *         limitations under the License.
 */
public class EncryptionUtilities
{
    private EncryptionUtilities()
    {
    }

    /**
     * Super-fast MD5 calculation from entire file.  Uses FileChannel and
     * direct ByteBuffer (internal JVM memory).
     * @param file File that from which to compute the MD5
     * @return String MD5 value.
     */
    public static String fastMD5(File file)
    {        
        try (FileInputStream in = new FileInputStream(file))
        {            
            return calculateMD5Hash(in.getChannel());
        }
        catch (IOException e)
        {
            return null;
        }        
    }

    public static String calculateMD5Hash(FileChannel ch) throws IOException
    {
        ByteBuffer bb = ByteBuffer.allocateDirect(65536);
        MessageDigest d = getMD5Digest();

        int nRead;

        while ((nRead = ch.read(bb)) != -1)
        {
            if (nRead == 0)
            {
                continue;
            }
            bb.position(0);
            bb.limit(nRead);
            d.update(bb);
            bb.clear();
        }
        return ByteUtilities.encode(d.digest());
    }

    /**
     * Calculate an MD5 Hash String from the passed in byte[].
     * @param bytes byte[] for which to obtain the MD5 hash.
     * @return String of hex digits representing MD5 hash.
     */
    public static String calculateMD5Hash(byte[] bytes)
    {
        return calculateHash(getMD5Digest(), bytes);
    }


    public static MessageDigest getDigest(String digest)
    {
        try
        {
            return MessageDigest.getInstance(digest);
        }
        catch (NoSuchAlgorithmException e)
        {
            throw new IllegalArgumentException(String.format("The requested MessageDigest (%s) does not exist", digest), e);
        }

    }

    public static MessageDigest getMD5Digest()
    {
        return getDigest("MD5");
    }

    /**
     * Calculate an MD5 Hash String from the passed in byte[].
     */
    public static String calculateSHA1Hash(byte[] bytes)
    {
        return calculateHash(getSHA1Digest(), bytes);
    }

    public static MessageDigest getSHA1Digest()
    {
        return getDigest("SHA-1");
    }

    /**
     * Calculate an SHA-256 Hash String from the passed in byte[].
     */
    public static String calculateSHA256Hash(byte[] bytes)
    {
        return calculateHash(getSHA256Digest(), bytes);
    }

    public static MessageDigest getSHA256Digest()
    {
        return getDigest("SHA-256");
    }

    /**
     * Calculate an SHA-512 Hash String from the passed in byte[].
     */
    public static String calculateSHA512Hash(byte[] bytes)
    {
        return calculateHash(getSHA512Digest(), bytes);
    }

    public static MessageDigest getSHA512Digest()
    {
        return getDigest("SHA-512");
    }

    public static byte[] createCipherBytes(String key, int bitsNeeded)
    {
        String word = calculateMD5Hash(key.getBytes(StandardCharsets.UTF_8));
        return word.substring(0, bitsNeeded / 8).getBytes(StandardCharsets.UTF_8);
    }

    public static Cipher createAesEncryptionCipher(String key) throws Exception
    {
        return createAesCipher(key, Cipher.ENCRYPT_MODE);
    }

    public static Cipher createAesDecryptionCipher(String key) throws Exception
    {
        return createAesCipher(key, Cipher.DECRYPT_MODE);
    }

    public static Cipher createAesCipher(String key, int mode) throws Exception
    {
        Key sKey = new SecretKeySpec(createCipherBytes(key, 128), "AES");
        return createAesCipher(sKey, mode);
    }

    /**
     * Creates a Cipher from the passed in key, using the passed in mode.
     * @param key  SecretKeySpec
     * @param mode Cipher.ENCRYPT_MODE or Cipher.DECRYPT_MODE
     * @return Cipher instance created with the passed in key and mode.
     */
    public static Cipher createAesCipher(Key key, int mode) throws Exception
    {
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
     * Get hex String of content String encrypted.
     */
    public static String encrypt(String key, String content)
    {
        try
        {
            return ByteUtilities.encode(createAesEncryptionCipher(key).doFinal(content.getBytes(StandardCharsets.UTF_8)));
        }
        catch (Exception e)
        {
            throw new IllegalStateException("Error occurred encrypting data", e);
        }
    }

    public static String encryptBytes(String key, byte[] content)
    {
        try
        {
            return ByteUtilities.encode(createAesEncryptionCipher(key).doFinal(content));
        }
        catch (Exception e)
        {
            throw new IllegalStateException("Error occurred encrypting data", e);
        }
    }

    /**
     * Get unencrypted String from encrypted hex String
     */
    public static String decrypt(String key, String hexStr)
    {
        try
        {
            return new String(createAesDecryptionCipher(key).doFinal(ByteUtilities.decode(hexStr)));
        }
        catch (Exception e)
        {
            throw new IllegalStateException("Error occurred decrypting data", e);
        }
    }


    /**
     * Get unencrypted byte[] from encrypted hex String
     */
    public static byte[] decryptBytes(String key, String hexStr)
    {
        try
        {
            return createAesDecryptionCipher(key).doFinal(ByteUtilities.decode(hexStr));
        }
        catch (Exception e)
        {
            throw new IllegalStateException("Error occurred decrypting data", e);
        }
    }

    /**
     * Calculate an SHA-256 Hash String from the passed in byte[].
     */
    public static String calculateHash(MessageDigest d, byte[] bytes)
    {
        if (bytes == null)
        {
            return null;
        }

        d.update(bytes);
        return ByteUtilities.encode(d.digest());
    }
}
