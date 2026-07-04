package com.cedarsoftware.util;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pins the GraphComparator / IntervalSet / IOUtilities review fixes:
 *
 * 1. IOUtilities used default-locale case conversion in its security checks: on a
 *    Turkish-locale JVM, an allowed-protocols setting of "FILE" lowercased to "fıle"
 *    (dotless ı) and never matched a URL's "file" protocol (legitimate access blocked),
 *    and "WINDOWS" in os.name lowercased to "wındows", silently disabling every
 *    Windows system-path security check. All security-relevant case conversions now
 *    use Locale.ROOT.
 *
 * 2. IOUtilities only validated the URL protocol on the download path; the upload paths
 *    (transfer(File, URLConnection, cb) and transfer(URLConnection, byte[])) went
 *    straight to getOutputStream(). Both now run the same SSRF protocol validation.
 *
 * 3. IOUtilities.getInputStream leaked the underlying connection stream when the
 *    GZIP header read failed (GZIPInputStream constructor throws); the stream is now
 *    closed before the exception is rethrown.
 *
 * 4. IOUtilities.uncompressBytes treated a 1-byte slice of a larger gzip array as gzip
 *    (isGzipped reads two bytes at offset, past the caller's declared range) and threw
 *    instead of returning the slice.
 *
 * 5. IntervalSet mutations (merge/split) removed entries before re-adding their
 *    replacements, so lock-free readers could transiently see covered values as
 *    uncovered. Mutations now write replacements before removing what they absorb.
 *
 * 6. GraphComparator's resize/position deltas truncated Long/BigInteger optionalKeys
 *    with intValue(): 2^32 silently became 0 (list cleared / array emptied). Values
 *    must now be exactly representable as a non-negative int.
 */
class GraphIntervalIoReviewTest {

    // ---------------------------------------------------------------
    // 1. IOUtilities — Turkish-locale protocol allowlist
    // ---------------------------------------------------------------

    @Test
    void testProtocolAllowlistParsingUnderTurkishLocale(@TempDir File tempDir) throws Exception {
        Locale originalLocale = Locale.getDefault();
        String originalProp = System.getProperty("io.allowed.protocols");
        try {
            Locale.setDefault(new Locale("tr", "TR"));
            // Uppercase FILE forces the lowercase conversion path: under tr-TR the old
            // default-locale toLowerCase() produced "fıle" (dotless ı) — never matching
            System.setProperty("io.allowed.protocols", "FILE,HTTP,HTTPS,JAR");

            File data = new File(tempDir, "io-review.txt");
            Files.write(data.toPath(), "hello".getBytes(StandardCharsets.UTF_8));

            URLConnection conn = data.toURI().toURL().openConnection();
            try (InputStream in = IOUtilities.getInputStream(conn)) {
                byte[] read = IOUtilities.inputStreamToBytes(in);
                assertArrayEquals("hello".getBytes(StandardCharsets.UTF_8), read);
            }
        } finally {
            Locale.setDefault(originalLocale);
            if (originalProp == null) {
                System.clearProperty("io.allowed.protocols");
            } else {
                System.setProperty("io.allowed.protocols", originalProp);
            }
        }
    }

    // ---------------------------------------------------------------
    // 2. IOUtilities — upload paths run SSRF protocol validation
    // ---------------------------------------------------------------

    /** URLConnection with a non-allowlisted protocol that never touches the network. */
    private static URLConnection disallowedProtocolConnection() throws Exception {
        URLStreamHandler handler = new URLStreamHandler() {
            @Override
            protected URLConnection openConnection(URL u) {
                return new URLConnection(u) {
                    @Override
                    public void connect() {
                    }

                    @Override
                    public OutputStream getOutputStream() {
                        throw new AssertionError("getOutputStream must not be reached — protocol validation comes first");
                    }
                };
            }
        };
        URL url = new URL("ftp", "localhost", 21, "/upload.bin", handler);
        return url.openConnection();
    }

    @Test
    void testTransferBytesToDisallowedProtocolRejected() throws Exception {
        URLConnection conn = disallowedProtocolConnection();
        assertThrows(SecurityException.class, () -> IOUtilities.transfer(conn, new byte[]{1, 2, 3}));
    }

    @Test
    void testTransferFileToDisallowedProtocolRejected(@TempDir File tempDir) throws Exception {
        File data = new File(tempDir, "payload.bin");
        Files.write(data.toPath(), new byte[]{1, 2, 3});
        URLConnection conn = disallowedProtocolConnection();
        assertThrows(SecurityException.class, () -> IOUtilities.transfer(data, conn, null));
    }

    // ---------------------------------------------------------------
    // 3. IOUtilities — bad GZIP header closes the connection stream
    // ---------------------------------------------------------------

    @Test
    void testGetInputStreamClosesConnectionStreamOnBadGzipHeader() throws Exception {
        AtomicBoolean closed = new AtomicBoolean(false);
        InputStream garbage = new ByteArrayInputStream(new byte[]{1, 2, 3}) {
            @Override
            public void close() throws IOException {
                closed.set(true);
                super.close();
            }
        };
        URLConnection conn = new URLConnection(new URL("http://localhost/")) {
            @Override
            public void connect() {
            }

            @Override
            public String getContentEncoding() {
                return "gzip";
            }

            @Override
            public InputStream getInputStream() {
                return garbage;
            }
        };

        assertThrows(IOException.class, () -> IOUtilities.getInputStream(conn));
        assertTrue(closed.get(), "underlying connection stream must be closed when the GZIP header read fails");
    }

    // ---------------------------------------------------------------
    // 4. IOUtilities — short slice of a gzip array is not gzip
    // ---------------------------------------------------------------

    @Test
    void testUncompressBytesOneByteSliceNotTreatedAsGzip() {
        byte[] gz = IOUtilities.compressBytes("hello".getBytes(StandardCharsets.UTF_8));
        byte[] slice = IOUtilities.uncompressBytes(gz, 0, 1);
        assertArrayEquals(new byte[]{gz[0]}, slice, "a 1-byte range cannot be gzip and must round-trip as-is");
    }

    @Test
    void testCompressUncompressNullMessages() {
        assertThrows(IllegalArgumentException.class, () -> IOUtilities.compressBytes((byte[]) null));
        assertThrows(NullPointerException.class, () -> IOUtilities.uncompressBytes(null));
    }

    // ---------------------------------------------------------------
    // 5. IntervalSet — lock-free readers vs merge/split mutation order
    // ---------------------------------------------------------------

    @Test
    void testAddMergeSemanticsPreserved() {
        IntervalSet<Integer> set = new IntervalSet<>();
        set.add(1, 5);
        set.add(1, 3);          // absorbed (same start, shorter end — exercises the fold branch)
        assertEquals("{[1-5)}", set.toString());
        set.add(1, 8);          // same start, longer end
        assertEquals("{[1-8)}", set.toString());
        set.add(8, 10);         // adjacent — merges
        assertEquals("{[1-10)}", set.toString());
        set.add(20, 30);
        set.add(40, 50);
        set.add(5, 45);         // spans and absorbs multiple
        assertEquals("{[1-50)}", set.toString());
    }

    @Test
    void testRemoveRangeSplitSemanticsPreserved() {
        IntervalSet<Integer> set = new IntervalSet<>();
        set.add(0, 100);
        set.removeRange(40, 60);        // split
        assertEquals("{[0-40), [60-100)}", set.toString());
        set.removeRange(0, 10);         // trim left edge
        assertEquals("{[10-40), [60-100)}", set.toString());
        set.removeRange(90, 100);       // trim right edge
        assertEquals("{[10-40), [60-90)}", set.toString());
        set.removeRange(30, 70);        // spans the gap, trims both
        assertEquals("{[10-30), [70-90)}", set.toString());
        set.removeRange(0, 200);        // removes everything
        assertTrue(set.isEmpty());
    }

    @Test
    void testConcurrentReadersNeverSeeRetainedCoverageVanish() throws Exception {
        IntervalSet<Integer> set = new IntervalSet<>();
        set.add(0, 100);
        // Value 10 remains covered at every quiescent point of the writer loop below.
        // Before the fix, merge/split did remove-then-put, so a lock-free reader could
        // catch the window where the covering entry was absent.
        AtomicBoolean stop = new AtomicBoolean(false);
        AtomicInteger misses = new AtomicInteger(0);
        Thread reader = new Thread(() -> {
            while (!stop.get()) {
                if (!set.contains(10)) {
                    misses.incrementAndGet();
                }
            }
        });
        reader.setDaemon(true);
        reader.start();

        long deadline = System.currentTimeMillis() + 300;
        while (System.currentTimeMillis() < deadline) {
            set.add(50, 200);           // merge with the interval covering 10
            set.removeRange(150, 250);  // trim on the right
            set.removeRange(40, 60);    // split — 10 stays in the left fragment
            set.add(40, 60);            // heal the split (merges back)
        }
        stop.set(true);
        reader.join(5000);

        assertEquals(0, misses.get(), "reader must never see a covered value as uncovered during merge/split");
        assertTrue(set.contains(10));
    }

    // ---------------------------------------------------------------
    // 6. GraphComparator — resize values must be exactly int
    // ---------------------------------------------------------------

    private static class ListHolder {
        long id = 1L;
        List<String> list = new ArrayList<>(Arrays.asList("a", "b", "c"));
    }

    private static final GraphComparator.ID ID_FETCHER = obj -> {
        if (obj instanceof ListHolder) {
            return ((ListHolder) obj).id;
        }
        throw new IllegalArgumentException("no id");
    };

    @Test
    void testListResizeRejectsTruncatingLongValue() {
        ListHolder holder = new ListHolder();
        GraphComparator.Delta delta = new GraphComparator.Delta(1L, "list", "ptr", null, null, 4294967296L); // 2^32
        delta.setCmd(GraphComparator.Delta.Command.LIST_RESIZE);

        List<GraphComparator.DeltaError> errors = GraphComparator.applyDelta(
                holder, Collections.singletonList(delta), ID_FETCHER, GraphComparator.getJavaDeltaProcessor());

        assertEquals(1, errors.size(), "2^32 is not an int — the delta must fail, not silently resize to 0");
        assertEquals(3, holder.list.size(), "list must be unchanged after the rejected resize");
    }

    @Test
    void testListResizeAcceptsExactLongValue() {
        ListHolder holder = new ListHolder();
        GraphComparator.Delta delta = new GraphComparator.Delta(1L, "list", "ptr", null, null, 5L);
        delta.setCmd(GraphComparator.Delta.Command.LIST_RESIZE);

        List<GraphComparator.DeltaError> errors = GraphComparator.applyDelta(
                holder, Collections.singletonList(delta), ID_FETCHER, GraphComparator.getJavaDeltaProcessor());

        assertTrue(errors.isEmpty(), () -> "unexpected errors: " + errors);
        assertEquals(5, holder.list.size());
        assertFalse(holder.list.contains("d"));  // padded with nulls
    }
}
