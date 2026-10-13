package com.cedarsoftware.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link MultiKeyMap#keySet()}, {@link MultiKeyMap#values()} and {@link MultiKeyMap#entrySet()} go through the map
 * in one order: the n-th key belongs to the n-th value. keySet() returned a HashSet, in the order of the keys' own
 * hash codes, so a caller that paired it with values() -- as a serializer writing the keys and then the values does
 * -- paired keys with the wrong values.
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
class MultiKeyMapViewOrderTest {

    /** The pair from the report: a two-part key and a single key. */
    @Test
    void theKeysLineUpWithTheValues() {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        map.putMultiKey("v", "a", "b");
        map.put("single", "w");

        List<Object> keys = new ArrayList<>(map.keySet());
        List<String> values = new ArrayList<>(map.values());

        assertEquals(2, keys.size());
        for (int i = 0; i < keys.size(); i++) {
            assertEquals(map.get(keys.get(i)), values.get(i), "key " + keys.get(i));
        }
    }

    /**
     * 300 maps from a fixed seed, of single keys, two-part and three-part keys, grown past several resizes and
     * with entries removed again: keySet(), values() and entrySet() agree position by position, and every key
     * finds its value.
     */
    @Test
    void theThreeViewsGoThroughTheMapInOneOrder() {
        Random random = new Random(20261012L);
        int entries = 0;
        for (int n = 0; n < 300; n++) {
            MultiKeyMap<Integer> map = new MultiKeyMap<>();
            int size = 1 + random.nextInt(120);
            for (int i = 0; i < size; i++) {
                switch (random.nextInt(3)) {
                    case 0:
                        map.put("k" + random.nextInt(200), i);
                        break;
                    case 1:
                        map.putMultiKey(i, "m" + random.nextInt(50), random.nextInt(9));
                        break;
                    default:
                        map.putMultiKey(i, random.nextInt(9), "x" + random.nextInt(50), random.nextBoolean());
                }
            }
            for (Object key : new ArrayList<>(map.keySet())) {
                if (random.nextInt(4) == 0) {
                    map.remove(key);
                }
            }

            List<Object> keys = new ArrayList<>(map.keySet());
            List<Integer> values = new ArrayList<>(map.values());
            assertEquals(map.size(), keys.size());
            assertEquals(map.size(), values.size());
            Iterator<Map.Entry<Object, Integer>> entry = map.entrySet().iterator();
            for (int i = 0; i < keys.size(); i++) {
                Map.Entry<Object, Integer> next = entry.next();
                assertEquals(next.getKey(), keys.get(i), "position " + i + " of map " + n);
                assertEquals(next.getValue(), values.get(i), "position " + i + " of map " + n);
                assertEquals(map.get(keys.get(i)), values.get(i), "key " + keys.get(i) + " of map " + n);
            }
            entries += keys.size();
        }
        assertTrue(entries > 10_000, entries + " entries");
    }
}
