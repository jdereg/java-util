package com.cedarsoftware.test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import com.cedarsoftware.util.ReflectionUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * The dangerous-class check must still see the real caller when it runs while ReflectionUtils works out what it
 * keeps on a class. That work runs inside a ClassValue, whose computeValue() frame belongs to a class nested in
 * ReflectionUtils (ReflectionUtils$N) -- a com.cedarsoftware.util.* frame that, taken for a java-util caller, would
 * trust every caller and switch the check off. Lives outside com.cedarsoftware.util so the caller is untrusted; each
 * test uses classes no other test asks about, so the check runs on first use here.
 */
class ReflectionUtilsPerClassCacheSecurityTest {

    static class GuardedParent {
        int parentField;
    }

    static class OpenChild extends GuardedParent {
        int childField;
    }

    static class GuardedAlone {
        int field;
    }

    @AfterEach
    void clearSecurityProperties() {
        System.clearProperty("reflectionutils.security.enabled");
        System.clearProperty("reflectionutils.dangerous.class.validation.enabled");
        System.clearProperty("reflectionutils.dangerous.class.patterns");
    }

    private static void guard(Class<?> c) {
        System.setProperty("reflectionutils.security.enabled", "true");
        System.setProperty("reflectionutils.dangerous.class.validation.enabled", "true");
        System.setProperty("reflectionutils.dangerous.class.patterns", c.getName());
    }

    @Test
    void aDangerousSuperclassIsLeftOutOfTheFieldsWorkedOutForAnUntrustedCaller() {
        guard(GuardedParent.class);
        List<String> names = new ArrayList<>();
        for (Field f : ReflectionUtils.getAllDeclaredFields(OpenChild.class)) {
            names.add(f.getName());
        }
        assertEquals(1, names.size(), "fields: " + names);
        assertEquals("childField", names.get(0));
    }

    @Test
    void aDangerousClassesFieldsAreRefusedWhenWorkedOutForAnUntrustedCaller() {
        guard(GuardedAlone.class);
        assertThrows(SecurityException.class, () -> ReflectionUtils.getDeclaredFields(GuardedAlone.class));
    }
}
