package com.cedarsoftware.ncube;

import com.cedarsoftware.util.IOUtilities;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

/**
 * Created by kpartlow on 5/30/2014.
 */
public class TestNCubeConcurrencyIssue
{
    @Test
    public void testConcurrencyWithDifferentFiles() throws Exception
    {
        concurrencyTest("StringFromRemoteUrlBig", "/files/ncube/FUNCDESC.txt");
        concurrencyTest("StringFromLocalUrl", "/files/some.txt");
    }

    private void concurrencyTest(final String site, String expectedFile) throws IOException
    {
        ByteArrayOutputStream out = new ByteArrayOutputStream(8192);
        URL url = NCubeManager.class.getResource(expectedFile);
        IOUtilities.transfer(new File(url.getFile()), out);
        final String expected = new String(out.toByteArray());

        //UrlCommandCell cell = new StringUrlCmd(false);
        //cell.setUrl("http://www.cedarsoftware.com/tests/ncube/some.txt");
        Thread[] threads = new Thread[16];
        final long[]iter = new long[16];


        final NCube n1 = NCubeManager.getNCubeFromResource("urlContent.json");

        final Map<String, String> items = Collections.synchronizedMap(new IdentityHashMap<String, String>());

        final int[] passed = new int[1];
        passed[0] = 0;
        final int[] count = new int[1];

        for (int i=0; i < 16; i++)
        {
            final int index = i;
            threads[i] = new Thread(new Runnable()
            {
                public void run()
                {
                    long start = System.currentTimeMillis();

                    while (System.currentTimeMillis() - start < 2000)
                    {
                        for (int j=0; j < 100; j++)
                        {
                            final Map coord = new HashMap();
                            coord.put("sites", site);
                            String item = (String)n1.getCell(coord);

                            items.put(item, item);
                            count[0] = count[0]+1;
                            assertEquals(expected, item);
                            assertNotSame(expected, item);
                        }
                        iter[index]++;
                    }
                }
            });
            threads[i].setName("NCubeConcurrencyTest" + i);
            threads[i].setDaemon(true);
            threads[i].start();
        }

        for (int i=0; i < 16; i++)
        {
            try
            {
                threads[i].join();
            }
            catch (InterruptedException e)
            {
                e.printStackTrace();
            }
        }

        assertTrue(String.format("Expected %d unique items, but only received %d", count[0], items.size()), items.size() == count[0]);
    }

}
