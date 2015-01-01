package com.cedarsoftware.ncube

import org.junit.After
import org.junit.Before
import org.junit.Test

/**
 * Created by kpartlow on 1/20/14.
 */
public class TestDynamicProperties
{
    @Before
    public void setUp() throws Exception
    {
        TestingDatabaseHelper.setupDatabase()
    }

    @After
    public void tearDown() throws Exception
    {
        TestingDatabaseHelper.tearDownDatabase()
    }

    public static Map getCprMap(String prop, String bu, String env)
    {
        return [cprName:prop, env:env, bu:bu]
    }

    public static String getCellAsString(NCube ncube, Map input)
    {
        return (String) ncube.getCell(input)
    }

    @Test
    public void testCprStyleProperties()
    {
        NCube cpr = NCubeManager.getNCubeFromResource 'cpr.json'

        assert 'CPR' == cpr.name

        String ret = getCellAsString cpr, getCprMap('cdn-base', 'Biz1', 'SANDBOX')
        assert 'res://pages' == ret

        ret = getCellAsString cpr, getCprMap('cdn-base', 'Biz1', 'DEV')
        assert 'res://pages' == ret

        ret = getCellAsString cpr, getCprMap('cdn-layout', 'Biz1', 'SANDBOX')
        assert 'Biz1/SANDBOX' == ret

        ret = getCellAsString cpr, getCprMap('cdn-layout', 'Biz1', 'DEV')
        assert 'Biz1/DEV' == ret

        ret = getCellAsString cpr, getCprMap('cdn-layout', 'Biz2', 'SANDBOX')
        assert 'Biz2/SANDBOX' == ret

        ret = getCellAsString cpr, getCprMap('cdn-layout', 'Biz2', 'DEV')
        assert 'Biz2/DEV' == ret

        ret = getCellAsString cpr, getCprMap('cdn-url', 'Biz1', 'SANDBOX')
        assert 'res://pages/Biz1/SANDBOX' == ret

        ret = getCellAsString cpr, getCprMap('cdn-url', 'Biz1', 'DEV')
        assert 'res://pages/Biz1/DEV' == ret

        ret = getCellAsString cpr, getCprMap('cdn-url', 'Biz2', 'SANDBOX')
        assert 'res://pages/Biz2/SANDBOX' == ret

        ret = getCellAsString cpr, getCprMap('cdn-url', 'Biz2', 'DEV')
        assert 'res://pages/Biz2/DEV' == ret
    }
}
