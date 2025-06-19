package com.cedarsoftware.util;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.*;

import static com.cedarsoftware.util.CompactMap.INSERTION;
import static com.cedarsoftware.util.CompactMap.SORTED;
import static com.cedarsoftware.util.CompactMap.UNORDERED;
import static org.junit.jupiter.api.Assertions.*;

public class MapUtilitiesDetectMapOrderingTest {

    @Test
    public void nullInputReturnsUnordered() {
        assertEquals(UNORDERED, MapUtilities.detectMapOrdering(null));
    }

    @Test
    public void underlyingCompactMapFromWrapper() {
        CompactMap<String, String> compact = new CompactMap<>();
        CaseInsensitiveMap<String, String> wrapper =
                new CaseInsensitiveMap<>(Collections.emptyMap(), compact);
        assertEquals(compact.getOrdering(), MapUtilities.detectMapOrdering(wrapper));
    }

    @Test
    public void sortedMapReturnsSorted() {
        assertEquals(SORTED, MapUtilities.detectMapOrdering(new TreeMap<>()));
    }

    @Test
    public void linkedHashMapReturnsInsertion() {
        assertEquals(INSERTION, MapUtilities.detectMapOrdering(new LinkedHashMap<>()));
    }

    @Test
    public void hashMapReturnsUnordered() {
        assertEquals(UNORDERED, MapUtilities.detectMapOrdering(new HashMap<>()));
    }

    @Test
    public void circularDependencyException() throws Exception {
        CaseInsensitiveMap<String, String> ci = new CaseInsensitiveMap<>();
        TrackingMap<String, String> tracking = new TrackingMap<>(ci);
        Field mapField = ReflectionUtils.getField(CaseInsensitiveMap.class, "map");
        mapField.set(ci, tracking);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> MapUtilities.detectMapOrdering(ci));
        assertTrue(ex.getMessage().startsWith(
                "Cannot determine map ordering: Circular map structure detected"));
    }
}

