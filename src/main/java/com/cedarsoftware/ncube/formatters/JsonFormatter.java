package com.cedarsoftware.ncube.formatters;

import com.cedarsoftware.ncube.Axis;
import com.cedarsoftware.ncube.CellTypes;
import com.cedarsoftware.ncube.Column;
import com.cedarsoftware.ncube.CommandCell;
import com.cedarsoftware.ncube.NCube;
import com.cedarsoftware.ncube.Range;
import com.cedarsoftware.ncube.RangeSet;
import com.cedarsoftware.ncube.proximity.LatLon;
import com.cedarsoftware.ncube.proximity.Point2D;
import com.cedarsoftware.ncube.proximity.Point3D;
import com.cedarsoftware.util.StringUtilities;
import com.cedarsoftware.util.io.JsonWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Format an NCube into an JSON document
 *
 * @author Ken Partlow (kpartlow@gmail.com)
 *         <br>
 *         Copyright (c) Cedar Software LLC
 *         <br><br>
 *         Licensed under the Apache License, Version 2.0 (the "License");
 *         you may not use this file except in compliance with the License.
 *         You may obtain axis copy of the License at
 *         <br><br>
 *         http://www.apache.org/licenses/LICENSE-2.0
 *         <br><br>
 *         Unless required by applicable law or agreed to in writing, software
 *         distributed under the License is distributed on an "AS IS" BASIS,
 *         WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *         See the License for the specific language governing permissions and
 *         limitations under the License.
 */
public class JsonFormatter extends BaseJsonFormatter implements NCubeFormatter
{
    private static final String MAX_INT = Long.toString(Integer.MAX_VALUE);
    public JsonFormatter() { }
    public JsonFormatter(OutputStream stream) { super(stream); }

    /**
     * Use this API to generate JSON view of this NCube.
     */
    public String format(NCube ncube)
    {
        if (!(builder instanceof StringWriter))
        {
            throw new IllegalStateException("Builder is not a StringWriter.  Use formatCube(ncube) to write to your stream.");
        }

        formatCube(ncube);
        return builder.toString();
    }

    public void formatCube(NCube ncube)
    {
        if (ncube == null)
        {
            throw new IllegalArgumentException("Cube to format cannot be null");
        }

        String name = ncube.getName();
        try
        {
            startObject();
            writeObjectKeyValue("ncube", name, true);
            Object defCellValue = ncube.getDefaultCellValue();

            if (defCellValue != null)
            {
                String valType = CellTypes.getType(defCellValue, "defaultCell");
                if (valType != null)
                {
                    writeObjectKeyValue(NCube.DEFAULT_CELL_VALUE_TYPE, valType, true);
                }
                if (defCellValue instanceof CommandCell)
                {
                    CommandCell cmd = (CommandCell) defCellValue;

                    if (cmd.isCacheable())
                    {
                        writeObjectKeyValue(NCube.DEFAULT_CELL_VALUE_CACHE, cmd.isCacheable(), true);
                    }
                    if (cmd.getUrl() != null)
                    {
                        writeObjectKeyValue(NCube.DEFAULT_CELL_VALUE_URL, cmd.getUrl(), true);
                    }
                    else
                    {
                        writeObjectKeyValue(NCube.DEFAULT_CELL_VALUE, cmd.getCmd(), true);
                    }
                }
                else
                {
                    writeObjectKeyValue(NCube.DEFAULT_CELL_VALUE, defCellValue, true);
                }
            }

            writeMetaProperties(ncube.getMetaProperties());
            writeAxes(ncube.getAxes());
            writeCells(ncube.getCellMap());
            endObject();
            closeStream();
        }
        catch (Exception e)
        {
            throw new IllegalStateException(String.format("Unable to format NCube '%s' into JSON", name), e);
        }
    }

    private void writeMetaProperties(Map<String, Object> metaProps) throws IOException
    {
        if (metaProps.size() < 1)
        {
            return;
        }

        for (Map.Entry<String, Object> entry : metaProps.entrySet())
        {
            final String key = entry.getKey();
            Object value = entry.getValue();
            if (value instanceof String || value instanceof Boolean || value instanceof Long || value == null)
            {   // Allows for simple key ==> value associations to be written when value is very simple type
                writeObjectKeyValue(key, value, false);
            }
            else
            {
                writeObjectKey(key);
                startObject();
                writeType(CellTypes.getType(value, "meta property"));

                if ((value instanceof CommandCell))
                {
                    writeCommandCell((CommandCell)value);
                }
                else
                {
                    writeObjectKeyValue("value", value, false);
                }
                endObject();
            }

            comma();
        }
    }

    void writeAxes(List<Axis> axes) throws IOException
    {
        if (axes.isEmpty())
        {   // Make sure the formatter writes valid JSON
            append("\"axes\":[],");
            return;
        }

        writeObjectKey("axes");
        startArray();
        boolean firstPass = true;
        for (Axis item : axes)
        {
            if (!firstPass) {
                comma();
            }
            writeAxis(item);
            firstPass = false;
        }
        endArray();
        comma();
    }

    // default is false, so no need to write those out.
    void writeAxis(Axis axis) throws IOException
    {
        startObject();

        // required inputs
        writeObjectKeyValue("name", axis.getName(), true);
        writeObjectKeyValue("type", axis.getType().name(), true);
        writeObjectKeyValue("valueType", axis.getValueType().name(), true);

        //  optional inputs that can use defaults
        writeObjectKeyValue("preferredOrder", axis.getColumnOrder(), true);
        writeObjectKeyValue("hasDefault", axis.hasDefaultColumn(), true);
        writeObjectKeyValue("fireAll", axis.isFireAll(), true);
        writeMetaProperties(axis.getMetaProperties());
        writeColumns(axis.getColumns());
        endObject();
    }

    void writeColumns(List<Column> columns) throws IOException
    {
        append("\"columns\":");
        startArray();
        boolean firstPass = true;

        for (Column item : columns)
        {
            if (!item.isDefault())
            {
                if (!firstPass) {
                    comma();
                }
                writeColumn(item);
                firstPass = false;
            }
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
        writeMetaProperties(column.getMetaProperties());
        if (column.getValue() instanceof CommandCell)
        {
            writeCommandCell((CommandCell) column.getValue());
        }
        else
        {
            writeObjectKeyValue("value", column.getValue(), false);
        }

        endObject();
    }

    void writeCommandCell(CommandCell cmd) throws IOException
    {
        if (cmd.isCacheable())
        {
            writeObjectKeyValue("cache", cmd.isCacheable(), true);
        }
        if (cmd.getUrl() != null)
        {
            writeObjectKeyValue("url", cmd.getUrl(), false);
        }
        else
        {
            writeObjectKeyValue("value", cmd.getCmd(), false);
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

        writeObjectKeyValue("type", type, true);
    }

    void writeCells(Map<Collection<Long>, ?> cells) throws IOException
    {
        append("\"cells\":");
        if (cells == null || cells.isEmpty())
        {
            append("[]");
            return;
        }
        startArray();
        boolean firstPass = true;
        for (Map.Entry<Collection<Long>, ?> cell : cells.entrySet())
        {
            if (!firstPass) {
                comma();
            }
            writeCell(cell);
            firstPass = false;
        }
        endArray();
    }

    private void writeCell(Map.Entry<Collection<Long>, ?> cell) throws IOException
    {
        startObject();
        writeIds(cell.getKey());
        writeType(CellTypes.getType(cell.getValue(), "cell"));

        if ((cell.getValue() instanceof CommandCell))
        {
            writeCommandCell((CommandCell)cell.getValue());
        }
        else
        {
            writeObjectKeyValue("value", cell.getValue(), false);
        }
        endObject();
    }


    void writeIds(Collection<Long> colIds)
    {
        append("\"id\":");
        startArray();

        boolean firstPass = true;

        for (Long colId : colIds)
        {
            final String idAsString = Long.toString(colId);
            if (!idAsString.endsWith(MAX_INT))
            {
                writeIdValue(colId, !firstPass);
                firstPass = false;
            }
        }

        endArray();
        comma();
    }

    void writeId(Long longId, boolean addComma) throws IOException
    {
        writeObjectKeyValue("id", longId, addComma);
    }

    void writeIdValue(Long longId, boolean addComma)
    {
        if (addComma)
        {
            comma();
        }

        append(longId);
    }

    public static String getColumnType(Object o)
    {
        if (o instanceof Range || o instanceof RangeSet) {
            return null;
        }

        return CellTypes.getType(o, "column");
    }

    protected void writeObjectValue(Object o) throws IOException
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
        else if (o instanceof Number)
        {
            builder.append(o.toString());
        }
        else if (o instanceof Date)
        {
            builder.append('"');
            builder.append(dateFormat.format(o));
            builder.append('"');
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
            writeObjectValue(r.getLow());
            comma();
            writeObjectValue(r.getHigh());
            endArray();
        }
        else if (o instanceof RangeSet)
        {
            RangeSet r = (RangeSet)o;
            Iterator i = r.iterator();
            startArray();
            boolean firstPass = true;
            while (i.hasNext())
            {
                if (!firstPass) {
                    comma();
                }
                writeObjectValue(i.next());
                firstPass = false;
            }
            endArray();
        }
        else if (o instanceof byte[])
        {
            builder.append('"');
            builder.append(StringUtilities.encode((byte[]) o));
            builder.append('"');
        }
        else if (o.getClass().isArray())
        {
            throw new IllegalArgumentException("Cell cannot be an array (except byte[]). Use Groovy Expression to make cell an array, a List, or a Map, etc.");
        }
        else
        {
            builder.append(o.toString());
        }
    }

}