package com.cedarsoftware.ncube;

import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.powermock.core.IdentityHashSet;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertTrue;

/**
 * Created by kpartlow on 5/30/2014.
 */
public class TestNCubeConcurrency
{
    @BeforeClass
    public static void initialize() {
        TestNCube.initialize();
    }

    @Ignore
    public void testConcurrencyWithDifferentFiles() throws Exception
    {
        concurrencyTest("StringFromRemoteUrlBig", true);
        concurrencyTest("StringFromLocalUrl", false);
        concurrencyTest("BinaryFromRemoteUrl", true);
        concurrencyTest("BinaryFromLocalUrl", false);
    }


    private void concurrencyTest(final String site, boolean cached) throws IOException
    {
        //UrlCommandCell cell = new StringUrlCmd(false);
        //cell.setUrl("http://www.cedarsoftware.com/tests/ncube/some.txt");
        Thread[] threads = new Thread[16];
        final long[]iter = new long[16];


        final NCube n1 = NCubeManager.getNCubeFromResource("urlContent.json");

        final Set<Object> items = Collections.synchronizedSet(new IdentityHashSet<Object>());

        final AtomicInteger count = new AtomicInteger(0);

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
                            Object item = n1.getCell(coord);

                            items.add(item);

                            count.incrementAndGet();
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

        if (cached)
        {
            assertTrue(String.format("Expected 1 unique item since cached, but received %d", items.size()), 1 == items.size());
        }
        else
        {
            assertTrue(String.format("Expected %d unique items, but only received %d", count.get(), items.size()), items.size() == count.get());
        }
    }
}
