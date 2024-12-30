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
    
    private enum ContainerType {
        ARRAY,          // Array (any dimension)
        SET,            // Unordered collection
        COLLECTION,     // Ordered collection
        MAP,           // Key/Value pairs
        OBJECT         // Instance fields
    }
    
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
            if (obj instanceof Set) {
                return ContainerType.SET;
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

    // Enum to represent different types of differences
    private enum DifferenceType {
        SIZE_MISMATCH,        // Different sizes in collections/arrays/maps
        VALUE_MISMATCH,       // Different values in simple types
        TYPE_MISMATCH,        // Different types
        NULL_MISMATCH,        // One value null, other non-null
        KEY_MISMATCH         // Map key not found
    }

    private static DifferenceType determineDifferenceType(ItemsToCompare item) {
        // Handle null cases
        if (item._key1 == null || item._key2 == null) {
            return DifferenceType.NULL_MISMATCH;
        }

        // Handle type mismatches
        if (!item._key1.getClass().equals(item._key2.getClass())) {
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

        // Must be a value mismatch
        return DifferenceType.VALUE_MISMATCH;
    }

    /**
     * Generates a breadcrumb path from the comparison stack.
     *
     * @param stack Deque of ItemsToCompare representing the path to the difference.
     * @return A formatted breadcrumb string.
     */
    private static String generateBreadcrumb(Deque<ItemsToCompare> stack) {
        ItemsToCompare diffItem = stack.peek();
        if (diffItem == null) {
            return "Unable to determine difference";
        }

        StringBuilder result = new StringBuilder();
        DifferenceType type = determineDifferenceType(diffItem);
        result.append(type).append("\n");

        StringBuilder pathStr = new StringBuilder();

        if (type == DifferenceType.SIZE_MISMATCH) {
            // For size mismatches, just show container type with generic info
            Object container = diffItem._key1;
            String typeInfo = getContainerTypeInfo(container);
            pathStr.append(container.getClass().getSimpleName())
                    .append(typeInfo);
        } else if (type == DifferenceType.TYPE_MISMATCH &&
                (diffItem._key1 instanceof Collection || diffItem._key1 instanceof Map)) {
            // For collection/map type mismatches, just show the container types
            Object container = diffItem._key1;
            String typeInfo = getContainerTypeInfo(container);
            pathStr.append(container.getClass().getSimpleName())
                    .append(typeInfo);
        } else if (diffItem.fieldName != null && "arrayLength".equals(diffItem.fieldName)) {
            // For array length mismatches, just show array type
            Object array = diffItem._key1;
            pathStr.append(array.getClass().getComponentType().getSimpleName())
                    .append("[]");
        } else {
            // Build path from root to difference
            List<ItemsToCompare> path = getPath(diffItem);

            // Format all but the last element
            for (int i = 0; i < path.size() - 1; i++) {
                ItemsToCompare item = path.get(i);
                boolean isArray = item.arrayIndices != null && item.arrayIndices.getClass().isArray();
                if (i > 0 && !isArray) {
                    // Don't place a "dot" in front of [], e.g. pets<Pet[]>[7].name<String>   (no dot in front of [7])
                    pathStr.append(".");
                }
                pathStr.append(formatPathElement(item));
            }
            
            // Handle the last element (diffItem)
            if (diffItem.arrayIndices != null) {
                pathStr.append(" at [").append(diffItem.arrayIndices[0]).append("]");
            } else if (diffItem.fieldName != null) {
                if ("unmatchedKey".equals(diffItem.fieldName)) {
                    pathStr.append(" key not found");
                } else if ("unmatchedElement".equals(diffItem.fieldName)) {
                    pathStr.append(" element not found");
                } else {
                    if (pathStr.length() > 0) pathStr.append(".");
                    // Get field type information
                    try {
                        Field field = ReflectionUtils.getField(diffItem.parent._key1.getClass(), diffItem.fieldName);
                        if (field != null) {
                            pathStr.append(diffItem.fieldName)
                                    .append("<")
                                    .append(getTypeDescription(field.getType()))
                                    .append(">");
                        } else {
                            pathStr.append(diffItem.fieldName);
                        }
                    } catch (Exception e) {
                        pathStr.append(diffItem.fieldName);
                    }
                }
            }
        }

        if (pathStr.length() > 0) {
            result.append(pathStr).append("\n");
        }

        // Format the actual difference
        if (diffItem.fieldName != null && "arrayLength".equals(diffItem.fieldName)) {
            result.append("  Expected length: ").append(Array.getLength(diffItem._key1))
                    .append("\n  Found length: ").append(Array.getLength(diffItem._key2));
        } else {
            formatDifference(result, diffItem, type);
        }

        return result.toString();
    }

    private static String getContainerTypeInfo(Object container) {
        if (container instanceof Collection) {
            Class<?> elementType = getCollectionElementType((Collection<?>)container);
            return elementType != null ? "<" + elementType.getSimpleName() + ">" : "";
        }
        if (container instanceof Map) {
            Map<?,?> map = (Map<?,?>)container;
            if (!map.isEmpty()) {
                Map.Entry<?,?> entry = map.entrySet().iterator().next();
                String keyType = entry.getKey() != null ? entry.getKey().getClass().getSimpleName() : "Object";
                String valueType = entry.getValue() != null ? entry.getValue().getClass().getSimpleName() : "Object";
                return "<" + keyType + "," + valueType + ">";
            }
        }
        return "";
    }

    private static Class<?> getCollectionElementType(Collection<?> collection) {
        if (collection.isEmpty()) {
            return null;
        }
        Object first = collection.iterator().next();
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

    private static String formatPathElement(ItemsToCompare item) {
        StringBuilder sb = new StringBuilder();

        // Add class name or field name
        if (item.parent == null) {
            // Root element - show class name with simple fields
            sb.append(formatValueConcise(item._key1));
        } else {
            // Non-root element - show field name or container access
            if (item.fieldName != null) {
                // Get the field from the parent class to determine its type
                try {
                    Field field = ReflectionUtils.getField(item.parent._key1.getClass(), item.fieldName);
                    if (field != null) {
                        sb.append(item.fieldName).append("<").append(getTypeDescription(field.getType())).append(">");
                    } else {
                        sb.append(item.fieldName);
                    }
                } catch (Exception e) {
                    sb.append(item.fieldName);
                }
            } else if (item.arrayIndices != null) {
                for (int index : item.arrayIndices) {
                    sb.append("[").append(index).append("]");
                }
            } else if (item.mapKey != null) {
                sb.append(".key(").append(formatValue(item.mapKey)).append(")");
            }
        }

        return sb.toString();
    }
    
    private static void formatDifference(StringBuilder result, ItemsToCompare item, DifferenceType type) {
        switch (type) {
            case NULL_MISMATCH:
                result.append("  Expected: ").append(formatValueConcise(item._key1))
                        .append("\n  Found: ").append(formatValueConcise(item._key2));
                break;

            case SIZE_MISMATCH:
                if (item.containerType == ContainerType.ARRAY) {
                    result.append("  Expected length: ").append(Array.getLength(item._key1))
                            .append("\n  Found length: ").append(Array.getLength(item._key2));
                } else {
                    result.append("  Expected size: ").append(getContainerSize(item._key1))
                            .append("\n  Found size: ").append(getContainerSize(item._key2));
                }
                break;

            case TYPE_MISMATCH:
                result.append("  Expected type: ")
                        .append(item._key1 != null ? item._key1.getClass().getSimpleName() : "null")
                        .append("\n  Found type: ")
                        .append(item._key2 != null ? item._key2.getClass().getSimpleName() : "null");
                break;

            case VALUE_MISMATCH:
                result.append("  Expected: ").append(formatValueConcise(item._key1))
                        .append("\n  Found: ").append(formatValueConcise(item._key2));
                break;
        }
    }

    private static String formatValueConcise(Object value) {
        if (value == null) return "null";

        // Handle collections
        if (value instanceof Collection) {
            Collection<?> col = (Collection<?>) value;
            return value.getClass().getSimpleName() + " (size=" + col.size() + ")";
        }

        // Handle maps
        if (value instanceof Map) {
            Map<?,?> map = (Map<?,?>) value;
            return value.getClass().getSimpleName() + " (size=" + map.size() + ")";
        }

        // Handle arrays
        if (value.getClass().isArray()) {
            int length = Array.getLength(value);
            return value.getClass().getComponentType().getSimpleName() +
                    "[] (length=" + length + ")";
        }

        // Handle simple types (String, Number, Boolean, etc.)
        if (Converter.isSimpleTypeConversionSupported(value.getClass(), value.getClass())) {
            return formatValue(value);
        }

        // For objects, include all simple fields
        try {
            Collection<Field> fields = ReflectionUtils.getAllDeclaredFields(value.getClass());
            StringBuilder sb = new StringBuilder(value.getClass().getSimpleName());
            sb.append(" {");
            boolean first = true;

            // Include all simple-type fields
            for (Field field : fields) {
                Object fieldValue = field.get(value);
                if (fieldValue != null &&
                        Converter.isSimpleTypeConversionSupported(field.getType(), field.getType())) {
                    if (!first) sb.append(", ");
                    sb.append(field.getName()).append(": ");
                    sb.append(formatSimpleValue(fieldValue));
                    first = false;
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
        if (value instanceof String) return "\"" + value + "\"";
        if (value instanceof Number) return value.toString();
        if (value instanceof Boolean) return value.toString();
        if (value instanceof Date) {
            return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format((Date)value);
        }
        // For other types, just show type and toString
        return value.getClass().getSimpleName() + ":" + value;
    }
    
    private static String formatValue(Object value) {
        if (value == null) return "null";

        if (value instanceof String) return "\"" + value + "\"";

        if (value instanceof Number) {
            if (value instanceof Float || value instanceof Double) {
                return String.format("%.10g", value);
            } else {
                return String.valueOf(value);
            }
        }

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

    private static String getTypeDescription(Class<?> type) {
        if (type.isArray()) {
            return type.getComponentType().getSimpleName() + "[]";
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
}