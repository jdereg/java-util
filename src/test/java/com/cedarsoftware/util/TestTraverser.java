package com.cedarsoftware.util;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.TimeZone;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
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

        final boolean[] visited = new boolean[4];
        visited[0] = false;
        visited[1] = false;
        visited[2] = false;
        visited[3] = false;

        Traverser.Visitor visitor = new Traverser.Visitor()
        {
            public void process(Object o)
            {
                if (o instanceof Alpha)
                {
                    visited[0] = true;
                }
                else if (o instanceof Beta)
                {
                    visited[1] = true;
                }
                else if (o instanceof Charlie)
                {
                    visited[2] = true;
                }
                else if (o instanceof TimeZone)
                {
                    visited[3] = true;
                }
            }
        };
        Traverser.traverse(alpha, visitor);
        assertTrue(visited[0]);
        assertTrue(visited[1]);
        assertTrue(visited[2]);
        assertTrue(visited[3]);

        visited[0] = false;
        visited[1] = false;
        visited[2] = false;
        visited[3] = false;
        Traverser.traverse(alpha, new Class[] { TimeZone.class }, visitor);
        assertTrue(visited[0]);
        assertTrue(visited[1]);
        assertTrue(visited[2]);
        assertFalse(visited[3]);
    }
}
