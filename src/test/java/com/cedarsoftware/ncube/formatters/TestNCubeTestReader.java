package com.cedarsoftware.ncube.formatters;

import com.cedarsoftware.util.IOUtilities;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.URL;

/**
 * Created by kpartlow on 9/25/2014.
 */
public class TestNCubeTestReader
{
    @Test
    public void testReading() throws Exception {

        String s = getResourceAsString("n-cube-tests/test.json");
        NCubeTestReader reader = new NCubeTestReader();
        Object o = reader.convert(s);
        System.out.println(o);
    }

    private static String getResourceAsString(String name) throws IOException
    {
        ByteArrayOutputStream out = new ByteArrayOutputStream(8192);
        URL url = TestNCubeTestReader.class.getResource("/" + name);
        IOUtilities.transfer(new File(url.getFile()), out);
        return new String(out.toByteArray(), "UTF-8");
    }


}
