package com.cedarsoftware.util.convert;

import java.awt.Dimension;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
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
 * Comprehensive tests for java.awt.Rectangle conversions in the Converter.
 * Tests conversion from various types to Rectangle and from Rectangle to various types.
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
class RectangleConversionsTest {

    private Converter converter;

    @BeforeEach
    void setUp() {
        converter = new Converter(new DefaultConverterOptions());
    }

    // ========================================
    // Null/Void to Rectangle Tests
    // ========================================

    @Test
    void testNullToRectangle() {
        Rectangle result = converter.convert(null, Rectangle.class);
        assertThat(result).isNull();
    }

    // ========================================
    // String to Rectangle Tests
    // ========================================

    @Test
    void testStringToRectangle_parenthesesFormat() {
        Rectangle result = converter.convert("(10,20,100,50)", Rectangle.class);
        assertThat(result.x).isEqualTo(10);
        assertThat(result.y).isEqualTo(20);
        assertThat(result.width).isEqualTo(100);
        assertThat(result.height).isEqualTo(50);
    }

    @Test
    void testStringToRectangle_commaSeparated() {
        Rectangle result = converter.convert("5,15,200,80", Rectangle.class);
        assertThat(result.x).isEqualTo(5);
        assertThat(result.y).isEqualTo(15);
        assertThat(result.width).isEqualTo(200);
        assertThat(result.height).isEqualTo(80);
    }

    @Test
    void testStringToRectangle_spaceSeparated() {
        Rectangle result = converter.convert("0 0 300 150", Rectangle.class);
        assertThat(result.x).isEqualTo(0);
        assertThat(result.y).isEqualTo(0);
        assertThat(result.width).isEqualTo(300);
        assertThat(result.height).isEqualTo(150);
    }

    @Test
    void testStringToRectangle_withWhitespace() {
        Rectangle result = converter.convert("  ( 25 , 30 , 400 , 200 )  ", Rectangle.class);
        assertThat(result.x).isEqualTo(25);
        assertThat(result.y).isEqualTo(30);
        assertThat(result.width).isEqualTo(400);
        assertThat(result.height).isEqualTo(200);
    }

    @Test
    void testStringToRectangle_negativeCoordinates() {
        Rectangle result = converter.convert("(-10,-20,100,50)", Rectangle.class);
        assertThat(result.x).isEqualTo(-10);
        assertThat(result.y).isEqualTo(-20);
        assertThat(result.width).isEqualTo(100);
        assertThat(result.height).isEqualTo(50);
    }

    @Test
    void testStringToRectangle_invalidFormat() {
        assertThatThrownBy(() -> converter.convert("invalid", Rectangle.class))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Unable to parse rectangle from string");
    }

    @Test
    void testStringToRectangle_emptyString() {
        assertThatThrownBy(() -> converter.convert("", Rectangle.class))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Cannot convert empty/null string to Rectangle");
    }

    @Test
    void testStringToRectangle_invalidElementCount() {
        assertThatThrownBy(() -> converter.convert("10,20,30", Rectangle.class))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Unable to parse rectangle from string");
    }

    // ========================================
    // Map to Rectangle Tests
    // ========================================

    @Test
    void testMapToRectangle_xyWidthHeight() {
        Map<String, Object> map = new HashMap<>();
        map.put("x", 10);
        map.put("y", 20);
        map.put("width", 100);
        map.put("height", 50);
        
        Rectangle result = converter.convert(map, Rectangle.class);
        assertThat(result.x).isEqualTo(10);
        assertThat(result.y).isEqualTo(20);
        assertThat(result.width).isEqualTo(100);
        assertThat(result.height).isEqualTo(50);
    }


    @Test
    void testMapToRectangle_stringValue() {
        Map<String, Object> map = new HashMap<>();
        map.put("value", "(0,0,300,150)");
        
        Rectangle result = converter.convert(map, Rectangle.class);
        assertThat(result.x).isEqualTo(0);
        assertThat(result.y).isEqualTo(0);
        assertThat(result.width).isEqualTo(300);
        assertThat(result.height).isEqualTo(150);
    }

    // ========================================
    // Array to Rectangle Tests
    // ========================================

    @Test
    void testIntArrayToRectangle() {
        int[] array = {10, 20, 100, 50};
        
        Rectangle result = converter.convert(array, Rectangle.class);
        assertThat(result.x).isEqualTo(10);
        assertThat(result.y).isEqualTo(20);
        assertThat(result.width).isEqualTo(100);
        assertThat(result.height).isEqualTo(50);
    }

    @Test
    void testIntArrayToRectangle_negativeValues() {
        int[] array = {-10, -20, 100, 50};
        
        Rectangle result = converter.convert(array, Rectangle.class);
        assertThat(result.x).isEqualTo(-10);
        assertThat(result.y).isEqualTo(-20);
        assertThat(result.width).isEqualTo(100);
        assertThat(result.height).isEqualTo(50);
    }

    @Test
    void testIntArrayToRectangle_invalidLength() {
        int[] array = {10, 20, 100};
        
        assertThatThrownBy(() -> converter.convert(array, Rectangle.class))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Rectangle array must have exactly 4 elements");
    }

    // ========================================
    // Number to Rectangle Tests (Area-based)
    // ========================================

    @Test
    void testIntegerToRectangle() {
        Rectangle result = converter.convert(100, Rectangle.class);
        assertThat(result.x).isEqualTo(0);
        assertThat(result.y).isEqualTo(0);
        assertThat(result.width).isEqualTo(10); // sqrt(100) = 10
        assertThat(result.height).isEqualTo(10);
    }

    @Test
    void testLongToRectangle() {
        Rectangle result = converter.convert(64L, Rectangle.class);
        assertThat(result.x).isEqualTo(0);
        assertThat(result.y).isEqualTo(0);
        assertThat(result.width).isEqualTo(8); // sqrt(64) = 8
        assertThat(result.height).isEqualTo(8);
    }

    @Test
    void testNumberToRectangle_negative() {
        assertThatThrownBy(() -> converter.convert(-100, Rectangle.class))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Rectangle area must be non-negative");
    }

    @Test
    void testBigIntegerToRectangle() {
        Rectangle result = converter.convert(BigInteger.valueOf(121), Rectangle.class);
        assertThat(result.x).isEqualTo(0);
        assertThat(result.y).isEqualTo(0);
        assertThat(result.width).isEqualTo(11); // sqrt(121) = 11
        assertThat(result.height).isEqualTo(11);
    }

    @Test
    void testBigDecimalToRectangle() {
        Rectangle result = converter.convert(BigDecimal.valueOf(144), Rectangle.class);
        assertThat(result.x).isEqualTo(0);
        assertThat(result.y).isEqualTo(0);
        assertThat(result.width).isEqualTo(12); // sqrt(144) = 12
        assertThat(result.height).isEqualTo(12);
    }

    @Test
    void testAtomicIntegerToRectangle() {
        Rectangle result = converter.convert(new AtomicInteger(49), Rectangle.class);
        assertThat(result.x).isEqualTo(0);
        assertThat(result.y).isEqualTo(0);
        assertThat(result.width).isEqualTo(7); // sqrt(49) = 7
        assertThat(result.height).isEqualTo(7);
    }

    @Test
    void testAtomicLongToRectangle() {
        Rectangle result = converter.convert(new AtomicLong(25), Rectangle.class);
        assertThat(result.x).isEqualTo(0);
        assertThat(result.y).isEqualTo(0);
        assertThat(result.width).isEqualTo(5); // sqrt(25) = 5
        assertThat(result.height).isEqualTo(5);
    }

    @Test
    void testAtomicBooleanToRectangle_true() {
        Rectangle result = converter.convert(new AtomicBoolean(true), Rectangle.class);
        assertThat(result.x).isEqualTo(1);
        assertThat(result.y).isEqualTo(1);
        assertThat(result.width).isEqualTo(1);
        assertThat(result.height).isEqualTo(1);
    }

    @Test
    void testAtomicBooleanToRectangle_false() {
        Rectangle result = converter.convert(new AtomicBoolean(false), Rectangle.class);
        assertThat(result.x).isEqualTo(0);
        assertThat(result.y).isEqualTo(0);
        assertThat(result.width).isEqualTo(0);
        assertThat(result.height).isEqualTo(0);
    }

    @Test
    void testBooleanToRectangle_true() {
        Rectangle result = converter.convert(Boolean.TRUE, Rectangle.class);
        assertThat(result.x).isEqualTo(1);
        assertThat(result.y).isEqualTo(1);
        assertThat(result.width).isEqualTo(1);
        assertThat(result.height).isEqualTo(1);
    }

    @Test
    void testBooleanToRectangle_false() {
        Rectangle result = converter.convert(Boolean.FALSE, Rectangle.class);
        assertThat(result.x).isEqualTo(0);
        assertThat(result.y).isEqualTo(0);
        assertThat(result.width).isEqualTo(0);
        assertThat(result.height).isEqualTo(0);
    }

    // ========================================
    // AWT Type Cross-Conversions to Rectangle
    // ========================================

    @Test
    void testPointToRectangle() {
        Point point = new Point(50, 75);
        Rectangle result = converter.convert(point, Rectangle.class);
        assertThat(result.x).isEqualTo(50);
        assertThat(result.y).isEqualTo(75);
        assertThat(result.width).isEqualTo(0);
        assertThat(result.height).isEqualTo(0);
    }

    @Test
    void testDimensionToRectangle() {
        Dimension dimension = new Dimension(200, 150);
        Rectangle result = converter.convert(dimension, Rectangle.class);
        assertThat(result.x).isEqualTo(0);
        assertThat(result.y).isEqualTo(0);
        assertThat(result.width).isEqualTo(200);
        assertThat(result.height).isEqualTo(150);
    }

    @Test
    void testInsetsToRectangle() {
        Insets insets = new Insets(10, 20, 30, 40);
        Rectangle result = converter.convert(insets, Rectangle.class);
        assertThat(result.x).isEqualTo(20); // left
        assertThat(result.y).isEqualTo(10); // top
        assertThat(result.width).isEqualTo(20); // right - left = 40 - 20
        assertThat(result.height).isEqualTo(20); // bottom - top = 30 - 10
    }

    // ========================================
    // Rectangle to String Tests
    // ========================================

    @Test
    void testRectangleToString() {
        Rectangle rectangle = new Rectangle(10, 20, 100, 50);
        String result = converter.convert(rectangle, String.class);
        assertThat(result).isEqualTo("(10,20,100,50)");
    }

    // ========================================
    // Rectangle to Integer Tests (Area)
    // ========================================

    @Test
    void testRectangleToInteger() {
        Rectangle rectangle = new Rectangle(0, 0, 10, 20);
        Integer result = converter.convert(rectangle, Integer.class);
        assertThat(result).isEqualTo(200); // 10 * 20 = 200
    }

    // ========================================
    // Rectangle to Long Tests (Area)
    // ========================================

    @Test
    void testRectangleToLong() {
        Rectangle rectangle = new Rectangle(5, 5, 100, 200);
        Long result = converter.convert(rectangle, Long.class);
        assertThat(result).isEqualTo(20000L); // 100 * 200 = 20000
    }

    // ========================================
    // Rectangle to BigInteger Tests (Area)
    // ========================================

    @Test
    void testRectangleToBigInteger() {
        Rectangle rectangle = new Rectangle(0, 0, 50, 60);
        BigInteger result = converter.convert(rectangle, BigInteger.class);
        assertThat(result).isEqualTo(BigInteger.valueOf(3000L)); // 50 * 60 = 3000
    }

    // ========================================
    // Rectangle to BigDecimal Tests (Area)
    // ========================================

    @Test
    void testRectangleToBigDecimal() {
        Rectangle rectangle = new Rectangle(10, 15, 25, 40);
        BigDecimal result = converter.convert(rectangle, BigDecimal.class);
        assertThat(result).isEqualTo(BigDecimal.valueOf(1000L)); // 25 * 40 = 1000
    }

    // ========================================
    // Rectangle to Map Tests
    // ========================================

    @Test
    void testRectangleToMap() {
        Rectangle rectangle = new Rectangle(10, 20, 100, 50);
        Map<String, Object> result = converter.convert(rectangle, Map.class);
        
        assertThat(result).containsEntry("x", 10);
        assertThat(result).containsEntry("y", 20);
        assertThat(result).containsEntry("width", 100);
        assertThat(result).containsEntry("height", 50);
        assertThat(result).hasSize(4);
    }

    // ========================================
    // Rectangle to int[] Tests
    // ========================================

    @Test
    void testRectangleToIntArray() {
        Rectangle rectangle = new Rectangle(5, 15, 200, 80);
        int[] result = converter.convert(rectangle, int[].class);
        
        assertThat(result).containsExactly(5, 15, 200, 80);
    }

    // ========================================
    // Rectangle Identity Tests
    // ========================================

    @Test
    void testRectangleToRectangle_identity() {
        Rectangle original = new Rectangle(0, 0, 300, 150);
        Rectangle result = converter.convert(original, Rectangle.class);
        
        assertThat(result).isSameAs(original);
    }

    // ========================================
    // Rectangle to Boolean Tests
    // ========================================

    @Test
    void testRectangleToBoolean_allZero() {
        Rectangle rectangle = new Rectangle(0, 0, 0, 0);
        Boolean result = converter.convert(rectangle, Boolean.class);
        assertThat(result).isFalse();
    }

    @Test
    void testRectangleToBoolean_nonZero() {
        Rectangle rectangle = new Rectangle(10, 20, 100, 50);
        Boolean result = converter.convert(rectangle, Boolean.class);
        assertThat(result).isTrue();
    }

    @Test
    void testRectangleToBoolean_partialZero() {
        Rectangle rectangle = new Rectangle(0, 0, 100, 0);
        Boolean result = converter.convert(rectangle, Boolean.class);
        assertThat(result).isTrue(); // Any non-zero coordinate is true
    }

    // ========================================
    // Rectangle to AWT Type Cross-Conversions
    // ========================================

    @Test
    void testRectangleToPoint() {
        Rectangle rectangle = new Rectangle(25, 35, 100, 200);
        Point result = converter.convert(rectangle, Point.class);
        assertThat(result.x).isEqualTo(25);
        assertThat(result.y).isEqualTo(35);
    }

    @Test
    void testRectangleToDimension() {
        Rectangle rectangle = new Rectangle(10, 20, 150, 250);
        Dimension result = converter.convert(rectangle, Dimension.class);
        assertThat(result.width).isEqualTo(150);
        assertThat(result.height).isEqualTo(250);
    }

    @Test
    void testRectangleToInsets() {
        Rectangle rectangle = new Rectangle(5, 10, 100, 200);
        Insets result = converter.convert(rectangle, Insets.class);
        assertThat(result.top).isEqualTo(10); // y
        assertThat(result.left).isEqualTo(5); // x
        assertThat(result.bottom).isEqualTo(210); // y + height = 10 + 200
        assertThat(result.right).isEqualTo(105); // x + width = 5 + 100
    }

    // ========================================
    // Round-trip Boolean Tests
    // ========================================

    @Test
    void testBooleanRectangleRoundTrip_true() {
        Boolean originalBoolean = Boolean.TRUE;
        
        // Boolean -> Rectangle -> Boolean
        Rectangle rectangle = converter.convert(originalBoolean, Rectangle.class);
        Boolean backToBoolean = converter.convert(rectangle, Boolean.class);
        
        assertThat(backToBoolean).isEqualTo(originalBoolean);
    }

    @Test
    void testBooleanRectangleRoundTrip_false() {
        Boolean originalBoolean = Boolean.FALSE;
        
        // Boolean -> Rectangle -> Boolean
        Rectangle rectangle = converter.convert(originalBoolean, Rectangle.class);
        Boolean backToBoolean = converter.convert(rectangle, Boolean.class);
        
        assertThat(backToBoolean).isEqualTo(originalBoolean);
    }

    // ========================================
    // Cross-Type Round-trip Tests
    // ========================================

    @Test
    void testRectanglePointRoundTrip() {
        Rectangle originalRectangle = new Rectangle(30, 40, 0, 0);
        
        // Rectangle -> Point -> Rectangle
        Point point = converter.convert(originalRectangle, Point.class);
        Rectangle backToRectangle = converter.convert(point, Rectangle.class);
        
        assertThat(backToRectangle.x).isEqualTo(originalRectangle.x);
        assertThat(backToRectangle.y).isEqualTo(originalRectangle.y);
        assertThat(backToRectangle.width).isEqualTo(0);
        assertThat(backToRectangle.height).isEqualTo(0);
    }

    @Test
    void testRectangleDimensionRoundTrip() {
        Rectangle originalRectangle = new Rectangle(0, 0, 120, 80);
        
        // Rectangle -> Dimension -> Rectangle  
        Dimension dimension = converter.convert(originalRectangle, Dimension.class);
        Rectangle backToRectangle = converter.convert(dimension, Rectangle.class);
        
        assertThat(backToRectangle.x).isEqualTo(0);
        assertThat(backToRectangle.y).isEqualTo(0);
        assertThat(backToRectangle.width).isEqualTo(originalRectangle.width);
        assertThat(backToRectangle.height).isEqualTo(originalRectangle.height);
    }
}