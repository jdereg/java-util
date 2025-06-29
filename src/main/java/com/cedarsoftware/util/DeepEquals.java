package com.cedarsoftware.util;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TimeZone;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static com.cedarsoftware.util.Converter.convert2BigDecimal;
import static com.cedarsoftware.util.Converter.convert2boolean;

/**
 * Performs a deep comparison of two objects, going beyond simple {@code equals()} checks.
 * Handles nested objects, collections, arrays, and maps while detecting circular references.
 *
 * <p><strong>Key features:</strong></p>
 * <ul>
 *   <li>Compares entire object graphs including nested structures</li>
 *   <li>Handles circular references safely</li>
 *   <li>Provides detailed difference descriptions for troubleshooting</li>
 *   <li>Supports numeric comparisons with configurable precision</li>
 *   <li>Supports selective ignoring of custom {@code equals()} implementations</li>
 *   <li>Supports string-to-number equality comparisons</li>
 * </ul>
 *
 * <p><strong>Options:</strong></p>
 * <ul>
 *   <li>
 *     <code>IGNORE_CUSTOM_EQUALS</code> (a {@code Set<Class<?>>}):
 *     <ul>
 *       <li><strong>{@code null}</strong> &mdash; Use <em>all</em> custom {@code equals()} methods (ignore none).</li>
 *       <li><strong>Empty set</strong> &mdash; Ignore <em>all</em> custom {@code equals()} methods.</li>
 *       <li><strong>Non-empty set</strong> &mdash; Ignore only those classes’ custom {@code equals()} implementations.</li>
 *     </ul>
 *   </li>
 *   <li>
 *     <code>ALLOW_STRINGS_TO_MATCH_NUMBERS</code> (a {@code Boolean}):
 *     If set to {@code true}, allows strings like {@code "10"} to match the numeric value {@code 10}.
 *   </li>
 * </ul>
 *
 * <p>The options {@code Map} acts as both input and output. When objects differ, the difference
 * description is placed in the options {@code Map} under the "diff" key
 * (see {@link DeepEquals#deepEquals(Object, Object, Map) deepEquals}).</p>
 * <p><strong>"diff" output notes:</strong></p>
 * <ul>
 *  <li>Empty lists, maps, and arrays are shown with (∅) or [∅]</li>
 *  <li>A Map of size 1 is shown as Map(0..0), an int[] of size 2 is shown as int[0..1], an empty list is List(∅)</li>
 *  <li>Sub-object fields on non-difference path shown as {..}</li>
 *  <li>Map entry shown with 《key ⇨ value》 and may be nested</li>
 *  <li>General pattern is [difference type] ▶ root context ▶ shorthand path starting at a root context element (Object field, array/collection element, Map key-value)</li>
 *  <li>If the root is not a container (Collection, Map, Array, or Object), no shorthand description is displayed</li>
 *  </ul>
 * <p><strong>Example usage:</strong></p>
 * <pre><code>
 * Map&lt;String, Object&gt; options = new HashMap&lt;&gt;();
 * options.put(IGNORE_CUSTOM_EQUALS, Set.of(MyClass.class, OtherClass.class));
 * options.put(ALLOW_STRINGS_TO_MATCH_NUMBERS, true);
 *
 * if (!DeepEquals.deepEquals(obj1, obj2, options)) {
 *     String diff = (String) options.get(DeepEquals.DIFF);  // Get difference description
 *     // Handle or log 'diff'
 *
 * Example output:
 * // Simple object difference
 * [field value mismatch] ▶ Person {name: "Jim Bob", age: 27} ▶ .age
 *   Expected: 27
 *   Found: 34
 *   
 * // Array element mismatch within an object that has an array
 * [array element mismatch] ▶ Person {id: 173679590720000287, first: "John", last: "Smith", favoritePet: {..}, pets: Pet[0..1]} ▶ .pets[0].nickNames[0]
 *   Expected: "Edward"
 *   Found: "Eddie"
 *
 * // Map with a different value associated to a key (Map size = 1 noted as 0..0)
 * [map value mismatch] ▶ LinkedHashMap(0..0) ▶ 《"key" ⇨ "value1"》
 *   Expected: "value1"
 *   Found: "value2"
 *   
 * </code></pre>
 *
 * <p><strong>Security and Performance Configuration:</strong></p>
 * <p>DeepEquals provides configurable security and performance options through system properties.
 * All security features are <strong>disabled by default</strong> for backward compatibility:</p>
 * <ul>
 *   <li><code>deepequals.secure.errors=false</code> &mdash; Enable error message sanitization</li>
 *   <li><code>deepequals.max.collection.size=0</code> &mdash; Collection size limit (0=disabled)</li>
 *   <li><code>deepequals.max.array.size=0</code> &mdash; Array size limit (0=disabled)</li>
 *   <li><code>deepequals.max.map.size=0</code> &mdash; Map size limit (0=disabled)</li>
 *   <li><code>deepequals.max.object.fields=0</code> &mdash; Object field count limit (0=disabled)</li>
 *   <li><code>deepequals.max.recursion.depth=0</code> &mdash; Recursion depth limit (0=disabled)</li>
 * </ul>
 *
 * @see #deepEquals(Object, Object)
 * @see #deepEquals(Object, Object, Map)
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
@SuppressWarnings("unchecked")
public class DeepEquals {
    // Option keys
    public static final String IGNORE_CUSTOM_EQUALS = "ignoreCustomEquals";
    public static final String ALLOW_STRINGS_TO_MATCH_NUMBERS = "stringsCanMatchNumbers";
    public static final String DIFF = "diff";
    private static final String EMPTY = "∅";
    private static final String TRIANGLE_ARROW = "▶";
    private static final String ARROW = "⇨";
    private static final String ANGLE_LEFT = "《";
    private static final String ANGLE_RIGHT = "》";
    private static final double SCALE_DOUBLE = Math.pow(10, 10);      // Scale according to epsilon for double
    private static final float SCALE_FLOAT = (float) Math.pow(10, 5); // Scale according to epsilon for float

    private static final ThreadLocal<Set<Object>> formattingStack = ThreadLocal.withInitial(() ->
            Collections.newSetFromMap(new IdentityHashMap<>()));
    
    // Epsilon values for floating-point comparisons
    private static final double doubleEpsilon = 1e-15;
    
    // Configuration for security-safe error messages
    private static final boolean SECURE_ERROR_MESSAGES = Boolean.parseBoolean(
            System.getProperty("deepequals.secure.errors", "false"));
    
    // Fields that should be redacted in error messages for security
    private static final Set<String> SENSITIVE_FIELD_NAMES = CollectionUtilities.setOf(
            "password", "pwd", "passwd", "secret", "key", "token", "credential", 
            "auth", "authorization", "authentication", "api_key", "apikey"
    );
    
    // Security limits to prevent memory exhaustion attacks
    // 0 or negative values = disabled, positive values = enabled with limit
    private static final int MAX_COLLECTION_SIZE = Integer.parseInt(
            System.getProperty("deepequals.max.collection.size", "0"));
    private static final int MAX_ARRAY_SIZE = Integer.parseInt(
            System.getProperty("deepequals.max.array.size", "0"));
    private static final int MAX_MAP_SIZE = Integer.parseInt(
            System.getProperty("deepequals.max.map.size", "0"));
    private static final int MAX_OBJECT_FIELDS = Integer.parseInt(
            System.getProperty("deepequals.max.object.fields", "0"));
    private static final int MAX_RECURSION_DEPTH = Integer.parseInt(
            System.getProperty("deepequals.max.recursion.depth", "0"));
    
    // Class to hold information about items being compared
    private final static class ItemsToCompare {
        private final Object _key1;
        private final Object _key2;
        private final ItemsToCompare parent;
        private final String fieldName;
        private final int[] arrayIndices;
        private final Object mapKey;
        private final Difference difference;    // New field

        // Modified constructors to include Difference

        // Constructor for root
        private ItemsToCompare(Object k1, Object k2) {
            this(k1, k2, null, null, null, null, null);
        }

        // Constructor for differences where the Difference does not need additional information
        private ItemsToCompare(Object k1, Object k2, ItemsToCompare parent, Difference difference) {
            this(k1, k2, parent, null, null, null, difference);
        }

        // Constructor for field access with difference
        private ItemsToCompare(Object k1, Object k2, String fieldName, ItemsToCompare parent, Difference difference) {
            this(k1, k2, parent, fieldName, null, null, difference);
        }

        // Constructor for array access with difference
        private ItemsToCompare(Object k1, Object k2, int[] indices, ItemsToCompare parent, Difference difference) {
            this(k1, k2, parent, null, indices, null, difference);
        }

        // Constructor for map access with difference
        private ItemsToCompare(Object k1, Object k2, Object mapKey, ItemsToCompare parent, boolean isMapKey, Difference difference) {
            this(k1, k2, parent, null, null, mapKey, difference);
        }

        // Base constructor
        private ItemsToCompare(Object k1, Object k2, ItemsToCompare parent,
                               String fieldName, int[] arrayIndices, Object mapKey, Difference difference) {
            this._key1 = k1;
            this._key2 = k2;
            this.parent = parent;
            this.fieldName = fieldName;
            this.arrayIndices = arrayIndices;
            this.mapKey = mapKey;
            this.difference = difference;
        }

        @Override
        public boolean equals(Object other) {
            if (!(other instanceof ItemsToCompare)) {
                return false;
            }
            ItemsToCompare that = (ItemsToCompare) other;

            // Only compare the actual objects being compared (by identity)
            return _key1 == that._key1 && _key2 == that._key2;
        }

        @Override
        public int hashCode() {
            return System.identityHashCode(_key1) * 31 + System.identityHashCode(_key2);
        }
    }

    /**
     * Performs a deep comparison between two objects, going beyond a simple {@code equals()} check.
     * <p>
     * This method is functionally equivalent to calling
     * {@link #deepEquals(Object, Object, Map) deepEquals(a, b, new HashMap&lt;&gt;())},
     * which means it uses no additional comparison options. In other words:
     * <ul>
     *   <li>{@code IGNORE_CUSTOM_EQUALS} is not set (all custom {@code equals()} methods are used)</li>
     *   <li>{@code ALLOW_STRINGS_TO_MATCH_NUMBERS} defaults to {@code false}</li>
     * </ul>
     * </p>
     *
     * @param a the first object to compare, may be {@code null}
     * @param b the second object to compare, may be {@code null}
     * @return {@code true} if the two objects are deeply equal, {@code false} otherwise
     * @see #deepEquals(Object, Object, Map)
     */
    public static boolean deepEquals(Object a, Object b) {
        return deepEquals(a, b, new HashMap<>());
    }

    /**
     * Performs a deep comparison between two objects with optional comparison settings.
     * <p>
     * In addition to comparing objects, collections, maps, and arrays for equality of nested
     * elements, this method can also:
     * <ul>
     *   <li>Ignore certain classes' custom {@code equals()} methods according to user-defined rules</li>
     *   <li>Allow string-to-number comparisons (e.g., {@code "10"} equals {@code 10})</li>
     *   <li>Handle floating-point comparisons with tolerance for precision</li>
     *   <li>Detect and handle circular references to avoid infinite loops</li>
     * </ul>
     *
     * <p><strong>Options:</strong></p>
     * <ul>
     *   <li>
     *     {@code DeepEquals.IGNORE_CUSTOM_EQUALS} (a {@code Collection<Class<?>>}):
     *     <ul>
     *       <li><strong>{@code null}</strong> &mdash; Use <em>all</em> custom {@code equals()} methods (ignore none). Default.</li>
     *       <li><strong>Empty set</strong> &mdash; Ignore <em>all</em> custom {@code equals()} methods.</li>
     *       <li><strong>Non-empty set</strong> &mdash; Ignore only those classes’ custom {@code equals()} implementations.</li>
     *     </ul>
     *   </li>
     *   <li>
     *     {@code DeepEquals.ALLOW_STRINGS_TO_MATCH_NUMBERS} (a {@code Boolean}):
     *     If set to {@code true}, allows strings like {@code "10"} to match the numeric value {@code 10}. Default false.
     *   </li>
     * </ul>
     *
     * <p>If the objects differ, a difference description string is stored in {@code options}
     * under the key {@code "diff"}. The key {@code "diff_item"} can provide additional context
     * regarding the specific location of the mismatch.</p>
     *
     * @param a       the first object to compare, may be {@code null}
     * @param b       the second object to compare, may be {@code null}
     * @param options a map of comparison options and, on return, possibly difference details
     * @return {@code true} if the two objects are deeply equal, {@code false} otherwise
     * @see #deepEquals(Object, Object)
     */
    public static boolean deepEquals(Object a, Object b, Map<String, ?> options) {
        try {
            Set<Object> visited = new HashSet<>();
            return deepEquals(a, b, options, visited);
        } finally {
            formattingStack.remove();   // Always remove.  When needed next time, initialValue() will be called.
        }
    }

    private static boolean deepEquals(Object a, Object b, Map<String, ?> options, Set<Object> visited) {
        Deque<ItemsToCompare> stack = new LinkedList<>();
        boolean result = deepEquals(a, b, stack, options, visited, 0);

        boolean isRecurive = Objects.equals(true, options.get("recursive_call"));
        if (!result && !stack.isEmpty()) {
            // Store both the breadcrumb and the difference ItemsToCompare
            ItemsToCompare top = stack.peek();
            String breadcrumb = generateBreadcrumb(stack);
            ((Map<String, Object>) options).put(DIFF, breadcrumb);
            ((Map<String, Object>) options).put("diff_item", top);
        }

        return result;
    }

    // Recursive deepEquals implementation
    private static boolean deepEquals(Object a, Object b, Deque<ItemsToCompare> stack,
                                      Map<String, ?> options, Set<Object> visited, int depth) {
        Collection<Class<?>> ignoreCustomEquals = (Collection<Class<?>>) options.get(IGNORE_CUSTOM_EQUALS);
        boolean allowAllCustomEquals = ignoreCustomEquals == null;
        boolean hasNonEmptyIgnoreSet = (ignoreCustomEquals != null && !ignoreCustomEquals.isEmpty());
        final boolean allowStringsToMatchNumbers = convert2boolean(options.get(ALLOW_STRINGS_TO_MATCH_NUMBERS));
        
        // Security check: prevent excessive recursion depth
        if (MAX_RECURSION_DEPTH > 0 && depth > MAX_RECURSION_DEPTH) {
            throw new SecurityException("Maximum recursion depth exceeded: " + MAX_RECURSION_DEPTH);
        }
        
        stack.addFirst(new ItemsToCompare(a, b));

        while (!stack.isEmpty()) {
            ItemsToCompare itemsToCompare = stack.peek();

            if (visited.contains(itemsToCompare)) {
                stack.removeFirst();
                continue;
            }
            visited.add(itemsToCompare);

            final Object key1 = itemsToCompare._key1;
            final Object key2 = itemsToCompare._key2;

            // Same instance is always equal to itself, null or otherwise.
            if (key1 == key2) {
                continue;
            }

            // If either one is null, they are not equal (key1 == key2 already checked)
            if (key1 == null || key2 == null) {
                stack.addFirst(new ItemsToCompare(key1, key2, stack.peek(), Difference.VALUE_MISMATCH));
                return false;
            }

            // Handle all numeric comparisons first
            if (key1 instanceof Number && key2 instanceof Number) {
                if (!compareNumbers((Number) key1, (Number) key2)) {
                    stack.addFirst(new ItemsToCompare(key1, key2, stack.peek(), Difference.VALUE_MISMATCH));
                    return false;
                }
                continue;
            }

            // Handle String-to-Number comparison if option is enabled
            if (allowStringsToMatchNumbers &&
                    ((key1 instanceof String && key2 instanceof Number) ||
                            (key1 instanceof Number && key2 instanceof String))) {
                try {
                    if (key1 instanceof String) {
                        if (compareNumbers(convert2BigDecimal(key1), (Number) key2)) {
                            continue;
                        }
                    } else {
                        if (compareNumbers((Number) key1, convert2BigDecimal(key2))) {
                            continue;
                        }
                    }
                } catch (Exception ignore) { }
                stack.addFirst(new ItemsToCompare(key1, key2, stack.peek(), Difference.VALUE_MISMATCH));
                return false;
            }

            if (key1 instanceof AtomicBoolean && key2 instanceof AtomicBoolean) {
                if (!compareAtomicBoolean((AtomicBoolean) key1, (AtomicBoolean) key2)) {
                    stack.addFirst(new ItemsToCompare(key1, key2, stack.peek(), Difference.VALUE_MISMATCH));
                    return false;
                } else {
                    continue;
                }
            }

            Class<?> key1Class = key1.getClass();
            Class<?> key2Class = key2.getClass();

            // Handle primitive wrappers, String, Date, Class, UUID, URL, URI, Temporal classes, etc.
            if (Converter.isSimpleTypeConversionSupported(key1Class)) {
                if (key1 instanceof Comparable && key2 instanceof Comparable) {
                    try {
                        if (((Comparable)key1).compareTo(key2) != 0) {
                            stack.addFirst(new ItemsToCompare(key1, key2, stack.peek(), Difference.VALUE_MISMATCH));
                            return false;
                        }
                        continue;
                    } catch (Exception ignored) { }   // Fall back to equals() if compareTo() fails
                }
                if (!key1.equals(key2)) {
                    stack.addFirst(new ItemsToCompare(key1, key2, stack.peek(), Difference.VALUE_MISMATCH));
                    return false;
                }
                continue;
            }

            // List interface defines order as required as part of equality
            if (key1 instanceof List) {   // If List, the order must match
                if (!(key2 instanceof Collection)) {
                    stack.addFirst(new ItemsToCompare(key1, key2, stack.peek(), Difference.TYPE_MISMATCH));
                    return false;
                }
                if (!decomposeOrderedCollection((Collection<?>) key1, (Collection<?>) key2, stack)) {
                    // Push VALUE_MISMATCH so parent's container-level description (e.g. "collection size mismatch")
                    // takes precedence over element-level differences
                    ItemsToCompare prior = stack.peek();
                    stack.addFirst(new ItemsToCompare(prior._key1, prior._key2, prior, Difference.VALUE_MISMATCH));
                    return false;
                }
                continue;
            } 

            // Unordered Collection comparison
            if (key1 instanceof Collection) {
                if (!(key2 instanceof Collection)) {
                    stack.addFirst(new ItemsToCompare(key1, key2, stack.peek(), Difference.COLLECTION_TYPE_MISMATCH));
                    return false;
                }
                if (!decomposeUnorderedCollection((Collection<?>) key1, (Collection<?>) key2,
                        stack, options, visited)) {
                    // Push VALUE_MISMATCH so parent's container-level description (e.g. "collection size mismatch")
                    // takes precedence over element-level differences
                    ItemsToCompare prior = stack.peek();
                    stack.addFirst(new ItemsToCompare(prior._key1, prior._key2, prior, Difference.VALUE_MISMATCH));
                    return false;
                }
                continue;
            } else if (key2 instanceof Collection) {
                stack.addFirst(new ItemsToCompare(key1, key2, stack.peek(), Difference.COLLECTION_TYPE_MISMATCH));
                return false;
            }

            // Map comparison
            if (key1 instanceof Map) {
                if (!(key2 instanceof Map)) {
                    stack.addFirst(new ItemsToCompare(key1, key2, stack.peek(), Difference.TYPE_MISMATCH));
                    return false;
                }
                if (!decomposeMap((Map<?, ?>) key1, (Map<?, ?>) key2, stack, options, visited)) {
                    // Push VALUE_MISMATCH so parent's container-level description (e.g. "map value mismatch")
                    // takes precedence over element-level differences
                    ItemsToCompare prior = stack.peek();
                    stack.addFirst(new ItemsToCompare(prior._key1, prior._key2, prior, Difference.VALUE_MISMATCH));
                    return false;
                }
                continue;
            } else if (key2 instanceof Map) {
                stack.addFirst(new ItemsToCompare(key1, key2, stack.peek(), Difference.TYPE_MISMATCH));
                return false;
            }

            // Array comparison
            if (key1Class.isArray()) {
                if (!key2Class.isArray()) {
                    stack.addFirst(new ItemsToCompare(key1, key2, stack.peek(), Difference.TYPE_MISMATCH));
                    return false;
                }
                if (!decomposeArray(key1, key2, stack)) {
                    // Push VALUE_MISMATCH so parent's container-level description (e.g. "array element mismatch")
                    // takes precedence over element-level differences
                    ItemsToCompare prior = stack.peek();
                    stack.addFirst(new ItemsToCompare(prior._key1, prior._key2, prior, Difference.VALUE_MISMATCH));
                    return false;
                }
                continue;
            } else if (key2Class.isArray()) {
                stack.addFirst(new ItemsToCompare(key1, key2, stack.peek(), Difference.TYPE_MISMATCH));
                return false;
            }

            // Must be same class if not a container type
            if (!key1Class.equals(key2Class)) {   // Must be same class
                stack.addFirst(new ItemsToCompare(key1, key2, stack.peek(), Difference.TYPE_MISMATCH));
                return false;
            }
            
            // If there is a custom equals and not ignored, compare using custom equals
            if (hasCustomEquals(key1Class)) {
                boolean useCustomEqualsForThisClass = hasNonEmptyIgnoreSet && !ignoreCustomEquals.contains(key1Class);
                if (allowAllCustomEquals || useCustomEqualsForThisClass) {
                    // No Field-by-field break down
                    if (!key1.equals(key2)) {
                        // Custom equals failed. Call "deepEquals()" below on failure of custom equals() above.
                        // This gets us the "detail" on WHY the custom equals failed (first issue).
                        Map<String, Object> newOptions = new HashMap<>(options);
                        newOptions.put("recursive_call", true);

                        // Create new ignore set preserving existing ignored classes
                        Set<Class<?>> ignoreSet = new HashSet<>();
                        if (ignoreCustomEquals != null) {
                            ignoreSet.addAll(ignoreCustomEquals);
                        }
                        ignoreSet.add(key1Class);
                        newOptions.put(IGNORE_CUSTOM_EQUALS, ignoreSet);

                        // Make recursive call to find the actual difference
                        deepEquals(key1, key2, newOptions);

                        // Get the difference and add it to our stack
                        ItemsToCompare diff = (ItemsToCompare) newOptions.get("diff_item");
                        if (diff != null) {
                            stack.addFirst(diff);
                        }
                        return false;
                    }
                    continue;
                }
            }
            
            // Decompose object into its fields (not using custom equals)
            decomposeObject(key1, key2, stack);
        }
        return true;
    }

    /**
     * Compares two unordered collections (e.g., Sets) deeply.
     *
     * @param col1    First collection.
     * @param col2    Second collection.
     * @param stack   Comparison stack.
     * @param options Comparison options.
     * @param visited Visited set used for cycle detection.
     * @return true if collections are equal, false otherwise.
     */
    private static boolean decomposeUnorderedCollection(Collection<?> col1, Collection<?> col2,
                                                       Deque<ItemsToCompare> stack, Map<String, ?> options,
                                                       Set<Object> visited) {
        ItemsToCompare currentItem = stack.peek();

        // Security check: validate collection sizes
        if (MAX_COLLECTION_SIZE > 0 && (col1.size() > MAX_COLLECTION_SIZE || col2.size() > MAX_COLLECTION_SIZE)) {
            throw new SecurityException("Collection size exceeds maximum allowed: " + MAX_COLLECTION_SIZE);
        }

        // Check sizes first
        if (col1.size() != col2.size()) {
            stack.addFirst(new ItemsToCompare(col1, col2, currentItem, Difference.COLLECTION_SIZE_MISMATCH));
            return false;
        }

        // Group col2 items by hash for efficient lookup
        Map<Integer, List<Object>> hashGroups = new HashMap<>();
        for (Object o : col2) {
            int hash = deepHashCode(o);
            hashGroups.computeIfAbsent(hash, k -> new ArrayList<>()).add(o);
        }

        // Find first item in col1 not found in col2
        for (Object item1 : col1) {
            int hash1 = deepHashCode(item1);
            List<Object> candidates = hashGroups.get(hash1);

            if (candidates == null || candidates.isEmpty()) {
                // No hash matches - first difference found
                stack.addFirst(new ItemsToCompare(item1, null, currentItem, Difference.COLLECTION_MISSING_ELEMENT));
                return false;
            }

            // Check candidates with matching hash
            boolean foundMatch = false;
            for (Object item2 : candidates) {
                if (deepEquals(item1, item2, options, visited)) {
                    foundMatch = true;
                    candidates.remove(item2);
                    if (candidates.isEmpty()) {
                        hashGroups.remove(hash1);
                    }
                    break;
                }
            }

            if (!foundMatch) {
                // No matching element found - first difference found
                stack.addFirst(new ItemsToCompare(item1, null, currentItem, Difference.COLLECTION_MISSING_ELEMENT));
                return false;
            }
        }

        return true;
    }

    private static boolean decomposeOrderedCollection(Collection<?> col1, Collection<?> col2, Deque<ItemsToCompare> stack) {
        ItemsToCompare currentItem = stack.peek();

        // Security check: validate collection sizes
        if (MAX_COLLECTION_SIZE > 0 && (col1.size() > MAX_COLLECTION_SIZE || col2.size() > MAX_COLLECTION_SIZE)) {
            throw new SecurityException("Collection size exceeds maximum allowed: " + MAX_COLLECTION_SIZE);
        }

        // Check sizes first
        if (col1.size() != col2.size()) {
            stack.addFirst(new ItemsToCompare(col1, col2, currentItem, Difference.COLLECTION_SIZE_MISMATCH));
            return false;
        }

        // Push elements in order
        Iterator<?> i1 = col1.iterator();
        Iterator<?> i2 = col2.iterator();
        int index = 0;

        while (i1.hasNext()) {
            Object item1 = i1.next();
            Object item2 = i2.next();

            stack.addFirst(new ItemsToCompare(item1, item2, new int[]{index++}, currentItem, Difference.COLLECTION_ELEMENT_MISMATCH));
        }

        return true;
    }
    
    private static boolean decomposeMap(Map<?, ?> map1, Map<?, ?> map2, Deque<ItemsToCompare> stack, Map<String, ?> options, Set<Object> visited) {
        ItemsToCompare currentItem = stack.peek();

        // Security check: validate map sizes
        if (MAX_MAP_SIZE > 0 && (map1.size() > MAX_MAP_SIZE || map2.size() > MAX_MAP_SIZE)) {
            throw new SecurityException("Map size exceeds maximum allowed: " + MAX_MAP_SIZE);
        }

        // Check sizes first
        if (map1.size() != map2.size()) {
            stack.addFirst(new ItemsToCompare(map1, map2, currentItem, Difference.MAP_SIZE_MISMATCH));
            return false;
        }

        // Build lookup of map2 entries for efficient matching
        Map<Integer, Collection<Map.Entry<?, ?>>> fastLookup = new HashMap<>();
        for (Map.Entry<?, ?> entry : map2.entrySet()) {
            int hash = deepHashCode(entry.getKey());
            fastLookup.computeIfAbsent(hash, k -> new ArrayList<>())
                    .add(new AbstractMap.SimpleEntry<>(entry.getKey(), entry.getValue()));
        }

        // Process map1 entries
        for (Map.Entry<?, ?> entry : map1.entrySet()) {
            Collection<Map.Entry<?, ?>> otherEntries = fastLookup.get(deepHashCode(entry.getKey()));

            // Key not found in map2
            if (otherEntries == null || otherEntries.isEmpty()) {
                stack.addFirst(new ItemsToCompare(entry.getKey(), null, currentItem, Difference.MAP_MISSING_KEY));
                return false;
            }

            // Find matching key in otherEntries
            boolean foundMatch = false;
            Iterator<Map.Entry<?, ?>> iterator = otherEntries.iterator();

            while (iterator.hasNext()) {
                Map.Entry<?, ?> otherEntry = iterator.next();

                // Check if keys are equal
                if (deepEquals(entry.getKey(), otherEntry.getKey(), options, visited)) {
                    // Push value comparison only - keys are known to be equal
                    stack.addFirst(new ItemsToCompare(
                            entry.getValue(),                // map1 value
                            otherEntry.getValue(),           // map2 value
                            entry.getKey(),                  // pass the key as 'mapKey'
                            currentItem,                     // parent
                            true,                            // isMapKey = true
                            Difference.MAP_VALUE_MISMATCH));

                    iterator.remove();
                    if (otherEntries.isEmpty()) {
                        fastLookup.remove(deepHashCode(entry.getKey()));
                    }
                    foundMatch = true;
                    break;
                }
            }

            if (!foundMatch) {
                stack.addFirst(new ItemsToCompare(entry.getKey(), null, currentItem, Difference.MAP_MISSING_KEY));
                return false;
            }
        }

        return true;
    }
    
    /**
     * Breaks an array into comparable pieces.
     *
     * @param array1        First array.
     * @param array2        Second array.
     * @param stack         Comparison stack.
     * @return true if arrays are equal, false otherwise.
     */
    private static boolean decomposeArray(Object array1, Object array2, Deque<ItemsToCompare> stack) {
        ItemsToCompare currentItem = stack.peek();  // This will be the parent

        // 1. Check dimensionality
        Class<?> type1 = array1.getClass();
        Class<?> type2 = array2.getClass();
        int dim1 = 0, dim2 = 0;
        while (type1.isArray()) {
            dim1++;
            type1 = type1.getComponentType();
        }
        while (type2.isArray()) {
            dim2++;
            type2 = type2.getComponentType();
        }

        if (dim1 != dim2) {
            stack.addFirst(new ItemsToCompare(array1, array2, currentItem, Difference.ARRAY_DIMENSION_MISMATCH));
            return false;
        }

        // 2. Check component types
        if (!array1.getClass().getComponentType().equals(array2.getClass().getComponentType())) {
            stack.addFirst(new ItemsToCompare(array1, array2, currentItem, Difference.ARRAY_COMPONENT_TYPE_MISMATCH));
            return false;
        }

        // 3. Check lengths
        int len1 = Array.getLength(array1);
        int len2 = Array.getLength(array2);
        
        // Security check: validate array sizes
        if (MAX_ARRAY_SIZE > 0 && (len1 > MAX_ARRAY_SIZE || len2 > MAX_ARRAY_SIZE)) {
            throw new SecurityException("Array size exceeds maximum allowed: " + MAX_ARRAY_SIZE);
        }
        
        if (len1 != len2) {
            stack.addFirst(new ItemsToCompare(array1, array2, currentItem, Difference.ARRAY_LENGTH_MISMATCH));
            return false;
        }

        // 4. Push all elements onto stack (with their full dimensional indices)
        for (int i = len1 - 1; i >= 0; i--) {
            stack.addFirst(new ItemsToCompare(Array.get(array1, i), Array.get(array2, i),
                    new int[]{i},    // For multidimensional arrays, this gets built up
                    currentItem, Difference.ARRAY_ELEMENT_MISMATCH));
        }

        return true;
    }

    private static boolean decomposeObject(Object obj1, Object obj2, Deque<ItemsToCompare> stack) {
        ItemsToCompare currentItem = stack.peek();

        // Get all fields from the object
        Collection<Field> fields = ReflectionUtils.getAllDeclaredFields(obj1.getClass());

        // Security check: validate field count
        if (MAX_OBJECT_FIELDS > 0 && fields.size() > MAX_OBJECT_FIELDS) {
            throw new SecurityException("Object field count exceeds maximum allowed: " + MAX_OBJECT_FIELDS);
        }

        // Push each field for comparison
        for (Field field : fields) {
            try {
                if (field.isSynthetic()) {
                    continue;
                }
                Object value1 = field.get(obj1);
                Object value2 = field.get(obj2);

                stack.addFirst(new ItemsToCompare(value1, value2, field.getName(), currentItem, Difference.FIELD_VALUE_MISMATCH));
            } catch (Exception ignored) {
            }
        }

        return true;
    }
    
    /**
     * Compares two numbers deeply, handling floating point precision.
     *
     * @param a First number.
     * @param b Second number.
     * @return true if numbers are equal within the defined precision, false otherwise.
     */
    private static boolean compareNumbers(Number a, Number b) {
        // Handle floating point comparisons
        if (a instanceof Float || a instanceof Double ||
                b instanceof Float || b instanceof Double) {

            // Check for overflow/underflow when comparing with BigDecimal
            if (a instanceof BigDecimal || b instanceof BigDecimal) {
                try {
                    BigDecimal bd;
                    if (a instanceof BigDecimal) {
                        bd = (BigDecimal) a;
                    } else {
                        bd = (BigDecimal) b;
                    }

                    // If BigDecimal is outside Double's range, they can't be equal
                    if (bd.compareTo(BigDecimal.valueOf(Double.MAX_VALUE)) > 0 ||
                            bd.compareTo(BigDecimal.valueOf(-Double.MAX_VALUE)) < 0) {
                        return false;
                    }
                } catch (Exception e) {
                    return false;
                }
            }

            // Normal floating point comparison
            double d1 = a.doubleValue();
            double d2 = b.doubleValue();
            return nearlyEqual(d1, d2, doubleEpsilon);
        }

        // For non-floating point numbers, use exact comparison
        try {
            BigDecimal x = convert2BigDecimal(a);
            BigDecimal y = convert2BigDecimal(b);
            return x.compareTo(y) == 0;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Correctly handles floating point comparisons.
     *
     * @param a       First number.
     * @param b       Second number.
     * @param epsilon Tolerance value.
     * @return true if numbers are nearly equal within the tolerance, false otherwise.
     */
    private static boolean nearlyEqual(double a, double b, double epsilon) {
        final double absA = Math.abs(a);
        final double absB = Math.abs(b);
        final double diff = Math.abs(a - b);

        if (a == b) { // shortcut, handles infinities
            return true;
        } else if (a == 0 || b == 0 || diff < Double.MIN_NORMAL) {
            // a or b is zero or both are extremely close to it
            // relative error is less meaningful here
            return diff < (epsilon * Double.MIN_NORMAL);
        } else { // use relative error
            return diff / (absA + absB) < epsilon;
        }
    }

    /**
     * Compares two AtomicBoolean instances.
     *
     * @param a First AtomicBoolean.
     * @param b Second AtomicBoolean.
     * @return true if both have the same value, false otherwise.
     */
    private static boolean compareAtomicBoolean(AtomicBoolean a, AtomicBoolean b) {
        return a.get() == b.get();
    }

    /**
     * Determines whether the given class has a custom {@code equals(Object)} method
     * distinct from {@code Object.equals(Object)}.
     * <p>
     * Useful for detecting when a class relies on a specialized equality definition,
     * which can be selectively ignored by deep-comparison if desired.
     * </p>
     *
     * @param c the class to inspect, must not be {@code null}
     * @return {@code true} if {@code c} declares its own {@code equals(Object)} method,
     *         {@code false} otherwise
     */
    public static boolean hasCustomEquals(Class<?> c) {
        Method equals = ReflectionUtils.getMethod(c, "equals", Object.class);   // cached
        return equals.getDeclaringClass() != Object.class;
    }

    /**
     * Determines whether the given class has a custom {@code hashCode()} method
     * distinct from {@code Object.hashCode()}.
     * <p>
     * This method helps identify classes that rely on a specialized hashing algorithm,
     * which can be relevant for certain comparison or hashing scenarios.
     * </p>
     *
     * <p>
     * <b>Usage Example:</b>
     * </p>
     * <pre>{@code
     * Class<?> clazz = MyCustomClass.class;
     * boolean hasCustomHashCode = hasCustomHashCodeMethod(clazz);
     * System.out.println("Has custom hashCode(): " + hasCustomHashCode);
     * }</pre>
     *
     * <p>
     * <b>Notes:</b>
     * </p>
     * <ul>
     *   <li>
     *     A class is considered to have a custom {@code hashCode()} method if it declares
     *     its own {@code hashCode()} method that is not inherited directly from {@code Object}.
     *   </li>
     *   <li>
     *     This method does not consider interfaces or abstract classes unless they declare
     *     a {@code hashCode()} method.
     *   </li>
     * </ul>
     *
     * @param c the class to inspect, must not be {@code null}
     * @return {@code true} if {@code c} declares its own {@code hashCode()} method,
     *         {@code false} otherwise
     * @throws IllegalArgumentException if the provided class {@code c} is {@code null}
     * @see Object#hashCode()
     */
    public static boolean hasCustomHashCode(Class<?> c) {
        Method hashCode = ReflectionUtils.getMethod(c, "hashCode");   // cached
        return hashCode.getDeclaringClass() != Object.class;
    }

    /**
     * Computes a deep hash code for the given object by traversing its entire graph.
     * <p>
     * This method considers the hash codes of nested objects, arrays, maps, and collections,
     * and uses cyclic reference detection to avoid infinite loops.
     * </p>
     * <p>
     * While deepHashCode() enables O(n) comparison performance in DeepEquals() when comparing
     * unordered collections and maps, it does not guarantee that objects which are deepEquals()
     * will have matching deepHashCode() values. This design choice allows for optimized
     * performance while maintaining correctness of equality comparisons.
     * </p>
     * <p>
     * You can use it for generating your own hashCodes() on complex items, but understand that
     * it *always* calls an instants hashCode() method if it has one that override's the
     * hashCode() method defined on Object.class.
     * </p>
     * @param obj the object to hash, may be {@code null}
     * @return an integer representing the object's deep hash code
     */
    public static int deepHashCode(Object obj) {
        Set<Object> visited = Collections.newSetFromMap(new IdentityHashMap<>());
        return deepHashCode(obj, visited);
    }

    private static int deepHashCode(Object obj, Set<Object> visited) {
        Deque<Object> stack = new LinkedList<>();
        stack.addFirst(obj);
        int hash = 0;

        while (!stack.isEmpty()) {
            obj = stack.removeFirst();
            if (obj == null || visited.contains(obj)) {
                continue;
            }

            visited.add(obj);

            // Ensure array order matters to hash
            if (obj.getClass().isArray()) {
                final int len = Array.getLength(obj);
                long result = 1;

                for (int i = 0; i < len; i++) {
                    Object element = Array.get(obj, i);
                    result = 31 * result + hashElement(visited, element);
                }
                hash += (int) result;
                continue;
            }

            // Order matters for List - it is defined as part of equality
            if (obj instanceof List) {
                List<?> col = (List<?>) obj;
                long result = 1;

                for (Object element : col) {
                    result = 31 * result + hashElement(visited, element);
                }
                hash += (int) result;
                continue;
            }

            // Ignore order for non-List Collections (not part of definition of equality)
            if (obj instanceof Collection) {
                addCollectionToStack(stack, (Collection<?>) obj);
                continue;
            }

            if (obj instanceof Map) {
                addCollectionToStack(stack, ((Map<?, ?>) obj).keySet());
                addCollectionToStack(stack, ((Map<?, ?>) obj).values());
                continue;
            }

            if (obj instanceof Float) {
                hash += hashFloat((Float) obj);
                continue;
            } else if (obj instanceof Double) {
                hash += hashDouble((Double) obj);
                continue;
            }

            if (hasCustomHashCode(obj.getClass())) {   // A real hashCode() method exists, call it.
                hash += obj.hashCode();
                continue;
            }

            Collection<Field> fields = ReflectionUtils.getAllDeclaredFields(obj.getClass());
            for (Field field : fields) {
                try {
                    if (field.isSynthetic()) {
                        continue;
                    }
                    stack.addFirst(field.get(obj));
                } catch (Exception ignored) {
                }
            }
        }
        return hash;
    }

    private static int hashElement(Set<Object> visited, Object element) {
        if (element == null) {
            return 0;
        } else if (element instanceof Double) {
            return hashDouble((Double) element);
        } else if (element instanceof Float) {
            return hashFloat((Float) element);
        } else if (Converter.isSimpleTypeConversionSupported(element.getClass())) {
            return element.hashCode();
        } else {
            return deepHashCode(element, visited);
        }
    }

    private static int hashDouble(double value) {
        double normalizedValue = Math.round(value * SCALE_DOUBLE) / SCALE_DOUBLE;
        long bits = Double.doubleToLongBits(normalizedValue);
        return (int) (bits ^ (bits >>> 32));
    }

    private static int hashFloat(float value) {
        float normalizedValue = Math.round(value * SCALE_FLOAT) / SCALE_FLOAT;
        return Float.floatToIntBits(normalizedValue);
    }

    private static void addCollectionToStack(Deque<Object> stack, Collection<?> collection) {
        List<?> items = (collection instanceof List) ? (List<?>) collection : new ArrayList<>(collection);
        for (int i = items.size() - 1; i >= 0; i--) {
            stack.addFirst(items.get(i));
        }
    }

    private enum DiffCategory {
        VALUE,
        TYPE,
        SIZE,
        LENGTH,
        DIMENSION
    }

    private enum Difference {
        // Basic value difference (includes numbers, atomic values, field values)
        VALUE_MISMATCH("value mismatch", DiffCategory.VALUE),
        FIELD_VALUE_MISMATCH("field value mismatch", DiffCategory.VALUE),

        // Collection-specific
        COLLECTION_SIZE_MISMATCH("collection size mismatch", DiffCategory.SIZE),
        COLLECTION_MISSING_ELEMENT("missing collection element", DiffCategory.VALUE),
        COLLECTION_TYPE_MISMATCH("collection type mismatch", DiffCategory.TYPE),
        COLLECTION_ELEMENT_MISMATCH("collection element mismatch", DiffCategory.VALUE),

        // Map-specific
        MAP_SIZE_MISMATCH("map size mismatch", DiffCategory.SIZE),
        MAP_MISSING_KEY("missing map key", DiffCategory.VALUE),
        MAP_VALUE_MISMATCH("map value mismatch", DiffCategory.VALUE),

        // Array-specific
        ARRAY_DIMENSION_MISMATCH("array dimensionality mismatch", DiffCategory.DIMENSION),
        ARRAY_COMPONENT_TYPE_MISMATCH("array component type mismatch", DiffCategory.TYPE),
        ARRAY_LENGTH_MISMATCH("array length mismatch", DiffCategory.LENGTH),
        ARRAY_ELEMENT_MISMATCH("array element mismatch", DiffCategory.VALUE),

        // General type mismatch (when classes don't match)
        TYPE_MISMATCH("type mismatch", DiffCategory.TYPE);

        private final String description;
        private final DiffCategory category;

        Difference(String description, DiffCategory category) {
            this.description = description;
            this.category = category;
        }

        String getDescription() { return description; }
        DiffCategory getCategory() { return category; }
    }

    private static String generateBreadcrumb(Deque<ItemsToCompare> stack) {
        ItemsToCompare diffItem = stack.peek();
        StringBuilder result = new StringBuilder();

        // Build the path AND get the mismatch phrase
        PathResult pr = buildPathContextAndPhrase(diffItem);
        String pathStr = pr.path;

        result.append("[");
        result.append(pr.mismatchPhrase);
        result.append("] ");
        result.append(TRIANGLE_ARROW);
        result.append(" ");
        result.append(pathStr);
        result.append("\n");

        // Format the difference details
        formatDifference(result, diffItem);

        return result.toString();
    }

    private static PathResult buildPathContextAndPhrase(ItemsToCompare diffItem) {
        List<ItemsToCompare> path = getPath(diffItem);
        // path.size is >= 2 always. Even with a root only diff like this deepEquals(4, 5)
        // because there is an initial root stack push, and then all 'false' paths push a
        // descriptive ItemsToCompare() on the stack before returning.

        // 1) Format root
        StringBuilder sb = new StringBuilder();
        ItemsToCompare rootItem = path.get(0);
        sb.append(formatRootObject(rootItem._key1));  // "Dictionary {...}"
        
        // 2) Build up child path
        StringBuilder sb2 = new StringBuilder();
        for (int i = 1; i < path.size(); i++) {
            ItemsToCompare cur = path.get(i);
            
            // If it's a mapKey, we do the " 《 key ⇨ value  》
            if (cur.mapKey != null) {
                appendSpaceIfNeeded(sb2);
                sb2.append(ANGLE_LEFT)
                        .append(formatMapKey(cur.mapKey))
                        .append(" ")
                        .append(ARROW)
                        .append(" ")
                        .append(formatValueConcise(cur._key1))
                        .append(ANGLE_RIGHT);
            }
            // If it's a normal field name
            else if (cur.fieldName != null) {
                sb2.append(".").append(cur.fieldName);
            }
            // If it’s array indices
            else if (cur.arrayIndices != null) {
                for (int idx : cur.arrayIndices) {
                    boolean isArray = cur.difference.name().contains("ARRAY");
                    sb2.append(isArray ? "[" : "(");
                    sb2.append(idx);
                    sb2.append(isArray ? "]" : ")");
                }
            }
        }

        // If we built child path text, attach it after " ▶ "
        if (sb2.length() > 0) {
            sb.append(" ");
            sb.append(TRIANGLE_ARROW);
            sb.append(" ");
            sb.append(sb2);
        }

        // 3) Find the correct mismatch phrase (it will be from the "container" of the difference's pov)
        String mismatchPhrase = getContainingDescription(path);
        return new PathResult(sb.toString(), mismatchPhrase);
    }

    /**
     * Gets the most appropriate difference description from the comparison path.
     * <p>
     * For container types (Arrays, Collections, Maps), the parent node's description
     * often provides better context than the leaf node. For example, an array length
     * mismatch is more informative than a simple value mismatch of its elements.
     * <p>
     * The method looks at the last two nodes in the path:
     * - If only one node exists, uses its description
     * - If two or more nodes exist, prefers the second-to-last node's description
     * - Falls back to the last node's description if the parent's is null
     *
     * @param path The list of ItemsToCompare representing the traversal path to the difference
     * @return The most appropriate difference description, or null if path is empty
     */
    private static String getContainingDescription(List<ItemsToCompare> path) {
        ListIterator<ItemsToCompare> it = path.listIterator(path.size());
        String a = it.previous().difference.getDescription();
        
        if (it.hasPrevious()) {
            Difference diff = it.previous().difference;
            if (diff != null) {
                String b = diff.getDescription();
                if (b != null) {
                    return b;
                }
            }
        }
        return a;
    }

    /**
     * Tiny struct-like class to hold both the path & the mismatch phrase.
     */
    private static class PathResult {
        final String path;
        final String mismatchPhrase;

        PathResult(String path, String mismatchPhrase) {
            this.path = path;
            this.mismatchPhrase = mismatchPhrase;
        }
    }
    
    private static void appendSpaceIfNeeded(StringBuilder sb) {
        if (sb.length() > 0) {
            char last = sb.charAt(sb.length() - 1);
            if (last != ' ' && last != '.' && last != '[') {
                sb.append(' ');
            }
        }
    }

    private static Class<?> getCollectionElementType(Collection<?> col) {
        if (col == null || col.isEmpty()) {
            return null;
        }
        for (Object item : col) {
            if (item != null) {
                return item.getClass();
            }
        }
        return null;
    }
    
    private static List<ItemsToCompare> getPath(ItemsToCompare diffItem) {
        List<ItemsToCompare> path = new ArrayList<>();
        ItemsToCompare current = diffItem;
        while (current != null) {
            path.add(0, current);  // Add to front to maintain root→diff order
            current = current.parent;
        }
        return path;
    }

    private static void formatDifference(StringBuilder result, ItemsToCompare item) {
        if (item.difference == null) {
            return;
        }

        DiffCategory category = item.difference.getCategory();
        if (item.parent.difference != null) {
            category = item.parent.difference.category;
        }
        switch (category) {
            case SIZE:
                result.append(String.format("  Expected size: %d%n  Found size: %d",
                        getContainerSize(item._key1),
                        getContainerSize(item._key2)));
                break;

            case TYPE:
                result.append(String.format("  Expected type: %s%n  Found type: %s",
                        getTypeDescription(item._key1 != null ? item._key1.getClass() : null),
                        getTypeDescription(item._key2 != null ? item._key2.getClass() : null)));
                break;

            case LENGTH:
                result.append(String.format("  Expected length: %d%n  Found length: %d",
                        Array.getLength(item._key1),
                        Array.getLength(item._key2)));
                break;

            case DIMENSION:
                result.append(String.format("  Expected dimensions: %d%n  Found dimensions: %d",
                        getDimensions(item._key1),
                        getDimensions(item._key2)));
                break;

            case VALUE:
            default:
                result.append(String.format("  Expected: %s%n  Found: %s",
                        formatDifferenceValue(item._key1),
                        formatDifferenceValue(item._key2)));
                break;
        }
    }
    
    private static String formatDifferenceValue(Object value) {
        if (value == null) {
            return "null";
        }

        // For simple types, show just the value (type is shown in context)
        if (Converter.isSimpleTypeConversionSupported(value.getClass())) {
            return formatSimpleValue(value);
        }

        // For arrays, collections, maps, and complex objects, use concise format
        return formatValueConcise(value);
    }
    
    private static int getDimensions(Object array) {
        if (array == null) return 0;

        int dimensions = 0;
        Class<?> type = array.getClass();
        while (type.isArray()) {
            dimensions++;
            type = type.getComponentType();
        }
        return dimensions;
    }

    private static String formatValueConcise(Object value) {
        if (value == null) {
            return "null";
        }

        try {
            // Handle collections
            if (value instanceof Collection) {
                Collection<?> col = (Collection<?>) value;
                String typeName = value.getClass().getSimpleName();
                return String.format("%s(%s)", typeName,
                        col.isEmpty() ? EMPTY : "0.." + (col.size() - 1));
            }

            // Handle maps
            if (value instanceof Map) {
                Map<?, ?> map = (Map<?, ?>) value;
                String typeName = value.getClass().getSimpleName();
                return String.format("%s(%s)", typeName,
                        map.isEmpty() ? EMPTY : "0.." + (map.size() - 1));
            }

            // Handle arrays
            if (value.getClass().isArray()) {
                int length = Array.getLength(value);
                String typeName = getTypeDescription(value.getClass().getComponentType());
                return String.format("%s[%s]", typeName,
                        length == 0 ? EMPTY : "0.." + (length - 1));
            }

            // Handle simple types
            if (Converter.isSimpleTypeConversionSupported(value.getClass())) {
                return formatSimpleValue(value);
            }

            // For objects, include basic fields
            Collection<Field> fields = ReflectionUtils.getAllDeclaredFields(value.getClass());
            StringBuilder sb = new StringBuilder(value.getClass().getSimpleName());
            sb.append(" {");
            boolean first = true;

            for (Field field : fields) {
                if (field.isSynthetic()) {
                    continue;
                }
                if (!first) sb.append(", ");
                first = false;

                Object fieldValue = field.get(value);
                String fieldName = field.getName();
                
                // Check if field is sensitive and security is enabled
                if (SECURE_ERROR_MESSAGES && isSensitiveField(fieldName)) {
                    sb.append(fieldName).append(": [REDACTED]");
                    continue;
                }
                
                sb.append(fieldName).append(": ");

                if (fieldValue == null) {
                    sb.append("null");
                    continue;
                }

                Class<?> fieldType = field.getType();
                if (Converter.isSimpleTypeConversionSupported(fieldType)) {
                    // Simple type - show value (already has security filtering)
                    sb.append(formatSimpleValue(fieldValue));
                }
                else if (fieldType.isArray()) {
                    // Array - show type and size
                    int length = Array.getLength(fieldValue);
                    String typeName = getTypeDescription(fieldType.getComponentType());
                    sb.append(String.format("%s[%s]", typeName,
                            length == 0 ? EMPTY : "0.." + (length - 1)));
                }
                else if (Collection.class.isAssignableFrom(fieldType)) {
                    // Collection - show type and size
                    Collection<?> col = (Collection<?>) fieldValue;
                    sb.append(String.format("%s(%s)", fieldType.getSimpleName(),
                            col.isEmpty() ? EMPTY : "0.." + (col.size() - 1)));
                }
                else if (Map.class.isAssignableFrom(fieldType)) {
                    // Map - show type and size
                    Map<?, ?> map = (Map<?, ?>) fieldValue;
                    sb.append(String.format("%s(%s)", fieldType.getSimpleName(),
                            map.isEmpty() ? EMPTY : "0.." + (map.size() - 1)));
                }
                else {
                    // Non-simple object - show {..}
                    sb.append("{..}");
                }
            }

            sb.append("}");
            return sb.toString();
        } catch (Exception e) {
            return value.getClass().getSimpleName();
        }
    }

    private static String formatSimpleValue(Object value) {
        if (value == null) return "null";

        if (value instanceof AtomicBoolean) {
            return String.valueOf(((AtomicBoolean) value).get());
        }
        if (value instanceof AtomicInteger) {
            return String.valueOf(((AtomicInteger) value).get());
        }
        if (value instanceof AtomicLong) {
            return String.valueOf(((AtomicLong) value).get());
        }

        if (value instanceof String) {
            String str = (String) value;
            return SECURE_ERROR_MESSAGES ? sanitizeStringValue(str) : "\"" + str + "\"";
        }
        if (value instanceof Character) return "'" + value + "'";
        if (value instanceof Number) {
            return formatNumber((Number) value);
        }
        if (value instanceof Boolean) return value.toString();
        if (value instanceof Date) {
            return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format((Date)value);
        }
        if (value instanceof TimeZone) {
            TimeZone timeZone = (TimeZone) value;
            return "TimeZone: " + timeZone.getID();
        }
        if (value instanceof URI) {
            return SECURE_ERROR_MESSAGES ? sanitizeUriValue((URI) value) : value.toString();
        }
        if (value instanceof URL) {
            return SECURE_ERROR_MESSAGES ? sanitizeUrlValue((URL) value) : value.toString();
        }
        if (value instanceof UUID) {
            return value.toString();  // UUID is generally safe to display
        }

        // For other types, show type and sanitized toString if security enabled
        if (SECURE_ERROR_MESSAGES) {
            return value.getClass().getSimpleName() + ":[REDACTED]";
        }
        return value.getClass().getSimpleName() + ":" + value;
    }
    
    private static String formatValue(Object value) {
        if (value == null) return "null";

        // Check if we're already formatting this object
        Set<Object> stack = formattingStack.get();
        if (!stack.add(value)) {
            return "<circular " + value.getClass().getSimpleName() + ">";
        }

        try {
            if (value instanceof Number) {
                return formatNumber((Number) value);
            }

            if (value instanceof String) return "\"" + value + "\"";
            if (value instanceof Character) return "'" + value + "'";

            if (value instanceof Date) {
                return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format((Date)value);
            }

            // If it's a simple type, use toString()
            if (Converter.isSimpleTypeConversionSupported(value.getClass())) {
                return String.valueOf(value);
            }

            if (value instanceof Collection) {
                return formatCollectionContents((Collection<?>) value);
            }

            if (value instanceof Map) {
                return formatMapContents((Map<?, ?>) value);
            }
            
            if (value.getClass().isArray()) {
                return formatArrayContents(value);
            }
            return formatComplexObject(value);
        } finally {
            stack.remove(value);
        }
    }

    private static String formatArrayContents(Object array) {
        final int limit = 3;

        // Get base type
        Class<?> type = array.getClass();
        Class<?> componentType = type;
        while (componentType.getComponentType() != null) {
            componentType = componentType.getComponentType();
        }

        StringBuilder sb = new StringBuilder();
        sb.append(componentType.getSimpleName());  // Base type (int, String, etc)

        // Only show outer dimensions
        int outerLength = Array.getLength(array);
        sb.append("[").append(outerLength).append("]");
        Class<?> current = type.getComponentType();
        while (current != null && current.isArray()) {
            sb.append("[]");
            current = current.getComponentType();
        }

        // Add contents
        sb.append("{");
        int length = Array.getLength(array);  // Using original array here
        if (length > 0) {
            int showItems = Math.min(length, limit);
            for (int i = 0; i < showItems; i++) {
                if (i > 0) sb.append(", ");
                Object item = Array.get(array, i);
                if (item == null) {
                    sb.append("null");
                } else if (item.getClass().isArray()) {
                    // For sub-arrays, just show their contents in brackets
                    int subLength = Array.getLength(item);
                    sb.append('[');
                    for (int j = 0; j < Math.min(subLength, limit); j++) {
                        if (j > 0) sb.append(", ");
                        sb.append(formatValue(Array.get(item, j)));
                    }
                    if (subLength > 3) sb.append(", ...");
                    sb.append(']');
                } else {
                    sb.append(formatValue(item));
                }
            }
            if (length > 3) sb.append(", ...");
        }
        sb.append("}");

        return sb.toString();
    }

    private static String formatCollectionContents(Collection<?> collection) {
        final int limit = 3;
        StringBuilder sb = new StringBuilder();

        // Get collection type and element type
        Class<?> type = collection.getClass();
        Type elementType = getCollectionElementType(collection);
        sb.append(type.getSimpleName());
        if (elementType != null) {
            sb.append("<").append(getTypeSimpleName(elementType)).append(">");
        }

        // Add size
        sb.append("(").append(collection.size()).append(")");

        // Add contents
        sb.append("{");
        if (!collection.isEmpty()) {
            Iterator<?> it = collection.iterator();
            int count = 0;
            while (count < limit && it.hasNext()) {
                if (count > 0) sb.append(", ");
                Object item = it.next();
                if (item == null) {
                    sb.append("null");
                } else if (item instanceof Collection) {
                    Collection<?> subCollection = (Collection<?>) item;
                    sb.append("(");
                    Iterator<?> subIt = subCollection.iterator();
                    for (int j = 0; j < Math.min(subCollection.size(), limit); j++) {
                        if (j > 0) sb.append(", ");
                        sb.append(formatValue(subIt.next()));
                    }
                    if (subCollection.size() > limit) sb.append(", ...");
                    sb.append(")");
                } else {
                    sb.append(formatValue(item));
                }
                count++;
            }
            if (collection.size() > limit) sb.append(", ...");
        }
        sb.append("}");

        return sb.toString();
    }

    private static String formatMapContents(Map<?, ?> map) {
        final int limit = 3;
        StringBuilder sb = new StringBuilder();

        // Get map type and key/value types
        Class<?> type = map.getClass();
        Type[] typeArgs = getMapTypes(map);

        sb.append(type.getSimpleName());
        if (typeArgs != null && typeArgs.length == 2) {
            sb.append("<")
                    .append(getTypeSimpleName(typeArgs[0]))
                    .append(", ")
                    .append(getTypeSimpleName(typeArgs[1]))
                    .append(">");
        }

        // Add size in parentheses
        sb.append("(").append(map.size()).append(")");

        // Add contents
        if (!map.isEmpty()) {
            Iterator<? extends Map.Entry<?, ?>> it = map.entrySet().iterator();
            int count = 0;
            while (count < limit && it.hasNext()) {
                if (count > 0) sb.append(", ");
                Map.Entry<?, ?> entry = it.next();
                sb.append(ANGLE_LEFT)
                        .append(formatValue(entry.getKey()))
                        .append(" ")
                        .append(ARROW)
                        .append(" ")
                        .append(formatValue(entry.getValue()))
                        .append(ANGLE_RIGHT);
                count++;
            }
            if (map.size() > limit) sb.append(", ...");
        }

        return sb.toString();
    }

    private static String getTypeSimpleName(Type type) {
        if (type instanceof Class) {
            return ((Class<?>) type).getSimpleName();
        }
        return type.getTypeName();
    }
    
    private static String formatComplexObject(Object obj) {
        StringBuilder sb = new StringBuilder();
        sb.append(obj.getClass().getSimpleName());
        sb.append(" {");

        Collection<Field> fields = ReflectionUtils.getAllDeclaredFields(obj.getClass());
        boolean first = true;

        for (Field field : fields) {
            try {
                if (field.isSynthetic()) {
                    continue;
                }
                if (!first) {
                    sb.append(", ");
                }
                first = false;

                sb.append(field.getName()).append(": ");
                Object value = field.get(obj);

                if (value == obj) {
                    sb.append("(this ").append(obj.getClass().getSimpleName()).append(")");
                } else {
                    sb.append(formatValue(value));  // Recursive call with cycle detection
                }
            } catch (Exception ignored) {
                // If we can't access a field, skip it
            }
        }

        sb.append("}");
        return sb.toString();
    }

    private static String formatArrayNotation(Object array) {
        if (array == null) return "null";

        int length = Array.getLength(array);
        String typeName = getTypeDescription(array.getClass().getComponentType());
        return String.format("%s[%s]", typeName,
                length == 0 ? EMPTY : "0.." + (length - 1));
    }
    
    private static String formatCollectionNotation(Collection<?> col) {
        StringBuilder sb = new StringBuilder();
        sb.append(col.getClass().getSimpleName());

        // Only add type parameter if it's more specific than Object
        Class<?> elementType = getCollectionElementType(col);
        if (elementType != null && elementType != Object.class) {
            sb.append("<").append(getTypeDescription(elementType)).append(">");
        }

        sb.append("(");
        if (col.isEmpty()) {
            sb.append(EMPTY);
        } else {
            sb.append("0..").append(col.size() - 1);
        }
        sb.append(")");

        return sb.toString();
    }

    private static String formatMapNotation(Map<?, ?> map) {
        if (map == null) return "null";

        StringBuilder sb = new StringBuilder();
        sb.append(map.getClass().getSimpleName());

        sb.append("(");
        if (map.isEmpty()) {
            sb.append(EMPTY);
        } else {
            sb.append("0..").append(map.size() - 1);
        }
        sb.append(")");

        return sb.toString();
    }

    private static String formatMapKey(Object key) {
        if (key == null) return "null";

        // If the key is truly a String, keep quotes
        if (key instanceof String) {
            return "\"" + key + "\"";
        }

        // Otherwise, format the key in a "concise" way,
        // but remove any leading/trailing quotes that come
        // from 'formatValueConcise()' if it decides it's a String.
        String text = formatValue(key);
        return StringUtilities.removeLeadingAndTrailingQuotes(text);
    }

    private static String formatNumber(Number value) {
        if (value == null) return "null";

        if (value instanceof BigDecimal) {
            BigDecimal bd = (BigDecimal) value;
            double doubleValue = bd.doubleValue();

            // Use scientific notation only for very large or very small values
            if (Math.abs(doubleValue) >= 1e16 || (Math.abs(doubleValue) < 1e-6 && doubleValue != 0)) {
                return String.format("%.6e", doubleValue);
            }

            // For values between -1 and 1, ensure we don't use scientific notation
            if (Math.abs(doubleValue) <= 1) {
                return bd.stripTrailingZeros().toPlainString();
            }

            // For other values, use regular decimal notation
            return bd.stripTrailingZeros().toPlainString();
        }

        if (value instanceof Double || value instanceof Float) {
            double d = value.doubleValue();
            if (Math.abs(d) >= 1e16 || (Math.abs(d) < 1e-6 && d != 0)) {
                return String.format("%.6e", d);
            }
            // For doubles, up to 15 decimal places
            if (value instanceof Double) {
                return String.format("%.15g", d).replaceAll("\\.?0+$", "");
            }
            // For floats, up to 7 decimal places
            return String.format("%.7g", d).replaceAll("\\.?0+$", "");
        }

        // For other number types (Integer, Long, etc.), use toString
        return value.toString();
    }

    private static String formatRootObject(Object obj) {
        if (obj == null) {
            return "null";
        }

        // For collections and maps, just show the container notation
        if (obj instanceof Collection) {
            return formatCollectionNotation((Collection<?>)obj);
        }
        if (obj instanceof Map) {
            return formatMapNotation((Map<?,?>)obj);
        }
        if (obj.getClass().isArray()) {
            return formatArrayNotation(obj);
        }

        // For simple types, show type: value
        if (Converter.isSimpleTypeConversionSupported(obj.getClass())) {
            return String.format("%s: %s",
                    getTypeDescription(obj.getClass()),
                    formatSimpleValue(obj));
        }

        // For objects, use the concise format
        return formatValueConcise(obj);
    }
    
    private static String getTypeDescription(Class<?> type) {
        if (type == null) return "Object";  // Default to Object for null types

        if (type.isArray()) {
            Class<?> componentType = type.getComponentType();
            return getTypeDescription(componentType) + "[]";
        }
        return type.getSimpleName();
    }

    private static Type[] getMapTypes(Map<?, ?> map) {
        // Try to get generic types from superclass
        Type type = map.getClass().getGenericSuperclass();
        if (type instanceof ParameterizedType) {
            return ((ParameterizedType) type).getActualTypeArguments();
        }
        return null;
    }
    
    private static int getContainerSize(Object container) {
        if (container == null) return 0;
        if (container instanceof Collection) return ((Collection<?>) container).size();
        if (container instanceof Map) return ((Map<?,?>) container).size();
        if (container.getClass().isArray()) return Array.getLength(container);
        return 0;
    }

    private static String sanitizeStringValue(String str) {
        if (str == null) return "null";
        if (str.length() == 0) return "\"\"";
        
        // Check if string looks like sensitive data
        String lowerStr = str.toLowerCase();
        if (looksLikeSensitiveData(lowerStr)) {
            return "\"[REDACTED:" + str.length() + " chars]\"";
        }
        
        // Limit string length in error messages
        if (str.length() > 100) {
            return "\"" + str.substring(0, 97) + "...\"";
        }
        
        return "\"" + str + "\"";
    }

    private static String sanitizeUriValue(URI uri) {
        if (uri == null) return "null";
        
        String scheme = uri.getScheme();
        String host = uri.getHost();
        int port = uri.getPort();
        String path = uri.getPath();
        
        // Remove query parameters and fragment that might contain sensitive data
        StringBuilder sanitized = new StringBuilder();
        if (scheme != null) {
            sanitized.append(scheme).append("://");
        }
        if (host != null) {
            sanitized.append(host);
        }
        if (port != -1) {
            sanitized.append(":").append(port);
        }
        if (path != null && !path.isEmpty()) {
            sanitized.append(path);
        }
        
        // Indicate if query or fragment was removed
        if (uri.getQuery() != null || uri.getFragment() != null) {
            sanitized.append("?[QUERY_REDACTED]");
        }
        
        return sanitized.toString();
    }

    private static String sanitizeUrlValue(URL url) {
        if (url == null) return "null";
        
        String protocol = url.getProtocol();
        String host = url.getHost();
        int port = url.getPort();
        String path = url.getPath();
        
        // Remove query parameters and fragment that might contain sensitive data
        StringBuilder sanitized = new StringBuilder();
        if (protocol != null) {
            sanitized.append(protocol).append("://");
        }
        if (host != null) {
            sanitized.append(host);
        }
        if (port != -1) {
            sanitized.append(":").append(port);
        }
        if (path != null && !path.isEmpty()) {
            sanitized.append(path);
        }
        
        // Indicate if query was removed
        if (url.getQuery() != null || url.getRef() != null) {
            sanitized.append("?[QUERY_REDACTED]");
        }
        
        return sanitized.toString();
    }

    private static boolean looksLikeSensitiveData(String lowerStr) {
        // Check for patterns that look like sensitive data
        if (lowerStr.matches(".*\\b(password|pwd|secret|token|key|credential|auth)\\b.*")) {
            return true;
        }
        
        // Check for patterns that look like encoded data (base64, hex)
        if (lowerStr.matches("^[a-f0-9]{32,}$") || // Hex patterns 32+ chars
            lowerStr.matches("^[a-zA-Z0-9+/]+=*$")) { // Base64 patterns
            return true;
        }
        
        // Check for UUID patterns
        if (lowerStr.matches("^[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}$")) {
            return false; // UUIDs are generally safe to display
        }
        
        return false;
    }

    private static boolean isSensitiveField(String fieldName) {
        if (fieldName == null) return false;
        String lowerFieldName = fieldName.toLowerCase();
        return SENSITIVE_FIELD_NAMES.contains(lowerFieldName) ||
               lowerFieldName.contains("password") ||
               lowerFieldName.contains("secret") ||
               lowerFieldName.contains("token") ||
               lowerFieldName.contains("key");
    }
}