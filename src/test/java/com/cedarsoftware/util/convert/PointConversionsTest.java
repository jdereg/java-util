package com.cedarsoftware.util.convert;

import java.awt.Dimension;
import java.awt.Point;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.HashMap;
import java.util.Map;

import com.cedarsoftware.util.convert.DefaultConverterOptions;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Comprehensive tests for java.awt.Point conversions in the Converter.
 * Tests conversion from various types to Point and from Point to various types.
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
class PointConversionsTest {

    private Converter converter;

    @BeforeEach
    void setUp() {
        converter = new Converter(new DefaultConverterOptions());
    }

    // ========================================
    // Null/Void to Point Tests
    // ========================================

    @Test
    void testNullToPoint() {
        Point result = converter.convert(null, Point.class);
        assertThat(result).isNull();
    }

    // ========================================
    // String to Point Tests
    // ========================================

    @Test
    void testStringToPoint_parenthesesFormat() {
        Point result = converter.convert("(100,200)", Point.class);
        assertThat(result.x).isEqualTo(100);
        assertThat(result.y).isEqualTo(200);
    }

    @Test
    void testStringToPoint_commaSeparated() {
        Point result = converter.convert("150,250", Point.class);
        assertThat(result.x).isEqualTo(150);
        assertThat(result.y).isEqualTo(250);
    }

    @Test
    void testStringToPoint_spaceSeparated() {
        Point result = converter.convert("300 400", Point.class);
        assertThat(result.x).isEqualTo(300);
        assertThat(result.y).isEqualTo(400);
    }

    @Test
    void testStringToPoint_withWhitespace() {
        Point result = converter.convert("  ( 50 , 75 )  ", Point.class);
        assertThat(result.x).isEqualTo(50);
        assertThat(result.y).isEqualTo(75);
    }

    @Test
    void testStringToPoint_negativeCoordinates() {
        Point result = converter.convert("(-10,-20)", Point.class);
        assertThat(result.x).isEqualTo(-10);
        assertThat(result.y).isEqualTo(-20);
    }

    @Test
    void testStringToPoint_invalidFormat() {
        assertThatThrownBy(() -> converter.convert("invalid", Point.class))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Unable to parse point from string");
    }

    @Test
    void testStringToPoint_emptyString() {
        assertThatThrownBy(() -> converter.convert("", Point.class))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Cannot convert empty/null string to Point");
    }

    // ========================================
    // Map to Point Tests
    // ========================================

    @Test
    void testMapToPoint_xyCoordinates() {
        Map<String, Object> map = new HashMap<>();
        map.put("x", 100);
        map.put("y", 200);
        
        Point result = converter.convert(map, Point.class);
        assertThat(result.x).isEqualTo(100);
        assertThat(result.y).isEqualTo(200);
    }

    @Test
    void testMapToPoint_stringValue() {
        Map<String, Object> map = new HashMap<>();
        map.put("value", "(75,125)");
        
        Point result = converter.convert(map, Point.class);
        assertThat(result.x).isEqualTo(75);
        assertThat(result.y).isEqualTo(125);
    }

    // ========================================
    // Array to Point Tests
    // ========================================

    @Test
    void testIntArrayToPoint() {
        int[] array = {300, 400};
        
        Point result = converter.convert(array, Point.class);
        assertThat(result.x).isEqualTo(300);
        assertThat(result.y).isEqualTo(400);
    }

    @Test
    void testIntArrayToPoint_negativeValues() {
        int[] array = {-50, -100};
        
        Point result = converter.convert(array, Point.class);
        assertThat(result.x).isEqualTo(-50);
        assertThat(result.y).isEqualTo(-100);
    }

    @Test
    void testIntArrayToPoint_invalidLength() {
        int[] array = {100};
        
        assertThatThrownBy(() -> converter.convert(array, Point.class))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Point array must have exactly 2 elements");
    }

    // ========================================
    // Number to Point Tests
    // ========================================

    @Test
    void testIntegerToPoint() {
        Point result = converter.convert(250, Point.class);
        assertThat(result.x).isEqualTo(250);
        assertThat(result.y).isEqualTo(250);
    }

    @Test
    void testLongToPoint() {
        Point result = converter.convert(500L, Point.class);
        assertThat(result.x).isEqualTo(500);
        assertThat(result.y).isEqualTo(500);
    }

    @Test
    void testNumberToPoint_negative() {
        Point result = converter.convert(-100, Point.class);
        assertThat(result.x).isEqualTo(-100);
        assertThat(result.y).isEqualTo(-100);
    }

    @Test
    void testBigIntegerToPoint() {
        Point result = converter.convert(BigInteger.valueOf(750), Point.class);
        assertThat(result.x).isEqualTo(750);
        assertThat(result.y).isEqualTo(750);
    }

    @Test
    void testBigDecimalToPoint() {
        Point result = converter.convert(BigDecimal.valueOf(850), Point.class);
        assertThat(result.x).isEqualTo(850);
        assertThat(result.y).isEqualTo(850);
    }

    @Test
    void testAtomicIntegerToPoint() {
        Point result = converter.convert(new AtomicInteger(300), Point.class);
        assertThat(result.x).isEqualTo(300);
        assertThat(result.y).isEqualTo(300);
    }

    @Test
    void testAtomicLongToPoint() {
        Point result = converter.convert(new AtomicLong(400), Point.class);
        assertThat(result.x).isEqualTo(400);
        assertThat(result.y).isEqualTo(400);
    }

    @Test
    void testAtomicBooleanToPoint_true() {
        Point result = converter.convert(new AtomicBoolean(true), Point.class);
        assertThat(result.x).isEqualTo(1);
        assertThat(result.y).isEqualTo(1);
    }

    @Test
    void testAtomicBooleanToPoint_false() {
        Point result = converter.convert(new AtomicBoolean(false), Point.class);
        assertThat(result.x).isEqualTo(0);
        assertThat(result.y).isEqualTo(0);
    }

    @Test
    void testBooleanToPoint_true() {
        Point result = converter.convert(Boolean.TRUE, Point.class);
        assertThat(result.x).isEqualTo(1);
        assertThat(result.y).isEqualTo(1);
    }

    @Test
    void testBooleanToPoint_false() {
        Point result = converter.convert(Boolean.FALSE, Point.class);
        assertThat(result.x).isEqualTo(0);
        assertThat(result.y).isEqualTo(0);
    }

    // ========================================
    // Dimension to Point Tests
    // ========================================

    @Test
    void testDimensionToPoint() {
        Dimension dimension = new Dimension(800, 600);
        Point result = converter.convert(dimension, Point.class);
        assertThat(result.x).isEqualTo(800);
        assertThat(result.y).isEqualTo(600);
    }

    // ========================================
    // Point to String Tests
    // ========================================

    @Test
    void testPointToString() {
        Point point = new Point(100, 200);
        String result = converter.convert(point, String.class);
        assertThat(result).isEqualTo("(100,200)");
    }

    @Test
    void testPointToString_negativeCoordinates() {
        Point point = new Point(-50, -75);
        String result = converter.convert(point, String.class);
        assertThat(result).isEqualTo("(-50,-75)");
    }

    // ========================================
    // Point to Integer Tests (X coordinate)
    // ========================================

    @Test
    void testPointToInteger() {
        Point point = new Point(150, 250);
        Integer result = converter.convert(point, Integer.class);
        assertThat(result).isEqualTo(150); // Returns x coordinate
    }

    // ========================================
    // Point to Long Tests (X coordinate)
    // ========================================

    @Test
    void testPointToLong() {
        Point point = new Point(300, 400);
        Long result = converter.convert(point, Long.class);
        assertThat(result).isEqualTo(300L); // Returns x coordinate
    }

    // ========================================
    // Point to BigInteger Tests (X coordinate)
    // ========================================

    @Test
    void testPointToBigInteger() {
        Point point = new Point(500, 600);
        BigInteger result = converter.convert(point, BigInteger.class);
        assertThat(result).isEqualTo(BigInteger.valueOf(500L)); // Returns x coordinate
    }

    // ========================================
    // Point to BigDecimal Tests (X coordinate)
    // ========================================

    @Test
    void testPointToBigDecimal() {
        Point point = new Point(750, 850);
        BigDecimal result = converter.convert(point, BigDecimal.class);
        assertThat(result).isEqualTo(BigDecimal.valueOf(750L)); // Returns x coordinate
    }

    // ========================================
    // Point to Map Tests
    // ========================================

    @Test
    void testPointToMap() {
        Point point = new Point(100, 200);
        Map<String, Object> result = converter.convert(point, Map.class);
        
        assertThat(result).containsEntry("x", 100);
        assertThat(result).containsEntry("y", 200);
        assertThat(result).hasSize(2);
    }

    // ========================================
    // Point to int[] Tests
    // ========================================

    @Test
    void testPointToIntArray() {
        Point point = new Point(400, 500);
        int[] result = converter.convert(point, int[].class);
        
        assertThat(result).containsExactly(400, 500);
    }

    // ========================================
    // Point to Dimension Tests
    // ========================================

    @Test
    void testPointToDimension() {
        Point point = new Point(640, 480);
        Dimension result = converter.convert(point, Dimension.class);
        
        assertThat(result.width).isEqualTo(640);
        assertThat(result.height).isEqualTo(480);
    }

    // ========================================
    // Point Identity Tests
    // ========================================

    @Test
    void testPointToPoint_identity() {
        Point original = new Point(100, 200);
        Point result = converter.convert(original, Point.class);
        
        assertThat(result).isSameAs(original);
    }

    // ========================================
    // Round-trip Tests
    // ========================================

    @Test
    void testPointDimensionRoundTrip() {
        Point originalPoint = new Point(800, 600);
        
        // Point -> Dimension -> Point
        Dimension dimension = converter.convert(originalPoint, Dimension.class);
        Point backToPoint = converter.convert(dimension, Point.class);
        
        assertThat(backToPoint.x).isEqualTo(originalPoint.x);
        assertThat(backToPoint.y).isEqualTo(originalPoint.y);
    }

    @Test
    void testStringPointRoundTrip() {
        String originalString = "(150,250)";
        
        // String -> Point -> String
        Point point = converter.convert(originalString, Point.class);
        String backToString = converter.convert(point, String.class);
        
        assertThat(backToString).isEqualTo(originalString);
    }

    // ========================================
    // Point to Boolean Tests
    // ========================================

    @Test
    void testPointToBoolean_zeroZero() {
        Point point = new Point(0, 0);
        Boolean result = converter.convert(point, Boolean.class);
        assertThat(result).isFalse();
    }

    @Test
    void testPointToBoolean_nonZero() {
        Point point = new Point(100, 200);
        Boolean result = converter.convert(point, Boolean.class);
        assertThat(result).isTrue();
    }

    @Test
    void testPointToBoolean_partialZero() {
        Point point = new Point(0, 100);
        Boolean result = converter.convert(point, Boolean.class);
        assertThat(result).isTrue(); // Any non-zero coordinate is true
    }

    @Test
    void testPointToBoolean_negativeCoordinates() {
        Point point = new Point(-10, -20);
        Boolean result = converter.convert(point, Boolean.class);
        assertThat(result).isTrue(); // Non-zero (even negative) is true
    }

    // ========================================
    // Round-trip Boolean Tests
    // ========================================

    @Test
    void testBooleanPointRoundTrip_true() {
        Boolean originalBoolean = Boolean.TRUE;
        
        // Boolean -> Point -> Boolean
        Point point = converter.convert(originalBoolean, Point.class);
        Boolean backToBoolean = converter.convert(point, Boolean.class);
        
        assertThat(backToBoolean).isEqualTo(originalBoolean);
    }

    @Test
    void testBooleanPointRoundTrip_false() {
        Boolean originalBoolean = Boolean.FALSE;
        
        // Boolean -> Point -> Boolean
        Point point = converter.convert(originalBoolean, Point.class);
        Boolean backToBoolean = converter.convert(point, Boolean.class);
        
        assertThat(backToBoolean).isEqualTo(originalBoolean);
    }
}