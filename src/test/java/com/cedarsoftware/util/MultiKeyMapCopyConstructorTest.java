package com.cedarsoftware.util;

import org.junit.jupiter.api.Test;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive test for MultiKeyMap copy constructor.
 * Tests that the copy constructor creates a proper deep copy with correct behavior
 * for defensive copying, configuration inheritance, and data isolation.
 */
public class MultiKeyMapCopyConstructorTest {

    @Test
    void testBasicCopyConstructor() {
        // Create original map with various key types
        MultiKeyMap<String> original = new MultiKeyMap<>();
        
        // Add different types of entries
        original.put("simple", "value1");
        original.put(Arrays.asList("list", "key"), "value2");
        original.put(new Object[]{"array", "key"}, "value3");
        original.putMultiKey("value4", "multi1", "multi2");
        original.put(null, "nullKeyValue");
        original.put("nullValue", null);
        
        // Create copy
        MultiKeyMap<String> copy = new MultiKeyMap<>(original);
        
        // Verify size
        assertEquals(original.size(), copy.size());
        
        // Verify all entries are copied correctly
        assertEquals("value1", copy.get("simple"));
        assertEquals("value2", copy.get(Arrays.asList("list", "key")));
        assertEquals("value3", copy.get(new Object[]{"array", "key"}));
        assertEquals("value4", copy.getMultiKey("multi1", "multi2"));
        assertEquals("nullKeyValue", copy.get(null));
        assertNull(copy.get("nullValue"));
        
        // Verify copy has correct containsKey behavior
        assertTrue(copy.containsKey("simple"));
        assertTrue(copy.containsKey(Arrays.asList("list", "key")));
        assertTrue(copy.containsKey(new Object[]{"array", "key"}));
        assertTrue(copy.containsMultiKey("multi1", "multi2"));
        assertTrue(copy.containsKey(null));
        assertTrue(copy.containsKey("nullValue")); // Key exists with null value
    }

    @Test
    void testCopyWithDefensiveCopiesEnabled() {
        // Create original with defensive copies enabled
        MultiKeyMap<String> original = MultiKeyMap.<String>builder()
            .defensiveCopies(true)
            .build();
        
        // Use mutable collections and arrays
        List<String> mutableList = new ArrayList<>(Arrays.asList("mutable", "list"));
        Object[] mutableArray = {"mutable", "array"};
        
        original.put(mutableList, "listValue");
        original.put(mutableArray, "arrayValue");
        
        // Create copy (should also have defensive copies enabled)
        MultiKeyMap<String> copy = new MultiKeyMap<>(original);
        
        // Verify values are copied correctly
        assertEquals("listValue", copy.get(Arrays.asList("mutable", "list")));
        assertEquals("arrayValue", copy.get(new Object[]{"mutable", "array"}));
        
        // Modify original collections - should not affect the copy
        mutableList.add("modified");
        mutableArray[0] = "modified";
        
        // Verify copy is unaffected (defensive copies working)
        assertEquals("listValue", copy.get(Arrays.asList("mutable", "list")));
        assertEquals("arrayValue", copy.get(new Object[]{"mutable", "array"}));
        
        // Verify we can't find modified versions in copy
        assertNull(copy.get(Arrays.asList("mutable", "list", "modified")));
        assertNull(copy.get(new Object[]{"modified", "array"}));
    }

    @Test
    void testCopyWithDefensiveCopiesDisabled() {
        // Create original with defensive copies disabled
        MultiKeyMap<String> original = MultiKeyMap.<String>builder()
            .defensiveCopies(false)
            .build();
        
        Object[] sharedArray = {"shared", "array"};
        original.put(sharedArray, "sharedValue");
        
        // Create copy (should inherit defensiveCopies=false)
        MultiKeyMap<String> copy = new MultiKeyMap<>(original);
        
        // Verify value is copied correctly
        assertEquals("sharedValue", copy.get(new Object[]{"shared", "array"}));
        assertEquals("sharedValue", copy.get(sharedArray)); // Should work with same array reference
    }

    @Test
    void testCopyConfigurationInheritance() {
        // Create original with specific configuration
        MultiKeyMap<String> original = MultiKeyMap.<String>builder()
            .collectionKeyMode(MultiKeyMap.CollectionKeyMode.COLLECTIONS_NOT_EXPANDED)
            .flattenDimensions(true)
            .defensiveCopies(false)
            .build();
        
        // Add some data to verify the configuration works
        List<String> testList = Arrays.asList("test", "list");
        original.put(testList, "configValue");
        
        // Create copy
        MultiKeyMap<String> copy = new MultiKeyMap<>(original);
        
        // Verify configuration was inherited by testing behavior
        assertEquals("configValue", copy.get(testList));
        
        // In COLLECTIONS_NOT_EXPANDED mode, the list should be treated as a regular key
        // So different list instances with same content should not be equivalent
        List<String> differentListInstance = new ArrayList<>(Arrays.asList("test", "list"));
        assertEquals("configValue", copy.get(differentListInstance)); // Should work due to equals()
    }

    @Test
    void testCopyDataIsolation() {
        // Create original map
        MultiKeyMap<String> original = new MultiKeyMap<>();
        original.put("shared", "originalValue");
        original.put("original", "onlyInOriginal");
        
        // Create copy
        MultiKeyMap<String> copy = new MultiKeyMap<>(original);
        
        // Verify initial state
        assertEquals("originalValue", original.get("shared"));
        assertEquals("originalValue", copy.get("shared"));
        assertEquals("onlyInOriginal", original.get("original"));
        assertEquals("onlyInOriginal", copy.get("original"));
        
        // Modify original - should not affect copy
        original.put("shared", "modifiedValue");
        original.put("newInOriginal", "newValue");
        original.remove("original");
        
        // Verify copy is unaffected
        assertEquals("originalValue", copy.get("shared")); // Unchanged
        assertEquals("onlyInOriginal", copy.get("original")); // Still exists
        assertNull(copy.get("newInOriginal")); // Doesn't exist
        
        // Modify copy - should not affect original
        copy.put("shared", "copyModifiedValue");
        copy.put("newInCopy", "copyValue");
        copy.remove("original");
        
        // Verify original is unaffected
        assertEquals("modifiedValue", original.get("shared")); // Original's modification
        assertNull(original.get("newInCopy")); // Copy's addition doesn't appear
        assertFalse(original.containsKey("original")); // Original's removal stands
        
        // Verify final states
        assertEquals("copyModifiedValue", copy.get("shared"));
        assertEquals("copyValue", copy.get("newInCopy"));
        assertFalse(copy.containsKey("original"));
    }

    @Test
    void testCopyEmptyMap() {
        MultiKeyMap<String> original = new MultiKeyMap<>();
        MultiKeyMap<String> copy = new MultiKeyMap<>(original);
        
        assertEquals(0, original.size());
        assertEquals(0, copy.size());
        assertTrue(copy.isEmpty());
        
        // Verify operations work on empty copy
        copy.put("test", "value");
        assertEquals(1, copy.size());
        assertEquals(0, original.size()); // Original unaffected
    }

    @Test
    void testCopyLargeMap() {
        // Create a large map to test performance and correctness
        MultiKeyMap<Integer> original = new MultiKeyMap<>();
        
        // Add many entries with different key types
        for (int i = 0; i < 1000; i++) {
            original.put("simple" + i, i);
            original.put(Arrays.asList("list", i), i + 1000);
            original.put(new Object[]{"array", i}, i + 2000);
            if (i % 10 == 0) {
                original.putMultiKey(i + 3000, "multi", i, "key");
            }
        }
        
        int originalSize = original.size();
        assertTrue(originalSize > 3000); // Should have many entries
        
        // Create copy
        MultiKeyMap<Integer> copy = new MultiKeyMap<>(original);
        
        // Verify size matches
        assertEquals(originalSize, copy.size());
        
        // Spot check some entries
        assertEquals((Integer) 0, copy.get("simple0"));
        assertEquals((Integer) 1500, copy.get(Arrays.asList("list", 500)));
        assertEquals((Integer) 2999, copy.get(new Object[]{"array", 999}));
        assertEquals((Integer) 3000, copy.getMultiKey("multi", 0, "key"));
        
        // Verify independence
        copy.put("newKey", 9999);
        assertNull(original.get("newKey"));
        assertEquals((Integer) 9999, copy.get("newKey"));
    }

    @Test
    void testCopyWithComplexNestedStructures() {
        MultiKeyMap<String> original = new MultiKeyMap<>();
        
        // Create complex nested keys
        Object[][] nestedArray = {{"level1", "array"}, {"level2", null}};
        List<List<String>> nestedList = Arrays.asList(
            Arrays.asList("level1", "list"),
            Arrays.asList("level2", null)
        );
        
        original.put(nestedArray, "nestedArrayValue");
        original.put(nestedList, "nestedListValue");
        
        // Create copy
        MultiKeyMap<String> copy = new MultiKeyMap<>(original);
        
        // Verify complex keys work in copy
        assertEquals("nestedArrayValue", copy.get(nestedArray));
        assertEquals("nestedListValue", copy.get(nestedList));
        
        // Verify with equivalent structures
        Object[][] equivalentArray = {{"level1", "array"}, {"level2", null}};
        List<List<String>> equivalentList = Arrays.asList(
            Arrays.asList("level1", "list"),
            Arrays.asList("level2", null)
        );
        
        assertEquals("nestedArrayValue", copy.get(equivalentArray));
        assertEquals("nestedListValue", copy.get(equivalentList));
    }

    @Test
    void testCopyWithNullKeysAndValues() {
        MultiKeyMap<String> original = new MultiKeyMap<>();
        
        // Add entries with null keys and values
        original.put(null, "nullKey");
        original.put("nullValue", null);
        original.put(Arrays.asList("list", null, "key"), "listWithNull");
        original.put(new Object[]{null, "array", null}, "arrayWithNulls");
        
        // Create copy
        MultiKeyMap<String> copy = new MultiKeyMap<>(original);
        
        // Verify null handling
        assertEquals("nullKey", copy.get(null));
        assertNull(copy.get("nullValue"));
        assertEquals("listWithNull", copy.get(Arrays.asList("list", null, "key")));
        assertEquals("arrayWithNulls", copy.get(new Object[]{null, "array", null}));
        
        // Verify containsKey works correctly with nulls
        assertTrue(copy.containsKey(null));
        assertTrue(copy.containsKey("nullValue")); // Key exists, value is null
        assertTrue(copy.containsKey(Arrays.asList("list", null, "key")));
        assertTrue(copy.containsKey(new Object[]{null, "array", null}));
    }

    @Test
    void testCopyPreservesOptimizedPaths() {
        MultiKeyMap<String> original = new MultiKeyMap<>();
        
        // Add entries that would use optimized paths
        original.put("single", "singleValue");                              // Simple single key
        original.put(new Object[]{"arrayOne"}, "oneElementArray");          // 1-element array
        original.put(new Object[]{"two", "elements"}, "twoElementArray");   // 2-element array
        original.put(Arrays.asList("listOne"), "oneElementList");           // 1-element collection
        original.put(Arrays.asList("listTwo", "elements"), "twoElementList"); // 2-element collection
        
        // Create copy
        MultiKeyMap<String> copy = new MultiKeyMap<>(original);
        
        // Verify all optimized paths work in copy
        assertEquals("singleValue", copy.get("single"));
        assertEquals("oneElementArray", copy.get(new Object[]{"arrayOne"}));
        assertEquals("twoElementArray", copy.get(new Object[]{"two", "elements"}));
        assertEquals("oneElementList", copy.get(Arrays.asList("listOne")));
        assertEquals("twoElementList", copy.get(Arrays.asList("listTwo", "elements")));
        
        // Verify containsKey works with optimized paths
        assertTrue(copy.containsKey("single"));
        assertTrue(copy.containsKey(new Object[]{"arrayOne"}));
        assertTrue(copy.containsKey(new Object[]{"two", "elements"}));
        assertTrue(copy.containsKey(Arrays.asList("listOne")));
        assertTrue(copy.containsKey(Arrays.asList("listTwo", "elements")));
    }

    @Test
    void testCopyConstructorWithGenericWildcards() {
        // Test the copy constructor's generic signature: MultiKeyMap(MultiKeyMap<? extends V> source)
        MultiKeyMap<Object> originalWithObjects = new MultiKeyMap<>();
        originalWithObjects.put("key1", "stringValue");
        originalWithObjects.put("key2", 42);
        originalWithObjects.put("key3", Arrays.asList("list", "value"));
        
        // This should compile due to ? extends V
        MultiKeyMap<Object> copyOfObjects = new MultiKeyMap<>(originalWithObjects);
        
        assertEquals("stringValue", copyOfObjects.get("key1"));
        assertEquals(42, copyOfObjects.get("key2"));
        assertEquals(Arrays.asList("list", "value"), copyOfObjects.get("key3"));
        
        // Test with more specific type
        MultiKeyMap<String> originalWithStrings = new MultiKeyMap<>();
        originalWithStrings.put("key1", "value1");
        originalWithStrings.put("key2", "value2");
        
        // This should also work: String extends Object
        MultiKeyMap<Object> copyFromStrings = new MultiKeyMap<>(originalWithStrings);
        assertEquals("value1", copyFromStrings.get("key1"));
        assertEquals("value2", copyFromStrings.get("key2"));
    }
}