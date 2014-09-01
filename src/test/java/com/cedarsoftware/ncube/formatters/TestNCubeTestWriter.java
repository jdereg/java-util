package com.cedarsoftware.ncube.formatters;

import com.cedarsoftware.ncube.NCube;
import com.cedarsoftware.ncube.NCubeManager;
import com.cedarsoftware.ncube.NCubeTest;
import com.cedarsoftware.util.DeepEquals;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Created by kpartlow on 8/27/2014.
 */
public class TestNCubeTestWriter
{
    @Test
    public void testWriting() throws Exception {
        NCube ncube = NCubeManager.getNCubeFromResource("stringIds.json");
        List<NCubeTest> generatedTests = ncube.generateNCubeTests();


        List<NCubeTest> tests = TestNCubeTestParser.getTestsFromResource("stringIds-test-data.json");


        for (NCubeTest test: tests) {
            Object o = ncube.getCell(test.getCoordinate());
            assertEquals(o, test.getExpectedResult());
        }

        String s = new NCubeTestWriter().write(tests);
        List<NCubeTest> repop = new NCubeTestParser().parse(s);

        assertEquals(tests.size(), repop.size());
        assertTrue(DeepEquals.deepEquals(tests, repop));

    }

    @Test
    public void testNullWrite() throws Exception {
        assertNull(new NCubeTestWriter().write(null));
    }
}
