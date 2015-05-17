package com.cedarsoftware.ncube

import org.junit.Test

import static org.junit.Assert.assertFalse
import static org.junit.Assert.assertTrue

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
class TestRange
{
    @Test
    void testRangeEquality()
    {
        Range a = new Range(5, 10)
        Range b = new Range(5, 10)
        Range c = new Range(1, 10)
        Range d = new Range(0, 11)

        assertTrue(a.equals(b))
        assertFalse(a.equals(c))
        assertFalse(a.equals(d))
        assertFalse(a.equals(5))
    }

    @Test
    void testRangeSetEquality()
    {
        RangeSet a = new RangeSet(1)
        a.add(new Range(5, 10))

        RangeSet b = new RangeSet(1)
        b.add(new Range(5, 10))

        assertTrue(a.equals(b))

        RangeSet c = new RangeSet(1)
        c.add(new Range(5, 11))
        assertFalse(a.equals(c))
        assertFalse(a.equals(1))
    }

    @Test
    void testRangeSetOverlap()
    {
        RangeSet set = new RangeSet(3)
        set.add(1)
        set.add(new Range(10, 20))
        set.add(25)
        assertTrue(set.size() == 4)

        RangeSet set1 = new RangeSet(15)
        assertTrue(set.overlap(set1))

        set1.clear()
        set1.add(new Range(5, 15))
        assertTrue(set.overlap(set1))

        set1.clear()
        set1.add(new Range(2, 5))
        assertTrue(set.overlap(set1))

        set1.clear()
        set1.add(25)
        assertTrue(set.overlap(set1))

        set1.clear()
        set1.add(2)
        set1.add(4)
        set1.add(new Range(20, 25))
        assertFalse(set.overlap(set1))
    }
}
