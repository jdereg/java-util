package com.cedarsoftware.util;

import java.lang.reflect.Proxy;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Supplier;
import java.util.stream.Stream;

import com.cedarsoftware.util.cache.LockingLRUCacheStrategy;
import com.cedarsoftware.util.cache.ThreadedLRUCacheStrategy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * No java-util container may die with {@link StackOverflowError} when its {@code toString()} meets a cyclic graph
 * -- and none may change what it prints for anything that is not a cycle.
 * <p>
 * Every java-util Map, Set and List is exercised against four cycle shapes: holding itself, a loop through a plain
 * JDK map, a loop through a plain JDK list, and a loop through a second instance of its own class. Before
 * 4.112.0, fifteen of them overflowed on at least one shape, for one of four reasons:
 * <ul>
 *   <li><b>A wrapper delegated its toString()</b> to the container it wraps ({@link TrackingMap},
 *       {@link CaseInsensitiveSet}, {@link CompactSet} and its subclasses, {@link LRUCache}). The inner
 *       container's self-guard compares against the INNER container, never the wrapper, so even a wrapper
 *       holding itself overflowed.</li>
 *   <li><b>A direct-only guard</b> -- {@code e == this ? "(this ...)" : e} -- which a two-step loop walks past
 *       ({@link ConcurrentSet}, {@link ConcurrentList}, {@link LockingLRUCacheStrategy},
 *       {@link MapUtilities#mapToString}).</li>
 *   <li><b>No guard at all</b> ({@link TTLCache}), or the JDK's direct-only guard inherited unchanged
 *       ({@link CaseInsensitiveMap}, {@link ClassValueMap}, {@link IdentitySet},
 *       {@link ConcurrentNavigableSetNullSafe}).</li>
 *   <li><b>A view that hashed values.</b> {@link ThreadedLRUCacheStrategy} and {@link MultiKeyMap} built
 *       {@code entrySet()} as a hash set of entries, and {@code Map.Entry.hashCode()} hashes the VALUE -- so
 *       {@code entrySet()}, and the {@code toString()} that walks it, recursed without rendering anything.</li>
 * </ul>
 * All of them now render through one shared walk ({@code CycleSafeToString}), whose per-thread path is what lets
 * a loop be seen when it closes through a different container than it started in.
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
class ContainerToStringCycleTest {

    // ------------------------------------------------------------------------------------------- the maps

    static Stream<Arguments> maps() {
        return Stream.of(
                map("CaseInsensitiveMap", CaseInsensitiveMap::new),
                map("CompactMap", CompactMap::new),
                map("CompactCIHashMap", CompactCIHashMap::new),
                map("CompactCILinkedMap", CompactCILinkedMap::new),
                map("CompactLinkedMap", CompactLinkedMap::new),
                map("ConcurrentHashMapNullSafe", ConcurrentHashMapNullSafe::new),
                map("ConcurrentNavigableMapNullSafe", ConcurrentNavigableMapNullSafe::new),
                map("TrackingMap", () -> new TrackingMap<>(new LinkedHashMap<>())),
                map("LRUCache(LOCKING)", () -> new LRUCache<>(100, LRUCache.StrategyType.LOCKING)),
                map("LRUCache(THREADED)", () -> new LRUCache<>(100, LRUCache.StrategyType.THREADED)),
                map("LockingLRUCacheStrategy", () -> new LockingLRUCacheStrategy<>(100)),
                map("ThreadedLRUCacheStrategy", () -> new ThreadedLRUCacheStrategy<>(100, 60_000)),
                map("TTLCache", () -> new TTLCache<>(60_000)));
    }

    private static Arguments map(String name, Supplier<Map<Object, Object>> factory) {
        return Arguments.of(name, factory);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("maps")
    void mapHoldingItselfPrintsThisMap(String name, Supplier<Map<Object, Object>> factory) {
        Map<Object, Object> map = factory.get();
        map.put("self", map);
        assertEquals("{self=(this Map)}", map.toString());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("maps")
    void loopThroughAJdkMapTerminates(String name, Supplier<Map<Object, Object>> factory) {
        Map<Object, Object> map = factory.get();
        Map<Object, Object> hop = new LinkedHashMap<>();
        map.put("hop", hop);
        hop.put("back", map);
        assertEquals("{hop={back=(cycle)}}", map.toString());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("maps")
    void loopThroughAJdkListTerminates(String name, Supplier<Map<Object, Object>> factory) {
        Map<Object, Object> map = factory.get();
        List<Object> hop = new ArrayList<>();
        map.put("hop", hop);
        hop.add(map);
        assertEquals("{hop=[(cycle)]}", map.toString());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("maps")
    void loopThroughAnotherOfTheSameClassTerminates(String name, Supplier<Map<Object, Object>> factory) {
        Map<Object, Object> map = factory.get();
        Map<Object, Object> other = factory.get();
        map.put("other", other);
        other.put("back", map);
        assertEquals("{other={back=(cycle)}}", map.toString());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("maps")
    void theViewsDoNotHashTheirValues(String name, Supplier<Map<Object, Object>> factory) {
        // entrySet() is what toString() walks, and what callers iterate. Building it must not hash a value.
        Map<Object, Object> map = factory.get();
        Map<Object, Object> hop = new LinkedHashMap<>();
        map.put("hop", hop);
        hop.put("back", map);
        assertEquals(1, count(map.entrySet()));
        assertEquals(1, count(map.keySet()));
        assertEquals(1, count(map.values()));
        assertTrue(map.entrySet().iterator().next().getValue() == hop);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("maps")
    void acyclicOutputMatchesTheJdkRenderingOfItsOwnOrder(String name, Supplier<Map<Object, Object>> factory) {
        // The JDK's own rendering of exactly this map's iteration order: nothing that is not a cycle may change.
        Map<Object, Object> map = factory.get();
        map.put("a", 1);
        map.put("b", "two");
        map.put("c", Arrays.asList(1, null, 3));
        map.put("d", new TreeMap<>(java.util.Collections.singletonMap("x", "y")));
        map.put("e", new int[] {1, 2});
        assertEquals(new LinkedHashMap<>(map).toString(), map.toString());
    }

    @Test
    void classValueMapTerminatesOnACycle() {
        // Its keys must be Classes, so it cannot join the parameterized set above.
        ClassValueMap<Object> map = new ClassValueMap<>();
        Map<Object, Object> hop = new LinkedHashMap<>();
        map.put(String.class, hop);
        hop.put("back", map);
        assertEquals("{class java.lang.String={back=(cycle)}}", map.toString());

        ClassValueMap<Object> self = new ClassValueMap<>();
        self.put(String.class, self);
        assertEquals("{class java.lang.String=(this Map)}", self.toString());
    }

    // ------------------------------------------------------------------------------------ the collections

    private static final Comparator<Object> BY_IDENTITY =
            (a, b) -> Integer.compare(System.identityHashCode(a), System.identityHashCode(b));

    static Stream<Arguments> collections() {
        return Stream.of(
                collection("CaseInsensitiveSet", CaseInsensitiveSet::new, "(this Collection)"),
                collection("CompactSet", CompactSet::new, "(this Collection)"),
                collection("CompactCIHashSet", CompactCIHashSet::new, "(this Collection)"),
                collection("CompactCILinkedSet", CompactCILinkedSet::new, "(this Collection)"),
                collection("CompactLinkedSet", CompactLinkedSet::new, "(this Collection)"),
                collection("ConcurrentSet", ConcurrentSet::new, "(this Set)"),
                collection("ConcurrentList", ConcurrentList::new, "(this Collection)"),
                collection("IdentitySet", IdentitySet::new, "(this Collection)"),
                collection("ConcurrentNavigableSetNullSafe",
                        () -> new ConcurrentNavigableSetNullSafe<>(BY_IDENTITY), "(this Collection)"));
    }

    private static Arguments collection(String name, Supplier<Collection<Object>> factory, String selfMarker) {
        return Arguments.of(name, factory, selfMarker);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("collections")
    void collectionHoldingItselfPrintsItsSelfMarker(String name, Supplier<Collection<Object>> factory, String self) {
        Collection<Object> c = factory.get();
        c.add(c);   // hashed while still empty, so a hash-based set can store it
        assertEquals("[" + self + "]", c.toString());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("collections")
    void loopThroughAJdkMapTerminates(String name, Supplier<Collection<Object>> factory, String self) {
        Collection<Object> c = factory.get();
        Map<Object, Object> hop = new LinkedHashMap<>();
        c.add(hop);
        hop.put("back", c);
        assertEquals("[{back=(cycle)}]", c.toString());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("collections")
    void loopThroughAJdkListTerminates(String name, Supplier<Collection<Object>> factory, String self) {
        Collection<Object> c = factory.get();
        List<Object> hop = new ArrayList<>();
        c.add(hop);
        hop.add(c);
        assertEquals("[[(cycle)]]", c.toString());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("collections")
    void loopThroughAnotherOfTheSameClassTerminates(String name, Supplier<Collection<Object>> factory,
                                                    String self) {
        Collection<Object> c = factory.get();
        Collection<Object> other = factory.get();
        c.add(other);
        other.add(c);
        assertEquals("[[(cycle)]]", c.toString());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("collections")
    void acyclicOutputMatchesTheJdkRenderingOfItsOwnOrder(String name, Supplier<Collection<Object>> factory,
                                                         String self) {
        Collection<Object> c = factory.get();
        c.add("a");
        c.add(Arrays.asList(1, 2));
        c.add(new TreeMap<>(java.util.Collections.singletonMap("x", "y")));
        c.add(new int[] {1, 2});
        assertEquals(new ArrayList<>(c).toString(), c.toString());
    }

    // ------------------------------------------------------------------------------------------ MultiKeyMap

    @Test
    void multiKeyMapKeepsItsFormatAndTerminatesOnEveryLoop() {
        MultiKeyMap<Object> self = new MultiKeyMap<>();
        self.put("k", self);
        assertTrue(self.toString().contains("(this Map ♻️)"), self.toString());

        MultiKeyMap<Object> viaMap = new MultiKeyMap<>();
        Map<Object, Object> hop = new LinkedHashMap<>();
        viaMap.put("k", hop);
        hop.put("back", viaMap);
        assertTrue(viaMap.toString().contains("{back=(cycle)}"), viaMap.toString());

        MultiKeyMap<Object> a = new MultiKeyMap<>();
        MultiKeyMap<Object> b = new MultiKeyMap<>();
        a.put("b", b);
        b.put("a", a);
        assertTrue(a.toString().contains("(cycle)"), a.toString());
    }

    @Test
    void multiKeyMapValueThatContainsItselfTerminates() {
        // Its value formatter walks collections and arrays itself, with no memory of where it had been.
        List<Object> list = new ArrayList<>();
        list.add("x");
        list.add(list);
        Object[] array = new Object[2];
        array[0] = "y";
        array[1] = array;
        MultiKeyMap<Object> map = new MultiKeyMap<>();
        map.put("list", list);
        map.put("array", array);

        String s = map.toString();
        assertTrue(s.contains("[x, (cycle)]"), s);
        assertTrue(s.contains("[y, (cycle)]"), s);
    }

    @Test
    void multiKeyMapEntrySetDoesNotHashItsValues() {
        MultiKeyMap<Object> map = new MultiKeyMap<>();
        Map<Object, Object> hop = new LinkedHashMap<>();
        map.put("k", hop);
        hop.put("back", map);
        assertEquals(1, map.entrySet().size());
        assertTrue(map.entrySet().iterator().next().getValue() == hop);
    }

    @Test
    void multiKeyMapAcceptsACollectionKeyThatContainsItself() {
        // Arrays were already handled; collections hashed the nested element before checking what it was.
        List<Object> key = new ArrayList<>();
        key.add("x");
        key.add(key);
        MultiKeyMap<String> map = new MultiKeyMap<>();
        map.put(key, "found");
        assertEquals("found", map.get(key));
        assertTrue(map.containsKey(key));
    }

    // ------------------------------------------------------------------------------------- GraphComparator

    @Test
    void deltaToStringTerminatesOnACyclicValue() {
        Map<String, Object> cyclic = new LinkedHashMap<>();
        List<Object> list = new ArrayList<>();
        cyclic.put("list", list);
        list.add(cyclic);
        GraphComparator.Delta delta = new GraphComparator.Delta(1L, "field", "ptr", cyclic, "target", null);
        assertTrue(delta.toString().contains("srcValue={list=[(cycle)]}"), delta.toString());
    }

    static class Doc {
        long id;
        String name;

        Doc(long id, String name) {
            this.id = id;
            this.name = name;
        }
    }

    @Test
    void applyDeltaReportsACyclicCauseChainOnceAndFinishes() {
        // The error text walks the cause chain. A chain that loops (outer caused by inner caused by outer)
        // used to append until the heap ran out.
        RuntimeException outer = new RuntimeException("outer");
        RuntimeException inner = new RuntimeException("inner", outer);
        outer.initCause(inner);
        GraphComparator.DeltaProcessor thrower = (GraphComparator.DeltaProcessor) Proxy.newProxyInstance(
                getClass().getClassLoader(), new Class<?>[] {GraphComparator.DeltaProcessor.class},
                (proxy, method, args) -> {
                    throw outer;
                });
        GraphComparator.ID ids = o -> o instanceof Doc ? ((Doc) o).id : null;
        Doc source = new Doc(1, "before");
        List<GraphComparator.Delta> deltas = GraphComparator.compare(source, new Doc(1, "after"), ids);
        assertTrue(!deltas.isEmpty(), "the two docs differ, so there is a delta to apply");

        List<GraphComparator.DeltaError> errors = assertTimeoutPreemptively(Duration.ofSeconds(10),
                () -> GraphComparator.applyDelta(source, deltas, ids, thrower));
        assertEquals(deltas.size(), errors.size());
        assertEquals("outer, caused by: inner", errors.get(0).getError());
    }

    private static int count(Iterable<?> items) {
        int n = 0;
        for (Iterator<?> i = items.iterator(); i.hasNext(); i.next()) {
            n++;
        }
        return n;
    }
}
