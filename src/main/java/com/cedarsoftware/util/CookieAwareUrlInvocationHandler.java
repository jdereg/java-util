package com.cedarsoftware.util;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Map;

/**
 * This invocation handler maintains the cookies for the connection.
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
public class CookieAwareUrlInvocationHandler extends UrlInvocationHandler
{
    private final Map _store;

    public CookieAwareUrlInvocationHandler(URL url, Map store)
    {
        super(url);
        _store = store;
    }

    protected void getCookies(URLConnection c) throws IOException
    {
        UrlUtilities.getCookies(c, _store);
    }

    protected void setCookies(URLConnection c) throws IOException
    {
        UrlUtilities.setCookies(c, _store);
    }

}
