package com.cedarsoftware.ncube;

import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.FilenameFilter;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by kpartlow on 3/18/14.
 */
public class TestJsonFormatter {

    @Test
    public void testFormatter() throws Exception {
        //runAllTests();
        //List<String> s = getAllTestFiles();
        List<String> s = new ArrayList<String>();
        s.add("simpleJsonArrayTest.json");
        runAllTests(s);
    }

    public List<String> getAllTestFiles()
    {
        URL u = getClass().getClassLoader().getResource("");
        File dir = new File(u.getFile());

        File[] files = dir.listFiles(new FilenameFilter()
        {
            public boolean accept(File f, String s)
            {
                return s != null && s.endsWith(".json") &&
                        !(s.endsWith("idBasedCubeError.json") || s.endsWith("idBasedCubeError2.json"));
            }
        });

        List<String> names = new ArrayList<String>(files.length);
        for (File f : files) {
            if (names.add(f.getName()));
        }
        return names;
    }

    public void runAllTests(List<String> strings)
    {
        for (String f : strings)
        {
            //System.out.print("\"" + f.getName() + "\",\n");


            try
            {
                NCube ncube = NCubeManager.getNCubeFromResource(f);
                String s = ncube.toFormattedJson();
                System.out.println(s);
                NCube res = NCube.fromSimpleJson(s);
                Assert.assertEquals(res, ncube);
            } catch (Exception e) {
                System.out.println("Exception:  " + f);
                e.printStackTrace();
            } catch (Error e) {
                System.out.println("Error: " + f);
                e.printStackTrace();
                //System.out.println("Exception:  " + e);
            }

        }
    }
}