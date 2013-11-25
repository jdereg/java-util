package com.cedarsoftware.ncube;

/**
 * This class defines allowable n-cube axis value types.
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
public enum AxisValueType 
{
	STRING, 		// For Java Strings.  Strings will be compared with .equals()
	LONG, 			// For any integral java type (byte, short, int, long).  All of those will be promoted to long internally.
	BIG_DECIMAL, 	// For float, double, or BigDecimal.  All of those will be promoted to BigDecimal internally.
	DOUBLE, 		// For float or double.  Float will be promoted to double internally.
	DATE, 			// For Date.  Calendar and Long can be passed in for comparison against.
    EXPRESSION,     // For when the axis type is RULE
	COMPARABLE		// For all other objects.  For example, Character, LatLon, or a Class that implements Comparable.
}
