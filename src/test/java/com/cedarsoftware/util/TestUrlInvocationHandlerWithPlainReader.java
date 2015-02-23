package com.cedarsoftware.util;

import com.google.gson.Gson;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

import static org.junit.Assert.fail;


/**
 * Created by kpartlow on 5/11/2014.
 */
public class TestUrlInvocationHandlerWithPlainReader
{
    private static final Logger LOG = LogManager.getLogger(TestUrlInvocationHandlerWithPlainReader.class);

    @Test
    public void testWithBadUrl() {
        TestUrlInvocationInterface item = ProxyFactory.create(TestUrlInvocationInterface.class,
                new UrlInvocationHandler(new UrlInvocationHandlerJsonStrategy("http://cedarsoftware.com/invalid/url", "F012982348484444")));
        Assert.assertNull(item.foo());
    }

    @Test
    public void test() {
        TestUrlInvocationInterface item = ProxyFactory.create(TestUrlInvocationInterface.class,
                new UrlInvocationHandler(new UrlInvocationHandlerJsonStrategy("http://www.cedarsoftware.com/tests/java-util/url-invocation-handler-test.json", "F012982348484444")));
        Assert.assertEquals("test-passed", item.foo());
    }

    @Test
    public void testWithSessionAwareInvocationHandler() {
        TestUrlInvocationInterface item = ProxyFactory.create(TestUrlInvocationInterface.class,
                new UrlInvocationHandler(new UrlInvocationHandlerJsonStrategy("http://www.cedarsoftware.com/tests/java-util/url-invocation-handler-test.json", "F012982348484444")));
        Assert.assertEquals("test-passed", item.foo());
    }

    @Test
    public void testUrlInvocationHandlerWithException() {
        TestUrlInvocationInterface item = ProxyFactory.create(TestUrlInvocationInterface.class,
                new UrlInvocationHandler(new UrlInvocationHandlerJsonStrategy("http://www.cedarsoftware.com/tests/java-util/url-invocation-handler-exception.json", "F012982348484444")));
        try
        {
            item.foo();
            fail();
        }
        catch (ClassCastException ignored)
        { }
    }

    @Test
    public void testUrlInvocationHandlerWithNonInvocationException() {
        TestUrlInvocationInterface item = ProxyFactory.create(TestUrlInvocationInterface.class,
                new UrlInvocationHandler(new UrlInvocationHandlerJsonStrategy("http://www.cedarsoftware.com/tests/java-util/url-invocation-handler-non-invocation-exception.json", "F012982348484444")));
        try
        {
            item.foo();
            fail();
        }
        catch (ClassCastException ignored)
        { }
    }


    private interface TestUrlInvocationInterface
    {
        public String foo();
    }


    /**
     * Created by kpartlow on 5/11/2014.
     */
    private class UrlInvocationHandlerJsonStrategy implements UrlInvocationHandlerStrategy
    {
        private String _url;
        private String _sessionId;

        public UrlInvocationHandlerJsonStrategy(String url, String sessionId)
        {
            _url = url;
            _sessionId = sessionId;
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
        }

        public void setRequestHeaders(URLConnection c)
        {

        }

        public void setCookies(URLConnection c)
        {
            c.setRequestProperty("Cookie", "JSESSIONID=" + _sessionId);
        }

        public byte[] generatePostData(Object proxy, Method m, Object[] args) throws IOException
        {
            return "[\"foo\",null]".getBytes("UTF-8");
        }

        public Object readResponse(URLConnection c) throws IOException
        {
            Reader reader = null;

            try
            {
                reader = new InputStreamReader(IOUtilities.getInputStream(c));
                Gson gson = new Gson();
                Object[] res = gson.fromJson(reader, Object[].class);
                return res[0];
            }
            finally
            {
                IOUtilities.close(reader);
            }
        }
    }
}
