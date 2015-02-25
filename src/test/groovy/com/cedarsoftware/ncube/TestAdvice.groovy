package com.cedarsoftware.ncube

import org.junit.After
import org.junit.Before
import org.junit.Test

import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertFalse
import static org.junit.Assert.assertNull
import static org.junit.Assert.fail

/**
 * NCube Advice Tests (Advice often used for security annotations on Groovy Methods / Expressions)
 *
 * @author John DeRegnaucourt (jdereg@gmail.com)
 *         <br/>
 *         Copyright (c) Cedar Software LLC
 *         <br/><br/>
 *         Licensed under the Apache License, Version 2.0 (the "License")
 *         you may not use this file except in compliance with the License.
 *         You may obtain a copy of the License at
 *         <br/><br/>
 *         http://www.apache.org/licenses/LICENSE-2.0
 *         <br/><br/>
 *         Unless required by applicable law or agreed to in writing, software
 *         distributed under the License is distributed on an "AS IS" BASIS,
 *         WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *         See the License for the specific language governing permissions and
 *         limitations under the License.
 */
class TestAdvice
{
    static final String USER_ID = "jdirt";

    def alpha = { 'alpha' }
    def beta = { 'beta' }
    def advice1 = [getName:alpha,before:null, after:null]
    def advice2 = [getName:beta,before:null, after:null]

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

    @Test
    void testExpression()
    {
        NCube ncube2 = NCubeManager.getNCubeFromResource("urlPieces.json")
        NCube ncube = NCubeManager.getNCubeFromResource("urlWithNcubeRefs.json")
        NCubeManager.createCube(TestNCubeManager.defaultSnapshotApp, ncube, USER_ID)
        NCubeManager.createCube(TestNCubeManager.defaultSnapshotApp, ncube2, USER_ID)

        // These methods are called more than you think.  Internally, these cube call
        // themselves, and those calls too go through the Advice.

        advice1.before = {  method, cube, input, output ->
                    output.put("_btime1", System.nanoTime())
                    true }

        advice1.after = { method, cube, input, output, returnVal ->  output.put("_atime1", System.nanoTime()) }

        NCubeManager.addAdvice(TestNCubeManager.defaultSnapshotApp, '*', advice1 as Advice)

        advice2.before = { method, cube, input, output ->
                    output.put("_btime2", System.nanoTime())
                    true  }

        advice2.after = { method, cube, input, output, returnVal ->  output.put("_atime2", System.nanoTime())  }

        NCubeManager.addAdvice(TestNCubeManager.defaultSnapshotApp, "*", advice2 as Advice)

        def output = [:]
        ncube.getCell([env_level:'local', protocol:'http',content:'95'], output)

        assert output._atime1 > output._atime2
        assert output._btime1 < output._btime2
        assert output._btime2 < output._atime1
        assert output._btime1 < output._atime1
    }

    @Test
    void testAdvice() throws Exception
    {
        NCube ncube = NCubeManager.getNCubeFromResource("testGroovyMethods.json")

        advice1.before = {  method, cube, input, output ->
            output.put("before", true)

            // Could be 4 because of env and user.name being added to input coordinate

            assert input.size() == 2 || input.size() == 3 || input.size() == 4
            boolean ret = true
            if ("foo".equals(method.getName()))
            {
                assertEquals("foo", input.get("method"))
            }
            else if ("bar".equals(method.getName()))
            {
                assertEquals("bar", input.get("method"))
            }
            else if ("qux".equals(method.getName()))
            {
                assertEquals("qux", input.get("method"))
            }
            else if ("qaz".equals(method.getName()))
            {
                ret = false
            }
            ret
        }

        advice1.after = { method, cube, input, output, returnVal ->
            output.put("after", true)
            if ("foo".equals(method.getName()) && "OH".equals(input.get("state")))
            {
                assertEquals(2, returnVal)
            }
            else if ("bar".equals(method.getName()) && "OH".equals(input.get("state")))
            {
                assertEquals(4, returnVal)
            }
            else if ("qux".equals(method.getName()) && "TX".equals(input.get("state")))
            {
                assertEquals(81, returnVal)
            }
        }

        // These methods are called more than you think.  Internally, these cube call
        // themselves, and those calls too go through the Advice.
        NCubeManager.addAdvice(ApplicationID.defaultAppId, ncube.name + ".*()", advice1 as Advice)

        def output = [:]
        def coord = [method:'foo',state:'OH']
        ncube.getCell(coord, output)
        assert output.containsKey("before")
        assert output.containsKey("after")

        output.clear()
        coord.state = "OH"
        coord.method = "bar"
        ncube.getCell(coord, output)
        assert output.containsKey("before")
        assert output.containsKey("after")

        output.clear()
        coord.state = "TX"
        coord.method = "qux"
        ncube.getCell(coord, output)
        assert output.containsKey("before")
        assert output.containsKey("after")
    }

    @Test
    void testAdviceSubsetMatching() throws Exception
    {
        NCube ncube = NCubeManager.getNCubeFromResource("testGroovyMethods.json")

        advice1.before = {  method, cube, input, output ->
            output.put("before", true)

            boolean ret = true
            if ("foo".equals(method.getName()))
            {
                assertEquals("foo", input.get("method"))
            }
            else if ("bar".equals(method.getName()))
            {
                output.put("bar", true)
                assertEquals("bar", input.get("method"))
            }
            else if ("baz".equals(method.getName()))
            {
                output.put("baz", true)
            }
            else if ("qux".equals(method.getName()))
            {
                assertEquals("qux", input.get("method"))
            }
            else if ("qaz".equals(method.getName()))
            {
                ret = false
            }
            ret
        }

        advice1.after = { method, cube, input, output, returnVal ->
            output.put("after", true)
            if ("foo".equals(method.getName()) && "OH".equals(input.get("state")))
            {
                assertEquals(2, returnVal)
            }
            else if ("bar".equals(method.getName()) && "OH".equals(input.get("state")))
            {
                assertEquals(4, returnVal)
            }
            else if ("qux".equals(method.getName()) && "TX".equals(input.get("state")))
            {
                assertEquals(81, returnVal)
            }
        }

        // These methods are called more than you think.  Internally, these cube call
        // themselves, and those calls too go through the Advice.
        NCubeManager.addAdvice(ApplicationID.defaultAppId, ncube.name + ".ba*()", advice1 as Advice)

        def output = [:]
        def coord = [method:'foo', state:'OH']
        ncube.getCell(coord, output)
        assertFalse(output.containsKey("before"))
        assertFalse(output.containsKey("after"))

        output.clear()
        coord.state = "OH"
        coord.method = "bar"
        ncube.getCell(coord, output)
        assert output.containsKey("before")
        assert output.containsKey("after")
        assert output.containsKey("bar")

        output.clear()
        coord.state = "OH"
        coord.method = "baz"
        ncube.getCell(coord, output)
        assert output.containsKey("before")
        assert output.containsKey("after")
        assert output.containsKey("baz")

        output.clear()
        coord.state = "TX"
        coord.method = "qux"
        ncube.getCell(coord, output)
        // Controller method Qux calls baz via getCell() which then is intercepted at sets the output keys before, after.
        assert output.containsKey("before")
        assert output.containsKey("after")

        output.clear()
        coord.state = "OH"
        coord.method = "qux"
        ncube.getCell(coord, output)
        // Controller method Qux calls baz directly which is NOT intercepted
        assertFalse(output.containsKey("before"))
        assertFalse(output.containsKey("after"))

        ncube.clearAdvices()
    }

    @Test
    void testAdviceSubsetMatchingLateLoad()
    {
        advice1.before = {  method, cube, input, output ->
            output.put("before", true)

            boolean ret = true
            if ("foo".equals(method.getName()))
            {
                assertEquals("foo", input.get("method"))
            }
            else if ("bar".equals(method.getName()))
            {
                output.put("bar", true)
                assertEquals("bar", input.get("method"))
            }
            else if ("baz".equals(method.getName()))
            {
                output.put("baz", true)
            }
            else if ("qux".equals(method.getName()))
            {
                assertEquals("qux", input.get("method"))
            }
            else if ("qaz".equals(method.getName()))
            {
                ret = false
            }
            ret
        }

        advice1.after = { method, cube, input, output, returnVal ->
            output.put("after", true)
            if ("foo".equals(method.getName()) && "OH".equals(input.get("state")))
            {
                assertEquals(2, returnVal)
            }
            else if ("bar".equals(method.getName()) && "OH".equals(input.get("state")))
            {
                assertEquals(4, returnVal)
            }
            else if ("qux".equals(method.getName()) && "TX".equals(input.get("state")))
            {
                assertEquals(81, returnVal)
            }
        }

        // These methods are called more than you think.  Internally, these cube call
        // themselves, and those calls too go through the Advice.
        NCubeManager.addAdvice(ApplicationID.defaultAppId, "*.ba*()", advice1 as Advice)

        // Note: advice is added to the manager *ahead* of any cubes being loaded.
        NCube ncube = NCubeManager.getNCubeFromResource("testGroovyMethods.json")

        def output = [:]
        def coord = [method:'foo', state:'OH']
        ncube.getCell(coord, output)
        assertFalse(output.containsKey("before"))
        assertFalse(output.containsKey("after"))

        output.clear()
        coord.state = "OH"
        coord.method = "bar"
        ncube.getCell(coord, output)
        assert output.containsKey("before")
        assert output.containsKey("after")
        assert output.containsKey("bar")

        output.clear()
        coord.state = "OH"
        coord.method = "baz"
        ncube.getCell(coord, output)
        assert output.containsKey("before")
        assert output.containsKey("after")
        assert output.containsKey("baz")

        output.clear()
        coord.state = "TX"
        coord.method = "qux"
        ncube.getCell(coord, output)
        // Controller method Qux calls baz via getCell() which then is intercepted at sets the output keys before, after.
        assert output.containsKey("before")
        assert output.containsKey("after")

        output.clear()
        coord.state = "OH"
        coord.method = "qux"
        ncube.getCell(coord, output)
        // Controller method Qux calls baz directly which is NOT intercepted
        assertFalse(output.containsKey("before"))
        assertFalse(output.containsKey("after"))
    }

    @Test
    void testAdviceSubsetMatchingLateLoadExpressions()
    {
        advice1.before = {  method, cube, input, output ->
            output.put("before", true)
            true
        }

        advice1.after = { method, cube, input, output, returnVal ->  output.put("after", true) }

        // These methods are called more than you think.  Internally, these cube call
        // themselves, and those calls too go through the Advice.
        NCubeManager.addAdvice(ApplicationID.defaultAppId, "*.run()", advice1 as Advice)
        NCube ncube = NCubeManager.getNCubeFromResource("debugExp.json")

        def output = [:]
        ncube.getCell([Age:10], output)

        // This advice was placed on all expressions ("exp") in the loaded cube.
        // This advice was placed into the Manager first, and then onto the cube later.
        assert output.containsKey("before")
        assert output.containsKey("after")
    }

    @Test
    void testAdviceNoCallForward()
    {
        NCube ncube = NCubeManager.getNCubeFromResource("testGroovyMethods.json")

        advice1.before = {  method, cube, input, output ->
            false
        }

        advice1.after = { method, cube, input, output, returnVal ->  fail() }

        // These methods are called more than you think.  Internally, these cube call
        // themselves, and those calls too go through the Advice.
        NCubeManager.addAdvice(ApplicationID.defaultAppId, ncube.name + "*", advice1 as Advice)
        assertNull(ncube.getCell([method:'foo', state:'OH']))
        ncube.clearAdvices()
    }

    @Test
    void testMultiAdvice()
    {
        NCube ncube = NCubeManager.getNCubeFromResource("testGroovyMethods.json")
        NCubeManager.createCube(TestNCubeManager.defaultSnapshotApp, ncube, USER_ID)

        advice1.before = {  method, cube, input, output ->
            output.put("_btime1", System.nanoTime())
            true
        }

        advice1.after = { method, cube, input, output, returnVal ->  output.put("_atime1", System.nanoTime()) }

        // These methods are called more than you think.  Internally, these cube call
        // themselves, and those calls too go through the Advice.
        NCubeManager.addAdvice(TestNCubeManager.defaultSnapshotApp, ncube.name + "*()", advice1 as Advice)

        advice2.before = {  method, cube, input, output ->
            output.put("_btime2", System.nanoTime())
            true
        }

        advice2.after = { method, cube, input, output, returnVal ->  output.put("_atime2", System.nanoTime()) }

        // These methods are called more than you think.  Internally, these cube call
        // themselves, and those calls too go through the Advice.
        NCubeManager.addAdvice(TestNCubeManager.defaultSnapshotApp, ncube.name + "*()", advice2 as Advice)

        def output = [:]
        ncube.getCell([method:'foo', state:'OH'], output)
        assert output.containsKey("_atime1")
        assert output.containsKey("_btime1")
        assert output.containsKey("_atime2")
        assert output.containsKey("_btime2")
        assert output._atime1 > output._atime2
        assert output._btime1 < output._btime2
        assert output._btime2 < output._atime1
        assert output._btime1 < output._atime1

        ncube.clearAdvices()
    }
}
