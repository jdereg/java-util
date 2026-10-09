package com.cedarsoftware.util.convert;

import java.util.HashMap;
import java.util.Map;

import com.cedarsoftware.util.geom.Rectangle;
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
    // String to Rectangle Tests
    // ========================================

    @Test
    void testStringToRectangle_spaceSeparated() {
        Rectangle result = converter.convert("0 0 300 150", Rectangle.class);
        assertThat(result.getX()).isEqualTo(0);
        assertThat(result.getY()).isEqualTo(0);
        assertThat(result.getWidth()).isEqualTo(300);
        assertThat(result.getHeight()).isEqualTo(150);
    }

    @Test
    void testStringToRectangle_withWhitespace() {
        Rectangle result = converter.convert("  ( 25 , 30 , 400 , 200 )  ", Rectangle.class);
        assertThat(result.getX()).isEqualTo(25);
        assertThat(result.getY()).isEqualTo(30);
        assertThat(result.getWidth()).isEqualTo(400);
        assertThat(result.getHeight()).isEqualTo(200);
    }

    @Test
    void testStringToRectangle_negativeCoordinates() {
        Rectangle result = converter.convert("(-10,-20,100,50)", Rectangle.class);
        assertThat(result.getX()).isEqualTo(-10);
        assertThat(result.getY()).isEqualTo(-20);
        assertThat(result.getWidth()).isEqualTo(100);
        assertThat(result.getHeight()).isEqualTo(50);
    }

    @Test
    void testStringToRectangle_invalidFormat() {
        assertThatThrownBy(() -> converter.convert("invalid", Rectangle.class))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Unable to parse rectangle from string");
    }

    @Test
    void testStringToRectangle_emptyString() {
        assertThat((Object) converter.convert("", Rectangle.class)).isNull();   // blank text: null, as for the dates
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
    void testMapToRectangle_stringValue() {
        Map<String, Object> map = new HashMap<>();
        map.put("value", "(0,0,300,150)");
        
        Rectangle result = converter.convert(map, Rectangle.class);
        assertThat(result.getX()).isEqualTo(0);
        assertThat(result.getY()).isEqualTo(0);
        assertThat(result.getWidth()).isEqualTo(300);
        assertThat(result.getHeight()).isEqualTo(150);
    }

    // ========================================
    // Array to Rectangle Tests
    // ========================================

    @Test
    void testIntArrayToRectangle_negativeValues() {
        int[] array = {-10, -20, 100, 50};
        
        Rectangle result = converter.convert(array, Rectangle.class);
        assertThat(result.getX()).isEqualTo(-10);
        assertThat(result.getY()).isEqualTo(-20);
        assertThat(result.getWidth()).isEqualTo(100);
        assertThat(result.getHeight()).isEqualTo(50);
    }

    @Test
    void testIntArrayToRectangle_invalidLength() {
        int[] array = {10, 20, 100};
        
        assertThatThrownBy(() -> converter.convert(array, Rectangle.class))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Rectangle array must have exactly 4 elements");
    }

    // ========================================
    // Rectangle to Boolean Tests
    // ========================================

    @Test
    void testRectangleToBoolean_partialZero() {
        Rectangle rectangle = new Rectangle(0, 0, 100, 0);
        Boolean result = converter.convert(rectangle, Boolean.class);
        assertThat(result).isTrue(); // Any non-zero coordinate is true
    }
}