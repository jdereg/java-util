package com.cedarsoftware.util.convert;

import java.util.LinkedHashMap;
import java.util.Map;

import com.cedarsoftware.util.geom.Point;

/**
 * Conversions to and from com.cedarsoftware.util.Point.
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
        return "(" + point.getX() + "," + point.getY() + ")";
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
        target.put(MapConversions.X, point.getX());
        target.put(MapConversions.Y, point.getY());
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
        return new int[]{point.getX(), point.getY()};
    }

    /**
     * Convert Point to Boolean. (0,0) -> false, anything else -> true.
     * @param from Point instance
     * @param converter Converter instance
     * @return Boolean value
     */
    static Boolean toBoolean(Object from, Converter converter) {
        Point point = (Point) from;
        return point.getX() != 0 || point.getY() != 0;
    }
}
