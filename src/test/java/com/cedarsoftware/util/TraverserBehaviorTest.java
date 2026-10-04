package com.cedarsoftware.util;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

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
}
