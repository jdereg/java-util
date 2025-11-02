package com.cedarsoftware.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Additional tests for IOUtilities covering APIs not exercised by IOUtilitiesTest.
 */
public class IOUtilitiesAdditionalTest {
    @Test
    public void testTransferInputStreamToFileWithCallback() throws Exception {
        byte[] data = "Callback test".getBytes(StandardCharsets.UTF_8);
        ByteArrayInputStream in = new ByteArrayInputStream(data);
        File f = File.createTempFile("iou", "cb");
        AtomicInteger transferred = new AtomicInteger();

        IOUtilities.transfer(in, f, new IOUtilities.TransferCallback() {
            public void bytesTransferred(byte[] bytes, int count) {
                transferred.addAndGet(count);
            }
        });

        byte[] result = Files.readAllBytes(f.toPath());
        assertEquals("Callback test", new String(result, StandardCharsets.UTF_8));
        assertEquals(data.length, transferred.get());
        assertFalse(new IOUtilities.TransferCallback() {
            public void bytesTransferred(byte[] b, int c) {}
        }.isCancelled());
        f.delete();
    }

    @Test
    public void testTransferURLConnectionWithByteArray() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        URLConnection conn = mock(URLConnection.class);
        when(conn.getOutputStream()).thenReturn(out);

        byte[] bytes = "abc123".getBytes(StandardCharsets.UTF_8);
        IOUtilities.transfer(conn, bytes);

        assertArrayEquals(bytes, out.toByteArray());
    }

    @Test
    public void testInputStreamToBytesWithLimit() throws Exception {
        ByteArrayInputStream in = new ByteArrayInputStream("hello".getBytes(StandardCharsets.UTF_8));
        byte[] bytes = IOUtilities.inputStreamToBytes(in, 10);
        assertEquals("hello", new String(bytes, StandardCharsets.UTF_8));
    }

    @Test
    public void testInputStreamToBytesOverLimit() {
        ByteArrayInputStream in = new ByteArrayInputStream("toolong".getBytes(StandardCharsets.UTF_8));
        IOException ex = assertThrows(IOException.class, () -> IOUtilities.inputStreamToBytes(in, 4));
        assertTrue(ex.getMessage().contains("Stream exceeds"));
    }

    @Test
    public void testCompressBytesUsingStreams() throws Exception {
        ByteArrayOutputStream original = new ByteArrayOutputStream();
        original.write("compress me".getBytes(StandardCharsets.UTF_8));
        ByteArrayOutputStream compressed = new ByteArrayOutputStream();

        IOUtilities.compressBytes(original, compressed);
        byte[] result = IOUtilities.uncompressBytes(compressed.toByteArray());
        assertEquals("compress me", new String(result, StandardCharsets.UTF_8));
    }

    @Test
    public void testCompressBytesWithOffset() {
        byte[] data = "0123456789".getBytes(StandardCharsets.UTF_8);
        byte[] compressed = IOUtilities.compressBytes(data, 3, 4);
        byte[] result = IOUtilities.uncompressBytes(compressed);
        assertEquals("3456", new String(result, StandardCharsets.UTF_8));
    }

    @Test
    public void testCloseCloseableThrowsUnchecked() {
        AtomicBoolean closed = new AtomicBoolean(false);
        Closeable c = () -> { closed.set(true); throw new IOException("fail"); };

        // close() should throw IOException as unchecked
        assertThrows(IOException.class, () -> IOUtilities.close(c));
        assertTrue(closed.get());
    }

    @Test
    public void testTransferStreamToStream() throws Exception {
        ByteArrayInputStream in = new ByteArrayInputStream("ABC".getBytes(StandardCharsets.UTF_8));
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        IOUtilities.transfer(in, out);
        assertEquals("ABC", new String(out.toByteArray(), StandardCharsets.UTF_8));
    }
}
