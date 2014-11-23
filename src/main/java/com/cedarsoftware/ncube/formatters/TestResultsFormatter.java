package com.cedarsoftware.ncube.formatters;

import com.cedarsoftware.ncube.Binding;
import com.cedarsoftware.ncube.NCube;
import com.cedarsoftware.ncube.RuleInfo;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Implements an n-cube.  This is a hyper (n-dimensional) cube
 * of cells, made up of 'n' number of axes.  Each Axis is composed
 * of Columns that denote discrete nodes along an axis.  Use NCubeManager
 * to manage a list of NCubes.  Documentation on Github.
 *
 * @author Ken Partlow (kpartlow@gmail.com)
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
public class TestResultsFormatter
{
    private Map output;
    private StringBuilder builder = new StringBuilder();
    private static final String newLine = "\n";

    public TestResultsFormatter(Map out)
    {
        this.output = out;
    }

    public String format()
    {
        formatAxisBinding();
        formatLastExecutedStatement();
        formatAssertions();
        formatOutputMap();
        formatSystemOut("System.out");
        formatSystemOut("System.err");
        return builder.toString();
    }

    public void formatAxisBinding()
    {
        RuleInfo ruleInfo = (RuleInfo) output.get(NCube.RULE_EXEC_INFO);
        builder.append("<b>Axis bindings</b>");
        builder.append("<pre>");
        builder.append(newLine);
        for (Iterator<Binding> iterator = ruleInfo.getAxisBindings().iterator(); iterator.hasNext(); )
        {
            Binding binding = iterator.next();
            builder.append(binding.toHtml());
            builder.append(newLine);
            if (iterator.hasNext())
            {
                builder.append("<hr class=\"hr-small\"/>");
            }
        }
        builder.append("</pre>");
    }

    public void formatLastExecutedStatement()
    {
        RuleInfo ruleInfo = (RuleInfo) output.get(NCube.RULE_EXEC_INFO);
        builder.append("<b>Last statement (cell) executed</b>");
        builder.append("<pre>");
        builder.append(newLine);
        builder.append(ruleInfo.getLastExecutedStatementValue());
        builder.append(newLine);
        builder.append("</pre>");
    }

    public void formatAssertions()
    {
        RuleInfo ruleInfo = (RuleInfo) output.get(NCube.RULE_EXEC_INFO);
        builder.append("<b>Assertions</b>");
        builder.append("<pre>");
        builder.append(newLine);

        Set<String> failures = ruleInfo.getAssertionFailures();
        if (failures.isEmpty())
        {
            builder.append("No assertion failures");
            builder.append(newLine);
        }
        else
        {
            for (String entry : failures)
            {
                builder.append(entry);
                builder.append(newLine);
            }
            builder.setLength(builder.length()-1);
        }
        builder.append("</pre>");
    }

    public void formatOutputMap()
    {
        builder.append("<b>Output Map</b>");
        builder.append("<pre>");
        builder.append(newLine);

        if (output.containsKey(NCube.RULE_EXEC_INFO) && output.containsKey("return") && output.size() <= 2)
        {   // size() == 1 minimum (_rule metakey).
            builder.append("No output");
            builder.append(newLine);
        }
        else
        {
            for (Object o : output.entrySet())
            {
                Map.Entry item = (Map.Entry) o;

                final Object key = item.getKey();
                if (NCube.RULE_EXEC_INFO.equals(key))
                {
                    continue;
                }
                builder.append(item.getKey());
                builder.append(" = ");
                builder.append(item.getValue());
                builder.append(newLine);
            }
        }
        builder.append("</pre>");
    }

    public void formatSystemOut(String section)
    {
        boolean isErr = section.toLowerCase().contains("err");
        builder.append("<b>");
        builder.append(section);
        builder.append("</b>");
        if (isErr)
        {
            builder.append("<pre style=\"color:darkred\">");
        }
        else
        {
            builder.append("<pre>");
        }
        builder.append(newLine);

        RuleInfo ruleInfo = (RuleInfo) output.get(NCube.RULE_EXEC_INFO);
        if (isErr)
        {
            builder.append(ruleInfo.getSystemErr());
        }
        else
        {
            builder.append(ruleInfo.getSystemOut());
        }
        builder.append("</pre>");
    }
}
