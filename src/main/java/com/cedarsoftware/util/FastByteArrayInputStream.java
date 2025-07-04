package com.cedarsoftware.util;

import java.io.IOException;
import java.io.InputStream;

/**
 * Faster version of ByteArrayInputStream that does not have synchronized methods.
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
public class FastByteArrayInputStream extends InputStream {

    private final byte[] buffer;
    private int pos;
    private int mark = 0;
    private final int count;

    public FastByteArrayInputStream(byte[] buf) {
        this.buffer = buf;
        this.pos = 0;
        this.count = buf.length;
    }

    public int read() {
        return (pos < count) ? (buffer[pos++] & 0xff) : -1;
    }

    @Override
    public int read(byte[] b, int off, int len) {
        if (b == null) {
            throw new NullPointerException();
        } else if (off < 0 || len < 0 || len > b.length - off) {
            throw new IndexOutOfBoundsException();
        } else if (pos >= count) {
            return -1;
        }

        int avail = count - pos;
        if (len > avail) {
            len = avail;
        }
        if (len <= 0) {
            return 0;
        }
        System.arraycopy(buffer, pos, b, off, len);
        pos += len;
        return len;
    }

    @Override
    public long skip(long n) {
        //  is this intentionally a long?
        long k = count - pos;
        if (n < k) {
            k = n < 0 ? 0 : n;
        }

        pos += k;
        return k;
    }

    @Override
    public int available() {
        return count - pos;
    }

    @Override
    public void mark(int readLimit) {
        mark = pos;
    }

    @Override
    public void reset() {
        pos = mark;
    }

    @Override
    public boolean markSupported() {
        return true;
    }

    @Override
    public void close() {
        // Optionally implement if resources need to be released
    }
}
