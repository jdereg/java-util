package com.cedarsoftware.ncube;

import com.cedarsoftware.util.UniqueIdGenerator;
import com.cedarsoftware.ncube.exception.AxisOverlapException;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Implements an Axis of an NCube. When modeling, think of an axis as a 'condition'
 * or decision point.  An input variable (like 'X:1' in a cartesian coordinate system)
 * is passed in, and the Axis's job is to locate the column that best matches the input,
 * as quickly as possible.
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
public class Axis
{
	public static final int SORTED = 0;
	public static final int DISPLAY = 1;
	final long id;
	private String name;
	private final AxisType type;
	private final AxisValueType valueType;
	private final List<Column> columns = new ArrayList<Column>();
    private Column defaultCol;
	private int preferredOrder = SORTED;
    private boolean multiMatch = false;

    // used to get O(1) on SET axis for the discrete elements in the Set
    private transient Map<Comparable, Column> discreteToCol = new TreeMap<Comparable, Column>();

    // used to get O(1) on Ranges for SET access
    private transient List<RangeToColumn> rangeToCol = new ArrayList<RangeToColumn>();

    // used to get O(1) access to columns by ID
    transient Map<Long, Column> idToCol = new HashMap<Long, Column>();


    public Axis(String name, AxisType type, AxisValueType valueType, boolean hasDefault)
	{
		this(name, type, valueType, hasDefault, SORTED);
	}

    public Axis(String name, AxisType type, AxisValueType valueType, boolean hasDefault, int order)
    {
        this(name, type, valueType, hasDefault, order, false);
    }

	public Axis(String name, AxisType type, AxisValueType valueType, boolean hasDefault, int order, boolean multiMatch)
	{
		this.name = name;
		this.type = type;
        this.preferredOrder = order;
		this.valueType = valueType;
        this.multiMatch = multiMatch;
        if (type == AxisType.RULE)
        {
            this.multiMatch = true;
            if (order == SORTED)
            {
                throw new IllegalArgumentException("RULE axis '" + name + "' cannot be set to SORTED");
            }
            if (valueType != AxisValueType.EXPRESSION)
            {
                throw new IllegalArgumentException("RULE axis '" + name + "' must have valueType set to EXPRESSION");
            }
        }
        if (hasDefault)
        {
            if (type == AxisType.NEAREST)
            {
                throw new IllegalArgumentException("NEAREST type axis '" + name + "' cannot have a default column");
            }
            defaultCol = new Column(null);
            defaultCol.setDisplayOrder(Integer.MAX_VALUE);  // Always at the end
            columns.add(defaultCol);
            idToCol.put(defaultCol.id, defaultCol);
        }
        id = UniqueIdGenerator.getUniqueId();
	}

    /**
     * Scaffolding is extra, indexing structures (transient members) that are not part of
     * the persistent state, but are created and maintained internally so that searches for
     * SETs (RANGE_SET)s are O(Log(n)) or better O(1) for the discrete value in a RANGE_SET.
     * Variables 'discreteToCol' and 'rangeToCol' fall into this category.
     *
     * All column ids are mapped to the column instances to support the setCellById(),
     * getCellByIdNoExecute(), removeCellById(), and containsCellById().  Variable
     * 'idToCol' is the scaffolding member that maintains this relationship.  This is built-in
     * scaffolding that ALWAYS must be present.  The scaffolding to support RANGE_SETs is only
     * used on a RANGE_SET axis, when it is not in 'multiMatch' mode.
     */
    boolean hasScaffolding()
    {
        return type == AxisType.SET && !multiMatch;
    }

    public boolean isMultiMatch()
    {
        return multiMatch;
    }

    void buildScaffolding()
    {
        for (Column column : columns)
        {
            addScaffolding(column);
        }
    }

    private void addScaffolding(Column column)
    {
        idToCol.put(column.id, column);

        if (!hasScaffolding())
        {
            return;
        }

        RangeSet set = (RangeSet)column.getValue();
        if (set == null)
        {   // Default column being processed
            return;
        }

        final int len = set.size();
        for (int i=0; i < len; i++)
        {
            Comparable elem = set.get(i);
            if (elem instanceof Range)
            {
                Range range = (Range) elem;
                RangeToColumn rc = new RangeToColumn(range, column);
                int where = Collections.binarySearch(rangeToCol, rc);
                if (where < 0)
                {
                    where = Math.abs(where + 1);
                }
                rangeToCol.add(where, rc);
            }
            else
            {
                discreteToCol.put(elem, column);
            }
        }
    }

    public long getId()
    {
        return id;
    }

    public String toString()
    {
        StringBuilder s = new StringBuilder();
        s.append("Axis: ");
        s.append(name);
        s.append(" [");
        s.append(type);
        s.append(", ");
        s.append(valueType);
        s.append("]\n");
        s.append("  hasDefault column: ");
        s.append(hasDefaultColumn());
        s.append("\n");
        s.append("  preferred Order: ");
        s.append(getColumnOrder());
        s.append("\n");
        
        for (Comparable value : columns)
        {
            s.append("  ");
            s.append(value);
            s.append('\n');
        }

        return s.toString();
    }

	public String getName() 
	{
		return name;
	}

	void setName(String name)
	{
		this.name = name;
	}

	public AxisType getType() 
	{
		return type;
	}

	public AxisValueType getValueType() 
	{
		return valueType;
	}
	
	public List<Column> getColumns()
	{
        List<Column> cols = new ArrayList<Column>(columns);
		if (preferredOrder == SORTED)
		{
			return cols;	// Return a copy of the columns, not our internal values list.
		}
		sortColumnsByDisplayOrder(cols);
		return cols;
	}

	public Column addColumn(Comparable value)
	{
        Comparable v;
        if (value == null)
        {  // Attempting to add Default column to axis
            if (defaultCol != null)
            {
                throw new IllegalArgumentException("Cannot add default column to axis '" + name + "' because it already has a default column.");
            }
            if (type == AxisType.NEAREST)
            {
                throw new IllegalArgumentException("Cannot add default column to NEAREST axis '" + name + "' as it would never be chosen.");
            }
            v = null;
        }
        else
        {
            v = standardizeColumnValue(value);
            if (type == AxisType.DISCRETE)
            {
                doesMatchExistingValue(v);
            }
            else if (type == AxisType.RANGE)
            {
                Range range = (Range)v;
                if (!multiMatch && doesOverlap(range))
                {
                    throw new AxisOverlapException("Passed in Range overlaps existing Range on axis '" + name + "'");
                }
            }
            else if (type == AxisType.SET)
            {
                RangeSet set = (RangeSet)v;
                if (!multiMatch && doesOverlap(set))
                {
                    throw new AxisOverlapException("Passed in RangeSet overlaps existing RangeSet on axis '" + name + "'");
                }
            }
            else if (type == AxisType.RULE)
            {
                if (!(value instanceof CommandCell))
                {
                    throw new IllegalArgumentException("Columns for RULE axis must be a CommandCell, axis '" + name + "'");
                }
            }
            else if (type == AxisType.NEAREST)
            {
                doesMatchNearestValue(v);
            }
            else
            {
                throw new IllegalStateException("New axis type added without complete support.");
            }
        }
		Column column = new Column(v);
        if (v == null)
        {
            defaultCol = column;
        }

        // New columns are always added at the end in terms of displayOrder, but internally they are added
        // in the correct sort order location.  The sort order of the list is required because binary searches
        // are done against it.
        column.setDisplayOrder(v == null ? Integer.MAX_VALUE : size());
        int where = Collections.binarySearch(columns, v);
        if (where < 0)
        {
            where = Math.abs(where + 1);
        }
        columns.add(where, column);
        addScaffolding(column);
        return column;
    }

    /**
	 * This method deletes a column from an Axis.  It is intentionally package
	 * scoped because there are two parts to deleting a column - this removes
	 * the column from the Axis, the other part removes the Cells that reference
	 * the column (that is within NCube).
	 * @param value Comparable value used to identify the column to delete.
	 * @return Column that was deleted, or null if no column would be deleted.
	 */
	Column deleteColumn(Comparable value)
	{
		Column col = findColumn(value);
		if (col == null)
		{	// Not found.
			return null;
		}

        columns.remove(col);
        if (col.isDefault())
        {
            defaultCol = null;
        }
		
		List<Column> cols = new ArrayList<Column>(columns);
		sortColumnsByDisplayOrder(cols);
        assignDisplayOrder(cols);

        // Remove column from scaffolding
        removeColumnFromScaffolding(value, col);
        return col;
	}

    private void removeColumnFromScaffolding(Comparable value, Column col)
    {
        if (!hasScaffolding())
        {
            return;
        }

        if (discreteToCol != null)
        {
            Iterator<Map.Entry<Comparable, Column>> j = discreteToCol.entrySet().iterator();
            while (j.hasNext())
            {
                Map.Entry<Comparable, Column> entry = j.next();
                Column column = entry.getValue();
                if (col.equals(column))
                {   // Multiple discrete values may have pointed to the passed in column, so we must loop through all
                    j.remove();
                }
            }
        }

        if (rangeToCol != null)
        {
            Iterator<RangeToColumn> i = rangeToCol.iterator();
            while (i.hasNext())
            {
                Axis.RangeToColumn rangeToColumn = i.next();
                if (rangeToColumn.getRange().isWithin(value) == 0)
                {   // Multiple ranges may have pointed to the passed in column, so we must loop through all
                    i.remove();
                }
            }
        }

        // Remove from col id to column map
        idToCol.remove(col.id);
    }

    public boolean moveColumn(int curPos, int newPos)
	{
		if (preferredOrder != DISPLAY)
		{
			throw new IllegalStateException("Axis '" + name + "' must be in DISPLAY order to permit column reordering");
		}
		
		if (curPos == newPos)
		{	// That was easy
			return true;
		}
		
		if (curPos < 0 || curPos >= columns.size() || newPos < 0 || newPos >= columns.size())
		{
			throw new IllegalArgumentException("Position must be >= 0 and < number of Columns to reorder column, axis '" + name +"'");
		}

        if (columns.get(curPos).isDefault() || columns.get(newPos).isDefault())
        {
            throw new IllegalArgumentException("Cannot move 'Default' column, axis '" + name +"'");
        }

        List<Column> cols = new ArrayList<Column>(columns);
        sortColumnsByDisplayOrder(cols);
        cols.add(newPos, cols.remove(curPos));
        assignDisplayOrder(cols);
        return true;
	}
	
	private static void assignDisplayOrder(final List<Column> cols) 
	{
		final int size = cols.size(); 
		for (int k=0; k < size; k++)
		{
            Column col = cols.get(k);
            col.setDisplayOrder(col.isDefault() ? Integer.MAX_VALUE : k);
		}
	}	
	
	public int getColumnOrder()
	{
		return preferredOrder;
	}
	
	public void setColumnOrder(int order)
	{
		preferredOrder = order;
	}	
	
	private static void sortColumnsByDisplayOrder(List<Column> cols)
	{
		Collections.sort(cols, new Comparator<Column>()
		{
			public int compare(Column c1, Column c2) 
			{
				return c1.getDisplayOrder() - c2.getDisplayOrder();
			}
		});
	}
	
	public int size()
	{
		return columns.size();
	}
	
	/**
	 * This method takes the input value (could be Number, String, Range, etc.) 
	 * and 'promotes' it to the same type as the Axis.
	 * @param value Comparable value to promote (to highest of it's type [e.g., short to long])
	 * @return Comparable promoted value.  For example, a Long would be returned a
	 * Byte value were passed in, and this was a LONG axis.
	 */
	public Comparable standardizeColumnValue(Comparable value)
	{
		if (value == null)
		{	
			throw new IllegalArgumentException("'null' cannot be used as an axis value, axis: " + name);
		}
		
		if (type == AxisType.DISCRETE)
		{
			return promoteValue(value);
		}
		else if (type == AxisType.RANGE)
		{
			if (!(value instanceof Range))
			{
				throw new IllegalArgumentException("Must only add Range values to " + type + " axis '" + name + "' - attempted to add: " + value.getClass().getName());
			}
			return promoteRange(new Range(((Range)value).low, ((Range)value).high));
		}
		else if (type == AxisType.SET)
		{
			if (!(value instanceof RangeSet))
			{
				throw new IllegalArgumentException("Must only add RangeSet values to " + type + " axis '" + name + "' - attempted to add: " + value.getClass().getName());
			}
            RangeSet set = new RangeSet();
            Iterator<Comparable> i = ((RangeSet)value).iterator();
            while (i.hasNext())
            {
                Comparable val = i.next();
                if (val instanceof Range)
                {
                    promoteRange((Range)val);
                }
                else
                {
                    val = promoteValue(val);
                }
                set.add(val);
            }
            return set;
		}
		else if (type == AxisType.NEAREST)
		{	// Standardizing a NEAREST axis entails ensuring conformity amongst values (must all be Point2D, LatLon, Date, Long, String, etc.)
			value = promoteValue(value);
			if (!getColumnsWithoutDefault().isEmpty())
			{
				Column col = columns.iterator().next();
                if (value.getClass() != col.getValue().getClass())
				{
					throw new IllegalArgumentException("Value '" + value.getClass().getName() + "' cannot be added to axis '" + name + "' where the values are of type: " + col.getValue().getClass().getName());
				}
			}
			return value;	// First value added does not need to be checked
		}
        else if (type == AxisType.RULE)
        {
            return value;
        }
		else
		{
			throw new IllegalArgumentException("New AxisType added '" + type + "' but code support for it is not there.");
		}
	}
	
	/**
	 * Promote passed in range's low and high values to the largest
	 * data type of their 'kinds' (e.g., byte to long).
	 * @param range Range to be promoted
	 * @return Range with the low and high values promoted and in proper order (low < high)
	 */
	private Range promoteRange(Range range) 
	{
		final Comparable low = promoteValue(range.low);
		final Comparable high = promoteValue(range.high);
		ensureOrder(range, low, high);
		return range;
	}
	
	private static void ensureOrder(Range range, final Comparable low, final Comparable high)
	{
		if (low.compareTo(high) > 0)
		{
			range.low = high;
			range.high = low;
		}
		else
		{
			range.low = low;
			range.high = high;
		}
	}	
	
	/**
	 * Convert passed in value to a similar value of the highest type.  Axis
	 * values and inputs are always promoted before being stored or compared.
	 * @param value Comparable to promote
	 * @return promoted value, or the same value if no promotion occurs.
	 */
	private Comparable promoteValue(Comparable value) 
	{
		switch(valueType)
		{
			case STRING:
				return getString(value);
			case LONG:
				return getLong(value);
			case BIG_DECIMAL:
				return getBigDecimal(value);
			case DOUBLE:
				return getDouble(value);
			case DATE:
				return getDate(value);
			case COMPARABLE:
            case EXPRESSION:
				return value;
			default:
				throw new IllegalArgumentException("AxisValueType '" + valueType + "' added but not code to support it.");
		}
	}

	private String getString(Comparable value)
	{
		if (value instanceof String)
		{
			return (String) value;
		}
		throw getBadTypeException(value, "String");
	}
	
	/**
	 * Promote any Number (or String) type to a BigDecimal in the best possible manner.
	 * @return BigDecimal equivalent of value, or null if it could not be converted.
	 */
	private BigDecimal getBigDecimal(Comparable value)
	{
		try 
		{
			if (value instanceof BigDecimal)
			{
				return (BigDecimal) value;
			}
			else if (value instanceof BigInteger)
			{
				return new BigDecimal((BigInteger)value);
			}
			else if (value instanceof String)
			{
				return new BigDecimal((String) value);
			}
			else if (value instanceof Number)
			{
				return new BigDecimal(((Number)value).doubleValue());
			}
		} 
		catch (Exception e) 
		{
			throw getConversionException(value, e);
		}
		throw getBadTypeException(value, "BigDecimal");
	}
	
	/**
	 * Promote Number (or String) to a Double in the best possible manner.
	 * @return Double equivalent of value, or null if it could not be converted.
	 */
	private Double getDouble(Comparable value)
	{
		try
		{
			if (value instanceof Double)
			{
				return (Double) value;
			}
			else if (value instanceof Number)
			{
				return ((Number)value).doubleValue();
			}
			else if (value instanceof String)
			{
				return Double.valueOf((String) value);
			}
		}
		catch(Exception e)
		{
			throw getConversionException(value, e);
		}
		throw getBadTypeException(value, "Double");
	}
	
	/**
	 * Promote Number (or String) to a Long in the best possible manner.
	 * @return Long equivalent of value, or null if it could not be converted.
	 */
	private Long getLong(Comparable value)
	{
		try
		{
			if (value instanceof Long)
			{
				return (Long) value;
			}
			else if (value instanceof Number)
			{
				return ((Number)value).longValue();
			}
			else if (value instanceof String)
			{
				return Long.valueOf((String) value);
			}
		}
		catch(Exception e)
		{
			throw getConversionException(value, e);
		}
		throw getBadTypeException(value, "Long");
	}
			
	/**
	 * Promote Number (or String) to a Long in the best possible manner.
	 * @return Long equivalent of value, or null if it could not be converted.
	 */
	private Date getDate(Comparable value)
	{
		if (value instanceof Date)
		{
			return (Date) value;
		}
		else if (value instanceof Calendar)
		{
			return ((Calendar)value).getTime();
		}
		else if (value instanceof Long)
		{
			return new Date((Long)value);
		}
		throw getBadTypeException(value, "Date");
	}
	
	private IllegalArgumentException getConversionException(Comparable value, Exception e)
	{
		return new IllegalArgumentException("value [" + value.getClass().getName() + "] could not be converted to a Long for axis '" + name + "'", e);						
	}
	
	private IllegalArgumentException getBadTypeException(Comparable value, String theType)
	{
		return new IllegalArgumentException("Unsupported value type [" + value.getClass().getName() + "] attempting to convert to '" + theType + "' for axis '" + name + "'");
	}
	
	public boolean hasDefaultColumn()
	{
		return defaultCol != null;
	}
	
	/**
	 * @param value to test against this Axis
	 * @return boolean true if the value will be found along the access, false
	 * if the value does not match anything along the axis.
	 */
	public boolean contains(Comparable value)
	{
		try 
		{
			return findColumn(value) != null;
		}
		catch (Exception e) 
		{
			return false;
		}
	}

    public Column getDefaultColumn()
    {
        return defaultCol;
    }

    /**
     * Locate the column (AvisValue) along an axis.
     * @param value Comparable - A value that can be checked against the axis
     * @return Column that 'matches' the passed in value, or null if no column
     * found.  'Matches' because matches depends on AxisType.
     */
     Column findColumn(Comparable value)
     {
        if (value == null)
        {
            if (defaultCol != null)
            {
                return defaultCol;
            }
            throw new IllegalArgumentException("'null' passed to axis '" + name + "' which does not have a default column");
        }

        final Comparable promotedValue = promoteValue(value);
        int pos;
        if (type == AxisType.DISCRETE || type == AxisType.RANGE)
        {	// DISCRETE and RANGE axis searched in O(Log n) time using a binary search
            pos = binarySearchAxis(promotedValue);
        }
        else if (type == AxisType.SET)
        {	// The SET axis searched in O(Log n)
            return findOnSetAxis(promotedValue);
        }
        else if (type == AxisType.NEAREST)
        {   // The NEAREST axis type must be searched linearly O(n)
            pos = findNearest(promotedValue);
        }
        else
        {
            throw new IllegalArgumentException("Axis type '" + type + "' added but no code supporting it.");
        }

        if (pos >= 0)
        {
            return columns.get(pos);
        }

        return defaultCol;
    }

    /**
     * In the case of a multiMatch axis, return all columns that match.
     */
    List<Column> findColumns(Comparable value)
    {
        List<Column> cols = new ArrayList<Column>();
        if (value == null)
        {
            if (defaultCol != null)
            {
                cols.add(defaultCol);
                return cols;
            }
            throw new IllegalArgumentException("'null' passed to axis '" + name + "' which does not have a default column");
        }

        final Comparable promotedValue = promoteValue(value);

        if (multiMatch)
        {
            if (type == AxisType.SET)
            {   // Linearly scan all columns, because multiMatch is true
                for (Column column : getColumnsWithoutDefault())
                {
                    RangeSet set = (RangeSet) column.getValue();
                    if (set.contains(promotedValue))
                    {
                        cols.add(column);
                    }
                }
            }
            else if (type == AxisType.RANGE)
            {   // Linearly scan all columns, because multiMatch is true
                for (Column column : getColumnsWithoutDefault())
                {
                    Range range = (Range) column.getValue();
                    if (range.isWithin(promotedValue) == 0)
                    {
                        cols.add(column);
                    }
                }
            }
            if (cols.isEmpty() && hasDefaultColumn())
            {   // Add in default, but only if no matches occurred.
                cols.add(defaultCol);
            }
        }
        else
        {
            Column col = findColumn(promotedValue);
            if (col != null)
            {
                cols.add(col);
            }
        }
        return cols;
    }

    private Column findOnSetAxis(final Comparable promotedValue)
	{
        Column col = discreteToCol.get(promotedValue);
        if (discreteToCol.containsKey(promotedValue))
        {
            return col;
        }

        int pos = binarySearchRanges(promotedValue);
        if (pos >= 0)
        {
            return (rangeToCol.get(pos)).column;
        }

        return getDefaultColumn();
    }

    private int binarySearchRanges(final Comparable promotedValue)
    {
        return Collections.binarySearch(rangeToCol, promotedValue, new Comparator()
        {
            public int compare(Object r1, Object r2)
            {
                RangeToColumn rc = (RangeToColumn) r1;
                Range range = rc.getRange();
                return -1 * range.isWithin(promotedValue);
            }
        });
    }

	private int binarySearchAxis(final Comparable promotedValue)
	{
        List cols = getColumnsWithoutDefault();
		return Collections.binarySearch(cols, promotedValue, new Comparator()
		{
			public int compare(Object o1, Object o2)
			{
				Column column = (Column) o1;

				if (type == AxisType.DISCRETE)
				{
					return column.compareTo(promotedValue);
				}
				else if (type == AxisType.RANGE)
				{
					Range range = (Range)column.getValue();
					return -1 * range.isWithin(promotedValue);
				}
                else
                {
                    throw new IllegalStateException("Cannot binary search axis type: '" + type + "'");
                }
			}
		});
	}

    private int findNearest(final Comparable promotedValue)
    {
        double min = Double.MAX_VALUE;
        int savePos = -1;
        int pos = 0;

        for (Column column : getColumnsWithoutDefault())
        {
            double d = Proximity.distance(promotedValue, column.getValue());
            if (d < min)
            {	// Record column that set's new minimum record
                min = d;
                savePos = pos;
            }
            pos++;
        }
        return savePos;
    }

    /**
     * Ensure that the passed in range does not overlap an existing Range on this
     * 'Range-type' axis.  This method is only called in non-multiMatch mode.
     * Test low range limit to see if it is valid.  Axis is already a RANGE type
     * before this method is called.
     * @param value Range (value) that is intended to be a new low range limit.
     * @return true if the Range overlaps this axis, false otherwise.
     */
    private boolean doesOverlap(Range value)
    {
        // Start just before where this range would be inserted.
        int where = binarySearchAxis(value.low);
        if (where < 0)
        {
            where = Math.abs(where + 1);
        }
        where = Math.max(0, where - 1);
        int size = getColumnsWithoutDefault().size();

        for (int i = where; i < size; i++)
        {
            Column column = getColumnsWithoutDefault().get(i);
            Range range = (Range) column.getValue();
            if (value.overlap(range))
            {
                return true;
            }

            if (value.low.compareTo(range.low) <= 0)
            {   // No need to continue, once the passed in low is less or equals to the low of the next column
                break;
            }
        }
        return false;
	}

    /**
     * Test RangeSet to see if it overlaps any of the existing columns on
     * this cube.  Axis is already a RangeSet type before this method is called.
     * @param value RangeSet (value) to be checked
     * @return true if the RangeSet overlaps this axis, false otherwise.
     */
    private boolean doesOverlap(RangeSet value)
    {
        for (Column column : getColumnsWithoutDefault())
        {
            RangeSet set = (RangeSet) column.getValue();
            if (value.overlap(set))
            {
                return true;
            }
        }
        return false;
}

    private void doesMatchExistingValue(Comparable v)
    {
        if (binarySearchAxis(v) >= 0)
        {
            throw new AxisOverlapException("Passed in value '" + v + "' matches a value already on axis '" + name + "'");
        }
    }

    private void doesMatchNearestValue(Comparable v)
    {
        for (Column col : columns)
        {
            Object val = col.getValue();
            if (v.equals(val))
            {
                throw new AxisOverlapException("Passed in value '" + v + "' matches a value already on axis '" + name + "'");
            }
        }
    }

    List<Column> getColumnsWithoutDefault()
    {
        if (defaultCol != null)
        {
            if (columns.size() == 1)
            {
                return new ArrayList<Column>();
            }
            return columns.subList(0, columns.size() - 1);
        }
        return columns;
    }

    private static class RangeToColumn implements Comparable<RangeToColumn>
    {
        private Range range;
        private Column column;

        private RangeToColumn(Range range, Column column)
        {
            this.range = range;
            this.column = column;
        }

        private Range getRange()
        {
            return range;
        }

        public int compareTo(RangeToColumn rc)
        {
            return range.compareTo(rc.range);
        }
    }
}
