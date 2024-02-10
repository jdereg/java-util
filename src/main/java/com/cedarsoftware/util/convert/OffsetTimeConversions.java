package com.cedarsoftware.util.convert;

import java.time.OffsetTime;
import java.time.format.DateTimeFormatter;

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
final class OffsetTimeConversions {
    private OffsetTimeConversions() {}

    static String toString(Object from, Converter converter) {
        OffsetTime offsetTime = (OffsetTime) from;
        return offsetTime.format(DateTimeFormatter.ISO_OFFSET_TIME);
    }
}
