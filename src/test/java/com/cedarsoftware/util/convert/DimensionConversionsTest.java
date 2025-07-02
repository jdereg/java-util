package com.cedarsoftware.util.convert;

import java.awt.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Comprehensive tests for java.awt.Dimension conversions in the Converter.
 * Tests conversion from various types to Dimension and from Dimension to various types.
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
class DimensionConversionsTest {

    private Converter converter;

    @BeforeEach
    void setUp() {
        converter = new Converter(new DefaultConverterOptions());
    }

    // ========================================
    // Null/Void to Dimension Tests
    // ========================================

    @Test
    void testNullToDimension() {
        Dimension result = converter.convert(null, Dimension.class);
        assertThat(result).isNull();
    }

    // ========================================
    // String to Dimension Tests
    // ========================================

    @Test
    void testStringToDimension_widthXheight() {
        Dimension result = converter.convert("800x600", Dimension.class);
        assertThat(result.width).isEqualTo(800);
        assertThat(result.height).isEqualTo(600);
    }

    @Test
    void testStringToDimension_commaSeparated() {
        Dimension result = converter.convert("1920,1080", Dimension.class);
        assertThat(result.width).isEqualTo(1920);
        assertThat(result.height).isEqualTo(1080);
    }

    @Test
    void testStringToDimension_spaceSeparated() {
        Dimension result = converter.convert("640 480", Dimension.class);
        assertThat(result.width).isEqualTo(640);
        assertThat(result.height).isEqualTo(480);
    }

    @Test
    void testStringToDimension_withWhitespace() {
        Dimension result = converter.convert("  1024 x 768  ", Dimension.class);
        assertThat(result.width).isEqualTo(1024);
        assertThat(result.height).isEqualTo(768);
    }

    @Test
    void testStringToDimension_invalidFormat() {
        assertThatThrownBy(() -> converter.convert("invalid", Dimension.class))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Unable to parse dimension from string");
    }

    @Test
    void testStringToDimension_emptyString() {
        assertThatThrownBy(() -> converter.convert("", Dimension.class))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Cannot convert empty/null string to Dimension");
    }

    // ========================================
    // Map to Dimension Tests
    // ========================================

    @Test
    void testMapToDimension_widthHeight() {
        Map<String, Object> map = new HashMap<>();
        map.put("width", 800);
        map.put("height", 600);
        
        Dimension result = converter.convert(map, Dimension.class);
        assertThat(result.width).isEqualTo(800);
        assertThat(result.height).isEqualTo(600);
    }

    @Test
    void testMapToDimension_shortKeys() {
        Map<String, Object> map = new HashMap<>();
        map.put("w", 1920);
        map.put("h", 1080);
        
        Dimension result = converter.convert(map, Dimension.class);
        assertThat(result.width).isEqualTo(1920);
        assertThat(result.height).isEqualTo(1080);
    }

    @Test
    void testMapToDimension_stringValue() {
        Map<String, Object> map = new HashMap<>();
        map.put("value", "640x480");
        
        Dimension result = converter.convert(map, Dimension.class);
        assertThat(result.width).isEqualTo(640);
        assertThat(result.height).isEqualTo(480);
    }

    // ========================================
    // Array to Dimension Tests
    // ========================================

    @Test
    void testIntArrayToDimension() {
        int[] array = {800, 600};
        
        Dimension result = converter.convert(array, Dimension.class);
        assertThat(result.width).isEqualTo(800);
        assertThat(result.height).isEqualTo(600);
    }

    @Test
    void testIntArrayToDimension_invalidLength() {
        int[] array = {800};
        
        assertThatThrownBy(() -> converter.convert(array, Dimension.class))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Dimension array must have exactly 2 elements");
    }

    @Test
    void testIntArrayToDimension_negativeValues() {
        int[] array = {-800, 600};
        
        assertThatThrownBy(() -> converter.convert(array, Dimension.class))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Width and height must be non-negative");
    }

    // ========================================
    // Number to Dimension Tests
    // ========================================

    @Test
    void testIntegerToDimension() {
        Dimension result = converter.convert(500, Dimension.class);
        assertThat(result.width).isEqualTo(500);
        assertThat(result.height).isEqualTo(500);
    }

    @Test
    void testLongToDimension() {
        Dimension result = converter.convert(1000L, Dimension.class);
        assertThat(result.width).isEqualTo(1000);
        assertThat(result.height).isEqualTo(1000);
    }

    @Test
    void testNumberToDimension_negative() {
        assertThatThrownBy(() -> converter.convert(-100, Dimension.class))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Dimension size must be non-negative");
    }

    @Test
    void testBigIntegerToDimension() {
        Dimension result = converter.convert(BigInteger.valueOf(300), Dimension.class);
        assertThat(result.width).isEqualTo(300);
        assertThat(result.height).isEqualTo(300);
    }

    @Test
    void testBigDecimalToDimension() {
        Dimension result = converter.convert(BigDecimal.valueOf(400), Dimension.class);
        assertThat(result.width).isEqualTo(400);
        assertThat(result.height).isEqualTo(400);
    }

    @Test
    void testAtomicIntegerToDimension() {
        Dimension result = converter.convert(new AtomicInteger(250), Dimension.class);
        assertThat(result.width).isEqualTo(250);
        assertThat(result.height).isEqualTo(250);
    }

    @Test
    void testAtomicLongToDimension() {
        Dimension result = converter.convert(new AtomicLong(350), Dimension.class);
        assertThat(result.width).isEqualTo(350);
        assertThat(result.height).isEqualTo(350);
    }

    @Test
    void testAtomicBooleanToDimension_true() {
        Dimension result = converter.convert(new AtomicBoolean(true), Dimension.class);
        assertThat(result.width).isEqualTo(1);
        assertThat(result.height).isEqualTo(1);
    }

    @Test
    void testAtomicBooleanToDimension_false() {
        Dimension result = converter.convert(new AtomicBoolean(false), Dimension.class);
        assertThat(result.width).isEqualTo(0);
        assertThat(result.height).isEqualTo(0);
    }

    @Test
    void testBooleanToDimension_true() {
        Dimension result = converter.convert(Boolean.TRUE, Dimension.class);
        assertThat(result.width).isEqualTo(1);
        assertThat(result.height).isEqualTo(1);
    }

    @Test
    void testBooleanToDimension_false() {
        Dimension result = converter.convert(Boolean.FALSE, Dimension.class);
        assertThat(result.width).isEqualTo(0);
        assertThat(result.height).isEqualTo(0);
    }

    // ========================================
    // Dimension to String Tests
    // ========================================

    @Test
    void testDimensionToString() {
        Dimension dimension = new Dimension(800, 600);
        String result = converter.convert(dimension, String.class);
        assertThat(result).isEqualTo("800x600");
    }

    // ========================================
    // Dimension to Integer Tests (Area)
    // ========================================

    @Test
    void testDimensionToInteger() {
        Dimension dimension = new Dimension(10, 20);
        Integer result = converter.convert(dimension, Integer.class);
        assertThat(result).isEqualTo(200); // 10 * 20 = 200
    }

    // ========================================
    // Dimension to Long Tests (Area)
    // ========================================

    @Test
    void testDimensionToLong() {
        Dimension dimension = new Dimension(100, 200);
        Long result = converter.convert(dimension, Long.class);
        assertThat(result).isEqualTo(20000L); // 100 * 200 = 20000
    }

    // ========================================
    // Dimension to BigInteger Tests (Area)
    // ========================================

    @Test
    void testDimensionToBigInteger() {
        Dimension dimension = new Dimension(50, 60);
        BigInteger result = converter.convert(dimension, BigInteger.class);
        assertThat(result).isEqualTo(BigInteger.valueOf(3000L)); // 50 * 60 = 3000
    }

    // ========================================
    // Dimension to BigDecimal Tests (Area)
    // ========================================

    @Test
    void testDimensionToBigDecimal() {
        Dimension dimension = new Dimension(25, 40);
        BigDecimal result = converter.convert(dimension, BigDecimal.class);
        assertThat(result).isEqualTo(BigDecimal.valueOf(1000L)); // 25 * 40 = 1000
    }

    // ========================================
    // Dimension to Map Tests
    // ========================================

    @Test
    void testDimensionToMap() {
        Dimension dimension = new Dimension(800, 600);
        Map<String, Object> result = converter.convert(dimension, Map.class);
        
        assertThat(result).containsEntry("width", 800);
        assertThat(result).containsEntry("height", 600);
        assertThat(result).hasSize(2);
    }

    // ========================================
    // Dimension to int[] Tests
    // ========================================

    @Test
    void testDimensionToIntArray() {
        Dimension dimension = new Dimension(1920, 1080);
        int[] result = converter.convert(dimension, int[].class);
        
        assertThat(result).containsExactly(1920, 1080);
    }

    // ========================================
    // Dimension Identity Tests
    // ========================================

    @Test
    void testDimensionToDimension_identity() {
        Dimension original = new Dimension(640, 480);
        Dimension result = converter.convert(original, Dimension.class);
        
        assertThat(result).isSameAs(original);
    }

    // ========================================
    // Dimension to Boolean Tests
    // ========================================

    @Test
    void testDimensionToBoolean_zeroZero() {
        Dimension dimension = new Dimension(0, 0);
        Boolean result = converter.convert(dimension, Boolean.class);
        assertThat(result).isFalse();
    }

    @Test
    void testDimensionToBoolean_nonZero() {
        Dimension dimension = new Dimension(100, 200);
        Boolean result = converter.convert(dimension, Boolean.class);
        assertThat(result).isTrue();
    }

    @Test
    void testDimensionToBoolean_partialZero() {
        Dimension dimension = new Dimension(0, 100);
        Boolean result = converter.convert(dimension, Boolean.class);
        assertThat(result).isTrue(); // Any non-zero coordinate is true
    }

    // ========================================
    // Round-trip Boolean Tests
    // ========================================

    @Test
    void testBooleanDimensionRoundTrip_true() {
        Boolean originalBoolean = Boolean.TRUE;
        
        // Boolean -> Dimension -> Boolean
        Dimension dimension = converter.convert(originalBoolean, Dimension.class);
        Boolean backToBoolean = converter.convert(dimension, Boolean.class);
        
        assertThat(backToBoolean).isEqualTo(originalBoolean);
    }

    @Test
    void testBooleanDimensionRoundTrip_false() {
        Boolean originalBoolean = Boolean.FALSE;
        
        // Boolean -> Dimension -> Boolean
        Dimension dimension = converter.convert(originalBoolean, Dimension.class);
        Boolean backToBoolean = converter.convert(dimension, Boolean.class);
        
        assertThat(backToBoolean).isEqualTo(originalBoolean);
    }
}