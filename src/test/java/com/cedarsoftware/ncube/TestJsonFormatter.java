package com.cedarsoftware.ncube;

import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.FilenameFilter;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by kpartlow on 3/18/14.
 */
public class TestJsonFormatter {

    @Test
    public void testJsonFormatter() throws Exception {
        // when running a single test.
        //List<String> s = new ArrayList<String>();
        //s.add("template2.json");
        List<String> s = getAllTestFiles();
        runAllTests(s);
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
                          s.endsWith("testCubeList.json"));
            }
        });

        List<String> names = new ArrayList<String>(files.length);
        for (File f : files) {
            names.add(f.getName());
        }
        return names;
    }

    public void runAllTests(List<String> strings)
    {
        for (String f : strings)
        {
            NCube ncube = NCubeManager.getNCubeFromResource(f);
            String s = ncube.toFormattedJson();
            NCube res = NCube.fromSimpleJson(s);
            Assert.assertEquals(res, ncube);
        }
    }
}