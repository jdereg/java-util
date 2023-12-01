package com.cedarsoftware.util;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.net.URLConnection;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipException;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Useful System utilities for common tasks
 *
 * @author Ken Partlow (kpartlow@gmail.com)
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
public class TestIOUtilities
{
    private final String _expected = "This is for an IO test!";
    
    @Test
    public void testConstructorIsPrivate() throws Exception {
        Class<?> c = IOUtilities.class;
        assertEquals(Modifier.FINAL, c.getModifiers() & Modifier.FINAL);

        Constructor<?> con = c.getDeclaredConstructor();
        assertEquals(Modifier.PRIVATE, con.getModifiers() & Modifier.PRIVATE);
        con.setAccessible(true);

        assertNotNull(con.newInstance());
    }

    @Test
    public void testTransferFileToOutputStream() throws Exception {
        ByteArrayOutputStream s = new ByteArrayOutputStream(4096);
        URLConnection c = mock(URLConnection.class);
        when(c.getOutputStream()).thenReturn(s);
        URL u = TestIOUtilities.class.getClassLoader().getResource("io-test.txt");
        IOUtilities.transfer(new File(u.getFile()), c, null);
        assertEquals(_expected, new String(s.toByteArray(), "UTF-8"));
    }

    @Test
    public void testTransferFileToOutputStreamWithDeflate() throws Exception {
        File f = File.createTempFile("test", "test");

        // perform test
        URL inUrl = TestIOUtilities.class.getClassLoader().getResource("test.inflate");
        FileInputStream in = new FileInputStream(new File(inUrl.getFile()));
        URLConnection c = mock(URLConnection.class);
        when(c.getInputStream()).thenReturn(in);
        when(c.getContentEncoding()).thenReturn("deflate");
        IOUtilities.transfer(c, f, null);
        IOUtilities.close(in);

        // load actual result
        FileInputStream actualIn = new FileInputStream(f);
        ByteArrayOutputStream actualResult = new ByteArrayOutputStream(8192);
        IOUtilities.transfer(actualIn, actualResult);
        IOUtilities.close(actualIn);
        IOUtilities.close(actualResult);


        // load expected result
        ByteArrayOutputStream expectedResult = getUncompressedByteArray();
        assertArrayEquals(expectedResult.toByteArray(), actualResult.toByteArray());
        f.delete();
    }

    @Test
    public void testTransferWithGzip() throws Exception {
        gzipTransferTest("gzip");
    }

    @Test
    public void testTransferWithXGzip() throws Exception {
        gzipTransferTest("x-gzip");
    }

    public void gzipTransferTest(String encoding) throws Exception {
        File f = File.createTempFile("test", "test");

        // perform test
        URL inUrl = TestIOUtilities.class.getClassLoader().getResource("test.gzip");
        FileInputStream in = new FileInputStream(new File(inUrl.getFile()));
        URLConnection c = mock(URLConnection.class);
        when(c.getInputStream()).thenReturn(in);
        when(c.getContentEncoding()).thenReturn(encoding);
        IOUtilities.transfer(c, f, null);
        IOUtilities.close(in);

        // load actual result
        FileInputStream actualIn = new FileInputStream(f);
        ByteArrayOutputStream actualResult = new ByteArrayOutputStream(8192);
        IOUtilities.transfer(actualIn, actualResult);
        IOUtilities.close(actualIn);
        IOUtilities.close(actualResult);


        // load expected result
        ByteArrayOutputStream expectedResult = getUncompressedByteArray();
        assertArrayEquals(expectedResult.toByteArray(), actualResult.toByteArray());
        f.delete();
    }

    @Test
    public void testCompressBytes() throws Exception
    {
        // load start
        ByteArrayOutputStream start = getUncompressedByteArray();
        byte[] small = IOUtilities.compressBytes(start.toByteArray());
        byte[] restored = IOUtilities.uncompressBytes(small);
        assert small.length < restored.length;
        DeepEquals.deepEquals(start.toByteArray(), restored);
    }

    @Test
    public void testFastCompressBytes() throws Exception
    {
        // load start
        FastByteArrayOutputStream start = getFastUncompressedByteArray();
        FastByteArrayOutputStream small = new FastByteArrayOutputStream(8192);
        IOUtilities.compressBytes(start, small);
        byte[] restored = IOUtilities.uncompressBytes(small.toByteArray(), 0, small.size());

        assert small.size() < start.size();

        String restoredString = new String(restored);
        String origString = new String(start.toByteArray(), 0, start.size());
        assert origString.equals(restoredString);
    }

    @Test
    public void testCompressBytesWithException() throws Exception
    {
        try
        {
            IOUtilities.compressBytes(null);
            fail();
        }
        catch (Exception ignore)
        { }
    }

    @Test
    public void testUncompressBytesThatDontNeedUncompressed() throws Exception
    {
        byte[] bytes = { 0x05, 0x10, 0x10};
        byte[] result = IOUtilities.uncompressBytes(bytes);
        assertSame(bytes, result);
    }

    @Test
    public void testUncompressBytesWithException() throws Exception {
        try
        {
            IOUtilities.uncompressBytes(new byte[] {(byte)0x1F, (byte)0x8b, 0x01});
            fail();
        }
        catch (RuntimeException e)
        {
            assertEquals(ZipException.class, e.getCause().getClass());
            assertTrue(e.getMessage().toLowerCase().contains("error"));
            assertTrue(e.getMessage().toLowerCase().contains("uncompressing"));
        }

    }

    private ByteArrayOutputStream getUncompressedByteArray() throws IOException
    {
        URL inUrl = TestIOUtilities.class.getClassLoader().getResource("test.txt");
        ByteArrayOutputStream start = new ByteArrayOutputStream(8192);
        FileInputStream in = new FileInputStream(inUrl.getFile());
        IOUtilities.transfer(in, start);
        IOUtilities.close(in);
        return start;
    }

    private FastByteArrayOutputStream getFastUncompressedByteArray() throws IOException
    {
        URL inUrl = TestIOUtilities.class.getClassLoader().getResource("test.txt");
        FastByteArrayOutputStream start = new FastByteArrayOutputStream(8192);
        FileInputStream in = new FileInputStream(inUrl.getFile());
        IOUtilities.transfer(in, start);
        IOUtilities.close(in);
        return start;
    }

    @Test
    public void testUncompressBytes() throws Exception
    {
        ByteArrayOutputStream expectedResult = getCompressedByteArray();

        // load start
        ByteArrayOutputStream start = getUncompressedByteArray();

        ByteArrayOutputStream result = new ByteArrayOutputStream(8192);
        byte[] uncompressedBytes = IOUtilities.uncompressBytes(expectedResult.toByteArray());

        assertArrayEquals(start.toByteArray(), uncompressedBytes);
    }

    private ByteArrayOutputStream getCompressedByteArray() throws IOException
    {
        // load expected result
        URL expectedUrl = TestIOUtilities.class.getClassLoader().getResource("test.gzip");
        ByteArrayOutputStream expectedResult = new ByteArrayOutputStream(8192);
        FileInputStream expected = new FileInputStream(expectedUrl.getFile());
        IOUtilities.transfer(expected, expectedResult);
        IOUtilities.close(expected);
        return expectedResult;
    }

    @Test
    public void testTransferInputStreamToFile() throws Exception
    {
        File f = File.createTempFile("test", "test");
        URL u = TestIOUtilities.class.getClassLoader().getResource("io-test.txt");
        IOUtilities.transfer(u.openConnection(), f, null);


        ByteArrayOutputStream s = new ByteArrayOutputStream(4096);
        FileInputStream in = new FileInputStream(f);
        IOUtilities.transfer(in, s);
        IOUtilities.close(in);
        assertEquals(_expected, new String(s.toByteArray(), "UTF-8"));
        f.delete();
    }

    @Test
    public void transferInputStreamToBytes() throws Exception {
        URL u = TestIOUtilities.class.getClassLoader().getResource("io-test.txt");
        FileInputStream in = new FileInputStream(new File(u.getFile()));
        byte[] bytes = new byte[23];
        IOUtilities.transfer(in, bytes);
        assertEquals(_expected, new String(bytes, "UTF-8"));
    }

    public void transferInputStreamToBytesWithNotEnoughBytes() throws Exception {
        URL u = TestIOUtilities.class.getClassLoader().getResource("io-test.txt");
        FileInputStream in = new FileInputStream(new File(u.getFile()));
        byte[] bytes = new byte[24];
        try
        {
            IOUtilities.transfer(in, bytes);
            fail("should not make it here");
        }
        catch (IOException e)
        {
        }
    }

    @Test
    public void transferInputStreamWithFileAndOutputStream() throws Exception {
        URL u = TestIOUtilities.class.getClassLoader().getResource("io-test.txt");
        ByteArrayOutputStream out = new ByteArrayOutputStream(8192);
        IOUtilities.transfer(new File(u.getFile()), out);
        assertEquals(_expected, new String(out.toByteArray()));
    }


    @Test
    public void transferInputStreamToOutputStreamWithCallback() throws Exception {
        ByteArrayInputStream in = new ByteArrayInputStream("This is a test".getBytes());
        ByteArrayOutputStream out = new ByteArrayOutputStream(8192);

        IOUtilities.transfer(in, out, new IOUtilities.TransferCallback()
        {
            @Override
            public void bytesTransferred(byte[] bytes, int count)
            {
                assertEquals(14, count);
            }

            @Override
            public boolean isCancelled()
            {
                return true;
            }
        });
        assertEquals("This is a test", new String(out.toByteArray()));
    }

    @Test
    public void testInputStreamToBytes()
    {
        ByteArrayInputStream in = new ByteArrayInputStream("This is a test".getBytes());

        byte[] bytes = IOUtilities.inputStreamToBytes(in);
        assertEquals("This is a test", new String(bytes));
    }

    @Test
    public void transferInputStreamToBytesWithNull()
    {
        assertNull(IOUtilities.inputStreamToBytes(null));
    }

    @Test
    public void testGzipInputStream() throws Exception
    {
        URL outUrl = TestIOUtilities.class.getClassLoader().getResource("test.gzip");
        URL inUrl = TestIOUtilities.class.getClassLoader().getResource("test.txt");

        OutputStream out = new GZIPOutputStream(new FileOutputStream(outUrl.getFile()));
        InputStream in = new FileInputStream(inUrl.getFile());
        IOUtilities.transfer(in, out);
        IOUtilities.close(in);
        IOUtilities.flush(out);
        IOUtilities.close(out);
    }

    @Test
    public void testInflateInputStream() throws Exception
    {
        URL outUrl = TestIOUtilities.class.getClassLoader().getResource("test.inflate");
        URL inUrl = TestIOUtilities.class.getClassLoader().getResource("test.txt");

        OutputStream out = new DeflaterOutputStream(new FileOutputStream(outUrl.getFile()));
        InputStream in = new FileInputStream(new File(inUrl.getFile()));
        IOUtilities.transfer(in, out);
        IOUtilities.close(in);
        IOUtilities.flush(out);
        IOUtilities.close(out);
    }

    @Test
    public void testXmlStreamReaderClose()
    {
        XMLInputFactory factory = XMLInputFactory.newInstance();
        try
        {
            XMLStreamReader reader = factory.createXMLStreamReader(new ByteArrayInputStream("<root></root>".getBytes("UTF-8")));
            IOUtilities.close(reader);
        }
        catch (Exception e)
        {
            fail();
        }

        IOUtilities.close((XMLStreamReader)null);
    }

    @Test
    public void testXmlStreamWriterFlushClose()
    {
        XMLOutputFactory xmlOutputFactory = XMLOutputFactory.newFactory();
        try
        {
            XMLStreamWriter writer = xmlOutputFactory.createXMLStreamWriter(new BufferedOutputStream(new ByteArrayOutputStream()), "UTF-8");
            IOUtilities.flush(writer);
            IOUtilities.close(writer);
        }
        catch (Exception e)
        {
            fail();
        }
        IOUtilities.close((XMLStreamWriter)null);
    }
}
