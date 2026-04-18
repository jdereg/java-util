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
    private static final int MAX_CONSECUTIVE_ZERO_READS = 3;
    private Reader in;
    private final char[] buf;
    private final int bufferSize;
    private final int pushbackBufferSize;
    private int position; // Current position in the buffer
    private int limit;    // Number of characters currently in the buffer, or -1 for EOF
    private final char[] pushbackBuffer;
    private int pushbackPosition; // Current position in the pushback buffer
    // Line/col tracking removed for performance - use getLastSnippet() for error context

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

    private void fill() {
        if (limit != -1 && position >= limit) {
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
                    ExceptionUtilities.uncheckedThrow(new IOException("Underlying Reader repeatedly returned 0 characters"));
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
        pushbackBuffer[--pushbackPosition] = ch;
    }

    @Override
    public int read() {
        if (in == null) {
            ExceptionUtilities.uncheckedThrow(new IOException("in is null"));
        }

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

        int totalRead = 0;
        final char[] locBuf = buf;

        // First, drain any pushback buffer
        int pbPos = pushbackPosition;
        while (pbPos < pushbackBufferSize && totalRead < maxLen) {
            char c = pushbackBuffer[pbPos];
            if (c == delim1 || c == delim2) {
                // Found delimiter in pushback - don't consume it
                pushbackPosition = pbPos;
                return totalRead;
            }
            dest[off++] = c;
            pbPos++;
            totalRead++;
        }
        pushbackPosition = pbPos;

        // Ensure buffer has data before entering scan loop.
        // fill() has a try-catch which inhibits JIT optimization of the scan loop,
        // so we keep it out of the hot path — only called here and at loop bottom.
        if (position >= limit) {
            fill();
            if (limit == -1) {
                return totalRead > 0 ? totalRead : -1;
            }
        }

        // Cache member fields into locals for the scan loop.
        // position and limit are only written back at exit points or before refill().
        int pos = position;
        int locLimit = limit;

        while (totalRead < maxLen) {
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

            if (scanPos < end) {
                // Found delimiter — write back position and return
                position = scanPos;
                return totalRead;
            }

            // Buffer exhausted — refill. Write back position before fill() reads it.
            position = scanPos;
            fill();
            locLimit = limit;
            if (locLimit == -1) {
                return totalRead > 0 ? totalRead : -1;
            }
            pos = position;  // fill() resets position to 0
        }

        position = pos;
        return totalRead;
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

        int total = 0;

        // --- Drain pushback buffer (rare path) ---
        int pbPos = pushbackPosition;
        while (pbPos < pushbackBufferSize && total < maxLen) {
            char c = pushbackBuffer[pbPos];
            if (c <= '\r' && (c == '\n' || c == '\r')) {
                pbPos++;
                pushbackPosition = pbPos;
                // Handle \r\n across pushback boundary
                if (c == '\r') {
                    if (pbPos < pushbackBufferSize) {
                        if (pushbackBuffer[pbPos] == '\n') {
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

        // --- Main buffer scan ---
        while (total < maxLen) {
            // Ensure buffer has data
            if (position >= limit) {
                if (refill() == -1) {
                    return total > 0 ? total : -1;
                }
            }

            final char[] b = buf;
            int pos = position;
            final int lim = limit;
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

            // No line ending found in this chunk — copy and continue
            int copyLen = scanPos - pos;
            if (copyLen > 0) {
                System.arraycopy(b, pos, dest, off, copyLen);
                off += copyLen;
                total += copyLen;
            }
            position = scanPos;
        }

        return total;
    }

    @Override
    public void close() {
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
