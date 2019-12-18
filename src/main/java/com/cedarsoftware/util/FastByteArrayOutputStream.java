package com.cedarsoftware.util;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;

/**
 * Faster version of ByteArrayOutputStream that does not have synchronized methods and
 * also provides direct access to its internal buffer so that it does not need to be
 * duplicated when read.
 * 
 * @author John DeRegnaucourt (john@cedarsoftware.com)
 *         <br>
 *         Copyright (c) Cedar Software LLC
 *         <br><br>
 *         Licensed under the Apache License, Version 2.0 (the "License");
 *         you may not use this file except in compliance with the License.
 *         You may obtain a copy of the License at
 *         <br><br>
 *         http://www.apache.org/licenses/LICENSE-2.0
 *         <br><br>
 *         Unless required by applicable law or agreed to in writing, software
 *         distributed under the License is distributed on an "AS IS" BASIS,
 *         WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *         See the License for the specific language governing permissions and
 *         limitations under the License.
 */
public class FastByteArrayOutputStream extends OutputStream
{
    protected byte buffer[];
    protected int size;
    protected int delta;

    /**
     * Construct a new FastByteArrayOutputStream with a logical size of 0,
     * but an initial capacity of 1K (1024 bytes).  The delta increment is x2.
     */
    public FastByteArrayOutputStream()
    {
        this(1024, -1);
    }

    /**
     * Construct a new FastByteArrayOutputStream with the passed in capacity, and a
     * default delta (1024).  The delta increment is x2.
     * @param capacity int size of internal buffer
     */
    public FastByteArrayOutputStream(int capacity)
    {
        this(capacity, -1);
    }

    /**
     * Construct a new FastByteArrayOutputStream with a logical size of 0,
     * but an initial capacity of 'capacity'.
     * @param capacity int capacity (internal buffer size), must be &gt; 0
     * @param delta int delta, size to increase the internal buffer by when limit reached.  If the value
     * is negative, then the internal buffer is doubled in size when additional capacity is needed.
     */
    public FastByteArrayOutputStream(int capacity, int delta)
    {
        if (capacity < 1)
        {
            throw new IllegalArgumentException("Capacity must be at least 1 byte, passed in capacity=" + capacity);
        }
        buffer = new byte[capacity];
        this.delta = delta;
    }

    /**
     * @return byte[], the internal byte buffer.  Remember, the length of this array is likely larger
     * than 'size' (whats been written to it).  Therefore, use this byte[] along with 0 to size() to
     * fetch the contents of this buffer without creating a new byte[].
     */
    public byte[] getBuffer()
    {
        return buffer;
    }

    /**
     * Increases the capacity of the internal buffer (if necessary) to hold 'minCapacity' bytes.
     * The internal buffer will be reallocated and expanded if necessary.  Therefore, be careful
     * use the byte[] returned from getBuffer(), as it's address will change as the buffer is
     * expanded.  However, if you are no longer adding to this stream, you can use the internal
     * buffer.
     * @param minCapacity the desired minimum capacity
     */
    private void ensureCapacity(int minCapacity)
    {
        if (minCapacity - buffer.length > 0)
        {
            int oldCapacity = buffer.length;
            int newCapacity;

            if (delta < 1)
            {   // Either double internal buffer
                newCapacity = oldCapacity << 1;
            }
            else
            {   // Increase internal buffer size by 'delta'
                newCapacity = oldCapacity + delta;
            }
            
            if (newCapacity - minCapacity < 0)
            {
                newCapacity = minCapacity;
            }
            buffer = Arrays.copyOf(buffer, newCapacity);
        }
    }

    /**
     * Writes the specified byte to this byte array output stream.
     *
     * @param b the byte to be written.
     */
    public void write(int b)
    {
        ensureCapacity(size + 1);
        buffer[size] = (byte) b;
        size += 1;
    }

    /**
     * Writes <code>len</code> bytes from the specified byte array
     * starting at offset <code>off</code> to this stream.
     *
     * @param bytes byte[] the data to write to this stream.
     * @param offset the start offset in the data.
     * @param len the number of bytes to write.
     */
    public void write(byte[] bytes, int offset, int len)
    {
        if (bytes == null)
        {
            return;
        }
        if ((offset < 0) || (offset > bytes.length) || (len < 0) || ((offset + len) - bytes.length > 0))
        {
            throw new IndexOutOfBoundsException("offset=" + offset + ", len=" + len + ", bytes.length=" + bytes.length);
        }
        ensureCapacity(size + len);
        System.arraycopy(bytes, offset, buffer, size, len);
        size += len;
    }

    /**
     * Convenience method to copy the contained byte[] to the passed in OutputStream.
     * You could also code out.write(fastBa.getBuffer(), 0, fastBa.size())
     * @param out OutputStream target
     * @throws IOException if one occurs
     */
    public void writeTo(OutputStream out) throws IOException
    {
        out.write(buffer, 0, size);
    }

    /**
     * Copy the internal byte[] to the passed in byte[].  No new space is allocated.
     * @param dest byte[] target
     */
    public void writeTo(byte[] dest)
    {
        if (dest.length < size)
        {
            throw new IllegalArgumentException("Passed in byte[] is not large enough");
        }

        System.arraycopy(buffer, 0, dest, 0, size);
    }

    /**
     * @return String (UTF-8) from the byte[] in this object.
     */
    public String toString()
    {
        try
        {
            return new String(buffer, 0, size, "UTF-8");
        }
        catch (UnsupportedEncodingException e)
        {
            throw new IllegalStateException("Unable to convert byte[] into UTF-8 string.");
        }
    }

    /**
     * Reset the stream so it can be used again.  The size() will be 0,
     * but the internal storage is still allocated.
     */
    public void clear()
    {
        size = 0;
    }

    /**
     * The logical size of the byte[] this stream represents, not
     * its physical size, which could be larger.
     * @return int the number of bytes written to this stream
     */
    public int size()
    {
        return size;
    }
}
