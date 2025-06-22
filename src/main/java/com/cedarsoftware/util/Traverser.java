package com.cedarsoftware.util;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.ArrayDeque;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import com.cedarsoftware.util.LoggingConfig;

/**
 * A Java Object Graph traverser that visits all object reference fields and invokes a
 * provided callback for each encountered object, including the root. It properly
 * detects cycles within the graph to prevent infinite loops. For each visited node,
 * complete field information including metadata is provided.
 *
 * <p>
 * <b>Usage Examples:</b>
 * </p>
 *
 * <p><b>Using the Modern API (Recommended):</b></p>
 * <pre>{@code
 * // Define classes to skip (optional)
 * Set<Class<?>> classesToSkip = new HashSet<>();
 * classesToSkip.add(String.class);
 *
 * // Traverse with full node information
 * Traverser.traverse(root, classesToSkip, visit -> {
 *     System.out.println("Node: " + visit.getNode());
 *     visit.getFields().forEach((field, value) -> {
 *         System.out.println("  Field: " + field.getName() +
 *             " (type: " + field.getType().getSimpleName() + ") = " + value);
 *
 *         // Access field metadata if needed
 *         if (field.isAnnotationPresent(JsonProperty.class)) {
 *             JsonProperty ann = field.getAnnotation(JsonProperty.class);
 *             System.out.println("    JSON property: " + ann.value());
 *         }
 *     });
 * });
 * }</pre>
 *
 * <p><b>Using the Legacy API (Deprecated):</b></p>
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
 */
public class Traverser {

    private static final Logger LOG = Logger.getLogger(Traverser.class.getName());
    static { LoggingConfig.init(); }

    /**
     * Represents a node visit during traversal, containing the node and its field information.
     */
    public static class NodeVisit {
        private final Object node;
        private final java.util.function.Supplier<Map<Field, Object>> fieldsSupplier;
        private Map<Field, Object> fields;

        public NodeVisit(Object node, Map<Field, Object> fields) {
            this(node, () -> fields == null ? Collections.emptyMap() : fields);
            this.fields = Collections.unmodifiableMap(new HashMap<>(fields));
        }

        public NodeVisit(Object node, java.util.function.Supplier<Map<Field, Object>> supplier) {
            this.node = node;
            this.fieldsSupplier = supplier;
        }

        /**
         * @return The object (node) being visited
         */
        public Object getNode() { return node; }

        /**
         * @return Unmodifiable map of fields to their values, including metadata about each field
         */
        public Map<Field, Object> getFields() {
            if (fields == null) {
                Map<Field, Object> f = fieldsSupplier == null ? Collections.emptyMap() : fieldsSupplier.get();
                if (f == null) {
                    f = Collections.emptyMap();
                }
                fields = Collections.unmodifiableMap(new HashMap<>(f));
            }
            return fields;
        }

        /**
         * @return The class of the node being visited
         */
        public Class<?> getNodeClass() { return node.getClass(); }
    }

    /**
     * A visitor interface to process each object encountered during traversal.
     * <p>
     * <b>Note:</b> This interface is deprecated in favor of using {@link Consumer<NodeVisit>}
     * with the new {@code traverse} method.
     * </p>
     *
     * @deprecated Use {@link #traverse(Object, Set, Consumer)} instead.
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

    private final Set<Object> objVisited = Collections.newSetFromMap(new IdentityHashMap<>());
    private final Consumer<NodeVisit> nodeVisitor;
    private final boolean collectFields;

    private Traverser(Consumer<NodeVisit> nodeVisitor, boolean collectFields) {
        this.nodeVisitor = nodeVisitor;
        this.collectFields = collectFields;
    }

    /**
     * Traverses the object graph with complete node visiting capabilities.
     *
     * @param root          the root object to start traversal
     * @param classesToSkip classes to skip during traversal (can be null)
     * @param visitor       visitor that receives detailed node information
     */
    public static void traverse(Object root, Consumer<NodeVisit> visitor, Set<Class<?>> classesToSkip) {
        traverse(root, visitor, classesToSkip, true);
    }

    public static void traverse(Object root, Consumer<NodeVisit> visitor, Set<Class<?>> classesToSkip, boolean collectFields) {
        if (visitor == null) {
            throw new IllegalArgumentException("visitor cannot be null");
        }
        Traverser traverser = new Traverser(visitor, collectFields);
        traverser.walk(root, classesToSkip);
    }

    private static void traverse(Object root, Set<Class<?>> classesToSkip, Consumer<Object> objectProcessor) {
        if (objectProcessor == null) {
            throw new IllegalArgumentException("objectProcessor cannot be null");
        }
        traverse(root, visit -> objectProcessor.accept(visit.getNode()), classesToSkip, true);
    }

    /**
     * @deprecated Use {@link #traverse(Object, Set, Consumer)} instead.
     */
    @Deprecated
    public static void traverse(Object root, Visitor visitor) {
        if (visitor == null) {
            throw new IllegalArgumentException("visitor cannot be null");
        }
        traverse(root, visit -> visitor.process(visit.getNode()), null, true);
    }

    /**
     * @deprecated Use {@link #traverse(Object, Set, Consumer)} instead.
     */
    @Deprecated
    public static void traverse(Object root, Class<?>[] skip, Visitor visitor) {
        if (visitor == null) {
            throw new IllegalArgumentException("visitor cannot be null");
        }
        Set<Class<?>> classesToSkip = (skip == null) ? null : new HashSet<>(Arrays.asList(skip));
        traverse(root, visit -> visitor.process(visit.getNode()), classesToSkip, true);
    }

    private void walk(Object root, Set<Class<?>> classesToSkip) {
        if (root == null) {
            return;
        }

        Deque<Object> stack = new ArrayDeque<>();
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

            if (collectFields) {
                nodeVisitor.accept(new NodeVisit(current, collectFields(current)));
            } else {
                nodeVisitor.accept(new NodeVisit(current, () -> collectFields(current)));
            }

            if (clazz.isArray()) {
                processArray(stack, current, classesToSkip);
            } else if (current instanceof Collection) {
                processCollection(stack, (Collection<?>) current, classesToSkip);
            } else if (current instanceof Map) {
                processMap(stack, (Map<?, ?>) current, classesToSkip);
            } else {
                processFields(stack, current, classesToSkip);
            }
        }
    }

    private Map<Field, Object> collectFields(Object obj) {
        Map<Field, Object> fields = new HashMap<>();
        Collection<Field> allFields = ReflectionUtils.getAllDeclaredFields(
                obj.getClass(),
                field -> ReflectionUtils.DEFAULT_FIELD_FILTER.test(field) && !field.isSynthetic());

        for (Field field : allFields) {
            try {
                fields.put(field, field.get(obj));
            } catch (IllegalAccessException e) {
                ClassUtilities.logFieldAccessIssue(field, e);
                fields.put(field, "<inaccessible>");
            }
        }
        return fields;
    }

    private boolean shouldSkipClass(Class<?> clazz, Set<Class<?>> classesToSkip) {
        if (classesToSkip == null) {
            return false;
        }
        for (Class<?> skipClass : classesToSkip) {
            if (skipClass != null && skipClass.isAssignableFrom(clazz)) {
                return true;
            }
        }
        return false;
    }

    private void processArray(Deque<Object> stack, Object array, Set<Class<?>> classesToSkip) {
        int length = Array.getLength(array);
        Class<?> componentType = array.getClass().getComponentType();

        if (!componentType.isPrimitive()) {
            for (int i = 0; i < length; i++) {
                Object element = Array.get(array, i);
                if (element != null && !shouldSkipClass(element.getClass(), classesToSkip)) {
                    stack.addFirst(element);
                }
            }
        }
    }

    private void processCollection(Deque<Object> stack, Collection<?> collection, Set<Class<?>> classesToSkip) {
        for (Object element : collection) {
            if (element != null && !shouldSkipClass(element.getClass(), classesToSkip)) {
                stack.addFirst(element);
            }
        }
    }

    private void processMap(Deque<Object> stack, Map<?, ?> map, Set<Class<?>> classesToSkip) {
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            Object key = entry.getKey();
            Object value = entry.getValue();

            if (key != null && !shouldSkipClass(key.getClass(), classesToSkip)) {
                stack.addFirst(key);
            }
            if (value != null && !shouldSkipClass(value.getClass(), classesToSkip)) {
                stack.addFirst(value);
            }
        }
    }

    private void processFields(Deque<Object> stack, Object object, Set<Class<?>> classesToSkip) {
        Collection<Field> fields = ReflectionUtils.getAllDeclaredFields(
                object.getClass(),
                field -> ReflectionUtils.DEFAULT_FIELD_FILTER.test(field) && !field.isSynthetic());
        for (Field field : fields) {
            if (!field.getType().isPrimitive()) {
                try {
                    Object value = field.get(object);
                    if (value != null && !shouldSkipClass(value.getClass(), classesToSkip)) {
                        stack.addFirst(value);
                    }
                } catch (IllegalAccessException ignored) {
                }
            }
        }
    }
}
