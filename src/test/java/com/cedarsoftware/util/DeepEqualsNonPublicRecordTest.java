package com.cedarsoftware.util;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledForJreRange;
import org.junit.jupiter.api.condition.JRE;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * DeepEquals compares a record by its components, read through their accessors. ReflectionUtils invoked an accessor
 * without making it accessible, so for a record that is not public -- package-private, or nested and private, the
 * usual ways to declare one -- every read failed and read as null, and any two records of the type compared equal.
 * The accessor is now made accessible. A component still unreadable (its accessor throws) leaves the answer to the
 * record's equals(), as for a field DeepEquals cannot read. The records are compiled at run time, since java-util
 * compiles for Java 8.
 */
@EnabledForJreRange(min = JRE.JAVA_16)
class DeepEqualsNonPublicRecordTest {

    @TempDir
    static Path dir;
    private static CompiledRecords records;

    @BeforeAll
    static void compileRecords() throws Exception {
        records = new CompiledRecords(dir);
    }

    @AfterAll
    static void closeLoader() throws Exception {
        if (records != null) {
            records.close();
        }
    }

    private static Object make(String what, Object... args) throws Exception {
        return records.make(what, args);
    }

    @Test
    void recordsThatAreNotPublicAreComparedByTheirComponents() throws Exception {
        assertFalse(DeepEquals.deepEquals(make("pkg", 1, 2), make("pkg", 3, 4)));
        assertTrue(DeepEquals.deepEquals(make("pkg", 1, 2), make("pkg", 1, 2)));
        assertFalse(DeepEquals.deepEquals(make("priv", 1, "a"), make("priv", 1, "b")));
        assertTrue(DeepEquals.deepEquals(make("priv", 1, "a"), make("priv", 1, "a")));

        // Compared deeply by component: equal arrays are equal, which the record's own equals() would deny
        assertTrue(DeepEquals.deepEquals(make("arr", new int[]{1, 2}), make("arr", new int[]{1, 2})));
        assertFalse(DeepEquals.deepEquals(make("arr", new int[]{1, 2}), make("arr", new int[]{1, 3})));

        Map<String, Object> options = new HashMap<>();
        assertFalse(DeepEquals.deepEquals(make("pkg", 1, 2), make("pkg", 1, 9), options));
        String diff = (String) options.get(DeepEquals.DIFF);
        assertTrue(diff.contains(".y") && diff.contains("Expected: 2"), diff);
    }

    @Test
    void recordsThatAreNotPublicAreComparedInsideContainers() throws Exception {
        assertFalse(DeepEquals.deepEquals(Arrays.asList(make("pkg", 1, 2)), Arrays.asList(make("pkg", 3, 4))));
        assertFalse(DeepEquals.deepEquals(Collections.singletonMap("k", make("priv", 1, "a")),
                Collections.singletonMap("k", make("priv", 2, "b"))));
        assertTrue(DeepEquals.deepEquals(Collections.singleton(make("pkg", 1, 2)),
                Collections.singleton(make("pkg", 1, 2))));
    }

    @Test
    void aComponentWhoseAccessorThrowsLeavesTheAnswerToTheRecordsEquals() throws Exception {
        assertTrue(DeepEquals.deepEquals(make("throwing", 1), make("throwing", 1)));

        Map<String, Object> options = new HashMap<>();
        assertFalse(DeepEquals.deepEquals(make("throwing", 1), make("throwing", 2), options));
        String diff = (String) options.get(DeepEquals.DIFF);
        assertTrue(diff.contains("could not be read"), diff);

        Map<String, Object> skip = new HashMap<>();
        skip.put(DeepEquals.SKIP_UNREADABLE_FIELDS, true);
        assertTrue(DeepEquals.deepEquals(make("throwing", 1), make("throwing", 2), skip));
    }

    @Test
    void aPublicRecordIsComparedAsBefore() throws Exception {
        assertFalse(DeepEquals.deepEquals(make("pub", 1, 2), make("pub", 3, 4)));
        assertTrue(DeepEquals.deepEquals(make("pub", 1, 2), make("pub", 1, 2)));
    }

    @Test
    void getRecordComponentValueReadsARecordThatIsNotPublic() throws Exception {
        Object point = make("pkg", 5, 6);
        Object[] components = ReflectionUtils.getRecordComponents(point.getClass());
        assertEquals(5, ReflectionUtils.getRecordComponentValue(components[0], point));
        assertEquals(6, ReflectionUtils.getRecordComponentValue(components[1], point));
    }
}
