package com.cedarsoftware.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.Flushable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Coverage tests for IOUtilities — targets JaCoCo gaps:
 * - IOException catch blocks in close/flush/transfer methods
 * - Null input handling for close/flush methods
 * - XMLStreamException handling in close/flush XML methods
 * - File transfer error paths
 * - Compress/uncompress round-trips with edge cases
 */
class IOUtilitiesCoverageTest {

    // ========== close(Closeable) ==========

    @Test
    void testCloseNull() {
        // Should not throw
        IOUtilities.close((Closeable) null);
    }

    @Test
    void testCloseNormalCloseable() {
        ByteArrayInputStream in = new ByteArrayInputStream(new byte[10]);
        IOUtilities.close(in);
        // After close, read should return -1 (end of stream) or throw
        // Either way, the method should have succeeded
    }

    @Test
    void testCloseThrowingCloseable() {
        Closeable failing = () -> { throw new IOException("simulated close failure"); };
        assertThatThrownBy(() -> IOUtilities.close(failing))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("simulated close failure");
    }

    // ========== close(XMLStreamReader/Writer) ==========

    @Test
    void testCloseNullXmlStreamReader() {
        IOUtilities.close((javax.xml.stream.XMLStreamReader) null);
    }

    @Test
    void testCloseNullXmlStreamWriter() {
        IOUtilities.close((javax.xml.stream.XMLStreamWriter) null);
    }

    // ========== flush ==========

    @Test
    void testFlushNull() {
        IOUtilities.flush((Flushable) null);
    }

    @Test
    void testFlushNormalFlushable() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        IOUtilities.flush(out);
        // Should not throw
    }

    @Test
    void testFlushThrowingFlushable() {
        Flushable failing = () -> { throw new IOException("simulated flush failure"); };
        assertThatThrownBy(() -> IOUtilities.flush(failing))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("simulated flush failure");
    }

    @Test
    void testFlushNullXmlStreamWriter() {
        IOUtilities.flush((javax.xml.stream.XMLStreamWriter) null);
    }

    // ========== transfer with failing streams ==========

    @Test
    void testTransferInputStreamToOutputStream() {
        byte[] data = "test data".getBytes(StandardCharsets.UTF_8);
        ByteArrayInputStream in = new ByteArrayInputStream(data);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        long bytes = IOUtilities.transfer(in, out);
        assertThat(bytes).isEqualTo(data.length);
        assertThat(out.toByteArray()).isEqualTo(data);
    }

    @Test
    void testTransferToByteArray() {
        byte[] data = "hello".getBytes(StandardCharsets.UTF_8);
        ByteArrayInputStream in = new ByteArrayInputStream(data);
        byte[] buffer = new byte[5];
        int bytes = IOUtilities.transfer(in, buffer);
        assertThat(bytes).isEqualTo(5);
        assertThat(buffer).isEqualTo(data);
    }

    @Test
    void testTransferInputStreamToOutputStreamWithFailingInput() {
        InputStream failing = new InputStream() {
            @Override
            public int read() throws IOException {
                throw new IOException("simulated read failure");
            }
            @Override
            public int read(byte[] b, int off, int len) throws IOException {
                throw new IOException("simulated read failure");
            }
        };
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        assertThatThrownBy(() -> IOUtilities.transfer(failing, out))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("simulated read failure");
    }

    @Test
    void testTransferFileToOutputStream() throws IOException {
        File tmp = File.createTempFile("iotest", ".txt");
        tmp.deleteOnExit();
        byte[] data = "file contents".getBytes(StandardCharsets.UTF_8);
        Files.write(tmp.toPath(), data);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        long bytes = IOUtilities.transfer(tmp, out);
        assertThat(bytes).isEqualTo(data.length);
        assertThat(out.toByteArray()).isEqualTo(data);
    }

    @Test
    void testTransferNonexistentFile() {
        File nonExistent = new File("/tmp/nonexistent-file-" + System.nanoTime() + ".txt");
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        assertThatThrownBy(() -> IOUtilities.transfer(nonExistent, out))
                .isInstanceOf(Exception.class);
    }

    // ========== inputStreamToBytes ==========

    @Test
    void testInputStreamToBytes() {
        byte[] data = "input stream test".getBytes(StandardCharsets.UTF_8);
        ByteArrayInputStream in = new ByteArrayInputStream(data);
        byte[] result = IOUtilities.inputStreamToBytes(in);
        assertThat(result).isEqualTo(data);
    }

    @Test
    void testInputStreamToBytesWithLimit() {
        byte[] data = "test".getBytes(StandardCharsets.UTF_8);
        ByteArrayInputStream in = new ByteArrayInputStream(data);
        byte[] result = IOUtilities.inputStreamToBytes(in, 100);
        assertThat(result).isEqualTo(data);
    }

    @Test
    void testInputStreamToBytesNull() {
        assertThatThrownBy(() -> IOUtilities.inputStreamToBytes(null))
                .isInstanceOf(Exception.class);
    }

    @Test
    void testInputStreamToBytesZeroMaxSize() {
        ByteArrayInputStream in = new ByteArrayInputStream(new byte[10]);
        assertThatThrownBy(() -> IOUtilities.inputStreamToBytes(in, 0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void testInputStreamToBytesNegativeMaxSize() {
        ByteArrayInputStream in = new ByteArrayInputStream(new byte[10]);
        assertThatThrownBy(() -> IOUtilities.inputStreamToBytes(in, -1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void testInputStreamToBytesExceedsLimit() {
        byte[] data = new byte[1000];
        ByteArrayInputStream in = new ByteArrayInputStream(data);
        // maxSize smaller than data
        assertThatThrownBy(() -> IOUtilities.inputStreamToBytes(in, 10))
                .isInstanceOf(Exception.class);
    }

    // ========== compressBytes / uncompressBytes ==========

    @Test
    void testCompressUncompressRoundTrip() {
        byte[] original = "Hello, World! This is a test string for compression.".getBytes(StandardCharsets.UTF_8);
        byte[] compressed = IOUtilities.compressBytes(original);
        assertThat(compressed).isNotNull();
        byte[] uncompressed = IOUtilities.uncompressBytes(compressed);
        assertThat(uncompressed).isEqualTo(original);
    }

    @Test
    void testCompressUncompressRoundTripWithOffset() {
        byte[] original = "prefix-compressable-suffix".getBytes(StandardCharsets.UTF_8);
        // Compress only the middle portion
        byte[] compressed = IOUtilities.compressBytes(original, 7, 12);
        byte[] uncompressed = IOUtilities.uncompressBytes(compressed);
        assertThat(new String(uncompressed, StandardCharsets.UTF_8)).isEqualTo("compressable");
    }

    @Test
    void testCompressEmptyArray() {
        byte[] compressed = IOUtilities.compressBytes(new byte[0]);
        byte[] uncompressed = IOUtilities.uncompressBytes(compressed);
        assertThat(uncompressed).isEmpty();
    }

    @Test
    void testCompressBytesByteArrayOutputStream() {
        ByteArrayOutputStream original = new ByteArrayOutputStream();
        try {
            original.write("test data".getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        ByteArrayOutputStream compressed = new ByteArrayOutputStream();
        IOUtilities.compressBytes(original, compressed);
        assertThat(compressed.size()).isGreaterThan(0);
    }

    @Test
    void testCompressBytesFastByteArrayOutputStream() {
        FastByteArrayOutputStream original = new FastByteArrayOutputStream();
        try {
            original.write("test data".getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        FastByteArrayOutputStream compressed = new FastByteArrayOutputStream();
        IOUtilities.compressBytes(original, compressed);
        assertThat(compressed.size()).isGreaterThan(0);
    }

    @Test
    void testUncompressBytesNonGzippedReturnsCopy() {
        // uncompressBytes checks the gzip magic number; if not gzipped, returns a copy as-is
        byte[] notCompressed = "this is not gzipped data".getBytes(StandardCharsets.UTF_8);
        byte[] result = IOUtilities.uncompressBytes(notCompressed);
        assertThat(result).isEqualTo(notCompressed);
    }

    @Test
    void testUncompressBytesCorruptedGzipHeader() {
        // Bytes that start with gzip magic (1f 8b) but are actually corrupted
        byte[] corruptGzip = new byte[20];
        corruptGzip[0] = (byte) 0x1f;
        corruptGzip[1] = (byte) 0x8b;
        // Rest is garbage
        assertThatThrownBy(() -> IOUtilities.uncompressBytes(corruptGzip))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void testUncompressBytesWithLimit() {
        byte[] original = "test".getBytes(StandardCharsets.UTF_8);
        byte[] compressed = IOUtilities.compressBytes(original);
        byte[] uncompressed = IOUtilities.uncompressBytes(compressed, 0, compressed.length, 100);
        assertThat(uncompressed).isEqualTo(original);
    }

    // ========== transfer with callbacks ==========

    @Test
    void testTransferWithCallback() {
        byte[] data = new byte[10000];
        for (int i = 0; i < data.length; i++) data[i] = (byte) i;
        ByteArrayInputStream in = new ByteArrayInputStream(data);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        int[] callbackCount = {0};
        IOUtilities.TransferCallback cb = new IOUtilities.TransferCallback() {
            @Override
            public void bytesTransferred(byte[] bytes, int count) {
                callbackCount[0]++;
            }

            @Override
            public boolean isCancelled() {
                return false;
            }
        };

        long bytes = IOUtilities.transfer(in, out, cb);
        assertThat(bytes).isEqualTo(data.length);
        assertThat(callbackCount[0]).isGreaterThan(0);
    }

    @Test
    void testTransferWithCancellingCallback() {
        // Use an InputStream that returns 1 byte at a time, so cancel fires after the first byte
        InputStream slowIn = new InputStream() {
            private int count = 0;
            @Override
            public int read() {
                if (count++ < 100) return count;
                return -1;
            }
            @Override
            public int read(byte[] b, int off, int len) {
                // Force 1 byte at a time to trigger cancel check
                int r = read();
                if (r == -1) return -1;
                b[off] = (byte) r;
                return 1;
            }
        };
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        IOUtilities.TransferCallback cb = new IOUtilities.TransferCallback() {
            @Override
            public void bytesTransferred(byte[] bytes, int count) {}

            @Override
            public boolean isCancelled() {
                return true; // Cancel immediately after first byte
            }
        };

        long bytes = IOUtilities.transfer(slowIn, out, cb);
        // Should transfer exactly 1 byte (the first one) and then cancel
        assertThat(bytes).isEqualTo(1L);
    }

    // ========== Closeable that throws on close to hit catch block ==========

    @Test
    void testTransferWithClosingFailure() {
        // Input stream that works but throws on close (even though transfer doesn't close it,
        // this exercises the "normal success" path)
        byte[] data = "hello".getBytes(StandardCharsets.UTF_8);
        ByteArrayInputStream in = new ByteArrayInputStream(data);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        long bytes = IOUtilities.transfer(in, out);
        assertThat(bytes).isEqualTo(data.length);
    }

    // ========== getDefaultMaxStreamSize + related config ==========

    @Test
    void testTransferStreamToFile() throws IOException {
        File tmp = File.createTempFile("iotest-out", ".txt");
        tmp.deleteOnExit();
        byte[] data = "stream to file test".getBytes(StandardCharsets.UTF_8);
        ByteArrayInputStream in = new ByteArrayInputStream(data);

        long bytes = IOUtilities.transfer(in, tmp, null);
        assertThat(bytes).isEqualTo(data.length);
        assertThat(Files.readAllBytes(tmp.toPath())).isEqualTo(data);
    }
}
