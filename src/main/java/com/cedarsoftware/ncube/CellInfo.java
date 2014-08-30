package com.cedarsoftware.ncube;

import com.cedarsoftware.ncube.proximity.LatLon;
import com.cedarsoftware.ncube.proximity.Point2D;
import com.cedarsoftware.ncube.proximity.Point3D;
import com.cedarsoftware.util.DateUtilities;
import com.cedarsoftware.util.StringUtilities;
import com.cedarsoftware.util.io.JsonObject;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Date;
import java.util.regex.Matcher;

/**
 * Get information about a cell (makes it a uniform query-able object).  Optional method
 * exists to collapse types for UI.
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
public class CellInfo
{
    public String value;
    public String dataType;
    public boolean isUrl;
    public boolean isCached;

    public CellInfo(Object cell)
    {
        isUrl = false;
        isCached = false;
        value = null;
        dataType = null;

        if (cell == null)
        {
            return;
        }

        if (cell instanceof String)
        {
            value = (String) cell;
            dataType = CellTypes.String.desc();
        }
        else if (cell instanceof Long)
        {
            value = cell.toString();
            dataType = CellTypes.Long.desc();
        }
        else if (cell instanceof Boolean)
        {
            value = cell.toString();
            dataType = CellTypes.Boolean.desc();
        }
        else if (cell instanceof GroovyExpression)
        {
            GroovyExpression exp = (GroovyExpression) cell;
            isUrl = StringUtilities.hasContent(exp.getUrl());
            value = isUrl ? exp.getUrl() : exp.getCmd();
            dataType = CellTypes.Exp.desc();
            isCached = true;
        }
        else if (cell instanceof Byte)
        {
            value = cell.toString();
            dataType = CellTypes.Byte.desc();
        }
        else if (cell instanceof Short)
        {
            value = cell.toString();
            dataType = CellTypes.Short.desc();
        }
        else if (cell instanceof Integer)
        {
            value = cell.toString();
            dataType = CellTypes.Integer.desc();
        }
        else if (cell instanceof Date)
        {
            value = Column.formatDiscreteValue((Date) cell);
            dataType = CellTypes.Date.desc();
        }
        else if (cell instanceof Double)
        {
            value = Column.formatDiscreteValue((Double) cell);
            dataType = CellTypes.Double.desc();
        }
        else if (cell instanceof Float)
        {
            value = Column.formatDiscreteValue((Float) cell);
            dataType = CellTypes.Float.desc();
        }
        else if (cell instanceof BigDecimal)
        {
            value = Column.formatDiscreteValue((BigDecimal) cell);
            dataType = CellTypes.BigDecimal.desc();
        }
        else if (cell instanceof BigInteger)
        {
            value = Column.formatDiscreteValue((BigInteger) cell);
            dataType = CellTypes.BigInteger.desc();
        }
        else if (cell instanceof byte[])
        {
            value = StringUtilities.encode((byte[]) cell);
            dataType = CellTypes.Binary.desc();
        }
        else if (cell instanceof Point2D)
        {
            value = cell.toString();
            dataType = CellTypes.Point2D.desc();
        }
        else if (cell instanceof Point3D)
        {
            value = cell.toString();
            dataType = CellTypes.Point3D.desc();
        }
        else if (cell instanceof LatLon)
        {
            value = cell.toString();
            dataType = CellTypes.LatLon.desc();
        }
        else if (cell instanceof GroovyMethod)
        {
            GroovyMethod method = (GroovyMethod) cell;
            value = method.getCmd();
            dataType = CellTypes.Method.desc();
            isUrl = StringUtilities.hasContent(method.getUrl());
            isCached = true;
        }
        else if (cell instanceof StringUrlCmd)
        {
            StringUrlCmd strCmd = (StringUrlCmd) cell;
            value = strCmd.getUrl();
            dataType = CellTypes.String.desc();
            isUrl = true;
            isCached = strCmd.isCacheable();
        }
        else if (cell instanceof BinaryUrlCmd)
        {
            BinaryUrlCmd binCmd = (BinaryUrlCmd) cell;
            value = binCmd.getUrl();
            dataType = CellTypes.Binary.desc();
            isUrl = true;
            isCached = binCmd.isCacheable();
        }
        else if (cell instanceof GroovyTemplate)
        {
            GroovyTemplate templateCmd = (GroovyTemplate) cell;
            isUrl = StringUtilities.hasContent(templateCmd.getUrl());
            value = isUrl ? templateCmd.getUrl() : templateCmd.getCmd();
            dataType = CellTypes.Template.desc();
            isCached = templateCmd.isCacheable();
        }
        else
        {
            throw new IllegalStateException("Unknown cell value type, value: " + cell.toString() + ", class: " + cell.getClass().getName());
        }
    }

    /**
     * Collapse: byte, short, int ==> long
     * Collapse: float ==> double
     * Collapse: BigInteger ==> BigDecimal
     */
    public void collapseToUiSupportedTypes()
    {
        if (CellTypes.Byte.desc().equals(dataType) || CellTypes.Short.desc().equals(dataType) || CellTypes.Integer.desc().equals(dataType))
        {
            dataType = CellTypes.Long.desc();
        }
        else if (CellTypes.Float.desc().equals(dataType))
        {
            dataType = CellTypes.Double.desc();
        }
        else if (CellTypes.BigInteger.desc().equals(dataType))
        {
            dataType = CellTypes.BigDecimal.desc();
        }
    }

    public static Object parseJsonValue(final Object val, final String url, final String type, boolean cache)
    {
        if (url != null)
        {
            if (CellTypes.Exp.desc().equalsIgnoreCase(type))
            {
                return new GroovyExpression(null, url);
            }
            else if (CellTypes.Method.desc().equalsIgnoreCase(type))
            {
                return new GroovyMethod(null, url);
            }
            else if (CellTypes.Template.desc().equalsIgnoreCase(type))
            {
                return new GroovyTemplate(null, url, cache);
            }
            else if (CellTypes.String.desc().equalsIgnoreCase(type))
            {
                return new StringUrlCmd(url, cache);
            }
            else if (CellTypes.Binary.desc().equalsIgnoreCase(type))
            {
                return new BinaryUrlCmd(url, cache);
            }
            else
            {
                throw new IllegalArgumentException("url can only be specified with 'exp', 'method', 'template', 'string', or 'binary' types");
            }
        }

        return parseJsonValue(type, val);
    }

    static Object parseJsonValue(String type, Object val)
    {
        if (CellTypes.Null.desc().equals(val) || val == null)
        {
            return null;
        }
        else if (val instanceof Double)
        {
            if (CellTypes.BigDecimal.desc().equals(type))
            {
                return new BigDecimal((Double)val);
            }
            else if (CellTypes.Float.desc().equals(type))
            {
                return ((Double)val).floatValue();
            }
            return val;
        }
        else if (val instanceof Long)
        {
            if (CellTypes.Integer.desc().equals(type))
            {
                return ((Long)val).intValue();
            }
            else if (CellTypes.BigInteger.desc().equals(type))
            {
                return new BigInteger(val.toString());
            }
            else if (CellTypes.Byte.desc().equals(type))
            {
                return ((Long)val).byteValue();
            }
            else if (CellTypes.Short.desc().equals(type))
            {
                return (((Long) val).shortValue());
            }
            else if (CellTypes.BigDecimal.desc().equals(type))
            {
                return new BigDecimal((Long)val);
            }
            return val;
        }
        else if (val instanceof Boolean)
        {
            return val;
        }
        else if (val instanceof String)
        {
            val = ((String) val).trim();
            if (StringUtilities.isEmpty(type))
            {
                return val;
            }
            else if (CellTypes.Boolean.desc().equals(type))
            {
                return "true".equalsIgnoreCase((String)val);
            }
            else if (CellTypes.Byte.desc().equals(type))
            {
                return Byte.parseByte((String)val);
            }
            else if (CellTypes.Short.desc().equals(type))
            {
                return Short.parseShort((String)val);
            }
            else if (CellTypes.Integer.desc().equals(type))
            {
                return Integer.parseInt((String)val);
            }
            else if (CellTypes.Long.desc().equals(type))
            {
                return Long.parseLong((String)val);
            }
            else if (CellTypes.Double.desc().equals(type))
            {
                return Double.parseDouble((String)val);
            }
            else if (CellTypes.Float.desc().equals(type))
            {
                return Float.parseFloat((String)val);
            }
            else if (CellTypes.Exp.desc().equals(type))
            {
                return new GroovyExpression((String)val, null);
            }
            else if (CellTypes.Method.desc().equals(type))
            {
                return new GroovyMethod((String) val, null);
            }
            else if (CellTypes.Date.desc().equals(type) || "datetime".equals(type))
            {
                try
                {
                    Date date = DateUtilities.parseDate((String) val);
                    return (date == null) ? val : date;
                }
                catch (Exception e)
                {
                    throw new IllegalArgumentException("Could not parse '" + type + "': " + val);
                }
            }
            else if (CellTypes.Template.desc().equals(type))
            {
                return new GroovyTemplate((String)val, null, true);
            }
            else if (CellTypes.String.desc().equals(type))
            {
                return val;
            }
            else if (CellTypes.Binary.desc().equals(type))
            {   // convert hex string "10AF3F" as byte[]
                return StringUtilities.decode((String) val);
            }
            else if (CellTypes.BigInteger.desc().equals(type))
            {
                return new BigInteger((String) val);
            }
            else if (CellTypes.BigDecimal.desc().equals(type))
            {
                return new BigDecimal((String)val);
            }
            else if (CellTypes.LatLon.desc().equals(type))
            {
                Matcher m = Regexes.valid2Doubles.matcher((String) val);
                if (!m.matches())
                {
                    throw new IllegalArgumentException(String.format("Illegal Lat/Long value (%s)", val));
                }
                return new LatLon(Double.parseDouble(m.group(1)), Double.parseDouble(m.group(2)));
            }
            else if (CellTypes.Point2D.desc().equals(type))
            {
                Matcher m = Regexes.valid2Doubles.matcher((String) val);
                if (!m.matches())
                {
                    throw new IllegalArgumentException(String.format("Illegal Point2D value (%s)", val));
                }
                return new Point2D(Double.parseDouble(m.group(1)), Double.parseDouble(m.group(2)));
            }
            else if (CellTypes.Point3D.desc().equals(type))
            {
                Matcher m = Regexes.valid3Doubles.matcher((String) val);
                if (!m.matches())
                {
                    throw new IllegalArgumentException(String.format("Illegal Point3D value (%s)", val));
                }
                return new Point3D(Double.parseDouble(m.group(1)), Double.parseDouble(m.group(2)), Double.parseDouble(m.group(3)));
            }
            else
            {
                throw new IllegalArgumentException("Unknown value (" + type + ") for 'type' field");
            }
        }
        else if (val instanceof JsonObject)
        {   // Legacy support - remove once we drop support for array type (can be done using GroovyExpression).
            Object[] values = ((JsonObject) val).getArray();
            for (int i=0; i < values.length; i++)
            {
                values[i] = parseJsonValue(values[i], null, type, false);
            }
            return values;
        }
        else
        {
            throw new IllegalArgumentException("Error reading value of type '" + val.getClass().getName() + "' - Simple JSON format for NCube only supports Long, Double, String, String Date, Boolean, or null");
        }
    }

}
