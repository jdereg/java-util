package com.cedarsoftware.util.convert;

import java.io.File;
import java.lang.reflect.Array;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Converter.ClassPairMap keeps a class's entries in one small open-addressed table, probed by identity hash and
 * replaced whole on every write. These pin what that structure could get wrong: an entry reached through a collision
 * chain after others are removed, growth and shrinking to empty, null classes and null values, one winner among
 * concurrent putIfAbsent calls, iteration that writes back, and which class of a pair holds it. A probe walks
 * occupied slots until it finds a free one, so a table filled to the brim spins where an interrupt cannot stop it:
 * the class is timed on a separate thread.
 */
@Timeout(value = 60, unit = TimeUnit.SECONDS, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
class ConverterClassPairMapTest {

    private static final String FIXTURE = "com.cedarsoftware.util.convert.ConverterClassLoaderLeakTest$Fixture";

    /** 160 distinct JDK classes: 40 classes and their arrays of one to three dimensions. */
    private static List<Class<?>> manyClasses() {
        Class<?>[] bases = {String.class, Integer.class, Long.class, Short.class, Byte.class, Double.class,
                Float.class, Boolean.class, Character.class, Object.class, Number.class, CharSequence.class,
                StringBuilder.class, java.math.BigDecimal.class, java.math.BigInteger.class, java.util.Date.class,
                java.sql.Date.class, java.sql.Timestamp.class, java.time.Instant.class, java.time.LocalDate.class,
                java.time.LocalDateTime.class, java.time.ZonedDateTime.class, java.time.Duration.class,
                java.util.UUID.class, java.util.List.class, java.util.ArrayList.class, java.util.Map.class,
                java.util.HashMap.class, java.util.Set.class, java.util.TreeSet.class, java.util.Calendar.class,
                java.util.Locale.class, java.util.Currency.class, java.util.TimeZone.class, java.net.URI.class,
                java.net.URL.class, java.io.File.class, int.class, long.class, double.class};
        List<Class<?>> classes = new ArrayList<>();
        for (Class<?> base : bases) {
            Class<?> c = base;
            for (int d = 0; d < 4; d++) {
                classes.add(c);
                c = Array.newInstance(c, 0).getClass();
            }
        }
        return classes;
    }

    @Test
    void everyEntryIsFoundAsOthersAreRemoved() {
        Converter.ClassPairMap<String> map = new Converter.ClassPairMap<>();
        List<Class<?>> classes = manyClasses();
        List<Class<?>[]> pairs = new ArrayList<>();
        for (Class<?> c : classes) {
            pairs.add(new Class<?>[]{String.class, c});     // one class with 160 partners
            pairs.add(new Class<?>[]{c, void.class});       // 160 classes with one
        }
        for (Class<?>[] pair : pairs) {
            assertNull(map.put(pair[0], pair[1], value(pair)));
        }
        assertEquals(pairs.size(), map.size());
        Collections.shuffle(pairs, new Random(7));
        for (int k = 0; k < pairs.size(); k++) {
            Class<?>[] gone = pairs.get(k);
            assertEquals(value(gone), map.remove(gone[0], gone[1]));
            assertNull(map.remove(gone[0], gone[1]));
            for (int j = 0; j < pairs.size(); j++) {
                Class<?>[] pair = pairs.get(j);
                assertEquals(j <= k ? null : value(pair), map.get(pair[0], pair[1]));
            }
            assertEquals(pairs.size() - k - 1, map.size());
        }
        assertTrue(map.isEmpty());
        assertNull(map.put(String.class, Long.class, "again"));
        assertEquals("again", map.get(String.class, Long.class));
        assertFalse(map.isEmpty());
    }

    private static String value(Class<?>[] pair) {
        return pair[0].getName() + ">" + pair[1].getName();
    }

    @Test
    void nullClassesAndNullValuesAreKeptLikeAnyOther() {
        Converter.ClassPairMap<String> map = new Converter.ClassPairMap<>();
        assertNull(map.put(null, String.class, "a"));
        assertNull(map.put(String.class, null, "b"));
        assertNull(map.put(null, null, "c"));
        assertNull(map.put(Long.class, Integer.class, null));
        assertEquals("a", map.get(null, String.class));
        assertEquals("b", map.get(String.class, null));
        assertEquals("c", map.get(null, null));
        assertNull(map.get(Long.class, Integer.class));
        assertEquals(4, map.size());                        // the entry holding null is an entry

        assertNull(map.putIfAbsent(Long.class, Integer.class, "d"));    // ... that putIfAbsent treats as absent
        assertEquals("d", map.putIfAbsent(Long.class, Integer.class, "e"));
        assertEquals("d", map.get(Long.class, Integer.class));
        assertEquals("a", map.putIfAbsent(null, String.class, "f"));

        Set<String> seen = new HashSet<>();
        map.forEach((source, target, value) -> seen.add(source + " > " + target + " = " + value));
        Set<String> expected = new HashSet<>();
        expected.add("null > class java.lang.String = a");
        expected.add("class java.lang.String > null = b");
        expected.add("null > null = c");
        expected.add("class java.lang.Long > class java.lang.Integer = d");
        assertEquals(expected, seen);

        map.removeIf((source, target) -> source == null && target == null);
        assertNull(map.get(null, null));
        assertEquals("a", map.remove(null, String.class));
        assertNull(map.get(null, String.class));
        assertEquals(2, map.size());
    }

    @Test
    void concurrentPutIfAbsentHasOneWinnerPerPair() throws Exception {
        List<Class<?>> classes = manyClasses();
        int threads = 8;
        for (int round = 0; round < 50; round++) {
            Converter.ClassPairMap<Object> map = new Converter.ClassPairMap<>();
            Object[][] seen = new Object[threads][classes.size()];
            CountDownLatch start = new CountDownLatch(1);
            List<Thread> workers = new ArrayList<>();
            List<Throwable> failures = Collections.synchronizedList(new ArrayList<>());
            for (int t = 0; t < threads; t++) {
                final int id = t;
                Thread worker = new Thread(() -> {
                    try {
                        start.await();
                        for (int i = 0; i < classes.size(); i++) {
                            // Half the threads walk the classes backwards, so writers meet on every table
                            int k = id % 2 == 0 ? i : classes.size() - 1 - i;
                            Object mine = new Object();
                            Object previous = map.putIfAbsent(classes.get(k), String.class, mine);
                            seen[id][k] = previous == null ? mine : previous;
                        }
                    } catch (Throwable e) {
                        failures.add(e);
                    }
                });
                worker.setDaemon(true);
                worker.start();
                workers.add(worker);
            }
            start.countDown();
            for (Thread worker : workers) {
                worker.join();
            }
            assertEquals(Collections.emptyList(), failures);
            for (int k = 0; k < classes.size(); k++) {
                Object winner = map.get(classes.get(k), String.class);
                for (int t = 0; t < threads; t++) {
                    assertSame(winner, seen[t][k], "thread " + t + " saw another value for " + classes.get(k));
                }
            }
            assertEquals(classes.size(), map.size());
        }
    }

    /** forEach walks the tables as they were when it began, so writing back to the map from it is safe. */
    @Test
    void forEachMayWriteToTheMap() {
        Converter.ClassPairMap<String> map = new Converter.ClassPairMap<>();
        List<Class<?>> classes = manyClasses().subList(0, 40);
        for (Class<?> c : classes) {
            map.put(c, Void.class, "forward");
        }
        List<String> visited = new ArrayList<>();
        map.forEach((source, target, value) -> {
            visited.add(value);
            map.put(target, source, "back");            // Void gains a table: a new class holding entries
            map.remove(source, target);
        });
        assertEquals(classes.size(), visited.size());
        assertEquals(Collections.nCopies(classes.size(), "forward"), visited);
        assertEquals(classes.size(), map.size());
        for (Class<?> c : classes) {
            assertNull(map.get(c, Void.class));
            assertEquals("back", map.get(Void.class, c));
        }
    }

    /**
     * A pair is kept on the class whose loader is the deeper, and not at all when neither loader is the other's
     * ancestor; iteration and removeIf report it the right way round.
     */
    @Test
    void eachPairIsKeptWhereItsLoadersAllow() throws Exception {
        URL testClasses = new File("target/test-classes").toURI().toURL();
        try (URLClassLoader first = new URLClassLoader(new URL[]{testClasses}, null);
             URLClassLoader second = new URLClassLoader(new URL[]{testClasses}, null)) {
            Class<?> a = Class.forName(FIXTURE, false, first);
            Class<?> b = Class.forName(FIXTURE, false, second);
            assertNotSame(a, b);
            Converter.ClassPairMap<String> map = new Converter.ClassPairMap<>();
            assertNull(map.put(String.class, a, "jdk to fixture"));         // kept on the fixture, the deeper
            assertNull(map.put(a, String.class, "fixture to jdk"));         // kept on the fixture
            assertNull(map.put(a, b, "unrelated"));                         // kept nowhere
            assertNull(map.put(getClass(), a, "app to fixture"));           // app loader and first are unrelated
            assertEquals("jdk to fixture", map.get(String.class, a));
            assertEquals("fixture to jdk", map.get(a, String.class));
            assertNull(map.get(a, b));
            assertNull(map.get(getClass(), a));
            assertEquals("jdk to fixture", map.putIfAbsent(String.class, a, "other"));
            assertNull(map.putIfAbsent(b, a, "unrelated"));
            assertNull(map.get(b, a));
            assertEquals(2, map.size());

            Set<String> seen = new HashSet<>();
            map.forEach((source, target, value) -> seen.add(source.getSimpleName() + " > " + target.getSimpleName()
                    + " = " + value));
            Set<String> expected = new HashSet<>();
            expected.add("String > Fixture = jdk to fixture");
            expected.add("Fixture > String = fixture to jdk");
            assertEquals(expected, seen);

            map.removeIf((source, target) -> source == String.class);
            assertNull(map.get(String.class, a));
            assertEquals("fixture to jdk", map.get(a, String.class));
            assertEquals(1, map.size());
            assertEquals("fixture to jdk", map.remove(a, String.class));
            assertTrue(map.isEmpty());
        }
    }
}
