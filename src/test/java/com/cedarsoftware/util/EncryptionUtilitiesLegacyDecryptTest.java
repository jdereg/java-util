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
 * decrypt() reads what encrypt() writes, AES-GCM, and then, as a second try, the format of java-util 3.3.2 and
 * earlier, unless the application turns that off ({@code encryptionutilities.legacy.decrypt.enabled=false}).
 * <p>
 * encrypt() has written AES-GCM, which detects a changed ciphertext, since 3.3.3. The earlier format is AES-CBC with
 * no authentication, a 64-bit key and one IV for every message under a password. Applications still hold data in it,
 * so it is read by default. Turned off, decrypt() closes what the old format opens: anyone who can hand decrypt() an
 * input and see whether it failed can have it accept a ciphertext they built, decrypting to text of their choosing,
 * without the password (the old format's padding check answers one question per try, and about 4,000 tries are
 * enough); and a failed tag check on an AES-GCM payload is retried as the old format, where about 1 wrong password
 * in 256, or a chosen change to the ciphertext, passes the padding check and returns noise.
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

    /** Ciphertexts written by the removed createAesEncryptionCipher() read as they always did, with no setting. */
    @Test
    void dataWrittenByTheOldFormatIsReadByDefault() throws Exception {
        for (String[] known : LegacyAesFormat.KNOWN) {
            String password = known[0];
            String text = known[1];
            String hex = known[2];
            assertEquals(hex, LegacyAesFormat.encrypt(password, text.getBytes(StandardCharsets.UTF_8)));
            assertEquals(text, EncryptionUtilities.decrypt(password, hex));
            assertEquals(text, EncryptionUtilities.decrypt(password, hex.toLowerCase()));
            assertArrayEquals(text.getBytes(StandardCharsets.UTF_8), EncryptionUtilities.decryptBytes(password, hex));
        }
        System.setProperty(SWITCH, "true");                             // said explicitly: the same
        assertEquals(LegacyAesFormat.KNOWN[0][1],
                EncryptionUtilities.decrypt(LegacyAesFormat.KNOWN[0][0], LegacyAesFormat.KNOWN[0][2]));
    }

    @Test
    void turnedOffTheOldFormatIsRefused() throws Exception {
        String old = LegacyAesFormat.encrypt("the password", "written by 3.3.2".getBytes(StandardCharsets.UTF_8));
        assertEquals("written by 3.3.2", EncryptionUtilities.decrypt("the password", old));

        for (String off : new String[]{"false", "FALSE", " false "}) {
            System.setProperty(SWITCH, off);
            IllegalStateException refused = assertThrows(IllegalStateException.class,
                    () -> EncryptionUtilities.decrypt("the password", old));
            assertTrue(refused.getMessage().contains(SWITCH + "=false"), refused.getMessage());
            assertTrue(refusal("the password", old).contains(SWITCH));
            // what encrypt() writes is read either way
            String fresh = EncryptionUtilities.encrypt("the password", "new");
            assertEquals("new", EncryptionUtilities.decrypt("the password", fresh));
        }
    }

    /**
     * Turned off, an input in the old format is refused before any cipher sees it, with the same answer whether its
     * padding is right or wrong. That answer is the oracle the old format opens: "decrypted" for right padding, an
     * error for wrong.
     */
    @Test
    void turnedOffAnInputInTheOldFormatLearnsNothingFromTheAnswer() throws Exception {
        System.setProperty(SWITCH, "false");
        String rightPadding = LegacyAesFormat.encrypt("the password", new byte[40]);
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
     * A ciphertext from encrypt() that fails its tag check is retried as the old format when its length is a multiple
     * of 16, and a chosen change to one byte makes the old format's padding come out right: the 48 bytes of
     * encrypt("abc") then decrypt to 47 bytes of noise. That is the price of reading the old format by default.
     * Turned off, the changed ciphertext fails.
     */
    @Test
    void aChangedCiphertextFailsOnlyWithTheOldFormatTurnedOff() throws Exception {
        byte[] written = ByteUtilities.decode(EncryptionUtilities.encrypt("rightKey", "abc"));
        assertEquals(48, written.length);
        String changedHex = null;
        for (int change = 1; change < 256 && changedHex == null; change++) {
            byte[] changed = written.clone();
            changed[31] ^= (byte) change;                               // the last byte of the next-to-last block
            try {
                LegacyAesFormat.cipher("rightKey", Cipher.DECRYPT_MODE).doFinal(changed);
                changedHex = ByteUtilities.encode(changed);             // the old format's padding is right
            } catch (Exception wrongPadding) {
                // try the next value: one of the 256 gives a last byte of 0x01
            }
        }
        assertNotNull(changedHex, "one value of the byte makes the old padding right");
        String hex = changedHex;

        assertEquals(47, EncryptionUtilities.decryptBytes("rightKey", hex).length);   // the default: noise

        System.setProperty(SWITCH, "false");
        assertThrows(IllegalStateException.class, () -> EncryptionUtilities.decryptBytes("rightKey", hex));
        assertThrows(IllegalStateException.class, () -> EncryptionUtilities.decrypt("rightKey", hex));
    }

    /** A wrong password on old-format data still fails (bad padding), apart from the 1-in-256 noise case. */
    @Test
    void theOldFormatWithAWrongPasswordFails() throws Exception {
        String text = "some text that is longer than one block";
        String old = LegacyAesFormat.encrypt("right", text.getBytes(StandardCharsets.UTF_8));
        int failures = 0;
        for (int i = 0; i < 50; i++) {
            try {
                String back = EncryptionUtilities.decrypt("wrong" + i, old);
                assertTrue(!text.equals(back));
            } catch (IllegalStateException expected) {
                failures++;
            }
        }
        assertTrue(failures >= 40, "most wrong passwords fail the padding check: " + failures);
    }
}
