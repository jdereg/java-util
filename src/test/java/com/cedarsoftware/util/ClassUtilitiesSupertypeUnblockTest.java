package com.cedarsoftware.util;

import java.io.FileOutputStream;
import java.io.FileWriter;
import java.util.Collections;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * allowClass(supertype) exempts a class only from the rules that match through that supertype, as its Javadoc says
 * (and as the classutilities.security.unblocked property already did). The first supertype unblocked at runtime
 * returned "not blocked", lifting every built-in rule: allowClass("java.io.Flushable") unblocked FileOutputStream and
 * FileWriter, and allowClass("java.lang.Object") unblocked Runtime and ProcessBuilder -- newInstance(ProcessBuilder)
 * then succeeded. An unblock of the class itself still overrides every rule.
 */
class ClassUtilitiesSupertypeUnblockTest {

    /** Blocked only because it extends ClassLoader. */
    public static class MyLoader extends ClassLoader {
    }

    public interface Marker {
    }

    /** Extends ClassLoader and implements Marker. */
    public static class LoaderAndMarker extends ClassLoader implements Marker {
    }

    @AfterEach
    void clearRules() {
        ClassUtilities.SecurityChecker.clearSecurityOverrides();
    }

    private static boolean blocked(Class<?> c) {
        return ClassUtilities.SecurityChecker.isSecurityBlocked(c);
    }

    @Test
    void unblockingASupertypeLeavesAClassesOwnBlockInPlace() {
        ClassUtilities.SecurityChecker.allowClass("java.io.Flushable");
        assertTrue(blocked(FileOutputStream.class));
        assertTrue(blocked(FileWriter.class));

        ClassUtilities.SecurityChecker.allowClass("java.lang.Object");
        assertTrue(blocked(Runtime.class));
        assertTrue(blocked(ProcessBuilder.class));
        assertThrows(SecurityException.class,
                () -> ClassUtilities.newInstance(ProcessBuilder.class, Collections.emptyList()));
    }

    @Test
    void unblockingTheClassItselfStillOverridesEveryRule() {
        ClassUtilities.SecurityChecker.allowClass("java.io.FileOutputStream");
        assertFalse(blocked(FileOutputStream.class));
    }

    @Test
    void unblockingASupertypeExemptsFromTheRulesThatMatchThroughIt() {
        assertTrue(blocked(MyLoader.class), "blocked through ClassLoader");
        ClassUtilities.SecurityChecker.allowClass("java.lang.ClassLoader");
        assertFalse(blocked(MyLoader.class));
    }

    @Test
    void aRuntimeBlockThroughAnotherSupertypeStillApplies() {
        ClassUtilities.SecurityChecker.allowClass("java.lang.ClassLoader");
        assertFalse(blocked(LoaderAndMarker.class));
        ClassUtilities.SecurityChecker.addBlockedClass(Marker.class.getName());
        assertTrue(blocked(LoaderAndMarker.class), "blocked through Marker, whatever the supertype order");
    }
}
