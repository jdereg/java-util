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
public class FastReader extends Reader {
    private Reader in;
    private final char[] buf;
    private final int bufferSize;
    private final int pushbackBufferSize;
    private int position; // Current position in the buffer
    private int limit; // Number of characters currently in the buffer
    private final char[] pushbackBuffer;
    private int pushbackPosition; // Current position in the pushback buffer
    private int line = 1;
    private int col = 0;

    public FastReader(Reader in) {
        this(in, 16384, 16);
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

    private void fill() throws IOException {
        if (position >= limit) {
            limit = in.read(buf, 0, bufferSize);
            if (limit > 0) {
                position = 0;
            }
        }
    }

    public void pushback(char ch) throws IOException {
        if (pushbackPosition == 0) {
            throw new IOException("Pushback buffer overflow");
        }
        pushbackBuffer[--pushbackPosition] = ch;
        if (ch == 0x0a) {
            line--;
        }
        else {
            col--;
        }
    }

    protected void movePosition(char ch)
    {
        if (ch == 0x0a) {
            line++;
            col = 0;
        }
        else {
            col++;
        }
    }

    @Override
    public int read() throws IOException {
        if (in == null) {
            throw new IOException("FastReader stream is closed.");
        }
        char ch;
        if (pushbackPosition < pushbackBufferSize) {
            ch = pushbackBuffer[pushbackPosition++];
            movePosition(ch);
            return ch;
        }

        fill();
        if (limit == -1) {
            return -1;
        }

        ch = buf[position++];
        movePosition(ch);
        return ch;
    }

    public int read(char[] cbuf, int off, int len) throws IOException {
        if (in == null) {
            throw new IOException("FastReader stream is closed.");
        }
        int bytesRead = 0;

        while (len > 0) {
            int available = pushbackBufferSize - pushbackPosition;
            if (available > 0) {
                int toRead = Math.min(available, len);
                System.arraycopy(pushbackBuffer, pushbackPosition, cbuf, off, toRead);
                pushbackPosition += toRead;
                off += toRead;
                len -= toRead;
                bytesRead += toRead;
            } else {
                fill();
                if (limit == -1) {
                    return bytesRead > 0 ? bytesRead : -1;
                }
                int toRead = Math.min(limit - position, len);
                System.arraycopy(buf, position, cbuf, off, toRead);
                position += toRead;
                off += toRead;
                len -= toRead;
                bytesRead += toRead;
            }
        }

        return bytesRead;
    }

    public void close() throws IOException {
        if (in != null) {
            in.close();
            in = null;
        }
    }

    public int getLine()
    {
        return line;
    }

    public int getCol()
    {
        return col;
    }

    public String getLastSnippet()
    {
        StringBuilder s = new StringBuilder();
        for (int i=0; i < position; i++)
        {
            s.append(buf[i]);
        }
        return s.toString();
    }
}
