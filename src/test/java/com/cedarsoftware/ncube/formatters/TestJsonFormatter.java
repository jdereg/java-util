package com.cedarsoftware.ncube.formatters;

import com.cedarsoftware.ncube.NCube;
import com.cedarsoftware.ncube.NCubeManager;
import com.cedarsoftware.ncube.TestNCube;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.FilenameFilter;
import java.math.BigInteger;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;

/**
 * Created by kpartlow on 3/18/14.
 */
public class TestJsonFormatter
{
    @BeforeClass
    public static void setUp() throws Exception
    {
        TestNCube.initialize();
    }

    @Test
    public void testJsonFormatter() throws Exception
    {
        // when running a single test.
        //List<String> s = new ArrayList<String>();
        //s.add("urlContent.json");
        List<String> s = getAllTestFiles();
        runAllTests(s);
    }

    @Test
    public void testConvertArray() throws Exception
    {
        //  Load Arrays Type, write to new formatted type (they'll become Groovy Expressions
        NCube ncube = NCubeManager.getNCubeFromResource("arrays.json");

        Map<String, Object> coord = new HashMap<String, Object>();

        coord.put("Code", "longs");
        assertEquals(9223372036854775807L, ((Object[]) ncube.getCell(coord))[2]);

        coord.put("Code", "ints");
        assertEquals(2147483647, ((Object[]) ncube.getCell(coord))[2]);

        coord.put("Code", "bytes");
        assertEquals((byte) 127, ((Object[]) ncube.getCell(coord))[2]);

        coord.put("Code", "shorts");
        assertEquals((short) 32767, ((Object[]) ncube.getCell(coord))[2]);

        coord.put("Code", "booleans");
        assertEquals(Boolean.TRUE, ((Object[]) ncube.getCell(coord))[2]);
        assertEquals(Boolean.FALSE, ((Object[]) ncube.getCell(coord))[3]);

        coord.put("Code", "floats");
        assertEquals(new Float(3.8), ((Object[]) ncube.getCell(coord))[2]);

        coord.put("Code", "doubles");
        assertEquals(10.1, ((Object[]) ncube.getCell(coord))[2]);

        coord.put("Code", "bigints");
        assertEquals(new BigInteger("0"), ((Object[]) ncube.getCell(coord))[0]);
        assertEquals(new BigInteger("9223372036854775807"), ((Object[]) ncube.getCell(coord))[2]);
        assertEquals(new BigInteger("147573952589676410000"), ((Object[]) ncube.getCell(coord))[3]);


        String s = ncube.toFormattedJson();
        ncube = NCube.fromSimpleJson(s);

        coord.put("Code", "longs");
        assertEquals(9223372036854775807L, ((Object[]) ncube.getCell(coord))[2]);

        coord.put("Code", "ints");
        assertEquals(2147483647, ((Object[]) ncube.getCell(coord))[2]);

        coord.put("Code", "bytes");
        assertEquals((byte) 127, ((Object[]) ncube.getCell(coord))[2]);

        coord.put("Code", "shorts");
        assertEquals((short) 32767, ((Object[]) ncube.getCell(coord))[2]);

        coord.put("Code", "booleans");
        assertEquals(Boolean.TRUE, ((Object[]) ncube.getCell(coord))[2]);
        assertEquals(Boolean.FALSE, ((Object[]) ncube.getCell(coord))[3]);

        coord.put("Code", "floats");
        assertEquals(new Float(3.8), ((Object[]) ncube.getCell(coord))[2]);

        coord.put("Code", "doubles");
        assertEquals(10.1, ((Object[]) ncube.getCell(coord))[2]);

        coord.put("Code", "bigints");
        assertEquals(new BigInteger("0"), ((Object[]) ncube.getCell(coord))[0]);
        assertEquals(new BigInteger("9223372036854775807"), ((Object[]) ncube.getCell(coord))[2]);
        assertEquals(new BigInteger("147573952589676410000"), ((Object[]) ncube.getCell(coord))[3]);
    }

    @Test
    public void testInvalidNCube()
    {
        NCube ncube = new NCube(null);
        JsonFormatter formatter = new JsonFormatter();
        assertEquals("{\"ncube\":null,\"axes\":],\"cells\":[]}", formatter.format(ncube));
    }

    @Test
    public void testWriteObjectException() throws Exception
    {
        JsonFormatter formatter = new JsonFormatter();
        formatter.writeObjectValue(null);
    }

    public List<String> getAllTestFiles()
    {
        URL u = getClass().getClassLoader().getResource("");
        File dir = new File(u.getFile());

        File[] files = dir.listFiles(new FilenameFilter()
        {
            //  The *CubeError.json are files that have intentional parsing errors
            //  testCubeList.json is an array of cubes and JsonFormatter only knows aboue one cube at a time.
            public boolean accept(File f, String s)
            {
                return s != null && s.endsWith(".json") &&
                        !(s.endsWith("idBasedCubeError.json") ||
                                s.endsWith("idBasedCubeError2.json") ||
                                s.endsWith("error.json") ||
                                s.endsWith("arrays.json") ||  /** won't have equivalency **/
                                s.endsWith("testCubeList.json"));   /** list of cubes **/
            }
        });

        List<String> names = new ArrayList<>(files.length);
        for (File f : files)
        {
            names.add(f.getName());
        }
        return names;
    }

    public void runAllTests(List<String> strings)
    {
        for (String f : strings)
        {
//            System.out.println("Starting " + f);
            NCube ncube = NCubeManager.getNCubeFromResource(f);
            String s = ncube.toFormattedJson();
//            System.out.println(s);
            NCube res = NCube.fromSimpleJson(s);
            assertEquals(res, ncube);
        }
    }
}