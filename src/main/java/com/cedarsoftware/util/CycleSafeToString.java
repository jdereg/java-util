package com.cedarsoftware.util;

import java.util.AbstractCollection;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.function.Function;
import java.util.function.Supplier;

import com.cedarsoftware.util.cache.LockingLRUCacheStrategy;
import com.cedarsoftware.util.cache.ThreadedLRUCacheStrategy;

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
 * Nothing acyclic changes. Nested maps and collections whose {@code toString()} is the JDK's standard one, or
 * this library's own in the same format, are walked here, which renders them exactly as their own
 * {@code toString()} would and lets the walk see through them; a container with its own format is handed to that
 * {@code toString()} and keeps it; every other value is rendered by its own {@code toString()}, arrays included
 * (never expanded).
 * <p>
 * How deeply such containers nest is not limited by the stack. The first {@link #RECURSION_LIMIT} levels are
 * walked by plain recursion, three frames a level, which is the fastest way to write the shallow structures that
 * nearly all are; below that the walk keeps its place in a list instead, and a chain of a hundred thousand maps
 * prints. (From 4.112.0 through 4.118.0 each level of this library's maps took twelve stack frames where the
 * inherited {@code AbstractMap.toString()} takes three, and a chain 4.111.0 printed overflowed at a tenth of the
 * depth.) Stack is still spent where a value renders itself and has containers inside it.
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

    /** How many containers deep the walk recurses before it goes on in a loop: see the class comment. */
    static final int RECURSION_LIMIT = 64;

    // The containers being rendered on this thread. Present only while a render is in progress.
    private static final ThreadLocal<Path> PATH = new ThreadLocal<>();

    private static final Set<Class<?>> OWN_PLAIN_FORMAT = new HashSet<>(Arrays.<Class<?>>asList(
            CaseInsensitiveMap.class, CompactMap.class, AbstractConcurrentNullSafeMap.class, ClassValueMap.class,
            TTLCache.class, LockingLRUCacheStrategy.class, ThreadedLRUCacheStrategy.class,
            CaseInsensitiveSet.class, CompactSet.class, ConcurrentNavigableSetNullSafe.class, IdentitySet.class));

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
                        || declarer == CopyOnWriteArraySet.class
                        // This library's containers whose toString() is map(this, this) or
                        // collection(this, this::iterator, THIS_COLLECTION): walking one in place is that
                        // toString(). (Not ConcurrentSet, LRUCache, TrackingMap, ConcurrentList or MultiKeyMap:
                        // another marker, another source, or a format of their own.)
                        || OWN_PLAIN_FORMAT.contains(declarer);
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
        Path path = PATH.get();
        final boolean outermost = path == null;
        if (outermost) {
            path = new Path();
            PATH.set(path);
        }
        try {
            if (!path.add(self)) {
                return CYCLE;
            }
            try {
                if (path.exhausted()) {
                    return ELIDED;
                }
                Iterator<?> entries = source.entrySet().iterator();
                if (!entries.hasNext()) {
                    return "{}";
                }
                StringBuilder sb = new StringBuilder();
                appendEntries(sb, self, entries, path);
                return sb.toString();
            } finally {
                path.remove(self);   // path, not visited: a node reached twice by different routes is not a loop
            }
        } finally {
            if (outermost) {
                PATH.remove();
            }
        }
    }

    /**
     * Renders a collection in the JDK's format: {@code [a, b]}.
     *
     * @param self       the collection being rendered (the identity on the path)
     * @param items      its elements, fetched only once {@code self} is known not to be a cycle
     * @param selfMarker what to print where the collection contains itself -- normally {@link #THIS_COLLECTION}
     */
    static String collection(Object self, Supplier<? extends Iterator<?>> items, String selfMarker) {
        Path path = PATH.get();
        final boolean outermost = path == null;
        if (outermost) {
            path = new Path();
            PATH.set(path);
        }
        try {
            if (!path.add(self)) {
                return CYCLE;
            }
            try {
                if (path.exhausted()) {
                    return ELIDED;
                }
                Iterator<?> members = items.get();
                if (!members.hasNext()) {
                    return "[]";
                }
                StringBuilder sb = new StringBuilder();
                appendItems(sb, self, members, selfMarker, path);
                return sb.toString();
            } finally {
                path.remove(self);
            }
        } finally {
            if (outermost) {
                PATH.remove();
            }
        }
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
     * by its own {@code toString()}, and a java-util container with a format of its own re-enters {@link #render}
     * on this same path.
     */
    static void append(StringBuilder sb, Object value, Path path) {
        path.countValue();
        final int kind = kindOf(value);
        if (kind == LEAF) {
            sb.append(value);
            return;
        }
        if (!path.add(value)) {
            sb.append(CYCLE);
            return;
        }
        try {
            if (path.exhausted()) {
                sb.append(ELIDED);
                return;
            }
            Iterator<?> members = membersOf(value, kind);
            if (!members.hasNext()) {
                sb.append(kind == MAP ? "{}" : "[]");
            } else if (path.nesting >= RECURSION_LIMIT) {
                walk(sb, new Level(value, members, kind == MAP, kind == MAP ? THIS_MAP : THIS_COLLECTION), path);
            } else {
                path.nesting++;
                try {
                    if (kind == MAP) {
                        appendEntries(sb, value, members, path);
                    } else {
                        appendItems(sb, value, members, THIS_COLLECTION, path);
                    }
                } finally {
                    path.nesting--;
                }
            }
        } finally {
            path.remove(value);
        }
    }

    /** By recursion: a map that has entries. */
    private static void appendEntries(StringBuilder sb, Object self, Iterator<?> entries, Path path) {
        sb.append('{');
        for (; ; ) {
            Map.Entry<?, ?> e = (Map.Entry<?, ?>) entries.next();
            Object key = e.getKey();
            Object value = e.getValue();                    // both read before either is written, as the JDK does
            appendMember(sb, key, self, THIS_MAP, path);
            sb.append('=');
            appendMember(sb, value, self, THIS_MAP, path);
            if (!entries.hasNext()) {
                sb.append('}');
                return;
            }
            sb.append(',').append(' ');
        }
    }

    /** By recursion: a collection that has elements. */
    private static void appendItems(StringBuilder sb, Object self, Iterator<?> items, String selfMarker,
                                    Path path) {
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

    private static final int LEAF = 0;
    private static final int MAP = 1;
    private static final int COLLECTION = 2;

    /** MAP or COLLECTION for a container this class walks itself; LEAF for whatever renders itself. */
    private static int kindOf(Object value) {
        if (value instanceof Map) {
            return STANDARD_FORMAT.get(value.getClass()) ? MAP : LEAF;
        }
        if (value instanceof Collection) {
            return STANDARD_FORMAT.get(value.getClass()) ? COLLECTION : LEAF;
        }
        return LEAF;
    }

    private static Iterator<?> membersOf(Object container, int kind) {
        return kind == MAP ? ((Map<?, ?>) container).entrySet().iterator() : ((Collection<?>) container).iterator();
    }

    /**
     * Past {@link #RECURSION_LIMIT} levels: writes {@code top}, a container that has members and is already on
     * the path, and every container below it that this class walks, as the JDK's {@code toString()} writes them.
     * <p>
     * The containers open at any moment are a list of {@link Level}s, not calls on the stack: a member that is a
     * container becomes the next level, and the loop goes on there until it has no more members and the level
     * before it is the last again. A member that renders itself does so through {@code StringBuilder.append}, and
     * may come back into this class, where its own walk runs above this one's levels and ends before returning.
     */
    private static void walk(StringBuilder sb, Level top, Path path) {
        final int base = path.depth;
        path.open(top);
        sb.append(top.map ? '{' : '[');
        try {
            Level level = top;
            for (; ; ) {
                // Members are written here until one is a container to walk, which comes back as the next level
                Level inner = level.map ? entries(sb, level, path) : items(sb, level, path);
                if (inner != null) {
                    level = inner;
                    continue;
                }
                path.close();                               // that level is finished
                if (path.depth == base) {
                    return;                                 // top's place on the path is its caller's to give up
                }
                path.remove(level.self);
                level = path.levels[path.depth - 1];
            }
        } finally {
            // Only when a member's toString() or an iterator threw: give up what this walk still holds, so that
            // the path is as its caller left it.
            while (path.depth > base) {
                Level level = path.levels[path.depth - 1];
                path.close();
                if (path.depth > base) {
                    path.remove(level.self);
                }
            }
        }
    }

    /** A map's entries from where it stands: to its closing brace (null), or to a member to walk (that level). */
    private static Level entries(StringBuilder sb, Level level, Path path) {
        final Iterator<?> entries = level.members;
        final Object self = level.self;
        final String selfMarker = level.selfMarker;
        if (level.valuePending) {                           // back from a key that was a container
            level.valuePending = false;
            Object value = level.value;
            level.value = null;
            sb.append('=');
            Level inner = member(sb, value, self, selfMarker, path);
            if (inner != null) {
                return inner;
            }
        }
        boolean first = !level.started;
        level.started = true;
        while (entries.hasNext()) {
            if (first) {
                first = false;
            } else {
                sb.append(',').append(' ');
            }
            Map.Entry<?, ?> entry = (Map.Entry<?, ?>) entries.next();
            Object key = entry.getKey();
            Object value = entry.getValue();                // both read before either is written, as the JDK does
            Level inner = member(sb, key, self, selfMarker, path);
            if (inner != null) {
                level.value = value;
                level.valuePending = true;
                return inner;
            }
            sb.append('=');
            inner = member(sb, value, self, selfMarker, path);
            if (inner != null) {
                return inner;
            }
        }
        sb.append('}');
        return null;
    }

    /** A collection's elements from where it stands: to its closing bracket (null), or to one to walk. */
    private static Level items(StringBuilder sb, Level level, Path path) {
        final Iterator<?> items = level.members;
        final Object self = level.self;
        final String selfMarker = level.selfMarker;
        boolean first = !level.started;
        level.started = true;
        while (items.hasNext()) {
            if (first) {
                first = false;
            } else {
                sb.append(',').append(' ');
            }
            Level inner = member(sb, items.next(), self, selfMarker, path);
            if (inner != null) {
                return inner;
            }
        }
        sb.append(']');
        return null;
    }

    /** Writes one member, unless it is a container to walk: then that container's level, opened. */
    private static Level member(StringBuilder sb, Object member, Object self, String selfMarker, Path path) {
        if (member == self) {
            sb.append(selfMarker);
            return null;
        }
        path.countValue();
        final int kind = kindOf(member);
        if (kind == LEAF) {
            sb.append(member);
            return null;
        }
        return container(sb, member, kind, path);
    }

    private static Level container(StringBuilder sb, Object container, int kind, Path path) {
        if (!path.add(container)) {
            sb.append(CYCLE);
            return null;
        }
        Iterator<?> members = null;
        try {
            if (!path.exhausted()) {
                members = membersOf(container, kind);
            }
        } finally {
            if (members == null || !members.hasNext()) {    // nothing to walk, or it threw
                path.remove(container);
            }
        }
        if (members == null) {
            sb.append(ELIDED);
            return null;
        }
        if (!members.hasNext()) {
            sb.append(kind == MAP ? "{}" : "[]");
            return null;
        }
        Level level = new Level(container, members, kind == MAP, kind == MAP ? THIS_MAP : THIS_COLLECTION);
        path.open(level);
        sb.append(kind == MAP ? '{' : '[');
        return level;
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

    /** One container the walk has open: where it is among its members. */
    private static final class Level {
        final Object self;
        final Iterator<?> members;
        final boolean map;
        final String selfMarker;
        boolean started;            // a member has been written: the next is preceded by ", "
        boolean valuePending;       // map only: the key of the current entry is written, its value is not
        Object value;

        Level(Object self, Iterator<?> members, boolean map, String selfMarker) {
            this.self = self;
            this.members = members;
            this.map = map;
            this.selfMarker = selfMarker;
        }
    }

    /**
     * The containers being rendered on this thread, by identity, and how far the render has got -- whether it has
     * closed a loop, and how many values it has rendered -- for the {@link #LOOP_BUDGET}.
     * <p>
     * Containers go on and come off in order, so they are a stack in an array. Looking one up is a scan of that
     * array while it is short, which it nearly always is, and an identity set beside it once it is not.
     */
    static final class Path {
        private static final int SCAN_LIMIT = 16;
        private Object[] containers = new Object[8];
        private int size;
        private IdentitySet<Object> index;      // every container on the path, once there are more than SCAN_LIMIT
        private Level[] levels;                 // the levels walk() has open, innermost last: none until it runs
        private int depth;
        private int nesting;                    // how many containers deep the recursion in append() is
        private boolean looped;
        private int values;

        /** Puts a container on the path. Returns false when it is already there: the loop closing. */
        boolean add(Object container) {
            if (index != null) {
                if (!index.add(container)) {
                    looped = true;
                    return false;
                }
            } else {
                for (int i = size - 1; i >= 0; i--) {
                    if (containers[i] == container) {
                        looped = true;
                        return false;
                    }
                }
                if (size == SCAN_LIMIT) {
                    index = new IdentitySet<>();
                    for (int i = 0; i < size; i++) {
                        index.add(containers[i]);
                    }
                    index.add(container);
                }
            }
            if (size == containers.length) {
                containers = Arrays.copyOf(containers, size * 2);
            }
            containers[size++] = container;
            return true;
        }

        void remove(Object container) {
            int at = size - 1;
            while (at >= 0 && containers[at] != container) {     // the last one put on, unless a caller's are crossed
                at--;
            }
            if (at < 0) {
                return;
            }
            System.arraycopy(containers, at + 1, containers, at, size - at - 1);
            containers[--size] = null;
            if (index != null) {
                index.remove(container);
            }
        }

        private void open(Level level) {
            if (levels == null) {
                levels = new Level[16];
            } else if (depth == levels.length) {
                levels = Arrays.copyOf(levels, depth * 2);
            }
            levels[depth++] = level;
        }

        private void close() {
            levels[--depth] = null;
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
