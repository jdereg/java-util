package com.cedarsoftware.ncube.formatters;

import com.cedarsoftware.ncube.Axis;
import com.cedarsoftware.ncube.CellTypes;
import com.cedarsoftware.ncube.Column;
import com.cedarsoftware.ncube.NCube;
import com.cedarsoftware.ncube.Range;
import com.cedarsoftware.ncube.RangeSet;
import com.cedarsoftware.ncube.UrlCommandCell;
import com.cedarsoftware.ncube.proximity.LatLon;
import com.cedarsoftware.ncube.proximity.Point2D;
import com.cedarsoftware.ncube.proximity.Point3D;
import com.cedarsoftware.util.SafeSimpleDateFormat;
import com.cedarsoftware.util.StringUtilities;
import com.cedarsoftware.util.io.JsonWriter;

import java.io.IOException;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Format an NCube into an JSON document
 *
 * @author Ken Partlow (kpartlow@gmail.com)
 *         <br/>
 *         Copyright (c) Cedar Software LLC
 *         <br/><br/>
 *         Licensed under the Apache License, Version 2.0 (the "License");
 *         you may not use this file except in compliance with the License.
 *         You may obtain axis copy of the License at
 *         <br/><br/>
 *         http://www.apache.org/licenses/LICENSE-2.0
 *         <br/><br/>
 *         Unless required by applicable law or agreed to in writing, software
 *         distributed under the License is distributed on an "AS IS" BASIS,
 *         WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *         See the License for the specific language governing permissions and
 *         limitations under the License.
 */
public class JsonFormatter implements NCubeFormatter
{
    static final SafeSimpleDateFormat dateFormat = new SafeSimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
    protected StringBuilder builder = new StringBuilder();
    protected String quotedStringFormat = "\"%s\"";

    public JsonFormatter() { }

    /**
     * Use this API to generate JSON view of this NCube.
     */
    public String format(NCube ncube)
    {
        try
        {
            String name = ncube.getName();
            builder.setLength(0);
            startObject();
            writeAttribute("ncube", name, true);
            Object defCellValue = ncube.getDefaultCellValue();

            if (defCellValue != null)
            {
                String valType = CellTypes.getType(defCellValue, "defaultCell");
                if (valType != null)
                {
                    writeValue("defaultCellValueType", valType);
                    comma();
                }
                writeValue("defaultCellValue", ncube.getDefaultCellValue());
                comma();
            }

            Map<String, Object> metaProps = ncube.getMetaProperties();
            if (metaProps.size() > 0)
            {
                for (Map.Entry<String, Object> entry : metaProps.entrySet())
                {
                    writeValue(entry.getKey(), entry.getValue());
                    comma();
                }
            }

            writeAxes(ncube.getAxes());
            writeCells(ncube.getCellMap());
            endObject();
            return builder.toString();
        }
        catch (Exception e)
        {
            throw new IllegalStateException(String.format("Unable to format NCube '%s' into JSON", ncube.getName()), e);
        }
    }

    void writeAxes(List<Axis> axes) throws IOException
    {
        builder.append(String.format(quotedStringFormat, "axes"));
        builder.append(':');
        startArray();
        for (Axis item : axes)
        {
            writeAxis(item);
            comma();
        }
        uncomma();
        endArray();
        comma();
    }

    // default is false, so no need to write those out.
    void writeAxis(Axis axis) throws IOException
    {
        startObject();

        // required inputs
        writeAttribute("name", axis.getName(), true);
        writeAttribute("type", axis.getType().name(), true);
        writeAttribute("valueType", axis.getValueType().name(), true);

        //  optional inputs that can use defaults
        writeAttribute("preferredOrder", axis.getColumnOrder(), true);
        writeAttribute("hasDefault", axis.hasDefaultColumn(), true);

        if (axis.getMetaProperties().size() > 0)
        {
            Map<String, Object> metaProps = axis.getMetaProperties();
            for (Map.Entry<String, Object> entry : metaProps.entrySet())
            {
                writeValue(entry.getKey(), entry.getValue());
                comma();
            }
        }

        writeColumns(axis.getColumns());
        endObject();
    }

    void writeColumns(List<Column> columns) throws IOException
    {
        builder.append("\"columns\":");
        startArray();
        boolean commaWritten = false;

        for (Column item : columns)
        {
            if (!item.isDefault())
            {
                commaWritten = true;
                writeColumn(item);
                comma();
            }
        }

        if (commaWritten)
        {
            uncomma();
        }

        endArray();
    }

    void writeColumn(Column column) throws IOException
    {
        startObject();

        //  Check to see if id exists anywhere. then optimize
        String columnType = getColumnType(column.getValue());
        writeId(column.getId(), true);
        writeType(columnType);
        if (column.getValue() instanceof UrlCommandCell)
        {
            writeCommandCell((UrlCommandCell)column.getValue());
        }
        else
        {
            writeValue("value", column.getValue());
        }

        if (column.getMetaProperties().size() > 0)
        {
            comma();
            Map<String, Object> metaProps = column.getMetaProperties();
            for (Map.Entry<String, Object> entry : metaProps.entrySet())
            {
                writeValue(entry.getKey(), entry.getValue());
                comma();
            }
            uncomma();
        }

        endObject();
    }

    void writeCommandCell(UrlCommandCell cmd) throws IOException
    {
        if (!cmd.isCacheable())
        {
            writeAttribute("cache", cmd.isCacheable(), true);
        }
        if (cmd.getUrl() != null)
        {
            writeAttribute("url", cmd.getUrl(), false);
        }
        else
        {
            writeAttribute("value", cmd.getCmd(), false);
        }
    }

    /**
     * According to parseJsonValue reading in, if your item is one of the following end types it does not need to
     * specify the end type:  String, Long, Boolean, Double.  These items will all be picked up automatically
     * so to save on those types I don't write out the type.
     * @param type Type to write, if null don't write anything because its axis default type
     */
    void writeType(String type) throws IOException
    {
        if (type == null) {
            return;
        }

        writeAttribute("type", type, true);
    }

    void writeCells(Map<Set<Column>, ?> cells) throws IOException
    {
        builder.append("\"cells\":");
        if (cells == null || cells.isEmpty())
        {
            builder.append("[]");
            return;
        }
        startArray();
        for (Map.Entry<Set<Column>, ?> cell : cells.entrySet())
        {
            startObject();
            writeIds(cell);
            writeType(CellTypes.getType(cell.getValue(), "cell"));

            if ((cell.getValue() instanceof UrlCommandCell))
            {
                writeCommandCell((UrlCommandCell)cell.getValue());
            }
            else
            {
                writeValue("value", cell.getValue());
            }
            endObject();
            comma();
        }
        uncomma();
        endArray();
    }


    void writeIds(Map.Entry<Set<Column>, ?> item) throws IOException
    {
        builder.append("\"id\":");
        startArray();

        boolean commaWritten = false;

        for (Column c : item.getKey())
        {
            if (!c.isDefault())
            {
                commaWritten = true;
                writeIdValue(c.getId(), true);
            }
        }

        if (commaWritten)
        {
            uncomma();
        }
        endArray();
        comma();
    }

    void writeId(Long longId, boolean addComma) throws IOException
    {
        builder.append(String.format(quotedStringFormat, "id"));
        builder.append(':');
        writeIdValue(longId, addComma);
    }

    void writeIdValue(Long longId, boolean addComma) throws IOException
    {
        builder.append(longId);

        if (addComma)
        {
            comma();
        }
    }

    public static String getColumnType(Object o)
    {
        if (o instanceof Range || o instanceof RangeSet) {
            return null;
        }

        return CellTypes.getType(o, "column");
    }

    void startArray() {
        builder.append("[");
    }

    void endArray() {
        builder.append("]");
    }

    void startObject() {
        builder.append("{");
    }

    void endObject() {
        builder.append("}");
    }

    void comma() {
        builder.append(",");
    }

    void writeValue(String attr, Object o) throws IOException
    {
        builder.append(String.format(quotedStringFormat, attr));
        builder.append(':');
        writeObject(o);
    }

    void writeObject(Object o) throws IOException
    {
        if (o == null)
        {
            builder.append("null");
        }
        else if (o instanceof String)
        {
            StringWriter w = new StringWriter();
            JsonWriter.writeJsonUtf8String(o.toString(), w);
            builder.append(w.toString());
        }
        else if (o instanceof Date)
        {
            builder.append(String.format(quotedStringFormat, dateFormat.format(o)));
        }
        else if (o instanceof LatLon)
        {
            LatLon l = (LatLon)o;
            builder.append('"');
            builder.append(l.toString());
            builder.append('"');
        }
        else if (o instanceof Point2D)
        {
            Point2D pt = (Point2D)o;
            builder.append('"');
            builder.append(pt.toString());
            builder.append('"');
        }
        else if (o instanceof Point3D)
        {
            Point3D pt = (Point3D)o;
            builder.append('"');
            builder.append(pt.toString());
            builder.append('"');
        }
        else if (o instanceof Range)
        {
            Range r = (Range)o;
            startArray();
            writeObject(r.getLow());
            comma();
            writeObject(r.getHigh());
            endArray();
        }
        else if (o instanceof RangeSet)
        {
            RangeSet r = (RangeSet)o;
            Iterator i = r.iterator();
            startArray();
            while (i.hasNext()) {
                writeObject(i.next());
                comma();
            }
            uncomma();
            endArray();
        }
        else if (o instanceof byte[])
        {
            builder.append(String.format(quotedStringFormat, StringUtilities.encode((byte[]) o)));
        }
        else if (o.getClass().isArray())
        {
            throw new IllegalStateException("Cell cannot be an array (except byte[]). Use Groovy Expression to make cell an array, a List, or a Map, etc.");
        }
        else if (o instanceof BigInteger)
        {
            BigInteger i = (BigInteger)o;
            builder.append('"');
            builder.append(i.toString());
            builder.append('"');
        }
        else if (o instanceof BigDecimal)
        {
            BigDecimal d = (BigDecimal)o;
            builder.append('"');
            builder.append(d.stripTrailingZeros().toPlainString());
            builder.append('"');
        }
        else
        {
            builder.append(o.toString());
        }
    }

    void uncomma()
    {
        builder.setLength(builder.length() - 1);
    }

    void writeAttribute(String attr, Object value, boolean includeComma) throws IOException
    {
        if (value instanceof String)
        {
            StringWriter w = new StringWriter();
            JsonWriter.writeJsonUtf8String((String) value, w);
            value = w.toString();
        }
        builder.append(String.format(quotedStringFormat, attr));
        builder.append(":");
        builder.append(value.toString());
        if (includeComma)
        {
            builder.append(",");
        }
    }
}