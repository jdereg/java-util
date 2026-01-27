package com.cedarsoftware.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;

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
        if (buf == null) {
            throw new NullPointerException("Input byte array cannot be null");
        }
        this.buffer = buf;
        this.pos = 0;
        this.count = buf.length;
    }

    @Override
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
        long k = count - pos;
        if (n < k) {
            k = n < 0 ? 0 : n;
        }

        pos += (int) k;  // Safe cast: k is bounded by (count - pos) which fits in int
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

    /**
     * Reads all remaining bytes from this input stream.
     * This method provides an optimized implementation that directly
     * copies from the internal buffer rather than using the default
     * InputStream implementation which uses intermediate buffers.
     * <p>
     * This method is compatible with JDK 9+ where it overrides
     * {@code InputStream.readAllBytes()}, and works as a regular
     * method on JDK 8.
     *
     * @return a byte array containing all remaining bytes from this stream
     */
    public byte[] readAllBytes() {
        byte[] result = Arrays.copyOfRange(buffer, pos, count);
        pos = count;
        return result;
    }

    /**
     * Reads up to a specified number of bytes from this input stream.
     * This method provides an optimized implementation that directly
     * copies from the internal buffer.
     * <p>
     * This method is compatible with JDK 11+ where it overrides
     * {@code InputStream.readNBytes(int)}, and works as a regular
     * method on earlier JDK versions.
     *
     * @param len the maximum number of bytes to read
     * @return a byte array containing the bytes read from this stream
     * @throws IllegalArgumentException if len is negative
     */
    public byte[] readNBytes(int len) {
        if (len < 0) {
            throw new IllegalArgumentException("len < 0");
        }
        int remaining = count - pos;
        int bytesToRead = Math.min(len, remaining);
        byte[] result = Arrays.copyOfRange(buffer, pos, pos + bytesToRead);
        pos += bytesToRead;
        return result;
    }

    /**
     * Transfers all remaining bytes from this input stream to the given output stream.
     * This method provides an optimized implementation that writes directly
     * from the internal buffer in a single operation.
     * <p>
     * This method is compatible with JDK 9+ where it overrides
     * {@code InputStream.transferTo(OutputStream)}, and works as a regular
     * method on JDK 8.
     *
     * @param out the output stream to write to
     * @return the number of bytes transferred
     * @throws IOException if an I/O error occurs when writing to the output stream
     * @throws NullPointerException if out is null
     */
    public long transferTo(OutputStream out) throws IOException {
        if (out == null) {
            throw new NullPointerException("Output stream cannot be null");
        }
        int remaining = count - pos;
        if (remaining > 0) {
            out.write(buffer, pos, remaining);
            pos = count;
        }
        return remaining;
    }

    @Override
    public void close() {
        // Optionally implement if resources need to be released
    }
}
