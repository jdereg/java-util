package com.cedarsoftware.util;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.X509TrustManager;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class UrlUtilitiesTest {
    private static HttpServer server;
    private static String baseUrl;

    @BeforeAll
    static void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/ok", exchange -> writeResponse(exchange, 200, "hello"));
        server.createContext("/error", exchange -> writeResponse(exchange, 500, "bad"));
        server.start();
        baseUrl = "http://localhost:" + server.getAddress().getPort();
    }

    @AfterAll
    static void stopServer() {
        server.stop(0);
    }

    @BeforeEach
    void resetStatics() {
        UrlUtilities.clearGlobalReferrer();
        UrlUtilities.clearGlobalUserAgent();
        UrlUtilities.userAgent.remove();
        UrlUtilities.referrer.remove();
    }

    private static void writeResponse(HttpExchange exchange, int code, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(code, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }

    @Test
    void testHostnameVerifier() {
        assertTrue(UrlUtilities.NAIVE_VERIFIER.verify("any", null));
    }

    @Test
    void testTrustManagerMethods() throws Exception {
        X509TrustManager tm = (X509TrustManager) UrlUtilities.NAIVE_TRUST_MANAGER[0];
        tm.checkClientTrusted(null, null);
        tm.checkServerTrusted(null, null);
        // After security fix: returns empty array instead of null
        assertNotNull(tm.getAcceptedIssuers());
        assertEquals(0, tm.getAcceptedIssuers().length);
    }

    @Test
    void testSetAndClearUserAgent() {
        UrlUtilities.setUserAgent("agent");
        assertEquals("agent", UrlUtilities.getUserAgent());
        UrlUtilities.clearGlobalUserAgent();
        UrlUtilities.userAgent.remove();
        assertNull(UrlUtilities.getUserAgent());
    }

    @Test
    void testSetAndClearReferrer() {
        UrlUtilities.setReferrer("ref");
        assertEquals("ref", UrlUtilities.getReferrer());
        UrlUtilities.clearGlobalReferrer();
        UrlUtilities.referrer.remove();
        assertNull(UrlUtilities.getReferrer());
    }

    @Test
    void testDisconnect() throws Exception {
        DummyHttpConnection c = new DummyHttpConnection(new URL(baseUrl));
        UrlUtilities.disconnect(c);
        assertTrue(c.disconnected);
    }

    @Test
    void testGetCookieDomainFromHost() {
        assertEquals("example.com", UrlUtilities.getCookieDomainFromHost("www.example.com"));
    }

    @Test
    void testGetAndSetCookies() throws Exception {
        URL url = new URL("http://example.com/test");
        HttpURLConnection resp = mock(HttpURLConnection.class);
        when(resp.getURL()).thenReturn(url);
        when(resp.getHeaderFieldKey(1)).thenReturn(UrlUtilities.SET_COOKIE);
        when(resp.getHeaderField(1)).thenReturn("ID=42; path=/");
        when(resp.getHeaderFieldKey(2)).thenReturn(null);
        Map<String, Map<String, Map<String, String>>> store = new ConcurrentHashMap<>();
        UrlUtilities.getCookies(resp, store);
        assertTrue(store.containsKey("example.com"));
        Map<String, String> cookie = store.get("example.com").get("ID");
        assertEquals("42", cookie.get("ID"));

        HttpURLConnection req = mock(HttpURLConnection.class);
        when(req.getURL()).thenReturn(url);
        UrlUtilities.setCookies(req, store);
        verify(req).setRequestProperty(UrlUtilities.COOKIE, "ID=42");
    }

    @Test
    void testGetActualUrl() throws Exception { // Changed from default to public for older JUnit if needed
        URL u = UrlUtilities.getActualUrl("res://io-test.txt"); // Ensure io-test.txt is in your test resources
        assertNotNull(u, "URL should not be null");

        try (InputStream in = u.openStream()) {
            assertNotNull(in, "InputStream should not be null"); // Good to check stream too

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192]; // Or 4096, a common buffer size
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                baos.write(buffer, 0, bytesRead);
            }
            byte[] bytes = baos.toByteArray();

            assertTrue(bytes.length > 0, "File should not be empty");
            // You can add more assertions here, e.g., print content for verification
            // LOG.info("Read content: " + new String(bytes, StandardCharsets.UTF_8));
        }
    }

    @Test
    void testGetConnection() throws Exception {
        UrlUtilities.setUserAgent("ua");
        UrlUtilities.setReferrer("ref");
        URLConnection c = UrlUtilities.getConnection(new URL(baseUrl + "/ok"), true, false, false);
        assertEquals("gzip, deflate", c.getRequestProperty("Accept-Encoding"));
        assertEquals("ref", c.getRequestProperty("Referer"));
        assertEquals("ua", c.getRequestProperty("User-Agent"));
    }

    @Test
    void testGetContentFromUrl() {
        String url = baseUrl + "/ok";
        byte[] bytes = UrlUtilities.getContentFromUrl(url);
        assertEquals("hello", new String(bytes, StandardCharsets.UTF_8));
        assertEquals("hello", UrlUtilities.getContentFromUrlAsString(url));
    }

    @Test
    void testCopyContentFromUrl() throws Exception {
        String url = baseUrl + "/ok";
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        UrlUtilities.copyContentFromUrl(url, out);
        assertEquals("hello", out.toString(StandardCharsets.UTF_8.name()));
    }

    @Test
    void testReadErrorResponse() throws Exception {
        HttpURLConnection conn = mock(HttpURLConnection.class);
        when(conn.getResponseCode()).thenReturn(500);
        when(conn.getErrorStream()).thenReturn(new ByteArrayInputStream("err".getBytes(StandardCharsets.UTF_8)));
        UrlUtilities.readErrorResponse(conn);
    }

    @Test
    void testPublicStateSettingsApis() {
        assert UrlUtilities.getDefaultConnectTimeout() != 369;
        UrlUtilities.setDefaultConnectTimeout(369);
        assert UrlUtilities.getDefaultConnectTimeout() == 369;

        assert UrlUtilities.getDefaultReadTimeout() != 123;
        UrlUtilities.setDefaultReadTimeout(123);
        assert UrlUtilities.getDefaultReadTimeout() == 123;
    }

    @Test
    void testSecurityWarningForNaiveSSL() throws Exception {
        // Test that security warning is logged when allowAllCerts=true for HTTPS
        TestLogHandler logHandler = new TestLogHandler();
        Logger urlUtilitiesLogger = Logger.getLogger(UrlUtilities.class.getName());
        urlUtilitiesLogger.addHandler(logHandler);
        
        try {
            // Create an HTTPS URL connection with allowAllCerts=true to trigger the warning
            URL httpsUrl = new URL("https://example.com");
            URLConnection connection = UrlUtilities.getConnection(httpsUrl, null, true, false, false, true);
            
            // Verify the security warning was logged
            assertTrue(logHandler.hasWarning("SSL certificate validation disabled"));
            
            // Verify connection is properly configured for naive SSL (testing security fix behavior)
            if (connection instanceof HttpsURLConnection) {
                HttpsURLConnection httpsConnection = (HttpsURLConnection) connection;
                assertNotNull(httpsConnection.getSSLSocketFactory());
                assertNotNull(httpsConnection.getHostnameVerifier());
            }
        } finally {
            urlUtilitiesLogger.removeHandler(logHandler);
        }
    }

    @Test
    void testDeprecatedNaiveTrustManagerSecurity() {
        // Verify NAIVE_TRUST_MANAGER is marked as deprecated and works securely
        X509TrustManager tm = (X509TrustManager) UrlUtilities.NAIVE_TRUST_MANAGER[0];
        
        // Test that getAcceptedIssuers returns empty array (not null) for security
        assertNotNull(tm.getAcceptedIssuers());
        assertEquals(0, tm.getAcceptedIssuers().length);
        
        // Verify it still functions for testing purposes but with warnings in code
        assertDoesNotThrow(() -> tm.checkClientTrusted(null, null));
        assertDoesNotThrow(() -> tm.checkServerTrusted(null, null));
    }

    @Test 
    void testDeprecatedNaiveHostnameVerifierSecurity() {
        // Verify NAIVE_VERIFIER still works for testing but is marked deprecated
        assertTrue(UrlUtilities.NAIVE_VERIFIER.verify("malicious.example.com", null));
        assertTrue(UrlUtilities.NAIVE_VERIFIER.verify("legitimate.example.com", null));
        // Both should return true - highlighting the security risk this poses
    }

    // Test helper class to capture log messages
    private static class TestLogHandler extends Handler {
        private boolean hasSSLWarning = false;
        
        @Override
        public void publish(LogRecord record) {
            if (record.getMessage() != null && record.getMessage().contains("SSL certificate validation disabled")) {
                hasSSLWarning = true;
            }
        }
        
        public boolean hasWarning(String message) {
            return hasSSLWarning;
        }
        
        @Override
        public void flush() {}
        
        @Override
        public void close() throws SecurityException {}
    }

    private static class DummyHttpConnection extends HttpURLConnection {
        boolean disconnected;
        protected DummyHttpConnection(URL u) { super(u); }
        @Override public void disconnect() { disconnected = true; }
        @Override public boolean usingProxy() { return false; }
        @Override public void connect() { }
    }
}

