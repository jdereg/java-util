package com.cedarsoftware.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
        assertEquals("{\n  🆔 key → 🟣 value\n}", map.toString());
    }

    @Test
    void testMultiKeyToString() {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        map.putMultiKey("value", "key1", "key2");
        assertEquals("{\n  🆔 [key1, key2] → 🟣 value\n}", map.toString());
    }

    @Test
    void testNullKeyToString() {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        map.put(null, "nullValue");
        assertEquals("{\n  🆔 ∅ → 🟣 nullValue\n}", map.toString());
    }

    @Test
    void testNullValueToString() {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        map.put((Object) "key", (String) null);
        assertEquals("{\n  🆔 key → 🟣 ∅\n}", map.toString());
    }

    @Test
    void testSelfReferenceAsKey() {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        Map<Object, String> mapInterface = map; // Use Map interface to avoid ambiguity
        mapInterface.put(map, "someValue");
        
        String result = map.toString();
        assertEquals("{\n  🆔 (this Map ♻️) → 🟣 someValue\n}", result);
        
        // Should not throw StackOverflowError
        assertDoesNotThrow(() -> map.toString());
    }

    @Test
    void testSelfReferenceAsValue() {
        MultiKeyMap<MultiKeyMap> map = new MultiKeyMap<>();
        Map<Object, MultiKeyMap> mapInterface = map; // Use Map interface to avoid ambiguity
        mapInterface.put("someKey", map);
        
        String result = map.toString();
        assertEquals("{\n  🆔 someKey → 🟣 (this Map ♻️)\n}", result);
        
        // Should not throw StackOverflowError
        assertDoesNotThrow(() -> map.toString());
    }

    @Test
    void testSelfReferenceAsBothKeyAndValue() {
        MultiKeyMap<MultiKeyMap> map = new MultiKeyMap<>();
        Map<Object, MultiKeyMap> mapInterface = map; // Use Map interface to avoid ambiguity
        mapInterface.put(map, map);
        
        String result = map.toString();
        assertEquals("{\n  🆔 (this Map ♻️) → 🟣 (this Map ♻️)\n}", result);
        
        // Should not throw StackOverflowError
        assertDoesNotThrow(() -> map.toString());
    }

    @Test
    void testSelfReferenceInMultiKey() {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        map.putMultiKey("value", map, "key2", "key3");
        
        String result = map.toString();
        assertEquals("{\n  🆔 [(this Map ♻️), key2, key3] → 🟣 value\n}", result);
        
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
        assertTrue(result.contains("🆔 key1 → 🟣 value1"));
        assertTrue(result.contains("🆔 [key2a, key2b] → 🟣 value2"));
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
        assertTrue(result.contains("(this Map ♻️)"));
        assertTrue(result.contains("🆔 normal → 🟣 value"));
    }

    // ====================================================================================
    // Tests for List and Set notation: Lists use [], Sets use {}
    // ====================================================================================

    @Test
    void testListKeyUsesSquareBrackets() {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        List<Integer> listKey = Arrays.asList(1, 2, 3);
        map.put(listKey, "listValue");

        String result = map.toString();
        assertTrue(result.contains("[1, 2, 3]"), "Lists should use square brackets [ ]");
        assertTrue(result.contains("🟣 listValue"));
    }

    @Test
    void testSetKeyUsesCurlyBraces() {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        Set<Integer> setKey = new HashSet<>(Arrays.asList(4, 5, 6));
        map.put(setKey, "setValue");

        String result = map.toString();
        assertTrue(result.contains("{"), "Sets should use opening curly brace {");
        assertTrue(result.contains("}"), "Sets should use closing curly brace }");
        assertTrue(result.contains("4") && result.contains("5") && result.contains("6"));
        assertTrue(result.contains("🟣 setValue"));
    }

    @Test
    void testMixedListAndSetKey() {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        List<Integer> list = Arrays.asList(1, 2, 3);
        Set<Integer> set = new HashSet<>(Arrays.asList(4, 5, 6));
        map.put(new Object[]{list, set}, "mixedValue");

        String result = map.toString();
        assertTrue(result.contains("[1, 2, 3]"), "List portion should use square brackets");
        assertTrue(result.contains("{") && result.contains("}"), "Set portion should use curly braces");
        assertTrue(result.contains("🟣 mixedValue"));
    }

    @Test
    void testListWithNullElement() {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        List<Object> listWithNull = Arrays.asList(1, null, 3);
        map.put(listWithNull, "nullInList");

        String result = map.toString();
        assertTrue(result.contains("["));
        assertTrue(result.contains("∅"), "Null should be represented as ∅");
        assertTrue(result.contains("🟣 nullInList"));
    }

    @Test
    void testNestedSetInList() {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        Set<Integer> innerSet = new HashSet<>(Arrays.asList(10, 20));
        List<Object> outerList = Arrays.asList(1, innerSet, 3);
        map.put(outerList, "setInList");

        String result = map.toString();
        assertTrue(result.contains("["), "Outer List should use square brackets");
        assertTrue(result.contains("]"));
        assertTrue(result.contains("{"), "Inner Set should use curly braces");
        assertTrue(result.contains("}"));
        assertTrue(result.contains("10") && result.contains("20"));
        assertTrue(result.contains("🟣 setInList"));
    }

    @Test
    void testNestedListInSet() {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        List<Integer> innerList = Arrays.asList(10, 20);
        Set<Object> outerSet = new LinkedHashSet<>(Arrays.asList(1, innerList, 3));
        map.put(outerSet, "listInSet");

        String result = map.toString();
        assertTrue(result.contains("{"), "Outer Set should use curly braces");
        assertTrue(result.contains("}"));
        assertTrue(result.contains("["), "Inner List should use square brackets");
        assertTrue(result.contains("]"));
        assertTrue(result.contains("10") && result.contains("20"));
        assertTrue(result.contains("🟣 listInSet"));
    }

    @Test
    void testComplexMixedStructure() {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        List<Integer> list1 = Arrays.asList(1, 2);
        Set<Integer> set1 = new HashSet<>(Arrays.asList(3, 4));
        List<Integer> list2 = Arrays.asList(5, 6);
        map.put(new Object[]{list1, set1, list2}, "complex");

        String result = map.toString();

        // Count brackets and braces
        int openBrackets = countChar(result, '[');
        int closeBrackets = countChar(result, ']');
        int openBraces = countChar(result, '{');
        int closeBraces = countChar(result, '}');

        // Should have balanced brackets and braces
        assertEquals(openBrackets, closeBrackets, "Square brackets should be balanced");
        assertEquals(openBraces, closeBraces, "Curly braces should be balanced");

        // All elements should be present
        assertTrue(result.contains("1") && result.contains("2"));
        assertTrue(result.contains("3") && result.contains("4"));
        assertTrue(result.contains("5") && result.contains("6"));
        assertTrue(result.contains("🟣 complex"));
    }

    @Test
    void testEmptyListAndSet() {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        List<Integer> emptyList = new ArrayList<>();
        Set<Integer> emptySet = new HashSet<>();
        map.put(new Object[]{emptyList, emptySet}, "emptyCollections");

        String result = map.toString();
        assertTrue(result.contains("[") && result.contains("]"), "Empty List should show brackets");
        assertTrue(result.contains("{") && result.contains("}"), "Empty Set should show braces");
        assertTrue(result.contains("🟣 emptyCollections"));
    }

    @Test
    void testSingleElementSet() {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        Set<Integer> singleSet = Collections.singleton(99);
        map.put(singleSet, "singleSet");

        String result = map.toString();
        assertTrue(result.contains("{"), "Single-element Set should use curly braces");
        assertTrue(result.contains("99"));
        assertTrue(result.contains("}"));
        assertTrue(result.contains("🟣 singleSet"));
    }

    @Test
    void testMultipleEntriesWithListsAndSets() {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        map.put(Arrays.asList(1, 2), "list12");
        map.put(new HashSet<>(Arrays.asList(3, 4)), "set34");
        map.put("simple", "simpleValue");

        String result = map.toString();
        assertTrue(result.contains("[1, 2]"), "Should show List with brackets");
        assertTrue(result.contains("list12"));
        assertTrue(result.contains("{") && result.contains("}"), "Should show Set with braces");
        assertTrue(result.contains("3") && result.contains("4"));
        assertTrue(result.contains("set34"));
        assertTrue(result.contains("simple"));
        assertTrue(result.contains("simpleValue"));
    }

    // Helper method to count character occurrences
    private int countChar(String str, char ch) {
        int count = 0;
        for (int i = 0; i < str.length(); i++) {
            if (str.charAt(i) == ch) {
                count++;
            }
        }
        return count;
    }
}