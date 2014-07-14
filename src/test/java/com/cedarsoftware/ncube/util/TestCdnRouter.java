package com.cedarsoftware.ncube.util;

import com.cedarsoftware.ncube.NCube;
import com.cedarsoftware.ncube.NCubeManager;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import javax.servlet.ServletInputStream;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest({NCubeManager.class})
public class TestCdnRouter
{

    @BeforeClass
    public static void setup() throws Exception{
        NCubeManager.clearCubeList();
        setClassPath("file");
    }

    public void tearDown() throws Exception {
        NCubeManager.clearCubeList();
    }

    public static void setClassPath(String version) throws Exception {
        List<String> urls = new ArrayList<String>();
        URL url = NCubeManager.class.getResource("/");
        urls.add(url.toString());
        urls.add("http://www.cedarsoftware.com");
        NCubeManager.setUrlClassLoader(urls, version);
    }

//    @Test
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

        CdnRouter.setCdnRoutingProvider(new CdnRoutingProvider()
        {
            public void setupCoordinate(Map coord)
            {
                coord.put(CdnRouter.CONNECTION, "dummy");
                coord.put(CdnRouter.APP, "ncube.test");
                coord.put(CdnRouter.STATUS, "release");
                coord.put(CdnRouter.CUBE_NAME, "test");
                coord.put(CdnRouter.CUBE_VERSION, "file");
                coord.put(CdnRouter.DATE, null);
            }

            public boolean isAuthorized(String type)
            {
                return true;
            }
        });

        URLClassLoader loader = NCubeManager.getUrlClassLoader("file");
        NCube routerCube = NCubeManager.getNCubeFromResource("cdnRouterTest.json");
        PowerMockito.mockStatic(NCubeManager.class);
        when(NCubeManager.getCube(anyString(), anyString())).thenReturn(routerCube);
        when(NCubeManager.getUrlClassLoader("file")).thenReturn(loader);
        CdnRouter router = new CdnRouter();
        router.route(request, response);
        byte[] bytes = ((DumboOutputStream)out).getBytes();
        String s = new String(bytes);
        assertEquals("CAFEBABE", s);
    }

    @Test
    public void test500() throws Exception {
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

        CdnRouter.setCdnRoutingProvider(new CdnRoutingProvider()
        {
            public void setupCoordinate(Map coord)
            {
                coord.put(CdnRouter.CONNECTION, conn);
                coord.put(CdnRouter.APP, "ncube.test");
                coord.put(CdnRouter.STATUS, "release");
                coord.put(CdnRouter.CUBE_NAME, "test");
                coord.put(CdnRouter.CUBE_VERSION, "file");
                coord.put(CdnRouter.DATE, null);

            }

            public boolean isAuthorized(String type)
            {
                return true;
            }
        });


        URLClassLoader loader = NCubeManager.getUrlClassLoader("file");
        NCube routerCube = NCubeManager.getNCubeFromResource("cdnRouterTest.json");

        PowerMockito.mockStatic(NCubeManager.class);
        when(NCubeManager.getCube(anyString(), anyString())).thenReturn(routerCube);
        when(NCubeManager.getUrlClassLoader("file")).thenReturn(loader);

        CdnRouter router = new CdnRouter();
        router.route(request, response);
        byte[] bytes = ((DumboOutputStream)out).getBytes();
        String s = new String(bytes);
    }

    @Test
    public void test404() throws Exception {
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

        final Connection conn = Mockito.mock(Connection.class);

        CdnRouter.setCdnRoutingProvider(new CdnRoutingProvider()
        {
            public void setupCoordinate(Map coord)
            {
                coord.put(CdnRouter.CONNECTION, conn);
                coord.put(CdnRouter.APP, "ncube.test");
                coord.put(CdnRouter.STATUS, "release");
                coord.put(CdnRouter.CUBE_NAME, "test");
                coord.put(CdnRouter.CUBE_VERSION, "file");
                coord.put(CdnRouter.DATE, null);

            }

            public boolean isAuthorized(String type)
            {
                return true;
            }
        });


        URLClassLoader loader = NCubeManager.getUrlClassLoader("file");
        NCube routerCube = NCubeManager.getNCubeFromResource("cdnRouterTest.json");

        PowerMockito.mockStatic(NCubeManager.class);
        when(NCubeManager.getCube(anyString(), anyString())).thenReturn(routerCube);
        when(NCubeManager.getUrlClassLoader("file")).thenReturn(loader);

        CdnRouter router = new CdnRouter();
        router.route(request, response);
        byte[] bytes = ((DumboOutputStream)out).getBytes();
        String s = new String(bytes);

        verify(response, times(1)).sendError(404, "Not Found");
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
            { }
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

        @Override
        public int read() throws IOException
        {
            return bao.read();
        }
    }

}