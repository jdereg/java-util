package com.cedarsoftware.util.convert;

import com.cedarsoftware.util.Dimension;
import com.cedarsoftware.util.Insets;
import com.cedarsoftware.util.Point;
import com.cedarsoftware.util.Rectangle;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Conversions to and from com.cedarsoftware.util.Dimension.
 * Supports conversion from various formats including Map with width/height keys,
 * int arrays, and strings to Dimension objects, as well as converting Dimension
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
final class DimensionConversions {

    private DimensionConversions() {
    }

    /**
     * Convert Dimension to String representation.
     * @param from Dimension instance
     * @param converter Converter instance
     * @return String like "800x600"
     */
    static String toString(Object from, Converter converter) {
        Dimension dimension = (Dimension) from;
        return dimension.getWidth() + "x" + dimension.getHeight();
    }

    /**
     * Convert Dimension to Map with width and height keys.
     * @param from Dimension instance
     * @param converter Converter instance
     * @return Map with "width" and "height" keys
     */
    static Map<String, Object> toMap(Object from, Converter converter) {
        Dimension dimension = (Dimension) from;
        Map<String, Object> target = new LinkedHashMap<>();
        target.put(MapConversions.WIDTH, dimension.getWidth());
        target.put(MapConversions.HEIGHT, dimension.getHeight());
        return target;
    }

    /**
     * Convert Dimension to int array [width, height].
     * @param from Dimension instance
     * @param converter Converter instance
     * @return int array with width and height values
     */
    static int[] toIntArray(Object from, Converter converter) {
        Dimension dimension = (Dimension) from;
        return new int[]{dimension.getWidth(), dimension.getHeight()};
    }

    /**
     * Convert Dimension to Integer (area: width * height).
     * @param from Dimension instance
     * @param converter Converter instance
     * @return Area as integer value
     */
    static Integer toInteger(Object from, Converter converter) {
        Dimension dimension = (Dimension) from;
        return dimension.getWidth() * dimension.getHeight();
    }

    /**
     * Convert Dimension to Long (area: width * height as long).
     * @param from Dimension instance
     * @param converter Converter instance
     * @return Area as long value
     */
    static Long toLong(Object from, Converter converter) {
        Dimension dimension = (Dimension) from;
        return (long) dimension.getWidth() * dimension.getHeight();
    }

    /**
     * Convert Dimension to BigInteger (area).
     * @param from Dimension instance
     * @param converter Converter instance
     * @return BigInteger representation of area
     */
    static BigInteger toBigInteger(Object from, Converter converter) {
        Dimension dimension = (Dimension) from;
        return BigInteger.valueOf((long) dimension.getWidth() * dimension.getHeight());
    }

    /**
     * Unsupported conversion from Dimension to BigDecimal.
     * @param from Dimension instance
     * @param converter Converter instance
     * @return Never returns - throws exception
     * @throws IllegalArgumentException Always thrown to indicate unsupported conversion
     */
    static BigDecimal toBigDecimal(Object from, Converter converter) {
        throw new IllegalArgumentException("Unsupported conversion from Dimension to BigDecimal - no meaningful conversion exists.");
    }

    /**
     * Convert Dimension to Point (width becomes x, height becomes y).
     * @param from Dimension instance
     * @param converter Converter instance
     * @return Point with x=width and y=height
     */
    static Point toPoint(Object from, Converter converter) {
        Dimension dimension = (Dimension) from;
        return new Point(dimension.getWidth(), dimension.getHeight());
    }

    /**
     * Convert Dimension to Boolean. (0,0) → false, anything else → true.
     * @param from Dimension instance
     * @param converter Converter instance
     * @return Boolean value
     */
    static Boolean toBoolean(Object from, Converter converter) {
        Dimension dimension = (Dimension) from;
        return dimension.getWidth() != 0 || dimension.getHeight() != 0;
    }

    /**
     * Convert Dimension to AtomicBoolean. (0,0) → false, anything else → true.
     * @param from Dimension instance
     * @param converter Converter instance
     * @return AtomicBoolean value
     */
    static java.util.concurrent.atomic.AtomicBoolean toAtomicBoolean(Object from, Converter converter) {
        return new java.util.concurrent.atomic.AtomicBoolean(toBoolean(from, converter));
    }

    /**
     * Unsupported conversion from Dimension to AtomicLong.
     * @param from Dimension instance
     * @param converter Converter instance
     * @return Never returns - throws exception
     * @throws IllegalArgumentException Always thrown to indicate unsupported conversion
     */
    static java.util.concurrent.atomic.AtomicLong toAtomicLong(Object from, Converter converter) {
        throw new IllegalArgumentException("Unsupported conversion from Dimension to AtomicLong - no meaningful conversion exists.");
    }


    /**
     * Convert Dimension to Rectangle (size becomes dimensions, position is 0,0).
     * @param from Dimension instance
     * @param converter Converter instance
     * @return Rectangle with x=0, y=0, width=width, height=height
     */
    static Rectangle toRectangle(Object from, Converter converter) {
        Dimension dimension = (Dimension) from;
        return new Rectangle(0, 0, dimension.getWidth(), dimension.getHeight());
    }

    /**
     * Convert Dimension to Insets (uniform insets with all sides equal to minimum dimension).
     * @param from Dimension instance
     * @param converter Converter instance
     * @return Insets with all sides = min(width, height)
     */
    static Insets toInsets(Object from, Converter converter) {
        Dimension dimension = (Dimension) from;
        int value = Math.min(dimension.getWidth(), dimension.getHeight());
        return new Insets(value, value, value, value);
    }
}