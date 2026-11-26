package com.cedarsoftware.util;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * How deeply containers nest does not decide whether {@code toString()} returns.
 * <p>
 * From 4.112.0 through 4.118.0 every level of this library's maps cost twelve stack frames in {@code toString()}
 * (the map's own toString, {@code mapToString}, and the cycle-safe walk with two lambdas in it) where the
 * inherited {@code AbstractMap.toString()} of 4.111.0 cost three: a chain of 500 CaseInsensitiveMaps, nothing
 * cyclic in it, overflowed a 512 KB stack that a HashMap chain of the same depth, and 4.111.0, printed on. The walk
 * now writes this library's plain-format containers in place, as it always wrote the JDK's, by recursion for the
 * first 64 levels and in a loop below them.
 * <p>
 * The JDK's own {@code toString()} on a structure of plain JDK containers is the oracle for everything acyclic.
 */
class CycleSafeToStringDepthTest {
    private static final int LIMIT = CycleSafeToString.RECURSION_LIMIT;

    /** Runs on a thread with a small stack; rethrows what it threw. */
    private static <T> T onASmallStack(java.util.concurrent.Callable<T> work) throws Exception {
        AtomicReference<T> result = new AtomicReference<>();
        AtomicReference<Throwable> failure = new AtomicReference<>();
        Thread thread = new Thread(null, () -> {
            try {
                result.set(work.call());
            } catch (Throwable e) {
                failure.set(e);
            }
        }, "small-stack", 256 * 1024);
        thread.start();
        thread.join();
        if (failure.get() != null) {
            throw new AssertionError("on a 256 KB stack: " + failure.get(), failure.get());
        }
        return result.get();
    }

    private static String repeat(String text, int times) {
        StringBuilder sb = new StringBuilder(text.length() * times);
        for (int i = 0; i < times; i++) {
            sb.append(text);
        }
        return sb.toString();
    }

    @Test
    void aChainOfThisLibrarysMapsPrintsAtAnyDepth() throws Exception {
        for (int depth : new int[]{1, LIMIT - 1, LIMIT, LIMIT + 1, 500, 100_000}) {
            Map<String, Object> innermost = new CaseInsensitiveMap<>();
            Map<String, Object> chain = innermost;
            for (int i = 0; i < depth; i++) {
                Map<String, Object> outer = (i & 1) == 0 ? new CaseInsensitiveMap<>()
                        : CompactMap.<String, Object>builder().build();
                outer.put("k", chain);
                chain = outer;
            }
            final Map<String, Object> top = chain;
            String printed = onASmallStack(top::toString);
            assertEquals(repeat("{k=", depth) + "{}" + repeat("}", depth), printed, "depth " + depth);
        }
    }

    @Test
    void aChainOfJdkContainersInsideOneOfOursPrintsAtAnyDepth() throws Exception {
        final int depth = 100_000;
        Object chain = new ArrayList<>(Arrays.asList("leaf", 7));
        for (int i = 0; i < depth; i++) {
            if ((i & 1) == 0) {
                Map<Object, Object> map = new LinkedHashMap<>();
                map.put("a", 1);
                map.put("in", chain);
                map.put("z", null);
                chain = map;
            } else {
                chain = new ArrayList<>(Arrays.asList("x", chain, "y"));
            }
        }
        Map<String, Object> top = new CaseInsensitiveMap<>();
        top.put("top", chain);
        String printed = onASmallStack(top::toString);
        StringBuilder expected = new StringBuilder("{top=");
        for (int i = depth - 1; i >= 0; i--) {
            expected.append((i & 1) == 0 ? "{a=1, in=" : "[x, ");
        }
        expected.append("[leaf, 7]");
        for (int i = 0; i < depth; i++) {
            expected.append((i & 1) == 0 ? ", z=null}" : ", y]");
        }
        expected.append('}');
        assertEquals(expected.toString(), printed);
        // the same through the entry point for any map, and for a diagnostic value
        final Object nest = top.get("top");
        assertEquals(printed, onASmallStack(() -> MapUtilities.mapToString(Collections.singletonMap("top", nest))));
        assertEquals(printed.substring(5, printed.length() - 1), onASmallStack(() -> CycleSafeToString.value(nest)));
    }

    /**
     * Random nests of this library's containers and the JDK's print exactly as the same nest built from
     * LinkedHashMap, ArrayList and LinkedHashSet does with the JDK's own toString(): to twice the depth at which
     * the walk changes from recursion to its loop, with containers as map keys, nulls, empties and diamonds.
     */
    @Test
    void whatIsNotCyclicPrintsAsTheJdkPrintsIt() throws Exception {
        Random random = new Random(20261125);
        for (int round = 0; round < 300; round++) {
            Object[] pair = nest(random, random.nextInt(2 * LIMIT + 10), new ArrayList<>());
            Map<String, Object> ours = new CaseInsensitiveMap<>();
            Map<String, Object> jdk = new LinkedHashMap<>();
            ours.put("root", pair[0]);
            jdk.put("root", pair[1]);
            final String expected = jdk.toString();
            assertEquals(expected, ours.toString(), "round " + round);
            assertEquals(expected, MapUtilities.mapToString(jdk), "round " + round);
            assertEquals(expected.substring(6, expected.length() - 1), CycleSafeToString.value(pair[0]),
                    "round " + round);
        }
    }

    /** {ours, the JDK's}: the same shape twice. {@code shared} holds finished pairs, to make diamonds of. */
    private static Object[] nest(Random random, int depth, List<Object[]> shared) {
        if (depth == 0) {
            Object leaf = random.nextInt(4) == 0 ? null
                    : random.nextBoolean() ? "s" + random.nextInt(50) : (Object) random.nextInt(50);
            return new Object[]{leaf, leaf};
        }
        if (!shared.isEmpty() && random.nextInt(12) == 0) {
            return shared.get(random.nextInt(shared.size()));
        }
        int width = random.nextInt(8) == 0 ? 0 : 1 + random.nextInt(3);
        int deepAt = width == 0 ? -1 : random.nextInt(width);
        Object[] pair;
        switch (random.nextInt(7)) {
            case 0: case 1: {
                Map<Object, Object> ours = random.nextBoolean() ? new CaseInsensitiveMap<>()
                        : CompactMap.builder().insertionOrder().build();
                Map<Object, Object> jdk = new LinkedHashMap<>();
                for (int i = 0; i < width; i++) {
                    Object[] value = nest(random, i == deepAt ? depth - 1 : 0, shared);
                    ours.put("k" + i, value[0]);
                    jdk.put("k" + i, value[1]);
                }
                pair = new Object[]{ours, jdk};
                break;
            }
            case 2: {
                Map<Object, Object> ours = new LinkedHashMap<>();
                Map<Object, Object> jdk = new LinkedHashMap<>();
                for (int i = 0; i < width; i++) {
                    Object[] value = nest(random, i == deepAt ? depth - 1 : 0, shared);
                    if (random.nextInt(5) == 0) {           // a container as the key
                        ours.put(value[0] == null ? "n" + i : value[0] instanceof Collection || value[0] instanceof Map
                                ? new IdentityKey(value[0]) : value[0], i);
                        jdk.put(value[1] == null ? "n" + i : value[1] instanceof Collection || value[1] instanceof Map
                                ? new IdentityKey(value[1]) : value[1], i);
                    } else {
                        ours.put("k" + i, value[0]);
                        jdk.put("k" + i, value[1]);
                    }
                }
                pair = new Object[]{ours, jdk};
                break;
            }
            case 3: {
                Collection<Object> oursSet = new CaseInsensitiveSet<>();
                Collection<Object> jdkSet = new LinkedHashSet<>();
                for (int i = 0; i < width; i++) {
                    oursSet.add("e" + i);
                    jdkSet.add("e" + i);
                }
                Object[] inner = nest(random, depth - 1, shared);
                pair = new Object[]{new ArrayList<>(Arrays.asList(oursSet, inner[0])),
                        new ArrayList<>(Arrays.asList(jdkSet, inner[1]))};
                break;
            }
            default:
                pair = nestList(random, depth, shared);
        }
        shared.add(pair);
        return pair;
    }

    private static Object[] nestList(Random random, int depth, List<Object[]> shared) {
        int width = 1 + random.nextInt(3);
        int deepAt = random.nextInt(width);
        List<Object> ours = random.nextBoolean() ? new ArrayList<>()
                : new java.util.concurrent.CopyOnWriteArrayList<>();
        List<Object> jdk = new ArrayList<>();
        for (int i = 0; i < width; i++) {
            Object[] value = nest(random, i == deepAt ? depth - 1 : 0, shared);
            ours.add(value[0]);
            jdk.add(value[1]);
        }
        return new Object[]{ours, jdk};
    }

    /** A map key that holds a container without hashing it (hashing a deep nest is its own recursion). */
    private static final class IdentityKey {
        private final Object held;

        IdentityKey(Object held) {
            this.held = held;
        }

        @Override
        public String toString() {
            return "key" + CycleSafeToString.value(held);      // back into the walk, on the same path
        }
    }

    /** A loop that closes below the depth where the walk changes over is still seen, and still printed once. */
    @Test
    void aLoopThatClosesDeepDownIsSeen() throws Exception {
        for (int depth : new int[]{3, LIMIT - 1, LIMIT, LIMIT + 5, 5_000}) {
            Map<String, Object> top = new CaseInsensitiveMap<>();
            Map<String, Object> at = top;
            for (int i = 0; i < depth; i++) {
                Map<String, Object> inner = (i & 1) == 0 ? new LinkedHashMap<>() : new CaseInsensitiveMap<>();
                at.put("k", inner);
                at = inner;
            }
            at.put("self", at);             // itself: the JDK's marker
            at.put("up", top);              // the outermost: a loop
            List<Object> list = new ArrayList<>();
            list.add(list);
            list.add(top);
            at.put("list", list);
            String printed = onASmallStack(top::toString);
            assertEquals(repeat("{k=", depth) + "{self=(this Map), up=(cycle), list=[(this Collection), (cycle)]}"
                    + repeat("}", depth), printed, "depth " + depth);
        }
    }

    /** A member that throws from far down leaves nothing behind: the next toString() on this thread is whole. */
    @Test
    void aMemberThatThrowsDeepDownLeavesNothingOnTheThread() throws Exception {
        final Object bomb = new Object() {
            @Override
            public String toString() {
                throw new IllegalStateException("no text");
            }
        };
        for (int depth : new int[]{2, LIMIT + 20}) {
            Map<String, Object> top = new CaseInsensitiveMap<>();
            Map<String, Object> at = top;
            for (int i = 0; i < depth; i++) {
                Map<String, Object> inner = new CaseInsensitiveMap<>();
                at.put("k", inner);
                at = inner;
            }
            at.put("ok", 1);
            at.put("bomb", bomb);
            assertThrows(IllegalStateException.class, top::toString);

            Field field = CycleSafeToString.class.getDeclaredField("PATH");
            field.setAccessible(true);
            assertNull(((ThreadLocal<?>) field.get(null)).get(), "the path is still on the thread");
            at.remove("bomb");
            assertEquals(repeat("{k=", depth) + "{ok=1}" + repeat("}", depth), top.toString());
        }
    }

    /** One of ours with a toString() of its own keeps it: only the plain format is written in place. */
    @Test
    void aSubclassWithItsOwnFormatKeepsIt() {
        Map<String, Object> custom = new CaseInsensitiveMap<String, Object>() {
            @Override
            public String toString() {
                return "<" + size() + " entries>";
            }
        };
        custom.put("a", 1);
        Map<String, Object> top = new CaseInsensitiveMap<>();
        top.put("plain", new CaseInsensitiveMap<>(Collections.singletonMap("x", 1)));
        top.put("custom", custom);
        top.put("set", new CaseInsensitiveSet<>(Arrays.asList("p", "q")));
        top.put("concurrent", new ConcurrentHashMapNullSafe<>(Collections.singletonMap("c", null)));
        assertEquals("{plain={x=1}, custom=<1 entries>, set=[p, q], concurrent={c=null}}", top.toString());
        assertEquals(new HashMap<>(Collections.singletonMap("m", top)).toString(),
                MapUtilities.mapToString(Collections.singletonMap("m", top)));
    }
}
