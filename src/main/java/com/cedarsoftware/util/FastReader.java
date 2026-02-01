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

    private void fill() {
        // Once EOF is reached, avoid re-reading.
        if (limit == -1) {
            return;
        }
        if (position >= limit) {
            try {
                limit = in.read(buf, 0, bufferSize);
            } catch (IOException e) {
                ExceptionUtilities.uncheckedThrow(e);
            }
            if (limit > 0) {
                position = 0;
            }
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
    public int readUntil(char[] dest, int off, int maxLen, char delim1, char delim2) {
        if (in == null) {
            ExceptionUtilities.uncheckedThrow(new IOException("in is null"));
        }

        int totalRead = 0;

        // Copy member variables to locals for faster loop access (avoids repeated field loads)
        final char[] localPushbackBuffer = pushbackBuffer;
        final int localPushbackBufferSize = pushbackBufferSize;
        int localPushbackPosition = pushbackPosition;
        final char[] localBuf = buf;
        int localPosition = position;
        int localLimit = limit;

        // First, drain any pushback buffer
        while (localPushbackPosition < localPushbackBufferSize && totalRead < maxLen) {
            char c = localPushbackBuffer[localPushbackPosition];
            if (c == delim1 || c == delim2) {
                // Found delimiter in pushback - don't consume it
                pushbackPosition = localPushbackPosition;  // Write back before return
                return totalRead > 0 ? totalRead : 0;
            }
            dest[off++] = c;
            localPushbackPosition++;
            totalRead++;
        }
        pushbackPosition = localPushbackPosition;  // Write back after pushback loop

        // Now read from main buffer
        while (totalRead < maxLen) {
            // Write back position before fill() since fill() may modify position/limit
            position = localPosition;
            fill();
            // Re-read after fill() since it may have changed position and limit
            localPosition = position;
            localLimit = limit;

            if (localLimit == -1) {
                // EOF reached
                return totalRead > 0 ? totalRead : -1;
            }

            // Scan current buffer for delimiters
            while (localPosition < localLimit && totalRead < maxLen) {
                char c = localBuf[localPosition];
                if (c == delim1 || c == delim2) {
                    // Found delimiter - don't consume it
                    position = localPosition;  // Write back before return
                    return totalRead;
                }
                dest[off++] = c;
                localPosition++;
                totalRead++;
            }
        }

        position = localPosition;  // Write back after main loop
        return totalRead;
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
