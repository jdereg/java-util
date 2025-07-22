package com.cedarsoftware.util;

import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Test toString() method including self-reference handling.
 */
class MultiKeyMapToStringTest {

    @Test
    void testEmptyMapToString() {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        assertEquals("{}", map.toString());
    }

    @Test
    void testSingleKeyToString() {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        map.put("key", "value");
        assertEquals("{\n  🔑key → value\n}", map.toString());
    }

    @Test
    void testMultiKeyToString() {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        map.putMultiKey("value", "key1", "key2");
        assertEquals("{\n  🔑[key1, key2] → value\n}", map.toString());
    }

    @Test
    void testNullKeyToString() {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        map.put(null, "nullValue");
        assertEquals("{\n  🔑∅ → nullValue\n}", map.toString());
    }

    @Test
    void testNullValueToString() {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        map.put((Object) "key", (String) null);
        assertEquals("{\n  🔑key → null\n}", map.toString());
    }

    @Test
    void testSelfReferenceAsKey() {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        Map<Object, String> mapInterface = map; // Use Map interface to avoid ambiguity
        mapInterface.put(map, "someValue");
        
        String result = map.toString();
        assertEquals("{\n  🔑(this Map) → someValue\n}", result);
        
        // Should not throw StackOverflowError
        assertDoesNotThrow(() -> map.toString());
    }

    @Test
    void testSelfReferenceAsValue() {
        MultiKeyMap<MultiKeyMap> map = new MultiKeyMap<>();
        Map<Object, MultiKeyMap> mapInterface = map; // Use Map interface to avoid ambiguity
        mapInterface.put("someKey", map);
        
        String result = map.toString();
        assertEquals("{\n  🔑someKey → (this Map)\n}", result);
        
        // Should not throw StackOverflowError
        assertDoesNotThrow(() -> map.toString());
    }

    @Test
    void testSelfReferenceAsBothKeyAndValue() {
        MultiKeyMap<MultiKeyMap> map = new MultiKeyMap<>();
        Map<Object, MultiKeyMap> mapInterface = map; // Use Map interface to avoid ambiguity
        mapInterface.put(map, map);
        
        String result = map.toString();
        assertEquals("{\n  🔑(this Map) → (this Map)\n}", result);
        
        // Should not throw StackOverflowError
        assertDoesNotThrow(() -> map.toString());
    }

    @Test
    void testSelfReferenceInMultiKey() {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        map.putMultiKey("value", map, "key2", "key3");
        
        String result = map.toString();
        assertEquals("{\n  🔑[(this Map), key2, key3] → value\n}", result);
        
        // Should not throw StackOverflowError
        assertDoesNotThrow(() -> map.toString());
    }

    @Test
    void testMultipleEntriesToString() {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        map.put("key1", "value1");
        map.putMultiKey("value2", "key2a", "key2b");
        
        String result = map.toString();
        
        // Should contain both entries (order may vary)
        assertTrue(result.contains("🔑key1 → value1"));
        assertTrue(result.contains("🔑[key2a, key2b] → value2"));
        assertTrue(result.startsWith("{"));
        assertTrue(result.endsWith("}"));
    }

    @Test
    void testComplexSelfReferenceScenario() {
        MultiKeyMap<Object> map = new MultiKeyMap<>();
        
        // Add normal entries
        map.put("normal", "value");
        
        // Add self-reference as key in multi-key
        map.putMultiKey("selfInMulti", map, "otherKey");
        
        // Add self-reference as value  
        Map<Object, Object> mapInterface = map;
        mapInterface.put("selfAsValue", map);
        
        String result = map.toString();
        
        // Should handle all cases without infinite recursion
        assertDoesNotThrow(() -> map.toString());
        
        // Should contain self-reference markers
        assertTrue(result.contains("(this Map)"));
        assertTrue(result.contains("🔑normal → value"));
    }
}