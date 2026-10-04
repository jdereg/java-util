package com.cedarsoftware.util;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.function.Predicate;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The order Traverser visits a graph in, against a model of it: depth first, the last child pushed visited first;
 * every reachable object once, queued at most once; a skipped class neither visited nor walked through; a JDK
 * class visited as a leaf. Random graphs with shared nodes, cycles, collections, maps and arrays.
 */
class TraverserVisitOrderTest {

    static class Node {
        Object a;
        Object b;
        Object c;
        int k;
        String s;
    }

    static class Bag extends Node {
        Object[] array;
        List<Object> list;
        Map<Object, Object> map;
    }

    private static final Predicate<Field> FIELDS =
            field -> ReflectionUtils.DEFAULT_FIELD_FILTER.test(field) && !field.isSynthetic();

    @Test
    void visitsEachReachableObjectOnceInDepthFirstOrder() {
        List<Set<Class<?>>> skips = Arrays.asList(null, Collections.singleton(String.class),
                Collections.singleton(Bag.class), new HashSet<>(Arrays.asList(Integer.class, LinkedList.class)));
        for (long seed = 0; seed < 600; seed++) {
            Random random = new Random(seed);
            Object root = build(random, 1 + random.nextInt(60));
            Set<Class<?>> skip = skips.get(random.nextInt(skips.size()));
            List<Object> visited = new ArrayList<>();
            Traverser.traverse(root, visit -> visited.add(visit.getNode()), skip, random.nextBoolean());
            List<Object> expected = model(root, skip);
            assertEquals(expected.size(), visited.size(), "seed " + seed);
            for (int i = 0; i < expected.size(); i++) {
                assertSame(expected.get(i), visited.get(i), "seed " + seed + " visit " + i);
            }
        }
    }

    static class Holder {
        Object marker = "<inaccessible>";       // the very String Traverser reports for a field it cannot read
        Object next;
    }

    /**
     * A field that really holds the String Traverser reports for an unreadable field is still read, reported and
     * walked: the walk tells "not read" apart from that value.
     */
    @Test
    void aFieldHoldingTheInaccessibleMarkerIsReadAndWalked() {
        Holder holder = new Holder();
        holder.next = new Holder();
        for (boolean collectFields : new boolean[]{true, false}) {
            List<Object> visited = new ArrayList<>();
            List<Object> markers = new ArrayList<>();
            Traverser.traverse(holder, visit -> {
                visited.add(visit.getNode());
                for (Map.Entry<Field, Object> field : visit.getFields().entrySet()) {
                    if (field.getKey().getName().equals("marker")) {
                        markers.add(field.getValue());
                    }
                }
            }, null, collectFields);
            assertEquals(3, visited.size(), "both Holders and the shared marker String");
            assertTrue(visited.stream().anyMatch(node -> node == holder.marker), "the marker String is walked to");
            assertEquals(Arrays.asList("<inaccessible>", "<inaccessible>"), markers);
        }
    }

    private static List<Object> model(Object root, Set<Class<?>> skip) {
        List<Object> order = new ArrayList<>();
        Map<Object, Boolean> queued = new IdentityHashMap<>();
        Deque<Object> stack = new ArrayDeque<>();
        stack.add(root);
        queued.put(root, Boolean.TRUE);
        while (!stack.isEmpty()) {
            Object current = stack.pollFirst();
            if (skipped(current, skip)) {
                continue;
            }
            order.add(current);
            for (Object child : children(current)) {
                if (child != null && !queued.containsKey(child) && !skipped(child, skip)) {
                    stack.addFirst(child);
                    queued.put(child, Boolean.TRUE);
                }
            }
        }
        return order;
    }

    private static boolean skipped(Object o, Set<Class<?>> skip) {
        if (skip != null) {
            for (Class<?> c : skip) {
                if (c.isAssignableFrom(o.getClass())) {
                    return true;
                }
            }
        }
        return false;
    }

    private static List<Object> children(Object o) {
        List<Object> children = new ArrayList<>();
        Class<?> c = o.getClass();
        if (c.isArray()) {
            if (!c.getComponentType().isPrimitive()) {
                for (int i = 0; i < Array.getLength(o); i++) {
                    children.add(Array.get(o, i));
                }
            }
        } else if (o instanceof Collection) {
            children.addAll((Collection<?>) o);
        } else if (o instanceof Map) {
            for (Map.Entry<?, ?> entry : ((Map<?, ?>) o).entrySet()) {
                children.add(entry.getKey());
                children.add(entry.getValue());
            }
        } else if (c.getClassLoader() != null) {                      // a JDK class is a leaf
            for (Field field : ReflectionUtils.getAllDeclaredFields(c, FIELDS)) {
                if (!field.getType().isPrimitive()) {
                    try {
                        children.add(field.get(o));
                    } catch (IllegalAccessException e) {
                        throw new AssertionError(e);
                    }
                }
            }
        }
        return children;
    }

    private static Object build(Random random, int n) {
        List<Node> pool = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            pool.add(random.nextBoolean() ? new Node() : new Bag());
        }
        for (Node node : pool) {
            node.a = pick(random, pool);
            node.b = pick(random, pool);
            node.c = pick(random, pool);
            node.k = random.nextInt();
            node.s = "t" + random.nextInt(9);
            if (node instanceof Bag) {
                Bag bag = (Bag) node;
                bag.array = new Object[random.nextInt(4)];
                for (int i = 0; i < bag.array.length; i++) {
                    bag.array[i] = pick(random, pool);
                }
                bag.list = random.nextBoolean() ? new ArrayList<>() : new LinkedList<>();
                for (int i = random.nextInt(4); i > 0; i--) {
                    bag.list.add(pick(random, pool));
                }
                bag.map = random.nextBoolean() ? new LinkedHashMap<>() : new IdentityHashMap<>();
                for (int i = random.nextInt(4); i > 0; i--) {
                    Object key = pick(random, pool);
                    bag.map.put(key == null ? "key" + i : key, pick(random, pool));
                }
            }
        }
        return pool.get(0);
    }

    private static Object pick(Random random, List<Node> pool) {
        switch (random.nextInt(9)) {
            case 0:
            case 1:
            case 2:
            case 3:
                return pool.get(random.nextInt(pool.size()));             // shared nodes and cycles
            case 4:
                return null;
            case 5:
                return "s" + random.nextInt(50);
            case 6:
                return random.nextInt(300);
            case 7:
                return new int[]{1, 2};
            default:
                return new ArrayList<>(Arrays.asList(pool.get(random.nextInt(pool.size())), "x"));
        }
    }
}
