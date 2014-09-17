package com.cedarsoftware.ncube.formatters;

import com.cedarsoftware.ncube.RuleInfo;
import groovy.util.MapEntry;

import java.util.List;
import java.util.Map;

/**
 * Created by kpartlow on 9/16/2014.
 */
public class TestResultsFormatter
{
    private Map _output;
    private StringBuilder _builder = new StringBuilder();
    private static final String newLine = System.getProperty("line.separator");

    public TestResultsFormatter(Map output)
    {
        _output = output;
    }

    public String format() {

        _builder.append("Result:  " + _output.get("return"));
        _builder.append(newLine);

        RuleInfo info = (RuleInfo)_output.get("_rule");
        format(info.getRuleExecutionTrace());

        return _builder.toString();
    }

    public void format(List<MapEntry> trace) {
        StringBuilder spaces = new StringBuilder();
        for (MapEntry entry : trace) {

            if (entry.getValue() instanceof Map) {
                ((Map)entry.getValue()).remove("ncube");
            }

            if (isEnd(entry.getKey()))
            {
                spaces.setLength(spaces.length()-3);
            }

            _builder.append(spaces);
            _builder.append(entry.getKey());

            if (isBegin(entry.getKey()))
            {
                spaces.append("   ");
            }
            _builder.append(" = ");
            _builder.append(entry.getValue());
            _builder.append(newLine);
        }
    }

    public boolean isBegin(Object o) {
        return o instanceof String && ((String)o).startsWith("begin:");
    }

    public boolean isEnd(Object o) {
        return o instanceof String && ((String)o).startsWith("end:");
    }
}
