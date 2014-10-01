package com.cedarsoftware.ncube.formatters;

import com.cedarsoftware.ncube.NCubeTest;
import com.cedarsoftware.util.IOUtilities;
import org.junit.Ignore;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * Created by kpartlow on 9/25/2014.
 */
public class TestNCubeTestReader
{
    // This test is broken.
    @Ignore
    public void testReading() throws Exception {

        String s = getResourceAsString("n-cube-tests/test.json");
        NCubeTestReader reader = new NCubeTestReader();
        List<NCubeTest> list = reader.convert(s);
        assertEquals(17, list.size());
    }

    private static String getResourceAsString(String name) throws IOException
    {
        ByteArrayOutputStream out = new ByteArrayOutputStream(8192);
        URL url = TestNCubeTestReader.class.getResource("/" + name);
        IOUtilities.transfer(new File(url.getFile()), out);
        return new String(out.toByteArray(), "UTF-8");
    }


}
