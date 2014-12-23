package com.cedarsoftware.ncube;

import com.cedarsoftware.util.UniqueIdGenerator;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;

/**
 * Created by kpartlow on 12/23/2014.
 */
public class AbstractJdbcTestingDatabaseManager
{
    JdbcConnectionProvider provider;

    AbstractJdbcTestingDatabaseManager(JdbcConnectionProvider p) {
        provider = p;
    }

    public void addCubes(ApplicationID appId, String username, String ... names) throws Exception {
        for (String name : names) {
            URL url = NCubeManager.class.getResource("/" + name);
            File jsonFile = new File(url.getFile());
            try (InputStream in = new FileInputStream(jsonFile)) {
                byte[] data = new byte[(int) jsonFile.length()];
                in.read(data);

                String str = new String(data, "UTF-8");
                // parse cube just to get the name.
                NCube ncube = NCube.fromSimpleJson(str);

                Connection c = provider.getConnection();
                try (PreparedStatement insert = c.prepareStatement("INSERT INTO n_cube (n_cube_id, app_cd, n_cube_nm, cube_value_bin, version_no_cd, create_dt, create_hid, tenant_cd, revision_number, test_data_bin) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"))
                {
                    insert.setLong(1, UniqueIdGenerator.getUniqueId());
                    insert.setString(2, appId.getApp());
                    insert.setString(3, ncube.getName());
                    insert.setBytes(4, data);
                    insert.setString(5, appId.getVersion());
                    java.sql.Date now = new java.sql.Date(System.currentTimeMillis());
                    insert.setDate(6, now);
                    insert.setString(7, username);
                    insert.setString(8, appId.getTenant());
                    // initial adds of cubes like create will be 0.
                    insert.setLong(9, 0);

                    //TODO:  should we also push the notes forward now that createCube is used for updates, etc.?
                    insert.setBytes(10, null);

                    int rowCount = insert.executeUpdate();
                    if (rowCount != 1)
                    {
                        throw new IllegalStateException("error inserting new n-cube: " + ncube.getName() + "', app: " + appId + " (" + rowCount + " rows inserted, should be 1)");
                    }
                }
                finally
                {
                    provider.releaseConnection(c);
                }
            }
        }
    }
}
