package com.cedarsoftware.util;

import java.io.InputStream;
import java.lang.reflect.Method;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A file URL is judged by the path it opens, whatever its spelling. The checks of IOUtilities compared the path
 * as written in the URL, and the same file has many spellings: a percent-escape for any character
 * ({@code /%65tc/hosts}, {@code /etc%2Fhosts}, {@code /tmp/%2e%2e/etc/hosts}), more slashes
 * ({@code //etc/hosts}), a "." segment ({@code /./etc/hosts}), a slash at the end ({@code /backup/shadow/}).
 * Each of those was read where the plain spelling was refused.
 * <p>
 * The oracle of the main test is that sentence: a respelled path gets the verdict of the path. The tests with
 * real files show what the JDK does with each spelling -- it opens the same file.
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
class IOUtilitiesPathSpellingTest {

    private static final String SYSTEM = "refused: File URL accesses system path";
    private static final String SUSPICIOUS = "refused: Suspicious file path detected";
    private static final String TRAVERSAL = "refused: Dangerous path patterns detected in file URL";
    private static final String JAR_TRAVERSAL = "refused: Dangerous path patterns detected in jar URL";
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

    /** The text of the file a URL opens, or the verdict when IOUtilities refuses it. */
    private static String read(String url) throws Exception {
        try {
            URLConnection connection = new URL(url).openConnection();
            try (InputStream in = IOUtilities.getInputStream(connection)) {
                return new String(IOUtilities.inputStreamToBytes(in), "UTF-8");
            }
        } catch (SecurityException e) {
            return verdict(url);
        }
    }

    // ---------------------------------------------------------------- one spelling at a time

    @Test
    void anEscapedCharacterIsTheCharacter() {
        assertEquals(SYSTEM, verdict("file:///%65tc/hosts"));
        assertEquals(SYSTEM, verdict("file:///etc%2Fhosts"));
        assertEquals(SYSTEM, verdict("file:///etc%2fhosts"));
        assertEquals(SYSTEM, verdict("file:///%64%65%76/null"));
        assertEquals(SYSTEM, verdict("file:///%70rivate/etc/hosts"));
        assertEquals(TRAVERSAL, verdict("file:///tmp/%2e%2e/etc/hosts"));
        assertEquals(TRAVERSAL, verdict("file:///tmp/.%2E/etc/hosts"));
        assertEquals(TRAVERSAL, verdict("file:///tmp/a%00.txt"));
        assertEquals(SUSPICIOUS, verdict("file:///home/u/%2Essh/authorized_keys"));
        assertEquals(SUSPICIOUS, verdict("file:///home/u/.ssh%2Fauthorized_keys"));
        assertEquals(SUSPICIOUS, verdict("file:///home/u/.a%77s/credentials"));
        assertEquals(SUSPICIOUS, verdict("file:///home/u/id%5Frsa"));
        assertEquals(SUSPICIOUS, verdict("file:///home/u/backup/sh%61dow"));
        assertEquals(SUSPICIOUS, verdict("file:///opt/app/ssl/%70rivate/server.pem"));
    }

    @Test
    void dotSegmentsAndTheSlashAtTheEnd() {
        assertEquals(SYSTEM, verdict("file:///./etc/hosts"));
        assertEquals(SYSTEM, verdict("file:///././dev/null"));
        assertEquals(SYSTEM, verdict("file:///./private/./etc/hosts"));
        assertEquals(SUSPICIOUS, verdict("file:///home/u/backup/shadow/"));
        assertEquals(SUSPICIOUS, verdict("file:///home/u/backup/passwd/."));
        assertEquals(SUSPICIOUS, verdict("file:///home/u/backup/passwd//"));
        assertEquals(SUSPICIOUS, verdict("file:///home/u/.gnupg/."));
        assertEquals(SUSPICIOUS, verdict("file:///private/./tmp/keys/private.key"));
    }

    /** Where the separator is a slash, more of them at the start are still the root. */
    @Test
    @DisabledOnOs(OS.WINDOWS)
    void moreSlashesAtTheStart() {
        assertEquals(SYSTEM, verdict("file:////etc/hosts"));
        assertEquals(SYSTEM, verdict("file://///etc/hosts"));
        assertEquals(SYSTEM, verdict("file:///%2Fetc/hosts"));
        assertEquals(SYSTEM, verdict("file:////private//etc/hosts"));
        assertEquals(SYSTEM, verdict("file:////dev"));
    }

    /** What was read before is read now: the spelling changes no verdict into a refusal. */
    @Test
    void aPathThatPassesStillPasses() {
        String[] urls = {"file:///home/u/project/target/classes/", "file:///home/u/project/./data.json",
                "file:///home/u//project/data.json", "file:///home/u/My%20Project/data.json",
                "file:///home/u/caf%C3%A9/menu.json", "file:///home/u/a+b/c%2Bd.txt", "file:///home/u/100%25/x.txt",
                "file:///", "file:///tmp/", "file:///home/u/%252e%252e/data.txt", "file:///home/u/%2565tc/hosts",
                "file:etc/app.json", "file:./etc//app.json", "file:dev/notes.txt", "file:etc"};
        for (String url : urls) {
            assertEquals(PASSED, verdict(url), url);
        }
    }

    @Test
    void aJarUrlIsJudgedWithItsEscapesResolved() {
        assertEquals(JAR_TRAVERSAL, verdict("jar:file:///tmp/%2e%2e/etc/x.jar!/a.txt"));
        assertEquals(JAR_TRAVERSAL, verdict("jar:file:///tmp/lib/x.jar!/a%2F%2E%2E%2Fb.txt"));
        assertEquals(JAR_TRAVERSAL, verdict("jar:file:///tmp/lib/../x.jar!/a.txt"));
        assertEquals(PASSED, verdict("jar:file:///tmp/lib/x.jar!/com/acme/a%20b.txt"));
        assertEquals(PASSED, verdict("jar:file:///tmp/my%20lib/x.jar!/com/acme/a.txt"));
    }

    // ---------------------------------------------------------------- every spelling of many paths

    private static final String[] ROOTS = {"/tmp", "/var", "/etc", "/home/u", "/dev", "/opt/app", "/private/tmp"};
    private static final String[] NAMES = {"work", "data.txt", "T", "a.json", ".ssh", ".aws", ".docker", "private",
            "private.key", "id_rsa", "passwd", "shadow", "..", "etc", "dev", "x.pem", "classes", "My-Project"};
    private static final char[] HEX_UPPER = "0123456789ABCDEF".toCharArray();
    private static final char[] HEX_LOWER = "0123456789abcdef".toCharArray();

    private static String escape(char c, Random random) {
        char[] hex = random.nextBoolean() ? HEX_UPPER : HEX_LOWER;
        return "%" + hex[c >> 4] + hex[c & 15];
    }

    /** Another spelling of a path: escapes, doubled slashes, "." segments, and something at the end. */
    private static String respell(String path, Random random) {
        StringBuilder spelled = new StringBuilder();
        for (int i = 0; i < path.length(); i++) {
            char c = path.charAt(i);
            int choice = random.nextInt(10);
            if (i == 0) {
                spelled.append(choice == 0 ? "/./" : "/");      // the slashes at the start have their own test
            } else if (c == '/') {
                spelled.append(choice == 0 ? "//" : choice == 1 ? "/./" : choice == 2 ? escape(c, random)
                        : choice == 3 ? "/.//" : "/");
            } else {
                spelled.append(choice < 3 ? escape(c, random) : String.valueOf(c));
            }
        }
        int end = random.nextInt(6);
        return spelled.append(end == 0 ? "/" : end == 1 ? "/." : end == 2 ? "//" : "").toString();
    }

    /**
     * 800 paths from a fixed seed -- some that pass, some refused for each of the three reasons -- and three other
     * spellings of each. Every spelling gets the verdict of the path.
     */
    @Test
    void everySpellingGetsTheVerdictOfThePath() {
        Random random = new Random(20261014L);
        Map<String, Integer> verdicts = new LinkedHashMap<>();
        int respelled = 0;
        for (int n = 0; n < 800; n++) {
            StringBuilder plain = new StringBuilder(ROOTS[random.nextInt(ROOTS.length)]);
            for (int i = random.nextInt(4); i > 0; i--) {
                plain.append('/').append(NAMES[random.nextInt(NAMES.length)]);
            }
            String path = plain.toString();
            String expected = verdict("file://" + path);
            verdicts.merge(expected, 1, Integer::sum);

            for (int i = 0; i < 3; i++) {
                String spelling = respell(path, random);
                assertEquals(expected, verdict("file://" + spelling), path + " spelled " + spelling);
                respelled += spelling.equals(path) ? 0 : 1;
            }
        }
        for (String verdict : new String[] {PASSED, SYSTEM, SUSPICIOUS, TRAVERSAL}) {
            assertTrue(verdicts.getOrDefault(verdict, 0) >= 40, verdicts.toString());
        }
        assertTrue(respelled > 2300, respelled + " respelled");
    }

    // ---------------------------------------------------------------- what the JDK opens

    /** Each spelling of a file that passes opens that file. */
    @Test
    void theJdkOpensTheSameFileUnderEverySpelling(@TempDir Path temp) throws Exception {
        Path directory = temp.toRealPath();
        Files.write(directory.resolve("data.txt"), "data".getBytes("UTF-8"));
        Files.write(directory.resolve("a+b.txt"), "plus".getBytes("UTF-8"));
        Files.write(Files.createDirectory(directory.resolve("%2e%2e")).resolve("data.txt"), "once".getBytes("UTF-8"));
        String base = "file://" + directory;

        for (String name : new String[] {"/data.txt", "//data.txt", "/./data.txt", "/data%2Etxt", "/%64ata.txt",
                "/data.txt/", "%2Fdata.txt"}) {
            assertEquals("data", read(base + name), name);
        }
        assertEquals("plus", read(base + "/a+b.txt"));
        assertEquals("plus", read(base + "/a%2Bb.txt"));
        // An escape is resolved once: %252e is the three characters %2e, the name of a directory here
        assertEquals("once", read(base + "/%252e%252e/data.txt"));
    }

    /** Each spelling of a file that is refused is refused: before, these returned the content. */
    @Test
    void aRefusedFileIsNotReadUnderAnotherSpelling(@TempDir Path temp) throws Exception {
        Path directory = temp.toRealPath();
        Files.write(directory.resolve("shadow"), "secret".getBytes("UTF-8"));
        Files.write(Files.createDirectory(directory.resolve(".ssh")).resolve("key"), "secret".getBytes("UTF-8"));
        String base = "file://" + directory;

        for (String name : new String[] {"/shadow", "/shadow/", "/./shadow/", "/shadow/.", "/sh%61dow", "/%73hadow/",
                "/.ssh/key", "/.ssh%2Fkey", "/%2Essh/key", "/%2e%73%73%68%2fkey", "/.ssh/", "/.ssh/.", "/%2Essh"}) {
            assertEquals(SUSPICIOUS, read(base + name), name);
        }
    }

    // ---------------------------------------------------------------- the two helpers, exactly

    private static String call(String method, Object... arguments) throws Exception {
        Class<?>[] types = new Class<?>[arguments.length];
        for (int i = 0; i < arguments.length; i++) {
            types[i] = arguments[i] instanceof Character ? char.class : String.class;
        }
        Method helper = IOUtilities.class.getDeclaredMethod(method, types);
        helper.setAccessible(true);
        return (String) helper.invoke(null, arguments);
    }

    /** As the JDK resolves them: UTF-8, once, a plus sign stays one; what cannot be resolved is left as written. */
    @Test
    void percentEscapesAreResolvedAsTheJdkResolvesThem() throws Exception {
        assertEquals("/a+b c+d", call("percentDecoded", "/a+b%20c%2Bd"));
        assertEquals("/caf\u00e9/\u20ac", call("percentDecoded", "/caf%C3%A9/%E2%82%AC"));
        assertEquals("/%2e%2e", call("percentDecoded", "/%252e%252e"));
        assertEquals("/a%zz", call("percentDecoded", "/a%zz"));
        assertEquals("/a%2", call("percentDecoded", "/a%2"));
        assertEquals("/%2e%zz", call("percentDecoded", "/%2e%zz"));
        String plain = "/home/u/data.txt";
        assertSame(plain, call("percentDecoded", plain));
    }

    /** On Windows two slashes at the start name a server, and a path to a server called dev is no system path. */
    @Test
    void whereTheSeparatorIsABackslashTwoSlashesAtTheStartStay() throws Exception {
        assertEquals("//dev/builds/app.json", call("asOpened", "//dev/builds//app.json", '\\'));
        assertEquals("//dev/builds/app.json", call("asOpened", "////dev/./builds/app.json/", '\\'));
        assertEquals("/C:/data/app.json", call("asOpened", "/C:/data/./app.json", '\\'));
        assertEquals("/dev/builds/app.json", call("asOpened", "//dev/builds//app.json", '/'));
        assertEquals("/", call("asOpened", "//", '/'));
        assertEquals("", call("asOpened", "./", '/'));
        assertEquals("etc/app.json", call("asOpened", "./etc//app.json", '/'));
    }
}
