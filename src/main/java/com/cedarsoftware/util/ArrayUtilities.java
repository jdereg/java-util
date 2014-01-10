/*
 *         Copyright (c) Cedar Software LLC
 *
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
package com.cedarsoftware.util;

/**
 * Useful utilities for working with Java arrays.
 *
 * @author John DeRegnaucourt (jdereg@gmail.com)
 * @author Ken Partlow (kpartlow@gmail.com)
 *
 */
@Deprecated
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
     * This is a null-safe isEmpty check.  It uses the Array
     * static class for doing a length check.  This check is actually
     * .0001 ms slower than the following typed check:
     * <p/>
     * <code>return array == null || array.length == 0;</code>
     * <p/>
     * but gives you more flexibility, since it checks for all array
     * types.
     *
     * @param array array to check
     * @return true if empty or null
     */
    public static boolean isEmpty(final Object array)
    {
        return com.cedarsoftware.lang.reflect.ArrayUtilities.isEmpty(array);
    }

    /**
     * This is a null-safe size check.  It uses the Array
     * static class for doing a length check.  This check is actually
     * .0001 ms slower than the following typed check:
     * <p/>
     * <code>return (array == null) ? 0 : array.length;</code>
     *
     * @deprecated use com.cedarsoftware.lang.reflect.ArrayUtilities.size();
     * @param array array to check
     * @return true if empty or null
     */
    public static int size(final Object array)
    {
        return com.cedarsoftware.lang.reflect.ArrayUtilities.size(array);
    }


    /**
     * <p>Shallow copies an array of Objects
     * <p/>
     * <p>The objects in the array are not cloned, thus there is no special
     * handling for multi-dimensional arrays.</p>
     * <p/>
     * <p>This method returns <code>null</code> if <code>null</code> array input.</p>
     *
     * @param array the array to shallow clone, may be <code>null</code>
     * @return the cloned array, <code>null</code> if <code>null</code> input
     */
    public static Object[] shallowCopy(final Object[] array)
    {
        return com.cedarsoftware.lang.reflect.ArrayUtilities.shallowCopy(array);
//        if (array == null)
//        {
//            return null;
//        }
//        return array.clone();
    }

    /**
     * <p>Adds all the elements of the given arrays into a new array.</p>
     * <p>The new array contains all of the element of <code>array1</code> followed
     * by all of the elements <code>array2</code>. When an array is returned, it is always
     * a new array.</p>
     * <p/>
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
     * @return The new array, <code>null</code> if <code>null</code> array inputs.
     *         The type of the new array is the type of the first array.
     */
    public static Object[] addAll(final Object[] array1, final Object[] array2)
    {
        return com.cedarsoftware.lang.reflect.ArrayUtilities.addAll(array1, array2);
//        if (array1 == null)
//        {
//            return shallowCopy(array2);
//        }
//        else if (array2 == null)
//        {
//            return shallowCopy(array1);
//        }
//        final Object[] newArray = (Object[]) Array.newInstance(array1.getClass().getComponentType(),
//                array1.length + array2.length);
//        System.arraycopy(array1, 0, newArray, 0, array1.length);
//        System.arraycopy(array2, 0, newArray, array1.length, array2.length);
//        return newArray;
    }

    public static <T> T[] removeItem(T[] array, int pos)
    {
        return com.cedarsoftware.lang.reflect.ArrayUtilities.remove(array, pos);
    }

    /**
     * @deprecated Use com.cedarsoftware.lang.reflect.ArrayUtilities.createSubarray instead
     */
    public static <T> T[] getArraySubset(T[] array, int start, int end)
    {
        return com.cedarsoftware.lang.reflect.ArrayUtilities.createSubarray(array, start, end);
    }
}
