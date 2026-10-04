package com.cedarsoftware.util;

import java.net.URI;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Cookie attributes are read the same way whatever the JVM's default locale. The responses are built in memory: no
 * connection is opened.
 */
class UrlUtilitiesCookieLocaleTest {

    @AfterEach
    void clearProperty() {
        System.clearProperty("urlutilities.strict.cookie.domain");
    }

    /**
     * Attribute names and domains were lowered with the default locale, and Turkish lowers I to a dotless i. Under
     * tr-TR, "DOMAIN" became "domaın", which is not "domain": the domain check was skipped, so a cookie for another
     * site kept its domain, and the attribute was stored under the mangled name. "WIKI.EXAMPLE.COM" became
     * "wıkı.example.com", which did not match the host wiki.example.com. All three are now lowered in Locale.ROOT.
     */
    @Test
    void attributesAreReadUnderATurkishDefaultLocale() throws Exception {
        Locale saved = Locale.getDefault();
        try {
            Locale.setDefault(new Locale("tr", "TR"));
            System.setProperty("urlutilities.strict.cookie.domain", "true");
            Map<String, Map<String, Map<String, String>>> store = new HashMap<>();
            UrlUtilities.getCookies(response("http://wiki.example.com/",
                    "a=1; DOMAIN=example.com; PATH=/",
                    "b=2; DOMAIN=evil.com",
                    "c=3; Domain=WIKI.EXAMPLE.COM"), store);
            Map<String, Map<String, String>> cookies = store.get("example.com");

            assertEquals("example.com", cookies.get("a").get("domain"));
            assertEquals("/", cookies.get("a").get("path"));
            for (Map<String, String> cookie : cookies.values()) {
                assertFalse(cookie.containsValue("evil.com"), "a cookie kept the domain evil.com: " + cookie);
            }
            assertEquals("WIKI.EXAMPLE.COM", cookies.get("c").get("domain"));
        } finally {
            Locale.setDefault(saved);
        }
    }

    /** A response that carries these Set-Cookie headers, from this URL. */
    static URLConnection response(String url, String... setCookies) throws Exception {
        return new URLConnection(URI.create(url).toURL()) {
            @Override
            public void connect() {
            }

            @Override
            public String getHeaderFieldKey(int n) {
                return n >= 1 && n <= setCookies.length ? "Set-Cookie" : null;
            }

            @Override
            public String getHeaderField(int n) {
                return n >= 1 && n <= setCookies.length ? setCookies[n - 1] : null;
            }
        };
    }
}
