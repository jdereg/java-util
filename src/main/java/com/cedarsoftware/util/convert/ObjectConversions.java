package com.cedarsoftware.util.convert;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

import com.cedarsoftware.util.ClassUtilities;
import com.cedarsoftware.util.MathUtilities;
import com.cedarsoftware.util.ReflectionUtils;
import com.cedarsoftware.util.SystemUtilities;

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
     * Converts any Object to a Map representation with deep field traversal.
     * This method handles the generic object-to-map conversion logic while delegating
     * to MapConversions for specific type handling and Record conversions.
     * 
     * @param from The Object to convert
     * @param converter The Converter instance for type conversions  
     * @return A Map representation of the object
     */
    static Map<String, Object> objectToMap(Object from, Converter converter) {
        if (from == null) {
            return null;
        }
        
        // Handle Map objects specially - delegate to MapConversions for proper Map-to-Map conversion
        if (from instanceof Map) {
            // Use the universal Map converter with LinkedHashMap as default target
            Map<?, ?> result = MapConversions.mapToMapWithTarget(from, converter, LinkedHashMap.class);
            // Cast to expected return type - this is safe since we're returning LinkedHashMap
            @SuppressWarnings("unchecked")
            Map<String, Object> mapResult = (Map<String, Object>) result;
            return mapResult;
        }
        
        // For target-unaware conversions, continue with regular object processing
        Map<?, ?> result = objectToMapWithTarget(from, converter, LinkedHashMap.class);
        // Cast to expected return type - this is safe since we're returning LinkedHashMap
        @SuppressWarnings("unchecked")
        Map<String, Object> mapResult = (Map<String, Object>) result;
        return mapResult;
    }
    
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
            result.put(MapConversions.V, convertToJsonCompatible(from, converter));
            return result;
        }
        
        // Handle Records specially - delegate to MapConversions
        if (isRecord(from.getClass())) {
            return MapConversions.recordToMap(from, converter);
        }
        
        // Handle regular objects with field traversal
        return traverseObjectFields(from, converter);
    }
    
    /**
     * Iteratively traverses object fields to build a Map representation using a work queue.
     * Uses IdentityHashMap for visited tracking to avoid stack overflow on deep object graphs.
     */
    private static Map<String, Object> traverseObjectFields(Object rootObj, Converter converter) {
        if (rootObj == null) {
            return null;
        }
        
        // Use IdentityHashMap for visited tracking (object identity, not equals)
        Set<Object> visited = Collections.newSetFromMap(new IdentityHashMap<>());
        
        // Work queue for iterative processing
        Deque<WorkItem> workQueue = new LinkedList<>();
        Map<String, Object> rootResult = new LinkedHashMap<>();
        
        // Add root object to work queue
        workQueue.add(new WorkItem(rootObj, null, null));
        
        while (!workQueue.isEmpty()) {
            WorkItem current = workQueue.removeFirst();
            Object obj = current.obj;
            
            // Skip if already visited (prevents cycles)
            if (visited.contains(obj)) {
                if (current.targetMap != null && current.fieldName != null) {
                    current.targetMap.put(current.fieldName, null); // or reference marker
                }
                continue;
            }
            visited.add(obj);
            
            // Process the current object's fields
            Map<String, Object> currentMap = (current.targetMap == null) ? rootResult : new LinkedHashMap<>();
            
            try {
                Class<?> clazz = obj.getClass();
                
                // Get all declared fields including from superclasses using ReflectionUtils
                Collection<Field> fields = ReflectionUtils.getAllDeclaredFields(clazz);
                
                for (Field field : fields) {
                    // Skip static, transient, and synthetic fields
                    if (shouldSkipField(field)) {
                        continue;
                    }
                    
                    try {
                        // Get field value - ReflectionUtils already made fields accessible
                        Object value = field.get(obj);
                        
                        if (value != null) {
                            Object convertedValue = convertFieldValueIterative(value, converter, workQueue, currentMap, field.getName());
                            if (convertedValue != null) {
                                currentMap.put(field.getName(), convertedValue);
                            }
                        }
                    } catch (Exception e) {
                        // Skip fields that can't be accessed
                        continue;
                    }
                }
                
                // Place the result in the parent map if this isn't the root
                if (current.targetMap != null && current.fieldName != null) {
                    current.targetMap.put(current.fieldName, currentMap);
                }
                
            } catch (Exception e) {
                // Skip objects that can't be processed
                if (current.targetMap != null && current.fieldName != null) {
                    current.targetMap.put(current.fieldName, null);
                }
            }
        }
        
        return rootResult;
    }
    
    /**
     * Work item for iterative object traversal.
     */
    private static class WorkItem {
        final Object obj;
        final Map<String, Object> targetMap;
        final String fieldName;
        
        WorkItem(Object obj, Map<String, Object> targetMap, String fieldName) {
            this.obj = obj;
            this.targetMap = targetMap;
            this.fieldName = fieldName;
        }
    }
    
    /**
     * Converts a field value for iterative processing, adding complex objects to the work queue.
     */
    private static Object convertFieldValueIterative(Object value, Converter converter, Deque<WorkItem> workQueue, 
                                                  Map<String, Object> parentMap, String fieldName) {
        if (value == null) {
            return null;
        }
        
        Class<?> valueClass = value.getClass();
        
        // Handle primitives and wrappers
        if (isPrimitiveOrWrapper(valueClass)) {
            return convertToJsonCompatible(value, converter);
        }
        
        // Handle Strings
        if (value instanceof String) {
            return value;
        }
        
        // Handle Collections
        if (value instanceof Collection) {
            Collection<?> collection = (Collection<?>) value;
            List<Object> result = new ArrayList<>();
            for (Object item : collection) {
                if (item != null && !isPrimitiveOrWrapper(item.getClass()) && !(item instanceof String)) {
                    // For complex objects in collections, we need to process them iteratively
                    // For now, convert them to string representation to avoid complexity
                    result.add(item.toString());
                } else {
                    result.add(convertToJsonCompatible(item, converter));
                }
            }
            return result;
        }
        
        // Handle Maps  
        if (value instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) value;
            Map<String, Object> result = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                String key = entry.getKey() != null ? entry.getKey().toString() : null;
                if (key != null) {
                    Object entryValue = entry.getValue();
                    if (entryValue != null && !isPrimitiveOrWrapper(entryValue.getClass()) && !(entryValue instanceof String)) {
                        // For complex objects in maps, convert to string for simplicity
                        result.put(key, entryValue.toString());
                    } else {
                        result.put(key, convertToJsonCompatible(entryValue, converter));
                    }
                }
            }
            return result;
        }
        
        // Handle arrays
        if (valueClass.isArray()) {
            List<Object> result = new ArrayList<>();
            int length = java.lang.reflect.Array.getLength(value);
            for (int i = 0; i < length; i++) {
                Object item = java.lang.reflect.Array.get(value, i);
                if (item != null && !isPrimitiveOrWrapper(item.getClass()) && !(item instanceof String)) {
                    // For complex objects in arrays, convert to string for simplicity
                    result.add(item.toString());
                } else {
                    result.add(convertToJsonCompatible(item, converter));
                }
            }
            return result;
        }
        
        // Handle Records specially - delegate to MapConversions
        if (isRecord(valueClass)) {
            try {
                return MapConversions.recordToMap(value, converter);
            } catch (Exception e) {
                return value.toString();
            }
        }
        
        // Handle complex objects - add to work queue for processing
        workQueue.add(new WorkItem(value, parentMap, fieldName));
        return null; // Will be filled in when the work item is processed
    }
    
    /**
     * Converts primitives and wrappers to JSON-compatible types using MathUtilities for optimal numeric types.
     */
    private static Object convertToJsonCompatible(Object value, Converter converter) {
        if (value == null) {
            return null;
        }
        
        // Handle numeric types with MathUtilities for optimal representation
        if (value instanceof Number) {
            // Convert to string and parse back to get minimal type  
            String numberStr = value.toString();
            try {
                return MathUtilities.parseToMinimalNumericType(numberStr);
            } catch (Exception e) {
                // Fallback to original value
                return value;
            }
        }
        
        // Boolean and String pass through
        if (value instanceof Boolean || value instanceof String) {
            return value;
        }
        
        // Character to String
        if (value instanceof Character) {
            return value.toString();
        }
        
        // Everything else to String representation
        return value.toString();
    }
    
    /**
     * Determines if a field should be skipped during traversal.
     */
    private static boolean shouldSkipField(java.lang.reflect.Field field) {
        int modifiers = field.getModifiers();
        return java.lang.reflect.Modifier.isStatic(modifiers) ||
               java.lang.reflect.Modifier.isTransient(modifiers) ||
               field.isSynthetic();
    }
    
    /**
     * Check if a class represents a primitive or wrapper type.
     */
    private static boolean isPrimitiveOrWrapper(Class<?> clazz) {
        return ClassUtilities.isPrimitive(clazz);
    }
    
    /**
     * Determines if a Map object is suitable for simple Map-to-Map conversion
     * vs. complex object traversal that preserves references and object structure.
     */
    private static boolean isSimpleMapConversion(Object from) {
        if (!(from instanceof Map)) {
            return false;
        }
        
        // CompactMap and other complex Map implementations should use object traversal
        // to preserve references, complex object graphs, etc.
        Class<?> clazz = from.getClass();
        String className = clazz.getName();
        
        // Exclude CompactMap and other complex java-util Maps
        if (className.contains("CompactMap") || 
            className.contains("CaseInsensitiveMap")) {
            return false;
        }
        
        // Allow standard JDK Map types for simple conversion
        if (clazz == HashMap.class ||
            clazz == LinkedHashMap.class ||
            clazz == TreeMap.class ||
            clazz == ConcurrentHashMap.class ||
            className.contains("EmptyMap") ||
            className.contains("SingletonMap") ||
            className.contains("UnmodifiableMap") ||
            className.contains("SynchronizedMap")) {
            return true;
        }
        
        // For other Map types, be conservative and use object traversal
        return false;
    }
    
    /**
     * Check if a class is a Record using SystemUtilities for version detection.
     */
    private static boolean isRecord(Class<?> clazz) {
        // Records are only available in JDK 14+
        if (!SystemUtilities.isJavaVersionAtLeast(14, 0)) {
            return false;
        }
        
        try {
            // Use ReflectionUtils to check for Record class (available in JDK 14+)
            java.lang.reflect.Method isRecordMethod = ReflectionUtils.getMethod(Class.class, "isRecord");
            if (isRecordMethod != null) {
                return (Boolean) ReflectionUtils.call(clazz, isRecordMethod);
            }
            return false;
        } catch (Exception e) {
            // Records not supported in this JVM
            return false;
        }
    }
    
    /**
     * ConvertWithTarget implementation for Object to Map conversions.
     * This provides target-aware Object->Map conversion while properly delegating
     * Map inputs to the Map-to-Map converter.
     */
    static final ConvertWithTarget<Map<?, ?>> OBJECT_TO_MAP_CONVERTER = new ConvertWithTarget<Map<?, ?>>() {
        @Override
        public Map<?, ?> convertWithTarget(Object from, Converter converter, Class<?> target) {
            return objectToMapWithTarget(from, converter, target);
        }
    };
}