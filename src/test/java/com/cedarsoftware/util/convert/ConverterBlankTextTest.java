package com.cedarsoftware.util.convert;

import java.io.File;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDate;
import java.time.Period;
import java.time.Year;
import java.util.Currency;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import com.cedarsoftware.util.geom.Color;
import com.cedarsoftware.util.geom.Dimension;
import com.cedarsoftware.util.geom.Insets;
import com.cedarsoftware.util.geom.Point;
import com.cedarsoftware.util.geom.Rectangle;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Blank text ("", "   ") converts to null for every type without a zero, as it already did for the dates and times
 * (John's rule); the number types and Boolean keep their zero and false. It was year 0 for Year, and an exception for
 * Duration, Period, UUID, Currency, Class, an enum, File, Path and the geometry types.
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
class ConverterBlankTextTest {

    private final Converter converter = new Converter(new DefaultConverterOptions());

    @Test
    void blankTextIsNullForATypeWithoutAZero() {
        Class<?>[] types = {Year.class, Duration.class, Period.class, UUID.class, Currency.class, Class.class,
                TimeUnit.class, File.class, Path.class, Color.class, Dimension.class, Insets.class, Point.class,
                Rectangle.class, LocalDate.class};
        for (Class<?> type : types) {
            for (String blank : new String[] {"", "   ", "\t\n"}) {
                assertNull(converter.convert(blank, type), type.getSimpleName() + " from '" + blank + "'");
            }
        }
    }

    @Test
    void theNumberTypesAndBooleanKeepTheirZero() {
        assertEquals(Integer.valueOf(0), converter.convert("", Integer.class));
        assertEquals(Long.valueOf(0), converter.convert("  ", Long.class));
        assertEquals(BigDecimal.ZERO, converter.convert("", BigDecimal.class));
        assertEquals(Long.valueOf(0), converter.convert("", Number.class));
        assertEquals(Boolean.FALSE, converter.convert("", Boolean.class));
    }
}
