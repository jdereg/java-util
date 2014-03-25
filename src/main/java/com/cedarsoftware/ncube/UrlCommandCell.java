package com.cedarsoftware.ncube;

import com.cedarsoftware.util.UrlUtilities;
import groovy.lang.GroovyShell;

import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *  * @author John DeRegnaucourt (jdereg@gmail.com)
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
public abstract class UrlCommandCell extends CommandCell
{
    static final Pattern groovyRelRefCubeCellPatternA = Pattern.compile("([^a-zA-Z0-9_]|^)@([" + NCube.validCubeNameChars + "]+)(\\[[^\\]]+\\])");
    private String url = null;
    private final boolean cache;
    private boolean urlExpanded = false;

    public UrlCommandCell(String cmd, boolean cache)
    {
        super(cmd);
        this.cache = cache;
    }

    public void setUrl(String url)
    {
        this.url = url;
    }

    public String getUrl()
    {
        return url;
    }

    protected void preRun(Map args)
    {
        if (url != null)
        {
            if (!urlExpanded)
            {
                url = expandUrl(args);
                urlExpanded = true;
            }
            processUrl(args);
        }
    }

    private void processUrl(Map args)
    {
        NCube ncube = (NCube) args.get("ncube");

        try
        {
            fetchContentFromUrl();
        }
        catch (Exception e)
        {
            setCompileErrorMsg("Failed to load cell contents from URL: " + getUrl() + ", NCube '" + ncube.getName() + "'");
            throw new IllegalStateException(getCompileErrorMsg(), e);
        }
        if (cache)
        {
            setUrl(null);  // indicates that content has been processed
        }
    }

    protected void fetchContentFromUrl()
    {
        setCmd(UrlUtilities.getContentFromUrlAsString(getUrl(), proxyServer, proxyPort, null, null, true));
    }

    protected String expandUrl(Map args)
    {
        NCube ncube = (NCube) args.get("ncube");
        Matcher m = groovyRelRefCubeCellPatternA.matcher(url);
        StringBuilder expandedUrl = new StringBuilder();
        int last = 0;
        Map input = (Map) args.get("input");
        GroovyShell shell = new GroovyShell();

        while (m.find())
        {
            expandedUrl.append(url.substring(last, m.start()));
            String cubeName = m.group(2);
            NCube refCube = NCubeManager.getCube(cubeName, ncube.getVersion());
            if (refCube == null)
            {
                throw new IllegalStateException("Reference to not-loaded NCube '" + cubeName + "', from NCube '" + ncube.getName() + "', url: " + url);
            }

            Map coord = (Map) shell.evaluate(m.group(3));
            input.putAll(coord);
            Object val = refCube.getCell(input);
            val = (val == null) ? "" : val.toString();
            expandedUrl.append(val);
            last = m.end();
        }

        expandedUrl.append(url.substring(last));
        return expandedUrl.toString();
    }

    public void getCubeNamesFromCommandText(Set<String> cubeNames)
    {
    }

    public void getScopeKeys(Set<String> scopeKeys)
    {
    }

    public boolean isCacheable() {
        return cache;
    }
}
