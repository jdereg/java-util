package com.cedarsoftware.util;

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

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Ken Partlow
 *         <br>
 *         Copyright (c) Cedar Software LLC
 *         <br><br>
 *         Licensed under the Apache License, Version 2.0 (the "License");
 *         you may not use this file except in compliance with the License.
 *         You may obtain a copy of the License at
 *         <br><br>
 *         http://www.apache.org/licenses/LICENSE-2.0
 *         <br><br>
 *         Unless required by applicable law or agreed to in writing, software
 *         distributed under the License is distributed on an "AS IS" BASIS,
 *         WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *         See the License for the specific language governing permissions and
 *         limitations under the License.
 */
@PowerMockIgnore("javax.management.*")
@RunWith(PowerMockRunner.class)
@PrepareForTest({UrlUtilities.class})
public class TestUrlInvocationHandlerWhenExceptionsAreThrown
{
    @Test
    public void testUrlInvocationHandlerWithThreadDeath() throws Exception {
        //  mock url calls
        URL input = PowerMockito.mock(URL.class);
        when(input.getHost()).thenReturn("cedarsoftware.com");
        when(input.getPath()).thenReturn("/integration/doWork");

        // mock streams
        HttpURLConnection c = mock(HttpURLConnection.class);
        when(c.getOutputStream()).thenThrow(ThreadDeath.class);

        PowerMockito.stub(PowerMockito.method(UrlUtilities.class, "getConnection", URL.class, boolean.class, boolean.class, boolean.class)).toReturn(c);


        try {
            AInt intf = ProxyFactory.create(AInt.class, new UrlInvocationHandler(new UrlInvocationHandlerJsonStrategy("http://foo", 1, 0)));
            intf.foo();
            fail();
        } catch (ThreadDeath td) {
        }
    }

    @Test
    public void testUrlInvocationHandlerWithOtherExceptionThrown() throws Exception {
        //  mock url calls
        URL input = PowerMockito.mock(URL.class);
        when(input.getHost()).thenReturn("cedarsoftware.com");
        when(input.getPath()).thenReturn("/integration/doWork");

        // mock streams
        HttpURLConnection c = mock(HttpURLConnection.class);
        when(c.getOutputStream()).thenThrow(IOException.class);

        PowerMockito.stub(PowerMockito.method(UrlUtilities.class, "getConnection", URL.class, boolean.class, boolean.class, boolean.class)).toReturn(c);


        AInt intf = ProxyFactory.create(AInt.class, new UrlInvocationHandler(new UrlInvocationHandlerJsonStrategy("http://foo", 1, 1000)));
        long time = System.currentTimeMillis();
        assertNull(intf.foo());
        assertTrue(System.currentTimeMillis() - time > 1000);
    }

    private interface AInt {
        public String foo();
    }

    /**
     * Created by kpartlow on 5/11/2014.
     */
    private static class UrlInvocationHandlerJsonStrategy implements UrlInvocationHandlerStrategy
    {
        private final String _url;
        private final int _retries;
        private final long _retrySleepTime;
        Map _store = new HashMap();

        public UrlInvocationHandlerJsonStrategy(String url, int retries, long retrySleepTime)
        {
            _url = url;
            _retries = retries;
            _retrySleepTime = retrySleepTime;
        }

        public URL buildURL(Object proxy, Method m, Object[] args) throws MalformedURLException
        {
            return new URL(_url);
        }

        public int getRetryAttempts()
        {
            return _retries;
        }
        public long getRetrySleepTime() { return _retrySleepTime; }

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
