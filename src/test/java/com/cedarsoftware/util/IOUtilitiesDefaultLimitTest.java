package com.cedarsoftware.util;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.URL;
import java.util.zip.GZIPOutputStream;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The limits on data read into memory are ones a JVM can reach.
 * <p>
 * io.max.stream.size and io.max.decompression.size defaulted to Integer.MAX_VALUE. No array of that size can be
 * allocated, so by default the limit never spoke: a 1 MB gzip that inflates to 1 GB ended in OutOfMemoryError, not
 * in the exception the limit documents. The default is now half of the JVM's maximum heap (a read that held more
 * could not finish before either: it held its data twice at the end), and never more than the largest array.
 * <p>
 * And a gzip response that getInputStream() inflates is held to io.max.decompression.size when the application
 * sets it: it had no limit at all there.
 */
@Timeout(value = 60, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
class IOUtilitiesDefaultLimitTest {
    private static final String STREAM = "io.max.stream.size";
    private static final String DECOMPRESSION = "io.max.decompression.size";
    private static final int LARGEST_ARRAY = Integer.MAX_VALUE - 8;
    private String streamBefore;
    private String decompressionBefore;
    private HttpServer server;

    @BeforeEach
    void defaults() {
        streamBefore = System.getProperty(STREAM);
        decompressionBefore = System.getProperty(DECOMPRESSION);
        System.clearProperty(STREAM);
        System.clearProperty(DECOMPRESSION);
    }

    @AfterEach
    void restore() {
        set(STREAM, streamBefore);
        set(DECOMPRESSION, decompressionBefore);
        if (server != null) {
            server.stop(0);
        }
    }

    private static void set(String name, String value) {
        if (value == null) {
            System.clearProperty(name);
        } else {
            System.setProperty(name, value);
        }
    }

    private static int limit(String method) throws Exception {
        Method m = IOUtilities.class.getDeclaredMethod(method);
        m.setAccessible(true);
        return (Integer) m.invoke(null);
    }

    /**
     * Half the heap, give or take: the JVM's own figure for its maximum heap moves a little while it runs (JDK 8's
     * default collector leaves out a survivor space whose size it keeps adjusting), so two readings differ.
     */
    private static void assertHalfTheHeap(int limit) {
        long half = Runtime.getRuntime().maxMemory() / 2;
        if (half >= LARGEST_ARRAY) {
            assertEquals(LARGEST_ARRAY, limit);
        } else {
            assertTrue(limit >= half * 0.75 && limit <= half * 1.25, "limit " + limit + ", half the heap " + half);
        }
        assertTrue(limit > 0 && limit <= LARGEST_ARRAY);
    }

    @Test
    void theDefaultIsHalfTheHeapAndNeverMoreThanTheLargestArray() throws Exception {
        assertHalfTheHeap(limit("getCachedMaxStreamSize"));
        assertHalfTheHeap(limit("getCachedMaxDecompressionSize"));
    }

    @Test
    void aLimitSetPastTheLargestArrayIsTheLargestArray() throws Exception {
        System.setProperty(STREAM, "2147483647");
        System.setProperty(DECOMPRESSION, "9999999999");
        assertEquals(LARGEST_ARRAY, limit("getCachedMaxStreamSize"));
        assertEquals(LARGEST_ARRAY, limit("getCachedMaxDecompressionSize"));
        System.setProperty(STREAM, "1000");
        System.setProperty(DECOMPRESSION, "2000");
        assertEquals(1000, limit("getCachedMaxStreamSize"));
        assertEquals(2000, limit("getCachedMaxDecompressionSize"));
        System.setProperty(STREAM, "not a number");                 // an unusable setting is the default, as before
        assertHalfTheHeap(limit("getCachedMaxStreamSize"));
    }

    private static byte[] gzip(int size) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (GZIPOutputStream gzip = new GZIPOutputStream(out)) {
            gzip.write(new byte[size]);
        }
        return out.toByteArray();
    }

    @Test
    void aSetLimitRefusesWhatInflatesPastItInMemory() throws Exception {
        byte[] small = gzip(3 * 1024 * 1024);                       // about 3 KB that inflate to 3 MB
        assertEquals(3 * 1024 * 1024, IOUtilities.uncompressBytes(small).length);
        System.setProperty(DECOMPRESSION, "1048576");
        RuntimeException refused = assertThrows(RuntimeException.class, () -> IOUtilities.uncompressBytes(small));
        assertTrue(refused.getCause().getMessage().contains("exceeds maximum allowed size: 1048576"));
    }

    /** A gzip response is inflated without limit by default, and to the limit the application sets. */
    @Test
    void aSetLimitHoldsAResponseThatGetInputStreamInflates() throws Exception {
        byte[] body = gzip(3 * 1024 * 1024);
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/big", exchange -> {
            exchange.getResponseHeaders().add("Content-Encoding", "gzip");
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(body);
            }
        });
        server.start();
        URL url = new URL("http://127.0.0.1:" + server.getAddress().getPort() + "/big");

        ByteArrayOutputStream all = new ByteArrayOutputStream();
        try (InputStream in = IOUtilities.getInputStream((HttpURLConnection) url.openConnection())) {
            IOUtilities.transfer(in, all);
        }
        assertEquals(3 * 1024 * 1024, all.size());                  // no limit set: all of it, as before

        System.setProperty(DECOMPRESSION, "1048576");
        ByteArrayOutputStream limited = new ByteArrayOutputStream();
        Exception refused = assertThrows(Exception.class, () -> {
            try (InputStream in = IOUtilities.getInputStream((HttpURLConnection) url.openConnection())) {
                IOUtilities.transfer(in, limited);
            }
        });
        assertTrue(String.valueOf(refused.getMessage()).contains("exceeds maximum allowed size: 1048576"), refused.toString());
        assertTrue(limited.size() <= 1048576 + 65536, "stopped near the limit: " + limited.size());
    }
}
