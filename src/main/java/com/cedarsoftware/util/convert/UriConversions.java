package com.cedarsoftware.util.convert;

import java.net.URI;
import java.net.URL;
import java.util.Map;

import com.cedarsoftware.util.CompactLinkedMap;

import static com.cedarsoftware.util.convert.MapConversions.URI_KEY;

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
final class UriConversions {

    private UriConversions() {}

    static Map toMap(Object from, Converter converter) {
        URI uri = (URI) from;
        Map<String, Object> target = new CompactLinkedMap<>();
        target.put(URI_KEY, uri.toString());
        return target;
    }

    static URL toURL(Object from, Converter converter) {
        URI uri = (URI) from;
        try {
            return uri.toURL();
        } catch (Exception e) {
            throw new IllegalArgumentException("Unable to convert URI to URL, input URI: " + uri, e);
        }
    }
}
