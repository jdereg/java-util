package com.cedarsoftware.util;

import java.io.IOException;
import java.io.Reader;

/**
 * Buffered, Pushback, Reader that does not use synchronization.   Much faster than the JDK variants because
 * they use synchronization.  Typically, this class is used with a separate instance per thread, so
 * synchronization is not needed.
 * <p>
 * Pushed-back characters are stored in the same array as the buffered input, just before the next character to
 * read. Each refill leaves room at the front of the array for the requested pushback capacity, so that much
 * pushback is always possible; between refills, characters already read make more room. A read therefore never
 * checks a separate pushback buffer first.
 * <p>
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
public final class FastReader extends Reader {
    /**
     * Returned by borrowed-slice methods when the requested token/line is not
     * fully available in the current internal buffer and the caller should fall
     * back to the copying API.
     */
    public static final int COPY_REQUIRED = Integer.MIN_VALUE;
    private static final int MAX_CONSECUTIVE_ZERO_READS = 3;
    private Reader in;
    private final char[] buf;     // [0, reserve): room for pushback after a refill; [reserve, buf.length): input
    private final int reserve;    // the pushback capacity guaranteed right after a refill
    private int position;         // the next character to return
    private int limit;            // one past the last valid character
    private boolean eof;          // the underlying reader has returned -1 (sticky)
    private BufferSlice activeBorrowedSlice;
    // Line/col tracking removed for performance - use getLastSnippet() for error context

    /**
     * Borrowed view of a contiguous range inside {@link FastReader}'s internal buffers.
     * The contents are valid only until {@link #release()} or the next read operation on
     * the same reader, whichever comes first.
     * <p>
     * Callers must consume or copy the slice contents, then call {@link #release()}
     * before invoking any other {@link FastReader} method that reads, pushes back, or
     * closes the reader. With assertions enabled, {@code FastReader} verifies this
     * lifecycle and fails fast if a borrowed slice is left outstanding.
     */
    public static final class BufferSlice {
        private char[] buffer;
        private int offset;
        private int length;
        private FastReader reader;
        private boolean released = true;

        public char[] getBuffer() {
            assert isAccessibleForDebug() : "BufferSlice is not active; consume borrowed data before release";
            return buffer;
        }

        public int getOffset() {
            assert isAccessibleForDebug() : "BufferSlice is not active; consume borrowed data before release";
            return offset;
        }

        public int getLength() {
            assert isAccessibleForDebug() : "BufferSlice is not active; consume borrowed data before release";
            return length;
        }

        /**
         * Mark this borrowed slice as consumed. Call after copying or otherwise
         * materializing the borrowed contents, and before the next FastReader operation.
         */
        public void release() {
            assert releaseForDebug();
        }

        private void set(char[] buffer, int offset, int length) {
            this.buffer = buffer;
            this.offset = offset;
            this.length = length;
        }

        private boolean attachForDebug(FastReader reader) {
            this.reader = reader;
            this.released = false;
            return true;
        }

        private boolean releaseForDebug() {
            released = true;
            FastReader owner = reader;
            if (owner != null && owner.activeBorrowedSlice == this) {
                owner.activeBorrowedSlice = null;
            }
            return true;
        }

        private boolean isAccessibleForDebug() {
            return !released;
        }
    }

    /**
     * Create a reader with an 8,192-character buffer and room for 16 pushed-back characters.
     *
     * @param in the reader to read from
     */
    public FastReader(Reader in) {
        this(in, 8192, 16);
    }

    /**
     * Create a reader with the given buffer size and pushback capacity.
     *
     * @param in                 the reader to read from
     * @param bufferSize         characters read from {@code in} at a time
     * @param pushbackBufferSize characters that can always be pushed back (more often can)
     */
    public FastReader(Reader in, int bufferSize, int pushbackBufferSize) {
        super(in);
        if (bufferSize <= 0 || pushbackBufferSize < 0 || bufferSize > Integer.MAX_VALUE - 8 - pushbackBufferSize) {
            throw new IllegalArgumentException("bufferSize must be positive, pushbackBufferSize must be non-negative");
        }
        this.in = in;
        this.reserve = pushbackBufferSize;
        this.buf = new char[pushbackBufferSize + bufferSize];
        this.position = reserve;
        this.limit = reserve;
    }

    /**
     * Create reader using caller-provided buffers. The buffer array is used directly (no copy) when it is longer
     * than the pushback capacity; its first {@code pushbackBuffer.length} characters are then kept for pushback,
     * the rest for input. Otherwise a buffer of both lengths is allocated. The pushback array itself is used only
     * for its length.
     *
     * @param in             the reader to read from
     * @param buffer         the array to buffer input in
     * @param pushbackBuffer an array whose length is the pushback capacity
     */
    public FastReader(Reader in, char[] buffer, char[] pushbackBuffer) {
        super(in);
        if (buffer == null || buffer.length == 0) {
            throw new IllegalArgumentException("buffer must be non-null and non-empty");
        }
        if (pushbackBuffer == null) {
            throw new IllegalArgumentException("pushbackBuffer must be non-null");
        }
        this.in = in;
        this.reserve = pushbackBuffer.length;
        // Use the caller's array when it leaves room for data after the pushback reserve.
        this.buf = buffer.length > reserve ? buffer : new char[reserve + buffer.length];
        this.position = reserve;
        this.limit = reserve;
    }

    private boolean noActiveBorrowedSliceForDebug() {
        BufferSlice slice = activeBorrowedSlice;
        return slice == null || slice.released;
    }

    private boolean borrowedSliceCreatedForDebug(BufferSlice slice) {
        activeBorrowedSlice = slice;
        return slice.attachForDebug(this);
    }

    /**
     * Refills the buffer from the underlying reader, after the reserved pushback room. Call only when nothing is
     * unread (position at limit) and the end of input has not been reached.
     *
     * @return true when characters were read; false (and the end of input is recorded) when there are none
     */
    private boolean refill() {
        try {
            int zeroReads = 0;
            int n;
            while ((n = in.read(buf, reserve, buf.length - reserve)) == 0) {
                if (++zeroReads >= MAX_CONSECUTIVE_ZERO_READS) {
                    throw new IOException("Underlying Reader repeatedly returned 0 characters");
                }
            }
            if (n < 0) {
                eof = true;
                return false;
            }
            position = reserve;
            limit = reserve + n;
            return true;
        } catch (IOException e) {
            ExceptionUtilities.uncheckedThrow(e);
            return false;
        }
    }

    /**
     * Push a character back, to be read next. At least the pushback capacity given at construction can always
     * be pushed back; between refills more can, since characters already read make room.
     *
     * @param ch the character to push back
     */
    public void pushback(char ch) {
        int pos = position;
        if (pos == 0) {
            ExceptionUtilities.uncheckedThrow(new IOException("Pushback buffer is full"));
        }
        assert noActiveBorrowedSliceForDebug() :
                "FastReader.BufferSlice must be released before pushback()";
        buf[--pos] = ch;
        position = pos;
    }

    @Override
    public int read() {
        if (in == null) {
            ExceptionUtilities.uncheckedThrow(new IOException("in is null"));
        }
        assert noActiveBorrowedSliceForDebug() :
                "FastReader.BufferSlice must be released before read()";
        int pos = position;
        if (pos < limit) {
            position = pos + 1;
            return buf[pos];
        }
        return readSlow();
    }

    private int readSlow() {
        if (eof || !refill()) {
            return -1;
        }
        return buf[position++];
    }

    /**
     * Reads up to {@code len} characters. Once it has some, it reads the underlying reader again only while that
     * reader is {@code ready()}: like BufferedReader, it does not block for more when it already has characters to
     * return (on a pipe or a socket that would wait for input that may not come).
     */
    @Override
    public int read(char[] cbuf, int off, int len) {
        if (in == null) {
            ExceptionUtilities.uncheckedThrow(new IOException("inputReader is null"));
        }
        if ((off < 0) || (off > cbuf.length) || (len < 0) ||
                ((off + len) > cbuf.length) || ((off + len) < 0)) {
            throw new IndexOutOfBoundsException();
        }
        if (len == 0) {
            return 0;
        }
        assert noActiveBorrowedSliceForDebug() :
                "FastReader.BufferSlice must be released before read(char[], int, int)";
        int n = 0;
        while (true) {
            int avail = limit - position;
            if (avail > 0) {
                int k = Math.min(avail, len - n);
                System.arraycopy(buf, position, cbuf, off + n, k);
                position += k;
                n += k;
                if (n == len) {
                    return n;
                }
            }
            // Buffer is empty. Like BufferedReader: once something was read, do not block for more.
            if (eof) {
                break;
            }
            if (n > 0) {
                try {
                    if (!in.ready()) {
                        break;
                    }
                } catch (IOException e) {
                    ExceptionUtilities.uncheckedThrow(e);
                }
            }
            if (!refill()) {
                break;
            }
        }
        return n > 0 ? n : -1;
    }

    /**
     * Tells whether a read will not block: characters are buffered or pushed back, or the underlying reader is
     * ready.
     */
    @Override
    public boolean ready() throws IOException {
        if (in == null) {
            throw new IOException("in is null");
        }
        return position < limit || (!eof && in.ready());
    }

    /**
     * Reads characters into the destination array until one of the two delimiter characters is found.
     * The delimiter character is NOT consumed - it remains available for the next read() call.
     * This method is optimized for scanning strings where we want to read until we hit a quote or backslash.
     *
     * @param dest the destination buffer to read characters into
     * @param off the offset in the destination buffer to start writing
     * @param maxLen the maximum number of characters to read
     * @param delim1 first delimiter character to stop at (typically quote char)
     * @param delim2 second delimiter character to stop at (typically backslash)
     * @return the number of characters read (not including delimiter), or -1 if EOF reached before any chars read
     */
    public int readUntil(final char[] dest, int off, int maxLen, char delim1, char delim2) {
        if (in == null) {
            ExceptionUtilities.uncheckedThrow(new IOException("in is null"));
        }
        assert noActiveBorrowedSliceForDebug() :
                "FastReader.BufferSlice must be released before readUntil()";
        final char[] b = buf;
        int pos = position;
        int lim = limit;
        if (pos >= lim) {
            if (eof || !refill()) {
                return -1;
            }
            pos = position;
            lim = limit;
        }
        int total = 0;
        while (total < maxLen) {
            if (pos >= lim) {
                position = pos;
                if (eof || !refill()) {
                    return total > 0 ? total : -1;
                }
                pos = position;
                lim = limit;
            }
            int end = pos + Math.min(maxLen - total, lim - pos);
            int scan = pos;
            do {
                char c = b[scan];
                if (c == delim1 || c == delim2) {
                    break;
                }
            } while (++scan < end);
            int n = scan - pos;
            if (n > 0) {
                System.arraycopy(b, pos, dest, off, n);
                off += n;
                total += n;
            }
            position = scan;
            if (scan < end) {
                return total;
            }
            pos = scan;
        }
        return total;
    }

    /**
     * Borrow a contiguous slice from the internal buffer up to either delimiter.
     * <p>
     * On success, the delimiter is not consumed and the returned length matches
     * {@code slice.getLength()}. The caller must consume or copy the borrowed
     * contents and call {@link BufferSlice#release()} before the next read operation
     * on this reader. Pushed-back characters are part of the buffer and can be borrowed.
     * If the requested range crosses the current buffer boundary before a delimiter or
     * {@code maxLen} is reached, this method returns {@link #COPY_REQUIRED} without
     * consuming any characters; callers should then use
     * {@link #readUntil(char[], int, int, char, char)}.
     *
     * @param slice destination for the borrowed buffer, offset, and length
     * @param maxLen maximum number of characters to borrow
     * @param delim1 first delimiter character to stop at
     * @param delim2 second delimiter character to stop at
     * @return borrowed length, -1 on EOF before any chars, or {@link #COPY_REQUIRED}
     */
    public int readUntilBorrowed(final BufferSlice slice, int maxLen, char delim1, char delim2) {
        if (in == null) {
            ExceptionUtilities.uncheckedThrow(new IOException("in is null"));
        }
        assert noActiveBorrowedSliceForDebug() :
                "FastReader.BufferSlice must be released before readUntilBorrowed()";
        int pos = position;
        int lim = limit;
        if (pos >= lim) {
            if (eof || !refill()) {
                return -1;
            }
            pos = position;
            lim = limit;
        }
        int end = pos + Math.min(maxLen, lim - pos);
        final char[] b = buf;
        int scan = pos;
        while (scan < end) {
            char c = b[scan];
            if (c == delim1 || c == delim2) {
                int len = scan - pos;
                slice.set(b, pos, len);
                assert borrowedSliceCreatedForDebug(slice);
                position = scan;
                return len;
            }
            scan++;
        }
        if (scan - pos == maxLen) {
            slice.set(b, pos, maxLen);
            assert borrowedSliceCreatedForDebug(slice);
            position = scan;
            return maxLen;
        }
        return COPY_REQUIRED;
    }

    /**
     * Reads a complete line into dest, handling \n, \r, and \r\n line endings.
     * The line ending is consumed but NOT included in the output.
     * <p>
     * Returns the number of characters in the line (excluding the line ending).
     * If maxLen is reached before a line ending is found, returns maxLen and the
     * line ending is NOT consumed — the caller should grow the buffer and call again.
     * Returns -1 on EOF with no data read. A \r ending a buffer fill is followed by a
     * look at the next fill for its \n, which takes none of the pushback capacity.
     * <p>
     * Uses a {@code c <= '\r'} range guard so that printable characters (the vast
     * majority) require only one comparison per character instead of two.
     *
     * @param dest   destination buffer
     * @param off    offset in dest to start writing
     * @param maxLen maximum number of characters to read
     * @return line length (excluding ending), or -1 on EOF
     */
    public int readLine(final char[] dest, int off, int maxLen) {
        if (in == null) {
            ExceptionUtilities.uncheckedThrow(new IOException("in is null"));
        }
        assert noActiveBorrowedSliceForDebug() :
                "FastReader.BufferSlice must be released before readLine()";
        final char[] b = buf;
        int pos = position;
        int lim = limit;
        int total = 0;
        while (total < maxLen) {
            if (pos >= lim) {
                position = pos;
                if (eof || !refill()) {
                    return total > 0 ? total : -1;
                }
                pos = position;
                lim = limit;
            }
            int end = pos + Math.min(maxLen - total, lim - pos);
            int scan = pos;
            while (scan < end) {
                char c = b[scan];
                if (c <= '\r' && (c == '\n' || c == '\r')) {
                    int n = scan - pos;
                    if (n > 0) {
                        System.arraycopy(b, pos, dest, off, n);
                        total += n;
                    }
                    scan++;
                    if (c == '\r') {
                        if (scan >= lim) {
                            // '\n' may start the next fill; nothing is unread, so refilling is safe.
                            position = scan;
                            if (!eof && refill() && buf[position] == '\n') {
                                position++;
                            }
                            return total;
                        }
                        if (b[scan] == '\n') {
                            scan++;
                        }
                    }
                    position = scan;
                    return total;
                }
                scan++;
            }
            int n = scan - pos;
            if (n > 0) {
                System.arraycopy(b, pos, dest, off, n);
                off += n;
                total += n;
            }
            pos = scan;
        }
        position = pos;
        return total;
    }

    /**
     * Borrow a complete line from the current internal buffer without copying.
     * <p>
     * The line ending is consumed but not included in the borrowed slice. The slice
     * must be consumed or copied and then released via {@link BufferSlice#release()}
     * before the next read operation on this reader. If the line crosses the current
     * buffer boundary, or CRLF handling would require looking into the next buffer,
     * this method returns {@link #COPY_REQUIRED} without consuming any characters;
     * callers should then use {@link #readLine(char[], int, int)}.
     *
     * @param slice destination for the borrowed buffer, offset, and length
     * @return line length, -1 on EOF before any chars, or {@link #COPY_REQUIRED}
     */
    public int readLineBorrowed(final BufferSlice slice) {
        if (in == null) {
            ExceptionUtilities.uncheckedThrow(new IOException("in is null"));
        }
        assert noActiveBorrowedSliceForDebug() :
                "FastReader.BufferSlice must be released before readLineBorrowed()";
        int pos = position;
        int lim = limit;
        if (pos >= lim) {
            if (eof || !refill()) {
                return -1;
            }
            pos = position;
            lim = limit;
        }
        final char[] b = buf;
        int scan = pos;
        while (scan < lim) {
            char c = b[scan];
            if (c <= '\r' && (c == '\n' || c == '\r')) {
                int after = scan + 1;
                if (c == '\r') {
                    if (after >= lim) {
                        return COPY_REQUIRED;
                    }
                    if (b[after] == '\n') {
                        after++;
                    }
                }
                int len = scan - pos;
                slice.set(b, pos, len);
                assert borrowedSliceCreatedForDebug(slice);
                position = after;
                return len;
            }
            scan++;
        }
        return COPY_REQUIRED;
    }

    @Override
    public void close() {
        assert noActiveBorrowedSliceForDebug() :
                "FastReader.BufferSlice must be released before close()";
        if (in != null) {
            try {
                in.close();
            } catch (IOException e) {
                ExceptionUtilities.uncheckedThrow(e);
            }
            in = null;
        }
    }

    /**
     * @return 0 - line tracking removed for performance. Use getLastSnippet() for error context.
     * @deprecated Line tracking removed for performance optimization
     */
    @Deprecated
    public int getLine() {
        return 0;
    }

    /**
     * @return 0 - column tracking removed for performance. Use getLastSnippet() for error context.
     * @deprecated Column tracking removed for performance optimization
     */
    @Deprecated
    public int getCol() {
        return 0;
    }

    /**
     * Returns the last portion of the buffer that has been read, useful for error context.
     * @return up to the last 200 characters read from the current buffer
     */
    public String getLastSnippet() {
        int start = Math.max(reserve, position - 200);
        return position > start ? new String(buf, start, position - start) : "";
    }
}
