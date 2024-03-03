package com.cedarsoftware.util;

import java.util.Map;

public class Convention {

    /**
     * statically accessed class
     */
    private Convention() {
    }

    /**
     * Throws an exception if null
     *
     * @param value   object to check if null
     * @param message message to use when thrown
     * @throws IllegalArgumentException if the string passed in is null or empty
     */
    public static void throwIfNull(Object value, String message) {
        if (value == null) {
            throw new IllegalArgumentException(message);
        }
    }

    /**
     * Throws an exception if null or empty
     *
     * @param value   string to check
     * @param message message to use when thrown
     * @throws IllegalArgumentException if the string passed in is null or empty
     */
    public static void throwIfNullOrEmpty(String value, String message) {
        if (StringUtilities.isEmpty(value)) {
            throw new IllegalArgumentException(message);
        }
    }

    public static void throwIfClassNotFound(String fullyQualifiedClassName, ClassLoader loader) {
        throwIfNullOrEmpty(fullyQualifiedClassName, "fully qualified ClassName cannot be null or empty");
        throwIfNull(loader, "loader cannot be null");

        Class<?> c = ClassUtilities.forName(fullyQualifiedClassName, loader);
        if (c == null) {
            throw new IllegalArgumentException("Unknown class: " + fullyQualifiedClassName + " was not found.");
        }
    }

    public static <K, V> void throwIfKeyExists(Map<K, V> map, K key, String message) {
        throwIfNull(map, "map cannot be null");
        throwIfNull(key, "key cannot be null");

        if (map.containsKey(key)) {
            throw new IllegalArgumentException(message);
        }
    }

    /**
     * Throws an exception if the logic is false.
     *
     * @param logic   test to see if we need to throw the exception.
     * @param message to include in the exception explaining why the the assertion failed
     */
    public static void throwIfFalse(boolean logic, String message) {
        if (!logic) {
            throw new IllegalArgumentException(message);
        }
    }
}
