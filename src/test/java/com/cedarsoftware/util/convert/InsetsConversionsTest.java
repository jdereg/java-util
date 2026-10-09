package com.cedarsoftware.util.convert;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import com.cedarsoftware.util.geom.Insets;
import com.cedarsoftware.util.geom.Point;
import com.cedarsoftware.util.geom.Rectangle;
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
    // String to Insets Tests
    // ========================================

    @Test
    void testStringToInsets_spaceSeparated() {
        Insets result = converter.convert("1 2 3 4", Insets.class);
        assertThat(result.getTop()).isEqualTo(1);
        assertThat(result.getLeft()).isEqualTo(2);
        assertThat(result.getBottom()).isEqualTo(3);
        assertThat(result.getRight()).isEqualTo(4);
    }

    @Test
    void testStringToInsets_withWhitespace() {
        Insets result = converter.convert("  ( 10 , 20 , 30 , 40 )  ", Insets.class);
        assertThat(result.getTop()).isEqualTo(10);
        assertThat(result.getLeft()).isEqualTo(20);
        assertThat(result.getBottom()).isEqualTo(30);
        assertThat(result.getRight()).isEqualTo(40);
    }

    @Test
    void testStringToInsets_negativeValues() {
        Insets result = converter.convert("(-5,-10,15,20)", Insets.class);
        assertThat(result.getTop()).isEqualTo(-5);
        assertThat(result.getLeft()).isEqualTo(-10);
        assertThat(result.getBottom()).isEqualTo(15);
        assertThat(result.getRight()).isEqualTo(20);
    }

    @Test
    void testStringToInsets_invalidFormat() {
        assertThatThrownBy(() -> converter.convert("invalid", Insets.class))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Unable to parse insets from string");
    }

    @Test
    void testStringToInsets_emptyString() {
        assertThat((Object) converter.convert("", Insets.class)).isNull();   // blank text: null, as for the dates
    }

    @Test
    void testStringToInsets_invalidElementCount() {
        assertThatThrownBy(() -> converter.convert("10,20,30", Insets.class))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Unable to parse insets from string");
    }

    @Test
    void testStringToInsets_nativeToStringFormat() {
        Insets result = converter.convert("java.awt.Insets[top=5,left=10,bottom=15,right=20]", Insets.class);
        assertThat(result.getTop()).isEqualTo(5);
        assertThat(result.getLeft()).isEqualTo(10);
        assertThat(result.getBottom()).isEqualTo(15);
        assertThat(result.getRight()).isEqualTo(20);
    }

    @Test
    void testStringToInsets_nativeToStringFormat_withWhitespace() {
        Insets result = converter.convert("  java.awt.Insets[top=8,left=12,bottom=16,right=24]  ", Insets.class);
        assertThat(result.getTop()).isEqualTo(8);
        assertThat(result.getLeft()).isEqualTo(12);
        assertThat(result.getBottom()).isEqualTo(16);
        assertThat(result.getRight()).isEqualTo(24);
    }

    @Test
    void testStringToInsets_nativeToStringFormat_negativeValues() {
        Insets result = converter.convert("java.awt.Insets[top=-5,left=-10,bottom=15,right=20]", Insets.class);
        assertThat(result.getTop()).isEqualTo(-5);
        assertThat(result.getLeft()).isEqualTo(-10);
        assertThat(result.getBottom()).isEqualTo(15);
        assertThat(result.getRight()).isEqualTo(20);
    }

    // ========================================
    // Map to Insets Tests
    // ========================================

    @Test
    void testMapToInsets_stringValue() {
        Map<String, Object> map = new HashMap<>();
        map.put("value", "(8,12,16,24)");
        
        Insets result = converter.convert(map, Insets.class);
        assertThat(result.getTop()).isEqualTo(8);
        assertThat(result.getLeft()).isEqualTo(12);
        assertThat(result.getBottom()).isEqualTo(16);
        assertThat(result.getRight()).isEqualTo(24);
    }

    @Test
    void testMapToInsets_withV() {
        Map<String, Object> map = new HashMap<>();
        map.put("_v", "1,2,3,4");
        
        Insets result = converter.convert(map, Insets.class);
        assertThat(result.getTop()).isEqualTo(1);
        assertThat(result.getLeft()).isEqualTo(2);
        assertThat(result.getBottom()).isEqualTo(3);
        assertThat(result.getRight()).isEqualTo(4);
    }

    // ========================================
    // Array to Insets Tests
    // ========================================

    @Test
    void testIntArrayToInsets_negativeValues() {
        int[] array = {-5, -10, 15, 20};
        
        Insets result = converter.convert(array, Insets.class);
        assertThat(result.getTop()).isEqualTo(-5);
        assertThat(result.getLeft()).isEqualTo(-10);
        assertThat(result.getBottom()).isEqualTo(15);
        assertThat(result.getRight()).isEqualTo(20);
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
    void testIntegerToInsetsBlocked() {
        assertThatThrownBy(() -> converter.convert(10, Insets.class))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Unsupported conversion, source type [Integer");
    }

    @Test
    void testLongToInsetsBlocked() {
        assertThatThrownBy(() -> converter.convert(25L, Insets.class))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Unsupported conversion, source type [Long");
    }

    @Test
    void testNumberToInsetsNegativeBlocked() {
        assertThatThrownBy(() -> converter.convert(-5, Insets.class))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Unsupported conversion, source type [Integer");
    }

    @Test
    void testBigIntegerToInsetsBlocked() {
        assertThatThrownBy(() -> converter.convert(BigInteger.valueOf(15), Insets.class))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Unsupported conversion, source type [BigInteger");
    }

    @Test
    void testAtomicIntegerToInsetsBlocked() {
        assertThatThrownBy(() -> converter.convert(new AtomicInteger(12), Insets.class))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Unsupported conversion, source type [AtomicInteger");
    }

    @Test
    void testAtomicLongToInsetsBlocked() {
        assertThatThrownBy(() -> converter.convert(new AtomicLong(18), Insets.class))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Unsupported conversion, source type [AtomicLong");
    }

    @Test
    void testAtomicBooleanToInsets_trueBlocked() {
        assertThatThrownBy(() -> converter.convert(new AtomicBoolean(true), Insets.class))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Unsupported conversion, source type [AtomicBoolean");
    }

    @Test
    void testAtomicBooleanToInsets_falseBlocked() {
        assertThatThrownBy(() -> converter.convert(new AtomicBoolean(false), Insets.class))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Unsupported conversion, source type [AtomicBoolean");
    }

    @Test
    void testBooleanToInsets_trueBlocked() {
        assertThatThrownBy(() -> converter.convert(Boolean.TRUE, Insets.class))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Unsupported conversion, source type [Boolean");
    }

    @Test
    void testBooleanToInsets_falseBlocked() {
        assertThatThrownBy(() -> converter.convert(Boolean.FALSE, Insets.class))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Unsupported conversion, source type [Boolean");
    }

    // ========================================
    // AWT Type Cross-Conversions to Insets
    // ========================================

    @Test
    void testPointToInsetsBlocked() {
        Point point = new Point(25, 35);
        assertThatThrownBy(() -> converter.convert(point, Insets.class))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Unsupported conversion, source type [Point");
    }

    @Test
    void testRectangleToInsetsBlocked() {
        Rectangle rectangle = new Rectangle(5, 10, 100, 200);
        assertThatThrownBy(() -> converter.convert(rectangle, Insets.class))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Unsupported conversion, source type [Rectangle");
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
    void testInsetsToBoolean_partialZero() {
        Insets insets = new Insets(0, 0, 15, 0);
        Boolean result = converter.convert(insets, Boolean.class);
        assertThat(result).isTrue(); // Any non-zero inset is true
    }

    // ========================================
    // Cross-Type Round-trip Tests
    // ========================================

    @Test
    void testInsetsNativeToStringRoundTrip() {
        Insets originalInsets = new Insets(5, 10, 15, 20);

        // Get the native toString() format
        String nativeString = originalInsets.toString();

        // Convert back to Insets using the native format
        Insets parsedInsets = converter.convert(nativeString, Insets.class);

        // Verify round-trip works perfectly
        assertThat(parsedInsets.getTop()).isEqualTo(originalInsets.getTop());
        assertThat(parsedInsets.getLeft()).isEqualTo(originalInsets.getLeft());
        assertThat(parsedInsets.getBottom()).isEqualTo(originalInsets.getBottom());
        assertThat(parsedInsets.getRight()).isEqualTo(originalInsets.getRight());
    }
}