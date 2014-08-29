package com.cedarsoftware.ncube.formatters;

import com.cedarsoftware.ncube.NCube;
import com.cedarsoftware.ncube.NCubeManager;
import com.cedarsoftware.ncube.NCubeTest;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * Created by kpartlow on 8/27/2014.
 */
public class TestNCubeTestWriter
{
    @Test
    public void test() throws Exception {
        NCube ncube = NCubeManager.getNCubeFromResource("stringIds.json");

        //List<NCubeTest> tests = TestNCubeTestParser.getTestsFromResource("stringIds-test-data.json");

        List<NCubeTest> tests = ncube.generateNCubeTests();
        for (NCubeTest test: tests) {
            Object o = ncube.getCell(test.getCoordinate());
            assertEquals(o, test.getExpectedResult());
        }

        System.out.println(new NCubeTestWriter().write(tests));
    }
}
