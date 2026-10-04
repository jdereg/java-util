package com.cedarsoftware.util;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.function.Function;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * removeAll() of a collection backed by the set itself -- the set, its backing map's key set, an unmodifiable view of
 * it -- removes everything, as it does for any JDK set. CaseInsensitiveSet's removeAll() and CaseInsensitiveMap's
 * keySet().removeAll() iterate the argument and remove each element (so that matching is case-insensitive whatever
 * the sizes), and removing from the set while iterating a view of it threw ConcurrentModificationException. They now
 * iterate a snapshot of the argument.
 */
class CaseInsensitiveSetSelfRemovalTest {

    private static List<Collection<String>> sources() {
        List<String> words = Arrays.asList("Alpha", "beta", "GAMMA");
        Set<String> keySetView = ConcurrentHashMap.newKeySet();
        keySetView.addAll(words);
        return Arrays.asList(words, new LinkedHashSet<>(words), new HashSet<>(words), new TreeSet<>(words),
                new ConcurrentSkipListSet<>(words), keySetView);
    }

    private static void removesEverything(String what, Function<CaseInsensitiveSet<String>, Collection<?>> self) {
        for (Collection<String> source : sources()) {
            CaseInsensitiveSet<String> set = new CaseInsensitiveSet<>(source);
            String label = what + " over "
                    + ((CaseInsensitiveMap<?, ?>) set.getBackingMap()).getWrappedMap().getClass().getSimpleName();
            assertTrue(set.removeAll(self.apply(set)), label);
            assertTrue(set.isEmpty(), label);
            assertFalse(set.removeAll(self.apply(set)), label);
        }
    }

    @Test
    void theSetItself() {
        removesEverything("removeAll(itself)", set -> set);
    }

    @Test
    void itsBackingMapsKeySet() {
        removesEverything("removeAll(getBackingMap().keySet())", set -> set.getBackingMap().keySet());
    }

    @Test
    void anUnmodifiableViewOfIt() {
        removesEverything("removeAll(unmodifiableSet(itself))", Collections::unmodifiableSet);
    }

    @Test
    @SuppressWarnings("deprecation")
    void minusItself() {
        for (Collection<String> source : sources()) {
            CaseInsensitiveSet<String> set = new CaseInsensitiveSet<>(source);
            set.minus((Iterable<String>) set);
            assertTrue(set.isEmpty());
        }
    }

    @Test
    void aCaseInsensitiveMapsKeySetToo() {
        for (Function<Set<String>, Collection<?>> self : Arrays.<Function<Set<String>, Collection<?>>>asList(
                keys -> keys, Collections::unmodifiableSet)) {
            Map<String, Integer> map = new CaseInsensitiveMap<>();
            map.put("Alpha", 1);
            map.put("beta", 2);
            assertTrue(map.keySet().removeAll(self.apply(map.keySet())));
            assertTrue(map.isEmpty());
        }
    }

    @Test
    void otherCollectionsStillMatchCaseInsensitively() {
        CaseInsensitiveSet<String> set = new CaseInsensitiveSet<>(Arrays.asList("Alpha", "beta"));
        assertTrue(set.removeAll(Arrays.asList("ALPHA", "x", "y")));
        assertEquals(Collections.singleton("beta"), new HashSet<>(set));
    }
}
