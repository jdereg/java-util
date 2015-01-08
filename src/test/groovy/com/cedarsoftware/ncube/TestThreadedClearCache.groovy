package com.cedarsoftware.ncube

import org.junit.After
import org.junit.Before
import org.junit.Test

import static org.junit.Assert.assertEquals

/**
 * @author John DeRegnaucourt (jdereg@gmail.com)
 *         <br/>
 *         Copyright (c) Cedar Software LLC
 *         <br/><br/>
 *         Licensed under the Apache License, Version 2.0 (the 'License')
 *         you may not use this file except in compliance with the License.
 *         You may obtain a copy of the License at
 *         <br/><br/>
 *         http://www.apache.org/licenses/LICENSE-2.0
 *         <br/><br/>
 *         Unless required by applicable law or agreed to in writing, software
 *         distributed under the License is distributed on an 'AS IS' BASIS,
 *         WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *         See the License for the specific language governing permissions and
 *         limitations under the License.
 */
public class TestThreadedClearCache
{
    public static String USER_ID = TestNCubeManager.USER_ID
    public static ApplicationID appId = new ApplicationID(ApplicationID.DEFAULT_TENANT, "clearCacheTest", ApplicationID.DEFAULT_VERSION, ReleaseStatus.SNAPSHOT.name())
    public static ApplicationID appId2 = new ApplicationID(ApplicationID.DEFAULT_TENANT, "clearCacheTest2", ApplicationID.DEFAULT_VERSION, ReleaseStatus.SNAPSHOT.name())

    private TestingDatabaseManager manager;

    @Before
    public void setup() throws Exception {
        manager = TestingDatabaseHelper.getTestingDatabaseManager()
        manager.setUp()

        NCubeManager.setNCubePersister(TestingDatabaseHelper.getPersister())
    }

    @After
    public void tearDown() throws Exception {
        manager.tearDown()
        manager = null;

        NCubeManager.clearCache()
    }

    @Test
    public void testCubesWithThreadedClearCache() throws Exception {
        NCube[] ncubes = TestingDatabaseHelper.getCubesFromDisk("sys.classpath.2per.app.json", "math.controller.json");

        // add cubes for this test.
        manager.addCubes(appId, USER_ID, ncubes)
        manager.addCubes(appId2, USER_ID, ncubes)

        concurrencyTest();

        // remove cubes
        manager.removeCubes(appId, USER_ID, ncubes);
        manager.removeCubes(appId2, USER_ID, ncubes);
    }


    @Test
    public void testCubesWithThreadedClearCacheWithAppId() throws Exception {
        NCube[] ncubes = TestingDatabaseHelper.getCubesFromDisk("sys.classpath.2per.app.json", "math.controller.json");

        // add cubes for this test.
        manager.addCubes(appId, USER_ID, ncubes)

        concurrencyTestWithAppId();

        // remove cubes
        manager.removeCubes(appId, USER_ID, ncubes);
    }

    private void concurrencyTest() throws Exception
    {
        def firstApp =
                {
                    long start = System.currentTimeMillis()
                    while (System.currentTimeMillis() - start < 3000) {
                        for (int j = 0; j < 100; j++) {
                            NCube cube = NCubeManager.getCube(appId, "MathController")

                            def input = [:]
                            input.env = "a"
                            input.x = 5
                            input.method = 'square'

                            assertEquals(25, cube.getCell(input))

                            input.method = 'factorial'
                            assertEquals(120, cube.getCell(input))

                            input.env = "b"
                            input.x = 6
                            input.method = 'square'
                            assertEquals(6, cube.getCell(input))
                            input.method = 'factorial'
                            assertEquals(6, cube.getCell(input))
                        }
                    }
                }

        def secondApp =
                {
                    long start = System.currentTimeMillis()
                    while (System.currentTimeMillis() - start < 3000) {
                        for (int j = 0; j < 100; j++) {
                            NCube cube = NCubeManager.getCube(appId, "MathController")

                            def input = [:]
                            input.env = "a"
                            input.x = 5
                            input.method = 'square'

                            assertEquals(25, cube.getCell(input))

                            input.method = 'factorial'
                            assertEquals(120, cube.getCell(input))

                            input.env = "b"
                            input.x = 6
                            input.method = 'square'
                            assertEquals(6, cube.getCell(input))
                            input.method = 'factorial'
                            assertEquals(6, cube.getCell(input))
                        }
                    }
                }

        def clearCache = {
            long start = System.currentTimeMillis()
            while (System.currentTimeMillis() - start < 3000) {
                NCubeManager.clearCache();
            }
        }


        Thread[] firstAppThreads = new Thread[8]
        Thread[] secondAppThreads = new Thread[8]

        for (int i = 0; i < 8; i++)
        {
            firstAppThreads[i] = new Thread(firstApp);
            firstAppThreads[i].name = 'FirstApp' + i
            firstAppThreads[i].daemon = true

            secondAppThreads[i] = new Thread(secondApp);
            secondAppThreads[i].name = 'SecondApp' + i
            secondAppThreads[i].daemon = true
        }

        Thread clear = new Thread(clearCache);
        clear.name = "ClearCache";
        clear.daemon = true;

        // Start all at the same time (more concurrent than starting them during construction)
        for (int i = 0; i < 8; i++)
        {
            firstAppThreads[i].start()
            secondAppThreads[i].start()
        }
        clear.start();

        for (int i = 0; i < 8; i++)
        {
            try
            {
                firstAppThreads[i].join()
                secondAppThreads[i].join()
            }
            catch (InterruptedException ignored)
            { }
        }
        try {
            clear.join();
        } catch (InterruptedException ignored) {
        }
    }

    private void concurrencyTestWithAppId() throws Exception
    {
        def run =
                {
                    long start = System.currentTimeMillis()
                    while (System.currentTimeMillis() - start < 3000) {
                        for (int j = 0; j < 100; j++) {
                            NCube cube = NCubeManager.getCube(appId, "MathController")

                            def input = [:]
                            input.env = "a"
                            input.x = 5
                            input.method = 'square'

                            assertEquals(25, cube.getCell(input))

                            input.method = 'factorial'
                            assertEquals(120, cube.getCell(input))

                            input.env = "b"
                            input.x = 6
                            input.method = 'square'
                            assertEquals(6, cube.getCell(input))
                            input.method = 'factorial'
                            assertEquals(6, cube.getCell(input))
                        }
                    }
                }

        def clearCache = {
            long start = System.currentTimeMillis()
            while (System.currentTimeMillis() - start < 3000) {
                NCubeManager.clearCache(appId);
            }
        }


        Thread[] threads = new Thread[16]

        for (int i = 0; i < 16; i++)
        {
            threads[i] = new Thread(run);
            threads[i].name = 'NCubeConcurrencyTest' + i
            threads[i].daemon = true
        }

        Thread clear = new Thread(clearCache);
        clear.name = "ClearCache";
        clear.daemon = true;

        // Start all at the same time (more concurrent that starting them during construction)
        for (int i = 0; i < 16; i++)
        {
            threads[i].start()
        }
        clear.start();

        for (int i = 0; i < 16; i++)
        {
            try
            {
                threads[i].join()
            }
            catch (InterruptedException ignored)
            { }
        }
        clear.join();
    }
}
