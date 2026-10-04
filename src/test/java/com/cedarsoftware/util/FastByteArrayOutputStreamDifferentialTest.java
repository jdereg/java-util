package com.cedarsoftware.util;

import java.io.ByteArrayOutputStream;
import java.util.Random;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * FastByteArrayOutputStream against ByteArrayOutputStream: random single-byte and bulk writes, resets and
 * initial sizes, so the buffer grows through many sizes; contents and size compared after every step.
 */
class FastByteArrayOutputStreamDifferentialTest {

    @Test
    void everyStepHoldsWhatByteArrayOutputStreamHolds() {
        Random random = new Random(7);
        byte[] source = new byte[20_000];
        random.nextBytes(source);
        for (int run = 0; run < 200; run++) {
            int initial = 1 + random.nextInt(64);
            FastByteArrayOutputStream fast = new FastByteArrayOutputStream(initial);
            ByteArrayOutputStream reference = new ByteArrayOutputStream(initial);
            for (int step = 0; step < 60; step++) {
                int kind = random.nextInt(10);
                if (kind < 4) {
                    int b = random.nextInt(256);
                    fast.write(b);
                    reference.write(b);
                } else if (kind < 9) {
                    int len = random.nextInt(random.nextBoolean() ? 64 : 5000);
                    int off = random.nextInt(source.length - len);
                    fast.write(source, off, len);
                    reference.write(source, off, len);
                } else {
                    fast.reset();
                    reference.reset();
                }
                String where = "run " + run + " step " + step;
                assertEquals(reference.size(), fast.size(), where);
                assertArrayEquals(reference.toByteArray(), fast.toByteArray(), where);
            }
        }
    }
}
