package com.cedarsoftware.ncube.util;

import com.cedarsoftware.ncube.NCube;
import com.cedarsoftware.ncube.NCubeManager;
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
import java.util.Map;
import java.util.Vector;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest({NCubeManager.class})
public class TestCdnRouter
{
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
        when(request.getRequestURL()).thenReturn(new StringBuffer("http://www.cedarsoftware.com/ctx/dyn/view/index"));
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

        CdnRouter.setCdnRoutingProvider(new CdnRoutingProvider()
        {
            public void setupCoordinate(Map coord)
            {
                coord.put(CdnRouter.CONNECTION, "dummy");
                coord.put(CdnRouter.APP, "ncube.test");
                coord.put(CdnRouter.STATUS, "release");
                coord.put(CdnRouter.CUBE_NAME, "test");
                coord.put(CdnRouter.CUBE_VERSION, "1.0.0");
                coord.put(CdnRouter.DATE, null);
            }

            public boolean isAuthorized(String type)
            {
                return true;
            }
        });

        NCube routerCube = NCubeManager.getNCubeFromResource("cdnRouterTest.json");
        PowerMockito.mockStatic(NCubeManager.class);
        when(NCubeManager.getCube(anyString(), anyString())).thenReturn(routerCube);

        CdnRouter router = new CdnRouter();
        router.route(request, response);
        byte[] bytes = ((DumboOutputStream)out).getBytes();
        String s = new String(bytes);
        assertEquals("CAFEBABE", s);
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