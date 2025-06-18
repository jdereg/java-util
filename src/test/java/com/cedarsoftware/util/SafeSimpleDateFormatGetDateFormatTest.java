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

    @Test
    void testDifferentFormatsReturnDifferentInstances() {
        SimpleDateFormat df1 = SafeSimpleDateFormat.getDateFormat("yyyy-MM-dd");
        SimpleDateFormat df2 = SafeSimpleDateFormat.getDateFormat("MM/dd/yyyy");
        assertNotSame(df1, df2, "Different source strings should create different formatters");
    }

    @Test
    void testThreadLocalCaching() throws Exception {
        SimpleDateFormat main1 = SafeSimpleDateFormat.getDateFormat("yyyy-MM-dd");
        SimpleDateFormat main2 = SafeSimpleDateFormat.getDateFormat("yyyy-MM-dd");
        assertSame(main1, main2, "Expected cached formatter for same thread");

        final SimpleDateFormat[] holder = new SimpleDateFormat[2];
        Thread t = new Thread(() -> {
            holder[0] = SafeSimpleDateFormat.getDateFormat("yyyy-MM-dd");
            holder[1] = SafeSimpleDateFormat.getDateFormat("yyyy-MM-dd");
        });
        t.start();
        t.join();

        assertNotSame(main1, holder[0], "Formatter should be unique per thread");
        assertSame(holder[0], holder[1], "Same thread should reuse its formatter");
    }
}

