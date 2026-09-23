package com.cedarsoftware.util;

import java.util.AbstractCollection;
import java.util.AbstractMap;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * The one {@code toString()} walk every java-util container uses, so that none of them can die with
 * {@link StackOverflowError} on a cyclic graph.
 * <p>
 * The JDK's guard prints {@code (this Map)} / {@code (this Collection)} only when an element IS the container
 * being printed. A two-step cycle -- a map holding a list that holds the map -- walks straight past it: each
 * container hands the next to its own {@code toString()}, none of them remembers where the walk began, and the
 * stack runs out. A wrapper that delegates to its inner container is worse: the inner container's guard compares
 * against the inner container, never the wrapper, so even DIRECT self-containment overflows.
 * <p>
 * Here the containers being rendered are kept on a per-thread path, by identity, for the duration of the
 * outermost render. Every java-util container renders through this class, so the path is shared across all of
 * them: a loop is seen however many steps away it closes and whichever container it closes through, and the
 * container it closes at renders as {@code (cycle)}. Membership is by IDENTITY because {@code hashCode()} on a
 * cyclic map is itself unbounded recursion, and it is a PATH rather than a visited set -- entries are removed on
 * the way back out -- so a node reached twice by different routes is a diamond, not a loop, and renders in full
 * both times.
 * <p>
 * Nothing acyclic changes. Nested maps and collections whose {@code toString()} is the JDK's standard one are
 * walked here, which renders them exactly as their own {@code toString()} would and lets the walk see through
 * them; a container with its own format is handed to that {@code toString()} and keeps it; every other value is
 * rendered by its own {@code toString()}, arrays included (never expanded).
 * <p>
 * A render that has closed a loop is bounded. A path prints every route through a graph that does not repeat a
 * container -- exactly what the JDK prints for a DAG -- but in a densely cross-linked graph, such as a clique of maps
 * that each hold all the others, the number of such routes grows factorially: ten such maps render about 100 million
 * characters and eleven exhaust the heap, where the JDK would have overflowed the stack at once. So once a render has
 * printed {@code (cycle)}, which the JDK never prints, and has rendered {@link #LOOP_BUDGET} values, every container
 * it has not yet expanded prints as {@code ...}. Anything the JDK can print -- acyclic, or holding only itself -- never
 * meets the budget, and a loop that is not dense, such as a tree whose nodes point back to their parent, renders in
 * full up to that size.
 * <p>
 * One limit is inherent: a loop made entirely of containers rendered by their own {@code toString()} --
 * synchronized or unmodifiable wrappers, {@code Hashtable}, {@code Vector} -- never re-enters this class, so it
 * behaves exactly as calling {@code toString()} on those containers directly would.
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
final class CycleSafeToString {
    /** Rendered in place of a container that is already being rendered further up the current path. */
    static final String CYCLE = "(cycle)";
    static final String THIS_MAP = "(this Map)";
    static final String THIS_COLLECTION = "(this Collection)";
    /** Rendered in place of a container once a render that has closed a loop has spent its {@link #LOOP_BUDGET}. */
    static final String ELIDED = "...";

    /**
     * Values a render may print once it has closed a loop, before it stops expanding containers: far more than any
     * readable {@code toString()}, far less than the factorial blow-up of a densely cross-linked graph.
     */
    static final int LOOP_BUDGET = 100_000;

    // The containers being rendered on this thread. Present only while a render is in progress.
    private static final ThreadLocal<Path> PATH = new ThreadLocal<>();

    // Whether a container's toString() is the JDK's standard format, so that walking it here renders it
    // byte-for-byte as its own toString() would. Decided once per class, by who DECLARES toString(): a subclass
    // that overrides it has its own format and is delegated to.
    private static final ClassValue<Boolean> STANDARD_FORMAT = new ClassValue<Boolean>() {
        @Override
        protected Boolean computeValue(Class<?> type) {
            try {
                Class<?> declarer = type.getMethod("toString").getDeclaringClass();
                return declarer == AbstractMap.class
                        || declarer == AbstractCollection.class
                        // These override toString() with the identical format. Hashtable and Vector do too, but
                        // render under their own lock, which walking them from outside would give up.
                        || declarer == ConcurrentHashMap.class
                        || declarer == CopyOnWriteArrayList.class
                        || declarer == CopyOnWriteArraySet.class;
            } catch (Exception e) {
                return false;
            }
        }
    };

    private CycleSafeToString() {
    }

    /**
     * Renders a map in the JDK's format: {@code {k=v, k2=v2}}.
     *
     * @param self   the map being rendered: the identity placed on the path, and printed as {@code (this Map)}
     *               where it contains itself. For a wrapper this is the wrapper, not the map it delegates to.
     * @param source where the entries come from -- {@code self}, or the map a wrapper delegates to
     */
    static String map(Map<?, ?> self, Map<?, ?> source) {
        return render(self, path -> {
            StringBuilder sb = new StringBuilder();
            appendEntries(sb, self, source.entrySet().iterator(), path);
            return sb.toString();
        });
    }

    /**
     * Renders a collection in the JDK's format: {@code [a, b]}.
     *
     * @param self       the collection being rendered (the identity on the path)
     * @param items      its elements, fetched only once {@code self} is known not to be a cycle
     * @param selfMarker what to print where the collection contains itself -- normally {@link #THIS_COLLECTION}
     */
    static String collection(Object self, Supplier<? extends Iterator<?>> items, String selfMarker) {
        return render(self, path -> {
            StringBuilder sb = new StringBuilder();
            appendItems(sb, self, items.get(), selfMarker, path);
            return sb.toString();
        });
    }

    /**
     * Renders any value -- for diagnostics that print caller-supplied objects. A leaf renders as
     * {@code String.valueOf()} would; a container is walked as described above.
     */
    static String value(Object value) {
        return withPath(path -> {
            StringBuilder sb = new StringBuilder();
            append(sb, value, path);
            return sb.toString();
        });
    }

    /**
     * Runs {@code body} with {@code self} on this thread's render path, for a container that builds its own
     * format. Returns {@link #CYCLE} instead when {@code self} is already being rendered further up -- that is the
     * loop closing -- and {@link #ELIDED} once a render that has closed a loop has spent its {@link #LOOP_BUDGET}.
     * Elements should be rendered through {@link #append}, which shares the same path and counts them.
     */
    static String render(Object self, Function<Path, String> body) {
        return withPath(path -> {
            if (!path.add(self)) {
                return CYCLE;
            }
            try {
                return path.exhausted() ? ELIDED : body.apply(path);
            } finally {
                path.remove(self);   // path, not visited: a node reached twice by different routes is not a loop
            }
        });
    }

    /**
     * Appends one value. A standard-format map or collection is walked on {@code path}; anything else is rendered
     * by its own {@code toString()}, and a java-util container re-enters {@link #render} on this same path.
     */
    static void append(StringBuilder sb, Object value, Path path) {
        path.countValue();
        if (value instanceof Map && STANDARD_FORMAT.get(value.getClass())) {
            Map<?, ?> map = (Map<?, ?>) value;
            if (!path.add(map)) {
                sb.append(CYCLE);
                return;
            }
            try {
                if (path.exhausted()) {
                    sb.append(ELIDED);
                } else {
                    appendEntries(sb, map, map.entrySet().iterator(), path);
                }
            } finally {
                path.remove(map);
            }
        } else if (value instanceof Collection && STANDARD_FORMAT.get(value.getClass())) {
            Collection<?> collection = (Collection<?>) value;
            if (!path.add(collection)) {
                sb.append(CYCLE);
                return;
            }
            try {
                if (path.exhausted()) {
                    sb.append(ELIDED);
                } else {
                    appendItems(sb, collection, collection.iterator(), THIS_COLLECTION, path);
                }
            } finally {
                path.remove(collection);
            }
        } else {
            sb.append(value);
        }
    }

    private static void appendEntries(StringBuilder sb, Object self, Iterator<? extends Map.Entry<?, ?>> entries,
                                      Path path) {
        if (!entries.hasNext()) {
            sb.append("{}");
            return;
        }
        sb.append('{');
        for (; ; ) {
            Map.Entry<?, ?> e = entries.next();
            appendMember(sb, e.getKey(), self, THIS_MAP, path);
            sb.append('=');
            appendMember(sb, e.getValue(), self, THIS_MAP, path);
            if (!entries.hasNext()) {
                sb.append('}');
                return;
            }
            sb.append(',').append(' ');
        }
    }

    private static void appendItems(StringBuilder sb, Object self, Iterator<?> items, String selfMarker,
                                    Path path) {
        if (!items.hasNext()) {
            sb.append("[]");
            return;
        }
        sb.append('[');
        for (; ; ) {
            appendMember(sb, items.next(), self, selfMarker, path);
            if (!items.hasNext()) {
                sb.append(']');
                return;
            }
            sb.append(',').append(' ');
        }
    }

    private static void appendMember(StringBuilder sb, Object member, Object self, String selfMarker,
                                     Path path) {
        if (member == self) {
            sb.append(selfMarker);
        } else {
            append(sb, member, path);
        }
    }

    // The outermost call creates the path and removes it on the way out, so nothing is left on the thread --
    // not even when a leaf's toString() throws part-way through.
    private static String withPath(Function<Path, String> body) {
        Path path = PATH.get();
        if (path != null) {
            return body.apply(path);
        }
        path = new Path();
        PATH.set(path);
        try {
            return body.apply(path);
        } finally {
            PATH.remove();
        }
    }

    /**
     * The containers being rendered on this thread, by identity, and how far the render has got -- whether it has
     * closed a loop, and how many values it has rendered -- for the {@link #LOOP_BUDGET}.
     */
    static final class Path {
        private final IdentitySet<Object> containers = new IdentitySet<>();
        private boolean looped;
        private int values;

        /** Puts a container on the path. Returns false when it is already there: the loop closing. */
        boolean add(Object container) {
            if (containers.add(container)) {
                return true;
            }
            looped = true;
            return false;
        }

        void remove(Object container) {
            containers.remove(container);
        }

        /** Counts one value rendered. */
        void countValue() {
            values++;
        }

        /** Whether this render has closed a loop and spent its budget, so it must not expand another container. */
        boolean exhausted() {
            return looped && values >= LOOP_BUDGET;
        }
    }
}
