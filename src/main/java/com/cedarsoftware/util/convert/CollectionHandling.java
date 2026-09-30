package com.cedarsoftware.util.convert;

import java.lang.reflect.Modifier;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;
import java.util.SortedSet;
import java.util.Stack;
import java.util.TreeSet;
import java.util.Vector;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.BlockingQueue;
import com.cedarsoftware.util.ClassUtilities;
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
import com.cedarsoftware.util.CompactSet;
import com.cedarsoftware.util.ConcurrentNavigableSetNullSafe;
import com.cedarsoftware.util.ConcurrentSet;
import com.cedarsoftware.util.ReflectionUtils;

/**
 * Handles creation and conversion of collections while preserving characteristics
 * and supporting special collection types. Supports all JDK collection types and
 * java-util collection types, with careful attention to maintaining collection
 * characteristics during conversion.
 *
 * <p>Maintains state during a single conversion operation.
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
final class CollectionHandling {
    private CollectionHandling() { }

    // Special collection type markers with their handlers
    private static final Map<Class<?>, CollectionFactory> SPECIAL_HANDLERS = new LinkedHashMap<>();

    // Base collection type mappings (most specific to most general)
    private static final Map<Class<?>, Function<Integer, Collection<?>>> BASE_FACTORIES = new LinkedHashMap<>();

    // The two tables in their order, for the plans below; the last base factory is the default (ArrayList)
    private static final CollectionFactory[] SPECIAL_FACTORY_LIST;
    private static final Function<Integer, Collection<?>>[] BASE_FACTORY_LIST;
    // The class each base factory makes (BlockingQueue's is LinkedBlockingQueue), in the same order
    private static final Class<?>[] BASE_PRODUCTS;

    static {
        // Initialize special collection handlers (most specific to most general)
        initializeSpecialHandlers();

        // Initialize base collection factories (most specific to most general)
        initializeBaseFactories();

        validateMappings();

        SPECIAL_FACTORY_LIST = SPECIAL_HANDLERS.values().toArray(new CollectionFactory[0]);
        @SuppressWarnings("unchecked")
        Function<Integer, Collection<?>>[] base = new Function[BASE_FACTORIES.size() + 1];
        int i = 0;
        for (Function<Integer, Collection<?>> factory : BASE_FACTORIES.values()) {
            base[i++] = factory;
        }
        base[i] = ArrayList::new;       // Default factory
        BASE_FACTORY_LIST = base;
        BASE_PRODUCTS = new Class<?>[base.length];
        int j = 0;
        for (Map.Entry<Class<?>, Function<Integer, Collection<?>>> entry : BASE_FACTORIES.entrySet()) {
            Class<?> product;
            try {
                product = entry.getValue().apply(16).getClass();
            } catch (IllegalArgumentException refused) {
                product = entry.getKey();       // SynchronousQueue, DelayQueue: refused as targets
            }
            BASE_PRODUCTS[j++] = product;
        }
        BASE_PRODUCTS[j] = ArrayList.class;
    }

    /**
     * Per target class, how createCollection() makes one: an index, a JDK type, since a java-util factory kept in a
     * JDK class would pin java-util's class loader. 0 .. S-1 is a special handler (the first whose marker class the
     * target is), S and above is S + a base factory (the first assignable one, else the default), and a negative
     * value ~b is the target class itself -- a concrete class of the caller's own, made by its no-argument
     * constructor -- falling back to base factory b. It was a ClassValueMap of factories, consulted after scanning
     * every special handler with isAssignableFrom() on each conversion.
     */
    private static final ClassValue<Integer> PLANS = new ClassValue<Integer>() {
        @Override
        protected Integer computeValue(Class<?> type) {
            int i = 0;
            for (Class<?> marker : SPECIAL_HANDLERS.keySet()) {
                if (marker.isAssignableFrom(type)) {
                    return i;
                }
                i++;
            }
            int base = closestBaseIndex(type);
            return isOwnConcreteType(type) ? ~base : SPECIAL_FACTORY_LIST.length + base;
        }
    };

    @SuppressWarnings({"unchecked"})
    private static void initializeSpecialHandlers() {
        // Empty collections
        SPECIAL_HANDLERS.put(CollectionsWrappers.getEmptyNavigableSetClass(), (size, source) ->
                Collections.emptyNavigableSet());
        SPECIAL_HANDLERS.put(CollectionsWrappers.getEmptySortedSetClass(), (size, source) ->
                Collections.emptySortedSet());
        SPECIAL_HANDLERS.put(CollectionsWrappers.getEmptySetClass(), (size, source) ->
                Collections.emptySet());
        SPECIAL_HANDLERS.put(CollectionsWrappers.getEmptyListClass(), (size, source) ->
                Collections.emptyList());
        SPECIAL_HANDLERS.put(CollectionsWrappers.getEmptyCollectionClass(), (size, source) ->
                Collections.emptyList());

        // Unmodifiable collections
        SPECIAL_HANDLERS.put(CollectionsWrappers.getUnmodifiableNavigableSetClass(), (size, source) ->
                createOptimalNavigableSet(source, size));
        SPECIAL_HANDLERS.put(CollectionsWrappers.getUnmodifiableSortedSetClass(), (size, source) ->
                createOptimalSortedSet(source, size));
        SPECIAL_HANDLERS.put(CollectionsWrappers.getUnmodifiableSetClass(), (size, source) ->
                createOptimalSet(source, size));
        SPECIAL_HANDLERS.put(CollectionsWrappers.getUnmodifiableListClass(), (size, source) ->
                createOptimalList(source, size));
        SPECIAL_HANDLERS.put(CollectionsWrappers.getUnmodifiableCollectionClass(), (size, source) ->
                createOptimalCollection(source, size));
        
        // Synchronized collections
        SPECIAL_HANDLERS.put(CollectionsWrappers.getSynchronizedNavigableSetClass(), (size, source) ->
                Collections.synchronizedNavigableSet(createOptimalNavigableSet(source, size)));
        SPECIAL_HANDLERS.put(CollectionsWrappers.getSynchronizedSortedSetClass(), (size, source) ->
                Collections.synchronizedSortedSet(createOptimalSortedSet(source, size)));
        SPECIAL_HANDLERS.put(CollectionsWrappers.getSynchronizedSetClass(), (size, source) ->
                Collections.synchronizedSet(createOptimalSet(source, size)));
        SPECIAL_HANDLERS.put(CollectionsWrappers.getSynchronizedListClass(), (size, source) ->
                Collections.synchronizedList(createOptimalList(source, size)));
        SPECIAL_HANDLERS.put(CollectionsWrappers.getSynchronizedCollectionClass(), (size, source) ->
                Collections.synchronizedCollection(createOptimalCollection(source, size)));

        // Checked collections
        SPECIAL_HANDLERS.put(CollectionsWrappers.getCheckedNavigableSetClass(), (size, source) -> {
            NavigableSet<?> navigableSet = createOptimalNavigableSet(source, size);
            Class<Object> elementType = (Class<Object>) getElementTypeFromSource(source);
            return Collections.checkedNavigableSet((NavigableSet<Object>) navigableSet, elementType);
        });

        SPECIAL_HANDLERS.put(CollectionsWrappers.getCheckedSortedSetClass(), (size, source) -> {
            SortedSet<?> sortedSet = createOptimalSortedSet(source, size);
            Class<Object> elementType = (Class<Object>) getElementTypeFromSource(source);
            return Collections.checkedSortedSet((SortedSet<Object>) sortedSet, elementType);
        });

        SPECIAL_HANDLERS.put(CollectionsWrappers.getCheckedSetClass(), (size, source) -> {
            Set<?> set = createOptimalSet(source, size);
            Class<Object> elementType = (Class<Object>) getElementTypeFromSource(source);
            return Collections.checkedSet((Set<Object>) set, elementType);
        });

        SPECIAL_HANDLERS.put(CollectionsWrappers.getCheckedListClass(), (size, source) -> {
            List<?> list = createOptimalList(source, size);
            Class<Object> elementType = (Class<Object>) getElementTypeFromSource(source);
            return Collections.checkedList((List<Object>) list, elementType);
        });

        SPECIAL_HANDLERS.put(CollectionsWrappers.getCheckedCollectionClass(), (size, source) -> {
            Collection<?> collection = createOptimalCollection(source, size);
            Class<Object> elementType = (Class<Object>) getElementTypeFromSource(source);
            return Collections.checkedCollection((Collection<Object>) collection, elementType);
        });
    }

    private static void initializeBaseFactories() {
        // Case-insensitive collections (java-util)
        BASE_FACTORIES.put(CaseInsensitiveSet.class, size -> new CaseInsensitiveSet<>());

        // Concurrent collections (java-util)
        BASE_FACTORIES.put(ConcurrentNavigableSetNullSafe.class, size -> new ConcurrentNavigableSetNullSafe<>());
        BASE_FACTORIES.put(ConcurrentSet.class, size -> new ConcurrentSet<>());

        // Compact collections (java-util)
        BASE_FACTORIES.put(CompactSet.class, size -> new CompactSet<>());

        // JDK Concurrent collections
        BASE_FACTORIES.put(ConcurrentSkipListSet.class, size -> new ConcurrentSkipListSet<>());
        BASE_FACTORIES.put(CopyOnWriteArraySet.class, size -> new CopyOnWriteArraySet<>());
        BASE_FACTORIES.put(ConcurrentLinkedQueue.class, size -> new ConcurrentLinkedQueue<>());
        BASE_FACTORIES.put(ConcurrentLinkedDeque.class, size -> new ConcurrentLinkedDeque<>());
        BASE_FACTORIES.put(CopyOnWriteArrayList.class, size -> new CopyOnWriteArrayList<>());

        // JDK Blocking collections
        BASE_FACTORIES.put(LinkedBlockingDeque.class, size -> new LinkedBlockingDeque<>(size));
        BASE_FACTORIES.put(ArrayBlockingQueue.class, size -> new ArrayBlockingQueue<>(size));
        BASE_FACTORIES.put(LinkedBlockingQueue.class, size -> new LinkedBlockingQueue<>(size));
        BASE_FACTORIES.put(PriorityBlockingQueue.class, size -> new PriorityBlockingQueue<>(size));
        BASE_FACTORIES.put(LinkedTransferQueue.class, size -> new LinkedTransferQueue<>());
        BASE_FACTORIES.put(SynchronousQueue.class, size -> {
            throw new IllegalArgumentException("SynchronousQueue cannot be used as a conversion target because it has zero capacity and cannot hold elements.");
        });
        BASE_FACTORIES.put(DelayQueue.class, size -> {
            throw new IllegalArgumentException("DelayQueue cannot be used as a conversion target because it requires elements implementing java.util.concurrent.Delayed.");
        });

        // Standard JDK Queue implementations
        BASE_FACTORIES.put(ArrayDeque.class, size -> new ArrayDeque<>(size));
        BASE_FACTORIES.put(LinkedList.class, size -> new LinkedList<>());
        BASE_FACTORIES.put(PriorityQueue.class, size -> new PriorityQueue<>(size));

        // Standard JDK Set implementations
        BASE_FACTORIES.put(TreeSet.class, size -> new TreeSet<>());
        BASE_FACTORIES.put(LinkedHashSet.class, size -> new LinkedHashSet<>(size));
        BASE_FACTORIES.put(HashSet.class, size -> new HashSet<>(size));

        // Standard JDK List implementations
        BASE_FACTORIES.put(ArrayList.class, size -> new ArrayList<>(size));
        BASE_FACTORIES.put(Stack.class, size -> new Stack<>());
        BASE_FACTORIES.put(Vector.class, size -> new Vector<>(size));

        // Interface implementations (most general)
        BASE_FACTORIES.put(BlockingDeque.class, size -> new LinkedBlockingDeque<>(size));
        BASE_FACTORIES.put(BlockingQueue.class, size -> new LinkedBlockingQueue<>(size));
        BASE_FACTORIES.put(Deque.class, size -> new ArrayDeque<>(size));
        BASE_FACTORIES.put(Queue.class, size -> new LinkedList<>());
        BASE_FACTORIES.put(NavigableSet.class, size -> new TreeSet<>());
        BASE_FACTORIES.put(SortedSet.class, size -> new TreeSet<>());
        BASE_FACTORIES.put(Set.class, size -> new LinkedHashSet<>(Math.max(size, 16)));
        BASE_FACTORIES.put(List.class, size -> new ArrayList<>(size));
        BASE_FACTORIES.put(Collection.class, size -> new ArrayList<>(size));
    }

    /**
     * Validates that collection type mappings are ordered correctly (most specific to most general).
     * Throws IllegalStateException if mappings are incorrectly ordered.
     */
    private static void validateMappings() {
        validateMapOrder(BASE_FACTORIES);
        validateMapOrder(SPECIAL_HANDLERS);
    }

    private static void validateMapOrder(Map<Class<?>, ?> map) {
        List<Class<?>> interfaces = new ArrayList<>(map.keySet());

        int len = interfaces.size();
        for (int i = 0; i < len; i++) {
            Class<?> current = interfaces.get(i);
            for (int j = i + 1; j < len; j++) {
                Class<?> next = interfaces.get(j);
                if (current != next && current.isAssignableFrom(next)) {
                    throw new IllegalStateException("Mapping order error: " + next.getName() +
                            " should come before " + current.getName());
                }
            }
        }
    }

    /**
     * Creates a collection matching the target type and special characteristics if any
     */
    static Collection<?> createCollection(Object source, Class<?> targetType) {
        int plan = PLANS.get(targetType);
        if (plan >= 0 && plan < SPECIAL_FACTORY_LIST.length) {
            // Special collection types first: they decide whether the collection is modifiable
            return SPECIAL_FACTORY_LIST[plan].create(sizeOrDefault(source), source);
        }
        if (plan < 0) {
            // A concrete class of the caller's own (MyList extends ArrayList, say): make one of it, as the Map
            // conversions do, rather than the closest known type, which is not an instance of what was asked for.
            // ClassUtilities.newInstance() applies its security checks; if it cannot make one, the closest known
            // type it is.
            try {
                Object created = ClassUtilities.newInstance(targetType, (Object) null);
                if (targetType.isInstance(created)) {
                    return (Collection<?>) created;
                }
            } catch (Exception ignored) {
                // fall back below
            }
            plan = ~plan;
        } else {
            plan -= SPECIAL_FACTORY_LIST.length;
        }
        // Base collection types (always modifiable)
        return BASE_FACTORY_LIST[plan].apply(sizeOrDefault(source));
    }

    /**
     * The index in BASE_FACTORY_LIST of the first base factory the type is assignable to, else the default's --
     * unless that factory's product is not of the type (TransferQueue is a BlockingQueue, whose factory makes a
     * LinkedBlockingQueue, which is not a TransferQueue): then the first factory whose product is (a
     * LinkedTransferQueue). With none (a JDK class it cannot make), the closest known type, as before.
     */
    private static int closestBaseIndex(Class<?> type) {
        int i = 0;
        for (Class<?> baseType : BASE_FACTORIES.keySet()) {
            if (baseType.isAssignableFrom(type)) {
                break;
            }
            i++;
        }
        if (!type.isAssignableFrom(BASE_PRODUCTS[i])) {
            for (int j = 0; j < BASE_PRODUCTS.length; j++) {
                if (type.isAssignableFrom(BASE_PRODUCTS[j])) {
                    return j;
                }
            }
        }
        return i;
    }

    /**
     * True for a concrete Collection class that is not one of the types with a factory of their own, and that has a
     * no-argument constructor: one the caller can mean to get exactly. (A JDK class made by a static factory, like
     * the list Arrays.asList() returns, has no such constructor, and keeps getting the closest known type.)
     */
    private static boolean isOwnConcreteType(Class<?> type) {
        if (type.isInterface() || Modifier.isAbstract(type.getModifiers()) || BASE_FACTORIES.containsKey(type)
                || !Collection.class.isAssignableFrom(type)) {
            return false;
        }
        try {
            return ReflectionUtils.getConstructor(type) != null;
        } catch (Exception e) {
            return false;
        }
    }

    // Helper methods to create optimal collection types while preserving characteristics
    private static NavigableSet<?> createOptimalNavigableSet(Object source, int size) {
        if (source instanceof ConcurrentNavigableSetNullSafe) {
            return new ConcurrentNavigableSetNullSafe<>();
        }
        if (source instanceof ConcurrentSkipListSet) {
            return new ConcurrentSkipListSet<>();
        }
        return new TreeSet<>();
    }

    private static SortedSet<?> createOptimalSortedSet(Object source, int size) {
        if (source instanceof ConcurrentNavigableSetNullSafe) {
            return new ConcurrentNavigableSetNullSafe<>();
        }
        if (source instanceof ConcurrentSkipListSet) {
            return new ConcurrentSkipListSet<>();
        }
        return new TreeSet<>();
    }

    private static Set<?> createOptimalSet(Object source, int size) {
        if (source instanceof CaseInsensitiveSet) {
            return new CaseInsensitiveSet<>();
        }     
        if (source instanceof CompactSet) {
            return new CompactSet<>();
        }
        if (source instanceof ConcurrentSet) {
            return new ConcurrentSet<>();
        }
        if (source instanceof LinkedHashSet) {
            return new LinkedHashSet<>(size);
        }
        return new LinkedHashSet<>(Math.max(size, 16));
    }

    private static List<?> createOptimalList(Object source, int size) {
        if (source instanceof CopyOnWriteArrayList) {
            return new CopyOnWriteArrayList<>();
        }
        if (source instanceof Vector) {
            return new Vector<>(size);
        }
        if (source instanceof LinkedList) {
            return new LinkedList<>();
        }
        return new ArrayList<>(size);
    }

    private static Collection<?> createOptimalCollection(Object source, int size) {
        if (source instanceof Set) {
            return createOptimalSet(source, size);
        }
        if (source instanceof List) {
            return createOptimalList(source, size);
        }
        return new ArrayList<>(size);
    }

    private static int sizeOrDefault(Object source) {
        if (source instanceof Collection) {
            return ((Collection<?>) source).size();
        }
        if (source != null && source.getClass().isArray()) {
            return java.lang.reflect.Array.getLength(source);
        }
        return 16;
    }

    private static Class<?> getElementTypeFromSource(Object source) {
        if (source instanceof Collection<?>) {
            for (Object element : (Collection<?>) source) {
                if (element != null) {
                    return element.getClass();
                }
            }
        }
        return Object.class; // Fallback to Object.class if no non-null elements are found
    }
    
    @FunctionalInterface
    interface CollectionFactory {
        Collection<?> create(int size, Object source);
    }
}