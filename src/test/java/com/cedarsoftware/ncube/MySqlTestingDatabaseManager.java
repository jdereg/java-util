package com.cedarsoftware.ncube;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Created by kpartlow on 10/28/2014.
 */
public class MySqlTestingDatabaseManager
{
    public void setUp(Connection c) throws SQLException {
        try (Statement s = c.createStatement())
        {
            s.execute("        drop table if exists `ncube`.n_cube;\n" +
                    "        CREATE TABLE `ncube`.n_cube (\n" +
                    "            n_cube_id bigint NOT NULL,\n" +
                    "            n_cube_nm varchar(100) NOT NULL,\n" +
                    "            tenant_cd char(10),\n" +
                    "            cube_value_bin longtext,\n" +
                    "            create_dt date NOT NULL,\n" +
                    "            update_dt date DEFAULT NULL,\n" +
                    "            create_hid varchar(20),\n" +
                    "            update_hid varchar(20),\n" +
                    "            version_no_cd varchar(16) NOT NULL,\n" +
                    "        status_cd varchar(16) DEFAULT 'SNAPSHOT' NOT NULL,\n" +
                    "        sys_effective_dt date,\n" +
                    "        sys_expiration_dt date,\n" +
                    "        business_effective_dt date,\n" +
                    "        business_expiration_dt date,\n" +
                    "        app_cd varchar(20),\n" +
                    "            test_data_bin longtext,\n" +
                    "            notes_bin longtext,\n" +
                    "            revision_number bigint,\n" +
                    "            PRIMARY KEY (n_cube_id),\n" +
                    "            UNIQUE (tenant_cd, n_cube_nm, version_no_cd, app_cd, status_cd, revision_number)\n" +
                    "        );\n" +
                    "        drop trigger if exists `ncube`.sysEffDateTrigger;\n" +
                    "        DELIMITER ;;\n" +
                    "        CREATE trigger `ncube`.sysEffDateTrigger BEFORE INSERT ON `ncube`.n_cube\n" +
                    "        FOR EACH ROW\n" +
                    "            BEGIN\n" +
                    "        SET NEW.sys_effective_dt = NOW();\n" +
                    "        END ;;\n" +
                    "        DELIMITER ;\n");
        }
    }

    public void tearDown(Connection c) throws SQLException {
        // don't accidentally erase your MySql database.
    }
}
