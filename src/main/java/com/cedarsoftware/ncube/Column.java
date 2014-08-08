package com.cedarsoftware.ncube;

import com.cedarsoftware.ncube.proximity.Distance;
import com.cedarsoftware.util.CaseInsensitiveMap;
import com.cedarsoftware.util.SafeSimpleDateFormat;
import com.cedarsoftware.util.UniqueIdGenerator;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.Collections;
import java.util.Date;
import java.util.Map;

/**
 * Holds the value of a 'column' on an axis.
 * This class exists in order to allow additional
 * columns to be inserted onto an axis, without
 * having to "move" the existing cells.  Cells
 * reference columns by their ID, not ordinal position.
 * 
 * Furthermore, for some axis types (String), it is
 * often better for display purposes to use the
 * display order, as opposed to it's sort order 
 * (e.g., Months-of-year) for display.
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
public class Column implements Comparable<Comparable>
{
	long id;
	private int displayOrder;
	private Comparable value;
    Map<String, Object> metaProps = null;
    static final SafeSimpleDateFormat dateFormat = new SafeSimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    public static final String NAME = "name";

    /**
     * 2nd argument is there to prevent this constructor from matching constructor that takes only Comparable.
     */
    Column(long id, boolean notUsed)
    {
        value = 0;
        this.id = id;
    }

	public Column(Comparable value)
	{
		this.value = value;
		id = UniqueIdGenerator.getUniqueId();
	}

    /**
     * @return Map (case insensitive keys) containing meta (additional) properties for the n-cube.
     */
    public Map<String, Object> getMetaProperties()
    {
        Map ret = metaProps == null ? new CaseInsensitiveMap() : metaProps;
        return Collections.unmodifiableMap(ret);
    }

    /**
     * Set (add / overwrite) a Meta Property associated to this Column.
     * @param key String key name of meta property
     * @param metaPropValue Object value to associate to key
     * @return prior value associated to key or null if none was associated prior
     */
    public Object setMetaProperty(String key, Object metaPropValue)
    {
        if (metaProps == null)
        {
            metaProps = new CaseInsensitiveMap<>();
        }
        return metaProps.put(key, metaPropValue);
    }

    /**
     * Add a Map of meta properties all at once.
     * @param allAtOnce Map of meta properties to add
     */
    public void addMetaProperties(Map<String, Object> allAtOnce)
    {
        if (metaProps == null)
        {
            metaProps = new CaseInsensitiveMap<>();
        }
        metaProps.putAll(allAtOnce);
    }

    /**
     * Remove all meta properties associated to this Column.
     */
    public void clearMetaProperties()
    {
        if (metaProps != null)
        {
            metaProps.clear();
            metaProps = null;
        }
    }

    public long getId()
    {
        return id;
    }

    void setId(long id)
    {
        this.id = id;
    }

    public int hashCode()
    {
    	final long x = id;
        // do not change the formula below.  It is been hand crafted and tested for performance.
        // If this does not hash well, ncube breaks down in performance.  The BigCube tests are
        // greatly slowed down as proper hashing is vital or cells will be really slow to access
        // when there are a lot of them in the ncube.
        return (int)(x * 347 ^ (x >>> 32) * 7);
    }

    public boolean equals(Object that)
    {
        return that instanceof Column && id == ((Column) that).id;
    }

	public Comparable getValue()
	{
		return value;
	}

    void setValue(Comparable v)
    {
        value = v;
    }

    public boolean isDefault()
    {
        return value == null;
    }

    /**
     * @return a value that will match this column.  This returns column value
     * if it is a DISCRETE axis column.  If it is a Range axis column, the 'low'
     * value will be returned (low is inclusive, high is exclusive).  If it is a
     * RangeSet axis column, then the first value will be returned.  If it is a Range,
     * then the low value of that Range will be returned.  In all cases, the returned
     * value can be used to match against an axis including this column and the returned
     * value will match this column.
     */
    public Comparable getValueThatMatches()
    {
        if (value instanceof Range)
        {
            return ((Range)value).low;
        }
        else if (value instanceof RangeSet)
        {
            RangeSet set = (RangeSet) value;
            Comparable v = set.get(0);
            return v instanceof Range ? ((Range) v).low : v;
        }

        return value;
    }
	
	void setDisplayOrder(int order)
	{
		displayOrder = order;
	}
	
	public int getDisplayOrder()
	{
		return displayOrder;
	}

	public int compareTo(Comparable that)
	{
        if (that instanceof Column)
        {
            that = ((Column)that).getValue();
        }

        if (value == null)
        {
            return that == null ? 0 : 1;
        }

        if (that == null)
        {
            return -1;
        }

		return value.compareTo(that);
	}

	public String toString()
	{
        if (value instanceof Range || value instanceof RangeSet || value instanceof Distance)
        {
            return value.toString();
        }
        else
        {
		    return formatDiscreteValue(value);
        }
	}

    public static String formatDiscreteValue(Comparable val)
    {
        if (val instanceof Date)
        {
            return dateFormat.format(val);
        }
        else if (val instanceof Double)
        {
            return new DecimalFormat("#,##0.0##############").format(val);
        }
        else if (val instanceof BigDecimal)
        {
            BigDecimal x = (BigDecimal) val;
            return x.stripTrailingZeros().toPlainString();
        }
        else if (val instanceof Number)
        {
            return new DecimalFormat("#,##0").format(val);
        }
        else if (val == null)
        {
            return "Default";
        }
        else
        {
            return val.toString();
        }
    }
}
