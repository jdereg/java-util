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
    void testIntegerToRectangleBlocked() {
        assertThatThrownBy(() -> converter.convert(100, Rectangle.class))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Unsupported conversion, source type [Integer");
    }

    @Test
    void testLongToRectangleBlocked() {
        assertThatThrownBy(() -> converter.convert(64L, Rectangle.class))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Unsupported conversion, source type [Long");
    }

    @Test
    void testNumberToRectangleNegativeBlocked() {
        assertThatThrownBy(() -> converter.convert(-100, Rectangle.class))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Unsupported conversion, source type [Integer");
    }

    @Test
    void testBigIntegerToRectangleBlocked() {
        assertThatThrownBy(() -> converter.convert(BigInteger.valueOf(121), Rectangle.class))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Unsupported conversion, source type [BigInteger");
    }


    @Test
    void testAtomicIntegerToRectangleBlocked() {
        assertThatThrownBy(() -> converter.convert(new AtomicInteger(49), Rectangle.class))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Unsupported conversion, source type [AtomicInteger");
    }

    @Test
    void testAtomicLongToRectangleBlocked() {
        assertThatThrownBy(() -> converter.convert(new AtomicLong(25), Rectangle.class))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Unsupported conversion, source type [AtomicLong");
    }

    @Test
    void testAtomicBooleanToRectangle_trueBlocked() {
        assertThatThrownBy(() -> converter.convert(new AtomicBoolean(true), Rectangle.class))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Unsupported conversion, source type [AtomicBoolean");
    }

    @Test
    void testAtomicBooleanToRectangle_falseBlocked() {
        assertThatThrownBy(() -> converter.convert(new AtomicBoolean(false), Rectangle.class))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Unsupported conversion, source type [AtomicBoolean");
    }

    @Test
    void testBooleanToRectangle_trueBlocked() {
        assertThatThrownBy(() -> converter.convert(Boolean.TRUE, Rectangle.class))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Unsupported conversion, source type [Boolean");
    }

    @Test
    void testBooleanToRectangle_falseBlocked() {
        assertThatThrownBy(() -> converter.convert(Boolean.FALSE, Rectangle.class))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Unsupported conversion, source type [Boolean");
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




    // ========================================
    // Round-trip Boolean Tests
    // ========================================

    @Test
    void testBooleanRectangleRoundTrip_trueBlocked() {
        Boolean originalBoolean = Boolean.TRUE;
        
        // Boolean -> Rectangle should be blocked
        assertThatThrownBy(() -> converter.convert(originalBoolean, Rectangle.class))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Unsupported conversion, source type [Boolean");
    }

    @Test
    void testBooleanRectangleRoundTrip_falseBlocked() {
        Boolean originalBoolean = Boolean.FALSE;
        
        // Boolean -> Rectangle should be blocked
        assertThatThrownBy(() -> converter.convert(originalBoolean, Rectangle.class))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Unsupported conversion, source type [Boolean");
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