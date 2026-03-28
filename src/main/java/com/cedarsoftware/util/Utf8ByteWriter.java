package com.cedarsoftware.util;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;

/**
 * A Writer that encodes characters directly to UTF-8 bytes and writes them to an OutputStream,
 * bypassing the OutputStreamWriter → StreamEncoder → OutputStream chain.
 * <p>
 * For ASCII content (99%+ of JSON/TOON data), each character is a single byte store into the
 * output buffer — no intermediate char[] buffering or encoding passes. Multi-byte UTF-8 sequences
 * (2-byte, 3-byte, 4-byte including surrogate pairs) are handled inline.
 * <p>
 * This class does NOT use synchronization, matching {@link FastWriter}'s design. It is intended
 * for single-threaded use with one instance per serialization operation.
 * <p>
 * Drop-in replacement for {@code new FastWriter(new OutputStreamWriter(out, UTF_8))} when
 * the destination is an OutputStream.
 *
 * @author John DeRegnaucourt (jdereg@gmail.com)
 *         <br>
 *         Copyright (c) Cedar Software LLC
 *         <br><br>
 *         Licensed under the Apache License, Version 2.0 (the "License");
 *         you may not use this file except in compliance with the License.
 *         You may obtain a copy of the License at
 *         <br><br>
 *         <a href="http://www.apache.org/licenses/LICENSE-2.0">License</a>
 *         <br><br>
 *         Unless required by applicable law or agreed to in writing, software
 *         distributed under the License is distributed on an "AS IS" BASIS,
 *         WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *         See the License for the specific language governing permissions and
 *         limitations under the License.
 */
public final class Utf8ByteWriter extends Writer {
    private static final int DEFAULT_BUFFER_SIZE = 8192;
    // Margin kept free in buffer to allow multi-byte writes without per-byte flush checks.
    // Worst case: a 4-byte UTF-8 sequence from a surrogate pair.
    private static final int BUFFER_MARGIN = 4;

    private OutputStream out;
    private byte[] buf;
    private int pos;

    public Utf8ByteWriter(OutputStream out) {
        this(out, DEFAULT_BUFFER_SIZE);
    }

    public Utf8ByteWriter(OutputStream out, int bufferSize) {
        super(out);
        if (bufferSize <= BUFFER_MARGIN) {
            throw new IllegalArgumentException("Buffer size must be > " + BUFFER_MARGIN);
        }
        this.out = out;
        this.buf = new byte[bufferSize];
        this.pos = 0;
    }

    /**
     * Create writer using a caller-provided byte buffer.
     * The provided array is used directly (no copy).
     */
    public Utf8ByteWriter(OutputStream out, byte[] buffer) {
        super(out);
        if (buffer == null || buffer.length <= BUFFER_MARGIN) {
            throw new IllegalArgumentException("Buffer must be non-null and length > " + BUFFER_MARGIN);
        }
        this.out = out;
        this.buf = buffer;
        this.pos = 0;
    }

    private void flushBuffer() throws IOException {
        if (pos > 0) {
            out.write(buf, 0, pos);
            pos = 0;
        }
    }

    /**
     * Write a single character. For ASCII (0-127), this is a single byte store.
     */
    @Override
    public void write(int c) throws IOException {
        if (out == null) {
            throw new IOException("Utf8ByteWriter stream is closed");
        }
        if (c < 0x80) {
            buf[pos++] = (byte) c;
            if (pos >= buf.length - BUFFER_MARGIN) {
                flushBuffer();
            }
        } else {
            writeMultiByte(c);
        }
    }

    /**
     * Write a portion of a character array.
     */
    @Override
    public void write(char[] cbuf, int off, int len) throws IOException {
        if (out == null) {
            throw new IOException("Utf8ByteWriter stream is closed");
        }
        int end = off + len;
        for (int i = off; i < end; i++) {
            char c = cbuf[i];
            if (c < 0x80) {
                buf[pos++] = (byte) c;
                if (pos >= buf.length - BUFFER_MARGIN) {
                    flushBuffer();
                }
            } else if (Character.isHighSurrogate(c) && i + 1 < end && Character.isLowSurrogate(cbuf[i + 1])) {
                writeSurrogatePair(c, cbuf[++i]);
            } else {
                writeMultiByte(c);
            }
        }
    }

    /**
     * Write a portion of a string. This is the hot path for JSON/TOON serialization.
     * For ASCII content, the inner loop is a single byte store per character.
     */
    @Override
    public void write(String str, int off, int len) throws IOException {
        if (out == null) {
            throw new IOException("Utf8ByteWriter stream is closed");
        }
        final int end = off + len;
        final byte[] b = buf;
        final int flushAt = b.length - BUFFER_MARGIN;
        int p = pos;

        for (int i = off; i < end; i++) {
            char c = str.charAt(i);
            if (c < 0x80) {
                b[p++] = (byte) c;
                if (p >= flushAt) {
                    pos = p;
                    flushBuffer();
                    p = 0;
                }
            } else if (Character.isHighSurrogate(c) && i + 1 < end && Character.isLowSurrogate(str.charAt(i + 1))) {
                pos = p;
                writeSurrogatePair(c, str.charAt(++i));
                p = pos;
            } else {
                pos = p;
                writeMultiByte(c);
                p = pos;
            }
        }
        pos = p;
    }

    /**
     * Encode a BMP character (0x80-0xFFFF, excluding surrogates) to 2 or 3 UTF-8 bytes.
     */
    private void writeMultiByte(int c) throws IOException {
        if (c < 0x800) {
            // 2-byte: 110xxxxx 10xxxxxx
            buf[pos++] = (byte) (0xC0 | (c >> 6));
            buf[pos++] = (byte) (0x80 | (c & 0x3F));
        } else {
            // 3-byte: 1110xxxx 10xxxxxx 10xxxxxx
            buf[pos++] = (byte) (0xE0 | (c >> 12));
            buf[pos++] = (byte) (0x80 | ((c >> 6) & 0x3F));
            buf[pos++] = (byte) (0x80 | (c & 0x3F));
        }
        if (pos >= buf.length - BUFFER_MARGIN) {
            flushBuffer();
        }
    }

    /**
     * Encode a surrogate pair to 4 UTF-8 bytes (supplementary characters: emoji, rare CJK, etc.)
     */
    private void writeSurrogatePair(char high, char low) throws IOException {
        int codePoint = Character.toCodePoint(high, low);
        // 4-byte: 11110xxx 10xxxxxx 10xxxxxx 10xxxxxx
        buf[pos++] = (byte) (0xF0 | (codePoint >> 18));
        buf[pos++] = (byte) (0x80 | ((codePoint >> 12) & 0x3F));
        buf[pos++] = (byte) (0x80 | ((codePoint >> 6) & 0x3F));
        buf[pos++] = (byte) (0x80 | (codePoint & 0x3F));
        if (pos >= buf.length - BUFFER_MARGIN) {
            flushBuffer();
        }
    }

    @Override
    public void flush() throws IOException {
        if (out == null) {
            return;
        }
        flushBuffer();
        out.flush();
    }

    @Override
    public void close() throws IOException {
        if (out == null) {
            return;
        }
        try {
            flushBuffer();
        } finally {
            try {
                out.close();
            } finally {
                out = null;
                buf = null;
            }
        }
    }

    /**
     * Returns the last portion of the buffer that has been written, useful for error context.
     * Decodes the buffered UTF-8 bytes back to a String.
     *
     * @return up to the last 200 characters of buffered content
     */
    public String getLastSnippet() {
        if (buf == null || pos == 0) {
            return "";
        }
        // Walk backwards to find a safe UTF-8 start position (~200 chars)
        int snippetBytes = Math.min(pos, 600); // ~200 chars × 3 bytes max
        int start = pos - snippetBytes;
        // Align to UTF-8 character boundary (skip continuation bytes 10xxxxxx)
        while (start < pos && (buf[start] & 0xC0) == 0x80) {
            start++;
        }
        return new String(buf, start, pos - start, java.nio.charset.StandardCharsets.UTF_8);
    }
}
