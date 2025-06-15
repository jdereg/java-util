package com.cedarsoftware.util;

import org.junit.jupiter.api.Test;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.security.MessageDigest;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Additional tests for low level APIs in {@link EncryptionUtilities}.
 */
public class EncryptionUtilitiesLowLevelTest {

    private static final String SAMPLE = "The quick brown fox jumps over the lazy dog";

    @Test
    public void testCalculateHash() {
        MessageDigest digest = EncryptionUtilities.getSHA1Digest();
        String hash = EncryptionUtilities.calculateHash(digest, SAMPLE.getBytes(StandardCharsets.UTF_8));
        assertEquals(EncryptionUtilities.calculateSHA1Hash(SAMPLE.getBytes(StandardCharsets.UTF_8)), hash);
        assertNull(EncryptionUtilities.calculateHash(digest, null));
    }

    @Test
    public void testCreateCipherBytes() {
        byte[] bytes = EncryptionUtilities.createCipherBytes("password", 128);
        assertArrayEquals("5F4DCC3B5AA765D6".getBytes(StandardCharsets.UTF_8), bytes);
    }

    @Test
    public void testCreateAesEncryptionDecryptionCipher() throws Exception {
        String key = "secret";
        Cipher enc = EncryptionUtilities.createAesEncryptionCipher(key);
        Cipher dec = EncryptionUtilities.createAesDecryptionCipher(key);
        byte[] plain = "hello world".getBytes(StandardCharsets.UTF_8);
        byte[] cipherText = enc.doFinal(plain);
        assertArrayEquals(plain, dec.doFinal(cipherText));
    }

    @Test
    public void testCreateAesCipherWithKey() throws Exception {
        byte[] b = EncryptionUtilities.createCipherBytes("password", 128);
        Key key = new SecretKeySpec(b, "AES");
        Cipher enc = EncryptionUtilities.createAesCipher(key, Cipher.ENCRYPT_MODE);
        Cipher dec = EncryptionUtilities.createAesCipher(key, Cipher.DECRYPT_MODE);
        byte[] value = "binary".getBytes(StandardCharsets.UTF_8);
        byte[] encrypted = enc.doFinal(value);
        assertArrayEquals(value, dec.doFinal(encrypted));
    }

    @Test
    public void testDeriveKey() {
        byte[] salt = {0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15};
        byte[] key = EncryptionUtilities.deriveKey("password", salt, 128);
        assertEquals(16, key.length);
        assertEquals("274A9A8F481754C732CD0E0B328D478C", ByteUtilities.encode(key));
    }

    @Test
    public void testFastShaAlgorithms() {
        File file = new File(getClass().getClassLoader().getResource("fast-md5-test.txt").getFile());
        assertEquals("8707DA8D6F770B154D1E5031AA747E85818F3653", EncryptionUtilities.fastSHA1(file));
        assertEquals("EAB59F8BD10D480728DC00DBC66432CAB825C40767281171A84AE27F7C38795A", EncryptionUtilities.fastSHA256(file));
        assertEquals("3C3BE710A85E41F2BCAD99EF0D194246C2431C53DBD4498BD83298E9411397F8C981B1457B102952B0EC9736A420EF8E", EncryptionUtilities.fastSHA384(file));
        assertEquals("F792CDBE5293BE2E5200563E879808A9C8F32CBBBF044C11DA8A6BD120B8133AA8A4516BA2898B85AC2FDC6CD21DED02568EB468D8F0D212B6C030C579D906DA", EncryptionUtilities.fastSHA512(file));
        assertEquals("468A784A890FEB2FF56ACE89737D11ABD6E933F5730D237445265A27A8D6232C", EncryptionUtilities.fastSHA3_256(file));
        assertEquals("2573F2DD2416A3CE28FA2F0C6B2C865FB90A23E7057E831A4870CD91360DC4CAAEC00BD39B90CE76B2BFBC6C6C4D0F1492C6181E29491AF472EC41A2FDCF6E5D", EncryptionUtilities.fastSHA3_512(file));
    }

    @Test
    public void testFastShaNullFile() {
        assertNull(EncryptionUtilities.fastSHA1(new File("missing")));
        assertNull(EncryptionUtilities.fastSHA512(new File("missing")));
    }

    @Test
    public void testGetDigestAlgorithms() {
        assertEquals("SHA-1", EncryptionUtilities.getSHA1Digest().getAlgorithm());
        assertEquals("SHA-256", EncryptionUtilities.getSHA256Digest().getAlgorithm());
        assertEquals("SHA-384", EncryptionUtilities.getSHA384Digest().getAlgorithm());
        assertEquals("SHA3-256", EncryptionUtilities.getSHA3_256Digest().getAlgorithm());
        assertEquals("SHA3-512", EncryptionUtilities.getSHA3_512Digest().getAlgorithm());
        assertEquals("SHA-512", EncryptionUtilities.getSHA512Digest().getAlgorithm());
    }
}
