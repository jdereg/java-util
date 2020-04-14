package com.cedarsoftware.util;

import org.junit.Test;

import java.util.Set;

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
public class TestCompactSet
{
    @Test
    public void testSimpleCases()
    {
        Set<String> set = new CompactSet<>();
        assert set.isEmpty();
        assert set.size() == 0;
        assert set.add("foo");
        assert set.size() == 1;
        assert !set.remove("bar");
        assert set.remove("foo");
        assert set.isEmpty();
    }

    @Test
    public void testSimpleCases2()
    {
        Set<String> set = new CompactSet<>();
        assert set.isEmpty();
        assert set.size() == 0;
        assert set.add("foo");
        assert set.add("bar");
        assert set.size() == 2;
        assert !set.remove("baz");
        assert set.remove("foo");
        assert set.remove("bar");
        assert set.isEmpty();
    }
}
