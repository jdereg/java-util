package com.cedarsoftware.ncube

import org.junit.After
import org.junit.Before
import org.junit.Test

/**
 * Created by jderegnaucourt on 2015/01/12.
 */
class TestGitNCubeManager
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
        persister.setRepositoryDir('/Users/jderegnaucourt/Development/cubes/.git')
        NCubeManager.setNCubePersister(persister);
        Object[] cubes = NCubeManager.getCubeRecordsFromDatabase(ApplicationID.defaultAppId, '*')
        for (NCubeInfoDto info : cubes)
        {
            println info
        }
    }
}
