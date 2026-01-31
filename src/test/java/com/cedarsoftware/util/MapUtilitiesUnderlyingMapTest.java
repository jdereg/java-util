package com.cedarsoftware.util;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MapUtilitiesUnderlyingMapTest {

    private Map<?, ?> invoke(Map<?, ?> map) throws Exception {
        Method m = ReflectionUtils.getMethod(MapUtilities.class, "getUnderlyingMap", Map.class);
        return (Map<?, ?>) m.invoke(null, map);
    }

    @Test
    public void nullInputReturnsNull() throws Exception {
        assertNull(invoke(null));
    }

    @Test
    public void detectsCircularDependency() throws Exception {
        CaseInsensitiveMap<String, String> ci = new CaseInsensitiveMap<>();
        TrackingMap<String, String> tracking = new TrackingMap<>(ci);
        Field mapField = ReflectionUtils.getField(CaseInsensitiveMap.class, "map");
        mapField.set(ci, tracking);

        Method m = ReflectionUtils.getMethod(MapUtilities.class, "getUnderlyingMap", Map.class);
        InvocationTargetException ex = assertThrows(InvocationTargetException.class, () -> m.invoke(null, ci));
        assertTrue(ex.getCause() instanceof IllegalArgumentException);
    }

    @Test
    public void unwrapsCompactMapWhenMap() throws Exception {
        CompactMap<String, String> compact = new CompactMap<>();
        Map<String, String> inner = new HashMap<>();
        Field valField = ReflectionUtils.getField(CompactMap.class, "val");
        valField.set(compact, inner);

        assertSame(inner, invoke(compact));
    }

    @Test
    public void returnsCompactMapWhenNotMap() throws Exception {
        CompactMap<String, String> compact = new CompactMap<>();
        assertSame(compact, invoke(compact));
    }

    @Test
    public void unwrapsCaseInsensitiveMap() throws Exception {
        CaseInsensitiveMap<String, String> ci = new CaseInsensitiveMap<>();
        Field mapField = ReflectionUtils.getField(CaseInsensitiveMap.class, "map");
        Map<?, ?> inner = (Map<?, ?>) mapField.get(ci);
        assertSame(inner, invoke(ci));
    }

    @Test
    public void unwrapsTrackingMap() throws Exception {
        Map<String, String> inner = new HashMap<>();
        TrackingMap<String, String> tracking = new TrackingMap<>(inner);
        assertSame(inner, invoke(tracking));
    }

    @Test
    public void baseMapReturnedDirectly() throws Exception {
        Map<String, String> map = new HashMap<>();
        assertSame(map, invoke(map));
    }
}
