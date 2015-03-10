package com.cedarsoftware.ncube

import org.junit.After
import org.junit.Before
import org.junit.Test

import java.lang.reflect.Method

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
        final NCube ncube = NCubeManager.getNCubeFromResource("urlWithNcubeRefs.json")
        NCubeManager.createCube(TestNCubeManager.defaultSnapshotApp, ncube, USER_ID)
        NCubeManager.createCube(TestNCubeManager.defaultSnapshotApp, ncube2, USER_ID)

        // These methods are called more than you think.  Internally, these cube call
        // themselves, and those calls too go through the Advice.
        Advice advice1 = new Advice() {
            String getName()
            {
                return 'timing advice1'
            }

            boolean before(Method method, NCube cube, Map input, Map output)
            {
                output._btime1 = System.nanoTime()
                return true
            }

            void after(Method method, NCube cube, Map input, Map output, Object returnValue)
            {
                output._atime1 = System.nanoTime()
            }
        }

        NCubeManager.addAdvice(TestNCubeManager.defaultSnapshotApp, '*', advice1)

        Advice advice2 = new Advice() {
            String getName()
            {
                return 'timing advice2'
            }

            boolean before(Method method, NCube cube, Map input, Map output)
            {
                output._btime2 = System.nanoTime()
                return true
            }

            void after(Method method, NCube cube, Map input, Map output, Object returnValue)
            {
                output._atime2 = System.nanoTime()
            }
        }

        NCubeManager.addAdvice(TestNCubeManager.defaultSnapshotApp, "*", advice2)

        Map output = [:]
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

        Advice advice1 = new Advice() {
            String getName()
            {
                return 'alpha'
            }

            boolean before(Method method, NCube cube, Map input, Map output)
            {
                output.before = true

                // Could be 4 because of env and user.name being added to input coordinate

                assert input.size() == 2 || input.size() == 3 || input.size() == 4
                boolean ret = true
                if ("foo".equals(method.name))
                {
                    assertEquals("foo", input.method)
                }
                else if ("bar".equals(method.name))
                {
                    assertEquals("bar", input.method)
                }
                else if ("qux".equals(method.name))
                {
                    assertEquals("qux", input.method)
                }
                else if ("qaz".equals(method.name))
                {
                    ret = false
                }
                return ret
            }

            void after(Method method, NCube cube, Map input, Map output, Object returnValue)
            {
                output.after = true
                if ("foo".equals(method.name) && "OH".equals(input.state))
                {
                    assertEquals(2, returnValue)
                }
                else if ("bar".equals(method.name) && "OH".equals(input.state))
                {
                    assertEquals(4, returnValue)
                }
                else if ("qux".equals(method.name) && "TX".equals(input.state))
                {
                    assertEquals(81, returnValue)
                }
            }
        }

        // These methods are called more than you think.  Internally, these cube call
        // themselves, and those calls too go through the Advice.
        NCubeManager.addAdvice(ApplicationID.testAppId, ncube.name + ".*()", advice1)

        Map output = [:]
        Map coord = [method:'foo',state:'OH']
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

        Advice advice1 = new Advice() {
            String getName()
            {
                return 'beta'
            }

            boolean before(Method method, NCube cube, Map input, Map output)
            {
                output.before = true

                boolean ret = true
                if ("foo".equals(method.name))
                {
                    assertEquals("foo", input.method)
                }
                else if ("bar".equals(method.name))
                {
                    output.bar = true
                    assertEquals("bar", input.method)
                }
                else if ("baz".equals(method.name))
                {
                    output.baz = true
                }
                else if ("qux".equals(method.name))
                {
                    assertEquals("qux", input.method)
                }
                else if ("qaz".equals(method.name))
                {
                    ret = false
                }
                ret
            }

            void after(Method method, NCube cube, Map input, Map output, Object returnValue)
            {
                output.after = true
                if ("foo".equals(method.name) && "OH".equals(input.state))
                {
                    assertEquals(2, returnValue)
                }
                else if ("bar".equals(method.name) && "OH".equals(input.state))
                {
                    assertEquals(4, returnValue)
                }
                else if ("qux".equals(method.name) && "TX".equals(input.state))
                {
                    assertEquals(81, returnValue)
                }
            }
        }

        // These methods are called more than you think.  Internally, these cube call
        // themselves, and those calls too go through the Advice.
        NCubeManager.addAdvice(ApplicationID.testAppId, ncube.name + ".ba*()", advice1)

        Map output = [:]
        Map coord = [method:'foo', state:'OH']
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
        Advice advice1 = new Advice() {
            String getName()
            {
                return 'charlie'
            }

            boolean before(Method method, NCube ncube, Map input, Map output)
            {
                output.before = true

                boolean ret = true
                if ("foo".equals(method.name))
                {
                    assertEquals("foo", input.method)
                }
                else if ("bar".equals(method.name))
                {
                    output.bar = true
                    assertEquals("bar", input.method)
                }
                else if ("baz".equals(method.name))
                {
                    output.baz = true
                }
                else if ("qux".equals(method.name))
                {
                    assertEquals("qux", input.method)
                }
                else if ("qaz".equals(method.name))
                {
                    ret = false
                }
                ret
            }

            void after(Method method, NCube ncube, Map input, Map output, Object returnValue)
            {
                output.after = true
                if ("foo".equals(method.name) && "OH".equals(input.state))
                {
                    assertEquals(2, returnValue)
                }
                else if ("bar".equals(method.name) && "OH".equals(input.state))
                {
                    assertEquals(4, returnValue)
                }
                else if ("qux".equals(method.name) && "TX".equals(input.state))
                {
                    assertEquals(81, returnValue)
                }
            }
        }

        // These methods are called more than you think.  Internally, these cube call
        // themselves, and those calls too go through the Advice.
        NCubeManager.addAdvice(ApplicationID.testAppId, "*.ba*()", advice1)

        // Note: advice is added to the manager *ahead* of any cubes being loaded.
        NCube ncube = NCubeManager.getNCubeFromResource("testGroovyMethods.json")

        Map output = [:]
        Map coord = [method:'foo', state:'OH']
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
        Advice advice1 = new Advice() {
            String getName()
            {
                return null
            }

            boolean before(Method method, NCube ncube, Map input, Map output)
            {
                output.before = true
                true
            }

            void after(Method method, NCube ncube, Map input, Map output, Object returnValue)
            {
                output.after = true
            }
        }

        // These methods are called more than you think.  Internally, these cube call
        // themselves, and those calls too go through the Advice.
        NCubeManager.addAdvice(ApplicationID.testAppId, "*.run()", advice1)
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

        Advice advice1 = new Advice() {
            String getName()
            {
                return 'echo'
            }

            boolean before(Method method, NCube cube, Map input, Map output)
            {
                return false
            }

            void after(Method method, NCube cube, Map input, Map output, Object returnValue)
            {
                fail()
            }
        }

        // These methods are called more than you think.  Internally, these cube call
        // themselves, and those calls too go through the Advice.
        NCubeManager.addAdvice(ApplicationID.testAppId, ncube.name + "*", advice1)
        assertNull(ncube.getCell([method:'foo', state:'OH']))
        ncube.clearAdvices()
    }

    @Test
    void testMultiAdvice()
    {
        NCube ncube = NCubeManager.getNCubeFromResource("testGroovyMethods.json")
        NCubeManager.createCube(TestNCubeManager.defaultSnapshotApp, ncube, USER_ID)

        Advice advice1 = new Advice() {
            String getName()
            {
                return 'foxtrot'
            }

            boolean before(Method method, NCube cube, Map input, Map output)
            {
                output._btime1 = System.nanoTime()
                output.advice1before = true
                return true
            }

            void after(Method method, NCube cube, Map input, Map output, Object returnValue)
            {
                output._atime1 = System.nanoTime()
                output.advice1after = true
            }
        }

        // These methods are called more than you think.  Internally, these cube call
        // themselves, and those calls too go through the Advice.
        NCubeManager.addAdvice(TestNCubeManager.defaultSnapshotApp, ncube.name + "*()", advice1)

        Advice advice2 = new Advice() {
            String getName()
            {
                return 'golf'
            }

            boolean before(Method method, NCube cube, Map input, Map output)
            {
                output._btime2 = System.nanoTime()
                output.advice2before = true
                return true
            }

            void after(Method method, NCube cube, Map input, Map output, Object returnValue)
            {
                output._atime2 = System.nanoTime()
                output.advice2after = true
            }
        }

        // These methods are called more than you think.  Internally, these cube call
        // themselves, and those calls too go through the Advice.
        NCubeManager.addAdvice(TestNCubeManager.defaultSnapshotApp, ncube.name + "*()", advice2)

        def output = [:]
        ncube.getCell([method:'foo', state:'OH'], output)

        assert output.advice1before == true
        assert output.advice1after == true
        assert output.advice2before == true
        assert output.advice2after == true

        assert output._atime1 > output._atime2
        assert output._btime1 < output._btime2
        assert output._btime2 < output._atime1
        assert output._btime1 < output._atime1

        ncube.clearAdvices()
    }
}
