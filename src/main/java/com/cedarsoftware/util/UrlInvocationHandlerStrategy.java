package com.cedarsoftware.util;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

/**
 * Useful String utilities for common tasks
 *
 * @author Ken Partlow (kpartlow@gmail.com)
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
public interface UrlInvocationHandlerStrategy
{
    URL buildURL(Object proxy, Method m, Object[] args) throws MalformedURLException;

    int getRetryAttempts();
    long getRetrySleepTime();

    void setCookies(URLConnection c);
    void getCookies(URLConnection c);

    void setRequestHeaders(URLConnection c);

    /**
     * @param proxy Proxy object
     * @param m Method to be called
     * @param args Object[] Arguments to method
     * @return byte[] return value
     * @throws IOException
     */
    byte[] generatePostData(Object proxy, Method m, Object[] args) throws IOException;

    /**
     * @param c HttpConnectionObject from which to receive data.
     * @return an object from the proxied server
     * @throws IOException
     */
    Object readResponse(URLConnection c) throws IOException;
}
