package com.cedarsoftware.util;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link MapUtilities#mapToString(Map)} must terminate on ANY cycle, not only on direct self-containment.
 * <p>
 * The JDK's guard, which mapToString copied, prints {@code (this Map)} only when a value IS the map being
 * printed. A two-step cycle -- a map holding a map (or a list) that holds the first map -- walks straight
 * past it: each container hands the next to its own {@code toString()}, none of them remembers where the
 * walk began, and the thread dies with {@link StackOverflowError}. These graphs are rarely built on
 * purpose; they arise from a mutable map reused as its own context, a parent/child pair, or a
 * {@code Map.put()} whose return value is stored back. n-cube hit exactly this in its execution trace
 * (6.73.0, {@code TraceValue}).
 * <p>
 * Three java-util maps delegate their {@code toString()} here -- {@link CompactMap},
 * {@link AbstractConcurrentNullSafeMap} ({@link ConcurrentHashMapNullSafe},
 * {@link ConcurrentNavigableMapNullSafe}) and the THREADED {@link LRUCache} strategy -- so each of them
 * overflowed on the same input.
 * <p>
 * The rule these tests pin: a container that is already being rendered, further up the current path,
 * renders as {@code (cycle)}; direct self-containment keeps the JDK's {@code (this Map)} /
 * {@code (this Collection)}; and anything that is NOT a cycle renders exactly as it always did.
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
class MapUtilitiesCycleTest {

    // ---------------------------------------------------------------- cycles that used to overflow

    @Test
    void cycleThroughASecondMapTerminates() {
        Map<String, Object> outer = new LinkedHashMap<>();
        Map<String, Object> inner = new LinkedHashMap<>();
        outer.put("inner", inner);
        inner.put("outer", outer);

        assertEquals("{inner={outer=(cycle)}}", MapUtilities.mapToString(outer));
    }

    @Test
    void cycleThroughAListTerminates() {
        Map<String, Object> map = new LinkedHashMap<>();
        List<Object> list = new ArrayList<>();
        map.put("list", list);
        list.add(map);

        assertEquals("{list=[(cycle)]}", MapUtilities.mapToString(map));
    }

    @Test
    void longCycleTerminates() {
        // a -> b -> c -> a: the loop closes three hops from where it began
        Map<String, Object> a = new LinkedHashMap<>();
        Map<String, Object> b = new LinkedHashMap<>();
        Map<String, Object> c = new LinkedHashMap<>();
        a.put("b", b);
        b.put("c", c);
        c.put("a", a);

        assertEquals("{b={c={a=(cycle)}}}", MapUtilities.mapToString(a));
    }

    @Test
    void cycleThroughAKeyTerminates() {
        // IdentityHashMap so the cyclic key can be stored at all (a HashMap would overflow hashing it)
        Map<Object, Object> map = new IdentityHashMap<>();
        List<Object> key = new ArrayList<>();
        key.add(map);
        map.put(key, "v");

        assertEquals("{[(cycle)]=v}", MapUtilities.mapToString(map));
    }

    @Test
    void cycleThroughAConcurrentHashMapTerminates() {
        // ConcurrentHashMap overrides toString() with the same format; it is walked, not delegated to
        Map<String, Object> map = new LinkedHashMap<>();
        Map<String, Object> chm = new ConcurrentHashMap<>();
        map.put("chm", chm);
        chm.put("back", map);

        assertEquals("{chm={back=(cycle)}}", MapUtilities.mapToString(map));
    }

    @Test
    void cycleThroughACopyOnWriteArrayListTerminates() {
        // CopyOnWriteArrayList.toString() is Arrays.toString(), which has no self-guard at all
        Map<String, Object> map = new LinkedHashMap<>();
        List<Object> cow = new CopyOnWriteArrayList<>();
        map.put("cow", cow);
        cow.add(map);

        assertEquals("{cow=[(cycle)]}", MapUtilities.mapToString(map));
    }

    // ---------------------------------------------------- the java-util maps that delegate here

    @Test
    void compactMapToStringTerminatesOnACycle() {
        assertCycleThroughHopTerminates(new CompactMap<>());
    }

    @Test
    void concurrentHashMapNullSafeToStringTerminatesOnACycle() {
        assertCycleThroughHopTerminates(new ConcurrentHashMapNullSafe<>());
    }

    @Test
    void concurrentNavigableMapNullSafeToStringTerminatesOnACycle() {
        assertCycleThroughHopTerminates(new ConcurrentNavigableMapNullSafe<>());
    }

    @Test
    void threadedLruCacheToStringTerminatesOnACycle() {
        assertCycleThroughHopTerminates(new LRUCache<>(10, LRUCache.StrategyType.THREADED));
    }

    /** The java-util map holds a plain map that holds the java-util map: the loop closes through toString(). */
    private static void assertCycleThroughHopTerminates(Map<String, Object> javaUtilMap) {
        Map<String, Object> hop = new LinkedHashMap<>();
        javaUtilMap.put("hop", hop);
        hop.put("back", javaUtilMap);

        assertEquals("{hop={back=(cycle)}}", javaUtilMap.toString());
    }

    @Test
    void cycleClosingThroughADelegatedContainerIsStillSeen() {
        // A container with its OWN toString() format is handed to that toString(), not re-rendered, so its
        // format survives. The walk must still see the loop close when that toString() re-enters mapToString.
        Map<String, Object> compact = new CompactMap<>();
        List<Object> bracketed = new BracketedList();
        compact.put("list", bracketed);
        bracketed.add(compact);

        assertEquals("{list=<[(cycle)]>}", compact.toString());
    }

    // ------------------------------------------------------------- what must NOT change

    @Test
    void directSelfContainmentKeepsTheJdkMarker() {
        Map<String, Object> self = new LinkedHashMap<>();
        self.put("self", self);
        assertEquals("{self=(this Map)}", MapUtilities.mapToString(self));

        Map<String, Object> holder = new LinkedHashMap<>();
        List<Object> selfList = new ArrayList<>();
        selfList.add(selfList);
        holder.put("list", selfList);
        assertEquals("{list=[(this Collection)]}", MapUtilities.mapToString(holder));
    }

    @Test
    void aSharedNodeIsADiamondNotACycle() {
        // The same map reached twice by different routes is not a loop, and must render in full both times.
        Map<String, Object> shared = new LinkedHashMap<>();
        shared.put("x", 1);
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("a", shared);
        root.put("b", shared);
        root.put("list", Arrays.asList(shared, shared));

        assertEquals("{a={x=1}, b={x=1}, list=[{x=1}, {x=1}]}", MapUtilities.mapToString(root));
    }

    @Test
    void acyclicOutputIsIdenticalToTheJdk() {
        // Anything that rendered before renders byte-for-byte the same: same separators, same null
        // handling, arrays still printed as the JDK prints them (never expanded), custom objects via
        // their own toString().
        Map<String, Object> nested = new TreeMap<>();
        nested.put("m", 1);
        nested.put("n", null);

        Map<Object, Object> map = new LinkedHashMap<>();
        map.put("string", "text");
        map.put("number", 42);
        map.put("null", null);
        map.put(null, "nullKey");
        map.put("nested", nested);
        map.put("list", new ArrayList<>(Arrays.asList(1, "two", null, new ArrayList<>())));
        map.put("set", new LinkedHashSet<>(Arrays.asList("a", "b")));
        map.put("emptyMap", new HashMap<>());
        map.put("emptyList", Collections.emptyList());
        map.put("unmodifiable", Collections.unmodifiableMap(nested));
        map.put("array", new int[] {1, 2, 3});
        map.put("custom", new Labelled("L"));
        map.put("chm", new ConcurrentHashMap<>(Collections.singletonMap("k", "v")));

        assertEquals(map.toString(), MapUtilities.mapToString(map));
    }

    @Test
    void theWalkedJdkOverridesRenderExactlyAsTheirOwnToString() {
        // ConcurrentHashMap and CopyOnWriteArrayList/Set are walked rather than delegated to, which is only
        // correct if their iteration order and format match their own toString() -- pinned on enough entries
        // to span several hash buckets.
        Map<String, Object> chm = new ConcurrentHashMap<>();
        List<Object> cowList = new CopyOnWriteArrayList<>();
        for (int i = 0; i < 64; i++) {
            chm.put("key" + i, i);
            cowList.add("item" + i);
        }
        Map<String, Object> holder = new LinkedHashMap<>();
        holder.put("chm", chm);
        holder.put("cowList", cowList);
        holder.put("cowSet", new java.util.concurrent.CopyOnWriteArraySet<>(cowList));

        assertEquals(holder.toString(), MapUtilities.mapToString(holder));
    }

    @Test
    void aContainerWithItsOwnToStringKeepsItsFormat() {
        Map<String, Object> map = new LinkedHashMap<>();
        List<Object> bracketed = new BracketedList();
        bracketed.add("x");
        map.put("list", bracketed);

        assertEquals("{list=<[x]>}", MapUtilities.mapToString(map));
    }

    @Test
    void aFailedRenderLeavesNothingBehindOnTheThread() {
        // A leaf that throws aborts the render. The next render on the same thread must not inherit the
        // aborted walk's path and misreport an ordinary map as a cycle.
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("bad", new Object() {
            @Override
            public String toString() {
                throw new IllegalStateException("boom");
            }
        });
        Map<String, Object> compact = new CompactMap<>();
        compact.put("map", map);
        assertThrows(IllegalStateException.class, compact::toString);

        map.remove("bad");
        map.put("ok", 1);
        assertEquals("{map={ok=1}}", compact.toString());
    }

    // ------------------------------------------------------------------------- the loop budget

    @Test
    void aDenselyCrossLinkedGraphIsBounded() {
        // A clique: twelve maps, each holding all the others. Every route that does not repeat a map is a distinct
        // path, and there are about 11! of them -- ten maps already rendered ~100 million characters, and eleven
        // exhausted the heap. Once the render has closed a loop and spent its budget, it stops expanding.
        assertTimeoutPreemptively(Duration.ofSeconds(10), () -> {
            String s = MapUtilities.mapToString(clique(12, LinkedHashMap::new));
            assertBoundedRender(s);
            String viaCompactMaps = clique(12, CompactMap::new).toString();
            assertBoundedRender(viaCompactMaps);
        });
    }

    @Test
    void aDenselyCrossLinkedGraphInsideAMultiKeyMapValueIsBounded() {
        // MultiKeyMap walks the collections in its values itself, on the same path, so it spends the same budget
        assertTimeoutPreemptively(Duration.ofSeconds(10), () -> {
            List<List<Object>> lists = new ArrayList<>();
            for (int i = 0; i < 12; i++) {
                lists.add(new ArrayList<>());
            }
            for (List<Object> list : lists) {
                for (List<Object> other : lists) {
                    if (other != list) {
                        list.add(other);
                    }
                }
            }
            MultiKeyMap<Object> mkm = new MultiKeyMap<>();
            mkm.put("k", lists.get(0));
            assertBoundedRender(mkm.toString());
        });
    }

    @Test
    void aGraphTheJdkCanPrintIsNeverCut() {
        // Far past the budget, but no loop has closed -- holding itself is not one, the JDK prints that -- so the
        // output is the JDK's, all of it.
        Map<String, Object> big = bigMapHoldingItself();
        assertEquals(big.toString(), MapUtilities.mapToString(big));
    }

    @Test
    void aBudgetSpentByOneRenderDoesNotCarryIntoTheNext() {
        assertBoundedRender(MapUtilities.mapToString(clique(12, LinkedHashMap::new)));
        Map<String, Object> big = bigMapHoldingItself();
        assertEquals(big.toString(), MapUtilities.mapToString(big));
    }

    private static Map<String, Object> bigMapHoldingItself() {
        Map<String, Object> big = new LinkedHashMap<>();
        for (int i = 0; i < 150_000; i++) {
            big.put("k" + i, Arrays.asList(i, "v"));
        }
        big.put("self", big);
        return big;
    }

    @Test
    void aSparseLoopRendersInFull() {
        // A tree whose nodes point back to their parent is cyclic, but each node is reached by one route only:
        // 3,000 nodes render in full, every one of them.
        Map<String, Object> root = node("n0", null);
        List<Map<String, Object>> all = new ArrayList<>(Collections.singletonList(root));
        for (int i = 1; i < 3000; i++) {
            Map<String, Object> parent = all.get((i - 1) / 3);
            Map<String, Object> child = node("n" + i, parent);
            ((List<Object>) parent.get("children")).add(child);
            all.add(child);
        }
        String s = MapUtilities.mapToString(root);
        assertFalse(s.contains("..."));
        assertEquals(3000, s.split("name=n", -1).length - 1);
    }

    private static void assertBoundedRender(String s) {
        assertTrue(s.startsWith("{") && s.endsWith("}"), "still a well-formed render");
        assertTrue(s.contains("(cycle)"));
        assertTrue(s.contains("..."), "the budget was reached");
        assertTrue(s.length() < 5_000_000, "bounded: " + s.length() + " chars");
    }

    private static Map<String, Object> clique(int n, java.util.function.Supplier<Map<String, Object>> factory) {
        List<Map<String, Object>> maps = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            maps.add(factory.get());
        }
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                if (i != j) {
                    maps.get(i).put("m" + j, maps.get(j));
                }
            }
        }
        return maps.get(0);
    }

    private static Map<String, Object> node(String name, Map<String, Object> parent) {
        Map<String, Object> node = new LinkedHashMap<>();
        node.put("name", name);
        node.put("parent", parent);
        node.put("children", new ArrayList<>());
        return node;
    }

    // ---------------------------------------------------------------------------- fixtures

    /** A list with its own toString() format, wrapping the standard one in angle brackets. */
    private static final class BracketedList extends ArrayList<Object> {
        @Override
        public String toString() {
            return "<" + super.toString() + ">";
        }
    }

    private static final class Labelled {
        private final String label;

        Labelled(String label) {
            this.label = label;
        }

        @Override
        public String toString() {
            return "Labelled(" + label + ")";
        }
    }
}
