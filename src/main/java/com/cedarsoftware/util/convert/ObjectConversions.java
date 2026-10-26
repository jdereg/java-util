package com.cedarsoftware.util.convert;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.cedarsoftware.util.ArrayUtilities;
import com.cedarsoftware.util.ClassUtilities;
import com.cedarsoftware.util.ReflectionUtils;

/**
 * Conversions for generic Object to Map transformations.
 * This class handles the generic object traversal logic for converting any Object to a Map representation,
 * while MapConversions handles specific type conversions and Record handling.
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
final class ObjectConversions {
    /**
     * Converts any Object to a Map representation with target type awareness.
     * This method handles the generic object-to-map conversion logic while delegating
     * to MapConversions for specific type handling and Record conversions.
     * 
     * @param from The Object to convert
     * @param converter The Converter instance for type conversions  
     * @param toType The target Map type to convert to
     * @return A Map representation of the object
     */
    static Map<?, ?> objectToMapWithTarget(Object from, Converter converter, Class<?> toType) {
        if (from == null) {
            return null;
        }
        
        // Handle Map objects specially - delegate to MapConversions for proper Map-to-Map conversion
        if (from instanceof Map) {
            return MapConversions.mapToMapWithTarget(from, converter, toType);
        }
        
        // Handle primitives and wrapper types
        if (isPrimitiveOrWrapper(from.getClass())) {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put(MapConversions.V, convertToJsonCompatible(from));
            return result;
        }
        
        // Handle Records specially - delegate to MapConversions
        if (ReflectionUtils.isRecord(from.getClass())) {
            return MapConversions.recordToMap(from, converter);
        }
        
        // Handle regular objects with field traversal
        return traverseObjectFields(from, converter);
    }
    
    /**
     * An object's fields as a Map, walked depth first without recursion, so a deep graph cannot overflow the stack.
     * A value (a primitive or wrapper, String, enum, a type the Converter treats as simple, a JDK class that is not a
     * container) stays a value; an object, Collection, Map or array becomes a Map or List. An object reached again --
     * a second time, or from inside itself (a loop) -- is the same Map or List in each place, so the result is a graph
     * like the object's, nothing dropped. Print one that loops with {@code MapUtilities.mapToString()} and compare it
     * with {@code DeepEquals}: a JDK map's own toString(), hashCode() and equals() follow the loop without end. A
     * field the class hides (one its superclass declares under the same name) is left out, as Java hides it; a null
     * field too.
     */
    private static Map<String, Object> traverseObjectFields(Object rootObj, Converter converter) {
        if (rootObj == null) {
            return null;
        }
        // Each object begun, with its Frame: its Map or List is made when it is begun, so a loop back to it finds it
        Map<Object, Frame> converted = new IdentityHashMap<>(8);
        Deque<Frame> path = new ArrayDeque<>(8);
        Map<String, Object> rootMap = new LinkedHashMap<>();
        Frame root = new Frame(rootObj, rootMap);
        converted.put(rootObj, root);
        path.push(root);

        while (!path.isEmpty()) {
            Frame frame = path.peek();
            if (!frame.advance()) {
                path.pop();
                continue;
            }
            Object child = frame.childValue;
            Object out;
            if (child == null) {
                out = null;
            } else if (isValue(child, converter)) {
                out = valueOf(child);
            } else {
                Frame seen = converted.get(child);
                if (seen != null) {
                    out = seen.container;           // reached again, or from inside itself: the same Map
                } else if (ReflectionUtils.isRecord(child.getClass())) {
                    Frame record = new Frame(child, recordToMap(child, converter));
                    converted.put(child, record);
                    out = record.container;
                } else {
                    Frame childFrame = new Frame(child, null);
                    converted.put(child, childFrame);
                    path.push(childFrame);
                    out = childFrame.container;
                }
            }
            frame.put(out);
        }
        return rootMap;
    }

    private static Object recordToMap(Object record, Converter converter) {
        try {
            return MapConversions.recordToMap(record, converter);
        } catch (Exception e) {
            return record.toString();
        }
    }

    /** True for what stays as it is in the Map, rather than becoming a Map or List of its own. */
    private static boolean isValue(Object value, Converter converter) {
        Class<?> c = value.getClass();
        if (value instanceof String || value instanceof Enum || isPrimitiveOrWrapper(c)) {
            return true;
        }
        if (value instanceof Collection || value instanceof Map || c.isArray()) {
            return false;
        }
        // BigDecimal, Date, LocalDate, UUID...: walking their fields gave {} on JDK 17+ and internals on JDK 8
        return converter.isSimpleTypeConversionSupported(c, c) || c.getClassLoader() == null;
    }

    private static Object valueOf(Object value) {
        return value instanceof String || value instanceof Enum || !isPrimitiveOrWrapper(value.getClass())
                ? value : convertToJsonCompatible(value);
    }

    /**
     * A class's fields as its Map holds them, worked out once per class: not static, transient or synthetic, and
     * of two with one name (a subclass hiding its superclass's), the subclass's, as Java resolves the name.
     */
    private static final ClassValue<Field[]> MAPPED_FIELDS = new ClassValue<Field[]>() {
        @Override
        protected Field[] computeValue(Class<?> c) {
            List<Field> mapped = new ArrayList<>();
            Set<String> names = new HashSet<>();
            for (Field field : ReflectionUtils.getAllDeclaredFields(c)) {      // the class's own first
                if (!shouldSkipField(field) && names.add(field.getName())) {
                    mapped.add(field);
                }
            }
            return mapped.toArray(new Field[0]);
        }
    };

    /** One object, Collection, Map or array being converted: walks its children and fills its Map or List. */
    private static final class Frame {
        private static final int FIELDS = 0;
        private static final int ENTRIES = 1;
        private static final int ELEMENTS = 2;

        final Object source;
        final Object container;             // a LinkedHashMap<String, Object>, or an ArrayList<Object> for elements
        private final int kind;
        private final Iterator<?> children;     // entries or elements
        private Field[] fields;                 // or fields
        private int fieldIndex;
        private String key;
        Object childValue;

        /** A record, already converted: nothing left to walk. */
        Frame(Object source, Object converted) {
            this.source = source;
            this.container = converted;
            this.kind = ELEMENTS;
            this.children = java.util.Collections.emptyIterator();
        }


        Frame(Object source, Map<String, Object> rootMap) {
            this.source = source;
            Iterator<?> it;
            int k;
            Object out;
            if (rootMap == null && source instanceof Collection) {
                k = ELEMENTS;
                it = iteratorOf((Collection<?>) source);
                out = new ArrayList<>();
            } else if (rootMap == null && source instanceof Map) {
                k = ENTRIES;
                it = entriesOf((Map<?, ?>) source);
                out = new LinkedHashMap<String, Object>();
            } else if (rootMap == null && source.getClass().isArray()) {
                k = ELEMENTS;
                it = new ArrayIterator(source);
                out = new ArrayList<>();
            } else {
                k = FIELDS;
                it = null;
                fields = MAPPED_FIELDS.get(source.getClass());
                out = rootMap != null ? rootMap : new LinkedHashMap<String, Object>();
            }
            kind = k;
            children = it;
            container = out;
        }

        /** Moves to the next child to convert; false when there is none (or the source cannot be read further). */
        @SuppressWarnings("unchecked")
        boolean advance() {
            if (kind == FIELDS) {
                while (fieldIndex < fields.length) {
                    Field field = fields[fieldIndex++];
                    Object value;
                    try {
                        value = field.get(source);          // ReflectionUtils already made fields accessible
                    } catch (Exception e) {
                        continue;                           // a field that cannot be read is left out
                    }
                    if (value != null) {
                        key = field.getName();
                        childValue = value;
                        return true;
                    }
                }
                return false;
            }
            try {
                while (children.hasNext()) {
                    Object next = children.next();
                    if (kind == ENTRIES) {
                        Map.Entry<?, ?> entry = (Map.Entry<?, ?>) next;
                        if (entry.getKey() == null) {
                            continue;
                        }
                        key = entry.getKey().toString();
                        childValue = entry.getValue();
                        return true;
                    }
                    childValue = next;
                    return true;
                }
            } catch (RuntimeException e) {
                // a container that fails while being read (a lazy proxy without its session): what was read stays
            }
            return false;
        }

        @SuppressWarnings("unchecked")
        void put(Object out) {
            if (kind == ELEMENTS) {
                ((List<Object>) container).add(out);
            } else {
                ((Map<String, Object>) container).put(key, out);
            }
        }

        private static Iterator<?> entriesOf(Map<?, ?> map) {
            try {
                return map.entrySet().iterator();
            } catch (RuntimeException e) {
                return java.util.Collections.emptyIterator();      // one that cannot be read: left empty
            }
        }

        private static Iterator<?> iteratorOf(Collection<?> collection) {
            try {
                return collection.iterator();
            } catch (RuntimeException e) {
                return java.util.Collections.emptyIterator();      // one that cannot be read: left empty
            }
        }
    }

    private static final class ArrayIterator implements Iterator<Object> {
        private final Object array;
        private final int length;
        private int index;

        ArrayIterator(Object array) {
            this.array = array;
            this.length = ArrayUtilities.getLength(array);
        }

        @Override
        public boolean hasNext() {
            return index < length;
        }

        @Override
        public Object next() {
            return ArrayUtilities.getElement(array, index++);
        }
    }

    /**
     * Converts primitives and wrappers to JSON-compatible types, preserving original numeric types.
     */
    private static Object convertToJsonCompatible(Object value) {
        if (value == null) {
            return null;
        }

        // Standard numeric types pass through directly — no string round-trip needed
        if (value instanceof Number || value instanceof Boolean) {
            return value;
        }

        if (value instanceof String) {
            return value;
        }

        // Character and other types to String representation
        return value.toString();
    }
    
    /**
     * Determines if a field should be skipped during traversal.
     */
    private static boolean shouldSkipField(java.lang.reflect.Field field) {
        int modifiers = field.getModifiers();
        return Modifier.isStatic(modifiers) ||
               Modifier.isTransient(modifiers) ||
               field.isSynthetic();
    }
    
    /**
     * Check if a class represents a primitive or wrapper type.
     */
    private static boolean isPrimitiveOrWrapper(Class<?> clazz) {
        return ClassUtilities.isPrimitive(clazz);
    }

}