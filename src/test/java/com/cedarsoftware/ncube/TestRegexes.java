package com.cedarsoftware.ncube;

import org.junit.Test;

import java.util.regex.Matcher;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Created by kpartlow on 4/2/2014.
 */
public class TestRegexes
{
    @Test
    public void testLatLongRegex() {

        Matcher m = Regexes.valid2Doubles.matcher("25.8977899,56.899988");
        assertTrue(m.matches());
        assertEquals("25.8977899", m.group(1));
        assertEquals("56.899988", m.group(2));

        m = Regexes.valid2Doubles.matcher(" 25.8977899, 56.899988 ");
        assertTrue(m.matches());
        assertEquals("25.8977899", m.group(1));
        assertEquals("56.899988", m.group(2));

        m = Regexes.valid2Doubles.matcher("-25.8977899,-56.899988 ");
        assertTrue(m.matches());
        assertEquals("-25.8977899", m.group(1));
        assertEquals("-56.899988", m.group(2));

        m = Regexes.valid2Doubles.matcher("N25.8977899, 56.899988 ");
        assertFalse(m.matches());

        m = Regexes.valid2Doubles.matcher("25.8977899, E56.899988 ");
        assertFalse(m.matches());

        m = Regexes.valid2Doubles.matcher("25., 56.899988 ");
        assertFalse(m.matches());

        m = Regexes.valid2Doubles.matcher("25.891919, 56. ");
        assertFalse(m.matches());
    }
}
