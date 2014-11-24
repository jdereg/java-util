package com.cedarsoftware.ncube;

import com.cedarsoftware.util.CaseInsensitiveMap;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * This class represents a binding to a Set of columns, and the associated
 * return value.
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
public class Binding
{
    private final String cubeName;
    private final Map<String, Column> axes = new CaseInsensitiveMap<>();
    private Object value;
    private static final String newLine = "\n";

    public Binding(String cubeName)
    {
        this.cubeName = cubeName;
    }

    public String getCubeName()
    {
        return cubeName;
    }

    public void bind(String axisName, Column column)
    {
        axes.put(axisName, column);
    }

    public void setValue(Object value)
    {
        this.value = value;
    }

    public int getNumBoundAxes()
    {
        return axes.size();
    }

    public Set<Column> getBoundColsForAxis()
    {
        return new LinkedHashSet<>(axes.values());
    }

    public String toHtml()
    {
        StringBuilder s = new StringBuilder(cubeName);
        s.append(newLine);
        for (Map.Entry<String, Column> entry : axes.entrySet())
        {
            s.append("  ");
            s.append(entry.getKey());
            s.append(": ");
            s.append(entry.getValue().getValue());
            s.append(newLine);
        }

        s.append("  <b>value = ");
        s.append(value == null ? "null" : value.toString());
        s.append("</b>");
        return s.toString();
    }
}
