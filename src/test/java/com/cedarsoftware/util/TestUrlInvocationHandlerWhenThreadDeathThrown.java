package com.cedarsoftware.util;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

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
@PowerMockIgnore("javax.management.*")
@RunWith(PowerMockRunner.class)
@PrepareForTest({UrlUtilities.class})
public class TestUrlInvocationHandlerWhenThreadDeathThrown
{
    @Test(expected=ThreadDeath.class)
    public void testUrlInvocationHandlerWithThreadDeath() throws Exception {
        //  mock url calls
        URL input = PowerMockito.mock(URL.class);
        when(input.getHost()).thenReturn("cedarsoftware.com");
        when(input.getPath()).thenReturn("/integration/doWork");

        // mock streams
        HttpURLConnection c = mock(HttpURLConnection.class);
        when(c.getOutputStream()).thenThrow(ThreadDeath.class);

        PowerMockito.stub(PowerMockito.method(UrlUtilities.class, "getConnection", URL.class, boolean.class, boolean.class, boolean.class)).toReturn(c);


        AInt intf = ProxyFactory.create(AInt.class, new UrlInvocationHandler(new UrlInvocationHandlerJsonStrategy("http://foo")));
        Assert.assertEquals("bar", intf.foo());
    }

    private interface AInt {
        public String foo();
    }

    /**
     * Created by kpartlow on 5/11/2014.
     */
    private static class UrlInvocationHandlerJsonStrategy implements UrlInvocationHandlerStrategy
    {
        private String _url;
        Map _store = new HashMap();

        public UrlInvocationHandlerJsonStrategy(String url)
        {
            _url = url;
        }

        public URL buildURL(Object proxy, Method m, Object[] args) throws MalformedURLException
        {
            return new URL(_url);
        }

        public int getRetryAttempts()
        {
            return 0;
        }

        public void getCookies(URLConnection c)
        {
            UrlUtilities.getCookies(c, null);
        }

        public void setRequestHeaders(URLConnection c)
        {

        }

        public void setCookies(URLConnection c)
        {
            try
            {
                UrlUtilities.setCookies(c, _store);
            } catch (Exception e) {
                // ignore
            }
        }

        public byte[] generatePostData(Object proxy, Method m, Object[] args) throws IOException
        {
            return "[\"foo\",null]".getBytes();
        }

        public Object readResponse(URLConnection c) throws IOException
        {
            return null;
        }
    }


}
