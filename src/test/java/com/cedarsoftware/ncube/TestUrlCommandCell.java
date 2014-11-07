package com.cedarsoftware.ncube;

import com.cedarsoftware.ncube.util.CdnRouter;
import org.junit.Test;
import org.mockito.Mockito;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
        cube.setApplicationID(new ApplicationID(cube.getApplicationID().getTenant(), cube.getApplicationID().getApp(), "9.8.7", cube.getApplicationID().getStatus()));
        try
        {
            cube.getCell(coord);
            fail("Should not make it here");
        }
        catch (Exception ignored)
        {
        }
    }

    @Test
    public void testProxyFetchSocketTimeout() throws Exception {
        UrlCommandCell cell = new StringUrlCmd("http://www.cedarsoftware.com", false);

        NCube ncube = Mockito.mock(NCube.class);
        HttpServletResponse response = Mockito.mock(HttpServletResponse.class);
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);

        when(request.getHeaderNames()).thenThrow(SocketTimeoutException.class);

        when(ncube.getName()).thenReturn("foo-cube");
        when(ncube.getVersion()).thenReturn("foo-version");

        Map map = new HashMap();
        map.put("ncube", ncube);

        Map input = new HashMap();
        input.put(CdnRouter.HTTP_RESPONSE, response);
        input.put(CdnRouter.HTTP_REQUEST, request);

        map.put("input", input);

        cell.proxyFetch(map);

        verify(response, times(1)).sendError(HttpServletResponse.SC_NOT_FOUND, "File not found: http://www.cedarsoftware.com");
    }

    @Test
    public void testProxyFetchSocketTimeoutWithResponseSendErrorIssue() throws Exception {
        UrlCommandCell cell = new StringUrlCmd("http://www.cedarsoftware.com", false);

        NCube ncube = Mockito.mock(NCube.class);
        HttpServletResponse response = Mockito.mock(HttpServletResponse.class);
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);

        when(request.getHeaderNames()).thenThrow(SocketTimeoutException.class);
        doThrow(IOException.class).when(response).sendError(HttpServletResponse.SC_NOT_FOUND, "File not found: http://www.cedarsoftware.com");
        when(ncube.getName()).thenReturn("foo-cube");
        when(ncube.getVersion()).thenReturn("foo-version");

        Map map = new HashMap();
        map.put("ncube", ncube);

        Map input = new HashMap();
        input.put(CdnRouter.HTTP_RESPONSE, response);
        input.put(CdnRouter.HTTP_REQUEST, request);

        map.put("input", input);

        cell.proxyFetch(map);
    }
}
