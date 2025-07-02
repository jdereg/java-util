package com.cedarsoftware.util.convert;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
class ConversionDateTest {
    private Converter converter;

    @BeforeEach
    void setUp() {
        this.converter = new Converter(new DefaultConverterOptions());
    }

    @Test
    void testUtilDateToUtilDate() {
        Date utilNow = new Date();
        Date coerced = converter.convert(utilNow, Date.class);

        assertEquals(utilNow, coerced);
        assertFalse(coerced instanceof java.sql.Date);
        assertNotSame(utilNow, coerced);
    }

    @Test
    void testUtilDateToSqlDate() {
        Date utilNow = new Date();
        java.sql.Date sqlCoerced = converter.convert(utilNow, java.sql.Date.class);

        LocalDate expectedLD = Instant.ofEpochMilli(utilNow.getTime())
                .atZone(converter.getOptions().getZoneId())
                .toLocalDate();
        java.sql.Date expectedSql = java.sql.Date.valueOf(expectedLD);

        assertEquals(expectedSql.toString(), sqlCoerced.toString());
    }

    @Test
    void testSqlDateToSqlDate() {
        Date utilNow = new Date();
        java.sql.Date sqlNow = new java.sql.Date(utilNow.getTime());

        LocalDate expectedLD = Instant.ofEpochMilli(sqlNow.getTime())
                .atZone(ZoneOffset.systemDefault())
                .toLocalDate();
        java.sql.Date expectedSql = java.sql.Date.valueOf(expectedLD);
        java.sql.Date sqlCoerced = converter.convert(sqlNow, java.sql.Date.class);

        assertEquals(expectedSql.toString(), sqlCoerced.toString());
    }

    @Test
    void testDateToTimestampConversions() {
        Date utilNow = new Date();

        // Use the ZoneId from ConverterOptions
        ZoneId zoneId = converter.getOptions().getZoneId();

        // Convert to LocalDate using the configured ZoneId
        LocalDate expectedLocalDate = utilNow.toInstant()
                .atZone(zoneId)
                .toLocalDate();

        Timestamp tstamp = converter.convert(utilNow, Timestamp.class);
        LocalDate timestampLocalDate = tstamp.toInstant()
                .atZone(zoneId)
                .toLocalDate();
        assertEquals(expectedLocalDate, timestampLocalDate, "Date portions should match using configured timezone");

        Date someDate = converter.convert(tstamp, Date.class);
        LocalDate convertedLocalDate = someDate.toInstant()
                .atZone(zoneId)
                .toLocalDate();
        assertEquals(expectedLocalDate, convertedLocalDate, "Date portions should match using configured timezone");
        assertFalse(someDate instanceof Timestamp);
    }

    @Test
    void testStringToDateConversions() {
        Calendar cal = Calendar.getInstance();
        cal.clear();
        cal.set(2015, 0, 17, 9, 54);

        Date date = converter.convert("2015-01-17 09:54", Date.class);
        assertEquals(cal.getTime(), date);
        assertNotNull(date);
        assertFalse(date instanceof java.sql.Date);

        java.sql.Date sqlDate = converter.convert("2015-01-17 09:54", java.sql.Date.class);
        assertEquals("2015-01-17", sqlDate.toString());
        assertNotNull(sqlDate);
    }

    @Test
    void testCalendarToDateConversions() {
        Calendar cal = Calendar.getInstance();
        cal.clear();
        cal.set(2015, 0, 17, 9, 54);

        Date date = converter.convert(cal, Date.class);
        assertEquals(cal.getTime(), date);
        assertNotNull(date);
        assertFalse(date instanceof java.sql.Date);
    }

    @Test
    void testLongToDateConversions() {
        long now = System.currentTimeMillis();
        Date dateNow = new Date(now);

        Date converted = converter.convert(now, Date.class);
        assertNotNull(converted);
        assertEquals(dateNow, converted);
        assertFalse(converted instanceof java.sql.Date);
    }

    @Test
    void testAtomicLongToDateConversions() {
        long now = System.currentTimeMillis();
        Date dateNow = new Date(now);

        Date converted = converter.convert(new AtomicLong(now), Date.class);
        assertNotNull(converted);
        assertEquals(dateNow, converted);
        assertFalse(converted instanceof java.sql.Date);
    }

    @Test
    void testBigNumberToDateConversions() {
        long now = System.currentTimeMillis();
        BigInteger bigInt = new BigInteger("" + now);                    // millis (legacy class rule)
        BigDecimal bigDec = new BigDecimal(now / 1000);                  // seconds

        LocalDate expectedLD = Instant.ofEpochMilli(now)
                .atZone(ZoneOffset.systemDefault())
                .toLocalDate();
        java.sql.Date expectedSql = java.sql.Date.valueOf(expectedLD);

        assertEquals(expectedSql.toLocalDate(), converter.convert(bigInt, java.sql.Date.class).toLocalDate());
        assertEquals(expectedSql.toLocalDate(), converter.convert(bigDec, java.sql.Date.class).toLocalDate());
    }

    @Test
    void testInvalidSourceType() {
        assertThrows(IllegalArgumentException.class, () ->
                        converter.convert(TimeZone.getDefault(), Date.class),
                "Should throw exception for invalid source type"
        );

        assertThrows(IllegalArgumentException.class, () ->
                        converter.convert(TimeZone.getDefault(), java.sql.Date.class),
                "Should throw exception for invalid source type"
        );
    }

    @Test
    void testInvalidDateString() {
        assertThrows(IllegalArgumentException.class, () ->
                        converter.convert("2015/01/33", Date.class),
                "Should throw exception for invalid date"
        );

        assertThrows(IllegalArgumentException.class, () ->
                        converter.convert("2015/01/33", java.sql.Date.class),
                "Should throw exception for invalid date"
        );
    }
}