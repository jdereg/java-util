package com.cedarsoftware.ncube;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Created by kpartlow on 12/23/2014.
 */
public class TestNCubeNewWay
{

    @Before
    public void setup() throws Exception {
        TestingDatabaseHelper.getTestingDatabaseManager().setUp();
        NCubeManager.setNCubePersister(TestingDatabaseHelper.getPersister());
    }

    @After
    public void tearDown() throws Exception {
        TestingDatabaseHelper.getTestingDatabaseManager().tearDown();
        NCubeManager.clearCache();
    }

    public void addCubesToDbManually() {

    }

    @Test
    public void test() {

    }

}
