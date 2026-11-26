package com.cedarsoftware.util;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.Stack;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The 4.119.0 once-over of CollectionUtilities and MapUtilities: what {@code deepCopyContainers} got wrong for sets
 * nested through a list or an array, for arrays of a concrete collection class and for this library's own sets;
 * {@code MapUtilities.getOrThrow} on a map other threads change; and the two sources {@code CompactMap.builder()}
 * refused though they were in the order asked for.
 */
class CollectionMapOnceOverTest {

    // ---------------------------------------------------------------------------------------------------------
    // deepCopyContainers: a set is filled when everything below it is complete, however far below
    // ---------------------------------------------------------------------------------------------------------

    /**
     * {@code Set{List[Set{x}]}}: the outer set was hashed while the inner one, inside the list, was still empty,
     * so the copy could not find its own element and was not equal to the source it was copied from.
     */
    @Test
    void aSetHoldingAListOfSetsIsHashedAfterTheInnerSetsAreFilled() {
        Set<Object> inner = new HashSet<>(Collections.singletonList("x"));
        List<Object> between = new ArrayList<>(Collections.singletonList(inner));
        Set<Object> source = new HashSet<>(Collections.singletonList(between));

        Set<Object> copy = CollectionUtilities.deepCopyContainers(source);
        assertNotSame(source, copy);
        assertEquals(source, copy);
        assertEquals(copy, source);
        assertTrue(copy.contains(between), "the copy cannot find an element equal to its own");
        Object copied = copy.iterator().next();
        assertNotSame(between, copied);
        assertTrue(copy.contains(copied));
        assertTrue(copy.remove(copied));
        assertTrue(copy.isEmpty());

        // through an array, and through a deque, the same
        Set<Object> viaArray = new HashSet<>();
        viaArray.add(new Object[]{new HashSet<>(Collections.singletonList("y"))});
        Set<Object> viaArrayCopy = CollectionUtilities.deepCopyContainers(viaArray);
        Object[] array = (Object[]) viaArrayCopy.iterator().next();
        assertEquals(new HashSet<>(Collections.singletonList("y")), array[0]);
    }

    /** A sorted set ordered by what its elements hold kept one of three: all three compared as empty. */
    @Test
    void aSortedSetOrderedByWhatItsElementsHoldKeepsThemAll() {
        Comparator<List<Set<String>>> byInnerSize = Comparator.comparingInt(list -> list.get(0).size());
        TreeSet<List<Set<String>>> source = new TreeSet<>(byInnerSize);
        source.add(new ArrayList<>(Collections.singletonList(new HashSet<>(Arrays.asList("a", "b", "c")))));
        source.add(new ArrayList<>(Collections.singletonList(new HashSet<>(Arrays.asList("a")))));
        source.add(new ArrayList<>(Collections.singletonList(new HashSet<>(Arrays.asList("a", "b")))));

        TreeSet<List<Set<String>>> copy = CollectionUtilities.deepCopyContainers(source);
        assertEquals(3, copy.size());
        assertEquals(source.toString(), copy.toString());
        assertNotSame(source.first(), copy.first());

        // a heap, the same
        PriorityQueue<Deque<Set<String>>> queue = new PriorityQueue<>(Comparator.comparingInt(d -> d.peek().size()));
        for (int size : new int[]{4, 1, 3, 5, 2}) {
            Set<String> set = new HashSet<>();
            for (int i = 0; i < size; i++) {
                set.add("e" + i);
            }
            queue.add(new ArrayDeque<>(Collections.singletonList(set)));
        }
        PriorityQueue<Deque<Set<String>>> queueCopy = CollectionUtilities.deepCopyContainers(queue);
        StringBuilder order = new StringBuilder();
        while (!queueCopy.isEmpty()) {
            order.append(queueCopy.poll().peek().size());
        }
        assertEquals("12345", order.toString());
    }

    /**
     * The fill went round its list of sets once for every level of nesting: 20,000 sorted sets one inside the
     * next took 1.4 seconds, 40,000 would take six. It is one walk now, a few milliseconds.
     */
    @Test
    @Timeout(20)
    void aLongChainOfSortedSetsIsCopiedInOneWalk() {
        Comparator<Object> byIdentity = Comparator.comparingInt(System::identityHashCode);
        final int depth = 40_000;
        TreeSet<Object> innermost = new TreeSet<>(byIdentity);
        innermost.add("leaf");
        TreeSet<Object> chain = innermost;
        for (int i = 0; i < depth; i++) {
            TreeSet<Object> outer = new TreeSet<>(byIdentity);
            outer.add(chain);
            chain = outer;
        }
        long start = System.nanoTime();
        TreeSet<Object> copy = CollectionUtilities.deepCopyContainers(chain);
        long millis = (System.nanoTime() - start) / 1_000_000;

        Object at = copy;
        int levels = 0;
        while (at instanceof TreeSet) {
            at = ((TreeSet<?>) at).first();
            levels++;
        }
        assertEquals(depth + 1, levels);
        assertEquals("leaf", at);
        assertTrue(millis < 2_000, depth + " levels took " + millis + " ms");
    }

    /** Sets that hold each other still copy, as two sets that hold each other. */
    @Test
    void setsThatHoldEachOtherStillCopy() {
        Set<Object> a = Collections.newSetFromMap(new java.util.IdentityHashMap<>());
        Set<Object> b = Collections.newSetFromMap(new java.util.IdentityHashMap<>());
        a.add(b);
        a.add("in a");
        b.add(new ArrayList<>(Collections.singletonList(a)));
        b.add("in b");
        IdentitySet<Object> root = new IdentitySet<>();
        root.add(a);
        IdentitySet<Object> copy = CollectionUtilities.deepCopyContainers(root);
        assertEquals(1, copy.size());
        Set<?> aCopy = (Set<?>) copy.iterator().next();
        assertNotSame(a, aCopy);
        assertEquals(2, aCopy.size());
    }

    // ---------------------------------------------------------------------------------------------------------
    // deepCopyContainers: classes kept where they have to be
    // ---------------------------------------------------------------------------------------------------------

    /** An array of a concrete collection class cannot hold the usual substitute: it was an ArrayStoreException. */
    @Test
    void anArrayOfAConcreteCollectionClassIsCopiedWithThatClass() {
        ArrayDeque<String>[] deques = new ArrayDeque[]{new ArrayDeque<>(Arrays.asList("a", "b"))};
        ArrayDeque<String>[] dequesCopy = CollectionUtilities.deepCopyContainers(deques);
        assertNotSame(deques[0], dequesCopy[0]);
        assertEquals("[a, b]", dequesCopy[0].toString());

        Vector<String> vector = new Vector<>(Arrays.asList("v"));
        Stack<String> stack = new Stack<>();
        stack.push("s");
        ConcurrentSkipListSet<String> skip = new ConcurrentSkipListSet<>(Comparator.<String>reverseOrder());
        skip.addAll(Arrays.asList("a", "b", "c"));
        Object[][] cases = {
                {new Vector[]{vector}, Vector.class},
                {new Stack[]{stack}, Stack.class},
                {new CopyOnWriteArrayList[]{new CopyOnWriteArrayList<>(Arrays.asList("c"))},
                        CopyOnWriteArrayList.class},
                {new ConcurrentLinkedDeque[]{new ConcurrentLinkedDeque<>(Arrays.asList("d"))},
                        ConcurrentLinkedDeque.class},
                {new ConcurrentSkipListSet[]{skip}, ConcurrentSkipListSet.class},
        };
        for (Object[] each : cases) {
            Object[] source = (Object[]) each[0];
            Object[] copy = CollectionUtilities.deepCopyContainers(source);
            assertSame(source.getClass(), copy.getClass());
            assertSame(each[1], copy[0].getClass());
            assertNotSame(source[0], copy[0]);
            assertEquals(source[0].toString(), copy[0].toString());
        }
        ConcurrentSkipListSet<String>[] skipCopy =
                CollectionUtilities.deepCopyContainers(new ConcurrentSkipListSet[]{skip});
        assertEquals("[c, b, a]", skipCopy[0].toString(), "the comparator came with it");

        // where nothing demands the class, the substitutes are what they were: declare the result by its interface
        Deque<String> source = new ArrayDeque<>(Arrays.asList("x"));
        Deque<String> deque = CollectionUtilities.deepCopyContainers(source);
        assertEquals(java.util.LinkedList.class, deque.getClass());
        Object[] objects = CollectionUtilities.deepCopyContainers(new Object[]{new ArrayDeque<>(Arrays.asList("x"))});
        assertEquals(java.util.LinkedList.class, objects[0].getClass());

        // and a class with no constructor to call says so, rather than ArrayStoreException
        java.util.concurrent.ArrayBlockingQueue<String>[] bounded =
                new java.util.concurrent.ArrayBlockingQueue[]{new java.util.concurrent.ArrayBlockingQueue<String>(2)};
        IllegalArgumentException refused = assertThrows(IllegalArgumentException.class,
                () -> CollectionUtilities.deepCopyContainers(bounded));
        assertTrue(refused.getMessage().contains("ArrayBlockingQueue"), refused.getMessage());
    }

    /** This library's sets keep what makes them what they are: identity, case-insensitivity, a null element. */
    @Test
    void thisLibrarysSetsAreCopiedAsThemselves() {
        String one = new String("k");
        String other = new String("k");
        IdentitySet<Object> identity = new IdentitySet<>();
        identity.add(one);
        identity.add(other);
        identity.add(new ArrayList<>(Collections.singletonList("x")));
        identity.add(new ArrayList<>(Collections.singletonList("x")));
        IdentitySet<Object> identityCopy = CollectionUtilities.deepCopyContainers(identity);
        assertEquals(4, identityCopy.size(), "equal but distinct members became one");
        assertTrue(identityCopy.contains(one) && identityCopy.contains(other));

        CaseInsensitiveSet<String> names = new CaseInsensitiveSet<>(Arrays.asList("Alpha", "beta"));
        Set<String> namesCopy = CollectionUtilities.deepCopyContainers(names);
        assertNotSame(names, namesCopy);
        assertTrue(namesCopy.contains("ALPHA") && namesCopy.contains("Beta"));
        assertEquals("[Alpha, beta]", namesCopy.toString());

        ConcurrentNavigableSetNullSafe<String> withNull = new ConcurrentNavigableSetNullSafe<>();
        withNull.add("a");
        withNull.add(null);
        Set<String> withNullCopy = CollectionUtilities.deepCopyContainers(withNull);
        assertNotSame(withNull, withNullCopy);
        assertEquals(withNull.toString(), withNullCopy.toString());
        assertTrue(withNullCopy.contains(null));
    }

    // ---------------------------------------------------------------------------------------------------------
    // MapUtilities
    // ---------------------------------------------------------------------------------------------------------

    /**
     * getOrThrow asked containsKey() and then get(): with another thread removing the key between the two, it
     * returned null for an absent key, from a ConcurrentHashMap, which holds no nulls.
     */
    @Test
    void getOrThrowNeverReturnsNullFromAMapThatHoldsNone() throws Exception {
        final Map<String, String> map = new ConcurrentHashMap<>();
        final AtomicBoolean stop = new AtomicBoolean();
        final IllegalStateException absent = new IllegalStateException("absent");
        Thread writer = new Thread(() -> {
            while (!stop.get()) {
                map.put("k", "v");
                map.remove("k");
            }
        });
        writer.start();
        AtomicLong nulls = new AtomicLong();
        long values = 0;
        long thrown = 0;
        long until = System.nanoTime() + 400_000_000L;
        try {
            while (System.nanoTime() < until) {
                for (int i = 0; i < 2_000; i++) {
                    try {
                        Object value = MapUtilities.getOrThrow(map, "k", absent);
                        if (value == null) {
                            nulls.incrementAndGet();
                        } else {
                            values++;
                        }
                    } catch (IllegalStateException e) {
                        thrown++;
                    }
                }
            }
        } finally {
            stop.set(true);
            writer.join();
        }
        assertEquals(0, nulls.get(),
                "null for an absent key, " + values + " values and " + thrown + " thrown beside it");

        // and what it always did: a null that is there is returned, an absent key throws
        Map<String, String> plain = new LinkedHashMap<>();
        plain.put("n", null);
        assertEquals(null, MapUtilities.getOrThrow(plain, "n", absent));
        assertSame(absent,
                assertThrows(IllegalStateException.class, () -> MapUtilities.getOrThrow(plain, "x", absent)));
        Map<String, Object> ci = new CaseInsensitiveMap<>(Collections.singletonMap("Key", 1));
        assertEquals(1, MapUtilities.getOrThrow(ci, "KEY", absent));
    }

    private enum Day { MON, TUE, WED }

    /**
     * CompactMap.builder() refuses a source map whose order is not the one asked for. It refused a reverse-ordered
     * TreeMap for reverseOrder() (and took it for sortedOrder()), and an EnumMap, which iterates in its enum's
     * order, for sortedOrder(). Both are taken; nothing that was taken is refused.
     */
    @Test
    void aSourceMapInTheOrderAskedForIsNotRefused() {
        Map<String, Integer> descending = new TreeMap<>(Comparator.<String>reverseOrder());
        descending.put("a", 1);
        descending.put("b", 2);
        descending.put("c", 3);
        assertEquals("[c, b, a]", CompactMap.<String, Integer>builder().reverseOrder().sourceMap(descending).build()
                .keySet().toString());
        assertEquals("[a, b, c]", CompactMap.<String, Integer>builder().sortedOrder().sourceMap(descending).build()
                .keySet().toString(), "taken before, taken now");
        Map<String, Integer> descendingToo = new TreeMap<>(Comparator.<String>naturalOrder().reversed());
        descendingToo.putAll(descending);
        assertEquals("[c, b, a]", CompactMap.<String, Integer>builder().reverseOrder().sourceMap(descendingToo).build()
                .keySet().toString());

        Map<Day, Integer> days = new EnumMap<>(Day.class);
        days.put(Day.WED, 3);
        days.put(Day.MON, 1);
        days.put(Day.TUE, 2);
        assertEquals("[MON, TUE, WED]", CompactMap.<Day, Integer>builder().sortedOrder().sourceMap(days).build()
                .keySet().toString());
        assertEquals("[MON, TUE, WED]", CompactMap.<Day, Integer>builder().insertionOrder().sourceMap(days).build()
                .keySet().toString(), "taken before, taken now");

        // what conflicts still conflicts
        Map<String, Integer> ascending = new TreeMap<>(descending);
        assertThrows(IllegalArgumentException.class,
                () -> CompactMap.<String, Integer>builder().reverseOrder().sourceMap(ascending).build());
        assertThrows(IllegalArgumentException.class,
                () -> CompactMap.<String, Integer>builder().sortedOrder()
                        .sourceMap(new LinkedHashMap<>(descending)).build());
        assertFalse(MapUtilities.isAlsoOrdered(ascending, CompactMap.REVERSE));
        assertEquals(CompactMap.SORTED, MapUtilities.detectMapOrdering(descending), "the name it had");
    }
}
