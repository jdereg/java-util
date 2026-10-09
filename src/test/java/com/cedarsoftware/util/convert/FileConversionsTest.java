package com.cedarsoftware.util.convert;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Comprehensive tests for java.io.File conversions in the Converter.
 * Tests conversion from various types to File and from File to various types.
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
class FileConversionsTest {

    private Converter converter;
    
    private static String systemPath(String path) {
        return new File(path).getPath();
    }

    @BeforeEach
    void setUp() {
        converter = new Converter(new DefaultConverterOptions());
    }

    // ========================================
    // String to File Tests
    // ========================================

    @Test
    void testStringToFile_emptyString() {
        assertThat((Object) converter.convert("", File.class)).isNull();   // blank text: null, as for the dates
    }

    @Test
    void testStringToFile_whitespaceOnly() {
        assertThat((Object) converter.convert("   ", File.class)).isNull();   // blank text: null, as for the dates
    }

    // ========================================
    // Map to File Tests
    // ========================================

    @Test
    void testMapToFile_valueKey() {
        Map<String, Object> map = new HashMap<>();
        map.put("value", "/home/user/document.pdf");
        
        File result = converter.convert(map, File.class);
        assertThat(result.getPath()).isEqualTo(systemPath("/home/user/document.pdf"));
    }

    @Test
    void testMapToFile_vKey() {
        Map<String, Object> map = new HashMap<>();
        map.put("_v", "C:\\Program Files\\app.exe");
        
        File result = converter.convert(map, File.class);
        assertThat(result.getPath()).isEqualTo(systemPath("C:\\Program Files\\app.exe"));
    }

    // ========================================
    // char[] to File Tests
    // ========================================

    @Test
    void testCharArrayToFile_emptyArray() {
        char[] array = new char[0];
        
        assertThat((Object) converter.convert(array, File.class)).isNull();   // blank text: null, as for the dates
    }

    // ========================================
    // byte[] to File Tests
    // ========================================

    @Test
    void testByteArrayToFile_emptyArray() {
        byte[] array = new byte[0];
        
        assertThat((Object) converter.convert(array, File.class)).isNull();   // blank text: null, as for the dates
    }

    // ========================================
    // File Identity Tests
    // ========================================

    @Test
    void testFileToFile_identity() {
        File original = new File("/proc/version");
        File result = converter.convert(original, File.class);
        
        assertThat(result).isSameAs(original);
    }
}
