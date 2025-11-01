package com.cedarsoftware.util.convert;

import com.cedarsoftware.util.Color;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

import com.cedarsoftware.util.convert.DefaultConverterOptions;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Comprehensive tests for java.awt.Color conversions in the Converter.
 * Tests conversion from various types to Color and from Color to various types.
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
class ColorConversionsTest {

    private Converter converter;

    @BeforeEach
    void setUp() {
        converter = new Converter(new DefaultConverterOptions());
    }

    // ========================================
    // String to Color Tests
    // ========================================

    @Test
    void testStringToColor_hexWithHash() {
        Color result = converter.convert("#FF8040", Color.class);
        assertThat(result.getRed()).isEqualTo(255);
        assertThat(result.getGreen()).isEqualTo(128);
        assertThat(result.getBlue()).isEqualTo(64);
        assertThat(result.getAlpha()).isEqualTo(255);
    }

    @Test
    void testStringToColor_hexWithoutHash() {
        Color result = converter.convert("FF8040", Color.class);
        assertThat(result.getRed()).isEqualTo(255);
        assertThat(result.getGreen()).isEqualTo(128);
        assertThat(result.getBlue()).isEqualTo(64);
        assertThat(result.getAlpha()).isEqualTo(255);
    }

    @Test
    void testStringToColor_hexWithAlpha() {
        Color result = converter.convert("#80FF8040", Color.class);
        assertThat(result.getAlpha()).isEqualTo(128);
        assertThat(result.getRed()).isEqualTo(255);
        assertThat(result.getGreen()).isEqualTo(128);
        assertThat(result.getBlue()).isEqualTo(64);
    }

    @Test
    void testStringToColor_namedColors() {
        assertThat(converter.convert("red", Color.class)).isEqualTo(Color.RED);
        assertThat(converter.convert("GREEN", Color.class)).isEqualTo(Color.GREEN);
        assertThat(converter.convert("Blue", Color.class)).isEqualTo(Color.BLUE);
        assertThat(converter.convert("white", Color.class)).isEqualTo(Color.WHITE);
        assertThat(converter.convert("black", Color.class)).isEqualTo(Color.BLACK);
        assertThat(converter.convert("yellow", Color.class)).isEqualTo(Color.YELLOW);
        assertThat(converter.convert("cyan", Color.class)).isEqualTo(Color.CYAN);
        assertThat(converter.convert("magenta", Color.class)).isEqualTo(Color.MAGENTA);
        assertThat(converter.convert("orange", Color.class)).isEqualTo(Color.ORANGE);
        assertThat(converter.convert("pink", Color.class)).isEqualTo(Color.PINK);
        assertThat(converter.convert("gray", Color.class)).isEqualTo(Color.GRAY);
        assertThat(converter.convert("grey", Color.class)).isEqualTo(Color.GRAY);
        assertThat(converter.convert("dark_gray", Color.class)).isEqualTo(Color.DARK_GRAY);
        assertThat(converter.convert("light-gray", Color.class)).isEqualTo(Color.LIGHT_GRAY);
    }

    @Test
    void testStringToColor_rgbFormat() {
        Color result = converter.convert("rgb(255, 128, 64)", Color.class);
        assertThat(result.getRed()).isEqualTo(255);
        assertThat(result.getGreen()).isEqualTo(128);
        assertThat(result.getBlue()).isEqualTo(64);
        assertThat(result.getAlpha()).isEqualTo(255);
    }

    @Test
    void testStringToColor_rgbaFormat() {
        Color result = converter.convert("rgba(255, 128, 64, 192)", Color.class);
        assertThat(result.getRed()).isEqualTo(255);
        assertThat(result.getGreen()).isEqualTo(128);
        assertThat(result.getBlue()).isEqualTo(64);
        assertThat(result.getAlpha()).isEqualTo(192);
    }

    @Test
    void testStringToColor_invalidFormats() {
        assertThatThrownBy(() -> converter.convert("invalid", Color.class))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Unable to parse color from string");

        assertThatThrownBy(() -> converter.convert("", Color.class))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Cannot convert empty/null string to Color");

        assertThatThrownBy(() -> converter.convert("#GGGGGG", Color.class))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Unable to parse color from string");
    }

    // ========================================
    // Integer/Long to Color Tests
    // ========================================

    @Test
    void testIntegerToColorBlocked() {
        assertThatThrownBy(() -> converter.convert(0xFF0000, Color.class))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Unsupported conversion, source type [Integer");
    }

    @Test
    void testLongToColorBlocked() {
        assertThatThrownBy(() -> converter.convert(0x80FF0000L, Color.class))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Unsupported conversion, source type [Long");
    }

    // ========================================
    // Array to Color Tests
    // ========================================

    @Test
    void testIntArrayToColor_rgb() {
        int[] rgb = {255, 128, 64};
        Color result = converter.convert(rgb, Color.class);
        assertThat(result.getRed()).isEqualTo(255);
        assertThat(result.getGreen()).isEqualTo(128);
        assertThat(result.getBlue()).isEqualTo(64);
        assertThat(result.getAlpha()).isEqualTo(255);
    }

    @Test
    void testIntArrayToColor_rgba() {
        int[] rgba = {255, 128, 64, 192};
        Color result = converter.convert(rgba, Color.class);
        assertThat(result.getRed()).isEqualTo(255);
        assertThat(result.getGreen()).isEqualTo(128);
        assertThat(result.getBlue()).isEqualTo(64);
        assertThat(result.getAlpha()).isEqualTo(192);
    }

    @Test
    void testIntArrayToColor_invalidLength() {
        assertThatThrownBy(() -> converter.convert(new int[]{255, 128}, Color.class))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Color array must have 3 (RGB) or 4 (RGBA) elements");

        assertThatThrownBy(() -> converter.convert(new int[]{255, 128, 64, 192, 100}, Color.class))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Color array must have 3 (RGB) or 4 (RGBA) elements");
    }

    @Test
    void testIntArrayToColor_invalidValues() {
        assertThatThrownBy(() -> converter.convert(new int[]{300, 128, 64}, Color.class))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("RGB values must be between 0-255");

        assertThatThrownBy(() -> converter.convert(new int[]{255, 128, 64, 300}, Color.class))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Alpha value must be between 0-255");
    }

    // ========================================
    // Map to Color Tests
    // ========================================

    @Test
    void testMapToColor_rgbComponents() {
        Map<String, Object> map = new HashMap<>();
        map.put("red", 255);
        map.put("green", 128);
        map.put("blue", 64);

        Color result = converter.convert(map, Color.class);
        assertThat(result.getRed()).isEqualTo(255);
        assertThat(result.getGreen()).isEqualTo(128);
        assertThat(result.getBlue()).isEqualTo(64);
        assertThat(result.getAlpha()).isEqualTo(255);
    }

    @Test
    void testMapToColor_rgbaComponents() {
        Map<String, Object> map = new HashMap<>();
        map.put("red", 255);
        map.put("green", 128);
        map.put("blue", 64);
        map.put("alpha", 192);

        Color result = converter.convert(map, Color.class);
        assertThat(result.getRed()).isEqualTo(255);
        assertThat(result.getGreen()).isEqualTo(128);
        assertThat(result.getBlue()).isEqualTo(64);
        assertThat(result.getAlpha()).isEqualTo(192);
    }

    @Test
    void testMapToColor_packedRgb() {
        Map<String, Object> map = new HashMap<>();
        map.put("rgb", 0xFF8040);

        Color result = converter.convert(map, Color.class);
        assertThat(result.getRed()).isEqualTo(255);
        assertThat(result.getGreen()).isEqualTo(128);
        assertThat(result.getBlue()).isEqualTo(64);
        assertThat(result.getAlpha()).isEqualTo(255);
    }

    @Test
    void testMapToColor_hexValue() {
        Map<String, Object> map = new HashMap<>();
        map.put("color", "#FF8040");

        Color result = converter.convert(map, Color.class);
        assertThat(result.getRed()).isEqualTo(255);
        assertThat(result.getGreen()).isEqualTo(128);
        assertThat(result.getBlue()).isEqualTo(64);
        assertThat(result.getAlpha()).isEqualTo(255);
    }

    @Test
    void testMapToColor_fallbackValue() {
        Map<String, Object> map = new HashMap<>();
        map.put("value", "red");

        Color result = converter.convert(map, Color.class);
        assertThat(result).isEqualTo(Color.RED);
    }

    @Test
    void testMapToColor_shortKeys_rgb() {
        Map<String, Object> map = new HashMap<>();
        map.put("r", 255);
        map.put("g", 128);
        map.put("b", 64);

        Color result = converter.convert(map, Color.class);
        assertThat(result.getRed()).isEqualTo(255);
        assertThat(result.getGreen()).isEqualTo(128);
        assertThat(result.getBlue()).isEqualTo(64);
        assertThat(result.getAlpha()).isEqualTo(255);
    }

    @Test
    void testMapToColor_shortKeys_rgba() {
        Map<String, Object> map = new HashMap<>();
        map.put("r", 255);
        map.put("g", 128);
        map.put("b", 64);
        map.put("a", 192);

        Color result = converter.convert(map, Color.class);
        assertThat(result.getRed()).isEqualTo(255);
        assertThat(result.getGreen()).isEqualTo(128);
        assertThat(result.getBlue()).isEqualTo(64);
        assertThat(result.getAlpha()).isEqualTo(192);
    }

    @Test
    void testMapToColor_shortKeys_withTypeConversion() {
        Map<String, Object> map = new HashMap<>();
        map.put("r", "255");      // String that needs conversion
        map.put("g", 128.7);      // Double that needs conversion  
        map.put("b", new java.util.concurrent.atomic.AtomicInteger(64)); // AtomicInteger
        map.put("a", "192");      // String alpha

        Color result = converter.convert(map, Color.class);
        assertThat(result.getRed()).isEqualTo(255);
        assertThat(result.getGreen()).isEqualTo(128);
        assertThat(result.getBlue()).isEqualTo(64);
        assertThat(result.getAlpha()).isEqualTo(192);
    }

    // ========================================
    // Color to String Tests
    // ========================================

    @Test
    void testColorToString_rgb() {
        Color color = new Color(255, 128, 64);
        String result = converter.convert(color, String.class);
        assertThat(result).isEqualTo("#FF8040");
    }

    @Test
    void testColorToString_rgba() {
        Color color = new Color(255, 128, 64, 192);
        String result = converter.convert(color, String.class);
        assertThat(result).isEqualTo("#C0FF8040");
    }

    @Test
    void testColorToString_standardColors() {
        assertThat(converter.convert(Color.RED, String.class)).isEqualTo("#FF0000");
        assertThat(converter.convert(Color.GREEN, String.class)).isEqualTo("#00FF00");
        assertThat(converter.convert(Color.BLUE, String.class)).isEqualTo("#0000FF");
        assertThat(converter.convert(Color.WHITE, String.class)).isEqualTo("#FFFFFF");
        assertThat(converter.convert(Color.BLACK, String.class)).isEqualTo("#000000");
    }

    // ========================================
    // Color to Number Tests
    // ========================================


    // ========================================
    // Color to Array Tests
    // ========================================

    @Test
    void testColorToIntArray_rgb() {
        Color color = new Color(255, 128, 64);
        int[] result = converter.convert(color, int[].class);
        assertThat(result).isEqualTo(new int[]{255, 128, 64});
    }

    @Test
    void testColorToIntArray_rgba() {
        Color color = new Color(255, 128, 64, 192);
        int[] result = converter.convert(color, int[].class);
        assertThat(result).isEqualTo(new int[]{255, 128, 64, 192});
    }

    // ========================================
    // Color to Map Tests
    // ========================================

    @Test
    void testColorToMap() {
        Color color = new Color(255, 128, 64, 192);
        Map<String, Object> result = converter.convert(color, Map.class);

        assertThat(result).containsEntry("red", 255);
        assertThat(result).containsEntry("green", 128);
        assertThat(result).containsEntry("blue", 64);
        assertThat(result).containsEntry("alpha", 192);
        assertThat(result).containsEntry("rgb", color.getRGB());
    }

    // ========================================
    // Round-trip Tests
    // ========================================

    @Test
    void testRoundTrip_colorToMapToColor() {
        Color original = new Color(255, 128, 64, 192);
        Map<String, Object> map = converter.convert(original, Map.class);
        Color restored = converter.convert(map, Color.class);

        assertThat(restored).isEqualTo(original);
    }

    @Test
    void testRoundTrip_shortKeysMapToColor() {
        // Test that short keys also work for round-trip with manually created map
        Map<String, Object> shortKeyMap = new HashMap<>();
        shortKeyMap.put("r", 255);
        shortKeyMap.put("g", 128);
        shortKeyMap.put("b", 64);
        shortKeyMap.put("a", 192);
        
        Color color = converter.convert(shortKeyMap, Color.class);
        assertThat(color.getRed()).isEqualTo(255);
        assertThat(color.getGreen()).isEqualTo(128);
        assertThat(color.getBlue()).isEqualTo(64);
        assertThat(color.getAlpha()).isEqualTo(192);
    }

    @Test
    void testRoundTrip_colorToStringToColor() {
        Color original = new Color(255, 128, 64);
        String hex = converter.convert(original, String.class);
        Color restored = converter.convert(hex, Color.class);

        assertThat(restored.getRed()).isEqualTo(original.getRed());
        assertThat(restored.getGreen()).isEqualTo(original.getGreen());
        assertThat(restored.getBlue()).isEqualTo(original.getBlue());
    }

    @Test
    void testRoundTrip_colorToIntArrayToColor() {
        Color original = new Color(255, 128, 64, 192);
        int[] array = converter.convert(original, int[].class);
        Color restored = converter.convert(array, Color.class);

        assertThat(restored).isEqualTo(original);
    }

    // Round-trip test removed - Integer to Color conversion is blocked

    // ========================================
    // Identity and Null Tests
    // ========================================

    @Test
    void testColorToColor_identity() {
        Color original = new Color(255, 128, 64);
        Color result = converter.convert(original, Color.class);
        assertThat(result).isSameAs(original);
    }

    @Test
    void testNullToColor() {
        Color result = converter.convert(null, Color.class);
        assertThat(result).isNull();
    }
}