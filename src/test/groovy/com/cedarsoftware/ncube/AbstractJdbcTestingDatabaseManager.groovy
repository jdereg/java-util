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

    void removeCubes(ApplicationID appId) throws Exception
    {
        Connection c = provider.connection;
        try
        {
            persister.deleteCubes(c, appId);
        }
        finally
        {
            provider.releaseConnection(c);
        }
    }

    void removeCubes(ApplicationID appId, String username, NCube[] cubes) throws Exception
    {
        Connection c = provider.connection;
        try
        {
            for (NCube ncube : cubes)
            {
                persister.deleteCube(c, appId, ncube.name, true, "test");
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
