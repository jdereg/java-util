package com.cedarsoftware.ncube;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
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
}
