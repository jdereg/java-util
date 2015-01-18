package com.cedarsoftware.ncube;

import com.cedarsoftware.ncube.exception.AxisOverlapException;
import com.cedarsoftware.ncube.exception.CoordinateNotFoundException;
import com.cedarsoftware.ncube.proximity.LatLon;
import com.cedarsoftware.ncube.proximity.Point3D;
import com.cedarsoftware.util.CaseInsensitiveMap;
import com.cedarsoftware.util.Converter;
import com.cedarsoftware.util.MapUtilities;
import com.cedarsoftware.util.StringUtilities;
import com.cedarsoftware.util.io.JsonReader;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
    final long id;
    long colIdBase = 0;
    private String name;
    private final AxisType type;
    private final AxisValueType valueType;
    final List<Column> columns = new CopyOnWriteArrayList<>();
    private Column defaultCol;
    private int preferredOrder = SORTED;
    private boolean fireAll = true;
    Map<String, Object> metaProps = null;

	public static final int SORTED = 0;
	public static final int DISPLAY = 1;
    private static final AtomicLong baseAxisIdForTesting = new AtomicLong(1);
    public static final Pattern rangePattern = Pattern.compile("\\s*([^,]+)[,](.*)\\s*$");

    // used to get O(1) on SET axis for the discrete elements in the Set
    transient Map<Comparable, Column> discreteToCol = new TreeMap<>();

    // used to get O(1) on Ranges for SET access
    transient List<RangeToColumn> rangeToCol = new ArrayList<>();

    // used to get O(1) access to columns by ID
    transient Map<Long, Column> idToCol = new HashMap<>();

    // used to get O(1) access to columns by rule-name
    transient Map<String, Column> colNameToCol = new CaseInsensitiveMap<>();

    // for testing
    Axis(String name, AxisType type, AxisValueType valueType, boolean hasDefault)
	{
		this(name, type, valueType, hasDefault, SORTED);
	}

    // for testing
    Axis(String name, AxisType type, AxisValueType valueType, boolean hasDefault, int order)
	{
        this(name, type, valueType, hasDefault, order, baseAxisIdForTesting.getAndIncrement());
	}

    public Axis(String name, AxisType type, AxisValueType valueType, boolean hasDefault, int order, long id)
    {
        this(name, type, valueType, hasDefault, order, id, true);
    }

    public Axis(String name, AxisType type, AxisValueType valueType, boolean hasDefault, int order, long id, boolean fireAll)
    {
        this.id = id;
        this.name = name;
        this.type = type;
        preferredOrder = order;
        this.fireAll = fireAll;
        if (type == AxisType.RULE)
        {
            preferredOrder = DISPLAY;
            this.valueType = AxisValueType.EXPRESSION;
        }
        else if (type == AxisType.NEAREST)
        {
            preferredOrder = SORTED;
            this.valueType = valueType;
            defaultCol = null;
        }
        else
        {
            this.valueType = valueType;
        }

        if (hasDefault && type != AxisType.NEAREST)
        {
            defaultCol = new Column(null, getDefaultColId());
            defaultCol.setDisplayOrder(Integer.MAX_VALUE);  // Always at the end
            columns.add(defaultCol);
            idToCol.put(defaultCol.id, defaultCol);
        }
    }

    long getNextColId()
    {
        return id * 1000000000000L + (++colIdBase);
    }

    long getDefaultColId()
    {
        return id * 1000000000000L + Integer.MAX_VALUE;
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
     * Set (add / overwrite) a Meta Property associated to this axis.
     * @param key String key name of meta property
     * @param value Object value to associate to key
     * @return prior value associated to key or null if none was associated prior
     */
    public Object setMetaProperty(String key, Object value)
    {
        if (metaProps == null)
        {
            metaProps = new CaseInsensitiveMap<>();
        }
        return metaProps.put(key, value);
    }

    /**
     * Fetch the value associated to the passed in Key from the MetaProperties (if any exist).  If
     * none exist, null is returned.
     */
    public Object getMetaProperty(String key)
    {
        if (metaProps == null)
        {
            return null;
        }
        return metaProps.get(key);
    }

    /**
     * Remove a meta-property entry
     */
    public Object removeMetaProperty(String key)
    {
        if (metaProps == null)
        {
            return null;
        }
        return metaProps.remove(key);
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
     * Remove all meta properties associated to this Axis.
     */
    public void clearMetaProperties()
    {
        if (metaProps != null)
        {
            metaProps.clear();
            metaProps = null;
        }
    }

    public boolean isFireAll()
    {
        return fireAll;
    }

    public void setFireAll(boolean fireAll)
    {
        this.fireAll = fireAll;
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
     * scaffolding that ALWAYS must be present.
     */
    boolean hasScaffolding()
    {
        return type == AxisType.SET;
    }

    /**
     * Use Column id to retrieve column (hash map lookup), O(1)
     */
    public Column getColumnById(long colId)
    {
        return idToCol.get(colId);
    }

    void buildScaffolding()
    {
        rangeToCol.clear();
        discreteToCol.clear();
        idToCol.clear();
        colNameToCol.clear();
        for (Column column : columns)
        {
            addScaffolding(column);
        }
    }

    private void addScaffolding(Column column)
    {
        idToCol.put(column.id, column);
        String colName = (String) column.getMetaProperty(Column.NAME);
        if (StringUtilities.hasContent(colName))
        {
            colNameToCol.put(colName, column);
        }

        if (!hasScaffolding())
        {   // This check is required because this API may be called from other than buildScaffolding() (e.g., addColumn)
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

    public String getAxisPropString()
    {
        StringBuilder s = new StringBuilder();
        s.append("Axis: ");
        s.append(name);
        s.append(" [");
        s.append(type);
        s.append(", ");
        s.append(valueType);
        s.append(hasDefaultColumn() ? ", default-column" : ", no-default-column");
        s.append(Axis.SORTED == preferredOrder ? ", sorted" : ", unsorted");
        s.append(']');
        return s.toString();
    }

    public String toString()
    {
        StringBuilder s = new StringBuilder(getAxisPropString());
        if (!MapUtilities.isEmpty(metaProps))
        {
            s.append("\n");
            s.append("  metaProps: " + metaProps);
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
        List<Column> cols = new ArrayList<>(columns);
        if (type != AxisType.RULE)
        {
            if (preferredOrder == SORTED)
            {
                return cols;	// Return a copy of the columns, not our internal values list.
            }
            sortColumnsByDisplayOrder(cols);
        }
        return cols;
	}

    List<Column> getColumnsInternal()
    {
        return columns;
    }

    /**
     * Given the passed in 'raw' value, get a Column from the passed in value, which entails
     * converting the 'raw' value to the correct type, promoting the value to the appropriate
     * internal value for comparison, and so on.
     * @param value Comparable typically a primitive, but can also be an n-cube Range, RangeSet, CommandCell,
     *              or 2D, 3D, or LatLon
     * @return a Column with the up-promoted value as the column's value, and a unique ID on the column.  If
     * the original value is a Range or RangeSet, the components in the Range or RangeSet are also up-promoted.
     */
    Column createColumnFromValue(Comparable value)
    {
        Comparable v;
        if (value == null)
        {  // Attempting to add Default column to axis
            v = null;
        }
        else
        {
            if (type == AxisType.DISCRETE)
            {
                v = standardizeColumnValue(value);
            }
            else if (type == AxisType.RANGE || type == AxisType.SET)
            {
                v = value instanceof String ? convertStringToColumnValue((String) value) : standardizeColumnValue(value);
            }
            else if (type == AxisType.RULE)
            {
                v = value instanceof String ? convertStringToColumnValue((String) value) : value;
            }
            else if (type == AxisType.NEAREST)
            {
                v = standardizeColumnValue(value);
            }
            else
            {
                throw new IllegalStateException("New axis type added without complete support.");
            }
        }
        return new Column(v, getNextColId());
    }

    /**
     * Will throw IllegalArgumentException if passed in value duplicates a value on this axis.
     */
    void ensureUnique(Comparable value)
    {
        if (value == null)
        {  // Attempting to add Default column to axis
            if (hasDefaultColumn())
            {
                throw new IllegalArgumentException("Cannot add default column to axis '" + name + "' because it already has a default column.");
            }
            if (type == AxisType.NEAREST)
            {
                throw new IllegalArgumentException("Cannot add default column to NEAREST axis '" + name + "' as it would never be chosen.");
            }
        }
        else
        {
            if (type == AxisType.DISCRETE)
            {
                doesMatchExistingValue(value);
            }
            else if (type == AxisType.RANGE)
            {
                Range range = (Range)value;
                if (doesOverlap(range))
                {
                    throw new AxisOverlapException("Passed in Range overlaps existing Range on axis: " + name + ", value: " + value);
                }
            }
            else if (type == AxisType.SET)
            {
                RangeSet set = (RangeSet)value;
                if (doesOverlap(set))
                {
                    throw new AxisOverlapException("Passed in RangeSet overlaps existing RangeSet on axis: " + name + ", value: " + value);
                }
            }
            else if (type == AxisType.RULE)
            {
                if (!(value instanceof CommandCell))
                {
                    throw new IllegalArgumentException("Columns for RULE axis must be a CommandCell, axis: " + name + ", value: " + value);
                }
            }
            else if (type == AxisType.NEAREST)
            {
                doesMatchNearestValue(value);
            }
            else
            {
                throw new IllegalStateException("New axis type added without complete support.");
            }
        }
    }

	public Column addColumn(Comparable value)
	{
        return addColumn(value, null);
    }

	public Column addColumn(Comparable value, String name)
	{
        final Column column = createColumnFromValue(value);
        if (StringUtilities.hasContent(name))
        {
            column.setMetaProperty(Column.NAME, name);
        }
        addColumnInternal(column);
        return column;
    }

	Column addColumnInternal(Column column)
	{
        ensureUnique(column.getValue());

        if (column.getValue() == null)
        {
            column.setId(getDefaultColId());
            defaultCol = column;
        }

        // New columns are always added at the end in terms of displayOrder, but internally they are added
        // in the correct sort order location.  The sort order of the list is required because binary searches
        // are done against it.
        int dispOrder = hasDefaultColumn() ? size() - 1 : size();
        column.setDisplayOrder(column.getValue() == null ? Integer.MAX_VALUE : dispOrder);
        if (type == AxisType.RULE)
        {   // Rule columns are added in 'occurrence' order
            if (column != defaultCol && hasDefaultColumn())
            {   // Insert right before default column at the end
                columns.add(columns.size() - 1, column);
            }
            else
            {
                columns.add(column);
            }
        }
        else
        {
            int where = Collections.binarySearch(columns, column.getValue());
            if (where < 0)
            {
                where = Math.abs(where + 1);
            }
            columns.add(where, column);
        }
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

        return deleteColumnById(col.id);
	}

    Column deleteColumnById(long colId)
    {
        Column col = idToCol.get(colId);
        if (col == null)
        {
            return null;
        }

        columns.remove(col);
        if (col.isDefault())
        {
            defaultCol = null;
        }

        // Remove column from scaffolding
        removeColumnFromScaffolding(col);
        return col;
    }

    private void removeColumnFromScaffolding(Column col)
    {
        if (!hasScaffolding())
        {
            idToCol.remove(col.id);
            colNameToCol.remove(col.getMetaProperty(Column.NAME));
            return;
        }

        if (discreteToCol != null)
        {
            Iterator<Column> j = discreteToCol.values().iterator();
            while (j.hasNext())
            {
                Column column = j.next();
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
                if (rangeToColumn.column.equals(col))
                {   // Multiple ranges may have pointed to the passed in column, so we must loop through all
                    i.remove();
                }
            }
        }

        // Remove from col id to column map
        idToCol.remove(col.id);
    }

    /**
     * Update (change) the value of an existing column.  This entails not only
     * changing the value, but resorting the axis's columns (columns are always in
     * sorted order for quick retrieval).  The display order of the columns is not
     * rebuilt, because the column is changed in-place (e.g., changing Mon to Monday
     * does not change it's display order.)
     * @param colId long Column ID to update
     * @param value 'raw' value to set into the new column (will be up-promoted).
     */
    public void updateColumn(long colId, Comparable value)
    {
        Column col = idToCol.get(colId);
        deleteColumnById(colId);
        Column newCol = createColumnFromValue(value);
        ensureUnique(newCol.getValue());
        newCol.setId(colId);
        newCol.setDisplayOrder(col.getDisplayOrder());

        // Updated column is added in the same 'displayOrder' location.  For example, the months are a
        // displayOrder Axis type.  Updating 'Jun' to 'June' will use the same displayOrder value.
        // However, the columns are stored internally in sorted order (for fast lookup), so we need to
        // find where it should go (updating Fune to June, for example (fixing a misspelling), will
        // result in the column being sorted to a different location (while maintaining its display
        // order, because displayOrder is stored on the column).
        int where = Collections.binarySearch(columns, newCol.getValue());
        if (where < 0)
        {
            where = Math.abs(where + 1);
        }
        columns.add(where, newCol);
        addScaffolding(newCol);
    }

    /**
     * Update columns on this Axis, from the passed in Axis.  Columns that exist on both axes,
     * will have their values updated.  Columns that exist on this axis, but not exist in the
     * 'newCols' will be deleted (and returned as a Set of deleted Columns).  Columns that
     * exist in newCols but not on this are new columns.
     *
     * NOTE: The columns field within the newCols axis are NOT in sorted order as they normally are
     * within the Axis class.  Instead, they are in display order (this order is typically set forth by a UI).
     * Axis is used as a Data-Transfer-Object (DTO) in this case, not the normal way it is typically used
     * where the columns would always be sorted for quick access.
     */
    public Set<Long> updateColumns(final Axis newCols)
    {
        Set<Long> colsToDelete = new HashSet<>();
        Map<Long, Column> newColumnMap = new LinkedHashMap<>();

        // Step 1. Map all columns coming in from "DTO" Axis by ID
        for (Column col : newCols.columns)
        {
            Column newColumn = createColumnFromValue(col.getValue());
            Map<String, Object> metaProperties = col.getMetaProperties();
            for (Map.Entry<String, Object> entry : metaProperties.entrySet())
            {
                newColumn.setMetaProperty(entry.getKey(), entry.getValue());
            }

            newColumnMap.put(col.id, newColumn);
        }

        // Step 2.  Build list of columns that no longer exist (add to deleted list)
        // AND update existing columns that match by ID columns from the passed in DTO.
        List<Column> tempCol = new ArrayList<>(getColumnsWithoutDefault());
        Iterator<Column> i = tempCol.iterator();

        while (i.hasNext())
        {
            Column col = i.next();
            if (newColumnMap.containsKey(col.id))
            {   // Update case - matches existing column
                Column newCol = newColumnMap.get(col.id);
                col.setValue(newCol.getValue());

                if (newCol.getMetaProperty(Column.NAME) != null)
                {   // Copy 'name' meta-property (used on Rule axis Expression [condition] columns)
                    col.setMetaProperty(Column.NAME, newCol.getMetaProperty(Column.NAME));
                }
                Map<String, Object> metaProperties = newCol.getMetaProperties();
                for (Map.Entry<String, Object> entry : metaProperties.entrySet())
                {
                    col.setMetaProperty(entry.getKey(), entry.getValue());
                }
            }
            else
            {   // Delete case - existing column id no longer found
                colsToDelete.add(col.id);
                i.remove();
            }
        }

        columns.clear();
        for (Column column : tempCol)
        {
            addColumnInternal(column);
        }

        Map<Long, Column> realColumnMap = new LinkedHashMap<>();

        for (Column column : columns)
        {
            realColumnMap.put(column.id, column);
        }
        int displayOrder = 0;

        // Step 4. Add new columns (they exist in the passed in Axis, but not in this Axis) and
        // set display order to match the columns coming in from the DTO axis (argument).
        for (Column col : newCols.getColumns())
        {
            if (col.getValue() == null)
            {   // Skip Default column
                continue;
            }
            long realId = col.id;
            if (col.id < 0)
            {   // Add case - negative id, add new column to 'columns' List.
                Column newCol = addColumnInternal(newColumnMap.get(col.id));
                realId = newCol.id;
                realColumnMap.put(realId, newCol);
            }
            Column realColumn = realColumnMap.get(realId);
            if (realColumn == null)
            {
                throw new IllegalArgumentException("Columns to be added should have negative ID values.");
            }
            realColumn.setDisplayOrder(displayOrder++);
        }

        if (type == AxisType.RULE)
        {   // required because RULE columns are stored in execution order
            sortColumnsByDisplayOrder(columns);
        }

        // Put default column back if it was already there.
        if (hasDefaultColumn())
        {
            columns.add(defaultCol);
        }

        // index
        buildScaffolding();
        return colsToDelete;
    }

    /**
     * Sorted this way to allow for CopyOnWriteArrayList or regular ArrayLists to be sorted.
     * CopyOnWriteArrayList does not support iterator operations .set() for example, which
     * would be called by Collections.sort();
     * @param cols List of Columns to sort
     */
    private static void sortColumns(List cols, Comparator comparator)
    {
        Object[] colArray = cols.toArray();
        Arrays.sort(colArray, comparator);

        final int len = colArray.length;
        for (int i=0; i < len; i++)
        {
            cols.set(i, colArray[i]);
        }
    }

    // Take the passed in value, and prepare it to be allowed on a given axis type.
    public Comparable convertStringToColumnValue(String value)
    {
        if (StringUtilities.isEmpty(value))
        {
            throw new IllegalArgumentException("Column value cannot be empty, axis: " + name);
        }

        switch(type)
        {
            case DISCRETE:
                return standardizeColumnValue(value);

            case RANGE:
                Matcher matcher = rangePattern.matcher(value);
                if (matcher.matches())
                {
                    String one = matcher.group(1);
                    String two = matcher.group(2);
                    return standardizeColumnValue(new Range(one.trim(), two.trim()));
                }
                else
                {
                    throw new IllegalArgumentException("Value (" + value + ") cannot be parsed as a Range.  Use [value1, value2], axis: " + name);
                }

            case SET:
                value = "[" + value + "]";
                try
                {
                    Object[] array = (Object[])JsonReader.jsonToJava(value);
                    RangeSet set = new RangeSet();
                    for (Object pt : array)
                    {
                        if (pt instanceof Object[])
                        {
                            Object[] rangeValues = (Object[]) pt;
                            if (rangeValues.length != 2)
                            {
                                throw new IllegalArgumentException("Set Ranges must have two values only, range length: " + rangeValues.length + ", axis: " + name);
                            }
                            if (!(rangeValues[0] instanceof Comparable) || !(rangeValues[1] instanceof Comparable))
                            {
                                throw new IllegalArgumentException("Set Ranges must have two Comparable values, axis: " + name);
                            }
                            Range range = new Range((Comparable)rangeValues[0], (Comparable)rangeValues[1]);
                            set.add(range);
                        }
                        else
                        {
                            if (!(pt instanceof Comparable))
                            {
                                throw new IllegalArgumentException("Set values must implement Comparable, axis: " + name);
                            }
                            set.add((Comparable)pt);
                        }
                    }
                    return standardizeColumnValue(set);
                }
                catch (Exception e)
                {
                    throw new IllegalArgumentException("Value: " + value + " cannot be parsed as a Set.  Use v1, v2, [low, high], v3, ... , axis: " + name, e);
                }

            case NEAREST:
                return standardizeColumnValue(value);

            case RULE:
                return new GroovyExpression(value, null);

            default:
                throw new IllegalStateException("Unsupported axis type (" + type + ") for axis '" + name + "', trying to parse value: " + value);
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

    /**
     * @param cols List of Column instances to be sorted.
     */
	private static void sortColumnsByDisplayOrder(List<Column> cols)
	{
        sortColumns(cols, new Comparator<Column>()
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
			return promoteValue(valueType, value);
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
                    val = promoteValue(valueType, val);
                }
                set.add(val);
            }
            return set;
		}
		else if (type == AxisType.NEAREST)
		{	// Standardizing a NEAREST axis entails ensuring conformity amongst values (must all be Point2D, LatLon, Date, Long, String, etc.)
			value = promoteValue(valueType, value);
			if (!getColumnsWithoutDefault().isEmpty())
			{
				Column col = columns.get(0);
                if (value.getClass() != col.getValue().getClass())
				{
					throw new IllegalArgumentException("Value '" + value.getClass().getName() + "' cannot be added to axis '" + name + "' where the values are of type: " + col.getValue().getClass().getName());
				}
			}
			return value;	// First value added does not need to be checked
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
		final Comparable low = promoteValue(valueType, range.low);
		final Comparable high = promoteValue(valueType, range.high);
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
     * Convert passed in value to a similar value of the highest type.  If the
     * valueType is not the same basic type as the value passed in, intelligent
     * conversions will happen, and the result will be of the requested type.
     *
     * An intelligent conversion example - String to date, it will parse the String
     * attempting to convert it to a date.  Or a String to a long, it will try to
     * parse the String as a long.  Long to String, it will .toString() the long,
     * and so on.
     * @return promoted value, or the same value if no promotion occurs.
     */
    public static Comparable promoteValue(AxisValueType srcValueType, Comparable value)
    {
        switch(srcValueType)
        {
            case STRING:
                return (String) Converter.convert(value, String.class);
            case LONG:
                return (Long) Converter.convert(value, Long.class);
            case BIG_DECIMAL:
                return (BigDecimal) Converter.convert(value, BigDecimal.class);
            case DOUBLE:
                return (Double) Converter.convert(value, Double.class);
            case DATE:
                return (Date) Converter.convert(value, Date.class);
            case COMPARABLE:
                if (value instanceof String)
                {
                    Matcher m = Regexes.valid2Doubles.matcher((String) value);
                    if (m.matches())
                    {   // No way to determine if it was supposed to be a Point2D. Specify as JSON for Point2D
                        return new LatLon((Double)Converter.convert(m.group(1), double.class), (Double)Converter.convert(m.group(2), double.class));
                    }

                    m = Regexes.valid3Doubles.matcher((String) value);
                    if (m.matches())
                    {
                        return new Point3D((Double)Converter.convert(m.group(1), double.class), (Double)Converter.convert(m.group(2), double.class), (Double)Converter.convert(m.group(3), double.class));
                    }

                    try
                    {   // Try as JSON
                        return (Comparable) JsonReader.jsonToJava((String) value);
                    }
                    catch (Exception e)
                    {
                        return value;
                    }
                }
                return value;
            case EXPRESSION:
                return value;
            default:
                throw new IllegalArgumentException("AxisValueType '" + srcValueType + "' added but no code to support it.");
        }
    }

	public boolean hasDefaultColumn()
	{
		return defaultCol != null;
	}

	/**
	 * @param value to test against this Axis
	 * @return boolean true if the value will be found along the axis, false
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

    List<Column> getRuleColumnsStartingAt(String ruleName)
    {
        if (StringUtilities.isEmpty(ruleName))
        {   // Since no rule name specified, all rule columns are returned to have their conditions evaluated.
            return getColumns();
        }

        List<Column> cols = new ArrayList<>();
        Column firstRule = findColumn(ruleName);
        if (firstRule == null)
        {   // A name was specified for a rule, but did not match any rule names and there is no default column.
            throw new CoordinateNotFoundException("Rule named '" + ruleName + "' matches no column names on the rule axis '" + name + "', and there is no default column.");
        }
        else if (firstRule == defaultCol)
        {   // Matched no names, but there is a default column
            cols.add(defaultCol);
            return cols;
        }

        int pos = firstRule.getDisplayOrder();
        final List<Column> allColumns = getColumns();
        final int len = allColumns.size();

        for (int i=pos; i < len; i++)
        {
            cols.add(allColumns.get(i));
        }
        return cols;
    }

    /**
     * Locate the column (AvisValue) along an axis.
     * @param value Comparable - A value that can be checked against the axis
     * @return Column that 'matches' the passed in value, or null if no column
     * found.  'Matches' because matches depends on AxisType.
     */
     public Column findColumn(Comparable value)
     {
        if (value == null)
        {
            if (hasDefaultColumn())
            {
                return defaultCol;
            }
            throw new IllegalArgumentException("'null' passed to axis '" + name + "' which does not have a default column");
        }

        final Comparable promotedValue = promoteValue(valueType, value);
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
        else if (type == AxisType.RULE)
        {
            if (!(promotedValue instanceof String))
            {
                throw new IllegalArgumentException("A column on a rule axis can only be located by the 'name' attribute, which must be a String, axis: " + name);
            }

            return findColumnByName((String)promotedValue);
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
     * Locate a column on an axis using the 'name' meta property.  If the value passed in matches no names, then
     * the Default column will be returned if one exists, otherwise null will be returned.
     * Note: This is a case-insensitive match.
     */
    public Column findColumnByName(String name)
    {
        Column col = colNameToCol.get(name);
        if (col != null)
        {
            return col;
        }
        return defaultCol;
    }

    private Column findOnSetAxis(final Comparable promotedValue)
	{
        if (discreteToCol.containsKey(promotedValue))
        {
            return discreteToCol.get(promotedValue);
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
            public int compare(Object r1, Object key)
            {   // key not used as promoteValue, already of type Comparable, is the exact same thing, and already final
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
			public int compare(Object o1, Object key)
			{   // key not used as promoteValue, already of type Comparable, is the exact same thing, and already final
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
     * 'Range-type' axis.  Test low range limit to see if it is valid.
     * Axis is already a RANGE type before this method is called.
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

    public List<Column> getColumnsWithoutDefault()
    {
        if (columns.size() == 0)
        {
            return columns;
        }
        if (hasDefaultColumn())
        {
            if (columns.size() == 1)
            {
                return new ArrayList<>();
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

    public boolean areAxisPropsEqual(Object o)
    {
        if (this == o)
        {
            return true;
        }
        if (!(o instanceof Axis))
        {
            return false;
        }

        Axis axis = (Axis) o;

        if (preferredOrder != axis.preferredOrder)
        {
            return false;
        }
        if (defaultCol != null ? !defaultCol.equals(axis.defaultCol) : axis.defaultCol != null)
        {
            return false;
        }
        if (!name.equals(axis.name))
        {
            return false;
        }
        if (type != axis.type)
        {
            return false;
        }
        if (valueType != axis.valueType)
        {
            return false;
        }
        if (fireAll != axis.fireAll)
        {
            return false;
        }

        return true;
    }
}
