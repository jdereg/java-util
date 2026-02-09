package com.cedarsoftware.util.convert;

import java.io.File;
import java.lang.reflect.Array;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.EnumSet;
import java.util.IdentityHashMap;

import com.cedarsoftware.util.geom.Color;
import com.cedarsoftware.util.geom.Dimension;
import com.cedarsoftware.util.geom.Insets;
import com.cedarsoftware.util.geom.Point;
import com.cedarsoftware.util.geom.Rectangle;

import static com.cedarsoftware.util.ArrayUtilities.getElement;
import static com.cedarsoftware.util.ArrayUtilities.getLength;
import static com.cedarsoftware.util.ArrayUtilities.setElement;

/**
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
final class ArrayConversions {
    private ArrayConversions() { }

    /**
     * Converts an array to another array of the specified target array type.
     * Handles multidimensional arrays iteratively with cycle detection.
     * Uses IdentityHashMap to detect and handle circular references without stack overflow.
     *
     * Circular reference handling:
     * - Reference to reference arrays: Cycle preserved (target array points to itself)
     * - Reference to primitive arrays: Zero-value used (0, 0.0, false, '\0')
     *
     * @param sourceArray     The source array to convert
     * @param targetArrayType The desired target array type
     * @param converter       The converter for element conversion
     * @return A new array of the specified target type
     */
    static Object arrayToArray(Object sourceArray, Class<?> targetArrayType, Converter converter) {
        // IdentityHashMap to track source array -> target array mappings (for cycle detection)
        IdentityHashMap<Object, Object> visited = new IdentityHashMap<>();

        // Create root target array
        int length = getLength(sourceArray);
        Class<?> targetComponentType = targetArrayType.getComponentType();
        Object targetArray = Array.newInstance(targetComponentType, length);

        // Track the root mapping
        visited.put(sourceArray, targetArray);

        // Work queue for iterative processing (avoids stack overflow)
        Deque<ArrayWorkItem> queue = new ArrayDeque<>();
        queue.add(new ArrayWorkItem(sourceArray, targetArray, targetComponentType));

        while (!queue.isEmpty()) {
            ArrayWorkItem work = queue.poll();
            Object src = work.sourceArray;
            Object tgt = work.targetArray;
            Class<?> compType = work.targetComponentType;

            int len = getLength(src);
            for (int i = 0; i < len; i++) {
                Object value = getElement(src, i);
                Object convertedValue;

                if (value == null) {
                    convertedValue = null;
                } else if (value.getClass().isArray()) {
                    // Source value is an array
                    if (visited.containsKey(value)) {
                        // Cycle detected - reuse the target array we already created
                        Object existingTarget = visited.get(value);
                        if (compType.isAssignableFrom(existingTarget.getClass())) {
                            // Target type can hold the existing target - preserve cycle
                            convertedValue = existingTarget;
                        } else if (compType.isPrimitive()) {
                            // Converting reference cycle to primitive - use zero value
                            convertedValue = getPrimitiveZeroValue(compType);
                        } else {
                            // Type mismatch - try to use existing target anyway
                            convertedValue = existingTarget;
                        }
                    } else if (compType.isArray() || compType == Object.class || compType.isAssignableFrom(value.getClass())) {
                        // Target can hold arrays (either it's an array type, Object.class, or assignable)
                        // Convert nested array recursively
                        int valueLen = getLength(value);
                        Class<?> nestedComponentType = compType.isArray() ? compType.getComponentType() : Object.class;
                        Object nestedTarget = Array.newInstance(nestedComponentType, valueLen);
                        visited.put(value, nestedTarget);
                        queue.add(new ArrayWorkItem(value, nestedTarget, nestedComponentType));
                        convertedValue = nestedTarget;
                    } else {
                        // Source is array but target component doesn't support arrays
                        // Try converting via Converter (may flatten or fail)
                        convertedValue = converter.convert(value, compType);
                    }
                } else if (compType.isAssignableFrom(value.getClass())) {
                    // Direct assignment if types are compatible
                    convertedValue = value;
                } else {
                    // Convert the value to the target component type
                    convertedValue = converter.convert(value, compType);
                }

                setElement(tgt, i, convertedValue);
            }
        }

        return targetArray;
    }

    /**
     * Returns the zero-value for primitive types.
     * Used when a circular reference cannot be preserved (e.g., reference to primitive array).
     */
    private static Object getPrimitiveZeroValue(Class<?> primitiveType) {
        if (primitiveType == int.class) return 0;
        if (primitiveType == long.class) return 0L;
        if (primitiveType == double.class) return 0.0;
        if (primitiveType == float.class) return 0.0f;
        if (primitiveType == short.class) return (short) 0;
        if (primitiveType == byte.class) return (byte) 0;
        if (primitiveType == char.class) return '\0';
        if (primitiveType == boolean.class) return false;
        return null;  // Not a primitive
    }

    /**
     * Work item for iterative array processing.
     */
    private static class ArrayWorkItem {
        final Object sourceArray;
        final Object targetArray;
        final Class<?> targetComponentType;

        ArrayWorkItem(Object sourceArray, Object targetArray, Class<?> targetComponentType) {
            this.sourceArray = sourceArray;
            this.targetArray = targetArray;
            this.targetComponentType = targetComponentType;
        }
    }

    /**
     * Converts a collection to an array, handling nested collections iteratively with cycle detection.
     * Uses IdentityHashMap to detect and handle circular references without stack overflow.
     *
     * Circular reference handling:
     * - Collection to reference arrays: Cycle preserved (target array points to itself)
     * - Collection to primitive arrays: Zero-value used (0, 0.0, false, '\0')
     *
     * @param collection The source collection to convert
     * @param arrayType  The target array type
     * @param converter  The converter instance for type conversions
     * @return An array of the specified type containing the collection elements
     */
    static Object collectionToArray(Collection<?> collection, Class<?> arrayType, Converter converter) {
        // IdentityHashMap to track source collection -> target array mappings (for cycle detection)
        IdentityHashMap<Object, Object> visited = new IdentityHashMap<>();

        // Create root target array
        Class<?> componentType = arrayType.getComponentType();
        Object array = Array.newInstance(componentType, collection.size());

        // Track the root mapping
        visited.put(collection, array);

        // Work queue for iterative processing (avoids stack overflow)
        Deque<CollectionWorkItem> queue = new ArrayDeque<>();
        queue.add(new CollectionWorkItem(collection, array, componentType));

        while (!queue.isEmpty()) {
            CollectionWorkItem work = queue.poll();
            Collection<?> srcCollection = work.sourceCollection;
            Object tgtArray = work.targetArray;
            Class<?> compType = work.targetComponentType;

            int index = 0;
            for (Object item : srcCollection) {
                Object convertedValue;

                if (item == null) {
                    convertedValue = null;
                } else if (item instanceof Collection) {
                    // Source item is a collection
                    if (visited.containsKey(item)) {
                        // Cycle detected - reuse the target array we already created
                        Object existingTarget = visited.get(item);
                        if (compType.isAssignableFrom(existingTarget.getClass())) {
                            // Target type can hold the existing target - preserve cycle
                            convertedValue = existingTarget;
                        } else if (compType.isPrimitive()) {
                            // Converting reference cycle to primitive - use zero value
                            convertedValue = getPrimitiveZeroValue(compType);
                        } else {
                            // Type mismatch - try to use existing target anyway
                            convertedValue = existingTarget;
                        }
                    } else if (compType.isArray()) {
                        // Target explicitly wants arrays - convert nested collection to array
                        Collection<?> nestedCollection = (Collection<?>) item;
                        Class<?> nestedComponentType = compType.getComponentType();
                        Object nestedTarget = Array.newInstance(nestedComponentType, nestedCollection.size());
                        visited.put(item, nestedTarget);
                        queue.add(new CollectionWorkItem(nestedCollection, nestedTarget, nestedComponentType));
                        convertedValue = nestedTarget;
                    } else if (compType.isAssignableFrom(item.getClass())) {
                        // Target type can hold collections (e.g., Object.class) - keep as collection
                        convertedValue = item;
                    } else {
                        // Source is collection but target component doesn't support it
                        // Try converting via Converter (may flatten or fail)
                        convertedValue = converter.convert(item, compType);
                    }
                } else if (compType.isAssignableFrom(item.getClass())) {
                    // Direct assignment if types are compatible
                    convertedValue = item;
                } else {
                    // Convert the item to the target component type
                    convertedValue = converter.convert(item, compType);
                }

                setElement(tgtArray, index++, convertedValue);
            }
        }

        return array;
    }

    /**
     * Work item for iterative collection processing.
     */
    private static class CollectionWorkItem {
        final Collection<?> sourceCollection;
        final Object targetArray;
        final Class<?> targetComponentType;

        CollectionWorkItem(Collection<?> sourceCollection, Object targetArray, Class<?> targetComponentType) {
            this.sourceCollection = sourceCollection;
            this.targetArray = targetArray;
            this.targetComponentType = targetComponentType;
        }
    }

    /**
     * Converts an EnumSet to an array of the specified target array type.
     *
     * @param enumSet        The EnumSet to convert
     * @param targetArrayType The target array type
     * @return An array of the specified type containing the EnumSet elements
     */
    static Object enumSetToArray(EnumSet<?> enumSet, Class<?> targetArrayType) {
        Class<?> componentType = targetArrayType.getComponentType();
        Object array = Array.newInstance(componentType, enumSet.size());
        int i = 0;

        if (componentType == String.class) {
            for (Enum<?> value : enumSet) {
                setElement(array, i++, value.name());
            }
        } else if (componentType == Integer.class || componentType == int.class) {
            for (Enum<?> value : enumSet) {
                setElement(array, i++, value.ordinal());
            }
        } else if (componentType == Long.class || componentType == long.class) {
            for (Enum<?> value : enumSet) {
                setElement(array, i++, (long) value.ordinal());
            }
        } else if (componentType == Short.class || componentType == short.class) {
            for (Enum<?> value : enumSet) {
                int ordinal = value.ordinal();
                if (ordinal > Short.MAX_VALUE) {
                    throw new IllegalArgumentException("Enum ordinal too large for short: " + ordinal);
                }
                setElement(array, i++, (short) ordinal);
            }
        } else if (componentType == Byte.class || componentType == byte.class) {
            for (Enum<?> value : enumSet) {
                int ordinal = value.ordinal();
                if (ordinal > Byte.MAX_VALUE) {
                    throw new IllegalArgumentException("Enum ordinal too large for byte: " + ordinal);
                }
                setElement(array, i++, (byte) ordinal);
            }
        } else if (componentType == Class.class) {
            for (Enum<?> value : enumSet) {
                setElement(array, i++, value.getDeclaringClass());
            }
        } else {
            // Default case for other types
            for (Enum<?> value : enumSet) {
                setElement(array, i++, value);
            }
        }
        return array;
    }

    /**
     * Convert int array to java.awt.Color. Supports [r,g,b] or [r,g,b,a] format.
     * @param from int array with RGB or RGBA values
     * @param converter Converter instance
     * @return Color instance
     * @throws IllegalArgumentException if array length is not 3 or 4, or values are out of range
     */
    static Color toColor(Object from, Converter converter) {
        int[] array = (int[]) from;
        
        if (array.length < 3 || array.length > 4) {
            throw new IllegalArgumentException("Color array must have 3 (RGB) or 4 (RGBA) elements, got: " + array.length);
        }
        
        int r = array[0];
        int g = array[1];
        int b = array[2];
        
        // Validate RGB values
        if (r < 0 || r > 255 || g < 0 || g > 255 || b < 0 || b > 255) {
            throw new IllegalArgumentException("RGB values must be between 0-255, got: [" + r + ", " + g + ", " + b + "]");
        }
        
        if (array.length == 4) {
            int a = array[3];
            if (a < 0 || a > 255) {
                throw new IllegalArgumentException("Alpha value must be between 0-255, got: " + a);
            }
            return new Color(r, g, b, a);
        } else {
            return new Color(r, g, b);
        }
    }

    /**
     * Convert int array to Dimension. Array must contain exactly 2 elements: [width, height].
     * @param from int array with width and height values
     * @param converter Converter instance
     * @return Dimension instance  
     * @throws IllegalArgumentException if array length is not 2, or values are negative
     */
    static Dimension toDimension(Object from, Converter converter) {
        int[] array = (int[]) from;
        
        if (array.length != 2) {
            throw new IllegalArgumentException("Dimension array must have exactly 2 elements [width, height], got: " + array.length);
        }
        
        int width = array[0];
        int height = array[1];
        
        // Validate width and height (should be non-negative for Dimension)
        if (width < 0 || height < 0) {
            throw new IllegalArgumentException("Width and height must be non-negative, got: [" + width + ", " + height + "]");
        }
        
        return new Dimension(width, height);
    }

    /**
     * Convert int array to Point. Array must contain exactly 2 elements: [x, y].
     * @param from int array with x and y values
     * @param converter Converter instance
     * @return Point instance  
     * @throws IllegalArgumentException if array length is not 2
     */
    static Point toPoint(Object from, Converter converter) {
        int[] array = (int[]) from;
        
        if (array.length != 2) {
            throw new IllegalArgumentException("Point array must have exactly 2 elements [x, y], got: " + array.length);
        }
        
        int x = array[0];
        int y = array[1];
        
        return new Point(x, y);
    }

    /**
     * Convert int array to Rectangle. Array must contain exactly 4 elements: [x, y, width, height].
     * @param from int array with x, y, width, and height values
     * @param converter Converter instance
     * @return Rectangle instance  
     * @throws IllegalArgumentException if array length is not 4, or width/height are negative
     */
    static Rectangle toRectangle(Object from, Converter converter) {
        int[] array = (int[]) from;
        
        if (array.length != 4) {
            throw new IllegalArgumentException("Rectangle array must have exactly 4 elements [x, y, width, height], got: " + array.length);
        }
        
        int x = array[0];
        int y = array[1];
        int width = array[2];
        int height = array[3];
        
        // Validate width and height (should be non-negative for Rectangle)
        if (width < 0 || height < 0) {
            throw new IllegalArgumentException("Width and height must be non-negative, got: [width=" + width + ", height=" + height + "]");
        }
        
        return new Rectangle(x, y, width, height);
    }

    /**
     * Convert int array to Insets. Array must contain exactly 4 elements: [top, left, bottom, right].
     * @param from int array with top, left, bottom, and right values
     * @param converter Converter instance
     * @return Insets instance  
     * @throws IllegalArgumentException if array length is not 4, or values are negative
     */
    static Insets toInsets(Object from, Converter converter) {
        int[] array = (int[]) from;
        
        if (array.length != 4) {
            throw new IllegalArgumentException("Insets array must have exactly 4 elements [top, left, bottom, right], got: " + array.length);
        }
        
        int top = array[0];
        int left = array[1];
        int bottom = array[2];
        int right = array[3];
        
        // Note: Insets can have negative values (unlike Dimension/Rectangle width/height)
        // so we don't validate for non-negative values here
        
        return new Insets(top, left, bottom, right);
    }

    /**
     * Convert char[] to File.
     *
     * @param from char[] array to convert
     * @param converter Converter instance
     * @return File instance
     */
    static File charArrayToFile(Object from, Converter converter) {
        char[] array = (char[]) from;
        String path = new String(array);
        return converter.convert(path, File.class);
    }

    /**
     * Convert byte[] to File.
     *
     * @param from byte[] array to convert
     * @param converter Converter instance
     * @return File instance
     */
    static File byteArrayToFile(Object from, Converter converter) {
        byte[] array = (byte[]) from;
        String path = new String(array, StandardCharsets.UTF_8);
        return converter.convert(path, File.class);
    }

    /**
     * Convert char[] to Path.
     *
     * @param from char[] array to convert
     * @param converter Converter instance
     * @return Path instance
     */
    static Path charArrayToPath(Object from, Converter converter) {
        char[] array = (char[]) from;
        String path = new String(array);
        return converter.convert(path, Path.class);
    }

    /**
     * Convert byte[] to Path.
     *
     * @param from byte[] array to convert
     * @param converter Converter instance
     * @return Path instance
     */
    static Path byteArrayToPath(Object from, Converter converter) {
        byte[] array = (byte[]) from;
        String path = new String(array, StandardCharsets.UTF_8);
        return converter.convert(path, Path.class);
    }
}
