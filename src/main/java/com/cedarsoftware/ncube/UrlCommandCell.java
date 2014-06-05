package com.cedarsoftware.ncube;

import com.cedarsoftware.util.EncryptionUtilities;
import com.cedarsoftware.util.StringUtilities;
import com.cedarsoftware.util.SystemUtilities;
import com.cedarsoftware.util.UrlUtilities;
import groovy.lang.GroovyShell;

import java.io.UnsupportedEncodingException;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;

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
public abstract class UrlCommandCell implements CommandCell
{
    private String cmd;
    private transient String cmdHash;
    private volatile transient Class runnableCode = null;
    private volatile transient String errorMsg = null;
    static final String proxyServer;
    static final int proxyPort;
    private static final String nullSHA1 = EncryptionUtilities.calculateSHA1Hash("".getBytes());

    private String url = null;
    private final boolean cachable;
    private Object cache = null;
    private boolean isUrlExpanded;
    //private volatile AtomicBoolean isUrlExpanded = new AtomicBoolean(false);

    static
    {
        /**
         * These shouldn't be set this way.  If they are going to be environment variables
         * they should set it at Tomcat or application startup.  Then all proxying will
         * be done automatically.
         */
        proxyServer = SystemUtilities.getExternalVariable("http.proxy.host");
        String port = SystemUtilities.getExternalVariable("http.proxy.port");
        if (proxyServer != null)
        {
            try
            {
                proxyPort = Integer.parseInt(port);
            }
            catch (Exception e)
            {
                throw new IllegalArgumentException("http.proxy.port must be an integer: " + port, e);
            }
        }
        else
        {
            proxyPort = 0;
        }
    }



    public UrlCommandCell(String cmd, boolean cachable)
    {
        this.cmd = cmd;
        this.cachable = cachable;
    }

    public void setUrl(String url)
    {
        this.url = url;
    }

    public String getUrl()
    {
        return url;
    }

    public Object fetch(Map args)
    {

        try
        {
            return fetchContentFromUrl();
        }
        catch (Exception e)
        {
            NCube ncube = (NCube) args.get("ncube");
            setErrorMessage("Failed to load cell contents from URL: " + getUrl() + ", NCube '" + ncube.getName() + "'");
            throw new IllegalStateException(getErrorMessage(), e);
        }
    }

    protected Object fetchContentFromUrl()
    {
        return UrlUtilities.getContentFromUrlAsString(getUrl(), proxyServer, proxyPort, null, null, true);
    }

    public synchronized void cache(Object o) {
        if (cachable) {
            cache = o;
        }
    }

    public void expandUrl(String url, Map args)
    {
        synchronized(this) {
            if (this.isUrlExpanded) {
                return;
            }
        }

        NCube ncube = (NCube) args.get("ncube");
        Matcher m = Regexes.groovyRelRefCubeCellPatternA.matcher(url);
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

        synchronized(this) {
            this.url = expandedUrl.toString();
            this.isUrlExpanded = true;
        }
    }

    public boolean isCachable() {
        return cachable;
    }


    public static NCube getNCube(Map args)
    {
        NCube ncube = (NCube) args.get("ncube");
        if (ncube == null)
        {
            throw new IllegalStateException("'ncube' not set for CommandCell to execute.  Arguments: " + args);
        }
        return ncube;
    }

    public static Map getInput(Map args)
    {
        Map input = (Map) args.get("input");
        if (input == null)
        {
            throw new IllegalStateException("'input' not set for CommandCell to execute.  Arguments: " + args);
        }
        return input;
    }

    public static Map getOutput(Map args)
    {
        Map output = (Map) args.get("output");
        if (output == null)
        {
            throw new IllegalStateException("'output' not set for CommandCell to execute.  Arguments: " + args);
        }
        return output;

    }

    public boolean equals(Object other)
    {
        if (!(other instanceof CommandCell))
        {
            return false;
        }

        CommandCell that = (CommandCell) other;

        if (cmd != null) {
            return cmd.equals(that.getCmd());
        }

        return url.equals(that.getUrl());
    }

    public int hashCode()
    {
        return cmd == null ? url == null ? 0 : url.hashCode() : cmd.hashCode();
    }

    public synchronized boolean isCached() {
        return this.cache != null;
    }

    public void prepare(Object cmd, Map ctx) {
    }

    public Class getRunnableCode()
    {
        return runnableCode;
    }

    public void setRunnableCode(Class runnableCode)
    {
        this.runnableCode = runnableCode;
    }


    public String getCmd()
    {
        return cmd;
    }

    public synchronized Object getOperableCmd() {

        return cache == null ? (cmd == null || cmd.isEmpty()) ? null : cmd : cache;
    }


    public String getCmdHash(String cmd)
    {
        if (StringUtilities.isEmpty(cmd))
        {
            return nullSHA1;
        }

        if (cmdHash == null)
        {
            try
            {
                cmdHash = EncryptionUtilities.calculateSHA1Hash(cmd.getBytes("UTF-8"));
            }
            catch (UnsupportedEncodingException e)
            {
                cmdHash = EncryptionUtilities.calculateSHA1Hash(cmd.getBytes());
            }
        }
        return cmdHash;
    }


    //public void setCmd(String cmd)
    {
        this.cmd = cmd;
    }

    public String toString()
    {
        return url == null ? cmd : url;
    }

    public void failOnErrors() {
        if (errorMsg != null) {
            throw new IllegalStateException(errorMsg);
        }
    }

    public void setErrorMessage(String errorMsg)
    {
        this.errorMsg = errorMsg;
    }
    public String getErrorMessage() { return this.errorMsg; }

    public int compareTo(CommandCell cmdCell)
    {
        if (cmd != null) {
            return cmd.compareToIgnoreCase(cmdCell.getCmd());
        }

        return url.compareToIgnoreCase(cmdCell.getUrl());
    }

    public void getCubeNamesFromCommandText(Set<String> cubeNames) {}

    public void getScopeKeys(Set<String> scopeKeys) {}

}
