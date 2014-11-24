package com.cedarsoftware.ncube.formatters;

import com.cedarsoftware.ncube.ApplicationID;
import com.cedarsoftware.ncube.CellInfo;
import com.cedarsoftware.ncube.GroovyExpression;
import com.cedarsoftware.ncube.NCube;
import com.cedarsoftware.ncube.NCubeManager;
import com.cedarsoftware.ncube.NCubeTest;
import com.cedarsoftware.ncube.RuleInfo;
import com.cedarsoftware.ncube.StringValuePair;
import com.cedarsoftware.ncube.TestingDatabaseHelper;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

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
        ncube.getCell(coord, output);
        String s = new TestResultsFormatter(output).format();
        assertEquals("<b>Axis bindings</b><pre>\n" +
                "idNoValue\n" +
                "  age: 18\n" +
                "  state: OH\n" +
                "  <b>value = 18 OH</b>\n" +
                "</pre><b>Last statement (cell) executed</b><pre>\n" +
                "18 OH\n" +
                "</pre><b>Assertions</b><pre>\n" +
                "No assertion failures\n" +
                "</pre><b>Output Map</b><pre>\n" +
                "No output\n" +
                "</pre><b>System.out</b><pre>\n" +
                "</pre><b>System.err</b><pre style=\"color:darkred\">\n" +
                "</pre>", s);
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
        output.put("foo.name", "John");

        ncube.getCell(coord, output);

        Set<String> assertionFailures = new HashSet<>();
        assertionFailures.add("[some assertion happened]");

        RuleInfo ruleInfo = (RuleInfo) output.get(NCube.RULE_EXEC_INFO);
        ruleInfo.setAssertionFailures(assertionFailures);


        String s = new TestResultsFormatter(output).format();
        assertEquals("<b>Axis bindings</b><pre>\n" +
                "idNoValue\n" +
                "  age: 18\n" +
                "  state: OH\n" +
                "  <b>value = 18 OH</b>\n" +
                "</pre><b>Last statement (cell) executed</b><pre>\n" +
                "18 OH\n" +
                "</pre><b>Assertions</b><pre>\n" +
                "[some assertion happened]</pre><b>Output Map</b><pre>\n" +
                "foo.name = John\n" +
                "foo.age = 56\n" +
                "return = 18 OH\n" +
                "</pre><b>System.out</b><pre>\n" +
                "</pre><b>System.err</b><pre style=\"color:darkred\">\n" +
                "</pre>", s);
    }

    @Test
    public void testOutput() throws Exception {
        StringValuePair<CellInfo>[] coord = new StringValuePair[0];
        CellInfo[] expected = new CellInfo[3];
        expected[0] = new CellInfo(3.0);
        expected[1] = new CellInfo(3.0f);
        expected[2] = new CellInfo(new GroovyExpression("help me", null));

        NCubeTest test = new NCubeTest("testName", coord, expected);
    }
}
