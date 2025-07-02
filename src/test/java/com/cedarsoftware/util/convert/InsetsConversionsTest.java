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
 * Comprehensive tests for java.awt.Insets conversions in the Converter.
 * Tests conversion from various types to Insets and from Insets to various types.
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
class InsetsConversionsTest {

    private Converter converter;

    @BeforeEach
    void setUp() {
        converter = new Converter(new DefaultConverterOptions());
    }

    // ========================================
    // Null/Void to Insets Tests
    // ========================================

    @Test
    void testNullToInsets() {
        Insets result = converter.convert(null, Insets.class);
        assertThat(result).isNull();
    }

    // ========================================
    // String to Insets Tests
    // ========================================

    @Test
    void testStringToInsets_parenthesesFormat() {
        Insets result = converter.convert("(5,10,15,20)", Insets.class);
        assertThat(result.top).isEqualTo(5);
        assertThat(result.left).isEqualTo(10);
        assertThat(result.bottom).isEqualTo(15);
        assertThat(result.right).isEqualTo(20);
    }

    @Test
    void testStringToInsets_commaSeparated() {
        Insets result = converter.convert("8,12,16,24", Insets.class);
        assertThat(result.top).isEqualTo(8);
        assertThat(result.left).isEqualTo(12);
        assertThat(result.bottom).isEqualTo(16);
        assertThat(result.right).isEqualTo(24);
    }

    @Test
    void testStringToInsets_spaceSeparated() {
        Insets result = converter.convert("1 2 3 4", Insets.class);
        assertThat(result.top).isEqualTo(1);
        assertThat(result.left).isEqualTo(2);
        assertThat(result.bottom).isEqualTo(3);
        assertThat(result.right).isEqualTo(4);
    }

    @Test
    void testStringToInsets_withWhitespace() {
        Insets result = converter.convert("  ( 10 , 20 , 30 , 40 )  ", Insets.class);
        assertThat(result.top).isEqualTo(10);
        assertThat(result.left).isEqualTo(20);
        assertThat(result.bottom).isEqualTo(30);
        assertThat(result.right).isEqualTo(40);
    }

    @Test
    void testStringToInsets_negativeValues() {
        Insets result = converter.convert("(-5,-10,15,20)", Insets.class);
        assertThat(result.top).isEqualTo(-5);
        assertThat(result.left).isEqualTo(-10);
        assertThat(result.bottom).isEqualTo(15);
        assertThat(result.right).isEqualTo(20);
    }

    @Test
    void testStringToInsets_allZero() {
        Insets result = converter.convert("(0,0,0,0)", Insets.class);
        assertThat(result.top).isEqualTo(0);
        assertThat(result.left).isEqualTo(0);
        assertThat(result.bottom).isEqualTo(0);
        assertThat(result.right).isEqualTo(0);
    }

    @Test
    void testStringToInsets_invalidFormat() {
        assertThatThrownBy(() -> converter.convert("invalid", Insets.class))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Unable to parse insets from string");
    }

    @Test
    void testStringToInsets_emptyString() {
        assertThatThrownBy(() -> converter.convert("", Insets.class))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Cannot convert empty/null string to Insets");
    }

    @Test
    void testStringToInsets_invalidElementCount() {
        assertThatThrownBy(() -> converter.convert("10,20,30", Insets.class))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Unable to parse insets from string");
    }

    // ========================================
    // Map to Insets Tests
    // ========================================

    @Test
    void testMapToInsets_topLeftBottomRight() {
        Map<String, Object> map = new HashMap<>();
        map.put("top", 5);
        map.put("left", 10);
        map.put("bottom", 15);
        map.put("right", 20);
        
        Insets result = converter.convert(map, Insets.class);
        assertThat(result.top).isEqualTo(5);
        assertThat(result.left).isEqualTo(10);
        assertThat(result.bottom).isEqualTo(15);
        assertThat(result.right).isEqualTo(20);
    }

    @Test
    void testMapToInsets_stringValue() {
        Map<String, Object> map = new HashMap<>();
        map.put("value", "(8,12,16,24)");
        
        Insets result = converter.convert(map, Insets.class);
        assertThat(result.top).isEqualTo(8);
        assertThat(result.left).isEqualTo(12);
        assertThat(result.bottom).isEqualTo(16);
        assertThat(result.right).isEqualTo(24);
    }

    @Test
    void testMapToInsets_withV() {
        Map<String, Object> map = new HashMap<>();
        map.put("_v", "1,2,3,4");
        
        Insets result = converter.convert(map, Insets.class);
        assertThat(result.top).isEqualTo(1);
        assertThat(result.left).isEqualTo(2);
        assertThat(result.bottom).isEqualTo(3);
        assertThat(result.right).isEqualTo(4);
    }

    // ========================================
    // Array to Insets Tests
    // ========================================

    @Test
    void testIntArrayToInsets() {
        int[] array = {5, 10, 15, 20};
        
        Insets result = converter.convert(array, Insets.class);
        assertThat(result.top).isEqualTo(5);
        assertThat(result.left).isEqualTo(10);
        assertThat(result.bottom).isEqualTo(15);
        assertThat(result.right).isEqualTo(20);
    }

    @Test
    void testIntArrayToInsets_negativeValues() {
        int[] array = {-5, -10, 15, 20};
        
        Insets result = converter.convert(array, Insets.class);
        assertThat(result.top).isEqualTo(-5);
        assertThat(result.left).isEqualTo(-10);
        assertThat(result.bottom).isEqualTo(15);
        assertThat(result.right).isEqualTo(20);
    }

    @Test
    void testIntArrayToInsets_invalidLength() {
        int[] array = {5, 10, 15};
        
        assertThatThrownBy(() -> converter.convert(array, Insets.class))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Insets array must have exactly 4 elements");
    }

    // ========================================
    // Number to Insets Tests (Uniform values)
    // ========================================

    @Test
    void testIntegerToInsets() {
        Insets result = converter.convert(10, Insets.class);
        assertThat(result.top).isEqualTo(10);
        assertThat(result.left).isEqualTo(10);
        assertThat(result.bottom).isEqualTo(10);
        assertThat(result.right).isEqualTo(10);
    }

    @Test
    void testLongToInsets() {
        Insets result = converter.convert(25L, Insets.class);
        assertThat(result.top).isEqualTo(25);
        assertThat(result.left).isEqualTo(25);
        assertThat(result.bottom).isEqualTo(25);
        assertThat(result.right).isEqualTo(25);
    }

    @Test
    void testNumberToInsets_negative() {
        Insets result = converter.convert(-5, Insets.class);
        assertThat(result.top).isEqualTo(-5);
        assertThat(result.left).isEqualTo(-5);
        assertThat(result.bottom).isEqualTo(-5);
        assertThat(result.right).isEqualTo(-5);
    }

    @Test
    void testBigIntegerToInsets() {
        Insets result = converter.convert(BigInteger.valueOf(15), Insets.class);
        assertThat(result.top).isEqualTo(15);
        assertThat(result.left).isEqualTo(15);
        assertThat(result.bottom).isEqualTo(15);
        assertThat(result.right).isEqualTo(15);
    }

    @Test
    void testBigDecimalToInsets() {
        Insets result = converter.convert(BigDecimal.valueOf(8), Insets.class);
        assertThat(result.top).isEqualTo(8);
        assertThat(result.left).isEqualTo(8);
        assertThat(result.bottom).isEqualTo(8);
        assertThat(result.right).isEqualTo(8);
    }

    @Test
    void testAtomicIntegerToInsets() {
        Insets result = converter.convert(new AtomicInteger(12), Insets.class);
        assertThat(result.top).isEqualTo(12);
        assertThat(result.left).isEqualTo(12);
        assertThat(result.bottom).isEqualTo(12);
        assertThat(result.right).isEqualTo(12);
    }

    @Test
    void testAtomicLongToInsets() {
        Insets result = converter.convert(new AtomicLong(18), Insets.class);
        assertThat(result.top).isEqualTo(18);
        assertThat(result.left).isEqualTo(18);
        assertThat(result.bottom).isEqualTo(18);
        assertThat(result.right).isEqualTo(18);
    }

    @Test
    void testAtomicBooleanToInsets_true() {
        Insets result = converter.convert(new AtomicBoolean(true), Insets.class);
        assertThat(result.top).isEqualTo(1);
        assertThat(result.left).isEqualTo(1);
        assertThat(result.bottom).isEqualTo(1);
        assertThat(result.right).isEqualTo(1);
    }

    @Test
    void testAtomicBooleanToInsets_false() {
        Insets result = converter.convert(new AtomicBoolean(false), Insets.class);
        assertThat(result.top).isEqualTo(0);
        assertThat(result.left).isEqualTo(0);
        assertThat(result.bottom).isEqualTo(0);
        assertThat(result.right).isEqualTo(0);
    }

    @Test
    void testBooleanToInsets_true() {
        Insets result = converter.convert(Boolean.TRUE, Insets.class);
        assertThat(result.top).isEqualTo(1);
        assertThat(result.left).isEqualTo(1);
        assertThat(result.bottom).isEqualTo(1);
        assertThat(result.right).isEqualTo(1);
    }

    @Test
    void testBooleanToInsets_false() {
        Insets result = converter.convert(Boolean.FALSE, Insets.class);
        assertThat(result.top).isEqualTo(0);
        assertThat(result.left).isEqualTo(0);
        assertThat(result.bottom).isEqualTo(0);
        assertThat(result.right).isEqualTo(0);
    }

    // ========================================
    // AWT Type Cross-Conversions to Insets
    // ========================================

    @Test
    void testPointToInsets() {
        Point point = new Point(25, 35);
        Insets result = converter.convert(point, Insets.class);
        assertThat(result.top).isEqualTo(25); // x becomes top
        assertThat(result.left).isEqualTo(35); // y becomes left
        assertThat(result.bottom).isEqualTo(0); // bottom is 0
        assertThat(result.right).isEqualTo(0); // right is 0
    }

    @Test
    void testDimensionToInsets() {
        Dimension dimension = new Dimension(100, 200);
        Insets result = converter.convert(dimension, Insets.class);
        int minValue = Math.min(100, 200); // min(width, height) = 100
        assertThat(result.top).isEqualTo(minValue); // all sides = min value
        assertThat(result.left).isEqualTo(minValue);
        assertThat(result.bottom).isEqualTo(minValue);
        assertThat(result.right).isEqualTo(minValue);
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
    // Insets to String Tests
    // ========================================

    @Test
    void testInsetsToString() {
        Insets insets = new Insets(5, 10, 15, 20);
        String result = converter.convert(insets, String.class);
        assertThat(result).isEqualTo("(5,10,15,20)");
    }

    // ========================================
    // Insets to Integer Tests (Sum)
    // ========================================

    @Test
    void testInsetsToInteger() {
        Insets insets = new Insets(5, 10, 15, 20);
        Integer result = converter.convert(insets, Integer.class);
        assertThat(result).isEqualTo(50); // 5 + 10 + 15 + 20 = 50
    }

    // ========================================
    // Insets to Long Tests (Sum)
    // ========================================

    @Test
    void testInsetsToLong() {
        Insets insets = new Insets(100, 200, 300, 400);
        Long result = converter.convert(insets, Long.class);
        assertThat(result).isEqualTo(1000L); // 100 + 200 + 300 + 400 = 1000
    }

    // ========================================
    // Insets to BigInteger Tests (Sum)
    // ========================================

    @Test
    void testInsetsToBigInteger() {
        Insets insets = new Insets(25, 50, 75, 100);
        BigInteger result = converter.convert(insets, BigInteger.class);
        assertThat(result).isEqualTo(BigInteger.valueOf(250L)); // 25 + 50 + 75 + 100 = 250
    }

    // ========================================
    // Insets to BigDecimal Tests (Sum)
    // ========================================

    @Test
    void testInsetsToBigDecimal() {
        Insets insets = new Insets(12, 24, 36, 48);
        BigDecimal result = converter.convert(insets, BigDecimal.class);
        assertThat(result).isEqualTo(BigDecimal.valueOf(120L)); // 12 + 24 + 36 + 48 = 120
    }

    // ========================================
    // Insets to Map Tests
    // ========================================

    @Test
    void testInsetsToMap() {
        Insets insets = new Insets(5, 10, 15, 20);
        Map<String, Object> result = converter.convert(insets, Map.class);
        
        assertThat(result).containsEntry("top", 5);
        assertThat(result).containsEntry("left", 10);
        assertThat(result).containsEntry("bottom", 15);
        assertThat(result).containsEntry("right", 20);
        assertThat(result).hasSize(4);
    }

    // ========================================
    // Insets to int[] Tests
    // ========================================

    @Test
    void testInsetsToIntArray() {
        Insets insets = new Insets(8, 12, 16, 24);
        int[] result = converter.convert(insets, int[].class);
        
        assertThat(result).containsExactly(8, 12, 16, 24);
    }

    // ========================================
    // Insets Identity Tests
    // ========================================

    @Test
    void testInsetsToInsets_identity() {
        Insets original = new Insets(1, 2, 3, 4);
        Insets result = converter.convert(original, Insets.class);
        
        assertThat(result).isSameAs(original);
    }

    // ========================================
    // Insets to Boolean Tests
    // ========================================

    @Test
    void testInsetsToBoolean_allZero() {
        Insets insets = new Insets(0, 0, 0, 0);
        Boolean result = converter.convert(insets, Boolean.class);
        assertThat(result).isFalse();
    }

    @Test
    void testInsetsToBoolean_nonZero() {
        Insets insets = new Insets(5, 10, 15, 20);
        Boolean result = converter.convert(insets, Boolean.class);
        assertThat(result).isTrue();
    }

    @Test
    void testInsetsToBoolean_partialZero() {
        Insets insets = new Insets(0, 0, 15, 0);
        Boolean result = converter.convert(insets, Boolean.class);
        assertThat(result).isTrue(); // Any non-zero inset is true
    }

    // ========================================
    // Insets to AWT Type Cross-Conversions
    // ========================================

    @Test
    void testInsetsToPoint() {
        Insets insets = new Insets(25, 35, 45, 55);
        Point result = converter.convert(insets, Point.class);
        assertThat(result.x).isEqualTo(25); // top becomes x
        assertThat(result.y).isEqualTo(35); // left becomes y
    }

    @Test
    void testInsetsToDimension() {
        Insets insets = new Insets(10, 20, 30, 40);
        Dimension result = converter.convert(insets, Dimension.class);
        assertThat(result.width).isEqualTo(60); // left + right = 20 + 40
        assertThat(result.height).isEqualTo(40); // top + bottom = 10 + 30
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
    // Round-trip Boolean Tests
    // ========================================

    @Test
    void testBooleanInsetsRoundTrip_true() {
        Boolean originalBoolean = Boolean.TRUE;
        
        // Boolean -> Insets -> Boolean
        Insets insets = converter.convert(originalBoolean, Insets.class);
        Boolean backToBoolean = converter.convert(insets, Boolean.class);
        
        assertThat(backToBoolean).isEqualTo(originalBoolean);
    }

    @Test
    void testBooleanInsetsRoundTrip_false() {
        Boolean originalBoolean = Boolean.FALSE;
        
        // Boolean -> Insets -> Boolean
        Insets insets = converter.convert(originalBoolean, Insets.class);
        Boolean backToBoolean = converter.convert(insets, Boolean.class);
        
        assertThat(backToBoolean).isEqualTo(originalBoolean);
    }

    // ========================================
    // Cross-Type Round-trip Tests
    // ========================================

    @Test
    void testInsetsPointRoundTrip() {
        Insets originalInsets = new Insets(15, 25, 0, 0);
        
        // Insets -> Point -> Insets (round-trip preserves top/left since bottom/right were 0)
        Point point = converter.convert(originalInsets, Point.class);
        Insets backToInsets = converter.convert(point, Insets.class);
        
        assertThat(backToInsets.top).isEqualTo(originalInsets.top);
        assertThat(backToInsets.left).isEqualTo(originalInsets.left);
        assertThat(backToInsets.bottom).isEqualTo(0); // Point -> Insets always sets bottom to 0
        assertThat(backToInsets.right).isEqualTo(0); // Point -> Insets always sets right to 0
    }

    @Test
    void testInsetsDimensionRoundTrip() {
        Insets originalInsets = new Insets(20, 20, 20, 20); // uniform insets
        
        // Insets -> Dimension -> Insets (partial round-trip due to min() logic)
        Dimension dimension = converter.convert(originalInsets, Dimension.class);
        Insets backToInsets = converter.convert(dimension, Insets.class);
        
        // Dimension conversion uses min(width, height), so all sides become the same
        int expectedValue = Math.min(dimension.width, dimension.height);
        assertThat(backToInsets.top).isEqualTo(expectedValue);
        assertThat(backToInsets.left).isEqualTo(expectedValue);
        assertThat(backToInsets.bottom).isEqualTo(expectedValue);
        assertThat(backToInsets.right).isEqualTo(expectedValue);
    }

    // ========================================
    // Numeric Sum Tests
    // ========================================

    @Test
    void testInsetsToNumber_zeroSum() {
        Insets insets = new Insets(0, 0, 0, 0);
        Integer result = converter.convert(insets, Integer.class);
        assertThat(result).isEqualTo(0);
    }

    @Test
    void testInsetsToNumber_negativeSum() {
        Insets insets = new Insets(-10, -20, 5, 15);
        Integer result = converter.convert(insets, Integer.class);
        assertThat(result).isEqualTo(-10); // -10 + -20 + 5 + 15 = -10
    }

    @Test
    void testInsetsToNumber_largeSum() {
        Insets insets = new Insets(1000, 2000, 3000, 4000);
        Long result = converter.convert(insets, Long.class);
        assertThat(result).isEqualTo(10000L); // 1000 + 2000 + 3000 + 4000 = 10000
    }
}