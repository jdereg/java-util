package com.cedarsoftware.ncube.formatters;

import com.cedarsoftware.ncube.NCube;
import com.cedarsoftware.ncube.NCubeManager;
import com.cedarsoftware.ncube.TestNCube;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

/**
 * Created by kpartlow on 9/16/2014.
 */
public class TestTestResultsFormatter
{
    @BeforeClass
    public static void init() throws Exception
    {
        TestNCube.initialize();
    }

    @After
    public void tearDown() throws Exception
    {
        NCubeManager.clearCubeList();
    }

    @Test
    public void testResultsFormatting()
    {
        NCube<String> ncube = NCubeManager.getNCubeFromResource("idNoValue.json");
        Map coord = new HashMap();
        coord.put("age", 18);
        coord.put("state", "OH");

        Map output = new HashMap();
        ncube.getCells(coord, output);
        String s = new TestResultsFormatter(output).format();
        assertEquals("Result:  18 OH\n" +
                "begin: idNoValue = {age=18, state=OH}\n" +
                "   {Age=18, State=OH} = 18 OH\n" +
                "end: idNoValue = 1\n", s);
    }


}
