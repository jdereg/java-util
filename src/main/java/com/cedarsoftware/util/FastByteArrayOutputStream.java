package com.cedarsoftware.util;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.Arrays;

/**
 * Faster version of ByteArrayOutputStream that does not have synchronized methods.
 * Like ByteArrayOutputStream, this class is not thread-safe and has a theoretical
 * limit of approximately 2GB (Integer.MAX_VALUE bytes).
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

    /**
     * The maximum size of array to allocate.
     * Some VMs reserve header words in an array, so we use a slightly smaller max.
     */
    private static final int MAX_ARRAY_SIZE = Integer.MAX_VALUE - 8;

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

    /**
     * Create stream using a caller-provided initial buffer.
     * The provided array is used directly (no copy).
     */
    public FastByteArrayOutputStream(byte[] initialBuffer) {
        if (initialBuffer == null || initialBuffer.length == 0) {
            throw new IllegalArgumentException("Initial buffer cannot be null or empty");
        }
        this.buf = initialBuffer;
    }

    private void ensureCapacity(int minCapacity) {
        if (minCapacity - buf.length > 0) {
            grow(minCapacity);
        }
    }

    private void grow(int minCapacity) {
        int oldCapacity = buf.length;
        // Use 1.5x growth to reduce risk of overflow (compared to 2x)
        int newCapacity = oldCapacity + (oldCapacity >> 1);

        // Handle overflow or insufficient growth
        if (newCapacity - minCapacity < 0) {
            newCapacity = minCapacity;
        }

        // Check if we've exceeded maximum array size
        if (newCapacity - MAX_ARRAY_SIZE > 0) {
            newCapacity = hugeCapacity(minCapacity);
        }

        buf = Arrays.copyOf(buf, newCapacity);
    }

    private static int hugeCapacity(int minCapacity) {
        if (minCapacity < 0) { // overflow
            throw new OutOfMemoryError("Required array size too large");
        }
        return (minCapacity > MAX_ARRAY_SIZE) ? Integer.MAX_VALUE : MAX_ARRAY_SIZE;
    }

    public void write(int b) {
        ensureCapacity(count + 1);
        buf[count] = (byte) b;
        count += 1;
    }

    @Override
    public void write(byte[] b, int off, int len) {
        if (b == null) {
            throw new NullPointerException("Input byte array cannot be null");
        }
        if (len == 0) {
            return;
        }
        if ((off < 0) || (len < 0) || (off > b.length) || (off + len > b.length) || (off + len < 0)) {
            throw new IndexOutOfBoundsException();
        }
        int minCapacity = count + len;
        // Detect integer overflow (count + len wrapped negative)
        if (minCapacity < 0) {
            throw new OutOfMemoryError("Required array size too large");
        }
        ensureCapacity(minCapacity);
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

    /**
     * Returns the internal buffer directly without copying.
     * <p>
     * <b>Warning:</b> The returned array may be larger than the actual data written.
     * Use {@link #getCount()} to determine how many bytes are valid.
     * Modifying the returned array will affect this stream's data.
     *
     * @return the internal buffer (not a copy)
     */
    public byte[] getInternalBuffer() {
        return buf;
    }

    /**
     * Returns the number of valid bytes in the internal buffer.
     * Use with {@link #getInternalBuffer()} for zero-copy access.
     *
     * @return the number of valid bytes written to this stream
     */
    public int getCount() {
        return count;
    }

    /**
     * Creates a FastByteArrayInputStream that reads from this stream's data.
     * <p>
     * <b>Note:</b> This creates a copy of the data. For zero-copy access,
     * use {@link #getInternalBuffer()} and {@link #getCount()} directly.
     *
     * @return a new FastByteArrayInputStream containing this stream's data
     */
    public FastByteArrayInputStream toInputStream() {
        return new FastByteArrayInputStream(toByteArray());
    }

    public int size() {
        return count;
    }

    public String toString() {
        return new String(buf, 0, count);
    }

    /**
     * Converts the buffer's contents into a string using the specified charset.
     *
     * @param charset the charset to use for decoding the bytes
     * @return the string decoded from the buffer's contents
     */
    public String toString(Charset charset) {
        return new String(buf, 0, count, charset);
    }

    public void writeTo(OutputStream out) {
        try {
            out.write(buf, 0, count);
        } catch (IOException e) {
            ExceptionUtilities.uncheckedThrow(e);
        }
    }

    @Override
    public void close() {
        // No resources to close
    }
}
