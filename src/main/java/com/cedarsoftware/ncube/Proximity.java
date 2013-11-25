package com.cedarsoftware.ncube;

import com.cedarsoftware.ncube.proximity.Distance;

import java.math.BigDecimal;
import java.util.Date;

import static java.lang.Math.abs;
import static java.lang.Math.min;

/**
 * Class used to compute distance (proximity) for many different
 * data types.  For basic data types like ints, longs, Dates, etc.,
 * simple subtraction can be used.  For Strings, the LevenshteinDistance
 * is used.  For Lat/Lon, the Haversine calculation is used, and
 * for 2D, 3D, etc., the pythagorean distance is used.
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
public class Proximity
{
	public static double distance(Comparable source, Comparable target)
	{
        if (source == null || target == null)
        {
            throw new IllegalArgumentException("Neither source nor target can be null for NEAREST axis comparison." +
            "\nSource: " + source + ", Target: " + target);
        }

        if (source.getClass() != target.getClass())
        {
            throw new IllegalArgumentException("Source and Target data types must be the same for a NEAREST axis comparison." +
            "\nSource: " + source + ", Target: " + target);
        }

		if (source instanceof Long)
		{   // This covers byte, short, int, and long (data type promotion has already occurred).
			return abs((Long)source - (Long) target);
		}
        else if (source instanceof Date)
        {
            long v = ((Date)source).getTime();
            long thatValue = ((Date)target).getTime();
            return abs(v - thatValue);
        }
        else if (source instanceof Double)
        {   // Covers float and double
            double v = (Double) source;
            double thatValue = (Double) target;
            return abs(v - thatValue);
        }
        else if (source instanceof BigDecimal)
        {
            BigDecimal v = (BigDecimal) source;
            BigDecimal thatValue = (BigDecimal) target;
            return v.subtract(thatValue).abs().doubleValue();
        }
        else if (source instanceof String)
        {
            return levenshteinDistance((String)source, (String) target);
        }
        else if (source instanceof Distance)
        {
            return ((Distance) source).distance(target);
        }
        throw new IllegalArgumentException("Unsupported datatype for NEAREST axis." +
        "\nSource: " + source.getClass().getName() + ", Source Value: " + source +
        "\nTarget Value: " + target);
	}
	
    public static int levenshteinDistance(CharSequence str1, CharSequence str2)
    {
        if (str1 == null || "".equals(str1))
        {
            return str2 == null || "".equals(str2) ? 0 : str2.length();
        }
        else if (str2 == null || "".equals(str2))
        {
            return str1.length();
        }

        final int len1 = str1.length();
        final int len2 = str2.length();
        final int[][] distance = new int[len1 + 1][len2 + 1];

        for (int i = 0; i <= len1; i++)
        {
            distance[i][0] = i;
        }

        for (int j = 1; j <= len2; j++)
        {
            distance[0][j] = j;
        }

        for (int i = 1; i <= len1; i++)
        {
            for (int j = 1; j <= len2; j++)
            {
                distance[i][j] = minimum(
                        distance[i - 1][j] + 1,
                        distance[i][j - 1] + 1,
                        distance[i - 1][j - 1] + ((str1.charAt(i - 1) == str2.charAt(j - 1)) ? 0 : 1));
            }
        }
        return distance[len1][len2];
    }
    
    private static int minimum(int a, int b, int c)
    {
        return min(min(a, b), c);
    }
}
