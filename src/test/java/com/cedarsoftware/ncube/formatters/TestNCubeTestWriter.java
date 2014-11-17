package com.cedarsoftware.ncube.formatters;

import com.cedarsoftware.ncube.CellInfo;
import com.cedarsoftware.ncube.NCubeTest;
import com.cedarsoftware.ncube.StringValuePair;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Created by ken on 11/17/2014.
 */
public class TestNCubeTestWriter {
    private NCubeTest verySimpleTest = new NCubeTest("foo", new StringValuePair[0], new CellInfo[]{});

    @Test
    public void testVerySimpleCase() throws Exception {
        NCubeTest[] tests = new NCubeTest[]
        {
            verySimpleTest
        };

        String s = new NCubeTestWriter().format(tests);
        assertEquals("[{\"name\":\"foo\",\"coord\":[],\"assertions\":[]}]", s);
    }

    @Test
    public void testNullCase() throws Exception {
        String s = new NCubeTestWriter().format(null);
        assertEquals("[]", s);
    }

    @Test
    public void testEmptyCase() throws Exception {
        String s = new NCubeTestWriter().format(new NCubeTest[0]);
        assertEquals("[]", s);
    }
}
