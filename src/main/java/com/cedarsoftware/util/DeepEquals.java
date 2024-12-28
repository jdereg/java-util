package com.cedarsoftware.util;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
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
    private static final double floatEpsilon = 1e-6;

    // Set of primitive wrapper classes
    private static final Set<Class<?>> prims = new HashSet<>();

    static {
        prims.add(Byte.class);
        prims.add(Integer.class);
        prims.add(Long.class);
        prims.add(Double.class);
        prims.add(Character.class);
        prims.add(Float.class);
        prims.add(Boolean.class);
        prims.add(Short.class);
    }

    // Enum to represent different access types in the object graph
    private enum ContainerAccessType {
        FIELD,
        ARRAY_INDEX,
        COLLECTION,
        MAP_KEY,
        MAP_VALUE
    }

    // Class to hold information about items being compared
    private final static class ItemsToCompare {
        private final Object _key1;
        private final Object _key2;
        private final String fieldName;        // for FIELD access
        private final int[] arrayIndices;      // for ARRAY_INDEX access
        private final String mapKey;           // for MAP_KEY/MAP_VALUE access
        private final ContainerAccessType accessType;
        private final Class<?> containingClass;

        // Constructor for field access
        private ItemsToCompare(Object k1, Object k2, String fieldName, Class<?> containingClass) {
            _key1 = k1;
            _key2 = k2;
            this.fieldName = fieldName;
            this.arrayIndices = null;
            this.mapKey = null;
            this.accessType = ContainerAccessType.FIELD;
            this.containingClass = containingClass;
        }

        // Constructor for array access (supports multi-dimensional)
        private ItemsToCompare(Object k1, Object k2, int[] indices, Class<?> containingClass) {
            _key1 = k1;
            _key2 = k2;
            this.fieldName = null;
            this.arrayIndices = indices;
            this.mapKey = null;
            this.accessType = ContainerAccessType.ARRAY_INDEX;
            this.containingClass = containingClass;
        }

        // Constructor for map access
        private ItemsToCompare(Object k1, Object k2, String mapKey, Class<?> containingClass, boolean isMapKey) {
            _key1 = k1;
            _key2 = k2;
            this.fieldName = null;
            this.arrayIndices = null;
            this.mapKey = mapKey;
            this.accessType = isMapKey ? ContainerAccessType.MAP_KEY : ContainerAccessType.MAP_VALUE;
            this.containingClass = containingClass;
        }

        // Constructor for collection access
        private ItemsToCompare(Object k1, Object k2, Class<?> containingClass) {
            _key1 = k1;
            _key2 = k2;
            this.fieldName = null;
            this.arrayIndices = null;
            this.mapKey = null;
            this.accessType = ContainerAccessType.COLLECTION;
            this.containingClass = containingClass;
        }

        @Override
        public boolean equals(Object other) {
            if (!(other instanceof ItemsToCompare)) {
                return false;
            }
            ItemsToCompare that = (ItemsToCompare) other;

            // Must be comparing the same objects (identity)
            if (_key1 != that._key1 || _key2 != that._key2) {
                return false;
            }

            // Must have same access type and containing class
            if (this.accessType != that.accessType || !Objects.equals(containingClass, that.containingClass)) {
                return false;
            }

            // Compare based on access type and context
            switch (accessType) {
                case FIELD:
                    return Objects.equals(fieldName, that.fieldName);
                case ARRAY_INDEX:
                    return Arrays.equals(arrayIndices, that.arrayIndices);
                case MAP_KEY:
                case MAP_VALUE:
                    return Objects.equals(mapKey, that.mapKey);
                case COLLECTION:
                    return true;
                default:
                    return false;
            }
        }

        @Override
        public int hashCode() {
            int result = System.identityHashCode(_key1);
            result = 31 * result + System.identityHashCode(_key2);
            result = 31 * result + (containingClass != null ? containingClass.hashCode() : 0);
            result = 31 * result + accessType.hashCode();

            switch (accessType) {
                case FIELD:
                    result = 31 * result + (fieldName != null ? fieldName.hashCode() : 0);
                    break;
                case ARRAY_INDEX:
                    result = 31 * result + Arrays.hashCode(arrayIndices);
                    break;
                case MAP_KEY:
                case MAP_VALUE:
                    result = 31 * result + (mapKey != null ? mapKey.hashCode() : 0);
                    break;
            }
            return result;
        }
    }

    // Main deepEquals method without options
    public static boolean deepEquals(Object a, Object b) {
        return deepEquals(a, b, new HashMap<>());
    }

    // Main deepEquals method with options
    public static boolean deepEquals(Object a, Object b, Map<String, ?> options) {
        Set<ItemsToCompare> visited = new HashSet<>();
        Deque<ItemsToCompare> stack = new LinkedList<>();
        Class<?> rootClass = a != null ? a.getClass() : (b != null ? b.getClass() : null);
        boolean result = deepEquals(a, b, stack, options, visited, rootClass);

        if (!result && !stack.isEmpty()) {
            String breadcrumb = generateBreadcrumb(stack);
            System.out.println(breadcrumb);
            System.out.println("--------------------");
            ((Map<String, Object>) options).put("diff", breadcrumb);
        }

        return result;
    }

    // Recursive deepEquals implementation
    private static boolean deepEquals(Object a, Object b, Deque<ItemsToCompare> stack,
                                      Map<String, ?> options, Set<ItemsToCompare> visited, Class<?> containingClass) {
        Set<Class<?>> ignoreCustomEquals = (Set<Class<?>>) options.get(IGNORE_CUSTOM_EQUALS);
        final boolean allowStringsToMatchNumbers = convert2boolean(options.get(ALLOW_STRINGS_TO_MATCH_NUMBERS));

        stack.addFirst(new ItemsToCompare(a, b, containingClass));

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
                if (!compareUnorderedCollection((Collection<?>) key1, (Collection<?>) key2, key1Class)) {
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
                if (!compareOrderedCollection((Collection<?>) key1, (Collection<?>) key2, stack, visited, key1Class)) {
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
                if (!compareMap((Map<?, ?>) key1, (Map<?, ?>) key2, stack, visited, options, containingClass)) {
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
                if (!compareArrays(key1, key2, stack, visited, key1Class)) {
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
                        return false;
                    }
                    continue;
                }
            }

            // Perform field-by-field comparison
            Collection<Field> fields = ReflectionUtils.getAllDeclaredFields(key1Class);

            for (Field field : fields) {
                try {
                    Object value1 = field.get(key1);
                    Object value2 = field.get(key2);
                    ItemsToCompare dk = new ItemsToCompare(value1, value2, field.getName(), key1Class);
                    if (!visited.contains(dk)) {
                        stack.addFirst(dk);
                    }
                } catch (Exception ignored) {
                }
            }
        }
        return true;
    }
    
    /**
     * Generates a breadcrumb path from the comparison stack.
     *
     * @param stack Deque of ItemsToCompare representing the path to the difference.
     * @return A formatted breadcrumb string.
     */
    private static String generateBreadcrumb(Deque<ItemsToCompare> stack) {
        ItemsToCompare rootItem = stack.peek();
        DifferenceBuilder builder = null;
        Iterator<ItemsToCompare> it = stack.descendingIterator(); // Start from root

        // Initialize builder based on the root item's difference type
        if (it.hasNext()) {
            rootItem = it.next();
            builder = initializeDifferenceBuilder(rootItem);
        }

        if (builder == null) {
            return "Unable to determine difference type";
        }

        // Traverse the stack and build the path
        while (it.hasNext()) {
            ItemsToCompare item = it.next();
            switch (item.accessType) {
                case FIELD:
                    builder.appendField(
                            item.containingClass != null ? item.containingClass.getSimpleName() : "UnknownClass",
                            item.fieldName,
                            item._key1,
                            item.arrayIndices
                    );
                    break;
                case ARRAY_INDEX:
                    builder.appendField(
                            item.containingClass != null ? item.containingClass.getSimpleName() : "UnknownClass",
                            null,  // no field name for array access
                            item._key1,
                            item.arrayIndices
                    );
                    break;
                case COLLECTION:
                    if (builder.expectedSize != null && builder.foundSize != null) {
                        builder.appendCollectionAccess(
                                item.containingClass != null ? item.containingClass.getSimpleName() : "Collection",
                                builder.expectedSize,
                                builder.foundSize
                        );
                    }
                    break;
                case MAP_KEY:
                    builder.appendMapKey(item.mapKey);
                    break;
                case MAP_VALUE:
                    builder.appendMapValue(item.mapKey);
                    break;
                default:
                    builder.appendMapValue("What is this? *****");
                    break;
            }
        }

        return builder.toString();
    }

    /**
     * Initializes the DifferenceBuilder based on the root item's difference type.
     *
     * @param rootItem The root ItemsToCompare instance.
     * @return An initialized DifferenceBuilder.
     */
    private static DifferenceBuilder initializeDifferenceBuilder(ItemsToCompare rootItem) {
        DifferenceType type = null;
        Object expected = null;
        Object found = null;

        if (rootItem._key1 == null || rootItem._key2 == null) {
            type = DifferenceType.NULL_CHECK;
            expected = rootItem._key2;
            found = rootItem._key1;
        } else if (!rootItem._key1.getClass().equals(rootItem._key2.getClass())) {
            type = DifferenceType.TYPE_MISMATCH;
            expected = rootItem._key2;  // Use the actual objects
            found = rootItem._key1;     // Use the actual objects
        } else if (rootItem._key1 instanceof Collection || rootItem._key1 instanceof Map) {
            int size1 = rootItem._key1 instanceof Collection ?
                    ((Collection<?>) rootItem._key1).size() : ((Map<?, ?>) rootItem._key1).size();
            int size2 = rootItem._key2 instanceof Collection ?
                    ((Collection<?>) rootItem._key2).size() : ((Map<?, ?>) rootItem._key2).size();
            if (size1 != size2) {
                type = DifferenceType.SIZE_MISMATCH;
                expected = rootItem._key2.getClass().getSimpleName();
                found = rootItem._key1.getClass().getSimpleName();
            }
        } else {
            type = DifferenceType.VALUE_MISMATCH;
            expected = rootItem._key2;
            found = rootItem._key1;
        }

        if (type == null) {
            return null;
        }

        DifferenceBuilder builder = new DifferenceBuilder(type, expected, found);
        if (type == DifferenceType.SIZE_MISMATCH) {
            String containerType = rootItem.containingClass != null ? rootItem.containingClass.getSimpleName() : "UnknownContainer";
            int expectedSize = rootItem._key2 instanceof Collection ? ((Collection<?>) rootItem._key2).size() :
                    (rootItem._key2 instanceof Map ? ((Map<?, ?>) rootItem._key2).size() : 0);
            int foundSize = rootItem._key1 instanceof Collection ? ((Collection<?>) rootItem._key1).size() :
                    (rootItem._key1 instanceof Map ? ((Map<?, ?>) rootItem._key1).size() : 0);
            builder.withContainerInfo(containerType, expectedSize, foundSize);
        }
        return builder;
    }

    /**
     * Compares two arrays deeply.
     *
     * @param array1        First array.
     * @param array2        Second array.
     * @param stack         Comparison stack.
     * @param visited       Set of visited ItemsToCompare.
     * @param containingClass The class containing the arrays.
     * @return true if arrays are equal, false otherwise.
     */
    private static boolean compareArrays(Object array1, Object array2, Deque<ItemsToCompare> stack, Set<ItemsToCompare> visited, Class<?> containingClass) {
        final int len = Array.getLength(array1);
        if (len != Array.getLength(array2)) {
            return false;
        }

        for (int i = len - 1; i >= 0; i--) {
            Object elem1 = Array.get(array1, i);
            Object elem2 = Array.get(array2, i);

            ItemsToCompare dk = new ItemsToCompare(
                    elem1,
                    elem2,
                    new int[]{i},
                    containingClass
            );
            if (!visited.contains(dk)) {
                stack.addFirst(dk);
            }
        }
        return true;
    }

    /**
     * Compares two ordered collections (e.g., Lists) deeply.
     *
     * @param col1           First collection.
     * @param col2           Second collection.
     * @param stack          Comparison stack.
     * @param visited        Set of visited ItemsToCompare.
     * @param containingClass The class containing the collections.
     * @return true if collections are equal, false otherwise.
     */
    private static boolean compareOrderedCollection(Collection<?> col1, Collection<?> col2,
                                                    Deque<ItemsToCompare> stack,
                                                    Set<ItemsToCompare> visited,
                                                    Class<?> containingClass) {
        if (col1.size() != col2.size()) {
            return false;
        }

        Iterator<?> i1 = col1.iterator();
        Iterator<?> i2 = col2.iterator();
        int index = 0;

        while (i1.hasNext()) {
            Object item1 = i1.next();
            Object item2 = i2.next();

            // Make sure we're using the array index constructor
            ItemsToCompare dk = new ItemsToCompare(
                    item1,
                    item2,
                    new int[]{index++},
                    containingClass
            );

            if (!visited.contains(dk)) {
                stack.addFirst(dk);
            }
        }
        return true;
    }
    
    /**
     * Compares two unordered collections (e.g., Sets) deeply.
     *
     * @param col1           First collection.
     * @param col2           Second collection.
     * @param containingClass The class containing the collections.
     * @return true if collections are equal, false otherwise.
     */
    private static boolean compareUnorderedCollection(Collection<?> col1, Collection<?> col2,
                                                      Class<?> containingClass) {
        if (col1.size() != col2.size()) {
            return false;
        }

        // Group col2 items by hash
        Map<Integer, List<Object>> hashGroups = new HashMap<>();
        for (Object o : col2) {
            int hash = deepHashCode(o);
            hashGroups.computeIfAbsent(hash, k -> new ArrayList<>()).add(o);
        }

        // For each item in col1
        outer: for (Object item1 : col1) {
            int hash1 = deepHashCode(item1);
            List<Object> candidates = hashGroups.get(hash1);

            if (candidates == null || candidates.isEmpty()) {
                return false;  // No items with matching hash
            }

            // Try each candidate with matching hash
            for (int i = 0; i < candidates.size(); i++) {
                Object item2 = candidates.get(i);

                if (deepEquals(item1, item2, new LinkedList<>(), new HashMap<>(), new HashSet<>(), containingClass)) {
                    candidates.remove(i);  // Remove matched item
                    if (candidates.isEmpty()) {
                        hashGroups.remove(hash1);
                    }
                    continue outer;
                }
            }
            return false;  // No match found among hash candidates
        }

        return true;
    }
    
    /**
     * Compares two maps deeply.
     *
     * @param map1           First map.
     * @param map2           Second map.
     * @param stack          Comparison stack.
     * @param visited        Set of visited ItemsToCompare.
     * @param options        Comparison options.
     * @param containingClass The class containing the maps.
     * @return true if maps are equal, false otherwise.
     */
    private static boolean compareMap(Map<?, ?> map1, Map<?, ?> map2,
                                      Deque<ItemsToCompare> stack,
                                      Set<ItemsToCompare> visited,
                                      Map<String, ?> options,
                                      Class<?> containingClass) {
        if (map1.size() != map2.size()) {
            return false;
        }

        Map<Integer, Collection<Map.Entry<?, ?>>> fastLookup = new HashMap<>();

        // Build lookup of map2 entries
        for (Map.Entry<?, ?> entry : map2.entrySet()) {
            int hash = deepHashCode(entry.getKey());
            fastLookup.computeIfAbsent(hash, k -> new ArrayList<>())
                    .add(new AbstractMap.SimpleEntry<>(entry.getKey(), entry.getValue()));
        }

        for (Map.Entry<?, ?> entry : map1.entrySet()) {
            Collection<Map.Entry<?, ?>> otherEntries = fastLookup.get(deepHashCode(entry.getKey()));
            if (otherEntries == null || otherEntries.isEmpty()) {
                // Key not found in other Map
                return false;
            }

            if (otherEntries.size() == 1) {
                // No hash collision, direct comparison
                Map.Entry<?, ?> entry2 = otherEntries.iterator().next();

                // Compare keys
                ItemsToCompare keyCompare = new ItemsToCompare(
                        entry.getKey(),
                        entry2.getKey(),
                        formatValue(entry.getKey()),
                        containingClass,
                        true // isMapKey
                );
                if (!visited.contains(keyCompare)) {
                    stack.addFirst(keyCompare);
                }

                // Compare values
                ItemsToCompare valueCompare = new ItemsToCompare(
                        entry.getValue(),
                        entry2.getValue(),
                        formatValue(entry.getKey()),
                        containingClass,
                        false // isMapValue
                );
                if (!visited.contains(valueCompare)) {
                    stack.addFirst(valueCompare);
                }
            } else {
                // Handle hash collision
                if (!isContainedInMapEntries(entry, otherEntries, containingClass)) {
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * Checks if a map entry is contained within other map entries using deep equality.
     *
     * @param entry           The map entry to find.
     * @param otherEntries    The collection of other map entries to search within.
     * @param containingClass The class containing the map.
     * @return true if contained, false otherwise.
     */
    private static boolean isContainedInMapEntries(Map.Entry<?, ?> entry,
                                                   Collection<?> otherEntries,
                                                   Class<?> containingClass) {
        Iterator<?> i = otherEntries.iterator();
        while (i.hasNext()) {
            Map.Entry<?, ?> otherEntry = (Map.Entry<?, ?>) i.next();

            // Create temporary stacks for key and value comparison
            Deque<ItemsToCompare> tempStack = new LinkedList<>();
            Set<ItemsToCompare> tempVisited = new HashSet<>();

            // Compare both key and value with containing class context
            if (deepEquals(entry.getKey(), otherEntry.getKey(), tempStack,
                    new HashMap<>(), tempVisited, containingClass) &&
                    deepEquals(entry.getValue(), otherEntry.getValue(), tempStack,
                            new HashMap<>(), tempVisited, containingClass)) {
                i.remove();
                return true;
            }
        }
        return false;
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
    public enum DifferenceType {
        VALUE_MISMATCH,
        TYPE_MISMATCH,
        NULL_CHECK,
        SIZE_MISMATCH,
        CYCLE
    }

    // Class to build and format the difference output
    static class DifferenceBuilder {
        private final DifferenceType type;
        private final StringBuilder pathBuilder = new StringBuilder();
        private final Object expected;
        private final Object found;
        private String containerType;
        private Integer expectedSize;
        private Integer foundSize;
        private String currentClassName = null;
        private int indentLevel = 0;
        private int diffIndex = -1;  // Add field to track array difference index


        DifferenceBuilder(DifferenceType type, Object expected, Object found) {
            this.type = type;
            this.expected = expected;
            this.found = found;
        }

        DifferenceBuilder withContainerInfo(String containerType, int expectedSize, int foundSize) {
            this.containerType = containerType;
            this.expectedSize = expectedSize;
            this.foundSize = foundSize;
            return this;
        }

        // Add setter for diff index
        public DifferenceBuilder withDiffIndex(int index) {
            this.diffIndex = index;
            return this;
        }

        private void indent() {
            indentLevel += 2;
        }

        private void unindent() {
            indentLevel = Math.max(0, indentLevel - 2);
        }

        private String getIndent() {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < indentLevel; i++) {
                sb.append(' ');
            }
            return sb.toString();
        }

        private String getTypeName(Object obj) {
            if (obj == null) return "null";
            return obj.getClass().getSimpleName();
        }

        public void appendField(String className, String fieldName, Object fieldValue, int[] arrayIndices) {
            if (pathBuilder.length() > 0) {
                pathBuilder.append("\n");
            }

            // If we're switching to a new class, unindent the previous class context
            if (currentClassName != null && !Objects.equals(className, currentClassName)) {
                unindent();
            }

            // Start new class context if needed
            if (!Objects.equals(className, currentClassName)) {
                pathBuilder.append(getIndent()).append(className).append("\n");
                currentClassName = className;
                indent();
            }

            if (fieldValue != null && fieldValue.getClass().isArray()) {
                // Show array context
                if (arrayIndices != null && arrayIndices.length > 0) {
                    pathBuilder.append(getIndent());
                    if (fieldName != null) {
                        pathBuilder.append(".").append(fieldName);
                    }

                    // Show array type
                    pathBuilder.append(" (")
                            .append(fieldValue.getClass().getComponentType().getSimpleName())
                            .append("[])\n");

                    // Show array element with index
                    pathBuilder.append(getIndent())
                            .append("  [")
                            .append(arrayIndices[0])
                            .append("]: ")
                            .append(formatValue(Array.get(fieldValue, arrayIndices[0])));
                } else {
                    // If no specific index, show array type
                    pathBuilder.append(getIndent());
                    if (fieldName != null) {
                        pathBuilder.append(".").append(fieldName);
                    }
                    pathBuilder.append("(")
                            .append(fieldValue.getClass().getComponentType().getSimpleName())
                            .append("[])");
                }
            } else {
                // Handle non-array fields
                pathBuilder.append(getIndent());
                if (fieldName != null) {
                    pathBuilder.append(".").append(fieldName);
                }
                if (fieldValue != null) {
                    pathBuilder.append("(")
                            .append(formatValue(fieldValue))
                            .append(")");
                }
            }
        }

        private void appendArrayElement(Object array, int index) {
            pathBuilder.append(getIndent())
                    .append("  [")
                    .append(index)
                    .append("]: ")
                    .append(formatValue(Array.get(array, index)))
                    .append("\n");
        }

        // Append collection access details to the path
        public void appendCollectionAccess(String collectionType, int expectedSize, int foundSize) {
            pathBuilder.append("<")
                    .append(collectionType)
                    .append(" size=")
                    .append(expectedSize)
                    .append("/")
                    .append(foundSize)
                    .append(">");
        }

        // Append map key access to the path
        public void appendMapKey(String key) {
            pathBuilder.append(".key(\"").append(key).append("\")");
        }

        // Append map value access to the path
        public void appendMapValue(String key) {
            pathBuilder.append(".value(\"").append(key).append("\")");
        }

        private boolean isDimensionalityMismatch(Object arr1, Object arr2) {
            if (arr1 == null || arr2 == null) return false;

            Class<?> type1 = arr1.getClass();
            Class<?> type2 = arr2.getClass();

            int dim1 = 0;
            while (type1.isArray()) {
                dim1++;
                type1 = type1.getComponentType();
            }

            int dim2 = 0;
            while (type2.isArray()) {
                dim2++;
                type2 = type2.getComponentType();
            }

            return dim1 != dim2;
        }

        private String formatArrayDimensionality(Object arr) {
            if (arr == null) return "null";

            Class<?> type = arr.getClass();
            StringBuilder dims = new StringBuilder();
            int dimension = 0;

            // Get base type
            while (type.isArray()) {
                dimension++;
                type = type.getComponentType();
            }

            // Build the type with proper dimensions
            dims.append(type.getSimpleName());
            for (int i = 0; i < dimension; i++) {
                dims.append("[]");
            }

            dims.append(" (").append(dimension).append("D)");
            return dims.toString();
        }

        private String formatArrayComponentType(Object arr) {
            if (arr == null) return "null";

            Class<?> type = arr.getClass();
            StringBuilder result = new StringBuilder();
            List<Class<?>> types = new ArrayList<>();

            // Collect all component types
            while (type.isArray()) {
                types.add(type.getComponentType());
                type = type.getComponentType();
            }

            // Build the nested type representation
            for (int i = 0; i < types.size(); i++) {
                if (i == 0) {
                    result.append(types.get(i).getSimpleName());
                } else {
                    result.append("[").append(types.get(i).getSimpleName()).append("]");
                }
            }

            return result.toString();
        }

        private String formatArrayLengths(Object arr) {
            if (arr == null) return "null";

            Class<?> type = arr.getClass();
            StringBuilder result = new StringBuilder();
            List<Integer> lengths = new ArrayList<>();
            Object current = arr;

            // Collect lengths at each dimension
            while (type.isArray()) {
                lengths.add(Array.getLength(current));
                if (Array.getLength(current) > 0) {
                    current = Array.get(current, 0);
                    if (current != null) {
                        type = current.getClass();
                    } else {
                        break;
                    }
                } else {
                    break;
                }
            }

            // Build base type
            type = arr.getClass();
            while (type.isArray()) {
                type = type.getComponentType();
            }
            result.append(type.getSimpleName());

            // Add dimensions with lengths
            for (Integer length : lengths) {
                result.append("[").append(length).append("]");
            }

            return result.toString();
        }
        
        private String formatArrayType(Object array) {
            if (array == null) return "null";
            Class<?> arrayClass = array.getClass();
            StringBuilder sb = new StringBuilder();
            while (arrayClass.isArray()) {
                sb.append(arrayClass.getComponentType().getSimpleName());
                sb.append("[]");
                arrayClass = arrayClass.getComponentType();
            }
            return sb.toString();
        }

        private String formatArrayValue(Object array) {
            if (array == null) return "null";

            Class<?> componentType = array.getClass().getComponentType();
            int length = Array.getLength(array);
            StringBuilder sb = new StringBuilder();
            sb.append("[");

            for (int i = 0; i < length; i++) {
                if (i > 0) sb.append(", ");
                Object element = Array.get(array, i);

                if (componentType.isPrimitive()) {
                    sb.append(element);
                } else {
                    sb.append(formatValue(element));
                }
            }

            sb.append("]");
            return sb.toString();
        }

        private String formatArrayDifference(Object array1, Object array2, int diffIndex) {
            if (diffIndex < 0) {
                return "Arrays differ but index unknown\n" +
                        "Expected: " + formatArrayValue(array1) + "\n" +
                        "Found: " + formatArrayValue(array2);
            }

            StringBuilder sb = new StringBuilder();
            int len1 = Array.getLength(array1);
            int len2 = Array.getLength(array2);
            String componentTypeName = array1.getClass().getComponentType().getSimpleName();

            sb.append("Array Type: ").append(componentTypeName).append("\n");

            // Show context: one before, difference, one after
            if (diffIndex > 0) {
                sb.append("  [").append(diffIndex - 1).append("]: ")
                        .append(formatValue(Array.get(array1, diffIndex - 1))).append("\n");
            }

            // Show difference
            sb.append("  [").append(diffIndex).append("]: ")
                    .append(formatValue(Array.get(array1, diffIndex)))
                    .append(" ≠ ")
                    .append(formatValue(Array.get(array2, diffIndex))).append("\n");

            if (diffIndex < len1 - 1 && diffIndex < len2 - 1) {
                sb.append("  [").append(diffIndex + 1).append("]: ")
                        .append(formatValue(Array.get(array1, diffIndex + 1)));
            }

            // Add complete arrays at the end
            sb.append("\nComplete arrays:\n");
            sb.append("Expected: ").append(formatArrayValue(array1)).append("\n");
            sb.append("Found: ").append(formatArrayValue(array2));

            return sb.toString();
        }

        @Override
        public String toString() {
            StringBuilder result = new StringBuilder();
            result.append("Difference Type: ").append(type).append("\n");

            // If we have a container type mismatch
            if (type == DifferenceType.TYPE_MISMATCH &&
                    found != null && expected != null &&
                    (found instanceof Collection || found instanceof Map ||
                            expected instanceof Collection || expected instanceof Map)) {

                result.append("Container Type Mismatch\n");
                result.append("  Found: ").append(found.getClass().getSimpleName()).append("\n");
                result.append("  Expected: ").append(expected.getClass().getSimpleName());
                return result.toString();
            }

            // Handle array-specific differences
            if (expected != null && expected.getClass().isArray() ||
                    found != null && found.getClass().isArray()) {

                // Add the path/trail information without the "Path:" label
                result.append(pathBuilder.toString().trim()).append("\n");

                switch (type) {
                    case TYPE_MISMATCH:
                        if (isDimensionalityMismatch(expected, found)) {
                            result.append("Array Dimensionality Mismatch\n");
                            result.append("Expected: ").append(formatArrayDimensionality(expected)).append("\n");
                            result.append("Found: ").append(formatArrayDimensionality(found));
                        } else {
                            result.append("Array Component Type Mismatch\n");
                            result.append("Expected: ").append(formatArrayComponentType(expected)).append("\n");
                            result.append("Found: ").append(formatArrayComponentType(found));
                        }
                        break;

                    case SIZE_MISMATCH:
                        result.append("Array Length Mismatch\n");
                        result.append("Expected: ").append(formatArrayLengths(expected)).append("\n");
                        result.append("Found: ").append(formatArrayLengths(found));
                        break;

                    case VALUE_MISMATCH:
                        // The path builder will have already built the array indices
                        result.append("Expected: ").append(formatValue(expected)).append("\n");
                        result.append("Found: ").append(formatValue(found));
                        break;
                }
                return result.toString();
            }

            // Regular (non-array) differences
            String path = pathBuilder.toString().trim();
            if (!path.isEmpty()) {
                result.append(path).append("\n");
            }

            switch (type) {
                case SIZE_MISMATCH:
                    result.append("Container Type: ").append(containerType).append("\n");
                    result.append("Expected Size: ").append(expectedSize).append("\n");
                    result.append("Found Size: ").append(foundSize);
                    break;
                case VALUE_MISMATCH:
                case NULL_CHECK:
                    result.append("Expected: ").append(formatValue(expected)).append("\n");
                    result.append("Found: ").append(formatValue(found));
                    break;
                case TYPE_MISMATCH:
                    result.append("Expected Type: ").append(getTypeName(expected)).append("\n");
                    result.append("Found Type: ").append(getTypeName(found));
                    break;
            }

            return result.toString();
        }
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

        return value.getClass().getSimpleName() + "#" +
                Integer.toHexString(System.identityHashCode(value));
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
}