package com.cedarsoftware.util;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.text.ParseException;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Useful utilities for working with UrlConnections and IO.
 *
 * @author John DeRegnaucourt (jdereg@gmail.com) & Ken Partlow
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
public class UrlUtilities
{
    private static final Log LOG = LogFactory.getLog(UrlUtilities.class);
    private static String _referer = null;
    private static String _userAgent = null;
    private static final Pattern resPattern = Pattern.compile("^res\\:\\/\\/", Pattern.CASE_INSENSITIVE);
    public static final String SET_COOKIE = "Set-Cookie";
    public static final String COOKIE_VALUE_DELIMITER = ";";
    public static final String PATH = "path";
    public static final String EXPIRES = "expires";
    public static final SafeSimpleDateFormat DATE_FORMAT = new SafeSimpleDateFormat("EEE, dd-MMM-yyyy hh:mm:ss z");
    public static final String SET_COOKIE_SEPARATOR = "; ";
    public static final String COOKIE = "Cookie";
    public static final char NAME_VALUE_SEPARATOR = '=';
    public static final char DOT = '.';

    public static final TrustManager[] NAIVE_TRUST_MANAGER = new TrustManager[]
    {
            new X509TrustManager()
            {
                public void checkClientTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException
                {
                }
                public void checkServerTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException
                {
                }
                public X509Certificate[] getAcceptedIssuers()
                {
                    return null;
                }
            }
    };

    public static final HostnameVerifier NAIVE_VERIFIER = new HostnameVerifier()
    {
        public boolean verify(String s, SSLSession sslSession)
        {
            return true;
        }
    };

    static
    {
        try
        {
            // Default new HTTP connections to follow redirects
            HttpURLConnection.setFollowRedirects(true);
        }
        catch (Exception ignored) {}
    }

    private UrlUtilities()
    {
        super();
    }

    public static void setReferer(String referer)
    {
        _referer = referer;
    }

    public static void setUserAgent(String userAgent)
    {
        _userAgent = userAgent;
    }

    public static String getUserAgent()
    {
        return _userAgent;
    }

    private static void setProperty(String name, String value)
    {
        System.setProperty(name, value);
    }

    public static URLConnection getConnection(String url, boolean input, boolean output, boolean cache) throws IOException
    {
        return getConnection(new URL(url), input, output, cache);
    }

    public static URLConnection getConnection(URL url, boolean input, boolean output, boolean cache) throws IOException
    {
        URLConnection c = url.openConnection();
        c.setRequestProperty("Accept-Encoding", "gzip, deflate");
        if (StringUtilities.hasContent(_referer))
        {
            c.setRequestProperty("Referer", _referer);
        }
        if (StringUtilities.hasContent(_userAgent))
        {
            c.setRequestProperty("User-Agent", _userAgent);
        }
        c.setAllowUserInteraction(false);
        c.setDoOutput(output); // true
        c.setDoInput(input); // true
        c.setUseCaches(cache);  // false

        setTimeouts(c, 220000, 45000);
        return c;
    }

    public static void setTimeouts(URLConnection c, int read, int connect)
    {
        c.setReadTimeout(read);
        c.setConnectTimeout(connect);
    }

    public static void readErrorResponse(URLConnection c)
    {
        if (c == null)
        {
            return;
        }
        InputStream in = null;
        try
        {
            int error = ((HttpURLConnection) c).getResponseCode();
            in = ((HttpURLConnection) c).getErrorStream();
            if (in == null)
            {
                return;
            }
            LOG.warn("error response: " + ((HttpURLConnection) c).getResponseMessage());
            // read the response body
            ByteArrayOutputStream out = new ByteArrayOutputStream(1024);
            int count;
            byte[] bytes = new byte[8192];
            while ((count = in.read(bytes)) != -1)
            {
                out.write(bytes, 0, count);
            }
            LOG.warn("error Code:  " + error);
        }
        catch (ConnectException e)
        {
            LOG.error("Connection exception trying to read error response", e);
        }
        catch (IOException e)
        {
            LOG.error("IO Exception trying to read error response", e);
        }
        catch (Exception e)
        {
            LOG.error("Exception trying to read error response", e);
        }
        finally
        {
            IOUtilities.close(in);
        }
    }

    public static void disconnect(HttpURLConnection c)
    {
        if (c != null)
        {
            try
            {
                c.disconnect();
            }
            catch (Exception ignored) {}
        }
    }

    /**
     * Retrieves and stores cookies returned by the host on the other side of
     * the open java.net.URLConnection.
     * <p/>
     * The connection MUST have been opened using the connect() method or a
     * IOException will be thrown.
     *
     * @param conn a java.net.URLConnection - must be open, or IOException will
     *             be thrown
     */
    public static void getCookies(URLConnection conn, Map store)
    {
        // let's determine the domain from where these cookies are being sent
        String domain = getCookieDomainFromHost(conn.getURL().getHost());
        Map domainStore; // this is where we will store cookies for this domain

        // now let's check the store to see if we have an entry for this domain
        if (store.containsKey(domain))
        {
            // we do, so lets retrieve it from the store
            domainStore = (Map) store.get(domain);
        }
        else
        {
            // we don't, so let's create it and put it in the store
            domainStore = new ConcurrentHashMap();
            store.put(domain, domainStore);
        }

        if (domainStore.containsKey("JSESSIONID"))
        {
            // No need to continually get the JSESSIONID (and set-cookies header) as this does not change throughout the session.
            return;
        }

        // OK, now we are ready to get the cookies out of the URLConnection
        String headerName;
        for (int i = 1; (headerName = conn.getHeaderFieldKey(i)) != null; i++)
        {
            if (headerName.equalsIgnoreCase(SET_COOKIE))
            {
                Map cookie = new ConcurrentHashMap();
                StringTokenizer st = new StringTokenizer(conn.getHeaderField(i), COOKIE_VALUE_DELIMITER);

                // the specification dictates that the first name/value pair
                // in the string is the cookie name and value, so let's handle
                // them as a special case:

                if (st.hasMoreTokens())
                {
                    String token = st.nextToken();
                    String key = token.substring(0, token.indexOf(NAME_VALUE_SEPARATOR)).trim();
                    String value = token.substring(token.indexOf(NAME_VALUE_SEPARATOR) + 1);
                    domainStore.put(key, cookie);
                    cookie.put(key, value);
                }

                while (st.hasMoreTokens())
                {
                    String token = st.nextToken();
                    int pos = token.indexOf(NAME_VALUE_SEPARATOR);
                    if (pos != -1)
                    {
                        String key = token.substring(0, pos).toLowerCase().trim();
                        String value = token.substring(token.indexOf(NAME_VALUE_SEPARATOR) + 1);
                        cookie.put(key, value);
                    }
                }
            }
        }
    }

    /**
     * Prior to opening a URLConnection, calling this method will set all
     * unexpired cookies that match the path or subpaths for thi underlying URL
     * <p/>
     * The connection MUST NOT have been opened
     * method or an IOException will be thrown.
     *
     * @param conn a java.net.URLConnection - must NOT be open, or IOException will be thrown
     * @throws IOException Thrown if conn has already been opened.
     */
    public static void setCookies(URLConnection conn, Map store) throws IOException
    {
        // let's determine the domain and path to retrieve the appropriate cookies
        URL url = conn.getURL();
        String domain = getCookieDomainFromHost(url.getHost());
        String path = url.getPath();

        Map domainStore = (Map) store.get(domain);
        if (domainStore == null)
        {
            return;
        }
        StringBuilder cookieStringBuffer = new StringBuilder();
        Iterator cookieNames = domainStore.keySet().iterator();

        while (cookieNames.hasNext())
        {
            String cookieName = (String) cookieNames.next();
            Map cookie = (Map) domainStore.get(cookieName);
            // check cookie to ensure path matches and cookie is not expired
            // if all is cool, add cookie to header string
            if (comparePaths((String) cookie.get(PATH), path) && isNotExpired((String) cookie.get(EXPIRES)))
            {
                cookieStringBuffer.append(cookieName);
                cookieStringBuffer.append('=');
                cookieStringBuffer.append((String) cookie.get(cookieName));
                if (cookieNames.hasNext())
                {
                    cookieStringBuffer.append(SET_COOKIE_SEPARATOR);
                }
            }
        }
        try
        {
            conn.setRequestProperty(COOKIE, cookieStringBuffer.toString());
        }
        catch (IllegalStateException e)
        {
            throw new IOException("Illegal State! Cookies cannot be set on a URLConnection that is already connected. "
                    + "Only call setCookies(java.net.URLConnection) AFTER calling java.net.URLConnection.connect().");
        }
    }

    public static String getCookieDomainFromHost(String host)
    {
        while (host.indexOf(DOT) != host.lastIndexOf(DOT))
        {
            host = host.substring(host.indexOf(DOT) + 1);
        }
        return host;
    }

    private static boolean isNotExpired(String cookieExpires)
    {
        if (cookieExpires == null)
        {
            return true;
        }

        try
        {
            return new Date().compareTo(DATE_FORMAT.parse(cookieExpires)) <= 0;
        }
        catch (ParseException e)
        {
            LOG.info("Parse error on cookie expires value: " + cookieExpires, e);
            return false;
        }
    }

    private static boolean comparePaths(String cookiePath, String targetPath)
    {
        if (cookiePath == null)
        {
            return true;
        }
        else if ("/".equals(cookiePath))
        {
            return true;
        }
        else if (targetPath.regionMatches(0, cookiePath, 0, cookiePath.length()))
        {
            return true;
        }
        return false;
    }

    /**
     * Get content from the passed in URL.  This code will open a connection to
     * the passed in server, fetch the requested content, and return it as a
     * byte[].
     *
     * @param url URL to hit
     * @param proxy proxy to use to create connection
     * @return String read from URL or null in the case of error.
     */
    public static String getContentFromUrlAsString(String url, Proxy proxy)
    {
        try
        {
            byte[] bytes = getContentFromUrl(url, proxy);
            return bytes == null ? null : new String(bytes, "UTF-8");
        }
        catch (UnsupportedEncodingException e)
        {
            throw new RuntimeException("UTF-8 not supported by your JVM.  Get a newer JVM.", e);
        }
    }

    /**
     * Get content from the passed in URL.  This code will open a connection to
     * the passed in server, fetch the requested content, and return it as a
     * String.
     *
     * @param url URL to hit
     * @return UTF-8 String read from URL or null in the case of error.
     */
    public static String getContentFromUrlAsString(String url)
    {
        return getContentFromUrlAsString(url, Proxy.NO_PROXY);
    }

    /**
     * Get content from the passed in URL.  This code will open a connection to
     * the passed in server, fetch the requested content, and return it as a
     * byte[].
     *
     * @param url URL to hit
     * @return byte[] read from URL or null in the case of error.
     */
    public static byte[] getContentFromUrl(String url)
    {
        return getContentFromUrl(url, Proxy.NO_PROXY);
    }

    /**
     * Get content from the passed in URL.  This code will open a connection to
     * the passed in server, fetch the requested content, and return it as a
     * byte[].
     *
     * @param url URL to hit
     * @param proxy proxy to use to create connection
     * @return byte[] read from URL or null in the case of error.
     */
    public static byte[] getContentFromUrl(String url, Proxy proxy)
    {
        return getContentFromUrl(url, null, null, proxy, buildNaiveSSLSocketFactory(), NAIVE_VERIFIER);
    }


    /**
     * Get content from the passed in URL.  This code will open a connection to
     * the passed in server, fetch the requested content, and return it as a
     * String.
     *
     * @param url URL to hit
     * @param proxyServer String named of proxy server
     * @param port port to access proxy server
     * @param inCookies Map of session cookies (or null if not needed)
     * @param outCookies Map of session cookies (or null if not needed)
     * @param ignoreSec if true, SSL connection will always be trusted.
     * @return String of content fetched from URL.
     */
    public static String getContentFromUrlAsString(String url, String proxyServer, int port, Map inCookies, Map outCookies, boolean ignoreSec)
    {
        try
        {
            byte[] bytes = getContentFromUrl(url, proxyServer, port, inCookies, outCookies, ignoreSec);
            return bytes == null ? null : new String(bytes, "UTF-8");
        }
        catch (UnsupportedEncodingException e)
        {
            throw new RuntimeException("UTF-8 not supported by your JVM.  Get a newer JVM.", e);
        }
    }

    /**
     * Get content from the passed in URL.  This code will open a connection to
     * the passed in server, fetch the requested content, and return it as a
     * byte[].
     *
     * @param url URL to hit
     * @param proxy Proxy server to create connection (or null if not needed)
     * @param factory custom SSLSocket factory (or null if not needed)
     * @param verifier custom Hostnameverifier (or null if not needed)
     * @return byte[] of content fetched from URL.
     */
    public static byte[] getContentFromUrl(String url, Proxy proxy, SSLSocketFactory factory, HostnameVerifier verifier)
    {
        URLConnection c = null;
        try
        {
            Matcher m = resPattern.matcher(url);
            URL u = m.find() ? UrlUtilities.class.getClassLoader().getResource(url.substring(m.end())) : new URL(url);

            c = getConnection(u, null, true, false, false, proxy, factory, verifier);

            ByteArrayOutputStream out = new ByteArrayOutputStream(16384);
            InputStream stream = IOUtilities.getInputStream(c);
            IOUtilities.transfer(stream, out);
            stream.close();

            return out.toByteArray();
        }
        catch (SSLHandshakeException sslE)
        {
            // Don't read error response.  it will just cause another exception.
            LOG.warn("SSL Exception: " + url, sslE);
            return null;
        }
        catch (Exception e)
        {
            readErrorResponse(c);
            LOG.warn("Exception occurred fetching content from url: " + url, e);
            return null;
        }
        finally
        {
            if (c instanceof HttpURLConnection)
            {
                disconnect((HttpURLConnection)c);
            }
        }
    }

    /**
     * Get content from the passed in URL.  This code will open a connection to
     * the passed in server, fetch the requested content, and return it as a
     * byte[].
     *
     * @param url URL to hit
     * @param inCookies Map of session cookies (or null if not needed)
     * @param outCookies Map of session cookies (or null if not needed)
     * @param proxy Proxy server to create connection (or null if not needed)
     * @param factory custom SSLSocket factory (or null if not needed)
     * @param verifier custom Hostnameverifier (or null if not needed)
     * @return byte[] of content fetched from URL.
     */
    public static byte[] getContentFromUrl(String url, Map inCookies, Map outCookies, Proxy proxy, SSLSocketFactory factory, HostnameVerifier verifier)
    {
        URLConnection c = null;
        try
        {
            Matcher m = resPattern.matcher(url);
            URL u = m.find() ? UrlUtilities.class.getClassLoader().getResource(url.substring(m.end())) : new URL(url);

            c = getConnection(u, inCookies, true, false, false, proxy, factory, verifier);

            ByteArrayOutputStream out = new ByteArrayOutputStream(16384);
            InputStream stream = IOUtilities.getInputStream(c);
            IOUtilities.transfer(stream, out);
            stream.close();

            if (outCookies != null)
            {   // [optional] Fetch cookies from server and update outCookie Map (pick up JSESSIONID, other headers)
                getCookies(c, outCookies);
            }

            return out.toByteArray();
        }
        catch (SSLHandshakeException sslE)
        {
            // Don't read error response.  it will just cause another exception.
            LOG.warn("SSL Exception: " + url, sslE);
            return null;
        }
        catch (Exception e)
        {
            readErrorResponse(c);
            LOG.warn("Exception occurred fetching content from url: " + url, e);
            return null;
        }
        finally
        {
            if (c instanceof HttpURLConnection)
            {
                disconnect((HttpURLConnection)c);
            }
        }
    }

    /**
     * Get content from the passed in URL.  This code will open a connection to
     * the passed in server, fetch the requested content, and return it as a
     * byte[].
     *
     * @param url URL to hit
     * @param proxyServer String named of proxy server
     * @param port port to access proxy server
     * @param inCookies Map of session cookies (or null if not needed)
     * @param outCookies Map of session cookies (or null if not needed)
     * @param ignoreSec if true, SSL connection will always be trusted.
     * @return byte[] of content fetched from URL.
     */
    public static byte[] getContentFromUrl(String url, String proxyServer, int port, Map inCookies, Map outCookies, boolean ignoreSec)
    {
        //  if proxy server is passed
        Proxy proxy = null;
        if (proxyServer != null) {
            proxy = new Proxy(java.net.Proxy.Type.HTTP, new InetSocketAddress(proxyServer, port));
        }

        //  If we are trusting all ssl connections
        SSLSocketFactory factory = null;
        HostnameVerifier verifier = null;
        if (ignoreSec) {
            factory = buildNaiveSSLSocketFactory();
            verifier = NAIVE_VERIFIER;
        }

        return getContentFromUrl(url, inCookies, outCookies, proxy, factory, verifier);
    }

    public static SSLSocketFactory buildNaiveSSLSocketFactory() {
        try {
            //  could be other algorithms (prob need to calculate this another way.
            final SSLContext sslContext = SSLContext.getInstance( "SSL" );
            sslContext.init(null, NAIVE_TRUST_MANAGER, new SecureRandom());

            // Create an ssl socket factory with our all-trusting manager
            return sslContext.getSocketFactory();
        } catch (Exception e) {
            // failed to build trusting socket factory
            return null;
        }
    }

    public static URLConnection getConnection(URL url, Map inCookies, boolean input, boolean output, boolean cache, Proxy proxy, SSLSocketFactory socketFactory, HostnameVerifier verifier) throws IOException
    {
        //Proxy proxy = new Proxy(java.net.Proxy.Type.HTTP, new InetSocketAddress(proxyServer, port));
        URLConnection c = (proxy != null) ? url.openConnection(proxy) : url.openConnection();
        c.setRequestProperty("Accept-Encoding", "gzip, deflate");
        c.setAllowUserInteraction(false);
        c.setDoOutput(output);
        c.setDoInput(input);
        c.setUseCaches(cache);
        c.setReadTimeout(220000);
        c.setConnectTimeout(45000);

        if (c instanceof HttpURLConnection)
        {
            ((HttpURLConnection)c).setFollowRedirects(true);
        }

        if (c instanceof HttpsURLConnection)
        {
            try
            {
                HttpsURLConnection sc = (HttpsURLConnection) c;
                if (socketFactory != null) {
                    sc.setSSLSocketFactory(socketFactory);
                }
                if (verifier != null) {
                    sc.setHostnameVerifier(verifier);
                }
            }
            catch(Exception e)
            {
                LOG.warn("Could not access '" + url.toString() + "'", e);
            }
        }
        // Set cookies in the HTTP header
        if (inCookies != null)
        {   // [optional] place cookies (JSESSIONID) into HTTP headers
            setCookies(c, inCookies);
        }
        return c;
    }


    public static URLConnection getConnection(URL url, String proxyServer, int port, Map inCookies, boolean input, boolean output, boolean cache, boolean ignoreSec) throws IOException
    {
        Proxy proxy = (proxyServer == null) ?  Proxy.NO_PROXY : new Proxy(java.net.Proxy.Type.HTTP, new InetSocketAddress(proxyServer, port));
        SSLSocketFactory sslFactory = ignoreSec ? buildNaiveSSLSocketFactory() : null;
        HostnameVerifier verifier = ignoreSec ? NAIVE_VERIFIER : null;

        return getConnection(url, inCookies, input, output, cache, proxy, sslFactory, verifier);
    }

    public static String getHostName()
    {
        try
        {
            return InetAddress.getLocalHost().getHostName();
        }
        catch (UnknownHostException e)
        {
            LOG.warn("Unable to fetch 'hostname'", e);
            return "localhost";
        }
    }
}
