package com.cedarsoftware.util;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import com.cedarsoftware.util.convert.Converter;
import com.cedarsoftware.util.convert.DefaultConverterOptions;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledForJreRange;
import org.junit.jupiter.api.condition.JRE;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * ReflectionUtils lists a record class's components, names and accessors once, the accessors made accessible, and
 * DeepEquals and the Converter read records through it; they each looked the components up again, reflectively, for
 * every record they read. What callers see must not change: the answers below held before the cache, apart from
 * {@link ReflectionUtils#getRecordComponentValues(Object)}, which is new, and a record that is not public, which the
 * previous commit made readable.
 */
@EnabledForJreRange(min = JRE.JAVA_16)
class ReflectionUtilsRecordTest {

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

    @Test
    void isRecordIsTrueOnlyForRecords() throws Exception {
        assertTrue(ReflectionUtils.isRecord(records.make("pkg", 1, 2).getClass()));
        assertTrue(ReflectionUtils.isRecord(records.make("priv", 1, "a").getClass()));
        assertTrue(ReflectionUtils.isRecord(records.make("pub", 1, 2).getClass()));
        assertFalse(ReflectionUtils.isRecord(String.class));
        assertFalse(ReflectionUtils.isRecord(HashMap.class));
        assertFalse(ReflectionUtils.isRecord(Object.class));
        assertFalse(ReflectionUtils.isRecord(int.class));
        assertFalse(ReflectionUtils.isRecord(Runnable.class));
        assertFalse(ReflectionUtils.isRecord(null));
    }

    @Test
    void getRecordComponentsHandsOutANewArrayInDeclarationOrder() throws Exception {
        Class<?> point = records.make("pkg", 1, 2).getClass();
        Object[] first = ReflectionUtils.getRecordComponents(point);
        Object[] second = ReflectionUtils.getRecordComponents(point);
        assertNotSame(first, second);
        assertEquals(Arrays.asList("x", "y"), names(first));
        first[0] = null;
        assertEquals(Arrays.asList("x", "y"), names(ReflectionUtils.getRecordComponents(point)));
        assertNull(ReflectionUtils.getRecordComponents(String.class));
    }

    @Test
    void getRecordComponentValuesReadsEveryComponentInOrder() throws Exception {
        Map<String, Object> expected = new LinkedHashMap<>();
        expected.put("x", 5);
        expected.put("y", 6);
        assertEquals(new ArrayList<>(expected.entrySet()),
                new ArrayList<>(ReflectionUtils.getRecordComponentValues(records.make("pkg", 5, 6)).entrySet()));
        assertEquals(expected, ReflectionUtils.getRecordComponentValues(records.make("pub", 5, 6)));
        Map<String, Object> priv = ReflectionUtils.getRecordComponentValues(records.make("priv", 7, "q"));
        assertEquals(7, priv.get("x"));
        assertEquals("q", priv.get("s"));
    }

    @Test
    void getRecordComponentValuesSaysWhatItCannotRead() throws Exception {
        IllegalArgumentException notRecord = assertThrows(IllegalArgumentException.class,
                () -> ReflectionUtils.getRecordComponentValues("text"));
        assertTrue(notRecord.getMessage().contains("java.lang.String"), notRecord.getMessage());
        IllegalArgumentException unreadable = assertThrows(IllegalArgumentException.class,
                () -> ReflectionUtils.getRecordComponentValues(records.make("throwing", 1)));
        assertTrue(unreadable.getMessage().contains("'x'"), unreadable.getMessage());
        assertNull(ReflectionUtils.getRecordComponentValue(ReflectionUtils.getRecordComponents(
                records.make("throwing", 1).getClass())[0], records.make("throwing", 1)));
    }

    @Test
    void theConverterTurnsARecordThatIsNotPublicIntoAMap() throws Exception {
        Converter converter = new Converter(new DefaultConverterOptions());
        Map<?, ?> map = converter.convert(records.make("pkg", 3, 4), Map.class);
        assertEquals(Arrays.asList("x", "y"), new ArrayList<>(map.keySet()));
        assertEquals(3, map.get("x"));
        assertEquals(4, map.get("y"));
        assertThrows(IllegalArgumentException.class, () -> converter.convert(records.make("throwing", 1), Map.class));
    }

    private static java.util.List<String> names(Object[] components) {
        java.util.List<String> names = new ArrayList<>();
        for (Object component : components) {
            names.add(ReflectionUtils.getRecordComponentName(component));
        }
        return names;
    }
}
