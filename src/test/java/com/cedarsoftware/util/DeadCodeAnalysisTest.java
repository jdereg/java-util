package com.cedarsoftware.util;

import org.junit.jupiter.api.Test;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test to verify that compareObjectArrayToCollection() is potentially dead code.
 * Non-RandomAccess collections get converted to Object[] during normalization,
 * so they never participate in Collection vs Object[] comparisons.
 */
class DeadCodeAnalysisTest {

    @Test
    void proveNonRandomAccessCollectionsGetConvertedToArrays() {
        MultiKeyMap<String> map = MultiKeyMap.<String>builder()
                .capacity(1) // Force single bucket for collision-based comparison
                .build();
        
        // Store a LinkedList (non-RandomAccess)
        LinkedList<String> linkedList = new LinkedList<>();
        linkedList.add("a");
        linkedList.add("b");
        map.put(linkedList, "linked");
        
        // Try to lookup with Object[] - this should work because LinkedList
        // was normalized to Object[] internally
        Object[] arrayLookup = {"a", "b"};
        String result = map.get(arrayLookup);
        
        // If this succeeds, it proves LinkedList was converted to Object[]
        // If compareObjectArrayToCollection was used, this would fail
        // due to different hash bucket or comparison logic
        assertEquals("linked", result);
        
        // Additional verification: Try the reverse
        map.clear();
        map.put(arrayLookup, "array");
        assertEquals("array", map.get(linkedList));
    }
    
    @Test 
    void demonstrateRandomAccessNonListCollectionsStayAsCollections() {
        // Custom RandomAccess Collection (not a List)
        class RACollection<E> implements Collection<E>, RandomAccess {
            private final ArrayList<E> delegate = new ArrayList<>();
            
            @SafeVarargs
            RACollection(E... items) { 
                delegate.addAll(Arrays.asList(items)); 
            }
            
            @Override public int size() { return delegate.size(); }
            @Override public boolean isEmpty() { return delegate.isEmpty(); }
            @Override public boolean contains(Object o) { return delegate.contains(o); }
            @Override public Iterator<E> iterator() { return delegate.iterator(); }
            @Override public Object[] toArray() { return delegate.toArray(); }
            @Override public <T> T[] toArray(T[] a) { return delegate.toArray(a); }
            @Override public boolean add(E e) { return delegate.add(e); }
            @Override public boolean remove(Object o) { return delegate.remove(o); }
            @Override public boolean containsAll(Collection<?> c) { return delegate.containsAll(c); }
            @Override public boolean addAll(Collection<? extends E> c) { return delegate.addAll(c); }
            @Override public boolean removeAll(Collection<?> c) { return delegate.removeAll(c); }
            @Override public boolean retainAll(Collection<?> c) { return delegate.retainAll(c); }
            @Override public void clear() { delegate.clear(); }
        }
        
        MultiKeyMap<String> map = MultiKeyMap.<String>builder()
                .capacity(1) // Force single bucket
                .build();
        
        // Store RandomAccess Collection (not List)
        RACollection<String> raColl = new RACollection<>("x", "y");
        map.put(raColl, "ra_collection");
        
        // This Collection should stay as Collection (not converted to array)
        // So lookup with Object[] should trigger compareCollectionToObjectArray
        // which delegates to compareObjectArrayToCollection
        Object[] arrayLookup = {"x", "y"};
        String result = map.get(arrayLookup);
        
        assertEquals("ra_collection", result);
    }
    
    @Test
    void analyzeNormalizationBehavior() {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        
        // Test different collection types and see which ones work cross-type
        List<String> arrayList = Arrays.asList("test");      // RandomAccess + List
        LinkedList<String> linkedList = new LinkedList<>();  // Not RandomAccess
        linkedList.add("test");
        
        // Store with ArrayList
        map.put(arrayList, "arraylist_value");
        
        // Can we retrieve with LinkedList? If yes, they were both normalized the same way
        String result1 = map.get(linkedList);
        
        map.clear();
        
        // Store with LinkedList  
        map.put(linkedList, "linkedlist_value");
        
        // Can we retrieve with ArrayList?
        String result2 = map.get(arrayList);
        
        // If both work, both were normalized to Object[]
        System.out.println("ArrayList->LinkedList lookup: " + result1);
        System.out.println("LinkedList->ArrayList lookup: " + result2);
        
        // Both should work because both get normalized to Object[] for cross-compatibility
        assertEquals("arraylist_value", result1);
        assertEquals("linkedlist_value", result2);
    }
}