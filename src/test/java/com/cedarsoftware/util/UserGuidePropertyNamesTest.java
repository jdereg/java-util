package com.cedarsoftware.util;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The system properties the user guide and the README name are ones java-util reads, and the guide's examples turn
 * the limits on. The guide named eight properties nothing reads -- {@code bytes.max.*} for
 * {@code byteutilities.max.*}, {@code math.max.*} for {@code mathutilities.max.*}, and three limits that do not exist
 * -- so whoever set them, as the guide said to, had no limit. Its ByteUtilities and MathUtilities examples also left
 * out the master switch, without which no limit applies.
 * <p>
 * The guide ends with the "System Property Reference", the one list of every property (4.119.0; it replaced a
 * separate file, features.md). That list is held to the code from the other side as well: a property the code reads
 * is in it, and where a test can try a limit, the default the list states is the one applied.
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

    // The guide's last section: one row for each property java-util reads -- | `name` | default | what it does |
    private static final String REFERENCE = "\n## System Property Reference\n";
    private static final Pattern REFERENCE_ROW = Pattern.compile("^\\| `([^`]+)` \\| ([^|]*) \\|", Pattern.MULTILINE);
    private static final Pattern PROPERTY_NAME = Pattern.compile("[a-z][a-zA-Z0-9]*(?:\\.[a-zA-Z0-9]+)+");
    // How main code hands a name to what reads a system property: System.getProperty and its typed cousins, a helper
    // named ...Property(...), or a constant named ...PROP...
    private static final Pattern READ = Pattern.compile(
            "(?:getProperty|getBoolean|getInteger|getLong|\\w*Property\\w*)\\(\\s*\"([^\"]+)\""
                    + "|\\b[A-Z0-9_]*PROP[A-Z0-9_]*\\s*=\\s*\"([^\"]+)\"");
    private static final Pattern LITERAL = Pattern.compile("\"([^\"]+)\"");
    // Read by java-util and not its own: LoggingConfig looks for these to tell a test run
    private static final Set<String> BUILD_TOOLS = new HashSet<>(Arrays.asList("maven.test.skip",
            "surefire.test.class.path"));

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

    /**
     * The reference that ends the guide lists every property java-util reads, once. A property is one a main source
     * hands to System.getProperty (or Boolean.getBoolean, Integer.getInteger, Long.getLong), to a helper named
     * ...Property(...), or keeps in a constant named ...PROP...; and any other literal that begins like a listed
     * one ("traverser.", "io.", ...), which is how a helper of another name -- Traverser's limit("...") -- is seen.
     * <p>
     * The list this replaced, features.md, had no such test: written once, it lacked 16 of the properties the code
     * read and named three the code did not.
     */
    @Test
    void everyPropertyJavaUtilReadsIsInTheGuidesReference() throws IOException {
        Map<String, String> rows = referenceRows();
        Set<String> families = new HashSet<>();
        for (String name : rows.keySet()) {
            families.add(name.substring(0, name.indexOf('.') + 1));
        }

        Set<String> unlisted = new TreeSet<>();
        try (Stream<Path> files = Files.walk(Paths.get("src", "main", "java"))) {
            for (Path file : (Iterable<Path>) files.filter(p -> p.toString().endsWith(".java"))::iterator) {
                String code = withoutCommentLines(new String(Files.readAllBytes(file), StandardCharsets.UTF_8));
                Set<String> read = new TreeSet<>();
                Matcher matcher = READ.matcher(code);
                while (matcher.find()) {
                    read.add(matcher.group(1) != null ? matcher.group(1) : matcher.group(2));
                }
                matcher = LITERAL.matcher(code);
                while (matcher.find()) {
                    String literal = matcher.group(1);
                    int dot = literal.indexOf('.');
                    if (dot > 0 && families.contains(literal.substring(0, dot + 1))) {
                        read.add(literal);
                    }
                }
                for (String name : read) {
                    if (PROPERTY_NAME.matcher(name).matches() && !isJdks(name) && !BUILD_TOOLS.contains(name)
                            && !rows.containsKey(name)) {
                        unlisted.add(file.getFileName() + ": " + name);
                    }
                }
            }
        }
        assertTrue(unlisted.isEmpty(), "read by java-util, missing from the guide's System Property Reference: "
                + unlisted);
    }

    /**
     * For the limits a test can try, the default the reference states is the one applied: with the property not set,
     * one past the stated default is refused, in a message that names that default. (A larger real default would
     * refuse nothing; a smaller one would name itself.) features.md stated "0 (disabled)" for ten of these.
     */
    @Test
    void theDefaultsTheReferenceStatesAreTheOnesApplied() throws IOException {
        Map<String, String> rows = referenceRows();

        // DeepEquals: no switch, and read once -- so read again here, with none of its properties set
        String[] deepEquals = {"deepequals.max.collection.size", "deepequals.max.array.size",
                "deepequals.max.map.size"};
        withPropertiesCleared(deepEquals, DeepEquals::reloadSecurityProperties, () -> {
            int size = defaultOf(rows, deepEquals[0]);
            refusedNaming(size, () -> DeepEquals.deepEquals(listOf(size + 1), listOf(size + 1)));
            int length = defaultOf(rows, deepEquals[1]);
            refusedNaming(length, () -> DeepEquals.deepEquals(new int[length + 1], new int[length + 1]));
            int entries = defaultOf(rows, deepEquals[2]);
            refusedNaming(entries, () -> DeepEquals.deepEquals(mapOf(entries + 1), mapOf(entries + 1)));
        });

        // ClassUtilities: the resource-name limit needs no switch. It is read once too, and a test that ran earlier
        // in this JVM may have left another limit in force, so it is read again here.
        withPropertiesCleared(new String[] {"classutilities.enhanced.security.enabled",
                "classutilities.max.resource.name.length"}, ClassUtilities::reinitializeSecuritySettings, () -> {
            int nameLength = defaultOf(rows, "classutilities.max.resource.name.length");
            refusedNaming(nameLength, () -> ClassUtilities.loadResourceAsBytes(repeat("a", nameLength + 1)));
        });

        // The rest wait for their master switch
        withMaster("byteutilities.security.enabled", new String[] {"byteutilities.max.hex.string.length",
                "byteutilities.max.array.size"}, () -> { }, () -> {
            int hex = defaultOf(rows, "byteutilities.max.hex.string.length");
            refusedNaming(hex, () -> ByteUtilities.decode(repeat("A", hex + 2)));
            int bytes = defaultOf(rows, "byteutilities.max.array.size");
            refusedNaming(bytes, () -> ByteUtilities.encode(new byte[bytes + 1]));
        });
        withMaster("mathutilities.security.enabled", new String[] {"mathutilities.max.array.size",
                "mathutilities.max.string.length", "mathutilities.max.permutation.size"}, () -> { }, () -> {
            int array = defaultOf(rows, "mathutilities.max.array.size");
            refusedNaming(array, () -> MathUtilities.minimum(new long[array + 1]));
            int text = defaultOf(rows, "mathutilities.max.string.length");
            refusedNaming(text, () -> MathUtilities.parseToMinimalNumericType(repeat("1", text + 1)));
            int list = defaultOf(rows, "mathutilities.max.permutation.size");
            refusedNaming(list, () -> MathUtilities.nextPermutation(listOf(list + 1)));
        });
        // DateUtilities reads its switches once, so they are read again around this
        List<String[]> dateSwitches = new ArrayList<>();
        dateSwitches.add(new String[] {"dateutilities.input.validation.enabled", "true"});
        withProperties(dateSwitches, () -> withMaster("dateutilities.security.enabled",
                new String[] {"dateutilities.max.input.length", "dateutilities.max.epoch.digits"},
                DateUtilities::resetSecurityConfigCacheForTesting, () -> {
                    int input = defaultOf(rows, "dateutilities.max.input.length");
                    refusedNaming(input, () -> DateUtilities.parseDate(repeat("x", input + 1)));
                    int digits = defaultOf(rows, "dateutilities.max.epoch.digits");
                    refusedNaming(digits, () -> DateUtilities.parseDate(repeat("1", digits + 1)));
                }));
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

    /** The reference's rows, each name with its Default cell. */
    private static Map<String, String> referenceRows() throws IOException {
        String guide = read("userguide.md");
        int start = guide.indexOf(REFERENCE);
        assertTrue(start >= 0, "userguide.md has the section" + REFERENCE.trim());
        int end = guide.indexOf("\n## ", start + REFERENCE.length());
        Map<String, String> rows = new LinkedHashMap<>();
        Matcher matcher = REFERENCE_ROW.matcher(guide.substring(start, end < 0 ? guide.length() : end));
        while (matcher.find()) {
            String earlier = rows.put(matcher.group(1), matcher.group(2).trim());
            assertTrue(earlier == null, matcher.group(1) + " is listed twice");
        }
        assertTrue(rows.size() > 80, "the reference lists " + rows.size() + " properties");
        return rows;
    }

    /** The source with its comment lines blanked: a name in Javadoc is not a name the code reads. */
    private static String withoutCommentLines(String source) {
        StringBuilder code = new StringBuilder(source.length());
        for (String line : source.split("\n", -1)) {
            String trimmed = line.trim();
            if (!trimmed.startsWith("*") && !trimmed.startsWith("//") && !trimmed.startsWith("/*")) {
                code.append(line);
            }
            code.append('\n');
        }
        return code.toString();
    }

    private static int defaultOf(Map<String, String> rows, String name) {
        String cell = rows.get(name);
        assertTrue(cell != null, name + " is in the reference");
        return Integer.parseInt(cell.replace("`", ""));
    }

    /** The call is refused, and the refusal names the limit: as a whole number, so 100 does not pass for 10. */
    private static void refusedNaming(int limit, Executable call) {
        SecurityException refusal = assertThrows(SecurityException.class, call);
        assertTrue(Pattern.compile("(?<!\\d)" + limit + "(?!\\d)").matcher(String.valueOf(refusal.getMessage())).find(),
                "a limit of " + limit + ": " + refusal.getMessage());
    }

    /** Runs the check with these properties not set; settings a class keeps are read again before and after. */
    private static void withPropertiesCleared(String[] names, Runnable readAgain, Runnable check) {
        Map<String, String> before = new LinkedHashMap<>();
        for (String name : names) {
            before.put(name, System.getProperty(name));
            System.clearProperty(name);
        }
        readAgain.run();
        try {
            check.run();
        } finally {
            for (Map.Entry<String, String> entry : before.entrySet()) {
                if (entry.getValue() != null) {
                    System.setProperty(entry.getKey(), entry.getValue());
                }
            }
            readAgain.run();
        }
    }

    /** The same, with the class's master switch on. */
    private static void withMaster(String master, String[] limits, Runnable readAgain, Runnable check) {
        List<String[]> on = new ArrayList<>();
        on.add(new String[] {master, "true"});
        withProperties(on, () -> withPropertiesCleared(limits, readAgain, check));
    }

    private static List<Integer> listOf(int size) {
        List<Integer> list = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            list.add(i);
        }
        return list;
    }

    private static Map<Integer, Integer> mapOf(int size) {
        Map<Integer, Integer> map = new HashMap<>(size * 2);
        for (int i = 0; i < size; i++) {
            map.put(i, i);
        }
        return map;
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
