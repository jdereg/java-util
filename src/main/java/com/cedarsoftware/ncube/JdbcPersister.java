package com.cedarsoftware.ncube;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public class JdbcPersister implements NCubePersister
{
    private NCubeJdbcPersister persister = new NCubeJdbcPersister();
    private JdbcConnectionProvider provider;

    public JdbcPersister(JdbcConnectionProvider provider)
    {
        this.provider = provider;
    }

    private Connection getConnection()
    {
        return provider.getConnection(dataSource);
    }

    private void releaseConnection(Connection c) {
        provider.releaseConnection(c);
    }


    @Override
    public void createCube(ApplicationID id, NCube cube)
    {
        try (Connection c = getTransaction())
        {
            try
            {
                persister.createCube(c, id, cube);
                commitTransaction(c);
            }
            catch (Exception e)
            {
                rollbackTransaction(c);
                throw new RuntimeException(e);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void updateCube(ApplicationID appId, NCube cube)
    {
        try (Connection c = beginTransaction())
        {
            try
            {
                persister.updateCube(c, appId, cube);
                commitTransaction(c);
            }
            catch (Exception e)
            {
                rollbackTransaction(c);
                throw new RuntimeException(e);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

    }

    @Override
    public List<NCube> loadCubes(ApplicationID appId)
    {
        try (Connection c = beginTransaction())
        {
            try
            {
                List<NCube> cubes = persister.loadCubes(c, appId);
                commitTransaction(c);
                return cubes;
            }
            catch (Exception e)
            {
                rollbackTransaction(c);
                throw new RuntimeException(e);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Object[] getNCubes(ApplicationID appId, String sqlLike)
    {
        try (Connection c = beginTransaction())
        {
            try
            {
                Object[] ret = persister.getNCubes(c, appId, sqlLike);
                commitTransaction(c);
                return ret;
            }
            catch (Exception e)
            {
                rollbackTransaction(c);
                throw new RuntimeException(e);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public NCube findCube(ApplicationID appId, String ncubeName)
    {
        try (Connection c = beginTransaction())
        {
            try
            {
                NCube cube = persister.findCube(c, appId, ncubeName);
                commitTransaction(c);
                return cube;
            }
            catch (Exception e)
            {
                rollbackTransaction(c);
                throw new RuntimeException(e);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean deleteCube(ApplicationID appId, String name, boolean allowDelete)
    {
        try (Connection c = beginTransaction())
        {
            try
            {
                boolean ret = persister.deleteCube(c, appId, name, allowDelete);
                commitTransaction(c);
                return ret;
            }
            catch (Exception e)
            {
                rollbackTransaction(c);
                throw new RuntimeException(e);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean doesCubeExist(ApplicationID id, String name)
    {
        try (Connection c = beginTransaction())
        {
            try
            {
                boolean ret = persister.doesCubeExist(c, id, name);
                commitTransaction(c);
                return ret;
            }
            catch (Exception e)
            {
                rollbackTransaction(c);
                throw new RuntimeException(e);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Object[] getAppNames()
    {
        try (Connection c = beginTransaction())
        {
            try
            {
                Object[] ret = persister.getAppNames(c);
                commitTransaction(c);
                return ret;
            }
            catch (Exception e)
            {
                rollbackTransaction(c);
                throw new RuntimeException(e);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Object[] getAppVersions(ApplicationID id)
    {
        try (Connection c = beginTransaction())
        {
            try
            {
                Object[] ret = persister.getAppVersions(c, id);
                commitTransaction(c);
                return ret;
            }
            catch (Exception e)
            {
                rollbackTransaction(c);
                throw new RuntimeException(e);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean updateNotes(ApplicationID id, String cubeName, String notes)
    {
        try (Connection c = beginTransaction())
        {
            try
            {
                boolean ret = persister.updateNotes(c, id, cubeName, notes);
                commitTransaction(c);
                return ret;
            }
            catch (Exception e)
            {
                rollbackTransaction(c);
                throw new RuntimeException(e);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getNotes(ApplicationID id, String cubeName)
    {
        try (Connection c = beginTransaction())
        {
            try
            {
                String s = persister.getNotes(c, id, cubeName);
                commitTransaction(c);
                return s;
            }
            catch (Exception e)
            {
                rollbackTransaction(c);
                throw new RuntimeException(e);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public int createSnapshotVersion(ApplicationID id, String newVersion)
    {
        try (Connection c = beginTransaction())
        {
            try
            {
                int ret = persister.createSnapshotVersion(c, id, newVersion);
                commitTransaction(c);
                return ret;
            }
            catch (Exception e)
            {
                rollbackTransaction(c);
                throw new RuntimeException(e);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public int changeVersionValue(ApplicationID id, String newVersion)
    {
        try (Connection c = beginTransaction())
        {
            try
            {
                int ret = persister.changeVersionValue(c, id, newVersion);
                commitTransaction(c);
                return ret;
            }
            catch (Exception e)
            {
                rollbackTransaction(c);
                throw new RuntimeException(e);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public int releaseCubes(ApplicationID id)
    {
        try (Connection c = beginTransaction())
        {
            try
            {
                int ret = persister.releaseCubes(c, id);
                commitTransaction(c);
                return ret;
            }
            catch (Exception e)
            {
                rollbackTransaction(c);
                throw new RuntimeException(e);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean renameCube(ApplicationID id, NCube oldCube, String newName)
    {
        try (Connection c = beginTransaction())
        {
            try
            {
                boolean ret = persister.renameCube(c, id, oldCube, newName);
                commitTransaction(c);
                return ret;
            }
            catch (Exception e)
            {
                rollbackTransaction(c);
                throw new RuntimeException(e);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean updateTestData(ApplicationID id, String cubeName, String testData)
    {
        try (Connection c = beginTransaction())
        {
            try
            {
                boolean ret = persister.updateTestData(c, id, cubeName, testData);
                commitTransaction(c);
                return ret;
            }
            catch (Exception e)
            {
                rollbackTransaction(c);
                throw new RuntimeException(e);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

    }

    @Override
    public String getTestData(ApplicationID id, String cubeName)
    {
        try (Connection c = beginTransaction())
        {
            try
            {
                String ret = persister.getTestData(c, id, cubeName);
                commitTransaction(c);
                return ret;
            }
            catch (Exception e)
            {
                rollbackTransaction(c);
                throw new RuntimeException(e);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
