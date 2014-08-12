package com.cedarsoftware.ncube.formatters;

import com.cedarsoftware.ncube.NCubeTestDto;
import com.cedarsoftware.util.IOUtilities;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

/**
 * Created by kpartlow on 8/12/2014.
 */
public class TestNCubeTestParser
{
    @Test
    public void testRead() {
        Map<String, NCubeTestDto> c = getTestsFromResource("valid-test-data.json");

        HashMap map = new HashMap();
        map.put("foo", "bar");
        map.put("style", "black");


        NCubeTestDto test1 = c.get("test1");
        Map<String, Object> coords = test1.getCoords();
        assertEquals("test1", test1.getName());
        assertEquals("value1", coords.get("coord1"));
        assertEquals(new Long(9), coords.get("coord2"));

        NCubeTestDto test2 = c.get("test2");
        coords = test2.getCoords();
        assertEquals("test2", test2.getName());
        assertEquals(true, coords.get("coord1"));
        assertEquals(5.9, coords.get("coord2"));

        NCubeTestDto test3 = c.get("test3");
        //assertEquals(map.getClass(), test3.getExpectedResult().getClass());
        assertEquals(map, test3.getExpectedResult());
    }

    public static Map<String, NCubeTestDto> getTestsFromResource(String name)
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
