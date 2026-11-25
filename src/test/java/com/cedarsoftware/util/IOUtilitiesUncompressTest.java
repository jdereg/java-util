package com.cedarsoftware.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.Arrays;
import java.util.Random;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * IOUtilities.uncompressBytes() reads straight into the array it returns, sized from the gzip trailer, and returns
 * what the previous reading returned: a GZIPInputStream read through inputStreamToBytes(), which staged every chunk
 * in a 32 KB buffer, collected them in a growing stream and copied the result out once more.
 * <p>
 * The size in a gzip trailer is whatever the sender wrote. It is used for the first allocation only, never beyond
 * 1,032 times the input or 16 MB, and the bytes returned are the ones the stream holds.
 */
class IOUtilitiesUncompressTest {

    /** The previous reading: the oracle for every result and every failure. */
    private static Object previously(byte[] bytes, int offset, int len, int maxSize) {
        try (GZIPInputStream gzip = new GZIPInputStream(new ByteArrayInputStream(bytes, offset, len), 32768)) {
            return IOUtilities.inputStreamToBytes(gzip, maxSize);
        } catch (Exception e) {
            return failure(e);
        }
    }

    private static Object now(byte[] bytes, int offset, int len, int maxSize) {
        try {
            return IOUtilities.uncompressBytes(bytes, offset, len, maxSize);
        } catch (Exception e) {
            return failure(e.getCause() == null ? e : e.getCause());
        }
    }

    private static String failure(Throwable e) {
        return "fails: " + e.getClass().getSimpleName() + " " + e.getMessage();
    }

    private static void assertSame(byte[] bytes, int offset, int len, int maxSize, String what) {
        Object before = previously(bytes, offset, len, maxSize);
        Object after = now(bytes, offset, len, maxSize);
        if (before instanceof byte[]) {
            assertTrue(after instanceof byte[], what + ": " + after);
            assertArrayEquals((byte[]) before, (byte[]) after, what);
        } else {
            assertEquals(before, after, what);
        }
    }

    private static byte[] gzip(byte[] data) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (GZIPOutputStream gzip = new GZIPOutputStream(out)) {
            gzip.write(data);
        }
        return out.toByteArray();
    }

    private static final String REPEATED = "{\"axis\":\"state\",\"value\":";

    private static byte[] data(Random random, int size) {
        byte[] data = new byte[size];
        int kind = random.nextInt(3);
        for (int i = 0; i < size; i++) {
            data[i] = kind == 0 ? (byte) random.nextInt(256)                 // does not compress
                    : kind == 1 ? (byte) ('a' + random.nextInt(4))           // compresses well
                    : (byte) REPEATED.charAt(i % REPEATED.length());         // repeats
        }
        return data;
    }

    @Test
    void everySizeAndEveryLimitAsBefore() throws IOException {
        Random random = new Random(20261125L);
        int[] sizes = {0, 1, 2, 17, 100, 511, 512, 513, 4000, 16383, 16384, 16385, 32768, 40_000, 65_536, 70_001,
                200_000, 1_100_000};
        for (int size : sizes) {
            for (int round = 0; round < 3; round++) {
                byte[] plain = data(random, size);
                byte[] zipped = gzip(plain);
                assertArrayEquals(plain, IOUtilities.uncompressBytes(zipped), "size " + size);
                for (int max : new int[]{Integer.MAX_VALUE, size + 1, Math.max(1, size), Math.max(1, size - 1),
                        Math.max(1, size / 2), 1, 16384, 16385}) {
                    assertSame(zipped, 0, zipped.length, max, "size " + size + " max " + max);
                }
                // inside a larger array
                byte[] padded = new byte[zipped.length + 11];
                System.arraycopy(zipped, 0, padded, 5, zipped.length);
                assertSame(padded, 5, zipped.length, Integer.MAX_VALUE, "offset, size " + size);
            }
        }
    }

    @Test
    void membersJoinedDamagedAndCutShortAsBefore() throws IOException {
        Random random = new Random(7L);
        for (int round = 0; round < 60; round++) {
            byte[] first = gzip(data(random, random.nextInt(50_000)));
            byte[] second = gzip(data(random, random.nextInt(50_000)));
            byte[] joined = Arrays.copyOf(first, first.length + second.length);
            System.arraycopy(second, 0, joined, first.length, second.length);
            assertSame(joined, 0, joined.length, Integer.MAX_VALUE, "two members");
            assertSame(joined, 0, joined.length, 30_000, "two members, limit");

            byte[] cut = Arrays.copyOf(first, Math.max(2, first.length - 1 - random.nextInt(Math.min(40, first.length - 2))));
            assertSame(cut, 0, cut.length, Integer.MAX_VALUE, "cut short by " + (first.length - cut.length));

            byte[] damaged = first.clone();
            damaged[2 + random.nextInt(damaged.length - 2)] ^= (byte) (1 << random.nextInt(8));
            assertSame(damaged, 0, damaged.length, Integer.MAX_VALUE, "one bit changed");
        }
    }

    /** A trailer that states a size the data does not have: the same failure as before, whatever it states. */
    @Test
    void aTrailerThatLiesAboutTheSizeIsTheFailureItWas() throws IOException {
        byte[] zipped = gzip(data(new Random(3L), 5_000));
        for (long stated : new long[]{0, 1, 4_999, 5_001, 1L << 20, 1L << 31, 0xFFFFFFFFL}) {
            byte[] lying = zipped.clone();
            for (int i = 0; i < 4; i++) {
                lying[lying.length - 4 + i] = (byte) (stated >>> (8 * i));
            }
            assertSame(lying, 0, lying.length, Integer.MAX_VALUE, "stated " + stated);
            assertSame(lying, 0, lying.length, 5_000, "stated " + stated + ", at the limit");
        }
    }

    /**
     * What a lying trailer can make the method allocate is held to 1,032 times the input: 30 bytes that state
     * 2 GB cost kilobytes, not gigabytes.
     */
    @Test
    void aLyingTrailerCannotAskForALargeArray() throws IOException {
        java.lang.management.ThreadMXBean bean = ManagementFactory.getThreadMXBean();
        assumeTrue(bean instanceof com.sun.management.ThreadMXBean);
        com.sun.management.ThreadMXBean counts = (com.sun.management.ThreadMXBean) bean;
        assumeTrue(counts.isThreadAllocatedMemorySupported() && counts.isThreadAllocatedMemoryEnabled());
        byte[] lying = gzip(new byte[]{1, 2, 3});
        lying[lying.length - 1] = 0x7F;                         // states about 2 GB
        lying[lying.length - 2] = (byte) 0xFF;
        now(new byte[]{31, -117, 8, 0}, 0, 4, 100);             // warm the failure path
        long id = Thread.currentThread().getId();
        long before = counts.getThreadAllocatedBytes(id);
        Object result = now(lying, 0, lying.length, Integer.MAX_VALUE);
        long allocated = counts.getThreadAllocatedBytes(id) - before;
        assertTrue(result instanceof String, "a wrong size is a failure");
        assertTrue(allocated < 500_000, lying.length + " bytes of input allocated " + allocated);
    }

    /** Data that is not gzip is returned as a copy, as before. */
    @Test
    void notGzipIsCopied() {
        byte[] plain = {1, 2, 3, 4, 5};
        byte[] result = IOUtilities.uncompressBytes(plain);
        assertArrayEquals(plain, result);
        assertTrue(result != plain);
        assertArrayEquals(new byte[]{2, 3}, IOUtilities.uncompressBytes(plain, 1, 2));
    }
}
