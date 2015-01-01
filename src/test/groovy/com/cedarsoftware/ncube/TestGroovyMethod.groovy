package com.cedarsoftware.ncube

import org.junit.Assert
import org.junit.Test

import java.lang.reflect.Constructor
import java.lang.reflect.Modifier

import static org.junit.Assert.assertEquals

/**
 * Created by kpartlow on 4/10/2014.
 */
public class TestGroovyMethod
{
    @Test
    public void testDefaultConstructorIsPrivateForSerialization() throws Exception
    {
        Class c = GroovyMethod.class;
        Constructor<GroovyMethod> con = c.getDeclaredConstructor()
        Assert.assertEquals Modifier.PRIVATE, con.modifiers & Modifier.PRIVATE
        con.accessible = true
        Assert.assertNotNull con.newInstance()
    }

    @Test
    public void testGetCubeNamesFromTestWhenEmpty()
    {
        Set set = new HashSet()
        GroovyBase.getCubeNamesFromText set, ''
        assertEquals 0, set.size()
    }

    @Test
    public void testGroovyMethod()
    {
        GroovyMethod m = new GroovyMethod('cmd', null)
        assertEquals 'foo', m.getMethodToExecute([input:[method:'foo']])
    }

    @Test
    public void testGetCubeNamesFromTestWithEmptyString()
    {
        GroovyMethod m = new GroovyMethod('cmd', null)
        Set<String> set = [] as Set
        m.getCubeNamesFromCommandText set
        assertEquals 0, set.size()
    }

    @Test
    public void testConstructionState()
    {
        GroovyMethod m = new GroovyMethod('cmd', 'com/foo/not/found/bar.groovy')
        assertEquals 'cmd', m.cmd
        assertEquals 'com/foo/not/found/bar.groovy', m.url
        assertEquals true, m.cacheable
    }

    @Test
    public void testGroovyMethodClearCache() throws Exception
    {
        TestingDatabaseHelper.setupDatabase()
        ApplicationID appId = new ApplicationID(ApplicationID.DEFAULT_TENANT, 'GroovyMethodCP', ApplicationID.DEFAULT_VERSION, ReleaseStatus.SNAPSHOT.name())

        NCube cpCube = NCubeManager.getNCubeFromResource appId, 'sys.classpath.cp1.json'
        NCubeManager.createCube appId, cpCube, TestNCubeManager.USER_ID

        NCube cube = NCubeManager.getNCubeFromResource appId, 'GroovyMethodClassPath1.json'
        NCubeManager.createCube appId, cube, TestNCubeManager.USER_ID

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
    public void testGroovyMethodClearCacheExplicitly() throws Exception
    {
        TestingDatabaseHelper.setupDatabase()
        ApplicationID appId = new ApplicationID(ApplicationID.DEFAULT_TENANT, 'GroovyMethodCP', ApplicationID.DEFAULT_VERSION, ReleaseStatus.SNAPSHOT.name())

        NCube cpCube = NCubeManager.getNCubeFromResource appId, 'sys.classpath.cp1.json'
        NCubeManager.createCube appId, cpCube, TestNCubeManager.USER_ID

        NCube cube = NCubeManager.getNCubeFromResource appId, 'GroovyMethodClassPath1.json'
        NCubeManager.createCube appId, cube, TestNCubeManager.USER_ID

        NCubeManager.clearCache appId
        cube = NCubeManager.getCube appId, 'GroovyMethodClassPath1'

        Object x = cube.getCell([method:'foo'])
        assertEquals 'foo', x

        x = cube.getCell([method:'foo2'])
        assertEquals('foo2', x)

        x = cube.getCell([method:'bar'])
        assertEquals('Bar', x)

        cpCube = NCubeManager.getNCubeFromResource appId, 'sys.classpath.cp2.json'
        NCubeManager.updateCube appId, cpCube, TestNCubeManager.USER_ID

        NCubeManager.clearCache appId
        cube = NCubeManager.getCube appId, 'GroovyMethodClassPath1'

        x = cube.getCell([method:'foo'])
        assertEquals 'boo', x

        x = cube.getCell([method:'foo2'])
        assertEquals 'boo2', x

        x = cube.getCell([method:'bar'])
        assertEquals('far', x)
        TestingDatabaseHelper.tearDownDatabase()
    }
}
