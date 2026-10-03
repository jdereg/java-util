package com.cedarsoftware.util;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * removeAll() of a collection backed by the map itself -- keySet().removeAll(keySet()),
 * entrySet().removeAll(entrySet()), a CompactSet's removeAll(itself), or an unmodifiable view of any of them --
 * empties it, as it does for any JDK collection. The key and entry views iterated the argument and removed each
 * element, which changed the collection being iterated: with two entries one was silently left behind, and with more
 * the iteration threw ConcurrentModificationException. They now iterate a snapshot of the argument.
 */
class CompactMapViewSelfRemovalTest {

    private static List<Supplier<CompactMap<String, Integer>>> maps() {
        return Arrays.asList(
                () -> CompactMap.<String, Integer>builder().compactSize(3).build(),
                () -> CompactMap.<String, Integer>builder().compactSize(3).insertionOrder().build(),
                () -> CompactMap.<String, Integer>builder().compactSize(3).sortedOrder().build(),
                () -> CompactMap.<String, Integer>builder().compactSize(3).caseSensitive(false).build(),
                CompactMap::new);
    }

    private static void emptiesTheMap(String what, Function<CompactMap<String, Integer>, Collection<?>> view,
                                      Function<CompactMap<String, Integer>, Collection<?>> argument) {
        for (Supplier<CompactMap<String, Integer>> supplier : maps()) {
            for (int entries : new int[]{1, 2, 3, 5, 60}) {
                CompactMap<String, Integer> map = supplier.get();
                for (int i = 0; i < entries; i++) {
                    map.put("k" + i, i);
                }
                view.apply(map).removeAll(argument.apply(map));
                assertTrue(map.isEmpty(), what + " with " + entries + " entries left " + map);
            }
        }
    }

    @Test
    void keySetRemovesItself() {
        emptiesTheMap("keySet().removeAll(keySet())", Map::keySet, Map::keySet);
        emptiesTheMap("keySet().removeAll(unmodifiable keySet())", Map::keySet,
                m -> Collections.unmodifiableSet(m.keySet()));
    }

    @Test
    void entrySetRemovesItself() {
        emptiesTheMap("entrySet().removeAll(entrySet())", Map::entrySet, Map::entrySet);
    }

    @Test
    void aCompactSetRemovesItself() {
        for (Supplier<CompactSet<String>> supplier : Arrays.<Supplier<CompactSet<String>>>asList(CompactSet::new,
                CompactLinkedSet::new, CompactCIHashSet::new,
                () -> CompactSet.<String>builder().compactSize(3).sortedOrder().build())) {
            for (int entries : new int[]{1, 2, 3, 5, 60}) {
                Set<String> set = supplier.get();
                for (int i = 0; i < entries; i++) {
                    set.add("k" + i);
                }
                set.removeAll(set);
                assertTrue(set.isEmpty(), set.getClass().getSimpleName() + " with " + entries + " left " + set);
            }
        }
    }
}
