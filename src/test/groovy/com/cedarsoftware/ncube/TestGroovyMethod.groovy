package com.cedarsoftware.ncube

import org.junit.Assert
import org.junit.Test

import java.lang.reflect.Constructor
import java.lang.reflect.Modifier

import static org.junit.Assert.assertEquals

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
class TestGroovyMethod
{
    @Test
    void testDefaultConstructorIsPrivateForSerialization()
    {
        Class c = GroovyMethod.class;
        Constructor<GroovyMethod> con = c.getDeclaredConstructor()
        Assert.assertEquals Modifier.PRIVATE, con.modifiers & Modifier.PRIVATE
        con.accessible = true
        Assert.assertNotNull con.newInstance()
    }

    @Test
    void testGetCubeNamesFromTestWhenEmpty()
    {
        Set set = new HashSet()
        GroovyBase.getCubeNamesFromText set, ''
        assertEquals 0, set.size()
    }

    @Test
    void testGroovyMethod()
    {
        GroovyMethod m = new GroovyMethod('cmd', null, false)
        assertEquals 'foo', m.getMethodToExecute([input:[method:'foo']])
    }

    @Test
    void testGetCubeNamesFromTestWithEmptyString()
    {
        GroovyMethod m = new GroovyMethod('cmd', null, false)
        Set<String> set = [] as Set
        m.getCubeNamesFromCommandText set
        assertEquals 0, set.size()
    }

    @Test
    void testConstructionState()
    {
        GroovyMethod m = new GroovyMethod('cmd', 'com/foo/not/found/bar.groovy', false)
        assertEquals 'cmd', m.cmd
        assertEquals 'com/foo/not/found/bar.groovy', m.url
        assertEquals false, m.cacheable

        m = new GroovyMethod('cmd', 'com/foo/not/found/bar.groovy', true)
        assertEquals 'cmd', m.cmd
        assertEquals 'com/foo/not/found/bar.groovy', m.url
        assertEquals true, m.cacheable
    }

    @Test
    void testGroovyMethodClearCache()
    {
        TestingDatabaseHelper.setupDatabase()
        ApplicationID appId = new ApplicationID(ApplicationID.DEFAULT_TENANT, 'GroovyMethodCP', ApplicationID.DEFAULT_VERSION, ApplicationID.DEFAULT_STATUS, ApplicationID.TEST_BRANCH)

        NCube cpCube = NCubeManager.getNCubeFromResource appId, 'sys.classpath.cp1.json'
        NCubeManager.updateCube appId, cpCube, TestNCubeManager.USER_ID

        NCube cube = NCubeManager.getNCubeFromResource appId, 'GroovyMethodClassPath1.json'
        NCubeManager.updateCube appId, cube, TestNCubeManager.USER_ID

        NCubeManager.clearCache()
        cube = NCubeManager.getCube appId, 'GroovyMethodClassPath1'

        Object x = cube.getCell([method:'foo'])
        assertEquals 'foo', x

        x = cube.getCell([method:'foo2'])
        assertEquals 'foo2', x

        x = cube.getCell([method:'bar'])
        assertEquals 'Bar', x

        cpCube = NCubeManager.getNCubeFromResource appId, 'sys.classpath.cp2.json'
        NCubeManager.updateCube appId, cpCube, TestNCubeManager.USER_ID

        NCubeManager.clearCache()
        cube = NCubeManager.getCube appId, 'GroovyMethodClassPath1'

        x = cube.getCell([method:'foo'])
        assertEquals 'boo', x

        x = cube.getCell([method:'foo2'])
        assertEquals 'boo2', x

        x = cube.getCell([method:'bar'])
        assertEquals 'far', x
        TestingDatabaseHelper.tearDownDatabase()
    }

    @Test
    void testGroovyMethodClearCacheExplicitly()
    {
        TestingDatabaseHelper.setupDatabase()
        ApplicationID appId = new ApplicationID(ApplicationID.DEFAULT_TENANT, 'GroovyMethodCP', ApplicationID.DEFAULT_VERSION, ReleaseStatus.SNAPSHOT.name(), ApplicationID.TEST_BRANCH)

        NCube cpCube = NCubeManager.getNCubeFromResource(appId, 'sys.classpath.cp1.json')
        NCubeManager.updateCube(appId, cpCube, TestNCubeManager.USER_ID)

        NCube cube = NCubeManager.getNCubeFromResource(appId, 'GroovyMethodClassPath1.json')
        NCubeManager.updateCube(appId, cube, TestNCubeManager.USER_ID)

        NCubeManager.clearCache(appId)
        cube = NCubeManager.getCube(appId, 'GroovyMethodClassPath1')

        Object x = cube.getCell([method:'foo'])
        assertEquals 'foo', x

        x = cube.getCell([method:'foo2'])
        assertEquals('foo2', x)

        x = cube.getCell([method:'bar'])
        assertEquals('Bar', x)

        cpCube = NCubeManager.getNCubeFromResource(appId, 'sys.classpath.cp2.json')
        NCubeManager.updateCube(appId, cpCube, TestNCubeManager.USER_ID)

        NCubeManager.clearCache(appId)
        cube = NCubeManager.getCube(appId, 'GroovyMethodClassPath1')

        x = cube.getCell([method:'foo'])
        assertEquals 'boo', x

        x = cube.getCell([method:'foo2'])
        assertEquals 'boo2', x

        x = cube.getCell([method:'bar'])
        assertEquals('far', x)
        TestingDatabaseHelper.tearDownDatabase()
    }
}
