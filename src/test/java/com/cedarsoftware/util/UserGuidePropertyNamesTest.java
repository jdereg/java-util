package com.cedarsoftware.util;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The system properties the user guide and the README name are ones java-util reads, and the guide's examples turn
 * the limits on. The guide named eight properties nothing reads -- {@code bytes.max.*} for
 * {@code byteutilities.max.*}, {@code math.max.*} for {@code mathutilities.max.*}, and three limits that do not exist
 * -- so whoever set them, as the guide said to, had no limit. Its ByteUtilities and MathUtilities examples also left
 * out the master switch, without which no limit applies.
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
class UserGuidePropertyNamesTest {

    // Where the documents name a property: a table row, a setProperty / getProperty call, -Dname=, or a line of a
    // .properties block
    private static final Pattern[] NAMED = {
            Pattern.compile("^\\|\\s*`([a-z][a-zA-Z0-9]*(?:\\.[a-zA-Z0-9]+)+)`\\s*\\|", Pattern.MULTILINE),
            Pattern.compile("(?:setProperty|getProperty|clearProperty)\\(\\s*\"([^\"]+)\""),
            Pattern.compile("-D([a-z][a-zA-Z0-9]*(?:\\.[a-zA-Z0-9]+)+)="),
            Pattern.compile("^\\s*([a-z][a-zA-Z0-9]*(?:\\.[a-zA-Z0-9]+){2,})\\s*=\\s*\\S+\\s*$", Pattern.MULTILINE)};
    // The JDK's own properties, which the documents may name too
    private static final String[] JDK_PREFIXES = {"java.", "javax.", "jdk.", "sun.", "user.", "os.", "file.", "line.",
            "path."};
    private static final Pattern SET_PROPERTY = Pattern.compile("System\\.setProperty\\(\"([^\"]+)\",\\s*\"([^\"]+)\"\\)");

    @Test
    void everyPropertyTheDocumentsNameIsOneJavaUtilReads() throws IOException {
        Set<String> literals = stringLiteralsOfMainSources();
        List<String> unread = new ArrayList<>();
        for (String document : new String[] {"userguide.md", "README.md"}) {
            Set<String> names = new TreeSet<>();
            String text = read(document);
            for (Pattern pattern : NAMED) {
                Matcher matcher = pattern.matcher(text);
                while (matcher.find()) {
                    names.add(matcher.group(1));
                }
            }
            assertTrue(names.size() > 10, document + " names " + names.size() + " properties");
            for (String name : names) {
                if (!isJdks(name) && !literals.contains(name)) {
                    unread.add(document + ": " + name);
                }
            }
        }
        assertTrue(unread.isEmpty(), "named in the documents, read by nothing: " + unread);
    }

    /** The guide's ByteUtilities example, applied as written, limits what decode() reads. */
    @Test
    void theGuidesByteUtilitiesExampleLimitsDecode() throws IOException {
        List<String[]> settings = firstExampleOf("## ByteUtilities");
        int limit = Integer.parseInt(valueOf(settings, "byteutilities.max.hex.string.length"));
        withProperties(settings, () -> assertThrows(SecurityException.class,
                () -> ByteUtilities.decode(repeat("A", limit + 2))));
    }

    /** The guide's MathUtilities example, applied as written, limits the arrays minimum() takes. */
    @Test
    void theGuidesMathUtilitiesExampleLimitsArrays() throws IOException {
        List<String[]> settings = firstExampleOf("## MathUtilities");
        int limit = Integer.parseInt(valueOf(settings, "mathutilities.max.array.size"));
        withProperties(settings, () -> assertThrows(SecurityException.class,
                () -> MathUtilities.minimum(new long[limit + 1])));
    }

    /** The System.setProperty calls of the first group (up to a blank line) of the section's first java example. */
    private static List<String[]> firstExampleOf(String heading) throws IOException {
        String guide = read("userguide.md");
        int section = guide.indexOf("\n" + heading + "\n");
        assertTrue(section >= 0, heading);
        int code = guide.indexOf("```java", guide.indexOf("**Usage Examples:**", section));
        String group = guide.substring(code, guide.indexOf("\n\n", code));
        List<String[]> settings = new ArrayList<>();
        Matcher matcher = SET_PROPERTY.matcher(group);
        while (matcher.find()) {
            settings.add(new String[] {matcher.group(1), matcher.group(2)});
        }
        assertTrue(!settings.isEmpty(), heading + " example sets nothing");
        return settings;
    }

    private static String valueOf(List<String[]> settings, String name) {
        for (String[] setting : settings) {
            if (setting[0].equals(name)) {
                return setting[1];
            }
        }
        throw new AssertionError("the example does not set " + name + ": it sets " + names(settings));
    }

    private static List<String> names(List<String[]> settings) {
        List<String> names = new ArrayList<>();
        for (String[] setting : settings) {
            names.add(setting[0]);
        }
        return names;
    }

    private static void withProperties(List<String[]> settings, Runnable check) {
        List<String[]> before = new ArrayList<>();
        for (String[] setting : settings) {
            before.add(new String[] {setting[0], System.getProperty(setting[0])});
            System.setProperty(setting[0], setting[1]);
        }
        try {
            check.run();
        } finally {
            for (String[] setting : before) {
                if (setting[1] == null) {
                    System.clearProperty(setting[0]);
                } else {
                    System.setProperty(setting[0], setting[1]);
                }
            }
        }
    }

    private static boolean isJdks(String name) {
        for (String prefix : JDK_PREFIXES) {
            if (name.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    private static Set<String> stringLiteralsOfMainSources() throws IOException {
        Set<String> literals = new HashSet<>();
        Pattern literal = Pattern.compile("\"([a-zA-Z0-9_.]+)\"");
        try (Stream<Path> files = Files.walk(Paths.get("src", "main", "java"))) {
            for (Path file : (Iterable<Path>) files.filter(p -> p.toString().endsWith(".java"))::iterator) {
                Matcher matcher = literal.matcher(new String(Files.readAllBytes(file), StandardCharsets.UTF_8));
                while (matcher.find()) {
                    literals.add(matcher.group(1));
                }
            }
        }
        assertTrue(literals.contains("byteutilities.security.enabled"), "read the main sources");
        return literals;
    }

    private static String read(String document) throws IOException {
        return new String(Files.readAllBytes(Paths.get(document)), StandardCharsets.UTF_8);
    }

    private static String repeat(String s, int count) {
        StringBuilder out = new StringBuilder(s.length() * count);
        for (int i = 0; i < count; i++) {
            out.append(s);
        }
        return out.toString();
    }
}
