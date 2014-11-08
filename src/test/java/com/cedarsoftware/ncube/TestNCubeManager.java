package com.cedarsoftware.ncube;

import com.cedarsoftware.util.io.JsonWriter;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

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
    static final String APP_ID = "ncube.test";
    static final String USER_ID = "jdirt";
    public static ApplicationID defaultSnapshotApp = new ApplicationID(ApplicationID.DEFAULT_TENANT, APP_ID, "1.0.0", ReleaseStatus.SNAPSHOT.name());
    public static ApplicationID defaultFileApp = new ApplicationID(ApplicationID.DEFAULT_TENANT, ApplicationID.DEFAULT_APP, ApplicationID.DEFAULT_VERSION, ReleaseStatus.SNAPSHOT.name());

    @Before
    public void setUp() throws Exception
    {
        TestingDatabaseHelper.setupDatabase();
    }

    @After
    public void tearDown() throws Exception
    {
        TestingDatabaseHelper.tearDownDatabase();
    }


    private static NCube createCube() throws Exception
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

        NCubeManager.createCube(defaultSnapshotApp, ncube, USER_ID);
        NCubeManager.updateTestData(defaultSnapshotApp, ncube.getName(), JsonWriter.objectToJson(coord));
        NCubeManager.updateNotes(defaultSnapshotApp, ncube.getName(), "notes follow");
        return ncube;
    }

//TODO:  Add in sys.classpath tests
/*
    @Test
    public void testLoadCubesWithSysClassPath() {
        NCube<Double> ncube = TestNCube.getTestNCube2D(true);

        NCubeManager.createCube(defaultSnapshotApp, ncube);

    }
*/
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
        NCubeManager.createCube(appId, ncube, USER_ID);
        NCubeManager.updateTestData(appId, ncube.getName(), JsonWriter.objectToJson(coord));
        NCubeManager.updateNotes(appId, ncube.getName(), "notes follow");

        assertTrue(NCubeManager.doesCubeExist(appId, name1));

        ncube = TestNCube.getTestNCube3D_Boolean();
        String name2 = ncube.getName();
        NCubeManager.createCube(appId, ncube, USER_ID);

        NCubeManager.clearCache(appId);
        NCubeManager.getCubeRecordsFromDatabase(appId, "");

        NCube ncube1 = NCubeManager.getCube(appId, name1);
        NCube ncube2 = NCubeManager.getCube(appId, name2);
        assertNotNull(ncube1);
        assertNotNull(ncube2);
        assertEquals(name1, ncube1.getName());
        assertEquals(name2, ncube2.getName());
        assertTrue(NCubeManager.isCubeCached(appId, name1));
        assertTrue(NCubeManager.isCubeCached(appId, name2));
        NCubeManager.clearCache(appId);
        assertFalse(NCubeManager.isCubeCached(appId, name1));
        assertFalse(NCubeManager.isCubeCached(appId, name2));

        NCubeManager.deleteCube(appId, name1, true, USER_ID);
        NCubeManager.deleteCube(appId, name2, true, USER_ID);
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
    public void testRenameWithMatchingNames() throws Exception
    {
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
        NCubeManager.addCube(defaultSnapshotApp, continentCounty);
        continentCounty.addAxis(TestNCube.getContinentAxis());
        Axis countries = new Axis("Country", AxisType.DISCRETE, AxisValueType.STRING, true);
        countries.addColumn("Canada");
        countries.addColumn("USA");
        countries.addColumn("Mexico");
        continentCounty.addAxis(countries);

        NCube<Object> canada = new NCube<>("test.Provinces");
        canada.setApplicationID(defaultSnapshotApp);
        NCubeManager.addCube(defaultSnapshotApp, canada);
        canada.addAxis(TestNCube.getProvincesAxis());

        NCube<Object> usa = new NCube<>("test.States");
        usa.setApplicationID(defaultSnapshotApp);
        NCubeManager.addCube(defaultSnapshotApp, usa);
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
        catch (Exception ignored)
        { }

        NCubeManager.createCube(defaultSnapshotApp, continentCounty, USER_ID);
        NCubeManager.createCube(defaultSnapshotApp, usa, USER_ID);
        NCubeManager.createCube(defaultSnapshotApp, canada, USER_ID);

        assertTrue(NCubeManager.getCubeNames(defaultSnapshotApp).size() == 3);

        NCubeManager.clearCache();
        NCubeManager.getCubeRecordsFromDatabase(defaultSnapshotApp, "");
        NCube test = NCubeManager.getCube(defaultSnapshotApp, "test.ContinentCountries");
        assertTrue((Double) test.getCell(coord1) == 1.0);

        NCubeManager.deleteCube(defaultSnapshotApp, "test.ContinentCountries", false, USER_ID);
        NCubeManager.deleteCube(defaultSnapshotApp, "test.States", false, USER_ID);
        NCubeManager.deleteCube(defaultSnapshotApp, "test.Provinces", false, USER_ID);
        assertTrue(NCubeManager.getCubeNames(defaultSnapshotApp).size() == 0);
    }

    @Test
    public void testGetReferencedCubeNames() throws Exception
    {
        NCube n1 = NCubeManager.getNCubeFromResource("template1.json");
        NCube n2 = NCubeManager.getNCubeFromResource("template2.json");

        NCubeManager.createCube(defaultSnapshotApp, n1, USER_ID);
        NCubeManager.createCube(defaultSnapshotApp, n2, USER_ID);

        Set refs = new TreeSet();
        NCubeManager.getReferencedCubeNames(defaultSnapshotApp, n1.getName(), refs);

        assertEquals(1, refs.size());
        assertTrue(refs.contains("Template2Cube"));

        refs.clear();
        NCubeManager.getReferencedCubeNames(defaultSnapshotApp, n2.getName(), refs);
        assertEquals(1, refs.size());
        assertTrue(refs.contains("Template1Cube"));

        assertTrue(NCubeManager.deleteCube(defaultSnapshotApp, n1.getName(), true, USER_ID));
        assertTrue(NCubeManager.deleteCube(defaultSnapshotApp, n2.getName(), true, USER_ID));

        try
        {
            NCubeManager.getReferencedCubeNames(defaultSnapshotApp, n2.getName(), null);
            fail();
        }
        catch (IllegalArgumentException ignored)
        { }
    }

    @Test
    public void testDuplicateNCube() throws Exception
    {
        NCube n1 = NCubeManager.getNCubeFromResource("stringIds.json");
        String ver = "1.1.1";
        NCubeManager.createCube(defaultSnapshotApp, n1, USER_ID);
        ApplicationID newId = new ApplicationID(ApplicationID.DEFAULT_TENANT, APP_ID, "1.1.2", ReleaseStatus.SNAPSHOT.name());

        NCubeManager.duplicate(defaultSnapshotApp, newId, n1.getName(), n1.getName(), USER_ID);
        NCube n2 = NCubeManager.getCube(defaultSnapshotApp, n1.getName());

        assertTrue(NCubeManager.deleteCube(defaultSnapshotApp, n1.getName(), true, USER_ID));
        assertTrue(NCubeManager.deleteCube(newId, n2.getName(), true, USER_ID));
        assertTrue(n1.equals(n2));
    }


    @Test
    public void testGetAppNames() throws Exception
    {
        NCube n1 = NCubeManager.getNCubeFromResource("stringIds.json");
        NCubeManager.createCube(defaultSnapshotApp, n1, USER_ID);

        Object[] names = NCubeManager.getAppNames(defaultSnapshotApp.getTenant());
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
        String version = "1.0.0";
        for (Object ver : vers)
        {
            if (version.equals(ver))
            {
                foundVer = true;
                break;
            }
        }

        assertTrue(NCubeManager.deleteCube(defaultSnapshotApp, n1.getName(), true, USER_ID));
        assertTrue(foundName);
        assertTrue(foundVer);
    }


    @Test
    public void testChangeVersionValue() throws Exception
    {
        NCube n1 = NCubeManager.getNCubeFromResource("stringIds.json");
        ApplicationID newId = defaultSnapshotApp.createNewSnapshotId("1.1.20");

        assertNull(NCubeManager.getCube(defaultSnapshotApp, "idTest"));
        assertNull(NCubeManager.getCube(newId, "idTest"));
        NCubeManager.createCube(defaultSnapshotApp, n1, USER_ID);

        assertNotNull(NCubeManager.getCube(defaultSnapshotApp, "idTest"));
        assertNull(NCubeManager.getCube(newId, "idTest"));
        NCubeManager.changeVersionValue(defaultSnapshotApp, "1.1.20");

        assertNotNull(NCubeManager.getCube(newId, "idTest"));

        NCube n2 = NCubeManager.getCube(newId, "idTest");
        assertEquals(n1, n2);

        assertTrue(NCubeManager.deleteCube(newId, n1.getName(), true, USER_ID));
    }

    @Test
    public void testGetNCubes() throws Exception
    {
        NCube ncube1 = TestNCube.getTestNCube3D_Boolean();
        NCube ncube2 = TestNCube.getTestNCube2D(true);

        NCubeManager.createCube(defaultSnapshotApp, ncube1, USER_ID);
        NCubeManager.createCube(defaultSnapshotApp, ncube2, USER_ID);

        Object[] cubeList = NCubeManager.getCubeRecordsFromDatabase(defaultSnapshotApp, "test.%");

        assertTrue(cubeList != null);
        assertTrue(cubeList.length == 2);

        assertTrue(ncube1.getNumDimensions() == 3);
        assertTrue(ncube2.getNumDimensions() == 2);

        ncube1.deleteAxis("bu");
        NCubeManager.updateCube(defaultSnapshotApp, ncube1, USER_ID);
        NCube cube1 = NCubeManager.getCube(defaultSnapshotApp, "test.ValidTrailorConfigs");
        assertTrue(cube1.getNumDimensions() == 2);    // used to be 3

        assertTrue(2 == NCubeManager.releaseCubes(defaultSnapshotApp));

        // After the line below, there should be 4 test cubes in the database (2 @ version 0.1.1 and 2 @ version 0.2.0)
        NCubeManager.createSnapshotCubes(defaultSnapshotApp, "0.2.0");

        ApplicationID newId = defaultSnapshotApp.createNewSnapshotId("0.2.0");

        String notes1 = NCubeManager.getNotes(defaultSnapshotApp, "test.ValidTrailorConfigs");
        NCubeManager.getNotes(newId, "test.ValidTrailorConfigs");

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
        assertFalse(NCubeManager.deleteCube(defaultSnapshotApp, ncube1.getName(), false, USER_ID));
        assertFalse(NCubeManager.deleteCube(defaultSnapshotApp, ncube2.getName(), false, USER_ID));

        // Delete ncubes using 'true' to allow the test to delete a released ncube.
        assertTrue(NCubeManager.deleteCube(defaultSnapshotApp, ncube1.getName(), true, USER_ID));
        assertTrue(NCubeManager.deleteCube(defaultSnapshotApp, ncube2.getName(), true, USER_ID));

        // Delete new SNAPSHOT cubes
        assertTrue(NCubeManager.deleteCube(newId, ncube1.getName(),  false, USER_ID));
        assertTrue(NCubeManager.deleteCube(newId, ncube2.getName(), false, USER_ID));

        // Ensure that all test ncubes are deleted
        cubeList = NCubeManager.getCubeRecordsFromDatabase(defaultSnapshotApp, "test.%");
        assertTrue(cubeList.length == 0);
    }

    @Test
    public void testRenameNCube() throws Exception
    {
        NCube ncube1 = TestNCube.getTestNCube3D_Boolean();
        NCube ncube2 = TestNCube.getTestNCube2D(true);

        NCubeManager.createCube(defaultSnapshotApp, ncube1, USER_ID);
        NCubeManager.createCube(defaultSnapshotApp, ncube2, USER_ID);

        NCubeManager.renameCube(defaultSnapshotApp, ncube1.getName(), "test.Floppy");

        Object[] cubeList = NCubeManager.getCubeRecordsFromDatabase(defaultSnapshotApp, "test.%");

        assertTrue(cubeList.length == 2);

        NCubeInfoDto nc1 = (NCubeInfoDto) cubeList[0];
        NCubeInfoDto nc2 = (NCubeInfoDto) cubeList[1];

        assertTrue(nc1.name.equals("test.Floppy") || nc2.name.equals("test.Floppy"));
        assertFalse(nc1.name.equals("test.Floppy") && nc2.name.equals("test.Floppy"));

        assertTrue(NCubeManager.deleteCube(defaultSnapshotApp, "test.Floppy", true, USER_ID));
        assertTrue(NCubeManager.deleteCube(defaultSnapshotApp, ncube2.getName(),true, USER_ID));
    }

    @Test
    public void testNCubeManagerGetCubes() throws Exception
    {
        NCube ncube1 = TestNCube.getTestNCube3D_Boolean();
        NCube ncube2 = TestNCube.getTestNCube2D(true);

        NCubeManager.createCube(defaultSnapshotApp, ncube1, USER_ID);
        NCubeManager.createCube(defaultSnapshotApp, ncube2, USER_ID);

        // This proves that null is turned into '%' (no exception thrown)
        Object[] cubeList = NCubeManager.getCubeRecordsFromDatabase(defaultSnapshotApp, null);

        assertEquals(2, cubeList.length);
    }


    @Test
    public void testNCubeManagerUpdateCube() throws Exception
    {
        try
        {
            NCubeManager.updateCube(defaultSnapshotApp, null, USER_ID);
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
            NCubeManager.updateCube(id, testCube, USER_ID);
            fail("should not make it here");
        }
        catch (IllegalStateException e)
        {
            assertTrue(e.getMessage().contains("error updating"));
            assertTrue(e.getMessage().contains("rows updated"));
        }
    }

    @Test
    public void testNCubeManagerCreateCubes() throws Exception
    {
        ApplicationID id = new ApplicationID(ApplicationID.DEFAULT_TENANT, "DASHBOARD", ApplicationID.DEFAULT_VERSION, ReleaseStatus.SNAPSHOT.name());
        try
        {
            NCubeManager.createCube(id, null, USER_ID);
            fail("should not make it here");
        }
        catch (IllegalArgumentException e)
        {
            assertTrue(e.getMessage().contains("cannot be null"));
        }

        NCube ncube1 = createCube();
        try
        {
            createCube();
            fail("Should not make it here");
        }
        catch (IllegalStateException e)
        {
            assertTrue(e.getMessage().contains("ube"));
            assertTrue(e.getMessage().contains("already exists"));
        }

        NCubeManager.deleteCube(defaultSnapshotApp, ncube1.getName(), true, USER_ID);
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
            createCube();
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
        assertFalse(NCubeManager.deleteCube(id, "DashboardRoles", true, USER_ID));
    }

    @Test
    public void testNotes() throws Exception
    {
        try
        {
            NCubeManager.getNotes(defaultSnapshotApp, "DashboardRoles");
            fail("should not make it here");
        }
        catch (IllegalArgumentException e)
        {
            assertTrue(e.getMessage().contains("not"));
            assertTrue(e.getMessage().contains("fetch"));
            assertTrue(e.getMessage().contains("notes"));
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
            assertTrue(e.getMessage().contains("no cube match"));
        }

        try
        {
            ApplicationID newId = defaultSnapshotApp.createNewSnapshotId("0.1.1");
            NCubeManager.getNotes(newId, "test.Age-Gender");
            fail("Should not make it here");
        }
        catch (IllegalArgumentException e)
        {
            assertTrue(e.getMessage().contains("not"));
            assertTrue(e.getMessage().contains("fetch"));
            assertTrue(e.getMessage().contains("notes"));
        }

        NCubeManager.deleteCube(defaultSnapshotApp, "test.Age-Gender", true, USER_ID);
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
            assertTrue(e.getMessage().contains("no"));
            assertTrue(e.getMessage().contains("cube"));
            assertTrue(e.getMessage().contains("match"));
        }

        ApplicationID newId = defaultSnapshotApp.createNewSnapshotId("0.1.1");
        try
        {
            NCubeManager.getTestData(newId, "test.Age-Gender");
            fail("Should not make it here");
        }
        catch (Exception e)
        {
            assertTrue(e.getMessage().contains("no"));
            assertTrue(e.getMessage().contains("cube"));
            assertTrue(e.getMessage().contains("match"));
        }

        assertTrue(NCubeManager.deleteCube(defaultSnapshotApp, "test.Age-Gender", USER_ID));
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
        NCubeManager.deleteCube(defaultSnapshotApp, ncube.getName(), true, USER_ID);
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
            NCubeManager.addBaseResourceUrls(appId, urls);
            fail("Should not make it here");
        }
        catch (Exception ignored)
        { }
    }

    @Test
    public void testLoadCubesWithNullApplicationID() throws Exception
    {
        try
        {
            // This API is now package friendly and only to be used by tests or NCubeManager implementation work.
            NCubeManager.getCubeRecordsFromDatabase(null, "");
            fail();
        }
        catch(Exception ignored)
        { }
    }

    @Test(expected=RuntimeException.class)
    public void testGetNCubesFromResourceException() throws Exception
    {
        NCubeManager.getNCubesFromResource(null);
    }

}
