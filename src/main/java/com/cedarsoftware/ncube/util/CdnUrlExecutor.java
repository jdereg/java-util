package com.cedarsoftware.ncube.util;

import com.cedarsoftware.ncube.CommandCell;
import com.cedarsoftware.ncube.executor.DefaultExecutor;
import com.cedarsoftware.util.IOUtilities;
import com.cedarsoftware.util.UrlUtilities;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;

/**
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
public class CdnUrlExecutor extends DefaultExecutor
{
    private HttpServletRequest request;
    private HttpServletResponse response;
    private static final Log LOG = LogFactory.getLog(UrlUtilities.class);

    public CdnUrlExecutor(HttpServletRequest request, HttpServletResponse response)
    {
        this.request = request;
        this.response = response;
    }

    public Object executeCommand(CommandCell command, Map<String, Object> ctx)
    {
        command.failOnErrors();
        // ignore local caching
        if (command.getUrl() != null)
        {
            command.expandUrl(ctx);

            HttpURLConnection conn = null;

            try
            {
                conn = (HttpURLConnection)new URL(command.getUrl()).openConnection();
                conn.setAllowUserInteraction(false);
                conn.setRequestMethod(request.getMethod() != null ? request.getMethod() : "GET");
                conn.setDoOutput(true); // true
                conn.setDoInput(true); // true
                conn.setReadTimeout(220000);
                conn.setConnectTimeout(45000);

                setupRequestHeaders(conn);
                conn.connect();
                transferToServer(conn);

                int resCode = conn.getResponseCode();

                if (resCode <= HttpServletResponse.SC_PARTIAL_CONTENT)
                {
                    transferResponseHeaders(conn);
                    transferFromServer(conn);
                }
                else
                {
                    UrlUtilities.readErrorResponse(conn);
                    response.sendError(resCode, conn.getResponseMessage());
                }
            }
            catch (Exception e)
            {
                try
                {
                    LOG.warn(e.getMessage(), e);
                    UrlUtilities.readErrorResponse(conn);
                    response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
                }
                catch (IOException e1)
                {
                    LOG.warn(e1.getMessage());
                }
            }
        }

        return null;
    }

    private void transferToServer(HttpURLConnection conn) throws IOException
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

    private void transferFromServer(HttpURLConnection conn) throws IOException
    {
        InputStream in = null;
        OutputStream out = null;
        try
        {
            in = new BufferedInputStream(conn.getInputStream(), 32768);
            out = response.getOutputStream();
            IOUtilities.transfer(in, out);
        }
        finally
        {
            IOUtilities.close(in);
            IOUtilities.close(out);
        }
    }

    private void setupRequestHeaders(HttpURLConnection c)
    {
        Enumeration headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements())
        {
            String key = (String) headerNames.nextElement();
            String value = request.getHeader(key);
            c.setRequestProperty(key, value);
        }
    }

    private void transferResponseHeaders(HttpURLConnection c)
    {
        Map<String, List<String>> headerFields = c.getHeaderFields();

        addHeaders("Content-Length", headerFields);
        addHeaders("Last-Modified", headerFields);
        addHeaders("Expires", headerFields);
        addHeaders("Content-Encoding", headerFields);
        addHeaders("Content-Type", headerFields);
        addHeaders("Cache-Control", headerFields);
        addHeaders("Etag", headerFields);
        addHeaders("Accept", headerFields);
    }

    private void addHeaders(String field, Map<String, List<String>> fields)
    {
        List<String> items = fields.get(field);

        if (items != null)
        {
            for (String s: items)
            {
                response.addHeader(field, s);
            }
        }
    }
}

