package com.cedarsoftware.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Function;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * EncryptionUtilities' digests are now copies of one cached, untouched digest per algorithm instead of a
 * MessageDigest.getInstance() per call. Nothing else may change: every hash equals the one a fresh getInstance()
 * digest computes, whatever the input and however often; each call hands out an instance of its own; many threads
 * hashing at once get correct hashes; an algorithm the JVM lacks (SHA-3 before JDK 9) still throws
 * IllegalArgumentException.
 */
class EncryptionUtilitiesDigestReuseTest {

    private static final class Algorithm {
        final String name;
        final Supplier<MessageDigest> digest;
        final Function<byte[], String> hash;

        Algorithm(String name, Supplier<MessageDigest> digest, Function<byte[], String> hash) {
            this.name = name;
            this.digest = digest;
            this.hash = hash;
        }
    }

    private static final Algorithm[] ALGORITHMS = {
            new Algorithm("MD5", EncryptionUtilities::getMD5Digest, EncryptionUtilities::calculateMD5Hash),
            new Algorithm("SHA-1", EncryptionUtilities::getSHA1Digest, EncryptionUtilities::calculateSHA1Hash),
            new Algorithm("SHA-256", EncryptionUtilities::getSHA256Digest, EncryptionUtilities::calculateSHA256Hash),
            new Algorithm("SHA-384", EncryptionUtilities::getSHA384Digest, EncryptionUtilities::calculateSHA384Hash),
            new Algorithm("SHA-512", EncryptionUtilities::getSHA512Digest, EncryptionUtilities::calculateSHA512Hash),
            new Algorithm("SHA3-256", EncryptionUtilities::getSHA3_256Digest,
                    EncryptionUtilities::calculateSHA3_256Hash),
            new Algorithm("SHA3-512", EncryptionUtilities::getSHA3_512Digest,
                    EncryptionUtilities::calculateSHA3_512Hash)};

    private static MessageDigest fresh(String algorithm) {
        try {
            return MessageDigest.getInstance(algorithm);
        } catch (NoSuchAlgorithmException e) {
            return null;
        }
    }

    private static String expected(String algorithm, byte[] bytes) {
        return ByteUtilities.encode(fresh(algorithm).digest(bytes));
    }

    @Test
    void everyHashEqualsAFreshDigestsWhateverTheInputAndHoweverOften() {
        Random random = new Random(13);
        for (Algorithm algorithm : ALGORITHMS) {
            if (fresh(algorithm.name) == null) {
                assertThrows(IllegalArgumentException.class, algorithm.digest::get, algorithm.name);
                assertThrows(IllegalArgumentException.class, () -> algorithm.hash.apply(new byte[1]), algorithm.name);
                continue;
            }
            for (int round = 0; round < 3; round++) {
                for (int size : new int[]{0, 1, 31, 32, 64, 1000, 65536}) {
                    byte[] bytes = new byte[size];
                    random.nextBytes(bytes);
                    assertEquals(expected(algorithm.name, bytes), algorithm.hash.apply(bytes),
                            algorithm.name + " " + size);
                    MessageDigest digest = algorithm.digest.get();
                    assertEquals(algorithm.name, digest.getAlgorithm());
                    digest.update(bytes);
                    assertEquals(expected(algorithm.name, bytes), ByteUtilities.encode(digest.digest()),
                            algorithm.name);
                }
            }
        }
    }

    @Test
    void eachCallHandsOutAnInstanceOfItsOwn() {
        MessageDigest first = EncryptionUtilities.getSHA1Digest();
        MessageDigest second = EncryptionUtilities.getSHA1Digest();
        assertNotSame(first, second);
        first.update(new byte[]{1, 2, 3});                    // left part-way through a hash
        assertEquals(expected("SHA-1", new byte[]{9}), ByteUtilities.encode(second.digest(new byte[]{9})));
        assertEquals(expected("SHA-1", new byte[]{7}), EncryptionUtilities.calculateSHA1Hash(new byte[]{7}));
        assertEquals(expected("SHA-1", new byte[]{1, 2, 3, 4}), ByteUtilities.encode(first.digest(new byte[]{4})));
    }

    @Test
    void manyThreadsHashingAtOnceGetCorrectHashes() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(8);
        try {
            List<Future<Integer>> results = new ArrayList<>();
            for (int t = 0; t < 8; t++) {
                final int seed = t;
                results.add(pool.submit(() -> {
                    Random random = new Random(seed);
                    for (int i = 0; i < 2000; i++) {
                        byte[] bytes = new byte[random.nextInt(200)];
                        random.nextBytes(bytes);
                        assertEquals(expected("SHA-256", bytes), EncryptionUtilities.calculateSHA256Hash(bytes));
                        assertEquals(expected("SHA-1", bytes), EncryptionUtilities.calculateSHA1Hash(bytes));
                    }
                    return seed;
                }));
            }
            for (Future<Integer> result : results) {
                result.get();
            }
        } finally {
            pool.shutdown();
        }
    }
}
