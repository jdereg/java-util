package com.cedarsoftware.ncube

import groovy.transform.CompileStatic;

/**
 * Created by ken on 3/30/2015.
 */
@CompileStatic
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
