package com.cedarsoftware.util.convert;

import java.io.File;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import static com.cedarsoftware.util.convert.MapConversions.FILE_KEY;

/**
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
final class FileConversions {

    private FileConversions() {}

    /**
     * Convert File to String using getPath().
     */
    static String toString(Object from, Converter converter) {
        File file = (File) from;
        return file.getPath();
    }

    /**
     * Convert File to Map.
     */
    static Map<String, Object> toMap(Object from, Converter converter) {
        File file = (File) from;
        Map<String, Object> target = new LinkedHashMap<>();
        target.put(FILE_KEY, file.getPath());
        return target;
    }

    /**
     * Convert File to URI.
     */
    static URI toURI(Object from, Converter converter) {
        File file = (File) from;
        return file.toURI();
    }

    /**
     * Convert File to URL.
     */
    static URL toURL(Object from, Converter converter) {
        File file = (File) from;
        try {
            return file.toURI().toURL();
        } catch (Exception e) {
            throw new IllegalArgumentException("Unable to convert File to URL, input File: " + file, e);
        }
    }

    /**
     * Convert File to Path.
     */
    static Path toPath(Object from, Converter converter) {
        File file = (File) from;
        return file.toPath();
    }

    /**
     * Convert File to char[].
     */
    static char[] toCharArray(Object from, Converter converter) {
        File file = (File) from;
        return file.getPath().toCharArray();
    }

    /**
     * Convert File to byte[].
     */
    static byte[] toByteArray(Object from, Converter converter) {
        File file = (File) from;
        return file.getPath().getBytes(StandardCharsets.UTF_8);
    }
}