package com.cedarsoftware.util.convert;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.atomic.DoubleAdder;
import java.util.concurrent.atomic.LongAdder;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * LongAdder and DoubleAdder conversions (4.113.0). An adder is its sum: it converts as a Long (a Double), and whatever
 * converts to a Long (a Double) converts to a new adder holding that value -- as AtomicLong does. There were none, so
 * json-io wrote an adder as an empty object and lost its count.
 * <p>
 * The pairs are in ConverterEverythingTest too (loadAdderTests), round-tripped through the test-scope json-io; this
 * class keeps the checks a pair cannot state, such as a copy being a new adder.
 */
class AdderConversionsTest {
    private final Converter converter = new Converter(new DefaultConverterOptions());

    private static LongAdder longAdder(long value) {
        LongAdder adder = new LongAdder();
        adder.add(value);
        return adder;
    }

    private static DoubleAdder doubleAdder(double value) {
        DoubleAdder adder = new DoubleAdder();
        adder.add(value);
        return adder;
    }

    @Test
    void anAdderConvertsAsItsSum() {
        assertEquals(5L, (long) converter.convert(longAdder(5), Long.class));
        assertEquals(5, (int) converter.convert(longAdder(5), int.class));
        assertEquals("5", converter.convert(longAdder(5), String.class));
        assertEquals(new BigDecimal("5"), converter.convert(longAdder(5), BigDecimal.class));
        assertEquals(2.5, converter.convert(doubleAdder(2.5), Double.class), 0.0);
        assertEquals("2.5", converter.convert(doubleAdder(2.5), String.class));

        Map<?, ?> map = converter.convert(longAdder(5), Map.class);
        assertEquals(5L, ((Number) map.values().iterator().next()).longValue());
    }

    @Test
    void whateverReachesALongOrDoubleReachesANewAdder() {
        assertEquals(7L, converter.convert(7L, LongAdder.class).sum());
        assertEquals(3L, converter.convert(3, LongAdder.class).sum());
        assertEquals(9L, converter.convert("9", LongAdder.class).sum());
        assertEquals(11L, converter.convert(Collections.singletonMap("value", 11), LongAdder.class).sum());
        assertEquals(5L, converter.convert(converter.convert(longAdder(5), Map.class), LongAdder.class).sum());

        assertEquals(1.5, converter.convert("1.5", DoubleAdder.class).sum(), 0.0);
        assertEquals(4.0, converter.convert(4, DoubleAdder.class).sum(), 0.0);
        assertEquals(5.0, converter.convert(longAdder(5), DoubleAdder.class).sum(), 0.0);

        assertNull(converter.convert(null, LongAdder.class));
        LongAdder original = longAdder(1);
        assertNotSame(original, converter.convert(original, LongAdder.class), "an adder is mutable: a copy");
        assertEquals(1L, converter.convert(original, LongAdder.class).sum());
    }
}
