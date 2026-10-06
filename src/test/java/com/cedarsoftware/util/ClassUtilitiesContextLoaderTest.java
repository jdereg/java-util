package com.cedarsoftware.util;

import java.net.URL;
import java.net.URLClassLoader;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * How ClassUtilities finds a loader and a class where the thread's context class loader is not the application's --
 * an OSGi framework, where it is usually the launcher's loader and sees no bundle. Under Felix json-io could not
 * resolve its own config classes, java-util's CompactMap templates or a TimeZone (each through
 * getClassLoader(anchor), which returned the context loader whatever it could see, and forName, which asked only
 * that loader). The context loader is still the first choice wherever it sees the anchor class: in a web container it
 * sees the application's classes too.
 *
 * @author John DeRegnaucourt (jdereg@gmail.com)
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
class ClassUtilitiesContextLoaderTest {

    private static final ClassLoader OWN = ClassUtilities.class.getClassLoader();

    /** Sees only the JDK's java.* (bootstrap) classes and nothing else, as an OSGi bundle that imports nothing. */
    static final class JavaOnlyLoader extends ClassLoader {
        JavaOnlyLoader() {
            super(null);
        }

        @Override
        protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            if (name.startsWith("java.")) {
                return super.loadClass(name, resolve);
            }
            throw new ClassNotFoundException(name);
        }
    }

    /** Also sees java-util's classes, as a bundle importing its packages does, without being a child of its loader. */
    static final class ImportingLoader extends ClassLoader {
        ImportingLoader() {
            super(null);
        }

        @Override
        protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            if (name.startsWith("com.cedarsoftware.util.")) {
                return OWN.loadClass(name);
            }
            return super.loadClass(name, resolve);
        }
    }

    /** Delegates to the application, but refuses one JDK class: its verdict stands. */
    static final class FilteringLoader extends ClassLoader {
        FilteringLoader() {
            super(OWN);
        }

        @Override
        protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            if (name.equals("java.util.zip.CRC32")) {
                throw new ClassNotFoundException(name);
            }
            return super.loadClass(name, resolve);
        }
    }

    private static ClassLoader withContext(ClassLoader context, Class<?> anchor) {
        Thread thread = Thread.currentThread();
        ClassLoader saved = thread.getContextClassLoader();
        try {
            thread.setContextClassLoader(context);
            return ClassUtilities.getClassLoader(anchor);
        } finally {
            thread.setContextClassLoader(saved);
        }
    }

    /** The launcher's loader under OSGi: it cannot see the anchor, so the anchor's own loader is the answer. */
    @Test
    void aContextLoaderThatCannotSeeTheAnchorIsPassedOver() {
        assertSame(OWN, withContext(new JavaOnlyLoader(), ClassUtilities.class));
        assertSame(OWN, withContext(new URLClassLoader(new URL[0], null), ClassUtilities.class));
    }

    @Test
    void aContextLoaderThatSeesTheAnchorIsStillTheAnswer() {
        ClassLoader child = new URLClassLoader(new URL[0], OWN);               // a web application's loader
        assertSame(child, withContext(child, ClassUtilities.class));
        ClassLoader importing = new ImportingLoader();                          // a bundle importing the package
        assertSame(importing, withContext(importing, ClassUtilities.class));
        assertSame(OWN, withContext(OWN, ClassUtilities.class));
        ClassLoader any = new JavaOnlyLoader();                                 // a JDK anchor: every loader sees it
        assertSame(any, withContext(any, String.class));
    }

    /** A context loader with its own copy of the anchor's class does not see the anchor: that copy is another class. */
    @Test
    void aContextLoaderWithAnotherCopyOfTheClassIsPassedOver() throws Exception {
        URL classes = ClassUtilities.class.getProtectionDomain().getCodeSource().getLocation();
        try (URLClassLoader copy = new URLClassLoader(new URL[] {classes}, null)) {
            assertNotSame(ClassUtilities.class, copy.loadClass(ClassUtilities.class.getName()));
            assertSame(OWN, withContext(copy, ClassUtilities.class));
        }
    }

    /** The answer for one context loader does not stick to the next one the thread gets. */
    @Test
    void theAnswerFollowsTheContextLoader() {
        ClassLoader blind = new JavaOnlyLoader();
        ClassLoader seeing = new URLClassLoader(new URL[0], OWN);
        for (int i = 0; i < 3; i++) {
            assertSame(OWN, withContext(blind, ClassUtilities.class));
            assertSame(seeing, withContext(seeing, ClassUtilities.class));
        }
    }

    /**
     * A loader that does not delegate to the JDK (an OSGi bundle's sees java.* and its imports) still finds the JDK's
     * own classes by name: sun.util.calendar.ZoneInfo, the class of every TimeZone, which json-io names in its config.
     */
    @Test
    void aLoaderThatDoesNotDelegateToTheJdkStillFindsJdkClasses() {
        ClassLoader bundle = new JavaOnlyLoader();
        Class<?> zoneInfo = java.util.TimeZone.getTimeZone("America/New_York").getClass();
        assertSame(zoneInfo, ClassUtilities.forName(zoneInfo.getName(), bundle));
        assertSame(java.util.TimeZone.class, ClassUtilities.forName("java.util.TimeZone", bundle));
        assertSame(int[][].class, ClassUtilities.forName("int[][]", bundle));
        assertSame(java.util.TimeZone[].class, ClassUtilities.forName("[Ljava.util.TimeZone;", bundle));
    }

    /** Only the JDK's classes are added: never the application's classpath, which such a loader does not see. */
    @Test
    void theApplicationClasspathIsNotAdded() {
        ClassLoader bundle = new JavaOnlyLoader();
        assertNull(ClassUtilities.forName("com.cedarsoftware.util.CaseInsensitiveMap", bundle));
        assertNull(ClassUtilities.forName(ClassUtilitiesContextLoaderTest.class.getName(), bundle));
    }

    /** A loader that delegates to the JDK keeps its own verdict: one that filters a JDK class still does. */
    @Test
    void aLoaderThatDelegatesToTheJdkKeepsItsVerdict() {
        assertNull(ClassUtilities.forName("java.util.zip.CRC32", new FilteringLoader()));
        assertNotNull(ClassUtilities.forName("java.util.zip.Adler32", new FilteringLoader()));
    }

    /** The common case is unchanged: the context loader is the application's and the anchor's own. */
    @Test
    void anOrdinaryThreadIsUnchanged() {
        assertSame(Thread.currentThread().getContextClassLoader(), ClassUtilities.getClassLoader(ClassUtilities.class));
    }
}
