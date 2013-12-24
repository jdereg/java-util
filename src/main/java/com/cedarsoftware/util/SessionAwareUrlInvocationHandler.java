package com.cedarsoftware.util;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;

/**
 * This handler adds the JSESSIONID cookie so that the Java Application (or Applet)
 * can respond with the HTTP session Cookie header.
 *
 * @author John DeRegnaucourt (jdereg@gmail.com)
 *         <br/>
 *         Copyright (c) Cedar Software LLC
 *         <br/><br/>
 *         Licensed under the Apache License, Version 2.0 (the "License");
 *         you may not use this file except in compliance with the License.
 *         You may obtain a copy of the License at
 *         <br/><br/>
 *         http://www.apache.org/licenses/LICENSE-2.0
 *         <br/><br/>
 *         Unless required by applicable law or agreed to in writing, software
 *         distributed under the License is distributed on an "AS IS" BASIS,
 *         WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *         See the License for the specific language governing permissions and
 *         limitations under the License.
 */
public class SessionAwareUrlInvocationHandler extends UrlInvocationHandler
{
    private final String _sessionId;

    public SessionAwareUrlInvocationHandler(URL url, String sessionId)
    {
        super(url);
        _sessionId = sessionId;
    }

    protected void getCookies(URLConnection c) throws IOException
    {
    }

    protected void setCookies(URLConnection c) throws IOException
    {
        c.setRequestProperty("Cookie", "JSESSIONID=" + _sessionId);
    }
}