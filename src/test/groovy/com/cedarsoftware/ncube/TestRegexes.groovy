package com.cedarsoftware.ncube

import org.junit.Test

import java.util.regex.Matcher

/**
 * Created by kpartlow on 4/2/2014.
 */
public class TestRegexes
{
    @Test
    public void testLatLongRegex()
    {
        Matcher m = Regexes.valid2Doubles.matcher '25.8977899,56.899988'
        assert m.matches()
        assert '25.8977899' == m.group(1)
        assert '56.899988' == m.group(2)

        m = Regexes.valid2Doubles.matcher ' 25.8977899, 56.899988 '
        assert m.matches()
        assert '25.8977899' == m.group(1)
        assert '56.899988' == m.group(2)

        m = Regexes.valid2Doubles.matcher '-25.8977899,-56.899988 '
        assert m.matches()
        assert '-25.8977899' == m.group(1)
        assert '-56.899988' == m.group(2)

        m = Regexes.valid2Doubles.matcher 'N25.8977899, 56.899988 '
        assert !m.matches()

        m = Regexes.valid2Doubles.matcher '25.8977899, E56.899988 '
        assert !m.matches()

        m = Regexes.valid2Doubles.matcher '25., 56.899988 '
        assert !m.matches()

        m = Regexes.valid2Doubles.matcher '25.891919, 56. '
        assert !m.matches()
    }
}
