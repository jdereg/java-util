package com.cedarsoftware.util.convert;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Comprehensive tests for java.nio.file.Path conversions in the Converter.
 * Tests conversion from various types to Path and from Path to various types.
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
class PathConversionsTest {

    private Converter converter;
    
    private static String systemPathString(String path) {
        return Paths.get(path).toString();
    }

    @BeforeEach
    void setUp() {
        converter = new Converter(new DefaultConverterOptions());
    }

    // ========================================
    // String to Path Tests
    // ========================================

    @Test
    void testStringToPath_emptyString() {
        assertThat((Object) converter.convert("", Path.class)).isNull();   // blank text: null, as for the dates
    }

    @Test
    void testStringToPath_whitespaceOnly() {
        assertThat((Object) converter.convert("   ", Path.class)).isNull();   // blank text: null, as for the dates
    }

    // ========================================
    // Map to Path Tests
    // ========================================

    @Test
    void testMapToPath_valueKey() {
        Map<String, Object> map = new HashMap<>();
        map.put("value", "/home/user/document.pdf");
        
        Path result = converter.convert(map, Path.class);
        assertThat(result.toString()).isEqualTo(systemPathString("/home/user/document.pdf"));
    }

    @Test
    void testMapToPath_vKey() {
        Map<String, Object> map = new HashMap<>();
        map.put("_v", "C:\\Program Files\\app.exe");
        
        Path result = converter.convert(map, Path.class);
        assertThat(result.toString()).isEqualTo(systemPathString("C:\\Program Files\\app.exe"));
    }

    // ========================================
    // char[] to Path Tests
    // ========================================

    @Test
    void testCharArrayToPath_emptyArray() {
        char[] array = new char[0];
        
        assertThat((Object) converter.convert(array, Path.class)).isNull();   // blank text: null, as for the dates
    }

    // ========================================
    // byte[] to Path Tests
    // ========================================

    @Test
    void testByteArrayToPath_emptyArray() {
        byte[] array = new byte[0];
        
        assertThat((Object) converter.convert(array, Path.class)).isNull();   // blank text: null, as for the dates
    }

    // ========================================
    // Path Identity Tests
    // ========================================

    @Test
    void testPathToPath_identity() {
        Path original = Paths.get("/proc/version");
        Path result = converter.convert(original, Path.class);
        
        assertThat(result).isSameAs(original);
    }

    // ========================================
    // Path Normalization Tests
    // ========================================

    @Test
    void testPathConversion_normalizedPath() {
        String pathWithDots = "/home/user/../user/./documents/file.txt";
        Path result = converter.convert(pathWithDots, Path.class);
        // Path will preserve/normalize according to the active filesystem.
        assertThat(result.toString()).isEqualTo(Paths.get(pathWithDots).toString());
    }

    // ========================================
    // File System Specific Tests
    // ========================================

    @Test
    void testPathConversion_currentDirectory() {
        String currentDir = ".";
        Path result = converter.convert(currentDir, Path.class);
        assertThat(result.toString()).isEqualTo(currentDir);
    }

    // ========================================
    // Bug: toFile should catch UnsupportedOperationException
    // for non-default filesystem paths
    // ========================================

    @Test
    void testPathToFile_nonDefaultFileSystem_shouldThrowDescriptiveException() throws IOException {
        // Create a temporary zip file to get a non-default filesystem Path
        Path tempZip = Files.createTempFile("test", ".zip");
        try {
            // Create a minimal valid zip file
            try (java.util.zip.ZipOutputStream zos = new java.util.zip.ZipOutputStream(Files.newOutputStream(tempZip))) {
                zos.putNextEntry(new java.util.zip.ZipEntry("entry.txt"));
                zos.write("hello".getBytes(StandardCharsets.UTF_8));
                zos.closeEntry();
            }

            // Open as a zip filesystem
            URI zipUri = URI.create("jar:" + tempZip.toUri());
            try (FileSystem zipFs = FileSystems.newFileSystem(zipUri, new HashMap<>())) {
                Path zipEntry = zipFs.getPath("/entry.txt");

                // This should throw IllegalArgumentException with a descriptive message,
                // NOT an UnsupportedOperationException
                assertThatThrownBy(() -> converter.convert(zipEntry, File.class))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessageContaining("Path")
                        .hasMessageContaining("File")
                        .hasCauseInstanceOf(UnsupportedOperationException.class);
            }
        } finally {
            Files.deleteIfExists(tempZip);
        }
    }
}
