package com.cedarsoftware.ncube;

import com.cedarsoftware.ncube.util.CdnRouter;
import com.cedarsoftware.util.EncryptionUtilities;
import com.cedarsoftware.util.IOUtilities;
import com.cedarsoftware.util.StringUtilities;
import com.cedarsoftware.util.SystemUtilities;
import com.cedarsoftware.util.UrlUtilities;
import groovy.lang.GroovyShell;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLConnection;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
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
    static final String proxyServer;
    static final int proxyPort;
    private String cmd;
    private transient String cmdHash;
    private volatile transient Class runnableCode = null;
    private volatile transient String errorMsg = null;
    private String url = null;
    private final boolean cacheable;
    private AtomicBoolean isUrlExpanded = new AtomicBoolean(false);
    private AtomicBoolean hasBeenFetched = new AtomicBoolean(false);
    private Object cache;
    private int hash;
    private static final GroovyShell shell = new GroovyShell();
    private static Map<String, String> extToMimeType = new ConcurrentHashMap<>();
    private static final Log LOG = LogFactory.getLog(CdnRouter.class);


    //TODO  These are really not needed and should be set for the environment with the following -D options
    // or could be set as environment variables
    // http.proxyHost
    // http.proxyPort (default: 80)
    // http.nonProxyHosts (default: <none>), but should alwasy include localhost
    // https.proxyHost
    // https.proxyPort
    // Example:  -Dhttp.proxyHost=proxy.example.org -Dhttp.proxyPort=8080 -Dhttps.proxyHost=proxy.example.org -Dhttps.proxyPort=8080 -Dhttp.nonProxyHosts=*.foo.com|localhost|*.td.afg
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

        extToMimeType.put(".css", "text/css");
        extToMimeType.put(".html", "text/html");
        extToMimeType.put(".js", "application/javascript");
        extToMimeType.put(".xml", "application/xml");
        extToMimeType.put(".json", "application/json");
        extToMimeType.put(".jpg", "image/jpeg");
        extToMimeType.put(".png", "image/png");
        extToMimeType.put(".gif", "image/gif");
        extToMimeType.put(".bmp", "image/bmp");
    }

    public UrlCommandCell(String cmd, String url, boolean cacheable)
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
        Map input = getInput(args);
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
        HttpServletRequest request = (HttpServletRequest) input.get(CdnRouter.HTTP_REQUEST);
        HttpServletResponse response = (HttpServletResponse) input.get(CdnRouter.HTTP_RESPONSE);
        HttpURLConnection conn = null;
        URL actualUrl = null;

        try
        {
            actualUrl = getActualUrl(cube);
            URLConnection connection = actualUrl.openConnection();
            if (!(connection instanceof HttpURLConnection))
            {   // Handle a "file://" URL
                connection.connect();
                addFileHeader(actualUrl, response);
                return transferFromServer(connection, response);
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
        catch (SocketTimeoutException e)
        {
            try
            {
                LOG.warn("Socket time out occurred fetching: " + actualUrl, e);
                response.sendError(HttpServletResponse.SC_NOT_FOUND, "File not found: " + actualUrl.toString());
            }
            catch (IOException ignore) { }
        }
        catch (Exception e)
        {
            try
            {
                LOG.error("Error occurred fetching: " + actualUrl, e);
                UrlUtilities.readErrorResponse(conn);
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
            }
            catch (IOException ignored) { }
        }
        return null;
    }

    static void addFileHeader(URL actualUrl, HttpServletResponse response)
    {
        if (actualUrl == null)
        {
            return;
        }

        String url = actualUrl.toString().toLowerCase();

        for (Map.Entry<String, String> entry : extToMimeType.entrySet())
        {
            if (url.endsWith(entry.getKey()))
            {
                response.addHeader("content-type", entry.getValue());
                break;
            }
        }
    }

    protected Object simpleFetch(Map args)
    {
        NCube cube = getNCube(args);

        try
        {
            URL u = getActualUrl(cube);
            //TODO:  java-util change remove u.toString() when we have a URL version of this call
            return UrlUtilities.getContentFromUrlAsString(u.toString(), proxyServer, proxyPort, null, null, true);
        }
        catch (Exception e)
        {
            setErrorMessage("Failed to load cell contents from URL: " + getUrl() + ", n-cube: " + cube.getName() + "', version: " + cube.getVersion());
            throw new IllegalStateException(getErrorMessage(), e);
        }
    }

    protected URL getActualUrl(NCube ncube) throws MalformedURLException
    {
        URL actualUrl;

        try
        {
            String localUrl = url.toLowerCase();

            if (localUrl.startsWith("http:") || localUrl.startsWith("https:") || localUrl.startsWith("file:"))
            {   // Absolute URL
                actualUrl = new URL(url);
            }
            else
            {   // Relative URL
                URLClassLoader loader = NCubeManager.getUrlClassLoader(ncube.getApplicationID().getAppStr(""));
                if (loader == null)
                {
                    // TODO: Make attempt to load them from sys.classpath
                    throw new IllegalStateException("No root URLs are set for relative path resources to be loaded, ncube: " + ncube.getName() + ", version: " + ncube.getVersion());
                }
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
            throw new IllegalArgumentException("Invalid URL:  " + url + ", ncube: " + ncube.getName() + ", version: " + ncube.getVersion(), e);
        }

        if (actualUrl == null)
        {
            throw new IllegalStateException("n-cube cell URL resolved to null, url: " + url + ", ncube: " + ncube.getName() + ", version: " + ncube.getVersion());
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
            NCube ncube = getNCube(args);
            Matcher m = Regexes.groovyRelRefCubeCellPatternA.matcher(url);
            StringBuilder expandedUrl = new StringBuilder();
            int last = 0;
            Map input = getInput(args);

            while (m.find())
            {
                expandedUrl.append(url.substring(last, m.start()));
                String cubeName = m.group(2);
                NCube refCube = NCubeManager.getCube(cubeName, ncube.getApplicationID());
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
        return ncube;
    }

    public static Map getInput(Map args)
    {
        Map input = (Map) args.get("input");
        return input;
    }

    public static Map getOutput(Map args)
    {
        Map output = (Map) args.get("output");
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

    public void prepare(Object command, Map ctx)
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
        if (cmdHash == null)
        {
            cmdHash = EncryptionUtilities.calculateSHA1Hash(StringUtilities.getBytes(command, "UTF-8"));
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

    private static void transferToServer(URLConnection conn, HttpServletRequest request) throws IOException
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
            in = new BufferedInputStream(conn.getInputStream(), 32768);
            if (cacheable)
            {
                in = new CachingInputStream(in);
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

    private static void setupRequestHeaders(URLConnection c, HttpServletRequest request)
    {
        Enumeration headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements())
        {
            String key = (String) headerNames.nextElement();
            String value = request.getHeader(key);
            c.setRequestProperty(key, value);
        }
    }

    private static void transferResponseHeaders(URLConnection c, HttpServletResponse response)
    {
        Map<String, List<String>> headerFields = c.getHeaderFields();
        Set<Map.Entry<String, List<String>>> entries = headerFields.entrySet();

        for (Map.Entry<String, List<String>> entry : entries)
        {
            if (entry.getValue() != null && entry.getKey() != null)
            {
                for (String s : entry.getValue())
                {
                    response.addHeader(entry.getKey(), s);
                }
            }
        }
    }

    static class CachingInputStream extends FilterInputStream
    {
        ByteArrayOutputStream cache = new ByteArrayOutputStream();

        /**
         * Creates a {@code FilterInputStream}
         * by assigning the  argument {@code in}
         * to the field {@code this.in} so as
         * to remember it for later use.
         * @param in the underlying input stream, or {@code null} if
         *           this instance is to be created without an underlying stream.
         */
        protected CachingInputStream(InputStream in)
        {
            super(in);
        }

        public int read(byte[] b, int off, int len) throws IOException
        {
            int count = super.read(b, off, len);
            if (count != -1)
            {
                cache.write(b, off, count);
            }
            return count;
        }

        public int read() throws IOException
        {
            int result = super.read();
            if (result != -1)
            {
                cache.write(result);
            }
            return result;
        }

        public byte[] getCache()
        {
            return cache.toByteArray();
        }
    }
}
