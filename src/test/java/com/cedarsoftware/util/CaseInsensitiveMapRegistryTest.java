package com.cedarsoftware.util;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.function.Function;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
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
class CaseInsensitiveMapRegistryTest {
    // Define the default registry as per the CaseInsensitiveMap's static initialization
    private static final List<Map.Entry<Class<?>, Function<Integer, ? extends Map<?, ?>>>> defaultRegistry = Arrays.asList(
            new AbstractMap.SimpleEntry<>(Hashtable.class, size -> new Hashtable<>()),
            new AbstractMap.SimpleEntry<>(TreeMap.class, size -> new TreeMap<>()),
            new AbstractMap.SimpleEntry<>(ConcurrentSkipListMap.class, size -> new ConcurrentSkipListMap<>()),
            new AbstractMap.SimpleEntry<>(WeakHashMap.class, size -> new WeakHashMap<>(size)),
            new AbstractMap.SimpleEntry<>(LinkedHashMap.class, size -> new LinkedHashMap<>(size)),
            new AbstractMap.SimpleEntry<>(HashMap.class, size -> new HashMap<>(size)),
            new AbstractMap.SimpleEntry<>(ConcurrentNavigableMapNullSafe.class, size -> new ConcurrentNavigableMapNullSafe<>()),
            new AbstractMap.SimpleEntry<>(ConcurrentHashMapNullSafe.class, size -> new ConcurrentHashMapNullSafe<>(size)),
            new AbstractMap.SimpleEntry<>(ConcurrentNavigableMap.class, size -> new ConcurrentSkipListMap<>()),
            new AbstractMap.SimpleEntry<>(ConcurrentMap.class, size -> new ConcurrentHashMap<>(size)),
            new AbstractMap.SimpleEntry<>(NavigableMap.class, size -> new TreeMap<>()),
            new AbstractMap.SimpleEntry<>(SortedMap.class, size -> new TreeMap<>())
    );

    /**
     * Sets up the default registry before each test to ensure isolation.
     */
    @BeforeEach
    void setUp() {
        // Restore the default registry before each test
        List<Map.Entry<Class<?>, Function<Integer, ? extends Map<?, ?>>>> copyDefault = new ArrayList<>(defaultRegistry);
        try {
            CaseInsensitiveMap.replaceRegistry(copyDefault);
        } catch (Exception e) {
            fail("Failed to set up default registry: " + e.getMessage());
        }
    }

    /**
     * Restores the default registry after each test to maintain test independence.
     */
    @AfterEach
    void tearDown() {
        // Restore the default registry after each test
        List<Map.Entry<Class<?>, Function<Integer, ? extends Map<?, ?>>>> copyDefault = new ArrayList<>(defaultRegistry);
        try {
            CaseInsensitiveMap.replaceRegistry(copyDefault);
        } catch (Exception e) {
            fail("Failed to restore default registry: " + e.getMessage());
        }
    }

    /**
     * Test replacing the registry with a new, smaller list.
     * Verifies that only the new mappings are used and others default to LinkedHashMap.
     */
    @Test
    void testReplaceRegistryWithSmallerList() {
        // Create a new, smaller registry with only TreeMap and LinkedHashMap
        List<Map.Entry<Class<?>, Function<Integer, ? extends Map<?, ?>>>> newRegistry = Arrays.asList(
                new AbstractMap.SimpleEntry<>(TreeMap.class, size -> new TreeMap<>()),
                new AbstractMap.SimpleEntry<>(LinkedHashMap.class, size -> new LinkedHashMap<>(size))
        );

        // Replace the registry
        CaseInsensitiveMap.replaceRegistry(newRegistry);

        // Create a source map of TreeMap type
        Map<String, String> treeSource = new TreeMap<>();
        treeSource.put("One", "1");
        treeSource.put("Two", "2");

        // Create a CaseInsensitiveMap with TreeMap source
        CaseInsensitiveMap<String, String> ciMapTree = new CaseInsensitiveMap<>(treeSource);
        assertTrue(ciMapTree.getWrappedMap() instanceof TreeMap, "Backing map should be TreeMap");
        assertEquals("1", ciMapTree.get("one"));
        assertEquals("2", ciMapTree.get("TWO"));

        // Create a source map of HashMap type, which is not in the new registry
        Map<String, String> hashSource = new HashMap<>();
        hashSource.put("Three", "3");
        hashSource.put("Four", "4");

        // Create a CaseInsensitiveMap with HashMap source
        CaseInsensitiveMap<String, String> ciMapHash = new CaseInsensitiveMap<>(hashSource);
        assertTrue(ciMapHash.getWrappedMap() instanceof LinkedHashMap, "Backing map should default to LinkedHashMap");
        assertEquals("3", ciMapHash.get("three"));
        assertEquals("4", ciMapHash.get("FOUR"));
    }

    /**
     * Test replacing the registry with map types in improper order.
     * Expects an IllegalStateException due to incorrect mapping order.
     */
    @Test
    void testReplaceRegistryWithImproperOrder() {
        // Attempt to replace the registry with HashMap before LinkedHashMap, which is improper
        // since LinkedHashMap is a subclass of HashMap
        List<Map.Entry<Class<?>, Function<Integer, ? extends Map<?, ?>>>> improperRegistry = Arrays.asList(
                new AbstractMap.SimpleEntry<>(HashMap.class, size -> new HashMap<>(size)),
                new AbstractMap.SimpleEntry<>(LinkedHashMap.class, size -> new LinkedHashMap<>(size))
        );

        // Attempt to replace registry and expect IllegalStateException due to improper order
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            CaseInsensitiveMap.replaceRegistry(improperRegistry);
        });

        assertTrue(exception.getMessage().contains("should come before"), "Exception message should indicate mapping order error");
    }

    /**
     * Test replacing the registry with a list that includes IdentityHashMap.
     * Expects an IllegalStateException because IdentityHashMap is unsupported.
     */
    @Test
    void testReplaceRegistryWithIdentityHashMap() {
        // Attempt to replace the registry with IdentityHashMap included, which is not allowed
        List<Map.Entry<Class<?>, Function<Integer, ? extends Map<?, ?>>>> invalidRegistry = Arrays.asList(
                new AbstractMap.SimpleEntry<>(TreeMap.class, size -> new TreeMap<>()),
                new AbstractMap.SimpleEntry<>(IdentityHashMap.class, size -> new IdentityHashMap<>())
        );

        // Attempt to replace registry and expect IllegalStateException due to IdentityHashMap
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            CaseInsensitiveMap.replaceRegistry(invalidRegistry);
        });

        assertTrue(exception.getMessage().contains("IdentityHashMap is not supported"), "Exception message should indicate IdentityHashMap is not supported");
    }

    /**
     * Test replacing the registry with map types in the correct order.
     * Verifies that no exception is thrown and the registry is updated correctly.
     */
    @Test
    void testReplaceRegistryWithProperOrder() {
        // Define a new registry with LinkedHashMap followed by HashMap (proper order: more specific before general)
        List<Map.Entry<Class<?>, Function<Integer, ? extends Map<?, ?>>>> properRegistry = Arrays.asList(
                new AbstractMap.SimpleEntry<>(LinkedHashMap.class, size -> new LinkedHashMap<>(size)),
                new AbstractMap.SimpleEntry<>(HashMap.class, size -> new HashMap<>(size))
        );

        // Replace the registry and expect no exception
        assertDoesNotThrow(() -> {
            CaseInsensitiveMap.replaceRegistry(properRegistry);
        }, "Replacing registry with proper order should not throw an exception");

        // Create a source map of LinkedHashMap type
        Map<String, String> linkedSource = new LinkedHashMap<>();
        linkedSource.put("Five", "5");
        linkedSource.put("Six", "6");

        // Create a CaseInsensitiveMap with LinkedHashMap source
        CaseInsensitiveMap<String, String> ciMapLinked = new CaseInsensitiveMap<>(linkedSource);
        assertInstanceOf(LinkedHashMap.class, ciMapLinked.getWrappedMap(), "Backing map should be LinkedHashMap");
        assertEquals("5", ciMapLinked.get("five"));
        assertEquals("6", ciMapLinked.get("SIX"));

        // Create a source map of HashMap type
        Map<String, String> hashSource = new HashMap<>();
        hashSource.put("Seven", "7");
        hashSource.put("Eight", "8");

        // Create a CaseInsensitiveMap with HashMap source
        CaseInsensitiveMap<String, String> ciMapHash = new CaseInsensitiveMap<>(hashSource);
        assertInstanceOf(HashMap.class, ciMapHash.getWrappedMap(), "Backing map should be HashMap");
        assertEquals("7", ciMapHash.get("seven"));
        assertEquals("8", ciMapHash.get("EIGHT"));
    }

    /**
     * Test attempting to replace the registry with a list containing a non-map class.
     * Expects a NullPointerException or IllegalArgumentException.
     */
    @Test
    void testReplaceRegistryWithNullEntries() {
        // Attempt to replace the registry with a null list
        assertThrows(NullPointerException.class, () -> {
            CaseInsensitiveMap.replaceRegistry(null);
        }, "Replacing registry with null should throw NullPointerException");

        // Attempt to replace the registry with a list containing null entries
        List<Map.Entry<Class<?>, Function<Integer, ? extends Map<?, ?>>>> registryWithNull = Arrays.asList(
                new AbstractMap.SimpleEntry<>(TreeMap.class, size -> new TreeMap<>()),
                null
        );

        assertThrows(NullPointerException.class, () -> {
            CaseInsensitiveMap.replaceRegistry(registryWithNull);
        }, "Replacing registry with null entries should throw NullPointerException");
    }

    /**
     * Test attempting to replace the registry with a list containing duplicate map types.
     * Expects an IllegalArgumentException.
     */
    @Test
    void testReplaceRegistryWithDuplicateMapTypes() {
        // Attempt to replace the registry with duplicate HashMap entries
        List<Map.Entry<Class<?>, Function<Integer, ? extends Map<?, ?>>>> duplicateRegistry = Arrays.asList(
                new AbstractMap.SimpleEntry<>(HashMap.class, size -> new HashMap<>(size)),
                new AbstractMap.SimpleEntry<>(HashMap.class, size -> new HashMap<>(size))
        );

        // Attempt to replace registry and expect IllegalArgumentException due to duplicates
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            CaseInsensitiveMap.replaceRegistry(duplicateRegistry);
        });

        assertTrue(exception.getMessage().contains("Duplicate map type in registry"), "Exception message should indicate duplicate map types");
    }
}
