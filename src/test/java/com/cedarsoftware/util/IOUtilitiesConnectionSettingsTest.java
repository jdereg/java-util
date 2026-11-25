package com.cedarsoftware.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * IOUtilities reads and writes an HTTP connection with the settings its caller gave it, and adds its own only where
 * the caller gave none.
 * <p>
 * getInputStream() used to set them all: redirects on, the connect and read timeouts to io.connect.timeout and
 * io.read.timeout, Accept-Encoding to its own list. So a caller that had turned redirects off, usually to keep a
 * request on the host it had checked, was taken to wherever the server pointed; a caller's 60 second timeout became
 * 5; and a connection the caller had already connected failed with "Already connected". The uploads set no timeouts
 * at all. The server here is the JDK's, on the loopback address.
 */
@Timeout(value = 60, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
class IOUtilitiesConnectionSettingsTest {
    private HttpServer server;
    private String base;
    private volatile int uploaded = -1;

    @BeforeEach
    void startServer() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/redirect", exchange -> {
            exchange.getResponseHeaders().add("Location", "/secret");
            exchange.sendResponseHeaders(302, -1);
            exchange.close();
        });
        server.createContext("/secret", exchange -> reply(exchange, "SECRET-INTERNAL"));
        server.createContext("/plain", exchange -> reply(exchange, "plain body"));
        server.createContext("/encoding", exchange ->
                reply(exchange, String.valueOf(exchange.getRequestHeaders().getFirst("Accept-Encoding"))));
        server.createContext("/upload", exchange -> {
            uploaded = IOUtilities.inputStreamToBytes(exchange.getRequestBody()).length;
            reply(exchange, "ok");
        });
        server.start();
        base = "http://127.0.0.1:" + server.getAddress().getPort();
    }

    private static void reply(com.sun.net.httpserver.HttpExchange exchange, String body) throws java.io.IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(bytes);
        }
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    private HttpURLConnection open(String path) throws Exception {
        return (HttpURLConnection) new URL(base + path).openConnection();
    }

    private static String body(InputStream in) throws Exception {
        try (InputStream stream = in) {
            return new String(IOUtilities.inputStreamToBytes(stream), StandardCharsets.UTF_8);
        }
    }

    @Test
    void redirectsTheCallerTurnedOffStayOff() throws Exception {
        HttpURLConnection connection = open("/redirect");
        connection.setInstanceFollowRedirects(false);
        assertEquals("", body(IOUtilities.getInputStream(connection)));
        assertEquals(302, connection.getResponseCode());
        assertEquals(base + "/redirect", connection.getURL().toString());

        HttpURLConnection byDefault = open("/redirect");                    // followed, as before
        assertEquals("SECRET-INTERNAL", body(IOUtilities.getInputStream(byDefault)));
    }

    @Test
    void theCallersTimeoutsAreKeptAndTheConfiguredOnesFillIn() throws Exception {
        HttpURLConnection chosen = open("/plain");
        chosen.setConnectTimeout(60_000);
        chosen.setReadTimeout(120_000);
        assertEquals("plain body", body(IOUtilities.getInputStream(chosen)));
        assertEquals(60_000, chosen.getConnectTimeout());
        assertEquals(120_000, chosen.getReadTimeout());

        HttpURLConnection unset = open("/plain");
        assertEquals(0, unset.getConnectTimeout());
        assertEquals("plain body", body(IOUtilities.getInputStream(unset)));
        assertTrue(unset.getConnectTimeout() > 0, "io.connect.timeout fills in");
        assertTrue(unset.getReadTimeout() > 0, "io.read.timeout fills in");
    }

    @Test
    void theCallersAcceptEncodingIsKept() throws Exception {
        HttpURLConnection chosen = open("/encoding");
        chosen.setRequestProperty("Accept-Encoding", "identity");
        assertEquals("identity", body(IOUtilities.getInputStream(chosen)));
        assertEquals("gzip, x-gzip, deflate", body(IOUtilities.getInputStream(open("/encoding"))));
    }

    @Test
    void aConnectionTheCallerConnectedIsReadAsItIs() throws Exception {
        HttpURLConnection connection = open("/plain");
        assertEquals(200, connection.getResponseCode());                    // connects
        assertEquals("plain body", body(IOUtilities.getInputStream(connection)));
    }

    @Test
    void anUploadGetsTheConfiguredTimeoutsToo(@TempDir Path temp) throws Exception {
        File file = temp.resolve("upload.bin").toFile();
        Files.write(file.toPath(), new byte[5000]);
        HttpURLConnection fromFile = open("/upload");
        fromFile.setDoOutput(true);
        assertEquals(5000, IOUtilities.transfer(file, fromFile, null));
        assertTrue(fromFile.getConnectTimeout() > 0 && fromFile.getReadTimeout() > 0);
        assertEquals(200, fromFile.getResponseCode());
        assertEquals(5000, uploaded);

        HttpURLConnection fromBytes = open("/upload");
        fromBytes.setDoOutput(true);
        fromBytes.setReadTimeout(77_000);
        assertEquals(300, IOUtilities.transfer(fromBytes, new byte[300]));
        assertTrue(fromBytes.getConnectTimeout() > 0);
        assertEquals(77_000, fromBytes.getReadTimeout());                   // the caller's own
        assertEquals(200, fromBytes.getResponseCode());
        assertEquals(300, uploaded);
    }

    /** transfer(in, out, callback) flushes what it wrote, as transfer(in, out) does. */
    @Test
    void aTransferWithACallbackFlushes() {
        byte[] data = new byte[100];
        ByteArrayOutputStream sink = new ByteArrayOutputStream();
        IOUtilities.transfer(new ByteArrayInputStream(data), new BufferedOutputStream(sink, 8192));
        assertEquals(100, sink.size());

        ByteArrayOutputStream withCallback = new ByteArrayOutputStream();
        IOUtilities.TransferCallback callback = new IOUtilities.TransferCallback() {
            public void bytesTransferred(byte[] bytes, int count) { }
            public boolean isCancelled() { return false; }
        };
        assertEquals(100, IOUtilities.transfer(new ByteArrayInputStream(data),
                new BufferedOutputStream(withCallback, 8192), callback));
        assertEquals(100, withCallback.size());
    }
}
