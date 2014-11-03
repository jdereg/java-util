package com.cedarsoftware.ncube;

import org.hsqldb.jdbc.JDBCDataSource;

/**
 * Created by kpartlow on 11/2/2014.
 */
public class TestDataSourcePersister extends AbstractPersisterTest
{
    public NCubePersister getPersister(int db) throws Exception
    {
        JDBCDataSource source = new JDBCDataSource();
        source.setUrl("jdbc:hsqldb:mem:testdb");
        source.setUser("SA");
        source.setPassword("");

        return new JdbcPersister(source);
    }
}
