package com.cedarsoftware.ncube.proximity;

import com.cedarsoftware.ncube.CellInfo;

/**
 * This class is used to represent a 3D point.  This
 * class implements the Proximity interface so that it
 * can work with NCube.
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
public class Point3D implements Comparable<Point3D>, Distance<Point3D>
{
	private final double x;
	private final double y;
	private final double z;

	public Point3D(double x, double y, double z)
	{
		this.x = x;
		this.y = y;
		this.z = z;
	}

	public boolean equals(Object obj)
	{
		if (!(obj instanceof Point3D))
		{
			return false;
		}

		Point3D that = (Point3D) obj;
		return x == that.x && y == that.y && z == that.z;
	}

	public int compareTo(Point3D that)
	{
		if (x < that.x)
		{
			return -1;
		}
		if (x > that.x)
		{
			return 1;
		}
		if (y < that.y)
		{
			return -1;
		}
		if (y > that.y)
		{
			return 1;
		}
		if (z < that.z)
		{
			return -1;
		}
		if (z > that.z)
		{
			return 1;
		}
		return 0;
	}

	public double distance(Point3D that)
	{
		double dx = that.x - x;
		double dy = that.y - y;
		double dz = that.z - z;

		return Math.sqrt(dx * dx + dy * dy + dz * dz);
	}

	public String toString()
	{
        return String.format("%s, %s, %s",
                CellInfo.formatForEditing(x),
                CellInfo.formatForEditing(y),
                CellInfo.formatForEditing(z));
	}

    public double getX()
    {
        return x;
    }

    public double getY()
    {
        return y;
    }

    public double getZ()
    {
        return z;
    }

}
