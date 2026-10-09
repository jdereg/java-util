package com.cedarsoftware.util.convert;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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
