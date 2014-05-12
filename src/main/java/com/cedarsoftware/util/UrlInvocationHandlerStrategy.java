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

    byte[] generatePostData(Object proxy, Method m, Object[] args) throws IOException;
    Object readResponse(URLConnection c) throws IOException;
}
