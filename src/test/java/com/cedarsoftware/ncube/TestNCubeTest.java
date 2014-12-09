package com.cedarsoftware.ncube;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;

/**
 * Created by kpartlow on 10/24/2014.
 */
public class TestNCubeTest
{
    @Before
    public void setUp() throws Exception
    {
        TestingDatabaseHelper.setupDatabase();
    }

    @After
    public void tearDown() throws Exception
    {
        TestingDatabaseHelper.tearDownDatabase();
    }

    @Test
    public void testNCubeTest()
    {
        String name = "test-1";
        StringValuePair[] params = new StringValuePair[3];
        params[0] = new StringValuePair("int", new CellInfo("int", "5", false, false));
        params[1] = new StringValuePair("double", new CellInfo("double", "5.75", false, false));
        params[2] = new StringValuePair("key1", new CellInfo("string", "foo", false, false));

        CellInfo[] info = new CellInfo[1];
        info[0] = new CellInfo("exp", "'bar'", false, false);

        NCubeTest test = new NCubeTest(name, params, info);

        CellInfo[] assertions = test.getAssertions();
        assertEquals(1, assertions.length);

        StringValuePair<CellInfo>[] coord = test.getCoord();
        assertEquals(3, coord.length);

        final Map<String, Object> coord1 = test.createCoord();
        assertEquals(5, coord1.get("int"));
        assertEquals(5.75, coord1.get("double"));
        assertEquals("foo", coord1.get("key1"));

        final List<GroovyExpression> testAssertions = test.createAssertions();

        // unfortunately you have to pass in ncube to the execute.
        HashMap map = new HashMap();
        map.put("ncube", new NCube("hello"));
        map.put("input", new HashMap());
        map.put("output", new HashMap());

        assertEquals("bar", testAssertions.get(0).execute(map));

    }
}
