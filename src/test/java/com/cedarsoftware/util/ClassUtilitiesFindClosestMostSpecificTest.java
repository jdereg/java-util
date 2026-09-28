package com.cedarsoftware.util;

import java.io.Serializable;
import java.util.AbstractCollection;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Random;
import java.util.RandomAccess;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * findClosest() chooses the most specific candidate the class is assignable to, as Java chooses the most specific
 * overload (JLS 15.12.2.5): a candidate that another applicable candidate is a subtype of is never chosen, however
 * few steps away it is. It chose by distance, preferring a class on a tie, so for an ArrayList it chose Object over
 * Collection (3 steps each) and over Iterable (4).
 */
class ClassUtilitiesFindClosestMostSpecificTest {

    private static Map<Class<?>, String> candidates(Class<?>... types) {
        Map<Class<?>, String> map = new LinkedHashMap<>();
        for (Class<?> type : types) {
            map.put(type, type.getSimpleName());
        }
        return map;
    }

    @Test
    void aSubtypeIsChosenOverItsSupertype() {
        assertEquals("Collection", ClassUtilities.findClosest(ArrayList.class, candidates(Object.class, Collection.class), "none"));
        assertEquals("Iterable", ClassUtilities.findClosest(ArrayList.class, candidates(Object.class, Iterable.class), "none"));
        assertEquals("List", ClassUtilities.findClosest(ArrayList.class,
                candidates(Object.class, Iterable.class, Collection.class, List.class), "none"));
        assertEquals("Map", ClassUtilities.findClosest(ConcurrentHashMap.class, candidates(Object.class, Map.class), "none"));
    }

    @Test
    void unrelatedCandidatesAreOrderedByDistanceThenClassOverInterface() {
        // RandomAccess (1 step) is unrelated to AbstractCollection (2), which is more specific than Collection
        assertEquals("RandomAccess", ClassUtilities.findClosest(ArrayList.class,
                candidates(Object.class, Collection.class, AbstractCollection.class, RandomAccess.class), "none"));
        // AbstractList and List are both 1 step from ArrayList; AbstractList implements List, so it is more specific
        assertEquals("AbstractList", ClassUtilities.findClosest(ArrayList.class,
                candidates(List.class, AbstractList.class), "none"));
        // Unrelated at the same distance: the class
        assertEquals("AbstractList", ClassUtilities.findClosest(ArrayList.class,
                candidates(RandomAccess.class, Cloneable.class, AbstractList.class), "none"));
        assertEquals("none", ClassUtilities.findClosest(ArrayList.class, candidates(Map.class, Queue.class), "none"));
        assertSame(null, ClassUtilities.findClosest(LinkedList.class, candidates(RandomAccess.class), null));
    }

    /**
     * The rule restated: the answer is applicable, no other applicable candidate is a subtype of it, and none of the
     * candidates that pass that test is fewer steps away. It does not depend on the map's order unless two such
     * candidates tie. Checked over random candidate sets, each in every order it can be given.
     */
    @Test
    void theChoiceIsTheMostSpecificWhateverTheOrder() {
        Class<?>[] pool = {Object.class, Serializable.class, Cloneable.class, Iterable.class, Collection.class,
                List.class, Queue.class, RandomAccess.class, AbstractCollection.class, AbstractList.class, Map.class,
                CharSequence.class, Comparable.class, Number.class};
        Class<?>[] subjects = {ArrayList.class, LinkedList.class, HashMap.class, ConcurrentHashMap.class,
                String.class, Integer.class, StringBuilder.class, Object[].class, String[].class};
        Random random = new Random(42);
        for (int round = 0; round < 400; round++) {
            List<Class<?>> chosen = new ArrayList<>(Arrays.asList(pool));
            Collections.shuffle(chosen, random);
            chosen = chosen.subList(0, 2 + random.nextInt(4));
            for (Class<?> subject : subjects) {
                String expected = null;
                for (int order = 0; order < 6; order++) {
                    Collections.shuffle(chosen, random);
                    Map<Class<?>, String> map = candidates(chosen.toArray(new Class<?>[0]));
                    String answer = ClassUtilities.findClosest(subject, map, "none");
                    assertOracle(subject, chosen, answer);
                    if (!hasTie(subject, chosen)) {
                        if (expected == null) {
                            expected = answer;
                        }
                        assertEquals(expected, answer, subject.getSimpleName() + " " + chosen);
                    }
                }
            }
        }
    }

    private static List<Class<?>> undominated(Class<?> subject, List<Class<?>> chosen) {
        List<Class<?>> applicable = new ArrayList<>();
        for (Class<?> c : chosen) {
            if (c.isAssignableFrom(subject)) {
                applicable.add(c);
            }
        }
        List<Class<?>> result = new ArrayList<>();
        for (Class<?> c : applicable) {
            boolean dominated = false;
            for (Class<?> other : applicable) {
                dominated |= other != c && c.isAssignableFrom(other);
            }
            if (!dominated) {
                result.add(c);
            }
        }
        return result;
    }

    private static void assertOracle(Class<?> subject, List<Class<?>> chosen, String answer) {
        List<Class<?>> best = undominated(subject, chosen);
        if (best.isEmpty()) {
            assertEquals("none", answer, subject.getSimpleName() + " " + chosen);
            return;
        }
        int least = Integer.MAX_VALUE;
        for (Class<?> c : best) {
            least = Math.min(least, ClassUtilities.computeInheritanceDistance(subject, c));
        }
        boolean found = false;
        for (Class<?> c : best) {
            found |= c.getSimpleName().equals(answer) && ClassUtilities.computeInheritanceDistance(subject, c) == least;
        }
        assertTrue(found, subject.getSimpleName() + " " + chosen + " gave " + answer + ", expected one of " + best
                + " at " + least + " steps");
    }

    private static boolean hasTie(Class<?> subject, List<Class<?>> chosen) {
        List<Class<?>> best = undominated(subject, chosen);
        int least = Integer.MAX_VALUE;
        int count = 0;
        for (Class<?> c : best) {
            int d = ClassUtilities.computeInheritanceDistance(subject, c);
            if (d < least) {
                least = d;
                count = 1;
            } else if (d == least) {
                count++;
            }
        }
        return count > 1;
    }
}
