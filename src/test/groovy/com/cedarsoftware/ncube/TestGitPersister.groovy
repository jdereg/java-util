package com.cedarsoftware.ncube

import org.junit.After
import org.junit.Before
import org.junit.Test

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
    void testOpenRepository()
    {
        NCubeGitPersister persister = new NCubeGitPersister()
        persister.repositoryDir = '/Users/jderegnaucourt/Development/cubes/.git'
        NCubeManager.NCubePersister = persister;
        Object[] cubes = NCubeManager.getCubeRecordsFromDatabase(ApplicationID.defaultAppId, '*')
        for (NCubeInfoDto info : cubes)
        {
//            println info
        }
    }

    @Test
    void testLoadCube()
    {
        NCubeGitPersister persister = new NCubeGitPersister()
        persister.repositoryDir = '/Users/jderegnaucourt/Development/cubes/.git'
        NCubeManager.NCubePersister = persister;
        NCube cube = NCubeManager.getCube(ApplicationID.defaultAppId, 'aa')
        println cube.toHtml()
    }
}
