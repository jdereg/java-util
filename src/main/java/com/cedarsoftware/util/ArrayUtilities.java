package com.cedarsoftware.util;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;

/**
 * Handy utilities for working with Java arrays.
 *
 * @author Ken Partlow
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
public final class ArrayUtilities
{
    /**
     * Immutable common arrays.
     */
    public static final Object[] EMPTY_OBJECT_ARRAY = new Object[0];
    public static final byte[] EMPTY_BYTE_ARRAY = new byte[0];
    public static final char[] EMPTY_CHAR_ARRAY = new char[0];
    public static final Character[] EMPTY_CHARACTER_ARRAY = new Character[0];
    public static final Class[] EMPTY_CLASS_ARRAY = new Class[0];

    /**
     * Private constructor to promote using as static class.
     */
    private ArrayUtilities()
    {
        super();
    }

    /**
     * This is a null-safe isEmpty check.  It uses the Array
     * static class for doing a length check.  This check is actually
     * .0001 ms slower than the following typed check:
     * <p>
     * <code>return array == null || array.length == 0;</code>
     * </p>
     * but gives you more flexibility, since it checks for all array
     * types.
     *
     * @param array array to check
     * @return true if empty or null
     */
    public static boolean isEmpty(final Object array)
    {
        return array == null || Array.getLength(array) == 0;
    }

    /**
     * This is a null-safe size check.  It uses the Array
     * static class for doing a length check.  This check is actually
     * .0001 ms slower than the following typed check:
     * <p>
     * <code>return (array == null) ? 0 : array.length;</code>
     * </p>
     * @param array array to check
     * @return true if empty or null
     */
    public static int size(final Object array)
    {
        return array == null ? 0 : Array.getLength(array);
    }


    /**
     * <p>Shallow copies an array of Objects
     * </p>
     * <p>The objects in the array are not cloned, thus there is no special
     * handling for multi-dimensional arrays.
     * </p>
     * <p>This method returns <code>null</code> if <code>null</code> array input.</p>
     *
     * @param array the array to shallow clone, may be <code>null</code>
     * @param <T> the array type
     * @return the cloned array, <code>null</code> if <code>null</code> input
     */
    public static <T> T[] shallowCopy(final T[] array)
    {
        if (array == null)
        {
            return null;
        }
        return array.clone();
    }

    /**
     * Creates and returns an array containing the provided elements.
     *
     * <p>This method accepts a variable number of arguments and returns them as an array of type {@code T[]}.
     * It is primarily used to facilitate array creation in generic contexts, where type inference is necessary.
     *
     * <p><strong>Example Usage:</strong>
     * <pre>{@code
     * String[] stringArray = createArray("Apple", "Banana", "Cherry");
     * Integer[] integerArray = createArray(1, 2, 3, 4);
     * Person[] personArray = createArray(new Person("Alice"), new Person("Bob"));
     * }</pre>
     *
     * <p><strong>Important Considerations:</strong>
     * <ul>
     *     <li><strong>Type Safety:</strong> Due to type erasure in Java generics, this method does not perform any type checks
     *     beyond what is already enforced by the compiler. Ensure that all elements are of the expected type {@code T} to avoid
     *     {@code ClassCastException} at runtime.</li>
     *     <li><strong>Heap Pollution:</strong> The method is annotated with {@link SafeVarargs} to suppress warnings related to heap
     *     pollution when using generics with varargs. It is safe to use because the method does not perform any unsafe operations
     *     on the varargs parameter.</li>
     *     <li><strong>Null Elements:</strong> The method does not explicitly handle {@code null} elements. If {@code null} values
     *     are passed, they will be included in the returned array.</li>
     *     <li><strong>Immutable Arrays:</strong> The returned array is mutable. To create an immutable array, consider wrapping it
     *     using {@link java.util.Collections#unmodifiableList(List)} or using third-party libraries like Guava's
     *     {@link com.google.common.collect.ImmutableList}.</li>
     * </ul>
     *
     * @param <T>      the component type of the array
     * @param elements the elements to be stored in the array
     * @return an array containing the provided elements
     * @throws NullPointerException if the {@code elements} array is {@code null}
     */
    @SafeVarargs
    public static <T> T[] createArray(T... elements) {
        return elements;
    }
    
    /**
     * <p>Adds all the elements of the given arrays into a new array.
     * </p>
     * <p>The new array contains all of the element of <code>array1</code> followed
     * by all of the elements <code>array2</code>. When an array is returned, it is always
     * a new array.
     * </p>
     * <pre>
     * ArrayUtilities.addAll(null, null)     = null
     * ArrayUtilities.addAll(array1, null)   = cloned copy of array1
     * ArrayUtilities.addAll(null, array2)   = cloned copy of array2
     * ArrayUtilities.addAll([], [])         = []
     * ArrayUtilities.addAll([null], [null]) = [null, null]
     * ArrayUtilities.addAll(["a", "b", "c"], ["1", "2", "3"]) = ["a", "b", "c", "1", "2", "3"]
     * </pre>
     *
     * @param array1 the first array whose elements are added to the new array, may be <code>null</code>
     * @param array2 the second array whose elements are added to the new array, may be <code>null</code>
     * @param <T> the array type
     * @return The new array, <code>null</code> if <code>null</code> array inputs.
     *         The type of the new array is the type of the first array.
     */
    @SuppressWarnings("unchecked")
    public static <T> T[] addAll(final T[] array1, final T[] array2)
    {
        if (array1 == null)
        {
            return shallowCopy(array2);
        }
        else if (array2 == null)
        {
            return shallowCopy(array1);
        }
        final T[] newArray = (T[]) Array.newInstance(array1.getClass().getComponentType(), array1.length + array2.length);
        System.arraycopy(array1, 0, newArray, 0, array1.length);
        System.arraycopy(array2, 0, newArray, array1.length, array2.length);
        return newArray;
    }

    @SuppressWarnings("unchecked")
    public static <T> T[] removeItem(T[] array, int pos)
    {
        final int len = Array.getLength(array);
        T[] dest = (T[]) Array.newInstance(array.getClass().getComponentType(), len - 1);

        System.arraycopy(array, 0, dest, 0, pos);
        System.arraycopy(array, pos + 1, dest, pos, len - pos - 1);
        return dest;
    }

    public static <T> T[] getArraySubset(T[] array, int start, int end)
    {
        return Arrays.copyOfRange(array, start, end);
    }

    /**
     * Convert Collection to a Java (typed) array [].
     * @param classToCastTo array type (Object[], Person[], etc.)
     * @param c Collection containing items to be placed into the array.
     * @param <T> Type of the array
     * @return Array of the type (T) containing the items from collection 'c'.
     */
    @SuppressWarnings("unchecked")
    public static <T> T[] toArray(Class<T> classToCastTo, Collection<?> c)
    {
        T[] array = c.toArray((T[]) Array.newInstance(classToCastTo, c.size()));
        Iterator i = c.iterator();
        int idx = 0;
        while (i.hasNext())
        {
            Array.set(array, idx++, i.next());
        }
        return array;
    }
}
