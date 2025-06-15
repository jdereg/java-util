package com.cedarsoftware.util;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

public class UrlInvocationHandlerTest {
    private static HttpServer server;
    private static String baseUrl;

    @BeforeAll
    static void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/echo", exchange -> writeResponse(exchange, 200, "ok"));
        server.start();
        baseUrl = "http://localhost:" + server.getAddress().getPort();
    }

    @AfterAll
    static void stopServer() {
        server.stop(0);
    }

    private static void writeResponse(HttpExchange exchange, int code, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(code, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }

    private interface EchoService {
        String call();
    }

    private static class DummyStrategy implements UrlInvocationHandlerStrategy {
        final URL url;
        boolean setCookies;
        boolean getCookies;
        boolean setHeaders;
        boolean postData;
        boolean readResp;
        int retries = 1;

        DummyStrategy(URL url) {
            this.url = url;
        }

        public URL buildURL(Object proxy, Method m, Object[] args) {
            return url;
        }

        public int getRetryAttempts() {
            return retries;
        }

        public long getRetrySleepTime() {
            return 1;
        }

        public void setCookies(URLConnection c) {
            setCookies = true;
        }

        public void getCookies(URLConnection c) {
            getCookies = true;
        }

        public void setRequestHeaders(URLConnection c) {
            setHeaders = true;
        }

        public byte[] generatePostData(Object proxy, Method m, Object[] args) {
            postData = true;
            return "data".getBytes(StandardCharsets.UTF_8);
        }

        public Object readResponse(URLConnection c) throws IOException {
            readResp = true;
            try (InputStream in = c.getInputStream()) {
                byte[] bytes = IOUtilities.inputStreamToBytes(in);
                return new String(bytes, StandardCharsets.UTF_8);
            }
        }
    }

    @Test
    void testInvokeSuccess() throws Throwable {
        DummyStrategy strategy = new DummyStrategy(new URL(baseUrl + "/echo"));
        UrlInvocationHandler handler = new UrlInvocationHandler(strategy);
        EchoService proxy = ProxyFactory.create(EchoService.class, handler);
        String result = proxy.call();
        assertEquals("ok", result);
        assertTrue(strategy.setCookies);
        assertTrue(strategy.getCookies);
        assertTrue(strategy.setHeaders);
        assertTrue(strategy.postData);
        assertTrue(strategy.readResp);
    }

    @Test
    void testCheckForThrowable() {
        assertDoesNotThrow(() -> UrlInvocationHandler.checkForThrowable("none"));
        Throwable cause = new RuntimeException("bad");
        InvocationTargetException ite = new InvocationTargetException(cause);
        RuntimeException thrown = assertThrows(RuntimeException.class,
                () -> UrlInvocationHandler.checkForThrowable(ite));
        assertSame(cause, thrown);
    }

    @Test
    void testInvokeReturnsNullWhenThrowable() throws Throwable {
        DummyStrategy strategy = new DummyStrategy(new URL(baseUrl + "/echo")) {
            public Object readResponse(URLConnection c) {
                return new IllegalStateException("boom");
            }
        };
        UrlInvocationHandler handler = new UrlInvocationHandler(strategy);
        EchoService proxy = ProxyFactory.create(EchoService.class, handler);
        assertNull(proxy.call());
    }
}
