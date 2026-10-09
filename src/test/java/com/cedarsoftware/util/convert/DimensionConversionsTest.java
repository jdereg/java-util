package com.cedarsoftware.util.convert;

import java.util.HashMap;
import java.util.Map;

import com.cedarsoftware.util.geom.Dimension;
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
    // String to Dimension Tests
    // ========================================

    @Test
    void testStringToDimension_commaSeparated() {
        Dimension result = converter.convert("1920,1080", Dimension.class);
        assertThat(result.getWidth()).isEqualTo(1920);
        assertThat(result.getHeight()).isEqualTo(1080);
    }

    @Test
    void testStringToDimension_spaceSeparated() {
        Dimension result = converter.convert("640 480", Dimension.class);
        assertThat(result.getWidth()).isEqualTo(640);
        assertThat(result.getHeight()).isEqualTo(480);
    }

    @Test
    void testStringToDimension_withWhitespace() {
        Dimension result = converter.convert("  1024 x 768  ", Dimension.class);
        assertThat(result.getWidth()).isEqualTo(1024);
        assertThat(result.getHeight()).isEqualTo(768);
    }

    @Test
    void testStringToDimension_invalidFormat() {
        assertThatThrownBy(() -> converter.convert("invalid", Dimension.class))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Unable to parse dimension from string");
    }

    @Test
    void testStringToDimension_emptyString() {
        assertThat((Object) converter.convert("", Dimension.class)).isNull();   // blank text: null, as for the dates
    }

    // ========================================
    // Map to Dimension Tests
    // ========================================

    @Test
    void testMapToDimension_shortKeys() {
        Map<String, Object> map = new HashMap<>();
        map.put("w", 1920);
        map.put("h", 1080);
        
        Dimension result = converter.convert(map, Dimension.class);
        assertThat(result.getWidth()).isEqualTo(1920);
        assertThat(result.getHeight()).isEqualTo(1080);
    }

    @Test
    void testMapToDimension_stringValue() {
        Map<String, Object> map = new HashMap<>();
        map.put("value", "640x480");
        
        Dimension result = converter.convert(map, Dimension.class);
        assertThat(result.getWidth()).isEqualTo(640);
        assertThat(result.getHeight()).isEqualTo(480);
    }

    // ========================================
    // Array to Dimension Tests
    // ========================================

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
    // Dimension to Boolean Tests
    // ========================================

    @Test
    void testDimensionToBoolean_partialZero() {
        Dimension dimension = new Dimension(0, 100);
        Boolean result = converter.convert(dimension, Boolean.class);
        assertThat(result).isTrue(); // Any non-zero coordinate is true
    }
}