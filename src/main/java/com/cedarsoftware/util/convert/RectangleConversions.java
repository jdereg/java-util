package com.cedarsoftware.util.convert;

import java.util.LinkedHashMap;
import java.util.Map;

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
     * Convert Rectangle to Boolean. (0,0,0,0) -> false, anything else -> true.
     * @param from Rectangle instance
     * @param converter Converter instance
     * @return Boolean value
     */
    static Boolean toBoolean(Object from, Converter converter) {
        Rectangle rectangle = (Rectangle) from;
        return rectangle.getX() != 0 || rectangle.getY() != 0 || rectangle.getWidth() != 0 || rectangle.getHeight() != 0;
    }
}
