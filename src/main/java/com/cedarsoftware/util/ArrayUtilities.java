package com.cedarsoftware.util;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;


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
 * <h2>Security Configuration</h2>
 * <p>ArrayUtilities provides configurable security controls to prevent various attack vectors including
 * memory exhaustion, reflection attacks, and array manipulation exploits.
 * All security features are <strong>disabled by default</strong> for backward compatibility.</p>
 *
 * <p>Security controls can be enabled via system properties:</p>
 * <ul>
 *   <li><code>arrayutilities.security.enabled=false</code> &mdash; Master switch for all security features</li>
 *   <li><code>arrayutilities.component.type.validation.enabled=false</code> &mdash; Block dangerous system classes</li>
 *   <li><code>arrayutilities.max.array.size=2147483639</code> &mdash; Maximum array size (default=Integer.MAX_VALUE-8 when enabled)</li>
 *   <li><code>arrayutilities.dangerous.class.patterns=java.lang.Runtime,java.lang.ProcessBuilder,...</code> &mdash; Comma-separated dangerous class patterns</li>
 * </ul>
 *
 * <h3>Security Features</h3>
 * <ul>
 *   <li><b>Component Type Validation:</b> Prevents creation of arrays with dangerous system classes (Runtime, ProcessBuilder, etc.)</li>
 *   <li><b>Array Size Validation:</b> Prevents integer overflow and memory exhaustion through oversized arrays</li>
 *   <li><b>Dangerous Class Filtering:</b> Blocks array creation for security-sensitive classes</li>
 *   <li><b>Error Message Sanitization:</b> Prevents information disclosure in error messages</li>
 * </ul>
 *
 * <h3>Usage Example</h3>
 * <pre>{@code
 * // Enable security with custom limits
 * System.setProperty("arrayutilities.security.enabled", "true");
 * System.setProperty("arrayutilities.max.array.size", "1000000");
 * System.setProperty("arrayutilities.dangerous.classes.validation.enabled", "true");
 *
 * // These will now enforce security controls
 * String[] array = ArrayUtilities.nullToEmpty(String.class, null);
 * }</pre>
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
    public static final Class<?>[] EMPTY_CLASS_ARRAY = new Class<?>[0];
    
    // Default security limits (used when security is enabled)
    private static final int DEFAULT_MAX_ARRAY_SIZE = Integer.MAX_VALUE - 8; // JVM array size limit
    
    // Default dangerous class patterns (moved to system properties in static initializer)
    private static final String DEFAULT_DANGEROUS_CLASS_PATTERNS = 
        "java.lang.Runtime,java.lang.ProcessBuilder,java.lang.System,java.security.,javax.script.,sun.,com.sun.,java.lang.Class";
    
    static {
        // Initialize system properties with defaults if not already set (backward compatibility)
        initializeSystemPropertyDefaults();
    }
    
    private static void initializeSystemPropertyDefaults() {
        // Set dangerous class patterns if not explicitly configured
        if (System.getProperty("arrayutilities.dangerous.class.patterns") == null) {
            System.setProperty("arrayutilities.dangerous.class.patterns", DEFAULT_DANGEROUS_CLASS_PATTERNS);
        }
        
        // Set max array size if not explicitly configured
        if (System.getProperty("arrayutilities.max.array.size") == null) {
            System.setProperty("arrayutilities.max.array.size", String.valueOf(DEFAULT_MAX_ARRAY_SIZE));
        }
    }
    
    // Security configuration methods
    
    private static boolean isSecurityEnabled() {
        return Boolean.parseBoolean(System.getProperty("arrayutilities.security.enabled", "false"));
    }
    
    private static boolean isComponentTypeValidationEnabled() {
        return Boolean.parseBoolean(System.getProperty("arrayutilities.component.type.validation.enabled", "false"));
    }
    
    private static boolean isDangerousClassValidationEnabled() {
        return Boolean.parseBoolean(System.getProperty("arrayutilities.dangerous.classes.validation.enabled", "false"));
    }
    
    private static long getMaxArraySize() {
        String maxSizeProp = System.getProperty("arrayutilities.max.array.size");
        if (maxSizeProp != null) {
            try {
                return Long.parseLong(maxSizeProp);
            } catch (NumberFormatException e) {
                // Fall through to default
            }
        }
        return isSecurityEnabled() ? DEFAULT_MAX_ARRAY_SIZE : Long.MAX_VALUE;
    }
    
    private static String[] getDangerousClassPatterns() {
        String patterns = System.getProperty("arrayutilities.dangerous.class.patterns", DEFAULT_DANGEROUS_CLASS_PATTERNS);
        return patterns.split(",");
    }

    /**
     * Private constructor to promote using as static class.
     */
    private ArrayUtilities() {
        super();
    }
    
    /**
     * Security: Validates that the component type is safe for array creation.
     * This prevents creation of arrays of dangerous system classes.
     * 
     * @param componentType the component type to validate
     * @throws SecurityException if the component type is dangerous and validation is enabled
     */
    private static void validateComponentType(Class<?> componentType) {
        if (componentType == null) {
            return; // Allow null check to be handled elsewhere
        }
        
        // Only validate if security features are enabled
        if (!isSecurityEnabled() || !isComponentTypeValidationEnabled()) {
            return;
        }
        
        String className = componentType.getName();
        String[] dangerousPatterns = getDangerousClassPatterns();
        
        // Check if class name matches any dangerous patterns
        for (String pattern : dangerousPatterns) {
            pattern = pattern.trim();
            if (pattern.endsWith(".")) {
                // Package prefix pattern (e.g., "java.security.")
                if (className.startsWith(pattern)) {
                    throw new SecurityException("Array creation denied for security-sensitive class: " + className);
                }
            } else {
                // Exact class name pattern (e.g., "java.lang.Class")
                if (className.equals(pattern)) {
                    throw new SecurityException("Array creation denied for security-sensitive class: " + className);
                }
            }
        }
    }
    
    /**
     * Security: Validates array size to prevent integer overflow and memory exhaustion.
     * 
     * @param size the proposed array size
     * @throws SecurityException if size is negative or too large and validation is enabled
     */
    static void validateArraySize(long size) {
        // Only validate if security features are enabled
        if (!isSecurityEnabled()) {
            return;
        }
        
        if (size < 0) {
            throw new SecurityException("Array size cannot be negative");
        }
        
        long maxSize = getMaxArraySize();
        if (size > maxSize) {
            throw new SecurityException("Array size too large: " + size + " > " + maxSize);
        }
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
     * Null-safe check whether the given array contains at least one element.
     *
     * @param array array to check
     * @return {@code true} if array is non-null and has a positive length
     */
    public static boolean isNotEmpty(final Object array) {
        return !isEmpty(array);
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
     * Return the supplied array, or an empty array if {@code null}.
     *
     * @param componentType the component type for the empty array when {@code array} is {@code null}
     * @param array         array which may be {@code null}
     * @param <T>           array component type
     * @return the original array, or a new empty array of the specified type if {@code array} is {@code null}
     */
    @SuppressWarnings("unchecked")
    public static <T> T[] nullToEmpty(Class<T> componentType, T[] array) {
        Objects.requireNonNull(componentType, "componentType is null");
        // Security: Validate component type before array creation
        validateComponentType(componentType);
        return array == null ? (T[]) Array.newInstance(componentType, 0) : array;
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
        if (elements == null) {
            return null;
        }
        return Arrays.copyOf(elements, elements.length);
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
        
        // Security: Check for integer overflow when combining arrays
        long combinedLength = (long) array1.length + (long) array2.length;
        validateArraySize(combinedLength);
        
        Class<?> componentType = array1.getClass().getComponentType();
        // Security: Validate component type before array creation
        validateComponentType(componentType);
        
        final T[] newArray = (T[]) Array.newInstance(componentType, (int) combinedLength);
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
     * <p><strong>Time Complexity:</strong> O(n) where n is the array length</p>
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
        Objects.requireNonNull(array, "array cannot be null");
        final int len = array.length;
        if (pos < 0 || pos >= len) {
            // Security: Don't expose array contents in error message
            throw new ArrayIndexOutOfBoundsException("Invalid array index");
        }

        Class<?> componentType = array.getClass().getComponentType();
        // Security: Validate component type before array creation
        validateComponentType(componentType);
        
        T[] dest = (T[]) Array.newInstance(componentType, len - 1);
        System.arraycopy(array, 0, dest, 0, pos);
        System.arraycopy(array, pos + 1, dest, pos, len - pos - 1);
        return dest;
    }

    /**
     * Append a single element to an array, returning a new array containing the element.
     *
     * @param componentType component type for the array when {@code array} is {@code null}
     * @param array         existing array, may be {@code null}
     * @param item          element to append
     * @param <T>           array component type
     * @return new array with {@code item} appended
     */
    @SuppressWarnings("unchecked")
    public static <T> T[] addItem(Class<T> componentType, T[] array, T item) {
        Objects.requireNonNull(componentType, "componentType is null");
        // Security: Validate component type before array creation
        validateComponentType(componentType);
        
        if (array == null) {
            T[] result = (T[]) Array.newInstance(componentType, 1);
            result[0] = item;
            return result;
        }
        
        // Security: Check for integer overflow when adding item
        long newLength = (long) array.length + 1;
        validateArraySize(newLength);
        
        T[] newArray = Arrays.copyOf(array, (int) newLength);
        newArray[array.length] = item;
        return newArray;
    }

    /**
     * Locate the first index of {@code item} within {@code array}.
     *
     * @param array array to search
     * @param item  item to locate
     * @param <T>   array component type
     * @return index of the item or {@code -1} if not found or array is {@code null}
     */
    public static <T> int indexOf(T[] array, T item) {
        if (array == null) {
            return -1;
        }
        for (int i = 0; i < array.length; i++) {
            if (Objects.equals(array[i], item)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Locate the last index of {@code item} within {@code array}.
     *
     * @param array array to search
     * @param item  item to locate
     * @param <T>   array component type
     * @return index of the item or {@code -1} if not found or array is {@code null}
     */
    public static <T> int lastIndexOf(T[] array, T item) {
        if (array == null) {
            return -1;
        }
        for (int i = array.length - 1; i >= 0; i--) {
            if (Objects.equals(array[i], item)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Determine whether the provided array contains the specified item.
     *
     * @param array the array to search, may be {@code null}
     * @param item  the item to find
     * @param <T>   the array component type
     * @return {@code true} if the item exists in the array; {@code false} otherwise
     */
    public static <T> boolean contains(T[] array, T item) {
        return indexOf(array, item) >= 0;
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
        Objects.requireNonNull(classToCastTo, "classToCastTo is null");
        Objects.requireNonNull(c, "collection is null");
        
        // Security: Validate component type before array creation
        validateComponentType(classToCastTo);
        
        // Security: Validate collection size to prevent memory exhaustion
        validateArraySize(c.size());

        T[] array = (T[]) Array.newInstance(classToCastTo, c.size());
        return c.toArray(array);
    }

    /**
     * Creates a deep copy of all container structures (arrays and collections) while preserving
     * references to non-container objects. This method delegates to 
     * {@link CollectionUtilities#deepCopyContainers(Object)} which performs iterative traversal.
     * 
     * <p>See {@link CollectionUtilities#deepCopyContainers(Object)} for full documentation.</p>
     * 
     * @param <T> the type of the input array
     * @param array the array to deep copy (can contain nested arrays and collections)
     * @return a deep copy of all containers with same references to non-containers,
     *         or the same reference if array is not actually an array
     * @see CollectionUtilities#deepCopyContainers(Object)
     */
    public static <T> T deepCopyContainers(T array) {
        return CollectionUtilities.deepCopyContainers(array);
    }

    /**
     * Sets an element in a primitive array at the specified index with optimized handling
     * that avoids reflection and boxing/unboxing overhead. This method is designed for
     * primitive arrays (int[], long[], etc.) or when the array type is unknown at compile time.
     *
     * <p><b>Important:</b> If you know at compile time that you have an Object[] array
     * (including String[], Integer[], Employee[], etc.), use direct array assignment instead
     * for maximum performance:</p>
     * <pre>{@code
     * // For known Object[] types - FASTEST (no method call overhead)
     * Object[] refArray = ...;
     * refArray[index] = element;
     *
     * // Only use this method for primitives or unknown types
     * int[] primitiveArray = ...;
     * ArrayUtilities.setPrimitiveElement(primitiveArray, index, element);
     * }</pre>
     *
     * <p><b>Performance Characteristics:</b></p>
     * <ul>
     *   <li><b>Primitive arrays:</b> Type-specific casting with no boxing - ~2-5x faster than Array.set()</li>
     *   <li><b>No instanceof check:</b> Assumes caller has already handled Object[] case</li>
     *   <li><b>Fallback to Array.set():</b> For edge cases only</li>
     * </ul>
     *
     * <p><b>Primitive Type Handling:</b></p>
     * <ul>
     *   <li>Null values are converted to primitive defaults (0, 0L, 0.0, false, '\0', etc.)</li>
     *   <li>Direct casting and unboxing to avoid unnecessary object creation</li>
     *   <li>Type-specific branches allow JIT optimization</li>
     * </ul>
     *
     * <p><b>Usage Example:</b></p>
     * <pre>{@code
     * // Primitive array - uses type-specific casting
     * int[] numbers = new int[10];
     * ArrayUtilities.setPrimitiveElement(numbers, 0, 42);
     *
     * // Handles null values for primitives
     * ArrayUtilities.setPrimitiveElement(numbers, 1, null);  // Sets to 0
     *
     * // When type is unknown at compile time
     * Object array = getArrayFromSomewhere();
     * if (array instanceof Object[]) {
     *     ((Object[])array)[index] = value;  // Direct assignment
     * } else {
     *     ArrayUtilities.setPrimitiveElement(array, index, value);  // Primitive handling
     * }
     * }</pre>
     *
     * @param array the array to modify (must not be null, should be a primitive array)
     * @param index the index at which to set the element
     * @param element the element to set (can be null for primitives, converted to default values)
     * @throws IllegalArgumentException if the element cannot be stored in the array due to type mismatch
     * @throws ArrayIndexOutOfBoundsException if the index is out of bounds
     * @throws NullPointerException if array is null
     * @see Array#set(Object, int, Object)
     */
    public static void setPrimitiveElement(Object array, int index, Object element) {
        // For primitive arrays, use type-specific assignments to avoid boxing/unboxing
        // NOTE: Caller should handle Object[] case directly for maximum performance
        Class<?> arrayClass = array.getClass();
        if (!arrayClass.isArray()) {
            throw new IllegalArgumentException("setPrimitiveElement() requires an array, but received: " + arrayClass.getName());
        }
        Class<?> componentType = arrayClass.getComponentType();

        // Use if/else instead of reflection for common primitive types
        try {
            if (componentType == int.class) {
                ((int[])array)[index] = element == null ? 0 : ((Number)element).intValue();
                return;
            } else if (componentType == long.class) {
                ((long[])array)[index] = element == null ? 0L : ((Number)element).longValue();
                return;
            } else if (componentType == double.class) {
                ((double[])array)[index] = element == null ? 0.0 : ((Number)element).doubleValue();
                return;
            } else if (componentType == boolean.class) {
                ((boolean[])array)[index] = element != null && (element instanceof Boolean) && (Boolean)element;
                return;
            } else if (componentType == byte.class) {
                ((byte[])array)[index] = element == null ? 0 : ((Number)element).byteValue();
                return;
            } else if (componentType == char.class) {
                if (element == null) {
                    ((char[])array)[index] = '\0';
                } else if (element instanceof Character) {
                    ((char[])array)[index] = (Character)element;
                } else if (element instanceof String && ((String)element).length() > 0) {
                    ((char[])array)[index] = ((String)element).charAt(0);
                } else {
                    ((char[])array)[index] = '\0';
                }
                return;
            } else if (componentType == short.class) {
                ((short[])array)[index] = element == null ? 0 : ((Number)element).shortValue();
                return;
            } else if (componentType == float.class) {
                ((float[])array)[index] = element == null ? 0.0f : ((Number)element).floatValue();
                return;
            } else {
                // Non-primitive arrays (Object[], String[], etc.) should use direct assignment
                // This method should ONLY be called for primitive arrays
                throw new IllegalArgumentException("setPrimitiveElement() should only be used for primitive arrays. " +
                        "For reference type arrays like " + componentType.getName() + "[], use direct assignment: array[index] = value");
            }
        } catch (ClassCastException | NullPointerException e) {
            // Let it fall through to the error handling below
        }

        // Error handling
        String elementType = element == null ? "null" : element.getClass().getName();
        String arrayType = array.getClass().getComponentType().getName() + "[]";

        throw new IllegalArgumentException("Cannot set '" + elementType + "' (value: " + element +
                ") into '" + arrayType + "' at index " + index);
    }

}
