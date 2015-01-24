package com.cedarsoftware.ncube

import org.junit.After
import org.junit.Before
import org.junit.Test

/**
 * Created by jderegnaucourt on 2015/01/12.
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
        for (int i = 0; i < 1; i++)
        {
            Object[] cubes = NCubeManager.getCubeRecordsFromDatabase(ApplicationID.defaultAppId, '*')
            for (NCubeInfoDto info : cubes)
            {
//            println info
            }
        }
    }
}
