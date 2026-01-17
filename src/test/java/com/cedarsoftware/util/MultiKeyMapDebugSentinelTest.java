package com.cedarsoftware.util;

import org.junit.jupiter.api.Test;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.logging.Logger;

/**
 * Debug test to understand why NULL_SENTINEL objects aren't being caught
 */
class MultiKeyMapDebugSentinelTest {
    private static final Logger log = Logger.getLogger(MultiKeyMapDebugSentinelTest.class.getName());

    @Test
    void debugSentinelObjects() throws Exception {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        
        // Create a simple structure with null
        Object[] arrayWithNull = {"test", null, "end"};
        map.put(arrayWithNull, "debug_value");
        
        log.info("=== Debug Sentinel Objects ===");
        log.info("Map toString output:");
        log.info(map.toString());
        log.info("");
        
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
        
        log.info("NULL_SENTINEL object: " + NULL_SENTINEL);
        log.info("OPEN object: " + OPEN);
        log.info("CLOSE object: " + CLOSE);
        log.info("");
        
        // Access the dumpExpandedKeyStatic method
        Method dumpMethod = MultiKeyMap.class.getDeclaredMethod("dumpExpandedKeyStatic", Object.class, boolean.class, MultiKeyMap.class);
        dumpMethod.setAccessible(true);
        
        String result = (String) dumpMethod.invoke(null, arrayWithNull, true, map);
        log.info("dumpExpandedKeyStatic result: " + result);
        
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
            IdentitySet<Object> visited = new IdentitySet<>();
            int runningHash = 1;

            int resultHash = (int) expandAndHashMethod.invoke(null, arrayWithNull, expanded, visited, runningHash, false, true);
            
            log.info("\nExpanded list contents:");
            for (int i = 0; i < expanded.size(); i++) {
                Object obj = expanded.get(i);
                log.info("  [" + i + "] " + obj + " (class: " + obj.getClass().getSimpleName() + ")");
                log.info("      == NULL_SENTINEL: " + (obj == NULL_SENTINEL));
                log.info("      == OPEN: " + (obj == OPEN));
                log.info("      == CLOSE: " + (obj == CLOSE));
            }
        }
    }
}