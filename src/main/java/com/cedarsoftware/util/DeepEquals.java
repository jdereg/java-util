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

/**
 * Test two objects for equivalence with a 'deep' comparison.  This will traverse
 * the Object graph and perform either a field-by-field comparison on each
 * object (if no .equals() method has been overridden from Object), or it
 * will call the customized .equals() method if it exists.  This method will
 * allow object graphs loaded at different times (with different object ids)
 * to be reliably compared.  Object.equals() / Object.hashCode() rely on the
 * object's identity, which would not consider two equivalent objects necessarily
 * equals.  This allows graphs containing instances of Classes that did not
 * overide .equals() / .hashCode() to be compared.  For example, testing for
 * existence in a cache.  Relying on an object's identity will not locate an
 * equivalent object in a cache.<br><br>
 * <p>
 * This method will handle cycles correctly, for example A-&gt;B-&gt;C-&gt;A.  Suppose a and
 * a' are two separate instances of A with the same values for all fields on
 * A, B, and C.  Then a.deepEquals(a') will return true.  It uses cycle detection
 * storing visited objects in a Set to prevent endless loops.<br><br>
 * <p>
 * Numbers will be compared for value.  Meaning an int that has the same value
 * as a long will match.  Similarly, a double that has the same value as a long
 * will match.  If the flag "ALLOW_STRING_TO_MATCH_NUMBERS" is passed in the options
 * are set to true, then Strings will be converted to BigDecimal and compared to
 * the corresponding non-String Number.  Two Strings will not be compared as numbers,
 * however.
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
    private DeepEquals() {
    }

    public static final String IGNORE_CUSTOM_EQUALS = "ignoreCustomEquals";
    public static final String ALLOW_STRINGS_TO_MATCH_NUMBERS = "stringsCanMatchNumbers";
    private static final Map<String, Boolean> _customEquals = new ConcurrentHashMap<>();
    private static final Map<String, Boolean> _customHash = new ConcurrentHashMap<>();
    private static final double doubleEplison = 1e-15;
    private static final double floatEplison = 1e-6;
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

    private final static class ItemsToCompare {
        private final Object _key1;
        private final Object _key2;
        private final String fieldName;
        private final Integer arrayIndex;
        private final Class<?> containingClass;
        
        private ItemsToCompare(Object k1, Object k2, Class<?> containingClass) {
            _key1 = k1;
            _key2 = k2;
            fieldName = null;
            arrayIndex = null;
            this.containingClass = containingClass;
        }

        private ItemsToCompare(Object k1, Object k2, String fieldName, Class<?> containingClass) {
            _key1 = k1;
            _key2 = k2;
            this.fieldName = fieldName;
            this.arrayIndex = null;
            this.containingClass = containingClass;
        }

        private ItemsToCompare(Object k1, Object k2, Integer arrayIndex, Class<?> containingClass) {
            _key1 = k1;
            _key2 = k2;
            this.fieldName = null;
            this.arrayIndex = arrayIndex;
            this.containingClass = containingClass;
        }

        public boolean equals(Object other) {
            if (!(other instanceof ItemsToCompare)) {
                return false;
            }
            ItemsToCompare that = (ItemsToCompare) other;
            return _key1 == that._key1 && _key2 == that._key2 &&
                    Objects.equals(containingClass, that.containingClass);
        }

        public int hashCode() {
            int h1 = _key1 != null ? _key1.hashCode() : 0;
            int h2 = _key2 != null ? _key2.hashCode() : 0;
            int h3 = containingClass != null ? containingClass.hashCode() : 0;
            return h1 + h2 + h3;
        }

//        public String toString() {
//            if (_key1.getClass().isPrimitive() && _key2.getClass().isPrimitive()) {
//                return _key1 + " | " + _key2;
//            }
//            return _key1.getClass().getName() + " | " + _key2.getClass().getName();
//        }
    }

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

        public void appendToPath(String className, String fieldName, Object fieldValue) {
            if (pathBuilder.length() > 0) {
                pathBuilder.append("\n");
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
                            .append(" <")
                            .append(getTypeName(fieldValue))
                            .append(">)");
                }
            }
        }

        private boolean isComplexObject(Object obj) {
            if (obj == null) return false;
            return !obj.getClass().isPrimitive()
                    && !obj.getClass().getName().startsWith("java.lang")
                    && !(obj instanceof Number)
                    && !(obj instanceof String)
                    && !(obj instanceof Date);
        }

        public void appendArrayIndex(int index) {
            pathBuilder.append("[").append(index).append("]");
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
            result.append("Path:\n").append(pathBuilder.toString().trim());

            switch (type) {
                case SIZE_MISMATCH:
                    result.append("\n  <").append(containerType)
                            .append(" size ").append(expectedSize)
                            .append(" vs ").append(foundSize).append(">");
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
            }

            return result.toString();
        }
    }
    
    // Modify your generateBreadcrumb method to use the new formatting:
    private static String generateBreadcrumb(Deque<ItemsToCompare> stack) {
        DifferenceBuilder builder = null;
        String currentClassName = null;
        Iterator<ItemsToCompare> it = stack.descendingIterator();

        // Get the root item to determine the type of difference
        ItemsToCompare rootItem = stack.peekLast();
        if (rootItem != null) {
            if (rootItem._key1 == null || rootItem._key2 == null) {
                builder = new DifferenceBuilder(DifferenceType.NULL_CHECK, rootItem._key2, rootItem._key1);
            } else if (!rootItem._key1.getClass().equals(rootItem._key2.getClass())) {
                builder = new DifferenceBuilder(DifferenceType.TYPE_MISMATCH, rootItem._key2, rootItem._key1);
            } else if (rootItem._key1 instanceof Collection || rootItem._key1 instanceof Map) {
                int size1 = rootItem._key1 instanceof Collection ?
                        ((Collection<?>)rootItem._key1).size() : ((Map<?,?>)rootItem._key1).size();
                int size2 = rootItem._key2 instanceof Collection ?
                        ((Collection<?>)rootItem._key2).size() : ((Map<?,?>)rootItem._key2).size();
                if (size1 != size2) {
                    builder = new DifferenceBuilder(DifferenceType.SIZE_MISMATCH, rootItem._key2, rootItem._key1)
                            .withContainerInfo(rootItem._key1.getClass().getSimpleName(), size2, size1);
                }
            } else {
                builder = new DifferenceBuilder(DifferenceType.VALUE_MISMATCH, rootItem._key2, rootItem._key1);
            }
        }

        if (builder == null) {
            return "Unable to determine difference type";
        }

        while (it.hasNext()) {
            ItemsToCompare item = it.next();

            // Get the containing class from the stack context
            Class<?> containingClass = determineContainingClass(item, stack);

            // Use getSimpleName() to get the String class name
            String className = containingClass != null ? containingClass.getSimpleName() : null;

            builder.appendToPath(className, item.fieldName, item._key1);

            if (item.arrayIndex != null) {
                builder.appendArrayIndex(item.arrayIndex);
            }
        }
        
        return builder.toString();
    }

    private static Class<?> determineContainingClass(ItemsToCompare item, Deque<ItemsToCompare> stack) {
        // This method would need to be implemented to determine the actual containing class
        // It might need to look at the previous items in the stack or might need additional
        // context stored in ItemsToCompare

        // For now, this is a placeholder
        return item.containingClass; // We'd need to add this field to ItemsToCompare
    }
    
    /**
     * Compare two objects with a 'deep' comparison.  This will traverse the
     * Object graph and perform either a field-by-field comparison on each
     * object (if not .equals() method has been overridden from Object), or it
     * will call the customized .equals() method if it exists.  This method will
     * allow object graphs loaded at different times (with different object ids)
     * to be reliably compared.  Object.equals() / Object.hashCode() rely on the
     * object's identity, which would not consider to equivalent objects necessarily
     * equals.  This allows graphs containing instances of Classes that did no
     * overide .equals() / .hashCode() to be compared.  For example, testing for
     * existence in a cache.  Relying on an objects identity will not locate an
     * object in cache, yet relying on it being equivalent will.<br><br>
     * <p>
     * This method will handle cycles correctly, for example A-&gt;B-&gt;C-&gt;A.  Suppose a and
     * a' are two separate instances of the A with the same values for all fields on
     * A, B, and C.  Then a.deepEquals(a') will return true.  It uses cycle detection
     * storing visited objects in a Set to prevent endless loops.
     *
     * @param a Object one to compare
     * @param b Object two to compare
     * @return true if a is equivalent to b, false otherwise.  Equivalent means that
     * all field values of both subgraphs are the same, either at the field level
     * or via the respectively encountered overridden .equals() methods during
     * traversal.
     */
    public static boolean deepEquals(Object a, Object b) {
        return deepEquals(a, b, new HashMap<>());
    }

    /**
     * Compare two objects with a 'deep' comparison.  This will traverse the
     * Object graph and perform either a field-by-field comparison on each
     * object (if not .equals() method has been overridden from Object), or it
     * will call the customized .equals() method if it exists.  This method will
     * allow object graphs loaded at different times (with different object ids)
     * to be reliably compared.  Object.equals() / Object.hashCode() rely on the
     * object's identity, which would not consider to equivalent objects necessarily
     * equals.  This allows graphs containing instances of Classes that did no
     * overide .equals() / .hashCode() to be compared.  For example, testing for
     * existence in a cache.  Relying on an objects identity will not locate an
     * object in cache, yet relying on it being equivalent will.<br><br>
     * <p>
     * This method will handle cycles correctly, for example A-&gt;B-&gt;C-&gt;A.  Suppose a and
     * a' are two separate instances of the A with the same values for all fields on
     * A, B, and C.  Then a.deepEquals(a') will return true.  It uses cycle detection
     * storing visited objects in a Set to prevent endless loops.
     *
     * @param a       Object one to compare
     * @param b       Object two to compare
     * @param options Map options for compare. With no option, if a custom equals()
     *                method is present, it will be used.  If IGNORE_CUSTOM_EQUALS is
     *                present, it will be expected to be a Set of classes to ignore.
     *                It is a black-list of classes that will not be compared
     *                using .equals() even if the classes have a custom .equals() method
     *                present.  If it is and empty set, then no custom .equals() methods
     *                will be called.
     * @return true if a is equivalent to b, false otherwise.  Equivalent means that
     * all field values of both subgraphs are the same, either at the field level
     * or via the respectively encountered overridden .equals() methods during
     * traversal.
     */
    public static boolean deepEquals(Object a, Object b, Map<String, ?> options) {
        Set<ItemsToCompare> visited = new HashSet<>();
        Deque<ItemsToCompare> stack = new LinkedList<>();
        Class<?> rootClass = a != null ? a.getClass() : (b != null ? b.getClass() : null);
        boolean result = deepEquals(a, b, stack, options, visited, rootClass);
        
        if (!result && !stack.isEmpty()) {
            String breadcrumb = generateBreadcrumb(stack);
            System.out.println(breadcrumb);
            ((Map<String, Object>)options).put("diff", breadcrumb);
        }

        return result;
    }

    private static boolean deepEquals(Object a, Object b, Deque<ItemsToCompare> stack,
                                      Map<String, ?> options, Set<ItemsToCompare> visited, Class<?> containingClass) {
        Set<Class<?>> ignoreCustomEquals = (Set<Class<?>>) options.get(IGNORE_CUSTOM_EQUALS);
        final boolean allowStringsToMatchNumbers = convert2boolean(options.get(ALLOW_STRINGS_TO_MATCH_NUMBERS));

        stack.addFirst(new ItemsToCompare(a, b, containingClass));

        while (!stack.isEmpty()) {
            ItemsToCompare itemsToCompare = stack.removeFirst();
            visited.add(itemsToCompare);

            final Object key1 = itemsToCompare._key1;
            final Object key2 = itemsToCompare._key2;
            if (key1 == key2) {   // Same instance is always equal to itself.
                continue;
            }

            if (key1 == null || key2 == null) {   // If either one is null, they are not equal (both can't be null, due to above comparison).
                stack.addFirst(itemsToCompare);
                return false;
            }

            // Handle all numeric comparisons first
            if (key1 instanceof Number && key2 instanceof Number) {
                if (!compareNumbers((Number) key1, (Number) key2)) {
                    stack.addFirst(itemsToCompare);
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
                stack.addFirst(itemsToCompare);
                return false;
            }

            if (key1 instanceof AtomicBoolean && key2 instanceof AtomicBoolean) {
                if (!compareAtomicBoolean((AtomicBoolean) key1, (AtomicBoolean) key2)) {
                    stack.addFirst(itemsToCompare);
                    return false;
                } else {
                    continue;
                }
            }

            Class<?> key1Class = key1.getClass();

            if (key1Class.isPrimitive() || prims.contains(key1Class) || key1 instanceof String || key1 instanceof Date || key1 instanceof Class) {
                if (!key1.equals(key2)) {
                    stack.addFirst(itemsToCompare);
                    return false;
                }
                continue;   // Nothing further to push on the stack
            }

            if (key1 instanceof Set) {
                if (!(key2 instanceof Set)) {
                    stack.addFirst(itemsToCompare);
                    return false;
                }
            } else if (key2 instanceof Set) {
                stack.addFirst(itemsToCompare);
                return false;
            }

            if (key1 instanceof Collection) {   // If Collections, they both must be Collection
                if (!(key2 instanceof Collection)) {
                    stack.addFirst(itemsToCompare);
                    return false;
                }
            } else if (key2 instanceof Collection) {
                stack.addFirst(itemsToCompare);
                return false;
            }

            if (key1 instanceof Map) {
                if (!(key2 instanceof Map)) {
                    stack.addFirst(itemsToCompare);
                    return false;
                }
            } else if (key2 instanceof Map) {
                stack.addFirst(itemsToCompare);
                return false;
            }

            Class<?> key2Class = key2.getClass();
            if (key1Class.isArray()) {
                if (!key2Class.isArray()) {
                    stack.addFirst(itemsToCompare);
                    return false;
                }
            } else if (key2Class.isArray()) {
                stack.addFirst(itemsToCompare);
                return false;
            }

            if (!isContainerType(key1) && !isContainerType(key2) && !key1Class.equals(key2.getClass())) {   // Must be same class
                stack.addFirst(itemsToCompare);
                return false;
            }

            // Special handle Sets - items matter but order does not for equality.
            if (key1 instanceof Set<?>) {
                if (!compareUnorderedCollection((Collection<?>) key1, (Collection<?>) key2, stack, visited, key1Class)) {
                    stack.addFirst(itemsToCompare);
                    return false;
                }
                continue;
            }

            // Collections must match in items and order for equality.
            if (key1 instanceof Collection<?>) {
                if (!compareOrderedCollection((Collection<?>) key1, (Collection<?>) key2, stack, visited, key1Class)) {
                    stack.addFirst(itemsToCompare);
                    return false;
                }
                continue;
            }

            // Compare two Maps. This is a slightly more expensive comparison because
            // order cannot be assumed, therefore a temporary Map must be created, however the
            // comparison still runs in O(N) time.
            if (key1 instanceof Map) {
                if (!compareMap((Map<?, ?>) key1, (Map<?, ?>) key2, stack, visited, options, key1Class)) {
                    stack.addFirst(itemsToCompare);
                    return false;
                }
                continue;
            }

            // Handle all [] types.  In order to be equal, the arrays must be the same
            // length, be of the same type, be in the same order, and all elements within
            // the array must be deeply equivalent.
            if (key1Class.isArray()) {
                if (!compareArrays(key1, key2, stack, visited, key1Class)) {
                    stack.addFirst(itemsToCompare);
                    return false;
                }
                continue;
            }

            // If there is a custom equals ... AND
            // the caller has not specified any classes to skip ... OR
            // the caller has specified come classes to ignore and this one is not in the list ... THEN
            // compare using the custom equals.
            if (hasCustomEquals(key1Class)) {
                if (ignoreCustomEquals == null || (ignoreCustomEquals.size() > 0 && !ignoreCustomEquals.contains(key1Class))) {
                    if (!key1.equals(key2)) {
                        stack.addFirst(itemsToCompare);
                        return false;
                    }
                    continue;
                }
            }

            Collection<Field> fields = ReflectionUtils.getAllDeclaredFields(key1Class);

            for (Field field : fields) {
                try {
                    ItemsToCompare dk = new ItemsToCompare(field.get(key1), field.get(key2), field.getName(), key1Class);
                    if (!visited.contains(dk)) {
                        stack.addFirst(dk);
                    }
                } catch (Exception ignored) {
                }
            }
        }

        return true;
    }

    public static boolean isContainerType(Object o) {
        return o instanceof Collection || o instanceof Map;
    }

    /**
     * Deeply compare to Arrays []. Both arrays must be of the same type, same length, and all
     * elements within the arrays must be deeply equal in order to return true.
     *
     * @param array1  [] type (Object[], String[], etc.)
     * @param array2  [] type (Object[], String[], etc.)
     * @param stack   add items to compare to the Stack (Stack versus recursion)
     * @param visited Set of objects already compared (prevents cycles)
     * @return true if the two arrays are the same length and contain deeply equivalent items.
     */
    private static boolean compareArrays(Object array1, Object array2, Deque<ItemsToCompare> stack, Set<ItemsToCompare> visited, Class<?> containingClass) {
        final int len = Array.getLength(array1);
        if (len != Array.getLength(array2)) {
            stack.addFirst(new ItemsToCompare(array1, array2, containingClass));  // Push back for breadcrumb
            return false;
        }

        for (int i = 0; i < len; i++) {
            ItemsToCompare dk = new ItemsToCompare(
                    Array.get(array1, i),
                    Array.get(array2, i),
                    i,     // but we do have an index
                    containingClass
            );
            if (!visited.contains(dk)) {
                stack.addFirst(dk);
            }
        }
        return true;
    }
    
    /**
     * Deeply compare two Collections that must be same length and in same order.
     *
     * @param col1    First collection of items to compare
     * @param col2    Second collection of items to compare
     * @param stack   add items to compare to the Stack (Stack versus recursion)
     * @param visited Set of objects already compared (prevents cycles)
     *                value of 'true' indicates that the Collections may be equal, and the sets
     *                items will be added to the Stack for further comparison.
     */
    private static boolean compareOrderedCollection(Collection<?> col1, Collection<?> col2,
                                                    Deque<ItemsToCompare> stack,
                                                    Set<ItemsToCompare> visited,
                                                    Class<?> containingClass) {
        // Same instance check already performed...

        if (col1.size() != col2.size()) {
            stack.addFirst(new ItemsToCompare(col1, col2, containingClass));
            return false;
        }

        Iterator<?> i1 = col1.iterator();
        Iterator<?> i2 = col2.iterator();
        int index = 0;  // Add index tracking for better context

        while (i1.hasNext()) {
            Object item1 = i1.next();
            Object item2 = i2.next();

            // If the items are of the same type and that type matches the containing class,
            // use it as the containing class for the comparison, otherwise use the collection's class
            Class<?> itemContainingClass = (item1 != null && item2 != null &&
                    item1.getClass().equals(item2.getClass()) &&
                    item1.getClass().equals(containingClass))
                    ? containingClass
                    : col1.getClass();

            ItemsToCompare dk = new ItemsToCompare(
                    item1,
                    item2,
                    index++,  // Pass the index for better context in the breadcrumb
                    itemContainingClass
            );

            if (!visited.contains(dk)) {
                stack.addFirst(dk);
            }
        }
        return true;
    }

    /**
     * Deeply compare the two sets referenced by ItemsToCompare.  This method attempts
     * to quickly determine inequality by length, then if lengths match, it
     * places one collection into a temporary Map by deepHashCode(), so that it
     * can walk the other collection and look for each item in the map, which
     * runs in O(N) time, rather than an O(N^2) lookup that would occur if each
     * item from collection one was scanned for in collection two.
     *
     * @param col1    First collection of items to compare
     * @param col2    Second collection of items to compare
     * @param stack   add items to compare to the Stack (Stack versus recursion)
     * @param visited Set containing items that have already been compared, to prevent cycles.
     * @return boolean false if the Collections are for certain not equals. A
     * value of 'true' indicates that the Collections may be equal, and the sets
     * items will be added to the Stack for further comparison.
     */
    private static boolean compareUnorderedCollection(Collection<?> col1, Collection<?> col2,
                                                      Deque<ItemsToCompare> stack,
                                                      Set<ItemsToCompare> visited,
                                                      Class<?> containingClass) {
        // Same instance check already performed...
        if (col1.size() != col2.size()) {
            stack.addFirst(new ItemsToCompare(col1, col2, containingClass));
            return false;
        }

        Map<Integer, Collection<Object>> fastLookup = new HashMap<>();
        for (Object o : col2) {
            int hash = deepHashCode(o);
            fastLookup.computeIfAbsent(hash, k -> new ArrayList<>()).add(o);
        }

        int index = 0;  // Add index tracking for better context
        for (Object o : col1) {
            Collection<?> other = fastLookup.get(deepHashCode(o));
            if (other == null || other.isEmpty()) {
                // Item not found in other Collection
                ItemsToCompare dk = new ItemsToCompare(
                        o,
                        null,
                        index,
                        containingClass
                );
                stack.addFirst(dk);
                return false;
            }

            if (other.size() == 1) {
                // No hash collision, direct comparison
                Object otherObj = other.iterator().next();

                // Determine appropriate containing class for the comparison
                Class<?> itemContainingClass = (o != null && otherObj != null &&
                        o.getClass().equals(otherObj.getClass()) &&
                        o.getClass().equals(containingClass))
                        ? containingClass
                        : col1.getClass();

                ItemsToCompare dk = new ItemsToCompare(
                        o,
                        otherObj,
                        index,
                        itemContainingClass
                );

                if (!visited.contains(dk)) {
                    stack.addFirst(dk);
                }
            } else {
                // Handle hash collision
                if (!isContained(o, other, containingClass)) {
                    ItemsToCompare dk = new ItemsToCompare(
                            o,
                            other,
                            index,
                            containingClass
                    );
                    stack.addFirst(dk);
                    return false;
                }
            }
            index++;
        }
        return true;
    }

    // Modified isContained method to handle containing class context
    private static boolean isContained(Object o, Collection<?> other, Class<?> containingClass) {
        Iterator<?> i = other.iterator();
        while (i.hasNext()) {
            Object x = i.next();

            // Create temporary stack and visited set for deep comparison
            Deque<ItemsToCompare> tempStack = new LinkedList<>();
            Set<ItemsToCompare> tempVisited = new HashSet<>();

            // Use deepEquals with containing class context
            if (deepEquals(o, x, tempStack, new HashMap<>(), tempVisited, containingClass)) {
                i.remove(); // Remove matched item
                return true;
            }
        }
        return false;
    }

    /**
     * Deeply compare two Map instances.  After quick short-circuit tests, this method
     * uses a temporary Map so that this method can run in O(N) time.
     *
     * @param map1    Map one
     * @param map2    Map two
     * @param stack   add items to compare to the Stack (Stack versus recursion)
     * @param visited Set containing items that have already been compared, to prevent cycles.
     * @param options the options for comparison (see {@link #deepEquals(Object, Object, Map)}
     * @return false if the Maps are for certain not equals.  'true' indicates that 'on the surface' the maps
     * are equal, however, it will place the contents of the Maps on the stack for further comparisons.
     */
    private static boolean compareMap(Map<?, ?> map1, Map<?, ?> map2,
                                      Deque<ItemsToCompare> stack,
                                      Set<ItemsToCompare> visited,
                                      Map<String, ?> options,
                                      Class<?> containingClass) {
        // Same instance check already performed...

        if (map1.size() != map2.size()) {
            stack.addFirst(new ItemsToCompare(map1, map2, containingClass));
            return false;
        }

        Map<Integer, Collection<Object>> fastLookup = new HashMap<>();

        // Build lookup of map2 entries
        for (Map.Entry<?, ?> entry : map2.entrySet()) {
            int hash = deepHashCode(entry.getKey());
            Collection<Object> items = fastLookup.computeIfAbsent(hash, k -> new ArrayList<>());

            // Use SimpleEntry to normalize entry type across different Map implementations
            items.add(new AbstractMap.SimpleEntry<>(entry.getKey(), entry.getValue()));
        }

        for (Map.Entry<?, ?> entry : map1.entrySet()) {
            Collection<Object> other = fastLookup.get(deepHashCode(entry.getKey()));
            if (other == null || other.isEmpty()) {
                // Key not found in other Map
                ItemsToCompare dk = new ItemsToCompare(
                        entry,
                        null,
                        "key(" + formatValue(entry.getKey()) + ")",  // Add key context to fieldName
                        containingClass
                );
                stack.addFirst(dk);
                return false;
            }

            if (other.size() == 1) {
                // No hash collision, direct comparison
                Map.Entry<?, ?> entry2 = (Map.Entry<?, ?>) other.iterator().next();

                // Compare keys
                Class<?> keyContainingClass = (entry.getKey() != null && entry2.getKey() != null) ?
                        entry.getKey().getClass() : containingClass;
                ItemsToCompare dk = new ItemsToCompare(
                        entry.getKey(),
                        entry2.getKey(),
                        "key",
                        keyContainingClass
                );
                if (!visited.contains(dk)) {
                    stack.addFirst(dk);
                }

                // Compare values
                Class<?> valueContainingClass = (entry.getValue() != null && entry2.getValue() != null) ?
                        entry.getValue().getClass() : containingClass;
                dk = new ItemsToCompare(
                        entry.getValue(),
                        entry2.getValue(),
                        "value(" + formatValue(entry.getKey()) + ")",  // Include key in context
                        valueContainingClass
                );
                if (!visited.contains(dk)) {
                    stack.addFirst(dk);
                }
            } else {
                // Handle hash collision
                if (!isContainedInMapEntries(entry, other, containingClass)) {
                    ItemsToCompare dk = new ItemsToCompare(
                            entry,
                            other,
                            "entry",
                            containingClass
                    );
                    stack.addFirst(dk);
                    return false;
                }
            }
        }

        return true;
    }

    private static boolean isContainedInMapEntries(Map.Entry<?, ?> entry,
                                                   Collection<?> other,
                                                   Class<?> containingClass) {
        Iterator<?> i = other.iterator();
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

    private static String formatValue(Object value) {
        if (value == null) {
            return "null";
        }
        if (value instanceof String) {
            return "\"" + value + "\"";
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
     * @return true if the passed in o is within the passed in Collection, using a deepEquals comparison
     * element by element.  Used only for hash collisions.
     */
    private static boolean isContained(Object o, Collection<?> other) {
        Iterator<?> i = other.iterator();
        while (i.hasNext()) {
            Object x = i.next();
            if (Objects.equals(o, x)) {
                i.remove(); // can only be used successfully once - remove from list
                return true;
            }
        }
        return false;
    }

    private static boolean compareAtomicBoolean(AtomicBoolean a, AtomicBoolean b) {
        return a.get() == b.get();
    }

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
            return nearlyEqual(d1, d2, doubleEplison);
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
     * Compare if two floating point numbers are within a given range
     */
    private static boolean compareFloatingPointNumbers(Object a, Object b, double epsilon) {
        double a1 = a instanceof Double ? (Double) a : (Float) a;
        double b1 = b instanceof Double ? (Double) b : (Float) b;
        return nearlyEqual(a1, b1, epsilon);
    }

    /**
     * Correctly handles floating point comparisons. <br>
     * source: http://floating-point-gui.de/errors/comparison/
     *
     * @param a       first number
     * @param b       second number
     * @param epsilon double tolerance value
     * @return true if a and b are close enough
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
     * Determine if the passed in class has a non-Object.equals() method.  This
     * method caches its results in static ConcurrentHashMap to benefit
     * execution performance.
     *
     * @param c Class to check.
     * @return true, if the passed in Class has a .equals() method somewhere between
     * itself and just below Object in it's inheritance.
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
     * Get a deterministic hashCode (int) value for an Object, regardless of
     * when it was created or where it was loaded into memory.  The problem
     * with java.lang.Object.hashCode() is that it essentially relies on
     * memory location of an object (what identity it was assigned), whereas
     * this method will produce the same hashCode for any object graph, regardless
     * of how many times it is created.<br><br>
     * <p>
     * This method will handle cycles correctly (A-&gt;B-&gt;C-&gt;A).  In this case,
     * Starting with object A, B, or C would yield the same hashCode.  If an
     * object encountered (root, sub-object, etc.) has a hashCode() method on it
     * (that is not Object.hashCode()), that hashCode() method will be called
     * and it will stop traversal on that branch.
     *
     * @param obj Object who hashCode is desired.
     * @return the 'deep' hashCode value for the passed in object.
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

            // Protects Floats and Doubles from causing inequality, even if there are within an epsilon distance
            // of one another.  It does this by marshalling values of IEEE 754 numbers to coarser grained resolution,
            // allowing for dynamic range on obviously different values, but identical values for IEEE 754 values
            // that are near each other. Since hashes do not have to be unique, this upholds the hashCode()
            // contract...two hash values that are not the same guarantee the objects are not equal, however, two
            // values that are the same mean the two objects COULD be equals.
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
        // Normalize the value to a fixed precision
        double normalizedValue = Math.round(value * SCALE_DOUBLE) / SCALE_DOUBLE;
        // Convert to long for hashing
        long bits = Double.doubleToLongBits(normalizedValue);
        // Standard way to hash a long in Java
        return (int) (bits ^ (bits >>> 32));
    }

    private static final float SCALE_FLOAT = (float) Math.pow(10, 5); // Scale according to epsilon for float

    private static int hashFloat(float value) {
        // Normalize the value to a fixed precision
        float normalizedValue = Math.round(value * SCALE_FLOAT) / SCALE_FLOAT;
        // Convert to int for hashing, as float bits can be directly converted
        int bits = Float.floatToIntBits(normalizedValue);
        // Return the hash
        return bits;
    }

    /**
     * Determine if the passed in class has a non-Object.hashCode() method.  This
     * method caches its results in static ConcurrentHashMap to benefit
     * execution performance.
     *
     * @param c Class to check.
     * @return true, if the passed in Class has a .hashCode() method somewhere between
     * itself and just below Object in it's inheritance.
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
}
