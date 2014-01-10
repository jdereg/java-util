/*
 * Licensed under the Apache License, Version 2.0 (the "License"); you 
 * may not use this file except in compliance with the License.  You may 
 * obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.cedarsoftware.lang.reflect;

import java.lang.reflect.Array;
import java.text.MessageFormat;

/**
 * Handy utilities for working with Java arrays.
 *
 */
public final class ArrayUtilities
{
    /**
     * Immutable common arrays.
     */
    public static final Object[] EMPTY_OBJECT_ARRAY = new Object[0];
    public static final Class[] EMPTY_CLASS_ARRAY = new Class[0];

    /**
     * Private constructor to promote using as static class.
     */
    private ArrayUtilities()
    {
        super();
    }

    /**
     * <p>
     * This is a null-safe isEmpty check. It uses the Array static class for
     * doing a length check. This check is actually .0001 ms slower than the
     * following typed check:
     * <p/>
     * <code>return array == null || array.length == 0;</code>
     * <p/>
     * but gives you more flexibility, since it checks for all array types.
     *
     * @param array
     *            array to check
     * @return true if empty or null
     */
    public static boolean isEmpty(final Object array)
    {
        return array == null || Array.getLength(array) == 0;
    }

    /**
     * <p>
     * This is a null-safe hasContent check. It uses the Array static class for
     * doing a length check. This check is actually .0001 ms slower than the
     * following typed check:
     * <p/>
     * but gives you more flexibility, since it checks for all array types.
     *
     * @param array
     *            array to check
     * @return true if array is not empty
     */
    public static boolean hasContent(final Object array)
    {
        return array != null && Array.getLength(array) > 0;
    }

    /**
     * This is a null-safe size check. It uses the Array static class for doing
     * a length check. This check is actually .0001 ms slower than the following
     * typed check:
     * <p/>
     * <code>return (array == null) ? 0 : array.length;</code>
     *
     * @param array
     *            array to check
     * @return true if empty or null
     */
    public static int size(final Object array)
    {
        return array == null ? 0 : Array.getLength(array);
    }

    // Generic array
    // -----------------------------------------------------------------------
    /**
     * <p>
     * Create a type-safe generic array from parameterized elements.
     * </p>
     *
     * <p>
     * Note, this method makes only sense to provide arguments of the same type
     * so that the compiler can deduce the type of the array itself. While it is
     * possible to select the type explicitly like in
     * <code>Number[] array = ArrayUtilities.&lt;Number&gt;toArray(Integer.valueOf(42), Double.valueOf(Math.PI))</code>
     * , there is no real advantage when compared to
     * <code>new Number[] {Integer.valueOf(42), Double.valueOf(Math.PI)}</code>.
     * </p>
     *
     * @param <T>
     *            the array's element type
     * @param items
     *            the varargs array items, null allowed
     * @return the array, not null unless a null array is passed in
     */
    public static <T> T[] toArray(final T... items)
    {
        return items;
    }

    // Clone
    // -----------------------------------------------------------------------
    /**
     * <p>
     * Shallow clones an array returning a typecast result and handling
     * {@code null}.
     * </p>
     *
     * <p>
     * The objects in the array are not cloned, thus there is no special
     * handling for multi-dimensional arrays.
     * </p>
     *
     * <p>
     * This method returns {@code null} for a {@code null} input array.
     * </p>
     *
     * @param <T>
     *            the component type of the array
     * @param array
     *            the array to shallow clone, may be {@code null}
     * @return the cloned array, {@code null} if {@code null} input
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
     * <p>
     * Adds all the elements of the given arrays into a new array.
     * </p>
     * <p>
     * The new array contains all of the element of {@code array1} followed by
     * all of the elements {@code array2}. When an array is returned, it is
     * always a new array.
     * </p>
     *
     * <pre>
     * ArrayUtils.addAll(null, null)     = null
     * ArrayUtils.addAll(array1, null)   = cloned copy of array1
     * ArrayUtils.addAll(null, array2)   = cloned copy of array2
     * ArrayUtils.addAll([], [])         = []
     * ArrayUtils.addAll([null], [null]) = [null, null]
     * ArrayUtils.addAll(["a", "b", "c"], ["1", "2", "3"]) = ["a", "b", "c", "1", "2", "3"]
     * </pre>
     *
     * @param <T>
     *            the component type of the array
     * @param array1
     *            the first array whose elements are added to the new array, may
     *            be {@code null}
     * @param array2
     *            the second array whose elements are added to the new array,
     *            may be {@code null}
     * @return The new array, {@code null} if both arrays are {@code null}. The
     *         type of the new array is the type of the first array, unless the
     *         first array is null, in which case the type is the same as the
     *         second array.
     * @throws IllegalArgumentException
     *             if the array types are incompatible
     */
    public static <T> T[] addAll(T[] array1, T... array2)
    {
        if (array1 == null)
        {
            return shallowCopy(array2);
        }
        else if (array2 == null)
        {
            return shallowCopy(array1);
        }
        final Class<?> t = array1.getClass().getComponentType();
        // OK, because array is of type T
        @SuppressWarnings("unchecked")
        T[] joinedArray = (T[])Array.newInstance(t, array1.length + array2.length);
        System.arraycopy(array1, 0, joinedArray, 0, array1.length);
        System.arraycopy(array2, 0, joinedArray, array1.length, array2.length);
        return joinedArray;
    }

    /**
     * <p>
     * Removes the element at the specified position from the specified array.
     * All subsequent elements are shifted to the left (subtracts one from their
     * indices).
     * </p>
     *
     * <p>
     * This method returns a new array with the same elements of the input array
     * except the element on the specified position. The component type of the
     * returned array is always the same as that of the input array.
     * </p>
     *
     * <p>
     * If the input array is {@code null}, an IndexOutOfBoundsException will be
     * thrown, because in that case no valid index can be specified.
     * </p>
     *
     * <pre>
     * ArrayUtils.remove(["a"], 0)           = []
     * ArrayUtils.remove(["a", "b"], 0)      = ["b"]
     * ArrayUtils.remove(["a", "b"], 1)      = ["a"]
     * ArrayUtils.remove(["a", "b", "c"], 1) = ["a", "c"]
     * </pre>
     *
     * @param <T>
     *            the component type of the array
     * @param array
     *            the array to remove the element from, may not be {@code null}
     * @param index
     *            the position of the element to be removed
     * @return A new array containing the existing elements except the element
     *         at the specified position.
     * @throws IndexOutOfBoundsException
     *             if the index is out of range (index < 0 || index >=
     *             array.length), or if the array is {@code null}.
     */
    @SuppressWarnings("unchecked")
    // remove() always creates an array of the same type as its input
    public static <T> T[] remove(T[] array, int index)
    {
        return (T[])remove((Object)array, index);
    }

    /**
     * <p>
     * Removes the element at the specified position from the specified array.
     * All subsequent elements are shifted to the left (subtracts one from their
     * indices).
     * </p>
     *
     * <p>
     * This method returns a new array with the same elements of the input array
     * except the element on the specified position. The component type of the
     * returned array is always the same as that of the input array.
     * </p>
     *
     * <p>
     * If the input array is {@code null}, an IndexOutOfBoundsException will be
     * thrown, because in that case no valid index can be specified.
     * </p>
     *
     * @param array
     *            the array to remove the element from, may not be {@code null}
     * @param index
     *            the position of the element to be removed
     * @return A new array containing the existing elements except the element
     *         at the specified position.
     * @throws IndexOutOfBoundsException
     *             if the index is out of range (index < 0 || index >=
     *             array.length), or if the array is {@code null}.
     */
    public static Object remove(Object array, int index)
    {
        int length = size(array);
        if (index < 0 || index >= length)
        {
            throw new IndexOutOfBoundsException(MessageFormat.format("Index: {0}, Length: {1}", index, length));
        }

        Object result = Array.newInstance(array.getClass().getComponentType(), length - 1);
        System.arraycopy(array, 0, result, 0, index);
        if (index < length - 1)
        {
            System.arraycopy(array, index + 1, result, index, length - index - 1);
        }

        return result;
    }

    /**
     * <p>
     * Returns a subset of the passed in array.
     * </p>
     *
     * <p>
     * This method returns a subset of the passed in array. If the passed in
     * array is null we just return null. The size of the new array will be end
     * - start. The component type of the returned array is always the same as
     * that of the input array.
     * </p>
     *
     * @param array
     *            the array to create our subset from
     * @param start
     *            the position of the first element in the subset
     * @param end
     *            the position of the last element in the subset
     * @return A new array containing the existing elements except the element
     *         at the specified position.
     */
    @SuppressWarnings("unchecked")
    public static <T> T[] createSubarray(T[] array, int start, int end)
    {
        if (array == null)
        {
            return null;
        }

        if (start < 0)
        {
            start = 0;
        }

        if (end > array.length)
        {
            end = array.length;
        }

        // if start is greater than end, return []
        final Class<?> t = array.getClass().getComponentType();

        if (start > end)
        {
            return (T[])Array.newInstance(t, 0);
        }

        Object subset = Array.newInstance(t, end - start);
        System.arraycopy(array, start, subset, 0, end - start);
        return (T[])subset;
    }

}
