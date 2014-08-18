package com.cedarsoftware.ncube.formatters;

import com.cedarsoftware.ncube.Axis;
import com.cedarsoftware.ncube.BinaryUrlCmd;
import com.cedarsoftware.ncube.Column;
import com.cedarsoftware.ncube.GroovyExpression;
import com.cedarsoftware.ncube.GroovyMethod;
import com.cedarsoftware.ncube.GroovyTemplate;
import com.cedarsoftware.ncube.NCube;
import com.cedarsoftware.ncube.Range;
import com.cedarsoftware.ncube.RangeSet;
import com.cedarsoftware.ncube.StringUrlCmd;
import com.cedarsoftware.ncube.UrlCommandCell;
import com.cedarsoftware.ncube.proximity.LatLon;
import com.cedarsoftware.ncube.proximity.Point2D;
import com.cedarsoftware.ncube.proximity.Point3D;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
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
public class JsonFormatter extends AbstractJsonFormat implements NCubeFormatter
{
    private String name;
    private Map<Long, Object> userIds = new HashMap<Long, Object>();
    private Map<Long, Long> generatedIds = new HashMap<Long, Long>();
    private long idCounter;

    public JsonFormatter()
    {
    }

    /**
     * Use this API to generate axis JSON view of this NCube.
     * @return String containing axis JSON view of this NCube.
     */
    public String format(NCube ncube)
    {
        try
        {
            name = ncube.getName();
            builder.setLength(0);
            userIds.clear();
            generatedIds.clear();
            idCounter = 0;
            walkIds(ncube.getAxes());

            startObject();

            writeAttribute("ncube", name, true);
            Object defCellValue = ncube.getDefaultCellValue();
            if (defCellValue != null)
            {
                String valType = getCellType(defCellValue, "defaultCell");
                if (valType != null)
                {
                    writeValue("defaultCellValueType", valType);
                    comma();
                }
                writeValue("defaultCellValue", ncube.getDefaultCellValue());
                comma();
            }
            if (ncube.getMetaProperties().size() > 0)
            {
                Map<String, Object> metaProps = ncube.getMetaProperties();
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

    public void walkIds(List<Axis> axes)
    {
        Set<Comparable> set = new HashSet<Comparable>();
        for (Axis item : axes)
        {
            for (Column c : item.getColumnsWithoutDefault())
            {
                if ((c.getValue() instanceof String) || (c.getValue() instanceof Long))
                {
                    if (!set.contains(c.getValue()))
                    {
                        set.add(c.getValue());
                        userIds.put(c.getId(), c.getValue());
                    }
                }
            }
        }
    }


    public void writeAxes(List<Axis> axes) throws IOException
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
    public void writeAxis(Axis axis) throws IOException
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

    public void writeColumns(List<Column> columns) throws IOException
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

    public void writeColumn(Column column) throws IOException
    {
        startObject();

        //  Check to see if id exists anywhere. then optimize
        Object o = userIds.get(column.getId());
        String columnType = getColumnType(column.getValue());
        if (o != null && o.equals(column.getValue()))
        {
            writeType(columnType);
            writeId(column.getId(), false);
        }
        else
        {
            writeId(column.getId(), true);
            writeType(columnType);
            if (column.getValue() instanceof UrlCommandCell) {
                writeCommandCell((UrlCommandCell)column.getValue());
            } else {
                writeValue("value", column.getValue());
            }
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

    public void writeCommandCell(UrlCommandCell cmd) throws IOException
    {
        if (!cmd.isCacheable()) {
            writeAttribute("cache", cmd.isCacheable(), true);
        }
        if (cmd.getUrl() != null) {
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
    public void writeType(String type) throws IOException
    {
        if (type == null) {
            return;
        }

        writeAttribute("type", type, true);
    }

    private static String getColumnType(Object o)
    {
        if (o instanceof Range || o instanceof RangeSet) {
            return null;
        }

        if (o instanceof LatLon) {
            return "latlon";
        }

        if (o instanceof Point2D) {
            return "point2d";
        }

        if (o instanceof Point3D) {
            return "point3d";
        }

        return getCellType(o, "column");
    }

    static String getCellType(Object cell, String type)
    {
        if (cell == null || cell instanceof String || cell instanceof Double || cell instanceof Long || cell instanceof Boolean) {
            return null;
        }

        if (cell instanceof BigDecimal) {
            return "bigdec";
        }

        if (cell instanceof Float) {
            return "float";
        }

        if (cell instanceof Integer) {
            return "int";
        }

        if (cell instanceof BigInteger) {
            return "bigint";
        }

        if (cell instanceof Byte) {
            return "byte";
        }

        if (cell instanceof Short) {
            return "short";
        }

        if (cell instanceof Date) {
            return "date";
        }

        if (cell instanceof BinaryUrlCmd || cell instanceof byte[]) {
            return "binary";
        }

        if (cell instanceof GroovyExpression || cell instanceof Collection || cell.getClass().isArray()) {
            return "exp";
        }

        if (cell instanceof GroovyMethod) {
            return "method";
        }

        if (cell instanceof GroovyTemplate) {
            return "template";
        }

        if (cell instanceof StringUrlCmd) {
            return "string";
        }

        throw new IllegalArgumentException(String.format("Unsupported type %s located in %s", cell.getClass().getName(), type));
    }

    public void writeCells(Map<Set<Column>, ?> cells) throws IOException
    {
        builder.append("\"cells\":");
        if (cells == null || cells.isEmpty()) {
            builder.append("[]");
            return;
        }
        startArray();
        for (Map.Entry<Set<Column>, ?> cell : cells.entrySet())
        {
            startObject();
            writeIds(cell);
            writeType(getCellType(cell.getValue(), "cell"));

            if ((cell.getValue() instanceof UrlCommandCell)) {
                writeCommandCell((UrlCommandCell)cell.getValue());
            } else {
                writeValue("value", cell.getValue());
            }
            endObject();
            comma();
        }
        uncomma();
        endArray();
    }


    public void writeIds(Map.Entry<Set<Column>, ?> item) throws IOException
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

    public void writeId(Long longId, boolean addComma) throws IOException
    {
        builder.append(String.format(quotedStringFormat, "id"));
        builder.append(':');
        writeIdValue(longId, addComma);
    }

    public void writeIdValue(Long longId, boolean addComma) throws IOException
    {
        Object userId = userIds.get(longId);

        if (userId != null) {
            writeObject(userId);
        } else {
            Long generatedId = generatedIds.get(longId);

            if (generatedId == null) {
                generatedId = ++idCounter;
                generatedIds.put(longId, generatedId);
            }

            builder.append(generatedId);
            builder.append(".0");
        }

        if (addComma) {
            comma();
        }
    }

}