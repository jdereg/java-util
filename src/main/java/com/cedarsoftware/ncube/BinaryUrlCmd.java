package com.cedarsoftware.ncube;

import com.cedarsoftware.util.UrlUtilities;

import java.util.Map;
import java.util.Set;

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
public class BinaryUrlCmd extends CommandCell
{
    private byte[] content;
    private final boolean cache;

    public BinaryUrlCmd(boolean cache)
    {
        super("");
        this.cache = cache;
    }

    protected void processUrl(Map args)
    {
        if (getUrl() == null)
        {
            return;
        }
        try
        {
            content = UrlUtilities.getContentFromUrl(getUrl(), proxyServer, proxyPort, null, null, true);
        }
        catch (Exception e)
        {
            NCube ncube = (NCube) args.get("ncube");
            setCompileErrorMsg("Failed to load binary cell contents from URL: " + getUrl() + ", NCube '" + ncube.getName() + "'");
            throw new RuntimeException(getCompileErrorMsg(), e);
        }
        if (cache)
        {
            setUrl(null);  // indicates that URL has been processed
        }
    }

    protected Object runFinal(Map args)
    {
        return content;
    }

    public void getCubeNamesFromCommandText(Set<String> cubeNames)
    {
    }

    public void getScopeKeys(Set<String> scopeKeys)
    {
    }
}
