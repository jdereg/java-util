package com.cedarsoftware.util.convert;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.LinkedHashMap;
import java.util.Map;

import com.cedarsoftware.util.geom.Color;

/**
 * Conversions to and from com.cedarsoftware.util.Color.
 * Supports conversion from various formats including hex strings, RGB maps, 
 * packed integers, and arrays to Color objects, as well as converting Color 
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
final class ColorConversions {

    private ColorConversions() {
    }

    /**
     * Convert Color to String representation (hex format).
     * @param from Color instance
     * @param converter Converter instance
     * @return Hex string like "#FF8040" or "#80FF8040" (with alpha)
     */
    static String toString(Object from, Converter converter) {
        Color color = (Color) from;
        if (color.getAlpha() == 255) {
            // Standard RGB hex format
            return String.format("#%02X%02X%02X", color.getRed(), color.getGreen(), color.getBlue());
        } else {
            // ARGB hex format with alpha
            return String.format("#%02X%02X%02X%02X", color.getAlpha(), color.getRed(), color.getGreen(), color.getBlue());
        }
    }

    /**
     * Convert Color to Integer (packed RGB value).
     * @param from Color instance
     * @param converter Converter instance
     * @return Packed RGB integer value
     */
    static Integer toInteger(Object from, Converter converter) {
        Color color = (Color) from;
        return color.getRGB();
    }

    /**
     * Convert Color to Long (packed RGB value as long).
     * @param from Color instance
     * @param converter Converter instance
     * @return Packed RGB value as long
     */
    static Long toLong(Object from, Converter converter) {
        Color color = (Color) from;
        return (long) color.getRGB();
    }

    /**
     * Convert Color to BigInteger.
     * @param from Color instance
     * @param converter Converter instance
     * @return BigInteger representation of packed RGB value
     */
    static BigInteger toBigInteger(Object from, Converter converter) {
        Color color = (Color) from;
        return BigInteger.valueOf(color.getRGB());
    }

    /**
     * Convert Color to BigDecimal.
     * @param from Color instance
     * @param converter Converter instance
     * @return BigDecimal representation of packed RGB value
     */
    static BigDecimal toBigDecimal(Object from, Converter converter) {
        Color color = (Color) from;
        return BigDecimal.valueOf(color.getRGB());
    }

    /**
     * Convert Color to int array [r, g, b] or [r, g, b, a].
     * @param from Color instance
     * @param converter Converter instance
     * @return int array with RGB or RGBA values
     */
    static int[] toIntArray(Object from, Converter converter) {
        Color color = (Color) from;
        if (color.getAlpha() == 255) {
            return new int[]{color.getRed(), color.getGreen(), color.getBlue()};
        } else {
            return new int[]{color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha()};
        }
    }

    /**
     * Convert Color to Map with RGB/RGBA component keys.
     * @param from Color instance
     * @param converter Converter instance
     * @return Map with "red", "green", "blue", "alpha", and "rgb" keys
     */
    static Map<String, Object> toMap(Object from, Converter converter) {
        Color color = (Color) from;
        Map<String, Object> target = new LinkedHashMap<>();
        target.put(MapConversions.RED, color.getRed());
        target.put(MapConversions.GREEN, color.getGreen());
        target.put(MapConversions.BLUE, color.getBlue());
        target.put(MapConversions.ALPHA, color.getAlpha());
        target.put(MapConversions.RGB, color.getRGB());
        return target;
    }
}