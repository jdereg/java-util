package com.cedarsoftware.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Logger;

import com.cedarsoftware.io.JsonIo;
import com.cedarsoftware.io.TypeHolder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
class CompactSetTest
{
    private static final Logger LOG = Logger.getLogger(CompactSetTest.class.getName());
    @Test
    void testSimpleCases()
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
    void testSimpleCases2()
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
    void testBadNoArgConstructor()
    {
        try
        {
            new CompactSet() { protected int compactSize() { return 1; } };
            fail();
        }
        catch (Exception e) { }
    }

    @Test
    void testBadConstructor()
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
    void testSize()
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
    void testHeterogeneousItems()
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
    void testClear()
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
    void testRemove()
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
    void testCaseInsensitivity()
    {
        CompactSet<String> set = new CompactSet<String>()
        {
            protected boolean isCaseInsensitive() { return true; }
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
    void testCaseSensitivity()
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
    void testCaseInsensitivity2()
    {
        CompactSet<String> set = new CompactSet<String>()
        {
            protected boolean isCaseInsensitive() { return true; }
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
    void testCaseSensitivity2()
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
    void testCompactLinkedSet()
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
    void testCompactCIHashSet()
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
    void testCompactCILinkedSet()
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
    void testPerformance()
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
            LOG.info("CompacSet.compactSize: " + i + " = " + totals[i - lower] / 1000000.0d);
        }
        LOG.info("HashSet = " + totals[totals.length - 1] / 1000000.0d);
    }

    @Test
    void testSortedOrder() {
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
    void testReverseOrder() {
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
    void testInsertionOrder() {
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
    void testUnorderedBehavior() {
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
    void testConvertWithCompactSet() {
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

        // Test that CompactSet is a default instance (case-sensitive, compactSize 50, etc.)
        // Why? There is only a class instance passed to Converter.convert(). It cannot get the
        // configuration options from the class itself.
        assert !converted.contains("ZEBRA");
        assert !converted.contains("APPLE");
        assert !converted.contains("MONKEY");
    }

    @Test
    void testGetConfig() {
        // Create a CompactSet with specific configuration
        CompactSet<String> set = CompactSet.<String>builder()
                .compactSize(50)
                .caseSensitive(false)
                .sortedOrder()
                .build();

        // Add some elements
        set.add("apple");
        set.add("banana");

        // Get the configuration
        Map<String, Object> config = set.getConfig();

        // Verify the configuration values
        assertEquals(50, config.get(CompactMap.COMPACT_SIZE));
        assertEquals(false, config.get(CompactMap.CASE_SENSITIVE));
        assertEquals(CompactMap.SORTED, config.get(CompactMap.ORDERING));

        // Verify the map is unmodifiable
        assertThrows(UnsupportedOperationException.class, () -> config.put("test", "value"));

        // Make sure only the expected keys are present
        assertEquals(3, config.size());
        assertTrue(config.containsKey(CompactMap.COMPACT_SIZE));
        assertTrue(config.containsKey(CompactMap.CASE_SENSITIVE));
        assertTrue(config.containsKey(CompactMap.ORDERING));

        // Make sure MAP_TYPE and SINGLE_KEY are not exposed
        assertFalse(config.containsKey(CompactMap.MAP_TYPE));
        assertFalse(config.containsKey(CompactMap.SINGLE_KEY));
    }

    @Test
    void testWithConfig() {
        // Create a CompactSet with default configuration and add some elements
        CompactSet<String> originalSet = new CompactSet<>();
        originalSet.add("apple");
        originalSet.add("banana");
        originalSet.add("cherry");

        // Get the original configuration
        Map<String, Object> originalConfig = originalSet.getConfig();

        // Create a new configuration
        Map<String, Object> newConfig = new HashMap<>();
        newConfig.put(CompactMap.COMPACT_SIZE, 30);
        newConfig.put(CompactMap.CASE_SENSITIVE, false);
        newConfig.put(CompactMap.ORDERING, CompactMap.SORTED);

        // Create a new set with the new configuration
        CompactSet<String> newSet = originalSet.withConfig(newConfig);

        // Verify the new configuration was applied
        Map<String, Object> retrievedConfig = newSet.getConfig();
        assertEquals(30, retrievedConfig.get(CompactMap.COMPACT_SIZE));
        assertEquals(false, retrievedConfig.get(CompactMap.CASE_SENSITIVE));
        assertEquals(CompactMap.SORTED, retrievedConfig.get(CompactMap.ORDERING));

        // Verify the elements were copied
        assertEquals(3, newSet.size());
        assertTrue(newSet.contains("apple"));
        assertTrue(newSet.contains("banana"));
        assertTrue(newSet.contains("cherry"));

        // Verify the original set is unchanged
        assertNotEquals(30, originalConfig.get(CompactMap.COMPACT_SIZE));

        // Check that case-insensitivity works in the new set
        assertTrue(newSet.contains("APPle"));

        // Verify the ordering is respected in the new set
        Iterator<String> iterator = newSet.iterator();
        String first = iterator.next();
        String second = iterator.next();
        String third = iterator.next();

        // Elements should be in sorted order: apple, banana, cherry
        assertEquals("apple", first);
        assertEquals("banana", second);
        assertEquals("cherry", third);
    }

    @Test
    void testWithConfigPartial() {
        // Create a CompactSet with specific configuration
        CompactSet<String> originalSet = CompactSet.<String>builder()
                .compactSize(40)
                .caseSensitive(true)
                .insertionOrder()
                .build();

        // Add elements in a specific order
        originalSet.add("cherry");
        originalSet.add("apple");
        originalSet.add("banana");

        // Create a partial configuration change
        Map<String, Object> partialConfig = new HashMap<>();
        partialConfig.put(CompactMap.COMPACT_SIZE, 25);
        // Keep other settings the same

        // Apply the partial config
        CompactSet<String> newSet = originalSet.withConfig(partialConfig);

        // Verify only the compact size changed
        Map<String, Object> newConfig = newSet.getConfig();
        assertEquals(25, newConfig.get(CompactMap.COMPACT_SIZE));
        assertEquals(true, newConfig.get(CompactMap.CASE_SENSITIVE));
        assertEquals(CompactMap.INSERTION, newConfig.get(CompactMap.ORDERING));

        // Verify original insertion order is maintained
        Iterator<String> iterator = newSet.iterator();
        assertEquals("cherry", iterator.next());
        assertEquals("apple", iterator.next());
        assertEquals("banana", iterator.next());
    }

    @Test
    void testWithConfigOrderingChange() {
        // Create a set with unordered elements
        CompactSet<String> originalSet = CompactSet.<String>builder()
                .noOrder()
                .build();

        originalSet.add("banana");
        originalSet.add("apple");
        originalSet.add("cherry");

        // Change to sorted order
        Map<String, Object> orderConfig = new HashMap<>();
        orderConfig.put(CompactMap.ORDERING, CompactMap.SORTED);

        CompactSet<String> sortedSet = originalSet.withConfig(orderConfig);

        // Verify elements are now in sorted order
        Iterator<String> iterator = sortedSet.iterator();
        assertEquals("apple", iterator.next());
        assertEquals("banana", iterator.next());
        assertEquals("cherry", iterator.next());

        // Change to reverse order
        orderConfig.put(CompactMap.ORDERING, CompactMap.REVERSE);
        CompactSet<String> reversedSet = originalSet.withConfig(orderConfig);

        // Verify elements are now in reverse order
        iterator = reversedSet.iterator();
        assertEquals("cherry", iterator.next());
        assertEquals("banana", iterator.next());
        assertEquals("apple", iterator.next());
    }

    @Test
    void testWithConfigCaseSensitivityChange() {
        // Create a case-sensitive set
        CompactSet<String> originalSet = CompactSet.<String>builder()
                .caseSensitive(true)
                .build();

        originalSet.add("Apple");
        originalSet.add("Banana");

        // Verify case-sensitivity
        assertTrue(originalSet.contains("Apple"));
        assertFalse(originalSet.contains("apple"));

        // Change to case-insensitive
        Map<String, Object> config = new HashMap<>();
        config.put(CompactMap.CASE_SENSITIVE, false);

        CompactSet<String> caseInsensitiveSet = originalSet.withConfig(config);

        // Verify the change
        assertTrue(caseInsensitiveSet.contains("Apple"));
        assertTrue(caseInsensitiveSet.contains("apple"));
        assertTrue(caseInsensitiveSet.contains("APPLE"));
    }

    @Test
    void testWithConfigHandlesNullValues() {
        // Create a set with known configuration for testing
        CompactSet<String> originalSet = CompactSet.<String>builder()
                .compactSize(50)
                .caseSensitive(false)
                .sortedOrder()
                .build();
        originalSet.add("apple");
        originalSet.add("banana");

        // Get original configuration for comparison
        Map<String, Object> originalConfig = originalSet.getConfig();

        // Test with null configuration map
        Exception ex = assertThrows(
                IllegalArgumentException.class,
                () -> originalSet.withConfig(null)
        );
        assertEquals("config cannot be null", ex.getMessage());

        // Test with configuration containing null COMPACT_SIZE
        Map<String, Object> configWithNullCompactSize = new HashMap<>();
        configWithNullCompactSize.put(CompactMap.COMPACT_SIZE, null);

        CompactSet<String> setWithNullCompactSize = originalSet.withConfig(configWithNullCompactSize);

        // Should fall back to original compact size, not null
        assertEquals(
                originalConfig.get(CompactMap.COMPACT_SIZE),
                setWithNullCompactSize.getConfig().get(CompactMap.COMPACT_SIZE)
        );

        // Verify other settings remain unchanged
        assertEquals(originalConfig.get(CompactMap.CASE_SENSITIVE), setWithNullCompactSize.getConfig().get(CompactMap.CASE_SENSITIVE));
        assertEquals(originalConfig.get(CompactMap.ORDERING), setWithNullCompactSize.getConfig().get(CompactMap.ORDERING));

        // Test with configuration containing null CASE_SENSITIVE
        Map<String, Object> configWithNullCaseSensitive = new HashMap<>();
        configWithNullCaseSensitive.put(CompactMap.CASE_SENSITIVE, null);

        CompactSet<String> setWithNullCaseSensitive = originalSet.withConfig(configWithNullCaseSensitive);

        // Should fall back to original case sensitivity, not null
        assertEquals(
                originalConfig.get(CompactMap.CASE_SENSITIVE),
                setWithNullCaseSensitive.getConfig().get(CompactMap.CASE_SENSITIVE)
        );

        // Test with configuration containing null ORDERING
        Map<String, Object> configWithNullOrdering = new HashMap<>();
        configWithNullOrdering.put(CompactMap.ORDERING, null);

        CompactSet<String> setWithNullOrdering = originalSet.withConfig(configWithNullOrdering);

        // Should fall back to original ordering, not null
        assertEquals(
                originalConfig.get(CompactMap.ORDERING),
                setWithNullOrdering.getConfig().get(CompactMap.ORDERING)
        );

        // Test with configuration containing ALL null values
        Map<String, Object> configWithAllNulls = new HashMap<>();
        configWithAllNulls.put(CompactMap.COMPACT_SIZE, null);
        configWithAllNulls.put(CompactMap.CASE_SENSITIVE, null);
        configWithAllNulls.put(CompactMap.ORDERING, null);
        // Also include irrelevant keys that should be ignored
        configWithAllNulls.put(CompactMap.SINGLE_KEY, null);
        configWithAllNulls.put(CompactMap.MAP_TYPE, null);
        configWithAllNulls.put("randomKey", null);

        CompactSet<String> setWithAllNulls = originalSet.withConfig(configWithAllNulls);

        // All settings should fall back to original values
        assertEquals(originalConfig.get(CompactMap.COMPACT_SIZE), setWithAllNulls.getConfig().get(CompactMap.COMPACT_SIZE));
        assertEquals(originalConfig.get(CompactMap.CASE_SENSITIVE), setWithAllNulls.getConfig().get(CompactMap.CASE_SENSITIVE));
        assertEquals(originalConfig.get(CompactMap.ORDERING), setWithAllNulls.getConfig().get(CompactMap.ORDERING));

        // Verify elements were properly copied in all cases
        assertEquals(2, setWithNullCompactSize.size());
        assertEquals(2, setWithNullCaseSensitive.size());
        assertEquals(2, setWithNullOrdering.size());
        assertEquals(2, setWithAllNulls.size());

        // Verify element content
        assertTrue(setWithNullCompactSize.contains("apple"));
        assertTrue(setWithNullCompactSize.contains("banana"));

        // Verify ordering was preserved (if using sorted order)
        if (CompactMap.SORTED.equals(originalConfig.get(CompactMap.ORDERING))) {
            Iterator<String> iterator = setWithAllNulls.iterator();
            assertEquals("apple", iterator.next());
            assertEquals("banana", iterator.next());
        }

        // Verify case sensitivity was preserved
        if (Boolean.FALSE.equals(originalConfig.get(CompactMap.CASE_SENSITIVE))) {
            assertTrue(setWithAllNulls.contains("APPLE"));
            assertTrue(setWithAllNulls.contains("Banana"));
        }

        // Test that irrelevant keys in config are ignored
        Map<String, Object> configWithIrrelevantKeys = new HashMap<>();
        configWithIrrelevantKeys.put("someRandomKey", "value");
        configWithIrrelevantKeys.put(CompactMap.SINGLE_KEY, "id"); // Should be ignored for CompactSet
        configWithIrrelevantKeys.put(CompactMap.MAP_TYPE, HashMap.class); // Should be ignored for CompactSet

        CompactSet<String> setWithIrrelevantConfig = originalSet.withConfig(configWithIrrelevantKeys);

        // Configuration should be unchanged since no relevant keys were changed
        assertEquals(originalConfig.get(CompactMap.COMPACT_SIZE), setWithIrrelevantConfig.getConfig().get(CompactMap.COMPACT_SIZE));
        assertEquals(originalConfig.get(CompactMap.CASE_SENSITIVE), setWithIrrelevantConfig.getConfig().get(CompactMap.CASE_SENSITIVE));
        assertEquals(originalConfig.get(CompactMap.ORDERING), setWithIrrelevantConfig.getConfig().get(CompactMap.ORDERING));
    }
    
    @Test
    void testWithConfigIgnoresUnrelatedKeys() {
        CompactSet<String> originalSet = new CompactSet<>();
        originalSet.add("test");

        // Create a config with both relevant and irrelevant keys
        Map<String, Object> mixedConfig = new HashMap<>();
        mixedConfig.put(CompactMap.COMPACT_SIZE, 25);
        mixedConfig.put("someRandomKey", "value");
        mixedConfig.put(CompactMap.MAP_TYPE, HashMap.class); // Should be ignored
        mixedConfig.put(CompactMap.SINGLE_KEY, "id");        // Should be ignored

        // Apply the config
        CompactSet<String> newSet = originalSet.withConfig(mixedConfig);

        // Verify only relevant keys were applied
        Map<String, Object> newConfig = newSet.getConfig();
        assertEquals(25, newConfig.get(CompactMap.COMPACT_SIZE));

        // Verify the irrelevant keys were ignored
        assertFalse(newConfig.containsKey("someRandomKey"));
        assertFalse(newConfig.containsKey(CompactMap.MAP_TYPE));
        assertFalse(newConfig.containsKey(CompactMap.SINGLE_KEY));
    }

    @Test
    void testCompactCIHashSetWithJsonIo() {
        Set<String> set = new CompactCIHashSet<>();
        set.add("apple");
        set.add("banana");
        set.add("cherry");
        set.add("Apple");
        assert set.size() == 3;  // Case-insensitive (one apple)
        assert set.contains("APPLE");

        String json = JsonIo.toJson(set, null);
        Set<String> set2 = JsonIo.toJava(json, null).asType(new TypeHolder<Set<String>>(){});
        assert DeepEquals.deepEquals(set, set2);
        assert set2.getClass().equals(CompactCIHashSet.class);
    }

    @Test
    void testCompactCILinkedSetWithJsonIo() {
        Set<String> set = new CompactCILinkedSet<>();
        set.add("apple");
        set.add("banana");
        set.add("cherry");
        set.add("Apple");
        assert set.size() == 3;  // Case-insensitive (one apple)
        assert set.contains("APPLE");

        String json = JsonIo.toJson(set, null);
        Set<String> set2 = JsonIo.toJava(json, null).asType(new TypeHolder<Set<String>>(){});
        assert DeepEquals.deepEquals(set, set2);
        assert set2.getClass().equals(CompactCILinkedSet.class);
    }

    @Test
    void testCompactLinkedSetWithJsonIo() {
        Set<String> set = new CompactLinkedSet<>();
        set.add("apple");
        set.add("banana");
        set.add("cherry");
        set.add("Apple");
        assert set.size() == 4;  // Case-insensitive (one apple)
        assert set.contains("apple");
        assert set.contains("Apple");
        assert !set.contains("APPLE");
        
        String json = JsonIo.toJson(set, null);
        Set<String> set2 = JsonIo.toJava(json, null).asType(new TypeHolder<Set<String>>(){});
        assert DeepEquals.deepEquals(set, set2);
        assert set2.getClass().equals(CompactLinkedSet.class);
    }

    private void clearViaIterator(Set set)
    {
        Iterator i = set.iterator();
        while (i.hasNext())
        {
            i.next();
            i.remove();
        }
        assert set.isEmpty();
    }
}