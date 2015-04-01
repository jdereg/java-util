package com.cedarsoftware.ncube

import com.cedarsoftware.util.StringUtilities

import java.sql.Connection
import java.util.regex.Matcher

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

    void insertCubeWithNoSha1(ApplicationID appId, String username, NCube cube) throws Exception
    {
        Connection c = provider.connection;
        try
        {
            String s = cube.toFormattedJson();
            Matcher m = Regexes.sha1Pattern.matcher(s);
            StringBuffer buffer = new StringBuffer();
            if (m.find() && m.groupCount() > 0)
            {
                m.appendReplacement(buffer, "");
            }
            m.appendTail(buffer);

            byte[] cubeData = StringUtilities.getBytes(buffer.toString(), "UTF-8");
            persister.insertCube(c, appId, cube.name, 0L, cubeData, null, "Inserted without sha1-1", username)
    }
        finally
        {
            provider.releaseConnection(c);
        }
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
