package com.cedarsoftware.util;

import java.io.Reader;
import java.io.StringReader;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * FastReader at the edges of its arguments and of its input.
 */
class FastReaderEdgeTest {

    /** A reader that counts the calls made on it. */
    static final class CountingReader extends Reader {
        private final Reader delegate;
        int reads;

        CountingReader(String text) {
            delegate = new StringReader(text);
        }

        @Override
        public int read(char[] cbuf, int off, int len) throws java.io.IOException {
            reads++;
            return delegate.read(cbuf, off, len);
        }

        @Override
        public void close() {
        }
    }

    /**
     * readLine with maxLen near Integer.MAX_VALUE (no limit, in effect) once reading had started: position plus
     * maxLen overflowed, nothing was copied, and the call spun forever.
     */
    @Test
    void readLineWithNoPracticalLimitReadsTheLine() throws Exception {
        FastReader reader = new FastReader(new StringReader("abc\ndef"), 16, 4);
        assertEquals('a', reader.read());
        char[] dest = new char[16];
        int[] result = new int[1];
        Thread thread = new Thread(() -> result[0] = reader.readLine(dest, 0, Integer.MAX_VALUE));
        thread.setDaemon(true);
        thread.start();
        thread.join(5000);
        assertTrue(!thread.isAlive(), "readLine never returned");
        assertEquals(2, result[0]);
        assertEquals("bc", new String(dest, 0, 2));
        assertEquals(3, reader.readLine(dest, 0, Integer.MAX_VALUE));
        assertEquals("def", new String(dest, 0, 3));
    }

    /**
     * Once the input has ended, readLine answers -1 without asking the underlying reader again; on a terminal or a
     * pipe each such read blocks. read(), readUntil and the borrowed methods already stopped at the end.
     */
    @Test
    void readLineDoesNotReadPastTheEnd() {
        CountingReader counting = new CountingReader("a\n");
        FastReader reader = new FastReader(counting, 16, 4);
        char[] dest = new char[16];
        assertEquals(1, reader.readLine(dest, 0, 16));
        assertEquals(-1, reader.readLine(dest, 0, 16));
        int readsAtEnd = counting.reads;
        assertEquals(-1, reader.readLine(dest, 0, 16));
        assertEquals(-1, reader.readLine(dest, 0, 16));
        assertEquals(-1, reader.read());
        assertEquals(readsAtEnd, counting.reads);
    }
}
