package com.cedarsoftware.util;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * forName() honors security rules added at runtime for names it has already cached or that an alias names.
 * addBlockedClass() is documented to take effect "immediately, including for classes already checked", but a cached
 * name or an alias was returned without a check, so a class kept resolving after it was blocked. The reverse held too:
 * a class that only allowClass() admitted stayed resolvable after clearSecurityOverrides(). (newInstance() checks
 * again, so the exposure was limited to callers of forName().)
 */
class ClassUtilitiesRuntimeRulesCachedTest {

    public static class CachedThenBlocked {
    }

    public static class AliasedThenBlocked {
    }

    public static class CachedThenPackageBlocked {
    }

    private static final ClassLoader LOADER = ClassUtilities.getClassLoader(ClassUtilitiesRuntimeRulesCachedTest.class);

    @AfterEach
    void clearRules() {
        ClassUtilities.SecurityChecker.clearSecurityOverrides();
    }

    @Test
    void aClassCachedBeforeABlockIsRefusedAfterIt() {
        String name = CachedThenBlocked.class.getName();
        assertSame(CachedThenBlocked.class, ClassUtilities.forName(name, LOADER));
        assertSame(CachedThenBlocked.class, ClassUtilities.forName(name, LOADER));     // cached

        ClassUtilities.SecurityChecker.addBlockedClass(name);
        assertTrue(ClassUtilities.SecurityChecker.isSecurityBlocked(CachedThenBlocked.class));
        assertThrows(SecurityException.class, () -> ClassUtilities.forName(name, LOADER));

        ClassUtilities.SecurityChecker.clearSecurityOverrides();
        assertSame(CachedThenBlocked.class, ClassUtilities.forName(name, LOADER), "resolvable again once unblocked");
    }

    @Test
    void anAliasRegisteredBeforeABlockIsRefusedAfterIt() {
        ClassUtilities.addPermanentClassAlias(AliasedThenBlocked.class, "aliasedThenBlocked");
        try {
            assertSame(AliasedThenBlocked.class, ClassUtilities.forName("aliasedThenBlocked", LOADER));
            ClassUtilities.SecurityChecker.addBlockedClass(AliasedThenBlocked.class.getName());
            assertThrows(SecurityException.class, () -> ClassUtilities.forName("aliasedThenBlocked", LOADER));
        } finally {
            ClassUtilities.removePermanentClassAlias("aliasedThenBlocked");
        }
    }

    @Test
    void aClassCachedBeforeAPackageBlockIsRefusedAfterIt() {
        String name = CachedThenPackageBlocked.class.getName();
        assertSame(CachedThenPackageBlocked.class, ClassUtilities.forName(name, LOADER));
        ClassUtilities.SecurityChecker.addBlockedPackage(name);                      // a raw prefix: the class itself
        assertThrows(SecurityException.class, () -> ClassUtilities.forName(name, LOADER));
    }

    @Test
    void aClassOnlyAnUnblockAdmittedIsRefusedOnceTheUnblockIsCleared() {
        String name = "java.lang.ProcessBuilder";                                   // blocked by the built-in list
        assertThrows(SecurityException.class, () -> ClassUtilities.forName(name, LOADER));

        ClassUtilities.SecurityChecker.allowClass(name);
        assertSame(ProcessBuilder.class, ClassUtilities.forName(name, LOADER));
        assertSame(ProcessBuilder.class, ClassUtilities.forName(name, LOADER));     // cached

        ClassUtilities.SecurityChecker.clearSecurityOverrides();
        assertThrows(SecurityException.class, () -> ClassUtilities.forName(name, LOADER));
    }
}
