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
    private static enum ContainerAccessType {
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
        private final String fieldName;
        private final Integer arrayIndex;
        private final String mapKey;
        private final ContainerAccessType accessType;
        private final Class<?> containingClass;

        // Constructor for field access
        private ItemsToCompare(Object k1, Object k2, String fieldName, Class<?> containingClass) {
            _key1 = k1;
            _key2 = k2;
            this.fieldName = fieldName;
            this.arrayIndex = null;
            this.mapKey = null;
            this.accessType = ContainerAccessType.FIELD;
            this.containingClass = containingClass;
        }

        // Constructor for array index access
        private ItemsToCompare(Object k1, Object k2, Integer arrayIndex, Class<?> containingClass) {
            _key1 = k1;
            _key2 = k2;
            this.fieldName = null;
            this.arrayIndex = arrayIndex;
            this.mapKey = null;
            this.accessType = ContainerAccessType.ARRAY_INDEX;
            this.containingClass = containingClass;
        }

        // Constructor for map key access
        private ItemsToCompare(Object k1, Object k2, String mapKey, Class<?> containingClass, boolean isMapKey) {
            _key1 = k1;
            _key2 = k2;
            this.fieldName = null;
            this.arrayIndex = null;
            this.mapKey = mapKey;
            this.accessType = isMapKey ? ContainerAccessType.MAP_KEY : ContainerAccessType.MAP_VALUE;
            this.containingClass = containingClass;
        }

        // Constructor for collection access (if needed)
        private ItemsToCompare(Object k1, Object k2, Class<?> containingClass) {
            _key1 = k1;
            _key2 = k2;
            this.fieldName = null;
            this.arrayIndex = null;
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

            // Must have same access type
            if (this.accessType != that.accessType) {
                return false;
            }

            // Must have same containing class
            if (!Objects.equals(containingClass, that.containingClass)) {
                return false;
            }

            // Compare based on access type and context
            switch (accessType) {
                case FIELD:
                    // Field access must have same field name
                    return Objects.equals(fieldName, that.fieldName);

                case ARRAY_INDEX:
                    // Array/List access must have same index
                    return Objects.equals(arrayIndex, that.arrayIndex);

                case MAP_KEY:
                case MAP_VALUE:
                    // Map access must have same key
                    return Objects.equals(mapKey, that.mapKey);

                case COLLECTION:
                    // Collection access with no index
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
                    result = 31 * result + (arrayIndex != null ? arrayIndex.hashCode() : 0);
                    break;
                case MAP_KEY:
                case MAP_VALUE:
                    result = 31 * result + (mapKey != null ? mapKey.hashCode() : 0);
                    break;
            }
            return result;
        }
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

        public void appendField(String className, String fieldName, Object fieldValue) {
            if (pathBuilder.length() > 0) {
                pathBuilder.append("\n");
            }

            // Add debug info for empty paths
            if (className == null && fieldName == null) {
                pathBuilder.append("DEBUG: Empty path detected");
                return;
            }

            // Start new class context if needed
            if (!Objects.equals(className, currentClassName)) {
                pathBuilder.append(getIndent()).append(className).append("\n");
                currentClassName = className;
                indent();
            }

            // Add field information
            if (fieldName != null) {
                pathBuilder.append(getIndent())
                        .append(".")
                        .append(fieldName);

                if (fieldValue != null) {
                    pathBuilder.append("(")
                            .append(formatValue(fieldValue))
                            .append(")");
                }
            }
        }
        
        // Append array index to the path
        public void appendArrayIndex(int index) {
            pathBuilder.append("[").append(index).append("]");
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

        public void appendContainerMismatch(Class<?> foundClass, Class<?> expectedClass) {
            // Clear any existing path info for container mismatches
            pathBuilder.setLength(0);

            pathBuilder.append("Container Type Mismatch\n")
                    .append(getIndent())
                    .append("Found Container: ")
                    .append(foundClass.getSimpleName())
                    .append("\n")
                    .append(getIndent())
                    .append("Expected Container: ")
                    .append(expectedClass.getSimpleName());
        }
        
        private String getTypeName(Object obj) {
            if (obj == null) return "null";
            return obj.getClass().getSimpleName();
        }

        private String formatValue(Object value) {
            if (value == null) return "null";
            if (value instanceof String) return "\"" + value + "\"";
            if (value instanceof Date) {
                return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format((Date)value);
            }
            if (value.getClass().getName().startsWith("com.cedarsoftware")) {
                return String.format("%s#%s",
                        value.getClass().getSimpleName(),
                        Integer.toHexString(System.identityHashCode(value)));
            }
            return String.valueOf(value);
        }

        @Override
        public String toString() {
            StringBuilder result = new StringBuilder();
            result.append("Difference Type: ").append(type).append("\n");
            result.append("Path:\n");

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

            // Regular path handling
            result.append(pathBuilder.toString().trim());

            switch (type) {
                case SIZE_MISMATCH:
                    result.append("\nContainer Type: ").append(containerType)
                            .append("\nExpected Size: ").append(expectedSize)
                            .append("\nFound Size: ").append(foundSize);
                    break;
                case VALUE_MISMATCH:
                case NULL_CHECK:
                    result.append("\nExpected: ").append(formatValue(expected))
                            .append("\nFound: ").append(formatValue(found));
                    break;
                case TYPE_MISMATCH:
                    result.append("\nExpected Type: ").append(getTypeName(expected))
                            .append("\nFound Type: ").append(getTypeName(found));
                    break;
                case CYCLE:
                    result.append("\nExpected: ").append(formatValue(expected))
                            .append("\nFound: ").append(formatValue(found));
                    break;
            }

            return result.toString();
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
                            item._key1
                    );
                    break;
                case ARRAY_INDEX:
                    builder.appendArrayIndex(item.arrayIndex);
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
                    // Handle other types if necessary
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
     * Formats a value for display in the breadcrumb.
     *
     * @param value The value to format.
     * @return A formatted string representation of the value.
     */
    private static String formatValue(Object value) {
        if (value == null) return "null";
        if (value instanceof String) return "\"" + value + "\"";
        if (value instanceof Number) {
            if (value instanceof Float || value instanceof Double) {
                return String.format("%.10g", value); // Use scientific notation for floats
            } else {
                return String.valueOf(value);
            }
        }
        if (value instanceof Date) {
            return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format((Date)value);
        }
        if (value.getClass().getName().startsWith("com.cedarsoftware")) {
            return value.getClass().getSimpleName() + "#" +
                    Integer.toHexString(System.identityHashCode(value));
        }
        return String.valueOf(value);
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
                    i,
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
                    Integer.valueOf(index++),  // Explicitly use Integer constructor
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
}