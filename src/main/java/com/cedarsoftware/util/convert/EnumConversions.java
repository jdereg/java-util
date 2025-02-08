package com.cedarsoftware.util.convert;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Map;

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
final class EnumConversions {

    private EnumConversions() {}

    static Map<String, Object> toMap(Object from, Converter converter) {
        Enum<?> enumInstance = (Enum<?>) from;
        Map<String, Object> target = new LinkedHashMap<>();
        target.put("name", enumInstance.name());
        return target;
    }

    @SuppressWarnings("unchecked")
    static <T extends Enum<T>> EnumSet<T> toEnumSet(Object from, Converter converter, Class<?> target) {
        if (!target.isEnum()) {
            throw new IllegalArgumentException("target type " + target.getName() + " must be an Enum, which instructs the EnumSet type to create.");
        }

        Class<T> enumClass = (Class<T>) target;
        EnumSet<T> enumSet = EnumSet.noneOf(enumClass);

        if (from instanceof Collection) {
            processElements((Collection<?>) from, enumSet, enumClass);
        } else if (from.getClass().isArray()) {
            processArrayElements(from, enumSet, enumClass);
        } else {
            throw new IllegalArgumentException("Source must be a Collection or Array, found: " + from.getClass().getName());
        }

        return enumSet;
    }

    private static <T extends Enum<T>> void processArrayElements(Object array, EnumSet<T> enumSet, Class<T> enumClass) {
        int length = Array.getLength(array);
        T[] enumConstants = null;  // Lazy initialization

        for (int i = 0; i < length; i++) {
            Object element = Array.get(array, i);
            if (element != null) {
                enumConstants = processElement(element, enumSet, enumClass, enumConstants);
            }
        }
    }

    private static <T extends Enum<T>> void processElements(Collection<?> collection, EnumSet<T> enumSet, Class<T> enumClass) {
        T[] enumConstants = null;  // Lazy initialization

        for (Object element : collection) {
            if (element != null) {
                enumConstants = processElement(element, enumSet, enumClass, enumConstants);
            }
        }
    }

    private static <T extends Enum<T>> T[] processElement(Object element, EnumSet<T> enumSet, Class<T> enumClass, T[] enumConstants) {
        if (enumClass.isInstance(element)) {
            enumSet.add(enumClass.cast(element));
        } else if (element instanceof String) {
            enumSet.add(Enum.valueOf(enumClass, (String) element));
        } else if (element instanceof Number) {
            // Lazy load enum constants when first numeric value is encountered
            if (enumConstants == null) {
                enumConstants = enumClass.getEnumConstants();
            }

            int ordinal = ((Number) element).intValue();

            if (ordinal < 0 || ordinal >= enumConstants.length) {
                throw new IllegalArgumentException(
                        String.format("Invalid ordinal value %d for enum %s. Must be between 0 and %d",
                                ordinal, enumClass.getName(), enumConstants.length - 1));
            }
            enumSet.add(enumConstants[ordinal]);
        } else {
            throw new IllegalArgumentException(element.getClass().getName() +
                    " found in source collection/array is not convertible to " + enumClass.getName());
        }

        return enumConstants;
    }
}
