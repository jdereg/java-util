package com.cedarsoftware.util;

import java.util.Date;

import javax.script.ScriptEngineManager;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Pins the defensive fixes made to ClassUtilities:
 * <ul>
 *   <li>Array-typed constructor parameters default to an EMPTY ARRAY of the right type
 *       (previously the assignable-interface scan matched Cloneable→ArrayList for every array
 *       type, an incompatible default that forced the constructor call to fail and fall back
 *       to null).</li>
 *   <li>The class-level security check blocks classes that merely EXTEND/IMPLEMENT a
 *       blocked-by-name type (e.g., a subclass of javax.script.ScriptEngineManager), not just
 *       exact name matches.</li>
 *   <li>{@code forName()} returns null (per its contract) instead of letting a LinkageError
 *       from broken/incompatible class bytes escape to the caller.</li>
 *   <li>The per-ClassLoader name cache stays correct across multiple loaders and across
 *       {@code clearCaches()} (guards the lock-free loader-cache memo).</li>
 * </ul>
 *
 * @author John DeRegnaucourt (jdereg@gmail.com)
 */
class ClassUtilitiesDefensiveFixesTest {

    // ------------------------------------------------------------------
    // Array-typed constructor parameter defaults
    // ------------------------------------------------------------------

    static class DateArrayHolder {
        final Date[] dates;
        DateArrayHolder(Date[] dates) {
            this.dates = dates;
        }
    }

    static class TwoDimHolder {
        final String[][] grid;
        TwoDimHolder(String[][] grid) {
            this.grid = grid;
        }
    }

    @Test
    void arrayParameterDefaultsToEmptyArrayNotNull() {
        // No arguments supplied: the fill phase must produce a type-compatible empty array,
        // so the (allowNulls=false) first pass succeeds directly.
        DateArrayHolder holder = (DateArrayHolder) ClassUtilities.newInstance(DateArrayHolder.class, null);
        assertNotNull(holder.dates, "Date[] parameter should default to an empty array, not null");
        assertEquals(0, holder.dates.length);
    }

    @Test
    void multiDimensionalArrayParameterDefaultsToEmptyArray() {
        TwoDimHolder holder = (TwoDimHolder) ClassUtilities.newInstance(TwoDimHolder.class, null);
        assertNotNull(holder.grid, "String[][] parameter should default to an empty array, not null");
        assertEquals(0, holder.grid.length);
    }

    // ------------------------------------------------------------------
    // Hierarchy-aware blocked-name security check
    // ------------------------------------------------------------------

    static class SubclassedScriptEngineManager extends ScriptEngineManager {
        SubclassedScriptEngineManager() {
            super(null);   // avoid provider discovery in the test JVM
        }
    }

    @Test
    void subclassOfBlockedByNameClassIsBlocked() {
        // javax.script.ScriptEngineManager is blocked BY NAME; a subclass must not slip through
        // the class-level check just because its own name differs.
        assertThrows(SecurityException.class,
                () -> ClassUtilities.SecurityChecker.verifyClass(SubclassedScriptEngineManager.class));
    }

    @Test
    void directlyBlockedByNameClassStillBlocked() {
        assertThrows(SecurityException.class,
                () -> ClassUtilities.SecurityChecker.verifyClass(ScriptEngineManager.class));
    }

    // ------------------------------------------------------------------
    // forName() contract: LinkageError -> null, not an escaping Error
    // ------------------------------------------------------------------

    static final class LinkageErrorLoader extends ClassLoader {
        LinkageErrorLoader() {
            super(null);
        }

        @Override
        protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            if (name.startsWith("broken.")) {
                throw new NoClassDefFoundError("simulated broken class: " + name);
            }
            return super.loadClass(name, resolve);
        }
    }

    @Test
    void forNameReturnsNullOnLinkageError() {
        assertNull(ClassUtilities.forName("broken.Widget", new LinkageErrorLoader()),
                "forName() is documented to return null when the class cannot be provided");
    }

    // ------------------------------------------------------------------
    // Per-loader cache isolation (guards the loader-cache memo fast path)
    // ------------------------------------------------------------------

    /** Resolves one virtual name to a fixed, already-loaded class; delegates everything else. */
    static final class VirtualLoader extends ClassLoader {
        private final Class<?> target;

        VirtualLoader(Class<?> target) {
            super(VirtualLoader.class.getClassLoader());
            this.target = target;
        }

        @Override
        protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            if ("virtual.Foo".equals(name)) {
                return target;
            }
            return super.loadClass(name, resolve);
        }
    }

    @Test
    void nameCacheIsIsolatedPerClassLoaderAndSurvivesClearCaches() {
        ClassLoader loaderA = new VirtualLoader(String.class);
        ClassLoader loaderB = new VirtualLoader(Integer.class);

        // Interleave lookups so the one-entry memo flips between loaders repeatedly:
        // a cross-contaminated memo would return the wrong class for one of them.
        for (int i = 0; i < 100; i++) {
            assertSame(String.class, ClassUtilities.forName("virtual.Foo", loaderA));
            assertSame(Integer.class, ClassUtilities.forName("virtual.Foo", loaderB));
        }

        // clearCaches() must also drop the memo — a stale memo would resurrect the old holder.
        ClassUtilities.clearCaches();
        assertSame(Integer.class, ClassUtilities.forName("virtual.Foo", loaderB));
        assertSame(String.class, ClassUtilities.forName("virtual.Foo", loaderA));
    }
}
