package com.cedarsoftware.util;

import java.io.IOException;
import java.io.Reader;

/**
 * Buffered, Pushback, Reader that does not use synchronization.   Much faster than the JDK variants because
 * they use synchronization.  Typically, this class is used with a separate instance per thread, so
 * synchronization is not needed.
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
    private final char[] buf;
    private final int bufferSize;
    private final int pushbackBufferSize;
    private int position; // Current position in the buffer
    private int limit;    // Number of characters currently in the buffer, or -1 for EOF
    private final char[] pushbackBuffer;
    private int pushbackPosition; // Current position in the pushback buffer
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

    public FastReader(Reader in) {
        this(in, 8192, 16);
    }

    public FastReader(Reader in, int bufferSize, int pushbackBufferSize) {
        super(in);
        if (bufferSize <= 0 || pushbackBufferSize < 0) {
            throw new IllegalArgumentException("bufferSize must be positive, pushbackBufferSize must be non-negative");
        }
        this.in = in;
        this.bufferSize = bufferSize;
        this.pushbackBufferSize = pushbackBufferSize;
        this.buf = new char[bufferSize];
        this.pushbackBuffer = new char[pushbackBufferSize];
        this.position = 0;
        this.limit = 0;
        this.pushbackPosition = pushbackBufferSize; // Start from the end of pushbackBuffer
    }

    /**
     * Create reader using caller-provided buffers.
     * Arrays are used directly (no copy).
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
        this.bufferSize = buffer.length;
        this.pushbackBufferSize = pushbackBuffer.length;
        this.buf = buffer;
        this.pushbackBuffer = pushbackBuffer;
        this.position = 0;
        this.limit = 0;
        this.pushbackPosition = this.pushbackBufferSize;
    }

    private boolean noActiveBorrowedSliceForDebug() {
        BufferSlice slice = activeBorrowedSlice;
        return slice == null || slice.released;
    }

    private boolean borrowedSliceCreatedForDebug(BufferSlice slice) {
        activeBorrowedSlice = slice;
        return slice.attachForDebug(this);
    }

    private void fill() {
        final int lim = limit;
        if (lim != -1 && position >= lim) {
            refill();
        }
    }

    /**
     * Unconditionally refills the buffer from the underlying reader.
     * Caller must ensure this is only called when a refill is actually needed
     * (i.e., buffer is exhausted and EOF has not been reached).
     *
     * @return the new limit (number of chars read, or -1 for EOF)
     */
    private int refill() {
        try {
            int zeroReads = 0;
            int n;
            while (true) {
                n = in.read(buf, 0, bufferSize);
                if (n != 0) {
                    break;
                }
                if (++zeroReads >= MAX_CONSECUTIVE_ZERO_READS) {
                    throw new IOException("Underlying Reader repeatedly returned 0 characters");
                }
            }
            limit = n;
            if (n > 0) {
                position = 0;
            }
            return n;
        } catch (IOException e) {
            ExceptionUtilities.uncheckedThrow(e);
            return -1; // unreachable, but satisfies compiler
        }
    }

    public void pushback(char ch) {
        if (pushbackPosition == 0) {
            ExceptionUtilities.uncheckedThrow(new IOException("Pushback buffer is full"));
        }
        assert noActiveBorrowedSliceForDebug() :
                "FastReader.BufferSlice must be released before pushback()";
        pushbackBuffer[--pushbackPosition] = ch;
    }

    @Override
    public int read() {
        if (in == null) {
            ExceptionUtilities.uncheckedThrow(new IOException("in is null"));
        }
        assert noActiveBorrowedSliceForDebug() :
                "FastReader.BufferSlice must be released before read()";

        // First, serve from pushback buffer if available
        if (pushbackPosition < pushbackBufferSize) {
            return pushbackBuffer[pushbackPosition++];
        }

        fill();
        if (limit == -1) {
            return -1;
        }

        return buf[position++];
    }

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

        int charsRead = 0;

        while (len > 0) {
            // Consume from pushback buffer first
            int availableFromPushback = pushbackBufferSize - pushbackPosition;
            if (availableFromPushback > 0) {
                int toRead = Math.min(availableFromPushback, len);
                System.arraycopy(pushbackBuffer, pushbackPosition, cbuf, off, toRead);
                pushbackPosition += toRead;
                off += toRead;
                len -= toRead;
                charsRead += toRead;
            } else {
                // Consume from main buffer
                fill();
                if (limit == -1) {
                    // EOF: return what we've got, or -1 if we have nothing
                    return charsRead > 0 ? charsRead : -1;
                }

                int availableFromMain = limit - position;
                if (availableFromMain <= 0) {
                    // Shouldn't normally happen if fill() behaves, but guard anyway
                    return charsRead > 0 ? charsRead : -1;
                }

                int toRead = Math.min(availableFromMain, len);
                System.arraycopy(buf, position, cbuf, off, toRead);
                position += toRead;
                off += toRead;
                len -= toRead;
                charsRead += toRead;
            }
        }

        return charsRead;
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

        int totalRead = 0;
        final char[] locBuf = buf;

        // Drain pushback buffer first (rare path — empty in most calls). Cache
        // pushbackBufferSize into pbSize so the inner while-condition check does not
        // re-issue GETFIELD per iteration. pushbackBuffer is NOT cached — it is
        // read at one source location, so a local would add a stack variable with
        // no second reader to amortize it against.
        int pbPos = pushbackPosition;
        final int pbSize = pushbackBufferSize;
        if (pbPos < pbSize) {
            while (pbPos < pbSize && totalRead < maxLen) {
                char c = pushbackBuffer[pbPos];
                if (c == delim1 || c == delim2) {
                    // Found delimiter in pushback — don't consume it.
                    pushbackPosition = pbPos;
                    return totalRead;
                }
                dest[off++] = c;
                pbPos++;
                totalRead++;
            }
            pushbackPosition = pbPos;
        }

        // Cache position / limit into locals. These are the ONLY fields fill() mutates,
        // so every assignment from `limit` must be followed by enough reads to amortize
        // the local-store cost. The refill check is placed at the TOP of the scan loop
        // so that each in-loop `locLimit = limit;` is immediately used TWICE in the
        // same iteration (EOF check and `end = locLimit` boundary init) — no reliance
        // on cross-iteration loop-back to hit the second read.
        int pos = position;
        int locLimit = limit;

        // Entry refill-and-EOF guard: preserves the "return -1 if EOF reached before
        // any chars read" contract even when maxLen == 0 (which would otherwise skip
        // the while loop entirely and return 0). The locLimit assignment inside this
        // block is used twice — the EOF check here, then (if we don't return) the
        // `end = locLimit` line in the first scan iteration.
        if (pos >= locLimit) {
            fill();
            locLimit = limit;
            if (locLimit == -1) {
                return totalRead > 0 ? totalRead : -1;
            }
            pos = position;
        }

        while (totalRead < maxLen) {
            // In-loop refill when the current buffer slice is exhausted. Kept at the
            // top of the loop so the `locLimit = limit;` assignment below is used
            // twice within this same iteration (EOF check + end init) and NOT reliant
            // on loop-back.
            if (pos >= locLimit) {
                fill();
                locLimit = limit;
                if (locLimit == -1) {
                    return totalRead > 0 ? totalRead : -1;
                }
                pos = position;
            }

            // Compute scan boundary: end = min(locLimit, pos + maxLen - totalRead)
            int end = locLimit;
            int destEnd = pos + maxLen - totalRead;
            if (destEnd < end) {
                end = destEnd;
            }

            // Tight scan loop — reads only, no writes, no method calls.
            // JIT can keep this entirely in registers without exception-handler interference.
            int scanPos = pos;
            do {
                char c = locBuf[scanPos];
                if (c == delim1 || c == delim2) break;
            } while (++scanPos < end);

            // Bulk-copy scanned characters
            int copyLen = scanPos - pos;
            if (copyLen > 0) {
                System.arraycopy(locBuf, pos, dest, off, copyLen);
                off += copyLen;
                totalRead += copyLen;
            }

            // Write back position once — needed by fill()'s refill check on the next
            // iteration and by the caller's next read after a delimiter match.
            position = scanPos;

            if (scanPos < end) {
                // Found delimiter — don't consume it.
                return totalRead;
            }

            // Scan hit `end` without a delimiter. If `end == destEnd` we've also hit
            // maxLen, and the `while` condition will exit; no refill happens this call
            // (previously we eagerly called fill() here — wasted work when the caller
            // only wanted a bounded read). If `end == locLimit`, pos will be >= locLimit
            // on the next iteration and the top-of-loop refill fires.
            pos = scanPos;
        }

        // Loop exit: totalRead >= maxLen. position was written inside the loop body
        // (to scanPos), so no bottom write-back is needed. If the loop never ran
        // (maxLen == 0, or pushback drain already filled it), pos still equals the
        // entry-time position — no change required.
        return totalRead;
    }

    /**
     * Borrow a contiguous slice from the internal buffer up to either delimiter.
     * <p>
     * On success, the delimiter is not consumed and the returned length matches
     * {@code slice.getLength()}. The caller must consume or copy the borrowed
     * contents and call {@link BufferSlice#release()} before the next read operation
     * on this reader. If pushback is active or the requested range crosses the
     * current buffer boundary before a delimiter or {@code maxLen} is reached, this
     * method returns {@link #COPY_REQUIRED} without consuming any characters; callers
     * should then use {@link #readUntil(char[], int, int, char, char)}.
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
        if (pushbackPosition < pushbackBufferSize) {
            return COPY_REQUIRED;
        }

        int pos = position;
        int lim = limit;
        if (pos >= lim) {
            if (lim == -1) {
                return -1;
            }
            lim = refill();
            if (lim == -1) {
                return -1;
            }
            pos = position;
        }

        int end = lim;
        int requestedEnd = pos + maxLen;
        if (requestedEnd < end) {
            end = requestedEnd;
        }

        char[] b = buf;
        int scanPos = pos;
        while (scanPos < end) {
            char c = b[scanPos];
            if (c == delim1 || c == delim2) {
                int len = scanPos - pos;
                slice.set(b, pos, len);
                assert borrowedSliceCreatedForDebug(slice);
                position = scanPos;
                return len;
            }
            scanPos++;
        }

        if (scanPos - pos == maxLen) {
            slice.set(b, pos, maxLen);
            assert borrowedSliceCreatedForDebug(slice);
            position = scanPos;
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
     * Returns -1 on EOF with no data read.
     * <p>
     * Optimized for the common case: no pushback active, line fits within the
     * current buffer. Uses a {@code c <= '\r'} range guard so that printable
     * characters (the vast majority) require only one comparison per character
     * instead of two.
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

        int total = 0;

        // --- Drain pushback buffer (rare path — empty in most calls) ---
        // Cache pushbackBufferSize (3 uses: guard, while-check, \r-ahead check) and
        // pushbackBuffer (2 uses: main scan + \r-ahead probe). Wrapping the drain in
        // `if (pbPos < pbSize)` also skips the unconditional `pushbackPosition = pbPos;`
        // PUTFIELD that the original always executed even when the drain didn't run.
        int pbPos = pushbackPosition;
        final int pbSize = pushbackBufferSize;
        if (pbPos < pbSize) {
            final char[] pbBuf = pushbackBuffer;
            while (pbPos < pbSize && total < maxLen) {
                char c = pbBuf[pbPos];
                if (c <= '\r' && (c == '\n' || c == '\r')) {
                    pbPos++;
                    pushbackPosition = pbPos;
                    // Handle \r\n across pushback boundary
                    if (c == '\r') {
                        if (pbPos < pbSize) {
                            if (pbBuf[pbPos] == '\n') {
                                pushbackPosition = pbPos + 1;
                            }
                        } else {
                            // \n might be in main buffer
                            int peek = read();
                            if (peek >= 0 && peek != '\n') pushback((char) peek);
                        }
                    }
                    return total;
                }
                dest[off++] = c;
                pbPos++;
                total++;
            }
            pushbackPosition = pbPos;
        }

        // --- Main buffer scan ---
        // Cache buf / position / limit into locals ONCE. `buf` is never reassigned by
        // refill() (refill reads INTO buf at index 0), so it can be final. `pos` and
        // `lim` are refreshed only after refill() — the sole field mutator on this
        // path. Hoisting these out of the outer-while eliminates 3 per-iteration
        // GETFIELDs (buf, position, limit re-reads) and the 2 GETFIELDs of the old
        // top-of-loop `position >= limit` check, leaving just the two post-refill
        // reads on the rare iteration where refill actually fires.
        final char[] b = buf;
        int pos = position;
        int lim = limit;

        while (total < maxLen) {
            if (pos >= lim) {
                // refill() does not read `position` (it writes pos=0 unconditionally
                // on success), so no write-back is needed before the call.
                if (refill() == -1) {
                    return total > 0 ? total : -1;
                }
                pos = position;
                lim = limit;
            }

            int end = lim;
            int remaining = maxLen - total;
            if (pos + remaining < end) {
                end = pos + remaining;
            }

            // Tight scan loop — single array load per iteration. The outer c <= '\r' check is
            // a performance prefilter (printable chars > 13 need only ONE comparison); only
            // control chars <= 13 (which are rare in typical text) enter the inner equality
            // tests. Eliminating the prior double-read of b[scanPos] removes one potential
            // bounds check per character even when JIT CSE is imperfect.
            int scanPos = pos;
            while (scanPos < end) {
                char c = b[scanPos];
                if (c <= '\r') {
                    if (c == '\n' || c == '\r') {
                        // Found line ending — bulk-copy content, consume ending, return
                        int copyLen = scanPos - pos;
                        if (copyLen > 0) {
                            System.arraycopy(b, pos, dest, off, copyLen);
                            total += copyLen;
                        }
                        scanPos++; // consume the \n or \r
                        // Handle \r\n
                        if (c == '\r') {
                            if (scanPos < lim) {
                                if (b[scanPos] == '\n') scanPos++;
                            } else {
                                // \n might be in next buffer fill
                                position = scanPos;
                                int peek = read();
                                if (peek >= 0 && peek != '\n') pushback((char) peek);
                                return total;
                            }
                        }
                        position = scanPos;
                        return total;
                    }
                }
                scanPos++;
            }

            // No line ending found in this chunk — copy what we have and let the
            // top-of-loop `pos >= lim` check fire the refill on the next iteration.
            int copyLen = scanPos - pos;
            if (copyLen > 0) {
                System.arraycopy(b, pos, dest, off, copyLen);
                off += copyLen;
                total += copyLen;
            }
            pos = scanPos;
        }

        // Loop exited via `total >= maxLen` without finding a terminator — write back
        // the final position so the next reader sees where we stopped.
        position = pos;
        return total;
    }

    /**
     * Borrow a complete line from the current internal buffer without copying.
     * <p>
     * The line ending is consumed but not included in the borrowed slice. The slice
     * must be consumed or copied and then released via {@link BufferSlice#release()}
     * before the next read operation on this reader. If pushback is active, the line
     * crosses the current buffer boundary, or CRLF handling would require looking
     * into the next buffer, this method returns {@link #COPY_REQUIRED} without
     * consuming any characters; callers should then use
     * {@link #readLine(char[], int, int)}.
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
        if (pushbackPosition < pushbackBufferSize) {
            return COPY_REQUIRED;
        }

        int pos = position;
        int lim = limit;
        if (pos >= lim) {
            if (lim == -1) {
                return -1;
            }
            lim = refill();
            if (lim == -1) {
                return -1;
            }
            pos = position;
        }

        char[] b = buf;
        int scanPos = pos;
        while (scanPos < lim) {
            char c = b[scanPos];
            if (c <= '\r' && (c == '\n' || c == '\r')) {
                int afterTerminator = scanPos + 1;
                if (c == '\r') {
                    if (afterTerminator >= lim) {
                        return COPY_REQUIRED;
                    }
                    if (b[afterTerminator] == '\n') {
                        afterTerminator++;
                    }
                }
                int len = scanPos - pos;
                slice.set(b, pos, len);
                assert borrowedSliceCreatedForDebug(slice);
                position = afterTerminator;
                return len;
            }
            scanPos++;
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
        int snippetLength = Math.min(position, 200);
        int start = position - snippetLength;
        return new String(buf, start, snippetLength);
    }

}
