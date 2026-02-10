package com.cedarsoftware.util.convert;

import java.util.LinkedHashMap;
import java.util.Map;

import com.cedarsoftware.util.geom.Insets;

/**
 * Conversions to and from com.cedarsoftware.util.Insets.
 * Supports conversion from various formats including Map with top/left/bottom/right keys,
 * int arrays, and strings to Insets objects, as well as converting Insets
 * objects to these various representations.
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
final class InsetsConversions {

    private InsetsConversions() {
    }

    /**
     * Convert Insets to String representation.
     * @param from Insets instance
     * @param converter Converter instance
     * @return String like "(5,10,5,10)" representing (top,left,bottom,right)
     */
    static String toString(Object from, Converter converter) {
        Insets insets = (Insets) from;
        return "(" + insets.getTop() + "," + insets.getLeft() + "," + insets.getBottom() + "," + insets.getRight() + ")";
    }

    /**
     * Convert Insets to Map with top, left, bottom, and right keys.
     * @param from Insets instance
     * @param converter Converter instance
     * @return Map with "top", "left", "bottom", and "right" keys
     */
    static Map<String, Object> toMap(Object from, Converter converter) {
        Insets insets = (Insets) from;
        Map<String, Object> target = new LinkedHashMap<>();
        target.put(MapConversions.TOP, insets.getTop());
        target.put(MapConversions.LEFT, insets.getLeft());
        target.put(MapConversions.BOTTOM, insets.getBottom());
        target.put(MapConversions.RIGHT, insets.getRight());
        return target;
    }

    /**
     * Convert Insets to int array [top, left, bottom, right].
     * @param from Insets instance
     * @param converter Converter instance
     * @return int array with top, left, bottom, and right values
     */
    static int[] toIntArray(Object from, Converter converter) {
        Insets insets = (Insets) from;
        return new int[]{insets.getTop(), insets.getLeft(), insets.getBottom(), insets.getRight()};
    }

    /**
     * Convert Insets to Boolean. (0,0,0,0) -> false, anything else -> true.
     * @param from Insets instance
     * @param converter Converter instance
     * @return Boolean value
     */
    static Boolean toBoolean(Object from, Converter converter) {
        Insets insets = (Insets) from;
        return insets.getTop() != 0 || insets.getLeft() != 0 || insets.getBottom() != 0 || insets.getRight() != 0;
    }
}
