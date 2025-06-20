package com.cedarsoftware.util;

import java.io.IOException;
import java.io.Writer;

/**
 * Buffered Writer that does not use synchronization.   Much faster than the JDK variants because
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
public class FastWriter extends Writer {
    private static final int DEFAULT_BUFFER_SIZE = 8192;

    private Writer out;
    private char[] cb;
    private int nextChar;

    public FastWriter(Writer out) {
        this(out, DEFAULT_BUFFER_SIZE);
    }

    public FastWriter(Writer out, int bufferSize) {
        super(out);
        if (bufferSize <= 0) {
            throw new IllegalArgumentException("Buffer size <= 0");
        }
        this.out = out;
        this.cb = new char[bufferSize];
        this.nextChar = 0;
    }

    private void flushBuffer() {
        if (nextChar == 0) {
            return;
        }
        try {
            out.write(cb, 0, nextChar);
        } catch (IOException e) {
            ExceptionUtilities.uncheckedThrow(e);
        }
        nextChar = 0;
    }

    @Override
    public void write(int c) {
        if (out == null) {
            ExceptionUtilities.uncheckedThrow(new IOException("FastWriter stream is closed"));
        }
        if (nextChar + 1 >= cb.length) {
            flushBuffer();
        }
        cb[nextChar++] = (char) c;
    }

    @Override
    public void write(char[] cbuf, int off, int len) {
        if (out == null) {
            ExceptionUtilities.uncheckedThrow(new IOException("FastWriter stream is closed"));
        }
        if ((off < 0) || (off > cbuf.length) || (len < 0) ||
                ((off + len) > cbuf.length) || ((off + len) < 0)) {
            throw new IndexOutOfBoundsException();
        } else if (len == 0) {
            return;
        }
        if (len >= cb.length) {
            // If the request length exceeds the size of the output buffer,
            // flush the buffer and then write the data directly.
            flushBuffer();
            try {
                out.write(cbuf, off, len);
            } catch (IOException e) {
                ExceptionUtilities.uncheckedThrow(e);
            }
            return;
        }
        if (len > cb.length - nextChar) {
            flushBuffer();
        }
        System.arraycopy(cbuf, off, cb, nextChar, len);
        nextChar += len;
    }

    @Override
    public void write(String str, int off, int len) {
        if (out == null) {
            ExceptionUtilities.uncheckedThrow(new IOException("FastWriter stream is closed"));
        }

        // Return early for empty strings
        if (len == 0) {
            return;
        }

        // Fast path for short strings that fit in buffer
        if (nextChar + len <= cb.length) {
            str.getChars(off, off + len, cb, nextChar);
            nextChar += len;
            if (nextChar == cb.length) {
                flushBuffer();
            }
            return;
        }

        // Medium path: fill what we can, flush, then continue
        int available = cb.length - nextChar;
        if (available > 0) {
            str.getChars(off, off + available, cb, nextChar);
            off += available;
            len -= available;
            nextChar = cb.length;
            flushBuffer();
        }

        // Write full buffer chunks directly - ensures buffer alignment
        try {
            while (len >= cb.length) {
                str.getChars(off, off + cb.length, cb, 0);
                off += cb.length;
                len -= cb.length;
                out.write(cb, 0, cb.length);
            }
        } catch (IOException e) {
            ExceptionUtilities.uncheckedThrow(e);
        }

        // Write final fragment into buffer (won't overflow by definition)
        if (len > 0) {
            str.getChars(off, off + len, cb, 0);
            nextChar = len;
        }
    }

    @Override
    public void flush() {
        flushBuffer();
        try {
            out.flush();
        } catch (IOException e) {
            ExceptionUtilities.uncheckedThrow(e);
        }
    }

    @Override
    public void close() {
        if (out == null) {
            return;
        }
        try {
            flushBuffer();
        } finally {
            try {
                out.close();
            } catch (IOException e) {
                ExceptionUtilities.uncheckedThrow(e);
            }
            out = null;
            cb = null;
        }
    }
}
