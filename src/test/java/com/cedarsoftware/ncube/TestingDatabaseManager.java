package com.cedarsoftware.ncube;

import java.sql.SQLException;

/**
 * Created by kpartlow on 10/28/2014.
 */
public interface TestingDatabaseManager
{
    public void setUp() throws SQLException;
    public void tearDown() throws SQLException;

    public void addCubes(ApplicationID appId, String username, String ... names) throws Exception;
}
