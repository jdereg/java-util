package com.cedarsoftware.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Streams over the null-safe maps' views and ConcurrentNavigableSetNullSafe while the collection changes. Their
 * spliterators promised a fixed size (the Collection default), so a stream whose source grew or shrank under it
 * threw IllegalStateException ("Accept exceeded fixed size", "End size is less than fixed size"). A peek() that
 * writes to the source stands in for another thread doing so.
 */
class ConcurrentNullSafeViewStreamTest {

    /** A view of a fresh source holding "a" .. "t", and how to add or remove elements in that source. */
    private static final class Case {
        final Supplier<Collection<?>> view;
        final java.util.function.Consumer<Integer> grow;
        final Runnable shrink;

        Case(Supplier<Collection<?>> view, java.util.function.Consumer<Integer> grow, Runnable shrink) {
            this.view = view;
            this.grow = grow;
            this.shrink = shrink;
        }
    }

    private static Map<String, Case> cases() {
        Map<String, Case> cases = new LinkedHashMap<>();
        for (String kind : new String[]{"hash", "navigable"}) {
            for (String which : new String[]{"keySet", "values", "entrySet", "descendingKeySet", "headMap.keySet"}) {
                if (kind.equals("hash") && (which.startsWith("descending") || which.startsWith("headMap"))) {
                    continue;
                }
                Map<String, Integer> map = kind.equals("hash") ? new ConcurrentHashMapNullSafe<>()
                        : new ConcurrentNavigableMapNullSafe<>();
                for (int i = 0; i < 20; i++) {
                    map.put(String.valueOf((char) ('a' + i)), i);
                }
                Supplier<Collection<?>> view;
                if (which.equals("keySet")) {
                    view = map::keySet;
                } else if (which.equals("values")) {
                    view = map::values;
                } else if (which.equals("entrySet")) {
                    view = map::entrySet;
                } else if (which.equals("descendingKeySet")) {
                    view = () -> ((NavigableMap<String, Integer>) map).descendingKeySet();
                } else {
                    view = () -> ((NavigableMap<String, Integer>) map).headMap("~").keySet();
                }
                cases.put(kind + " " + which, new Case(view,
                        n -> {
                            for (int i = 0; i < n; i++) {
                                map.put("n" + i, i);
                            }
                        },
                        () -> map.keySet().removeIf(k -> k.compareTo("c") > 0)));
            }
        }
        for (String which : new String[]{"set", "descendingSet"}) {
            ConcurrentNavigableSetNullSafe<String> set = new ConcurrentNavigableSetNullSafe<>();
            for (int i = 0; i < 20; i++) {
                set.add(String.valueOf((char) ('a' + i)));
            }
            Supplier<Collection<?>> view = which.equals("set") ? () -> set : set::descendingSet;
            cases.put("navigable set " + which, new Case(view,
                    n -> {
                        for (int i = 0; i < n; i++) {
                            set.add("n" + i);
                        }
                    },
                    () -> set.removeIf(e -> e.compareTo("c") > 0)));
        }
        return cases;
    }

    @Test
    void aStreamWhoseSourceGrowsDoesNotThrow() {
        for (Map.Entry<String, Case> e : cases().entrySet()) {
            Case c = e.getValue();
            boolean[] first = {true};
            Object[] seen = c.view.get().stream().peek(x -> {
                if (first[0]) {
                    first[0] = false;
                    c.grow.accept(50);
                }
            }).toArray();
            assertTrue(seen.length >= 20, e.getKey() + ": " + seen.length);
        }
    }

    @Test
    void aStreamWhoseSourceShrinksDoesNotThrow() {
        for (Map.Entry<String, Case> e : cases().entrySet()) {
            Case c = e.getValue();
            boolean[] first = {true};
            Object[] seen = c.view.get().stream().peek(x -> {
                if (first[0]) {
                    first[0] = false;
                    c.shrink.run();
                }
            }).toArray();
            assertTrue(seen.length >= 1 && seen.length <= 20, e.getKey() + ": " + seen.length);
        }
    }

    /** The stream hands out what iteration does, in the same order, null keys, values and elements included. */
    @Test
    void aStreamMatchesIterationNullsIncluded() {
        ConcurrentHashMapNullSafe<String, Integer> hash = new ConcurrentHashMapNullSafe<>();
        ConcurrentNavigableMapNullSafe<String, Integer> navigable = new ConcurrentNavigableMapNullSafe<>();
        ConcurrentNavigableSetNullSafe<String> set = new ConcurrentNavigableSetNullSafe<>();
        for (String k : Arrays.asList("b", null, "a", "c")) {
            hash.put(k, k == null ? null : k.length());
            navigable.put(k, k == null ? null : k.length());
            set.add(k);
        }
        List<Collection<?>> views = Arrays.asList(hash.keySet(), hash.values(), hash.entrySet(), navigable.keySet(),
                navigable.values(), navigable.entrySet(), navigable.descendingKeySet(), set, set.descendingSet());
        for (Collection<?> view : views) {
            List<Object> iterated = new ArrayList<>(view);
            List<Object> streamed = view.stream().collect(Collectors.toList());
            assertEquals(iterated, streamed, view.getClass().getName());
            assertEquals(1, streamed.stream().filter(Objects::isNull).count()
                    + streamed.stream().filter(x -> x instanceof Map.Entry
                            && ((Map.Entry<?, ?>) x).getKey() == null).count(), view.getClass().getName());
        }
        assertEquals(Arrays.asList("a", "b", "c", null), navigable.keySet().stream().collect(Collectors.toList()));
        assertEquals(Arrays.asList("a", "b", "c", null),
                navigable.keySet().stream().sorted(java.util.Comparator.nullsLast(
                        java.util.Comparator.<String>naturalOrder())).collect(Collectors.toList()));
    }

    /** An entry from the entry set's stream writes setValue through to the map, as an iterated one does. */
    @Test
    void aStreamedEntryWritesThrough() {
        ConcurrentHashMapNullSafe<String, Integer> map = new ConcurrentHashMapNullSafe<>();
        map.put("a", 1);
        map.put(null, 2);
        map.entrySet().stream().forEach(e -> e.setValue(e.getValue() * 10));
        assertEquals(Integer.valueOf(10), map.get("a"));
        assertEquals(Integer.valueOf(20), map.get(null));
        Set<String> keys = map.keySet();
        assertTrue(keys.stream().anyMatch(Objects::isNull));
    }
}
