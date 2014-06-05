package com.cedarsoftware.util;

import com.cedarsoftware.util.io.JsonReader;
import com.cedarsoftware.util.io.JsonWriter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;



/**
 * Created by kpartlow on 5/11/2014.
 */
public class TestUrlInvocationHandlerWithPlainReader
{
    private static final Log LOG = LogFactory.getLog(TestUrlInvocationHandlerWithPlainReader.class);

    @Test
    public void testWithBadUrl() {
        TestUrlInvocationInterface item = ProxyFactory.create(TestUrlInvocationInterface.class, new UrlInvocationHandler(new UrlInvocationHandlerJsonStrategy("http://cedarsoftware.com/invalid/url", "F012982348484444")));
        Assert.assertNull(item.foo());
    }

    @Test
    public void test() {
        TestUrlInvocationInterface item = ProxyFactory.create(TestUrlInvocationInterface.class, new UrlInvocationHandler(new UrlInvocationHandlerJsonStrategy("http://www.cedarsoftware.com/tests/java-util/url-invocation-handler-test.json", "F012982348484444")));
        Assert.assertEquals("test-passed", item.foo());
    }

    @Test
    public void testWithSessionAwareInvocationHandler() {
        TestUrlInvocationInterface item = ProxyFactory.create(TestUrlInvocationInterface.class, new UrlInvocationHandler(new UrlInvocationHandlerJsonStrategy("http://www.cedarsoftware.com/tests/java-util/url-invocation-handler-test.json", "F012982348484444")));
        Assert.assertEquals("test-passed", item.foo());
    }

    @Test
    public void testUrlInvocationHandlerWithException() {
        TestUrlInvocationInterface item = ProxyFactory.create(TestUrlInvocationInterface.class, new UrlInvocationHandler(new UrlInvocationHandlerJsonStrategy("http://www.cedarsoftware.com/tests/java-util/url-invocation-handler-exception.json", "F012982348484444")));
        Assert.assertNull(item.foo());
    }

    @Test
    public void testUrlInvocationHandlerWithNonInvocationException() {
        TestUrlInvocationInterface item = ProxyFactory.create(TestUrlInvocationInterface.class, new UrlInvocationHandler(new UrlInvocationHandlerJsonStrategy("http://www.cedarsoftware.com/tests/java-util/url-invocation-handler-non-invocation-exception.json", "F012982348484444")));
        Assert.assertNull(item.foo());
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
            ByteArrayOutputStream ba_out = new ByteArrayOutputStream();
            JsonWriter jwr = new JsonWriter(ba_out);
            jwr.write(new Object[]{m.getName(), args});
            IOUtilities.close(jwr);

            if (LOG.isDebugEnabled())
            {    // DEBUG
                String jsonCall = new String(ba_out.toByteArray(), "UTF-8");
                LOG.debug("Calling server:\n    " + jsonCall);
            }

            return ba_out.toByteArray();
        }

        public Object readResponse(URLConnection c) throws IOException
        {
            JsonReader reader = null;

            try
            {
                if (LOG.isDebugEnabled())
                {
                    ByteArrayOutputStream input = new ByteArrayOutputStream(32768);
                    IOUtilities.transfer(IOUtilities.getInputStream(c), input);
                    byte[] bytes = input.toByteArray();
                    String jsonResp = new String(bytes, "UTF-8");
                    LOG.debug(jsonResp);
                    reader = new JsonReader(new ByteArrayInputStream(bytes));
                }
                else
                {
                    reader = new JsonReader(IOUtilities.getInputStream(c));
                }
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
