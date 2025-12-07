package com.cedarsoftware.util.convert;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Test that verifies Map to java.sql.Date conversion preserves the exact type
 * when the map contains a java.sql.Date value under specific keys.
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
class MapToSqlDateKeyTest {
    private final Converter converter = new Converter(new DefaultConverterOptions());

    @Test
    void testMapWithSqlDateKey_returnsSqlDate() {
        // Given: a Map with "sqlDate" key containing a java.sql.Date
        java.sql.Date sqlDate = java.sql.Date.valueOf("2024-01-15");
        Map<String, Object> map = new HashMap<>();
        map.put("sqlDate", sqlDate);

        // When: converting to java.sql.Date
        Object result = converter.convert(map, java.sql.Date.class);

        // Then: the result should be exactly java.sql.Date (not a subclass or superclass)
        assertEquals(java.sql.Date.class, result.getClass());
        assertEquals(sqlDate, result);
    }

    @Test
    void testMapWithValueKey_returnsSqlDate() {
        // Given: a Map with "value" key containing a java.sql.Date
        java.sql.Date sqlDate = java.sql.Date.valueOf("2024-01-15");
        Map<String, Object> map = new HashMap<>();
        map.put("value", sqlDate);

        // When: converting to java.sql.Date
        Object result = converter.convert(map, java.sql.Date.class);

        // Then: the result should be exactly java.sql.Date (not a subclass or superclass)
        assertEquals(java.sql.Date.class, result.getClass());
        assertEquals(sqlDate, result);
    }

    @Test
    void testMapWithSqlDateKeyAsString_returnsSqlDate() {
        // Given: a Map with "sqlDate" key containing a String (like json-io would have)
        Map<String, Object> map = new HashMap<>();
        map.put("sqlDate", "2024-01-15");  // String, not java.sql.Date

        // When: converting to java.sql.Date
        Object result = converter.convert(map, java.sql.Date.class);

        // Then: the result should be exactly java.sql.Date
        assertEquals(java.sql.Date.class, result.getClass());
        assertEquals(java.sql.Date.valueOf("2024-01-15"), result);
    }

    @Test
    void testMapWithValueKeyAsString_returnsSqlDate() {
        // Given: a Map with "value" key containing a String (like json-io would have)
        Map<String, Object> map = new HashMap<>();
        map.put("value", "2024-01-15");  // String, not java.sql.Date

        // When: converting to java.sql.Date
        Object result = converter.convert(map, java.sql.Date.class);

        // Then: the result should be exactly java.sql.Date
        assertEquals(java.sql.Date.class, result.getClass());
        assertEquals(java.sql.Date.valueOf("2024-01-15"), result);
    }

    @Test
    void testMapWithSqlDateKeyAsLong_returnsSqlDate() {
        // Given: a Map with "sqlDate" key containing a Long (epoch millis)
        long epochMillis = java.sql.Date.valueOf("2024-01-15").getTime();
        Map<String, Object> map = new HashMap<>();
        map.put("sqlDate", epochMillis);

        // When: converting to java.sql.Date
        Object result = converter.convert(map, java.sql.Date.class);

        // Then: the result should be exactly java.sql.Date
        assertEquals(java.sql.Date.class, result.getClass());
        assertEquals(java.sql.Date.valueOf("2024-01-15"), result);
    }

    @Test
    void testMapWithValueKeyAsLong_returnsSqlDate() {
        // Given: a Map with "value" key containing a Long (epoch millis)
        long epochMillis = java.sql.Date.valueOf("2024-01-15").getTime();
        Map<String, Object> map = new HashMap<>();
        map.put("value", epochMillis);

        // When: converting to java.sql.Date
        Object result = converter.convert(map, java.sql.Date.class);

        // Then: the result should be exactly java.sql.Date
        assertEquals(java.sql.Date.class, result.getClass());
        assertEquals(java.sql.Date.valueOf("2024-01-15"), result);
    }
}
