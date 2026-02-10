package com.cedarsoftware.util.convert;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
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

    @BeforeEach
    void setUp() {
        converter = new Converter(new DefaultConverterOptions());
    }

    // ========================================
    // Null/Void to Path Tests
    // ========================================

    @Test
    void testNullToPath() {
        Path result = converter.convert(null, Path.class);
        assertThat(result).isNull();
    }

    // ========================================
    // String to Path Tests
    // ========================================

    @Test
    void testStringToPath_absolutePath() {
        Path result = converter.convert("/path/to/file.txt", Path.class);
        assertThat(result.toString()).isEqualTo("/path/to/file.txt");
    }

    @Test
    void testStringToPath_relativePath() {
        Path result = converter.convert("relative/path/file.txt", Path.class);
        assertThat(result.toString()).isEqualTo("relative/path/file.txt");
    }

    @Test
    void testStringToPath_windowsPath() {
        Path result = converter.convert("C:\\Windows\\System32\\file.txt", Path.class);
        assertThat(result.toString()).isEqualTo("C:\\Windows\\System32\\file.txt");
    }

    @Test
    void testStringToPath_withSpaces() {
        Path result = converter.convert("/path with spaces/file name.txt", Path.class);
        assertThat(result.toString()).isEqualTo("/path with spaces/file name.txt");
    }

    @Test
    void testStringToPath_emptyString() {
        assertThatThrownBy(() -> converter.convert("", Path.class))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Cannot convert empty/null string to Path");
    }

    @Test
    void testStringToPath_whitespaceOnly() {
        assertThatThrownBy(() -> converter.convert("   ", Path.class))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Cannot convert empty/null string to Path");
    }

    // ========================================
    // Map to Path Tests
    // ========================================

    @Test
    void testMapToPath_pathKey() {
        Map<String, Object> map = new HashMap<>();
        map.put("path", "/usr/local/bin/java");
        
        Path result = converter.convert(map, Path.class);
        assertThat(result.toString()).isEqualTo("/usr/local/bin/java");
    }

    @Test
    void testMapToPath_valueKey() {
        Map<String, Object> map = new HashMap<>();
        map.put("value", "/home/user/document.pdf");
        
        Path result = converter.convert(map, Path.class);
        assertThat(result.toString()).isEqualTo("/home/user/document.pdf");
    }

    @Test
    void testMapToPath_vKey() {
        Map<String, Object> map = new HashMap<>();
        map.put("_v", "C:\\Program Files\\app.exe");
        
        Path result = converter.convert(map, Path.class);
        assertThat(result.toString()).isEqualTo("C:\\Program Files\\app.exe");
    }

    // ========================================
    // URI to Path Tests
    // ========================================

    @Test
    void testURIToPath() throws Exception {
        URI uri = new URI("file:///path/to/file.txt");
        
        Path result = converter.convert(uri, Path.class);
        assertThat(result.toString()).isEqualTo("/path/to/file.txt");
    }

    @Test
    void testURIToPath_windowsPath() throws Exception {
        URI uri = new URI("file:///C:/Windows/System32/file.txt");
        
        Path result = converter.convert(uri, Path.class);
        // URI conversion may normalize the path
        assertThat(result.toString()).contains("file.txt");
    }

    // ========================================
    // URL to Path Tests
    // ========================================

    @Test
    void testURLToPath() throws Exception {
        URL url = new URL("file:///tmp/test.txt");
        
        Path result = converter.convert(url, Path.class);
        assertThat(result.toString()).isEqualTo("/tmp/test.txt");
    }

    // ========================================
    // File to Path Tests
    // ========================================

    @Test
    void testFileToPath() {
        File file = new File("/var/log/application.log");
        
        Path result = converter.convert(file, Path.class);
        assertThat(result.toString()).isEqualTo("/var/log/application.log");
    }

    @Test
    void testFileToPath_relativePath() {
        File file = new File("config/settings.properties");
        
        Path result = converter.convert(file, Path.class);
        assertThat(result.toString()).isEqualTo("config/settings.properties");
    }

    // ========================================
    // char[] to Path Tests
    // ========================================

    @Test
    void testCharArrayToPath() {
        char[] array = "/etc/passwd".toCharArray();
        
        Path result = converter.convert(array, Path.class);
        assertThat(result.toString()).isEqualTo("/etc/passwd");
    }

    @Test
    void testCharArrayToPath_emptyArray() {
        char[] array = new char[0];
        
        assertThatThrownBy(() -> converter.convert(array, Path.class))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Cannot convert empty/null string to Path");
    }

    // ========================================
    // byte[] to Path Tests
    // ========================================

    @Test
    void testByteArrayToPath() {
        byte[] array = "/opt/app/config.xml".getBytes(StandardCharsets.UTF_8);
        
        Path result = converter.convert(array, Path.class);
        assertThat(result.toString()).isEqualTo("/opt/app/config.xml");
    }

    @Test
    void testByteArrayToPath_emptyArray() {
        byte[] array = new byte[0];
        
        assertThatThrownBy(() -> converter.convert(array, Path.class))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Cannot convert empty/null string to Path");
    }

    // ========================================
    // Path to String Tests
    // ========================================

    @Test
    void testPathToString() {
        Path path = Paths.get("/home/user/documents/report.docx");
        String result = converter.convert(path, String.class);
        assertThat(result).isEqualTo("/home/user/documents/report.docx");
    }

    @Test
    void testPathToString_windowsPath() {
        Path path = Paths.get("C:\\Users\\Administrator\\Desktop\\file.txt");
        String result = converter.convert(path, String.class);
        assertThat(result).isEqualTo("C:\\Users\\Administrator\\Desktop\\file.txt");
    }

    // ========================================
    // Path to Map Tests
    // ========================================

    @Test
    void testPathToMap() {
        Path path = Paths.get("/usr/bin/gcc");
        Map<String, Object> result = converter.convert(path, Map.class);
        
        assertThat(result).containsEntry("path", "/usr/bin/gcc");
        assertThat(result).hasSize(1);
    }

    // ========================================
    // Path to URI Tests
    // ========================================

    @Test
    void testPathToURI() {
        Path path = Paths.get("/tmp/data.json");
        URI result = converter.convert(path, URI.class);
        
        assertThat(result.getScheme()).isEqualTo("file");
        assertThat(result.getPath()).isEqualTo("/tmp/data.json");
    }

    // ========================================
    // Path to URL Tests
    // ========================================

    @Test
    void testPathToURL() {
        Path path = Paths.get("/var/www/index.html");
        URL result = converter.convert(path, URL.class);
        
        assertThat(result.getProtocol()).isEqualTo("file");
        assertThat(result.getPath()).isEqualTo("/var/www/index.html");
    }

    // ========================================
    // Path to File Tests
    // ========================================

    @Test
    void testPathToFile() {
        Path path = Paths.get("/etc/hosts");
        File result = converter.convert(path, File.class);
        
        assertThat(result.getPath()).isEqualTo("/etc/hosts");
    }

    // ========================================
    // Path to char[] Tests
    // ========================================

    @Test
    void testPathToCharArray() {
        Path path = Paths.get("/lib64/libc.so.6");
        char[] result = converter.convert(path, char[].class);
        
        assertThat(new String(result)).isEqualTo("/lib64/libc.so.6");
    }

    // ========================================
    // Path to byte[] Tests
    // ========================================

    @Test
    void testPathToByteArray() {
        Path path = Paths.get("/boot/grub/grub.cfg");
        byte[] result = converter.convert(path, byte[].class);
        
        String resultString = new String(result, StandardCharsets.UTF_8);
        assertThat(resultString).isEqualTo("/boot/grub/grub.cfg");
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
    // Round-trip Tests
    // ========================================

    @Test
    void testPathStringRoundTrip() {
        Path originalPath = Paths.get("/system/bin/sh");
        
        // Path -> String -> Path
        String string = converter.convert(originalPath, String.class);
        Path backToPath = converter.convert(string, Path.class);
        
        assertThat(backToPath.toString()).isEqualTo(originalPath.toString());
    }

    @Test
    void testPathMapRoundTrip() {
        Path originalPath = Paths.get("/Applications/Safari.app");
        
        // Path -> Map -> Path
        Map<String, Object> map = converter.convert(originalPath, Map.class);
        Path backToPath = converter.convert(map, Path.class);
        
        assertThat(backToPath.toString()).isEqualTo(originalPath.toString());
    }

    @Test
    void testPathURIRoundTrip() {
        Path originalPath = Paths.get("/Library/Preferences/SystemConfiguration");
        
        // Path -> URI -> Path
        URI uri = converter.convert(originalPath, URI.class);
        Path backToPath = converter.convert(uri, Path.class);
        
        assertThat(backToPath.toString()).isEqualTo(originalPath.toString());
    }

    @Test
    void testPathFileRoundTrip() {
        Path originalPath = Paths.get("/usr/share/man/man1/ls.1");
        
        // Path -> File -> Path
        File file = converter.convert(originalPath, File.class);
        Path backToPath = converter.convert(file, Path.class);
        
        assertThat(backToPath.toString()).isEqualTo(originalPath.toString());
    }

    @Test
    void testPathCharArrayRoundTrip() {
        Path originalPath = Paths.get("/dev/null");
        
        // Path -> char[] -> Path
        char[] charArray = converter.convert(originalPath, char[].class);
        Path backToPath = converter.convert(charArray, Path.class);
        
        assertThat(backToPath.toString()).isEqualTo(originalPath.toString());
    }

    @Test
    void testPathByteArrayRoundTrip() {
        Path originalPath = Paths.get("/bin/bash");
        
        // Path -> byte[] -> Path
        byte[] byteArray = converter.convert(originalPath, byte[].class);
        Path backToPath = converter.convert(byteArray, Path.class);
        
        assertThat(backToPath.toString()).isEqualTo(originalPath.toString());
    }

    // ========================================
    // Cross-Platform Path Tests
    // ========================================

    @Test
    void testPathConversion_unixPath() {
        String unixPath = "/home/user/.bashrc";
        Path result = converter.convert(unixPath, Path.class);
        assertThat(result.toString()).isEqualTo(unixPath);
    }

    @Test
    void testPathConversion_windowsPath() {
        String windowsPath = "C:\\Windows\\System32\\drivers\\etc\\hosts";
        Path result = converter.convert(windowsPath, Path.class);
        assertThat(result.toString()).isEqualTo(windowsPath);
    }

    // ========================================
    // Special Characters Tests
    // ========================================

    @Test
    void testPathConversion_specialCharacters() {
        String pathWithSpecialChars = "/tmp/file-with_special.chars@domain.txt";
        Path result = converter.convert(pathWithSpecialChars, Path.class);
        assertThat(result.toString()).isEqualTo(pathWithSpecialChars);
    }

    @Test
    void testPathConversion_unicodeCharacters() {
        String pathWithUnicode = "/home/user/文档/测试文件.txt";
        Path result = converter.convert(pathWithUnicode, Path.class);
        assertThat(result.toString()).isEqualTo(pathWithUnicode);
    }

    // ========================================
    // Path Normalization Tests
    // ========================================

    @Test
    void testPathConversion_normalizedPath() {
        String pathWithDots = "/home/user/../user/./documents/file.txt";
        Path result = converter.convert(pathWithDots, Path.class);
        // Path will preserve the original string representation
        assertThat(result.toString()).isEqualTo(pathWithDots);
    }

    @Test
    void testPathConversion_multipleSeparators() {
        String pathWithMultipleSeps = "/home//user///documents////file.txt";
        Path result = converter.convert(pathWithMultipleSeps, Path.class);
        // Paths.get() normalizes multiple separators
        assertThat(result.toString()).isEqualTo("/home/user/documents/file.txt");
    }

    // ========================================
    // File System Specific Tests
    // ========================================

    @Test
    void testPathConversion_rootPath() {
        String rootPath = "/";
        Path result = converter.convert(rootPath, Path.class);
        assertThat(result.toString()).isEqualTo(rootPath);
    }

    @Test
    void testPathConversion_currentDirectory() {
        String currentDir = ".";
        Path result = converter.convert(currentDir, Path.class);
        assertThat(result.toString()).isEqualTo(currentDir);
    }

    @Test
    void testPathConversion_parentDirectory() {
        String parentDir = "..";
        Path result = converter.convert(parentDir, Path.class);
        assertThat(result.toString()).isEqualTo(parentDir);
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