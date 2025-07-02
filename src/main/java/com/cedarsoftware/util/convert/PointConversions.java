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
 * Conversions to and from java.awt.Point.
 * Supports conversion from various formats including Map with x/y keys,
 * int arrays, and strings to Point objects, as well as converting Point
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
final class PointConversions {

    private PointConversions() {
    }

    /**
     * Convert Point to String representation.
     * @param from Point instance
     * @param converter Converter instance
     * @return String like "(100,200)"
     */
    static String toString(Object from, Converter converter) {
        Point point = (Point) from;
        return "(" + point.x + "," + point.y + ")";
    }

    /**
     * Convert Point to Map with x and y keys.
     * @param from Point instance
     * @param converter Converter instance
     * @return Map with "x" and "y" keys
     */
    static Map<String, Object> toMap(Object from, Converter converter) {
        Point point = (Point) from;
        Map<String, Object> target = new LinkedHashMap<>();
        target.put(MapConversions.X, point.x);
        target.put(MapConversions.Y, point.y);
        return target;
    }

    /**
     * Convert Point to int array [x, y].
     * @param from Point instance
     * @param converter Converter instance
     * @return int array with x and y values
     */
    static int[] toIntArray(Object from, Converter converter) {
        Point point = (Point) from;
        return new int[]{point.x, point.y};
    }

    /**
     * Convert Point to Integer (x value only, as Point doesn't have a natural single integer representation).
     * @param from Point instance
     * @param converter Converter instance
     * @return X coordinate as integer value
     */
    static Integer toInteger(Object from, Converter converter) {
        Point point = (Point) from;
        return point.x;
    }

    /**
     * Convert Point to Long (x value as long).
     * @param from Point instance
     * @param converter Converter instance
     * @return X coordinate as long value
     */
    static Long toLong(Object from, Converter converter) {
        Point point = (Point) from;
        return (long) point.x;
    }

    /**
     * Convert Point to BigInteger (x value).
     * @param from Point instance
     * @param converter Converter instance
     * @return BigInteger representation of x coordinate
     */
    static BigInteger toBigInteger(Object from, Converter converter) {
        Point point = (Point) from;
        return BigInteger.valueOf(point.x);
    }

    /**
     * Convert Point to BigDecimal (x value).
     * @param from Point instance
     * @param converter Converter instance
     * @return BigDecimal representation of x coordinate
     */
    static BigDecimal toBigDecimal(Object from, Converter converter) {
        Point point = (Point) from;
        return BigDecimal.valueOf(point.x);
    }

    /**
     * Convert Point to Dimension (x becomes width, y becomes height).
     * @param from Point instance
     * @param converter Converter instance
     * @return Dimension with width=x and height=y
     */
    static Dimension toDimension(Object from, Converter converter) {
        Point point = (Point) from;
        return new Dimension(point.x, point.y);
    }

    /**
     * Convert Point to Boolean. (0,0) → false, anything else → true.
     * @param from Point instance
     * @param converter Converter instance
     * @return Boolean value
     */
    static Boolean toBoolean(Object from, Converter converter) {
        Point point = (Point) from;
        return point.x != 0 || point.y != 0;
    }

    /**
     * Convert Point to Rectangle (x,y become position, size is 0,0).
     * @param from Point instance
     * @param converter Converter instance
     * @return Rectangle with x=x, y=y, width=0, height=0
     */
    static Rectangle toRectangle(Object from, Converter converter) {
        Point point = (Point) from;
        return new Rectangle(point.x, point.y, 0, 0);
    }

    /**
     * Convert Point to Insets (x becomes top, y becomes left, zero for bottom and right).
     * @param from Point instance
     * @param converter Converter instance
     * @return Insets with top=x, left=y, bottom=0, right=0
     */
    static Insets toInsets(Object from, Converter converter) {
        Point point = (Point) from;
        return new Insets(point.x, point.y, 0, 0);
    }
}