package com.cedarsoftware.ncube;

import groovy.lang.GroovyShell;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;

/**
 * @author John DeRegnaucourt (jdereg@gmail.com)
 * @author Ken Partlow (kpartlow@gmail.com)
 * <br/>
 * Copyright (c) Cedar Software LLC
 * <br/><br/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <br/><br/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <br/><br/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
public abstract class UrlCommandCell implements CommandCell
{
    private String cmd;
    private volatile transient String errorMsg = null;
    private String url = null;
    private final AtomicBoolean isUrlExpanded = new AtomicBoolean(false);
    private int hash;
    private static final GroovyShell shell = new GroovyShell();
    public static final char EXTENSION_SEPARATOR = '.';

    //  Private constructor only for serialization.
    protected UrlCommandCell() { }

    public UrlCommandCell(String cmd, String url)
    {
        if (cmd == null && url == null)
        {
            throw new IllegalArgumentException("Both 'cmd' and 'url' cannot be null");
        }

        if (cmd != null && cmd.isEmpty())
        {   // Because of this, cmdHash() never has to worry about an empty ("") command (when url is null)
            throw new IllegalArgumentException("'cmd' cannot be empty");
        }

        this.cmd = cmd;
        this.url = url;
        this.hash = cmd == null ? url.hashCode() : cmd.hashCode();
    }

    public String getUrl()
    {
        return url;
    }

    public abstract boolean isCacheable();

    public void expandUrl(Map ctx)
    {
        if (isUrlExpanded.get())
        {
            return;
        }

        synchronized (this)
        {
            if (isUrlExpanded.get())
            {
                return;
            }
            NCube ncube = getNCube(ctx);
            Matcher m = Regexes.groovyRelRefCubeCellPatternA.matcher(url);
            StringBuilder expandedUrl = new StringBuilder();
            int last = 0;
            Map input = getInput(ctx);

            while (m.find())
            {
                expandedUrl.append(url.substring(last, m.start()));
                String cubeName = m.group(2);
                NCube refCube = NCubeManager.getCube(ncube.getApplicationID(), cubeName);
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
            url = expandedUrl.toString();
            isUrlExpanded.set(true);
        }
    }

    protected URL getActualUrl(Map ctx)
    {
        URL actualUrl;
        NCube ncube = getNCube(ctx);
        try
        {
            String localUrl = url.toLowerCase();

            if (localUrl.startsWith("http:") || localUrl.startsWith("https:") || localUrl.startsWith("file:"))
            {   // Absolute URL
                actualUrl = new URL(url);
            }
            else
            {   // Relative URL
                URLClassLoader loader = NCubeManager.getUrlClassLoader(ncube.getApplicationID(), getInput(ctx));

                // Make URL absolute (uses URL roots added to NCubeManager)
                actualUrl = loader.getResource(url);
            }
        }
        catch(IllegalStateException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            throw new IllegalArgumentException("Invalid URL:  " + url + ", ncube: " + ncube.name + ", app: " + ncube.getApplicationID(), e);
        }

        if (actualUrl == null)
        {
            throw new IllegalStateException("Unable to resolve URL, make sure appropriate resource urls are added to the sys.classpath cube, url: " +
                    url + ", cube: " + ncube.name + ", app: " + ncube.getApplicationID());
        }
        return actualUrl;
    }

    public static NCube getNCube(Map ctx)
    {
        NCube ncube = (NCube) ctx.get("ncube");
        return ncube;
    }

    public static Map getInput(Map ctx)
    {
        Map input = (Map) ctx.get("input");
        return input;
    }

    public static Map getOutput(Map ctx)
    {
        Map output = (Map) ctx.get("output");
        return output;
    }

    public boolean equals(Object other)
    {
        if (!(other instanceof UrlCommandCell))
        {
            return false;
        }

        UrlCommandCell that = (UrlCommandCell) other;

        if (cmd != null)
        {
            return cmd.equals(that.cmd);
        }

        return url.equals(that.getUrl());
    }

    public int hashCode()
    {
        return this.hash;
    }

    public String getCmd()
    {
        return cmd;
    }

    public String toString()
    {
        return url == null ? cmd : url;
    }

    public void failOnErrors()
    {
        if (errorMsg != null)
        {
            throw new IllegalStateException(errorMsg);
        }
    }

    public void setErrorMessage(String msg)
    {
        errorMsg = msg;
    }

    public String getErrorMessage()
    {
        return errorMsg;
    }

    public int compareTo(CommandCell cmdCell)
    {
        String cmd1 = cmd == null ? "" : cmd;
        String cmd2 = cmdCell.getCmd() == null ? "" : cmdCell.getCmd();

        int comp = cmd1.compareTo(cmd2);

        if (comp == 0)
        {
            String url1 = url == null ? "" : url;
            String url2 = cmdCell.getUrl() == null ? "" : cmdCell.getUrl();
            return url1.compareTo(url2);
        }

        return comp;
    }

    public void getCubeNamesFromCommandText(Set<String> cubeNames)
    {
    }

    public void getScopeKeys(Set<String> scopeKeys)
    {
    }

    public abstract Object execute(Map<String, Object> ctx);
}
