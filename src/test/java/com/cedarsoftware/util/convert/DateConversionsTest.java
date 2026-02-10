package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Tests for DateConversions bugs.
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
class DateConversionsTest {

    private Converter converter;

    @BeforeEach
    void setUp() {
        converter = new Converter(new DefaultConverterOptions());
    }

    @Test
    void toString_sqlDate_shouldNotThrowUnsupportedOperationException() {
        // java.sql.Date representing 2024-06-15
        java.sql.Date sqlDate = java.sql.Date.valueOf("2024-06-15");

        // Direct call — before fix, this throws UnsupportedOperationException
        String result = DateConversions.toString(sqlDate, converter);
        assertNotNull(result);
    }

    // Verify the methods still work for regular java.util.Date
    @Test
    void toString_utilDate_stillWorks() {
        java.util.Date date = new java.util.Date(1718409600000L); // 2024-06-15 approx
        String result = DateConversions.toString(date, converter);
        assertNotNull(result);
    }
}
