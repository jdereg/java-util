package com.cedarsoftware.util.convert;

import java.io.File;
import java.lang.ref.WeakReference;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Asking the Converter about a class must not keep that class -- or its ClassLoader -- alive. Its caches are keyed by
 * (source, target) pairs, and an entry refers to both classes (an InheritancePair[] lists their supertypes). Kept on
 * the target class, an entry for an application class converting to String lived inside String.class, for the life
 * of the JVM, and held the application class, its loader and every class that loader loaded. An entry is now kept on
 * the class whose loader is the deeper of the two (or, for two classes of one loader, on either), and an entry for
 * classes of unrelated loaders is not kept at all.
 */
class ConverterClassLoaderLeakTest {

    static final String FIXTURE = "com.cedarsoftware.util.convert.ConverterClassLoaderLeakTest$Fixture";
    private static final String FIXTURE_LIST =
            "com.cedarsoftware.util.convert.ConverterClassLoaderLeakTest$FixtureList";

    /** Loaded in throwaway ClassLoaders whose parent is the bootstrap loader: may depend only on java.base. */
    public static class Fixture implements Comparable<Fixture>, java.io.Serializable {
        public String name;

        @Override
        public int compareTo(Fixture o) {
            return 0;
        }
    }

    public static class FixtureList extends ArrayList<Object> {
    }

    static URLClassLoader throwawayLoader() throws Exception {
        return new URLClassLoader(new URL[]{new File("target/test-classes").toURI().toURL()}, null);
    }

    /**
     * Both cases, sharing one forced collection (one costs about a second late in a JDK 8 run): a class asked about
     * with JDK classes, and classes of two unrelated loaders -- which have no class to be kept on, so the answer is
     * worked out and kept nowhere.
     */
    @Test
    void askingAboutClassesDoesNotKeepTheirLoadersAlive() throws Exception {
        WeakReference<ClassLoader> loader = askAboutFixtures();
        WeakReference<?>[] unrelated = askAcrossTwoLoaders();
        gcUntilCleared(loader, unrelated[0], unrelated[1]);
        assertNull(loader.get(), "the Converter kept the class loader alive");
        assertNull(unrelated[0].get(), "classes of unrelated loaders kept the first loader alive");
        assertNull(unrelated[1].get(), "classes of unrelated loaders kept the second loader alive");
    }

    private static WeakReference<ClassLoader> askAboutFixtures() throws Exception {
        URLClassLoader loader = throwawayLoader();
        Class<?> fixture = Class.forName(FIXTURE, false, loader);
        Class<?> fixtureList = Class.forName(FIXTURE_LIST, false, loader);
        assertNotSame(Fixture.class, fixture);
        Converter converter = new Converter(new DefaultConverterOptions());
        for (int i = 0; i < 2; i++) {               // the second time answers from the caches
            assertFalse(converter.isConversionSupportedFor(fixture, String.class));
            assertFalse(converter.isConversionSupportedFor(Map.class, fixture));
            assertFalse(converter.isSimpleTypeConversionSupported(fixture, String.class));
            assertTrue(converter.isConversionSupportedFor(Collection.class, fixtureList));
            Object converted = converter.convert(new ArrayList<>(Arrays.asList(1, 2)), fixtureList);
            assertEquals(Arrays.asList(1, 2), new ArrayList<>((Collection<?>) converted));
        }
        loader.close();
        return new WeakReference<>(loader);
    }

    private static WeakReference<?>[] askAcrossTwoLoaders() throws Exception {
        URLClassLoader first = throwawayLoader();
        URLClassLoader second = throwawayLoader();
        Class<?> a = Class.forName(FIXTURE_LIST, false, first);
        Class<?> b = Class.forName(FIXTURE_LIST, false, second);
        assertNotSame(a, b);
        Converter converter = new Converter(new DefaultConverterOptions());
        for (int i = 0; i < 2; i++) {
            assertTrue(converter.isConversionSupportedFor(a, b));
            Object source = a.getConstructor().newInstance();
            @SuppressWarnings("unchecked") Collection<Object> items = (Collection<Object>) source;
            items.add("x");
            Object converted = converter.convert(source, b);
            assertEquals(Arrays.asList("x"), new ArrayList<>((Collection<?>) converted));
        }
        first.close();
        second.close();
        return new WeakReference<?>[]{new WeakReference<>(first), new WeakReference<>(second)};
    }

    static void gcUntilCleared(WeakReference<?>... refs) throws InterruptedException {
        for (int i = 0; i < 100 && java.util.Arrays.stream(refs).anyMatch(r -> r.get() != null); i++) {
            System.gc();
            byte[][] pressure = new byte[64][];
            for (int j = 0; j < pressure.length; j++) {
                pressure[j] = new byte[64 * 1024];
            }
            Thread.sleep(10);
        }
    }
}
