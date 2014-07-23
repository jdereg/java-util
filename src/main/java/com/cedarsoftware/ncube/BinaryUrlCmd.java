package com.cedarsoftware.ncube;

import com.cedarsoftware.util.UrlUtilities;

import java.net.URL;
import java.util.Map;

/**
 * Process a binary type (byte[]) that is specified at a URL.
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
public class BinaryUrlCmd extends UrlCommandCell
{
    public BinaryUrlCmd(String url)
    {
        super(null, url, false);
    }

    public BinaryUrlCmd(String url, boolean cache)
    {
        super(null, url, cache);
    }

    protected Object simpleFetch(Map args)
    {
        NCube ncube = (NCube) args.get("ncube");
        try
        {
            URL u = getActualUrl(ncube.getVersion(), ncube.getName());
            //TODO:  java-util change remove u.toString() when we have a URL version of this call
            return UrlUtilities.getContentFromUrl(u.toString(), proxyServer, proxyPort, null, null, true);
        }
        catch (Exception e)
        {
            setErrorMessage("Failed to load binary content from URL: " + getUrl() + ", NCube '" + ncube.getName() + "'");
            throw new IllegalStateException(getErrorMessage(), e);
        }
    }

    protected Object executeInternal(Object data, Map<String, Object> ctx)
    {
        return data;
    }
}
