package com.cedarsoftware.ncube;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

/**
 * Created by kpartlow on 8/2/2014.
 */
public class TestUrlCommandCell
{
    @Test
    public void testCachingInputStreamRead() throws Exception
    {
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
    public void testCachingInputStreamReadBytes() throws Exception
    {
        String s = "foo-bar";
        ByteArrayInputStream stream = new ByteArrayInputStream(s.getBytes("UTF-8"));
        UrlCommandCell.CachingInputStream in = new UrlCommandCell.CachingInputStream(stream);

        byte[] bytes = new byte[7];
        assertEquals(7, in.read(bytes, 0, 7));
        assertEquals("foo-bar", new String(bytes, "UTF-8"));
        assertEquals(-1, in.read(bytes, 0, 7));
    }

    @Test
    public void testBadUrlCommandCell()
    {
        try
        {
            new UrlCommandCell("", null, false)
            {
                protected Object executeInternal(Object data, Map<String, Object> ctx)
                {
                    return null;
                }
            };
            fail("should not make it here");
        }
        catch (IllegalArgumentException ignored)
        {
        }

        UrlCommandCell cell = new UrlCommandCell("println 'hello'", null, false)
        {
            protected Object executeInternal(Object data, Map<String, Object> ctx)
            {
                return null;
            }
        };

        // Nothing more than covering method calls and lines.  These methods
        // do nothing, therefore there is nothing to assert.
        cell.getCubeNamesFromCommandText(null);
        cell.getScopeKeys(null);

        assertFalse(cell.equals("String"));

        Map coord = new HashMap();
        coord.put("content.type", "view");
        coord.put("content.name", "badProtocol");
        NCube cube = NCubeManager.getNCubeFromResource("cdnRouterTest.json");
        try
        {
            cube.getCell(coord);
            fail("Should not make it here");
        }
        catch (Exception ignored)
        {
        }

        coord.put("content.name", "badRelative");
        try
        {
            cube.getCell(coord);
            fail("Should not make it here");
        }
        catch (Exception ignored)
        {
        }

        // Cause null urlLoader (it won't find URLs in NCubeManager for version 9.8.7)
        coord.put("content.name", "file");
        cube.setVersion("9.8.7");
        try
        {
            cube.getCell(coord);
            fail("Should not make it here");
        }
        catch (Exception ignored)
        {
        }
    }
}
