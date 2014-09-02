package com.cedarsoftware.ncube.formatters;

import com.cedarsoftware.ncube.CellInfo;
import com.cedarsoftware.ncube.CellTypes;
import com.cedarsoftware.ncube.GroovyExpression;
import com.cedarsoftware.ncube.Range;
import com.cedarsoftware.ncube.RangeSet;
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

/**
 * Created by kpartlow on 8/12/2014.
 */
public class GroovyJsonFormatter
{
    static final SafeSimpleDateFormat dateFormat = new SafeSimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");

    protected StringBuilder builder = new StringBuilder();
    protected String quotedStringFormat = "\"%s\"";

    public static String getColumnType(Object o)
    {
        if (o instanceof Range || o instanceof RangeSet) {
            return null;
        }

        return CellTypes.getType(o, "column");
    }


    public void startArray() {
        builder.append("[");
    }

    public void endArray() {
        builder.append("]");
    }

    public void startObject() {
        builder.append("{");
    }

    public void endObject() {
        builder.append("}");
    }

    public void comma() {
        builder.append(",");
    }

    public void writeValue(String attr, Object o) throws IOException
    {
        builder.append(String.format(quotedStringFormat, attr));
        builder.append(':');
        writeObject(o);
    }

    public void writeObject(Object o) throws IOException
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

    protected void uncomma()
    {
        builder.setLength(builder.length() - 1);
    }

    void writeGroovyObject(Object o)
    {
        if (o instanceof String)
        {
            builder.append("'");
            builder.append(o.toString());
            builder.append("'");
        }
        else if (o instanceof GroovyExpression)
        {
            builder.append("'");
            builder.append(((GroovyExpression) o).getCmd());
            builder.append("'");
        }
        else if (o instanceof Boolean)
        {
            builder.append((Boolean) o ? "true" : "false");
        }
        else if (o instanceof Double)
        {
            builder.append(CellInfo.formatForEditing(o));
            builder.append('d');
        }
        else if (o instanceof Integer)
        {
            builder.append(o);
            builder.append('i');
        }
        else if (o instanceof Long)
        {
            builder.append(o);
            builder.append('L');
        }
        else if (o instanceof BigDecimal)
        {
            builder.append(((BigDecimal) o).stripTrailingZeros().toPlainString());
            builder.append('g');
        }
        else if (o instanceof BigInteger)
        {
            builder.append(o);
            builder.append('g');
        }
        else if (o instanceof Byte)
        {
            builder.append(o);
            builder.append(" as Byte");
        }
        else if (o instanceof Float)
        {
            builder.append(CellInfo.formatForEditing(o));
            builder.append('f');
        }
        else if (o instanceof Short)
        {
            builder.append(o);
            builder.append(" as Short");
        }
        else
        {
            throw new IllegalArgumentException("Unknown Groovy Type : " + o.getClass());
        }
    }

    public void writeAttribute(String attr, Object value, boolean includeComma) throws IOException
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
