package com.cedarsoftware.util.convert;

import java.awt.Dimension;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Conversions to and from java.awt.Insets.
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
        return "(" + insets.top + "," + insets.left + "," + insets.bottom + "," + insets.right + ")";
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
        target.put(MapConversions.TOP, insets.top);
        target.put(MapConversions.LEFT, insets.left);
        target.put(MapConversions.BOTTOM, insets.bottom);
        target.put(MapConversions.RIGHT, insets.right);
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
        return new int[]{insets.top, insets.left, insets.bottom, insets.right};
    }

    /**
     * Convert Insets to Long (sum of all insets: top + left + bottom + right).
     * @param from Insets instance
     * @param converter Converter instance
     * @return Sum as long value
     */
    static Long toLong(Object from, Converter converter) {
        Insets insets = (Insets) from;
        return (long) insets.top + insets.left + insets.bottom + insets.right;
    }

    /**
     * Convert Insets to Integer (sum of all insets: top + left + bottom + right).
     * @param from Insets instance
     * @param converter Converter instance
     * @return Sum as integer value
     */
    static Integer toInteger(Object from, Converter converter) {
        Insets insets = (Insets) from;
        return insets.top + insets.left + insets.bottom + insets.right;
    }

    /**
     * Convert Insets to BigInteger (sum of all insets).
     * @param from Insets instance
     * @param converter Converter instance
     * @return BigInteger representation of sum
     */
    static BigInteger toBigInteger(Object from, Converter converter) {
        Insets insets = (Insets) from;
        return BigInteger.valueOf((long) insets.top + insets.left + insets.bottom + insets.right);
    }

    /**
     * Convert Insets to BigDecimal (sum of all insets).
     * @param from Insets instance
     * @param converter Converter instance
     * @return BigDecimal representation of sum
     */
    static BigDecimal toBigDecimal(Object from, Converter converter) {
        Insets insets = (Insets) from;
        return BigDecimal.valueOf((long) insets.top + insets.left + insets.bottom + insets.right);
    }

    /**
     * Convert Insets to Boolean. (0,0,0,0) → false, anything else → true.
     * @param from Insets instance
     * @param converter Converter instance
     * @return Boolean value
     */
    static Boolean toBoolean(Object from, Converter converter) {
        Insets insets = (Insets) from;
        return insets.top != 0 || insets.left != 0 || insets.bottom != 0 || insets.right != 0;
    }

    /**
     * Convert Insets to AtomicBoolean. (0,0,0,0) → false, anything else → true.
     * @param from Insets instance
     * @param converter Converter instance
     * @return AtomicBoolean value
     */
    static java.util.concurrent.atomic.AtomicBoolean toAtomicBoolean(Object from, Converter converter) {
        return new java.util.concurrent.atomic.AtomicBoolean(toBoolean(from, converter));
    }

    /**
     * Convert Insets to Point (top becomes x, left becomes y).
     * @param from Insets instance
     * @param converter Converter instance
     * @return Point with x=top and y=left
     */
    static Point toPoint(Object from, Converter converter) {
        Insets insets = (Insets) from;
        return new Point(insets.top, insets.left);
    }

    /**
     * Convert Insets to Dimension (sum of horizontal and vertical insets).
     * @param from Insets instance
     * @param converter Converter instance
     * @return Dimension with width=(left+right) and height=(top+bottom)
     */
    static Dimension toDimension(Object from, Converter converter) {
        Insets insets = (Insets) from;
        return new Dimension(insets.left + insets.right, insets.top + insets.bottom);
    }

    /**
     * Convert Insets to Rectangle (insets become the bounds).
     * @param from Insets instance
     * @param converter Converter instance
     * @return Rectangle with x=left, y=top, width=(right-left), height=(bottom-top)
     */
    static Rectangle toRectangle(Object from, Converter converter) {
        Insets insets = (Insets) from;
        // For insets, we interpret them as defining a rectangular area
        // where left/top are the position and right/bottom define the extent
        int width = Math.max(0, insets.right - insets.left);
        int height = Math.max(0, insets.bottom - insets.top);
        return new Rectangle(insets.left, insets.top, width, height);
    }
}