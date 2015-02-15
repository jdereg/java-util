package com.cedarsoftware.ncube

import org.junit.After
import org.junit.Before
import org.junit.Ignore

import static org.junit.Assert.assertFalse
import static org.junit.Assert.assertNotNull
import static org.junit.Assert.fail

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
class TestGitPersister
{
    @Before
    void setUp() throws Exception
    {
    }

    @After
    void tearDown() throws Exception
    {
    }

    @Ignore
    void testGetCubeRecords()
    {
        // TODO: Where can I place this .git repo inside n-cube's repo and not have it mess things up?
//        URL url = NCubeManager.class.getResource('/repos/pas/git')
//        Path resPath = Paths.get(url.toURI())
//        return new String(Files.readAllBytes(resPath), "UTF-8")

        NCubeGitPersister persister = new NCubeGitPersister()
        persister.repositoryDir = '/repos/cubes/.git'
        NCubeManager.NCubePersister = persister;
        Object[] cubes = NCubeManager.getCubeRecordsFromDatabase(ApplicationID.defaultAppId, null)
        boolean found_a = false
        boolean found_aa = false
        boolean found_big5D = false
        for (NCubeInfoDto info : cubes)
        {
            if (info.name.equals('a'))
            {
                found_a = true
            }
            if (info.name.equals('aa'))
            {
                found_aa = true
            }
            if (info.name.equals('big5D'))
            {
                found_big5D = true
            }
        }

        assert found_a
        assert found_aa
        assert found_big5D

        cubes = NCubeManager.getCubeRecordsFromDatabase(ApplicationID.defaultAppId, '*')
        found_a = false
        found_aa = false
        found_big5D = false
        for (NCubeInfoDto info : cubes)
        {
            if (info.name.equals('a'))
            {
                found_a = true
            }
            if (info.name.equals('aa'))
            {
                found_aa = true
            }
            if (info.name.equals('big5D'))
            {
                found_big5D = true
            }
        }

        assert found_a
        assert found_aa
        assert found_big5D

        cubes = NCubeManager.getCubeRecordsFromDatabase(ApplicationID.defaultAppId, 'A*')
        found_a = false
        found_aa = false
        found_big5D = false
        for (NCubeInfoDto info : cubes)
        {
            if (info.name.equals('a'))
            {
                found_a = true
            }
            if (info.name.equals('aa'))
            {
                found_aa = true
            }
            if (info.name.equals('big5D'))
            {
                found_big5D = true
            }
        }

        assert found_a
        assert found_aa
        assertFalse found_big5D
    }

    @Ignore
    void testLoadCube()
    {
        NCubeGitPersister persister = new NCubeGitPersister()
        persister.repositoryDir = '/repos/cubes/.git'
        NCubeManager.NCubePersister = persister;
        NCube cube = NCubeManager.getCube(ApplicationID.defaultAppId, 'aa')
        assertNotNull cube.getAxis('state').findColumn('OH')
    }

    @Ignore
    void testLoadCubeRevision()
    {
        NCubeGitPersister persister = new NCubeGitPersister()
        persister.repositoryDir = '/repos/cubes/.git'
        NCubeManager.NCubePersister = persister;
        Object[] cubeInfos = NCubeManager.getCubeRecordsFromDatabase(ApplicationID.defaultAppId, 'aA')
        assertNotNull cubeInfos
        assert cubeInfos.length == 1
        NCube ncube1 = persister.loadCube(cubeInfos[0], 0)
        assertNotNull ncube1
        assert ncube1.getAxis("state").getName().equals('State')
        assert 'aa'.equals(ncube1.name)

        NCube ncube2 = persister.loadCube(cubeInfos[0], 1)
        assertNotNull ncube2
        assert ncube2.getAxis("state").getName().equals('state')
        assert 'aa'.equals(ncube2.name)

        NCube ncube3 = persister.loadCube(cubeInfos[0], 2)
        assertNotNull ncube3
        assert ncube3.getAxis("state").getName().equals('state')
        assert 'aa'.equals(ncube3.name)

        NCube ncube4 = persister.loadCube(cubeInfos[0], 3)
        assertNotNull ncube4
        assert ncube4.getAxis("state").getName().equals('state')
        assert 'aa'.equals(ncube4.name)

        NCube ncube5 = persister.loadCube(cubeInfos[0], 4)
        assertNotNull ncube5
        assert ncube5.getAxis("state").getName().equals('State')
        assert 'aa'.equals(ncube5.name)

        NCube ncubeLatest = persister.loadCube(cubeInfos[0], null)
        assertNotNull ncubeLatest
        assert ncubeLatest.getAxis("state").getName().equals('State')
        assert 'aa'.equals(ncubeLatest.name)

        try
        {
            persister.loadCube(cubeInfos[0], 99999)
            fail()
        }
        catch (IllegalArgumentException e)
        {
            assert e.message.toLowerCase().contains('revision')
            assert e.message.toLowerCase().contains('does not exist')
        }
    }

    @Ignore
    void doesCubeExist()
    {
        NCubeGitPersister persister = new NCubeGitPersister()
        persister.repositoryDir = '/repos/cubes/.git'
        NCubeManager.NCubePersister = persister;
        assert NCubeManager.doesCubeExist(ApplicationID.defaultAppId, 'a')
        assert NCubeManager.doesCubeExist(ApplicationID.defaultAppId, 'aa')
        assert NCubeManager.doesCubeExist(ApplicationID.defaultAppId, 'big5D')
        assertFalse NCubeManager.doesCubeExist(ApplicationID.defaultAppId, 'SDFdsfl')
    }
}
