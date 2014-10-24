package com.cedarsoftware.ncube;

import com.cedarsoftware.util.UniqueIdGenerator;
import com.cedarsoftware.util.io.JsonReader;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class NCubeJdbcPersister implements NCubePersister
{
    private static final Log LOG = LogFactory.getLog(NCubeJdbcPersister.class);
    
    private NCubeConnectionProvider nCubeConnectionProvider;

    @Override
    public void saveNCube(NCube ncube)
    { 
        Connection connection = getConnection();
        ApplicationID appId = ncube.getApplicationID();
        
        if (doesCubeExist(connection, ncube.getName(), appId, null))
        {
            throw new IllegalStateException("NCube '" + ncube.getName() + "' (" + appId.getApp() + ") already exists.");
        }
        
        String app = appId.getApp();
        String version = appId.getVersion();        

        try (PreparedStatement insert = connection.prepareStatement("INSERT INTO n_cube (n_cube_id, app_cd, n_cube_nm, cube_value_bin, version_no_cd, create_dt, sys_effective_dt) VALUES (?, ?, ?, ?, ?, ?, ?)"))
        {            
            //TODO: remove sys effective date
            insert.setLong(1, UniqueIdGenerator.getUniqueId());
            insert.setString(2, app);
            insert.setString(3, ncube.getName());
            insert.setBytes(4, ncube.toFormattedJson().getBytes("UTF-8"));
            insert.setString(5, version);
            java.sql.Date now = new java.sql.Date(System.currentTimeMillis());
            insert.setDate(6, now);
            insert.setDate(7, now);
            
            int rowCount = insert.executeUpdate();
            
            if (rowCount != 1)
            {
                throw new IllegalStateException("error inserting new NCube: " + ncube.getName() + "', app: " + app + ", version: " + version + " (" + rowCount + " rows inserted, should be 1)");
            }            
        }
        catch (IllegalStateException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            String s = "Unable to save NCube: " + ncube.getName() + ", app: " + app + ", version: " + version + " to database";
            LOG.error(s, e);
            throw new RuntimeException(s, e);
        }
        finally
        {
            nCubeConnectionProvider.commitTransaction(connection);
        }
    }

    @Override
    public void updateNCube(ApplicationID appId, NCube ncube)
    {

    }

    @Override
    public NCube findNCube(ApplicationID appId, String ncubeName, boolean includeTests)
    {
        String query = includeTests ?
            "SELECT cube_value_bin, test_data_bin FROM n_cube WHERE n_cube_nm = ? AND app_cd = ? AND sys_effective_dt <= ? AND (sys_expiration_dt IS NULL OR sys_expiration_dt >= ?) AND version_no_cd = ? AND status_cd = ?" :
            "SELECT cube_value_bin FROM n_cube WHERE n_cube_nm = ? AND app_cd = ? AND sys_effective_dt <= ? AND (sys_expiration_dt IS NULL OR sys_expiration_dt >= ?) AND version_no_cd = ? AND status_cd = ?";

        Connection connection = getConnection();

        String app = appId.getApp();
        String version = appId.getVersion();
        String status = appId.getStatus();
        
        try (PreparedStatement stmt = connection.prepareStatement(query))
        {
            NCube ncube = null;
            
            java.sql.Date systemDate = new java.sql.Date(new Date().getTime());
            
            //todo - remove sys effective date and expiration date
            stmt.setString(1, ncubeName);
            stmt.setString(2, app);
            stmt.setDate(3, systemDate);
            stmt.setDate(4, systemDate);
            stmt.setString(5, version);
            stmt.setString(6, status);

            try (ResultSet rs = stmt.executeQuery())
            {
                if (rs.next())
                {
                    byte[] jsonBytes = rs.getBytes("cube_value_bin");
                    String json = new String(jsonBytes, "UTF-8");
                    ncube = ncubeFromJson(json);
                    
                    //todo - hydrate app, version, status, tenant into account
                    
                    if (includeTests)
                    {
                        byte[] bytes = rs.getBytes("test_data_bin");

                        if (bytes != null)
                        {
                            ncube.setTestData(new String(bytes, "UTF-8"));
                        }
                    }
                    
                    

                    if (rs.next())
                    {
                        throw new IllegalStateException("More than one NCube matching name: " + ncube.getName() + ", app: " + app + ", version: " + version + ", status: " + status );
                    }                    
                }
                
                return ncube;
            }
        }
        catch (IllegalStateException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            String s = "Unable to load nNCube: " + ncubeName + ", app: " + app + ", version: " + version + ", status: " + status + " from database";
            LOG.error(s, e);
            throw new RuntimeException(s, e);
        }
        finally
        {
            commitConnection(connection);
        }
    }

    @Override
    public List<NCube> findAllNCubes(ApplicationID appId)
    {        
        Connection connection = getConnection();

        String app = appId.getApp();
        String version = appId.getVersion();
        String status = appId.getStatus();
        
        try (PreparedStatement stmt = connection.prepareStatement("SELECT cube_value_bin FROM n_cube WHERE app_cd = ? AND sys_effective_dt <= ? AND (sys_expiration_dt IS NULL OR sys_expiration_dt >= ?) AND version_no_cd = ? AND status_cd = ?"))
        {
            List<NCube> ncubes = new ArrayList<>();
            java.sql.Date systemDate = new java.sql.Date(new Date().getTime());

            // TODO: Need to set account column from appId, -if- it exists.  Need to run a check to
            // TODO: see if the column exists, store the result for the entire app life cycle.
            // TODO: If account column does not exist, then account is null.
            
            //TODO: remove date params
            stmt.setString(1, app);
            stmt.setDate(2, systemDate);
            stmt.setDate(3, systemDate);
            stmt.setString(4, version);
            stmt.setString(5, status);
            ResultSet rs = stmt.executeQuery();

            while (rs.next())
            {
                byte[] jsonBytes = rs.getBytes("cube_value_bin");
                String json = new String(jsonBytes, "UTF-8");
                NCube ncube = ncubeFromJson(json);
                ncube.setApplicationID(appId);                
                ncubes.add(ncube);
            }
            
            return ncubes;
        }
        catch (Exception e)
        {
            String s = "Unable to load n-cubes, app: " + app + ", version: " + version + ", status: " + status + " from database";
            LOG.error(s, e);
            throw new RuntimeException(s, e);
        }
        finally
        {
            commitConnection(connection);
        }
     }

    @Override
    public void deleteNCube(ApplicationID appId, String ncubeName)
    {

    }

    @Override
    public void setNCubeConnectionProvider(NCubeConnectionProvider nCubeConnectionProvider)
    {            
        this.nCubeConnectionProvider = nCubeConnectionProvider;
    }
    
    
    //------------------------- private methods ---------------------------------------
    
    private Connection getConnection()
    {
        Object o = nCubeConnectionProvider.beginTransaction();
        
        if (!(o instanceof Connection))
            throw new IllegalStateException("Injected NCubeConnectionProvider must provide a jdbc connection...");
        
        return (Connection)o;
    }
    
    private void commitConnection(Connection connection)
    {
        nCubeConnectionProvider.commitTransaction(connection);
    }
    
    private boolean doesCubeExist(Connection connection, String nCubeName, ApplicationID appId, Date sysDate)
    {
        StringBuilder builder = 
            new StringBuilder("SELECT n_cube_id FROM n_cube WHERE app_cd = ? AND version_no_cd = ?  AND sys_effective_dt <= ? AND (sys_expiration_dt IS NULL OR sys_expiration_dt >= ?)");

        String app = appId.getApp();
        String status = appId.getStatus();
        String version = appId.getVersion();
        
        if (status != null) {
            builder.append(" AND status_cd = ?");
        }

        if (nCubeName != null) {
            builder.append(" AND n_cube_nm = ?");
        }

        java.sql.Date systemDate = new java.sql.Date((sysDate == null) ? new Date().getTime() : sysDate.getTime());

        try (PreparedStatement ps = connection.prepareStatement(builder.toString()))
        {
            // TODO: Need to set account column from appId, -if- it exists.  Need to run a check to
            // TODO: see if the column exists, store the result for the entire app life cycle.
            // TODO: If account column does not exist, then account is null.
            ps.setString(1, app);
            ps.setString(2, version);
            ps.setDate(3, systemDate);
            ps.setDate(4, systemDate);

            int count = 4;
            if (status != null)
            {
                ps.setString(++count, appId.getStatus());
            }

            if (nCubeName != null)
            {
                ps.setString(++count, nCubeName);
            }

            try (ResultSet rs = ps.executeQuery())
            {
                return rs.next();
            }
        }
        catch (Exception e)
        {
            String s = "Error finding cube: " + nCubeName + ", app: " + app + ", version: " + version + ", status: " + status + ", sysDate: " + sysDate + " from database";
            LOG.error(s, e);
            throw new RuntimeException(s, e);
        }
    }

    private NCube ncubeFromJson(String json) throws IOException
    {
        try
        {
            return NCube.fromSimpleJson(json);
        }
        catch (Exception e)
        {
            try
            {   // 2nd attempt in old format - when n-cubes where written by json-io (not the custom writer).
                NCube ncube = (NCube) JsonReader.jsonToJava(json);
                List<Axis> axes = ncube.getAxes();
                for (Axis axis : axes)
                {
                    axis.buildScaffolding();
                }
                ncube.setMetaProperty("sha1", ncube.sha1());
                return ncube;
            }
            catch (Exception e1)
            {
                throw e;
            }
        }
    }
}
