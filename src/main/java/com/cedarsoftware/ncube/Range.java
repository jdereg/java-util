package com.cedarsoftware.ncube;

/**
 * This class is used to represent a 'band' or 'range' of values (numeric, date, etc.)
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
public class Range implements Comparable<Range>
{
    Comparable low;
    Comparable high;
    public Range() {}	// For serialization support
    
    public Range(Comparable low, Comparable high)
    {
    	if (low == null || high == null)
    	{
    		throw new IllegalArgumentException("Range value cannot be null");
    	}
    	
    	if (low.equals(high))
    	{   // Using compareTo() because we know that it HAD to be implemented (whereas .equals() comes free)
    		throw new IllegalArgumentException("Range low and high must be different");
    	}
        this.low = low;
        this.high = high;        
    }
    
    public String toString()
    {
        return "[" + Column.formatDiscreteValue(low) + " - " + Column.formatDiscreteValue(high) + ")";
    }
    
	public int compareTo(Range that) 
	{
		return low.compareTo(that.low);
	}

    public boolean equals(Object other)
    {
        if (!(other instanceof Range))
        {
            return false;
        }
        Range that = (Range) other;

        return low.compareTo(that.low) == 0 && high.compareTo(that.high) == 0;
    }

    public int hashCode()
    {
        return low.hashCode() + high.hashCode();
    }
	
	/**
	 * @param value to compare with Range to determine if the value is
	 * within the Range [low, high).
	 * @return -1 if the value is less than the range low point, 0 if
	 * the value is within the range, and 1 if the value is greater than or equal
	 * to the range high point.
	 */
	public int isWithin(Comparable value)
	{
		if (value == null)
		{
			return 1;
		}
		
		if (value.compareTo(low) < 0)
		{
			return -1;
		}
		else if (value.compareTo(high) >= 0)
		{
			return 1;
		}
		return 0;			
	}
	
	/**
	 * @return boolean true if the line segments represented by these two ranges
	 * overlap.  Assumption that the Range objects 'low' value is less than the
	 * 'high' value.
	 */
	public boolean overlap(Range that)
	{
		return !(high.compareTo(that.low) <= 0 || low.compareTo(that.high) >= 0);
	}

    public Comparable getLow() {
        return low;
    }
    public Comparable getHigh() {
        return high;
    }
}
