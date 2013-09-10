package com.cedarsoftware.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.spec.AlgorithmParameterSpec;
import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Useful encryption utilities that simplify tasks like getting an
 * encrypted String return value (or MD5 hash String) for String or
 * Stream input.
 *
 * @author John DeRegnaucourt (jdereg@gmail.com)
 *         <br/>
 *         Copyright (c) John DeRegnaucourt
 *         <br/><br/>
 *         Licensed under the Apache License, Version 2.0 (the "License");
 *         you may not use this file except in compliance with the License.
 *         You may obtain a copy of the License at
 *         <br/><br/>
 *         http://www.apache.org/licenses/LICENSE-2.0
 *         <br/><br/>
 *         Unless required by applicable law or agreed to in writing, software
 *         distributed under the License is distributed on an "AS IS" BASIS,
 *         WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *         See the License for the specific language governing permissions and
 *         limitations under the License.
 */
public class EncryptionUtilities
{
    private EncryptionUtilities() { }

    /**
     * Super-fast MD5 calculation from entire file.  Uses FileChannel and
     * direct ByteBuffer (internal JVM memory).
     * @param file File that from which to compute the MD5
     * @return String MD5 value.
     */
    public static String fastMD5(File file)
    {
        FileInputStream in = null;
        try
        {
            in = new FileInputStream(file);
            FileChannel ch = in.getChannel();
            MessageDigest d = getMD5Digest();
            ByteBuffer bb = ByteBuffer.allocateDirect(65536);
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
            return StringUtilities.encode(d.digest());
        }
        catch(IOException e)
        {
            return null;
        }
        finally
        {
            IOUtilities.close(in);
        }
    }

    /**
     * Calculate an MD5 Hash String from the passed in byte[].
     */
    public static String calculateMD5Hash(byte[] bytes)
    {
        if (bytes == null)
        {
            return null;
        }

        MessageDigest d = getMD5Digest();
        d.update(bytes);
        return StringUtilities.encode(d.digest());
    }

    public static MessageDigest getMD5Digest()
    {
        try
        {
            return MessageDigest.getInstance("MD5");
        }
        catch (NoSuchAlgorithmException e)
        {
            return null;
        }
    }

    public static byte[] createCipherBytes(String key, int bitsNeeded)
    {
        String word = calculateMD5Hash(key.getBytes());
        return word.substring(0, bitsNeeded / 8).getBytes();
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
     * @param key SecretKeySpec
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
            return StringUtilities.encode(createAesEncryptionCipher(key).doFinal(content.getBytes()));
        }
        catch (Exception e)
        {
            return null;
        }
    }

    /**
     * Get unencrypted String from encrypted hex String
     */
    public static String decrypt(String key, String hexStr)
    {
        try
        {
            return new String(createAesDecryptionCipher(key).doFinal(StringUtilities.decode(hexStr)));
        }
        catch (Exception e)
        {
            return null;
        }
    }
}
