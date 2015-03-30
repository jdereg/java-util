package com.cedarsoftware.ncube;

/**
 * Created by ken on 3/30/2015.
 */
public class JdbcPreloadedDatabaseTest extends TestWithPreloadedDatabase {
    @Override
    public TestingDatabaseManager getTestingDatabaseManager() {
        return TestingDatabaseHelper.testingDatabaseManager
    }

    @Override
    public NCubePersister getNCubePersister() {
        return TestingDatabaseHelper.persister
    }
}
