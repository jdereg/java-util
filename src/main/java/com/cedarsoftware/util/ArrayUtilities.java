package com.cedarsoftware.util;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;

/**
 * A utility class that provides various static methods for working with Java arrays.
 * <p>
 * {@code ArrayUtilities} simplifies common array operations, such as checking for emptiness,
 * combining arrays, creating subsets, and converting collections to arrays. It includes
 * methods that are null-safe and type-generic, making it a flexible and robust tool
 * for array manipulation in Java.
 * </p>
 *
 * <h2>Key Features</h2>
 * <ul>
 *   <li>Immutable common arrays for common use cases, such as {@link #EMPTY_OBJECT_ARRAY} and {@link #EMPTY_BYTE_ARRAY}.</li>
 *   <li>Null-safe utility methods for checking array emptiness, size, and performing operations like shallow copying.</li>
 *   <li>Support for generic array creation and manipulation, including:
 *     <ul>
 *       <li>Combining multiple arrays into a new array ({@link #addAll}).</li>
 *       <li>Removing an item from an array by index ({@link #removeItem}).</li>
 *       <li>Creating subsets of an array ({@link #getArraySubset}).</li>
 *     </ul>
 *   </li>
 *   <li>Conversion utilities for working with arrays and collections, such as converting a {@link Collection} to an array
 *       of a specified type ({@link #toArray}).</li>
 * </ul>
 *
 * <h2>Usage Examples</h2>
 * <pre>{@code
 * // Check if an array is empty
 * boolean isEmpty = ArrayUtilities.isEmpty(new String[] {});
 *
 * // Combine two arrays
 * String[] combined = ArrayUtilities.addAll(new String[] {"a", "b"}, new String[] {"c", "d"});
 *
 * // Create a subset of an array
 * int[] subset = ArrayUtilities.getArraySubset(new int[] {1, 2, 3, 4, 5}, 1, 4); // {2, 3, 4}
 *
 * // Convert a collection to a typed array
 * List<String> list = List.of("x", "y", "z");
 * String[] array = ArrayUtilities.toArray(String.class, list);
 * }</pre>
 *
 * <h2>Performance Notes</h2>
 * <ul>
 *   <li>Methods like {@link #isEmpty} and {@link #size} are optimized for performance but remain null-safe.</li>
 *   <li>Some methods, such as {@link #toArray} and {@link #addAll}, involve array copying and may incur performance
 *       costs for very large arrays.</li>
 * </ul>
 *
 * <h2>Design Philosophy</h2>
 * <p>
 * This utility class is designed to simplify array operations in a type-safe and null-safe manner.
 * It avoids duplicating functionality already present in the JDK while extending support for
 * generic and collection-based workflows.
 * </p>
 *
 * @author Ken Partlow (kpartlow@gmail.com)
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
public final class ArrayUtilities {
    /**
     * Immutable common arrays.
     */
    public static final Object[] EMPTY_OBJECT_ARRAY = new Object[0];
    public static final byte[] EMPTY_BYTE_ARRAY = new byte[0];
    public static final char[] EMPTY_CHAR_ARRAY = new char[0];
    public static final Character[] EMPTY_CHARACTER_ARRAY = new Character[0];
    public static final Class<?>[] EMPTY_CLASS_ARRAY = new Class[0];

    /**
     * Private constructor to promote using as static class.
     */
    private ArrayUtilities() {
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
    public static boolean isEmpty(final Object array) {
        return array == null || Array.getLength(array) == 0;
    }

    /**
     * Returns the size (length) of the specified array in a null-safe manner.
     * <p>
     * If the provided array is {@code null}, this method returns {@code 0}.
     * Otherwise, it returns the length of the array using {@link Array#getLength(Object)}.
     * </p>
     *
     * <h2>Usage Example</h2>
     * <pre>{@code
     * int[] numbers = {1, 2, 3};
     * int size = ArrayUtilities.size(numbers); // size == 3
     *
     * int sizeOfNull = ArrayUtilities.size(null); // sizeOfNull == 0
     * }</pre>
     *
     * @param array the array whose size is to be determined, may be {@code null}
     * @return the size of the array, or {@code 0} if the array is {@code null}
     */
    public static int size(final Object array) {
        return array == null ? 0 : Array.getLength(array);
    }

    /**
     * <p>Shallow copies an array of Objects
     * </p>
     * <p>The objects in the array are not cloned, thus there is no special
     * handling for multidimensional arrays.
     * </p>
     * <p>This method returns <code>null</code> if <code>null</code> array input.</p>
     *
     * @param array the array to shallow clone, may be <code>null</code>
     * @param <T>   the array type
     * @return the cloned array, <code>null</code> if <code>null</code> input
     */
    public static <T> T[] shallowCopy(final T[] array) {
        if (array == null) {
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
     * <p>The new array contains all the element of <code>array1</code> followed
     * by all the elements <code>array2</code>. When an array is returned, it is always
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
     * @param <T>    the array type
     * @return The new array, <code>null</code> if <code>null</code> array inputs.
     * The type of the new array is the type of the first array.
     */
    @SuppressWarnings("unchecked")
    public static <T> T[] addAll(final T[] array1, final T[] array2) {
        if (array1 == null) {
            return shallowCopy(array2);
        } else if (array2 == null) {
            return shallowCopy(array1);
        }
        final T[] newArray = (T[]) Array.newInstance(array1.getClass().getComponentType(), array1.length + array2.length);
        System.arraycopy(array1, 0, newArray, 0, array1.length);
        System.arraycopy(array2, 0, newArray, array1.length, array2.length);
        return newArray;
    }

    /**
     * Removes an element at the specified position from an array, returning a new array with the element removed.
     * <p>
     * This method creates a new array with length one less than the input array and copies all elements
     * except the one at the specified position. The original array remains unchanged.
     * </p>
     *
     * <p><strong>Example:</strong></p>
     * <pre>{@code
     * Integer[] numbers = {1, 2, 3, 4, 5};
     * Integer[] result = ArrayUtilities.removeItem(numbers, 2);
     * // result = {1, 2, 4, 5}
     * }</pre>
     *
     * @param array the source array from which to remove an element
     * @param pos   the position of the element to remove (zero-based)
     * @param <T>   the component type of the array
     * @return a new array containing all elements from the original array except the element at the specified position
     * @throws ArrayIndexOutOfBoundsException if {@code pos} is negative or greater than or equal to the array length
     * @throws NullPointerException if the input array is null
     */
    @SuppressWarnings("unchecked")
    public static <T> T[] removeItem(T[] array, int pos) {
        final int len = Array.getLength(array);
        T[] dest = (T[]) Array.newInstance(array.getClass().getComponentType(), len - 1);

        System.arraycopy(array, 0, dest, 0, pos);
        System.arraycopy(array, pos + 1, dest, pos, len - pos - 1);
        return dest;
    }

    /**
     * Creates a new array containing elements from the specified range of the source array.
     * <p>
     * Returns a new array containing elements from index {@code start} (inclusive) to index {@code end} (exclusive).
     * The original array remains unchanged.
     * </p>
     *
     * <p><strong>Example:</strong></p>
     * <pre>{@code
     * String[] words = {"apple", "banana", "cherry", "date", "elderberry"};
     * String[] subset = ArrayUtilities.getArraySubset(words, 1, 4);
     * // subset = {"banana", "cherry", "date"}
     * }</pre>
     *
     * @param array the source array from which to extract elements
     * @param start the initial index of the range, inclusive
     * @param end   the final index of the range, exclusive
     * @param <T>   the component type of the array
     * @return a new array containing the specified range from the original array
     * @throws ArrayIndexOutOfBoundsException if {@code start} is negative, {@code end} is greater than the array length,
     *         or {@code start} is greater than {@code end}
     * @throws NullPointerException if the input array is null
     * @see Arrays#copyOfRange(Object[], int, int)
     */
    public static <T> T[] getArraySubset(T[] array, int start, int end) {
        return Arrays.copyOfRange(array, start, end);
    }

    /**
     * Convert Collection to a Java (typed) array [].
     *
     * @param classToCastTo array type (Object[], Person[], etc.)
     * @param c             Collection containing items to be placed into the array.
     * @param <T>           Type of the array
     * @return Array of the type (T) containing the items from collection 'c'.
     */
    @SuppressWarnings("unchecked")
    public static <T> T[] toArray(Class<T> classToCastTo, Collection<?> c) {
        T[] array = c.toArray((T[]) Array.newInstance(classToCastTo, c.size()));
        Iterator<?> i = c.iterator();
        int idx = 0;
        while (i.hasNext()) {
            Array.set(array, idx++, i.next());
        }
        return array;
    }
}
