package com.cedarsoftware.util;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.cedarsoftware.util.LoggingConfig;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.concurrent.atomic.AtomicReference;
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
 *  Anyone using the deprecated api calls for proxying to urls should update to use the new suggested calls.
 *  To let the jvm proxy for you automatically, use the following -D parameters:
 *
 *  http.proxyHost
 *  http.proxyPort (default: 80)
 *  http.nonProxyHosts (should always include localhost)
 *  https.proxyHost
 *  https.proxyPort
 *
 *  Example:  -Dhttp.proxyHost=proxy.example.org -Dhttp.proxyPort=8080 -Dhttps.proxyHost=proxy.example.org -Dhttps.proxyPort=8080 -Dhttp.nonProxyHosts=*.foo.com|localhost|*.td.afg
 *
 * @author Ken Partlow
 * @author John DeRegnaucourt (jdereg@gmail.com)
 *         <br>
 *         Copyright (c) Cedar Software LLC
 *         <br><br>
 *         Licensed under the Apache License, Version 2.0 (the "License");
 *         you may not use this file except in compliance with the License.
 *         You may obtain a copy of the License at
 *         <br><br>
 *         <a href="http://www.apache.org/licenses/LICENSE-2.0">License</a>
 *         <br><br>
 *         Unless required by applicable law or agreed to in writing, software
 *         distributed under the License is distributed on an "AS IS" BASIS,
 *         WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *         See the License for the specific language governing permissions and
 *         limitations under the License.
 */
public final class UrlUtilities {
    private static final AtomicReference<String> globalUserAgent = new AtomicReference<>();
    private static final AtomicReference<String> globalReferrer = new AtomicReference<>();
    public static final ThreadLocal<String> userAgent = new ThreadLocal<>();
    public static final ThreadLocal<String> referrer = new ThreadLocal<>();
    public static final String SET_COOKIE = "Set-Cookie";
    public static final String SET_COOKIE_SEPARATOR = "; ";
    public static final String COOKIE = "Cookie";
    public static final String COOKIE_VALUE_DELIMITER = ";";
    public static final String PATH = "path";
    public static final String EXPIRES = "expires";
    public static final SafeSimpleDateFormat DATE_FORMAT = new SafeSimpleDateFormat("EEE, dd-MMM-yyyy hh:mm:ss z");
    public static final char NAME_VALUE_SEPARATOR = '=';
    public static final char DOT = '.';

    private static volatile int defaultReadTimeout = 220000;
    private static volatile int defaultConnectTimeout = 45000;
    
    // Security: Resource consumption limits for download operations
    private static volatile long maxDownloadSize = 100 * 1024 * 1024; // 100MB default limit
    private static volatile int maxContentLength = 500 * 1024 * 1024; // 500MB Content-Length header limit

    private static final Pattern resPattern = Pattern.compile("^res://", Pattern.CASE_INSENSITIVE);

    /**
     * ⚠️ SECURITY WARNING ⚠️
     * This TrustManager accepts ALL SSL certificates without verification, including self-signed,
     * expired, or certificates from unauthorized Certificate Authorities. This completely disables
     * SSL/TLS certificate validation and makes connections vulnerable to man-in-the-middle attacks.
     * 
     * <b>DO NOT USE IN PRODUCTION</b> - Only suitable for development/testing against known safe endpoints.
     * 
     * For production use, consider:
     * 1. Use proper CA-signed certificates
     * 2. Import self-signed certificates into a custom TrustStore
     * 3. Use certificate pinning for additional security
     * 4. Implement custom TrustManager with proper validation logic
     * 
     * @deprecated This creates a serious security vulnerability. Use proper certificate validation.
     */
    @Deprecated
    public static final TrustManager[] NAIVE_TRUST_MANAGER = new TrustManager[]
            {
                    new X509TrustManager() {
                        public void checkClientTrusted(X509Certificate[] x509Certificates, String s) {
                            // WARNING: No validation performed - accepts any client certificate
                        }

                        public void checkServerTrusted(X509Certificate[] x509Certificates, String s)  {
                            // WARNING: No validation performed - accepts any server certificate
                        }

                        public X509Certificate[] getAcceptedIssuers() {
                            return new X509Certificate[0]; // Return empty array instead of null
                        }
                    }
            };

    /**
     * ⚠️ SECURITY WARNING ⚠️
     * This HostnameVerifier accepts ALL hostnames without verification, completely disabling
     * hostname verification for SSL/TLS connections. This makes connections vulnerable to
     * man-in-the-middle attacks where an attacker presents a valid certificate for a different domain.
     * 
     * <b>DO NOT USE IN PRODUCTION</b> - Only suitable for development/testing against known safe endpoints.
     * 
     * @deprecated This creates a serious security vulnerability. Use proper hostname verification.
     */
    @Deprecated
    public static final HostnameVerifier NAIVE_VERIFIER = (hostname, sslSession) -> {
        // WARNING: No hostname verification performed - accepts any hostname
        return true;
    };

    protected static SSLSocketFactory naiveSSLSocketFactory;
    private static final Logger LOG = Logger.getLogger(UrlUtilities.class.getName());

    static {
        LoggingConfig.init();
    }

    static {
        try {
            // Default new HTTP connections to follow redirects
            HttpURLConnection.setFollowRedirects(true);
        } catch (Exception ignored) {
        }

        try {
            //  could be other algorithms (prob need to calculate this another way.
            final SSLContext sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, NAIVE_TRUST_MANAGER, new SecureRandom());
            naiveSSLSocketFactory = sslContext.getSocketFactory();
        } catch (Exception e) {
            LOG.log(Level.WARNING, e.getMessage(), e);
        }
    }

    private UrlUtilities() {
        super();
    }

    public static void clearGlobalUserAgent() {
        globalUserAgent.set(null);
    }

    public static void clearGlobalReferrer() {
        globalReferrer.set(null);
    }

    public static void setReferrer(String referer) {
        if (StringUtilities.isEmpty(globalReferrer.get())) {
            globalReferrer.set(referer);
        }
        referrer.set(referer);
    }

    public static String getReferrer() {
        String localReferrer = referrer.get();
        if (StringUtilities.hasContent(localReferrer)) {
            return localReferrer;
        }
        return globalReferrer.get();
    }

    public static void setUserAgent(String agent) {
        if (StringUtilities.isEmpty(globalUserAgent.get())) {
            globalUserAgent.set(agent);
        }
        userAgent.set(agent);
    }

    public static String getUserAgent() {
        String localAgent = userAgent.get();
        if (StringUtilities.hasContent(localAgent)) {
            return localAgent;
        }
        return globalUserAgent.get();
    }

    public static void setDefaultConnectTimeout(int millis) {
        defaultConnectTimeout = millis;
    }

    public static void setDefaultReadTimeout(int millis) {
        defaultReadTimeout = millis;
    }

    public static int getDefaultConnectTimeout() {
        return defaultConnectTimeout;
    }

    public static int getDefaultReadTimeout() {
        return defaultReadTimeout;
    }
    
    /**
     * Set the maximum download size limit for URL content fetching operations.
     * This prevents memory exhaustion attacks from maliciously large downloads.
     * 
     * @param maxSizeBytes Maximum download size in bytes (default: 100MB)
     */
    public static void setMaxDownloadSize(long maxSizeBytes) {
        if (maxSizeBytes <= 0) {
            throw new IllegalArgumentException("Max download size must be positive: " + maxSizeBytes);
        }
        maxDownloadSize = maxSizeBytes;
    }
    
    /**
     * Get the current maximum download size limit.
     * 
     * @return Maximum download size in bytes
     */
    public static long getMaxDownloadSize() {
        return maxDownloadSize;
    }
    
    /**
     * Set the maximum Content-Length header value that will be accepted.
     * This prevents acceptance of responses claiming to be larger than reasonable limits.
     * 
     * @param maxLengthBytes Maximum Content-Length in bytes (default: 500MB)
     */
    public static void setMaxContentLength(int maxLengthBytes) {
        if (maxLengthBytes <= 0) {
            throw new IllegalArgumentException("Max content length must be positive: " + maxLengthBytes);
        }
        maxContentLength = maxLengthBytes;
    }
    
    /**
     * Get the current maximum Content-Length header limit.
     * 
     * @return Maximum Content-Length in bytes
     */
    public static int getMaxContentLength() {
        return maxContentLength;
    }

    public static void readErrorResponse(URLConnection c) {
        if (c == null) {
            return;
        }
        InputStream in = null;
        try {
            ((HttpURLConnection) c).getResponseCode();
            in = ((HttpURLConnection) c).getErrorStream();
            if (in == null) {
                return;
            }
            // read the response body
            ByteArrayOutputStream out = new ByteArrayOutputStream(1024);
            int count;
            byte[] bytes = new byte[8192];
            while ((count = in.read(bytes)) != -1) {
                out.write(bytes, 0, count);
            }
        } catch (Exception e) {
            LOG.log(Level.WARNING, e.getMessage(), e);
        } finally {
            IOUtilities.close(in);
        }
    }
    
    /**
     * Transfer data from input stream to output stream with size limits to prevent resource exhaustion.
     * 
     * @param input Source input stream
     * @param output Destination output stream  
     * @param maxBytes Maximum bytes to transfer before throwing SecurityException
     * @throws SecurityException if transfer exceeds maxBytes limit
     * @throws IOException if an I/O error occurs
     */
    private static void transferWithLimit(InputStream input, java.io.OutputStream output, long maxBytes) throws IOException {
        byte[] buffer = new byte[8192];
        long totalBytes = 0;
        int bytesRead;
        
        while ((bytesRead = input.read(buffer)) != -1) {
            totalBytes += bytesRead;
            
            // Security: Enforce download size limit to prevent memory exhaustion
            if (totalBytes > maxBytes) {
                throw new SecurityException("Download size exceeds maximum allowed: " + totalBytes + " > " + maxBytes);
            }
            
            output.write(buffer, 0, bytesRead);
        }
    }
    
    /**
     * Validate Content-Length header to prevent acceptance of unreasonably large responses.
     * 
     * @param connection The URL connection to check
     * @throws SecurityException if Content-Length exceeds the configured limit
     */
    private static void validateContentLength(URLConnection connection) {
        int contentLength = connection.getContentLength();
        
        // Content-Length of -1 means unknown length, which is acceptable
        if (contentLength == -1) {
            return;
        }
        
        // Check for unreasonably large declared content length
        if (contentLength > maxContentLength) {
            throw new SecurityException("Content-Length exceeds maximum allowed: " + contentLength + " > " + maxContentLength);
        }
        
        // Check for invalid content length values (should not be less than -1)
        if (contentLength < -1) {
            throw new SecurityException("Invalid Content-Length value: " + contentLength);
        }
    }
    
    /**
     * Validate cookie name to prevent injection attacks and enforce security constraints.
     * 
     * @param cookieName The cookie name to validate
     * @throws SecurityException if cookie name contains dangerous characters or is too long
     */
    private static void validateCookieName(String cookieName) {
        if (cookieName == null || cookieName.trim().isEmpty()) {
            throw new SecurityException("Cookie name cannot be null or empty");
        }
        
        // Security: Limit cookie name length to prevent memory exhaustion
        if (cookieName.length() > 256) {
            throw new SecurityException("Cookie name too long (max 256): " + cookieName.length());
        }
        
        // Security: Check for dangerous characters that could indicate injection attempts
        if (cookieName.contains("\n") || cookieName.contains("\r") || cookieName.contains("\0") || 
            cookieName.contains(";") || cookieName.contains("=") || cookieName.contains(" ")) {
            throw new SecurityException("Cookie name contains dangerous characters: " + cookieName);
        }
        
        // Security: Block suspicious cookie names that could be used for attacks
        String lowerName = cookieName.toLowerCase();
        if (lowerName.startsWith("__secure-") || lowerName.startsWith("__host-")) {
            // These are browser-reserved prefixes that applications shouldn't create
            LOG.warning("Cookie name uses reserved prefix: " + cookieName);
        }
    }
    
    /**
     * Validate cookie value to prevent injection attacks and enforce security constraints.
     * 
     * @param cookieValue The cookie value to validate  
     * @throws SecurityException if cookie value contains dangerous characters or is too long
     */
    private static void validateCookieValue(String cookieValue) {
        if (cookieValue == null) {
            return; // Null values are acceptable for cookies
        }
        
        // Security: Limit cookie value length to prevent memory exhaustion
        if (cookieValue.length() > 4096) {
            throw new SecurityException("Cookie value too long (max 4096): " + cookieValue.length());
        }
        
        // Security: Check for dangerous characters that could indicate injection attempts
        if (cookieValue.contains("\n") || cookieValue.contains("\r") || cookieValue.contains("\0")) {
            throw new SecurityException("Cookie value contains dangerous control characters");
        }
    }
    
    /**
     * Validate cookie domain to prevent domain-related security issues.
     * 
     * @param cookieDomain The cookie domain to validate
     * @param requestHost The host from the original request
     * @throws SecurityException if domain is invalid or potentially malicious
     */
    private static void validateCookieDomain(String cookieDomain, String requestHost) {
        if (cookieDomain == null || requestHost == null) {
            return; // No domain validation needed
        }
        
        // Security: Prevent domain hijacking by ensuring cookie domain matches request host
        String normalizedDomain = cookieDomain.toLowerCase().trim();
        String normalizedHost = requestHost.toLowerCase().trim();
        
        // Remove leading dot from domain if present  
        if (normalizedDomain.startsWith(".")) {
            normalizedDomain = normalizedDomain.substring(1);
        }
        
        // Security: Ensure cookie domain is a suffix of the request host
        if (!normalizedHost.equals(normalizedDomain) && !normalizedHost.endsWith("." + normalizedDomain)) {
            throw new SecurityException("Cookie domain mismatch - potential domain hijacking: " + 
                                      cookieDomain + " vs " + requestHost);
        }
        
        // Security: Block suspicious TLDs and prevent cookies from being set on public suffixes
        if (normalizedDomain.equals("com") || normalizedDomain.equals("org") || 
            normalizedDomain.equals("net") || normalizedDomain.equals("edu") ||
            normalizedDomain.equals("localhost") || normalizedDomain.equals("local")) {
            throw new SecurityException("Cookie domain cannot be set on public suffix: " + cookieDomain);
        }
    }

    public static void disconnect(HttpURLConnection c) {
        if (c != null) {
            try {
                c.disconnect();
            } catch (Exception ignored) {
            }
        }
    }

    /**
     * Retrieves and stores cookies returned by the host on the other side of
     * the open java.net.URLConnection.
     * <p>
     * The connection MUST have been opened using the connect() method or a
     * IOException will be thrown.
     *
     * @param conn a java.net.URLConnection - must be open, or IOException will
     *             be thrown
     */
    public static void getCookies(URLConnection conn, Map<String, Map<String, Map<String, String>>> store) {
        // let's determine the domain from where these cookies are being sent
        String domain = getCookieDomainFromHost(conn.getURL().getHost());
        String requestHost = conn.getURL().getHost();
        Map<String, Map<String, String>> domainStore; // this is where we will store cookies for this domain

        // now let's check the store to see if we have an entry for this domain
        if (store.containsKey(domain)) {
            // we do, so lets retrieve it from the store
            domainStore = store.get(domain);
        } else {
            // we don't, so let's create it and put it in the store
            domainStore = new ConcurrentHashMap<>();
            store.put(domain, domainStore);
        }

        if (domainStore.containsKey("JSESSIONID")) {
            // No need to continually get the JSESSIONID (and set-cookies header) as this does not change throughout the session.
            return;
        }

        // OK, now we are ready to get the cookies out of the URLConnection
        String headerName;
        for (int i = 1; (headerName = conn.getHeaderFieldKey(i)) != null; i++) {
            if (headerName.equalsIgnoreCase(SET_COOKIE)) {
                try {
                    Map<String, String> cookie = new ConcurrentHashMap<>();
                    StringTokenizer st = new StringTokenizer(conn.getHeaderField(i), COOKIE_VALUE_DELIMITER);

                    // the specification dictates that the first name/value pair
                    // in the string is the cookie name and value, so let's handle
                    // them as a special case:

                    if (st.hasMoreTokens()) {
                        String token = st.nextToken().trim();
                        int sepIndex = token.indexOf(NAME_VALUE_SEPARATOR);
                        if (sepIndex == -1) {
                            continue; // Skip invalid cookie format
                        }
                        
                        String key = token.substring(0, sepIndex).trim();
                        String value = token.substring(sepIndex + 1);
                        
                        // Security: Validate cookie name and value
                        validateCookieName(key);
                        validateCookieValue(value);
                        
                        domainStore.put(key, cookie);
                        cookie.put(key, value);
                    }

                    while (st.hasMoreTokens()) {
                        String token = st.nextToken().trim();
                        int pos = token.indexOf(NAME_VALUE_SEPARATOR);
                        if (pos != -1) {
                            String key = token.substring(0, pos).toLowerCase().trim();
                            String value = token.substring(pos + 1).trim();
                            
                            // Security: Validate cookie attributes
                            if ("domain".equals(key)) {
                                validateCookieDomain(value, requestHost);
                            }
                            
                            // Security: Validate attribute value length
                            if (value.length() > 4096) {
                                LOG.warning("Cookie attribute value too long, truncating: " + key);
                                continue;
                            }
                            
                            cookie.put(key, value);
                        }
                    }
                } catch (SecurityException e) {
                    // Security: Log and skip dangerous cookies rather than failing completely
                    LOG.log(Level.WARNING, "Rejecting dangerous cookie from " + requestHost + ": " + e.getMessage());
                } catch (Exception e) {
                    // General parsing errors - log and continue
                    LOG.log(Level.WARNING, "Error parsing cookie from " + requestHost + ": " + e.getMessage());
                }
            }
        }
    }

    /**
     * Prior to opening a URLConnection, calling this method will set all
     * unexpired cookies that match the path or subpaths for thi underlying URL
     * <p>
     * The connection MUST NOT have been opened
     * method or an IOException will be thrown.
     *
     * @param conn a java.net.URLConnection - must NOT be open, or IOException will be thrown
     * @throws IOException if the connection has already been opened (thrown as unchecked)
     */
    public static void setCookies(URLConnection conn, Map<String, Map<String, Map<String, String>>> store) {
        // let's determine the domain and path to retrieve the appropriate cookies
        URL url = conn.getURL();
        String domain = getCookieDomainFromHost(url.getHost());
        String path = url.getPath();

        Map<String, Map<String, String>> domainStore = store.get(domain);
        if (domainStore == null) {
            return;
        }
        StringBuilder cookieStringBuffer = new StringBuilder();
        Iterator<String> cookieNames = domainStore.keySet().iterator();

        while (cookieNames.hasNext()) {
            String cookieName = cookieNames.next();
            Map<String, String> cookie = domainStore.get(cookieName);
            // check cookie to ensure path matches and cookie is not expired
            // if all is cool, add cookie to header string
            if (comparePaths((String) cookie.get(PATH), path) && isNotExpired((String) cookie.get(EXPIRES))) {
                try {
                    // Security: Validate cookie before sending
                    validateCookieName(cookieName);
                    String cookieValue = (String) cookie.get(cookieName);
                    validateCookieValue(cookieValue);
                    
                    // Security: Limit total cookie header size to prevent header injection
                    if (cookieStringBuffer.length() + cookieName.length() + cookieValue.length() + 10 > 8192) {
                        LOG.warning("Cookie header size limit reached, stopping cookie addition");
                        break;
                    }
                    
                    cookieStringBuffer.append(cookieName);
                    cookieStringBuffer.append('=');
                    cookieStringBuffer.append(cookieValue);
                    if (cookieNames.hasNext()) {
                        cookieStringBuffer.append(SET_COOKIE_SEPARATOR);
                    }
                } catch (SecurityException e) {
                    // Security: Skip dangerous cookies rather than failing
                    LOG.log(Level.WARNING, "Skipping dangerous cookie in request: " + e.getMessage());
                }
            }
        }
        try {
            conn.setRequestProperty(COOKIE, cookieStringBuffer.toString());
        } catch (IllegalStateException e) {
            ExceptionUtilities.uncheckedThrow(new IOException(
                    "Illegal State! Cookies cannot be set on a URLConnection that is already connected. " +
                            "Only call setCookies(java.net.URLConnection) AFTER calling java.net.URLConnection.connect()."));
        }
    }

    public static String getCookieDomainFromHost(String host) {
        if (host == null) {
            return null;
        }
        String[] parts = host.split("\\.");
        if (parts.length <= 2) {
            return host;
        }
        String tld = parts[parts.length - 1];
        if (tld.length() == 2 && parts.length >= 3) {
            return parts[parts.length - 3] + '.' + parts[parts.length - 2] + '.' + tld;
        }
        return parts[parts.length - 2] + '.' + tld;
    }

    private static boolean isNotExpired(String cookieExpires) {
        if (cookieExpires == null) {
            return true;
        }

        try {
            return new Date().compareTo(DATE_FORMAT.parse(cookieExpires)) <= 0;
        } catch (ParseException e) {
            LOG.log(Level.WARNING, e.getMessage(), e);
            return false;
        }
    }

    private static boolean comparePaths(String cookiePath, String targetPath) {
        return cookiePath == null || "/".equals(cookiePath) || targetPath.regionMatches(0, cookiePath, 0, cookiePath.length());
    }

    /**
     * Get content from the passed in URL.  This code will open a connection to
     * the passed in server, fetch the requested content, and return it as a
     * String.
     *
     * @param url URL to hit
     * @return UTF-8 String read from URL or null in the case of error.
     */
    public static String getContentFromUrlAsString(String url) {
        return getContentFromUrlAsString(url, null, null, false);
    }

    /**
     * Get content from the passed in URL.  This code will open a connection to
     * the passed in server, fetch the requested content, and return it as a
     * String.
     *
     * @param url           URL to hit
     * @param allowAllCerts true to not verify certificates
     * @return UTF-8 String read from URL or null in the case of error.
     */
    public static String getContentFromUrlAsString(URL url, boolean allowAllCerts) {
        return getContentFromUrlAsString(url, null, null, allowAllCerts);
    }

    /**
     * Get content from the passed in URL.  This code will open a connection to
     * the passed in server, fetch the requested content, and return it as a
     * String.
     *
     * @param url           URL to hit
     * @param inCookies     Map of session cookies (or null if not needed)
     * @param outCookies    Map of session cookies (or null if not needed)
     * @param trustAllCerts if true, SSL connection will always be trusted.
     * @return String of content fetched from URL.
     */
    public static String getContentFromUrlAsString(String url, Map inCookies, Map outCookies, boolean trustAllCerts) {
        byte[] bytes = getContentFromUrl(url, inCookies, outCookies, trustAllCerts);
        return bytes == null ? null : StringUtilities.createString(bytes, "UTF-8");
    }

    /**
     * Get content from the passed in URL.  This code will open a connection to
     * the passed in server, fetch the requested content, and return it as a
     * String.
     *
     * @param url           URL to hit
     * @param inCookies     Map of session cookies (or null if not needed)
     * @param outCookies    Map of session cookies (or null if not needed)
     * @param trustAllCerts if true, SSL connection will always be trusted.
     * @return String of content fetched from URL.
     */
    public static String getContentFromUrlAsString(URL url, Map inCookies, Map outCookies, boolean trustAllCerts) {
        byte[] bytes = getContentFromUrl(url, inCookies, outCookies, trustAllCerts);
        return bytes == null ? null : StringUtilities.createString(bytes, "UTF-8");
    }


    /**
     * Get content from the passed in URL.  This code will open a connection to
     * the passed in server, fetch the requested content, and return it as a
     * byte[].
     *
     * @param url URL to hit
     * @return byte[] read from URL or null in the case of error.
     */
    public static byte[] getContentFromUrl(String url) {
        return getContentFromUrl(url, null, null, false);
    }

    /**
     * Get content from the passed in URL.  This code will open a connection to
     * the passed in server, fetch the requested content, and return it as a
     * byte[].
     *
     * @param url URL to hit
     * @return byte[] read from URL or null in the case of error.
     */
    public static byte[] getContentFromUrl(URL url, boolean allowAllCerts) {
        return getContentFromUrl(url, null, null, allowAllCerts);
    }

    /*
     * Get content from the passed in URL.  This code will open a connection to
     * the passed in server, fetch the requested content, and return it as a
     * byte[].
     *
     * @param url URL to hit
     * @param inCookies Map of session cookies (or null if not needed)
     * @param outCookies Map of session cookies (or null if not needed)
     * @param ignoreSec if true, SSL connection will always be trusted.
     * @return byte[] of content fetched from URL.
     */
    public static byte[] getContentFromUrl(String url, Map inCookies, Map outCookies, boolean allowAllCerts) {
        try {
            return getContentFromUrl(getActualUrl(url), inCookies, outCookies, allowAllCerts);
        } catch (Exception e) {
            LOG.log(Level.WARNING, e.getMessage(), e);
            return null;
        }
    }

    /**
     * Get content from the passed in URL.  This code will open a connection to
     * the passed in server, fetch the requested content, and return it as a
     * byte[].
     *
     * @param url           URL to hit
     * @param inCookies     Map of session cookies (or null if not needed)
     * @param outCookies    Map of session cookies (or null if not needed)
     * @param allowAllCerts override certificate validation?
     * @return byte[] of content fetched from URL.
     */
    public static byte[] getContentFromUrl(URL url, Map inCookies, Map outCookies, boolean allowAllCerts) {
        URLConnection c = null;
        try {
            c = getConnection(url, inCookies, true, false, false, allowAllCerts);

            FastByteArrayOutputStream out = new FastByteArrayOutputStream(65536);
            InputStream stream = IOUtilities.getInputStream(c);
            
            // Security: Validate Content-Length header after connection is established
            validateContentLength(c);
            
            // Security: Use size-limited transfer to prevent memory exhaustion
            transferWithLimit(stream, out, maxDownloadSize);
            stream.close();

            if (outCookies != null) {   // [optional] Fetch cookies from server and update outCookie Map (pick up JSESSIONID, other headers)
                getCookies(c, outCookies);
            }

            return out.toByteArray();
        } catch (SSLHandshakeException e) {   // Don't read error response.  it will just cause another exception.
            LOG.log(Level.WARNING, e.getMessage(), e);
            return null;
        } catch (SecurityException e) {
            // Security exceptions should be logged and re-thrown to alert callers
            LOG.log(Level.SEVERE, "Security violation in URL download: " + e.getMessage(), e);
            return null; // Return null for backward compatibility, but log the security issue
        } catch (Exception e) {
            readErrorResponse(c);
            LOG.log(Level.WARNING, e.getMessage(), e);
            return null;
        } finally {
            if (c instanceof HttpURLConnection) {
                disconnect((HttpURLConnection) c);
            }
        }
    }

    /**
     * Convenience method to copy content from a String URL to an output stream.
     */
    public static void copyContentFromUrl(String url, java.io.OutputStream out) {
        copyContentFromUrl(getActualUrl(url), out, null, null, false);
    }

    /**
     * Copy content from a URL to an output stream.
     */
    public static void copyContentFromUrl(URL url, java.io.OutputStream out, Map<String, Map<String, Map<String, String>>> inCookies, Map<String, Map<String, Map<String, String>>> outCookies, boolean allowAllCerts) {
        URLConnection c = null;
        try {
            c = getConnection(url, inCookies, true, false, false, allowAllCerts);
            
            InputStream stream = IOUtilities.getInputStream(c);
            
            // Security: Validate Content-Length header after connection is established
            validateContentLength(c);
            
            // Security: Use size-limited transfer to prevent memory exhaustion
            transferWithLimit(stream, out, maxDownloadSize);
            stream.close();
            
            if (outCookies != null) {
                getCookies(c, outCookies);
            }
        } catch (IOException e) {
            ExceptionUtilities.uncheckedThrow(e);
        } finally {
            if (c instanceof HttpURLConnection) {
                disconnect((HttpURLConnection) c);
            }
        }
    }

    /**
     * Get content from the passed in URL.  This code will open a connection to
     * the passed in server, fetch the requested content, and return it as a
     * byte[].
     *
     * @param url        URL to hit
     * @param inCookies  Map of session cookies (or null if not needed)
     * @param outCookies Map of session cookies (or null if not needed)
     * @return byte[] of content fetched from URL.
     */
    public static byte[] getContentFromUrl(String url, Map inCookies, Map outCookies) {
        return getContentFromUrl(url, inCookies, outCookies, false);
    }

    /**
     * @param input  boolean indicating whether this connection will be used for input
     * @param output boolean indicating whether this connection will be used for output
     * @param cache  boolean allow caching (be careful setting this to true for non-static retrievals).
     * @return URLConnection established URL connection.
     * @throws IOException if an I/O error occurs (thrown as unchecked)
     * @throws MalformedURLException if the URL is invalid (thrown as unchecked)
     */
    public static URLConnection getConnection(String url, boolean input, boolean output, boolean cache) {
        return getConnection(getActualUrl(url), null, input, output, cache, false);
    }

    /**
     * @param input  boolean indicating whether this connection will be used for input
     * @param output boolean indicating whether this connection will be used for output
     * @param cache  boolean allow caching (be careful setting this to true for non-static retrievals).
     * @return URLConnection established URL connection.
     */
    public static URLConnection getConnection(URL url, boolean input, boolean output, boolean cache) {
        return getConnection(url, null, input, output, cache, false);
    }

    /**
     * Gets a connection from a url.  All getConnection calls should go through this code.
     *
     * @param inCookies Supply cookie Map (received from prior setCookies calls from server)
     * @param input     boolean indicating whether this connection will be used for input
     * @param output    boolean indicating whether this connection will be used for output
     * @param cache     boolean allow caching (be careful setting this to true for non-static retrievals).
     * @return URLConnection established URL connection.
     * @throws IOException if an I/O error occurs (thrown as unchecked)
     */
    public static URLConnection getConnection(URL url, Map inCookies, boolean input, boolean output, boolean cache, boolean allowAllCerts) {
        URLConnection c = null;
        try {
            c = url.openConnection();
        } catch (IOException e) {
            ExceptionUtilities.uncheckedThrow(e);
        }
        c.setRequestProperty("Accept-Encoding", "gzip, deflate");
        c.setAllowUserInteraction(false);
        c.setDoOutput(output);
        c.setDoInput(input);
        c.setUseCaches(cache);
        c.setReadTimeout(defaultReadTimeout);
        c.setConnectTimeout(defaultConnectTimeout);

        String ref = getReferrer();
        if (StringUtilities.hasContent(ref)) {
            c.setRequestProperty("Referer", ref);
        }
        String agent = getUserAgent();
        if (StringUtilities.hasContent(agent)) {
            c.setRequestProperty("User-Agent", agent);
        }

        if (c instanceof HttpURLConnection) {   // setFollowRedirects is a static (global) method / setting - resetting it in case other code changed it?
            HttpURLConnection.setFollowRedirects(true);
        }

        if (c instanceof HttpsURLConnection && allowAllCerts) {
            // WARNING: This disables SSL certificate validation - use only for development/testing
            LOG.warning("SSL certificate validation disabled - this is a security risk in production environments");
            try {
                setNaiveSSLSocketFactory((HttpsURLConnection) c);
            } catch (Exception e) {
                LOG.log(Level.WARNING, e.getMessage(), e);
            }
        }

        // Set cookies in the HTTP header
        if (inCookies != null) {   // [optional] place cookies (JSESSIONID) into HTTP headers
            setCookies(c, inCookies);
        }
        return c;
    }

    /**
     * ⚠️ SECURITY WARNING ⚠️
     * This method disables SSL certificate and hostname verification.
     * Only use for development/testing with trusted endpoints.
     * 
     * @param sc the HttpsURLConnection to configure
     * @deprecated Use proper SSL certificate validation in production
     */
    @Deprecated
    private static void setNaiveSSLSocketFactory(HttpsURLConnection sc) {
        sc.setSSLSocketFactory(naiveSSLSocketFactory);
        sc.setHostnameVerifier(NAIVE_VERIFIER);
    }

    public static URL getActualUrl(String url) {
        Convention.throwIfNull(url, "URL cannot be null");
        
        Matcher m = resPattern.matcher(url);
        if (m.find()) {
            return ClassUtilities.getClassLoader().getResource(url.substring(m.end()));
        } else {
            try {
                URL parsedUrl = new URL(url);
                // Basic SSRF protection - validate protocol and host
                String protocol = parsedUrl.getProtocol();
                if (protocol == null || (!protocol.equals("http") && !protocol.equals("https") && !protocol.equals("ftp"))) {
                    throw new IllegalArgumentException("Unsupported protocol: " + protocol);
                }
                
                String host = parsedUrl.getHost();
                if (host != null && (host.equals("localhost") || host.equals("127.0.0.1") || host.startsWith("192.168.") || host.startsWith("10.") || host.startsWith("172."))) {
                    // Allow but log potential internal access
                    LOG.warning("Accessing internal/local host: " + host);
                }
                
                return parsedUrl;
            } catch (MalformedURLException e) {
                ExceptionUtilities.uncheckedThrow(e);
                return null; // never reached
            }
        }
    }
}
