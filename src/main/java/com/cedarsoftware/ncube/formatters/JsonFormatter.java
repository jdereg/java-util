package com.cedarsoftware.ncube.formatters;

import com.cedarsoftware.ncube.Axis;
import com.cedarsoftware.ncube.Column;
import com.cedarsoftware.ncube.GroovyExpression;
import com.cedarsoftware.ncube.GroovyMethod;
import com.cedarsoftware.ncube.GroovyTemplate;
import com.cedarsoftware.ncube.NCube;
import com.cedarsoftware.util.io.JsonWriter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collection;
import java.util.Date;
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
    Map<Long, Object> columnMap;

    private String _quotedStringFormat = "\"%s\"";


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
            writeAttribute("ncube", ncube.getName(), true);
            //writeAttribute("version", ncube.getVersion(), true);
            writeAttribute("ruleMode", ncube.getRuleMode(), true);
            //writeAttribute("defaultCellValue", ncube.getDefaultCellValue());

            writeAxes(ncube.getAxes());
            writeCells(ncube.getCellMap());

            //addCellDefinitions(ncube.getCellDefinitions());
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
        _builder.append(String.format(_quotedStringFormat, "axes"));
        _builder.append(':');
        startArray();
        for (Axis item : axes) {
            writeAxis(item);
            comma();
        }
        _builder.setLength(_builder.length() - 1);
        endArray();
        comma();
    }

    public boolean doesIdExistElsewhere(Object value) {
        return false;
    }

    // default is false, so no need to write those out.
    public void writeAxis(Axis a)
    {
        startObject();

        // required inputs
        writeAttribute("name", a.getName(), true);
        writeAttribute("type", a.getType().name(), true);
        writeAttribute("valueType", a.getValueType().name(), true);

        //  optional inputs that can use defaults
        if (a.getColumnOrder() != Axis.SORTED)
        {
            writeAttribute("preferredOrder", a.getColumnOrder(), true);
        }

        if (a.hasDefaultColumn())
        {
            writeAttribute("hasDefault", a.hasDefaultColumn(), true);
        }

        if (a.isMultiMatch())
        {
            writeAttribute("multiMatch", a.isMultiMatch(), true);
        }

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

        if (doesIdExistElsewhere(c.getValue()))
        {
            writeAttribute("id", c.getId(), true);
            writeType(c.getValue());
            writeValue("value", c.getValue());
        } else {
            writeType(c.getValue());
            writeValue("id", c.getValue());
        }
        //writeAttribute("displayOrder", c.getDisplayOrder(), true);
        //writeAttribute("value", c.getValue(), false);
        endObject();
    }

    /**
     * According to parseJsonValue reading in, if your item is one of the following end types it does not need to
     * specify the end type:  String, Long, Boolean, Double.  These items will all be picked up automatically
     * so to save on those types I don't write out the type.
     * @param cell Cell to write
     */
    public void writeType(Object cell) {
        if (cell == null || (cell instanceof String) || (cell instanceof Double) || (cell instanceof Long) || (cell instanceof Boolean)) {
            return;
        }

        String type = null;

        if (cell instanceof BigDecimal) {
            type = "bigdec";
        } else if (cell instanceof Float) {
            type = "float";
        } else if (cell instanceof Integer) {
            type = "int";
        } else if (cell instanceof BigInteger) {
            type = "bigint";
        } else if (cell instanceof Byte) {
            type = "byte";
        } else if (cell instanceof Short) {
            type = "short";
        } else if (cell instanceof Date) {
            type = "date";
        } else if (cell instanceof GroovyExpression) {
            type = "exp";
        } else if (cell instanceof GroovyMethod) {
            type = "method";
        } else if (cell instanceof GroovyTemplate) {
            type = "template";
        } else if (cell instanceof byte[]) {
            type = "binary";
        } else {
            // probably object[].  This is really legal, but don't want to handle this yet.
            throw new IllegalArgumentException("Could not pull type:  " + cell.getClass());
        }

        if (type == null) {
            return;
        }

        writeAttribute("type", type, true);
    }

    public void writeCells(Map<Set<Column>, ?> cells) {
        _builder.append("\"cells\":");
        startArray();
        for (Map.Entry<Set<Column>, ?> item : cells.entrySet()) {
            startObject();
            writeIds(item.getKey());
            writeValue("value", item.getValue());
            endObject();
            comma();
        }
        _builder.setLength(_builder.length() - 1);
        endArray();
    }

    public void writeIds(Set<Column> keys) {
        _builder.append("\"id\":");
        startArray();
        for (Column c : keys) {
            //c.getId();  Only do getValue() if unique
            writeValue(c.getValue());
            comma();
        }
        _builder.setLength(_builder.length() - 1);
        endArray();
        comma();
    }


    public void writeValue(Object o) {
        if (o == null) {
            _builder.append("null");
            return;
        }

        if (o instanceof GroovyExpression || o instanceof GroovyMethod) {
            GroovyExpression cmd = (GroovyExpression)o;
            writeAttribute("type", "exp", true);
            if (cmd.isCacheable()) {
                writeAttribute("cache", cmd.isCacheable(), true);
            }
            if (cmd.getUrl() != null) {
                writeAttribute("url", cmd.getUrl(), true);
            } else if (cmd.getCmd() != null) {
                writeAttribute("value", cmd.getCmd(), true);
            }
            return;
        }

        if (o instanceof Array) {
            startArray();
            for (int i=0; i<Array.getLength(o); i++) {
                writeValue(Array.get(o, i));
            }
            endArray();
            return;
        }

        if (o instanceof Collection) {
            Collection c = (Collection)o;
            startArray();
            for (Object it : c) {
                writeValue(it);
            }
            endArray();
            return;
        }


        if (!(o instanceof String)) {
            try
            {
                ByteArrayOutputStream out = new ByteArrayOutputStream(9182);
                JsonWriter w = new JsonWriter(out);
                w.write(o);
                _builder.append(out.toString("UTF-8"));
            } catch (Exception e) {
                e.printStackTrace();
            }

            return;
        }
        //System.out.println(o.getClass());
        _builder.append(String.format(_quotedStringFormat, o.toString()));
    }

    public void writeValue(String attr, Object o) {
        //_builder.append(String.format("\"type\":\"%s\",", convertValueType(o)));
        _builder.append(String.format(_quotedStringFormat, attr));
        _builder.append(':');

        writeValue(o);
    }

    public void writeAttribute(String name, String value, boolean includeComma) throws IllegalArgumentException {


        try
        {
            StringWriter w = new StringWriter(value.length());
            JsonWriter.writeJsonUtf8String(value, w);

            _builder.append(String.format(_quotedStringFormat, name));
            _builder.append(':');
            _builder.append(w.toString());
        } catch (IOException e) {
            // this way or hide the exception and write null for value?
            throw new IllegalArgumentException(String.format("Error writing attribute '%s' with value '%s'", name, value));
        }

        if (includeComma) {
            _builder.append(",");
        }
    }

    public void writeAttribute(String name, long value, boolean includeComma) {
        _builder.append(String.format(_quotedStringFormat, name));
        _builder.append(":");
        _builder.append(value);
        if (includeComma) {
            _builder.append(",");
        }
    }

    public void writeAttribute(String name, boolean value, boolean includeComma) {
        _builder.append(String.format(_quotedStringFormat, name));
        _builder.append(":");
        _builder.append(value);
        if (includeComma) {
            _builder.append(",");
        }
    }
}