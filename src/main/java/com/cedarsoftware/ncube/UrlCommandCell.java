package com.cedarsoftware.ncube;

import com.cedarsoftware.ncube.util.CdnRouter;
import com.cedarsoftware.util.EncryptionUtilities;
import com.cedarsoftware.util.IOUtilities;
import com.cedarsoftware.util.StringUtilities;
import com.cedarsoftware.util.SystemUtilities;
import com.cedarsoftware.util.UrlUtilities;
import groovy.lang.GroovyClassLoader;
import groovy.lang.GroovyShell;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;

/**
 * * @author John DeRegnaucourt (jdereg@gmail.com)
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
    static final String proxyServer;
    static final int proxyPort;
    private String cmd;
    private transient String cmdHash;
    private volatile transient Class runnableCode = null;
    private volatile transient String errorMsg = null;
    private static final String nullSHA1 = EncryptionUtilities.calculateSHA1Hash("".getBytes());
    private String url = null;
    private final boolean cacheable;
    private AtomicBoolean isUrlExpanded = new AtomicBoolean(false);
    private AtomicBoolean hasBeenFetched = new AtomicBoolean(false);
    private Object cache;
    private int hash;
    private static final GroovyShell shell = new GroovyShell();

    static
    {
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

    public UrlCommandCell(String cmd, String url, boolean cacheable)
    {
        if (cmd == null && url == null)
        {
            throw new IllegalArgumentException("Both 'cmd' and 'url' cannot be null");
        }

        if (cmd != null && cmd.isEmpty())
        {
            throw new IllegalArgumentException("'cmd' cannot be empty");
        }

        this.cmd = cmd;
        this.url = url;
        this.cacheable = cacheable;
        this.hash = cmd == null ? url.hashCode() : cmd.hashCode();
    }

    public String getUrl()
    {
        return url;
    }

    public Object fetch(Map args)
    {
        if (!cacheable)
        {
            return fetchContentFromUrl(args);
        }

        if (hasBeenFetched.get())
        {
            return cache;
        }

        synchronized (this)
        {
            if (hasBeenFetched.get())
            {
                return cache;
            }

            cache = fetchContentFromUrl(args);
            hasBeenFetched.set(true);
            return cache;
        }
    }

    protected Object fetchContentFromUrl(Map args)
    {
        Map input = (Map) args.get("input");
        if (input.containsKey(CdnRouter.HTTP_REQUEST) && input.containsKey(CdnRouter.HTTP_RESPONSE))
        {
            return proxyFetch(args);
        }
        else
        {
            return simpleFetch(args);
        }
    }

    protected Object proxyFetch(Map args)
    {
        NCube cube = getNCube(args);
        Map input = getInput(args);
        if (cacheable)
        {
            throw new IllegalStateException("Cache must be 'false' if content is being fetched from CDN via CdnRouter, input: " + input);
        }

        HttpServletRequest request = (HttpServletRequest) input.get(CdnRouter.HTTP_REQUEST);
        HttpServletResponse response = (HttpServletResponse) input.get(CdnRouter.HTTP_RESPONSE);
        HttpURLConnection conn = null;
        URL actualUrl = null;
        try
        {
            actualUrl = getActualUrl(cube.getVersion(), cube.getName());
            URLConnection connection = actualUrl.openConnection();
            if (!(connection instanceof HttpURLConnection))
            {   // Handle a "file://" URL
                connection.connect();
                transferResponseHeaders(connection, response);
                transferFromServer(connection, response);
                return null;
            }
            conn = (HttpURLConnection) connection;
            conn.setAllowUserInteraction(false);
            conn.setRequestMethod("GET");
            conn.setDoOutput(true);
            conn.setDoInput(true);
            conn.setReadTimeout(20000);
            conn.setConnectTimeout(10000);

            setupRequestHeaders(conn, request);
            conn.connect();
            transferToServer(conn, request);

            int resCode = conn.getResponseCode();

            if (resCode <= HttpServletResponse.SC_PARTIAL_CONTENT)
            {
                transferResponseHeaders(conn, response);
                return transferFromServer(conn, response);
            }
            else
            {
                UrlUtilities.readErrorResponse(conn);
                response.sendError(resCode, conn.getResponseMessage());
                return null;
            }
        }
        catch (SocketTimeoutException ignored) {
            try
            {
                response.sendError(HttpServletResponse.SC_NOT_FOUND, "File not found:  " + actualUrl.toString());

            }
            catch (IOException ignore)
            {
            }
        }
        catch (Exception e)
        {
            try
            {
                UrlUtilities.readErrorResponse(conn);
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Invalid url provided:  " + actualUrl.toString());
            }
            catch (IOException ignored)
            {
            }
        }
        return null;
    }

    protected Object simpleFetch(Map args)
    {
        NCube cube = getNCube(args);

        try
        {
            URL u = getActualUrl(cube.getVersion(), cube.getName());
            //TODO:  java-util change remove u.toString() when we have a URL version of this call
            return UrlUtilities.getContentFromUrlAsString(u.toString(), proxyServer, proxyPort, null, null, true);
        }
        catch (Exception e)
        {
            setErrorMessage("Failed to load cell contents from URL: " + getUrl() + ", NCube '" + cube.getName() + "'");
            throw new IllegalStateException(getErrorMessage(), e);
        }
    }

    protected URL getActualUrl(String version, String ncubeName) throws MalformedURLException
    {
        URL actualUrl;

        try
        {
            String localUrl = (url != null) ? url.toLowerCase() : null;

            if (localUrl != null && (localUrl.startsWith("http:") || localUrl.startsWith("https:") || localUrl.startsWith("file:")))
            {
                actualUrl = new URL(url);
            }
            else
            {
                GroovyClassLoader loader = (GroovyClassLoader)NCubeManager.getUrlClassLoader(version);
                if (loader == null)
                {
                    throw new IllegalStateException("No root URLs are set for relative path resources to be loaded, ncube: " + ncubeName + ", version: " + version);
                }
                actualUrl = loader.getResource(url);
            }
        }
        catch (Exception e)
        {
            throw new IllegalArgumentException("Invalid URL:  " + url + ", ncube: " + ncubeName + ", version: " + version, e);
        }

        if (actualUrl == null)
        {
            throw new IllegalStateException("n-cube cell URL resolved to null, url: " + getUrl() + ", ncube: " + ncubeName + ", version: " + version);
        }
        return actualUrl;
    }

    public void expandUrl(Map args)
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
            NCube ncube = (NCube) args.get("ncube");
            Matcher m = Regexes.groovyRelRefCubeCellPatternA.matcher(url);
            StringBuilder expandedUrl = new StringBuilder();
            int last = 0;
            Map input = (Map) args.get("input");

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
            url = expandedUrl.toString();
            isUrlExpanded.set(true);
        }
    }

    public boolean isCacheable()
    {
        return cacheable;
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

    public void prepare(Object cmd, Map ctx)
    {
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

    public String getCmdHash(String command)
    {
        if (StringUtilities.isEmpty(command))
        {
            return nullSHA1;
        }

        if (cmdHash == null)
        {
            try
            {
                cmdHash = EncryptionUtilities.calculateSHA1Hash(command.getBytes("UTF-8"));
            }
            catch (UnsupportedEncodingException e)
            {
                cmdHash = EncryptionUtilities.calculateSHA1Hash(command.getBytes());
            }
        }
        return cmdHash;
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

    public void setErrorMessage(String errorMsg)
    {
        this.errorMsg = errorMsg;
    }

    public String getErrorMessage()
    {
        return this.errorMsg;
    }

    public int compareTo(CommandCell cmdCell)
    {
        if (cmd != null)
        {
            String safeCmd = cmdCell.getCmd();
            return cmd.compareTo(safeCmd == null ? "" : safeCmd);
        }

        String safeUrl = cmdCell.getUrl();
        return url.compareTo(safeUrl == null ? "" : safeUrl);
    }

    public void getCubeNamesFromCommandText(Set<String> cubeNames)
    {
    }

    public void getScopeKeys(Set<String> scopeKeys)
    {
    }

    public Object execute(Map<String, Object> ctx)
    {
        failOnErrors();

        Object data;

        if (getUrl() == null)
        {
            data = getCmd();
        }
        else
        {
            expandUrl(ctx);
            data = fetch(ctx);
        }

        prepare(data, ctx);
        return executeInternal(data, ctx);
    }

    protected abstract Object executeInternal(Object data, Map<String, Object> ctx);

    private void transferToServer(URLConnection conn, HttpServletRequest request) throws IOException
    {
        OutputStream out = null;
        InputStream in = null;

        try
        {
            in = request.getInputStream();
            out = new BufferedOutputStream(conn.getOutputStream(), 32768);
            IOUtilities.transfer(in, out);
        }
        finally
        {
            IOUtilities.close(in);
            IOUtilities.close(out);
        }
    }

    private Object transferFromServer(URLConnection conn, HttpServletResponse response) throws IOException
    {
        InputStream in = null;
        OutputStream out = null;
        try
        {
            if (cacheable) {
                in = new CachingInputStream(new BufferedInputStream(conn.getInputStream(), 32768));
            } else {
                in = new BufferedInputStream(conn.getInputStream(), 32768);
            }
            out = response.getOutputStream();
            IOUtilities.transfer(in, out);

            return cacheable ? ((CachingInputStream) in).getCache() : null;
        }
        finally
        {
            IOUtilities.close(in);
            IOUtilities.close(out);
        }
    }

    private void setupRequestHeaders(URLConnection c, HttpServletRequest request)
    {
        Enumeration headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements())
        {
            String key = (String) headerNames.nextElement();
            String value = request.getHeader(key);
            c.setRequestProperty(key, value);
        }
    }

    private void transferResponseHeaders(URLConnection c, HttpServletResponse response)
    {
        Map<String, List<String>> headerFields = c.getHeaderFields();

        addHeaders("Content-Length", headerFields, response);
        addHeaders("Last-Modified", headerFields, response);
        addHeaders("Expires", headerFields, response);
        addHeaders("Content-Encoding", headerFields, response);
        addHeaders("Content-Type", headerFields, response);
        addHeaders("Cache-Control", headerFields, response);
        addHeaders("Etag", headerFields, response);
        addHeaders("Accept", headerFields, response);
    }

    private void addHeaders(String field, Map<String, List<String>> fields, HttpServletResponse response)
    {
        List<String> items = fields.get(field);

        if (items != null)
        {
            for (String s : items)
            {
                response.addHeader(field, s);
            }
        }
    }

    private class CachingInputStream extends FilterInputStream
    {
        ByteArrayOutputStream _out = new ByteArrayOutputStream();

        /**
         * Creates a <code>FilterInputStream</code>
         * by assigning the  argument <code>in</code>
         * to the field <code>this.in</code> so as
         * to remember it for later use.
         * @param in the underlying input stream, or <code>null</code> if
         *           this instance is to be created without an underlying stream.
         */
        protected CachingInputStream(InputStream in)
        {
            super(in);
        }

        public int read(byte[] b, int off, int len) throws IOException
        {
            int count = super.read(b, off, len);
            _out.write(b, off, count);
            return count;
        }

        public int read() throws IOException
        {
            int result = super.read();
            _out.write(result);
            return result;
        }

        public byte[] getCache()
        {
            return _out.toByteArray();
        }
    }
}
