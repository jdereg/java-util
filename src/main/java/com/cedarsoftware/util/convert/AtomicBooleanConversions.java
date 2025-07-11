package com.cedarsoftware.util.convert;

import java.time.Year;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author Kenny Partlow (kpartlow@gmail.com)
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
final class AtomicBooleanConversions {

    private AtomicBooleanConversions() {}

    static boolean toBoolean(Object from, Converter converter) {
        AtomicBoolean b = (AtomicBoolean) from;
        return b.get();
    }

    static AtomicBoolean toAtomicBoolean(Object from, Converter converter) {
        AtomicBoolean b = (AtomicBoolean) from;
        return new AtomicBoolean(b.get());
    }

    static Character toCharacter(Object from, Converter converter) {
        AtomicBoolean b = (AtomicBoolean) from;
        ConverterOptions options = converter.getOptions();
        return b.get() ? options.trueChar() : options.falseChar();
    }
}
