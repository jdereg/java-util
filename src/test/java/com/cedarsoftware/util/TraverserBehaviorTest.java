package com.cedarsoftware.util;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.AbstractCollection;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Traverser behavior found by review.
 */
class TraverserBehaviorTest {

    /** A system class loader installed with -Djava.system.class.loader, as agents and launchers do. */
    static class PassThroughLoader extends ClassLoader {
        PassThroughLoader(ClassLoader parent) {
            super(parent);
        }
    }

    /**
     * The JDK's own loader is found whatever system class loader is installed. It was taken to be the system
     * loader's parent, so under a custom system loader (whose parent is the application loader) every application
     * class counted as a JDK class: a leaf whose fields were not walked, and traversing A -> B -> C visited only A.
     */
    @Test
    void theJdkLoaderIsFoundUnderACustomSystemClassLoader() throws Exception {
        ClassLoader expected;
        try {
            expected = (ClassLoader) ClassLoader.class.getMethod("getPlatformClassLoader").invoke(null);
        } catch (NoSuchMethodException jdk8) {
            expected = ClassLoader.getSystemClassLoader().getParent();       // the extension loader
        }
        assertSame(expected, Traverser.platformLoader(ClassLoader.getSystemClassLoader()));
        assertSame(expected, Traverser.platformLoader(new PassThroughLoader(ClassLoader.getSystemClassLoader())));
    }

    static class TaggedException extends RuntimeException {
        final String code = "E1";

        TaggedException() {
            super("boom");
        }
    }

    /**
     * A user subclass of a JDK class is walked through its own fields only, the same on every JDK. The fields its
     * JDK superclass declares (an exception's message, cause and stack trace) were walked on JDK 8-16, where they
     * are readable, and not on 17+: 5 nodes for this exception on JDK 8, 2 on JDK 21.
     */
    @Test
    void fieldsAJdkSuperclassDeclaresAreNotWalked() {
        TaggedException root = new TaggedException();
        for (boolean collectFields : new boolean[]{true, false}) {
            List<Object> visited = new ArrayList<>();
            Traverser.traverse(root, visit -> visited.add(visit.getNode()), null, collectFields);
            assertEquals(2, visited.size(), "the exception and its code, collectFields=" + collectFields);
            assertSame(root, visited.get(0));
            assertSame(root.code, visited.get(1));
        }
    }

    static class Payload {
    }

    static class TaggedList extends ArrayList<Object> {
        Object tag;
    }

    static class TaggedMap extends HashMap<Object, Object> {
        Object tag;
    }

    /**
     * The fields a user subclass of a Collection or Map declares are walked, besides its elements. Only the
     * elements were: the Payload a TaggedList or TaggedMap carried in a field was never reached. The fields of the
     * JDK class it extends are still not walked.
     */
    @Test
    void aUserCollectionOrMapsOwnFieldsAreWalked() {
        for (boolean collectFields : new boolean[]{true, false}) {
            TaggedList list = new TaggedList();
            list.add("x");
            list.tag = new Payload();
            assertEquals(identitiesOf(list, "x", list.tag), identities(visit(list, collectFields)));

            TaggedMap map = new TaggedMap();
            map.put("k", "v");
            map.tag = new Payload();
            assertEquals(identitiesOf(map, "k", "v", map.tag), identities(visit(map, collectFields)));
        }
    }

    static class Pair {
        Object first;
        Object second;
    }

    /**
     * A collection or map that cannot be read (a lazy-loading proxy whose session is gone) is visited but not
     * walked, and the rest of the graph still is. Its exception ended the whole traversal; a failed field read was
     * already passed over.
     */
    @Test
    void aCollectionOrMapThatCannotBeReadDoesNotEndTheWalk() {
        Collection<Object> lazyList = new AbstractCollection<Object>() {
            @Override
            public Iterator<Object> iterator() {
                throw new IllegalStateException("could not initialize proxy - no Session");
            }

            @Override
            public int size() {
                return 1;
            }
        };
        Map<Object, Object> lazyMap = new AbstractMap<Object, Object>() {
            @Override
            public Set<Map.Entry<Object, Object>> entrySet() {
                throw new IllegalStateException("could not initialize proxy - no Session");
            }
        };
        for (Object lazy : Arrays.asList(lazyList, lazyMap)) {
            Pair root = new Pair();
            root.first = lazy;
            root.second = new Payload();
            for (boolean collectFields : new boolean[]{true, false}) {
                assertEquals(identitiesOf(root, lazy, root.second), identities(visit(root, collectFields)));
            }
        }

        Pair root = new Pair();
        root.first = new ArrayList<Integer>(Arrays.asList(1, 2, 3)) {
            @Override
            public Iterator<Integer> iterator() {
                Iterator<Integer> real = super.iterator();
                return new Iterator<Integer>() {
                    @Override
                    public boolean hasNext() {
                        return real.hasNext();
                    }

                    @Override
                    public Integer next() {
                        Integer next = real.next();
                        if (next == 2) {
                            throw new IllegalStateException("closed");
                        }
                        return next;
                    }
                };
            }
        };
        root.second = new Payload();
        assertEquals(identitiesOf(root, root.first, 1, root.second), identities(visit(root, false)),
                "the elements read before the failure are walked");
    }

    private static final String[] LIMITS = {"traverser.max.stack.depth", "traverser.max.objects.visited",
            "traverser.max.collection.size", "traverser.max.array.length"};

    /**
     * Loading Traverser writes no system properties, and a limit that is not set is off, as documented, whenever
     * it was cleared. Its static initializer set every unset limit property to "0", process-wide; a limit cleared
     * afterwards then fell back to the built-in defaults, so with security on a 60,000-element list failed the
     * 50,000-element default.
     */
    @Test
    void limitsAreReadNotWrittenAndAnUnsetLimitIsOff() throws Exception {
        Map<String, String> saved = new HashMap<>();
        for (String key : LIMITS) {
            saved.put(key, System.getProperty(key));
            System.clearProperty(key);
        }
        String securitySaved = System.getProperty("traverser.security.enabled");
        try {
            URL classes = Traverser.class.getProtectionDomain().getCodeSource().getLocation();
            try (URLClassLoader fresh = new URLClassLoader(new URL[]{classes}, null)) {
                Class.forName(Traverser.class.getName(), true, fresh);         // runs its static initializer
            }
            for (String key : LIMITS) {
                assertNull(System.getProperty(key), key);
            }

            System.setProperty("traverser.security.enabled", "true");
            List<Object> wide = new ArrayList<>();
            for (int i = 0; i < 60_000; i++) {
                wide.add(i);
            }
            int[] visited = {0};
            Traverser.traverse(wide, visit -> visited[0]++, null, false);
            assertTrue(visited[0] > 1);
        } finally {
            for (String key : LIMITS) {
                if (saved.get(key) == null) {
                    System.clearProperty(key);
                } else {
                    System.setProperty(key, saved.get(key));
                }
            }
            if (securitySaved == null) {
                System.clearProperty("traverser.security.enabled");
            } else {
                System.setProperty("traverser.security.enabled", securitySaved);
            }
        }
    }

    private static List<Object> visit(Object root, boolean collectFields) {
        List<Object> visited = new ArrayList<>();
        Traverser.traverse(root, visit -> visited.add(visit.getNode()), null, collectFields);
        return visited;
    }

    private static Set<Integer> identitiesOf(Object... objects) {
        return identities(Arrays.asList(objects));
    }

    private static Set<Integer> identities(List<Object> objects) {
        Set<Integer> identities = new HashSet<>();
        for (Object o : objects) {
            identities.add(System.identityHashCode(o));
        }
        assertEquals(objects.size(), identities.size(), "each object once");
        return identities;
    }
}
