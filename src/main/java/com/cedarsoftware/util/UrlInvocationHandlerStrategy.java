package com.cedarsoftware.util;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

/**
 * Created by kpartlow on 5/9/2014.
 */
public interface UrlInvocationHandlerStrategy
{
    URL buildURL(Object proxy, Method m, Object[] args) throws MalformedURLException;

    int getRetryAttempts();

    void setCookies(URLConnection c);
    void getCookies(URLConnection c);

    void setRequestHeaders(URLConnection c);

    /**
     *  Json Example:
     *  <code>ByteArrayOutputStream ba_out = new ByteArrayOutputStream();
     *  JsonWriter jwr = new JsonWriter(ba_out);
     *  jwr.write(new Object[]{m.getName(), args});
     *  IOUtilities.close(jwr);
     *  return ba_out.toByteArray();</code>
     *
     * @param proxy
     * @param m
     * @param args
     * @return
     * @throws IOException
     */
    byte[] generatePostData(Object proxy, Method m, Object[] args) throws IOException;

    /**
     * Json Example:
     * <code>JsonReader reader = null;
     *
     *        try
     *        {
     *            reader = new JsonReader(IOUtilities.getInputStream(c));
     *            Object[] res = (Object[]) reader.readObject();
     *            return res[0];
     *        }
     *        finally
     *        {
     *            IOUtilities.close(reader);
     *        }
     *        </code>
     * @param c HttpConnectionObject from which to receive data.
     * @return an object from the proxied server
     * @throws IOException
     */
    Object readResponse(URLConnection c) throws IOException;
}
