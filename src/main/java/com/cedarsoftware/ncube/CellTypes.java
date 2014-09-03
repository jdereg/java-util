package com.cedarsoftware.ncube;

import com.cedarsoftware.ncube.proximity.LatLon;
import com.cedarsoftware.ncube.proximity.Point2D;
import com.cedarsoftware.ncube.proximity.Point3D;

import java.text.MessageFormat;
import java.util.Collection;

/**
 * Allowed cell types for n-cube.
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
public enum CellTypes
{
    String("string"),
    Date("date"),
    Boolean("boolean"),
    Byte("byte"),
    Short("short"),
    Integer("int"),
    Long("long"),
    Float("float"),
    Double("double"),
    BigDecimal("bigdec"),
    BigInteger("bigint"),
    Binary("binary"),
    Exp("exp"),
    Method("method"),
    Template("template"),
    LatLon("latlon"),
    Point2D("point2d"),
    Point3D("point3d"),
    Null("null");

    private final String desc;

    private CellTypes(String desc)
    {
        this.desc=desc;
    }

    public String desc()
    {
        return desc;
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

        throw new IllegalArgumentException(MessageFormat.format("Unsupported type {0} found in {1}", cell.getClass().getName(), section));
    }


}
