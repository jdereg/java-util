package com.cedarsoftware.util;

import com.cedarsoftware.util.io.JsonReader;
import com.cedarsoftware.util.io.JsonWriter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;

/**
 * Useful utility for allowing Java code to make Ajax calls, yet the Java code
 * can make these calls via Dynamic Proxies created from Java interfaces for
 * the remote server(s).
 *
 * Example:
 *
 * Assume you have a tomcat instance running a JSON Command Servlet, like com.cedarsoftware's or
 * Spring MVC.
 *
 * Assume you have a Java interface 'Explorer' that is mapped to a Java bean that you are allowing
 * to be called through RESTful JSON calls (Ajax / XHR).
 *
 * Explorer has methods on it, like getFiles(userId), etc.
 *
 * You need to use a SessionAware (JSESSIONID only) or CookieAware UrlInvocationHandler to interact
 * with the server so that the cookies will be placed on all requests.  In Javascript within browsers,
 * this is taken care of for you.  Not so in the Java side.
 * <pre>
 * Map cookies = new HashMap();
 * String url = "http://www.mycompany.com:80/json/"
 * InvocationHandler handler = new CookieAwareUrlInvocationHandler(new URL(url), cookies);
 *  // This will handle all cookies, or you could use (where 'sessionId' holds the value of the JSESSIONID
 * InvocationHandler handler = new SessionAwareUrlInvocationHandler(new URL(url), sessionId);
 *
 * Explorer explorer = (Explorer) ProxyFactory.create(Explorer.class, handler);
 *
 * At this point, your Java code can do this:
 *
 * List files = explorer.getFiles(userId);
 * </pre>
 *
 * @author John DeRegnaucourt (jdereg@gmail.com) & Ken Partlow (kpartlow@gmail.com)
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
public class UrlInvocationHandler implements InvocationHandler
{
    private final URL _url;
    public static int SLEEP_TIME = 5000;
    public static int RETRY_ATTEMPTS = 20;
    private static final Log LOG = LogFactory.getLog(UrlInvocationHandler.class);

    public UrlInvocationHandler(URL url)
    {
        _url = url;
    }

    public Object invoke(Object proxy, Method m, Object[] args) throws Throwable
    {
        int retry = RETRY_ATTEMPTS;
        while (retry > 0)
        {
            HttpURLConnection c = null;

            try
            {
                c = (HttpURLConnection) UrlUtilities.getConnection(_url, true, true, false);
                c.setRequestMethod("POST");
                setCookies(c);

                // Formulate the JSON call as a String
                ByteArrayOutputStream ba_out = new ByteArrayOutputStream();
                JsonWriter jwr = new JsonWriter(ba_out);
                jwr.write(new Object[] {m.getName(), args});
                IOUtilities.close(jwr);

                if (LOG.isDebugEnabled())
                {    // DEBUG
                    String jsonCall = new String(ba_out.toByteArray(), "UTF-8");
                    LOG.debug("Calling MOD server:\n    " + jsonCall);
                }

                c.setRequestProperty("Content-Length", String.valueOf(ba_out.size()));
                OutputStream out = c.getOutputStream();
                ba_out.writeTo(out);
                IOUtilities.close(out);
                getCookies(c);

                // Get the return value of the call
                JsonReader reader;

                if (LOG.isDebugEnabled())
                {
                    ByteArrayOutputStream input = new ByteArrayOutputStream(32768);
                    IOUtilities.transfer(IOUtilities.getInputStream(c), input);
                    byte[] bytes = input.toByteArray();
                    String jsonResp = new String(bytes, "UTF-8");
                    LOG.debug(jsonResp);
                    reader = new JsonReader(new ByteArrayInputStream(bytes));
                }
                else
                {
                    reader = new JsonReader(IOUtilities.getInputStream(c));
                }
                Object[] res = (Object[]) reader.readObject();

                // Do we need to close reader?  Don't want to stop http keep alives.
                reader.close();
                Object result = res[0];
                checkForThrowable(result);
                return result;
            }
            catch (ThreadDeath e)
            {
                throw e;
            }
            catch (Throwable e)
            {
                LOG.error("Error occurred getting HTTP response from server", e);
                UrlUtilities.readErrorResponse(c);
                Thread.sleep(SLEEP_TIME);
                retry--;
            }
            finally
            {
                UrlUtilities.disconnect(c);
            }
        }
        return null;
    }

    protected static void checkForThrowable(Object object) throws Throwable
    {
        if (object instanceof Throwable)
        {
            Throwable t;
            if (object instanceof InvocationTargetException)
            {
                InvocationTargetException i = (InvocationTargetException) object;
                t = i.getTargetException();
                if (t == null)
                {
                    t = (Throwable) object;
                }
            }
            else
            {
                t = (Throwable) object;
            }

            t.fillInStackTrace();
            throw t;
        }
    }

    protected void getCookies(URLConnection c) throws IOException
    {
    }

    protected void setCookies(URLConnection conn) throws IOException
    {
    }
}
