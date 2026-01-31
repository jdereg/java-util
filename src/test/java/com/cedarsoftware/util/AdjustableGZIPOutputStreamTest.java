package com.cedarsoftware.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.zip.Deflater;
import java.util.zip.GZIPInputStream;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AdjustableGZIPOutputStreamTest {

    @Test
    public void testBufferAndLevelConstructor() throws Exception {
        byte[] input = new byte[2048];
        for (int i = 0; i < input.length; i++) {
            input[i] = 'A';
        }

        ByteArrayOutputStream fastOut = new ByteArrayOutputStream();
        try (AdjustableGZIPOutputStream out =
                 new AdjustableGZIPOutputStream(fastOut, 256, Deflater.BEST_SPEED)) {
            out.write(input);
        }
        byte[] fastBytes = fastOut.toByteArray();

        ByteArrayOutputStream bestOut = new ByteArrayOutputStream();
        try (AdjustableGZIPOutputStream out =
                 new AdjustableGZIPOutputStream(bestOut, 256, Deflater.BEST_COMPRESSION)) {
            out.write(input);
        }
        byte[] bestBytes = bestOut.toByteArray();

        assertArrayEquals(input, uncompress(bestBytes));
        assertArrayEquals(input, uncompress(fastBytes));
        assertTrue(bestBytes.length <= fastBytes.length);
    }

    private static byte[] uncompress(byte[] bytes) throws Exception {
        try (GZIPInputStream in = new GZIPInputStream(new ByteArrayInputStream(bytes));
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buf = new byte[128];
            int n;
            while ((n = in.read(buf)) > 0) {
                out.write(buf, 0, n);
            }
            return out.toByteArray();
        }
    }
}
