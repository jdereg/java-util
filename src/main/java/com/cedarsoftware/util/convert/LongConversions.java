package com.cedarsoftware.util.convert;

import java.time.LocalTime;

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
final class LongConversions {

    private LongConversions() {}

    static LocalTime toLocalTime(Object from, Converter converter) {
        long millis = (Long) from;
        try {
            return LocalTime.ofNanoOfDay(millis * 1_000_000);
        } catch (Exception e) {
            throw new IllegalArgumentException("Input value [" + millis + "] for conversion to LocalTime must be >= 0 && <= 86399999", e);
        }
    }
}
