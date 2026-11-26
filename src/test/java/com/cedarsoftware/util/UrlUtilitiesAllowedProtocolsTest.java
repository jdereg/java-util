package com.cedarsoftware.util;

import java.io.ByteArrayOutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * {@code urlutilities.allowed.protocols} is applied: it names the protocols a URL given as text may have, in place
 * of http, https and ftp.
 *
 * <p>From 3.6.0 through 4.118.0 the property was read by a private method that nothing called, while the Javadoc
 * and the user guide presented it as protection: with it set to {@code https}, an http URL was fetched. Its
 * neighbour {@code urlutilities.allow.internal.hosts} was the same, and is retired rather than made to work: a check
 * on how a host is written is not a check on where the request goes.
 *
 * @author Claude Fable 5.2
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
@SuppressWarnings("deprecation")
class UrlUtilitiesAllowedProtocolsTest {
    private static final String LIST = "urlutilities.allowed.protocols";
    private static final String RETIRED = "urlutilities.allow.internal.hosts";

    private String listBefore;
    private String retiredBefore;

    @BeforeEach
    void withNothingSet() {
        listBefore = System.getProperty(LIST);
        retiredBefore = System.getProperty(RETIRED);
        System.clearProperty(LIST);
        System.clearProperty(RETIRED);
    }

    @AfterEach
    void restore() {
        set(LIST, listBefore);
        set(RETIRED, retiredBefore);
    }

    /** Unchanged: with nothing set, a URL given as text is http, https or ftp. */
    @Test
    void withNothingSetTheThreeProtocolsAreAllowedAsBefore() {
        allowed("http://example.com/a");
        allowed("https://example.com/a");
        allowed("ftp://example.com/a");
        allowed("HTTPS://example.com/a");
        refused("file:///tmp/a", "file");
        refused("jar:file:/tmp/a.jar!/b", "jar");
    }

    @Test
    void theListReplacesTheThree() {
        System.setProperty(LIST, "https");
        allowed("https://example.com/a");
        refused("http://example.com/a", "http");
        refused("ftp://example.com/a", "ftp");
        refused("file:///tmp/a", "file");
    }

    @Test
    void theListIsReadWithSpacesAndInAnyCase() {
        System.setProperty(LIST, " HTTPS , Ftp ");
        allowed("https://example.com/a");
        allowed("ftp://example.com/a");
        refused("http://example.com/a", "http");
    }

    /** The list is the list: a protocol beyond the three is allowed when the application names it. */
    @Test
    void aProtocolTheListNamesIsAllowed() {
        System.setProperty(LIST, "https,file");
        allowed("file:///tmp/a");
        refused("http://example.com/a", "http");
    }

    @Test
    void aListThatNamesNothingLeavesTheThree() {
        for (String empty : new String[] {"", "  ", " , ,"}) {
            System.setProperty(LIST, empty);
            allowed("http://example.com/a");
            allowed("ftp://example.com/a");
            refused("file:///tmp/a", "file");
        }
    }

    /** The property is read on each call. */
    @Test
    void aChangeAppliesToTheNextCall() {
        allowed("http://example.com/a");
        System.setProperty(LIST, "https");
        refused("http://example.com/a", "http");
        System.clearProperty(LIST);
        allowed("http://example.com/a");
    }

    /** res:// names a classpath resource, which the list does not judge. */
    @Test
    void aClasspathResourceIsNotJudgedByTheList() {
        System.setProperty(LIST, "https");
        assertNotNull(UrlUtilities.getActualUrl("res://io-test.txt"));
    }

    /** Every method that takes the URL as text refuses it, and nothing is sent. */
    @Test
    void aRefusedUrlIsNotRequested() throws Exception {
        AtomicInteger requests = new AtomicInteger();
        HttpServer server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        server.createContext("/", exchange -> {
            requests.incrementAndGet();
            byte[] body = "served".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();
        try {
            String url = "http://127.0.0.1:" + server.getAddress().getPort() + "/x";
            assertEquals("served", UrlUtilities.getContentFromUrlAsString(url), "with nothing set, http is read");
            assertEquals(1, requests.get());

            System.setProperty(LIST, "https");
            assertNull(UrlUtilities.getContentFromUrlAsString(url));   // this one logs the refusal and returns null
            assertNull(UrlUtilities.getContentFromUrl(url));
            assertThrows(IllegalArgumentException.class,
                    () -> UrlUtilities.copyContentFromUrl(url, new ByteArrayOutputStream()));
            assertThrows(IllegalArgumentException.class, () -> UrlUtilities.getConnection(url, true, false, false));
            assertEquals(1, requests.get(), "no request left the JVM for a refused URL");

            // A URL object the caller built is opened as it is: the list judges text
            assertEquals("served", UrlUtilities.getContentFromUrlAsString(new URL(url), false));
            assertEquals(2, requests.get());
        } finally {
            server.stop(0);
        }
    }

    /** Never applied, and no longer read: an internal host is opened whatever this property says. */
    @Test
    void theRetiredInternalHostsPropertyChangesNothing() {
        System.setProperty(RETIRED, "false");
        allowed("http://localhost:8080/a");
        allowed("http://127.0.0.1/a");
        allowed("http://192.168.1.1/a");
    }

    private static void allowed(String url) {
        URL actual = UrlUtilities.getActualUrl(url);
        assertNotNull(actual, url);
        assertEquals(url.substring(0, url.indexOf(':')).toLowerCase(Locale.ROOT), actual.getProtocol(), url);
    }

    private static void refused(String url, String protocol) {
        IllegalArgumentException refusal = assertThrows(IllegalArgumentException.class,
                () -> UrlUtilities.getActualUrl(url), url);
        assertEquals("Unsupported protocol: " + protocol, refusal.getMessage());
    }

    private static void set(String name, String value) {
        if (value == null) {
            System.clearProperty(name);
        } else {
            System.setProperty(name, value);
        }
    }
}
