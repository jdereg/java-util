package com.cedarsoftware.ncube.formatters;

import com.cedarsoftware.ncube.NCube;
import com.cedarsoftware.ncube.NCubeManager;
import com.cedarsoftware.ncube.TestNCube;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Ignore;

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

    // TODO: Need better test that looks only for small snippets of expected values, not exact entire string.  That is
    // too brittle and OS dependent.
    @Ignore
    public void testResultsFormatting()
    {
        NCube<String> ncube = NCubeManager.getNCubeFromResource("idNoValue.json");
        Map coord = new HashMap();
        coord.put("age", 18);
        coord.put("state", "OH");

        Map output = new HashMap();
        output.put("foo.age", "56");
        output.put("foo.name", "John");
        ncube.getCells(coord, output);
        String s = new TestResultsFormatter(output).format();
        assertEquals("Result:\n" +
                "   18 OH\n" +
                "\n" +
                "Output:\n" +
                "   foo.name = John\n" +
                "   foo.age = 56\n" +
                "\n" +
                "Trace:\n" +
                "   begin: idNoValue(age:18,state:OH)\n" +
                "      {Age=18, State=OH} = 18 OH\n" +
                "   end: idNoValue = 1]", s);
    }


}
