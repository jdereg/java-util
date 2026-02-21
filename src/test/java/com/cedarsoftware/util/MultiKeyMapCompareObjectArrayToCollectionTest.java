package com.cedarsoftware.util;

import java.util.Arrays;
import java.util.LinkedList;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Specific test to hit compareObjectArrayToCollection() method.
 * Need: Object[] stored, non-RandomAccess Collection lookup
 */
class MultiKeyMapCompareObjectArrayToCollectionTest {

    @Test
    void hitCompareObjectArrayToCollection_valueBasedEquality() {
        MultiKeyMap<String> map = MultiKeyMap.<String>builder()
                .capacity(1) // Force single bucket for hash collision-based comparison
                .valueBasedEquality(true) // Hit the value-based branch
                .flattenDimensions(false) // Don't expand nested structures
                .build();

        // Store with Object[] - this should stay as Object[]
        Object[] storedKey = {1, 2};
        map.put(storedKey, "stored_with_array");
        assertEquals(1, map.size());

        // Lookup with non-RandomAccess Collection
        // LinkedList is NOT RandomAccess, so should hit compareObjectArrayToCollection
        LinkedList<Integer> lookupKey = new LinkedList<>();
        lookupKey.add(1);
        lookupKey.add(2);

        // This should call compareObjectArrayToCollection with valueBasedEquality=true
        String result = map.get(lookupKey);
        assertEquals("stored_with_array", result);
    }

    @Test
    void hitCompareObjectArrayToCollection_typeStrictEquality() {
        MultiKeyMap<String> map = MultiKeyMap.<String>builder()
                .capacity(1) // Force single bucket
                .valueBasedEquality(false) // Hit the type-strict branch
                .flattenDimensions(false)
                .build();

        // Store with Object[]
        Object[] storedKey = {1, 2};
        map.put(storedKey, "stored_with_array");

        // Lookup with non-RandomAccess Collection
        LinkedList<Integer> lookupKey = new LinkedList<>();
        lookupKey.add(1);
        lookupKey.add(2);

        // This should call compareObjectArrayToCollection with valueBasedEquality=false
        String result = map.get(lookupKey);
        assertEquals("stored_with_array", result);
    }

    @Test
    void hitCompareObjectArrayToCollection_mismatch() {
        MultiKeyMap<String> map = MultiKeyMap.<String>builder()
                .capacity(1)
                .valueBasedEquality(true)
                .flattenDimensions(false)
                .build();

        // Store with Object[]
        Object[] storedKey = {1, 2};
        map.put(storedKey, "stored");

        // Lookup with non-RandomAccess Collection that doesn't match
        LinkedList<Integer> lookupKey = new LinkedList<>();
        lookupKey.add(1);
        lookupKey.add(3); // Different value

        // Should return null due to mismatch in compareObjectArrayToCollection
        String result = map.get(lookupKey);
        assertNull(result);
    }

    @Test
    void debugNormalizationBehavior() {
        MultiKeyMap<String> map = new MultiKeyMap<>();

        // Store Object[]
        Object[] array = {1, 2};
        map.put(array, "array_value");

        // Store LinkedList
        LinkedList<Integer> linkedList = new LinkedList<>();
        linkedList.add(1);
        linkedList.add(2);
        map.put(linkedList, "linkedlist_value");

        // If LinkedList gets normalized to Object[], both stores map to the same
        // normalized key, so the second put overwrites the first.
        // Verify cross-type lookups work correctly.
        String arrayLookup = map.get(new Object[]{1, 2});
        String linkedListLookup = map.get(linkedList);
        String freshLinkedListLookup = map.get(new LinkedList<>(Arrays.asList(1, 2)));

        // All lookups with equivalent keys should return the same value
        assertEquals(arrayLookup, linkedListLookup);
        assertEquals(linkedListLookup, freshLinkedListLookup);
    }
}
