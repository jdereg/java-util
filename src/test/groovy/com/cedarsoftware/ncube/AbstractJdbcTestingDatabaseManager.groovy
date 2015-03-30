package com.cedarsoftware.ncube;

import java.sql.Connection;

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

    void addCubes(ApplicationID appId, String username, NCube[] cubes) throws Exception
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

    void removeBranches(ApplicationID[] appIds) throws Exception
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

    void updateCube(ApplicationID appId, String username, NCube ncube) throws Exception
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
