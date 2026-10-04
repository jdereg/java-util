package com.cedarsoftware.util;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * FastWriter after its underlying writer fails: a failed write loses nothing, and a later write retries it, as
 * BufferedWriter does.
 */
class FastWriterFailureTest {

    /** A writer whose first write fails; later writes go through. */
    static final class FailsOnce extends Writer {
        final StringWriter written = new StringWriter();
        boolean failed;

        @Override
        public void write(char[] cbuf, int off, int len) throws IOException {
            if (!failed) {
                failed = true;
                throw new IOException("first write fails");
            }
            written.write(cbuf, off, len);
        }

        @Override
        public void flush() {
        }

        @Override
        public void close() {
        }
    }

    /**
     * write(int) fills the buffer and flushes it; when that flush failed, the next write(int) stored past the end
     * of the buffer (ArrayIndexOutOfBoundsException, then every write and flush after it failed too).
     */
    @Test
    void aCharWriteAfterAFailedFlushRetriesIt() throws Exception {
        FailsOnce target = new FailsOnce();
        FastWriter writer = new FastWriter(target, 4);
        writer.write('a');
        writer.write('b');
        writer.write('c');
        assertThrows(IOException.class, () -> writer.write('d'));     // the full buffer's flush fails
        writer.write('e');
        writer.flush();
        assertEquals("abcde", target.written.toString());
    }

    /** A string write after a failed flush wrote over the buffered characters, losing them with no error. */
    @Test
    void aStringWriteAfterAFailedFlushKeepsTheBufferedCharacters() throws Exception {
        FailsOnce target = new FailsOnce();
        FastWriter writer = new FastWriter(target, 4);
        assertThrows(IOException.class, () -> writer.write("abcd"));  // fills the buffer; its flush fails
        writer.write("xyz12");
        writer.flush();
        assertEquals("abcdxyz12", target.written.toString());
    }

    /**
     * close() closes the writer even when the underlying close fails, and a flush failure is kept with the close
     * failure suppressed into it. The writer stayed open, and a write after the failed close went through.
     */
    @Test
    void closeClosesEvenWhenTheUnderlyingCloseFails() throws Exception {
        Writer failingClose = new StringWriter() {
            @Override
            public void close() throws IOException {
                throw new IOException("close fails");
            }
        };
        FastWriter writer = new FastWriter(failingClose, 16);
        writer.write("abc");
        IOException closeFailure = assertThrows(IOException.class, writer::close);
        assertEquals("close fails", closeFailure.getMessage());
        assertThrows(IOException.class, () -> writer.write('x'));
        writer.close();                                               // closing again is harmless

        Writer failingBoth = new Writer() {
            @Override
            public void write(char[] cbuf, int off, int len) throws IOException {
                throw new IOException("write fails");
            }

            @Override
            public void flush() {
            }

            @Override
            public void close() throws IOException {
                throw new IOException("close fails");
            }
        };
        FastWriter both = new FastWriter(failingBoth, 16);
        both.write("abc");
        IOException flushFailure = assertThrows(IOException.class, both::close);
        assertEquals("write fails", flushFailure.getMessage());
        assertEquals(1, flushFailure.getSuppressed().length);
        assertEquals("close fails", flushFailure.getSuppressed()[0].getMessage());
    }
}
