package com.cedarsoftware.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.Flushable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLConnection;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

/**
 * Useful IOUtilities that simplify common io tasks
 *
 * @author John DeRegnaucourt & Ken Partlow (jdereg@gmail.com)
 *         <br/>
 *         Copyright (c) Cedar Software LLC
 *         <br/><br/>
 *         Licensed under the Apache License, Version 2.0 (the "License");
 *         you may not use this file except in compliance with the License.
 *         You may obtain a copy of the License at
 *         <br/><br/>
 *         http://www.apache.org/licenses/LICENSE-2.0
 *         <br/><br/>
 *         Unless required by applicable law or agreed to in writing, software
 *         distributed under the License is distributed on an "AS IS" BASIS,
 *         WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *         See the License for the specific language governing permissions and
 *         limitations under the License.
 */
public final class IOUtilities
{
    private static final int TRANSFER_BUFFER = 32768;

    private IOUtilities()
    {
    }

    public static InputStream getInputStream(URLConnection c) throws IOException
    {
        InputStream is = c.getInputStream();
        String enc = c.getContentEncoding();

        if ("gzip".equalsIgnoreCase(enc) || "x-gzip".equalsIgnoreCase(enc))
        {
            is = new GZIPInputStream(is, TRANSFER_BUFFER);
        }
        else if ("deflate".equalsIgnoreCase(enc))
        {
            is = new InflaterInputStream(is, new Inflater(), TRANSFER_BUFFER);
        }

        return new BufferedInputStream(is);
    }

    public static void transfer(File f, URLConnection c, TransferCallback cb) throws Exception
    {
        InputStream in = null;
        OutputStream out = null;
        try
        {
            in = new BufferedInputStream(new FileInputStream(f));
            out = new BufferedOutputStream(c.getOutputStream());
            transfer(in, out, cb);
        }
        finally
        {
            close(in);
            close(out);
        }
    }

    public static void transfer(URLConnection c, File f, TransferCallback cb) throws Exception
    {
        InputStream in = null;
        try
        {
            in = getInputStream(c);
            transfer(in, f, cb);
        }
        finally
        {
            close(in);
        }
    }

    public static void transfer(InputStream s, File f, TransferCallback cb) throws Exception
    {
        OutputStream out = null;
        try
        {
            out = new BufferedOutputStream(new FileOutputStream(f));
            transfer(s, out, cb);
        }
        finally
        {
            close(out);
        }
    }

    /**
     * Transfers bytes from an input stream to an output stream.
     * Callers of this method are responsible for closing the streams
     * since they are the ones that opened the streams.
     */
    public static void transfer(InputStream in, OutputStream out, TransferCallback cb) throws IOException
    {
        byte[] bytes = new byte[TRANSFER_BUFFER];
        int count;
        while ((count = in.read(bytes)) != -1)
        {
            out.write(bytes, 0, count);
            if (cb != null)
            {
                cb.bytesTransferred(bytes, count);
                if (cb.isCancelled())
                {
                    break;
                }
            }
        }
    }

    /**
     * Use this when you expect a byte[] length of bytes to be read from the InputStream
     */
    public static void transfer(InputStream in, byte[] bytes) throws IOException
    {
        // Read in the bytes
        int offset = 0;
        int numRead;
        while (offset < bytes.length && (numRead = in.read(bytes, offset, bytes.length - offset)) >= 0)
        {
            offset += numRead;
        }

        if (offset < bytes.length)
        {
            throw new IOException("Retry:  Not all bytes were transferred correctly.");
        }
    }


    /**
     * Transfers bytes from an input stream to an output stream.
     * Callers of this method are responsible for closing the streams
     * since they are the ones that opened the streams.
     */
    public static void transfer(InputStream in, OutputStream out) throws IOException
    {
        byte[] bytes = new byte[TRANSFER_BUFFER];
        int count;
        while ((count = in.read(bytes)) != -1)
        {
            out.write(bytes, 0, count);
        }
    }

    public static void transfer(File file, OutputStream out) throws IOException
    {
        InputStream in = null;
        try
        {
            in = new BufferedInputStream(new FileInputStream(file), TRANSFER_BUFFER);
            transfer(in, out);
        }
        finally
        {
            flush(out);
            close(in);
        }
    }

    public static void close(Closeable c)
    {
        try
        {
            if (c != null)
            {
                c.close();
            }
        }
        catch (IOException  ignore) { }
    }

    public static void flush(Flushable f)
    {
        try
        {
            if (f != null)
            {
                f.flush();
            }
        }
        catch (Throwable ignore) { }
    }

    /**
     * Convert InputStream contents to a byte[].
     * Will return null on error.  Only use this API if you know that the stream length will be
     * relatively small.
     */
    public static byte[] inputStreamToBytes(InputStream in)
    {
        try
        {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            transfer(in, out);
            return out.toByteArray();
        }
        catch (Exception e)
        {
            return null;
        }
    }

    /**
     * Transfers a byte[] to the output stream of a URLConnection
     * @param c  Connection to transfer output
     * @param bytes the bytes to send
     * @throws IOException
     */
    public static void transfer(URLConnection c, byte[] bytes) throws IOException {
        OutputStream out = null;
        try {
            out = c.getOutputStream();
            out.write(bytes);
        } finally {
            close(out);
        }
    }

    public static void compressBytes(ByteArrayOutputStream original, ByteArrayOutputStream compressed) throws IOException
    {
        DeflaterOutputStream gzipStream = new GZIPOutputStream(compressed, 32768);
        original.writeTo(gzipStream);
        gzipStream.flush();
        gzipStream.close();
    }

    public interface TransferCallback
    {
        void bytesTransferred(byte[] bytes, int count);

        boolean isCancelled();
    }
}
