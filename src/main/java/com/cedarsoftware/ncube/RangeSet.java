package com.cedarsoftware.ncube;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * This class is used to represent a Set of values, where the values
 * can be a Comparable or a Range.  This allows "grouping", or "OR", or
 * SQL "IN" type of logic when matching a given value against an axis,
 * greatly reducing having duplicate cells in an ncube.
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
public class RangeSet implements Comparable<RangeSet>
{    
	private final List<Comparable> items = new ArrayList<Comparable>();
	public RangeSet() { }
    public RangeSet(Comparable c)
    {
        items.add(c);
    }

	public void add(Comparable c)
	{
		items.add(c);
	}
	
	public Comparable get(int index)
	{
		return items.get(index);
	}
	
	public int size()
	{
		return items.size();
	}

    public void clear()
    {
        items.clear();
    }

	public Iterator<Comparable> iterator()
	{
		return items.iterator();
	}
	
 	public int compareTo(RangeSet that) 
	{
        int size = size();
        if (items.isEmpty() || that.items.isEmpty())
        {
            return size - that.size();
        }
 		Comparable first = get(0);
 		Comparable thatFirst = that.get(0);
 		Comparable lowest = (first instanceof Range) ? ((Range)first).low : first;
 		Comparable thatLowest = (thatFirst instanceof Range) ?  ((Range)thatFirst).low : thatFirst;
		return lowest.compareTo(thatLowest);
	}

    public int hashCode()
    {
        return items.hashCode();
    }

    public boolean equals(Object other)
    {
        if (!(other instanceof RangeSet))
        {
            return false;
        }

        RangeSet that = (RangeSet) other;
        return items.equals(that.items);
    }

	/**
	 * @param value to compare with this RangeSet to determine if the value is
	 * within. For example, {v1, v2, v6-v25, v30} - in this case, the passed in 
	 * value must match v1, v2, or v30, or fall within [v6, v25).
	 * @return boolean true if the passed in value can be found within this
	 * RangeSet instance.  That means an individual value in this set must
	 * match the passed in value, or a Range inside this RangeSet must include
	 * the passed in value.
	 */
	public boolean contains(Comparable value)
	{
		Iterator<Comparable> i = iterator();
		while (i.hasNext())
		{
			Comparable item = i.next();
			if (item instanceof Range)
			{
				Range range = (Range) item;
                if (value instanceof Range)
                {   // RANGE against RANGE
                    if (range.overlap((Range)value))
                    {
                        return true;
                    }
                }
                else
                {   // This RANGE against that DISCRETE
                    if (range.isWithin(value) == 0)
                    {
                        return true;
                    }
                }
			}
			else
			{
                if (value instanceof Range)
                {   // That RANGE against this DISCRETE
                    Range range = (Range) value;
                    if (range.isWithin(item) == 0)
                    {
                        return true;
                    }
                }
                else
                {   // DISCRETE against DISCRETE
                    if (item.equals(value))
                    {
                        return true;
                    }
                }
			}
		}
		return false;
	}
	
	/**
	 * @return boolean true if the points and line segments represented by these 
	 * two RangeSets overlap.  Assumption that the Range objects 'low' value is less
	 * than the 'high' value.
	 */
	public boolean overlap(RangeSet that)
	{
		Iterator<Comparable> i = that.iterator();
		while (i.hasNext())
		{
			Comparable c = i.next();
			if (contains(c))
			{
				return true;
			}
		}
		return false;
	}

    public String toString()
    {
        StringBuilder s = new StringBuilder();
        Iterator<Comparable> i = iterator();
        while (i.hasNext())
        {
            Comparable item = i.next();
            if (item instanceof Range)
            {
                s.append(item.toString());
            }
            else
            {
                s.append(Column.formatDiscreteValue(item));
            }
            if (i.hasNext())
            {
                s.append(", ");
            }
        }
        return s.toString();
    }
}
