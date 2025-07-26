package com.cedarsoftware.util;

import org.junit.jupiter.api.Test;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.logging.Logger;

/**
 * Test the detection logic for already-flattened collections
 */
class MultiKeyMapDetectionLogicTest {
    private static final Logger log = Logger.getLogger(MultiKeyMapDetectionLogicTest.class.getName());

    @Test
    void testDetectionLogic() throws Exception {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        
        // Create a complex structure that will definitely create a flattened ArrayList
        String[][] berries2D = {{"raspberry", "blackberry"}, {null, "cranberry"}};
        List<String> berriesList1D = Arrays.asList("strawberry", null, "blueberry");
        Object[] complexArray = {berriesList1D, "middle_string", berries2D, null};
        map.put(complexArray, "debug_value");
        
        // Get the stored flattened structure
        Object storedKey = null;
        for (MultiKeyMap.MultiKeyEntry<String> entry : map.entries()) {
            storedKey = entry.keys[0];
            break;
        }
        
        log.info("Stored key type: " + storedKey.getClass().getSimpleName());
        log.info("Stored key: " + storedKey);
        
        if (!(storedKey instanceof Collection)) {
            log.info("Not a collection - using original debug test instead");
            return;
        }
        
        ArrayList<Object> flattenedList = (ArrayList<Object>) storedKey;
        
        log.info("=== Detection Logic Test ===");
        log.info("Flattened list: " + flattenedList);
        log.info("List size: " + flattenedList.size());
        
        // Get access to sentinel objects
        Field nullSentinelField = MultiKeyMap.class.getDeclaredField("NULL_SENTINEL");
        nullSentinelField.setAccessible(true);
        Object NULL_SENTINEL = nullSentinelField.get(null);
        
        Field openField = MultiKeyMap.class.getDeclaredField("OPEN");
        openField.setAccessible(true);
        Object OPEN = openField.get(null);
        
        Field closeField = MultiKeyMap.class.getDeclaredField("CLOSE");
        closeField.setAccessible(true);
        Object CLOSE = closeField.get(null);
        
        // Test the detection logic manually
        boolean isAlreadyFlattened = false;
        for (Object element : flattenedList) {
            log.info("Element: " + element + " (" + element.getClass().getSimpleName() + ")");
            log.info("  == NULL_SENTINEL: " + (element == NULL_SENTINEL));
            log.info("  == OPEN: " + (element == OPEN));
            log.info("  == CLOSE: " + (element == CLOSE));
            
            if (element == NULL_SENTINEL || element == OPEN || element == CLOSE) {
                log.info("  DETECTION: Found sentinel object!");
                isAlreadyFlattened = true;
                break;
            }
        }
        
        log.info("Final detection result: " + isAlreadyFlattened);
        
        // Now test dumpExpandedKeyStatic directly
        Method dumpMethod = MultiKeyMap.class.getDeclaredMethod("dumpExpandedKeyStatic", Object.class, boolean.class, MultiKeyMap.class);
        dumpMethod.setAccessible(true);
        
        String result = (String) dumpMethod.invoke(null, flattenedList, true, map);
        log.info("dumpExpandedKeyStatic result: " + result);
    }
}