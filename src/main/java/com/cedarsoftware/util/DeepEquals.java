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
        private final Class<?> elementClass;

        // Constructor for root
        private ItemsToCompare(Object k1, Object k2) {
            this(k1, k2, null, null, null, null);
        }

        // Constructor for field access
        private ItemsToCompare(Object k1, Object k2, String fieldName, ItemsToCompare parent) {
            this(k1, k2, parent, fieldName, null, null);
        }

        // Constructor for array access
        private ItemsToCompare(Object k1, Object k2, int[] indices, ItemsToCompare parent) {
            this(k1, k2, parent, null, indices, null);
        }

        // Constructor for map access
        private ItemsToCompare(Object k1, Object k2, String mapKey, ItemsToCompare parent, boolean isMapKey) {
            this(k1, k2, parent, null, null, mapKey);
        }

        // Base constructor
        private ItemsToCompare(Object k1, Object k2, ItemsToCompare parent,
                               String fieldName, int[] arrayIndices, String mapKey) {
            this._key1 = k1;
            this._key2 = k2;
            this.parent = parent;
            this.containerType = getContainerType(k1);
            this.fieldName = fieldName;
            this.arrayIndices = arrayIndices;
            this.mapKey = mapKey;
            this.elementClass = k1 != null ? k1.getClass() :
                    (k2 != null ? k2.getClass() : null);
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
    
        // Helper method to get containing class
        public Class<?> getContainingClass() {
            if (parent == null) {
                return _key1 != null ? _key1.getClass() :
                        _key2 != null ? _key2.getClass() : null;
            }
            return parent._key1 != null ? parent._key1.getClass() :
                    parent._key2 != null ? parent._key2.getClass() : null;
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
                return false;
            }

            // Handle all numeric comparisons first
            if (key1 instanceof Number && key2 instanceof Number) {
                if (!compareNumbers((Number) key1, (Number) key2)) {
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
                return false;
            }

            if (key1 instanceof AtomicBoolean && key2 instanceof AtomicBoolean) {
                if (!compareAtomicBoolean((AtomicBoolean) key1, (AtomicBoolean) key2)) {
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
                            return false;
                        }
                        continue;
                    } catch (Exception ignored) { }   // Fall back to equals() if compareTo() fails
                }
                if (!key1.equals(key2)) {
                    return false;
                }
                continue;
            }
            
            // Set comparison
            if (key1 instanceof Set) {
                if (!(key2 instanceof Set)) {
                    return false;
                }
                if (!decomposeUnorderedCollection((Collection<?>) key1, (Collection<?>) key2, stack)) {
                    return false;
                }
                continue;
            } else if (key2 instanceof Set) {
                return false;
            }

            // Collection comparison
            if (key1 instanceof Collection) {   // If Collections, they both must be Collection
                if (!(key2 instanceof Collection)) {
                    return false;
                }
                if (!decomposeOrderedCollection((Collection<?>) key1, (Collection<?>) key2, stack)) {
                    return false;
                }
                continue;
            } else if (key2 instanceof Collection) {
                return false;
            }

            // Map comparison
            if (key1 instanceof Map) {
                if (!(key2 instanceof Map)) {
                    return false;
                }
                if (!decomposeMap((Map<?, ?>) key1, (Map<?, ?>) key2, stack)) {
                    return false;
                }
                continue;
            } else if (key2 instanceof Map) {
                return false;
            }

            // Array comparison
            if (key1Class.isArray()) {
                if (!key2Class.isArray()) {
                    return false;
                }
                if (!decomposeArray(key1, key2, stack)) {
                    return false;
                }
                continue;
            } else if (key2Class.isArray()) {
                return false;
            }

            // Must be same class if not a container type
            if (!key1Class.equals(key2Class)) {   // Must be same class
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
                    currentItem
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
                stack.addFirst(new ItemsToCompare(
                        item1,
                        null,
                        "unmatchedElement",
                        currentItem
                ));
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
                stack.addFirst(new ItemsToCompare(
                        item1,
                        null,
                        "unmatchedElement",
                        currentItem
                ));
                return false;
            }
        }

        return true;
    }

    private static boolean decomposeOrderedCollection(Collection<?> col1, Collection<?> col2, Deque<ItemsToCompare> stack) {
        ItemsToCompare currentItem = stack.peek();

        // Check sizes first
        if (col1.size() != col2.size()) {
            stack.addFirst(new ItemsToCompare(
                    col1,
                    col2,
                    "size",
                    currentItem
            ));
            return false;
        }

        // Push elements in order
        Iterator<?> i1 = col1.iterator();
        Iterator<?> i2 = col2.iterator();
        int index = 0;

        while (i1.hasNext()) {
            Object item1 = i1.next();
            Object item2 = i2.next();

            stack.addFirst(new ItemsToCompare(
                    item1,
                    item2,
                    new int[]{index++},
                    currentItem
            ));
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
            stack.addFirst(new ItemsToCompare(
                    map1,
                    map2,
                    "size",
                    currentItem
            ));
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
                stack.addFirst(new ItemsToCompare(
                        entry.getKey(),
                        null,
                        "unmatchedKey",
                        currentItem
                ));
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
                            entry.getValue(),
                            otherEntry.getValue(),
                            formatValue(entry.getKey()),
                            currentItem,
                            false  // isMapValue
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
                stack.addFirst(new ItemsToCompare(
                        entry.getKey(),
                        null,
                        "unmatchedKey",
                        currentItem
                ));
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
            stack.addFirst(new ItemsToCompare(
                    array1,
                    array2,
                    "dimensionality",
                    currentItem
            ));
            return false;
        }

        // 2. Check component types
        if (!array1.getClass().getComponentType().equals(array2.getClass().getComponentType())) {
            stack.addFirst(new ItemsToCompare(
                    array1,
                    array2,
                    "componentType",
                    currentItem
            ));
            return false;
        }

        // 3. Check lengths
        int len1 = Array.getLength(array1);
        int len2 = Array.getLength(array2);
        if (len1 != len2) {
            stack.addFirst(new ItemsToCompare(
                    array1,
                    array2,
                    "arrayLength",
                    currentItem
            ));
            return false;
        }

        // 4. Push all elements onto stack (with their full dimensional indices)
        for (int i = len1 - 1; i >= 0; i--) {
            stack.addFirst(new ItemsToCompare(
                    Array.get(array1, i),
                    Array.get(array2, i),
                    new int[]{i},    // For multidimensional arrays, this gets built up
                    currentItem
            ));
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
                Object value1 = field.get(obj1);
                Object value2 = field.get(obj2);

                stack.addFirst(new ItemsToCompare(
                        value1,
                        value2,
                        field.getName(),
                        currentItem
                ));
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

    // Enum to represent types of differences
    public enum DifferenceType {
        TYPE_MISMATCH,
        SIZE_MISMATCH,
        VALUE_MISMATCH,
        KEY_MISMATCH,
        ELEMENT_NOT_FOUND,
        DIMENSIONALITY_MISMATCH
    }

    private static DifferenceType determineDifferenceType(ItemsToCompare item) {
        // Handle null cases
        if (item._key1 == null || item._key2 == null) {
            return DifferenceType.VALUE_MISMATCH;
        }

        // Handle type mismatches
        if (!item._key1.getClass().equals(item._key2.getClass())) {
            // Special case for array dimensionality
            if (item.fieldName != null && item.fieldName.equals("dimensionality")) {
                return DifferenceType.DIMENSIONALITY_MISMATCH;
            }
            return DifferenceType.TYPE_MISMATCH;
        }

        // Handle size mismatches for containers
        if (item.fieldName != null && item.fieldName.equals("size")) {
            return DifferenceType.SIZE_MISMATCH;
        }

        // Handle map key not found
        if (item.fieldName != null && item.fieldName.equals("unmatchedKey")) {
            return DifferenceType.KEY_MISMATCH;
        }

        // Handle collection element not found
        if (item.fieldName != null && item.fieldName.equals("unmatchedElement")) {
            return DifferenceType.ELEMENT_NOT_FOUND;
        }

        // Must be a value mismatch
        return DifferenceType.VALUE_MISMATCH;
    }

    private static String generateBreadcrumb(Deque<ItemsToCompare> stack) {
        ItemsToCompare diffItem = stack.peek();
        if (diffItem == null) {
            return "Unable to determine difference";
        }

        StringBuilder result = new StringBuilder();
        DifferenceType type = determineDifferenceType(diffItem);
        result.append(type).append("\n");

        // Get the path string (everything up to the @)
        String pathStr = formatObjectContext(diffItem, type);  // Pass the difference type

        // Only append the path if we have one
        if (!pathStr.isEmpty()) {
            result.append(pathStr).append("\n");
        }

        // Format the actual difference
        formatDifference(result, diffItem, type);

        return result.toString();
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

    private static void formatDifference(StringBuilder result, ItemsToCompare item, DifferenceType type) {
        switch (type) {
//            case NULL_MISMATCH:
//                result.append("  Expected: ").append(formatNullMismatchValue(item._key1))
//                        .append("\n  Found: ").append(formatNullMismatchValue(item._key2));
//                break;
                
            case SIZE_MISMATCH:
                if (item.containerType == ContainerType.ARRAY) {
                    result.append("  Expected: ").append(formatArrayNotation(item._key1))
                            .append("\n  Found: ").append(formatArrayNotation(item._key2));
                } else {
                    result.append("  Expected size: ").append(getContainerSize(item._key1))
                            .append("\n  Found size: ").append(getContainerSize(item._key2));
                }
                break;

            case TYPE_MISMATCH:
                result.append("  Expected type: ")
                        .append(getTypeDescription(item._key1 != null ? item._key1.getClass() : null))
                        .append("\n  Found type: ")
                        .append(getTypeDescription(item._key2 != null ? item._key2.getClass() : null));
                break;

            case VALUE_MISMATCH:
                if (item.fieldName != null && item.fieldName.equals("arrayLength")) {
                    // For array length mismatches, just show the lengths
                    int expectedLength = Array.getLength(item._key1);
                    int foundLength = Array.getLength(item._key2);
                    result.append("  Expected length: ").append(expectedLength)
                            .append("\n  Found length: ").append(foundLength);
                } else {
                    result.append("  Expected: ").append(formatDifferenceValue(item._key1))
                            .append("\n  Found: ").append(formatDifferenceValue(item._key2));
                }
                break;

            case DIMENSIONALITY_MISMATCH:
                // Get the dimensions of both arrays
                int dim1 = getDimensions(item._key1);
                int dim2 = getDimensions(item._key2);
                result.append("  Expected dimensions: ").append(dim1)
                        .append("\n  Found dimensions: ").append(dim2);
                break;
        }
    }

    private static String formatNullMismatchValue(Object value) {
        if (value == null) {
            return "null";
        }

        // For arrays, use consistent notation without elements
        if (value.getClass().isArray()) {
            return formatArrayNotation(value);
        }

        // For collections and complex objects, don't add type prefix
        if (value instanceof Collection ||
                value instanceof Map ||
                !Converter.isSimpleTypeConversionSupported(value.getClass(), value.getClass())) {
            return formatValueConcise(value);
        }

        // For simple types, show type
        return String.format("%s: %s",
                getTypeDescription(value.getClass()),
                formatValue(value));
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

    private static String formatObjectContext(ItemsToCompare item, DifferenceType diffType) {
        if (item._key1 == null && item._key2 == null) {
            return "";
        }

        List<ItemsToCompare> path = getPath(item);
        if (path.isEmpty()) {
            return "";
        }

        StringBuilder context = new StringBuilder();
        formatRootObjectPart(context, path.get(0), diffType);

        if (path.size() > 1) {
            context.append(" @ ");
            formatPathElements(context, path);
        }

        return context.toString();
    }

    private static void formatRootObjectPart(StringBuilder context, ItemsToCompare rootItem, DifferenceType diffType) {
        context.append(formatRootObject(rootItem._key1, diffType));
    }

    private static void formatPathElements(StringBuilder context, List<ItemsToCompare> path) {
        for (int i = 1; i < path.size(); i++) {
            ItemsToCompare pathItem = path.get(i);
            ItemsToCompare nextItem = (i < path.size() - 1) ? path.get(i + 1) : null;

            // Skip arrayLength as it's handled as part of error type
            if (pathItem.fieldName != null && pathItem.fieldName.equals("arrayLength")) {
                continue;
            }

            // Start of path element
            if (i == 1) {
                if (pathItem.fieldName != null) {
                    context.append("field ");
                } else if (pathItem.arrayIndices != null) {
                    context.append("element ");
                }
            }

            // Build field path
            if (pathItem.fieldName != null) {
                if (i > 1 && !isErrorType(pathItem)) {
                    context.append(".");
                }
                if (isErrorType(pathItem)) {
                    context.append(" "); // Space before error type
                    appendErrorType(context, pathItem.fieldName);
                } else {
                    context.append(pathItem.fieldName);
                }
            }
            else if (pathItem.arrayIndices != null) {
                context.append("[").append(pathItem.arrayIndices[0]).append("]");
            }
            else if (pathItem.mapKey != null) {
                context.append(" key:\"").append(formatMapKey(pathItem.mapKey)).append("\"");
            }

            // Handle error types that follow this element
            if (nextItem != null && isErrorType(nextItem)) {
                context.append(" ");
                appendErrorType(context, nextItem.fieldName);
                i++; // Skip the error type item
            }
        }
    }

    private static boolean isErrorType(ItemsToCompare item) {
        if (item.fieldName == null) return false;
        return item.fieldName.equals("arrayLength") ||
                item.fieldName.equals("unmatchedKey") ||
                item.fieldName.equals("unmatchedElement") ||
                item.fieldName.equals("componentType");
    }

    private static void appendErrorType(StringBuilder context, String fieldName) {
        switch (fieldName) {
            case "arrayLength":
                context.append("array length mismatch");
                break;
            case "unmatchedKey":
                context.append("has unmatched key");
                break;
            case "unmatchedElement":
                context.append("has unmatched element");
                break;
            case "componentType":
                context.append("component type mismatch");
                break;
        }
    }
    
    private static boolean isArrayLengthMismatch(ItemsToCompare item) {
        return item != null &&
                item.fieldName != null &&
                item.fieldName.equals("arrayLength");
    }

    private static void formatPathElement(StringBuilder context, int index, ItemsToCompare pathItem) {
        if (pathItem.fieldName != null) {
            formatFieldElement(context, index, pathItem);
        }
        else if (pathItem.arrayIndices != null) {
            formatArrayElement(context, index, pathItem);
        }
        else if (pathItem.mapKey != null) {
            formatMapElement(context, index, pathItem);
        }
    }

    private static void formatFieldElement(StringBuilder context, int index, ItemsToCompare pathItem) {
        if (pathItem.fieldName.equals("unmatchedKey")) {
            context.append("has unmatched key");
        }
        else if (pathItem.fieldName.equals("unmatchedElement")) {
            context.append("has unmatched element");
        }
        else if (pathItem.fieldName.equals("componentType")) {
            context.append("component type mismatch");
        }
        else if (pathItem.fieldName.equals("size")) {
            context.append(pathItem.fieldName);
        }
        // Remove arrayLength case as it's handled in handleArrayLengthMismatch
        else {
            if (index == 1) {
                context.append("field ");
            }
            context.append(pathItem.fieldName);
        }
    }

    private static void formatArrayElement(StringBuilder context, int index, ItemsToCompare pathItem) {
        if (index == 1) {
            context.append("element ");
        }
        context.append("[").append(pathItem.arrayIndices[0]).append("]");
    }

    private static void formatMapElement(StringBuilder context, int index, ItemsToCompare pathItem) {
        if (index > 1) {
            String fieldName = pathItem.fieldName;
            if (fieldName != null && !fieldName.equals("null")) {
                context.append(" ");
                context.append(fieldName);
            }
        }
        context.append(" key:\"").append(formatMapKey(pathItem.mapKey)).append("\"");
    }

    private static void handleArrayLengthMismatch(StringBuilder context, ItemsToCompare nextItem, int index) {
        if (isArrayLengthMismatch(nextItem)) {
            context.append(" array length mismatch");
            index++;  // Skip the next item since we've handled it
        }
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
    
    private static String formatRootObject(Object obj, DifferenceType diffType) {
        if (obj == null) {
            return "null";
        }

        // Special handling for TYPE_MISMATCH and VALUE_MISMATCH on simple types
        if ((diffType == DifferenceType.TYPE_MISMATCH ||
                diffType == DifferenceType.VALUE_MISMATCH) &&
                Converter.isSimpleTypeConversionSupported(obj.getClass(), obj.getClass())) {
            // For simple types, show type: value
            return String.format("%s: %s",
                    getTypeDescription(obj.getClass()),
                    formatSimpleValue(obj));
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

    enum ContainerType {
        ARRAY {
            @Override
            public String format(String name, Class<?> type, Object value) {
                int length = Array.getLength(value);
                return String.format("%s<%s>:[%s]",
                        name,
                        getTypeDescription(type),
                        length == 0 ? "0" : "0.." + (length - 1));
            }
        },
        COLLECTION {
            @Override
            public String format(String name, Class<?> type, Object value) {
                Collection<?> col = (Collection<?>) value;
                Class<?> elementType = getCollectionElementType(col);
                String typeInfo = elementType != Object.class ?
                        String.format("<%s>", getTypeDescription(elementType)) : "";
                return String.format("%s%s:(%s)",
                        name,
                        typeInfo,
                        col.size() == 0 ? "0" : "0.." + (col.size() - 1));
            }
        },
        MAP {
            @Override
            public String format(String name, Class<?> type, Object value) {
                Map<?, ?> map = (Map<?, ?>) value;
                return String.format("%s<%s>:[%s]",
                        name,
                        getTypeDescription(type),
                        map.size() == 0 ? "0" : "0.." + (map.size() - 1));
            }
        },
        OBJECT {
            @Override
            public String format(String name, Class<?> type, Object value) {
                return String.format("%s<%s>:{..}",
                        name,
                        getTypeDescription(type));
            }
        };

        public abstract String format(String name, Class<?> type, Object value);
    }
}