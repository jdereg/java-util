package com.cedarsoftware.util;

import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * A file URL that names a protected directory itself. The JDK answers a file URL that names a directory with the
 * listing of that directory, so {@code file:///etc} hands out the names of everything in /etc. IOUtilities refused
 * what is inside /etc, /proc, /sys and /dev and inside a .ssh, .gnupg, .aws or .docker directory, but not the
 * directory: the path had to go on with a slash.
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
class IOUtilitiesProtectedDirectoryTest {

    private static final String SYSTEM = "refused: File URL accesses system path";
    private static final String SUSPICIOUS = "refused: Suspicious file path detected";
    private static final String PASSED = "passed";

    private static final String[] SETTINGS = {
            "io.url.protocol.validation.disabled", "io.file.protocol.validation.disabled", "io.allowed.protocols"};

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

    @Test
    void aSystemDirectoryItselfIsRefused() {
        for (String directory : new String[] {"/etc", "/proc", "/sys", "/dev"}) {
            assertEquals(SYSTEM, verdict("file://" + directory), directory);
            assertEquals(SYSTEM, verdict("file://" + directory.toUpperCase()), directory);
            assertEquals(SYSTEM, verdict("file://localhost" + directory), directory);
            assertEquals(SYSTEM, verdict("file://" + directory + "/"), directory);
            assertEquals(SYSTEM, verdict("file://" + directory + "/hosts"), directory);
        }
    }

    @Test
    void aSensitiveHiddenDirectoryItselfIsRefused() {
        for (String directory : new String[] {".ssh", ".gnupg", ".aws", ".docker"}) {
            assertEquals(SUSPICIOUS, verdict("file:///home/u/" + directory), directory);
            assertEquals(SUSPICIOUS, verdict("file:///" + directory), directory);
            assertEquals(SUSPICIOUS, verdict("file:///home/u/" + directory + "/"), directory);
            assertEquals(SUSPICIOUS, verdict("file:///home/u/" + directory + "/config"), directory);
        }
    }

    /** With a directory that exists: before, the URL of the directory was answered with the names in it. */
    @Test
    void theListingOfASensitiveDirectoryIsNotHandedOut(@TempDir Path home) throws Exception {
        Path ssh = Files.createDirectory(home.resolve(".ssh"));
        Files.write(ssh.resolve("known_hosts"), "host".getBytes("UTF-8"));

        assertEquals(SUSPICIOUS, verdict("file://" + ssh));
        assertEquals(SUSPICIOUS, verdict("file://" + ssh + "/known_hosts"));
    }

    /** A name that only begins like a protected directory is not one. */
    @Test
    void aNameThatOnlyBeginsLikeOneIsNot() {
        String[] others = {"/etcetera", "/etcetera/a.txt", "/etc.txt", "/procedures/a.txt", "/sysadmin/a.txt",
                "/devices/a.txt", "/data/etc", "/data/dev/a.txt",
                "/home/u/.sshd", "/home/u/.sshd/a.txt", "/home/u/.ssh2", "/home/u/a.ssh", "/home/u/.gnupg.bak",
                "/home/u/.awsome/a.txt", "/home/u/.dockerignore", "/home/u/ssh", "/home/u/aws/a.txt"};
        for (String path : others) {
            assertEquals(PASSED, verdict("file://" + path), path);
        }
    }
}
