package com.cedarsoftware.ncube;

import com.cedarsoftware.util.UniqueIdGenerator;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Date;
import java.util.List;

public class NCubeJdbcPersister implements NCubePersister
{
    private static final Log LOG = LogFactory.getLog(NCubeJdbcPersister.class);
    
    private NCubeConnectionProvider nCubeConnectionProvider;

    @Override
    public void saveNCube(ApplicationID appId, NCube ncube)
    { 
        Connection connection = getConnection();
        
        if (doesCubeExist(connection, ncube.getName(), appId, null))
        {
            throw new IllegalStateException("NCube '" + ncube.getName() + "' (" + appId.getApp() + " " + appId.getVersion() + ") already exists.");
        }
        
        String app = appId.getApp();
        String version = appId.getVersion();
        

        try (PreparedStatement insert = connection.prepareStatement("INSERT INTO n_cube (n_cube_id, app_cd, n_cube_nm, cube_value_bin, version_no_cd, create_dt, sys_effective_dt) VALUES (?, ?, ?, ?, ?, ?, ?)"))
        {
            // TODO: Need to set account column from appId, -if- it exists.  Need to run a check to
            // TODO: see if the column exists, store the result for the entire app life cycle.
            // TODO: If account column does not exist, then account is null.
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
    }

    @Override
    public void updateNCube(ApplicationID appId, NCube ncube)
    {

    }

    @Override
    public NCube findNCube(ApplicationID appId, String ncubeName)
    {
        return null;
    }

    @Override
    public List<NCube> findAllNCubes(ApplicationID appId)
    {
        return null;
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
}
