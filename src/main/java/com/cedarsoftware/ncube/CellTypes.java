package com.cedarsoftware.ncube;

import com.cedarsoftware.ncube.proximity.LatLon;
import com.cedarsoftware.ncube.proximity.Point2D;
import com.cedarsoftware.ncube.proximity.Point3D;
import com.cedarsoftware.util.DateUtilities;
import com.cedarsoftware.util.StringUtilities;

import java.text.MessageFormat;
import java.util.Collection;
import java.util.regex.Matcher;

/**
 * Allowed cell types for n-cube.
 *
 * @author John DeRegnaucourt (jdereg@gmail.com)
 *         <br>
 *         Copyright (c) Cedar Software LLC
 *         <br><br>
 *         Licensed under the Apache License, Version 2.0 (the "License");
 *         you may not use this file except in compliance with the License.
 *         You may obtain a copy of the License at
 *         <br><br>
 *         http://www.apache.org/licenses/LICENSE-2.0
 *         <br><br>
 *         Unless required by applicable law or agreed to in writing, software
 *         distributed under the License is distributed on an "AS IS" BASIS,
 *         WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *         See the License for the specific language governing permissions and
 *         limitations under the License.
 */
public enum CellTypes
{
    String("string",
        new CellRecreator() { public Object recreate(String value, boolean isUrl, boolean isCached) { return isUrl ? new StringUrlCmd(value, isCached) : value; }}
    ),
    Date("date",
        new CellRecreator() { public Object recreate(String value, boolean isUrl, boolean isCached) { return DateUtilities.parseDate(value); }}
    ),
    Boolean("boolean",
        new CellRecreator() { public Object recreate(String value, boolean isUrl, boolean isCached) { return java.lang.Boolean.valueOf(value); }}
    ),
    Byte("byte",
        new CellRecreator() { public Object recreate(String value, boolean isUrl, boolean isCached) { return java.lang.Byte.valueOf(value); }}
    ),
    Short("short",
        new CellRecreator() { public Object recreate(String value, boolean isUrl, boolean isCached) { return java.lang.Short.valueOf(value); }}
    ),
    Integer("int",
        new CellRecreator() { public Object recreate(String value, boolean isUrl, boolean isCached) { return java.lang.Integer.valueOf(value); }}
    ),
    Long("long",
        new CellRecreator() { public Object recreate(String value, boolean isUrl, boolean isCached) { return java.lang.Long.valueOf(value); }}
    ),
    Float("float",
        new CellRecreator() { public Object recreate(String value, boolean isUrl, boolean isCached) { return java.lang.Float.valueOf(value); }}
    ),
    Double("double",
        new CellRecreator() { public Object recreate(String value, boolean isUrl, boolean isCached) { return java.lang.Double.valueOf(value); }}
    ),
    BigDecimal("bigdec",
        new CellRecreator() { public Object recreate(String value, boolean isUrl, boolean isCached) { return new java.math.BigDecimal(value); }}
    ),
    BigInteger("bigint",
        new CellRecreator() { public Object recreate(String value, boolean isUrl, boolean isCached) { return new java.math.BigInteger(value); }}
    ),
    Binary("binary",
        new CellRecreator() { public Object recreate(String value, boolean isUrl, boolean isCached) { return isUrl ? new BinaryUrlCmd(value, isCached) : StringUtilities.decode(value); }}
    ),
    Exp("exp",
        new CellRecreator() { public Object recreate(String value, boolean isUrl, boolean isCached) { return new GroovyExpression(isUrl ? null : value, isUrl ? value : null, isCached); }}
    ),
    Method("method",
        new CellRecreator() { public Object recreate(String value, boolean isUrl, boolean isCached) { return new GroovyMethod(isUrl ? null : value, isUrl ? value : null, isCached); }}
    ),
    Template("template",
        new CellRecreator() { public Object recreate(String value, boolean isUrl, boolean isCached) { return new GroovyTemplate(isUrl ? null : value, isUrl ? value : null, isCached); }}
    ),
    LatLon("latlon",
        new CellRecreator() { public Object recreate(String value, boolean isUrl, boolean isCached) {
            Matcher m = Regexes.valid2Doubles.matcher(value);
            if (!m.matches())
            {
                throw new IllegalArgumentException(java.lang.String.format("Invalid Lat/Long value (%s)", value));
            }
            return new LatLon(java.lang.Double.parseDouble(m.group(1)), java.lang.Double.parseDouble(m.group(2)));
        }}
    ),
    Point2D("point2d",
        new CellRecreator() { public Object recreate(String value, boolean isUrl, boolean isCached) {
            Matcher m = Regexes.valid2Doubles.matcher(value);
            if (!m.matches())
            {
                throw new IllegalArgumentException(java.lang.String.format("Invalid Point2D value (%s)", value));
            }
            return new Point2D(java.lang.Double.parseDouble(m.group(1)), java.lang.Double.parseDouble(m.group(2)));
        }}
    ),
    Point3D("point3d",
        new CellRecreator() { public Object recreate(String value, boolean isUrl, boolean isCached) {
            Matcher m = Regexes.valid3Doubles.matcher(value);
            if (!m.matches())
            {
                throw new IllegalArgumentException(java.lang.String.format("Invalid Point3D value (%s)", value));
            }
            return new Point3D(java.lang.Double.parseDouble(m.group(1)), java.lang.Double.parseDouble(m.group(2)), java.lang.Double.parseDouble(m.group(3)));
        }}
    ),
    Null("null",
        new CellRecreator() { public Object recreate(String value, boolean isUrl, boolean isCached) { return null; }}
    );

    private final String desc;
    private final CellRecreator recreator;

    private CellTypes(String desc, CellRecreator recreator)
    {
        this.desc = desc;
        this.recreator = recreator;
    }

    public String desc()
    {
        return desc;
    }

    public Object recreate(String value, boolean isUrl, boolean isCached) {
        if (value == null) {
            return null;
        }
        return recreator.recreate(value, isUrl, isCached);
    }

    public static CellTypes getTypeFromString(String type)
    {
        if (type == null) {
            return CellTypes.String;
        }

        switch (type)
        {
            case "string":
                return CellTypes.String;

            case "date":
                return CellTypes.Date;

            case "boolean":
                return CellTypes.Boolean;

            case "byte":
                return CellTypes.Byte;

            case "short":
                return CellTypes.Short;

            case "int":
                return CellTypes.Integer;

            case "long":
                return CellTypes.Long;

            case "float":
                return CellTypes.Float;

            case "double":
                return CellTypes.Double;

            case "bigdec":
                return CellTypes.BigDecimal;

            case "bigint":
                return CellTypes.BigInteger;

            case "binary":
                return CellTypes.Binary;

            case "exp":
                return CellTypes.Exp;

            case "method":
                return CellTypes.Method;

            case "template":
                return CellTypes.Template;

            case "latlon":
                return CellTypes.LatLon;

            case "point2d":
                return CellTypes.Point2D;

            case "point3d":
                return CellTypes.Point3D;

            default:
                throw new IllegalArgumentException("Invalid Type:  " + type);
        }
    }

    public static String getType(Object cell, String section)
    {
        if (cell == null) {
            return null;
        }

        if (cell instanceof String) {
            return String.desc();
        }

        if (cell instanceof Double) {
            return Double.desc();
        }

        if (cell instanceof Long) {
            return Long.desc();
        }

        if (cell instanceof Boolean) {
            return Boolean.desc();
        }

        if (cell instanceof java.math.BigDecimal) {
            return BigDecimal.desc();
        }

        if (cell instanceof Float) {
            return Float.desc();
        }

        if (cell instanceof Integer) {
            return Integer.desc();
        }

        if (cell instanceof java.math.BigInteger) {
            return BigInteger.desc();
        }

        if (cell instanceof Byte) {
            return Byte.desc();
        }

        if (cell instanceof Short) {
            return Short.desc();
        }

        if (cell instanceof java.util.Date) {
            return Date.desc();
        }

        if (cell instanceof BinaryUrlCmd || cell instanceof byte[]) {
            return Binary.desc();
        }

        if (cell instanceof GroovyExpression || cell instanceof Collection || cell.getClass().isArray()) {
            return Exp.desc();
        }

        if (cell instanceof GroovyMethod) {
            return Method.desc();
        }

        if (cell instanceof GroovyTemplate) {
            return Template.desc();
        }

        if (cell instanceof StringUrlCmd) {
            return String.desc();
        }

        if (cell instanceof Point2D)
        {
            return Point2D.desc();
        }

        if (cell instanceof Point3D)
        {
            return CellTypes.Point3D.desc();
        }

        if (cell instanceof LatLon)
        {
            return LatLon.desc();
        }

        if (cell instanceof ClassLoader)
        {   // Returned as type='exp' because the JSON is returned as a String[] of URLs
            // The JSON and HTML formatter turn URLClassLoaders back into their URL lists.
            return "exp";
        }
        throw new IllegalArgumentException(MessageFormat.format("Unsupported type {0} found in {1}", cell.getClass().getName(), section));
    }

    private interface CellRecreator {
        Object recreate(String value, boolean isUrl, boolean isCached);
    }
}
