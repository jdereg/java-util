package com.cedarsoftware.util;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

/**
 * A Java Object Graph traverser that visits all object reference fields and invokes a
 * provided callback for each encountered object, including the root. It properly
 * detects cycles within the graph to prevent infinite loops.
 *
 * <p>
 * <b>Usage Examples:</b>
 * </p>
 *
 * <p><b>Using the Old API with {@link Traverser.Visitor}:</b></p>
 * <pre>{@code
 * // Define a visitor that processes each object
 * Traverser.Visitor visitor = new Traverser.Visitor() {
 *     @Override
 *     public void process(Object o) {
 *         System.out.println("Visited: " + o);
 *     }
 * };
 *
 * // Create an object graph and traverse it
 * SomeClass root = new SomeClass();
 * Traverser.traverse(root, visitor);
 * }</pre>
 *
 * <p><b>Using the New API with Lambda and {@link Set} of classes to skip:</b></p>
 * <pre>{@code
 * // Define classes to skip
 * Set<Class<?>> classesToSkip = new HashSet<>();
 * classesToSkip.add(String.class);
 * classesToSkip.add(Integer.class);
 *
 * // Traverse the object graph with a lambda callback
 * Traverser.traverse(root, classesToSkip, o -> System.out.println("Visited: " + o));
 * }</pre>
 *
 * <p>
 * <b>Thread Safety:</b> This class is <i>not</i> thread-safe. If multiple threads access
 * a {@code Traverser} instance concurrently, external synchronization is required.
 * </p>
 *
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
 *
 * @see ReflectionUtils#getAllDeclaredFields(Class)
 */
public class Traverser {
    /**
     * A visitor interface to process each object encountered during traversal.
     * <p>
     * <b>Note:</b> This interface is deprecated in favor of using lambda expressions
     * with the new {@code traverse} method.
     * </p>
     *
     * @deprecated Use lambda expressions with {@link #traverse(Object, Set, Consumer)} instead.
     */
    @Deprecated
    @FunctionalInterface
    public interface Visitor {
        /**
         * Processes an encountered object.
         *
         * @param o the object to process
         */
        void process(Object o);
    }

    // Tracks visited objects to prevent cycles. Uses identity comparison.
    private final Set<Object> objVisited = Collections.newSetFromMap(new IdentityHashMap<>());

    /**
     * Traverses the object graph starting from the provided root object.
     * <p>
     * This method uses the new API with a {@code Set<Class<?>>} and a lambda expression.
     * </p>
     *
     * @param root            the root object to start traversal
     * @param classesToSkip   a {@code Set} of {@code Class} objects to skip during traversal; may be {@code null}
     * @param objectProcessor a lambda expression to process each encountered object
     */
    public static void traverse(Object root, Set<Class<?>> classesToSkip, Consumer<Object> objectProcessor) {
        if (objectProcessor == null) {
            throw new IllegalArgumentException("objectProcessor cannot be null");
        }
        Traverser traverser = new Traverser();
        traverser.walk(root, classesToSkip, objectProcessor);
    }

    /**
     * Traverses the object graph starting from the provided root object.
     *
     * @param root    the root object to start traversal
     * @param visitor the visitor to process each encountered object
     *
     * @deprecated Use {@link #traverse(Object, Set, Consumer)} instead with a lambda expression.
     */
    @Deprecated
    public static void traverse(Object root, Visitor visitor) {
        traverse(root, (Set<Class<?>>) null, visitor == null ? null : visitor::process);
    }

    /**
     * Traverses the object graph starting from the provided root object, skipping specified classes.
     *
     * @param root    the root object to start traversal
     * @param skip    an array of {@code Class} objects to skip during traversal; may be {@code null}
     * @param visitor the visitor to process each encountered object
     *
     * @deprecated Use {@link #traverse(Object, Set, Consumer)} instead with a {@code Set<Class<?>>} and a lambda expression.
     */
    @Deprecated
    public static void traverse(Object root, Class<?>[] skip, Visitor visitor) {
        Set<Class<?>> classesToSkip = (skip == null) ? null : new HashSet<>(Arrays.asList(skip));
        traverse(root, classesToSkip, visitor == null ? null : visitor::process);
    }

    /**
     * Traverses the object graph referenced by the provided root.
     *
     * @param root            the root object to start traversal
     * @param classesToSkip   a {@code Set} of {@code Class} objects to skip during traversal; may be {@code null}
     * @param objectProcessor a lambda expression to process each encountered object
     */
    private void walk(Object root, Set<Class<?>> classesToSkip, Consumer<Object> objectProcessor) {
        if (root == null) {
            return;
        }

        Deque<Object> stack = new LinkedList<>();
        stack.add(root);

        while (!stack.isEmpty()) {
            Object current = stack.pollFirst();

            if (current == null || objVisited.contains(current)) {
                continue;
            }

            Class<?> clazz = current.getClass();

            if (shouldSkipClass(clazz, classesToSkip)) {
                continue;
            }

            objVisited.add(current);
            objectProcessor.accept(current);

            if (clazz.isArray()) {
                processArray(stack, current, classesToSkip);
            } else if (current instanceof Collection) {
                processCollection(stack, (Collection<?>) current);
            } else if (current instanceof Map) {
                processMap(stack, (Map<?, ?>) current);
            } else {
                processFields(stack, current, classesToSkip);
            }
        }
    }

    /**
     * Determines whether the specified class should be skipped based on the provided skip set.
     *
     * @param clazz          the class to check
     * @param classesToSkip  a {@code Set} of {@code Class} objects to skip; may be {@code null}
     * @return {@code true} if the class should be skipped; {@code false} otherwise
     */
    private boolean shouldSkipClass(Class<?> clazz, Set<Class<?>> classesToSkip) {
        if (classesToSkip == null) {
            return false;
        }

        for (Class<?> skipClass : classesToSkip) {
            if (skipClass.isAssignableFrom(clazz)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Processes array elements, adding non-primitive and non-skipped elements to the stack.
     *
     * @param stack           the traversal stack
     * @param array           the array object to process
     * @param classesToSkip   a {@code Set} of {@code Class} objects to skip during traversal; may be {@code null}
     */
    private void processArray(Deque<Object> stack, Object array, Set<Class<?>> classesToSkip) {
        int length = Array.getLength(array);
        Class<?> componentType = array.getClass().getComponentType();

        if (!componentType.isPrimitive()) { // Skip primitive arrays
            for (int i = 0; i < length; i++) {
                Object element = Array.get(array, i);
                if (element != null && !shouldSkipClass(element.getClass(), classesToSkip)) {
                    stack.addFirst(element);
                }
            }
        }
    }

    /**
     * Processes elements of a {@link Collection}, adding non-primitive and non-skipped elements to the stack.
     *
     * @param stack        the traversal stack
     * @param collection   the collection to process
     */
    private void processCollection(Deque<Object> stack, Collection<?> collection) {
        for (Object element : collection) {
            if (element != null && !element.getClass().isPrimitive()) {
                stack.addFirst(element);
            }
        }
    }

    /**
     * Processes entries of a {@link Map}, adding non-primitive keys and values to the stack.
     *
     * @param stack    the traversal stack
     * @param map      the map to process
     */
    private void processMap(Deque<Object> stack, Map<?, ?> map) {
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            Object key = entry.getKey();
            Object value = entry.getValue();

            if (key != null && !key.getClass().isPrimitive()) {
                stack.addFirst(key);
            }
            if (value != null && !value.getClass().isPrimitive()) {
                stack.addFirst(value);
            }
        }
    }

    /**
     * Processes the fields of an object, adding non-primitive field values to the stack.
     *
     * @param stack           the traversal stack
     * @param object          the object whose fields are to be processed
     * @param classesToSkip   a {@code Set} of {@code Class} objects to skip during traversal; may be {@code null}
     */
    private void processFields(Deque<Object> stack, Object object, Set<Class<?>> classesToSkip) {
        Collection<Field> fields = ReflectionUtils.getAllDeclaredFields(object.getClass());

        for (Field field : fields) {
            Class<?> fieldType = field.getType();

            if (!fieldType.isPrimitive()) { // Only process reference fields
                try {
                    Object value = field.get(object);
                    if (value != null && !shouldSkipClass(value.getClass(), classesToSkip)) {
                        stack.addFirst(value);
                    }
                } catch (IllegalAccessException e) {
                    // Optionally log inaccessible fields
                    // For now, we'll ignore inaccessible fields
                }
            }
        }
    }
}