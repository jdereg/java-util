package com.cedarsoftware.ncube;

/**
 * Created by kpartlow on 11/2/2014.
 */
public class TestJdbcPersister extends AbstractPersisterTest
{
    public NCubePersister getPersister(int db) throws Exception
    {
        return TestingDatabaseHelper.getJdbcPersister(db);
    }

}
