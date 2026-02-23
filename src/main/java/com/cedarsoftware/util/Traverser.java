package com.cedarsoftware.util;

import java.lang.reflect.Field;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.logging.Logger;

/**
 * A Java Object Graph traverser that visits all object reference fields and invokes a
 * provided callback for each encountered object, including the root. It properly
 * detects cycles within the graph to prevent infinite loops. For each visited node,
 * complete field information including metadata is provided.
 *
 * <h2>Security Configuration</h2>
 * <p>Traverser provides configurable security controls to prevent resource exhaustion
 * and stack overflow attacks from malicious or deeply nested object graphs.
 * All security features are <strong>disabled by default</strong> for backward compatibility.</p>
 *
 * <p>Security controls can be enabled via system properties:</p>
 * <ul>
 *   <li><code>traverser.security.enabled=false</code> &mdash; Master switch for all security features</li>
 *   <li><code>traverser.max.stack.depth=0</code> &mdash; Maximum stack depth (0 = disabled)</li>
 *   <li><code>traverser.max.objects.visited=0</code> &mdash; Maximum objects visited (0 = disabled)</li>
 *   <li><code>traverser.max.collection.size=0</code> &mdash; Maximum collection size to process (0 = disabled)</li>
 *   <li><code>traverser.max.array.length=0</code> &mdash; Maximum array length to process (0 = disabled)</li>
 * </ul>
 *
 * <h3>Security Features</h3>
 * <ul>
 *   <li><b>Stack Depth Limiting:</b> Prevents stack overflow from deeply nested object graphs</li>
 *   <li><b>Object Count Limiting:</b> Prevents memory exhaustion from large object graphs</li>
 *   <li><b>Collection Size Limiting:</b> Limits processing of oversized collections and maps</li>
 *   <li><b>Array Length Limiting:</b> Limits processing of oversized arrays</li>
 * </ul>
 *
 * <h3>Usage Example</h3>
 * <pre>{@code
 * // Enable security with custom limits
 * System.setProperty("traverser.security.enabled", "true");
 * System.setProperty("traverser.max.stack.depth", "1000");
 * System.setProperty("traverser.max.objects.visited", "50000");
 *
 * // These will now enforce security controls
 * Traverser.traverse(root, classesToSkip, visit -> {
 *     // Process visit - will throw SecurityException if limits exceeded
 * });
 * }</pre>
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
 *     LOG.info("Node: " + visit.getNode());
 *     visit.getFields().forEach((field, value) -> {
 *         LOG.info("  Field: " + field.getName() +
 *             " (type: " + field.getType().getSimpleName() + ") = " + value);
 *
 *         // Access field metadata if needed
 *         if (field.isAnnotationPresent(JsonProperty.class)) {
 *             JsonProperty ann = field.getAnnotation(JsonProperty.class);
 *             LOG.info("    JSON property: " + ann.value());
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
 *         LOG.info("Visited: " + o);
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

    // Default security limits
    private static final int DEFAULT_MAX_STACK_DEPTH = 1000000;  // 1M depth for heap-based traversal
    private static final int DEFAULT_MAX_OBJECTS_VISITED = 100000;
    private static final int DEFAULT_MAX_COLLECTION_SIZE = 50000;
    private static final int DEFAULT_MAX_ARRAY_LENGTH = 50000;

    static {
        // Initialize system properties with defaults if not already set (backward compatibility)
        initializeSystemPropertyDefaults();
    }

    private static void initializeSystemPropertyDefaults() {
        // Set default values if not explicitly configured
        if (System.getProperty("traverser.max.stack.depth") == null) {
            System.setProperty("traverser.max.stack.depth", "0"); // Disabled by default
        }
        if (System.getProperty("traverser.max.objects.visited") == null) {
            System.setProperty("traverser.max.objects.visited", "0"); // Disabled by default
        }
        if (System.getProperty("traverser.max.collection.size") == null) {
            System.setProperty("traverser.max.collection.size", "0"); // Disabled by default
        }
        if (System.getProperty("traverser.max.array.length") == null) {
            System.setProperty("traverser.max.array.length", "0"); // Disabled by default
        }
    }

    // Security configuration methods

    private static boolean isSecurityEnabled() {
        return Boolean.parseBoolean(System.getProperty("traverser.security.enabled", "false"));
    }

    private static int getMaxStackDepth() {
        if (!isSecurityEnabled()) {
            return 0; // Disabled
        }
        String maxDepthProp = System.getProperty("traverser.max.stack.depth");
        if (maxDepthProp != null) {
            try {
                int value = Integer.parseInt(maxDepthProp);
                return Math.max(0, value); // 0 means disabled
            } catch (NumberFormatException e) {
                // Fall through to default
            }
        }
        return DEFAULT_MAX_STACK_DEPTH;
    }

    private static int getMaxObjectsVisited() {
        if (!isSecurityEnabled()) {
            return 0; // Disabled
        }
        String maxObjectsProp = System.getProperty("traverser.max.objects.visited");
        if (maxObjectsProp != null) {
            try {
                int value = Integer.parseInt(maxObjectsProp);
                return Math.max(0, value); // 0 means disabled
            } catch (NumberFormatException e) {
                // Fall through to default
            }
        }
        return DEFAULT_MAX_OBJECTS_VISITED;
    }

    private static int getMaxCollectionSize() {
        if (!isSecurityEnabled()) {
            return 0; // Disabled
        }
        String maxSizeProp = System.getProperty("traverser.max.collection.size");
        if (maxSizeProp != null) {
            try {
                int value = Integer.parseInt(maxSizeProp);
                return Math.max(0, value); // 0 means disabled
            } catch (NumberFormatException e) {
                // Fall through to default
            }
        }
        return DEFAULT_MAX_COLLECTION_SIZE;
    }

    private static int getMaxArrayLength() {
        if (!isSecurityEnabled()) {
            return 0; // Disabled
        }
        String maxLengthProp = System.getProperty("traverser.max.array.length");
        if (maxLengthProp != null) {
            try {
                int value = Integer.parseInt(maxLengthProp);
                return Math.max(0, value); // 0 means disabled
            } catch (NumberFormatException e) {
                // Fall through to default
            }
        }
        return DEFAULT_MAX_ARRAY_LENGTH;
    }
    
    /**
     * Represents a node visit during traversal, containing the node and its field information.
     */
    public static class NodeVisit {
        private final Object node;
        private final java.util.function.Supplier<Map<Field, Object>> fieldsSupplier;
        private Map<Field, Object> fields;

        public NodeVisit(Object node, Map<Field, Object> fields) {
            this.node = node;
            this.fieldsSupplier = null;
            // Wrap directly - the map is already freshly created by collectFieldValues
            this.fields = fields == null ? Collections.emptyMap() : Collections.unmodifiableMap(fields);
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
                // Wrap directly - the map is freshly created by collectFieldValues
                fields = (f == null) ? Collections.emptyMap() : Collections.unmodifiableMap(f);
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

    private final IdentitySet<Object> objVisited = new IdentitySet<>();
    private final IdentitySet<Object> objQueued = new IdentitySet<>();
    private final Map<Class<?>, Boolean> skipClassCache = new ClassValueMap<>();
    private final Consumer<NodeVisit> nodeVisitor;
    private final boolean collectFields;
    private int objectsVisited = 0;
    private static final String INACCESSIBLE_FIELD_VALUE = "<inaccessible>";
    private static final Predicate<Field> TRAVERSABLE_FIELD_FILTER =
            field -> ReflectionUtils.DEFAULT_FIELD_FILTER.test(field) && !field.isSynthetic();

    // Helper class to track object and its depth in heap-based traversal
    private static class TraversalNode {
        final Object object;
        final int depth;

        TraversalNode(Object object, int depth) {
            this.object = object;
            this.depth = depth;
        }
    }

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

    public static void traverse(Object root, Consumer<NodeVisit> visitor,
                                Set<Class<?>> classesToSkip, boolean collectFields) {
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

        Deque<TraversalNode> stack = new ArrayDeque<>();
        stack.add(new TraversalNode(root, 1));
        objQueued.add(root);
        objectsVisited = 0;

        // Hoist ALL security limits: they don't change during traversal
        final int maxStackDepth = getMaxStackDepth();
        final int maxObjectsVisited = getMaxObjectsVisited();
        final int maxCollectionSize = getMaxCollectionSize();
        final int maxArrayLength = getMaxArrayLength();

        while (!stack.isEmpty()) {
            TraversalNode node = stack.pollFirst();
            Object current = node.object;
            int currentDepth = node.depth;
            objQueued.remove(current);

            if (current == null || objVisited.contains(current)) {
                continue;
            }

            Class<?> clazz = current.getClass();
            if (shouldSkipClass(clazz, classesToSkip)) {
                continue;
            }

            // Security: Check stack depth limit only for nodes that will actually be visited
            if (maxStackDepth > 0 && currentDepth > maxStackDepth) {
                throw new SecurityException("Stack depth exceeded limit (max " + maxStackDepth + "): " + currentDepth);
            }

            objVisited.add(current);
            objectsVisited++;

            // Security: Check objects visited limit
            if (maxObjectsVisited > 0 && objectsVisited > maxObjectsVisited) {
                throw new SecurityException("Objects visited exceeded limit (max " +
                        maxObjectsVisited + "): " + objectsVisited);
            }

            if (clazz.isArray()) {
                if (collectFields) {
                    nodeVisitor.accept(new NodeVisit(current, collectFieldValues(current)));
                } else {
                    nodeVisitor.accept(new NodeVisit(current, () -> collectFieldValues(current)));
                }
                processArray(stack, current, classesToSkip, currentDepth, maxArrayLength, maxObjectsVisited);
            } else if (current instanceof Collection) {
                if (collectFields) {
                    nodeVisitor.accept(new NodeVisit(current, collectFieldValues(current)));
                } else {
                    nodeVisitor.accept(new NodeVisit(current, () -> collectFieldValues(current)));
                }
                processCollection(stack, (Collection<?>) current, classesToSkip,
                        currentDepth, maxCollectionSize, maxObjectsVisited);
            } else if (current instanceof Map) {
                if (collectFields) {
                    nodeVisitor.accept(new NodeVisit(current, collectFieldValues(current)));
                } else {
                    nodeVisitor.accept(new NodeVisit(current, () -> collectFieldValues(current)));
                }
                processMap(stack, (Map<?, ?>) current, classesToSkip,
                        currentDepth, maxCollectionSize, maxObjectsVisited);
            } else {
                if (collectFields) {
                    Map<Field, Object> fieldValues = collectFieldValuesAndProcess(
                            stack, current, classesToSkip, currentDepth, maxObjectsVisited);
                    nodeVisitor.accept(new NodeVisit(current, fieldValues));
                } else {
                    nodeVisitor.accept(new NodeVisit(current, () -> collectFieldValues(current)));
                    processFields(stack, current, classesToSkip, currentDepth, maxObjectsVisited);
                }
            }
        }
    }

    private Map<Field, Object> collectFieldValues(Object obj) {
        Map<Field, Object> fieldValues = new HashMap<>();
        Collection<Field> allFields = getTraversableFields(obj.getClass());

        for (Field field : allFields) {
            try {
                fieldValues.put(field, field.get(obj));
            } catch (Exception e) {
                // Field cannot be accessed - JVM/SecurityManager is in control
                fieldValues.put(field, INACCESSIBLE_FIELD_VALUE);
            }
        }
        return fieldValues;
    }

    private Map<Field, Object> collectFieldValuesAndProcess(Deque<TraversalNode> stack,
                                                            Object object,
                                                            Set<Class<?>> classesToSkip,
                                                            int depth,
                                                            int maxObjectsVisited) {
        Map<Field, Object> fieldValues = new HashMap<>();
        Collection<Field> fields = getTraversableFields(object.getClass());

        for (Field field : fields) {
            boolean isPrimitive = field.getType().isPrimitive();
            Object value;
            try {
                value = field.get(object);
                fieldValues.put(field, value);
            } catch (Exception e) {
                fieldValues.put(field, INACCESSIBLE_FIELD_VALUE);
                continue;
            }
            if (!isPrimitive) {
                enqueueNode(stack, value, classesToSkip, depth + 1, maxObjectsVisited);
            }
        }
        return fieldValues;
    }

    private Collection<Field> getTraversableFields(Class<?> clazz) {
        return ReflectionUtils.getAllDeclaredFields(clazz, TRAVERSABLE_FIELD_FILTER);
    }

    private boolean shouldSkipClass(Class<?> clazz, Set<Class<?>> classesToSkip) {
        if (classesToSkip == null) {
            return false;
        }
        return skipClassCache.computeIfAbsent(clazz, c -> {
            for (Class<?> skipClass : classesToSkip) {
                if (skipClass != null && skipClass.isAssignableFrom(c)) {
                    return true;
                }
            }
            return false;
        });
    }


    private void processCollection(Deque<TraversalNode> stack, Collection<?> collection,
                                   Set<Class<?>> classesToSkip, int depth, int maxCollectionSize,
                                   int maxObjectsVisited) {
        // Security: Validate collection size
        int size = collection.size();
        if (maxCollectionSize > 0 && size > maxCollectionSize) {
            throw new SecurityException("Collection size exceeded limit (max " + maxCollectionSize + "): " + size);
        }

        for (Object element : collection) {
            enqueueNode(stack, element, classesToSkip, depth + 1, maxObjectsVisited);
        }
    }

    private void processMap(Deque<TraversalNode> stack, Map<?, ?> map, Set<Class<?>> classesToSkip, int depth,
                            int maxCollectionSize, int maxObjectsVisited) {
        // Security: Validate map size
        int size = map.size();
        if (maxCollectionSize > 0 && size > maxCollectionSize) {
            throw new SecurityException("Collection size exceeded limit (max " + maxCollectionSize + "): " + size);
        }

        for (Map.Entry<?, ?> entry : map.entrySet()) {
            Object key = entry.getKey();
            Object value = entry.getValue();

            enqueueNode(stack, key, classesToSkip, depth + 1, maxObjectsVisited);
            enqueueNode(stack, value, classesToSkip, depth + 1, maxObjectsVisited);
        }
    }

    private void processFields(Deque<TraversalNode> stack, Object object,
                               Set<Class<?>> classesToSkip, int depth, int maxObjectsVisited) {
        Collection<Field> fields = getTraversableFields(object.getClass());

        for (Field field : fields) {
            if (!field.getType().isPrimitive()) {
                Object value;
                try {
                    value = field.get(object);
                } catch (Exception e) {
                    // Field cannot be accessed - JVM/SecurityManager is in control
                    // This is ok, continue gracefully (just don't traverse into this field)
                    continue;
                }
                enqueueNode(stack, value, classesToSkip, depth + 1, maxObjectsVisited);
            }
        }
    }

    private void processArray(Deque<TraversalNode> stack, Object array, Set<Class<?>> classesToSkip, int depth,
                              int maxArrayLength, int maxObjectsVisited) {
        int length = ArrayUtilities.getLength(array);
        Class<?> componentType = array.getClass().getComponentType();

        if (!componentType.isPrimitive()) {
            // Security: Validate array length only for object arrays
            if (maxArrayLength > 0 && length > maxArrayLength) {
                throw new SecurityException("Array length exceeded limit (max " + maxArrayLength + "): " + length);
            }

            for (int i = 0; i < length; i++) {
                Object element = ArrayUtilities.getElement(array, i);
                enqueueNode(stack, element, classesToSkip, depth + 1, maxObjectsVisited);
            }
        }
        // Primitive arrays are not traversed into, so no validation needed
    }

    private void enqueueNode(Deque<TraversalNode> stack, Object value,
                             Set<Class<?>> classesToSkip, int depth, int maxObjectsVisited) {
        if (value == null || objVisited.contains(value) || objQueued.contains(value)) {
            return;
        }
        if (shouldSkipClass(value.getClass(), classesToSkip)) {
            return;
        }

        // Security: prevent unbounded frontier expansion when object-visit limits are active.
        if (maxObjectsVisited > 0 && objectsVisited + objQueued.size() >= maxObjectsVisited) {
            int projected = objectsVisited + objQueued.size() + 1;
            throw new SecurityException("Objects visited exceeded limit (max " +
                    maxObjectsVisited + "): " + projected);
        }

        stack.addFirst(new TraversalNode(value, depth));
        objQueued.add(value);
    }
}
