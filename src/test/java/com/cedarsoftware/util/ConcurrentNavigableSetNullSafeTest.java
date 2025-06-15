package com.cedarsoftware.util;

import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ConcurrentNavigableSetNullSafe.
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
class ConcurrentNavigableSetNullSafeTest {

    @Test
    void testDefaultConstructor() {
        NavigableSet<String> set = new ConcurrentNavigableSetNullSafe<>();
        assertNotNull(set);
        assertTrue(set.isEmpty());

        // Test adding elements
        assertTrue(set.add("apple"));
        assertTrue(set.add("banana"));
        assertTrue(set.add(null));
        assertTrue(set.add("cherry"));

        // Test size and contains
        assertEquals(4, set.size());
        assertTrue(set.contains("apple"));
        assertTrue(set.contains("banana"));
        assertTrue(set.contains("cherry"));
        assertTrue(set.contains(null));

        // Test iterator (ascending order)
        Iterator<String> it = set.iterator();
        assertEquals("apple", it.next());
        assertEquals("banana", it.next());
        assertEquals("cherry", it.next());
        assertEquals(null, it.next());
        assertFalse(it.hasNext());
    }

    @Test
    void testComparatorConstructor() {
        Comparator<String> lengthComparator = Comparator.comparingInt(s -> s == null ? 0 : s.length());
        NavigableSet<String> set = new ConcurrentNavigableSetNullSafe<>(lengthComparator);

        assertNotNull(set);
        assertTrue(set.isEmpty());

        // Test adding elements
        assertTrue(set.add("kiwi"));
        assertTrue(set.add("apple"));
        assertTrue(set.add("banana"));
        assertTrue(set.add(null));

        // Test size and contains
        assertEquals(4, set.size());
        assertTrue(set.contains("kiwi"));
        assertTrue(set.contains("apple"));
        assertTrue(set.contains("banana"));
        assertTrue(set.contains(null));

        // Test iterator (ascending order by length)
        Iterator<String> it = set.iterator();
        assertEquals(null, it.next());      // Length 0
        assertEquals("kiwi", it.next());    // Length 4
        assertEquals("apple", it.next());   // Length 5
        assertEquals("banana", it.next());  // Length 6
        assertFalse(it.hasNext());
    }

    @Test
    void testCollectionConstructor() {
        Collection<String> collection = Arrays.asList("apple", "banana", null, "cherry");
        NavigableSet<String> set = new ConcurrentNavigableSetNullSafe<>(collection);

        assertNotNull(set);
        assertEquals(4, set.size());
        assertTrue(set.containsAll(collection));

        // Test iterator
        Iterator<String> it = set.iterator();
        assertEquals("apple", it.next());
        assertEquals("banana", it.next());
        assertEquals("cherry", it.next());
        assertEquals(null, it.next());
        assertFalse(it.hasNext());
    }

    @Test
    void testCollectionAndComparatorConstructor() {
        Collection<String> collection = Arrays.asList("apple", "banana", null, "cherry");
        Comparator<String> reverseComparator = (s1, s2) -> {
            if (s1 == null && s2 == null) return 0;
            if (s1 == null) return 1;
            if (s2 == null) return -1;
            return s2.compareTo(s1);
        };
        NavigableSet<String> set = new ConcurrentNavigableSetNullSafe<>(collection, reverseComparator);

        assertNotNull(set);
        assertEquals(4, set.size());

        // Test iterator (reverse order)
        Iterator<String> it = set.iterator();
        assertEquals("cherry", it.next());
        assertEquals("banana", it.next());
        assertEquals("apple", it.next());
        assertEquals(null, it.next());
        assertFalse(it.hasNext());
    }

    @Test
    void testAddRemoveContains() {
        NavigableSet<String> set = new ConcurrentNavigableSetNullSafe<>();
        set.add("apple");
        set.add("banana");
        set.add(null);

        assertTrue(set.contains("apple"));
        assertTrue(set.contains("banana"));
        assertTrue(set.contains(null));

        set.remove("banana");
        assertFalse(set.contains("banana"));
        assertEquals(2, set.size());

        set.remove(null);
        assertFalse(set.contains(null));
        assertEquals(1, set.size());

        set.clear();
        assertTrue(set.isEmpty());
    }

    @Test
    void testNavigationalMethods() {
        NavigableSet<String> set = new ConcurrentNavigableSetNullSafe<>();
        set.add("apple");
        set.add("banana");
        set.add("cherry");
        set.add(null);

        // Test lower
        assertEquals("banana", set.lower("cherry"));
        assertEquals("cherry", set.lower(null));
        assertNull(set.lower("apple"));

        // Test floor
        assertEquals("cherry", set.floor("cherry"));
        assertEquals(null, set.floor(null));
        assertNull(set.floor("aardvark"));

        // Test ceiling
        assertEquals("apple", set.ceiling("apple"));
        assertEquals("apple", set.ceiling("aardvark"));
        assertEquals(null, set.ceiling(null));

        // Test higher
        assertEquals("banana", set.higher("apple"));
        assertEquals(null, set.higher("cherry"));
        assertNull(set.higher(null));
    }

    @Test
    void testPollFirstPollLast() {
        NavigableSet<String> set = new ConcurrentNavigableSetNullSafe<>();
        set.add("apple");
        set.add("banana");
        set.add("cherry");
        set.add(null);

        assertEquals("apple", set.pollFirst());
        assertFalse(set.contains("apple"));

        assertEquals(null, set.pollLast());
        assertFalse(set.contains(null));

        assertEquals("banana", set.pollFirst());
        assertFalse(set.contains("banana"));

        assertEquals("cherry", set.pollLast());
        assertFalse(set.contains("cherry"));

        assertTrue(set.isEmpty());
    }

    @Test
    void testFirstLast() {
        NavigableSet<String> set = new ConcurrentNavigableSetNullSafe<>();
        set.add("apple");
        set.add("banana");
        set.add("cherry");
        set.add(null);

        assertEquals("apple", set.first());
        assertEquals(null, set.last());
    }

    @Test
    void testDescendingSet() {
        NavigableSet<String> set = new ConcurrentNavigableSetNullSafe<>();
        set.add("apple");
        set.add("banana");
        set.add("cherry");
        set.add(null);

        NavigableSet<String> descendingSet = set.descendingSet();
        Iterator<String> it = descendingSet.iterator();

        assertEquals(null, it.next());
        assertEquals("cherry", it.next());
        assertEquals("banana", it.next());
        assertEquals("apple", it.next());
        assertFalse(it.hasNext());
    }

    @Test
    void testSubSet() {
        NavigableSet<String> set = new ConcurrentNavigableSetNullSafe<>();
        set.add("apple");
        set.add("banana");
        set.add("cherry");
        set.add("date");
        set.add(null);

        NavigableSet<String> subSet = set.subSet("banana", true, "date", false);
        assertEquals(2, subSet.size());
        assertTrue(subSet.contains("banana"));
        assertTrue(subSet.contains("cherry"));
        assertFalse(subSet.contains("date"));
        assertFalse(subSet.contains("apple"));
        assertFalse(subSet.contains(null));

        // Test modification via subSet
        subSet.remove("banana");
        assertFalse(set.contains("banana"));

        subSet.add("blueberry");
        assertTrue(set.contains("blueberry"));
    }

    @Test
    void testHeadSet() {
        NavigableSet<String> set = new ConcurrentNavigableSetNullSafe<>();
        set.add("apple");
        set.add("banana");
        set.add("cherry");
        set.add(null);

        NavigableSet<String> headSet = set.headSet("cherry", false);
        assertEquals(2, headSet.size());
        assertTrue(headSet.contains("apple"));
        assertTrue(headSet.contains("banana"));
        assertFalse(headSet.contains("cherry"));
        assertFalse(headSet.contains(null));
    }

    @Test
    void testTailSet() {
        NavigableSet<String> set = new ConcurrentNavigableSetNullSafe<>();
        set.add("apple");
        set.add("banana");
        set.add("cherry");
        set.add(null);

        NavigableSet<String> tailSet = set.tailSet("banana", true);
        assertEquals(3, tailSet.size());
        assertTrue(tailSet.contains("banana"));
        assertTrue(tailSet.contains("cherry"));
        assertTrue(tailSet.contains(null));
        assertFalse(tailSet.contains("apple"));
    }

    @Test
    void testHeadAndTailSetViewModification() {
        NavigableSet<String> set = new ConcurrentNavigableSetNullSafe<>();
        set.add("apple");
        set.add("banana");
        set.add("cherry");
        set.add("date");
        set.add(null);

        NavigableSet<String> headSet = set.headSet("date", false);
        NavigableSet<String> tailSet = set.tailSet("banana", true);

        // Modify via headSet
        headSet.remove("banana");
        headSet.add("aardvark");
        assertFalse(set.contains("banana"));
        assertTrue(set.contains("aardvark"));

        // Modify via tailSet
        tailSet.remove(null);
        tailSet.add("elderberry");
        assertFalse(set.contains(null));
        assertTrue(set.contains("elderberry"));

        // Modify main set
        set.add("fig");
        set.remove("apple");
        assertFalse(headSet.contains("apple"));
        assertTrue(tailSet.contains("fig"));

        set.remove("cherry");
        assertFalse(headSet.contains("cherry"));
        assertFalse(tailSet.contains("cherry"));
    }

    @Test
    void testIteratorRemove() {
        NavigableSet<String> set = new ConcurrentNavigableSetNullSafe<>();
        set.add("apple");
        set.add("banana");
        set.add(null);

        Iterator<String> it = set.iterator();
        while (it.hasNext()) {
            String s = it.next();
            if ("banana".equals(s)) {
                it.remove();
            }
        }

        assertFalse(set.contains("banana"));
        assertEquals(2, set.size());
    }

    @Test
    void testComparatorConsistency() {
        Comparator<String> reverseComparator = Comparator.nullsFirst(Comparator.reverseOrder());
        NavigableSet<String> set = new ConcurrentNavigableSetNullSafe<>(reverseComparator);

        set.add("apple");
        set.add("banana");
        set.add("cherry");
        set.add(null);

        // Check that the elements are in reverse order
        Iterator<String> it = set.iterator();
        assertEquals(null, it.next());
        assertEquals("cherry", it.next());
        assertEquals("banana", it.next());
        assertEquals("apple", it.next());
        assertFalse(it.hasNext());
    }

    @Test
    void testCustomComparatorWithNulls() {
        // Comparator that treats null as less than any other element
        Comparator<String> nullFirstComparator = (s1, s2) -> {
            if (s1 == null && s2 == null) return 0;
            if (s1 == null) return -1;
            if (s2 == null) return 1;
            return s1.compareTo(s2);
        };

        NavigableSet<String> set = new ConcurrentNavigableSetNullSafe<>(nullFirstComparator);
        set.add("banana");
        set.add("apple");
        set.add(null);
        set.add("cherry");

        // Test iterator (null should be first)
        Iterator<String> it = set.iterator();
        assertEquals(null, it.next());
        assertEquals("apple", it.next());
        assertEquals("banana", it.next());
        assertEquals("cherry", it.next());
        assertFalse(it.hasNext());

        // Test navigational methods
        assertEquals(null, set.first());
        assertEquals("cherry", set.last());
    }

    @Test
    void testConcurrentModification() throws InterruptedException {
        NavigableSet<Integer> set = new ConcurrentNavigableSetNullSafe<>();
        for (int i = 0; i < 1000; i++) {
            set.add(i);
        }

        // Start a thread that modifies the set
        Thread modifier = new Thread(() -> {
            for (int i = 1000; i < 2000; i++) {
                set.add(i);
                set.remove(i - 1000);
            }
        });

        // Start a thread that iterates over the set
        Thread iterator = new Thread(() -> {
            Iterator<Integer> it = set.iterator();
            while (it.hasNext()) {
                it.next();
            }
        });

        modifier.start();
        iterator.start();

        modifier.join();
        iterator.join();

        // After modifications, the set should contain elements from 1000 to 1999
        assertEquals(1000, set.size());
        assertTrue(set.contains(1000));
        assertTrue(set.contains(1999));
        assertFalse(set.contains(0));
    }

    @Test
    void testNullHandling() {
        NavigableSet<String> set = new ConcurrentNavigableSetNullSafe<>();
        set.add(null);
        set.add("apple");

        assertTrue(set.contains(null));
        assertTrue(set.contains("apple"));

        // Test navigational methods with null
        assertEquals("apple", set.lower(null));
        assertEquals(null, set.floor(null));
        assertEquals(null, set.ceiling(null));
        assertNull(set.higher(null));
    }

    @Test
    void testSubSetWithNullBounds() {
        NavigableSet<String> set = new ConcurrentNavigableSetNullSafe<>();
        set.add("apple");
        set.add("banana");
        set.add("cherry");
        set.add(null);

        // Subset from "banana" to null (should include "banana", "cherry", null)
        NavigableSet<String> subSet = set.subSet("banana", true, null, true);
        assertEquals(3, subSet.size());
        assertTrue(subSet.contains("banana"));
        assertTrue(subSet.contains("cherry"));
        assertTrue(subSet.contains(null));

        // Subset from null to null (should include only null)
        NavigableSet<String> nullOnlySet = set.subSet(null, true, null, true);
        assertEquals(1, nullOnlySet.size());
        assertTrue(nullOnlySet.contains(null));
    }
}
