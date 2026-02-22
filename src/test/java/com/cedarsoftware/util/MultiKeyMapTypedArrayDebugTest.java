package com.cedarsoftware.util;

import java.util.logging.Logger;

import org.junit.jupiter.api.Test;

/**
 * Debug test to understand how typed arrays are being processed
 */
public class MultiKeyMapTypedArrayDebugTest {
    private static final Logger LOG = Logger.getLogger(MultiKeyMapTypedArrayDebugTest.class.getName());
    
    @Test
    void debugTypedArrayProcessing() {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        
        // Test the simplest case: String[] vs Object[]
        String[] stringArray = {"a", "b", "c"};
        Object[] objectArray = {"a", "b", "c"};
        
        LOG.info("=== Before putting arrays ===");
        LOG.info("String array type: " + stringArray.getClass());
        LOG.info("Object array type: " + objectArray.getClass());
        LOG.info("String array is Object[]: " + (stringArray instanceof Object[]));
        LOG.info("Object array is Object[]: " + (objectArray instanceof Object[]));
        
        map.put(stringArray, "string_array");
        LOG.info("After putting String[], map size: " + map.size());
        
        map.put(objectArray, "object_array");
        LOG.info("After putting Object[], map size: " + map.size());
        
        LOG.info("=== Lookup results ===");
        LOG.info("String array lookup: " + map.get(stringArray));
        LOG.info("Object array lookup: " + map.get(objectArray));
        
        // Test with new instances
        String[] newStringArray = {"a", "b", "c"};
        Object[] newObjectArray = {"a", "b", "c"};
        
        LOG.info("New String array lookup: " + map.get(newStringArray));
        LOG.info("New Object array lookup: " + map.get(newObjectArray));
        
        LOG.info("=== Key details ===");
        LOG.info("Map size: " + map.size());
        LOG.info("Keys in map:");
        for (Object key : map.keySet()) {
            LOG.info("  Key: " + key + " (type: " + key.getClass() + ")");
        }
    }
    
    @Test 
    void debugSingleElementTypedArrays() {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        
        // Test single element typed arrays
        int[] singleInt = {42};
        String[] singleString = {"hello"};
        Object[] singleObject = {"hello"};
        
        LOG.info("=== Single Element Arrays ===");
        LOG.info("int[] type: " + singleInt.getClass());
        LOG.info("String[] type: " + singleString.getClass()); 
        LOG.info("Object[] type: " + singleObject.getClass());
        
        map.put(singleInt, "single_int");
        LOG.info("After int[], map size: " + map.size());
        
        map.put(singleString, "single_string");
        LOG.info("After String[], map size: " + map.size());
        
        map.put(singleObject, "single_object");
        LOG.info("After Object[], map size: " + map.size());
        
        LOG.info("=== Direct element lookups ===");
        LOG.info("Lookup 42: " + map.get(42));
        LOG.info("Lookup 'hello': " + map.get("hello"));
        
        LOG.info("=== Array lookups ===");
        LOG.info("Lookup int[]{42}: " + map.get(new int[]{42}));
        LOG.info("Lookup String[]{'hello'}: " + map.get(new String[]{"hello"}));
        LOG.info("Lookup Object[]{'hello'}: " + map.get(new Object[]{"hello"}));
        
        LOG.info("=== Final map state ===");
        LOG.info("Map size: " + map.size());
        LOG.info("Keys in map:");
        for (Object key : map.keySet()) {
            LOG.info("  Key: " + key + " (type: " + key.getClass() + ")");
        }
    }
}