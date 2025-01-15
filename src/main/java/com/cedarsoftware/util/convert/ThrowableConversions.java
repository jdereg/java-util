package com.cedarsoftware.util.convert;

import java.util.Map;

import com.cedarsoftware.util.CompactMap;

import static com.cedarsoftware.util.convert.MapConversions.CAUSE;
import static com.cedarsoftware.util.convert.MapConversions.CAUSE_MESSAGE;
import static com.cedarsoftware.util.convert.MapConversions.CLASS;
import static com.cedarsoftware.util.convert.MapConversions.MESSAGE;

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
final class ThrowableConversions {

    private ThrowableConversions() {}

    static Map<String, Object> toMap(Object from, Converter converter) {
        Throwable throwable = (Throwable) from;
        Map<String, Object> target = CompactMap.<String, Object>builder().insertionOrder().build();
        target.put(CLASS, throwable.getClass().getName());
        target.put(MESSAGE, throwable.getMessage());
        if (throwable.getCause() != null) {
            target.put(CAUSE, throwable.getCause().getClass().getName());
            target.put(CAUSE_MESSAGE, throwable.getCause().getMessage());
        }
        return target;
    }
}