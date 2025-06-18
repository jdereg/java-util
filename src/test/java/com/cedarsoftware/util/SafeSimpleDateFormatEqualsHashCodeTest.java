package com.cedarsoftware.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

public class SafeSimpleDateFormatEqualsHashCodeTest {

    @Test
    void testEquals() {
        SafeSimpleDateFormat df1 = new SafeSimpleDateFormat("yyyy-MM-dd");
        SafeSimpleDateFormat df2 = new SafeSimpleDateFormat("yyyy-MM-dd");
        SafeSimpleDateFormat df3 = new SafeSimpleDateFormat("MM/dd/yyyy");

        assertEquals(df1, df2);
        assertEquals(df2, df1);
        assertEquals(df1, df1);
        assertNotEquals(df1, df3);
        assertNotEquals(df1, Boolean.TRUE);
    }

    @Test
    void testHashCode() {
        SafeSimpleDateFormat df1 = new SafeSimpleDateFormat("yyyy-MM-dd");
        SafeSimpleDateFormat df2 = new SafeSimpleDateFormat("yyyy-MM-dd");
        SafeSimpleDateFormat df3 = new SafeSimpleDateFormat("MM/dd/yyyy");

        assertEquals(df1.hashCode(), df2.hashCode());
        assertNotEquals(df1.hashCode(), df3.hashCode());
    }
}
