package com.cedarsoftware.ncube.util;

import com.cedarsoftware.ncube.NCube;
import com.cedarsoftware.ncube.NCubeManager;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;

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

        when(request.getServletPath()).thenReturn("/dyn/view/index");
        when(request.getRequestURL()).thenReturn(new StringBuffer("http://www.cedarsoftware.com/ctx/dyn/view/index"));

        ServletOutputStream out = new DumboOutputStream();

        when(response.getOutputStream()).thenReturn(out);

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
}