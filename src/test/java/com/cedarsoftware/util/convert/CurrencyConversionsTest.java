package com.cedarsoftware.util.convert;

import java.util.Collections;
import java.util.Currency;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static com.cedarsoftware.util.convert.MapConversions.VALUE;
import static org.assertj.core.api.Assertions.assertThat;
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
class CurrencyConversionsTest {
    private final Converter converter = new Converter(new DefaultConverterOptions());

    @Test
    void testStringToCurrency() {
        // Major currencies
        assertThat(converter.convert("USD", Currency.class)).isEqualTo(Currency.getInstance("USD"));
        assertThat(converter.convert("EUR", Currency.class)).isEqualTo(Currency.getInstance("EUR"));
        assertThat(converter.convert("GBP", Currency.class)).isEqualTo(Currency.getInstance("GBP"));
        assertThat(converter.convert("JPY", Currency.class)).isEqualTo(Currency.getInstance("JPY"));

        // Test trimming
        assertThat(converter.convert(" USD ", Currency.class)).isEqualTo(Currency.getInstance("USD"));

        // Invalid currency code
        assertThrows(IllegalArgumentException.class, () ->
                converter.convert("INVALID", Currency.class));
    }

    @Test
    void testCurrencyToString() {
        // Major currencies
        assertThat(converter.convert(Currency.getInstance("USD"), String.class)).isEqualTo("USD");
        assertThat(converter.convert(Currency.getInstance("EUR"), String.class)).isEqualTo("EUR");
        assertThat(converter.convert(Currency.getInstance("GBP"), String.class)).isEqualTo("GBP");
        assertThat(converter.convert(Currency.getInstance("JPY"), String.class)).isEqualTo("JPY");
    }

    @Test
    void testMapToCurrency() {
        Map<String, Object> map = Collections.singletonMap(VALUE, "USD");
        Currency currency = converter.convert(map, Currency.class);
        assertThat(currency).isEqualTo(Currency.getInstance("USD"));

        map = Collections.singletonMap(VALUE, "EUR");
        currency = converter.convert(map, Currency.class);
        assertThat(currency).isEqualTo(Currency.getInstance("EUR"));

        // Invalid currency in map
        Map<String, Object> map2 = Collections.singletonMap(VALUE, "INVALID");
        assertThrows(IllegalArgumentException.class, () -> converter.convert(map2, Currency.class));
    }

    @Test
    void testCurrencyToMap() {
        Currency currency = Currency.getInstance("USD");
        Map<String, String> map = converter.convert(currency, Map.class);
        assertThat(map).containsEntry(VALUE, "USD");

        currency = Currency.getInstance("EUR");
        map = converter.convert(currency, Map.class);
        assertThat(map).containsEntry(VALUE, "EUR");
    }

    @Test
    void testCurrencyToCurrency() {
        Currency original = Currency.getInstance("USD");
        Currency converted = converter.convert(original, Currency.class);
        assertThat(converted).isSameAs(original);  // Currency instances are cached

        original = Currency.getInstance("EUR");
        converted = converter.convert(original, Currency.class);
        assertThat(converted).isSameAs(original);  // Currency instances are cached
    }
}