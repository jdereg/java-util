package com.cedarsoftware.ncube.formatters;

import com.cedarsoftware.ncube.Axis;
import com.cedarsoftware.ncube.BinaryUrlCmd;
import com.cedarsoftware.ncube.Column;
import com.cedarsoftware.ncube.GroovyExpression;
import com.cedarsoftware.ncube.GroovyMethod;
import com.cedarsoftware.ncube.GroovyTemplate;
import com.cedarsoftware.ncube.NCube;
import com.cedarsoftware.ncube.StringUrlCmd;
import com.cedarsoftware.ncube.UrlCommandCell;
import com.cedarsoftware.util.io.JsonWriter;

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
        catch (IOException e)
        {
            throw new IllegalArgumentException(String.format("Unable to format NCube '%s' into JSON:  '%s'", ncube.getName(), e.toString()));
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

    public void writeAxes(List<Axis> axes) throws IOException {
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
    public void writeAxis(Axis a) throws IOException
    {
        startObject();

        // required inputs
        writeAttribute("name", a.getName(), true);
        writeAttribute("type", a.getType().name(), true);
        writeAttribute("valueType", a.getValueType().name(), true);

        //  optional inputs that can use defaults
        writeAttribute("preferredOrder", a.getColumnOrder(), true);

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

    public void writeColumns(List<Column> columns) throws IOException {
        _builder.append("\"columns\":");
        startArray();
        for(Column item : columns ) {
            if (!item.isDefault())
            {
                writeColumn(item);
                comma();
            }
        }
        _builder.setLength(_builder.length() - 1);
        endArray();
    }

    public void writeColumn(Column c) throws IOException {
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
        String type = getType(cell);

        if (type == null) {
            return;
        }

        writeAttribute("type", type, true);
    }

    private String getType(Object cell)
    {
        if (cell == null || (cell instanceof String) || (cell instanceof Double) || (cell instanceof Long) || (cell instanceof Boolean)) {
            return null;
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
        } else if (cell instanceof StringUrlCmd) {
            type = "string";
        } else if (cell instanceof BinaryUrlCmd) {
            type = "binary";
        } else if (cell instanceof byte[]) {
            type = "binary";
        } else if (cell instanceof Object[]) {
            type = walkArraysForType((Object[]) cell);
        } else {
//            throw new IllegalArgumentException("Could not pull type:  " + cell.getClass());
        }
        return type;
    }

    public String walkArraysForType(Object[] items) {

        if (items == null || items.length == 0) {
            return null;
        }

        String first = null;

        for (int i=0; i<items.length; i++) {

            String type = getType(items[i]);

            if (first == null) {
                first = type;
            }

            if (first != null && type != null && !first.equalsIgnoreCase(type))
            {
                return null;
            }
        }

        return first;
    }

    public void writeCells(Map<Set<Column>, ?> cells) throws IOException {
        _builder.append("\"cells\":");
        if (cells == null || cells.isEmpty()) {
            _builder.append("null");
            return;
        }
        startArray();
        for (Map.Entry<Set<Column>, ?> item : cells.entrySet()) {
            startObject();
            writeIds(item.getKey());
            writeType(item.getValue());
            writeCacheable(item.getValue());
            if (isUrlCommandCell(item.getValue())) {
                UrlCommandCell cmd = (UrlCommandCell)item.getValue();
                if (cmd.getUrl() != null) {
                    writeAttribute("url", cmd.getUrl(), false);
                } else if (cmd.getCmd() != null) {
                    writeAttribute("value", cmd.getCmd(), false);
                }
            } else {
                writeValue("value", item.getValue());
            }
            endObject();
            comma();
        }
        _builder.setLength(_builder.length() - 1);
        endArray();
    }

    public boolean isUrlCommandCell(Object o) {
        return o instanceof UrlCommandCell;
    }

    public void writeCacheable(Object o)
    {
        if (!isUrlCommandCell(o)) {
            return;
        }

        UrlCommandCell cmd = (UrlCommandCell)o;

        if (!cmd.isCacheable()) {
            writeAttribute("cache", cmd.isCacheable(), true);
        }

    }
    public void writeIds(Set<Column> keys) throws IOException {
        _builder.append("\"id\":");
        startArray();
        for (Column c : keys) {
            //c.getId();  Only do getValue() if unique
            writeObject(c.getValue());
            comma();
        }
        _builder.setLength(_builder.length() - 1);
        endArray();
        comma();
    }


    public void writeObject(Object o) throws IOException {
        if (o == null) {
            _builder.append("null");
            return;
        }

        if (o.getClass().isArray()) {
            //  check types
            startArray();
            for (int i=0; i<Array.getLength(o); i++) {
                writeObject(Array.get(o, i));
                comma();
            }
            _builder.setLength(_builder.length() - 1);
            endArray();
            return;
        }

        if (o instanceof Collection) {
            Collection c = (Collection)o;
            startArray();
            for (Object it : c) {
                writeObject(it);
                comma();
            }
            _builder.setLength(_builder.length() - 1);
            endArray();
            return;
        }


        if (!(o instanceof String)) {
            _builder.append(o.toString());
            return;
        }
        //System.out.println(o.getClass());

        //ByteArrayOutputStream out = new ByteArrayOutputStream(9182);
        StringWriter w = new StringWriter();
        JsonWriter.writeJsonUtf8String((String)o, w);
        _builder.append(w.toString());
        //_builder.append(String.format(_quotedStringFormat, o.toString()));
    }

    public void writeValue(String attr, Object o) throws IOException {
        _builder.append(String.format(_quotedStringFormat, attr));
        _builder.append(':');

        writeObject(o);
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