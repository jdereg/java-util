package com.cedarsoftware.util;

import org.junit.jupiter.api.Test;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;

/**
 * Debug test to understand why NULL_SENTINEL objects aren't being caught
 */
class MultiKeyMapDebugSentinelTest {

    @Test
    void debugSentinelObjects() throws Exception {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        
        // Create a simple structure with null
        Object[] arrayWithNull = {"test", null, "end"};
        map.put(arrayWithNull, "debug_value");
        
        System.out.println("=== Debug Sentinel Objects ===");
        System.out.println("Map toString output:");
        System.out.println(map.toString());
        System.out.println();
        
        // Get access to the private fields and methods via reflection
        Field nullSentinelField = MultiKeyMap.class.getDeclaredField("NULL_SENTINEL");
        nullSentinelField.setAccessible(true);
        Object NULL_SENTINEL = nullSentinelField.get(null);
        
        Field openField = MultiKeyMap.class.getDeclaredField("OPEN");
        openField.setAccessible(true);
        Object OPEN = openField.get(null);
        
        Field closeField = MultiKeyMap.class.getDeclaredField("CLOSE");
        closeField.setAccessible(true);
        Object CLOSE = closeField.get(null);
        
        System.out.println("NULL_SENTINEL object: " + NULL_SENTINEL);
        System.out.println("OPEN object: " + OPEN);
        System.out.println("CLOSE object: " + CLOSE);
        System.out.println();
        
        // Access the dumpExpandedKeyStatic method
        Method dumpMethod = MultiKeyMap.class.getDeclaredMethod("dumpExpandedKeyStatic", Object.class, boolean.class, MultiKeyMap.class);
        dumpMethod.setAccessible(true);
        
        String result = (String) dumpMethod.invoke(null, arrayWithNull, true, map);
        System.out.println("dumpExpandedKeyStatic result: " + result);
        
        // Let's also test the expandAndHash method directly
        Method expandAndHashMethod = null;
        for (Method m : MultiKeyMap.class.getDeclaredMethods()) {
            if (m.getName().equals("expandAndHash")) {
                expandAndHashMethod = m;
                break;
            }
        }
        
        if (expandAndHashMethod != null) {
            expandAndHashMethod.setAccessible(true);
            List<Object> expanded = new ArrayList<>();
            IdentityHashMap<Object, Boolean> visited = new IdentityHashMap<>();
            int[] dummyHash = new int[]{1};
            
            expandAndHashMethod.invoke(null, arrayWithNull, expanded, visited, dummyHash, false);
            
            System.out.println("\nExpanded list contents:");
            for (int i = 0; i < expanded.size(); i++) {
                Object obj = expanded.get(i);
                System.out.println("  [" + i + "] " + obj + " (class: " + obj.getClass().getSimpleName() + ")");
                System.out.println("      == NULL_SENTINEL: " + (obj == NULL_SENTINEL));
                System.out.println("      == OPEN: " + (obj == OPEN));
                System.out.println("      == CLOSE: " + (obj == CLOSE));
            }
        }
    }
}