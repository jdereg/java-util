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
 * Conversions to and from java.awt.Rectangle.
 * Supports conversion from various formats including Map with x/y/width/height keys,
 * int arrays, and strings to Rectangle objects, as well as converting Rectangle
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
final class RectangleConversions {

    private RectangleConversions() {
    }

    /**
     * Convert Rectangle to String representation.
     * @param from Rectangle instance
     * @param converter Converter instance
     * @return String like "(10,20,100,50)" representing (x,y,width,height)
     */
    static String toString(Object from, Converter converter) {
        Rectangle rectangle = (Rectangle) from;
        return "(" + rectangle.x + "," + rectangle.y + "," + rectangle.width + "," + rectangle.height + ")";
    }

    /**
     * Convert Rectangle to Map with x, y, width, and height keys.
     * @param from Rectangle instance
     * @param converter Converter instance
     * @return Map with "x", "y", "width", and "height" keys
     */
    static Map<String, Object> toMap(Object from, Converter converter) {
        Rectangle rectangle = (Rectangle) from;
        Map<String, Object> target = new LinkedHashMap<>();
        target.put(MapConversions.X, rectangle.x);
        target.put(MapConversions.Y, rectangle.y);
        target.put(MapConversions.WIDTH, rectangle.width);
        target.put(MapConversions.HEIGHT, rectangle.height);
        return target;
    }

    /**
     * Convert Rectangle to int array [x, y, width, height].
     * @param from Rectangle instance
     * @param converter Converter instance
     * @return int array with x, y, width, and height values
     */
    static int[] toIntArray(Object from, Converter converter) {
        Rectangle rectangle = (Rectangle) from;
        return new int[]{rectangle.x, rectangle.y, rectangle.width, rectangle.height};
    }

    /**
     * Convert Rectangle to Long (area: width * height).
     * @param from Rectangle instance
     * @param converter Converter instance
     * @return Area as long value
     */
    static Long toLong(Object from, Converter converter) {
        Rectangle rectangle = (Rectangle) from;
        return (long) rectangle.width * rectangle.height;
    }

    /**
     * Convert Rectangle to Integer (area: width * height).
     * @param from Rectangle instance
     * @param converter Converter instance
     * @return Area as integer value
     */
    static Integer toInteger(Object from, Converter converter) {
        Rectangle rectangle = (Rectangle) from;
        return rectangle.width * rectangle.height;
    }

    /**
     * Convert Rectangle to BigInteger (area).
     * @param from Rectangle instance
     * @param converter Converter instance
     * @return BigInteger representation of area
     */
    static BigInteger toBigInteger(Object from, Converter converter) {
        Rectangle rectangle = (Rectangle) from;
        return BigInteger.valueOf((long) rectangle.width * rectangle.height);
    }

    /**
     * Convert Rectangle to BigDecimal (area: width * height).
     * @param from Rectangle instance
     * @param converter Converter instance
     * @return BigDecimal representation of area
     */
    static BigDecimal toBigDecimal(Object from, Converter converter) {
        Rectangle rectangle = (Rectangle) from;
        return BigDecimal.valueOf((long) rectangle.width * rectangle.height);
    }

    /**
     * Convert Rectangle to Boolean. (0,0,0,0) → false, anything else → true.
     * @param from Rectangle instance
     * @param converter Converter instance
     * @return Boolean value
     */
    static Boolean toBoolean(Object from, Converter converter) {
        Rectangle rectangle = (Rectangle) from;
        return rectangle.x != 0 || rectangle.y != 0 || rectangle.width != 0 || rectangle.height != 0;
    }

    /**
     * Convert Rectangle to Point (x, y coordinates).
     * @param from Rectangle instance
     * @param converter Converter instance
     * @return Point with x=x and y=y from Rectangle
     */
    static Point toPoint(Object from, Converter converter) {
        Rectangle rectangle = (Rectangle) from;
        return new Point(rectangle.x, rectangle.y);
    }

    /**
     * Convert Rectangle to Dimension (width, height).
     * @param from Rectangle instance
     * @param converter Converter instance
     * @return Dimension with width=width and height=height from Rectangle
     */
    static Dimension toDimension(Object from, Converter converter) {
        Rectangle rectangle = (Rectangle) from;
        return new Dimension(rectangle.width, rectangle.height);
    }

    /**
     * Convert Rectangle to Insets (rectangle bounds become inset values).
     * @param from Rectangle instance
     * @param converter Converter instance
     * @return Insets with top=y, left=x, bottom=y+height, right=x+width
     */
    static Insets toInsets(Object from, Converter converter) {
        Rectangle rectangle = (Rectangle) from;
        return new Insets(rectangle.y, rectangle.x, 
                         rectangle.y + rectangle.height, 
                         rectangle.x + rectangle.width);
    }
}