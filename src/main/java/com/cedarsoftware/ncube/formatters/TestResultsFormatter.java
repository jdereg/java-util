package com.cedarsoftware.ncube.formatters;

import com.cedarsoftware.ncube.NCube;
import com.cedarsoftware.ncube.RuleInfo;
import groovy.util.MapEntry;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by kpartlow on 9/16/2014.
 */
public class TestResultsFormatter
{
    private Map _output;
    private StringBuilder _builder = new StringBuilder();
    private static final String newLine = "\n";

    public TestResultsFormatter(Map output)
    {
        _output = output;
    }

    public String format() {

        formatResult();
        formatOutput();

        RuleInfo info = (RuleInfo)_output.get(NCube.RULE_EXEC_INFO);
        format(info.getRuleExecutionTrace());

        return _builder.toString();
    }

    public void format(List<MapEntry> trace)
    {
        _builder.append("<b>Trace</b>");
        _builder.append("<pre>");
        _builder.append(newLine);
        StringBuilder spaces = new StringBuilder("   ");
        for (MapEntry entry : trace) {

            if (entry.getValue() instanceof Map)
            {
                ((Map)entry.getValue()).remove("ncube");
            }

            boolean end = isEnd(entry.getKey());
            boolean begin = isBegin(entry.getKey());

            if (end)
            {
                spaces.setLength(spaces.length()-3);
            }

            _builder.append(spaces);

            _builder.append(entry.getKey());

            if (begin)
            {
                spaces.append("   ");
                _builder.append("(");
                turnMapIntoCoords((Map<String, Object>) entry.getValue());
                _builder.append(")");
            }
            else
            {
                _builder.append(" = ");
                _builder.append(entry.getValue());
            }

            _builder.append(newLine);
        }
        _builder.setLength(_builder.length()-1);
        _builder.append("</pre>");
    }

    public boolean isBegin(Object o) {
        return o instanceof String && ((String)o).startsWith("begin:");
    }

    public boolean isEnd(Object o) {
        return o instanceof String && ((String)o).startsWith("end:");
    }

    public void turnMapIntoCoords(Map<String, Object> map)
    {
        for (Map.Entry<String, Object> entry : map.entrySet())
        {
            _builder.append(entry.getKey());
            _builder.append(":");
            _builder.append(entry.getValue());
            _builder.append(",");
        }
        _builder.setLength(_builder.length()-1);
    }

    public void formatResult() {
        _builder.append("<b>Result</b>");
        _builder.append("<pre>");
        _builder.append(newLine);
        _builder.append("   ");
        _builder.append(_output.get("return"));
        _builder.append(newLine);

        Set<String> failures = (Set<String>)_output.get("_failures");
        if (failures != null && !failures.isEmpty()) {
            _builder.append(newLine);
            for (String entry : failures)
            {
                _builder.append("   ");
                _builder.append(entry);
                _builder.append(newLine);
            }
            _builder.setLength(_builder.length()-1);
        }
        _builder.append("</pre>");
    }

    public void formatOutput()
    {
        _builder.append("<b>Output</b>");
        _builder.append("<pre>");
        _builder.append(newLine);

        if (_output.size() <= 3) {
            _builder.append("   No output");
            _builder.append(newLine);
        }
        else
        {
            java.util.Iterator i = _output.entrySet().iterator();
            while (i.hasNext())
            {
                Map.Entry item = (Map.Entry) i.next();

                if ("_rule".equals(item.getKey()) || "return".equals(item.getKey()) || "_failures".equals(item.getKey()))
                {
                    continue;
                }
                _builder.append("   ");
                _builder.append(item.getKey());
                _builder.append(" = ");
                _builder.append(item.getValue());
                _builder.append(newLine);
            }
        }
        _builder.append("</pre>");
    }
}
