package com.cedarsoftware.util;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A cookie the strict domain check rejects is not stored. The responses are built in memory: no connection is opened.
 */
class UrlUtilitiesCookieRejectTest {

    @AfterEach
    void clearProperty() {
        System.clearProperty("urlutilities.strict.cookie.domain");
    }

    /**
     * getCookies put the cookie in the store as soon as it read its name and value, then checked its attributes. When
     * the domain check rejected it ("Rejecting dangerous cookie"), the cookie was already stored and was sent back on
     * the next request. RFC 6265 (5.3) has a user agent ignore such a cookie entirely; now it is stored only once its
     * attributes pass.
     */
    @Test
    void aCookieWithARejectedDomainIsNotStored() throws Exception {
        System.setProperty("urlutilities.strict.cookie.domain", "true");
        Map<String, Map<String, Map<String, String>>> store = new HashMap<>();
        UrlUtilities.getCookies(UrlUtilitiesCookieLocaleTest.response("http://www.example.com/",
                "good=1; domain=example.com; path=/",
                "hijack=2; domain=evil.com; path=/"), store);
        Map<String, Map<String, String>> cookies = store.get("example.com");
        assertTrue(cookies.containsKey("good"));
        assertEquals("1", cookies.get("good").get("good"));
        assertFalse(cookies.containsKey("hijack"), "stored: " + cookies.get("hijack"));
    }

    /** With the strict check off (the default), the domain is not checked and the cookie is stored, as before. */
    @Test
    void withoutTheStrictCheckTheCookieIsStored() throws Exception {
        Map<String, Map<String, Map<String, String>>> store = new HashMap<>();
        UrlUtilities.getCookies(UrlUtilitiesCookieLocaleTest.response("http://www.example.com/",
                "other=2; domain=evil.com"), store);
        assertEquals("evil.com", store.get("example.com").get("other").get("domain"));
    }
}
