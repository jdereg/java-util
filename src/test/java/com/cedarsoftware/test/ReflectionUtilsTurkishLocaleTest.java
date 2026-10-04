package com.cedarsoftware.test;

import java.util.Locale;

import com.cedarsoftware.util.ReflectionUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The sensitive-field check blocks the same fields whatever the JVM's default locale.
 */
class ReflectionUtilsTurkishLocaleTest {

    public static class Keys {
        public String API_KEY;
        public String PRIVATE_KEY;
        public String DB_CREDENTIAL;
        public String userPin;
    }

    @AfterEach
    void clearSecurityProperties() {
        System.clearProperty("reflectionutils.security.enabled");
        System.clearProperty("reflectionutils.sensitive.field.validation.enabled");
        System.clearProperty("reflectionutils.sensitive.field.patterns");
    }

    /**
     * Field names were lowered with the default locale before matching the patterns, and Turkish lowers I to a
     * dotless i: API_KEY became "apı_key", which "api_key" does not match, so under tr-TR the check let API_KEY,
     * PRIVATE_KEY and DB_CREDENTIAL through. A pattern set by property with an I ("PIN") was likewise lowered to
     * "pın" and matched nothing. Both are now lowered in Locale.ROOT.
     */
    @Test
    void sensitiveFieldsAreBlockedUnderATurkishDefaultLocale() {
        Locale saved = Locale.getDefault();
        try {
            Locale.setDefault(new Locale("tr", "TR"));
            System.setProperty("reflectionutils.security.enabled", "true");
            System.setProperty("reflectionutils.sensitive.field.validation.enabled", "true");
            for (String name : new String[] {"API_KEY", "PRIVATE_KEY", "DB_CREDENTIAL"}) {
                assertThatThrownBy(() -> ReflectionUtils.getField(Keys.class, name))
                        .as(name)
                        .isInstanceOf(SecurityException.class)
                        .hasMessageContaining("Sensitive field");
            }
            System.setProperty("reflectionutils.sensitive.field.patterns", "PIN,turkishLocaleTest");
            assertThatThrownBy(() -> ReflectionUtils.getField(Keys.class, "userPin"))
                    .isInstanceOf(SecurityException.class)
                    .hasMessageContaining("Sensitive field");
        } finally {
            Locale.setDefault(saved);
        }
    }
}
