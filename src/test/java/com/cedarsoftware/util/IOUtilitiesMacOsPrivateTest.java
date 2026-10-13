package com.cedarsoftware.util;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * macOS keeps /etc, /tmp and /var under /private: the canonical form of /etc/passwd is /private/etc/passwd, and
 * every temporary file lives under /private/var/folders or /private/tmp. IOUtilities did not know this, in both
 * directions. Its check for file names that say "private" refused every file URL made from the real path of a
 * temporary file, or of a checkout under /private/tmp. And its check of the canonical path for /etc/passwd,
 * /etc/shadow and /etc/ssh never matched on a Mac, so the File methods read them there.
 * <p>
 * The rule now: a path that begins /private/etc, /private/tmp or /private/var is judged as the path it stands for.
 * The oracle of most tests here is that rule itself -- the verdict on the long form is the verdict on the short one.
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
class IOUtilitiesMacOsPrivateTest {

    private static final String SYSTEM = "refused: File URL accesses system path";
    private static final String SUSPICIOUS = "refused: Suspicious file path detected";
    private static final String TRAVERSAL = "refused: Dangerous path patterns detected in file URL";
    private static final String PASSED = "passed";

    private static final String[] SETTINGS = {"io.url.protocol.validation.disabled",
            "io.file.protocol.validation.disabled", "io.allowed.protocols", "io.path.validation.disabled"};

    private final Map<String, String> settingsBefore = new LinkedHashMap<>();

    @BeforeEach
    void defaultSettings() {
        for (String setting : SETTINGS) {
            settingsBefore.put(setting, System.getProperty(setting));
            System.clearProperty(setting);
        }
    }

    @AfterEach
    void restoreSettings() {
        for (Map.Entry<String, String> setting : settingsBefore.entrySet()) {
            if (setting.getValue() == null) {
                System.clearProperty(setting.getKey());
            } else {
                System.setProperty(setting.getKey(), setting.getValue());
            }
        }
    }

    /**
     * What IOUtilities says to a URL: "refused: " and the reason when its checks refuse it, "passed" when they let
     * it through -- whether the JDK then finds a file there or not.
     */
    private static String verdict(String url) {
        try {
            URLConnection connection = new URL(url).openConnection();
            try (InputStream in = IOUtilities.getInputStream(connection)) {
                return PASSED;
            }
        } catch (SecurityException e) {
            String reason = e.getMessage();
            int colon = reason.indexOf(':');
            return "refused: " + (colon < 0 ? reason : reason.substring(0, colon));
        } catch (Exception e) {
            return PASSED;
        }
    }

    // ---------------------------------------------------------------- file URLs

    @Test
    void aTemporaryFileUnderPrivateIsNotSuspicious() {
        assertEquals(PASSED, verdict("file:///private/tmp/work/data.txt"));
        assertEquals(PASSED, verdict("file:///private/tmp/checkout/target/test-classes/prices.json"));
        assertEquals(PASSED, verdict("file:///private/var/folders/br/bg1cj765/T/junit1234/data.json"));
        assertEquals(PASSED, verdict("file:///private/tmp"));
        assertEquals(PASSED, verdict("file:///private/var/"));
    }

    @Test
    void whatIsRefusedUnderTheShortFormIsRefusedUnderPrivate() {
        assertEquals(SYSTEM, verdict("file:///private/etc"));
        assertEquals(SYSTEM, verdict("file:///private/etc/"));
        assertEquals(SYSTEM, verdict("file:///private/etc/hosts"));
        assertEquals(SYSTEM, verdict("file:///private/etc/master.passwd"));
        assertEquals(SYSTEM, verdict("file:///private/etc/ssh/ssh_config"));
        assertEquals(SUSPICIOUS, verdict("file:///private/var/root/.ssh/authorized_keys"));
        assertEquals(SUSPICIOUS, verdict("file:///private/var/root/.ssh"));
        assertEquals(SUSPICIOUS, verdict("file:///private/tmp/keys/private.key"));
        assertEquals(SUSPICIOUS, verdict("file:///private/tmp/id_rsa"));
        assertEquals(SUSPICIOUS, verdict("file:///private/var/backup/shadow"));
        assertEquals(TRAVERSAL, verdict("file:///private/tmp/a/../b.txt"));
    }

    /** In any case: a Mac's file system does not tell /PRIVATE/etc from /private/etc. */
    @Test
    void theSystemDirectoryUnderPrivateInAnyCase() {
        assertEquals(SYSTEM, verdict("file:///PRIVATE/etc/hosts"));
        assertEquals(SYSTEM, verdict("file:///Private/Etc/hosts"));
        assertEquals(SYSTEM, verdict("file:///PRIVATE/ETC"));
        assertEquals(PASSED, verdict("file:///PRIVATE/tmp/data.txt"));
    }

    /** Only the three directories macOS keeps there. Any other "private" still marks the path. */
    @Test
    void anyOtherPrivateStillMarksThePath() {
        String[] paths = {"/private", "/private/", "/private/data.txt", "/private/home/u/a.txt",
                "/private/tmpfiles/a.txt", "/private/variable/a.txt", "/private/etcetera/a.txt",
                "/private/tmp.txt", "/private/var.log", "/privately/tmp/a.txt",
                "/home/u/private/tmp/a.txt", "/home/u/private/var/a.txt", "/tmp/private/tmp/a.txt",
                "/opt/app/ssl/private/server.pem", "/home/u/private.key", "/private/tmp/private/a.txt",
                "/private/var/folders/x/T/privately.txt"};
        for (String path : paths) {
            assertEquals(SUSPICIOUS, verdict("file://" + path), path);
        }
    }

    private static final String[] ROOTS = {"/tmp", "/var", "/etc"};
    private static final String[] NAMES = {"work", "data.txt", "folders", "T", "a.json", ".ssh", ".aws", "private",
            "private.key", "id_rsa", "passwd", "shadow", "..", "Private", "root", "db", "x.pem", "tmp", "etc"};

    /**
     * 600 paths from a fixed seed under /tmp, /var and /etc, of names that pass and names that are refused. Each
     * gets the same answer, for the same reason, with /private in front as without.
     */
    @Test
    void theLongFormGetsTheVerdictOfTheShortForm() {
        Random random = new Random(20261013L);
        Map<String, Integer> verdicts = new LinkedHashMap<>();
        for (int n = 0; n < 600; n++) {
            StringBuilder path = new StringBuilder(ROOTS[random.nextInt(ROOTS.length)]);
            for (int i = random.nextInt(5); i > 0; i--) {
                path.append('/').append(NAMES[random.nextInt(NAMES.length)]);
            }
            if (random.nextInt(8) == 0) {
                path.append('/');
            }
            String shortForm = verdict("file://" + path);
            assertEquals(shortForm, verdict("file:///private" + path), path.toString());
            verdicts.merge(shortForm, 1, Integer::sum);
        }
        for (String verdict : new String[] {PASSED, SYSTEM, SUSPICIOUS, TRAVERSAL}) {
            assertTrue(verdicts.getOrDefault(verdict, 0) >= 30, verdicts.toString());
        }
    }

    /** A real file, by its real path: on a Mac that path begins /private/var or /private/tmp. */
    @Test
    void aFileIsReadByItsRealPath(@TempDir Path directory) throws Exception {
        Path real = directory.toRealPath();
        byte[] content = "the content".getBytes("UTF-8");
        Files.write(real.resolve("data.txt"), content);

        URLConnection connection = new URL("file://" + real + "/data.txt").openConnection();
        try (InputStream in = IOUtilities.getInputStream(connection)) {
            assertArrayEquals(content, IOUtilities.inputStreamToBytes(in));
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        IOUtilities.transfer(real.resolve("data.txt").toFile(), out);
        assertArrayEquals(content, out.toByteArray());
    }

    // ---------------------------------------------------------------- the File methods

    /**
     * The File methods judge the canonical path, which on a Mac is /private/etc/passwd for /etc/passwd: the three
     * protected places of /etc were read there. Refused now on a Mac as on Linux, under either spelling.
     */
    @Test
    @DisabledOnOs(OS.WINDOWS)
    void theFileMethodsRefuseTheProtectedFilesOfEtc() {
        String[] paths = {"/etc/passwd", "/etc/shadow", "/etc/ssh/ssh_config", "/etc/ssh/keys/host_key",
                "/private/etc/passwd", "/private/etc/shadow", "/private/etc/ssh/ssh_config"};
        for (String path : paths) {
            SecurityException refused = assertThrows(SecurityException.class,
                    () -> IOUtilities.transfer(new File(path), new ByteArrayOutputStream()), path);
            assertTrue(refused.getMessage().contains("Access to system directory/file denied"),
                    path + ": " + refused.getMessage());
        }
    }
}
