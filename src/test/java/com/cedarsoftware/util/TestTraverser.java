package com.cedarsoftware.util;

import org.junit.Test;

import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author John DeRegnaucourt (jdereg@gmail.com)
 *         <br>
 *         Copyright (c) Cedar Software LLC
 *         <br><br>
 *         Licensed under the Apache License, Version 2.0 (the "License");
 *         you may not use this file except in compliance with the License.
 *         You may obtain a copy of the License at
 *         <br><br>
 *         http://www.apache.org/licenses/LICENSE-2.0
 *         <br><br>
 *         Unless required by applicable law or agreed to in writing, software
 *         distributed under the License is distributed on an "AS IS" BASIS,
 *         WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *         See the License for the specific language governing permissions and
 *         limitations under the License.
 */
public class TestTraverser
{
    class Alpha
    {
        String name;
        Collection contacts;
        Beta beta;
    }

    class Beta
    {
        int age;
        Map friends;
        Charlie charlie;
    }

    class Charlie
    {
        double salary;
        Collection timezones;
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
        alpha.contacts = new ArrayList();
        alpha.contacts.add(beta);
        alpha.contacts.add(charlie);
        alpha.contacts.add("Harry");

        beta.age = 45;
        beta.charlie = charlie;
        beta.friends = new LinkedHashMap();
        beta.friends = new LinkedHashMap();
        beta.friends.put("Tom", "Tom Jones");
        beta.friends.put(alpha, "Alpha beta");
        beta.friends.put("beta", beta);

        charlie.salary = 150000.01;
        charlie.alpha = alpha;
        charlie.timezones = new LinkedList();
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
}
