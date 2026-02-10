package com.cedarsoftware.util.convert;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

import com.cedarsoftware.util.LoggingConfig;
import com.cedarsoftware.util.geom.Dimension;
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

    private static final Logger LOG = Logger.getLogger(InsetsConversionsTest.class.getName());
    static {
        LoggingConfig.init();
    }

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
        assertThat(result.getTop()).isEqualTo(5);
        assertThat(result.getLeft()).isEqualTo(10);
        assertThat(result.getBottom()).isEqualTo(15);
        assertThat(result.getRight()).isEqualTo(20);
    }

    @Test
    void testStringToInsets_commaSeparated() {
        Insets result = converter.convert("8,12,16,24", Insets.class);
        assertThat(result.getTop()).isEqualTo(8);
        assertThat(result.getLeft()).isEqualTo(12);
        assertThat(result.getBottom()).isEqualTo(16);
        assertThat(result.getRight()).isEqualTo(24);
    }

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
    void testStringToInsets_allZero() {
        Insets result = converter.convert("(0,0,0,0)", Insets.class);
        assertThat(result.getTop()).isEqualTo(0);
        assertThat(result.getLeft()).isEqualTo(0);
        assertThat(result.getBottom()).isEqualTo(0);
        assertThat(result.getRight()).isEqualTo(0);
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
    void testMapToInsets_topLeftBottomRight() {
        Map<String, Object> map = new HashMap<>();
        map.put("top", 5);
        map.put("left", 10);
        map.put("bottom", 15);
        map.put("right", 20);
        
        Insets result = converter.convert(map, Insets.class);
        assertThat(result.getTop()).isEqualTo(5);
        assertThat(result.getLeft()).isEqualTo(10);
        assertThat(result.getBottom()).isEqualTo(15);
        assertThat(result.getRight()).isEqualTo(20);
    }

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
    void testIntArrayToInsets() {
        int[] array = {5, 10, 15, 20};
        
        Insets result = converter.convert(array, Insets.class);
        assertThat(result.getTop()).isEqualTo(5);
        assertThat(result.getLeft()).isEqualTo(10);
        assertThat(result.getBottom()).isEqualTo(15);
        assertThat(result.getRight()).isEqualTo(20);
    }

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
    void testDimensionToInsets() {
        Dimension dimension = new Dimension(100, 200);
        Insets result = converter.convert(dimension, Insets.class);
        int minValue = Math.min(100, 200); // min(width, height) = 100
        assertThat(result.getTop()).isEqualTo(minValue); // all sides = min value
        assertThat(result.getLeft()).isEqualTo(minValue);
        assertThat(result.getBottom()).isEqualTo(minValue);
        assertThat(result.getRight()).isEqualTo(minValue);
    }

    @Test
    void testRectangleToInsetsBlocked() {
        Rectangle rectangle = new Rectangle(5, 10, 100, 200);
        assertThatThrownBy(() -> converter.convert(rectangle, Insets.class))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Unsupported conversion, source type [Rectangle");
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

    @Test
    void testInsetsToStringFormat_actualJavaFormat() {
        // Test to see what the actual toString() format of java.awt.Insets looks like
        Insets insets = new Insets(5, 10, 15, 20);
        String actualToString = insets.toString();
        
        // Log the actual format for documentation
        LOG.info("Actual Insets.toString() format: " + actualToString);
        
        // Test if the current converter can parse this format back to Insets
        try {
            Insets parsedBack = converter.convert(actualToString, Insets.class);
            assertThat(parsedBack.getTop()).isEqualTo(5);
            assertThat(parsedBack.getLeft()).isEqualTo(10);
            assertThat(parsedBack.getBottom()).isEqualTo(15);
            assertThat(parsedBack.getRight()).isEqualTo(20);
            LOG.info("SUCCESS: Converter can parse the native toString() format!");
        } catch (Exception e) {
            LOG.warning("INFO: Converter cannot parse the native toString() format: " + e.getMessage());
            // This is expected if the format is not supported yet
        }
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




    // ========================================
    // Round-trip Boolean Tests
    // ========================================

    @Test
    void testBooleanInsetsRoundTrip_trueBlocked() {
        Boolean originalBoolean = Boolean.TRUE;
        
        // Boolean -> Insets should be blocked
        assertThatThrownBy(() -> converter.convert(originalBoolean, Insets.class))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Unsupported conversion, source type [Boolean");
    }

    @Test
    void testBooleanInsetsRoundTrip_falseBlocked() {
        Boolean originalBoolean = Boolean.FALSE;
        
        // Boolean -> Insets should be blocked
        assertThatThrownBy(() -> converter.convert(originalBoolean, Insets.class))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Unsupported conversion, source type [Boolean");
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

    // ========================================
    // Bug: toInteger/toDimension/toRectangle int overflow
    // (not registered in Converter, test directly)
    // ========================================

    @Test
    void testToInteger_overflowShouldThrow() {
        // All four values at MAX_VALUE/2+1 will overflow when summed
        Insets insets = new Insets(Integer.MAX_VALUE, 1, 0, 0);
        assertThatThrownBy(() -> InsetsConversions.toInteger(insets, converter))
                .isInstanceOf(ArithmeticException.class);
    }

    @Test
    void testToInteger_safeValues_shouldWork() {
        Insets insets = new Insets(5, 10, 15, 20);
        Integer result = InsetsConversions.toInteger(insets, converter);
        assertThat(result).isEqualTo(50);
    }

    @Test
    void testToDimension_horizontalOverflow_shouldThrow() {
        // left + right overflows
        Insets insets = new Insets(0, Integer.MAX_VALUE, 0, 1);
        assertThatThrownBy(() -> InsetsConversions.toDimension(insets, converter))
                .isInstanceOf(ArithmeticException.class);
    }

    @Test
    void testToDimension_verticalOverflow_shouldThrow() {
        // top + bottom overflows
        Insets insets = new Insets(Integer.MAX_VALUE, 0, 1, 0);
        assertThatThrownBy(() -> InsetsConversions.toDimension(insets, converter))
                .isInstanceOf(ArithmeticException.class);
    }

    @Test
    void testToDimension_safeValues_shouldWork() {
        Insets insets = new Insets(5, 10, 15, 20);
        Dimension result = InsetsConversions.toDimension(insets, converter);
        assertThat(result.getWidth()).isEqualTo(30);   // 10 + 20
        assertThat(result.getHeight()).isEqualTo(20);   // 5 + 15
    }

    @Test
    void testToRectangle_subtractionOverflow_shouldThrow() {
        // right - left overflows: MIN_VALUE - MAX_VALUE
        Insets insets = new Insets(0, Integer.MAX_VALUE, 0, Integer.MIN_VALUE);
        assertThatThrownBy(() -> InsetsConversions.toRectangle(insets, converter))
                .isInstanceOf(ArithmeticException.class);
    }

    @Test
    void testToRectangle_safeValues_shouldWork() {
        Insets insets = new Insets(10, 20, 50, 100);
        Rectangle result = InsetsConversions.toRectangle(insets, converter);
        assertThat(result.getX()).isEqualTo(20);
        assertThat(result.getY()).isEqualTo(10);
        assertThat(result.getWidth()).isEqualTo(80);   // 100 - 20
        assertThat(result.getHeight()).isEqualTo(40);   // 50 - 10
    }
}