package com.cedarsoftware.ncube

import org.junit.After
import org.junit.Before
import org.junit.Test

import static org.junit.Assert.assertFalse
import static org.junit.Assert.assertNotNull

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

    @Test
    void testGetCubeRecords()
    {
        NCubeGitPersister persister = new NCubeGitPersister()
        persister.repositoryDir = '/Users/jderegnaucourt/Development/cubes/.git'
        NCubeManager.NCubePersister = persister;
        Object[] cubes = NCubeManager.getCubeRecordsFromDatabase(ApplicationID.defaultAppId, '*')
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
    }

    @Test
    void testLoadCube()
    {
        NCubeGitPersister persister = new NCubeGitPersister()
        persister.repositoryDir = '/Users/jderegnaucourt/Development/cubes/.git'
        NCubeManager.NCubePersister = persister;
        NCube cube = NCubeManager.getCube(ApplicationID.defaultAppId, 'aa')
        assertNotNull cube.getAxis('state').findColumn('OH')
    }

    @Test
    void doesCubeExist()
    {
        NCubeGitPersister persister = new NCubeGitPersister()
        persister.repositoryDir = '/Users/jderegnaucourt/Development/cubes/.git'
        NCubeManager.NCubePersister = persister;
        assert NCubeManager.doesCubeExist(ApplicationID.defaultAppId, 'a')
        assert NCubeManager.doesCubeExist(ApplicationID.defaultAppId, 'aa')
        assert NCubeManager.doesCubeExist(ApplicationID.defaultAppId, 'big5D')
        assertFalse NCubeManager.doesCubeExist(ApplicationID.defaultAppId, 'SDFdsfl')
    }
}
