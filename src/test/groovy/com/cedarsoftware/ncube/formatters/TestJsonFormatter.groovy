package com.cedarsoftware.ncube.formatters

import com.cedarsoftware.ncube.ApplicationID
import com.cedarsoftware.ncube.NCube
import com.cedarsoftware.ncube.NCubeManager
import com.cedarsoftware.ncube.TestNCubeManager
import com.cedarsoftware.ncube.TestingDatabaseHelper
import com.cedarsoftware.ncube.TestingDatabaseManager
import com.cedarsoftware.util.IOUtilities
import org.junit.After
import org.junit.Before
import org.junit.Test

import static org.junit.Assert.assertEquals
import static org.junit.Assert.fail
import static org.mockito.Matchers.anyInt
import static org.mockito.Matchers.anyObject
import static org.mockito.Mockito.mock
import static org.mockito.Mockito.when

/**
 * @author John DeRegnaucourt (jdereg@gmail.com)
 *         <br/>
 *         Copyright (c) Cedar Software LLC
 *         <br/><br/>
 *         Licensed under the Apache License, Version 2.0 (the 'License');
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
class TestJsonFormatter
{
    public static String USER_ID = TestNCubeManager.USER_ID
    public static ApplicationID appId = new ApplicationID(ApplicationID.DEFAULT_TENANT, "clearCacheTest", ApplicationID.DEFAULT_VERSION, ApplicationID.DEFAULT_STATUS, ApplicationID.TEST_BRANCH)
    public static ApplicationID usedId = new ApplicationID(ApplicationID.DEFAULT_TENANT, "usedInvalidId", ApplicationID.DEFAULT_VERSION, ApplicationID.DEFAULT_STATUS, ApplicationID.TEST_BRANCH)

    private TestingDatabaseManager manager;

    @Before
    public void setup()
    {
        manager = TestingDatabaseHelper.testingDatabaseManager
        manager.setUp()
        NCubeManager.NCubePersister = TestingDatabaseHelper.persister
    }

    @After
    public void tearDown()
    {
        manager.tearDown()
        manager = null;
        NCubeManager.clearCache()
    }

    @Test
    void testJsonFormatter()
    {
        // when running a single test.
        //List<String> s = new ArrayList<String>()
        //s.add('urlContent.json')
        List<String> s = allTestFiles
        runAllTests(s)
    }

    @Test
    void testConvertArray()
    {

        NCube[] cubes = TestingDatabaseHelper.getCubesFromDisk('sys.classpath.tests.json', 'arrays.json');
        manager.addCubes(ApplicationID.testAppId, 'lol', cubes);

        NCube ncube = cubes[1];

        def coord = [Code:'longs']
        assertEquals 9223372036854775807L, ((Object[]) ncube.getCell(coord))[2]

        coord.Code = 'ints'
        assertEquals 2147483647, ((Object[]) ncube.getCell(coord))[2]

        coord.Code = 'bytes'
        assertEquals 127 as byte, ((Object[]) ncube.getCell(coord))[2]

        coord.Code = 'shorts'
        assertEquals 32767 as short, ((Object[]) ncube.getCell(coord))[2]

        coord.Code = 'booleans'
        assertEquals Boolean.TRUE, ((Object[]) ncube.getCell(coord))[2]
        assertEquals Boolean.FALSE, ((Object[]) ncube.getCell(coord))[3]

        coord.Code = 'floats'
        assertEquals 3.8f, ((Object[]) ncube.getCell(coord))[2], 0.00001d

        coord.Code = 'doubles'
        assertEquals 10.1d, ((Object[]) ncube.getCell(coord))[2], 0.00001d

        coord.Code = 'bigints'
        assertEquals 0g, ((Object[]) ncube.getCell(coord))[0]
        assertEquals 9223372036854775807g, ((Object[]) ncube.getCell(coord))[2]
        assertEquals 147573952589676410000g, ((Object[]) ncube.getCell(coord))[3]

        String s = ncube.toFormattedJson()
        ncube = NCube.fromSimpleJson s

        coord.Code = 'longs'
        assertEquals 9223372036854775807L, ((Object[]) ncube.getCell(coord))[2]

        coord.Code = 'ints'
        assertEquals 2147483647, ((Object[]) ncube.getCell(coord))[2]

        coord.Code = 'bytes'
        assertEquals 127 as byte, ((Object[]) ncube.getCell(coord))[2]

        coord.Code = 'shorts'
        assertEquals 32767 as short, ((Object[]) ncube.getCell(coord))[2]

        coord.Code = 'booleans'
        assertEquals Boolean.TRUE, ((Object[]) ncube.getCell(coord))[2]
        assertEquals Boolean.FALSE, ((Object[]) ncube.getCell(coord))[3]

        coord.Code = 'floats'
        assertEquals new Float(3.8), ((Object[]) ncube.getCell(coord))[2], 0.00001d

        coord.Code= 'doubles'
        assertEquals 10.1, ((Object[]) ncube.getCell(coord))[2], 0.00001d

        coord.Code = 'bigints'
        assertEquals new BigInteger('0'), ((Object[]) ncube.getCell(coord))[0]
        assertEquals new BigInteger('9223372036854775807'), ((Object[]) ncube.getCell(coord))[2]
        assertEquals new BigInteger('147573952589676410000'), ((Object[]) ncube.getCell(coord))[3]

        manager.removeBranches([ApplicationID.testAppId] as ApplicationID[]);
    }

    @Test
    void testInvalidNCube()
    {
        NCube ncube = new NCube(null)
        JsonFormatter formatter = new JsonFormatter()
        String json = formatter.format(ncube)
        assertEquals('{"ncube":null,"axes":[],"cells":[]}', json)
    }

    @Test
    void testNullValueGoingToAppend() {
        OutputStream stream = mock(OutputStream.class);
        when(stream.write(anyObject(), anyInt(), anyInt())).thenThrow(new IOException("foo error"));

        BufferedInputStream input = null;

        try
        {
            JsonFormatter formatter = new JsonFormatter(stream)
            formatter.append((String)null);
            fail();
        }
        catch (RuntimeException e)
        {
            assertEquals(NullPointerException.class, e.getCause().getClass());
        }
        finally
        {
            IOUtilities.close((Closeable)input);
        }
    }

    @Test
    void testNullCube()
    {
        try
        {
            new JsonFormatter().format(null)
            fail()
        }
        catch (IllegalArgumentException e)
        {
            assert e.message.contains('cannot be null')
        }
    }

    @Test
    void testCubeWithInvalidDefaultCell()
    {
        try
        {
            NCube cube = new NCube('foo')
            cube.defaultCellValue = new NCube('bar')
            new JsonFormatter().format(cube)
            fail()
        }
        catch (IllegalStateException e)
        {
            assertEquals IllegalArgumentException.class, e.cause.class
            assert e.message.contains('Unable to format NCube')
        }
    }

    @Test
    void testCubeWithInvalidDefaultCellArrayType()
    {
        try
        {
            NCube cube = new NCube('foo')
            cube.defaultCellValue = [] as Object[]
            new JsonFormatter().format cube
            fail 'should not make it here'
        }
        catch (IllegalStateException e)
        {
            assertEquals(IllegalArgumentException.class, e.cause.class)
            assert e.message.contains('Unable to format NCube')
            assert e.cause.message.contains('Cell cannot be an array')
            assert e.cause.message.contains('Use Groovy Expression')
        }
    }

    private static class TestFilenameFilter implements FilenameFilter
    {
        boolean accept(File dir, String name)
        {
            return name != null && name.endsWith('.json') &&
                    !(name.endsWith('idBasedCubeError.json') ||
                            name.endsWith('idBasedCubeError2.json') ||
                            name.endsWith('error.json') ||
                            name.endsWith('arrays.json') ||  /** won't have equivalency **/
                            name.endsWith('testCubeList.json'))   /** list of cubes **/
        }
    }

    public List<String> getAllTestFiles()
    {
        URL u = getClass().classLoader.getResource('')
        File dir = new File(u.file)
        File[] files = dir.listFiles(new TestFilenameFilter());
        List<String> names = new ArrayList<>(files.length)

        for (File f : files)
        {
            names.add f.name
        }
        return names;
    }

    public void runAllTests(List<String> strings)
    {
        for (String f : strings)
        {
            String original = NCubeManager.getResourceAsString(f)
            NCube ncube = NCube.fromSimpleJson(original)

            //long start = System.nanoTime()
            String s = ncube.toFormattedJson()
//            System.out.println(s)
            NCube res = NCube.fromSimpleJson(s)
            //long end = System.nanoTime()
            assertEquals(res, ncube)
            //long time = (end-start)/1000000;
            //if (time > 250) {
                //System.out.println(f + " " + time);
            //}
        }
    }
}