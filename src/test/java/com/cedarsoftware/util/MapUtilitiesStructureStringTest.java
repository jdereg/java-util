package com.cedarsoftware.util;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class MapUtilitiesStructureStringTest {

    @Test
    public void nullInputReturnsNull() {
        assertEquals("null", MapUtilities.getMapStructureString(null));
    }

    @Test
    public void detectsCircularDependency() throws Exception {
        CaseInsensitiveMap<String, String> ci = new CaseInsensitiveMap<>();
        TrackingMap<String, String> tracking = new TrackingMap<>(ci);
        Field mapField = ReflectionUtils.getField(CaseInsensitiveMap.class, "map");
        mapField.set(ci, tracking);

        String expected = "CaseInsensitiveMap -> TrackingMap -> CYCLE -> CaseInsensitiveMap";
        assertEquals(expected, MapUtilities.getMapStructureString(ci));
    }

    @Test
    public void unwrapsCompactMapWhenMap() throws Exception {
        CompactMap<String, String> compact = new CompactMap<>();
        Map<String, String> inner = new HashMap<>();
        Field valField = ReflectionUtils.getField(CompactMap.class, "val");
        valField.set(compact, inner);

        assertEquals("CompactMap(unordered) -> HashMap", MapUtilities.getMapStructureString(compact));
    }

    @Test
    public void returnsCompactMapWhenNotMap() {
        CompactMap<String, String> compact = new CompactMap<>();
        assertEquals("CompactMap(unordered) -> [EMPTY]", MapUtilities.getMapStructureString(compact));
    }

    @Test
    public void unwrapsCaseInsensitiveMap() {
        CaseInsensitiveMap<String, String> ci = new CaseInsensitiveMap<>();
        assertEquals("CaseInsensitiveMap -> LinkedHashMap", MapUtilities.getMapStructureString(ci));
    }

    @Test
    public void unwrapsTrackingMap() {
        TrackingMap<String, String> tracking = new TrackingMap<>(new HashMap<>());
        assertEquals("TrackingMap -> HashMap", MapUtilities.getMapStructureString(tracking));
    }

    @Test
    public void baseMapReturnedDirectly() {
        Map<String, String> map = new HashMap<>();
        assertEquals("HashMap", MapUtilities.getMapStructureString(map));
    }

    @Test
    public void navigableMapSuffix() {
        Map<String, String> map = new TreeMap<>();
        assertEquals("TreeMap(NavigableMap)", MapUtilities.getMapStructureString(map));
    }
}
