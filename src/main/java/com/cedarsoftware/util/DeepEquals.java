package com.cedarsoftware.util;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.AbstractMap;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;

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
 * Default safeguards are enabled to prevent excessive resource consumption:</p>
 * <ul>
 *   <li><code>deepequals.secure.errors=false</code> &mdash; Enable error message sanitization (default: false)</li>
 *   <li><code>deepequals.max.collection.size=100000</code> &mdash; Collection size limit (default: 100,000, 0=disabled)</li>
 *   <li><code>deepequals.max.array.size=100000</code> &mdash; Array size limit (default: 100,000, 0=disabled)</li>
 *   <li><code>deepequals.max.map.size=100000</code> &mdash; Map size limit (default: 100,000, 0=disabled)</li>
 *   <li><code>deepequals.max.object.fields=1000</code> &mdash; Object field count limit (default: 1,000, 0=disabled)</li>
 *   <li><code>deepequals.max.recursion.depth=1000000</code> &mdash; Recursion depth limit (default: 1,000,000, 0=disabled)</li>
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
    public static final String DIFF_ITEM = "diff_item";
    public static final String INCLUDE_DIFF_ITEM = "deepequals.include.diff_item";
    private static final String DEPTH_BUDGET = "__depthBudget";
    private static final String EMPTY = "∅";
    private static final String TRIANGLE_ARROW = "▶";
    private static final String ARROW = "⇨";
    private static final String ANGLE_LEFT = "《";
    private static final String ANGLE_RIGHT = "》";
    
    // Thread-safe UTC date formatter for consistent formatting across locales
    private static final ThreadLocal<SimpleDateFormat> TS_FMT = 
        ThreadLocal.withInitial(() -> {
            SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ROOT);
            f.setTimeZone(TimeZone.getTimeZone("UTC"));
            return f;
        });
    
    // Strict Base64 pattern that properly validates Base64 encoding
    // Matches strings that are properly padded Base64 (groups of 4 chars with proper padding)
    private static final Pattern BASE64_PATTERN = Pattern.compile(
            "^(?:[A-Za-z0-9+/]{4})*(?:[A-Za-z0-9+/]{2}==|[A-Za-z0-9+/]{3}=)?$");
    
    // Precompiled patterns for sensitive data detection
    // Note: HEX_32_PLUS and UUID_PATTERN use lowercase patterns since strings are lowercased before matching
    private static final Pattern HEX_32_PLUS = Pattern.compile("^[a-f0-9]{32,}$");
    private static final Pattern SENSITIVE_WORDS = Pattern.compile(
            ".*\\b(password|pwd|secret|token|credential|auth|apikey|api_key|secretkey|secret_key|privatekey|private_key)\\b.*");
    private static final Pattern UUID_PATTERN = Pattern.compile(
            "^[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}$");
    
    private static final double SCALE_DOUBLE = 1e12;  // Aligned with DOUBLE_EPSILON (1/epsilon)
    private static final float SCALE_FLOAT = 1e6f;     // Aligned with FLOAT_EPSILON (1/epsilon)

    private static final ThreadLocal<Set<Object>> formattingStack = ThreadLocal.withInitial(() ->
            Collections.newSetFromMap(new IdentityHashMap<>()));
    
    // Epsilon values for floating-point comparisons
    private static final double DOUBLE_EPSILON = 1e-12;
    private static final float FLOAT_EPSILON = 1e-6f;
    
    // Configuration for security-safe error messages - removed static final, now uses dynamic method
    
    // Fields that should be redacted in error messages for security
    // Note: "auth" removed to avoid false positives like "author" - it's caught by SENSITIVE_WORDS regex
    private static final Set<String> SENSITIVE_FIELD_NAMES = CollectionUtilities.setOf(
            "password", "pwd", "passwd", "secret", "token", "credential", 
            "authorization", "authentication", "api_key", "apikey", "secretkey"
    );
    
    // Default security limits
    private static final int DEFAULT_MAX_COLLECTION_SIZE = 100000;
    private static final int DEFAULT_MAX_ARRAY_SIZE = 100000;
    private static final int DEFAULT_MAX_MAP_SIZE = 100000;
    private static final int DEFAULT_MAX_OBJECT_FIELDS = 1000;
    private static final int DEFAULT_MAX_RECURSION_DEPTH = 1000000;  // 1M depth for heap-based traversal
    
    
    // Security configuration methods
    
    private static boolean isSecureErrorsEnabled() {
        return Boolean.parseBoolean(System.getProperty("deepequals.secure.errors", "false"));
    }
    
    private static int getMaxCollectionSize() {
        String maxSizeProp = System.getProperty("deepequals.max.collection.size");
        if (maxSizeProp != null) {
            try {
                int value = Integer.parseInt(maxSizeProp);
                return Math.max(0, value); // 0 means disabled
            } catch (NumberFormatException e) {
                // Fall through to default
            }
        }
        return DEFAULT_MAX_COLLECTION_SIZE;
    }
    
    private static int getMaxArraySize() {
        String maxSizeProp = System.getProperty("deepequals.max.array.size");
        if (maxSizeProp != null) {
            try {
                int value = Integer.parseInt(maxSizeProp);
                return Math.max(0, value); // 0 means disabled
            } catch (NumberFormatException e) {
                // Fall through to default
            }
        }
        return DEFAULT_MAX_ARRAY_SIZE;
    }
    
    private static int getMaxMapSize() {
        String maxSizeProp = System.getProperty("deepequals.max.map.size");
        if (maxSizeProp != null) {
            try {
                int value = Integer.parseInt(maxSizeProp);
                return Math.max(0, value); // 0 means disabled
            } catch (NumberFormatException e) {
                // Fall through to default
            }
        }
        return DEFAULT_MAX_MAP_SIZE;
    }
    
    private static int getMaxObjectFields() {
        String maxFieldsProp = System.getProperty("deepequals.max.object.fields");
        if (maxFieldsProp != null) {
            try {
                int value = Integer.parseInt(maxFieldsProp);
                return Math.max(0, value); // 0 means disabled
            } catch (NumberFormatException e) {
                // Fall through to default
            }
        }
        return DEFAULT_MAX_OBJECT_FIELDS;
    }
    
    private static int getMaxRecursionDepth() {
        String maxDepthProp = System.getProperty("deepequals.max.recursion.depth");
        if (maxDepthProp != null) {
            try {
                int value = Integer.parseInt(maxDepthProp);
                return Math.max(0, value); // 0 means disabled
            } catch (NumberFormatException e) {
                // Fall through to default
            }
        }
        return DEFAULT_MAX_RECURSION_DEPTH;
    }
    
    /**
     * Calculate the depth of the current item in the object graph by counting
     * the number of parent links. This is used for heap-based depth tracking.
     */
    
    // Class to hold information about items being compared
    private final static class ItemsToCompare {
        private final Object _key1;
        private final Object _key2;
        private final ItemsToCompare parent;
        private final String fieldName;
        private final int[] arrayIndices;
        private final Object mapKey;
        private final Difference difference;    // New field
        private final int depth;    // Track depth for recursion limit

        // Modified constructors to include Difference

        // Constructor for root
        private ItemsToCompare(Object k1, Object k2) {
            this(k1, k2, null, null, null, null, null, 0);
        }

        // Constructor for differences where the Difference does not need additional information
        private ItemsToCompare(Object k1, Object k2, ItemsToCompare parent, Difference difference) {
            this(k1, k2, parent, null, null, null, difference, parent != null ? parent.depth + 1 : 0);
        }

        // Constructor for field access with difference
        private ItemsToCompare(Object k1, Object k2, String fieldName, ItemsToCompare parent, Difference difference) {
            this(k1, k2, parent, fieldName, null, null, difference, parent != null ? parent.depth + 1 : 0);
        }

        // Constructor for array access with difference
        private ItemsToCompare(Object k1, Object k2, int[] indices, ItemsToCompare parent, Difference difference) {
            this(k1, k2, parent, null, indices, null, difference, parent != null ? parent.depth + 1 : 0);
        }

        // Constructor for map access with difference
        private ItemsToCompare(Object k1, Object k2, Object mapKey, ItemsToCompare parent, boolean isMapKey, Difference difference) {
            this(k1, k2, parent, null, null, mapKey, difference, parent != null ? parent.depth + 1 : 0);
        }

        // Base constructor
        private ItemsToCompare(Object k1, Object k2, ItemsToCompare parent,
                               String fieldName, int[] arrayIndices, Object mapKey, Difference difference, int depth) {
            this._key1 = k1;
            this._key2 = k2;
            this.parent = parent;
            this.fieldName = fieldName;
            this.arrayIndices = arrayIndices;
            this.mapKey = mapKey;
            this.difference = difference;
            this.depth = depth;
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
            return EncryptionUtilities.finalizeHash(System.identityHashCode(_key1) * 31 + System.identityHashCode(_key2));
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
     * under the key {@code "diff"}. To avoid retaining large object graphs, the {@code "diff_item"}
     * object is only stored if {@code "deepequals.include.diff_item"} is set to {@code true} in options.</p>
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
        Deque<ItemsToCompare> stack = new ArrayDeque<>();
        boolean result = deepEquals(a, b, stack, options, visited);

        if (!result && !stack.isEmpty()) {
            // Store the breadcrumb difference string
            ItemsToCompare top = stack.peek();
            String breadcrumb = generateBreadcrumb(stack);
            ((Map<String, Object>) options).put(DIFF, breadcrumb);
            
            // Optionally store the ItemsToCompare object (can retain large graphs)
            // Only include if explicitly requested to avoid memory retention
            Boolean includeDiffItem = (Boolean) options.get(INCLUDE_DIFF_ITEM);
            if (includeDiffItem != null && includeDiffItem) {
                ((Map<String, Object>) options).put(DIFF_ITEM, top);
            }
        }

        return result;
    }

    // Heap-based deepEquals implementation
    private static boolean deepEquals(Object a, Object b, Deque<ItemsToCompare> stack,
                                      Map<String, ?> options, Set<Object> visited) {
        final Collection<Class<?>> ignoreCustomEquals = (Collection<Class<?>>) options.get(IGNORE_CUSTOM_EQUALS);
        final boolean allowAllCustomEquals = ignoreCustomEquals == null;
        final boolean hasNonEmptyIgnoreSet = (ignoreCustomEquals != null && !ignoreCustomEquals.isEmpty());
        final boolean allowStringsToMatchNumbers = convert2boolean(options.get(ALLOW_STRINGS_TO_MATCH_NUMBERS));
        
        stack.addFirst(new ItemsToCompare(a, b));

        // Hoist size limits once at the start to avoid repeated system property reads
        final int configured = getMaxRecursionDepth();
        final Object budget = options.get(DEPTH_BUDGET);
        final int maxRecursionDepth = (budget instanceof Integer && (int)budget > 0)
            ? Math.min(configured > 0 ? configured : Integer.MAX_VALUE, (int)budget)
            : configured;
        final int maxCollectionSize = getMaxCollectionSize();
        final int maxArraySize = getMaxArraySize();
        final int maxMapSize = getMaxMapSize();
        final int maxObjectFields = getMaxObjectFields();

        while (!stack.isEmpty()) {
            ItemsToCompare itemsToCompare = stack.removeFirst();
            
            // Skip if already visited
            if (!visited.add(itemsToCompare)) {
                continue;
            }
            
            // Security check: prevent excessive recursion depth (heap-based depth tracking)
            if (maxRecursionDepth > 0 && itemsToCompare.depth > maxRecursionDepth) {
                throw new SecurityException("Maximum recursion depth exceeded: " + itemsToCompare.depth + " > " + maxRecursionDepth);
            }

            final Object key1 = itemsToCompare._key1;
            final Object key2 = itemsToCompare._key2;

            // Same instance is always equal to itself, null or otherwise.
            if (key1 == key2) {
                continue;
            }

            // If either one is null, they are not equal (key1 == key2 already checked)
            if (key1 == null || key2 == null) {
                stack.addFirst(new ItemsToCompare(key1, key2, itemsToCompare, Difference.VALUE_MISMATCH));
                return false;
            }

            // Handle all numeric comparisons first
            if (key1 instanceof Number && key2 instanceof Number) {
                if (!compareNumbers((Number) key1, (Number) key2)) {
                    stack.addFirst(new ItemsToCompare(key1, key2, itemsToCompare, Difference.VALUE_MISMATCH));
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
                stack.addFirst(new ItemsToCompare(key1, key2, itemsToCompare, Difference.VALUE_MISMATCH));
                return false;
            }

            if (key1 instanceof AtomicBoolean && key2 instanceof AtomicBoolean) {
                if (!compareAtomicBoolean((AtomicBoolean) key1, (AtomicBoolean) key2)) {
                    stack.addFirst(new ItemsToCompare(key1, key2, itemsToCompare, Difference.VALUE_MISMATCH));
                    return false;
                } else {
                    continue;
                }
            }

            final Class<?> key1Class = key1.getClass();
            final Class<?> key2Class = key2.getClass();

            // Handle Enums - they are singletons, use reference equality
            if (key1Class.isEnum() && key2Class.isEnum()) {
                if (key1 != key2) {  // Enum comparison is always == (same as Enum.equals)
                    stack.addFirst(new ItemsToCompare(key1, key2, itemsToCompare, Difference.VALUE_MISMATCH));
                    return false;
                }
                continue;
            }

            // Handle primitive wrappers, String, Date, Class, UUID, URL, URI, Temporal classes, etc.
            if (Converter.isSimpleTypeConversionSupported(key1Class)) {
                if (key1 instanceof Comparable<?> && key2 instanceof Comparable<?>) {
                    try {
                        if (((Comparable)key1).compareTo(key2) != 0) {
                            stack.addFirst(new ItemsToCompare(key1, key2, itemsToCompare, Difference.VALUE_MISMATCH));
                            return false;
                        }
                        continue;
                    } catch (Exception ignored) { }   // Fall back to equals() if compareTo() fails
                }
                if (!key1.equals(key2)) {
                    stack.addFirst(new ItemsToCompare(key1, key2, itemsToCompare, Difference.VALUE_MISMATCH));
                    return false;
                }
                continue;
            }

            // List and Deque interfaces define order as required as part of equality
            // Both represent ordered sequences that allow duplicates, so they can be compared
            boolean key1Ordered = key1 instanceof List || key1 instanceof Deque;
            boolean key2Ordered = key2 instanceof List || key2 instanceof Deque;
            
            if (key1Ordered || key2Ordered) {
                if (!(key1Ordered && key2Ordered)) {
                    // One is ordered, the other is not (or not a collection)
                    stack.addFirst(new ItemsToCompare(key1, key2, itemsToCompare, Difference.TYPE_MISMATCH));
                    return false;
                }
                // Both are ordered collections - compare with order
                if (!decomposeOrderedCollection((Collection<?>) key1, (Collection<?>) key2, stack, itemsToCompare, maxCollectionSize)) {
                    // Push VALUE_MISMATCH so parent's container-level description (e.g. "collection size mismatch")
                    // takes precedence over element-level differences
                    ItemsToCompare prior = stack.peek();
                    if (prior != null) {
                        stack.addFirst(new ItemsToCompare(prior._key1, prior._key2, prior, Difference.VALUE_MISMATCH));
                    }
                    return false;
                }
                continue;
            }

            // Unordered Collection comparison
            if (key1 instanceof Collection) {
                if (!(key2 instanceof Collection)) {
                    stack.addFirst(new ItemsToCompare(key1, key2, itemsToCompare, Difference.COLLECTION_TYPE_MISMATCH));
                    return false;
                }
                if (!decomposeUnorderedCollection((Collection<?>) key1, (Collection<?>) key2,
                        stack, options, visited, itemsToCompare, maxCollectionSize)) {
                    // Push VALUE_MISMATCH so parent's container-level description (e.g. "collection size mismatch")
                    // takes precedence over element-level differences
                    ItemsToCompare prior = stack.peek();
                    if (prior != null) {
                        stack.addFirst(new ItemsToCompare(prior._key1, prior._key2, prior, Difference.VALUE_MISMATCH));
                    }
                    return false;
                }
                continue;
            } else if (key2 instanceof Collection) {
                stack.addFirst(new ItemsToCompare(key1, key2, itemsToCompare, Difference.COLLECTION_TYPE_MISMATCH));
                return false;
            }

            // Map comparison
            if (key1 instanceof Map) {
                if (!(key2 instanceof Map)) {
                    stack.addFirst(new ItemsToCompare(key1, key2, itemsToCompare, Difference.TYPE_MISMATCH));
                    return false;
                }
                if (!decomposeMap((Map<?, ?>) key1, (Map<?, ?>) key2, stack, options, visited, itemsToCompare, maxMapSize)) {
                    // Push VALUE_MISMATCH so parent's container-level description (e.g. "map value mismatch")
                    // takes precedence over element-level differences
                    ItemsToCompare prior = stack.peek();
                    if (prior != null) {
                        stack.addFirst(new ItemsToCompare(prior._key1, prior._key2, prior, Difference.VALUE_MISMATCH));
                    }
                    return false;
                }
                continue;
            } else if (key2 instanceof Map) {
                stack.addFirst(new ItemsToCompare(key1, key2, itemsToCompare, Difference.TYPE_MISMATCH));
                return false;
            }

            // Array comparison
            if (key1Class.isArray()) {
                if (!key2Class.isArray()) {
                    stack.addFirst(new ItemsToCompare(key1, key2, itemsToCompare, Difference.TYPE_MISMATCH));
                    return false;
                }
                if (!decomposeArray(key1, key2, stack, itemsToCompare, maxArraySize)) {
                    // Push VALUE_MISMATCH so parent's container-level description (e.g. "array element mismatch")
                    // takes precedence over element-level differences
                    ItemsToCompare prior = stack.peek();
                    if (prior != null) {
                        stack.addFirst(new ItemsToCompare(prior._key1, prior._key2, prior, Difference.VALUE_MISMATCH));
                    }
                    return false;
                }
                continue;
            } else if (key2Class.isArray()) {
                stack.addFirst(new ItemsToCompare(key1, key2, itemsToCompare, Difference.TYPE_MISMATCH));
                return false;
            }

            // Must be same class if not a container type
            if (!key1Class.equals(key2Class)) {   // Must be same class
                stack.addFirst(new ItemsToCompare(key1, key2, itemsToCompare, Difference.TYPE_MISMATCH));
                return false;
            }
            
            // Special handling for Records (Java 14+) - use record components instead of fields
            if (ReflectionUtils.isRecord(key1Class)) {
                if (!decomposeRecord(key1, key2, stack, itemsToCompare)) {
                    return false;
                }
                continue;
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
                        
                        // Compute depth budget for recursive call
                        if (maxRecursionDepth > 0) {
                            int depthBudget = Math.max(0, maxRecursionDepth - itemsToCompare.depth);
                            newOptions.put(DEPTH_BUDGET, depthBudget);
                        }

                        // Make recursive call to find the actual difference
                        newOptions.put(INCLUDE_DIFF_ITEM, true);  // Need diff_item for internal use
                        deepEquals(key1, key2, newOptions);

                        // Get the difference and add it to our stack
                        ItemsToCompare diff = (ItemsToCompare) newOptions.get(DIFF_ITEM);
                        if (diff != null) {
                            stack.addFirst(diff);
                        }
                        return false;
                    }
                    continue;
                }
            }
            
            // Decompose object into its fields (not using custom equals)
            decomposeObject(key1, key2, stack, itemsToCompare, maxObjectFields);
        }
        return true;
    }

    private static boolean decomposeRecord(Object rec1, Object rec2, Deque<ItemsToCompare> stack, ItemsToCompare currentItem) {
        // Get record components using reflection (Java 14+ feature)
        Object[] components = ReflectionUtils.getRecordComponents(rec1.getClass());
        if (components == null) {
            // Fallback to regular object decomposition if record components unavailable
            return decomposeObject(rec1, rec2, stack, currentItem, Integer.MAX_VALUE);
        }
        
        // Compare each record component
        for (Object component : components) {
            String componentName = ReflectionUtils.getRecordComponentName(component);
            Object value1 = ReflectionUtils.getRecordComponentValue(component, rec1);
            Object value2 = ReflectionUtils.getRecordComponentValue(component, rec2);
            
            // Push component values for comparison with proper naming
            stack.addFirst(new ItemsToCompare(value1, value2, componentName, currentItem, Difference.FIELD_VALUE_MISMATCH));
        }
        
        return true;
    }

    // Create child options for nested comparisons, preserving semantics and
    // strictly *narrowing* any inherited depth budget.
    private static Map<String, Object> sanitizedChildOptions(Map<String, ?> options, ItemsToCompare currentItem) {
        Map<String, Object> child = new HashMap<>();
        if (options == null) {
            return child;
        }
        Object allow = options.get(ALLOW_STRINGS_TO_MATCH_NUMBERS);
        if (allow != null) {
            child.put(ALLOW_STRINGS_TO_MATCH_NUMBERS, allow);
        }
        Object ignore = options.get(IGNORE_CUSTOM_EQUALS);
        if (ignore != null) {
            child.put(IGNORE_CUSTOM_EQUALS, ignore);
        }
        // Depth budget: clamp to the tighter of (a) inherited budget (if any)
        // and (b) remaining configured budget based on current depth.
        Integer inherited = (options.get(DEPTH_BUDGET) instanceof Integer)
                ? (Integer) options.get(DEPTH_BUDGET) : null;
        int configured = getMaxRecursionDepth();
        Integer remainingFromConfigured = (configured > 0 && currentItem != null)
                ? Math.max(0, configured - currentItem.depth) : null;
        Integer childBudget = null;
        if (inherited != null && inherited > 0) {
            childBudget = inherited;
        }
        if (remainingFromConfigured != null) {
            childBudget = (childBudget == null) ? remainingFromConfigured
                    : Math.min(childBudget, remainingFromConfigured);
        }
        if (childBudget != null && childBudget > 0) {
            child.put(DEPTH_BUDGET, childBudget);
        }
        // Intentionally do NOT copy DIFF, "diff_item", "recursive_call", etc.
        return child;
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
                                                       Set<Object> visited, ItemsToCompare currentItem, int maxCollectionSize) {

        // Security check: validate collection sizes
        if (maxCollectionSize > 0 && (col1.size() > maxCollectionSize || col2.size() > maxCollectionSize)) {
            throw new SecurityException("Collection size exceeds maximum allowed: " + maxCollectionSize);
        }

        // Check sizes first
        if (col1.size() != col2.size()) {
            stack.addFirst(new ItemsToCompare(col1, col2, currentItem, Difference.COLLECTION_SIZE_MISMATCH));
            return false;
        }

        // Group col2 items by hash for efficient lookup (with slow-path fallback)
        // Pre-size to avoid rehashing: capacity = size * 4/3 to account for 0.75 load factor
        Map<Integer, List<Object>> hashGroups = new HashMap<>(Math.max(16, col2.size() * 4 / 3));
        for (Object o : col2) {
            int hash = deepHashCode(o);
            hashGroups.computeIfAbsent(hash, k -> new ArrayList<>()).add(o);
        }
        final Map<String, Object> childOptions = sanitizedChildOptions(options, currentItem);

        // Find first item in col1 not found in col2
        for (Object item1 : col1) {
            int hash1 = deepHashCode(item1);
            List<Object> candidates = hashGroups.get(hash1);

            if (candidates == null || candidates.isEmpty()) {
                // Slow-path: scan all remaining buckets to preserve correctness
                if (!tryMatchAcrossBuckets(item1, hashGroups, childOptions, visited)) {
                    stack.addFirst(new ItemsToCompare(item1, null, currentItem, Difference.COLLECTION_MISSING_ELEMENT));
                    return false;
                }
                continue;
            }

            // Check candidates with matching hash
            boolean foundMatch = false;
            for (Iterator<Object> it = candidates.iterator(); it.hasNext();) {
                Object item2 = it.next();
                // Use a copy of visited set to avoid polluting it with failed comparisons
                Set<Object> visitedCopy = new HashSet<>(visited);
                // Call 5-arg overload directly to bypass diff generation entirely
                Deque<ItemsToCompare> probeStack = new ArrayDeque<>();
                if (deepEquals(item1, item2, probeStack, childOptions, visitedCopy)) {
                    foundMatch = true;
                    it.remove();                  // safe removal during iteration
                    if (candidates.isEmpty()) {
                        hashGroups.remove(hash1);
                    }
                    break;
                }
            }

            if (!foundMatch) {
                // Slow-path: scan other buckets (excluding this one) before declaring a miss
                boolean foundInOtherBucket = tryMatchAcrossBucketsExcluding(item1, hashGroups, hash1, childOptions, visited);
                if (!foundInOtherBucket) {
                    stack.addFirst(new ItemsToCompare(item1, null, currentItem, Difference.COLLECTION_MISSING_ELEMENT));
                    return false;
                }
                // If found in another bucket, the item was already removed by tryMatchAcrossBucketsExcluding
            }
        }

        // Check if any elements remain in col2 (they would be unmatched)
        for (List<Object> remainingItems : hashGroups.values()) {
            if (!remainingItems.isEmpty()) {
                // col2 has elements not in col1
                stack.addFirst(new ItemsToCompare(null, remainingItems.get(0), currentItem, Difference.COLLECTION_MISSING_ELEMENT));
                return false;
            }
        }

        return true;
    }

    // Slow-path for unordered collections: search all buckets for a deep-equal match.
    private static boolean tryMatchAcrossBuckets(Object probe,
                                                 Map<Integer, List<Object>> buckets,
                                                 Map<String, ?> options,
                                                 Set<Object> visited) {
        for (Iterator<Map.Entry<Integer, List<Object>>> it = buckets.entrySet().iterator(); it.hasNext();) {
            Map.Entry<Integer, List<Object>> bucket = it.next();
            List<Object> list = bucket.getValue();
            for (Iterator<Object> li = list.iterator(); li.hasNext();) {
                Object cand = li.next();
                // Use a copy of visited set to avoid polluting it with failed comparisons
                Set<Object> visitedCopy = new HashSet<>(visited);
                // Call 5-arg overload directly to bypass diff generation entirely
                Deque<ItemsToCompare> probeStack = new ArrayDeque<>();
                if (deepEquals(probe, cand, probeStack, options, visitedCopy)) {
                    li.remove();
                    if (list.isEmpty()) it.remove();
                    return true;
                }
            }
        }
        return false;
    }

    // Slow-path for unordered collections: search buckets excluding a specific hash.
    private static boolean tryMatchAcrossBucketsExcluding(Object probe,
                                                          Map<Integer, List<Object>> buckets,
                                                          int excludeHash,
                                                          Map<String, ?> options,
                                                          Set<Object> visited) {
        for (Iterator<Map.Entry<Integer, List<Object>>> it = buckets.entrySet().iterator(); it.hasNext();) {
            Map.Entry<Integer, List<Object>> bucket = it.next();
            if (bucket.getKey() == excludeHash) {
                continue; // Skip the already-checked bucket
            }
            List<Object> list = bucket.getValue();
            for (Iterator<Object> li = list.iterator(); li.hasNext();) {
                Object cand = li.next();
                // Use a copy of visited set to avoid polluting it with failed comparisons
                Set<Object> visitedCopy = new HashSet<>(visited);
                // Call 5-arg overload directly to bypass diff generation entirely
                Deque<ItemsToCompare> probeStack = new ArrayDeque<>();
                if (deepEquals(probe, cand, probeStack, options, visitedCopy)) {
                    li.remove();
                    if (list.isEmpty()) it.remove();
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean decomposeOrderedCollection(Collection<?> col1, Collection<?> col2, Deque<ItemsToCompare> stack, ItemsToCompare currentItem, int maxCollectionSize) {

        // Security check: validate collection sizes
        if (maxCollectionSize > 0 && (col1.size() > maxCollectionSize || col2.size() > maxCollectionSize)) {
            throw new SecurityException("Collection size exceeds maximum allowed: " + maxCollectionSize);
        }

        // Check sizes first
        if (col1.size() != col2.size()) {
            stack.addFirst(new ItemsToCompare(col1, col2, currentItem, Difference.COLLECTION_SIZE_MISMATCH));
            return false;
        }

        // Push elements in reverse order so element 0 is compared first
        // Due to LIFO stack behavior, this means early termination on first mismatch
        List<?> list1 = (col1 instanceof List) ? (List<?>) col1 : new ArrayList<>(col1);
        List<?> list2 = (col2 instanceof List) ? (List<?>) col2 : new ArrayList<>(col2);
        
        for (int i = list1.size() - 1; i >= 0; i--) {
            stack.addFirst(new ItemsToCompare(list1.get(i), list2.get(i), 
                    new int[]{i}, currentItem, Difference.COLLECTION_ELEMENT_MISMATCH));
        }

        return true;
    }
    
    private static boolean decomposeMap(Map<?, ?> map1, Map<?, ?> map2, Deque<ItemsToCompare> stack, Map<String, ?> options, Set<Object> visited, ItemsToCompare currentItem, int maxMapSize) {

        // Security check: validate map sizes
        if (maxMapSize > 0 && (map1.size() > maxMapSize || map2.size() > maxMapSize)) {
            throw new SecurityException("Map size exceeds maximum allowed: " + maxMapSize);
        }

        // Check sizes first
        if (map1.size() != map2.size()) {
            stack.addFirst(new ItemsToCompare(map1, map2, currentItem, Difference.MAP_SIZE_MISMATCH));
            return false;
        }

        // Build lookup of map2 entries for efficient matching (with slow-path fallback)
        // Pre-size to avoid rehashing: capacity = size * 4/3 to account for 0.75 load factor
        Map<Integer, Collection<Map.Entry<?, ?>>> fastLookup = new HashMap<>(Math.max(16, map2.size() * 4 / 3));
        for (Map.Entry<?, ?> entry : map2.entrySet()) {
            int hash = deepHashCode(entry.getKey());
            fastLookup.computeIfAbsent(hash, k -> new ArrayList<>())
                    .add(new AbstractMap.SimpleEntry<>(entry.getKey(), entry.getValue()));
        }
        final Map<String, Object> childOptions = sanitizedChildOptions(options, currentItem);

        // Process map1 entries
        for (Map.Entry<?, ?> entry : map1.entrySet()) {
            int keyHash = deepHashCode(entry.getKey());
            Collection<Map.Entry<?, ?>> otherEntries = fastLookup.get(keyHash);

            // Key not found in map2
            if (otherEntries == null || otherEntries.isEmpty()) {
                // Slow-path: scan all buckets for an equal key before declaring a miss
                Map.Entry<?, ?> match = findAndRemoveMatchingKey(entry.getKey(), fastLookup, childOptions, visited);
                if (match == null) {
                    stack.addFirst(new ItemsToCompare(null, null, entry.getKey(), currentItem, true, Difference.MAP_MISSING_KEY));
                    return false;
                }
                // Found a matching key in another bucket; compare values
                stack.addFirst(new ItemsToCompare(
                        entry.getValue(), match.getValue(),
                        entry.getKey(), currentItem, true, Difference.MAP_VALUE_MISMATCH));
                continue;
            }

            // Find matching key in otherEntries
            boolean foundMatch = false;
            Iterator<Map.Entry<?, ?>> iterator = otherEntries.iterator();

            while (iterator.hasNext()) {
                Map.Entry<?, ?> otherEntry = iterator.next();

                // Check if keys are equal
                // Use a copy of visited set to avoid polluting it with failed comparisons
                Set<Object> visitedCopy = new HashSet<>(visited);
                // Call 5-arg overload directly to bypass diff generation for key probes
                Deque<ItemsToCompare> probeStack = new ArrayDeque<>();
                if (deepEquals(entry.getKey(), otherEntry.getKey(), probeStack, childOptions, visitedCopy)) {
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
                        fastLookup.remove(keyHash);
                    }
                    foundMatch = true;
                    break;
                }
            }

            if (!foundMatch) {
                // Slow-path: scan other buckets (excluding this one) for an equal key
                Map.Entry<?, ?> match = findAndRemoveMatchingKeyExcluding(entry.getKey(), fastLookup, keyHash, childOptions, visited);
                if (match == null) {
                    stack.addFirst(new ItemsToCompare(null, null, entry.getKey(), currentItem, true, Difference.MAP_MISSING_KEY));
                    return false;
                }
                stack.addFirst(new ItemsToCompare(
                        entry.getValue(), match.getValue(),
                        entry.getKey(), currentItem, true, Difference.MAP_VALUE_MISMATCH));
            }
        }

        // Check if any keys remain in map2 (they would be unmatched)
        for (Collection<Map.Entry<?, ?>> remainingEntries : fastLookup.values()) {
            if (!remainingEntries.isEmpty()) {
                // map2 has keys not in map1
                Map.Entry<?, ?> firstEntry = remainingEntries.iterator().next();
                stack.addFirst(new ItemsToCompare(null, null, firstEntry.getKey(), currentItem, true, Difference.MAP_MISSING_KEY));
                return false;
            }
        }

        return true;
    }

    // Slow-path for maps: search all buckets for a key deep-equal to 'key'.
    private static Map.Entry<?, ?> findAndRemoveMatchingKey(Object key,
                                                            Map<Integer, Collection<Map.Entry<?, ?>>> buckets,
                                                            Map<String, ?> options,
                                                            Set<Object> visited) {
        for (Iterator<Map.Entry<Integer, Collection<Map.Entry<?, ?>>>> it = buckets.entrySet().iterator(); it.hasNext();) {
            Map.Entry<Integer, Collection<Map.Entry<?, ?>>> b = it.next();
            Collection<Map.Entry<?, ?>> c = b.getValue();
            for (Iterator<Map.Entry<?, ?>> ci = c.iterator(); ci.hasNext();) {
                Map.Entry<?, ?> e = ci.next();
                // Use a copy of visited set to avoid polluting it with failed comparisons
                Set<Object> visitedCopy = new HashSet<>(visited);
                // Call 5-arg overload directly to bypass diff generation for key probes
                Deque<ItemsToCompare> probeStack = new ArrayDeque<>();
                if (deepEquals(key, e.getKey(), probeStack, options, visitedCopy)) {
                    ci.remove();
                    if (c.isEmpty()) it.remove();
                    return e;
                }
            }
        }
        return null;
    }

    // Slow-path for maps: search buckets (excluding specific hash) for a key deep-equal to 'key'.
    private static Map.Entry<?, ?> findAndRemoveMatchingKeyExcluding(Object key,
                                                                     Map<Integer, Collection<Map.Entry<?, ?>>> buckets,
                                                                     int excludeHash,
                                                                     Map<String, ?> options,
                                                                     Set<Object> visited) {
        for (Iterator<Map.Entry<Integer, Collection<Map.Entry<?, ?>>>> it = buckets.entrySet().iterator(); it.hasNext();) {
            Map.Entry<Integer, Collection<Map.Entry<?, ?>>> b = it.next();
            if (b.getKey() == excludeHash) {
                continue; // Skip the already-checked bucket
            }
            Collection<Map.Entry<?, ?>> c = b.getValue();
            for (Iterator<Map.Entry<?, ?>> ci = c.iterator(); ci.hasNext();) {
                Map.Entry<?, ?> e = ci.next();
                // Use a copy of visited set to avoid polluting it with failed comparisons
                Set<Object> visitedCopy = new HashSet<>(visited);
                // Call 5-arg overload directly to bypass diff generation for key probes
                Deque<ItemsToCompare> probeStack = new ArrayDeque<>();
                if (deepEquals(key, e.getKey(), probeStack, options, visitedCopy)) {
                    ci.remove();
                    if (c.isEmpty()) it.remove();
                    return e;
                }
            }
        }
        return null;
    }
    
    /**
     * Breaks an array into comparable pieces.
     *
     * @param array1        First array.
     * @param array2        Second array.
     * @param stack         Comparison stack.
     * @return true if arrays are equal, false otherwise.
     */
    private static boolean decomposeArray(Object array1, Object array2, Deque<ItemsToCompare> stack, ItemsToCompare currentItem, int maxArraySize) {

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
        if (maxArraySize > 0 && (len1 > maxArraySize || len2 > maxArraySize)) {
            throw new SecurityException("Array size exceeds maximum allowed: " + maxArraySize);
        }
        
        if (len1 != len2) {
            stack.addFirst(new ItemsToCompare(array1, array2, currentItem, Difference.ARRAY_LENGTH_MISMATCH));
            return false;
        }

        // 4. For primitive arrays, compare directly without pushing to stack
        Class<?> componentType = array1.getClass().getComponentType();
        
        if (componentType.isPrimitive()) {
            // Direct comparison for primitive arrays - avoids O(n) allocations
            if (componentType == boolean.class) {
                boolean[] a1 = (boolean[]) array1;
                boolean[] a2 = (boolean[]) array2;
                if (Arrays.equals(a1, a2)) { return true; }
                for (int i = 0; i < len1; i++) {
                    if (a1[i] != a2[i]) {
                        stack.addFirst(new ItemsToCompare(a1[i], a2[i], new int[]{i}, currentItem, Difference.ARRAY_ELEMENT_MISMATCH));
                        return false;
                    }
                }
            } else if (componentType == byte.class) {
                byte[] a1 = (byte[]) array1;
                byte[] a2 = (byte[]) array2;
                if (Arrays.equals(a1, a2)) { return true; }
                for (int i = 0; i < len1; i++) {
                    if (a1[i] != a2[i]) {
                        stack.addFirst(new ItemsToCompare(a1[i], a2[i], new int[]{i}, currentItem, Difference.ARRAY_ELEMENT_MISMATCH));
                        return false;
                    }
                }
            } else if (componentType == char.class) {
                char[] a1 = (char[]) array1;
                char[] a2 = (char[]) array2;
                if (Arrays.equals(a1, a2)) { return true; }
                for (int i = 0; i < len1; i++) {
                    if (a1[i] != a2[i]) {
                        stack.addFirst(new ItemsToCompare(a1[i], a2[i], new int[]{i}, currentItem, Difference.ARRAY_ELEMENT_MISMATCH));
                        return false;
                    }
                }
            } else if (componentType == short.class) {
                short[] a1 = (short[]) array1;
                short[] a2 = (short[]) array2;
                if (Arrays.equals(a1, a2)) { return true; }
                for (int i = 0; i < len1; i++) {
                    if (a1[i] != a2[i]) {
                        stack.addFirst(new ItemsToCompare(a1[i], a2[i], new int[]{i}, currentItem, Difference.ARRAY_ELEMENT_MISMATCH));
                        return false;
                    }
                }
            } else if (componentType == int.class) {
                int[] a1 = (int[]) array1;
                int[] a2 = (int[]) array2;
                if (Arrays.equals(a1, a2)) { return true; }
                for (int i = 0; i < len1; i++) {
                    if (a1[i] != a2[i]) {
                        stack.addFirst(new ItemsToCompare(a1[i], a2[i], new int[]{i}, currentItem, Difference.ARRAY_ELEMENT_MISMATCH));
                        return false;
                    }
                }
            } else if (componentType == long.class) {
                long[] a1 = (long[]) array1;
                long[] a2 = (long[]) array2;
                if (Arrays.equals(a1, a2)) { return true; }
                for (int i = 0; i < len1; i++) {
                    if (a1[i] != a2[i]) {
                        stack.addFirst(new ItemsToCompare(a1[i], a2[i], new int[]{i}, currentItem, Difference.ARRAY_ELEMENT_MISMATCH));
                        return false;
                    }
                }
            } else if (componentType == float.class) {
                float[] a1 = (float[]) array1;
                float[] a2 = (float[]) array2;
                if (Arrays.equals(a1, a2)) { return true; }  // exact fast-path
                for (int i = 0; i < len1; i++) {
                    // Use nearlyEqual for consistent floating-point comparison with tolerance
                    if (!nearlyEqual(a1[i], a2[i])) {
                        stack.addFirst(new ItemsToCompare(a1[i], a2[i], new int[]{i}, currentItem, Difference.ARRAY_ELEMENT_MISMATCH));
                        return false;
                    }
                }
            } else if (componentType == double.class) {
                double[] a1 = (double[]) array1;
                double[] a2 = (double[]) array2;
                if (Arrays.equals(a1, a2)) { return true; }  // exact fast-path
                for (int i = 0; i < len1; i++) {
                    // Use nearlyEqual for consistent floating-point comparison with tolerance
                    if (!nearlyEqual(a1[i], a2[i])) {
                        stack.addFirst(new ItemsToCompare(a1[i], a2[i], new int[]{i}, currentItem, Difference.ARRAY_ELEMENT_MISMATCH));
                        return false;
                    }
                }
            }
        } else {
            // For object arrays, push elements in reverse order
            // This ensures element 0 is compared first due to LIFO stack
            for (int i = len1 - 1; i >= 0; i--) {
                stack.addFirst(new ItemsToCompare(Array.get(array1, i), Array.get(array2, i),
                        new int[]{i}, currentItem, Difference.ARRAY_ELEMENT_MISMATCH));
            }
        }

        return true;
    }

    private static boolean decomposeObject(Object obj1, Object obj2, Deque<ItemsToCompare> stack, ItemsToCompare currentItem, int maxObjectFields) {

        // Get all fields from the object
        Collection<Field> fields = ReflectionUtils.getAllDeclaredFields(obj1.getClass());

        // Security check: validate field count
        if (maxObjectFields > 0 && fields.size() > maxObjectFields) {
            throw new SecurityException("Object field count exceeds maximum allowed: " + maxObjectFields);
        }

        // Push each field for comparison
        for (Field field : fields) {
            try {
                // Skip synthetic fields
                if (field.isSynthetic()) {
                    continue;
                }
                
                // Skip static fields - they're not part of instance state
                int modifiers = field.getModifiers();
                if (Modifier.isStatic(modifiers)) {
                    continue;
                }
                
                // Skip transient fields - they're typically not part of equality
                if (Modifier.isTransient(modifiers)) {
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
    
    private static boolean isIntegralNumber(Number n) {
        return n instanceof Byte || n instanceof Short || 
               n instanceof Integer || n instanceof Long ||
               n instanceof AtomicInteger || n instanceof AtomicLong;
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
            return nearlyEqual(d1, d2);
        }

        // Fast path for integral numbers (avoids BigDecimal conversion)
        if (isIntegralNumber(a) && isIntegralNumber(b)) {
            return a.longValue() == b.longValue();
        }
        
        // For other non-floating point numbers (e.g., BigDecimal, BigInteger), use exact comparison
        try {
            BigDecimal x = convert2BigDecimal(a);
            BigDecimal y = convert2BigDecimal(b);
            return x.compareTo(y) == 0;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Correctly handles floating point comparisons with proper NaN and near-zero handling.
     *
     * @param a First double.
     * @param b Second double.
     * @return true if numbers are nearly equal within epsilon, false otherwise.
     */
    private static boolean nearlyEqual(double a, double b) {
        // Fast path: bitwise equality handles NaN==NaN, +0.0==-0.0
        if (Double.doubleToLongBits(a) == Double.doubleToLongBits(b)) {
            return true;
        }
        // NaN values that aren't the same bit pattern are not equal
        if (Double.isNaN(a) || Double.isNaN(b)) {
            return false;
        }
        // Treat any infinity as unequal to finite numbers
        if (Double.isInfinite(a) || Double.isInfinite(b)) {
            return false;
        }
        
        double diff = Math.abs(a - b);
        double norm = Math.max(Math.abs(a), Math.abs(b));
        
        // Near zero: use absolute tolerance; elsewhere: use relative tolerance
        return (norm == 0.0) ? diff <= DeepEquals.DOUBLE_EPSILON : diff <= DeepEquals.DOUBLE_EPSILON * norm;
    }
    
    /**
     * Correctly handles floating point comparisons for floats.
     *
     * @param a First float.
     * @param b Second float.
     * @return true if numbers are nearly equal within epsilon, false otherwise.
     */
    private static boolean nearlyEqual(float a, float b) {
        // Fast path: bitwise equality handles NaN==NaN, +0.0f==-0.0f
        if (Float.floatToIntBits(a) == Float.floatToIntBits(b)) {
            return true;
        }
        // NaN values that aren't the same bit pattern are not equal
        if (Float.isNaN(a) || Float.isNaN(b)) {
            return false;
        }
        // Treat any infinity as unequal to finite numbers
        if (Float.isInfinite(a) || Float.isInfinite(b)) {
            return false;
        }
        
        float diff = Math.abs(a - b);
        float norm = Math.max(Math.abs(a), Math.abs(b));
        
        // Near zero: use absolute tolerance; elsewhere: use relative tolerance
        return (norm == 0.0f) ? diff <= DeepEquals.FLOAT_EPSILON : diff <= DeepEquals.FLOAT_EPSILON * norm;
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
     * LOG.info("Has custom hashCode(): " + hasCustomHashCode);
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
     * it *always* calls an instance's hashCode() method if it has one that override's the
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
        Deque<Object> stack = new ArrayDeque<>();
        if (obj != null) {
            stack.addFirst(obj);
        }
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

            // Order matters for List and Deque - it is defined as part of equality
            if (obj instanceof List || obj instanceof Deque) {
                Collection<?> col = (Collection<?>) obj;
                long result = 1;

                for (Object element : col) {
                    result = 31 * result + hashElement(visited, element);
                }
                hash += (int) result;
                continue;
            }

            // Ignore order for non-List/non-Deque Collections (not part of definition of equality)
            if (obj instanceof Collection) {
                addCollectionToStack(stack, (Collection<?>) obj);
                continue;
            }

            if (obj instanceof Map) {
                Map<?, ?> m = (Map<?, ?>) obj;
                int mapHash = 0;
                for (Map.Entry<?, ?> e : m.entrySet()) {
                    int kh = hashElement(visited, e.getKey());
                    int vh = hashElement(visited, e.getValue());
                    // XOR ensures order independence (a^b == b^a)
                    // But combine key and value first to prevent collision when swapping values
                    mapHash ^= (31 * kh + vh);
                }
                hash += mapHash;
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

            // Special handling for Records (Java 14+) - use record components for hashing
            if (ReflectionUtils.isRecord(obj.getClass())) {
                Object[] components = ReflectionUtils.getRecordComponents(obj.getClass());
                if (components != null) {
                    for (Object component : components) {
                        Object value = ReflectionUtils.getRecordComponentValue(component, obj);
                        if (value != null) {
                            stack.addFirst(value);
                        }
                    }
                    continue;
                }
                // Fallback to field-based hashing if record components unavailable
            }

            Collection<Field> fields = ReflectionUtils.getAllDeclaredFields(obj.getClass());
            for (Field field : fields) {
                try {
                    // Skip synthetic fields
                    if (field.isSynthetic()) {
                        continue;
                    }
                    
                    // Skip static fields - they're not part of instance state
                    int modifiers = field.getModifiers();
                    if (Modifier.isStatic(modifiers)) {
                        continue;
                    }
                    
                    // Skip transient fields - they're typically not part of equality
                    if (Modifier.isTransient(modifiers)) {
                        continue;
                    }
                    
                    Object fieldValue = field.get(obj);
                    if (fieldValue != null) {
                        stack.addFirst(fieldValue);
                    }
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
        // Handle special cases first
        if (Double.isNaN(value)) {
            return 0x7ff80000;  // Stable NaN bucket
        }
        if (Double.isInfinite(value)) {
            return value > 0 ? 0x7ff00000 : 0xfff00000;  // Separate buckets for +∞ and -∞
        }
        
        // Normalize the value according to epsilon
        double normalizedValue = Math.round(value * SCALE_DOUBLE) / SCALE_DOUBLE;
        
        // Normalize negative zero to positive zero
        if (normalizedValue == 0.0) {
            normalizedValue = 0.0;  // This ensures -0.0 becomes 0.0
        }
        
        long bits = Double.doubleToLongBits(normalizedValue);
        return (int) (bits ^ (bits >>> 32));
    }

    private static int hashFloat(float value) {
        // Handle special cases first
        if (Float.isNaN(value)) {
            return 0x7fc00000;  // Stable NaN bucket
        }
        if (Float.isInfinite(value)) {
            return value > 0 ? 0x7f800000 : 0xff800000;  // Separate buckets for +∞ and -∞
        }
        
        // Normalize the value according to epsilon
        float normalizedValue = Math.round(value * SCALE_FLOAT) / SCALE_FLOAT;
        
        // Normalize negative zero to positive zero
        if (normalizedValue == 0.0f) {
            normalizedValue = 0.0f;  // This ensures -0.0f becomes 0.0f
        }
        
        return Float.floatToIntBits(normalizedValue);
    }

    private static void addCollectionToStack(Deque<Object> stack, Collection<?> collection) {
        List<?> items = (collection instanceof List) ? (List<?>) collection : new ArrayList<>(collection);
        for (int i = items.size() - 1; i >= 0; i--) {
            Object item = items.get(i);
            if (item != null) {
                stack.addFirst(item);
            }
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
                // For a missing map key, show ∅ on the RHS in the breadcrumb
                String rhs = (cur.difference == Difference.MAP_MISSING_KEY)
                        ? EMPTY
                        : formatValueConcise(cur._key1);
                sb2.append(ANGLE_LEFT)
                        .append(formatMapKey(cur.mapKey))
                        .append(" ")
                        .append(ARROW)
                        .append(" ")
                        .append(rhs)
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
            path.add(current);  // Build forward for O(n) time
            current = current.parent;
        }
        Collections.reverse(path);  // Reverse once to get root→diff order
        return path;
    }

    private static void formatDifference(StringBuilder result, ItemsToCompare item) {
        if (item.difference == null) {
            return;
        }

        // Special handling for MAP_MISSING_KEY
        if (item.difference == Difference.MAP_MISSING_KEY) {
            result.append(String.format("  Expected: key '%s' present%n  Found: (missing)",
                    formatDifferenceValue(item.mapKey)));
            return;
        }

        // Choose the node that provided the phrase/details.
        // If the parent's category is a container-level one (non-VALUE),
        // use the parent for both the category and the concrete objects.
        ItemsToCompare detailNode = item;
        DiffCategory category = item.difference.getCategory();
        if (item.parent != null && item.parent.difference != null) {
            DiffCategory parentCat = item.parent.difference.getCategory();
            if (parentCat != DiffCategory.VALUE) {
                category = parentCat;
                detailNode = item.parent;
            }
        }
        switch (category) {
            case SIZE:
                result.append(String.format("  Expected size: %d%n  Found size: %d",
                        getContainerSize(detailNode._key1),
                        getContainerSize(detailNode._key2)));
                break;

            case TYPE:
                result.append(String.format("  Expected type: %s%n  Found type: %s",
                        getTypeDescription(detailNode._key1 != null ? detailNode._key1.getClass() : null),
                        getTypeDescription(detailNode._key2 != null ? detailNode._key2.getClass() : null)));
                break;

            case LENGTH:
                result.append(String.format("  Expected length: %d%n  Found length: %d",
                        Array.getLength(detailNode._key1),
                        Array.getLength(detailNode._key2)));
                break;

            case DIMENSION:
                result.append(String.format("  Expected dimensions: %d%n  Found dimensions: %d",
                        getDimensions(detailNode._key1),
                        getDimensions(detailNode._key2)));
                break;

            case VALUE:
            default:
                result.append(String.format("  Expected: %s%n  Found: %s",
                        formatDifferenceValue(detailNode._key1),
                        formatDifferenceValue(detailNode._key2)));
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
                int modifiers = field.getModifiers();
                if (Modifier.isStatic(modifiers) || Modifier.isTransient(modifiers)) {
                    continue; // align formatting with equality semantics
                }
                if (!first) sb.append(", ");
                first = false;

                Object fieldValue = field.get(value);
                String fieldName = field.getName();
                
                // Check if field is sensitive and security is enabled
                if (isSecureErrorsEnabled() && isSensitiveField(fieldName)) {
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
            return isSecureErrorsEnabled() ? sanitizeStringValue(str) : "\"" + str + "\"";
        }
        if (value instanceof Character) return "'" + value + "'";
        if (value instanceof Number) {
            return formatNumber((Number) value);
        }
        if (value instanceof Boolean) return value.toString();
        if (value instanceof Date) {
            return TS_FMT.get().format((Date)value) + " UTC";
        }
        if (value instanceof TimeZone) {
            TimeZone timeZone = (TimeZone) value;
            return "TimeZone: " + timeZone.getID();
        }
        if (value instanceof URI) {
            return isSecureErrorsEnabled() ? sanitizeUriValue((URI) value) : value.toString();
        }
        if (value instanceof URL) {
            return isSecureErrorsEnabled() ? sanitizeUrlValue((URL) value) : value.toString();
        }
        if (value instanceof UUID) {
            return value.toString();  // UUID is generally safe to display
        }

        // For other types, show type and sanitized toString if security enabled
        if (isSecureErrorsEnabled()) {
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

            if (value instanceof String) {
                String s = (String) value;
                return isSecureErrorsEnabled() ? sanitizeStringValue(s) : ("\"" + s + "\"");
            }
            if (value instanceof Character) return "'" + value + "'";

            if (value instanceof Date) {
                return TS_FMT.get().format((Date)value) + " UTC";
            }

            // Handle Enums - format as EnumType.NAME
            if (value.getClass().isEnum()) {
                return value.getClass().getSimpleName() + "." + ((Enum<?>) value).name();
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
        sb.append(componentType.getSimpleName());  // Base type (int, String, etc.)

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
                int modifiers = field.getModifiers();
                if (Modifier.isStatic(modifiers) || Modifier.isTransient(modifiers)) {
                    continue; // align formatting with equality semantics
                }
                if (!first) {
                    sb.append(", ");
                }
                first = false;

                final String fieldName = field.getName();
                sb.append(fieldName).append(": ");
                if (isSecureErrorsEnabled() && isSensitiveField(fieldName)) {
                    sb.append("[REDACTED]");
                    continue;
                }
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
            String s = (String) key;
            return isSecureErrorsEnabled() ? sanitizeStringValue(s) : ("\"" + s + "\"");
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
                return String.format(java.util.Locale.ROOT, "%.6e", doubleValue);
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
                return String.format(java.util.Locale.ROOT, "%.6e", d);
            }
            // For doubles, up to 15 decimal places
            if (value instanceof Double) {
                return String.format(java.util.Locale.ROOT, "%.15g", d).replaceAll("\\.?0+$", "");
            }
            // For floats, up to 7 decimal places
            return String.format(java.util.Locale.ROOT, "%.7g", d).replaceAll("\\.?0+$", "");
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
        if (str.isEmpty()) return "\"\"";
        
        // Check if string looks like sensitive data
        String lowerStr = str.toLowerCase(Locale.ROOT);
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
        // Note: "key" alone is too broad, we look for more specific patterns like "apikey", "secretkey", etc.
        if (SENSITIVE_WORDS.matcher(lowerStr).matches()) {
            return true;
        }
        
        // Check for long hex strings (32+ chars) - likely hashes or keys
        if (HEX_32_PLUS.matcher(lowerStr).matches()) {
            return true;
        }
        
        // Check for Base64 encoded data - only flag if length >= 32 to avoid false positives
        // The strict pattern ensures it's actually valid Base64, not just random text
        if (lowerStr.length() >= 32 && BASE64_PATTERN.matcher(lowerStr).matches()) {
            return true;
        }
        
        // Check for UUID patterns - these are generally safe to display
        if (UUID_PATTERN.matcher(lowerStr).matches()) {
            return false; // UUIDs are generally safe to display
        }
        
        return false;
    }

    private static boolean isSensitiveField(String fieldName) {
        if (fieldName == null) return false;
        String lowerFieldName = fieldName.toLowerCase(Locale.ROOT);
        // Check against explicit list and specific patterns
        // Note: Removed generic "key" check as it's too broad (matches "monkey", "keyboard", etc.)
        return SENSITIVE_FIELD_NAMES.contains(lowerFieldName) ||
               lowerFieldName.contains("password") ||
               lowerFieldName.contains("secret") ||
               lowerFieldName.contains("token");
    }
}