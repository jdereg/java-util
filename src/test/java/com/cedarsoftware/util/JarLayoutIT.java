package com.cedarsoftware.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The packaged jar (run by failsafe, after package) serves JDK 8 and the module system both: every class a JDK 8 tool
 * reads is a JDK 8 class file, and module-info lives under META-INF/versions/9 of a multi-release jar, where only
 * JDK 9+ looks. It sat at the root as a Java 9 class file, which JDK 8 tools that read every class in a jar can fail
 * on. The OSGi headers stay as they were.
 */
class JarLayoutIT {

    private static JarFile jar() throws IOException {
        String path = System.getProperty("java-util.jar");
        assertNotNull(path, "failsafe sets java-util.jar to the packaged jar");
        return new JarFile(new File(path));
    }

    private static int majorVersion(JarFile jar, JarEntry entry) throws IOException {
        try (InputStream in = jar.getInputStream(entry)) {
            byte[] head = new byte[8];
            int read = 0;
            while (read < 8) {
                int n = in.read(head, read, 8 - read);
                if (n < 0) {
                    break;
                }
                read += n;
            }
            return ((head[6] & 0xff) << 8) | (head[7] & 0xff);
        }
    }

    @Test
    void everyClassOutsideTheVersionedTreeIsAJdk8ClassFile() throws IOException {
        try (JarFile jar = jar()) {
            List<String> newer = new ArrayList<>();
            int classes = 0;
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String name = entry.getName();
                if (name.endsWith(".class") && !name.startsWith("META-INF/versions/")) {
                    classes++;
                    if (majorVersion(jar, entry) > 52) {
                        newer.add(name);
                    }
                }
            }
            assertTrue(classes > 100, classes + " classes");
            assertTrue(newer.isEmpty(), "newer than JDK 8: " + newer);
        }
    }

    @Test
    void moduleInfoIsUnderVersions9OfAMultiReleaseJar() throws IOException {
        try (JarFile jar = jar()) {
            assertNull(jar.getEntry("module-info.class"), "module-info.class at the root");
            JarEntry moduleInfo = jar.getJarEntry("META-INF/versions/9/module-info.class");
            assertNotNull(moduleInfo, "META-INF/versions/9/module-info.class");
            assertEquals(53, majorVersion(jar, moduleInfo));
            Attributes main = jar.getManifest().getMainAttributes();
            assertEquals("true", main.getValue("Multi-Release"));
        }
    }

    @Test
    void theOsgiHeadersAreKept() throws IOException {
        try (JarFile jar = jar()) {
            Attributes main = jar.getManifest().getMainAttributes();
            assertEquals("com.cedarsoftware.java-util", main.getValue("Bundle-SymbolicName"));
            String exports = main.getValue("Export-Package");
            for (String pkg : new String[] {"com.cedarsoftware.util;", "com.cedarsoftware.util.cache;",
                    "com.cedarsoftware.util.convert;", "com.cedarsoftware.util.geom;"}) {
                assertTrue(exports.contains(pkg), pkg + " in " + exports);
            }
            assertTrue(!exports.contains("META-INF"), exports);
        }
    }
}
