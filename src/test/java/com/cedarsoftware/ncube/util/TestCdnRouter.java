package com.cedarsoftware.ncube.util;

import com.cedarsoftware.ncube.Axis;
import com.cedarsoftware.ncube.NCube;
import com.cedarsoftware.ncube.NCubeManager;
import com.cedarsoftware.ncube.TestNCube;
import com.cedarsoftware.ncube.UrlCommandCell;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;

import javax.servlet.ServletInputStream;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URLClassLoader;
import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class TestCdnRouter
{
    @BeforeClass
    public static void initialize()
    {
         TestNCube.initialize();
    }

    @After
    public void tearDown() throws Exception
    {
        NCubeManager.clearCubeList();
    }

    @Test
    public void testRoute() throws Exception
    {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        HttpServletResponse response = Mockito.mock(HttpServletResponse.class);

        Vector<String> v = new Vector<String>();
        v.add("Accept");
        v.add("Accept-Encoding");
        v.add("Accept-Language");
        v.add("User-Agent");
        v.add("Cache-Control");

        when(request.getServletPath()).thenReturn("/dyn/view/index");
        when(request.getHeaderNames()).thenReturn(v.elements());
        when(request.getHeader("Accept")).thenReturn("text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8");
        when(request.getHeader("User-Agent")).thenReturn("Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/32.0.1700.102 Safari/537.36");
        when(request.getHeader("Accept-Encoding")).thenReturn("gzip,deflate");
        when(request.getHeader("Accept-Language")).thenReturn("n-US,en;q=0.8");
        when(request.getHeader("Cache-Control")).thenReturn("max-age=60");


        when(response.containsHeader("Content-Length")).thenReturn(true);
        when(response.containsHeader("Last-Modified")).thenReturn(true);
        when(response.containsHeader("Expires")).thenReturn(true);
        when(response.containsHeader("Content-Encoding")).thenReturn(true);
        when(response.containsHeader("Content-Type")).thenReturn(true);
        when(response.containsHeader("Cache-Control")).thenReturn(true);
        when(response.containsHeader("Etag")).thenReturn(true);

        ServletOutputStream out = new DumboOutputStream();
        ServletInputStream in = new DumboInputStream();

        when(response.getOutputStream()).thenReturn(out);
        when(request.getInputStream()).thenReturn(in);

        setDefaultCdnRoutingProvider();

        NCubeManager.getNCubeFromResource("cdnRouterTest.json");
        CdnRouter router = new CdnRouter();
        router.route(request, response);
        byte[] bytes = ((DumboOutputStream) out).getBytes();
        String s = new String(bytes);
        assertEquals("CAFEBABE", s);
    }

    @Test
    public void test500() throws Exception
    {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        HttpServletResponse response = Mockito.mock(HttpServletResponse.class);

        Vector<String> v = new Vector<String>();
        v.add("Accept");
        v.add("Accept-Encoding");
        v.add("Accept-Language");
        v.add("User-Agent");
        v.add("Cache-Control");

        when(request.getServletPath()).thenReturn("/dyn/view/500");
        when(request.getHeaderNames()).thenReturn(v.elements());
        when(request.getHeader("Accept")).thenReturn("text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8");
        when(request.getHeader("User-Agent")).thenReturn("Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/32.0.1700.102 Safari/537.36");
        when(request.getHeader("Accept-Encoding")).thenReturn("gzip,deflate");
        when(request.getHeader("Accept-Language")).thenReturn("n-US,en;q=0.8");
        when(request.getHeader("Cache-Control")).thenReturn("max-age=60");


        when(response.containsHeader("Content-Length")).thenReturn(true);
        when(response.containsHeader("Last-Modified")).thenReturn(true);
        when(response.containsHeader("Expires")).thenReturn(true);
        when(response.containsHeader("Content-Encoding")).thenReturn(true);
        when(response.containsHeader("Content-Type")).thenReturn(true);
        when(response.containsHeader("Cache-Control")).thenReturn(true);
        when(response.containsHeader("Etag")).thenReturn(true);

        ServletOutputStream out = new DumboOutputStream();
        ServletInputStream in = new DumboInputStream();

        when(response.getOutputStream()).thenReturn(out);
        when(request.getInputStream()).thenReturn(in);

        final Connection conn = Mockito.mock(Connection.class);

        setDefaultCdnRoutingProvider();


        NCube routerCube = NCubeManager.getNCubeFromResource("cdnRouterTest.json");
        CdnRouter router = new CdnRouter();
        router.route(request, response);

        verify(response, times(1)).sendError(500, "n-cube cell URL resolved to null, url: tests/does/not/exist/index.html, ncube: CdnRouterTest, version: file");
    }

    @Test
    public void testInvalidServletPath() throws Exception
    {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        HttpServletResponse response = Mockito.mock(HttpServletResponse.class);

        when(request.getServletPath()).thenReturn("/foo/bar");

        setDefaultCdnRoutingProvider();

        new CdnRouter().route(request, response);
        verify(response, times(1)).sendError(400, "CdnRouter - Invalid ServletPath (must start with /dyn/) request: /foo/bar");
    }


    @Test
    public void testInvalidVersion() throws Exception
    {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        HttpServletResponse response = Mockito.mock(HttpServletResponse.class);

        when(request.getServletPath()).thenReturn("/dyn/view/404");

        setCdnRoutingProvider("CdnRouterTest", null, true);

        new CdnRouter().route(request, response);
        verify(response, times(1)).sendError(500, "CdnRouter - CdnRoutingProvider did not set up 'router.cubeName' or 'router.version' in the Map coordinate.");
    }

    @Test
    public void testInvalidCubeName() throws Exception
    {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        HttpServletResponse response = Mockito.mock(HttpServletResponse.class);

        Vector<String> v = new Vector<String>();
        v.add("Accept");
        v.add("Accept-Encoding");
        v.add("Accept-Language");
        v.add("User-Agent");
        v.add("Cache-Control");

        when(request.getServletPath()).thenReturn("/dyn/view/404");
        when(request.getHeaderNames()).thenReturn(v.elements());
        when(request.getHeader("Accept")).thenReturn("text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8");
        when(request.getHeader("User-Agent")).thenReturn("Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/32.0.1700.102 Safari/537.36");
        when(request.getHeader("Accept-Encoding")).thenReturn("gzip,deflate");
        when(request.getHeader("Accept-Language")).thenReturn("n-US,en;q=0.8");
        when(request.getHeader("Cache-Control")).thenReturn("max-age=60");


        when(response.containsHeader("Content-Length")).thenReturn(true);
        when(response.containsHeader("Last-Modified")).thenReturn(true);
        when(response.containsHeader("Expires")).thenReturn(true);
        when(response.containsHeader("Content-Encoding")).thenReturn(true);
        when(response.containsHeader("Content-Type")).thenReturn(true);
        when(response.containsHeader("Cache-Control")).thenReturn(true);
        when(response.containsHeader("Etag")).thenReturn(true);

        ServletOutputStream out = new DumboOutputStream();
        ServletInputStream in = new DumboInputStream();

        when(response.getOutputStream()).thenReturn(out);
        when(request.getInputStream()).thenReturn(in);

        setCdnRoutingProvider("foo", "file", true);

        NCubeManager.getNCubeFromResource("cdnRouterTest.json");
        new CdnRouter().route(request, response);

        verify(response, times(1)).sendError(500, "CdnRouter - Error occurred: In order to use the n-cube CDN routing capabilities, a CdnRouter n-cube must already be loaded, and it's name passed in as CdnRouter.CUBE_NAME");
    }

    @Test
    public void test404() throws Exception
    {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        HttpServletResponse response = Mockito.mock(HttpServletResponse.class);

        Vector<String> v = new Vector<String>();

        when(request.getServletPath()).thenReturn("/dyn/view/404");
        when(request.getHeaderNames()).thenReturn(v.elements());

        ServletOutputStream out = new DumboOutputStream();
        ServletInputStream in = new DumboInputStream();

        when(response.getOutputStream()).thenReturn(out);
        when(request.getInputStream()).thenReturn(in);

        final Connection conn = Mockito.mock(Connection.class);

        setDefaultCdnRoutingProvider();


        URLClassLoader loader = NCubeManager.getUrlClassLoader("file");
        NCube routerCube = NCubeManager.getNCubeFromResource("cdnRouterTest.json");

        CdnRouter router = new CdnRouter();
        router.route(request, response);

        verify(response, times(1)).sendError(404, "Not Found");
    }

    private void setCdnRoutingProvider(final String cubeName, final String version, final boolean isAuthorized)
    {
        CdnRouter.setCdnRoutingProvider(new CdnRoutingProvider()
        {
            public void setupCoordinate(Map coord)
            {
                coord.put(CdnRouter.CUBE_NAME, cubeName);
                coord.put(CdnRouter.CUBE_VERSION, version);

            }

            public boolean isAuthorized(String type)
            {
                return isAuthorized;
            }
        });
    }

    private void setDefaultCdnRoutingProvider()
    {
        setCdnRoutingProvider("CdnRouterTest", "file", true);
    }

    @Test
    public void testNotAuthorized() throws Exception
    {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        HttpServletResponse response = Mockito.mock(HttpServletResponse.class);

        Vector<String> v = new Vector<String>();

        when(request.getServletPath()).thenReturn("/dyn/view/index");
        when(request.getHeaderNames()).thenReturn(v.elements());
        when(request.getRequestURL()).thenReturn(new StringBuffer("http://www.foo.com/dyn/view/index"));

        setCdnRoutingProvider("CdnRouterTest", "file", false);

        new CdnRouter().route(request, response);
        verify(response, times(1)).sendError(401, "CdnRouter - Unauthorized access, request: http://www.foo.com/dyn/view/index");
    }


    @Test
    public void testContentTypeTransfer() throws Exception
    {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        HttpServletResponse response = Mockito.mock(HttpServletResponse.class);

        when(request.getServletPath()).thenReturn("/dyn/view/xml");

        when(response.containsHeader("Content-Length")).thenReturn(true);
        when(response.containsHeader("Last-Modified")).thenReturn(true);
        when(response.containsHeader("Expires")).thenReturn(true);
        when(response.containsHeader("Content-Encoding")).thenReturn(true);
        when(response.containsHeader("Content-Type")).thenReturn(true);
        when(response.containsHeader("Cache-Control")).thenReturn(true);
        when(response.containsHeader("Etag")).thenReturn(true);

        ServletOutputStream out = new DumboOutputStream();
        ServletInputStream in = new DumboInputStream();
        when(request.getInputStream()).thenReturn(in);
        when(response.getOutputStream()).thenReturn(out);

        setDefaultCdnRoutingProvider();

        NCubeManager.getNCubeFromResource("cdnRouterTest.json");

        CdnRouter router = new CdnRouter();
        router.route(request, response);
        byte[] bytes = ((DumboOutputStream) out).getBytes();
        String s = new String(bytes);
        assertEquals("<cedarsoftware><jdereg name=\"john\"/></cedarsoftware>", s);
        verify(response, times(1)).addHeader("Content-Type", "application/xml");
        verify(response, times(1)).addHeader("Content-Length", "52");

    }

    @Test
    public void testExceptionOnException() throws Exception
    {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        HttpServletResponse response = Mockito.mock(HttpServletResponse.class);

        when(request.getServletPath()).thenThrow(new RuntimeException("foo"));
        doThrow(new IOException("bar")).when(response).sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "CdnRouter - Error occurred: Failure");

        setDefaultCdnRoutingProvider();
        new CdnRouter().route(request, response);

    }

    @Test
    public void testFileContentTypeTransfer() throws Exception
    {
        cdnRouteFile("file", false);
    }

    @Test
    public void testFileContentTypeCacheTransfer() throws Exception
    {
        cdnRouteFile("cachedFile", true);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBadUrlCommandCell()
    {
        new UrlCommandCell("", null, false)
        {
            protected Object executeInternal(Object data, Map<String, Object> ctx)
            {
                return null;
            }
        };
    }

    private void cdnRouteFile(String logicalFileName, boolean mustMatch) throws IOException
    {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        HttpServletResponse response = Mockito.mock(HttpServletResponse.class);

        Vector<String> v = new Vector<>();
        v.add("Accept");
        v.add("Accept-Encoding");
        v.add("Accept-Language");
        v.add("User-Agent");
        v.add("Cache-Control");

        when(request.getServletPath()).thenReturn("/dyn/view/" + logicalFileName);
        when(request.getHeaderNames()).thenReturn(v.elements());
        when(request.getHeader("Accept")).thenReturn("text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8");
        when(request.getHeader("User-Agent")).thenReturn("Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/32.0.1700.102 Safari/537.36");
        when(request.getHeader("Accept-Encoding")).thenReturn("gzip,deflate");
        when(request.getHeader("Accept-Language")).thenReturn("n-US,en;q=0.8");
        when(request.getHeader("Cache-Control")).thenReturn("max-age=60");

        ServletOutputStream out = new DumboOutputStream();
        ServletInputStream in = new DumboInputStream();

        when(response.getOutputStream()).thenReturn(out);
        when(request.getInputStream()).thenReturn(in);

        setDefaultCdnRoutingProvider();

        NCube cube = NCubeManager.getNCubeFromResource("cdnRouterTest.json");

        new CdnRouter().route(request, response);
        byte[] bytes = ((DumboOutputStream) out).getBytes();
        String s = new String(bytes);
        assertEquals("<html></html>", s);

        verify(response, times(1)).addHeader("content-type", "text/html");

        Map coord = new HashMap();
        coord.put("content.type", "view");
        coord.put("content.name", logicalFileName);
        String one = (String) cube.getCell(coord);
        String two = (String) cube.getCell(coord);

        if (mustMatch)
        {
            assertSame(one, two);
        }
        else
        {
            assertNotSame(one, two);
        }
    }

    @Test
    public void testDefaultRoute() throws Exception
    {
        NCube router = NCubeManager.getNCubeFromResource("cdnRouter.json");

        Axis axis = router.getAxis("content.name");
        assertEquals(5, axis.getColumns().size());

        Map coord = new HashMap();
        coord.put("content.name", "Glock");

        String answer = (String) router.getCell(coord);
        assertEquals(6, axis.getColumns().size());
        assertEquals("Glock.html", answer);

        answer = (String) router.getCell(coord);
        assertEquals(6, axis.getColumns().size());
        assertEquals("Glock.html", answer);

        coord.put("content.name", "Smith & Wesson");
        answer = (String) router.getCell(coord);
        assertEquals(7, axis.getColumns().size());
        assertEquals("Smith & Wesson.html", answer);
    }

    @Test
    public void testWithNoProvider() throws IOException
    {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        HttpServletResponse response = Mockito.mock(HttpServletResponse.class);

        CdnRouter.setCdnRoutingProvider(null);
        new CdnRouter().route(request, response);

        verify(response, times(1)).sendError(500, "CdnRouter - CdnRoutingProvider has not been set into the CdnRouter.");
    }


    static class DumboOutputStream extends ServletOutputStream
    {
        ByteArrayOutputStream bao = new ByteArrayOutputStream();

        public byte[] getBytes()
        {
            try
            {
                bao.flush();
            }
            catch (IOException ignored)
            {
            }
            return bao.toByteArray();
        }

        public void write(int b) throws IOException
        {
            bao.write(b);
        }
    }

    static class DumboInputStream extends ServletInputStream
    {
        ByteArrayInputStream bao = new ByteArrayInputStream(new byte[0]);

        public int read() throws IOException
        {
            return bao.read();
        }
    }

}