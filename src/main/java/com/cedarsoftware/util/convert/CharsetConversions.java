package com.cedarsoftware.util.convert;

import java.nio.charset.Charset;
import java.util.LinkedHashMap;
import java.util.Map;

import static com.cedarsoftware.util.convert.MapConversions.CHARSET;

/**
 * Conversions from {@link Charset}. A Charset is identified by its canonical name ({@link Charset#name()}),
 * which {@link Charset#forName(String)} maps back to the same instance.
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
public final class CharsetConversions {
    private CharsetConversions() {}

    static String toString(Object from, Converter converter) {
        return ((Charset) from).name();
    }

    static Map<?, ?> toMap(Object from, Converter converter) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put(CHARSET, toString(from, converter));
        return map;
    }
}
