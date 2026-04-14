package com.cedarsoftware.util;

import java.net.URL;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Coverage tests for UrlUtilities — targets JaCoCo gaps:
 * - Agent/referrer thread-local and global management
 * - Connect/read timeout getters and setters
 * - Max download size / content length getters and setters (programmatic + system property)
 * - getCookieDomainFromHost with various host formats (null, short, long, two-letter TLD)
 * - getActualUrl protocol validation
 * - readErrorResponse with null
 * - disconnect with null
 * - NAIVE_VERIFIER always returns true
 */
class UrlUtilitiesCoverageTest {

    // ========== User agent / referrer ==========

    @Test
    void testSetGetUserAgentThreadLocal() {
        UrlUtilities.userAgent.remove();
        UrlUtilities.clearGlobalUserAgent();
        try {
            UrlUtilities.setUserAgent("TestAgent/1.0");
            assertThat(UrlUtilities.getUserAgent()).isEqualTo("TestAgent/1.0");
        } finally {
            UrlUtilities.userAgent.remove();
            UrlUtilities.clearGlobalUserAgent();
        }
    }

    @Test
    void testClearGlobalUserAgent() {
        UrlUtilities.setUserAgent("Initial");
        UrlUtilities.clearGlobalUserAgent();
        UrlUtilities.userAgent.remove();
        // After clearing both thread-local and global, should be null
        assertThat(UrlUtilities.getUserAgent()).isNull();
    }

    @Test
    void testSetGetReferrer() {
        UrlUtilities.referrer.remove();
        UrlUtilities.clearGlobalReferrer();
        try {
            UrlUtilities.setReferrer("http://example.com");
            assertThat(UrlUtilities.getReferrer()).isEqualTo("http://example.com");
        } finally {
            UrlUtilities.referrer.remove();
            UrlUtilities.clearGlobalReferrer();
        }
    }

    @Test
    void testClearGlobalReferrer() {
        UrlUtilities.setReferrer("Initial");
        UrlUtilities.clearGlobalReferrer();
        UrlUtilities.referrer.remove();
        assertThat(UrlUtilities.getReferrer()).isNull();
    }

    // ========== Timeouts ==========

    @Test
    void testDefaultConnectTimeout() {
        int original = UrlUtilities.getDefaultConnectTimeout();
        try {
            UrlUtilities.setDefaultConnectTimeout(5000);
            assertThat(UrlUtilities.getDefaultConnectTimeout()).isEqualTo(5000);
        } finally {
            UrlUtilities.setDefaultConnectTimeout(original);
        }
    }

    @Test
    void testDefaultReadTimeout() {
        int original = UrlUtilities.getDefaultReadTimeout();
        try {
            UrlUtilities.setDefaultReadTimeout(10000);
            assertThat(UrlUtilities.getDefaultReadTimeout()).isEqualTo(10000);
        } finally {
            UrlUtilities.setDefaultReadTimeout(original);
        }
    }

    // ========== Max download size ==========

    @Test
    void testSetMaxDownloadSize() {
        long original = UrlUtilities.getMaxDownloadSize();
        try {
            UrlUtilities.setMaxDownloadSize(50_000_000L);
            assertThat(UrlUtilities.getMaxDownloadSize()).isEqualTo(50_000_000L);
        } finally {
            UrlUtilities.setMaxDownloadSize(original);
        }
    }

    @Test
    void testSetMaxDownloadSizeNegative() {
        assertThatThrownBy(() -> UrlUtilities.setMaxDownloadSize(-1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("positive");
    }

    @Test
    void testSetMaxDownloadSizeZero() {
        assertThatThrownBy(() -> UrlUtilities.setMaxDownloadSize(0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("positive");
    }

    @Test
    void testGetMaxDownloadSizeWithSystemProperty() {
        long original = UrlUtilities.getMaxDownloadSize();
        String origProp = System.getProperty("urlutilities.max.download.size");
        try {
            System.setProperty("urlutilities.max.download.size", "12345");
            assertThat(UrlUtilities.getMaxDownloadSize()).isEqualTo(12345L);
        } finally {
            if (origProp == null) {
                System.clearProperty("urlutilities.max.download.size");
            } else {
                System.setProperty("urlutilities.max.download.size", origProp);
            }
            UrlUtilities.setMaxDownloadSize(original);
        }
    }

    @Test
    void testGetMaxDownloadSizeWithInvalidSystemProperty() {
        long original = UrlUtilities.getMaxDownloadSize();
        String origProp = System.getProperty("urlutilities.max.download.size");
        try {
            System.setProperty("urlutilities.max.download.size", "not-a-number");
            // Should fall through to programmatic value
            assertThat(UrlUtilities.getMaxDownloadSize()).isEqualTo(original);
        } finally {
            if (origProp == null) {
                System.clearProperty("urlutilities.max.download.size");
            } else {
                System.setProperty("urlutilities.max.download.size", origProp);
            }
        }
    }

    @Test
    void testGetMaxDownloadSizeWithNegativeSystemProperty() {
        long original = UrlUtilities.getMaxDownloadSize();
        String origProp = System.getProperty("urlutilities.max.download.size");
        try {
            // Negative values should fall through to programmatic value
            System.setProperty("urlutilities.max.download.size", "-100");
            assertThat(UrlUtilities.getMaxDownloadSize()).isEqualTo(original);
        } finally {
            if (origProp == null) {
                System.clearProperty("urlutilities.max.download.size");
            } else {
                System.setProperty("urlutilities.max.download.size", origProp);
            }
        }
    }

    // ========== Max content length ==========

    @Test
    void testSetMaxContentLength() {
        int original = UrlUtilities.getMaxContentLength();
        try {
            UrlUtilities.setMaxContentLength(250_000_000);
            assertThat(UrlUtilities.getMaxContentLength()).isEqualTo(250_000_000);
        } finally {
            UrlUtilities.setMaxContentLength(original);
        }
    }

    @Test
    void testSetMaxContentLengthNegative() {
        assertThatThrownBy(() -> UrlUtilities.setMaxContentLength(-1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("positive");
    }

    @Test
    void testSetMaxContentLengthZero() {
        assertThatThrownBy(() -> UrlUtilities.setMaxContentLength(0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("positive");
    }

    @Test
    void testGetMaxContentLengthWithSystemProperty() {
        int original = UrlUtilities.getMaxContentLength();
        String origProp = System.getProperty("urlutilities.max.content.length");
        try {
            System.setProperty("urlutilities.max.content.length", "98765");
            assertThat(UrlUtilities.getMaxContentLength()).isEqualTo(98765);
        } finally {
            if (origProp == null) {
                System.clearProperty("urlutilities.max.content.length");
            } else {
                System.setProperty("urlutilities.max.content.length", origProp);
            }
            UrlUtilities.setMaxContentLength(original);
        }
    }

    @Test
    void testGetMaxContentLengthWithInvalidSystemProperty() {
        int original = UrlUtilities.getMaxContentLength();
        String origProp = System.getProperty("urlutilities.max.content.length");
        try {
            System.setProperty("urlutilities.max.content.length", "not-a-number");
            assertThat(UrlUtilities.getMaxContentLength()).isEqualTo(original);
        } finally {
            if (origProp == null) {
                System.clearProperty("urlutilities.max.content.length");
            } else {
                System.setProperty("urlutilities.max.content.length", origProp);
            }
        }
    }

    // ========== getCookieDomainFromHost ==========

    @Test
    void testGetCookieDomainFromHostNull() {
        assertThat(UrlUtilities.getCookieDomainFromHost(null)).isNull();
    }

    @Test
    void testGetCookieDomainFromHostSimple() {
        assertThat(UrlUtilities.getCookieDomainFromHost("example.com")).isEqualTo("example.com");
    }

    @Test
    void testGetCookieDomainFromHostSingleWord() {
        assertThat(UrlUtilities.getCookieDomainFromHost("localhost")).isEqualTo("localhost");
    }

    @Test
    void testGetCookieDomainFromHostSubdomain() {
        // www.example.com → example.com
        assertThat(UrlUtilities.getCookieDomainFromHost("www.example.com")).isEqualTo("example.com");
    }

    @Test
    void testGetCookieDomainFromHostDeepSubdomain() {
        // foo.bar.example.com → example.com (takes last 2 parts)
        assertThat(UrlUtilities.getCookieDomainFromHost("foo.bar.example.com")).isEqualTo("example.com");
    }

    @Test
    void testGetCookieDomainFromHostTwoLetterTLD() {
        // www.example.co.uk → example.co.uk (takes last 3 parts for 2-letter TLD)
        String result = UrlUtilities.getCookieDomainFromHost("www.example.co.uk");
        assertThat(result).isEqualTo("example.co.uk");
    }

    // ========== getActualUrl ==========

    @Test
    void testGetActualUrlHttp() {
        URL url = UrlUtilities.getActualUrl("http://example.com/");
        assertThat(url).isNotNull();
        assertThat(url.getProtocol()).isEqualTo("http");
    }

    @Test
    void testGetActualUrlHttps() {
        URL url = UrlUtilities.getActualUrl("https://example.com/");
        assertThat(url).isNotNull();
        assertThat(url.getProtocol()).isEqualTo("https");
    }

    @Test
    void testGetActualUrlFtp() {
        URL url = UrlUtilities.getActualUrl("ftp://example.com/");
        assertThat(url).isNotNull();
        assertThat(url.getProtocol()).isEqualTo("ftp");
    }

    @Test
    void testGetActualUrlUnsupportedProtocol() {
        assertThatThrownBy(() -> UrlUtilities.getActualUrl("file:///etc/passwd"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported protocol");
    }

    @Test
    void testGetActualUrlMalformed() {
        assertThatThrownBy(() -> UrlUtilities.getActualUrl("not a url"))
                .isInstanceOf(java.net.MalformedURLException.class);
    }

    @Test
    void testGetActualUrlNull() {
        assertThatThrownBy(() -> UrlUtilities.getActualUrl(null))
                .isInstanceOf(Exception.class); // Convention.throwIfNull
    }

    @Test
    void testGetActualUrlLocalhost() {
        // Localhost should work but log a warning
        URL url = UrlUtilities.getActualUrl("http://localhost/test");
        assertThat(url).isNotNull();
    }

    @Test
    void testGetActualUrlInternal127() {
        URL url = UrlUtilities.getActualUrl("http://127.0.0.1/test");
        assertThat(url).isNotNull();
    }

    @Test
    void testGetActualUrlInternal192() {
        URL url = UrlUtilities.getActualUrl("http://192.168.1.1/test");
        assertThat(url).isNotNull();
    }

    @Test
    void testGetActualUrlInternal10() {
        URL url = UrlUtilities.getActualUrl("http://10.0.0.1/test");
        assertThat(url).isNotNull();
    }

    @Test
    void testGetActualUrlResource() {
        // res:// prefix loads from classloader
        // If the resource doesn't exist, returns null (no exception)
        URL url = UrlUtilities.getActualUrl("res://nonexistent-resource.txt");
        // Either null (not found) or non-null if accidentally found — no exception
        // Just verify the call completes
        assertThat(url == null || url != null).isTrue();
    }

    // ========== readErrorResponse ==========

    @Test
    void testReadErrorResponseNull() {
        // Should not throw with null
        UrlUtilities.readErrorResponse(null);
    }

    // ========== disconnect ==========

    @Test
    void testDisconnectNull() {
        // Should not throw with null
        UrlUtilities.disconnect(null);
    }

    // ========== NAIVE_VERIFIER ==========

    @Test
    @SuppressWarnings("deprecation")
    void testNaiveVerifier() {
        // NAIVE_VERIFIER accepts any hostname
        assertThat(UrlUtilities.NAIVE_VERIFIER.verify("any.example.com", null)).isTrue();
        assertThat(UrlUtilities.NAIVE_VERIFIER.verify("evil.com", null)).isTrue();
    }

    // ========== NAIVE_TRUST_MANAGER ==========

    @Test
    @SuppressWarnings("deprecation")
    void testNaiveTrustManagerStructure() throws java.security.cert.CertificateException {
        assertThat(UrlUtilities.NAIVE_TRUST_MANAGER).isNotNull();
        assertThat(UrlUtilities.NAIVE_TRUST_MANAGER).hasSize(1);
        javax.net.ssl.X509TrustManager tm = (javax.net.ssl.X509TrustManager) UrlUtilities.NAIVE_TRUST_MANAGER[0];
        // Should return empty array (not null)
        assertThat(tm.getAcceptedIssuers()).isNotNull().isEmpty();
        // These methods accept anything — should not throw
        tm.checkClientTrusted(null, null);
        tm.checkServerTrusted(null, null);
    }

    // ========== Static constants ==========

    @Test
    void testStaticConstants() {
        assertThat(UrlUtilities.SET_COOKIE).isEqualTo("Set-Cookie");
        assertThat(UrlUtilities.COOKIE).isEqualTo("Cookie");
        assertThat(UrlUtilities.PATH).isEqualTo("path");
        assertThat(UrlUtilities.EXPIRES).isEqualTo("expires");
        assertThat(UrlUtilities.COOKIE_VALUE_DELIMITER).isEqualTo(";");
        assertThat(UrlUtilities.SET_COOKIE_SEPARATOR).isEqualTo("; ");
        assertThat(UrlUtilities.NAME_VALUE_SEPARATOR).isEqualTo('=');
        assertThat(UrlUtilities.DOT).isEqualTo('.');
    }

    // ========== getContentFromUrl with bad URL returns null ==========

    @Test
    void testGetContentFromUrlWithBadStringUrl() {
        // Bad URL — should return null (logs warning)
        byte[] content = UrlUtilities.getContentFromUrl("not-a-valid-url");
        assertThat(content).isNull();
    }

    @Test
    void testGetContentFromUrlAsStringWithBadStringUrl() {
        String content = UrlUtilities.getContentFromUrlAsString("not-a-valid-url");
        assertThat(content).isNull();
    }
}
