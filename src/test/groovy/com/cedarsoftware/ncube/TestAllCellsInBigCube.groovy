package com.cedarsoftware.ncube

import groovy.transform.CompileStatic
import org.junit.Test

import static org.junit.Assert.assertTrue

/**
 * Test creating a cube with 50^5 (100,000) cells.  Set every cell, then
 * read every value back.  Goal: Strive to make this test faster and faster
 * as it really exercises the guts of the rule engine.
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
@CompileStatic
class TestAllCellsInBigCube
{
    @Test
    void testAllCellsInBigCube()
    {
        for (int qq=0; qq < 1; qq++)
        {
            long start = System.nanoTime()
            NCube<Long> ncube = new NCube("bigCube")

            for (int i = 0; i < 5; i++)
            {
                Axis axis = new Axis("axis" + i, AxisType.DISCRETE, AxisValueType.LONG, i % 2 == 0)
                ncube.addAxis(axis)
                for (int j = 0; j < 10; j++)
                {
                    if (j % 2 == 0)
                    {
                        axis.addColumn(j)
                    }
                    else
                    {
                        ncube.addColumn("axis" + i, j)
                    }
                }
            }

            def coord = [:]
            for (int a = 1; a <= 11; a++)
            {
                coord.axis0 = a - 1
                for (int b = 1; b <= 10; b++)
                {
                    coord.axis1 = b - 1
                    for (int c = 1; c <= 11; c++)
                    {
                        coord.axis2 = c - 1
                        for (int d = 1; d <= 10; d++)
                        {
                            coord.axis3 = d - 1
                            for (long e = 1; e <= 11; e++)
                            {
                                coord.axis4 = e - 1
                                ncube.setCell(a * b * c * d * e, coord)
                            }
                        }
                    }
                }
            }

            for (int a = 1; a <= 11; a++)
            {
                coord.axis0 = a - 1
                for (int b = 1; b <= 10; b++)
                {
                    coord.axis1 = b - 1
                    for (int c = 1; c <= 11; c++)
                    {
                        coord.axis2 = c - 1
                        for (int d = 1; d <= 10; d++)
                        {
                            coord.axis3 = d - 1
                            for (long e = 1; e <= 11; e++)
                            {
                                coord.axis4 = e - 1
                                long v = ncube.getCell(coord)
                                assertTrue(v == a * b * c * d * e)
                            }
                        }
                    }
                }
            }
            long stop = System.nanoTime()
            double diff = (stop - start) / 1000000.0
            println("time to build and read allCellsInBigCube = " + diff)
        }
    }
}
