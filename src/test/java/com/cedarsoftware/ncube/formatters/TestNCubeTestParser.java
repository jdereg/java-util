package com.cedarsoftware.ncube.formatters;

import com.cedarsoftware.ncube.NCubeManager;
import com.cedarsoftware.ncube.NCubeTestDto;
import com.cedarsoftware.ncube.TestNCube;
import com.cedarsoftware.util.IOUtilities;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * Created by kpartlow on 8/12/2014.
 */
public class TestNCubeTestParser
{
    @BeforeClass
    public static void initialize() {
        TestNCube.initialize();
    }

    @After
    public void tearDown() throws Exception
    {
        NCubeManager.clearCubeList();
    }

    @Test
    public void testReadNull() throws Exception {
        assertNull(new NCubeTestParser().parse(null));
    }

    @Test(expected=IllegalArgumentException.class)
    public void testParseWithInvalidArgumnet() throws Exception {
        Map<String, Object> map = new HashMap<>();
        new NCubeTestParser().parseValue(map);
    }

    @Test
    public void testRead() {
        List<NCubeTestDto> c = getTestsFromResource("valid-test-data.json");
        assertEquals(3, c.size());

        NCubeTestDto dto = c.get(0);
        assertEquals("test1", dto.name);
        assertEquals("value1", dto.coords.get("coord1"));
        assertEquals(new Long(9), dto.coords.get("coord2"));
        assertEquals("foo", dto.expectedResult);


        dto = c.get(1);

        assertEquals("test2", dto.name);
        assertEquals(true, dto.coords.get("coord1"));
        assertEquals(5.9, dto.coords.get("coord2"));

        dto = c.get(2);

        assertEquals("test3", dto.name);
        assertEquals(true, dto.coords.get("coord1"));
        assertEquals(6, dto.coords.get("coord2"));

    }

    public static List<NCubeTestDto> getTestsFromResource(String name)
    {
        try
        {
            String json = getResourceAsString(name);
            return new NCubeTestParser().parse(json);
        }
        catch (Exception e)
        {
            throw new RuntimeException("Failed to load ncube from resource: " + name, e);
        }
    }

    private static String getResourceAsString(String name) throws IOException
    {
        ByteArrayOutputStream out = new ByteArrayOutputStream(8192);
        URL url = TestNCubeTestParser.class.getResource("/tests/" + name);
        IOUtilities.transfer(new File(url.getFile()), out);
        return new String(out.toByteArray(), "UTF-8");
    }



}
