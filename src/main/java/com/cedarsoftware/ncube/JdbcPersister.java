package com.cedarsoftware.ncube;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;

public class JdbcPersister implements NCubePersister
{
    private DataSource dataSource;

    private String databaseUrl;
    private String username;
    private String password;

    private NCubeJdbcPersister persister = new NCubeJdbcPersister();

    /**
     * Constructs a new NCubeJdbcConnectionProvider with an initialized Datasource.
     */
    public JdbcPersister(DataSource datasource)
    {
        if (datasource == null) {
            throw new NullPointerException("datasource cannot be null");
        }
        this.dataSource = datasource;
    }

    /**
     * Constructs a new NCubeJdbcConnectionProvider with the input parameters needed to create a single database connection.
     *
     * @param driverClass - name of the database driver class
     * @param databaseUrl - database connection url
     * @param username - username of the account to be used to log into the database
     * @param password - password for the account to be used to log into the database
     * @throws java.lang.IllegalArgumentException - if connection is not a valid connection
     */
    public JdbcPersister(String driverClass, String databaseUrl, String username, String password)
    {
        if (driverClass != null)
        {
            try
            {
                Class.forName(driverClass);
            }
            catch (Exception e)
            {
                throw new RuntimeException("Unable to locate driver class: " + driverClass, e);
            }
        }

        if (databaseUrl == null) {
            throw new NullPointerException("database url cannot be null...");
        }

        if (username == null) {
            throw new NullPointerException("database user cannot be null...");
        }

        if (password == null) {
            throw new NullPointerException("database password cannot be null...");
        }

        this.databaseUrl = databaseUrl;
        this.username = username;
        this.password = password;

    }

    public Connection getConnection() throws SQLException {
        if (dataSource == null) {
            return DriverManager.getConnection(databaseUrl, username, password);
        }

        return dataSource.getConnection();
    }

    public Connection beginTransaction()
    {
        try
        {
            Connection connection = getConnection();
            connection.setAutoCommit(false);
            return connection;
        }
        catch (SQLException e)
        {
            //todo - log connection that is in auto-commit mode
            throw new RuntimeException("Unable to begin transaction...", e);
        }
    }

    /**
     * @throws java.lang.IllegalStateException - when current connection is not valid
     */
    public void commitTransaction(Connection connection)
    {
        if (connection == null)
        {
            throw new IllegalArgumentException("Unable to commit transaction.  Connection is null.");
        }

        try
        {
            connection.commit();
        }
        catch (SQLException e)
        {
            throw new RuntimeException("Unable to commit active transaction...", e);
        }
    }

    /**
     * @throws java.lang.IllegalStateException - when current connection is not valid
     */
    public void rollbackTransaction(Connection connection)
    {
        if (connection == null)
        {
            throw new IllegalArgumentException("Unable to rollback transaction. Connection is null.");
        }

        try
        {
            connection.rollback();
        }
        catch (SQLException e)
        {
            throw new RuntimeException("Unable to rollback active transaction...", e);
        }
    }

    @Override
    public void createCube(ApplicationID id, NCube cube)
    {
        try (Connection c = beginTransaction())
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
