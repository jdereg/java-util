package com.cedarsoftware.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Writes and reads the format of java-util 3.3.2 and earlier, for tests. The public methods that built this cipher
 * (createAesEncryptionCipher, createAesDecryptionCipher, createAesCipher, createCipherBytes) were removed in
 * 4.119.0. AES/CBC/PKCS5Padding, no authentication; the key is the first 16 hex digits of the unsalted MD5 of the
 * password, and the IV is the MD5 of those key bytes.
 * <p>
 * {@link #KNOWN} holds ciphertexts made by the removed createAesEncryptionCipher() itself, so a test can tell this
 * copy, and decrypt(), still match what was written.
 * </p>
 */
final class LegacyAesFormat {
    /** password, text, hex ciphertext: written by java-util's createAesEncryptionCipher() (4.118.0 and earlier). */
    static final String[][] KNOWN = {
            {"GavynRocks", "written by java-util 3.3.2",
                    "2EB6A27C21AD6527E81814BCAC202D866D7CF7F76251939B08B7EA312DB8B2EC"},
            {"the password", "", "BA3ACCC434A47F73DD6DA31EBEA671B7"},
            {"p", "sixteen chars...", "6F55213896AE2ED5CC321214E67C013A49DFD71DC223971D598EC0ED967C7B2A"},
    };

    private LegacyAesFormat() {
    }

    static Cipher cipher(String password, int mode) throws Exception {
        byte[] key = EncryptionUtilities.calculateMD5Hash(password.getBytes(StandardCharsets.UTF_8)).substring(0, 16)
                .getBytes(StandardCharsets.UTF_8);
        byte[] iv = MessageDigest.getInstance("MD5").digest(key);
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(mode, new SecretKeySpec(key, "AES"), new IvParameterSpec(iv));
        return cipher;
    }

    /** The hex text java-util 3.3.2 and earlier returned from encrypt(). */
    static String encrypt(String password, byte[] plain) throws Exception {
        return ByteUtilities.encode(cipher(password, Cipher.ENCRYPT_MODE).doFinal(plain));
    }
}
