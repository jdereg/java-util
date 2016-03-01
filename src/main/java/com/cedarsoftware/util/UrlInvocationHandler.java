package com.cedarsoftware.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;

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
 *
 * InvocationHandler handler = new UrlInvocationHandler(new UrlInvocationHandlerStrategyImplementation(url, ...));
 * Explorer explorer = (Explorer) ProxyFactory.create(Explorer.class, handler);
 *
 * At this point, your Java code can do this:
 *
 * List files = explorer.getFiles(userId);
 * </pre>
 *
 * @author Ken Partlow (kpartlow@gmail.com)
 * @author John DeRegnaucourt (john@cedarsoftware.com)
 *         <br>
 *         Copyright (c) Cedar Software LLC
 *         <br><br>
 *         Licensed under the Apache License, Version 2.0 (the "License");
 *         you may not use this file except in compliance with the License.
 *         You may obtain a copy of the License at
 *         <br><br>
 *         http://www.apache.org/licenses/LICENSE-2.0
 *         <br><br>
 *         Unless required by applicable law or agreed to in writing, software
 *         distributed under the License is distributed on an "AS IS" BASIS,
 *         WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *         See the License for the specific language governing permissions and
 *         limitations under the License.
 */
public class UrlInvocationHandler implements InvocationHandler
{
    public static final int SLEEP_TIME = 5000;
    private final Logger LOG = LogManager.getLogger(UrlInvocationHandler.class);
    private final UrlInvocationHandlerStrategy _strategy;

    public UrlInvocationHandler(UrlInvocationHandlerStrategy strategy)
    {
        _strategy = strategy;
    }

    public Object invoke(Object proxy, Method m, Object[] args) throws Throwable
    {
        int retry = _strategy.getRetryAttempts();
        Object result = null;
        do
        {
            HttpURLConnection c = null;
            try
            {
                c = (HttpURLConnection) UrlUtilities.getConnection(_strategy.buildURL(proxy, m, args), true, true, false);
                c.setRequestMethod("POST");

                _strategy.setCookies(c);

                // Formulate the POST data for the output stream.
                byte[] bytes = _strategy.generatePostData(proxy, m, args);
                c.setRequestProperty("Content-Length", String.valueOf(bytes.length));

                _strategy.setRequestHeaders(c);

                // send the post data
                IOUtilities.transfer(c, bytes);

                _strategy.getCookies(c);

                // Get the return value of the call
                result = _strategy.readResponse(c);
            }
            catch (ThreadDeath e)
            {
                throw e;
            }
            catch (Throwable e)
            {
                LOG.error("Error occurred getting HTTP response from server", e);
                UrlUtilities.readErrorResponse(c);
                if (retry-- > 0)
                {
                    Thread.sleep(_strategy.getRetrySleepTime());
                }
            }
            finally
            {
                UrlUtilities.disconnect(c);
            }
        } while (retry > 0);

        try
        {
            checkForThrowable(result);
        } catch (Throwable t) {
            LOG.error("Error occurred on server", t);
            return null;
        }
        return result;
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
}
