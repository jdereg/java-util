package com.cedarsoftware.util.convert;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.LinkedHashMap;
import java.util.Map;

import com.cedarsoftware.util.geom.Dimension;
import com.cedarsoftware.util.geom.Insets;
import com.cedarsoftware.util.geom.Point;
import com.cedarsoftware.util.geom.Rectangle;

/**
 * Conversions to and from com.cedarsoftware.util.Rectangle.
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
        return "(" + rectangle.getX() + "," + rectangle.getY() + "," + rectangle.getWidth() + "," + rectangle.getHeight() + ")";
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
        target.put(MapConversions.X, rectangle.getX());
        target.put(MapConversions.Y, rectangle.getY());
        target.put(MapConversions.WIDTH, rectangle.getWidth());
        target.put(MapConversions.HEIGHT, rectangle.getHeight());
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
        return new int[]{rectangle.getX(), rectangle.getY(), rectangle.getWidth(), rectangle.getHeight()};
    }

    /**
     * Convert Rectangle to Long (area: width * height).
     * @param from Rectangle instance
     * @param converter Converter instance
     * @return Area as long value
     */
    static Long toLong(Object from, Converter converter) {
        Rectangle rectangle = (Rectangle) from;
        return (long) rectangle.getWidth() * rectangle.getHeight();
    }

    /**
     * Convert Rectangle to Integer (area: width * height).
     * @param from Rectangle instance
     * @param converter Converter instance
     * @return Area as integer value
     */
    static Integer toInteger(Object from, Converter converter) {
        Rectangle rectangle = (Rectangle) from;
        return rectangle.getWidth() * rectangle.getHeight();
    }

    /**
     * Convert Rectangle to BigInteger (area).
     * @param from Rectangle instance
     * @param converter Converter instance
     * @return BigInteger representation of area
     */
    static BigInteger toBigInteger(Object from, Converter converter) {
        Rectangle rectangle = (Rectangle) from;
        return BigInteger.valueOf((long) rectangle.getWidth() * rectangle.getHeight());
    }

    /**
     * Unsupported conversion from Rectangle to BigDecimal.
     * @param from Rectangle instance
     * @param converter Converter instance
     * @return Never returns - throws exception
     * @throws IllegalArgumentException Always thrown to indicate unsupported conversion
     */
    static BigDecimal toBigDecimal(Object from, Converter converter) {
        throw new IllegalArgumentException("Unsupported conversion from Rectangle to BigDecimal - no meaningful conversion exists.");
    }

    /**
     * Convert Rectangle to Boolean. (0,0,0,0) → false, anything else → true.
     * @param from Rectangle instance
     * @param converter Converter instance
     * @return Boolean value
     */
    static Boolean toBoolean(Object from, Converter converter) {
        Rectangle rectangle = (Rectangle) from;
        return rectangle.getX() != 0 || rectangle.getY() != 0 || rectangle.getWidth() != 0 || rectangle.getHeight() != 0;
    }

    /**
     * Convert Rectangle to AtomicBoolean. (0,0,0,0) → false, anything else → true.
     * @param from Rectangle instance
     * @param converter Converter instance
     * @return AtomicBoolean value
     */
    static java.util.concurrent.atomic.AtomicBoolean toAtomicBoolean(Object from, Converter converter) {
        return new java.util.concurrent.atomic.AtomicBoolean(toBoolean(from, converter));
    }

    /**
     * Convert Rectangle to Point (x, y coordinates).
     * @param from Rectangle instance
     * @param converter Converter instance
     * @return Point with x=x and y=y from Rectangle
     */
    static Point toPoint(Object from, Converter converter) {
        Rectangle rectangle = (Rectangle) from;
        return new Point(rectangle.getX(), rectangle.getY());
    }

    /**
     * Convert Rectangle to Dimension (width, height).
     * @param from Rectangle instance
     * @param converter Converter instance
     * @return Dimension with width=width and height=height from Rectangle
     */
    static Dimension toDimension(Object from, Converter converter) {
        Rectangle rectangle = (Rectangle) from;
        return new Dimension(rectangle.getWidth(), rectangle.getHeight());
    }

    /**
     * Convert Rectangle to Insets (rectangle bounds become inset values).
     * @param from Rectangle instance
     * @param converter Converter instance
     * @return Insets with top=y, left=x, bottom=y+height, right=x+width
     */
    static Insets toInsets(Object from, Converter converter) {
        Rectangle rectangle = (Rectangle) from;
        return new Insets(rectangle.getY(), rectangle.getX(), 
                         rectangle.getY() + rectangle.getHeight(), 
                         rectangle.getX() + rectangle.getWidth());
    }

}