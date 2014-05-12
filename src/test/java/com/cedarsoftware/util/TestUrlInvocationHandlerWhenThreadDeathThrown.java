package com.cedarsoftware.util;

import com.cedarsoftware.util.io.JsonReader;
import com.cedarsoftware.util.io.JsonWriter;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest({UrlUtilities.class})

/**
 * Created by kpartlow on 4/30/2014.
 */
public class TestUrlInvocationHandlerWhenThreadDeathThrown
{
    @Test(expected=ThreadDeath.class)
    public void testUrlInvocationHandlerWithThreadDeath() throws Exception {
        // Test Return
        //URL u = this.getClass().getClassLoader().getResource("urlInvocationTestReturn.json");
        //FileInputStream in = new FileInputStream(new File(u.getFile()));

        //  mock url calls
        URL input = PowerMockito.mock(URL.class);
        when(input.getHost()).thenReturn("cedarsoftware.com");
        when(input.getPath()).thenReturn("/integration/doWork");


        // mock streams
        ByteArrayOutputStream out = new ByteArrayOutputStream(8192);
        ByteArrayInputStream in = new ByteArrayInputStream("[\"bar\"]".getBytes("UTF-8"));

        HttpURLConnection c = mock(HttpURLConnection.class);
        when(c.getOutputStream()).thenThrow(ThreadDeath.class);
//        when(c.getInputStream()).thenReturn(in);

        PowerMockito.stub(PowerMockito.method(UrlUtilities.class, "getConnection", URL.class, boolean.class, boolean.class, boolean.class)).toReturn(c);


        Map map = new HashMap();
        AInt intf = (AInt)ProxyFactory.create(AInt.class, new UrlInvocationHandler(new UrlInvocationHandlerJsonStrategy("http://foo")));
        Assert.assertEquals("bar", intf.foo());

        //        PowerMockito.verifyStatic(Mockito.times(1));

        //verify(c).getContentEncoding();
        //verify(c).setRequestMethod("POST");

    }

    private interface AInt {
        public String foo();
    }

    /**
     * Created by kpartlow on 5/11/2014.
     */
    private class UrlInvocationHandlerJsonStrategy implements UrlInvocationHandlerStrategy
    {
        private String _url;

        public UrlInvocationHandlerJsonStrategy(String url)
        {
            _url = url;
        }

        @Override
        public URL buildURL(Object proxy, Method m, Object[] args) throws MalformedURLException
        {
            return new URL(_url);
        }

        @Override
        public int getRetryAttempts()
        {
            return 0;
        }

        @Override
        public void getCookies(URLConnection c)
        {
            //            UrlUtilities.getCookies(c, null);
        }

        @Override
        public void setCookies(URLConnection c)
        {
            //            UrlUtilities.setCookies(c, _store);
            //c.setRequestProperty("Cookie", "JSESSIONID=" + _sessionId);
        }

        @Override
        public byte[] generatePostData(Object proxy, Method m, Object[] args) throws IOException
        {
            ByteArrayOutputStream ba_out = new ByteArrayOutputStream();
            JsonWriter jwr = new JsonWriter(ba_out);
            jwr.write(new Object[]{m.getName(), args});
            IOUtilities.close(jwr);

            return ba_out.toByteArray();
        }

        public Object readResponse(URLConnection c) throws IOException
        {
            JsonReader reader = null;

            try
            {
                reader = new JsonReader(IOUtilities.getInputStream(c));
                Object[] res = (Object[]) reader.readObject();
                return res[0];
            }
            finally
            {
                IOUtilities.close(reader);
            }
        }
    }


}
