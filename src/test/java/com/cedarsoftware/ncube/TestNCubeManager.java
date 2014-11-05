package com.cedarsoftware.ncube;

import com.cedarsoftware.util.io.JsonWriter;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;

/**
 * NCubeManager Tests
 *
 * @author John DeRegnaucourt (jdereg@gmail.com)
 *         <br/>
 *         Copyright (c) Cedar Software LLC
 *         <br/><br/>
 *         Licensed under the Apache License, Version 2.0 (the "License");
 *         you may not use this file except in compliance with the License.
 *         You may obtain a copy of the License at
 *         <br/><br/>
 *         http://www.apache.org/licenses/LICENSE-2.0
 *         <br/><br/>
 *         Unless required by applicable law or agreed to in writing, software
 *         distributed under the License is distributed on an "AS IS" BASIS,
 *         WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *         See the License for the specific language governing permissions and
 *         limitations under the License.
 */
public class TestNCubeManager
{

    private static int test_db = TestingDatabaseHelper.HSQLDB;            // CHANGE to suit test needs (should be HSQLDB for normal JUnit testing)
    private TestingDatabaseManager _manager;

    static final String APP_ID = "ncube.test";

    private ApplicationID defaultSnapshotApp = new ApplicationID(ApplicationID.DEFAULT_TENANT, APP_ID, "1.0.0", ReleaseStatus.SNAPSHOT.name());

    @BeforeClass
    public static void init() throws Exception
    {
        TestNCube.initialize();
        NCubeManager.setNCubePersister(TestingDatabaseHelper.getPersister(test_db));
    }


    @Before
    public void setUp() throws Exception
    {
        _manager = TestingDatabaseHelper.getTestingDatabaseManager(test_db);
        _manager.setUp();
    }

    @After
    public void tearDown() throws Exception
    {
        _manager.tearDown();
        _manager = null;
        initManager();
    }


    public static void initManager() throws Exception
    {
        TestNCube.tearDown();
    }


    private NCube createCube() throws Exception
    {
        NCube<Double> ncube = TestNCube.getTestNCube2D(true);

        Map coord = new HashMap();
        coord.put("gender", "male");
        coord.put("age", "47");
        ncube.setCell(1.0, coord);

        coord.put("gender", "female");
        ncube.setCell(1.1, coord);

        coord.put("age", 16);
        ncube.setCell(1.5, coord);

        coord.put("gender", "male");
        ncube.setCell(1.8, coord);

        String version = "0.1.0";

        NCubeManager.createCube(defaultSnapshotApp, ncube);
        NCubeManager.updateTestData(defaultSnapshotApp, ncube.getName(), JsonWriter.objectToJson(coord));
        NCubeManager.updateNotes(defaultSnapshotApp, ncube.getName(), "notes follow");
        return ncube;
    }

    @Test
    public void testLoadCubes() throws Exception
    {
        NCube<Double> ncube = TestNCube.getTestNCube2D(true);

        Map coord = new HashMap();
        coord.put("gender", "male");
        coord.put("age", "47");
        ncube.setCell(1.0, coord);

        coord.put("gender", "female");
        ncube.setCell(1.1, coord);

        coord.put("age", 16);
        ncube.setCell(1.5, coord);

        coord.put("gender", "male");
        ncube.setCell(1.8, coord);

        String version = "0.1.0";
        String name1 = ncube.getName();

        ApplicationID appId = new ApplicationID(ApplicationID.DEFAULT_TENANT, APP_ID, version, ReleaseStatus.SNAPSHOT.name());
        NCubeManager.createCube(appId, ncube);
        NCubeManager.updateTestData(appId, ncube.getName(), JsonWriter.objectToJson(coord));
        NCubeManager.updateNotes(appId, ncube.getName(), "notes follow");

        assertTrue(NCubeManager.doesCubeExist(appId, name1));

        ncube = TestNCube.getTestNCube3D_Boolean();
        String name2 = ncube.getName();
        NCubeManager.createCube(appId, ncube);

        NCubeManager.clearCubeList(appId);
        NCubeManager.loadCubes(appId);

        NCube ncube1 = NCubeManager.getCube(name1, appId);
        NCube ncube2 = NCubeManager.getCube(name2, appId);
        assertNotNull(ncube1);
        assertNotNull(ncube2);
        assertEquals(name1, ncube1.getName());
        assertEquals(name2, ncube2.getName());
        NCubeManager.clearCubeList(appId);
        assertNull(NCubeManager.getCube(name1, appId));
        assertNull(NCubeManager.getCube(name2, appId));

        NCubeManager.deleteCube(appId, name1, true);
        NCubeManager.deleteCube(appId, name2, true);
        assertFalse(NCubeManager.doesCubeExist(appId, name1));
        assertFalse(NCubeManager.doesCubeExist(appId, name2));
    }

    @Test
    public void testDoesCubeExistInvalidId() {
        try
        {
            NCubeManager.doesCubeExist(defaultSnapshotApp, null);
            fail();
        } catch(IllegalArgumentException e) {
            assertEquals("n-cube name cannot be null or empty", e.getMessage());
        }

    }

    @Test
    public void testDoesCubeExistInvalidName() {
        try
        {
            NCubeManager.doesCubeExist(null, "foo");
            fail();
        }
        catch(IllegalArgumentException e)
        {
            assertTrue(e.getMessage().contains("cannot"));
            assertTrue(e.getMessage().contains("null"));
        }

    }
    //This exception is impossible to hit without mocking since we prohibit you on createCube() from
    //adding in a second duplicate cube with all the same parameters.
    @Test
    public void testGetReferencedCubesThatLoadsTwoCubes() throws Exception {
        try
        {
            Set<String> set = new HashSet<>();
            NCubeManager.getReferencedCubeNames(defaultSnapshotApp, "AnyCube", set);
            fail();
        } catch(IllegalArgumentException e) {
            assertNull(e.getCause());
        }
    }


    @Test
    public void testRenameWithMatchingNames() throws Exception {
        Connection c = mock(Connection.class);
        try
        {
            NCubeManager.renameCube(defaultSnapshotApp, "foo", "foo");
            fail();
        } catch(IllegalArgumentException e) {
            assertNull(e.getCause());
        }
    }

    @Test
    public void testBadCommandCellCommandWithJdbc() throws Exception
    {
        //setNCubePersister();
        NCube<Object> continentCounty = new NCube<>("test.ContinentCountries");
        continentCounty.setApplicationID(defaultSnapshotApp);
        NCubeManager.addCube(continentCounty, defaultSnapshotApp);
        continentCounty.addAxis(TestNCube.getContinentAxis());
        Axis countries = new Axis("Country", AxisType.DISCRETE, AxisValueType.STRING, true);
        countries.addColumn("Canada");
        countries.addColumn("USA");
        countries.addColumn("Mexico");
        continentCounty.addAxis(countries);

        NCube<Object> canada = new NCube<>("test.Provinces");
        canada.setApplicationID(defaultSnapshotApp);
        NCubeManager.addCube(canada, defaultSnapshotApp);
        canada.addAxis(TestNCube.getProvincesAxis());

        NCube<Object> usa = new NCube<>("test.States");
        usa.setApplicationID(defaultSnapshotApp);
        NCubeManager.addCube(usa, defaultSnapshotApp);
        usa.addAxis(TestNCube.getStatesAxis());

        Map coord1 = new HashMap();
        coord1.put("Continent", "North America");
        coord1.put("Country", "USA");
        coord1.put("State", "OH");

        Map coord2 = new HashMap();
        coord2.put("Continent", "North America");
        coord2.put("Country", "Canada");
        coord2.put("Province", "Quebec");

        continentCounty.setCell(new GroovyExpression("@test.States([:])", null), coord1);
        continentCounty.setCell(new GroovyExpression("$test.Provinces(crunch)", null), coord2);

        usa.setCell(1.0, coord1);
        canada.setCell(0.78, coord2);

        assertTrue((Double) continentCounty.getCell(coord1) == 1.0);

        try
        {
            assertTrue((Double) continentCounty.getCell(coord2) == 0.78);
            fail("should throw exception");
        }
        catch (Exception expected)
        {
        }

        NCubeManager.createCube(defaultSnapshotApp, continentCounty);
        NCubeManager.createCube(defaultSnapshotApp, usa);
        NCubeManager.createCube(defaultSnapshotApp, canada);

        assertTrue(NCubeManager.getCachedNCubes(defaultSnapshotApp).size() == 3);
        initManager();
        NCubeManager.loadCubes(defaultSnapshotApp);
        NCube test = NCubeManager.getCube("test.ContinentCountries", defaultSnapshotApp);
        assertTrue((Double) test.getCell(coord1) == 1.0);

        NCubeManager.deleteCube(defaultSnapshotApp, "test.ContinentCountries", false);
        NCubeManager.deleteCube(defaultSnapshotApp, "test.States", false);
        NCubeManager.deleteCube(defaultSnapshotApp, "test.Provinces", false);
        assertTrue(NCubeManager.getCachedNCubes(defaultSnapshotApp).size() == 0);
    }



    @Test
    public void testGetReferencedCubeNames() throws Exception
    {
        NCube n1 = NCubeManager.getNCubeFromResource("template1.json");
        NCube n2 = NCubeManager.getNCubeFromResource("template2.json");

        String ver = "1.1.1";
        NCubeManager.createCube(defaultSnapshotApp, n1);
        NCubeManager.createCube(defaultSnapshotApp, n2);

        Set refs = new TreeSet();
        NCubeManager.getReferencedCubeNames(defaultSnapshotApp, n1.getName(), refs);

        assertEquals(1, refs.size());
        assertTrue(refs.contains("Template2Cube"));

        refs.clear();
        NCubeManager.getReferencedCubeNames(defaultSnapshotApp, n2.getName(), refs);
        assertEquals(1, refs.size());
        assertTrue(refs.contains("Template1Cube"));

        assertTrue(NCubeManager.deleteCube(defaultSnapshotApp, n1.getName(), true));
        assertTrue(NCubeManager.deleteCube(defaultSnapshotApp, n2.getName(), true));

        try
        {
            NCubeManager.getReferencedCubeNames(defaultSnapshotApp, n2.getName(), null);
            fail();
        } catch (IllegalArgumentException e) {

        }
    }

    @Test
    public void testDuplicateNCube() throws Exception
    {
        NCube n1 = NCubeManager.getNCubeFromResource("stringIds.json");
        String ver = "1.1.1";
        NCubeManager.createCube(defaultSnapshotApp, n1);
        ApplicationID newId = new ApplicationID(ApplicationID.DEFAULT_TENANT, APP_ID, "1.1.2", ReleaseStatus.SNAPSHOT.name());

        NCubeManager.duplicate(defaultSnapshotApp, newId, n1.getName(), n1.getName());
        NCube n2 = NCubeManager.getCube(n1.getName(), defaultSnapshotApp);

        assertTrue(NCubeManager.deleteCube(defaultSnapshotApp, n1.getName(), true));
        assertTrue(NCubeManager.deleteCube(newId, n2.getName(), true));
        assertTrue(n1.equals(n2));
    }


    @Test
    public void testGetAppNames() throws Exception
    {
        NCube n1 = NCubeManager.getNCubeFromResource("stringIds.json");
        String version = "1.0.0";
        NCubeManager.createCube(defaultSnapshotApp, n1);

        Object[] names = NCubeManager.getAppNames();
        boolean foundName = false;
        for (Object name : names)
        {
            if ("ncube.test".equals(name))
            {
                foundName = true;
                break;
            }
        }

        Object[] vers = NCubeManager.getAppVersions(defaultSnapshotApp);
        boolean foundVer = false;
        for (Object ver : vers)
        {
            if (version.equals(ver))
            {
                foundVer = true;
                break;
            }
        }

        assertTrue(NCubeManager.deleteCube(defaultSnapshotApp, n1.getName(), true));
        assertTrue(foundName);
        assertTrue(foundVer);
    }


    @Test
    public void testChangeVersionValue() throws Exception {
        NCube n1 = NCubeManager.getNCubeFromResource("stringIds.json");
        ApplicationID newId = defaultSnapshotApp.createNewSnapshotId("1.1.20");

        assertNull(NCubeManager.getCube("idTest", defaultSnapshotApp));
        assertNull(NCubeManager.getCube("idTest", newId));
        NCubeManager.createCube(defaultSnapshotApp, n1);


        assertNotNull(NCubeManager.getCube("idTest", defaultSnapshotApp));
        assertNull(NCubeManager.getCube("idTest", newId));
        NCubeManager.changeVersionValue(defaultSnapshotApp, "1.1.20");

        //  When we remove cubes from the cache on renamed this test will fail
        assertNotNull(NCubeManager.getCube("idTest", defaultSnapshotApp));
        assertNotNull(NCubeManager.getCube("idTest", newId));

        NCube n2 = NCubeManager.getCube("idTest", newId);
        assertEquals(n1, n2);

        assertTrue(NCubeManager.deleteCube(newId, n1.getName(), true));
    }



    @Test
    public void testGetNCubes() throws Exception
    {
        NCube ncube1 = TestNCube.getTestNCube3D_Boolean();
        NCube ncube2 = TestNCube.getTestNCube2D(true);

        String version = "0.1.1";
        NCubeManager.createCube(defaultSnapshotApp, ncube1);
        NCubeManager.createCube(defaultSnapshotApp, ncube2);

        Object[] cubeList = NCubeManager.getNCubes(defaultSnapshotApp, "test.%");

        assertTrue(cubeList != null);
        assertTrue(cubeList.length == 2);

        assertTrue(ncube1.getNumDimensions() == 3);
        assertTrue(ncube2.getNumDimensions() == 2);

        ncube1.deleteAxis("bu");
        NCubeManager.updateCube(defaultSnapshotApp, ncube1);
        NCube cube1 = NCubeManager.getCube("test.ValidTrailorConfigs", defaultSnapshotApp);
        assertTrue(cube1.getNumDimensions() == 2);    // used to be 3

        assertTrue(2 == NCubeManager.releaseCubes(defaultSnapshotApp));

        // After the line below, there should be 4 test cubes in the database (2 @ version 0.1.1 and 2 @ version 0.2.0)
        NCubeManager.createSnapshotCubes(defaultSnapshotApp, "0.2.0");

        ApplicationID newId = defaultSnapshotApp.createNewSnapshotId("0.2.0");

        String notes1 = NCubeManager.getNotes(defaultSnapshotApp, "test.ValidTrailorConfigs");
        String notes2 = NCubeManager.getNotes(newId, "test.ValidTrailorConfigs");

        NCubeManager.updateNotes(defaultSnapshotApp, "test.ValidTrailorConfigs", null);
        notes1 = NCubeManager.getNotes(defaultSnapshotApp, "test.ValidTrailorConfigs");
        assertTrue("".equals(notes1));

        NCubeManager.updateNotes(defaultSnapshotApp, "test.ValidTrailorConfigs", "Trailer Config Notes");
        notes1 = NCubeManager.getNotes(defaultSnapshotApp, "test.ValidTrailorConfigs");
        assertTrue("Trailer Config Notes".equals(notes1));

        NCubeManager.updateTestData(newId, "test.ValidTrailorConfigs", null);
        String testData = NCubeManager.getTestData(newId, "test.ValidTrailorConfigs");
        assertTrue("".equals(testData));

        NCubeManager.updateTestData(newId, "test.ValidTrailorConfigs", "This is JSON data");
        testData = NCubeManager.getTestData(newId, "test.ValidTrailorConfigs");
        assertTrue("This is JSON data".equals(testData));

        // Verify that you cannot delete a RELEASE ncube
        assertFalse(NCubeManager.deleteCube(defaultSnapshotApp, ncube1.getName(), false));
        assertFalse(NCubeManager.deleteCube(defaultSnapshotApp, ncube2.getName(), false));

        // Delete ncubes using 'true' to allow the test to delete a released ncube.
        assertTrue(NCubeManager.deleteCube(defaultSnapshotApp, ncube1.getName(), true));
        assertTrue(NCubeManager.deleteCube(defaultSnapshotApp, ncube2.getName(), true));

        // Delete new SNAPSHOT cubes
        assertTrue(NCubeManager.deleteCube(newId, ncube1.getName(),  false));
        assertTrue(NCubeManager.deleteCube(newId, ncube2.getName(), false));

        // Ensure that all test ncubes are deleted
        cubeList = NCubeManager.getNCubes(defaultSnapshotApp, "test.%");
        assertTrue(cubeList.length == 0);
    }


    @Test
    public void testRenameNCube() throws Exception
    {
        NCube ncube1 = TestNCube.getTestNCube3D_Boolean();
        NCube ncube2 = TestNCube.getTestNCube2D(true);

        NCubeManager.createCube(defaultSnapshotApp, ncube1);
        NCubeManager.createCube(defaultSnapshotApp, ncube2);

        NCubeManager.renameCube(defaultSnapshotApp, ncube1.getName(), "test.Floppy");

        Object[] cubeList = NCubeManager.getNCubes(defaultSnapshotApp, "test.%");

        assertTrue(cubeList.length == 2);

        NCubeInfoDto nc1 = (NCubeInfoDto) cubeList[0];
        NCubeInfoDto nc2 = (NCubeInfoDto) cubeList[1];

        assertTrue(nc1.name.equals("test.Floppy") || nc2.name.equals("test.Floppy"));
        assertFalse(nc1.name.equals("test.Floppy") && nc2.name.equals("test.Floppy"));

        assertTrue(NCubeManager.deleteCube(defaultSnapshotApp, "test.Floppy", true));
        assertTrue(NCubeManager.deleteCube(defaultSnapshotApp, ncube2.getName(),true));
    }


    @Test
    public void testNCubeManagerGetCubes() throws Exception
    {
        NCube ncube1 = TestNCube.getTestNCube3D_Boolean();
        NCube ncube2 = TestNCube.getTestNCube2D(true);

        NCubeManager.createCube(defaultSnapshotApp, ncube1);
        NCubeManager.createCube(defaultSnapshotApp, ncube2);

        // This proves that null is turned into '%' (no exception thrown)
        Object[] cubeList = NCubeManager.getNCubes(defaultSnapshotApp, null);

        assertEquals(2, cubeList.length);
    }


    @Test
    public void testNCubeManagerUpdateCube() throws Exception
    {
        try
        {
            NCubeManager.updateCube(defaultSnapshotApp, null);
            fail("should not make it here");
        }
        catch (Exception e)
        {
            assertTrue(e instanceof IllegalArgumentException);
        }

        NCube testCube = TestNCube.getTestNCube2D(false);
        try
        {
            ApplicationID id = new ApplicationID(ApplicationID.DEFAULT_TENANT, "DASHBOARD", ApplicationID.DEFAULT_VERSION, ReleaseStatus.SNAPSHOT.name());
            NCubeManager.updateCube(id, testCube);
            fail("should not make it here");
        }
        catch (IllegalStateException e)
        {
            assertEquals("Only one (1) row should be updated.", e.getMessage());
        }
    }

    @Test
    public void testNCubeManagerCreateCubes() throws Exception
    {
        ApplicationID id = new ApplicationID(ApplicationID.DEFAULT_TENANT, "DASHBOARD", ApplicationID.DEFAULT_VERSION, ReleaseStatus.SNAPSHOT.name());
        try
        {
            NCubeManager.createCube(id, null);
            fail("should not make it here");
        }
        catch (IllegalArgumentException e)
        {
            assertEquals("NCube cannot be null when creating a new n-cube", e.getMessage());
        }

        NCube ncube1 = createCube();
        try
        {
            createCube();
            fail("Should not make it here");
        }
        catch (IllegalStateException e)
        {
            assertTrue(e.getMessage().contains("Cube already exists"));
        }

        NCubeManager.deleteCube(defaultSnapshotApp, ncube1.getName(), true);
    }


    @Test
    public void testNCubeManagerCreateSnapshots() throws Exception
    {
        try
        {
            NCubeManager.createSnapshotCubes(defaultSnapshotApp, "1.0.0");
            fail("versions are not allowed to match");
        }
        catch (IllegalArgumentException ignore)
        {
            assertTrue(ignore.getMessage().contains("cannot be the same as the RELEASE version."));
        }

        try
        {
            NCube ncube2 = createCube();
            NCubeManager.releaseCubes(defaultSnapshotApp);
            NCubeManager.createSnapshotCubes(defaultSnapshotApp, "0.1.1");
            NCubeManager.createSnapshotCubes(defaultSnapshotApp, "0.1.1");
            fail("should not make it here");
        }
        catch (IllegalStateException ignore)
        {
        }
    }


    @Test
    public void testNCubeManagerDelete() throws Exception
    {
        ApplicationID id = new ApplicationID(ApplicationID.DEFAULT_TENANT, "DASHBOARD", "0.1.0", ReleaseStatus.SNAPSHOT.name());
        assertFalse(NCubeManager.deleteCube(id, "DashboardRoles", true));
    }

    @Test
    public void testNotes() throws Exception
    {
        //ApplicationID id = new ApplicationID(ApplicationID.DEFAULT_TENANT, "DASHBOARD", "0.1.0", ReleaseStatus.SNAPSHOT.name());

        try
        {
            NCubeManager.getNotes(defaultSnapshotApp, "DashboardRoles");
            fail("should not make it here");
        }
        catch (IllegalArgumentException e)
        {
            assertTrue(e.getMessage().contains("No NCube matching passed in parameters"));
        }

        createCube();
        String notes = NCubeManager.getNotes(defaultSnapshotApp, "test.Age-Gender");
        assertNotNull(notes);
        assertTrue(notes.length() > 0);

        try
        {
            NCubeManager.updateNotes(defaultSnapshotApp, "test.funky", null);
            fail("should not make it here");
        }
        catch (IllegalStateException e)
        {
            assertTrue(e.getMessage().contains("No NCube matching"));
        }

        try
        {
            ApplicationID newId = defaultSnapshotApp.createNewSnapshotId("0.1.1");
            NCubeManager.getNotes(newId, "test.Age-Gender");
            fail("Should not make it here");
        }
        catch (IllegalArgumentException e)
        {
            assertTrue(e.getMessage().startsWith("No NCube matching"));
        }

        NCubeManager.deleteCube(defaultSnapshotApp, "test.Age-Gender", true);
    }


    @Test
    public void testNCubeManagerTestData() throws Exception
    {
        try
        {
            NCubeManager.getTestData(defaultSnapshotApp, "DashboardRoles");
            fail("should not make it here");
        }
        catch (Exception e)
        {
            assertTrue(e instanceof IllegalArgumentException);
        }

        createCube();
        String testData = NCubeManager.getTestData(defaultSnapshotApp, "test.Age-Gender");
        assertNotNull(testData);
        assertTrue(testData.length() > 0);

        try
        {
            NCubeManager.updateTestData(defaultSnapshotApp, "test.funky", null);
            fail("should not make it here");
        }
        catch (IllegalStateException e)
        {
            assertTrue(e.getMessage().startsWith("No NCube matching app"));
        }

        ApplicationID newId = defaultSnapshotApp.createNewSnapshotId("0.1.1");
        try
        {
            NCubeManager.getTestData(newId, "test.Age-Gender");
            fail("Should not make it here");
        }
        catch (Exception e)
        {
            assertTrue(e.getMessage().startsWith("No NCube matching passed"));
        }

        assertTrue(NCubeManager.deleteCube(defaultSnapshotApp, "test.Age-Gender"));
    }


    @Test
    public void testEmptyNCubeMetaProps() throws Exception
    {
        NCube ncube = createCube();
        String json = ncube.toFormattedJson();
        ncube = NCube.fromSimpleJson(json);
        assertTrue(ncube.getMetaProperties().size() == 1);  // sha1

        List<Axis> axes = ncube.getAxes();
        for (Axis axis : axes)
        {
            assertTrue(axis.getMetaProperties().size() == 0);

            for (Column column : axis.getColumns())
            {
                assertTrue(column.getMetaProperties().size() == 0);
            }
        }
        NCubeManager.deleteCube(defaultSnapshotApp, ncube.getName(), true);
    }

    @Test
    public void testBadUrlsAddedToClassLoader() throws Exception
    {
        String url = "htp://this wont work";
        List urls = new ArrayList();
        urls.add(url);
        try
        {
            ApplicationID appId = new ApplicationID(ApplicationID.DEFAULT_TENANT, ApplicationID.DEFAULT_APP, "2", ReleaseStatus.SNAPSHOT.name());
            NCubeManager.addBaseResourceUrls(urls, appId);
            fail("Should not make it here");
        }
        catch (Exception expected)
        { }
    }



    @Test
    public void testLoadCubesWithNullApplicationID() throws Exception
    {
        try
        {
            NCubeManager.loadCubes(null);
            fail();
        }
        catch(IllegalArgumentException e)
        {
            assertTrue(e.getMessage().contains("cannot"));
            assertTrue(e.getMessage().contains("null"));
        }
    }


    @Test(expected=RuntimeException.class)
    public void testGetNCubesFromResourceException() throws Exception
    {
        NCubeManager.getNCubesFromResource(null);
    }

}
