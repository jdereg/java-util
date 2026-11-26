package com.cedarsoftware.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The 4.119.0 once-over of SystemUtilities: loading the class writes nothing to the JVM's system properties; the
 * sensitive-name filter does not let a name through while its patterns are being changed; a temporary directory
 * is deleted at exit with what is in it; and a TZ written the POSIX way, with a colon, is not called invalid.
 */
class SystemUtilitiesOnceOverTest {
    private static final String[] PROPERTIES = {"systemutilities.sensitive.variable.patterns",
            "systemutilities.max.shutdown.hooks", "systemutilities.max.temp.prefix.length",
            "systemutilities.security.enabled", "systemutilities.environment.variable.validation.enabled",
            "systemutilities.file.system.validation.enabled", "ONCEOVER_BBB_VALUE"};
    private final String[] saved = new String[PROPERTIES.length];

    @BeforeEach
    void rememberProperties() {
        for (int i = 0; i < PROPERTIES.length; i++) {
            saved[i] = System.getProperty(PROPERTIES[i]);
        }
    }

    @AfterEach
    void restoreProperties() {
        for (int i = 0; i < PROPERTIES.length; i++) {
            if (saved[i] == null) {
                System.clearProperty(PROPERTIES[i]);
            } else {
                System.setProperty(PROPERTIES[i], saved[i]);
            }
        }
    }

    /**
     * Through 4.118.0 the static initializer set three properties that were not set, as defaults. Besides writing
     * to state the whole JVM shares, that made the class impossible to load where properties may be read and not
     * written (a SecurityManager on JDK 8 to 17), and UniqueIdGenerator and json-io load it.
     */
    @Test
    void loadingTheClassWritesNoSystemProperties() throws Exception {
        for (int i = 0; i < 3; i++) {
            System.clearProperty(PROPERTIES[i]);
        }
        URL classes = SystemUtilities.class.getProtectionDomain().getCodeSource().getLocation();
        // the library a second time, under the JDK's own loaders only
        ClassLoader jdkOnly = ClassLoader.getSystemClassLoader().getParent();
        try (URLClassLoader loader = new URLClassLoader(new URL[]{classes}, jdkOnly)) {
            Class<?> another = Class.forName(SystemUtilities.class.getName(), true, loader);
            assertTrue(another != SystemUtilities.class);
            assertTrue((Integer) another.getMethod("getAvailableProcessors").invoke(null) > 0);
            another.getMethod("getExternalVariable", String.class).invoke(null, "HOME");
        }
        for (int i = 0; i < 3; i++) {
            assertNull(System.getProperty(PROPERTIES[i]), PROPERTIES[i] + " was written");
        }
    }

    /** The limits that the properties defaulted to are the limits without them. */
    @Test
    void theLimitsAreWhatTheyWereWithoutTheirProperties() {
        for (int i = 0; i < 3; i++) {
            System.clearProperty(PROPERTIES[i]);
        }
        System.setProperty("systemutilities.security.enabled", "true");
        System.setProperty("systemutilities.file.system.validation.enabled", "true");
        char[] long101 = new char[101];
        Arrays.fill(long101, 'p');
        IllegalArgumentException tooLong = assertThrows(IllegalArgumentException.class,
                () -> SystemUtilities.createTempDirectory(new String(long101)));
        assertTrue(tooLong.getMessage().contains("max 100"), tooLong.getMessage());
        File ok = SystemUtilities.createTempDirectory(new String(long101, 0, 100));
        assertTrue(ok.isDirectory());
        assertTrue(ok.delete());

        // and a name that the default patterns block is still blocked
        System.setProperty("systemutilities.environment.variable.validation.enabled", "true");
        System.setProperty("ONCEOVER_BBB_VALUE", "visible");
        assertEquals("visible", SystemUtilities.getExternalVariable("ONCEOVER_BBB_VALUE"));
        System.setProperty("systemutilities.sensitive.variable.patterns", "BBB");
        assertNull(SystemUtilities.getExternalVariable("ONCEOVER_BBB_VALUE"));
    }

    /**
     * The patterns and the text they were made from were two fields. With one thread changing the property, a
     * reader could pair the new text with the old patterns and return a variable the new patterns block: several
     * hundred times in ten seconds.
     */
    @Test
    @Timeout(60)
    void aBlockedNameStaysBlockedWhileThePatternsAreChanged() throws Exception {
        System.setProperty("systemutilities.security.enabled", "true");
        System.setProperty("systemutilities.environment.variable.validation.enabled", "true");
        System.setProperty("ONCEOVER_BBB_VALUE", "must not be seen");
        final String property = "systemutilities.sensitive.variable.patterns";
        System.setProperty(property, "BBB");
        final AtomicBoolean stop = new AtomicBoolean();
        final AtomicLong changes = new AtomicLong();    // odd while the writer is between two values
        Thread writer = new Thread(() -> {
            while (!stop.get()) {
                for (String patterns : new String[]{"AAA", "BBB"}) {
                    changes.incrementAndGet();
                    System.setProperty(property, patterns);
                    changes.incrementAndGet();
                    for (int i = 0; i < 2_000; i++) {
                        Thread.yield();             // let the readers see this value for a while
                    }
                }
            }
        });
        final AtomicLong leaks = new AtomicLong();
        final AtomicLong counted = new AtomicLong();
        Thread[] readers = new Thread[4];
        for (int r = 0; r < readers.length; r++) {
            readers[r] = new Thread(() -> {
                while (!stop.get()) {
                    long before = changes.get();
                    boolean blocked = "BBB".equals(System.getProperty(property));
                    String value = SystemUtilities.getExternalVariable("ONCEOVER_BBB_VALUE");
                    if (blocked && (before & 1) == 0 && changes.get() == before) {
                        counted.incrementAndGet();  // the patterns were BBB from before the call to after it
                        if (value != null) {
                            leaks.incrementAndGet();
                        }
                    }
                }
            });
        }
        writer.start();
        for (Thread reader : readers) {
            reader.start();
        }
        Thread.sleep(1_500);
        stop.set(true);
        writer.join();
        for (Thread reader : readers) {
            reader.join();
        }
        assertTrue(counted.get() > 1_000, "only " + counted.get() + " reads to judge by");
        assertEquals(0, leaks.get(), "of " + counted.get() + " reads while the patterns blocked it");
    }

    /**
     * createTempDirectory() promises a directory deleted at JVM exit. File.deleteOnExit() deletes a directory only
     * when it is empty, so one that had been used stayed behind; and it remembered every path until the JVM ended.
     */
    @Test
    @Timeout(120)
    void aTemporaryDirectoryIsDeletedAtExitWithWhatIsInIt() throws Exception {
        List<String> printed = runAnotherJvm(null, "tempdir");
        assertEquals(2, printed.size(), printed.toString());
        File used = new File(printed.get(0));
        File empty = new File(printed.get(1));
        assertTrue(used.getName().startsWith("onceover"), used.toString());
        assertFalse(used.exists(), "a directory with files in it was left behind: " + used);
        assertFalse(empty.exists());

        // In this JVM: directories their caller deletes are forgotten, not kept for the hook until the JVM ends
        Field field = SystemUtilities.class.getDeclaredField("TEMP_DIRECTORIES");
        field.setAccessible(true);
        Set<?> remembered = (Set<?>) field.get(null);
        for (int i = 0; i < 2_000; i++) {
            File directory = SystemUtilities.createTempDirectory("onceover-forget");
            assertTrue(directory.delete());
        }
        assertTrue(remembered.size() < 1_100, "remembers " + remembered.size() + " directories that are gone");

        File kept = SystemUtilities.createTempDirectory("onceover-tree");
        Files.write(new File(kept, "a.txt").toPath(), "a".getBytes(StandardCharsets.UTF_8));
        File inner = new File(kept, "inner");
        assertTrue(inner.mkdir());
        Files.write(new File(inner, "b.txt").toPath(), "b".getBytes(StandardCharsets.UTF_8));
        SystemUtilities.deleteTree(kept.toPath());
        assertFalse(kept.exists());
    }

    /** POSIX writes TZ=":America/Chicago". The zone was found all the same, after a WARNING on every call. */
    @Test
    @Timeout(120)
    void aTimeZoneWrittenWithAColonIsNotCalledInvalid() throws Exception {
        List<String> printed = runAnotherJvm(":America/Chicago", "tz");
        assertEquals("America/Chicago", printed.get(printed.size() - 1), printed.toString());
        for (String line : printed) {
            assertFalse(line.contains("Invalid timezone"), line);
        }
        printed = runAnotherJvm("Not/AZone", "tz");
        assertTrue(printed.toString().contains("Invalid timezone in TZ environment variable: Not/AZone"),
                "one that is invalid is still reported: " + printed);
    }

    /** What the other JVM runs. */
    public static void main(String[] args) throws Exception {
        if ("tempdir".equals(args[0])) {
            File used = SystemUtilities.createTempDirectory("onceover-used");
            Files.write(new File(used, "data.txt").toPath(), "x".getBytes(StandardCharsets.UTF_8));
            File inner = new File(used, "inner");
            inner.mkdir();
            Files.write(new File(inner, "more.txt").toPath(), "y".getBytes(StandardCharsets.UTF_8));
            File empty = SystemUtilities.createTempDirectory("onceover-empty");
            System.out.println(used.getPath());
            System.out.println(empty.getPath());
        } else {
            SystemUtilities.getSystemTimeZone();
            TimeZone zone = SystemUtilities.getSystemTimeZone();
            System.out.println(zone.getID());
        }
    }

    private static List<String> runAnotherJvm(String tz, String mode) throws Exception {
        String java = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java";
        ProcessBuilder builder = new ProcessBuilder(java, "-cp", System.getProperty("java.class.path"),
                SystemUtilitiesOnceOverTest.class.getName(), mode);
        builder.redirectErrorStream(true);
        if (tz != null) {
            builder.environment().put("TZ", tz);
        }
        Process process = builder.start();
        List<String> lines = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            for (String line = reader.readLine(); line != null; line = reader.readLine()) {
                if (!line.trim().isEmpty()) {
                    lines.add(line.trim());
                }
            }
        }
        assertEquals(0, process.waitFor(), lines.toString());
        return lines;
    }
}
