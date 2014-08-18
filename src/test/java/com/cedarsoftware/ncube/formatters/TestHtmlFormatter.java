package com.cedarsoftware.ncube.formatters;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Created by ken on 8/15/2014.
 */
public class TestHtmlFormatter {

    @Test
    public void testGetCellValueAsString() {
        assertEquals("null", HtmlFormatter.getCellValueAsString(null));
        assertEquals("foo", HtmlFormatter.getCellValueAsString("foo"));
        assertEquals("[0, 1, 2, 3]", HtmlFormatter.getCellValueAsString(new int[] { 0, 1, 2, 3}));
        assertEquals("[true, false]", HtmlFormatter.getCellValueAsString(new Object[] { Boolean.TRUE, Boolean.FALSE}));
        assertEquals("5.0", HtmlFormatter.getCellValueAsString(5.0));
    }
}
