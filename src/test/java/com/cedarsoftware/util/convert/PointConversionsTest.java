package com.cedarsoftware.util.convert;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import com.cedarsoftware.util.geom.Dimension;
import com.cedarsoftware.util.geom.Point;
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
    // String to Point Tests
    // ========================================

    @Test
    void testStringToPoint_spaceSeparated() {
        Point result = converter.convert("300 400", Point.class);
        assertThat(result.getX()).isEqualTo(300);
        assertThat(result.getY()).isEqualTo(400);
    }

    @Test
    void testStringToPoint_withWhitespace() {
        Point result = converter.convert("  ( 50 , 75 )  ", Point.class);
        assertThat(result.getX()).isEqualTo(50);
        assertThat(result.getY()).isEqualTo(75);
    }

    @Test
    void testStringToPoint_negativeCoordinates() {
        Point result = converter.convert("(-10,-20)", Point.class);
        assertThat(result.getX()).isEqualTo(-10);
        assertThat(result.getY()).isEqualTo(-20);
    }

    @Test
    void testStringToPoint_invalidFormat() {
        assertThatThrownBy(() -> converter.convert("invalid", Point.class))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Unable to parse point from string");
    }

    @Test
    void testStringToPoint_emptyString() {
        assertThat((Object) converter.convert("", Point.class)).isNull();   // blank text: null, as for the dates
    }

    // ========================================
    // Map to Point Tests
    // ========================================

    @Test
    void testMapToPoint_stringValue() {
        Map<String, Object> map = new HashMap<>();
        map.put("value", "(75,125)");
        
        Point result = converter.convert(map, Point.class);
        assertThat(result.getX()).isEqualTo(75);
        assertThat(result.getY()).isEqualTo(125);
    }

    // ========================================
    // Array to Point Tests
    // ========================================

    @Test
    void testIntArrayToPoint_negativeValues() {
        int[] array = {-50, -100};
        
        Point result = converter.convert(array, Point.class);
        assertThat(result.getX()).isEqualTo(-50);
        assertThat(result.getY()).isEqualTo(-100);
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
    // Point to Boolean Tests
    // ========================================

    @Test
    void testPointToBoolean_partialZero() {
        Point point = new Point(0, 100);
        Boolean result = converter.convert(point, Boolean.class);
        assertThat(result).isTrue(); // Any non-zero coordinate is true
    }
}