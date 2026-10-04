package com.cedarsoftware.util;

import org.junit.jupiter.api.Test;

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
}
