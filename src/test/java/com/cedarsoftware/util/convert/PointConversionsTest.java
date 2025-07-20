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
    void testIntegerToPointBlocked() {
        assertThatThrownBy(() -> converter.convert(250, Point.class))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Unsupported conversion, source type [Integer");
    }

    @Test
    void testLongToPointBlocked() {
        assertThatThrownBy(() -> converter.convert(500L, Point.class))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Unsupported conversion, source type [Long");
    }

    @Test
    void testNumberToPointNegativeBlocked() {
        assertThatThrownBy(() -> converter.convert(-100, Point.class))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Unsupported conversion, source type [Integer");
    }

    @Test
    void testBigIntegerToPointBlocked() {
        assertThatThrownBy(() -> converter.convert(BigInteger.valueOf(750), Point.class))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Unsupported conversion, source type [BigInteger");
    }


    @Test
    void testAtomicIntegerToPointBlocked() {
        assertThatThrownBy(() -> converter.convert(new AtomicInteger(300), Point.class))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Unsupported conversion, source type [AtomicInteger");
    }

    @Test
    void testAtomicLongToPointBlocked() {
        assertThatThrownBy(() -> converter.convert(new AtomicLong(400), Point.class))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Unsupported conversion, source type [AtomicLong");
    }

    @Test
    void testAtomicBooleanToPoint_trueBlocked() {
        assertThatThrownBy(() -> converter.convert(new AtomicBoolean(true), Point.class))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Unsupported conversion, source type [AtomicBoolean");
    }

    @Test
    void testAtomicBooleanToPoint_falseBlocked() {
        assertThatThrownBy(() -> converter.convert(new AtomicBoolean(false), Point.class))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Unsupported conversion, source type [AtomicBoolean");
    }

    @Test
    void testBooleanToPoint_trueBlocked() {
        assertThatThrownBy(() -> converter.convert(Boolean.TRUE, Point.class))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Unsupported conversion, source type [Boolean");
    }

    @Test
    void testBooleanToPoint_falseBlocked() {
        assertThatThrownBy(() -> converter.convert(Boolean.FALSE, Point.class))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Unsupported conversion, source type [Boolean");
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
    void testPointToDimensionBlocked() {
        Point point = new Point(640, 480);
        assertThatThrownBy(() -> converter.convert(point, Dimension.class))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Unsupported conversion, source type [Point");
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
    void testPointDimensionRoundTripBlocked() {
        Point originalPoint = new Point(800, 600);
        
        // Point -> Dimension should be blocked
        assertThatThrownBy(() -> converter.convert(originalPoint, Dimension.class))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Unsupported conversion, source type [Point");
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
    void testBooleanPointRoundTrip_trueBlocked() {
        Boolean originalBoolean = Boolean.TRUE;
        
        // Boolean -> Point should be blocked
        assertThatThrownBy(() -> converter.convert(originalBoolean, Point.class))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Unsupported conversion, source type [Boolean");
    }

    @Test
    void testBooleanPointRoundTrip_falseBlocked() {
        Boolean originalBoolean = Boolean.FALSE;
        
        // Boolean -> Point should be blocked
        assertThatThrownBy(() -> converter.convert(originalBoolean, Point.class))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Unsupported conversion, source type [Boolean");
    }
}