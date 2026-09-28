package com.cedarsoftware.util;

import java.io.File;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A ClassValueSet no longer keeps its members -- or their ClassLoaders -- alive. It kept every member in a backing set,
 * so a class from a discarded loader (a redeployed webapp, a plugin, a recompiled script) stayed in memory, with its
 * loader and every class that loader loaded, until removed. Each class's membership now lives in a flag stored in the
 * class itself, and the members are listed only weakly: a member goes away when its class is unloaded. Everything else
 * about the set is as before, which the random-operations test pins against a LinkedHashSet.
 */
class ClassValueSetWeakMembersTest {

    private static final String FIXTURE = "com.cedarsoftware.util.ClassValueSetWeakMembersTest$LoaderFixture";

    /** Loaded in a throwaway ClassLoader whose parent is the bootstrap loader: may depend only on java.lang. */
    public static class LoaderFixture {
    }

    @Test
    void aMemberClassIsNotKeptAliveAndGoesAway() throws Exception {
        ClassValueSet set = new ClassValueSet();
        set.add(String.class);
        WeakReference<ClassLoader> loader = addAFixture(set);
        assertEquals(3, set.size());

        for (int i = 0; i < 100 && (loader.get() != null || set.size() != 1); i++) {
            System.gc();
            byte[][] pressure = new byte[64][];
            for (int j = 0; j < pressure.length; j++) {
                pressure[j] = new byte[64 * 1024];
            }
            Thread.sleep(10);
        }
        assertNull(loader.get(), "the set kept the class loader alive");
        assertEquals(1, set.size(), "the unloaded classes must go");
        assertEquals(new HashSet<>(Arrays.asList(String.class)), set.toSet());
        assertTrue(set.containsClass(String.class));
    }

    /** Adds a class of a throwaway loader and its array class, asks about a third, and drops the loader. */
    private static WeakReference<ClassLoader> addAFixture(ClassValueSet set) throws Exception {
        URLClassLoader loader = new URLClassLoader(new URL[]{new File("target/test-classes").toURI().toURL()}, null);
        Class<?> fixture = Class.forName(FIXTURE, false, loader);
        assertNotSame(LoaderFixture.class, fixture, "the fixture must be defined by the throwaway loader");
        set.add(fixture);
        set.add(java.lang.reflect.Array.newInstance(fixture, 0).getClass());
        assertTrue(set.contains(fixture));
        assertFalse(set.containsClass(java.lang.reflect.Array.newInstance(fixture, 0, 0).getClass()));
        loader.close();
        return new WeakReference<>(loader);
    }

    /**
     * The flag a set keeps in a class is a JDK type: a ClassValue's value lives inside its Class, and a java-util type
     * stored in a JDK class would keep java-util's own class loader alive for the life of the JVM.
     */
    @Test
    void theFlagKeptInAClassIsAJdkType() throws Exception {
        ClassValueSet set = ClassValueSet.of(String.class);
        set.contains(Integer.class);
        Field membersField = ClassValueSet.class.getDeclaredField("members");
        membersField.setAccessible(true);
        ClassValue<?> flags = (ClassValue<?>) membersField.get(set);     // the members holder is the ClassValue
        for (Class<?> c : new Class<?>[]{String.class, Integer.class}) {
            assertNull(flags.get(c).getClass().getClassLoader(), c + " holds a " + flags.get(c).getClass());
        }
    }

    /**
     * Random operations -- every Set method, the null element, iterator removal, and bulk operations against plain
     * sets and other ClassValueSets -- against a LinkedHashSet that stands for what a Set must answer. Fixed seed.
     */
    @Test
    void randomOperationsAgreeWithALinkedHashSet() {
        Class<?>[] pool = {null, String.class, Integer.class, Long.class, Object.class, List.class, Map.class,
                Thread.class, Number.class, Runnable.class, int.class, String[].class};
        ClassValueSet set = new ClassValueSet();
        Set<Class<?>> expected = new LinkedHashSet<>();
        Random random = new Random(20260928L);
        for (int step = 0; step < 20_000; step++) {
            Class<?> c = pool[random.nextInt(pool.length)];
            switch (random.nextInt(12)) {
                case 0:
                case 1:
                case 2:
                    assertEquals(expected.add(c), set.add(c), "add " + c);
                    break;
                case 3:
                case 4:
                    assertEquals(expected.remove(c), set.remove(c), "remove " + c);
                    break;
                case 5:
                    assertEquals(expected.contains(c), set.contains(c), "contains " + c);
                    assertEquals(expected.contains(c), set.containsClass(c), "containsClass " + c);
                    break;
                case 6: {
                    Set<Class<?>> other = randomSubset(pool, random);
                    Set<Class<?>> argument = random.nextBoolean() ? new ClassValueSet(other) : other;
                    assertEquals(expected.retainAll(other), set.retainAll(argument), "retainAll " + other);
                    break;
                }
                case 7: {
                    Set<Class<?>> other = randomSubset(pool, random);
                    Set<Class<?>> argument = random.nextBoolean() ? new ClassValueSet(other) : other;
                    assertEquals(expected.removeAll(other), set.removeAll(argument), "removeAll " + other);
                    break;
                }
                case 8: {
                    Iterator<Class<?>> it = set.iterator();
                    while (it.hasNext()) {
                        Class<?> next = it.next();
                        if (random.nextInt(4) == 0) {
                            it.remove();
                            expected.remove(next);
                        }
                    }
                    break;
                }
                case 9:
                    if (random.nextInt(50) == 0) {
                        set.clear();
                        expected.clear();
                    }
                    break;
                case 10:
                    assertEquals(expected.containsAll(Arrays.asList(c, String.class)),
                            set.containsAll(Arrays.asList(c, String.class)));
                    break;
                default:
                    break;
            }
            assertEquals(expected.size(), set.size(), "size after step " + step);
            assertEquals(expected.isEmpty(), set.isEmpty());
            assertEquals(expected, set.toSet());
            assertTrue(set.equals(expected) && expected.equals(set), "equals");
            assertEquals(expected.hashCode(), set.hashCode(), "hashCode");
        }
    }

    private static Set<Class<?>> randomSubset(Class<?>[] pool, Random random) {
        Set<Class<?>> subset = new HashSet<>();
        for (Class<?> c : pool) {
            if (random.nextInt(3) == 0) {
                subset.add(c);
            }
        }
        return subset;
    }

    @Test
    void concurrentAddsAndRemovesLeaveAConsistentSet() throws Exception {
        ClassValueSet set = new ClassValueSet();
        Class<?>[] classes = {String.class, Integer.class, Long.class, Double.class, Object.class, List.class, Map.class,
                Thread.class, Number.class, Runnable.class, HashMap.class, ArrayList.class};
        int threads = 6;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<?>> done = new ArrayList<>();
        for (int t = 0; t < threads; t++) {
            final int seed = t;
            done.add(pool.submit(() -> {
                start.await();
                Random random = new Random(seed);
                for (int i = 0; i < 20_000; i++) {
                    Class<?> c = classes[random.nextInt(classes.length)];
                    if (random.nextBoolean()) {
                        set.add(c);
                    } else {
                        set.remove(c);
                    }
                }
                return null;
            }));
        }
        start.countDown();
        for (Future<?> f : done) {
            f.get(30, TimeUnit.SECONDS);
        }
        pool.shutdown();
        int listed = 0;
        for (Class<?> c : set) {
            listed++;
            assertTrue(set.containsClass(c));
        }
        int present = 0;
        for (Class<?> c : classes) {
            if (set.containsClass(c)) {
                present++;
            }
        }
        assertEquals(present, listed, "iteration and containsClass() disagree");
        assertEquals(present, set.size(), "size() and containsClass() disagree");
    }
}
