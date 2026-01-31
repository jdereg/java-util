package com.cedarsoftware.util.convert;

import java.io.File;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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

    @BeforeEach
    void setUp() {
        converter = new Converter(new DefaultConverterOptions());
    }

    // ========================================
    // Null/Void to File Tests
    // ========================================

    @Test
    void testNullToFile() {
        File result = converter.convert(null, File.class);
        assertThat(result).isNull();
    }

    // ========================================
    // String to File Tests
    // ========================================

    @Test
    void testStringToFile_absolutePath() {
        File result = converter.convert("/path/to/file.txt", File.class);
        assertThat(result.getPath()).isEqualTo("/path/to/file.txt");
    }

    @Test
    void testStringToFile_relativePath() {
        File result = converter.convert("relative/path/file.txt", File.class);
        assertThat(result.getPath()).isEqualTo("relative/path/file.txt");
    }

    @Test
    void testStringToFile_windowsPath() {
        File result = converter.convert("C:\\Windows\\System32\\file.txt", File.class);
        assertThat(result.getPath()).isEqualTo("C:\\Windows\\System32\\file.txt");
    }

    @Test
    void testStringToFile_withSpaces() {
        File result = converter.convert("/path with spaces/file name.txt", File.class);
        assertThat(result.getPath()).isEqualTo("/path with spaces/file name.txt");
    }

    @Test
    void testStringToFile_emptyString() {
        assertThatThrownBy(() -> converter.convert("", File.class))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Cannot convert empty/null string to File");
    }

    @Test
    void testStringToFile_whitespaceOnly() {
        assertThatThrownBy(() -> converter.convert("   ", File.class))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Cannot convert empty/null string to File");
    }

    // ========================================
    // Map to File Tests
    // ========================================

    @Test
    void testMapToFile_fileKey() {
        Map<String, Object> map = new HashMap<>();
        map.put("file", "/usr/local/bin/java");
        
        File result = converter.convert(map, File.class);
        assertThat(result.getPath()).isEqualTo("/usr/local/bin/java");
    }

    @Test
    void testMapToFile_valueKey() {
        Map<String, Object> map = new HashMap<>();
        map.put("value", "/home/user/document.pdf");
        
        File result = converter.convert(map, File.class);
        assertThat(result.getPath()).isEqualTo("/home/user/document.pdf");
    }

    @Test
    void testMapToFile_vKey() {
        Map<String, Object> map = new HashMap<>();
        map.put("_v", "C:\\Program Files\\app.exe");
        
        File result = converter.convert(map, File.class);
        assertThat(result.getPath()).isEqualTo("C:\\Program Files\\app.exe");
    }

    // ========================================
    // URI to File Tests
    // ========================================

    @Test
    void testURIToFile() throws Exception {
        URI uri = new URI("file:///path/to/file.txt");
        
        File result = converter.convert(uri, File.class);
        assertThat(result.getPath()).isEqualTo("/path/to/file.txt");
    }

    @Test
    void testURIToFile_windowsPath() throws Exception {
        URI uri = new URI("file:///C:/Windows/System32/file.txt");
        
        File result = converter.convert(uri, File.class);
        // URI conversion may normalize the path
        assertThat(result.getPath()).contains("file.txt");
    }

    // ========================================
    // URL to File Tests
    // ========================================

    @Test
    void testURLToFile() throws Exception {
        URL url = new URL("file:///tmp/test.txt");
        
        File result = converter.convert(url, File.class);
        assertThat(result.getPath()).isEqualTo("/tmp/test.txt");
    }

    // ========================================
    // Path to File Tests
    // ========================================

    @Test
    void testPathToFile() {
        Path path = Paths.get("/var/log/application.log");
        
        File result = converter.convert(path, File.class);
        assertThat(result.getPath()).isEqualTo("/var/log/application.log");
    }

    @Test
    void testPathToFile_relativePath() {
        Path path = Paths.get("config/settings.properties");
        
        File result = converter.convert(path, File.class);
        assertThat(result.getPath()).isEqualTo("config/settings.properties");
    }

    // ========================================
    // char[] to File Tests
    // ========================================

    @Test
    void testCharArrayToFile() {
        char[] array = "/etc/passwd".toCharArray();
        
        File result = converter.convert(array, File.class);
        assertThat(result.getPath()).isEqualTo("/etc/passwd");
    }

    @Test
    void testCharArrayToFile_emptyArray() {
        char[] array = new char[0];
        
        assertThatThrownBy(() -> converter.convert(array, File.class))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Cannot convert empty/null string to File");
    }

    // ========================================
    // byte[] to File Tests
    // ========================================

    @Test
    void testByteArrayToFile() {
        byte[] array = "/opt/app/config.xml".getBytes(StandardCharsets.UTF_8);
        
        File result = converter.convert(array, File.class);
        assertThat(result.getPath()).isEqualTo("/opt/app/config.xml");
    }

    @Test
    void testByteArrayToFile_emptyArray() {
        byte[] array = new byte[0];
        
        assertThatThrownBy(() -> converter.convert(array, File.class))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Cannot convert empty/null string to File");
    }

    // ========================================
    // File to String Tests
    // ========================================

    @Test
    void testFileToString() {
        File file = new File("/home/user/documents/report.docx");
        String result = converter.convert(file, String.class);
        assertThat(result).isEqualTo("/home/user/documents/report.docx");
    }

    @Test
    void testFileToString_windowsPath() {
        File file = new File("C:\\Users\\Administrator\\Desktop\\file.txt");
        String result = converter.convert(file, String.class);
        assertThat(result).isEqualTo("C:\\Users\\Administrator\\Desktop\\file.txt");
    }

    // ========================================
    // File to Map Tests
    // ========================================

    @Test
    void testFileToMap() {
        File file = new File("/usr/bin/gcc");
        Map<String, Object> result = converter.convert(file, Map.class);
        
        assertThat(result).containsEntry("file", "/usr/bin/gcc");
        assertThat(result).hasSize(1);
    }

    // ========================================
    // File to URI Tests
    // ========================================

    @Test
    void testFileToURI() {
        File file = new File("/tmp/data.json");
        URI result = converter.convert(file, URI.class);
        
        assertThat(result.getScheme()).isEqualTo("file");
        assertThat(result.getPath()).isEqualTo("/tmp/data.json");
    }

    // ========================================
    // File to URL Tests
    // ========================================

    @Test
    void testFileToURL() {
        File file = new File("/var/www/index.html");
        URL result = converter.convert(file, URL.class);
        
        assertThat(result.getProtocol()).isEqualTo("file");
        assertThat(result.getPath()).isEqualTo("/var/www/index.html");
    }

    // ========================================
    // File to Path Tests
    // ========================================

    @Test
    void testFileToPath() {
        File file = new File("/etc/hosts");
        Path result = converter.convert(file, Path.class);
        
        assertThat(result.toString()).isEqualTo("/etc/hosts");
    }

    // ========================================
    // File to char[] Tests
    // ========================================

    @Test
    void testFileToCharArray() {
        File file = new File("/lib64/libc.so.6");
        char[] result = converter.convert(file, char[].class);
        
        assertThat(new String(result)).isEqualTo("/lib64/libc.so.6");
    }

    // ========================================
    // File to byte[] Tests
    // ========================================

    @Test
    void testFileToByteArray() {
        File file = new File("/boot/grub/grub.cfg");
        byte[] result = converter.convert(file, byte[].class);
        
        String resultString = new String(result, StandardCharsets.UTF_8);
        assertThat(resultString).isEqualTo("/boot/grub/grub.cfg");
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

    // ========================================
    // Round-trip Tests
    // ========================================

    @Test
    void testFileStringRoundTrip() {
        File originalFile = new File("/system/bin/sh");
        
        // File -> String -> File
        String string = converter.convert(originalFile, String.class);
        File backToFile = converter.convert(string, File.class);
        
        assertThat(backToFile.getPath()).isEqualTo(originalFile.getPath());
    }

    @Test
    void testFileMapRoundTrip() {
        File originalFile = new File("/Applications/Safari.app");
        
        // File -> Map -> File
        Map<String, Object> map = converter.convert(originalFile, Map.class);
        File backToFile = converter.convert(map, File.class);
        
        assertThat(backToFile.getPath()).isEqualTo(originalFile.getPath());
    }

    @Test
    void testFileURIRoundTrip() {
        File originalFile = new File("/Library/Preferences/SystemConfiguration");
        
        // File -> URI -> File
        URI uri = converter.convert(originalFile, URI.class);
        File backToFile = converter.convert(uri, File.class);
        
        assertThat(backToFile.getPath()).isEqualTo(originalFile.getPath());
    }

    @Test
    void testFilePathRoundTrip() {
        File originalFile = new File("/usr/share/man/man1/ls.1");
        
        // File -> Path -> File
        Path path = converter.convert(originalFile, Path.class);
        File backToFile = converter.convert(path, File.class);
        
        assertThat(backToFile.getPath()).isEqualTo(originalFile.getPath());
    }

    @Test
    void testFileCharArrayRoundTrip() {
        File originalFile = new File("/dev/null");
        
        // File -> char[] -> File
        char[] charArray = converter.convert(originalFile, char[].class);
        File backToFile = converter.convert(charArray, File.class);
        
        assertThat(backToFile.getPath()).isEqualTo(originalFile.getPath());
    }

    @Test
    void testFileByteArrayRoundTrip() {
        File originalFile = new File("/bin/bash");
        
        // File -> byte[] -> File
        byte[] byteArray = converter.convert(originalFile, byte[].class);
        File backToFile = converter.convert(byteArray, File.class);
        
        assertThat(backToFile.getPath()).isEqualTo(originalFile.getPath());
    }

    // ========================================
    // Cross-Platform Path Tests
    // ========================================

    @Test
    void testFileConversion_unixPath() {
        String unixPath = "/home/user/.bashrc";
        File result = converter.convert(unixPath, File.class);
        assertThat(result.getPath()).isEqualTo(unixPath);
    }

    @Test
    void testFileConversion_windowsPath() {
        String windowsPath = "C:\\Windows\\System32\\drivers\\etc\\hosts";
        File result = converter.convert(windowsPath, File.class);
        assertThat(result.getPath()).isEqualTo(windowsPath);
    }

    // ========================================
    // Special Characters Tests
    // ========================================

    @Test
    void testFileConversion_specialCharacters() {
        String pathWithSpecialChars = "/tmp/file-with_special.chars@domain.txt";
        File result = converter.convert(pathWithSpecialChars, File.class);
        assertThat(result.getPath()).isEqualTo(pathWithSpecialChars);
    }

    @Test
    void testFileConversion_unicodeCharacters() {
        String pathWithUnicode = "/home/user/文档/测试文件.txt";
        File result = converter.convert(pathWithUnicode, File.class);
        assertThat(result.getPath()).isEqualTo(pathWithUnicode);
    }
}