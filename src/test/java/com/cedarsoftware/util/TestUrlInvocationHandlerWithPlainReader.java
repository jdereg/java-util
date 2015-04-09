package com.cedarsoftware.util;

//import com.cedarsoftware.util.io.JsonReader;
//import com.cedarsoftware.util.io.JsonWriter;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;



/**
 * Created by kpartlow on 5/11/2014.
 */
public class TestUrlInvocationHandlerWithPlainReader
{
    private static final Logger LOG = LogManager.getLogger(TestUrlInvocationHandlerWithPlainReader.class);

    @Test
    public void testWithBadUrl() {
        TestUrlInvocationInterface item = ProxyFactory.create(TestUrlInvocationInterface.class, new UrlInvocationHandler(new UrlInvocationHandlerJsonStrategy("http://cedarsoftware.com/invalid/url", "F012982348484444")));
        Assert.assertNull(item.foo());
    }

    @Test
    public void testHappyPath() {
        TestUrlInvocationInterface item = ProxyFactory.create(TestUrlInvocationInterface.class, new UrlInvocationHandler(new UrlInvocationHandlerJsonStrategy("http://www.cedarsoftware.com/tests/java-util/url-invocation-handler-test.json", "F012982348484444")));
        Assert.assertEquals("[\"test-passed\"]", item.foo());
    }

    @Test
    public void testWithSessionAwareInvocationHandler() {
        TestUrlInvocationInterface item = ProxyFactory.create(TestUrlInvocationInterface.class, new UrlInvocationHandler(new UrlInvocationHandlerJsonStrategy("http://www.cedarsoftware.com/tests/java-util/url-invocation-handler-test.json", "F012982348484444")));
        Assert.assertEquals("[\"test-passed\"]", item.foo());
    }

    @Test
    public void testUrlInvocationHandlerWithException() {
        TestUrlInvocationInterface item = ProxyFactory.create(TestUrlInvocationInterface.class, new UrlInvocationHandler(new UrlInvocationHandlerStrategyThatThrowsInvocationTargetException("http://www.cedarsoftware.com/tests/java-util/url-invocation-handler-test.json")));
        Assert.assertNull(item.foo());
    }

    @Test
    public void testUrlInvocationHandlerWithInvocationExceptionAndNoCause() {
        TestUrlInvocationInterface item = ProxyFactory.create(TestUrlInvocationInterface.class, new UrlInvocationHandler(new UrlInvocationHandlerStrategyThatThrowsInvocationTargetExceptionWithNoCause("http://www.cedarsoftware.com/tests/java-util/url-invocation-handler-test.json")));
        Assert.assertNull(item.foo());
    }

    @Test
    public void testUrlInvocationHandlerWithNonInvocationException() {
        TestUrlInvocationInterface item = ProxyFactory.create(TestUrlInvocationInterface.class, new UrlInvocationHandler(new UrlInvocationHandlerStrategyThatThrowsNullPointerException("http://www.cedarsoftware.com/tests/java-util/url-invocation-handler-test.json")));
        Assert.assertNull(item.foo());
    }

    private interface TestUrlInvocationInterface
    {
        public String foo();
    }


    /**
     * Created by kpartlow on 5/11/2014.
     */
    private static class UrlInvocationHandlerJsonStrategy implements UrlInvocationHandlerStrategy
    {
        private String _url;
        private String _sessionId;

        public UrlInvocationHandlerJsonStrategy(String url, String sessionId)
        {
            _url = url;
            _sessionId = sessionId;
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
        }

        @Override
        public void setRequestHeaders(URLConnection c)
        {

        }

        @Override
        public void setCookies(URLConnection c)
        {
            c.setRequestProperty("Cookie", "JSESSIONID=" + _sessionId);
        }

        @Override
        public byte[] generatePostData(Object proxy, Method m, Object[] args) throws IOException
        {
            return new byte[0];
        }

        public Object readResponse(URLConnection c) throws IOException
        {
            ByteArrayOutputStream input = new ByteArrayOutputStream(32768);
            IOUtilities.transfer(IOUtilities.getInputStream(c), input);
            byte[] bytes = input.toByteArray();
            return new String(bytes, "UTF-8");
        }
    }

    /**
     * Created by kpartlow on 5/11/2014.
     */
    private static class UrlInvocationHandlerStrategyThatThrowsNullPointerException implements UrlInvocationHandlerStrategy
    {
        private String _url;

        public UrlInvocationHandlerStrategyThatThrowsNullPointerException(String url)
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
        }

        @Override
        public void setRequestHeaders(URLConnection c)
        {

        }

        @Override
        public void setCookies(URLConnection c)
        {

        }

        @Override
        public byte[] generatePostData(Object proxy, Method m, Object[] args) throws IOException
        {
            return new byte[0];
        }

        public Object readResponse(URLConnection c) throws IOException
        {
            return new NullPointerException("Error");
        }
    }

    /**
     * Created by kpartlow on 5/11/2014.
     */
    private static class UrlInvocationHandlerStrategyThatThrowsInvocationTargetException implements UrlInvocationHandlerStrategy
    {
        private String _url;

        public UrlInvocationHandlerStrategyThatThrowsInvocationTargetException(String url)
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
        }

        @Override
        public void setRequestHeaders(URLConnection c)
        {

        }

        @Override
        public void setCookies(URLConnection c)
        {

        }

        @Override
        public byte[] generatePostData(Object proxy, Method m, Object[] args) throws IOException
        {
            return new byte[0];
        }

        public Object readResponse(URLConnection c) throws IOException
        {
            return new InvocationTargetException(new NullPointerException("Error"));
        }
    }

    /**
     * Created by kpartlow on 5/11/2014.
     */
    private static class UrlInvocationHandlerWithTimeout implements UrlInvocationHandlerStrategy
    {
        private String _url;

        public UrlInvocationHandlerWithTimeout(String url)
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
        }

        @Override
        public void setRequestHeaders(URLConnection c)
        {

        }

        @Override
        public void setCookies(URLConnection c)
        {

        }

        @Override
        public byte[] generatePostData(Object proxy, Method m, Object[] args) throws IOException
        {
            return new byte[0];
        }

        public Object readResponse(URLConnection c) throws IOException
        {
            return new InvocationTargetException(new NullPointerException("Error"));
        }
    }

    /**
     * Created by kpartlow on 5/11/2014.
     */
    private static class UrlInvocationHandlerStrategyThatThrowsInvocationTargetExceptionWithNoCause implements UrlInvocationHandlerStrategy
    {
        private String _url;

        public UrlInvocationHandlerStrategyThatThrowsInvocationTargetExceptionWithNoCause(String url)
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
        }

        @Override
        public void setRequestHeaders(URLConnection c)
        {

        }

        @Override
        public void setCookies(URLConnection c)
        {

        }

        @Override
        public byte[] generatePostData(Object proxy, Method m, Object[] args) throws IOException
        {
            return new byte[0];
        }

        public Object readResponse(URLConnection c) throws IOException
        {
            return new InvocationTargetException(null);
        }
    }

}
