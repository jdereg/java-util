package com.cedarsoftware.ncube.proximity;

import static java.lang.Math.atan2;
import static java.lang.Math.cos;
import static java.lang.Math.sin;
import static java.lang.Math.sqrt;
import static java.lang.Math.toRadians;

/**
 * This class is used to represent a latitude / longitude coordinate.
 * This class implements the Proximity interface so that it can work with NCube.
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
public class LatLon implements Comparable<LatLon>, Distance<LatLon>
{
	public static final double EARTH_RADIUS = 6371.00; // Radius in Kilometers default
    private double lat;
	private double lon;
	
	/**
	 * @param lat decimal degrees latitude
	 * @param lon decimal degrees longitude
	 */
	public LatLon(double lat, double lon)
	{
		this.lat = lat;
		this.lon = lon;
	}
	
	public boolean equals(Object obj)
	{
		if (!(obj instanceof LatLon))
		{
			return false;
		}
		
		LatLon that = (LatLon) obj;
		return lat == that.lat && lon == that.lon;
	}
	
	public int compareTo(LatLon that)
	{
		if (lat < that.lat)
		{
			return -1;
		}
		if (lat > that.lat)
		{
			return 1;
		}
		if (lon < that.lon)
		{
			return -1;
		}
		if (lon > that.lon)
		{
			return 1;
		}
		return 0;
	}

	/**
	 * @return the distance between another latlon coordinate
	 * and this coordinate, in kilometers.
     * Implemented using the Haversine formula.
	 */
	public double distance(LatLon that) 
	{
        double earthRadius = EARTH_RADIUS;
        double dLat = toRadians(that.lat - lat);
        double dLng = toRadians(that.lon - lon);
        double sinDlat2 = sin(dLat / 2.0);
        double sinDlng2 = sin(dLng/2);
        double a = sinDlat2 * sinDlat2 + cos(toRadians(lat)) * cos(toRadians(that.lat)) * sinDlng2 * sinDlng2;
        double c = 2 * atan2(sqrt(a), sqrt(1-a));
        double dist = earthRadius * c;
        return dist;
	}
	
	public String toString()
	{
		return "(" + lat + ", " + lon + ")";
	}

    public double getLat() { return lat; }
    public double getLon() { return lon; }
}
