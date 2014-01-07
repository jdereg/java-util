package com.cedarsoftware.ncube.formatters;

import com.cedarsoftware.ncube.Axis;
import com.cedarsoftware.ncube.AxisType;
import com.cedarsoftware.ncube.NCube;
import com.cedarsoftware.util.CaseInsensitiveMap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import static java.lang.Math.abs;

/**
 * Base class for NCube formatters
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
public abstract class NCubeFormatter
{
    protected NCube ncube;

    public NCubeFormatter(NCube ncube)
    {
        this.ncube = ncube;
    }

    public abstract String format(String ... headers);

    /**
     * Calculate import values needed to display an NCube.
     * @return Object[], where element 0 is a List containing the axes
     * where the first axis (element 0) is the axis to be displayed at the
     * top and the rest are the axes sorted smallest to larges.  Element 1
     * of the returned object array is the height of the cells (how many
     * rows it would take to display the entire ncube). Element 2 is the
     * width of the cell matrix (the number of columns would it take to display
     * the cell portion of the NCube).
     */
    protected Object[] getDisplayValues(String ... headers)
    {
        if (headers == null)
        {
            headers = new String[]{};
        }
        Map headerStrings = new CaseInsensitiveMap();
        for (String header : headers)
        {
            headerStrings.put(header, null);
        }
        // Step 1. Sort axes from smallest to largest.
        // Hypercubes look best when the smaller axes are on the inside, and the larger axes are on the outside.
        List<Axis> axes = new ArrayList<Axis>(ncube.getAxes());
        Collections.sort(axes, new Comparator<Axis>()
        {
            public int compare(Axis a1, Axis a2)
            {
                return a2.size() - a1.size();
            }
        });

        // Step 2.  Now find an axis that is a good candidate for the single (top) axis.  This would be an axis
        // with the number of columns closest to 12.
        int smallestDelta = Integer.MAX_VALUE;
        int candidate = -1;
        int count = 0;

        for (Axis axis : axes)
        {
            if (headerStrings.keySet().contains(axis.getName()))
            {
                candidate = count;
                break;
            }
            int delta = abs(axis.size() - 12);
            if (delta < smallestDelta)
            {
                smallestDelta = delta;
                candidate = count;
            }
            count++;
        }

        // Step 3. Compute cell area size
        Axis top = axes.remove(candidate);
        axes.add(0, top);   // Top element is now first.
        top = axes.remove(0);   // Grab 1st (candidate axis) one more time
        if (top.getType() == AxisType.RULE)
        {   // If top is a rule axis, place it last.  It is recognized that there could
            // be more than one rule axis, and there could also be a single rule axis, in
            // which this is a no-op.
            axes.add(top);
        }
        else
        {
            axes.add(0, top);
        }
        long width = axes.get(0).size();
        long height = 1;
        final int len = axes.size();

        for (int i=1; i < len; i++)
        {
            height = axes.get(i).size() * height;
        }

        return new Object[] {axes, height, width};
    }
}
