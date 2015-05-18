package com.cedarsoftware.ncube

import org.junit.After
import org.junit.Before
import org.junit.Test

/**
 * @author John DeRegnaucourt (jdereg@gmail.com)
 *         <br/>
 *         Copyright (c) Cedar Software LLC
 *         <br/><br/>
 *         Licensed under the Apache License, Version 2.0 (the 'License')
 *         you may not use this file except in compliance with the License.
 *         You may obtain a copy of the License at
 *         <br/><br/>
 *         http://www.apache.org/licenses/LICENSE-2.0
 *         <br/><br/>
 *         Unless required by applicable law or agreed to in writing, software
 *         distributed under the License is distributed on an 'AS IS' BASIS,
 *         WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *         See the License for the specific language governing permissions and
 *         limitations under the License.
 */
class TestDynamicProperties
{
    @Before
    public void setUp()
    {
        TestingDatabaseHelper.setupDatabase()
    }

    @After
    public void tearDown()
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
    void testCprStyleProperties()
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
