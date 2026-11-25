package com.cedarsoftware.util;

import java.nio.charset.StandardCharsets;
import java.util.Random;

import javax.crypto.Cipher;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * decrypt() reads what encrypt() writes, AES-GCM, and nothing else unless the application says it still holds data
 * in the format of java-util 3.3.2 and earlier ({@code encryptionutilities.legacy.decrypt.enabled=true}).
 * <p>
 * encrypt() has written AES-GCM, which detects a changed ciphertext, since 3.3.3. decrypt() went on accepting the
 * earlier format for any input that was not an AES-GCM payload: AES-CBC with no authentication, a 64-bit key and one
 * IV for every message under a password. Anyone who could hand decrypt() an input and see whether it failed could
 * therefore have it accept a ciphertext they had built, decrypting to text of their choosing, without the password:
 * the padding check of the old format answers one question per try, and about 4,000 tries are enough. And a failed
 * tag check on an AES-GCM payload was retried as the old format, where about 1 wrong password in 256, or a chosen
 * change to the ciphertext, passes the padding check: decrypt() then returned noise instead of failing.
 */
class EncryptionUtilitiesLegacyDecryptTest {
    private static final String SWITCH = "encryptionutilities.legacy.decrypt.enabled";
    private String before;

    @BeforeEach
    void defaultSetting() {
        before = System.getProperty(SWITCH);
        System.clearProperty(SWITCH);
    }

    @AfterEach
    void restoreSetting() {
        if (before == null) {
            System.clearProperty(SWITCH);
        } else {
            System.setProperty(SWITCH, before);
        }
    }

    /** What java-util 3.3.2 and earlier wrote: the deprecated cipher, hex encoded. */
    private static String oldFormat(String password, byte[] plain) throws Exception {
        Cipher cipher = EncryptionUtilities.createAesEncryptionCipher(password);
        return ByteUtilities.encode(cipher.doFinal(plain));
    }

    private static String refusal(String password, String hex) {
        return assertThrows(IllegalStateException.class, () -> EncryptionUtilities.decryptBytes(password, hex)).getMessage();
    }

    @Test
    void whatEncryptWritesIsRead() {
        for (String text : new String[]{"", "a", "abc", "sixteen chars...", StringUtilities.repeat("long text ", 500),
                "é中😀"}) {
            assertEquals(text, EncryptionUtilities.decrypt("the password", EncryptionUtilities.encrypt("the password", text)));
            byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
            assertArrayEquals(bytes,
                    EncryptionUtilities.decryptBytes("the password", EncryptionUtilities.encryptBytes("the password", bytes)));
        }
    }

    @Test
    void theOldFormatIsRefusedUnlessTheApplicationAsksForIt() throws Exception {
        String old = oldFormat("the password", "written by 3.3.2".getBytes(StandardCharsets.UTF_8));
        IllegalStateException refused = assertThrows(IllegalStateException.class,
                () -> EncryptionUtilities.decrypt("the password", old));
        assertTrue(refused.getMessage().contains(SWITCH), refused.getMessage());
        assertTrue(refusal("the password", old).contains(SWITCH));

        System.setProperty(SWITCH, "true");
        assertEquals("written by 3.3.2", EncryptionUtilities.decrypt("the password", old));
        assertArrayEquals("written by 3.3.2".getBytes(StandardCharsets.UTF_8),
                EncryptionUtilities.decryptBytes("the password", old));
        // what encrypt() writes is read either way
        assertEquals("new", EncryptionUtilities.decrypt("the password", EncryptionUtilities.encrypt("the password", "new")));
    }

    /**
     * By default an input in the old format is refused before any cipher sees it, with the same answer whether its
     * padding is right or wrong. That answer was the oracle: "decrypted" for right padding, an error for wrong.
     */
    @Test
    void anInputInTheOldFormatLearnsNothingFromTheAnswer() throws Exception {
        String rightPadding = oldFormat("the password", new byte[40]);
        byte[] changed = ByteUtilities.decode(rightPadding);
        changed[changed.length - 17] ^= 0x55;                           // spoils the last block's padding
        String wrongPadding = ByteUtilities.encode(changed);
        byte[] random = new byte[32];
        new Random(4L).nextBytes(random);
        random[0] = 7;                                                  // not the version byte

        String answer = refusal("the password", rightPadding);
        assertEquals(answer, refusal("the password", wrongPadding));
        assertEquals(answer, refusal("the password", ByteUtilities.encode(random)));
        assertEquals(answer, refusal("another password", rightPadding));
    }

    /**
     * A ciphertext from encrypt() that fails its tag check fails. It was retried as the old format when its length
     * was a multiple of 16, and a chosen change to one byte makes the old format's padding come out right: the
     * 48 bytes of encrypt("abc") then decrypted to 47 bytes of noise.
     */
    @Test
    void aChangedCiphertextFailsAndIsNotReturnedAsNoise() throws Exception {
        byte[] written = ByteUtilities.decode(EncryptionUtilities.encrypt("rightKey", "abc"));
        assertEquals(48, written.length);
        String changedHex = null;
        for (int change = 1; change < 256 && changedHex == null; change++) {
            byte[] changed = written.clone();
            changed[31] ^= (byte) change;                               // the last byte of the next-to-last block
            try {
                EncryptionUtilities.createAesDecryptionCipher("rightKey").doFinal(changed);
                changedHex = ByteUtilities.encode(changed);             // the old format's padding is right
            } catch (Exception wrongPadding) {
                // try the next value: one of the 256 gives a last byte of 0x01
            }
        }
        assertNotNull(changedHex, "one value of the byte makes the old padding right");
        String hex = changedHex;

        assertThrows(IllegalStateException.class, () -> EncryptionUtilities.decryptBytes("rightKey", hex));
        assertThrows(IllegalStateException.class, () -> EncryptionUtilities.decrypt("rightKey", hex));

        System.setProperty(SWITCH, "true");                             // with the old format on, as before: noise
        assertEquals(47, EncryptionUtilities.decryptBytes("rightKey", hex).length);
    }
}
