package com.cedarsoftware.ncube;

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
    Point2d("point2d"),
    Point3d("point3d"),
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
}
