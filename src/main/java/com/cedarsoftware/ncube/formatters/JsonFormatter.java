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
import com.cedarsoftware.util.StringUtilities;
import com.cedarsoftware.util.io.JsonWriter;

import java.io.IOException;
import java.io.StringWriter;
import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
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
public class JsonFormatter implements NCubeFormatter
{
    private StringBuilder _builder = new StringBuilder();

    static final ThreadLocal<SimpleDateFormat> _dateFormat = new ThreadLocal<SimpleDateFormat>()
    {
        public SimpleDateFormat initialValue()
        {
            return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
        }
    };

    private String _quotedStringFormat = "\"%s\"";
    private String _singleDoubleFormat = "%f";
    private String _twoDoubleFormat = "\"%f,%f\"";
    private String _threeDoubleFormat = "\"%f,%f,%f\"";

    private Map<Long, Object> _userIds = new HashMap<Long, Object>();
    private Map<Long, Long> _generatedIds = new HashMap<Long, Long>();
    private long _idCounter;

    //private NCube _ncube;
    /*
    public String format(Collection<NCube> cubes)
    {
        startArray();
        for (NCube c : cubes)
        {
            writeNCube(c);
        }
        endArray();
        return _builder.toString();
    }
    */

    /**
     * Use this API to generate a JSON view of this NCube.
     *
     * @return String containing a JSON view of this NCube.
     */
    public String format(NCube ncube)
    {
        writeNCube(ncube);
        return _builder.toString();
    }

    public void writeNCube(NCube ncube) {
        try
        {
            _userIds.clear();
            _generatedIds.clear();
            _idCounter = 0;

            walkIds(ncube.getAxes());

            startObject();

            writeAttribute("ncube", ncube.getName(), true);
            writeAttribute("ruleMode", ncube.getRuleMode(), true);
            if (ncube.getDefaultCellValue() != null)
            {
                writeValue("defaultCellValue", ncube.getDefaultCellValue());
                comma();
            }
            writeAxes(ncube.getAxes());
            writeCells(ncube.getCellMap());

            endObject();
        }
        catch (Exception e)
        {
            throw new IllegalStateException(String.format("Unable to format NCube '%s' into JSON", ncube.getName()), e);
        }
    }

    public void walkIds(List<Axis> axes)
    {
        HashSet<Comparable> set = new HashSet<Comparable>();
        for (Axis item : axes)
        {
            for (Column c : item.getColumns())
            {
                if (!c.isDefault())
                {
                    // only use value for id on String and Longs
                    if ((c.getValue() instanceof String) || (c.getValue() instanceof Long))
                    {
                        if (!set.contains(c.getValue()))
                        {
                            set.add(c.getValue());
                            _userIds.put(c.getId(), c.getValue());
                        }
                    }
                }
            }
        }
    }

    public void startArray()
    {
        _builder.append("[");
    }

    public void endArray()
    {
        _builder.append("]");
    }

    public void startObject()
    {
        _builder.append("{");
    }

    public void endObject()
    {
        _builder.append("}");
    }

    public void comma()
    {
        _builder.append(",");
    }

    public void writeAxes(List<Axis> axes) throws IOException
    {
        _builder.append(String.format(_quotedStringFormat, "axes"));
        _builder.append(':');
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

    public void writeAxis(Axis a) throws IOException
    {
        startObject();

        // required inputs
        writeAttribute("name", a.getName(), true);
        writeAttribute("type", a.getType().name(), true);
        writeAttribute("valueType", a.getValueType().name(), true);

        //  optional inputs that can use defaults
        writeAttribute("preferredOrder", a.getColumnOrder(), true);
        writeAttribute("hasDefault", a.hasDefaultColumn(), true);
        writeAttribute("multiMatch", a.isMultiMatch(), true);

        writeColumns(a.getColumns());
        endObject();
    }

    public void writeColumns(List<Column> columns) throws IOException
    {
        _builder.append("\"columns\":");

        /*
        Always have at least one default column
        if (columns == null || columns.isEmpty()) {
            _builder.append("[]");
            return;
        }
         */

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

    public void writeColumn(Column c) throws IOException
    {
        startObject();
        //  Check to see if id exists anywhere. then optimize

        Object o = _userIds.get(c.getId());

        if (o != null && o.equals(c.getValue()))
        {
            writeType(getColumnType(c.getValue()));
            writeId(c.getId(), false);
        }
        else
        {
            writeId(c.getId(), true);
            writeType(getColumnType(c.getValue()));
            if (c.getValue() instanceof UrlCommandCell)
            {
                writeCommandCell((UrlCommandCell) c.getValue());
            }
            else
            {
                writeValue("value", c.getValue());
            }
        }
        endObject();
    }

    public void writeCommandCell(UrlCommandCell cmd) throws IOException
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
            if (cmd.getCmd() == null)
            {
                throw new IllegalStateException("Command and URL cannot both be null on a command cell");
            }
            writeAttribute("value", cmd.getCmd(), false);
        }
    }

    /**
     * According to parseJsonValue reading in, if your item is one of the following end types it does not need to
     * specify the end type:  String, Long, Boolean, Double.  These items will all be picked up automatically
     * so to save on those types I don't write out the type.
     *
     * @param type Type to write, if null don't write anything because its a default type
     */
    public void writeType(String type) throws IOException
    {
        if (type == null)
        {
            return;
        }

        writeAttribute("type", type, true);
    }

    private String getColumnType(Object o) throws IOException
    {
        if (o instanceof Range || o instanceof RangeSet)
        {
            return null;
        }

        if (o instanceof LatLon)
        {
            return "latlon";
        }

        if (o instanceof Point2D)
        {
            return "point2d";
        }

        if (o instanceof Point3D)
        {
            return "point3d";
        }

        return getCellType(o, "column");
    }

    String getCellType(Object cell, String type) throws IOException
    {
        if (cell == null || (cell instanceof String) || (cell instanceof Double) || (cell instanceof Long) || (cell instanceof Boolean))
        {
            return null;
        }

        if (cell instanceof BigDecimal)
        {
            return "bigdec";
        }

        if (cell instanceof Float)
        {
            return "float";
        }

        if (cell instanceof Integer)
        {
            return "int";
        }

        if (cell instanceof BigInteger)
        {
            return "bigint";
        }

        if (cell instanceof Byte)
        {
            return "byte";
        }

        if (cell instanceof Short)
        {
            return "short";
        }

        if (cell instanceof Date)
        {
            return "date";
        }

        if (cell instanceof BinaryUrlCmd || cell instanceof byte[])
        {
            return "binary";
        }

        if (cell instanceof GroovyExpression || cell instanceof Collection || cell.getClass().isArray())
        {
            return "exp";
        }

        if (cell instanceof GroovyMethod)
        {
            return "method";
        }

        if (cell instanceof GroovyTemplate)
        {
            return "template";
        }

        if (cell instanceof StringUrlCmd)
        {
            return "string";
        }

        throw new IllegalArgumentException(String.format("Unsupported %s Type:  %s", cell.getClass().getName(), type));
    }

    public void writeCells(Map<Set<Column>, ?> cells) throws IOException
    {
        _builder.append("\"cells\":");
        if (cells == null || cells.isEmpty())
        {
            _builder.append("[]");
            return;
        }
        startArray();
        for (Map.Entry<Set<Column>, ?> cell : cells.entrySet())
        {
            startObject();
            writeIds(cell);
            writeType(getCellType(cell.getValue(), "cell"));

            if ((cell.getValue() instanceof UrlCommandCell))
            {
                writeCommandCell((UrlCommandCell) cell.getValue());
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


    public void writeIds(Map.Entry<Set<Column>, ?> item) throws IOException
    {

        _builder.append("\"id\":");
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

    private void uncomma()
    {
        _builder.setLength(_builder.length() - 1);
    }

    void writeGroovyObject(Object o) throws IOException
    {
        if (o instanceof String)
        {
            _builder.append("'");
            _builder.append(o.toString());
            _builder.append("'");
            return;
        }

        if (o instanceof GroovyExpression)
        {
            _builder.append("'");
            _builder.append(((GroovyExpression) o).getCmd());
            _builder.append("'");
            return;
        }

        if (o instanceof Double)
        {
            _builder.append(String.format(_singleDoubleFormat, ((Double) o).doubleValue()));
            _builder.append('d');
            return;
        }

        if (o instanceof Float)
        {
            _builder.append(String.format(_singleDoubleFormat, ((Float) o).floatValue()));
            _builder.append('f');
            return;
        }

        if (o instanceof Integer)
        {
            _builder.append(o);
            _builder.append('i');
            return;
        }

        if (o instanceof Long)
        {
            _builder.append(o);
            _builder.append('L');
            return;
        }

        if (o instanceof Byte)
        {
            _builder.append(o);
            _builder.append("as Byte");
            return;
        }

        if (o instanceof Short)
        {
            _builder.append(o);
            _builder.append("as Short");
            return;
        }

        if (o instanceof BigDecimal)
        {
            _builder.append(((BigDecimal) o).toPlainString());
            _builder.append('g');
            return;
        }

        if (o instanceof BigInteger)
        {
            _builder.append(o);
            _builder.append('g');
            return;
        }

        if (o instanceof Boolean)
        {
            _builder.append(((Boolean) o).booleanValue() ? "true" : "false");
            return;
        }

        throw new IllegalArgumentException("Unknown Groovy Type : " + o.getClass());
    }

    void writeObject(Object o) throws IOException
    {
        if (o == null)
        {
            _builder.append("null");
            return;
        }

        if (o instanceof String)
        {
            StringWriter w = new StringWriter();
            JsonWriter.writeJsonUtf8String(o.toString(), w);
            _builder.append(w.toString());
            return;
        }

        if (o instanceof LatLon)
        {
            LatLon l = (LatLon) o;
            _builder.append(String.format(_twoDoubleFormat, l.getLat(), l.getLon()));
            return;
        }

        if (o instanceof Point2D)
        {
            Point2D l = (Point2D) o;
            _builder.append(String.format(_twoDoubleFormat, l.getX(), l.getY()));
            return;
        }

        if (o instanceof Point3D)
        {
            Point3D p = (Point3D) o;
            _builder.append(String.format(_threeDoubleFormat, p.getX(), p.getY(), p.getZ()));
            return;
        }

        if (o instanceof Range)
        {
            Range r = (Range) o;
            startArray();
            writeObject(r.getLow());
            comma();
            writeObject(r.getHigh());
            endArray();
            return;
        }

        if (o instanceof RangeSet)
        {
            RangeSet r = (RangeSet) o;
            Iterator i = r.iterator();
            startArray();
            while (i.hasNext())
            {
                writeObject(i.next());
                comma();
            }
            uncomma();
            endArray();
            return;
        }

        if (o instanceof Date)
        {
            _builder.append(String.format(_quotedStringFormat, _dateFormat.get().format((Date) o)));
            return;
        }

        if (o instanceof byte[])
        {
            _builder.append(String.format(_quotedStringFormat, StringUtilities.encode((byte[]) o)));
            return;
        }


        if (o.getClass().isArray())
        {
            //Class c = o.getClass().getComponentType();
            //  check types
            _builder.append("\"");
            startArray();
            int len = Array.getLength(o);
            for (int i = 0; i < len; i++)
            {
                writeGroovyObject(Array.get(o, i));
                comma();
            }
            uncomma();
            endArray();
            _builder.append(" as Object[]");
            _builder.append("\"");
            return;
        }

        /**  Never Called.  Keeping just to make sure not needed.
         if (o instanceof UrlCommandCell) {
         UrlCommandCell cmd = (UrlCommandCell)o;
         if (cmd.getUrl() != null)
         {
         writeObject(cmd.getUrl());
         }
         else
         {
         if (cmd.getCmd() == null)
         {
         throw new IllegalStateException("Command and URL cannot both be null, n-cube: " + ncube.getName());
         }
         writeObject(cmd.getCmd());
         }
         return;
         }

         if (o instanceof Collection) {
         Collection c = (Collection)o;
         startArray();
         for (Object it : c) {
         writeObject(it);
         comma();
         }
         uncomma();
         endArray();
         return;
         }
         */

        //  TODO: verify - Other types handled as string with no extra quotes (Long, Byte, Short), etc.
        _builder.append(o.toString());

    }

    public void writeValue(String attr, Object o) throws IOException
    {
        _builder.append(String.format(_quotedStringFormat, attr));
        _builder.append(':');

        writeObject(o);
    }

    public void writeId(Long longId, boolean addComma) throws IOException
    {
        _builder.append(String.format(_quotedStringFormat, "id"));
        _builder.append(':');

        writeIdValue(longId, addComma);
    }

    public void writeIdValue(Long longId, boolean addComma) throws IOException
    {

        Object userId = _userIds.get(longId);

        if (userId != null)
        {
            writeObject(userId);
        }
        else
        {
            Long generatedId = _generatedIds.get(longId);

            if (generatedId == null)
            {
                generatedId = ++_idCounter;
                _generatedIds.put(longId, generatedId);
            }

            _builder.append(generatedId);
            _builder.append(".0");
        }

        if (addComma)
        {
            comma();
        }
    }

    public void writeAttribute(String name, String value, boolean includeComma) throws IOException
    {
        StringWriter w = new StringWriter(value.length());
        JsonWriter.writeJsonUtf8String(value, w);

        _builder.append(String.format(_quotedStringFormat, name));
        _builder.append(':');
        _builder.append(w.toString());

        if (includeComma)
        {
            _builder.append(",");
        }
    }

    public void writeAttribute(String name, long value, boolean includeComma)
    {
        _builder.append(String.format(_quotedStringFormat, name));
        _builder.append(":");
        _builder.append(value);
        if (includeComma)
        {
            _builder.append(",");
        }
    }

    public void writeAttribute(String name, boolean value, boolean includeComma)
    {
        _builder.append(String.format(_quotedStringFormat, name));
        _builder.append(":");
        _builder.append(value);
        if (includeComma)
        {
            _builder.append(",");
        }
    }
}