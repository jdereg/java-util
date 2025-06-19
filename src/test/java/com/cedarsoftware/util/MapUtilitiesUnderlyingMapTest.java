package com.cedarsoftware.util;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class MapUtilitiesUnderlyingMapTest {

    private Map<?, ?> invoke(Map<?, ?> map) throws Exception {
        Method m = MapUtilities.class.getDeclaredMethod("getUnderlyingMap", Map.class);
        m.setAccessible(true);
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
        Field mapField = CaseInsensitiveMap.class.getDeclaredField("map");
        mapField.setAccessible(true);
        mapField.set(ci, tracking);

        Method m = MapUtilities.class.getDeclaredMethod("getUnderlyingMap", Map.class);
        m.setAccessible(true);
        InvocationTargetException ex = assertThrows(InvocationTargetException.class, () -> m.invoke(null, ci));
        assertTrue(ex.getCause() instanceof IllegalArgumentException);
    }

    @Test
    public void unwrapsCompactMapWhenMap() throws Exception {
        CompactMap<String, String> compact = new CompactMap<>();
        Map<String, String> inner = new HashMap<>();
        Field valField = CompactMap.class.getDeclaredField("val");
        valField.setAccessible(true);
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
        Field mapField = CaseInsensitiveMap.class.getDeclaredField("map");
        mapField.setAccessible(true);
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
