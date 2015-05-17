package com.cedarsoftware.ncube

import com.cedarsoftware.util.StringUtilities

import java.sql.Connection

/**
 * Created by kpartlow on 12/23/2014.
 */
abstract class AbstractJdbcTestingDatabaseManager implements TestingDatabaseManager
{
    JdbcConnectionProvider provider;
    NCubeJdbcPersister persister = new NCubeJdbcPersister();

    AbstractJdbcTestingDatabaseManager(JdbcConnectionProvider p) {
        provider = p;
    }

    void insertCubeWithNoSha1(ApplicationID appId, String username, NCube cube)
    {
        Connection c = provider.connection;
        try
        {
            byte[] cubeData = StringUtilities.getBytes(cube.toFormattedJson(), "UTF-8");
            persister.insertCube(c, appId, cube.name, 0L, cubeData, null, "Inserted without sha1-1", false, null, null, System.currentTimeMillis(), username)
    }
        finally
        {
            provider.releaseConnection(c);
        }
    }

    void addCubes(ApplicationID appId, String username, NCube[] cubes)
    {
        Connection c = provider.connection;
        try
        {
            for (NCube ncube : cubes)
            {
                persister.createCube(c, appId, ncube, username);
            }
        }
        finally
        {
            provider.releaseConnection(c);
        }
    }

    void removeBranches(ApplicationID[] appIds)
    {
        Connection c = provider.connection;
        try
        {
            for (ApplicationID appId : appIds)
            {
                persister.deleteBranch(c, appId)
            }
        }
        finally
        {
            provider.releaseConnection(c);
        }
    }

    void updateCube(ApplicationID appId, String username, NCube ncube)
    {
        Connection c = provider.connection;
        try
        {
            persister.updateCube(c, appId, ncube, username);
        }
        finally
        {
            provider.releaseConnection(c);
        }
    }
}
