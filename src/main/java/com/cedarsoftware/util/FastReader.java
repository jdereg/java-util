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
            throw new IllegalArgumentException("Buffer sizes must be positive");
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

    public String getLastSnippet() {
        StringBuilder s = new StringBuilder(position);
        for (int i = 0; i < position; i++) {
            s.append(buf[i]);
        }
        return s.toString();
    }

}
