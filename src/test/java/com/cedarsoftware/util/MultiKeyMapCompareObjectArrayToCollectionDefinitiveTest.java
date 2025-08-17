package com.cedarsoftware.util;

import org.junit.jupiter.api.Test;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Definitive test to hit compareObjectArrayToCollection() method.
 * Simple approach: Store Object[], lookup with LinkedList (non-RandomAccess).
 */
class MultiKeyMapCompareObjectArrayToCollectionDefinitiveTest {

    @Test
    void hitCompareObjectArrayToCollection_valueBasedEquality_matches() {
        MultiKeyMap<String> map = MultiKeyMap.<String>builder()
                .capacity(1) // Force hash collisions to trigger comparison
                .valueBasedEquality(true) // Target the value-based branch
                .build();
        
        // Store with Object[]
        Object[] array = {1, 2.0, "test"};
        map.put(array, "success");
        
        // Lookup with LinkedList (non-RandomAccess) 
        LinkedList<Object> linkedList = new LinkedList<>();
        linkedList.add(1.0);  // Different numeric type but value-equal
        linkedList.add(2);    // Different numeric type but value-equal  
        linkedList.add("test");
        
        // This should call compareObjectArrayToCollection with valueBasedEquality=true
        // and succeed due to numeric value equality
        String result = map.get(linkedList);
        assertEquals("success", result);
    }
    
    @Test
    void hitCompareObjectArrayToCollection_valueBasedEquality_fails() {
        MultiKeyMap<String> map = MultiKeyMap.<String>builder()
                .capacity(1)
                .valueBasedEquality(true)
                .build();
        
        // Store with Object[]
        Object[] array = {1, 2, "test"};
        map.put(array, "stored");
        
        // Lookup with LinkedList that doesn't match
        LinkedList<Object> linkedList = new LinkedList<>();
        linkedList.add(1);
        linkedList.add(3); // Different value
        linkedList.add("test");
        
        // Should fail in compareObjectArrayToCollection due to mismatch
        String result = map.get(linkedList);
        assertNull(result);
    }
    
    @Test
    void hitCompareObjectArrayToCollection_typeStrictEquality_matches() {
        MultiKeyMap<String> map = MultiKeyMap.<String>builder()
                .capacity(1)
                .valueBasedEquality(false) // Target the type-strict branch
                .build();
        
        // Store with Object[]
        Object[] array = {42, "hello", new AtomicInteger(5)};
        map.put(array, "success");
        
        // Lookup with LinkedList with exact same types
        LinkedList<Object> linkedList = new LinkedList<>();
        linkedList.add(42);                    // Same Integer
        linkedList.add("hello");               // Same String
        linkedList.add(new AtomicInteger(5));  // Same AtomicInteger value
        
        // Should succeed in type-strict mode
        String result = map.get(linkedList);
        assertEquals("success", result);
    }
    
    @Test 
    void hitCompareObjectArrayToCollection_typeStrictEquality_fails() {
        MultiKeyMap<String> map = MultiKeyMap.<String>builder()
                .capacity(1)
                .valueBasedEquality(false)
                .build();
        
        // Store with Object[]
        Object[] array = {42, "hello"};
        map.put(array, "stored");
        
        // Lookup with LinkedList with different types
        LinkedList<Object> linkedList = new LinkedList<>();
        linkedList.add(42L);     // Long instead of Integer - should fail in strict mode
        linkedList.add("hello");
        
        // Should fail due to type mismatch in strict mode
        String result = map.get(linkedList);
        assertNull(result);
    }
    
    @Test
    void hitCompareObjectArrayToCollection_withNulls() {
        MultiKeyMap<String> map = MultiKeyMap.<String>builder()
                .capacity(1)
                .valueBasedEquality(true)
                .build();
        
        // Store with Object[] containing nulls
        Object[] array = {null, 1, null, "test"};
        map.put(array, "with_nulls");
        
        // Lookup with LinkedList containing nulls
        LinkedList<Object> linkedList = new LinkedList<>();
        linkedList.add(null);
        linkedList.add(1.0); // Value-equal to 1
        linkedList.add(null);
        linkedList.add("test");
        
        // Should succeed - nulls should match
        String result = map.get(linkedList);
        assertEquals("with_nulls", result);
    }
    
    @Test
    void confirmLinkedListIsNotRandomAccess() {
        LinkedList<String> list = new LinkedList<>();
        assertFalse(list instanceof RandomAccess, "LinkedList should NOT be RandomAccess");
        
        ArrayList<String> arrayList = new ArrayList<>();
        assertTrue(arrayList instanceof RandomAccess, "ArrayList should be RandomAccess");
    }
}