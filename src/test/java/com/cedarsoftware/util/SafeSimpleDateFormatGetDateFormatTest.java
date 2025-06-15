package com.cedarsoftware.util;

import java.text.SimpleDateFormat;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Tests for {@link SafeSimpleDateFormat#getDateFormat(String)}.
 */
public class SafeSimpleDateFormatGetDateFormatTest {

    @Test
    void testSameThreadReturnsCachedInstance() {
        SimpleDateFormat df1 = SafeSimpleDateFormat.getDateFormat("yyyy-MM-dd");
        SimpleDateFormat df2 = SafeSimpleDateFormat.getDateFormat("yyyy-MM-dd");
        assertSame(df1, df2, "Expected cached formatter for same thread");
    }

    @Test
    void testDifferentThreadsReturnDifferentInstances() throws Exception {
        final SimpleDateFormat[] holder = new SimpleDateFormat[1];
        Thread t = new Thread(() -> holder[0] = SafeSimpleDateFormat.getDateFormat("yyyy-MM-dd"));
        t.start();
        t.join();
        SimpleDateFormat main = SafeSimpleDateFormat.getDateFormat("yyyy-MM-dd");
        assertNotSame(main, holder[0], "Threads should not share cached formatter");
    }
}
