package com.cedarsoftware.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * Comprehensive test coverage for MultiKeyMap.dumpExpandedKeyStatic method.
 * This method is private and used internally for toString() operations.
 */
class MultiKeyMapFormatSimpleKeyTest {

    private MultiKeyMap<String> map;
    private Method dumpExpandedKeyStaticMethod;
    private Object NULL_SENTINEL;

    @BeforeEach
    void setUp() throws Exception {
        map = new MultiKeyMap<>();
        
        // Access private method using reflection
        dumpExpandedKeyStaticMethod = MultiKeyMap.class.getDeclaredMethod("dumpExpandedKeyStatic", Object.class, boolean.class, MultiKeyMap.class);
        dumpExpandedKeyStaticMethod.setAccessible(true);
        
        // Access private NULL_SENTINEL field
        Field nullSentinelField = MultiKeyMap.class.getDeclaredField("NULL_SENTINEL");
        nullSentinelField.setAccessible(true);
        NULL_SENTINEL = nullSentinelField.get(null);
    }

    private String formatSimpleKey(Object key, MultiKeyMap<?> selfMap) throws Exception {
        return (String) dumpExpandedKeyStaticMethod.invoke(null, key, true, selfMap);
    }

    @Test
    void testFormatSimpleKey_NullKey() throws Exception {
        String result = formatSimpleKey(null, null);
        assertThat(result).isEqualTo("🆔 ∅");
    }

    @Test
    void testFormatSimpleKey_NullSentinelKey() throws Exception {
        String result = formatSimpleKey(NULL_SENTINEL, null);
        assertThat(result).isEqualTo("🆔 ∅");
    }

    @Test
    void testFormatSimpleKey_SimpleStringKey() throws Exception {
        String result = formatSimpleKey("testKey", null);
        assertThat(result).isEqualTo("🆔 testKey");
    }

    @Test
    void testFormatSimpleKey_SimpleIntegerKey() throws Exception {
        String result = formatSimpleKey(42, null);
        assertThat(result).isEqualTo("🆔 42");
    }

    @Test
    void testFormatSimpleKey_SimpleBooleanKey() throws Exception {
        String result = formatSimpleKey(true, null);
        assertThat(result).isEqualTo("🆔 true");
    }

    @Test
    void testFormatSimpleKey_SelfReferenceInSingleKey() throws Exception {
        String result = formatSimpleKey(map, map);
        assertThat(result).isEqualTo("🆔 (this Map)");
    }

    @Test
    void testFormatSimpleKey_SingleElementArray_String() throws Exception {
        String[] singleArray = {"element"};
        String result = formatSimpleKey(singleArray, null);
        assertThat(result).isEqualTo("🆔 element");
    }

    @Test
    void testFormatSimpleKey_SingleElementArray_Integer() throws Exception {
        Integer[] singleArray = {123};
        String result = formatSimpleKey(singleArray, null);
        assertThat(result).isEqualTo("🆔 123");
    }

    @Test
    void testFormatSimpleKey_SingleElementArray_Null() throws Exception {
        Object[] singleArray = {null};
        String result = formatSimpleKey(singleArray, null);
        assertThat(result).isEqualTo("🆔 ∅");
    }

    @Test
    void testFormatSimpleKey_SingleElementArray_NullSentinel() throws Exception {
        Object[] singleArray = {NULL_SENTINEL};
        String result = formatSimpleKey(singleArray, null);
        assertThat(result).isEqualTo("🆔 ∅");
    }

    @Test
    void testFormatSimpleKey_SingleElementArray_SelfReference() throws Exception {
        Object[] singleArray = {map};
        String result = formatSimpleKey(singleArray, map);
        assertThat(result).isEqualTo("🆔 (this Map)");
    }

    @Test
    void testFormatSimpleKey_SingleElementCollection_String() throws Exception {
        List<String> singleList = Arrays.asList("element");
        String result = formatSimpleKey(singleList, null);
        assertThat(result).isEqualTo("🆔 element");
    }

    @Test
    void testFormatSimpleKey_SingleElementCollection_Integer() throws Exception {
        Set<Integer> singleSet = new HashSet<>(Arrays.asList(456));
        String result = formatSimpleKey(singleSet, null);
        assertThat(result).isEqualTo("🆔 456");
    }

    @Test
    void testFormatSimpleKey_SingleElementCollection_Null() throws Exception {
        List<Object> singleList = new ArrayList<>();
        singleList.add(null);
        String result = formatSimpleKey(singleList, null);
        assertThat(result).isEqualTo("🆔 ∅");
    }

    @Test
    void testFormatSimpleKey_SingleElementCollection_NullSentinel() throws Exception {
        List<Object> singleList = Arrays.asList(NULL_SENTINEL);
        String result = formatSimpleKey(singleList, null);
        assertThat(result).isEqualTo("🆔 [∅]");
    }

    @Test
    void testFormatSimpleKey_SingleElementCollection_SelfReference() throws Exception {
        List<Object> singleList = Arrays.asList(map);
        String result = formatSimpleKey(singleList, map);
        assertThat(result).isEqualTo("🆔 (this Map)");
    }

    @Test
    void testFormatSimpleKey_MultiElementArray_Strings() throws Exception {
        String[] multiArray = {"key1", "key2", "key3"};
        String result = formatSimpleKey(multiArray, null);
        assertThat(result).isEqualTo("🆔 [key1, key2, key3]");
    }

    @Test
    void testFormatSimpleKey_MultiElementArray_Mixed() throws Exception {
        Object[] multiArray = {"string", 42, true};
        String result = formatSimpleKey(multiArray, null);
        assertThat(result).isEqualTo("🆔 [string, 42, true]");
    }

    @Test
    void testFormatSimpleKey_MultiElementArray_WithNull() throws Exception {
        Object[] multiArray = {"key1", null, "key3"};
        String result = formatSimpleKey(multiArray, null);
        assertThat(result).isEqualTo("🆔 [key1, ∅, key3]");
    }

    @Test
    void testFormatSimpleKey_MultiElementArray_WithNullSentinel() throws Exception {
        Object[] multiArray = {"key1", NULL_SENTINEL, "key3"};
        String result = formatSimpleKey(multiArray, null);
        assertThat(result).isEqualTo("🆔 [key1, ∅, key3]");
    }

    @Test
    void testFormatSimpleKey_MultiElementArray_WithSelfReference() throws Exception {
        Object[] multiArray = {"key1", map, "key3"};
        String result = formatSimpleKey(multiArray, map);
        assertThat(result).isEqualTo("🆔 [key1, (this Map), key3]");
    }

    @Test
    void testFormatSimpleKey_MultiElementCollection_Strings() throws Exception {
        List<String> multiList = Arrays.asList("key1", "key2", "key3");
        String result = formatSimpleKey(multiList, null);
        assertThat(result).isEqualTo("🆔 [key1, key2, key3]");
    }

    @Test
    void testFormatSimpleKey_MultiElementCollection_Mixed() throws Exception {
        List<Object> multiList = Arrays.asList("string", 42, true);
        String result = formatSimpleKey(multiList, null);
        assertThat(result).isEqualTo("🆔 [string, 42, true]");
    }

    @Test
    void testFormatSimpleKey_MultiElementCollection_WithNull() throws Exception {
        List<Object> multiList = new ArrayList<>();
        multiList.add("key1");
        multiList.add(null);
        multiList.add("key3");
        String result = formatSimpleKey(multiList, null);
        assertThat(result).isEqualTo("🆔 [key1, ∅, key3]");
    }

    @Test
    void testFormatSimpleKey_MultiElementCollection_WithNullSentinel() throws Exception {
        List<Object> multiList = Arrays.asList("key1", NULL_SENTINEL, "key3");
        String result = formatSimpleKey(multiList, null);
        assertThat(result).isEqualTo("🆔 [key1, ∅, key3]");
    }

    @Test
    void testFormatSimpleKey_MultiElementCollection_WithSelfReference() throws Exception {
        List<Object> multiList = Arrays.asList("key1", map, "key3");
        String result = formatSimpleKey(multiList, map);
        assertThat(result).isEqualTo("🆔 [key1, (this Map), key3]");
    }

    @Test
    void testFormatSimpleKey_EmptyArray() throws Exception {
        Object[] emptyArray = {};
        String result = formatSimpleKey(emptyArray, null);
        assertThat(result).isEqualTo("🆔 []");
    }

    @Test
    void testFormatSimpleKey_EmptyCollection() throws Exception {
        List<Object> emptyList = new ArrayList<>();
        String result = formatSimpleKey(emptyList, null);
        assertThat(result).isEqualTo("🆔 []");
    }

    @Test
    void testFormatSimpleKey_DifferentCollectionTypes() throws Exception {
        // Test different collection implementations
        Set<String> hashSet = new HashSet<>(Arrays.asList("a", "b"));
        String result = formatSimpleKey(hashSet, null);
        assertThat(result).startsWith("🆔 [");
        assertThat(result).endsWith("]");
        assertThat(result).contains("a");
        assertThat(result).contains("b");
        
        LinkedList<String> linkedList = new LinkedList<>(Arrays.asList("x", "y"));
        result = formatSimpleKey(linkedList, null);
        assertThat(result).isEqualTo("🆔 [x, y]");
    }

    @ParameterizedTest
    @ValueSource(strings = {"short", "mediumLengthString", "veryLongStringThatExceedsNormalLength"})
    void testFormatSimpleKey_VariousStringLengths(String input) throws Exception {
        String result = formatSimpleKey(input, null);
        assertThat(result).isEqualTo("🆔 " + input);
    }

    @Test
    void testFormatSimpleKey_SpecialCharacters() throws Exception {
        String specialChars = "!@#$%^&*()_+-={}[]|\\:;\"'<>?,./ ";
        String result = formatSimpleKey(specialChars, null);
        assertThat(result).isEqualTo("🆔 " + specialChars);
    }

    @Test
    void testFormatSimpleKey_UnicodeCharacters() throws Exception {
        String unicode = "αβγδε中文한국어🌟💯";
        String result = formatSimpleKey(unicode, null);
        assertThat(result).isEqualTo("🆔 " + unicode);
    }

    @Test
    void testFormatSimpleKey_EdgeCase_TwoElementArray() throws Exception {
        Object[] twoArray = {"first", "second"};
        String result = formatSimpleKey(twoArray, null);
        assertThat(result).isEqualTo("🆔 [first, second]");
    }

    @Test
    void testFormatSimpleKey_EdgeCase_TwoElementCollection() throws Exception {
        List<String> twoList = Arrays.asList("first", "second");
        String result = formatSimpleKey(twoList, null);
        assertThat(result).isEqualTo("🆔 [first, second]");
    }

    @Test
    void testFormatSimpleKey_ComplexObjects() throws Exception {
        Object complexObject = new Object() {
            @Override
            public String toString() {
                return "ComplexObject{id=123}";
            }
        };
        String result = formatSimpleKey(complexObject, null);
        assertThat(result).isEqualTo("🆔 ComplexObject{id=123}");
    }

    @Test
    void testFormatSimpleKey_NumberTypes() throws Exception {
        // Test various number types
        String result = formatSimpleKey(123L, null);
        assertThat(result).isEqualTo("🆔 123");
        
        result = formatSimpleKey(45.67, null);
        assertThat(result).isEqualTo("🆔 45.67");
        
        result = formatSimpleKey(89.0f, null);
        assertThat(result).isEqualTo("🆔 89.0");
    }

    @Test
    void testFormatSimpleKey_ArrayOfArrays_SingleElement() throws Exception {
        // When array contains nested structure but only one top-level element
        Object[][] nestedArray = {{"inner"}};
        String result = formatSimpleKey(nestedArray, null);
        // This should not collapse to single element since it contains arrays
        assertThat(result).startsWith("🆔 ");
        assertThat(result.length()).isGreaterThan("🆔 inner".length());
    }

    @Test
    void testFormatSimpleKey_CollectionOfCollections_SingleElement() throws Exception {
        // When collection contains nested structure but only one top-level element
        List<List<String>> nestedList = Arrays.asList(Arrays.asList("inner"));
        String result = formatSimpleKey(nestedList, null);
        // This should not collapse to single element since it contains collections
        assertThat(result).startsWith("🆔 ");
        assertThat(result.length()).isGreaterThan("🆔 inner".length());
    }
}