package com.cedarsoftware.ncube;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * NCube Advice Tests (Advice often used for security annotations on Groovy Methods / Expressions)
 *
 * @author John DeRegnaucourt (jdereg@gmail.com)
 *         <br/>
 *         Copyright (c) Cedar Software LLC
 *         <br/><br/>
 *         Licensed under the Apache License, Version 2.0 (the "License");
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
public class TestAdvice
{
    static final String USER_ID = "jdirt";

    @Before
    public void setUp() throws Exception
    {
        TestingDatabaseHelper.setupDatabase();
    }

    @After
    public void tearDown() throws Exception
    {
        TestingDatabaseHelper.tearDownDatabase();
    }

    @Test
    public void testExpression()
    {
        NCube ncube2 = NCubeManager.getNCubeFromResource("urlPieces.json");
        NCube ncube = NCubeManager.getNCubeFromResource("urlWithNcubeRefs.json");
        NCubeManager.createCube(TestNCubeManager.defaultSnapshotApp, ncube, USER_ID);
        NCubeManager.createCube(TestNCubeManager.defaultSnapshotApp, ncube2, USER_ID);

        // These methods are called more than you think.  Internally, these cube call
        // themselves, and those calls too go through the Advice.
        NCubeManager.addAdvice(TestNCubeManager.defaultSnapshotApp, "*", new Advice()
        {
            public String getName()
            {
                return "alpha";
            }

            public boolean before(Method method, NCube ncube, Map input, Map output)
            {
                output.put("_btime1", System.nanoTime());
                return true;
            }

            public void after(Method method, NCube ncube, Map input, Map output, Object returnValue)
            {
                output.put("_atime1", System.nanoTime());
            }
        });

        // These methods are called more than you think.  Internally, these cube call
        // themselves, and those calls too go through the Advice.
        NCubeManager.addAdvice(TestNCubeManager.defaultSnapshotApp, "*", new Advice()
        {
            public String getName()
            {
                return "beta";
            }

            public boolean before(Method method, NCube ncube, Map input, Map output)
            {
                output.put("_btime2", System.nanoTime());
                return true;
            }

            public void after(Method method, NCube ncube, Map input, Map output, Object returnValue)
            {
                output.put("_atime2", System.nanoTime());
            }
        });

        Map coord = new HashMap();
        Map output = new HashMap();
        coord.put("env_level", "local");
        coord.put("protocol", "http");
        coord.put("content", "95");
        ncube.getCell(coord, output);

        assertTrue((Long) output.get("_atime1") > (Long) output.get("_atime2"));
        assertTrue((Long) output.get("_btime1") < (Long) output.get("_btime2"));
        assertTrue((Long) output.get("_btime2") < (Long) output.get("_atime1"));
        assertTrue((Long) output.get("_btime1") < (Long) output.get("_atime1"));
    }

    @Test
    public void testAdvice() throws Exception
    {
        NCube ncube = NCubeManager.getNCubeFromResource("testGroovyMethods.json");

        // These methods are called more than you think.  Internally, these cube call
        // themselves, and those calls too go through the Advice.
        NCubeManager.addAdvice(TestNCubeManager.defaultFileApp, ncube.getName() + ".*()", new Advice()
        {
            public String getName()
            {
                return "alpha";
            }

            public boolean before(Method method, NCube ncube, Map input, Map output)
            {
                output.put("before", true);
                assertEquals(2, input.size());
                boolean ret = true;
                if ("foo".equals(method.getName()))
                {
                    assertEquals("foo", input.get("method"));
                }
                else if ("bar".equals(method.getName()))
                {
                    assertEquals("bar", input.get("method"));
                }
                else if ("qux".equals(method.getName()))
                {
                    assertEquals("qux", input.get("method"));
                }
                else if ("qaz".equals(method.getName()))
                {
                    ret = false;
                }
                return ret;
            }

            public void after(Method method, NCube ncube, Map input, Map output, Object returnValue)
            {
                output.put("after", true);
                if ("foo".equals(method.getName()) && "OH".equals(input.get("state")))
                {
                    assertEquals(2, returnValue);
                }
                else if ("bar".equals(method.getName()) && "OH".equals(input.get("state")))
                {
                    assertEquals(4, returnValue);
                }
                else if ("qux".equals(method.getName()) && "TX".equals(input.get("state")))
                {
                    assertEquals(81, returnValue);
                }
            }
        });

        Map output = new HashMap();
        Map coord = new HashMap();
        coord.put("method", "foo");
        coord.put("state", "OH");
        ncube.getCell(coord, output);
        assertTrue(output.containsKey("before"));
        assertTrue(output.containsKey("after"));

        output.clear();
        coord.put("state", "OH");
        coord.put("method", "bar");
        ncube.getCell(coord, output);
        assertTrue(output.containsKey("before"));
        assertTrue(output.containsKey("after"));

        output.clear();
        coord.put("state", "TX");
        coord.put("method", "qux");
        ncube.getCell(coord, output);
        assertTrue(output.containsKey("before"));
        assertTrue(output.containsKey("after"));
    }

    @Test
    public void testAdviceSubsetMatching() throws Exception
    {
        NCube ncube = NCubeManager.getNCubeFromResource("testGroovyMethods.json");

        // These methods are called more than you think.  Internally, these cube call
        // themselves, and those calls too go through the Advice.
        NCubeManager.addAdvice(TestNCubeManager.defaultFileApp, ncube.getName() + ".ba*()", new Advice()
        {
            public String getName()
            {
                return "alpha";
            }

            public boolean before(Method method, NCube ncube, Map input, Map output)
            {
                output.put("before", true);
                assertEquals(2, input.size());
                boolean ret = true;
                if ("foo".equals(method.getName()))
                {
                    assertEquals("foo", input.get("method"));
                }
                else if ("bar".equals(method.getName()))
                {
                    output.put("bar", true);
                    assertEquals("bar", input.get("method"));
                }
                else if ("baz".equals(method.getName()))
                {
                    output.put("baz", true);
                }
                else if ("qux".equals(method.getName()))
                {
                    assertEquals("qux", input.get("method"));
                }
                else if ("qaz".equals(method.getName()))
                {
                    ret = false;
                }
                return ret;
            }

            public void after(Method method, NCube ncube, Map input, Map output, Object returnValue)
            {
                output.put("after", true);
                if ("foo".equals(method.getName()) && "OH".equals(input.get("state")))
                {
                    assertEquals(2, returnValue);
                }
                else if ("bar".equals(method.getName()) && "OH".equals(input.get("state")))
                {
                    assertEquals(4, returnValue);
                }
                else if ("qux".equals(method.getName()) && "TX".equals(input.get("state")))
                {
                    assertEquals(81, returnValue);
                }
            }
        });

        Map output = new HashMap();
        Map coord = new HashMap();
        coord.put("method", "foo");
        coord.put("state", "OH");
        ncube.getCell(coord, output);
        assertFalse(output.containsKey("before"));
        assertFalse(output.containsKey("after"));

        output.clear();
        coord.put("state", "OH");
        coord.put("method", "bar");
        ncube.getCell(coord, output);
        assertTrue(output.containsKey("before"));
        assertTrue(output.containsKey("after"));
        assertTrue(output.containsKey("bar"));

        output.clear();
        coord.put("state", "OH");
        coord.put("method", "baz");
        ncube.getCell(coord, output);
        assertTrue(output.containsKey("before"));
        assertTrue(output.containsKey("after"));
        assertTrue(output.containsKey("baz"));

        output.clear();
        coord.put("state", "TX");
        coord.put("method", "qux");
        ncube.getCell(coord, output);
        // Controller method Qux calls baz via getCell() which then is intercepted at sets the output keys before, after.
        assertTrue(output.containsKey("before"));
        assertTrue(output.containsKey("after"));

        output.clear();
        coord.put("state", "OH");
        coord.put("method", "qux");
        ncube.getCell(coord, output);
        // Controller method Qux calls baz directly which is NOT intercepted
        assertFalse(output.containsKey("before"));
        assertFalse(output.containsKey("after"));

        ncube.clearAdvices();
    }

    @Test
    public void testAdviceSubsetMatchingLateLoad()
    {
        // These methods are called more than you think.  Internally, these cube call
        // themselves, and those calls too go through the Advice.
        NCubeManager.addAdvice(TestNCubeManager.defaultFileApp, "*.ba*()", new Advice()
        {
            public String getName()
            {
                return "alpha";
            }

            public boolean before(Method method, NCube ncube, Map input, Map output)
            {
                output.put("before", true);
                assertEquals(2, input.size());
                boolean ret = true;
                if ("foo".equals(method.getName()))
                {
                    assertEquals("foo", input.get("method"));
                }
                else if ("bar".equals(method.getName()))
                {
                    output.put("bar", true);
                    assertEquals("bar", input.get("method"));
                }
                else if ("baz".equals(method.getName()))
                {
                    output.put("baz", true);
                }
                else if ("qux".equals(method.getName()))
                {
                    assertEquals("qux", input.get("method"));
                }
                else if ("qaz".equals(method.getName()))
                {
                    ret = false;
                }
                return ret;
            }

            public void after(Method method, NCube ncube, Map input, Map output, Object returnValue)
            {
                output.put("after", true);
                if ("foo".equals(method.getName()) && "OH".equals(input.get("state")))
                {
                    assertEquals(2, returnValue);
                }
                else if ("bar".equals(method.getName()) && "OH".equals(input.get("state")))
                {
                    assertEquals(4, returnValue);
                }
                else if ("qux".equals(method.getName()) && "TX".equals(input.get("state")))
                {
                    assertEquals(81, returnValue);
                }
            }
        });

        NCube ncube = NCubeManager.getNCubeFromResource("testGroovyMethods.json");

        Map output = new HashMap();
        Map coord = new HashMap();
        coord.put("method", "foo");
        coord.put("state", "OH");
        ncube.getCell(coord, output);
        assertFalse(output.containsKey("before"));
        assertFalse(output.containsKey("after"));

        output.clear();
        coord.put("state", "OH");
        coord.put("method", "bar");
        ncube.getCell(coord, output);
        assertTrue(output.containsKey("before"));
        assertTrue(output.containsKey("after"));
        assertTrue(output.containsKey("bar"));

        output.clear();
        coord.put("state", "OH");
        coord.put("method", "baz");
        ncube.getCell(coord, output);
        assertTrue(output.containsKey("before"));
        assertTrue(output.containsKey("after"));
        assertTrue(output.containsKey("baz"));

        output.clear();
        coord.put("state", "TX");
        coord.put("method", "qux");
        ncube.getCell(coord, output);
        // Controller method Qux calls baz via getCell() which then is intercepted at sets the output keys before, after.
        assertTrue(output.containsKey("before"));
        assertTrue(output.containsKey("after"));

        output.clear();
        coord.put("state", "OH");
        coord.put("method", "qux");
        ncube.getCell(coord, output);
        // Controller method Qux calls baz directly which is NOT intercepted
        assertFalse(output.containsKey("before"));
        assertFalse(output.containsKey("after"));
    }

    @Test
    public void testAdviceNoCallForward()
    {
        NCube ncube = NCubeManager.getNCubeFromResource("testGroovyMethods.json");

        // These methods are called more than you think.  Internally, these cube call
        // themselves, and those calls too go through the Advice.
        NCubeManager.addAdvice(TestNCubeManager.defaultFileApp, ncube.getName() + "*", new Advice()
        {
            public String getName()
            {
                return "alpha";
            }

            public boolean before(Method method, NCube ncube, Map input, Map output)
            {
                return false;
            }

            public void after(Method method, NCube ncube, Map input, Map output, Object returnValue)
            {
                fail("should not make it here");
            }
        });

        Map coord = new HashMap();
        coord.put("method", "foo");
        coord.put("state", "OH");
        assertNull(ncube.getCell(coord));

        ncube.clearAdvices();
    }

    @Test
    public void testMultiAdvice()
    {
        NCube ncube = NCubeManager.getNCubeFromResource("testGroovyMethods.json");
        NCubeManager.createCube(TestNCubeManager.defaultSnapshotApp, ncube, USER_ID);

        // These methods are called more than you think.  Internally, these cube call
        // themselves, and those calls too go through the Advice.
        NCubeManager.addAdvice(TestNCubeManager.defaultSnapshotApp, ncube.getName() + "*()", new Advice()
        {
            public String getName()
            {
                return "alpha";
            }

            public boolean before(Method method, NCube ncube, Map input, Map output)
            {
                output.put("_btime1", System.nanoTime());
                return true;
            }

            public void after(Method method, NCube ncube, Map input, Map output, Object returnValue)
            {
                output.put("_atime1", System.nanoTime());
            }
        });

        // These methods are called more than you think.  Internally, these cube call
        // themselves, and those calls too go through the Advice.
        NCubeManager.addAdvice(TestNCubeManager.defaultSnapshotApp, ncube.getName() + "*()", new Advice()
        {
            public String getName()
            {
                return "beta";
            }

            public boolean before(Method method, NCube ncube, Map input, Map output)
            {
                output.put("_btime2", System.nanoTime());
                return true;
            }

            public void after(Method method, NCube ncube, Map input, Map output, Object returnValue)
            {
                output.put("_atime2", System.nanoTime());
            }
        });

        Map coord = new HashMap();
        Map output = new HashMap();
        coord.put("method", "foo");
        coord.put("state", "OH");
        ncube.getCell(coord, output);
        assertTrue(output.containsKey("_atime1"));
        assertTrue(output.containsKey("_btime1"));
        assertTrue(output.containsKey("_atime2"));
        assertTrue(output.containsKey("_btime2"));
        assertTrue((Long)output.get("_atime1") > (Long)output.get("_atime2"));
        assertTrue((Long)output.get("_btime1") < (Long)output.get("_btime2"));
        assertTrue((Long)output.get("_btime2") < (Long)output.get("_atime1"));
        assertTrue((Long)output.get("_btime1") < (Long)output.get("_atime1"));

        ncube.clearAdvices();
    }
}
