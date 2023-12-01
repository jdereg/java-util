package com.cedarsoftware.util;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
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
 *
 * This method will handle cycles correctly, for example A-&gt;B-&gt;C-&gt;A.  Suppose a and
 * a' are two separate instances of A with the same values for all fields on
 * A, B, and C.  Then a.deepEquals(a') will return true.  It uses cycle detection
 * storing visited objects in a Set to prevent endless loops.<br><br>
 *
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
public class DeepEquals
{
    private DeepEquals () {}

    public static final String IGNORE_CUSTOM_EQUALS = "ignoreCustomEquals";
    public static final String ALLOW_STRINGS_TO_MATCH_NUMBERS = "stringsCanMatchNumbers";
    private static final Map<String, Boolean> _customEquals = new ConcurrentHashMap<>();
    private static final Map<String, Boolean> _customHash = new ConcurrentHashMap<>();
    private static final double doubleEplison = 1e-15;
    private static final double floatEplison = 1e-6;
    private static final Set<Class<?>> prims = new HashSet<>();

    static
    {
        prims.add(Byte.class);
        prims.add(Integer.class);
        prims.add(Long.class);
        prims.add(Double.class);
        prims.add(Character.class);
        prims.add(Float.class);
        prims.add(Boolean.class);
        prims.add(Short.class);
    }

    private final static class ItemsToCompare
    {
        private final Object _key1;
        private final Object _key2;

        private ItemsToCompare(Object k1, Object k2)
        {
            _key1 = k1;
            _key2 = k2;
        }

        public boolean equals(Object other)
        {
            if (!(other instanceof ItemsToCompare))
            {
                return false;
            }

            ItemsToCompare that = (ItemsToCompare) other;
            return _key1 == that._key1 && _key2 == that._key2;
        }

        public int hashCode()
        {
            int h1 = _key1 != null ? _key1.hashCode() : 0;
            int h2 = _key2 != null ? _key2.hashCode() : 0;
            return h1 + h2;
        }

        public String toString()
        {
            if (_key1.getClass().isPrimitive() && _key2.getClass().isPrimitive())
            {
                return _key1 + " | " + _key2;
            }
            return _key1.getClass().getName() + " | " + _key2.getClass().getName();
        }
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
     *
     * This method will handle cycles correctly, for example A-&gt;B-&gt;C-&gt;A.  Suppose a and
     * a' are two separate instances of the A with the same values for all fields on
     * A, B, and C.  Then a.deepEquals(a') will return true.  It uses cycle detection
     * storing visited objects in a Set to prevent endless loops.
     * @param a Object one to compare
     * @param b Object two to compare
     * @return true if a is equivalent to b, false otherwise.  Equivalent means that
     * all field values of both subgraphs are the same, either at the field level
     * or via the respectively encountered overridden .equals() methods during
     * traversal.
     */
    public static boolean deepEquals(Object a, Object b)
    {
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
     *
     * This method will handle cycles correctly, for example A-&gt;B-&gt;C-&gt;A.  Suppose a and
     * a' are two separate instances of the A with the same values for all fields on
     * A, B, and C.  Then a.deepEquals(a') will return true.  It uses cycle detection
     * storing visited objects in a Set to prevent endless loops.
     * @param a Object one to compare
     * @param b Object two to compare
     * @param options Map options for compare. With no option, if a custom equals()
     *                method is present, it will be used.  If IGNORE_CUSTOM_EQUALS is
     *                present, it will be expected to be a Set of classes to ignore.
     *                It is a black-list of classes that will not be compared
     *                using .equals() even if the classes have a custom .equals() method
     *                present.  If it is and empty set, then no custom .equals() methods
     *                will be called.
     *
     * @return true if a is equivalent to b, false otherwise.  Equivalent means that
     * all field values of both subgraphs are the same, either at the field level
     * or via the respectively encountered overridden .equals() methods during
     * traversal.
     */
    public static boolean deepEquals(Object a, Object b, Map<String, ?> options)
    {
        Set<ItemsToCompare> visited = new HashSet<>();
        return deepEquals(a, b, options, visited);
    }

    private static boolean deepEquals(Object a, Object b, Map<String, ?> options, Set<ItemsToCompare> visited)
    {
        Deque<ItemsToCompare> stack = new LinkedList<>();
        Set<Class<?>> ignoreCustomEquals = (Set<Class<?>>) options.get(IGNORE_CUSTOM_EQUALS);
        final boolean allowStringsToMatchNumbers = convert2boolean(options.get(ALLOW_STRINGS_TO_MATCH_NUMBERS));

        stack.addFirst(new ItemsToCompare(a, b));

        while (!stack.isEmpty())
        {
            ItemsToCompare itemsToCompare = stack.removeFirst();
            visited.add(itemsToCompare);

            final Object key1 = itemsToCompare._key1;
            final Object key2 = itemsToCompare._key2;
            if (key1 == key2)
            {   // Same instance is always equal to itself.
                continue;
            }

            if (key1 == null || key2 == null)
            {   // If either one is null, they are not equal (both can't be null, due to above comparison).
                return false;
            }

            if (key1 instanceof Number && key2 instanceof Number && compareNumbers((Number)key1, (Number)key2))
            {
                continue;
            }

            if (key1 instanceof AtomicBoolean && key2 instanceof AtomicBoolean)
            {
                if (!compareAtomicBoolean((AtomicBoolean)key1, (AtomicBoolean)key2)) {
                    return false;
                } else {
                    continue;
                }
            }

            if (key1 instanceof Number || key2 instanceof Number)
            {   // If one is a Number and the other one is not, then optionally compare them as strings, otherwise return false
                if (allowStringsToMatchNumbers)
                {
                    try
                    {
                        if (key1 instanceof String && compareNumbers(convert2BigDecimal(key1), (Number)key2))
                        {
                            continue;
                        }
                        else if (key2 instanceof String && compareNumbers((Number)key1, convert2BigDecimal(key2)))
                        {
                            continue;
                        }
                    }
                    catch (Exception ignore) { }
                }
                return false;
            }

            Class<?> key1Class = key1.getClass();

            if (key1Class.isPrimitive() || prims.contains(key1Class) || key1 instanceof String || key1 instanceof Date || key1 instanceof Class)
            {
                if (!key1.equals(key2))
                {
                    return false;
                }
                continue;   // Nothing further to push on the stack
            }

            if (key1 instanceof Set)
            {
                if (!(key2 instanceof Set))
                {
                    return false;
                }
            }
            else if (key2 instanceof Set)
            {
                return false;
            }

            if (key1 instanceof Collection)
            {   // If Collections, they both must be Collection
                if (!(key2 instanceof Collection))
                {
                    return false;
                }
            }
            else if (key2 instanceof Collection)
            {
                return false;
            }

            if (key1 instanceof Map)
            {
                if (!(key2 instanceof Map))
                {
                    return false;
                }
            }
            else if (key2 instanceof Map)
            {
                return false;
            }

            Class<?> key2Class = key2.getClass();
            if (key1Class.isArray())
            {
                if (!key2Class.isArray())
                {
                    return false;
                }
            }
            else if (key2Class.isArray())
            {
                return false;
            }

            if (!isContainerType(key1) && !isContainerType(key2) && !key1Class.equals(key2.getClass()))
            {   // Must be same class
                return false;
            }

            // Special handle Sets - items matter but order does not for equality.
            if (key1 instanceof Set<?>)
            {
                if (!compareUnorderedCollection((Collection<?>) key1, (Collection<?>) key2, stack, visited, options))
                {
                    return false;
                }
                continue;
            }

            // Collections must match in items and order for equality.
            if (key1 instanceof Collection<?>)
            {
                if (!compareOrderedCollection((Collection<?>) key1, (Collection<?>) key2, stack, visited))
                {
                    return false;
                }
                continue;
            }

            // Compare two Maps. This is a slightly more expensive comparison because
            // order cannot be assumed, therefore a temporary Map must be created, however the
            // comparison still runs in O(N) time.
            if (key1 instanceof Map)
            {
                if (!compareMap((Map<?, ?>) key1, (Map<?, ?>) key2, stack, visited, options))
                {
                    return false;
                }
                continue;
            }

            // Handle all [] types.  In order to be equal, the arrays must be the same
            // length, be of the same type, be in the same order, and all elements within
            // the array must be deeply equivalent.
            if (key1Class.isArray())
            {
                if (!compareArrays(key1, key2, stack, visited))
                {
                    return false;
                }
                continue;
            }

            // If there is a custom equals ... AND
            // the caller has not specified any classes to skip ... OR
            // the caller has specified come classes to ignore and this one is not in the list ... THEN
            // compare using the custom equals.
            if (hasCustomEquals(key1Class))
            {
                if (ignoreCustomEquals == null || (ignoreCustomEquals.size() > 0 && !ignoreCustomEquals.contains(key1Class)))
                {
                    if (!key1.equals(key2))
                    {
                        return false;
                    }
                    continue;
                }
            }

            Collection<Field> fields = ReflectionUtils.getDeepDeclaredFields(key1Class);

            for (Field field : fields)
            {
                try
                {
                    ItemsToCompare dk = new ItemsToCompare(field.get(key1), field.get(key2));
                    if (!visited.contains(dk))
                    {
                        stack.addFirst(dk);
                    }
                }
                catch (Exception ignored)
                { }
            }
        }

        return true;
    }

    public static boolean isContainerType(Object o)
    {
        return o instanceof Collection || o instanceof Map;
    }

    /**
     * Deeply compare to Arrays []. Both arrays must be of the same type, same length, and all
     * elements within the arrays must be deeply equal in order to return true.
     * @param array1 [] type (Object[], String[], etc.)
     * @param array2 [] type (Object[], String[], etc.)
     * @param stack add items to compare to the Stack (Stack versus recursion)
     * @param visited Set of objects already compared (prevents cycles)
     * @return true if the two arrays are the same length and contain deeply equivalent items.
     */
    private static boolean compareArrays(Object array1, Object array2, Deque<ItemsToCompare> stack, Set<?> visited)
    {
        // Same instance check already performed...

        final int len = Array.getLength(array1);
        if (len != Array.getLength(array2))
        {
            return false;
        }

        for (int i = 0; i < len; i++)
        {
            ItemsToCompare dk = new ItemsToCompare(Array.get(array1, i), Array.get(array2, i));
            if (!visited.contains(dk))
            {   // push contents for further comparison
                stack.addFirst(dk);
            }
        }
        return true;
    }

    /**
     * Deeply compare two Collections that must be same length and in same order.
     * @param col1 First collection of items to compare
     * @param col2 Second collection of items to compare
     * @param stack add items to compare to the Stack (Stack versus recursion)
     * @param visited Set of objects already compared (prevents cycles)
     * value of 'true' indicates that the Collections may be equal, and the sets
     * items will be added to the Stack for further comparison.
     */
    private static boolean compareOrderedCollection(Collection<?> col1, Collection<?> col2, Deque<ItemsToCompare> stack, Set<?> visited)
    {
        // Same instance check already performed...

        if (col1.size() != col2.size())
        {
            return false;
        }

        Iterator<?> i1 = col1.iterator();
        Iterator<?> i2 = col2.iterator();

        while (i1.hasNext())
        {
            ItemsToCompare dk = new ItemsToCompare(i1.next(), i2.next());
            if (!visited.contains(dk))
            {   // push contents for further comparison
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
     * @param col1 First collection of items to compare
     * @param col2 Second collection of items to compare
     * @param stack add items to compare to the Stack (Stack versus recursion)
     * @param visited Set containing items that have already been compared,
     * so as to prevent cycles.
     * @param options the options for comparison (see {@link #deepEquals(Object, Object, Map)}
     * @return boolean false if the Collections are for certain not equals. A
     * value of 'true' indicates that the Collections may be equal, and the sets
     * items will be added to the Stack for further comparison.
     */
    private static boolean compareUnorderedCollection(Collection<?> col1, Collection<?> col2, Deque<ItemsToCompare> stack, Set<ItemsToCompare> visited, Map<String, ?> options)
    {
        // Same instance check already performed...

        if (col1.size() != col2.size())
        {
            return false;
        }

        Map<Integer, Collection<Object>> fastLookup = new HashMap<>();
        for (Object o : col2)
        {
            int hash = deepHashCode(o);
            Collection<Object> items = fastLookup.computeIfAbsent(hash, k -> new ArrayList<>());
            items.add(o);
        }

        for (Object o : col1)
        {
            Collection<?> other = fastLookup.get(deepHashCode(o));
            if (other == null || other.isEmpty())
            {   // fail fast: item not even found in other Collection, no need to continue.
                return false;
            }

            if (other.size() == 1)
            {   // no hash collision, items must be equivalent or deepEquals is false
                ItemsToCompare dk = new ItemsToCompare(o, other.iterator().next());
                if (!visited.contains(dk))
                {   // Place items on 'stack' for future equality comparison.
                    stack.addFirst(dk);
                }
            }
            else
            {   // hash collision: try all collided items against the current item (if 1 equals, we are good - remove it
                // from collision list, making further comparisons faster)
                if (!isContained(o, other, visited, options))
                {
                    return false;
                }
            }
        }
        return true;
    }
    
    /**
     * Deeply compare two Map instances.  After quick short-circuit tests, this method
     * uses a temporary Map so that this method can run in O(N) time.
     * @param map1 Map one
     * @param map2 Map two
     * @param stack add items to compare to the Stack (Stack versus recursion)
     * @param visited Set containing items that have already been compared, to prevent cycles.
     * @param options the options for comparison (see {@link #deepEquals(Object, Object, Map)}
     * @return false if the Maps are for certain not equals.  'true' indicates that 'on the surface' the maps
     * are equal, however, it will place the contents of the Maps on the stack for further comparisons.
     */
    private static boolean compareMap(Map<?, ?> map1, Map<?, ?> map2, Deque<ItemsToCompare> stack, Set<ItemsToCompare> visited, Map<String, ?> options)
    {
        // Same instance check already performed...

        if (map1.size() != map2.size())
        {
            return false;
        }

        Map<Integer, Collection<Object>> fastLookup = new HashMap<>();

        for (Map.Entry<?, ?> entry : map2.entrySet())
        {
            int hash = deepHashCode(entry.getKey());
            Collection<Object> items = fastLookup.computeIfAbsent(hash, k -> new ArrayList<>());

            // Use only key and value, not specific Map.Entry type for equality check.
            // This ensures that Maps that might use different Map.Entry types still compare correctly.
            items.add(new AbstractMap.SimpleEntry<>(entry.getKey(), entry.getValue()));
        }

        for (Map.Entry<?, ?> entry : map1.entrySet())
        {
            Collection<Object> other = fastLookup.get(deepHashCode(entry.getKey()));
            if (other == null || other.isEmpty())
            {
                return false;
            }

            if (other.size() == 1)
            {
                Map.Entry<?, ?> entry2 = (Map.Entry<?, ?>)other.iterator().next();
                ItemsToCompare dk = new ItemsToCompare(entry.getKey(), entry2.getKey());
                if (!visited.contains(dk))
                {   // Push keys for further comparison
                    stack.addFirst(dk);
                }

                dk = new ItemsToCompare(entry.getValue(), entry2.getValue());
                if (!visited.contains(dk))
                {   // Push values for further comparison
                    stack.addFirst(dk);
                }
            }
            else
            {   // hash collision: try all collided items against the current item (if 1 equals, we are good - remove it
                // from collision list, making further comparisons faster)
                if (!isContained(new AbstractMap.SimpleEntry<>(entry.getKey(), entry.getValue()), other, visited, options))
                {
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * @return true if the passed in o is within the passed in Collection, using a deepEquals comparison
     * element by element.  Used only for hash collisions.
     */
    private static boolean isContained(Object o, Collection<?> other, Set<ItemsToCompare> visited, Map<String, ?> options)
    {
        Iterator<?> i = other.iterator();
        while (i.hasNext())
        {
            Object x = i.next();
            Set<ItemsToCompare> visitedForSubelements = new HashSet<>(visited);
            visitedForSubelements.add(new ItemsToCompare(o, x));
            if (DeepEquals.deepEquals(o, x, options, visitedForSubelements))
            {
                i.remove(); // can only be used successfully once - remove from list
                return true;
            }
        }
        return false;
    }

    private static boolean compareAtomicBoolean(AtomicBoolean a, AtomicBoolean b) {
        return a.get() == b.get();
    }

    private static boolean compareNumbers(Number a, Number b)
    {
        if (a instanceof Float && (b instanceof Float || b instanceof Double))
        {
            return compareFloatingPointNumbers(a, b, floatEplison);
        }
        else if (a instanceof Double && (b instanceof Float || b instanceof Double))
        {
            return compareFloatingPointNumbers(a, b, doubleEplison);
        }

        try
        {
            BigDecimal x = convert2BigDecimal(a);
            BigDecimal y = convert2BigDecimal(b);
            return x.compareTo(y) == 0.0;
        }
        catch (Exception e)
        {
            return false;
        }
    }

    /**
     * Compare if two floating point numbers are within a given range
     */
    private static boolean compareFloatingPointNumbers(Object a, Object b, double epsilon)
    {
        double a1 = a instanceof Double ? (Double) a : (Float) a;
        double b1 = b instanceof Double ? (Double) b : (Float) b;
        return nearlyEqual(a1, b1, epsilon);
    }

    /**
     * Correctly handles floating point comparisions. <br>
     * source: http://floating-point-gui.de/errors/comparison/
     *
     * @param a       first number
     * @param b       second number
     * @param epsilon double tolerance value
     * @return true if a and b are close enough
     */
    private static boolean nearlyEqual(double a, double b, double epsilon)
    {
        final double absA = Math.abs(a);
        final double absB = Math.abs(b);
        final double diff = Math.abs(a - b);

        if (a == b)
        { // shortcut, handles infinities
            return true;
        }
        else if (a == 0 || b == 0 || diff < Double.MIN_NORMAL)
        {
            // a or b is zero or both are extremely close to it
            // relative error is less meaningful here
            return diff < (epsilon * Double.MIN_NORMAL);
        }
        else
        { // use relative error
            return diff / (absA + absB) < epsilon;
        }
    }

    /**
     * Determine if the passed in class has a non-Object.equals() method.  This
     * method caches its results in static ConcurrentHashMap to benefit
     * execution performance.
     * @param c Class to check.
     * @return true, if the passed in Class has a .equals() method somewhere between
     * itself and just below Object in it's inheritance.
     */
    public static boolean hasCustomEquals(Class<?> c)
    {
        StringBuilder sb = new StringBuilder(ReflectionUtils.getClassLoaderName(c));
        sb.append('.');
        sb.append(c.getName());
        String key = sb.toString();
        Boolean ret = _customEquals.get(key);
        
        if (ret != null)
        {
            return ret;
        }

        while (!Object.class.equals(c))
        {
            try
            {
                c.getDeclaredMethod("equals", Object.class);
                _customEquals.put(key, true);
                return true;
            }
            catch (Exception ignored) { }
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
     *
     * This method will handle cycles correctly (A-&gt;B-&gt;C-&gt;A).  In this case,
     * Starting with object A, B, or C would yield the same hashCode.  If an
     * object encountered (root, suboject, etc.) has a hashCode() method on it
     * (that is not Object.hashCode()), that hashCode() method will be called
     * and it will stop traversal on that branch.
     * @param obj Object who hashCode is desired.
     * @return the 'deep' hashCode value for the passed in object.
     */
    public static int deepHashCode(Object obj)
    {
        Set<Object> visited = new HashSet<>();
        LinkedList<Object> stack = new LinkedList<>();
        stack.addFirst(obj);
        int hash = 0;

        while (!stack.isEmpty())
        {
            obj = stack.removeFirst();
            if (obj == null || visited.contains(obj))
            {
                continue;
            }

            visited.add(obj);

            if (obj.getClass().isArray())
            {
                final int len = Array.getLength(obj);
                for (int i = 0; i < len; i++)
                {
                    stack.addFirst(Array.get(obj, i));
                }
                continue;
            }

            if (obj instanceof Collection)
            {
                stack.addAll(0, (Collection<?>)obj);
                continue;
            }

            if (obj instanceof Map)
            {
                stack.addAll(0, ((Map<?, ?>)obj).keySet());
                stack.addAll(0, ((Map<?, ?>)obj).values());
                continue;
            }

            if (obj instanceof Double || obj instanceof Float)
            {
                // just take the integral value for hashcode
                // equality tests things more comprehensively
                stack.add(Math.round(((Number) obj).doubleValue()));
                continue;
            }

            if (hasCustomHashCode(obj.getClass()))
            {   // A real hashCode() method exists, call it.
                hash += obj.hashCode();
                continue;
            }

            Collection<Field> fields = ReflectionUtils.getDeepDeclaredFields(obj.getClass());
            for (Field field : fields)
            {
                try
                {
                    stack.addFirst(field.get(obj));
                }
                catch (Exception ignored) { }
            }
        }
        return hash;
    }

    /**
     * Determine if the passed in class has a non-Object.hashCode() method.  This
     * method caches its results in static ConcurrentHashMap to benefit
     * execution performance.
     * @param c Class to check.
     * @return true, if the passed in Class has a .hashCode() method somewhere between
     * itself and just below Object in it's inheritance.
     */
    public static boolean hasCustomHashCode(Class<?> c)
    {
        StringBuilder sb = new StringBuilder(ReflectionUtils.getClassLoaderName(c));
        sb.append('.');
        sb.append(c.getName());
        String key = sb.toString();
        Boolean ret = _customHash.get(key);
        
        if (ret != null)
        {
            return ret;
        }

        while (!Object.class.equals(c))
        {
            try
            {
                c.getDeclaredMethod("hashCode");
                _customHash.put(key, true);
                return true;
            }
            catch (Exception ignored) { }
            c = c.getSuperclass();
        }
        _customHash.put(key, false);
        return false;
    }
}
