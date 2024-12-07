package com.cedarsoftware.util.convert;

import java.lang.reflect.Array;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;
import java.util.Stack;
import java.util.TreeSet;
import java.util.Vector;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.SynchronousQueue;
import java.util.function.Function;

import com.cedarsoftware.util.CaseInsensitiveSet;
import com.cedarsoftware.util.CompactCIHashSet;
import com.cedarsoftware.util.CompactCILinkedSet;
import com.cedarsoftware.util.CompactLinkedSet;
import com.cedarsoftware.util.CompactSet;
import com.cedarsoftware.util.ConcurrentList;
import com.cedarsoftware.util.ConcurrentNavigableSetNullSafe;
import com.cedarsoftware.util.ConcurrentSet;

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
final class CollectionConversions {
    private CollectionConversions() { }

    // Static helper class for creating collections
    static final class CollectionFactory {
        static final Map<Class<?>, Function<Integer, Collection<?>>> COLLECTION_FACTORIES = new LinkedHashMap<>();
        private static final Map<Class<?>, Function<Integer, Collection<?>>> FACTORY_CACHE = new ConcurrentHashMap<>();

        static {
            // Set implementations (most specific to most general)
            COLLECTION_FACTORIES.put(CaseInsensitiveSet.class, size -> new CaseInsensitiveSet<>());
            COLLECTION_FACTORIES.put(CompactLinkedSet.class, size -> new CompactLinkedSet<>());
            COLLECTION_FACTORIES.put(CompactCIHashSet.class, size -> new CompactCIHashSet<>());
            COLLECTION_FACTORIES.put(CompactCILinkedSet.class, size -> new CompactCILinkedSet<>());
            COLLECTION_FACTORIES.put(CompactSet.class, size -> new CompactSet<>());
            COLLECTION_FACTORIES.put(ConcurrentSkipListSet.class, size -> new ConcurrentSkipListSet<>());
            COLLECTION_FACTORIES.put(ConcurrentSet.class, size -> new ConcurrentSet<>());
            COLLECTION_FACTORIES.put(ConcurrentNavigableSetNullSafe.class, size -> new ConcurrentNavigableSetNullSafe<>());
            COLLECTION_FACTORIES.put(CopyOnWriteArraySet.class, size -> new CopyOnWriteArraySet<>());
            COLLECTION_FACTORIES.put(TreeSet.class, size -> new TreeSet<>());
            COLLECTION_FACTORIES.put(LinkedHashSet.class, size -> new LinkedHashSet<>(size)); // Do not replace with Method::reference
            COLLECTION_FACTORIES.put(HashSet.class, size -> new HashSet<>(size));
            COLLECTION_FACTORIES.put(Set.class, size -> new LinkedHashSet<>(Math.max(size, 16)));

            // Deque implementations
            COLLECTION_FACTORIES.put(LinkedBlockingDeque.class, size -> new LinkedBlockingDeque<>(size));
            COLLECTION_FACTORIES.put(BlockingDeque.class, size -> new LinkedBlockingDeque<>(size));
            COLLECTION_FACTORIES.put(ConcurrentLinkedDeque.class, size -> new ConcurrentLinkedDeque<>());
            COLLECTION_FACTORIES.put(ArrayDeque.class, size -> new ArrayDeque<>());
            COLLECTION_FACTORIES.put(LinkedList.class, size -> new LinkedList<>());
            COLLECTION_FACTORIES.put(Deque.class, size -> new ArrayDeque<>(size));

            // Queue implementations
            COLLECTION_FACTORIES.put(PriorityBlockingQueue.class, size -> new PriorityBlockingQueue<>(size));
            COLLECTION_FACTORIES.put(ArrayBlockingQueue.class, size -> new ArrayBlockingQueue<>(size));
            COLLECTION_FACTORIES.put(LinkedBlockingQueue.class, size -> new LinkedBlockingQueue<>());
            COLLECTION_FACTORIES.put(SynchronousQueue.class, size -> new SynchronousQueue<>());
            COLLECTION_FACTORIES.put(DelayQueue.class, size -> new DelayQueue<>());
            COLLECTION_FACTORIES.put(LinkedTransferQueue.class, size -> new LinkedTransferQueue<>());
            COLLECTION_FACTORIES.put(BlockingQueue.class, size -> new LinkedBlockingQueue<>(size));
            COLLECTION_FACTORIES.put(PriorityQueue.class, size -> new PriorityQueue<>(size));
            COLLECTION_FACTORIES.put(ConcurrentLinkedQueue.class, size -> new ConcurrentLinkedQueue<>());
            COLLECTION_FACTORIES.put(Queue.class, size -> new LinkedList<>());

            // List implementations
            COLLECTION_FACTORIES.put(CopyOnWriteArrayList.class, size -> new CopyOnWriteArrayList<>());
            COLLECTION_FACTORIES.put(ConcurrentList.class, size -> new ConcurrentList<>(size));
            COLLECTION_FACTORIES.put(Stack.class, size -> new Stack<>());
            COLLECTION_FACTORIES.put(Vector.class, size -> new Vector<>(size));
            COLLECTION_FACTORIES.put(List.class, size -> new ArrayList<>(size));
            COLLECTION_FACTORIES.put(Collection.class, size -> new ArrayList<>(size));

            validateMappings();
        }

        static Collection<?> createCollection(Class<?> targetType, int size) {
            Function<Integer, Collection<?>> factory = FACTORY_CACHE.get(targetType);
            if (factory == null) {
                // Look up the factory and cache it
                factory = FACTORY_CACHE.computeIfAbsent(targetType, type -> {
                    for (Map.Entry<Class<?>, Function<Integer, Collection<?>>> entry : COLLECTION_FACTORIES.entrySet()) {
                        if (entry.getKey().isAssignableFrom(type)) {
                            return entry.getValue();
                        }
                    }
                    return ArrayList::new; // Default factory
                });
            }
            return factory.apply(size);
        }


        /**
         * Validates that collection type mappings are ordered correctly (most specific to most general).
         * Throws IllegalStateException if mappings are incorrectly ordered.
         */
        static void validateMappings() {
            List<Class<?>> interfaces = new ArrayList<>(COLLECTION_FACTORIES.keySet());

            for (int i = 0; i < interfaces.size(); i++) {
                Class<?> current = interfaces.get(i);
                for (int j = i + 1; j < interfaces.size(); j++) {
                    Class<?> next = interfaces.get(j);
                    if (current != next && current.isAssignableFrom(next)) {
                        throw new IllegalStateException("Mapping order error: " + next.getName() + " should come before " + current.getName());
                    }
                }
            }
        }
    }

    /**
     * Converts an array to a collection.
     */
    static Object arrayToCollection(Object array, Class<?> targetType) {
        int length = Array.getLength(array);
        Collection<Object> collection = (Collection<Object>) CollectionFactory.createCollection(targetType, length);
        for (int i = 0; i < length; i++) {
            Object element = Array.get(array, i);
            if (element != null && element.getClass().isArray()) {
                element = arrayToCollection(element, targetType);
            }
            collection.add(element);
        }
        return collection;
    }
}
