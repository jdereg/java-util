package com.cedarsoftware.io;

import java.util.Map;

import com.cedarsoftware.util.CompactMap;
import com.cedarsoftware.util.ReflectionUtils;
import com.cedarsoftware.io.TypeHolder;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CompactMapCustomTypeTest {

    @Test
    void testCompactMapWithCustomMapType() {
        CompactMap<String, String> map = CompactMap.<String, String>builder()
                .caseSensitive(true)
                .compactSize(42)
                .singleValueKey("code")
                .noOrder()
                .mapType(CustomTestMap.class)
                .build();

        map.put("one", "First");
        map.put("two", "Second");
        map.put("three", "Third");

        for (int i = 0; i < 50; i++) {
            map.put("key" + i, "value" + i);
        }

        Map<?, ?> newMap = (Map<?, ?>) ReflectionUtils.call(map, "getNewMap");
        assertInstanceOf(CustomTestMap.class, newMap, "New map should be a CustomTestMap");

        String json = JsonIo.toJson(map, null);
        CompactMap<String, String> restored = JsonIo.toJava(json, null)
                .asType(new TypeHolder<CompactMap<String, String>>(){});

        assertEquals(53, restored.size());
        assertEquals("First", restored.get("one"));
        assertEquals("Second", restored.get("two"));
        assertEquals("Third", restored.get("three"));
        assertEquals(false, ReflectionUtils.call(restored, "isCaseInsensitive"));
        assertEquals(42, ReflectionUtils.call(restored, "compactSize"));
        assertEquals("code", ReflectionUtils.call(restored, "getSingleValueKey"));

        Map<?, ?> restoredNewMap = (Map<?, ?>) ReflectionUtils.call(restored, "getNewMap");
        assertInstanceOf(CustomTestMap.class, restoredNewMap, "Restored map should create CustomTestMap instances");
    }
}

