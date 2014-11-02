package com.cedarsoftware.ncube;

import com.cedarsoftware.util.io.JsonReader;
import com.cedarsoftware.util.io.JsonWriter;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * ApplicationID Tests
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
public class TestApplicationID
{
    @Test
    public void testBadConstructorArgs()
    {
        try
        {
            new ApplicationID(null, "Inventory", "99.99.99", ReleaseStatus.SNAPSHOT.name());
            fail("should not make it here");
        }
        catch (Exception ignored)
        {
        }

        try
        {
            new ApplicationID("Macy's", null, "9.9.9", ReleaseStatus.SNAPSHOT.name());
            fail("should not make it here");
        }
        catch (Exception ignored)
        {
        }

        try
        {
            new ApplicationID("Macy's", "Catalog", null, ReleaseStatus.SNAPSHOT.name());
            fail("should not make it here");
        }
        catch (Exception ignored)
        {
        }

        try
        {
            new ApplicationID("Macy's", "Catalog", "1.2.3", null);
            fail("should not make it here");
        }
        catch (Exception ignored)
        {
        }

        try
        {
            new ApplicationID("Macy's", "Catalog", "1.2.a", ReleaseStatus.SNAPSHOT.name());
            fail("should not make it here");
        }
        catch (Exception ignored)
        {
            System.out.println(ignored);
        }

        try
        {
            new ApplicationID("Macy's", "Catalog", "1.2.3.4", ReleaseStatus.SNAPSHOT.name());
            fail("should not make it here");
        }
        catch (Exception ignored)
        {
        }

        try
        {
            new ApplicationID("Macy's", "Catalog", "1.2", ReleaseStatus.SNAPSHOT.name());
            fail("should not make it here");
        }
        catch (Exception ignored)
        {
        }

        try
        {
            new ApplicationID("Macy's", "Catalog", "1.-2.3", ReleaseStatus.SNAPSHOT.name());
            fail("should not make it here");
        }
        catch (Exception ignored)
        {
        }

        try
        {
            new ApplicationID("Macy's", "Catalog", "1.2.3", "CRAPSHOT");
            fail("should not make it here");
        }
        catch (Exception ignored)
        {
        }
    }

    @Test
    public void testApplicationIDConstructorAndGetters()
    {
        ApplicationID appId = new ApplicationID("Sears", "Inventory", "1.0.0", ReleaseStatus.SNAPSHOT.name());
        assertEquals("Sears", appId.getAccount());
        assertEquals("Inventory", appId.getApp());
        assertEquals("1.0.0", appId.getVersion());
        assertEquals("SNAPSHOT", appId.getStatus());
    }

    @Test
    public void testAppKey()
    {
        ApplicationID appId = new ApplicationID("Sears", "Inventory", "1.0.0", ReleaseStatus.SNAPSHOT.name());
        assertEquals("sears/inventory/1.0.0/", appId.getAppStr(""));
        assertEquals("sears/inventory/1.0.0/", appId.toString());
    }

    @Test
    public void testEqualsAndHashcode()
    {
        ApplicationID appId1 = new ApplicationID("Sears", "Inventory", "1.0.0", ReleaseStatus.SNAPSHOT.name());
        ApplicationID appId2 = new ApplicationID("Sears", "Inventory", "1.0.0", ReleaseStatus.RELEASE.name());

        assertEquals(appId1.getAppStr(""), appId2.getAppStr(""));
        assertNotEquals(appId1, appId2);
        assertNotEquals(appId1.hashCode(), appId2.hashCode());

        appId1 = new ApplicationID("Sears", "Inventory", "1.0.0", ReleaseStatus.SNAPSHOT.name());
        appId2 = new ApplicationID("Sears", "Inventory", "1.0.1", ReleaseStatus.SNAPSHOT.name());

        assertNotEquals(appId1.getAppStr(""), appId2.getAppStr(""));
        assertNotEquals(appId1, appId2);
        assertNotEquals(appId1.hashCode(), appId2.hashCode());

        appId1 = new ApplicationID("Sears", "Inventory", "1.0.0", ReleaseStatus.SNAPSHOT.name());
        appId2 = new ApplicationID("Sears", "Inventori", "1.0.0", ReleaseStatus.SNAPSHOT.name());

        assertNotEquals(appId1.getAppStr(""), appId2.getAppStr(""));
        assertNotEquals(appId1, appId2);
        assertNotEquals(appId1.hashCode(), appId2.hashCode());

        appId1 = new ApplicationID("Sears", "Inventory", "1.0.0", ReleaseStatus.SNAPSHOT.name());
        appId2 = new ApplicationID("Pears", "Inventory", "1.0.0", ReleaseStatus.SNAPSHOT.name());

        assertNotEquals(appId1.getAppStr(""), appId2.getAppStr(""));
        assertNotEquals(appId1, appId2);
        assertNotEquals(appId1.hashCode(), appId2.hashCode());

        assertNotEquals(appId1, "Hey");
        assertEquals(appId1, appId1);

        appId1 = new ApplicationID("Lowes", "Inventory", "1.2.3", ReleaseStatus.SNAPSHOT.name());
        appId2 = new ApplicationID("Lowes", "Inventory", "1.2.3", ReleaseStatus.SNAPSHOT.name());
        assertEquals(appId1, appId2);
    }

    // Want to know if this assumption ever changes
    @Test
    public void testAppStrSameAsToString()
    {
        ApplicationID appId = new ApplicationID("Sears", "Inventory", "1.0.0", ReleaseStatus.SNAPSHOT.name());
        assertEquals(appId.toString(), appId.getAppStr(""));
    }

    // Want to know if this assumption ever changes
    @Test
    public void testApplicationIDSerialize() throws Exception
    {
        ApplicationID appId1 = new ApplicationID("Sears", "Inventory", "1.0.0", ReleaseStatus.SNAPSHOT.name());
        String json = JsonWriter.objectToJson(appId1);
        ApplicationID appId2 = (ApplicationID) JsonReader.jsonToJava(json);
        assertEquals(appId1, appId2);
    }

    @Test
    public void testIsSnapshotOrRelease() {
        ApplicationID snapshot = new ApplicationID("Sears", "Inventory", "1.0.0", ReleaseStatus.SNAPSHOT.name());
        assertTrue(snapshot.isSnapshot());
        assertFalse(snapshot.isRelease());
        ApplicationID releaseId = new ApplicationID("Sears", "Inventory", "1.0.0", ReleaseStatus.RELEASE.name());
        assertFalse(releaseId.isSnapshot());
        assertTrue(releaseId.isRelease());
    }

    @Test
    public void testValidateSnapshot() {
        ApplicationID snapshot = new ApplicationID("Sears", "Inventory", "1.0.0", ReleaseStatus.SNAPSHOT.name());
        snapshot.validateIsSnapshot();

        ApplicationID releaseId = new ApplicationID("Sears", "Inventory", "1.0.0", ReleaseStatus.RELEASE.name());
        try
        {
            releaseId.validateIsSnapshot();
        } catch (IllegalStateException e) {
            assertEquals("Application ID must be SNAPSHOT", e.getMessage());
        }
    }

    @Test
    public void testCreateReleaseId() {
        ApplicationID snapshot = new ApplicationID("Sears", "Inventory", "1.0.0", ReleaseStatus.SNAPSHOT.name());
        snapshot.validateIsSnapshot();


        ApplicationID releaseId = snapshot.createReleaseId();
        assertEquals(snapshot.getAccount(), releaseId.getAccount());
        assertEquals(snapshot.getApp(), releaseId.getApp());
        assertEquals(snapshot.getVersion(), releaseId.getVersion());
        assertEquals(ReleaseStatus.RELEASE.name(), releaseId.getStatus());
    }

    @Test
    public void testCreateNewSnapshotId() {
        ApplicationID releaseId = new ApplicationID("Sears", "Inventory", "1.0.0", ReleaseStatus.RELEASE.name());

        ApplicationID snapshotId = releaseId.createNewSnapshotId("1.1.0");
        assertEquals(releaseId.getAccount(), snapshotId.getAccount());
        assertEquals(releaseId.getApp(), snapshotId.getApp());
        assertEquals("1.1.0", snapshotId.getVersion());
        assertEquals(ReleaseStatus.SNAPSHOT.name(), snapshotId.getStatus());
    }

    @Test
    public void testValidateStatus() throws Exception
    {
        ApplicationID.validateStatus(ReleaseStatus.SNAPSHOT.name());
        ApplicationID.validateStatus(ReleaseStatus.RELEASE.name());
        try
        {
            ApplicationID.validateStatus("fubar");
            fail("should not make it here");
        }
        catch (Exception e)
        { }
    }


    @Test
    public void testValidateIsSnapshot() {
        ApplicationID snapshot = new ApplicationID("Sears", "Inventory", "1.0.0", ReleaseStatus.SNAPSHOT.name());
        snapshot.validateIsSnapshot();

        ApplicationID releaseId = new ApplicationID("Sears", "Inventory", "1.0.0", ReleaseStatus.RELEASE.name());
        try
        {
            releaseId.validateIsSnapshot();
        } catch (IllegalStateException e) {
            assertEquals("Application ID must be SNAPSHOT", e.getMessage());
        }

    }

    @Test
    public void testValidateTenant()
    {
        String msg = "Tenant cannot be null or empty";
        ApplicationID.validateTenant("foo");
        try
        {
            ApplicationID.validateTenant(null);
            fail("should not make it here");
        }
        catch (IllegalArgumentException e)
        {
            assertEquals(msg, e.getMessage());
        }

        try
        {
            ApplicationID.validateTenant("");
            fail("should not make it here");
        }
        catch (IllegalArgumentException e)
        {
            assertEquals(msg, e.getMessage());
        }
    }

    @Test
    public void testValidateApp()
    {
        String msg = "App cannot be null or empty";
        ApplicationID.validateApp("foo");
        try
        {
            ApplicationID.validateApp(null);
            fail("should not make it here");
        }
        catch (IllegalArgumentException e)
        {
            assertEquals(msg, e.getMessage());
        }

        try
        {
            ApplicationID.validateApp("");
            fail("should not make it here");
        }
        catch (IllegalArgumentException e)
        {
            assertEquals(msg, e.getMessage());
        }
    }

    @Test
    public void testValidateVersionNumbers()
    {
        String nullMessage = "n-cube version cannot be null or empty";

        ApplicationID.validateVersion("0.0.0");
        ApplicationID.validateVersion("9.9.9");
        ApplicationID.validateVersion("9999.99999.9999");
        try
        {
            ApplicationID.validateVersion(null);
            fail("should not make it here");
        }
        catch (IllegalArgumentException e)
        {
            assertEquals(nullMessage, e.getMessage());
        }
        try
        {
            ApplicationID.validateVersion("");
            fail("should not make it here");
        }
        catch (IllegalArgumentException e)
        {
            assertEquals(nullMessage, e.getMessage());
        }
        try
        {
            ApplicationID.validateVersion("0.1.a");
            fail("should not make it here");
        }
        catch (IllegalArgumentException e)
        {
        }
        try
        {
            ApplicationID.validateVersion("0.1.0.1");
            fail("should not make it here");
        }
        catch (IllegalArgumentException e)
        {
        }
        try
        {
            ApplicationID.validateVersion("0.1");
            fail("should not make it here");
        }
        catch (IllegalArgumentException e)
        {
        }
    }


}
