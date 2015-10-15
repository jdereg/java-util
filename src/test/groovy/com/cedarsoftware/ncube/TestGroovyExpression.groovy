package com.cedarsoftware.ncube

import ncube.grv.exp.NCubeGroovyExpression
import org.junit.Assert
import org.junit.Test

import java.lang.reflect.Constructor
import java.lang.reflect.Modifier

import static org.junit.Assert.fail

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
class TestGroovyExpression
{
    @Test
    void testDefaultConstructorIsPrivateForSerialization()
    {
        Class c = GroovyExpression.class
        Constructor<GroovyExpression> con = c.getDeclaredConstructor()
        Assert.assertEquals Modifier.PRIVATE, con.modifiers & Modifier.PRIVATE
        con.accessible = true
        Assert.assertNotNull con.newInstance()
    }

    @Test
    void testCompilerErrorOutput()
    {
        NCube ncube = NCubeManager.getNCubeFromResource("GroovyExpCompileError.json")
        Map coord = [state: 'OH']
        Object x = ncube.getCell(coord)
        assert 'Hello, Ohio' == x
        coord.state = 'TX'
        try
        {
            ncube.getCell(coord)
            fail();
        }
        catch (RuntimeException e)
        {
            String msg = e.getCause().getCause().message
            assert msg.toLowerCase().contains('no such property')
            assert msg.toLowerCase().contains('hi8')
        }
    }

    @Test
    void testRegexSubstitutions()
    {
        NCube ncube = new NCube('test')
        NCubeManager.addCube(ApplicationID.testAppId, ncube)
        Axis axis = new Axis('day', AxisType.DISCRETE, AxisValueType.STRING, false)
        axis.addColumn('mon')
        axis.addColumn('tue')
        axis.addColumn('wed')
        axis.addColumn('thu')
        axis.addColumn('fri')
        axis.addColumn('sat')
        axis.addColumn('sun')

        ncube.addAxis(axis)
        ncube.setApplicationID(ApplicationID.testAppId)

        ncube.setCell(1, [day: 'mon'])
        ncube.setCell(2, [day: 'tue'])
        ncube.setCell(3, [day: 'wed'])
        ncube.setCell(4, [day: 'thu'])
        ncube.setCell(5, [day: 'fri'])
        ncube.setCell(6, [day: 'sat'])
        ncube.setCell(7, [day: 'sun'])

        parse(ncube, '''
int ret = @[day:'mon']
return ret
''', 1)

        parse(ncube, '''
int ret = @[day:'mon'];
return ret
''', 1)

        parse(ncube, '''
int ret = $[day:'tue']
return ret
''', 2)

        parse(ncube, '''
int ret = $[day:'tue'];
return ret
''', 2)

        parse(ncube, '''
int ret = @test[day:'wed']
return ret
''', 3)

        parse(ncube, '''
int ret = @test[day:'wed'];
return ret
''', 3)

        parse(ncube, '''
int ret = $test[day:'thu']
return ret
''', 4)

        parse(ncube, '''
int ret = $test[day:'thu'];
return ret
''', 4)

        // Map variable passed in as coordinate
        parse(ncube, '''
Map inp = [day:'mon']
int ret = @(inp)
return ret
''', 1)

        parse(ncube, '''
Map inp = [day:'mon']
int ret = @(inp);
return ret
''', 1)

        parse(ncube, '''
Map inp = [day:'tue']
int ret = $(inp)
return ret
''', 2)

        parse(ncube, '''
Map inp = [day:'tue']
int ret = $(inp);
return ret
''', 2)

        parse(ncube, '''
Map inp = [day:'wed']
int ret = @test(inp)
return ret
''', 3)

        parse(ncube, '''
Map inp = [day:'wed']
int ret = @test(inp);
return ret
''', 3)

        parse(ncube, '''
Map inp = [day:'thu']
int ret = $test(inp)
return ret
''', 4)

        parse(ncube, '''
Map inp = [day:'thu']
int ret = $test(inp);
return ret
''', 4)
    }

    private void parse(NCube ncube, String cmd, int val)
    {
        GroovyExpression exp = new GroovyExpression(cmd, null, false)
        Map ctx = [input: [:], output: [:], ncube: ncube]
        assert exp.execute(ctx) == val
    }

    @Test
    void testCachedExpressionClassIsGarbageCollected()
    {
        NCube ncube = new NCube('test')
        NCubeManager.addCube(ApplicationID.testAppId, ncube)
        Axis axis = new Axis('day', AxisType.DISCRETE, AxisValueType.STRING, false)
        axis.addColumn('mon')
        axis.addColumn('tue')

        ncube.addAxis(axis)
        ncube.setApplicationID(ApplicationID.testAppId)

        ncube.setCell(new GroovyExpression("return 'hello'", null, false), [day:'mon'])
        assert 'hello' == ncube.getCell([day:'mon'])
        ncube.setCell(new GroovyExpression("return 'world'", null, true), [day:'tue'])
        assert 'world' == ncube.getCell([day:'tue'])

        assert 'hello' == ncube.getCell([day:'mon'])
        assert 'world' == ncube.getCell([day:'tue'])

        GroovyExpression exp = ncube.getCellByIdNoExecute(ncube.getCoordinateKey([day:'mon']))
        assert exp.cmd == "return 'hello'"
        assert exp.cacheable == false
        assert exp.getRunnableCode() != null
        assert NCubeGroovyExpression.class.isAssignableFrom(exp.getRunnableCode())

        exp = ncube.getCellByIdNoExecute(ncube.getCoordinateKey([day:'tue']))
        assert exp.cmd == "return 'world'"
        assert exp.cacheable == true
        assert exp.getRunnableCode() == null
    }
}