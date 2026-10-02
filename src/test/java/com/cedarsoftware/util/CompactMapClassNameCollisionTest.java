package com.cedarsoftware.util;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A builder map's class is named by the first 64 bits of its configuration's SHA-1, and a class found by that name was
 * used as it was: two configurations sharing a name would share a class, so a map would quietly take another
 * configuration's compact size, single-value key, ordering or case sensitivity. A class found by name is now used
 * only if it was made for the configuration asked for; otherwise the configuration gets another name. Finding two
 * configurations whose names collide is impractical, so the test makes the class under one configuration's name
 * another configuration's -- its compact size, and the record of what it was made for -- which is what a collision
 * looks like to the lookup.
 */
class CompactMapClassNameCollisionTest {

    private static CompactMap.Builder<String, Object> configuration() {
        return CompactMap.<String, Object>builder().compactSize(9).singleValueKey("collisionTestA");
    }

    @SuppressWarnings("unchecked")
    private static Map<String, String> configurationByClassName() throws Exception {
        Field field = Class.forName(CompactMap.class.getName() + "$TemplateGenerator")
                .getDeclaredField("CONFIGURATION_BY_CLASS_NAME");
        field.setAccessible(true);
        return (Map<String, String>) field.get(null);
    }

    /** Makes the class another configuration's, with compact size {@code size}; returns how to undo it. */
    private static Runnable makeAnotherConfigurations(Class<?> clazz, int size) throws Exception {
        Map<String, String> configurations = configurationByClassName();
        String configuration = configurations.get(clazz.getName());
        assertTrue(configuration.contains(";sz:9;"), configuration);
        Field compactSize = clazz.getDeclaredField("_compactSize");
        compactSize.setAccessible(true);
        compactSize.setInt(null, size);
        configurations.put(clazz.getName(), configuration.replace(";sz:9;", ";sz:" + size + ";"));
        return () -> {
            try {
                compactSize.setInt(null, 9);
            } catch (IllegalAccessException e) {
                throw new IllegalStateException(e);
            }
            configurations.put(clazz.getName(), configuration);
        };
    }

    @Test
    void aClassFoundByNameIsUsedOnlyIfItWasMadeForTheConfigurationAskedFor() throws Exception {
        Class<?> named = configuration().build().getClass();
        Runnable restore = makeAnotherConfigurations(named, 4);
        try {
            // a source map makes the builder look the class up by name instead of in its cache of built ones
            CompactMap<String, Object> map = configuration().sourceMap(new HashMap<>()).build();
            assertNotSame(named, map.getClass());
            assertEquals(9, map.getConfig().get(CompactMap.COMPACT_SIZE));
            for (int i = 0; i < 9; i++) {
                map.put("k" + i, i);
            }
            assertEquals(CompactMap.LogicalValueType.ARRAY, map.getLogicalValueType());
            map.put("k9", 9);
            assertEquals(CompactMap.LogicalValueType.MAP, map.getLogicalValueType());

            // the other name is found again, not generated again
            int configurations = CompactMap.generatedConfigurationCount();
            assertSame(map.getClass(), configuration().sourceMap(new HashMap<>()).build().getClass());
            assertEquals(configurations, CompactMap.generatedConfigurationCount());

            // and if that name were taken too, a third
            Runnable restoreSecond = makeAnotherConfigurations(map.getClass(), 5);
            try {
                CompactMap<String, Object> third = configuration().sourceMap(new HashMap<>()).build();
                assertNotSame(named, third.getClass());
                assertNotSame(map.getClass(), third.getClass());
                assertEquals(9, third.getConfig().get(CompactMap.COMPACT_SIZE));
            } finally {
                restoreSecond.run();
            }
        } finally {
            restore.run();
        }
    }

    @Test
    void aClassBuiltBeforeIsStillTheOneBuilt() {
        Class<?> first = configuration().build().getClass();
        assertSame(first, configuration().build().getClass());
        assertSame(first, configuration().sourceMap(new HashMap<>()).build().getClass());
    }
}
