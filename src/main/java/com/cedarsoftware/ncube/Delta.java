package com.cedarsoftware.ncube;

/**
 * This class records information about the delta (difference) between
 * two n-cubes.  It allows a level of determinism regarding the difference
 * that could not be obtain with purely textual differences.
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
public class Delta
{
    private String desc;
    private Location loc;
    private Type type;

    public enum Location
    {
        NCUBE,
        NCUBE_META,
        AXIS,
        AXIS_META,
        COLUMN,
        COLUMN_META,
        CELL,
        CELL_META
    }

    public enum Type
    {
        ADD,
        DELETE,
        UPDATE
    }

    public Delta(Location location, Type type, String description)
    {
        desc = description;
        loc = location;
        this.type = type;
    }

    public String getDescription()
    {
        return desc;
    }

    public Location getLocation()
    {
        return loc;
    }

    public Type getType()
    {
        return type;
    }

    public String toString()
    {
        return desc;
    }
}
