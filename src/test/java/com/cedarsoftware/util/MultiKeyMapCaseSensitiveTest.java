package com.cedarsoftware.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for the caseSensitive configuration option in MultiKeyMap.
 * Tests single CharSequence keys, arrays/collections with CharSequences,
 * and nested structures with CharSequences.
 * 
 * @author John DeRegnaucourt (jdereg@gmail.com)
 *         <br>
 *         Copyright (c) Cedar Software LLC
 *         <br><br>
 *         Licensed under the Apache License, Version 2.0 (the "License");
 *         you may not use this file except in compliance with the License.
 *         You may obtain a copy of the License at
 *         <br><br>
 *         <a href="http://www.apache.org/licenses/LICENSE-2.0">License</a>
 *         <br><br>
 *         Unless required by applicable law or agreed to in writing, software
 *         distributed under the License is distributed on an "AS IS" BASIS,
 *         WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *         See the License for the specific language governing permissions and
 *         limitations under the License.
 */
public class MultiKeyMapCaseSensitiveTest {

    @Test
    public void testSingleStringKey_CaseSensitive() {
        // Default behavior - case sensitive
        MultiKeyMap<String> map = new MultiKeyMap<>();
        
        map.put("Hello", "value1");
        map.put("hello", "value2");
        map.put("HELLO", "value3");
        
        assertEquals("value1", map.get("Hello"));
        assertEquals("value2", map.get("hello"));
        assertEquals("value3", map.get("HELLO"));
        assertNull(map.get("HeLLo"));
        
        assertEquals(3, map.size());
    }

    @Test
    public void testSingleStringKey_CaseInsensitive() {
        // Case-insensitive mode
        MultiKeyMap<String> map = MultiKeyMap.<String>builder()
            .caseSensitive(false)
            .build();
        
        map.put("Hello", "value1");
        map.put("hello", "value2");  // Should overwrite previous
        map.put("HELLO", "value3");  // Should overwrite previous
        
        assertEquals("value3", map.get("Hello"));
        assertEquals("value3", map.get("hello"));
        assertEquals("value3", map.get("HELLO"));
        assertEquals("value3", map.get("HeLLo"));
        
        assertEquals(1, map.size());
    }

    @Test
    public void testSingleStringBuilderKey_CaseSensitive() {
        // Default behavior - case sensitive
        // Note: StringBuilder uses identity equality by default, not content equality
        MultiKeyMap<String> map = new MultiKeyMap<>();
        
        StringBuilder sb1 = new StringBuilder("Hello");
        StringBuilder sb2 = new StringBuilder("hello");
        StringBuilder sb3 = new StringBuilder("HELLO");
        
        map.put(sb1, "value1");
        map.put(sb2, "value2");
        map.put(sb3, "value3");
        
        // Must use the same instances since StringBuilder uses identity equality
        assertEquals("value1", map.get(sb1));
        assertEquals("value2", map.get(sb2));
        assertEquals("value3", map.get(sb3));
        
        // Different StringBuilder instances won't match in case-sensitive mode
        assertNull(map.get(new StringBuilder("Hello")));
        assertNull(map.get(new StringBuilder("hello")));
        
        assertEquals(3, map.size());
    }

    @Test
    public void testSingleStringBuilderKey_CaseInsensitive() {
        // Case-insensitive mode
        MultiKeyMap<String> map = MultiKeyMap.<String>builder()
            .caseSensitive(false)
            .build();
        
        StringBuilder sb1 = new StringBuilder("Hello");
        StringBuilder sb2 = new StringBuilder("hello");
        StringBuilder sb3 = new StringBuilder("HELLO");
        
        map.put(sb1, "value1");
        map.put(sb2, "value2");  // Should overwrite previous
        map.put(sb3, "value3");  // Should overwrite previous
        
        assertEquals("value3", map.get(new StringBuilder("Hello")));
        assertEquals("value3", map.get(new StringBuilder("hello")));
        assertEquals("value3", map.get(new StringBuilder("HELLO")));
        assertEquals("value3", map.get(new StringBuilder("HeLLo")));
        
        assertEquals(1, map.size());
    }

    @Test
    public void testSingleStringBufferKey_CaseSensitive() {
        // Default behavior - case sensitive
        // Note: StringBuffer uses identity equality by default, not content equality
        MultiKeyMap<String> map = new MultiKeyMap<>();
        
        StringBuffer buf1 = new StringBuffer("Hello");
        StringBuffer buf2 = new StringBuffer("hello");
        StringBuffer buf3 = new StringBuffer("HELLO");
        
        map.put(buf1, "value1");
        map.put(buf2, "value2");
        map.put(buf3, "value3");
        
        // Must use the same instances since StringBuffer uses identity equality
        assertEquals("value1", map.get(buf1));
        assertEquals("value2", map.get(buf2));
        assertEquals("value3", map.get(buf3));
        
        // Different StringBuffer instances won't match in case-sensitive mode
        assertNull(map.get(new StringBuffer("Hello")));
        assertNull(map.get(new StringBuffer("hello")));
        
        assertEquals(3, map.size());
    }

    @Test
    public void testSingleStringBufferKey_CaseInsensitive() {
        // Case-insensitive mode
        MultiKeyMap<String> map = MultiKeyMap.<String>builder()
            .caseSensitive(false)
            .build();
        
        StringBuffer buf1 = new StringBuffer("Hello");
        StringBuffer buf2 = new StringBuffer("hello");
        StringBuffer buf3 = new StringBuffer("HELLO");
        
        map.put(buf1, "value1");
        map.put(buf2, "value2");  // Should overwrite previous
        map.put(buf3, "value3");  // Should overwrite previous
        
        assertEquals("value3", map.get(new StringBuffer("Hello")));
        assertEquals("value3", map.get(new StringBuffer("hello")));
        assertEquals("value3", map.get(new StringBuffer("HELLO")));
        assertEquals("value3", map.get(new StringBuffer("HeLLo")));
        
        assertEquals(1, map.size());
    }

    @Test
    public void testMixedCharSequenceTypes_CaseInsensitive() {
        // Case-insensitive mode with mixed CharSequence types
        MultiKeyMap<String> map = MultiKeyMap.<String>builder()
            .caseSensitive(false)
            .build();
        
        String str = "Hello";
        StringBuilder sb = new StringBuilder("HELLO");
        StringBuffer buf = new StringBuffer("hello");
        
        map.put(str, "value1");
        assertEquals("value1", map.get(sb));    // Different type, same value (case-insensitive)
        assertEquals("value1", map.get(buf));   // Different type, same value (case-insensitive)
        
        map.put(sb, "value2");  // Should overwrite
        assertEquals("value2", map.get(str));
        assertEquals("value2", map.get(buf));
        
        assertEquals(1, map.size());
    }

    @Test
    public void testArrayWithSingleString_CaseSensitive() {
        // Default behavior - case sensitive
        MultiKeyMap<String> map = new MultiKeyMap<>();
        
        map.put(new String[]{"Hello"}, "value1");
        map.put(new String[]{"hello"}, "value2");
        map.put(new String[]{"HELLO"}, "value3");
        
        assertEquals("value1", map.get(new String[]{"Hello"}));
        assertEquals("value2", map.get(new String[]{"hello"}));
        assertEquals("value3", map.get(new String[]{"HELLO"}));
        assertNull(map.get(new String[]{"HeLLo"}));
        
        assertEquals(3, map.size());
    }

    @Test
    public void testArrayWithSingleString_CaseInsensitive() {
        // Case-insensitive mode
        MultiKeyMap<String> map = MultiKeyMap.<String>builder()
            .caseSensitive(false)
            .build();
        
        map.put(new String[]{"Hello"}, "value1");
        map.put(new String[]{"hello"}, "value2");  // Should overwrite
        map.put(new String[]{"HELLO"}, "value3");  // Should overwrite
        
        assertEquals("value3", map.get(new String[]{"Hello"}));
        assertEquals("value3", map.get(new String[]{"hello"}));
        assertEquals("value3", map.get(new String[]{"HELLO"}));
        assertEquals("value3", map.get(new String[]{"HeLLo"}));
        
        assertEquals(1, map.size());
    }

    @Test
    public void testArrayWithSingleStringBuilder_CaseInsensitive() {
        // Case-insensitive mode with StringBuilder in array
        MultiKeyMap<String> map = MultiKeyMap.<String>builder()
            .caseSensitive(false)
            .build();
        
        map.put(new Object[]{new StringBuilder("Hello")}, "value1");
        map.put(new Object[]{new StringBuilder("hello")}, "value2");  // Should overwrite
        
        assertEquals("value2", map.get(new Object[]{new StringBuilder("HELLO")}));
        assertEquals("value2", map.get(new Object[]{"hello"}));  // Mixed types
        
        assertEquals(1, map.size());
    }

    @Test
    public void testArrayWithSingleStringBuffer_CaseInsensitive() {
        // Case-insensitive mode with StringBuffer in array
        MultiKeyMap<String> map = MultiKeyMap.<String>builder()
            .caseSensitive(false)
            .build();
        
        map.put(new Object[]{new StringBuffer("Hello")}, "value1");
        map.put(new Object[]{new StringBuffer("hello")}, "value2");  // Should overwrite
        
        assertEquals("value2", map.get(new Object[]{new StringBuffer("HELLO")}));
        assertEquals("value2", map.get(new Object[]{"hello"}));  // Mixed types
        
        assertEquals(1, map.size());
    }

    @Test
    public void testCollectionWithSingleString_CaseSensitive() {
        // Default behavior - case sensitive
        MultiKeyMap<String> map = new MultiKeyMap<>();
        
        List<String> list1 = Arrays.asList("Hello");
        List<String> list2 = Arrays.asList("hello");
        List<String> list3 = Arrays.asList("HELLO");
        
        map.put(list1, "value1");
        map.put(list2, "value2");
        map.put(list3, "value3");
        
        assertEquals("value1", map.get(Arrays.asList("Hello")));
        assertEquals("value2", map.get(Arrays.asList("hello")));
        assertEquals("value3", map.get(Arrays.asList("HELLO")));
        assertNull(map.get(Arrays.asList("HeLLo")));
        
        assertEquals(3, map.size());
    }

    @Test
    public void testCollectionWithSingleString_CaseInsensitive() {
        // Case-insensitive mode
        MultiKeyMap<String> map = MultiKeyMap.<String>builder()
            .caseSensitive(false)
            .build();
        
        List<String> list1 = Arrays.asList("Hello");
        List<String> list2 = Arrays.asList("hello");
        List<String> list3 = Arrays.asList("HELLO");
        
        map.put(list1, "value1");
        map.put(list2, "value2");  // Should overwrite
        map.put(list3, "value3");  // Should overwrite
        
        assertEquals("value3", map.get(Arrays.asList("Hello")));
        assertEquals("value3", map.get(Arrays.asList("hello")));
        assertEquals("value3", map.get(Arrays.asList("HELLO")));
        assertEquals("value3", map.get(Arrays.asList("HeLLo")));
        
        assertEquals(1, map.size());
    }

    @Test
    public void testCollectionWithSingleStringBuilder_CaseInsensitive() {
        // Case-insensitive mode with StringBuilder in collection
        MultiKeyMap<String> map = MultiKeyMap.<String>builder()
            .caseSensitive(false)
            .build();
        
        List<Object> list1 = Arrays.asList((Object)new StringBuilder("Hello"));
        List<Object> list2 = Arrays.asList((Object)new StringBuilder("hello"));
        
        map.put(list1, "value1");
        map.put(list2, "value2");  // Should overwrite
        
        assertEquals("value2", map.get(Arrays.asList((Object)new StringBuilder("HELLO"))));
        assertEquals("value2", map.get(Arrays.asList("hello")));  // Mixed types
        
        assertEquals(1, map.size());
    }

    @Test
    public void testCollectionWithSingleStringBuffer_CaseInsensitive() {
        // Case-insensitive mode with StringBuffer in collection
        MultiKeyMap<String> map = MultiKeyMap.<String>builder()
            .caseSensitive(false)
            .build();
        
        List<Object> list1 = Arrays.asList((Object)new StringBuffer("Hello"));
        List<Object> list2 = Arrays.asList((Object)new StringBuffer("hello"));
        
        map.put(list1, "value1");
        map.put(list2, "value2");  // Should overwrite
        
        assertEquals("value2", map.get(Arrays.asList((Object)new StringBuffer("HELLO"))));
        assertEquals("value2", map.get(Arrays.asList("hello")));  // Mixed types
        
        assertEquals(1, map.size());
    }

    @Test
    public void testNestedArrayWithStrings_CaseInsensitive() {
        // Case-insensitive mode with nested arrays
        MultiKeyMap<String> map = MultiKeyMap.<String>builder()
            .caseSensitive(false)
            .build();
        
        // Nested array: [["User", "Settings"], "Theme"]
        Object[] nested1 = new Object[]{new String[]{"User", "Settings"}, "Theme"};
        Object[] nested2 = new Object[]{new String[]{"user", "settings"}, "theme"};
        Object[] nested3 = new Object[]{new String[]{"USER", "SETTINGS"}, "THEME"};
        
        map.put(nested1, "value1");
        map.put(nested2, "value2");  // Should overwrite
        map.put(nested3, "value3");  // Should overwrite
        
        assertEquals("value3", map.get(new Object[]{new String[]{"User", "Settings"}, "Theme"}));
        assertEquals("value3", map.get(new Object[]{new String[]{"user", "settings"}, "theme"}));
        assertEquals("value3", map.get(new Object[]{new String[]{"UsEr", "SeTtInGs"}, "ThEmE"}));
        
        assertEquals(1, map.size());
    }

    @Test
    public void testMultiKeyWithMixedCharSequences_CaseInsensitive() {
        // Case-insensitive mode with var-args multi-key
        MultiKeyMap<String> map = MultiKeyMap.<String>builder()
            .caseSensitive(false)
            .build();
        
        String str = "User";
        StringBuilder sb = new StringBuilder("Settings");
        StringBuffer buf = new StringBuffer("Theme");
        
        map.putMultiKey("value1", str, sb, buf);
        
        // All case variations should find the same value
        assertEquals("value1", map.getMultiKey("user", "settings", "theme"));
        assertEquals("value1", map.getMultiKey("USER", "SETTINGS", "THEME"));
        assertEquals("value1", map.getMultiKey(
            new StringBuilder("User"), 
            new StringBuffer("Settings"), 
            "Theme"
        ));
        
        // Overwrite with different case
        map.putMultiKey("value2", "USER", "SETTINGS", "THEME");
        assertEquals("value2", map.getMultiKey("user", "settings", "theme"));
        
        assertEquals(1, map.size());
    }

    @Test
    public void testCaseSensitiveWithNonStringKeys() {
        // Verify that non-CharSequence keys are not affected by caseSensitive setting
        MultiKeyMap<String> map = MultiKeyMap.<String>builder()
            .caseSensitive(false)
            .build();
        
        // Integer keys should not be affected
        map.putMultiKey("value1", 1, "Hello", 3);
        map.putMultiKey("value2", 1, "hello", 3);  // Should overwrite due to "hello"
        
        assertEquals("value2", map.getMultiKey(1, "HELLO", 3));
        assertEquals(1, map.size());
        
        // But different integers should create different entries
        map.putMultiKey("value3", 2, "hello", 3);
        assertEquals(2, map.size());
        assertEquals("value2", map.getMultiKey(1, "hello", 3));
        assertEquals("value3", map.getMultiKey(2, "HELLO", 3));
    }

    @Test
    public void testArrayVsCollectionEquivalence_CaseInsensitive() {
        // Test that arrays and collections with same CharSequence values are equivalent
        MultiKeyMap<String> map = MultiKeyMap.<String>builder()
            .caseSensitive(false)
            .build();
        
        // Put with array
        map.put(new String[]{"User", "Settings", "Theme"}, "value1");
        
        // Get with collection (different case)
        List<String> list = Arrays.asList("user", "settings", "theme");
        assertEquals("value1", map.get(list));
        
        // Put with collection (overwrites)
        map.put(Arrays.asList("USER", "SETTINGS", "THEME"), "value2");
        
        // Get with array
        assertEquals("value2", map.get(new String[]{"User", "Settings", "Theme"}));
        
        assertEquals(1, map.size());
    }

    @Test
    public void testBuilderConfiguration() {
        // Test that builder properly sets caseSensitive
        MultiKeyMap<String> caseSensitiveMap = MultiKeyMap.<String>builder()
            .caseSensitive(true)
            .build();
        assertTrue(caseSensitiveMap.getCaseSensitive());
        
        MultiKeyMap<String> caseInsensitiveMap = MultiKeyMap.<String>builder()
            .caseSensitive(false)
            .build();
        assertFalse(caseInsensitiveMap.getCaseSensitive());
        
        // Default should be true
        MultiKeyMap<String> defaultMap = new MultiKeyMap<>();
        assertTrue(defaultMap.getCaseSensitive());
    }

    @Test
    public void testComplexNestedStructure_CaseInsensitive() {
        // Complex test with multiple levels of nesting
        MultiKeyMap<String> map = MultiKeyMap.<String>builder()
            .caseSensitive(false)
            .flattenDimensions(false)  // Preserve structure
            .build();
        
        // Create complex nested structure with CharSequences
        List<Object> innerList = new ArrayList<>();
        innerList.add(new StringBuilder("Config"));
        innerList.add(new StringBuffer("Settings"));
        
        Object[] complexKey = new Object[]{
            "User",
            innerList,
            new String[]{"Theme", "Dark"}
        };
        
        map.put(complexKey, "complex-value");
        
        // Access with different case variations
        List<Object> innerList2 = new ArrayList<>();
        innerList2.add("config");  // lowercase String
        innerList2.add("SETTINGS"); // uppercase String
        
        Object[] lookupKey = new Object[]{
            new StringBuilder("USER"),  // uppercase StringBuilder
            innerList2,
            new String[]{"theme", "dark"}  // lowercase array
        };
        
        assertEquals("complex-value", map.get(lookupKey));
    }

    @Test
    public void testEmptyCharSequences_CaseInsensitive() {
        // Edge case: empty strings should work correctly
        MultiKeyMap<String> map = MultiKeyMap.<String>builder()
            .caseSensitive(false)
            .build();
        
        map.putMultiKey("value1", "", "Hello", "");
        assertEquals("value1", map.getMultiKey("", "hello", ""));
        assertEquals("value1", map.getMultiKey(
            new StringBuilder(""), 
            new StringBuffer("HELLO"), 
            ""
        ));
    }

    @Test
    public void testNullAndCharSequences_CaseInsensitive() {
        // Test null handling with CharSequences
        MultiKeyMap<String> map = MultiKeyMap.<String>builder()
            .caseSensitive(false)
            .build();
        
        map.putMultiKey("value1", null, "Hello", null);
        assertEquals("value1", map.getMultiKey(null, "hello", null));
        assertEquals("value1", map.getMultiKey(null, "HELLO", null));
        
        // Null is different from empty string
        assertNull(map.getMultiKey("", "hello", ""));
    }
}