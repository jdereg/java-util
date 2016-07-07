package com.cedarsoftware.util;

import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertTrue;

/**
 * @author John DeRegnaucourt (john@cedarsoftware.com)
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
public class TestUniqueIdGenerator
{
    @Test
    public void testUniqueIdGeneration() throws Exception
    {
        int testSize = 1000000;
        long[] keep = new long[testSize];

        for (int i=0; i < testSize; i++)
        {
            keep[i] = UniqueIdGenerator.getUniqueId();
        }

        Set<Long> unique = new HashSet<>(testSize);
        for (int i=0; i < testSize; i++)
        {
            unique.add(keep[i]);
        }
        assertTrue(unique.size() == testSize);
    }
}
