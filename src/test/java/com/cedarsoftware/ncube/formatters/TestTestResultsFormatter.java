package com.cedarsoftware.ncube.formatters;

import com.cedarsoftware.ncube.ApplicationID;
import com.cedarsoftware.ncube.CellInfo;
import com.cedarsoftware.ncube.GroovyExpression;
import com.cedarsoftware.ncube.NCube;
import com.cedarsoftware.ncube.NCubeManager;
import com.cedarsoftware.ncube.NCubeTest;
import com.cedarsoftware.ncube.StringValuePair;
import com.cedarsoftware.ncube.TestingDatabaseHelper;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.Assert.assertEquals;

/**
 * Created by kpartlow on 9/16/2014.
 */
public class TestTestResultsFormatter
{
    @Before
    public void init() throws Exception
    {
        TestingDatabaseHelper.setupDatabase();
    }

    @After
    public void tearDown() throws Exception
    {
        TestingDatabaseHelper.tearDownDatabase();
    }

    @Test
    public void testResultsFromNCube()
    {
        NCube<String> ncube = NCubeManager.getNCubeFromResource(ApplicationID.defaultAppId, "idNoValue.json");
        Map coord = new HashMap();
        coord.put("age", 18);
        coord.put("state", "OH");


        Map output = new HashMap();
        output.put("_failures", new TreeSet());
        ncube.getCell(coord, output);
        String s = new TestResultsFormatter(output).format();
        assertEquals("<b>Result</b><pre>\n" +
                "   18 OH\n" +
                "</pre><b>Output</b><pre>\n" +
                "   No output\n" +
                "</pre><b>Trace</b><pre>\n" +
                "   begin: idNoValue(age:18,state:OH)\n" +
                "      {Age=18, State=OH} = 18 OH\n" +
                "   end: idNoValue = 1</pre>", s);
    }

    @Test
    public void testResultsWithOutputAndError() throws Exception
    {
        NCube<String> ncube = NCubeManager.getNCubeFromResource(ApplicationID.defaultAppId, "idNoValue.json");
        Map coord = new HashMap();
        coord.put("age", 18);
        coord.put("state", "OH");

        Map output = new HashMap();
        output.put("foo.age", "56");
        output.put("foo.nassertame", "John");

        ncube.getCell(coord, output);

        Set<String> set = new HashSet<>();
        set.add("[some assertion happened]");

        output.put("_failures", set);


        String s = new TestResultsFormatter(output).format();

        assertEquals("<b>Result</b><pre>\n" +
                "   18 OH\n" +
                "\n" +
                "   [some assertion happened]</pre><b>Output</b><pre>\n" +
                "   foo.nassertame = John\n" +
                "   foo.age = 56\n" +
                "</pre><b>Trace</b><pre>\n" +
                "   begin: idNoValue(age:18,state:OH)\n" +
                "      {Age=18, State=OH} = 18 OH\n" +
                "   end: idNoValue = 1</pre>", s);

    }

    @Test
    public void testOutput() throws Exception {
        StringValuePair<CellInfo>[] coord = new StringValuePair[0];
        CellInfo[] expected = new CellInfo[3];
        expected[0] = new CellInfo(new Double(3.0));
        expected[1] = new CellInfo(new Float(3.0));
        expected[2] = new CellInfo(new GroovyExpression("help me", null));

        NCubeTest test = new NCubeTest("testName", coord, expected);
    }
}
