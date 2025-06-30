package com.cedarsoftware.util.convert;

import java.awt.Color;
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
    void testIntegerToColor() {
        // RGB red (0xFF0000)
        Color result = converter.convert(0xFF0000, Color.class);
        assertThat(result.getRed()).isEqualTo(255);
        assertThat(result.getGreen()).isEqualTo(0);
        assertThat(result.getBlue()).isEqualTo(0);
        assertThat(result.getAlpha()).isEqualTo(255);
    }

    @Test
    void testLongToColor() {
        // ARGB with alpha (0x80FF0000)
        Color result = converter.convert(0x80FF0000L, Color.class);
        assertThat(result.getAlpha()).isEqualTo(128);
        assertThat(result.getRed()).isEqualTo(255);
        assertThat(result.getGreen()).isEqualTo(0);
        assertThat(result.getBlue()).isEqualTo(0);
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

    @Test
    void testColorToInteger() {
        Color color = new Color(255, 128, 64);
        Integer result = converter.convert(color, Integer.class);
        assertThat(result).isEqualTo(0xFF8040 | 0xFF000000); // includes alpha
    }

    @Test
    void testColorToLong() {
        Color color = new Color(255, 128, 64);
        Long result = converter.convert(color, Long.class);
        assertThat(result).isEqualTo((long)(0xFF8040 | 0xFF000000));
    }

    @Test
    void testColorToBigInteger() {
        Color color = new Color(255, 128, 64);
        BigInteger result = converter.convert(color, BigInteger.class);
        assertThat(result).isEqualTo(BigInteger.valueOf(0xFF8040 | 0xFF000000));
    }

    @Test
    void testColorToBigDecimal() {
        Color color = new Color(255, 128, 64);
        BigDecimal result = converter.convert(color, BigDecimal.class);
        assertThat(result).isEqualTo(BigDecimal.valueOf(0xFF8040 | 0xFF000000));
    }

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

    @Test
    void testRoundTrip_colorToIntegerToColor() {
        Color original = new Color(255, 128, 64);
        Integer packed = converter.convert(original, Integer.class);
        Color restored = converter.convert(packed, Color.class);

        assertThat(restored.getRed()).isEqualTo(original.getRed());
        assertThat(restored.getGreen()).isEqualTo(original.getGreen());
        assertThat(restored.getBlue()).isEqualTo(original.getBlue());
        // Note: Alpha may differ due to RGB vs ARGB interpretation
    }

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