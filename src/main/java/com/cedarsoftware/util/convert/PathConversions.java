package com.cedarsoftware.util.convert;

import java.io.File;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import static com.cedarsoftware.util.convert.MapConversions.PATH_KEY;

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
final class PathConversions {

    private PathConversions() {}

    /**
     * Convert Path to String using toString().
     */
    static String toString(Object from, Converter converter) {
        Path path = (Path) from;
        return path.toString();
    }

    /**
     * Convert Path to Map.
     */
    static Map<String, Object> toMap(Object from, Converter converter) {
        Path path = (Path) from;
        Map<String, Object> target = new LinkedHashMap<>();
        target.put(PATH_KEY, path.toString());
        return target;
    }

    /**
     * Convert Path to URI.
     */
    static URI toURI(Object from, Converter converter) {
        Path path = (Path) from;
        return path.toUri();
    }

    /**
     * Convert Path to URL.
     */
    static URL toURL(Object from, Converter converter) {
        Path path = (Path) from;
        try {
            return path.toUri().toURL();
        } catch (Exception e) {
            throw new IllegalArgumentException("Unable to convert Path to URL, input Path: " + path, e);
        }
    }

    /**
     * Convert Path to File.
     */
    static File toFile(Object from, Converter converter) {
        Path path = (Path) from;
        try {
            return path.toFile();
        } catch (UnsupportedOperationException e) {
            throw new IllegalArgumentException("Unable to convert Path to File, input Path: " + path, e);
        }
    }

    /**
     * Convert Path to char[].
     */
    static char[] toCharArray(Object from, Converter converter) {
        Path path = (Path) from;
        return path.toString().toCharArray();
    }

    /**
     * Convert Path to byte[].
     */
    static byte[] toByteArray(Object from, Converter converter) {
        Path path = (Path) from;
        return path.toString().getBytes(StandardCharsets.UTF_8);
    }
}