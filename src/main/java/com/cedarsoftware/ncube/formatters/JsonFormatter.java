package com.cedarsoftware.ncube.formatters;

import com.cedarsoftware.ncube.*;
import com.cedarsoftware.util.io.JsonWriter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Format an NCube into an JSON document
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
public class JsonFormatter extends NCubeFormatter
{
    StringBuilder _builder = new StringBuilder();

    public JsonFormatter(NCube ncube)
    {
        super(ncube);
        this.ncube = ncube;
    }

    /**
     * Use this API to generate a JSON view of this NCube.
     * @param headers ignored
     * @return String containing a JSON view of this NCube.
     */
    public String format(String ... headers)
    {
        try
        {
            _builder.setLength(0);

            startObject();
            addItem("ncube", ncube.getName(), true);
            //addItem("version", ncube.getVersion(), true);
            addItem("ruleMode", ncube.getRuleMode(), true);

            writeAxes(ncube.getAxes());
            writeCells(ncube.getCellMap());

            //addCellDefinitions(ncube.getCellDefinitions());
            //addItem(b, "defaultCellValue", ncube.getDefaultCellValue());
            // cell definitions

            endObject();

            return _builder.toString();
        }
        //catch (IOException e)
        finally
        {
            //throw new IllegalStateException("Unable to format NCube '" + ncube.getName() + "' into JSON", e);
        }
    }

    public void startArray() {
        _builder.append("[");
    }

    public void endArray() {
        _builder.append("]");
    }

    public void startObject() {
        _builder.append("{");
    }

    public void endObject() {
        _builder.append("}");
    }

    public void comma() {
        _builder.append(",");
    }

    public void writeAxes(List<Axis> axes) {
        _builder.append("\"axes\":");
        startArray();
        for (Axis item : axes) {
            writeAxis(item);
            comma();
        }
        _builder.setLength(_builder.length() - 1);
        endArray();
        comma();
    }

    public void writeAxis(Axis a)
    {
        startObject();
        addItem("name", a.getName(), true);
        addItem("type", a.getType().name(), true);
        addItem("valueType", a.getValueType().name(), true);
        addItem("hasDefault", a.hasDefaultColumn(), true);
        addItem("preferredOrder", a.getColumnOrder(), true);
        writeColumns(a.getColumns());
        endObject();
    }

    public void writeColumns(List<Column> columns) {
        _builder.append("\"columns\":");
        startArray();
        for(Column item : columns ) {
            writeColumn(item);
            comma();
        }
        _builder.setLength(_builder.length() - 1);
        endArray();
    }

    public void writeColumn(Column c) {
        startObject();
        //  Check to see if id exists anywhere. then optimize
        addItem("id", c.getId(), true);
        addItem("value", c.getValue(), false);
        endObject();
    }

    public void writeCells(Map<Set<Column>, ?> cells) {
        _builder.append("\"cells\":");
        startArray();
        for (Map.Entry<Set<Column>, ?> item : cells.entrySet()) {
            startObject();
            writeKeys(item.getKey());
            writeValue(item.getValue());
            endObject();
            comma();
        }
        _builder.setLength(_builder.length() - 1);
        endArray();
    }

    public void writeKeys(Set<Column> keys) {
        _builder.append("\"id\":");
        startArray();
        for (Column c : keys) {
            _builder.append(c.getValue());
            comma();
        }
        _builder.setLength(_builder.length() - 1);
        endArray();
        comma();
    }

    public void writeValue(Object o) {
        _builder.append(String.format("\"type\":\"%s\",", convertValueType(o)));
        _builder.append("\"value\":");
        startArray();
        _builder.append(o.toString());
        endArray();
    }

    public String convertValueType(Object type) {
        return "STRING";
    }

    public void addItem(String name, String value, boolean includeComma) {
        _builder.append(String.format("\"%s\":\"%s\"", name, value));
        if (includeComma) {
            _builder.append(",");
        }
    }

    public void addItem(String name, Comparable c, boolean includeComma) {
        addItem(name, c == null ? "null" : c.toString(), includeComma);
    }


    public void addItem(String name, long value, boolean includeComma) {
        addItem(name, Long.toString(value), includeComma);
    }

    public void addItem(String name, boolean value, boolean includeComma) {
        addItem(name, value ? "true" : "false", includeComma);
    }
}