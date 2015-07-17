package com.cedarsoftware.ncube

import com.cedarsoftware.util.io.JsonReader
import com.cedarsoftware.util.io.JsonWriter
import org.junit.Test

import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertFalse
import static org.junit.Assert.assertNotEquals
import static org.junit.Assert.assertNotNull
import static org.junit.Assert.assertNotSame
import static org.junit.Assert.assertSame
import static org.junit.Assert.assertTrue
import static org.junit.Assert.fail

/**
 * ApplicationID Tests
 *
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
class TestApplicationID
{
    @Test
    void testBadConstructorArgs()
    {
        try
        {
            new ApplicationID(null, 'Inventory', '99.99.99', ApplicationID.DEFAULT_STATUS, ApplicationID.TEST_BRANCH)
            fail 'should not make it here'
        }
        catch (Exception ignored)
        {
            assertTrue ignored.message.contains('Invalid tenant string')
        }

        try
        {
            new ApplicationID('Macys', null, '9.9.9', ApplicationID.DEFAULT_STATUS, ApplicationID.TEST_BRANCH)
            fail 'should not make it here'
        }
        catch (Exception ignored)
        {
            assertTrue ignored.message.contains('cannot be null')
        }

        try
        {
            new ApplicationID('Macys', 'Catalog', null, ApplicationID.DEFAULT_STATUS, ApplicationID.TEST_BRANCH)
            fail 'should not make it here'
        }
        catch (Exception ignored)
        {
            assertTrue ignored.message.contains('cannot be null')
        }

        try
        {
            new ApplicationID('Macys', 'Catalog', '1.2.3', null, ApplicationID.TEST_BRANCH)
            fail 'should not make it here'
        }
        catch (Exception ignored)
        {
            assertTrue(ignored.message.contains('cannot be null'))
        }

        try
        {
            new ApplicationID('Macys', 'Catalog', '1.2.a', ApplicationID.DEFAULT_STATUS, ApplicationID.TEST_BRANCH)
            fail 'should not make it here'
        }
        catch (Exception ignored)
        {
            assertTrue ignored.message.contains('Invalid version')
        }

        try
        {
            new ApplicationID('Macys', 'Catalog', '1.2.3.4', ApplicationID.DEFAULT_STATUS, ApplicationID.TEST_BRANCH)
            fail 'should not make it here'
        }
        catch (Exception ignored)
        {
            assertTrue ignored.message.contains('Invalid version')
        }

        try
        {
            new ApplicationID('Macys', 'Catalog', '1.2', ApplicationID.DEFAULT_STATUS, ApplicationID.TEST_BRANCH)
            fail 'should not make it here'
        }
        catch (Exception ignored)
        {
            assertTrue ignored.message.contains('Invalid version')
        }

        try
        {
            new ApplicationID('Macys', 'Catalog', '1.-2.3', ApplicationID.DEFAULT_STATUS, ApplicationID.TEST_BRANCH)
            fail 'should not make it here'
        }
        catch (Exception ignored)
        {
            assertTrue ignored.message.contains('Invalid version')
        }

        try
        {
            new ApplicationID('Macys', 'Catalog', '1.2.3', 'CRAPSHOT', ApplicationID.TEST_BRANCH)
            fail 'should not make it here'
        }
        catch (Exception ignored)
        {
            assertTrue ignored.message.contains('No enum constant')
        }

        try
        {
            new ApplicationID('Macy\'s', 'Catalog', '1.2.3', 'RELEASE', ApplicationID.TEST_BRANCH)
            fail 'should not make it here'
        }
        catch (Exception ignored)
        {
            assertTrue ignored.message.contains('Invalid tenant')
        }
    }

    @Test
    void testApplicationIDConstructorAndGetters()
    {
        ApplicationID appId = new ApplicationID('Sears', 'Inventory', '1.0.0', ApplicationID.DEFAULT_STATUS, ApplicationID.TEST_BRANCH)
        assertEquals 'Sears', appId.tenant
        assertEquals 'Inventory', appId.app
        assertEquals '1.0.0', appId.version
        assertEquals 'SNAPSHOT', appId.status
    }

    @Test
    void testAppKey()
    {
        ApplicationID appId = new ApplicationID('Sears', 'Inventory', '1.0.0', ApplicationID.DEFAULT_STATUS, ApplicationID.TEST_BRANCH)
        assertEquals 'sears / inventory / 1.0.0 / test /', appId.cacheKey('')
        assertEquals 'sears / inventory / 1.0.0 / test /', appId.toString()
    }

    @Test
    void testEqualsAndHashcode()
    {
        ApplicationID appId1 = new ApplicationID('Sears', 'Inventory', '1.0.0', ApplicationID.DEFAULT_STATUS, ApplicationID.TEST_BRANCH)
        ApplicationID appId2 = new ApplicationID('Sears', 'Inventory', '1.0.0', ReleaseStatus.RELEASE.name(), ApplicationID.TEST_BRANCH)

        assertEquals appId1.cacheKey(''), appId2.cacheKey('')
        assertNotEquals appId1, appId2
        assertNotEquals appId1.hashCode(), appId2.hashCode()

        appId1 = new ApplicationID('Sears', 'Inventory', '1.0.0', ApplicationID.DEFAULT_STATUS, ApplicationID.TEST_BRANCH)
        appId2 = new ApplicationID('Sears', 'Inventory', '1.0.1', ApplicationID.DEFAULT_STATUS, ApplicationID.TEST_BRANCH)

        assertNotEquals appId1.cacheKey(''), appId2.cacheKey('')
        assertNotEquals appId1, appId2
        assertNotEquals appId1.hashCode(), appId2.hashCode()

        appId1 = new ApplicationID('Sears', 'Inventory', '1.0.0', ApplicationID.DEFAULT_STATUS, ApplicationID.TEST_BRANCH)
        appId2 = new ApplicationID('Sears', 'Inventori', '1.0.0', ApplicationID.DEFAULT_STATUS, ApplicationID.TEST_BRANCH)

        assertNotEquals appId1.cacheKey(''), appId2.cacheKey('')
        assertNotEquals appId1, appId2
        assertNotEquals appId1.hashCode(), appId2.hashCode()

        appId1 = new ApplicationID('Sears', 'Inventory', '1.0.0', ApplicationID.DEFAULT_STATUS, ApplicationID.TEST_BRANCH)
        appId2 = new ApplicationID('Pears', 'Inventory', '1.0.0', ApplicationID.DEFAULT_STATUS, ApplicationID.TEST_BRANCH)

        assertNotEquals appId1.cacheKey(''), appId2.cacheKey('')
        assertNotEquals appId1, appId2
        assertNotEquals appId1.hashCode(), appId2.hashCode()

        assertNotEquals appId1, 'Hey'
        assertEquals appId1, appId1

        appId1 = new ApplicationID('Lowes', 'Inventory', '1.2.3', ApplicationID.DEFAULT_STATUS, ApplicationID.TEST_BRANCH)
        appId2 = new ApplicationID('Lowes', 'Inventory', '1.2.3', ApplicationID.DEFAULT_STATUS, ApplicationID.TEST_BRANCH)
        assertEquals appId1, appId2

        appId1 = new ApplicationID('Lowes', 'Inventory', '1.2.3', ReleaseStatus.SNAPSHOT.name(), 'JIRA-555')
        appId2 = new ApplicationID('Lowes', 'Inventory', '1.2.3', ReleaseStatus.SNAPSHOT.name(), 'Jira-555')
        assertEquals appId1, appId2
        assert appId1.hashCode() == appId2.hashCode()
    }

    // Want to know if this assumption ever changes
    @Test
    void testAppStrSameAsToString()
    {
        ApplicationID appId = new ApplicationID('Sears', 'Inventory', '1.0.0', ApplicationID.DEFAULT_STATUS, ApplicationID.TEST_BRANCH)
        assertEquals appId.toString(), appId.cacheKey('')
    }

    // Want to know if this assumption ever changes
    @Test
    void testApplicationIDSerialize()
    {
        ApplicationID appId1 = new ApplicationID('Sears', 'Inventory', '1.0.0', ApplicationID.DEFAULT_STATUS, ApplicationID.TEST_BRANCH)
        String json = JsonWriter.objectToJson(appId1)
        ApplicationID appId2 = (ApplicationID) JsonReader.jsonToJava(json)
        assertEquals appId1, appId2
    }

    @Test
    void testIsSnapshotOrRelease()
    {
        ApplicationID snapshot = new ApplicationID('Sears', 'Inventory', '1.0.0', ApplicationID.DEFAULT_STATUS, ApplicationID.TEST_BRANCH)
        assertTrue snapshot.snapshot
        assertFalse snapshot.release

        ApplicationID releaseId = new ApplicationID('Sears', 'Inventory', '1.0.0', ReleaseStatus.RELEASE.name(), ApplicationID.TEST_BRANCH)
        assertFalse releaseId.snapshot
        assertTrue releaseId.release
    }

    @Test
    void testValidateSnapshot()
    {
        ApplicationID snapshot = new ApplicationID('Sears', 'Inventory', '1.0.0', ApplicationID.DEFAULT_STATUS, ApplicationID.TEST_BRANCH)
        snapshot.validate()

        ApplicationID releaseId = new ApplicationID('Sears', 'Inventory', '1.0.0', ReleaseStatus.RELEASE.name(), ApplicationID.TEST_BRANCH)
        try
        {
            releaseId.validate()
        }
        catch (IllegalStateException e)
        {
            assertEquals 'Application ID must be SNAPSHOT', e.message
        }
    }

    @Test
    void testCreateReleaseId()
    {
        ApplicationID snapshot = new ApplicationID('Sears', 'Inventory', '1.0.0', ApplicationID.DEFAULT_STATUS, ApplicationID.TEST_BRANCH)
        snapshot.validate()

        ApplicationID releaseId = new ApplicationID('Sears', 'Inventory', '1.0.0', ReleaseStatus.RELEASE.name(), ApplicationID.TEST_BRANCH)
        assertEquals snapshot.tenant, releaseId.tenant
        assertEquals snapshot.app, releaseId.app
        assertEquals snapshot.version, releaseId.version
        assertEquals ReleaseStatus.RELEASE.name(), releaseId.status
    }

    @Test
    void testCreateNewSnapshotId()
    {
        ApplicationID releaseId = new ApplicationID('Sears', 'Inventory', '1.0.0', ReleaseStatus.RELEASE.name(), ApplicationID.TEST_BRANCH)

        ApplicationID snapshotId = releaseId.createNewSnapshotId('1.1.0')
        assertEquals releaseId.tenant, snapshotId.tenant
        assertEquals releaseId.app, snapshotId.app
        assertEquals '1.1.0', snapshotId.version
        assertEquals ReleaseStatus.SNAPSHOT.name(), snapshotId.status
    }

    @Test
    void testAsSnapshot()
    {
        ApplicationID releaseId = new ApplicationID('Sears', 'Inventory', '1.0.0', ReleaseStatus.RELEASE.name(), ApplicationID.TEST_BRANCH)

        ApplicationID snapshotId = releaseId.asSnapshot()

        assertNotSame(releaseId, snapshotId)
        assertEquals releaseId.tenant, snapshotId.tenant
        assertEquals releaseId.app, snapshotId.app
        assertEquals releaseId.version, snapshotId.version
        assertEquals ReleaseStatus.SNAPSHOT.name(), snapshotId.status
        assertEquals releaseId.branch, snapshotId.branch

        ApplicationID newSnapshotId = snapshotId.asSnapshot();
        assertSame(snapshotId, newSnapshotId);
    }

    @Test
    void testAsVersion()
    {
        ApplicationID versionId = new ApplicationID('Sears', 'Inventory', '1.0.0', ReleaseStatus.RELEASE.name(), ApplicationID.TEST_BRANCH)

        ApplicationID newVersionId = versionId.asVersion('1.1.1')


        assertNotSame(versionId, newVersionId)
        assertEquals versionId.tenant, newVersionId.tenant
        assertEquals versionId.app, newVersionId.app
        assertNotEquals versionId.version, newVersionId.version
        assertEquals versionId.status, newVersionId.status
        assertEquals versionId.branch, newVersionId.branch

        ApplicationID newerVersion = newVersionId.asVersion('1.1.1');
        assertSame(newerVersion, newVersionId);
    }

    @Test
    void teatAsRelease()
    {
        ApplicationID snapshotId = new ApplicationID('Sears', 'Inventory', '1.0.0', ReleaseStatus.SNAPSHOT.name(), ApplicationID.TEST_BRANCH)

        ApplicationID releaseId = snapshotId.asRelease()

        assertNotSame(releaseId, snapshotId)
        assertEquals releaseId.tenant, snapshotId.tenant
        assertEquals releaseId.app, snapshotId.app
        assertEquals releaseId.version, snapshotId.version
        assertEquals ReleaseStatus.RELEASE.name(), releaseId.status
        assertEquals ApplicationID.HEAD, releaseId.branch

        snapshotId = new ApplicationID('Sears', 'Inventory', '1.0.0', ReleaseStatus.RELEASE.name(), ApplicationID.TEST_BRANCH)

        releaseId = snapshotId.asRelease()

        assertNotSame(releaseId, snapshotId)
        assertEquals releaseId.tenant, snapshotId.tenant
        assertEquals releaseId.app, snapshotId.app
        assertEquals releaseId.version, snapshotId.version
        assertEquals ReleaseStatus.RELEASE.name(), releaseId.status
        assertEquals ApplicationID.HEAD, releaseId.branch


        ApplicationID newReleaseId = releaseId.asRelease();
        assertSame(releaseId, newReleaseId);
    }

    @Test
    void testAsHead()
    {
        ApplicationID snapshotId = new ApplicationID('Sears', 'Inventory', '1.0.0', ReleaseStatus.SNAPSHOT.name(), ApplicationID.TEST_BRANCH)
        ApplicationID headId = snapshotId.asHead();
        assertNotSame(headId, snapshotId);

        assertEquals headId.tenant, snapshotId.tenant
        assertEquals headId.app, snapshotId.app
        assertEquals headId.version, snapshotId.version
        assertEquals headId.status, snapshotId.status
        assertEquals ApplicationID.HEAD, headId.branch

        ApplicationID newHeadId = headId.asHead();
        assertSame(headId, newHeadId);
    }

    @Test
    void testValidateStatus()
    {
        ApplicationID.validateStatus ReleaseStatus.SNAPSHOT.name()
        ApplicationID.validateStatus ReleaseStatus.RELEASE.name()
        try
        {
            ApplicationID.validateStatus 'fubar'
            fail 'should not make it here'
        }
        catch (Exception e)
        {
            assertTrue e.message.toLowerCase().contains('no enum constant')
        }
    }


    @Test
    void testValidateIsSnapshot()
    {
        ApplicationID snapshot = new ApplicationID('Sears', 'Inventory', '1.0.0', ApplicationID.DEFAULT_STATUS, ApplicationID.TEST_BRANCH)
        snapshot.validate()

        ApplicationID releaseId = new ApplicationID('Sears', 'Inventory', '1.0.0', ReleaseStatus.RELEASE.name(), ApplicationID.TEST_BRANCH)
        try
        {
            releaseId.validate()
        }
        catch (IllegalStateException e)
        {
            assertEquals 'Application ID must be SNAPSHOT', e.message
        }

    }

    @Test
    void testValidateTenant()
    {
        ApplicationID.validateTenant 'foo'
        try
        {
            ApplicationID.validateTenant null
            fail 'should not make it here'
        }
        catch (IllegalArgumentException e)
        {
            assertTrue e.message.contains('nvalid')
            assertTrue e.message.contains('tenant')
            assertTrue e.message.contains('must')
        }

        try
        {
            ApplicationID.validateTenant ''
            fail 'should not make it here'
        }
        catch (IllegalArgumentException e)
        {
            assertTrue e.message.contains('nvalid')
            assertTrue e.message.contains('tenant')
            assertTrue e.message.contains('must')
        }
    }

    @Test
    void testValidateApp()
    {
        String msg = 'App cannot be null or empty';
        ApplicationID.validateApp 'foo'
        try
        {
            ApplicationID.validateApp null
            fail 'should not make it here'
        }
        catch (IllegalArgumentException e)
        {
            assertEquals msg, e.message
        }

        try
        {
            ApplicationID.validateApp ''
            fail 'should not make it here'
        }
        catch (IllegalArgumentException e)
        {
            assertEquals msg, e.message
        }
    }

    @Test
    void testValidateVersionNumbers()
    {
        String nullMessage = 'n-cube version cannot be null or empty';

        ApplicationID.validateVersion '0.0.0'
        ApplicationID.validateVersion '9.9.9'
        ApplicationID.validateVersion '9999.99999.9999'
        try
        {
            ApplicationID.validateVersion null
            fail 'should not make it here'
        }
        catch (IllegalArgumentException e)
        {
            assertEquals nullMessage, e.message
        }
        try
        {
            ApplicationID.validateVersion ''
            fail 'should not make it here'
        }
        catch (IllegalArgumentException e)
        {
            assertEquals nullMessage, e.message
        }
        try
        {
            ApplicationID.validateVersion '0.1.a'
            fail 'should not make it here'
        }
        catch (IllegalArgumentException e)
        {
            assertTrue e.message.toLowerCase().contains('invalid version')
            assertTrue e.message.toLowerCase().contains('must follow')
        }
        try
        {
            ApplicationID.validateVersion '0.1.0.1'
            fail 'should not make it here'
        }
        catch (IllegalArgumentException e)
        {
            assertTrue e.message.toLowerCase().contains('invalid version')
            assertTrue e.message.toLowerCase().contains('must follow')
        }
        try
        {
            ApplicationID.validateVersion '0.1'
            fail 'should not make it here'
        }
        catch (IllegalArgumentException e)
        {
            assertTrue e.message.toLowerCase().contains('invalid version')
            assertTrue e.message.toLowerCase().contains('must follow')
        }
    }

    @Test
    void testBranchName()
    {
        ApplicationID appId = new ApplicationID('Sears', 'Inventory', '1.0.0', ReleaseStatus.RELEASE.name(), '_alpha-')
        assertNotNull appId
        appId.validate()

        appId = new ApplicationID('Sears', 'Inventory', '1.0.0', ReleaseStatus.RELEASE.name(), 'roger.dodger')
        assertNotNull appId
        appId.validate()

        try
        {
            new ApplicationID('Sears', 'Inventory', '1.0.0', ReleaseStatus.RELEASE.name(), "")
            fail()
        }
        catch (IllegalArgumentException e)
        {
            assert e.message.toLowerCase().contains('branch cannot be null or empty')
        }

        try
        {
            new ApplicationID('Sears', 'Inventory', '1.0.0', ReleaseStatus.RELEASE.name(), null)
            fail()
        }
        catch (IllegalArgumentException e)
        {
            assert e.message.toLowerCase().contains('branch cannot be null or empty')
        }

        try
        {
            new ApplicationID('Sears', 'Inventory', '1.0.0', ReleaseStatus.RELEASE.name(), '   ')
            fail()
        }
        catch (IllegalArgumentException e)
        {
            assert e.message.toLowerCase().contains('branch cannot be null or empty')
        }

        try
        {
            new ApplicationID('Sears', 'Inventory', '1.0.0', ReleaseStatus.RELEASE.name(), 'alpha/bravo')
            fail()
        }
        catch (IllegalArgumentException e)
        {
            assert e.message.toLowerCase().contains('invalid branch')
        }
    }

    @Test
    void testGetChangeSet()
    {
        ApplicationID appId = new ApplicationID('Sears', 'Inventory', '1.0.0', ReleaseStatus.RELEASE.name(), 'JIRA-555')
        appId.validate()
        assert appId.getBranch() == 'JIRA-555'
        assert appId.cacheKey().contains('jira-555')
    }

    @Test
    void testGetBootstrapVersion()
    {
        ApplicationID id = ApplicationID.getBootVersion 'foo', 'bar'
        assertEquals 'foo', id.tenant
        assertEquals 'bar', id.app
        assertEquals '0.0.0', id.version
        assertEquals 'SNAPSHOT', id.status
    }

    @Test
    void testValidateIsNotRelease() {
        ApplicationID snap = new ApplicationID("NONE", "app", "1.28.0", "SNAPSHOT", "HEAD");
        snap.validateStatusIsNotRelease()

        ApplicationID rel = new ApplicationID("NONE", "app", "1.28.0", "RELEASE", "HEAD");
        try {
            rel.validateStatusIsNotRelease()
        } catch (IllegalArgumentException e) {
            assertEquals("Status cannot be 'RELEASE'", e.getMessage());
        }
    }

    @Test
    void testValidateIsNotHead() {
        ApplicationID snap = new ApplicationID("NONE", "app", "1.28.0", "SNAPSHOT", "FOO");
        snap.validateBranchIsNotHead()

        ApplicationID rel = new ApplicationID("NONE", "app", "1.28.0", "SNAPSHOT", "HEAD");
        try {
            rel.validateBranchIsNotHead()
        } catch (IllegalArgumentException e) {
            assertEquals("Branch cannot be 'HEAD'", e.getMessage());
        }
    }

    @Test
    void testOldConstrucgtor() {
        try {
            new ApplicationID("NONE", "app", "1.28.0", "SNAPSHOT");
        } catch (RuntimeException e) {
            assertTrue(e.message.contains("5 argument"))
            assertTrue(e.message.contains("add branch"))
        }
    }
}
