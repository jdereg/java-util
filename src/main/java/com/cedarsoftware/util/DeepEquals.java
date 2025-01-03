package com.cedarsoftware.util;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static com.cedarsoftware.util.Converter.convert2BigDecimal;
import static com.cedarsoftware.util.Converter.convert2boolean;

@SuppressWarnings("unchecked")
public class DeepEquals {
    // Option keys
    public static final String IGNORE_CUSTOM_EQUALS = "ignoreCustomEquals";
    public static final String ALLOW_STRINGS_TO_MATCH_NUMBERS = "stringsCanMatchNumbers";

    // Caches for custom equals and hashCode methods
    private static final Map<String, Boolean> _customEquals = new ConcurrentHashMap<>();
    private static final Map<String, Boolean> _customHash = new ConcurrentHashMap<>();

    // Epsilon values for floating-point comparisons
    private static final double doubleEpsilon = 1e-15;
    
    // Class to hold information about items being compared
    private final static class ItemsToCompare {
        private final Object _key1;
        private final Object _key2;
        private ItemsToCompare parent;
        private final ContainerType containerType;
        private final String fieldName;
        private final int[] arrayIndices;
        private final String mapKey;
        private final Difference difference;    // New field

        // Modified constructors to include Difference

        // Constructor for root
        private ItemsToCompare(Object k1, Object k2) {
            this(k1, k2, null, null, null, null, null);
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
        private ItemsToCompare(Object k1, Object k2, String mapKey, ItemsToCompare parent, boolean isMapKey, Difference difference) {
            this(k1, k2, parent, null, null, mapKey, difference);
        }

        // Base constructor
        private ItemsToCompare(Object k1, Object k2, ItemsToCompare parent,
                               String fieldName, int[] arrayIndices, String mapKey, Difference difference) {
            this._key1 = k1;
            this._key2 = k2;
            this.parent = parent;
            this.containerType = getContainerType(k1);
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

        private static ContainerType getContainerType(Object obj) {
            if (obj == null) {
                return null;
            }
            if (obj.getClass().isArray()) {
                return ContainerType.ARRAY;
            }
            if (obj instanceof Collection) {
                return ContainerType.COLLECTION;
            }
            if (obj instanceof Map) {
                return ContainerType.MAP;
            }
            if (Converter.isSimpleTypeConversionSupported(obj.getClass(), obj.getClass())) {
                return null;    // Simple type - not a container
            }
            return ContainerType.OBJECT;  // Must be object with fields
        }
    }
    
    // Main deepEquals method without options
    public static boolean deepEquals(Object a, Object b) {
        return deepEquals(a, b, new HashMap<>());
    }
    
    public static boolean deepEquals(Object a, Object b, Map<String, ?> options) {
        Set<ItemsToCompare> visited = new HashSet<>();
        Deque<ItemsToCompare> stack = new LinkedList<>();
        boolean result = deepEquals(a, b, stack, options, visited);

        boolean isRecurive = Objects.equals(true, options.get("recursive_call"));
        if (!result && !stack.isEmpty()) {
            // Store both the breadcrumb and the difference ItemsToCompare
            ItemsToCompare top = stack.peek();
            String breadcrumb = generateBreadcrumb(stack);
            ((Map<String, Object>) options).put("diff", breadcrumb);
            ((Map<String, Object>) options).put("diff_item", top);

            if (!isRecurive) {
                System.out.println(breadcrumb);
                System.out.println("--------------------");
                System.out.flush();
            }
        }

        return result;
    }

    // Recursive deepEquals implementation
    private static boolean deepEquals(Object a, Object b, Deque<ItemsToCompare> stack,
                                      Map<String, ?> options, Set<ItemsToCompare> visited) {
        Set<Class<?>> ignoreCustomEquals = (Set<Class<?>>) options.get(IGNORE_CUSTOM_EQUALS);
        final boolean allowStringsToMatchNumbers = convert2boolean(options.get(ALLOW_STRINGS_TO_MATCH_NUMBERS));
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

            // If either one is null, they are not equal
            if (key1 == null || key2 == null) {
                stack.addFirst(new ItemsToCompare(key1, key2, "value", stack.peek(), Difference.VALUE_MISMATCH));
                return false;
            }

            // Handle all numeric comparisons first
            if (key1 instanceof Number && key2 instanceof Number) {
                if (!compareNumbers((Number) key1, (Number) key2)) {
                    stack.addFirst(new ItemsToCompare(key1, key2, "value", stack.peek(), Difference.VALUE_MISMATCH));
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
                stack.addFirst(new ItemsToCompare(key1, key2, "value", stack.peek(), Difference.VALUE_MISMATCH));
                return false;
            }

            if (key1 instanceof AtomicBoolean && key2 instanceof AtomicBoolean) {
                if (!compareAtomicBoolean((AtomicBoolean) key1, (AtomicBoolean) key2)) {
                    stack.addFirst(new ItemsToCompare(key1, key2, "value", stack.peek(), Difference.VALUE_MISMATCH));
                    return false;
                } else {
                    continue;
                }
            }

            Class<?> key1Class = key1.getClass();
            Class<?> key2Class = key2.getClass();

            // Handle primitive wrappers, String, Date, Class, UUID, URL, URI, Temporal classes, etc.
            if (Converter.isSimpleTypeConversionSupported(key1Class, key1Class)) {
                if (key1 instanceof Comparable && key2 instanceof Comparable) {
                    try {
                        if (((Comparable)key1).compareTo(key2) != 0) {
                            stack.addFirst(new ItemsToCompare(key1, key2, "value", stack.peek(), Difference.VALUE_MISMATCH));
                            return false;
                        }
                        continue;
                    } catch (Exception ignored) { }   // Fall back to equals() if compareTo() fails
                }
                if (!key1.equals(key2)) {
                    stack.addFirst(new ItemsToCompare(key1, key2, "value", stack.peek(), Difference.VALUE_MISMATCH));
                    return false;
                }
                continue;
            }
            
            // Set comparison
            if (key1 instanceof Set) {
                if (!(key2 instanceof Set)) {
                    stack.addFirst(new ItemsToCompare(key1, key2, "value", stack.peek(), Difference.COLLECTION_TYPE_MISMATCH));
                    return false;
                }
                if (!decomposeUnorderedCollection((Collection<?>) key1, (Collection<?>) key2, stack)) {
                    return false;
                }
                continue;
            } else if (key2 instanceof Set) {
                stack.addFirst(new ItemsToCompare(key1, key2, "value", stack.peek(), Difference.COLLECTION_TYPE_MISMATCH));
                return false;
            }

            // Collection comparison
            if (key1 instanceof Collection) {   // If Collections, they both must be Collection
                if (!(key2 instanceof Collection)) {
                    stack.addFirst(new ItemsToCompare(key1, key2, "value", stack.peek(), Difference.TYPE_MISMATCH));
                    return false;
                }
                if (!decomposeOrderedCollection((Collection<?>) key1, (Collection<?>) key2, stack)) {
                    return false;
                }
                continue;
            } else if (key2 instanceof Collection) {
                stack.addFirst(new ItemsToCompare(key1, key2, "value", stack.peek(), Difference.TYPE_MISMATCH));
                return false;
            }

            // Map comparison
            if (key1 instanceof Map) {
                if (!(key2 instanceof Map)) {
                    stack.addFirst(new ItemsToCompare(key1, key2, "value", stack.peek(), Difference.TYPE_MISMATCH));
                    return false;
                }
                if (!decomposeMap((Map<?, ?>) key1, (Map<?, ?>) key2, stack)) {
                    return false;
                }
                continue;
            } else if (key2 instanceof Map) {
                stack.addFirst(new ItemsToCompare(key1, key2, "value", stack.peek(), Difference.TYPE_MISMATCH));
                return false;
            }

            // Array comparison
            if (key1Class.isArray()) {
                if (!key2Class.isArray()) {
                    stack.addFirst(new ItemsToCompare(key1, key2, "value", stack.peek(), Difference.TYPE_MISMATCH));
                    return false;
                }
                if (!decomposeArray(key1, key2, stack)) {
                    return false;
                }
                continue;
            } else if (key2Class.isArray()) {
                stack.addFirst(new ItemsToCompare(key1, key2, "value", stack.peek(), Difference.TYPE_MISMATCH));
                return false;
            }

            // Must be same class if not a container type
            if (!key1Class.equals(key2Class)) {   // Must be same class
                stack.addFirst(new ItemsToCompare(key1, key2, "value", stack.peek(), Difference.TYPE_MISMATCH));
                return false;
            }
            
            // If there is a custom equals and not ignored, compare using custom equals
            if (hasCustomEquals(key1Class)) {
                if (ignoreCustomEquals == null || (!ignoreCustomEquals.isEmpty() && !ignoreCustomEquals.contains(key1Class))) {
                    if (!key1.equals(key2)) {
                        // Create new options map with ignoreCustomEquals set
                        Map<String, Object> newOptions = new HashMap<>(options);
                        newOptions.put("recursive_call", true);
                        Set<Class<?>> ignoreSet = new HashSet<>();
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
            
            // Decompose object into its fields
            if (!decomposeObject(key1, key2, stack)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Compares two unordered collections (e.g., Sets) deeply.
     *
     * @param col1           First collection.
     * @param col2           Second collection.
     * @return true if collections are equal, false otherwise.
     */
    private static boolean decomposeUnorderedCollection(Collection<?> col1, Collection<?> col2, Deque<ItemsToCompare> stack) {
        ItemsToCompare currentItem = stack.peek();

        // Check sizes first
        if (col1.size() != col2.size()) {
            stack.addFirst(new ItemsToCompare(
                    col1,
                    col2,
                    "size",
                    currentItem,
                    Difference.COLLECTION_SIZE_MISMATCH
            ));
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
                stack.addFirst(new ItemsToCompare(item1, null, "unmatchedElement", currentItem, Difference.COLLECTION_MISSING_ELEMENT));
                return false;
            }

            // Check candidates with matching hash
            boolean foundMatch = false;
            for (Object item2 : candidates) {
                if (deepEquals(item1, item2)) {
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
                stack.addFirst(new ItemsToCompare(item1, null, "unmatchedElement", currentItem, Difference.COLLECTION_MISSING_ELEMENT));
                return false;
            }
        }

        return true;
    }

    private static boolean decomposeOrderedCollection(Collection<?> col1, Collection<?> col2, Deque<ItemsToCompare> stack) {
        ItemsToCompare currentItem = stack.peek();

        // Check sizes first
        if (col1.size() != col2.size()) {
            stack.addFirst(new ItemsToCompare(col1, col2, "size", currentItem, Difference.COLLECTION_SIZE_MISMATCH));
            return false;
        }

        // Push elements in order
        Iterator<?> i1 = col1.iterator();
        Iterator<?> i2 = col2.iterator();
        int index = 0;

        while (i1.hasNext()) {
            Object item1 = i1.next();
            Object item2 = i2.next();

            stack.addFirst(new ItemsToCompare(item1, item2, new int[]{index++}, currentItem, Difference.COLLECTION_MISSING_ELEMENT));
        }

        return true;
    }
    
    /**
     * Breaks a Map into its comparable pieces.
     *
     * @param map1           First map.
     * @param map2           Second map.
     * @param stack          Comparison stack.
     * @return true if maps are equal, false otherwise.
     */
    private static boolean decomposeMap(Map<?, ?> map1, Map<?, ?> map2, Deque<ItemsToCompare> stack) {
        ItemsToCompare currentItem = stack.peek();

        // Check sizes first
        if (map1.size() != map2.size()) {
            stack.addFirst(new ItemsToCompare(map1, map2, "size", currentItem, Difference.MAP_SIZE_MISMATCH));
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
                stack.addFirst(new ItemsToCompare(entry.getKey(), null, "unmatchedKey", currentItem, Difference.MAP_MISSING_KEY));
                return false;
            }

            // Find matching key in otherEntries
            boolean foundMatch = false;
            Iterator<Map.Entry<?, ?>> iterator = otherEntries.iterator();

            while (iterator.hasNext()) {
                Map.Entry<?, ?> otherEntry = iterator.next();

                // Check if keys are equal
                if (deepEquals(entry.getKey(), otherEntry.getKey())) {
                    // Push value comparison only - keys are known to be equal
                    stack.addFirst(new ItemsToCompare(
                            entry.getValue(),                // map1 value
                            otherEntry.getValue(),           // map2 value
                            formatMapKey(entry.getKey()),    // pass the key as 'mapKey'
                            currentItem,
                            true,                             // isMapKey = true
                            Difference.MAP_VALUE_MISMATCH
                    ));

                    iterator.remove();
                    if (otherEntries.isEmpty()) {
                        fastLookup.remove(deepHashCode(entry.getKey()));
                    }
                    foundMatch = true;
                    break;
                }
            }

            if (!foundMatch) {
                stack.addFirst(new ItemsToCompare(entry.getKey(), null, "unmatchedKey", currentItem, Difference.MAP_MISSING_KEY));
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
            stack.addFirst(new ItemsToCompare(array1, array2, "dimensionality", currentItem, Difference.ARRAY_DIMENSION_MISMATCH));
            return false;
        }

        // 2. Check component types
        if (!array1.getClass().getComponentType().equals(array2.getClass().getComponentType())) {
            stack.addFirst(new ItemsToCompare(array1, array2, "componentType", currentItem, Difference.ARRAY_COMPONENT_TYPE_MISMATCH));
            return false;
        }

        // 3. Check lengths
        int len1 = Array.getLength(array1);
        int len2 = Array.getLength(array2);
        if (len1 != len2) {
            stack.addFirst(new ItemsToCompare(array1, array2, "arrayLength", currentItem, Difference.ARRAY_LENGTH_MISMATCH));
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

        // Push each field for comparison
        for (Field field : fields) {
            try {
                if (field.getName().startsWith("this$")) {
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
                    double d;
                    if (a instanceof BigDecimal) {
                        bd = (BigDecimal) a;
                        d = b.doubleValue();
                    } else {
                        bd = (BigDecimal) b;
                        d = a.doubleValue();
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
     * Determines if a class has a custom equals method.
     *
     * @param c Class to check.
     * @return true if a custom equals method exists, false otherwise.
     */
    public static boolean hasCustomEquals(Class<?> c) {
        StringBuilder sb = new StringBuilder(ReflectionUtils.getClassLoaderName(c));
        sb.append('.');
        sb.append(c.getName());
        String key = sb.toString();
        Boolean ret = _customEquals.get(key);

        if (ret != null) {
            return ret;
        }

        while (!Object.class.equals(c)) {
            try {
                c.getDeclaredMethod("equals", Object.class);
                _customEquals.put(key, true);
                return true;
            } catch (Exception ignored) {
            }
            c = c.getSuperclass();
        }
        _customEquals.put(key, false);
        return false;
    }

    /**
     * Determines if a class has a custom hashCode method.
     *
     * @param c Class to check.
     * @return true if a custom hashCode method exists, false otherwise.
     */
    public static boolean hasCustomHashCode(Class<?> c) {
        StringBuilder sb = new StringBuilder(ReflectionUtils.getClassLoaderName(c));
        sb.append('.');
        sb.append(c.getName());
        String key = sb.toString();
        Boolean ret = _customHash.get(key);

        if (ret != null) {
            return ret;
        }

        while (!Object.class.equals(c)) {
            try {
                c.getDeclaredMethod("hashCode");
                _customHash.put(key, true);
                return true;
            } catch (Exception ignored) {
            }
            c = c.getSuperclass();
        }
        _customHash.put(key, false);
        return false;
    }

    /**
     * Generates a 'deep' hash code for an object, considering its entire object graph.
     *
     * @param obj Object to hash.
     * @return Deep hash code as an int.
     */
    public static int deepHashCode(Object obj) {
        Map<Object, Object> visited = new IdentityHashMap<>();
        return deepHashCode(obj, visited);
    }

    private static int deepHashCode(Object obj, Map<Object, Object> visited) {
        LinkedList<Object> stack = new LinkedList<>();
        stack.addFirst(obj);
        int hash = 0;

        while (!stack.isEmpty()) {
            obj = stack.removeFirst();
            if (obj == null || visited.containsKey(obj)) {
                continue;
            }

            visited.put(obj, null);

            // Ensure array order matters to hash
            if (obj.getClass().isArray()) {
                final int len = Array.getLength(obj);
                long result = 1;

                for (int i = 0; i < len; i++) {
                    Object element = Array.get(obj, i);
                    result = 31 * result + deepHashCode(element, visited); // recursive
                }
                hash += (int) result;
                continue;
            }

            // Ensure list order matters to hash
            if (obj instanceof List) {
                List<?> list = (List<?>) obj;
                long result = 1;

                for (Object element : list) {
                    result = 31 * result + deepHashCode(element, visited);  // recursive
                }
                hash += (int) result;
                continue;
            }

            if (obj instanceof Collection) {
                stack.addAll(0, (Collection<?>) obj);
                continue;
            }

            if (obj instanceof Map) {
                stack.addAll(0, ((Map<?, ?>) obj).keySet());
                stack.addAll(0, ((Map<?, ?>) obj).values());
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
                    stack.addFirst(field.get(obj));
                } catch (Exception ignored) {
                }
            }
        }
        return hash;
    }

    private static final double SCALE_DOUBLE = Math.pow(10, 10);

    private static int hashDouble(double value) {
        double normalizedValue = Math.round(value * SCALE_DOUBLE) / SCALE_DOUBLE;
        long bits = Double.doubleToLongBits(normalizedValue);
        return (int) (bits ^ (bits >>> 32));
    }

    private static final float SCALE_FLOAT = (float) Math.pow(10, 5); // Scale according to epsilon for float

    private static int hashFloat(float value) {
        float normalizedValue = Math.round(value * SCALE_FLOAT) / SCALE_FLOAT;
        int bits = Float.floatToIntBits(normalizedValue);
        return bits;
    }

    private enum DiffCategory {
        VALUE("Expected: %s\nFound: %s"),
        TYPE("Expected type: %s\nFound type: %s"),
        SIZE("Expected size: %d\nFound size: %d"),
        LENGTH("Expected length: %d\nFound length: %d"),
        DIMENSION("Expected dimensions: %d\nFound dimensions: %d");

        private final String formatString;

        DiffCategory(String formatString) {
            this.formatString = formatString;
        }

        public String format(Object expected, Object found) {
            return String.format(formatString, expected, found);
        }
    }

    private enum Difference {
        // Basic value difference (includes numbers, atomic values, field values)
        VALUE_MISMATCH("value mismatch", DiffCategory.VALUE),
        FIELD_VALUE_MISMATCH("field value mismatch", DiffCategory.VALUE),

        // Collection-specific
        COLLECTION_SIZE_MISMATCH("collection size mismatch", DiffCategory.SIZE),
        COLLECTION_MISSING_ELEMENT("missing collection element", DiffCategory.VALUE),
        COLLECTION_TYPE_MISMATCH("collection type mismatch", DiffCategory.TYPE),

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

        public String getDescription() { return description; }
        public DiffCategory getCategory() { return category; }
    }

    private static String generateBreadcrumb(Deque<ItemsToCompare> stack) {
        ItemsToCompare diffItem = stack.peek();
        if (diffItem == null) {
            return "Unable to determine difference";
        }

        StringBuilder result = new StringBuilder();

        // Build the path AND get the mismatch phrase
        PathResult pr = buildPathContextAndPhrase(diffItem);
        String pathStr = pr.path;

        // Format with unicode arrow (→) and the difference description
        if (diffItem.difference != null) {
            result.append("[").append(diffItem.difference.getDescription()).append("] → ")
                    .append(pathStr).append("\n");
        } else {
            result.append(pathStr).append("\n");
        }

        // Format the difference details
        formatDifference(result, diffItem);

        return result.toString();
    }

    private static PathResult buildPathContextAndPhrase(ItemsToCompare diffItem) {
        List<ItemsToCompare> path = getPath(diffItem);
        if (path.isEmpty()) {
            return new PathResult("", null);
        }

        // 1) Format root
        StringBuilder sb = new StringBuilder();
        ItemsToCompare rootItem = path.get(0);
        sb.append(formatRootObject(rootItem._key1));  // "Dictionary {...}"

        // If no deeper path, just return
        if (path.size() == 1) {
            return new PathResult(sb.toString(),
                    rootItem.difference != null ? rootItem.difference.getDescription() : null);
        }

        // 2) Build up child path
        StringBuilder sb2 = new StringBuilder();
        for (int i = 1; i < path.size(); i++) {
            ItemsToCompare cur = path.get(i);

            // If the path item is purely used to store the 'difference' placeholder,
            // and does not represent a real mapKey/fieldName/arrayIndices, skip printing it:
            // e.g. skip if fieldName is "arrayLength", "unmatchedElement", "size", etc.
            if (shouldSkipPlaceholder(cur.fieldName)) {
                continue;
            }

            // If it's a mapKey, we do the " key:"someKey" value: Something"
            if (cur.mapKey != null) {
                appendSpaceIfNeeded(sb2);
                sb2.append("key:\"")
                        .append(formatMapKey(cur.mapKey))
                        .append("\" value: ")
                        .append(formatValueConcise(cur._key1));
            }
            // If it's a normal field name
            else if (cur.fieldName != null) {
                appendSpaceIfEndsWithBrace(sb2);
                sb2.append(".").append(cur.fieldName);
            }
            // If it’s array indices
            else if (cur.arrayIndices != null) {
                for (int idx : cur.arrayIndices) {
                    appendSpaceIfEndsWithBrace(sb2);
                    sb2.append("[").append(idx).append("]");
                }
            }
        }

        // If we built child path text, attach it after " @ "
        if (sb2.length() > 0) {
            sb.append(" @ ");
            sb.append(sb2);
        }

        // 3) Find the first mismatch phrase from the path
        String mismatchPhrase = null;
        for (ItemsToCompare item : path) {
            if (item.difference != null) {
                mismatchPhrase = item.difference.getDescription();
                break;
            }
        }

        return new PathResult(sb.toString(), mismatchPhrase);
    }

    /**
     * Decide if a "fieldName" is purely a placeholder that you do NOT want
     * to print in the path.
     * e.g., "value", "size", "dimensionality", "unmatchedElement", etc.
     */
    private static boolean shouldSkipPlaceholder(String fieldName) {
        if (fieldName == null) {
            return false;
        }
        switch (fieldName) {
            case "value":
            case "size":
            case "type":
            case "dimensionality":
            case "componentType":
            case "unmatchedKey":
            case "unmatchedElement":
            case "arrayLength":
                return true;
            default:
                return false;
        }
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

    /**
     * If the last character in sb is '}', append exactly one space.
     * Otherwise do nothing.
     *
     * This ensures we get:
     *    Pet {...} .nickNames
     * instead of
     *    Pet {...}.nickNames
     */
    private static void appendSpaceIfEndsWithBrace(StringBuilder sb) {
        int len = sb.length();
        if (len > 0 && sb.charAt(len - 1) == '}') {
            sb.append(' ');
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
        Object first = col.iterator().next();
        return first != null ? first.getClass() : null;
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
        if (Converter.isSimpleTypeConversionSupported(value.getClass(), value.getClass())) {
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
        if (value == null) return "null";

        try {
            // Handle collections
            if (value instanceof Collection) {
                Collection<?> col = (Collection<?>) value;
                String typeName = value.getClass().getSimpleName();
                return String.format("%s(%s)", typeName,
                        col.isEmpty() ? "0..0" : "0.." + (col.size() - 1));
            }

            // Handle maps
            if (value instanceof Map) {
                Map<?, ?> map = (Map<?, ?>) value;
                String typeName = value.getClass().getSimpleName();
                return String.format("%s[%s]", typeName,
                        map.isEmpty() ? "0..0" : "0.." + (map.size() - 1));
            }

            // Handle arrays
            if (value.getClass().isArray()) {
                int length = Array.getLength(value);
                String typeName = getTypeDescription(value.getClass().getComponentType());
                return String.format("%s[%s]", typeName,
                        length == 0 ? "0..0" : "0.." + (length - 1));
            }

            // Handle simple types
            if (Converter.isSimpleTypeConversionSupported(value.getClass(), value.getClass())) {
                return formatSimpleValue(value);
            }

            // For objects, include basic fields
            Collection<Field> fields = ReflectionUtils.getAllDeclaredFields(value.getClass());
            StringBuilder sb = new StringBuilder(value.getClass().getSimpleName());
            sb.append(" {");
            boolean first = true;

            for (Field field : fields) {
                if (field.getName().startsWith("this$")) {
                    continue;
                }
                if (!first) sb.append(", ");
                first = false;

                Object fieldValue = field.get(value);
                sb.append(field.getName()).append(": ");

                if (fieldValue == null) {
                    sb.append("null");
                    continue;
                }

                Class<?> fieldType = field.getType();
                if (Converter.isSimpleTypeConversionSupported(fieldType, fieldType)) {
                    // Simple type - show value
                    sb.append(formatSimpleValue(fieldValue));
                }
                else if (fieldType.isArray()) {
                    // Array - show type and size
                    int length = Array.getLength(fieldValue);
                    String typeName = getTypeDescription(fieldType.getComponentType());
                    sb.append(String.format("%s[%s]", typeName,
                            length == 0 ? "0..0" : "0.." + (length - 1)));
                }
                else if (Collection.class.isAssignableFrom(fieldType)) {
                    // Collection - show type and size
                    Collection<?> col = (Collection<?>) fieldValue;
                    sb.append(String.format("%s(%s)", fieldType.getSimpleName(),
                            col.isEmpty() ? "0..0" : "0.." + (col.size() - 1)));
                }
                else if (Map.class.isAssignableFrom(fieldType)) {
                    // Map - show type and size
                    Map<?, ?> map = (Map<?, ?>) fieldValue;
                    sb.append(String.format("%s[%s]", fieldType.getSimpleName(),
                            map.isEmpty() ? "0..0" : "0.." + (map.size() - 1)));
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

        if (value instanceof String) return "\"" + value + "\"";
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
        // For other types, just show type and toString
        return value.getClass().getSimpleName() + ":" + value;
    }
    
    private static String formatValue(Object value) {
        if (value == null) return "null";

        if (value instanceof Number) {
            return formatNumber((Number) value);
        }

        if (value instanceof String) return "\"" + value + "\"";
        if (value instanceof Character) return "'" + value + "'";

        if (value instanceof Date) {
            return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format((Date)value);
        }

        // If it's a simple type, use toString()
        if (Converter.isSimpleTypeConversionSupported(value.getClass(), value.getClass())) {
            return String.valueOf(value);
        }

        // For complex objects (not Array, Collection, Map, or simple type)
        if (!(value.getClass().isArray() || value instanceof Collection || value instanceof Map)) {
            return formatComplexObject(value, new IdentityHashMap<>());
        }

        return value.getClass().getSimpleName() + " {" + formatObjectContents(value) + "}";
    }

    private static String formatObjectContents(Object obj) {
        if (obj == null) return "null";

        if (obj instanceof Collection) {
            return formatCollectionContents((Collection<?>) obj);
        }

        if (obj instanceof Map) {
            return formatMapContents((Map<?, ?>) obj);
        }

        if (obj.getClass().isArray()) {
            int length = Array.getLength(obj);
            StringBuilder sb = new StringBuilder();
            sb.append("length=").append(length);
            if (length > 0) {
                sb.append(", elements=[");
                for (int i = 0; i < length && i < 3; i++) {
                    if (i > 0) sb.append(", ");
                    sb.append(formatValue(Array.get(obj, i)));
                }
                if (length > 3) sb.append(", ...");
                sb.append("]");
            }
            return sb.toString();
        }

        Collection<Field> fields = ReflectionUtils.getAllDeclaredFields(obj.getClass());
        StringBuilder sb = new StringBuilder();
        boolean first = true;

        for (Field field : fields) {
            try {
                if (field.getName().startsWith("this$")) {
                    continue;
                }
                if (!first) sb.append(", ");
                first = false;
                sb.append(field.getName()).append(": ");
                Object value = field.get(obj);
                sb.append(formatValue(value));
            } catch (Exception ignored) {
            }
        }

        return sb.toString();
    }

    private static String formatCollectionContents(Collection<?> collection) {
        StringBuilder sb = new StringBuilder();
        sb.append("size=").append(collection.size());
        if (!collection.isEmpty()) {
            sb.append(", elements=[");
            Iterator<?> it = collection.iterator();
            for (int i = 0; i < 3 && it.hasNext(); i++) {
                if (i > 0) sb.append(", ");
                sb.append(formatValue(it.next()));
            }
            if (collection.size() > 3) sb.append(", ...");
            sb.append("]");
        }
        return sb.toString();
    }

    private static String formatMapContents(Map<?, ?> map) {
        StringBuilder sb = new StringBuilder();
        sb.append("size=").append(map.size());
        if (!map.isEmpty()) {
            sb.append(", entries=[");
            Iterator<?> it = map.entrySet().iterator();
            for (int i = 0; i < 3 && it.hasNext(); i++) {
                if (i > 0) sb.append(", ");
                Map.Entry<?, ?> entry = (Map.Entry<?, ?>) it.next();
                sb.append(formatValue(entry.getKey()))
                        .append("=")
                        .append(formatValue(entry.getValue()));
            }
            if (map.size() > 3) sb.append(", ...");
            sb.append("]");
        }
        return sb.toString();
    }

    private static String formatComplexObject(Object obj, IdentityHashMap<Object, Object> visited) {
        if (obj == null) return "null";

        // Check for cycles
        if (visited.containsKey(obj)) {
            return obj.getClass().getSimpleName() + "#" +
                    Integer.toHexString(System.identityHashCode(obj)) + " (cycle)";
        }

        visited.put(obj, obj);

        StringBuilder sb = new StringBuilder();
        sb.append(obj.getClass().getSimpleName());
        sb.append(" {");

        Collection<Field> fields = ReflectionUtils.getAllDeclaredFields(obj.getClass());
        boolean first = true;

        for (Field field : fields) {
            try {
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
        visited.remove(obj);  // Remove from visited as we're done with this object
        return sb.toString();
    }

    private static String formatArrayNotation(Object array) {
        if (array == null) return "null";

        int length = Array.getLength(array);
        String typeName = getTypeDescription(array.getClass().getComponentType());
        return String.format("%s[%s]", typeName,
                length == 0 ? "0..0" : "0.." + (length - 1));
    }
    
    private static String formatCollectionNotation(Collection<?> col) {
        if (col == null) return "null";

        StringBuilder sb = new StringBuilder();
        sb.append(col.getClass().getSimpleName());

        // Only add type parameter if it's more specific than Object
        Class<?> elementType = getCollectionElementType(col);
        if (elementType != null && elementType != Object.class) {
            sb.append("<").append(getTypeDescription(elementType)).append(">");
        }

        sb.append("(");
        if (col.isEmpty()) {
            sb.append("0..0");
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

        sb.append("[");
        if (map.isEmpty()) {
            sb.append("0..0");
        } else {
            sb.append("0..").append(map.size() - 1);
        }
        sb.append("]");

        return sb.toString();
    }

    private static String formatMapKey(Object key) {
        if (key instanceof String) {
            String strKey = (String) key;
            // Strip any existing double quotes
            if (strKey.startsWith("\"") && strKey.endsWith("\"")) {
                strKey = strKey.substring(1, strKey.length() - 1);
            }
            return strKey;
        }
        return formatValue(key);
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
        if (Converter.isSimpleTypeConversionSupported(obj.getClass(), obj.getClass())) {
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

    private static int getContainerSize(Object container) {
        if (container == null) return 0;
        if (container instanceof Collection) return ((Collection<?>) container).size();
        if (container instanceof Map) return ((Map<?,?>) container).size();
        if (container.getClass().isArray()) return Array.getLength(container);
        return 0;
    }

    private enum ContainerType {
        ARRAY,
        COLLECTION,
        MAP,
        OBJECT
    };
}