package com.cedarsoftware.ncube;

import java.sql.Connection;
import java.util.List;

/**
 * This adapter could be replaced by an adapting proxy.  Then you could
 * implement the interface and the class and not need this class.
 *
 * @author John DeRegnaucourt (jdereg@gmail.com)
 *         <br/>
 *         Copyright (c) Cedar Software LLC
 *         <br/><br/>
 *         Licensed under the Apache License, Version 2.0 (the "License");
 *         you may not use this file except in compliance with the License.
 *         You may obtain a copy of the License at
 *         <br/><br/>
 *         http://www.apache.org/licenses/LICENSE-2.0
 *         <br/><br/>
 *         Unless required by applicable law or agreed to in writing, software
 *         distributed under the License is distributed on an "AS IS" BASIS,
 *         WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *         See the License for the specific language governing permissions and
 *         limitations under the License.
 */
public class NCubeJdbcPersisterAdapter implements NCubePersister
{
    private NCubeJdbcPersister persister = new NCubeJdbcPersister();
    private JdbcConnectionProvider connectionProvider;

    public NCubeJdbcPersisterAdapter(JdbcConnectionProvider provider)
    {
        this.connectionProvider = provider;
    }

    @Override
    public void createCube(ApplicationID id, NCube cube)
    {
        Connection c = connectionProvider.getConnection();
        try
        {
            persister.createCube(c, id, cube);
        }
        finally
        {
            connectionProvider.releaseConnection(c);
        }
    }

    @Override
    public void updateCube(ApplicationID appId, NCube cube)
    {
        Connection c = connectionProvider.getConnection();
        try
        {
            persister.updateCube(c, appId, cube);
        }
        finally
        {
            connectionProvider.releaseConnection(c);
        }
    }

    @Override
    public List<NCube> loadCubes(ApplicationID appId)
    {
        Connection c = connectionProvider.getConnection();
        try
        {
            return persister.loadCubes(c, appId);
        }
        finally
        {
            connectionProvider.releaseConnection(c);
        }
    }

    @Override
    public Object[] getNCubes(ApplicationID appId, String sqlLike)
    {
        Connection c = connectionProvider.getConnection();
        try
        {
            return persister.getNCubes(c, appId, sqlLike);
        }
        finally
        {
            connectionProvider.releaseConnection(c);
        }
    }

    @Override
    public NCube findCube(ApplicationID appId, String ncubeName)
    {
        Connection c = connectionProvider.getConnection();
        try
        {
            return persister.findCube(c, appId, ncubeName);
        }
        finally
        {
            connectionProvider.releaseConnection(c);
        }
    }

    @Override
    public boolean deleteCube(ApplicationID appId, String name, boolean allowDelete)
    {
        Connection c = connectionProvider.getConnection();
        try
        {
            return persister.deleteCube(c, appId, name, allowDelete);
        }
        finally
        {
            connectionProvider.releaseConnection(c);
        }
    }

    @Override
    public boolean doesCubeExist(ApplicationID id, String name)
    {
        Connection c = connectionProvider.getConnection();
        try
        {
            return persister.doesCubeExist(c, id, name);
        }
        finally
        {
            connectionProvider.releaseConnection(c);
        }
    }

    @Override
    public Object[] getAppNames()
    {
        Connection c = connectionProvider.getConnection();
        try
        {
            return persister.getAppNames(c);
        }
        finally
        {
            connectionProvider.releaseConnection(c);
        }
    }

    @Override
    public Object[] getAppVersions(ApplicationID id)
    {
        Connection c = connectionProvider.getConnection();
        try
        {
            return persister.getAppVersions(c, id);
        }
        finally
        {
            connectionProvider.releaseConnection(c);
        }
    }

    @Override
    public boolean updateNotes(ApplicationID id, String cubeName, String notes)
    {
        Connection c = connectionProvider.getConnection();
        try
        {
            return persister.updateNotes(c, id, cubeName, notes);
        }
        finally
        {
            connectionProvider.releaseConnection(c);
        }
    }

    @Override
    public String getNotes(ApplicationID id, String cubeName)
    {
        Connection c = connectionProvider.getConnection();
        try
        {
            return persister.getNotes(c, id, cubeName);
        }
        finally
        {
            connectionProvider.releaseConnection(c);
        }
    }

    @Override
    public int createSnapshotVersion(ApplicationID id, String newVersion)
    {
        Connection c = connectionProvider.getConnection();
        try
        {
            return persister.createSnapshotVersion(c, id, newVersion);
        }
        finally
        {
            connectionProvider.releaseConnection(c);
        }
    }

    @Override
    public int changeVersionValue(ApplicationID id, String newVersion)
    {
        Connection c = connectionProvider.getConnection();
        try
        {
            return persister.changeVersionValue(c, id, newVersion);
        }
        finally
        {
            connectionProvider.releaseConnection(c);
        }
    }

    @Override
    public int releaseCubes(ApplicationID id)
    {
        Connection c = connectionProvider.getConnection();
        try
        {
            return persister.releaseCubes(c, id);
        }
        finally
        {
            connectionProvider.releaseConnection(c);
        }
    }

    @Override
    public boolean renameCube(ApplicationID id, NCube oldCube, String newName)
    {
        Connection c = connectionProvider.getConnection();
        try
        {
            return persister.renameCube(c, id, oldCube, newName);
        }
        finally
        {
            connectionProvider.releaseConnection(c);
        }
    }

    @Override
    public boolean updateTestData(ApplicationID id, String cubeName, String testData)
    {
        Connection c = connectionProvider.getConnection();
        try
        {
            return persister.updateTestData(c, id, cubeName, testData);
        }
        finally
        {
            connectionProvider.releaseConnection(c);
        }
    }

    @Override
    public String getTestData(ApplicationID id, String cubeName)
    {
        Connection c = connectionProvider.getConnection();
        try
        {
            return persister.getTestData(c, id, cubeName);
        }
        finally
        {
            connectionProvider.releaseConnection(c);
        }
    }
}
