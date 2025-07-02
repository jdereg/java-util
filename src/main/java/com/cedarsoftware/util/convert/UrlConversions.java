package com.cedarsoftware.util.convert;

import java.net.URI;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.Map;

import static com.cedarsoftware.util.convert.MapConversions.URL_KEY;

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
final class UrlConversions {

    private UrlConversions() {}

    static Map<String, Object> toMap(Object from, Converter converter) {
        URL url = (URL) from;
        Map<String, Object> target = new LinkedHashMap<>();
        target.put(URL_KEY, url.toString());
        return target;
    }

    static URI toURI(Object from, Converter converter) {
        URL url = (URL) from;
        try {
            return url.toURI();
        } catch (Exception e) {
            throw new IllegalArgumentException("Unable to convert URL to URI, input URL: " + url, e);
        }
    }

    static java.io.File toFile(Object from, Converter converter) {
        URL url = (URL) from;
        try {
            URI uri = url.toURI();
            return new java.io.File(uri);
        } catch (Exception e) {
            throw new IllegalArgumentException("Unable to convert URL to File, input URL: " + url, e);
        }
    }

    static java.nio.file.Path toPath(Object from, Converter converter) {
        URL url = (URL) from;
        try {
            URI uri = url.toURI();
            return java.nio.file.Paths.get(uri);
        } catch (Exception e) {
            throw new IllegalArgumentException("Unable to convert URL to Path, input URL: " + url, e);
        }
    }
}
