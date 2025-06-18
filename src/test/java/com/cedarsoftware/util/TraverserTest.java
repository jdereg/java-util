package com.cedarsoftware.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashMap;
import java.lang.reflect.Field;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.TimeZone;
import java.util.List;
import java.util.function.Consumer;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author John DeRegnaucourt (jdereg@gmail.com)
 *         <br>
 *         Copyright (c) Cedar Software LLC
 *         <br><br>
 *         Licensed under the Apache License, Version 2.0 (the "License");
 *         you may not use this file except in compliance with the License.
 *         You may obtain a copy of the License at
 *         <br><br>
 *         <a href="http://www.apache.org/licenses/LICENSE-2.0">License</a>
 *         <br><br>
 *         Unless required by applicable law or agreed to in writing, software
 *         distributed under the License is distributed on an "AS IS" BASIS,
 *         WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *         See the License for the specific language governing permissions and
 *         limitations under the License.
 */
public class TraverserTest
{
    class Alpha
    {
        String name;
        Collection<Object> contacts;
        Beta beta;
    }

    class Beta
    {
        int age;
        Map<Object, Object> friends;
        Charlie charlie;
    }

    class Charlie
    {
        double salary;
        Collection<Object> timezones;
        Object[] dates;
        Alpha alpha;
        TimeZone zone = TimeZone.getDefault();
        Delta delta;
    }

    class Delta
    {
        TimeZone timeZone = TimeZone.getDefault();
    }

    @Test
    public void testCyclicTraverse()
    {
        Alpha alpha = new Alpha();
        Beta beta = new Beta();
        Charlie charlie = new Charlie();

        alpha.name = "alpha";
        alpha.beta = beta;
        alpha.contacts = new ArrayList<>();
        alpha.contacts.add(beta);
        alpha.contacts.add(charlie);
        alpha.contacts.add("Harry");

        beta.age = 45;
        beta.charlie = charlie;
        beta.friends = new LinkedHashMap<>();
        beta.friends.put("Tom", "Tom Jones");
        beta.friends.put(alpha, "Alpha beta");
        beta.friends.put("beta", beta);

        charlie.salary = 150000.01;
        charlie.alpha = alpha;
        charlie.timezones = new LinkedList<>();
        charlie.timezones.add(TimeZone.getTimeZone("EST"));
        charlie.timezones.add(TimeZone.getTimeZone("GMT"));
        charlie.dates = new Date[] { new Date() };

        final int[] visited = new int[4];
        visited[0] = 0;
        visited[1] = 0;
        visited[2] = 0;
        visited[3] = 0;

        Traverser.Visitor visitor = new Traverser.Visitor()
        {
            public void process(Object o)
            {
                if (o instanceof Alpha)
                {
                    visited[0]++;
                }
                else if (o instanceof Beta)
                {
                    visited[1]++;
                }
                else if (o instanceof Charlie)
                {
                    visited[2]++;
                }
                else if (o instanceof TimeZone)
                {
                    visited[3]++;
                }
            }
        };
        Traverser.traverse(alpha, visitor);
        assertEquals(1, visited[0]);
        assertEquals(1, visited[1]);
        assertEquals(1, visited[2]);
        assertTrue(visited[3] >= 1);

        visited[0] = 0;
        visited[1] = 0;
        visited[2] = 0;
        visited[3] = 0;
        Traverser.traverse(alpha, new Class[] { TimeZone.class }, visitor);
        assertEquals(1, visited[0]);
        assertEquals(1, visited[1]);
        assertEquals(1, visited[2]);
        assertEquals(0, visited[3]);
    }

    @Test
    public void testNullSkipClass()
    {
        final int[] visited = new int[1];
        visited[0] = 0;

        Set<Class<?>> skip = new HashSet<>();
        skip.add(null);

        Traverser.traverse("test", visit -> visited[0]++, skip);
        assertEquals(1, visited[0]);
    }

    @Test
    public void testLazyFieldCollection() throws Exception
    {
        class Foo { int n = 7; }
        Foo foo = new Foo();

        Field nField = foo.getClass().getDeclaredField("n");

        Traverser.traverse(foo, visit -> {
            Map<Field, Object> fields = visit.getFields();
            assertEquals(1, fields.size());
            assertTrue(fields.containsKey(nField));
        }, null, false);
    }

    @Test
    public void testPrivateTraverseConsumer() throws Exception
    {
        class Child { }
        class Parent { Child child; }

        Parent root = new Parent();
        root.child = new Child();

        Method m = Traverser.class.getDeclaredMethod("traverse", Object.class, Set.class, Consumer.class);
        m.setAccessible(true);

        Set<Class<?>> skip = new HashSet<>();
        List<Object> visited = new ArrayList<>();
        m.invoke(null, root, skip, (Consumer<Object>) visited::add);

        assertEquals(2, visited.size());
        assertTrue(visited.contains(root));
        assertTrue(visited.contains(root.child));

        visited.clear();
        skip.add(Child.class);
        m.invoke(null, root, skip, (Consumer<Object>) visited::add);
        assertEquals(1, visited.size());
        assertTrue(visited.contains(root));
    }

    @Test
    public void testPrivateTraverseNullConsumer() throws Exception
    {
        Method m = Traverser.class.getDeclaredMethod("traverse", Object.class, Set.class, Consumer.class);
        m.setAccessible(true);

        InvocationTargetException ex = assertThrows(InvocationTargetException.class,
                () -> m.invoke(null, "root", null, null));
        assertTrue(ex.getCause() instanceof IllegalArgumentException);
    }
}
