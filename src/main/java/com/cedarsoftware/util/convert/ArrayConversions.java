package com.cedarsoftware.util.convert;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.EnumSet;
import java.awt.Color;

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
     * Handles multidimensional arrays recursively.
     *
     * @param sourceArray     The source array to convert
     * @param targetArrayType The desired target array type
     * @param converter       The converter for element conversion
     * @return A new array of the specified target type
     */
    static Object arrayToArray(Object sourceArray, Class<?> targetArrayType, Converter converter) {
        int length = Array.getLength(sourceArray);
        Class<?> targetComponentType = targetArrayType.getComponentType();
        Object targetArray = Array.newInstance(targetComponentType, length);

        for (int i = 0; i < length; i++) {
            Object value = Array.get(sourceArray, i);
            Object convertedValue;

            if (value != null && value.getClass().isArray()) {
                // Recursively handle nested arrays
                convertedValue = arrayToArray(value, targetComponentType, converter);
            } else if (value == null || targetComponentType.isAssignableFrom(value.getClass())) {
                // Direct assignment if types are compatible or value is null
                convertedValue = value;
            } else {
                // Convert the value to the target component type
                convertedValue = converter.convert(value, targetComponentType);
            }

            Array.set(targetArray, i, convertedValue);
        }
        return targetArray;
    }

    /**
     * Converts a collection to an array, handling nested collections recursively.
     *
     * @param collection The source collection to convert
     * @param arrayType  The target array type
     * @param converter  The converter instance for type conversions
     * @return An array of the specified type containing the collection elements
     */
    static Object collectionToArray(Collection<?> collection, Class<?> arrayType, Converter converter) {
        Class<?> componentType = arrayType.getComponentType();
        Object array = Array.newInstance(componentType, collection.size());
        int index = 0;

        for (Object item : collection) {
            Object convertedValue;

            if (item instanceof Collection && componentType.isArray()) {
                // Recursively handle nested collections
                convertedValue = collectionToArray((Collection<?>) item, componentType, converter);
            } else if (item == null || componentType.isAssignableFrom(item.getClass())) {
                // Direct assignment if types are compatible or item is null
                convertedValue = item;
            } else {
                // Convert the item to the target component type
                convertedValue = converter.convert(item, componentType);
            }

            Array.set(array, index++, convertedValue);
        }
        return array;
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
                Array.set(array, i++, value.name());
            }
        } else if (componentType == Integer.class || componentType == int.class ||
                componentType == Long.class || componentType == long.class) {
            for (Enum<?> value : enumSet) {
                Array.set(array, i++, value.ordinal());
            }
        } else if (componentType == Short.class || componentType == short.class) {
            for (Enum<?> value : enumSet) {
                int ordinal = value.ordinal();
                if (ordinal > Short.MAX_VALUE) {
                    throw new IllegalArgumentException("Enum ordinal too large for short: " + ordinal);
                }
                Array.set(array, i++, (short) ordinal);
            }
        } else if (componentType == Byte.class || componentType == byte.class) {
            for (Enum<?> value : enumSet) {
                int ordinal = value.ordinal();
                if (ordinal > Byte.MAX_VALUE) {
                    throw new IllegalArgumentException("Enum ordinal too large for byte: " + ordinal);
                }
                Array.set(array, i++, (byte) ordinal);
            }
        } else if (componentType == Class.class) {
            for (Enum<?> value : enumSet) {
                Array.set(array, i++, value.getDeclaringClass());
            }
        } else {
            // Default case for other types
            for (Enum<?> value : enumSet) {
                Array.set(array, i++, value);
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
}
