package com.cedarsoftware.ncube;

import org.junit.Test;

import java.io.ByteArrayInputStream;

import static org.junit.Assert.assertEquals;

/**
 * Created by kpartlow on 8/2/2014.
 */
public class TestUrlCommandCell
{
    @Test
    public void testCachingInputStreamRead() throws Exception {
        String s = "foo-bar";
        ByteArrayInputStream stream = new ByteArrayInputStream(s.getBytes("UTF-8"));
        UrlCommandCell.CachingInputStream in = new UrlCommandCell.CachingInputStream(stream);

        assertEquals(102, in.read());
        assertEquals(111, in.read());
        assertEquals(111, in.read());
        assertEquals(45, in.read());
        assertEquals(98, in.read());
        assertEquals(97, in.read());
        assertEquals(114, in.read());
        assertEquals(-1, in.read());
    }
    @Test
    public void testCachingInputStreamReadBytes() throws Exception {
        String s = "foo-bar";
        ByteArrayInputStream stream = new ByteArrayInputStream(s.getBytes("UTF-8"));
        UrlCommandCell.CachingInputStream in = new UrlCommandCell.CachingInputStream(stream);

        byte[] bytes = new byte[7];
        assertEquals(7, in.read(bytes, 0, 7));
        assertEquals("foo-bar", new String(bytes, "UTF-8"));
        assertEquals(-1, in.read(bytes, 0, 7));
    }



}
