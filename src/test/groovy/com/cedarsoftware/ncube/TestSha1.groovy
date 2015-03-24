package com.cedarsoftware.ncube

import org.junit.Test

/**
 * SHA-1 tests
 *
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
class TestSha1
{
    @Test
    void testSha1CellColumnMattersSorted()
    {
        NCube ncube1 = new NCube("foo")
        Axis axis1 = new Axis("state", AxisType.DISCRETE, AxisValueType.STRING, false, Axis.SORTED, 1)
        axis1.addColumn("GA")
        axis1.addColumn("OH")
        axis1.addColumn("TX")
        ncube1.addAxis(axis1)
        ncube1.setCell("a", [state:'GA'])

        NCube ncube2 = new NCube("foo")
        Axis axis2 = new Axis("state", AxisType.DISCRETE, AxisValueType.STRING, false, Axis.SORTED, 2)
        axis2.addColumn("GA")
        axis2.addColumn("OH")
        axis2.addColumn("TX")
        ncube2.addAxis(axis2)
        ncube2.setCell("a", [state:'TX'])

        assert ncube1.sha1() != ncube2.sha1()
    }

    @Test
    void testSha1CellColumnMattersDisplay()
    {
        NCube ncube1 = new NCube("foo")
        Axis axis1 = new Axis("state", AxisType.DISCRETE, AxisValueType.STRING, false, Axis.DISPLAY, 1)
        axis1.addColumn("GA")
        axis1.addColumn("OH")
        axis1.addColumn("TX")
        ncube1.addAxis(axis1)
        ncube1.setCell("a", [state:'GA'])

        NCube ncube2 = new NCube("foo")
        Axis axis2 = new Axis("state", AxisType.DISCRETE, AxisValueType.STRING, false, Axis.DISPLAY, 3)
        axis2.addColumn("GA")
        axis2.addColumn("OH")
        axis2.addColumn("TX")
        ncube2.addAxis(axis2)
        ncube2.setCell("a", [state:'TX'])

        assert ncube1.sha1() != ncube2.sha1()
    }

    @Test
    void testSha1CellStringVersusExpression()
    {
        NCube ncube1 = new NCube("foo")
        Axis axis1 = new Axis("state", AxisType.DISCRETE, AxisValueType.STRING, false, Axis.DISPLAY, 1)
        axis1.addColumn("GA")
        axis1.addColumn("OH")
        axis1.addColumn("TX")
        ncube1.addAxis(axis1)
        ncube1.setCell("return 'Hi'", [state:'OH'])

        NCube ncube2 = new NCube("foo")
        Axis axis2 = new Axis("state", AxisType.DISCRETE, AxisValueType.STRING, false, Axis.DISPLAY, 4)
        axis2.addColumn("TX")
        axis2.addColumn("OH")
        axis2.addColumn("GA")
        ncube2.addAxis(axis2)
        GroovyExpression exp = new GroovyExpression("return 'Hi'", null)
        ncube2.setCell(exp, [state:'OH'])

        assert ncube1.sha1() != ncube2.sha1()
    }

    @Test
    void testSha1IgnoresInsertionOrder()
    {
        NCube ncube1 = new NCube("foo")
        Axis axis1 = new Axis("state", AxisType.DISCRETE, AxisValueType.STRING, false, Axis.SORTED, 1)
        axis1.addColumn("GA")
        axis1.addColumn("OH")
        axis1.addColumn("TX")
        axis1.addColumn("WY")
        ncube1.addAxis(axis1)
        ncube1.setCell("A", [state:'GA'])
        ncube1.setCell("L", [state:'OH'])

        NCube ncube2 = new NCube("foo")
        Axis axis2 = new Axis("state", AxisType.DISCRETE, AxisValueType.STRING, false, Axis.SORTED, 5)
        axis2.addColumn("TX")
        axis2.addColumn("GA")
        axis2.addColumn("WY")
        axis2.addColumn("OH")
        ncube2.addAxis(axis2)
        ncube2.setCell("L", [state:'OH'])
        ncube2.setCell("A", [state:'GA'])

        // assert that the internal cell Map is not in same order (it is insertion order)
        assert ncube1.cells.iterator().next() != ncube2.cells.iterator().next()

        // assert same SHA-1 regardless of order.
        assert ncube1.sha1() == ncube2.sha1()
    }

    @Test
    void testSha1AxisFireAll()
    {
        NCube ncube1 = new NCube("foo")
        Axis axis1 = new Axis("state", AxisType.DISCRETE, AxisValueType.STRING, false, Axis.DISPLAY, 1, false)
        axis1.addColumn("OH")
        ncube1.addAxis(axis1)

        NCube ncube2 = new NCube("foo")
        Axis axis2 = new Axis("state", AxisType.DISCRETE, AxisValueType.STRING, false, Axis.DISPLAY, 6, true)
        axis2.addColumn("OH")
        ncube2.addAxis(axis2)

        // assert same SHA-1 regardless of order.
        assert ncube1.sha1() != ncube2.sha1()
    }

    @Test
    void testSha1AxisDefaultColumn()
    {
        NCube ncube1 = new NCube("foo")
        Axis axis1 = new Axis("state", AxisType.DISCRETE, AxisValueType.STRING, false, Axis.DISPLAY, 1, true)
        axis1.addColumn("OH")
        ncube1.addAxis(axis1)

        NCube ncube2 = new NCube("foo")
        Axis axis2 = new Axis("state", AxisType.DISCRETE, AxisValueType.STRING, true, Axis.DISPLAY, 7, true)
        axis2.addColumn("OH")
        ncube2.addAxis(axis2)

        // assert same SHA-1 regardless of order.
        assert ncube1.sha1() != ncube2.sha1()
    }

    @Test
    void testSha1AxisValueType()
    {
        NCube ncube1 = new NCube("foo")
        Axis axis1 = new Axis("state", AxisType.DISCRETE, AxisValueType.LONG, true, Axis.DISPLAY, 1, true)
        ncube1.addAxis(axis1)

        NCube ncube2 = new NCube("foo")
        Axis axis2 = new Axis("state", AxisType.DISCRETE, AxisValueType.STRING, true, Axis.DISPLAY, 8, true)
        ncube2.addAxis(axis2)

        // assert same SHA-1 regardless of order.
        assert ncube1.sha1() != ncube2.sha1()
    }

    @Test
    void testSha1AxisType()
    {
        NCube ncube1 = new NCube("foo")
        Axis axis1 = new Axis("state", AxisType.DISCRETE, AxisValueType.STRING, true, Axis.DISPLAY, 1, true)
        ncube1.addAxis(axis1)

        NCube ncube2 = new NCube("foo")
        Axis axis2 = new Axis("state", AxisType.RANGE, AxisValueType.STRING, true, Axis.DISPLAY, 9, true)
        ncube2.addAxis(axis2)

        // assert same SHA-1 regardless of order.
        assert ncube1.sha1() != ncube2.sha1()
    }

    @Test
    void testSha1AxisNameCaseInsensitive()
    {
        NCube ncube1 = new NCube("foo")
        Axis axis1 = new Axis("STATE", AxisType.RANGE, AxisValueType.STRING, true, Axis.DISPLAY, 1, true)
        ncube1.addAxis(axis1)

        NCube ncube2 = new NCube("foo")
        Axis axis2 = new Axis("state", AxisType.RANGE, AxisValueType.STRING, true, Axis.DISPLAY, 10, true)
        ncube2.addAxis(axis2)

        // assert same SHA-1 regardless of order.
        assert ncube1.sha1() == ncube2.sha1()
    }

    @Test
    void testSha1AxisNameEquals()
    {
        NCube ncube1 = new NCube("foo")
        Axis axis1 = new Axis("STATE", AxisType.RANGE, AxisValueType.STRING, true, Axis.DISPLAY, 1, true)
        ncube1.addAxis(axis1)

        NCube ncube2 = new NCube("foo")
        Axis axis2 = new Axis("state1", AxisType.RANGE, AxisValueType.STRING, true, Axis.DISPLAY, 11, true)
        ncube2.addAxis(axis2)

        // assert same SHA-1 regardless of order.
        assert ncube1.sha1() != ncube2.sha1()
    }
}
