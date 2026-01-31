package com.cedarsoftware.util.convert;

import java.util.Collection;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Map;

import com.cedarsoftware.util.ArrayUtilities;

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

    /**
     * Convert a String to an Enum constant by name.
     * The string must exactly match an enum constant name (case-sensitive).
     *
     * @param from      the String value (enum constant name)
     * @param converter the Converter instance (used for options)
     * @param target    the target Enum class
     * @return the corresponding Enum constant
     * @throws IllegalArgumentException if the string doesn't match any enum constant,
     *         target is abstract Enum.class, or the string exceeds maxEnumNameLength
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    static Enum<?> stringToEnum(Object from, Converter converter, Class<?> target) {
        if (target == Enum.class) {
            throw new IllegalArgumentException("Cannot convert String to abstract Enum.class - a concrete enum type is required");
        }
        String enumName = ((String) from).trim();
        if (enumName.isEmpty()) {
            throw new IllegalArgumentException("Cannot convert empty String to enum " + target.getName());
        }
        int maxLength = converter.getOptions().getMaxEnumNameLength();
        if (enumName.length() > maxLength) {
            throw new IllegalArgumentException("Enum name too long (" + enumName.length() + " chars, max " + maxLength + ") for enum " + target.getName());
        }
        return Enum.valueOf((Class<Enum>) target, enumName);
    }

    /**
     * Convert an int/Integer (ordinal) to an Enum constant.
     * This is the base conversion for ordinal-to-enum.
     *
     * @param from      the int/Integer value (enum ordinal)
     * @param converter the Converter instance (unused)
     * @param target    the target Enum class
     * @return the corresponding Enum constant
     * @throws IllegalArgumentException if the ordinal is out of range or target is abstract Enum.class
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    static Enum<?> intToEnum(Object from, Converter converter, Class<?> target) {
        Enum<?>[] enumConstants = ((Class<Enum>) target).getEnumConstants();

        if (enumConstants == null) {
            throw new IllegalArgumentException("Cannot convert " + from.getClass().getSimpleName() + " to abstract Enum.class - a concrete enum type is required");
        }

        int ordinal = ((Number) from).intValue();
        if (ordinal < 0 || ordinal >= enumConstants.length) {
            throw new IllegalArgumentException(
                    String.format("Invalid ordinal value %d for enum %s. Must be between 0 and %d",
                            ordinal, target.getName(), enumConstants.length - 1));
        }
        return enumConstants[ordinal];
    }

    /**
     * Convert any Number (ordinal) to an Enum constant.
     * Handles all Number subclasses (BigInteger, BigDecimal, AtomicLong, etc.)
     * by converting to int first, then delegating to intToEnum.
     *
     * @param from      the Number value (enum ordinal)
     * @param converter the Converter instance used to convert Number to int
     * @param target    the target Enum class
     * @return the corresponding Enum constant
     * @throws IllegalArgumentException if the ordinal is out of range
     */
    static Enum<?> numberToEnum(Object from, Converter converter, Class<?> target) {
        int ordinal = converter.convert(from, int.class);
        return intToEnum(ordinal, converter, target);
    }

    /**
     * Convert an Enum constant to its ordinal value.
     *
     * @param from      the Enum constant
     * @param converter the Converter instance (unused)
     * @return the ordinal value of the enum constant
     */
    static int enumToOrdinal(Object from, Converter converter) {
        return ((Enum<?>) from).ordinal();
    }

    static Map<String, Object> toMap(Object from, Converter converter) {
        Enum<?> enumInstance = (Enum<?>) from;
        Map<String, Object> target = new LinkedHashMap<>();
        target.put("name", enumInstance.name());
        return target;
    }

    @SuppressWarnings("unchecked")
    static <T extends Enum<T>> EnumSet<T> toEnumSet(Object from, Class<?> target) {
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
        int length = ArrayUtilities.getLength(array);
        T[] enumConstants = null;  // Lazy initialization

        for (int i = 0; i < length; i++) {
            Object element = ArrayUtilities.getElement(array, i);
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
