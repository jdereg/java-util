package com.cedarsoftware.util;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;

/**
 * Faster version of ByteArrayOutputStream that does not have synchronized methods and
 * also provides direct access to its internal buffer so that it does not need to be
 * duplicated when read.
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
public class FastByteArrayOutputStream extends OutputStream {

    private byte[] buf;
    private int count;

    public FastByteArrayOutputStream() {
        this(32);
    }

    public FastByteArrayOutputStream(int size) {
        if (size < 0) {
            throw new IllegalArgumentException("Negative initial size: " + size);
        }
        buf = new byte[size];
    }

    private void ensureCapacity(int minCapacity) {
        if (minCapacity - buf.length > 0) {
            grow(minCapacity);
        }
    }

    private void grow(int minCapacity) {
        int oldCapacity = buf.length;
        int newCapacity = oldCapacity << 1;
        if (newCapacity - minCapacity < 0) {
            newCapacity = minCapacity;
        }
        buf = Arrays.copyOf(buf, newCapacity);
    }

    public void write(int b) {
        ensureCapacity(count + 1);
        buf[count] = (byte) b;
        count += 1;
    }

    @Override
    public void write(byte[] b, int off, int len) {
        if ((b == null) || (off < 0) || (len < 0) ||
                (off > b.length) || (off + len > b.length) || (off + len < 0)) {
            throw new IndexOutOfBoundsException();
        }
        ensureCapacity(count + len);
        System.arraycopy(b, off, buf, count, len);
        count += len;
    }

    public void writeBytes(byte[] b) {
        write(b, 0, b.length);
    }

    public void reset() {
        count = 0;
    }

    public byte[] toByteArray() {
        return Arrays.copyOf(buf, count);
    }

    // Backwards compatibility
    public byte[] getBuffer() {
        return Arrays.copyOf(buf, count);
    }

    public int size() {
        return count;
    }

    public String toString() {
        return new String(buf, 0, count);
    }

    public void writeTo(OutputStream out) throws IOException {
        out.write(buf, 0, count);
    }

    @Override
    public void close() throws IOException {
        // No resources to close
    }
}
