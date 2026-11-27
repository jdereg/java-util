package com.cedarsoftware.util;

import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 4.119.0: JDK classes whose public constructors a JSON object's values can reach, with a side effect, are blocked.
 * A JDK constructor's parameters carry no names, so newInstance() matches the values to a constructor by type: an
 * object holding one string runs the class's (String) constructor. JEditorPane(String url) calls setPage(url) and
 * fetches the URL; DescriptorSupport(String) builds any class named in its text through that class's (String)
 * constructor, past the block list; DataFlavor(String) loads and initializes a named class; the image streams
 * create or open files; KerberosPrincipal(String, int) looks a host up in DNS.
 * <p>
 * Blocked: the javax.swing and javax.imageio.stream packages, every java.awt.Component and java.awt.MenuComponent
 * (AWT and Swing components and menus, and subclasses), SplashScreen, DescriptorSupport, DataFlavor and
 * KerberosPrincipal. The AWT value types (Point, Color, Rectangle, Dimension, Insets, Font, java.awt.geom) stay
 * allowed.
 * <p>
 * Every probe is side-effect free: the gate runs before any constructor, no probe passes a value, classes are loaded
 * with initialize=false, and the classes whose no-value construction has an effect (a temp file, a krb5.conf read)
 * are never handed to newInstance().
 */
class ClassUtilitiesGuiComponentBlockTest {

    /** A component an application declares itself: blocked through its supertype. */
    static class MyPane extends javax.swing.JEditorPane {
        private static final long serialVersionUID = 1L;
    }

    static class MyPanel extends java.awt.Panel {
        private static final long serialVersionUID = 1L;
    }

    /** An application class that only implements a Swing interface: refused too, through the package rule. */
    static class MyListener implements javax.swing.event.ChangeListener {
        @Override
        public void stateChanged(javax.swing.event.ChangeEvent e) {
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "javax.swing.JEditorPane",
            "javax.swing.JTextPane",
            "javax.swing.ImageIcon",
            "javax.swing.UIManager",
            "javax.swing.JFrame",
            "javax.swing.text.html.HTMLEditorKit",
            "javax.swing.tree.DefaultMutableTreeNode",
            "javax.management.modelmbean.DescriptorSupport",
            "java.awt.datatransfer.DataFlavor",
            "javax.security.auth.kerberos.KerberosPrincipal",
            "javax.imageio.stream.FileCacheImageInputStream",
            "javax.imageio.stream.FileImageInputStream",
            "java.awt.MenuComponent"
    })
    void swingIsBlockedByNameBeforeItLoads(String className) {
        assertTrue(ClassUtilities.SecurityChecker.isSecurityBlockedName(className), className);
        assertThrows(SecurityException.class, () -> ClassUtilities.forName(className, null),
                "forName must refuse " + className);
    }

    static Stream<String> awtGuiClasses() {
        return Stream.of(
                "java.awt.Component",
                "java.awt.Frame",
                "java.awt.Window",
                "java.awt.Button",
                "java.awt.Canvas",
                "java.awt.TextArea",
                "java.awt.SplashScreen",
                "java.awt.MenuItem",
                "java.awt.PopupMenu",
                "javax.swing.JEditorPane",
                "javax.swing.JTextPane",
                "javax.management.modelmbean.DescriptorSupport",
                "java.awt.datatransfer.DataFlavor"
        );
    }

    /** Refused once loaded; never constructed here: with no values, each would create a file or read krb5.conf. */
    @ParameterizedTest
    @ValueSource(strings = {
            "javax.imageio.stream.FileCacheImageInputStream",
            "javax.imageio.stream.FileImageInputStream",
            "javax.security.auth.kerberos.KerberosPrincipal"
    })
    void classesWithAnEffectEvenWithoutValuesAreBlockedOnceLoaded(String className) throws Exception {
        Class<?> c = Class.forName(className, false, ClassLoader.getSystemClassLoader());
        assertTrue(ClassUtilities.SecurityChecker.isSecurityBlocked(c), className);
        assertThrows(SecurityException.class, () -> ClassUtilities.SecurityChecker.verifyClass(c));
        assertThrows(SecurityException.class, () -> ClassUtilities.forName(className, null));
    }

    @ParameterizedTest
    @ValueSource(strings = {"javax.swing.JEditorPane[]", "[Ljavax.swing.JEditorPane;", "[Ljava.awt.Frame;",
            "java.awt.datatransfer.DataFlavor[][]"})
    void anArrayOfABlockedClassIsRefused(String name) {
        assertThrows(SecurityException.class, () -> ClassUtilities.forName(name, null), name);
    }

    @ParameterizedTest
    @MethodSource("awtGuiClasses")
    void guiClassesAreBlockedOnceLoaded(String className) throws Exception {
        Class<?> c = Class.forName(className, false, ClassLoader.getSystemClassLoader());
        assertTrue(ClassUtilities.SecurityChecker.isSecurityBlocked(c), className);
        assertThrows(SecurityException.class, () -> ClassUtilities.SecurityChecker.verifyClass(c));
        assertThrows(SecurityException.class, () -> ClassUtilities.forName(className, null));
        assertThrows(SecurityException.class, () -> ClassUtilities.newInstance(c, null),
                "newInstance must refuse " + className);
    }

    @Test
    void aSubclassOfAComponentOrAnImplementerOfASwingInterfaceIsBlocked() {
        for (Class<?> c : new Class<?>[]{MyPane.class, MyPanel.class, MyListener.class}) {
            assertTrue(ClassUtilities.SecurityChecker.isSecurityBlocked(c), c.getName());
            assertThrows(SecurityException.class, () -> ClassUtilities.forName(c.getName(), null));
            assertThrows(SecurityException.class, () -> ClassUtilities.newInstance(c, null));
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "java.awt.Point",
            "java.awt.Color",
            "java.awt.Rectangle",
            "java.awt.Dimension",
            "java.awt.Insets",
            "java.awt.Font",
            "java.awt.geom.Point2D$Double",
            "java.awt.geom.Rectangle2D$Double"
    })
    void awtValueTypesAreNotBlocked(String className) throws Exception {
        assertFalse(ClassUtilities.SecurityChecker.isSecurityBlockedName(className), className);
        Class<?> c = Class.forName(className, false, ClassLoader.getSystemClassLoader());
        assertFalse(ClassUtilities.SecurityChecker.isSecurityBlocked(c), className);
        assertSame(c, ClassUtilities.forName(className, null));
    }

    @Test
    void thePackageRuleIsAPackageNotASubstring() {
        assertFalse(ClassUtilities.SecurityChecker.isSecurityBlockedName("javax.swingx.Widget"));
        assertFalse(ClassUtilities.SecurityChecker.isSecurityBlockedName("com.example.javax.swing.Pane"));
    }
}
