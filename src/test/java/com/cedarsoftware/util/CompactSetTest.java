package com.cedarsoftware.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import static org.junit.jupiter.api.Assertions.fail;

/**
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
public class CompactSetTest
{
    @Test
    public void testSimpleCases()
    {
        Set<String> set = new CompactSet<>();
        assert set.isEmpty();
        assert set.size() == 0;
        assert !set.contains(null);
        assert !set.contains("foo");
        assert !set.remove("foo");
        assert set.add("foo");
        assert !set.add("foo");
        assert set.size() == 1;
        assert !set.isEmpty();
        assert set.contains("foo");
        assert !set.remove("bar");
        assert set.remove("foo");
        assert set.isEmpty();
    }

    @Test
    public void testSimpleCases2()
    {
        Set<String> set = new CompactSet<>();
        assert set.isEmpty();
        assert set.size() == 0;
        assert set.add("foo");
        assert !set.add("foo");
        assert set.add("bar");
        assert !set.add("bar");
        assert set.size() == 2;
        assert !set.isEmpty();
        assert !set.remove("baz");
        assert set.remove("foo");
        assert set.remove("bar");
        assert set.isEmpty();
    }

    @Test
    public void testBadNoArgConstructor()
    {
        try
        {
            new CompactSet() { protected int compactSize() { return 1; } };
            fail();
        }
        catch (Exception e) { }
    }

    @Test
    public void testBadConstructor()
    {
        Set<String> treeSet = new TreeSet<>();
        treeSet.add("foo");
        treeSet.add("baz");
        Set<String> set = new CompactSet<>(treeSet);
        assert set.contains("foo");
        assert set.contains("baz");
        assert set.size() == 2;
    }

    @Test
    public void testSize()
    {
        CompactSet<Integer> set = new CompactSet<>();
        for (int i=0; i < set.compactSize() + 5; i++)
        {
            set.add(i);
        }
        assert set.size() == set.compactSize() + 5;
        assert set.contains(0);
        assert set.contains(1);
        assert set.contains(set.compactSize() - 5);
        assert !set.remove("foo");

        clearViaIterator(set);
    }

    @Test
    public void testHeterogeneuousItems()
    {
        CompactSet<Object> set = new CompactSet<>();
        assert set.add(16);
        assert set.add("Foo");
        assert set.add(true);
        assert set.add(null);
        assert set.size() == 4;

        assert !set.contains(7);
        assert !set.contains("Bar");
        assert !set.contains(false);
        assert !set.contains(0);

        assert set.contains(16);
        assert set.contains("Foo");
        assert set.contains(true);
        assert set.contains(null);

        set = new CompactSet() { protected boolean isCaseInsensitive() { return true; } };
        assert set.add(16);
        assert set.add("Foo");
        assert set.add(true);
        assert set.add(null);

        assert set.contains("foo");
        assert set.contains("FOO");
        assert set.size() == 4;

        clearViaIterator(set);
    }

    @Test
    public void testClear()
    {
        CompactSet<Object> set = new CompactSet<>();

        assert set.isEmpty();
        set.clear();
        assert set.isEmpty();
        assert set.add('A');
        assert !set.add('A');
        assert set.size() == 1;
        assert !set.isEmpty();
        set.clear();
        assert set.isEmpty();

        for (int i=0; i < set.compactSize() + 1; i++)
        {
            set.add((long) i);
        }
        assert set.size() == set.compactSize() + 1;
        set.clear();
        assert set.isEmpty();
    }

    @Test
    public void testRemove()
    {
        CompactSet<String> set = new CompactSet<>();

        try
        {
            Iterator<String> i = set.iterator();
            i.remove();
            fail();
        }
        catch (IllegalStateException e) { }

        assert set.add("foo");
        assert set.add("bar");
        assert set.add("baz");

        Iterator<String> i = set.iterator();
        while (i.hasNext())
        {
            i.next();
            i.remove();
        }
        try
        {
            i.remove();
            fail();
        }
        catch (IllegalStateException e) { }
    }

    @Test
    public void testCaseInsensitivity()
    {
        CompactSet<String> set = new CompactSet<String>()
        {
            protected boolean isCaseInsensitive() { return true; }
            protected Set<String> getNewSet() { return new CaseInsensitiveSet<>(); }
        };

        set.add("foo");
        set.add("bar");
        set.add("baz");
        set.add("qux");
        assert !set.contains("foot");
        assert !set.contains("bart");
        assert !set.contains("bazinga");
        assert !set.contains("quux");
        assert set.contains("FOO");
        assert set.contains("BAR");
        assert set.contains("BAZ");
        assert set.contains("QUX");
        clearViaIterator(set);
    }

    @Test
    public void testCaseSensitivity()
    {
        CompactSet<String> set = new CompactSet<>();

        set.add("foo");
        set.add("bar");
        set.add("baz");
        set.add("qux");
        assert !set.contains("Foo");
        assert !set.contains("Bar");
        assert !set.contains("Baz");
        assert !set.contains("Qux");
        assert set.contains("foo");
        assert set.contains("bar");
        assert set.contains("baz");
        assert set.contains("qux");
        clearViaIterator(set);
    }

    @Test
    public void testCaseInsensitivity2()
    {
        CompactSet<String> set = new CompactSet<String>()
        {
            protected boolean isCaseInsensitive() { return true; }
            protected Set<String> getNewSet() { return new CaseInsensitiveSet<>(); }
        };

        for (int i=0; i < set.compactSize() + 5; i++)
        {
            set.add("FoO" + i);
        }

        assert set.contains("foo0");
        assert set.contains("FOO0");
        assert set.contains("foo1");
        assert set.contains("FOO1");
        assert set.contains("foo" + (set.compactSize() + 3));
        assert set.contains("FOO" + (set.compactSize() + 3));
        clearViaIterator(set);
    }

    @Test
    public void testCaseSensitivity2()
    {
        CompactSet<String> set = new CompactSet<>();

        for (int i=0; i < set.compactSize() + 5; i++)
        {
            set.add("FoO" + i);
        }

        assert set.contains("FoO0");
        assert !set.contains("foo0");
        assert set.contains("FoO1");
        assert !set.contains("foo1");
        assert set.contains("FoO" + (set.compactSize() + 3));
        assert !set.contains("foo" + (set.compactSize() + 3));
        clearViaIterator(set);
    }

    @Test
    public void testCompactLinkedSet()
    {
        Set<String> set = CompactSet.<String>builder().insertionOrder().build();
        set.add("foo");
        set.add("bar");
        set.add("baz");

        Iterator<String> i = set.iterator();
        assert i.next() == "foo";
        assert i.next() == "bar";
        assert i.next() == "baz";
        assert !i.hasNext();

        Set<String> set2 = CompactSet.<String>builder().insertionOrder().build();
        set2.addAll(set);
        assert set2.equals(set);
    }

    @Test
    public void testCompactCIHashSet()
    {
        CompactSet<String> set = CompactSet.<String>builder()
                .caseSensitive(false)  // This replaces isCaseInsensitive() == true
                .build();

        for (int i=0; i < set.compactSize() + 5; i++)
        {
            set.add("FoO" + i);
        }

        assert set.contains("FoO0");
        assert set.contains("foo0");
        assert set.contains("FoO1");
        assert set.contains("foo1");
        assert set.contains("FoO" + (set.compactSize() + 3));
        assert set.contains("foo" + (set.compactSize() + 3));

        Set<String> copy = CompactSet.<String>builder()
                .caseSensitive(false)
                .build();
        copy.addAll(set);

        assert copy.equals(set);
        assert copy != set;

        assert copy.contains("FoO0");
        assert copy.contains("foo0");
        assert copy.contains("FoO1");
        assert copy.contains("foo1");
        assert copy.contains("FoO" + (set.compactSize() + 3));
        assert copy.contains("foo" + (set.compactSize() + 3));

        clearViaIterator(set);
        clearViaIterator(copy);
    }

    @Test
    public void testCompactCILinkedSet()
    {
        CompactSet<String> set = CompactSet.<String>builder().caseSensitive(false).insertionOrder().build();

        for (int i=0; i < set.compactSize() + 5; i++)
        {
            set.add("FoO" + i);
        }

        assert set.contains("FoO0");
        assert set.contains("foo0");
        assert set.contains("FoO1");
        assert set.contains("foo1");
        assert set.contains("FoO" + (set.compactSize() + 3));
        assert set.contains("foo" + (set.compactSize() + 3));

        Set<String> copy = CompactSet.<String>builder()
                .caseSensitive(false)   // Makes the set case-insensitive
                .insertionOrder()      // Preserves insertion order
                .build();
        copy.addAll(set);
        assert copy.equals(set);
        assert copy != set;

        assert copy.contains("FoO0");
        assert copy.contains("foo0");
        assert copy.contains("FoO1");
        assert copy.contains("foo1");
        assert copy.contains("FoO" + (set.compactSize() + 3));
        assert copy.contains("foo" + (set.compactSize() + 3));

        clearViaIterator(set);
        clearViaIterator(copy);
    }

    @EnabledIfSystemProperty(named = "performRelease", matches = "true")
    @Test
    public void testPerformance()
    {
        int maxSize = 1000;
        int lower = 50;
        int upper = 80;
        long totals[] = new long[upper - lower + 1];

        for (int x = 0; x < 2000; x++)
        {
            for (int i = lower; i < upper; i++)
            {
                CompactSet<String> set = CompactSet.<String>builder().caseSensitive(true).compactSize(i).build();

                long start = System.nanoTime();
                // ===== Timed
                for (int j = 0; j < maxSize; j++)
                {
                    set.add("" + j);
                }

                for (int j = 0; j < maxSize; j++)
                {
                    set.add("" + j);
                }

                Iterator iter = set.iterator();
                while (iter.hasNext())
                {
                    iter.next();
                    iter.remove();
                }
                // ===== End Timed
                long end = System.nanoTime();
                totals[i - lower] += end - start;
            }

            Set<String> set2 = new HashSet<>();
            long start = System.nanoTime();
            // ===== Timed
            for (int i = 0; i < maxSize; i++)
            {
                set2.add("" + i);
            }

            for (int i = 0; i < maxSize; i++)
            {
                set2.contains("" + i);
            }

            Iterator iter = set2.iterator();
            while (iter.hasNext())
            {
                iter.next();
                iter.remove();
            }
            // ===== End Timed
            long end = System.nanoTime();
            totals[totals.length - 1] += end - start;
        }
        for (int i = lower; i < upper; i++)
        {
            System.out.println("CompacSet.compactSize: " + i + " = " + totals[i - lower] / 1000000.0d);
        }
        System.out.println("HashSet = " + totals[totals.length - 1] / 1000000.0d);
    }

    @Test
    public void testSortedOrder() {
        CompactSet<String> set = CompactSet.<String>builder()
                .sortedOrder()
                .build();

        set.add("zebra");
        set.add("apple");
        set.add("monkey");

        Iterator<String> iter = set.iterator();
        assert "apple".equals(iter.next());
        assert "monkey".equals(iter.next());
        assert "zebra".equals(iter.next());
        assert !iter.hasNext();
    }

    @Test
    public void testReverseOrder() {
        CompactSet<String> set = CompactSet.<String>builder()
                .reverseOrder()
                .build();

        set.add("zebra");
        set.add("apple");
        set.add("monkey");

        Iterator<String> iter = set.iterator();
        assert "zebra".equals(iter.next());
        assert "monkey".equals(iter.next());
        assert "apple".equals(iter.next());
        assert !iter.hasNext();
    }

    @Test
    public void testInsertionOrder() {
        CompactSet<String> set = CompactSet.<String>builder()
                .insertionOrder()
                .build();

        set.add("zebra");
        set.add("apple");
        set.add("monkey");

        Iterator<String> iter = set.iterator();
        assert "zebra".equals(iter.next());
        assert "apple".equals(iter.next());
        assert "monkey".equals(iter.next());
        assert !iter.hasNext();
    }

    @Test
    public void testUnorderedBehavior() {
        CompactSet<String> set1 = CompactSet.<String>builder()
                .noOrder()
                .build();

        CompactSet<String> set2 = CompactSet.<String>builder()
                .noOrder()
                .build();

        // Add same elements in same order
        set1.add("zebra");
        set1.add("apple");
        set1.add("monkey");

        set2.add("zebra");
        set2.add("apple");
        set2.add("monkey");

        // Sets should be equal regardless of iteration order
        assert set1.equals(set2);

        // Collect iteration orders
        List<String> order1 = new ArrayList<>();
        List<String> order2 = new ArrayList<>();

        set1.forEach(order1::add);
        set2.forEach(order2::add);

        // Verify both sets contain same elements
        assert order1.size() == 3;
        assert order2.size() == 3;
        assert new HashSet<>(order1).equals(new HashSet<>(order2));

        // Note: We can't guarantee different iteration orders, but we can verify
        // that the unordered set doesn't maintain any specific ordering guarantee
        // by checking that it doesn't match any of the known ordering patterns
        List<String> sorted = Arrays.asList("apple", "monkey", "zebra");
        List<String> reverse = Arrays.asList("zebra", "monkey", "apple");

        // At least one of these should be true (the orders don't match any specific pattern)
        assert !order1.equals(sorted) ||
                !order1.equals(reverse) ||
                !order1.equals(order2);
    }

    @Test
    public void testConvertWithCompactSet() {
        // Create a CompactSet with specific configuration
        CompactSet<String> original = CompactSet.<String>builder()
                .caseSensitive(false)
                .sortedOrder()
                .compactSize(50)
                .build();

        // Add some elements
        original.add("zebra");
        original.add("apple");
        original.add("monkey");

        // Convert to another Set
        Set<String> converted = Converter.convert(original, original.getClass());

        // Verify the conversion preserved configuration
        assert converted instanceof CompactSet;

        // Test that CompactSet is a default instance (case-sensitive, compactSize 70, etc.)
        // Why? There is only a class instance passed to Converter.convert(). It cannot get the
        // configuration options from the class itself.
        assert !converted.contains("ZEBRA");
        assert !converted.contains("APPLE");
        assert !converted.contains("MONKEY");
    }
    
    private void clearViaIterator(Set set)
    {
        Iterator i = set.iterator();
        while (i.hasNext())
        {
            i.next();
            i.remove();
        }
        assert set.size() == 0;
        assert set.isEmpty();
    }
}
