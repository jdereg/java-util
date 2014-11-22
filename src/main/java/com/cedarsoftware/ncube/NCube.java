package com.cedarsoftware.ncube;

import com.cedarsoftware.ncube.exception.CoordinateNotFoundException;
import com.cedarsoftware.ncube.exception.RuleJump;
import com.cedarsoftware.ncube.exception.RuleStop;
import com.cedarsoftware.ncube.formatters.HtmlFormatter;
import com.cedarsoftware.ncube.formatters.JsonFormatter;
import com.cedarsoftware.util.ArrayUtilities;
import com.cedarsoftware.util.CaseInsensitiveMap;
import com.cedarsoftware.util.CaseInsensitiveSet;
import com.cedarsoftware.util.DeepEquals;
import com.cedarsoftware.util.EncryptionUtilities;
import com.cedarsoftware.util.MapUtilities;
import com.cedarsoftware.util.ReflectionUtils;
import com.cedarsoftware.util.StringUtilities;
import com.cedarsoftware.util.io.JsonObject;
import com.cedarsoftware.util.io.JsonReader;
import com.cedarsoftware.util.io.JsonWriter;
import groovy.util.MapEntry;

import java.io.IOException;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;

/**
 * Implements an n-cube.  This is a hyper (n-dimensional) cube
 * of cells, made up of 'n' number of axes.  Each Axis is composed
 * of Columns that denote discrete nodes along an axis.  Use NCubeManager
 * to manage a list of NCubes.  Documentation on Github.
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
public class NCube<T>
{
    String name;
    private final Map<String, Axis> axisList = new LinkedHashMap<>();
    final Map<Set<Column>, T> cells = new HashMap<>();
    private T defaultCellValue;
    private volatile Set<String> optionalScopeKeys = null;
    private volatile Set<String> declaredScopeKeys = null;

    //  Sets up the defaultApplicationId for cubes loaded in from disk.
    private transient ApplicationID appId = ApplicationID.defaultAppId;

    public static final String validCubeNameChars = "0-9a-zA-Z:._-";
    public static final String RULE_EXEC_INFO = "_rule";
    private static final ThreadLocal<Deque<StackEntry>> executionStack = new ThreadLocal<Deque<StackEntry>>()
    {
        public Deque<StackEntry> initialValue()
        {
            return new ArrayDeque<>();
        }
    };
    private Map<String, Advice> advices = new LinkedHashMap<>();
    private Map<String, Object> metaProps = null;

    /**
     * @return Map (case insensitive keys) containing meta (additional) properties for the n-cube.
     */
    public Map<String, Object> getMetaProperties()
    {
        Map ret = metaProps == null ? new CaseInsensitiveMap() : metaProps;
        return Collections.unmodifiableMap(ret);
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
     * If a meta property value is fetched from an Axis or a Column, the value should be extracted
     * using this API, so as to allow executable values to be retreived.
     * @param value Object value to be extracted.
     */
    public Object extractMetaPropertyValue(Object value)
    {
        if (value instanceof CommandCell)
        {
            CommandCell cmd = (CommandCell) value;
            value = cmd.execute(prepareExecutionContext(new HashMap(), new HashMap()));
        }
        return value;
    }

    /**
     * Set (add / overwrite) a Meta Property associated to this n-cube.
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
     * Remove all meta properties associated to this n-cube.
     */
    public void clearMetaProperties()
    {
        if (metaProps != null)
        {
            metaProps.clear();
            metaProps = null;
        }
    }

    /**
     * This is a "Pointer" (or Key) to a cell in an NCube.
     * It consists of a String cube Name and a Set of
     * Column references (one Column per axis).
     */
    public static class StackEntry
    {
        final String cubeName;
        final Map<String, Object> coord;

        public StackEntry(String name, Map<String, Object> coordinate)
        {
            cubeName = name;
            coord = coordinate;
        }

        public String toString()
        {
            StringBuilder s = new StringBuilder();
            s.append(cubeName);
            s.append(":{");

            Iterator<Map.Entry<String, Object>> i = coord.entrySet().iterator();
            while (i.hasNext())
            {
                Map.Entry<String, Object> coordinate = i.next();
                s.append(coordinate.getKey());
                s.append(':');
                s.append(coordinate.getValue());
                if (i.hasNext())
                {
                    s.append(',');
                }
            }
            s.append('}');
            return s.toString();
        }
    }

    /**
     * Add advice to this n-cube that will be called before / after any Controller Method or
     * URL-based Expression, for the given method
     */
    void addAdvice(Advice advice, String method)
    {
        advices.put(advice.getName() + '/' + method, advice);
    }

    /**
     * @return List<Advice> advices added to this n-cube.
     */
    public List<Advice> getAdvices(String method)
    {
        List<Advice> result = new ArrayList<>();
        method = '/' + method;
        for (Map.Entry<String, Advice> entry : advices.entrySet())
        {
            // Entry key = "AdviceName/MethodName"
            if (entry.getKey().endsWith(method))
            {   // Entry.Value = Advice instance
                result.add(entry.getValue());
            }
        }

        return result;
    }

    /**
     * For testing, advices need to be removed after test completes.
     */
    void clearAdvices()
    {
        advices.clear();
    }

    /**
     * Creata a new NCube instance with the passed in name
     * @param name String name to use for the NCube.
     */
    public NCube(String name)
    {
        if (name != null)
        {   // If name is null, likely being instantiated via serialization
            validateCubeName(name);
        }
        this.name = name;
    }

    /**
     * @return ReleaseStatus of this n-cube as it was loaded.
     */
    public String getStatus()
    {
        return appId.getStatus();
    }

    /**
     * @return String version of this n-cube.  The version is set when the n-cube is loaded by
     * the NCubeManager.
     */
    public String getVersion()
    {
        return appId.getVersion();
    }

    public void setApplicationID(ApplicationID appId)
    {
        this.appId = appId;
    }

    /**
     * @return ApplicationID for this n-cube.  This contains the app name, version, etc. that this
     * n-cube is part of.
     */
    public ApplicationID getApplicationID()
    {
        return appId;
    }

    /**
     * @return String name of the NCube
     */
    public String getName()
    {
        return name;
    }

    /**
     * Clear (remove) the cell at the given coordinate.  The cell is dropped
     * from the internal sparse storage.
     * @param coordinate Map coordinate of Cell to remove.
     * @return value of cell that was removed
     */
    public T removeCell(final Map<String, Object> coordinate)
    {
        clearScopeKeyCaches();
        return cells.remove(getCoordinateKey(validateCoordinate(coordinate, true)));
    }

    /**
     * Clear a cell directly from the cell sparse-matrix specified by the passed in Column
     * IDs. After this call, containsCell() for the same coordinate would return false.
     */
    public T removeCellById(final Set<Long> coordinate)
    {
        clearScopeKeyCaches();
        Set<Column> cols = new HashSet<>();
        getColumnsAndCoordinateFromIds(coordinate, cols, null);
        return cells.remove(cols);
    }

    /**
     * @param coordinate Map (coordinate) of a cell
     * @return boolean true if a cell has been mapped at the specified coordinate,
     * false otherwise.  For RULE axes, the name of the Rule Axis must be
     * bound to a rule name (e.g. the 'name' attribute on the Column expression).
     */
    public boolean containsCell(final Map<String, Object> coordinate)
    {
        Set<Column> cols = getCoordinateKey(validateCoordinate(coordinate, true));
        return cells.containsKey(cols);
    }

    /**
     * @param coordinate Map (coordinate) of a cell
     * @return 1. boolean true if a defaultValue is set (non-null) and useDefault is true
     * 2. boolean true if a cell is located at the specified coordinate in the
     * sparse cell map.
     * For RULE axes, the name of the Rule Axis must be bound to a rule name
     * (e.g. the 'name' attribute on the Column expression).
     */
    public boolean containsCell(final Map<String, Object> coordinate, boolean useDefault)
    {
        if (useDefault)
        {
            if (defaultCellValue != null)
            {
                return true;
            }
        }
        Set<Column> cols = getCoordinateKey(validateCoordinate(coordinate, true));
        return cells.containsKey(cols);
    }

    /**
     * @return true if and only if there is a cell stored at the location
     * specified by the Set<Long> coordinate.  If the IDs don't locate a coordinate,
     * no exception is thrown - simply false is returned.
     */
    public boolean containsCellById(final Set<Long> coordinate)
    {
        Set<Column> cols = new HashSet<>();
        getColumnsAndCoordinateFromIds(coordinate, cols, null);
        return cells.containsKey(cols);
    }

    /**
     * Store a value in the cell at the passed in coordinate.
     * @param value A value to store in the NCube cell.
     * @param coordinate Map coordinate used to identify what cell to update.
     * The Map contains keys that are axis names, and values that will
     * locate to the nearest column on the axis.
     * @return the prior cells value.
     */
    public T setCell(final T value, final Map<String, Object> coordinate)
    {
        if (!(value instanceof byte[]) && value != null && value.getClass().isArray())
        {
            throw new IllegalArgumentException("Cannot set a cell to be an array type directly (except byte[]). Instead use GroovyExpression.");
        }
        clearScopeKeyCaches();
        return cells.put(getCoordinateKey(validateCoordinate(coordinate, true)), value);
    }

    /**
     * Set a cell directly into the cell sparse-matrix specified by the passed in
     * Column IDs.
     */
    public T setCellById(final T value, final Set<Long> coordinate)
    {
        if (!(value instanceof byte[]) && value != null && value.getClass().isArray())
        {
            throw new IllegalArgumentException("Cannot set a cell to be an array type directly (except byte[]). Instead use GroovyExpression.");
        }
        clearScopeKeyCaches();
        Set<Column> cols = new HashSet<>();
        getColumnsAndCoordinateFromIds(coordinate, cols, null);
        return cells.put(cols, value);
    }

    /**
     * Clear the require scope caches.  This is required when a cell, column, or axis
     * changes.
     */
    private void clearScopeKeyCaches()
    {
        synchronized(name)
        {
            optionalScopeKeys = null;
        }
    }

    /**
     * Mainly useful for displaying an ncube within an editor.  This will
     * get the actual stored cell, not execute it.  The caller will get
     * CommandCell instances for example, as opposed to the return value
     * of the executed CommandCell.
     */
    public T getCellByIdNoExecute(final Set<Long> coordinate)
    {
        Set<Column> cols = new HashSet<>();
        getColumnsAndCoordinateFromIds(coordinate, cols, null);
        return cells.get(cols);
    }

    /**
     * Fetch the contents of the cell at the location specified by the coordinate argument.
     * Be aware that if you have any rule cubes in the execution path, they can execute
     * more than one cell.  The cell value returned is the value of the last cell executed.
     * @param coordinate Map of String keys to values meant to bind to each axis of the n-cube.
     * @return Cell pinpointed by the input coordinate.
     */
    public T getCell(final Map<String, Object> coordinate)
    {
        return getCell(coordinate, new HashMap<String, Object>());
    }

    /**
     * Fetch the contents of the cell at the location specified by the coordinate argument.
     * Be aware that if you have any rule cubes in the execution path, they can execute
     * more than one cell.  The cell value returned is the value of the last cell executed.
     * Typically, in a rule cube, you are writing to specific keys within the rule cube, and
     * the calling code then accesses the 'output' Map to fetch the values at these specific
     * keys.
     * @param coordinate Map of String keys to values meant to bind to each axis of the n-cube.
     * @param output Map that can be written to by the code within the the n-cubes (for example,
     *               GroovyExpressions.
     * @return Cell pinpointed by the input coordinate.
     */
    public T getCell(final Map<String, Object> coordinate, final Map<String, Object> output)
    {
        final RuleInfo ruleInfo = getRuleInfo(output);
        final List<MapEntry> trace = ruleInfo.getRuleExecutionTrace();
        final Map<String, Object> validCoord = validateCoordinate(coordinate, false);
        Map<String, Object> input = new CaseInsensitiveMap<>(validCoord);
        boolean run = true;
        trace.add(new MapEntry("begin: " + getName(), coordinate));
        long numRulesExec = 0;
        T lastExecutedCellValue = null;

        while (run)
        {
            run = false;
            final Map<String, List<Column>> boundCoordinates = bindCoordinateToAxes(input);
            final String[] axisNames = getAxisNames(boundCoordinates);
            final Map<String, Integer> counters = getCountersPerAxis(boundCoordinates);
            final Set<Column> idCoord = new HashSet<>();
            final Map<Long, Object[]> cachedConditionValues = new HashMap<>();
            final Map<String, Integer> conditionsFiredCountPerAxis = new HashMap<>();
            boolean done = false;

            try
            {
                while (!done)
                {
                    idCoord.clear();
                    Map<String, Object> ruleIds = new LinkedHashMap<>();

                    for (final String axisName : axisNames)
                    {
                        final List<Column> cols = boundCoordinates.get(axisName);
                        final Column boundColumn = cols.get(counters.get(axisName) - 1);
                        final Axis axis = axisList.get(axisName);

                        if (axis.getType() == AxisType.RULE)
                        {
                            Object conditionValue;

                            // Use Object[] to hold cached condition value to distinguish from a condition
                            // that returned null as it's value.
                            Object[] cachedConditionValue = cachedConditionValues.get(boundColumn.id);
                            if (cachedConditionValue == null)
                            {   // Has the condition on the Rule axis been run this execution?  If not, run it and cache it.
                                CommandCell cmd = (CommandCell) boundColumn.getValue();
                                Map<String, Object> ctx = prepareExecutionContext(input, output);

                                // If the cmd == null, then we are looking at a default column on a rule axis.
                                // the conditionValue becomes 'true' for Default column when ruleAxisBindCount = 0
                                conditionValue = cmd == null ? isZero(conditionsFiredCountPerAxis.get(axisName)) : cmd.execute(ctx);
                                cachedConditionValues.put(boundColumn.id, new Object[]{conditionValue});

                                if (isTrue(conditionValue))
                                {   // Rule fired
                                    Integer count = conditionsFiredCountPerAxis.get(axisName);
                                    conditionsFiredCountPerAxis.put(axisName, count == null ? 1 : count + 1);
                                }
                            }
                            else
                            {   // re-use condition on this rule axis (happens when more than one rule axis on an n-cube)
                                conditionValue = cachedConditionValue[0];
                            }

                            // A rule column on a given axis can be accessed more than once (example: A, B, C on
                            // one rule axis, X, Y, Z on another).  This generates coordinate combinations
                            // (AX, AY, AZ, BX, BY, BZ, CX, CY, CZ).  The condition columns must be run only once, on
                            // subsequent access, the cached result of the condition is used.
                            if (isTrue(conditionValue))
                            {
                                bindColumn(idCoord, ruleIds, axis, boundColumn);
                            }
                        }
                        else
                        {
                            bindColumn(idCoord, ruleIds, axis, boundColumn);
                        }
                    }

                    // Step #2 Execute cell and store return value, associating it to the Axes and Columns it bound to
                    if (idCoord.size() == axisNames.length)
                    {   // Conditions on rule axes that do not evaluate to true, do not generate complete coordinates (intentionally skipped)
                        numRulesExec++;
                        MapEntry entry = new MapEntry(ruleIds, null);
                        try
                        {
                            lastExecutedCellValue = getCellById(idCoord, input, output);
                            entry.setValue(lastExecutedCellValue);
                            trace.add(entry);
                        }
                        catch (RuleStop e)
                        {   // Statement threw at RuleStop
                            entry.setValue("[RuleStop]");
                            trace.add(entry);
                            // Mark that RULE_STOP occurred
                            ruleInfo.ruleStopThrown();
                            throw e;
                        }
                        catch(RuleJump e)
                        {   // Statement threw at RuleJump
                            entry.setValue("[RuleJump]");
                            trace.add(entry);
                            throw e;
                        }
                        catch (Exception e)
                        {
                            String msg = e.getMessage();
                            if (StringUtilities.isEmpty(msg))
                            {
                                msg = e.getClass().getName();
                            }
                            entry.setValue("[" + msg + "]");
                            trace.add(entry);
                            throw e;
                        }
                    }

                    // Step #3 increment counters (variable radix increment)
                    done = incrementVariableRadixCount(counters, boundCoordinates, axisNames);
                }
            }
            catch (RuleStop ignored)
            {
                // ends this execution cycle
                ruleInfo.ruleStopThrown();
            }
            catch (RuleJump e)
            {
                input = e.getCoord();
                run = true;
            }
        }

        trace.add(new MapEntry("end: " + getName(), numRulesExec));
        ruleInfo.addToRulesExecuted(numRulesExec);
        output.put("return", lastExecutedCellValue);
        return lastExecutedCellValue;
    }

    /**
     * The lowest level cell fetch.  This method uses the Set<Column> to fetch an
     * exact cell, while maintaining the original input coordinate that the location
     * was derived from (required because a given input coordinate could map to more
     * than one cell).  Once the cell is located, it is executed and the value from
     * the executed cell is returned. In the case of Command Cells, it is the return
     * value of the execution, otherwise the return is the value stored in the cell,
     * and if there is no cell, the defaultCellValue from NCube is returned, if one
     * is set.
     * REQUIRED: The coordinate passed to this method must have already been run
     * through validateCoordinate(), which duplicates the coordinate and ensures the
     * coordinate has at least an entry for each axis.
     */
    T getCellById(final Set<Column> idCoord, final Map<String, Object> coordinate, final Map output)
    {
        // First, get a ThreadLocal copy of an NCube execution stack
        Deque<StackEntry> stackFrame = executionStack.get();
        boolean pushed = false;
        try
        {
            // Form fully qualified cell lookup (NCube name + coordinate)
            // Add fully qualified coordinate to ThreadLocal execution stack
            final StackEntry entry = new StackEntry(name, coordinate);
            stackFrame.push(entry);
            pushed = true;
            final T retVal = executeCellById(idCoord, coordinate, output);
            return retVal;  // split into 2 statements for debugging
        }
        finally
        {	// Unwind stack: always remove if stacked pushed, even if Exception has been thrown
            if (pushed)
            {
                stackFrame.pop();
            }
        }
    }


    /**
     * Execute the referenced cell. If the cell is a value, it will be returned.
     * If the cell is a CommandCell, then it will be executed.  That allows the
     * cell to further access 'this' ncube or other NCubes within the NCubeManager,
     * providing significant power and capability, as it each execution is effectively
     * a new 'Decision' within a decision tree.  Further more, because ncube supports
     * Groovy code within cells, a cell, when executing may perform calculations,
     * programmatic execution within the cell (looping, conditions, modifications),
     * as well as referencing back into 'this' or other ncubes.  The output map passed
     * into this method allows the executing cell to write out information that can be
     * accessed after the execution completes, or even during execution, as a parameter
     * passing.
     * @return T ultimate value reached by executing the contents of this cell.
     * If the passed in coordinate refers to a non-command cell, then the value
     * of that cell is returned, otherwise the command in the cell is executed,
     * resulting in recursion that will ultimately end when a non-command cell
     * is reached.
     */
    private T executeCellById(final Set<Column> idCoord, final Map<String, Object> coord, final Map output)
    {
        // Get internal representation of a coordinate (a Set of Column identifiers.)

        T cellValue = cells.containsKey(idCoord) ? cells.get(idCoord) : defaultCellValue;
        Map<String, Object> ctx = prepareExecutionContext(coord, output);

        try
        {
            if (cellValue instanceof CommandCell)
            {
                return (T) ((CommandCell) cellValue).execute(ctx);
            }
            else
            {
                return cellValue;
            }
        }
        catch (RuleStop | RuleJump e)
        {
            throw e;
        }
        catch (CoordinateNotFoundException e)
        {
            throw new CoordinateNotFoundException("Coordinate not found in NCube '" + name + "'\n" + stackToString(), e);
        }
        catch (Exception e)
        {
            throw new RuntimeException("Error occurred executing CommandCell in NCube '" + name + "'\n" + stackToString(), e);
        }
    }


    /**
     * Prepare the execution context by providing it with references to
     * important items like the input coordinate, output map, stack,
     * this (ncube), and the NCubeManager.
     */
    public Map<String, Object> prepareExecutionContext(final Map<String, Object> coord, final Map output)
    {
        final Map<String, Object> args = new HashMap<>();
        args.put("input", coord);   // Input coordinate is already a duplicate (CaseInsensitiveMap) at this point
        args.put("output", output);
        args.put("ncube", this);
        return args;
    }

    /**
     * Get a Map of column values and corresponding cell values where all axes
     * but one are held to a fixed (single) column, and one axis allows more than
     * one value to match against it.
     * @param coordinate Map - A coordinate where the keys are axis names, and the
     * values are intended to match a column on each axis, with one exception.  One
     * of the axis values in the coordinate input map must be an instanceof a Set.
     * If the set is empty, all columns and cell values for the given axis will be
     * returned in a Map.  If the Set has values in it, then only the columns
     * on the 'wildcard' axis that match the values in the set will be returned (along
     * with the corresponding cell values).
     * @return a Map containing Axis names and values to bind to those axes.  One of the
     * axes must have a Set bound to it.
     */
    public Map<Object, T> getMap(final Map<String, Object> coordinate)
    {
        final Map<String, Object> coord = validateCoordinate(coordinate, false);
        final Axis wildcardAxis = getWildcardAxis(coord);
        final List<Column> columns = getWildcardColumns(wildcardAxis, coord);
        final Map<Object, T> result = new HashMap<>();
        final String axisName = wildcardAxis.getName();

        for (final Column column : columns)
        {
            coord.put(axisName, column.getValueThatMatches());
            result.put(column.getValue(), getCell(coord));
        }

        return result;
    }

    private static void bindColumn(Set<Column> idCoord, Map<String, Object> ruleIds, Axis axis, Column boundColumn)
    {
        idCoord.add(boundColumn);
        ruleIds.put(axis.getName(), boundColumn.getValue() == null ? "null" : boundColumn.getValue().toString());
    }

    /**
     * Get / Create the RuleInfo Map stored at output[NCube.RULE_EXEC_INFO]
     */
    public static RuleInfo getRuleInfo(Map<String, Object> output)
    {
        final RuleInfo ruleInfo;
        if (output.containsKey(NCube.RULE_EXEC_INFO))
        {   // RULE_EXEC_INFO Map already exists, must be a recursive call.
            ruleInfo = (RuleInfo) output.get(NCube.RULE_EXEC_INFO);
        }
        else
        {   // RULE_EXEC_INFO Map does not exist, create it.
            ruleInfo = new RuleInfo();
            output.put(NCube.RULE_EXEC_INFO, ruleInfo);
        }
        return ruleInfo;
    }

    /**
     * Follow the exact same treatment of TRUTH as Groovy
     */
    public static boolean isTrue(Object ruleValue)
    {
        if (ruleValue == null)
        {   // null indicates rule did NOT fire
            return false;
        }

        if (ruleValue instanceof Number)
        {
            boolean isZero = ruleValue.equals((byte)0) ||
                    ruleValue.equals((short)0) ||
                    ruleValue.equals((int)0) ||
                    ruleValue.equals((long)0) ||
                    ruleValue.equals(0.0d) ||
                    ruleValue.equals(0.0f) ||
                    ruleValue.equals(BigInteger.ZERO) ||
                    ruleValue.equals(BigDecimal.ZERO);
            return !isZero;
        }

        if (ruleValue instanceof String)
        {
            return !"".equals(ruleValue);
        }

        if (ruleValue instanceof Map)
        {
            return ((Map)ruleValue).size() > 0;
        }

        if (ruleValue instanceof Collection)
        {
            return ((Collection)ruleValue).size() > 0;
        }

        if (ruleValue instanceof Enumeration)
        {
            return ((Enumeration)ruleValue).hasMoreElements();
        }

        if (ruleValue instanceof Iterator)
        {
            return ((Iterator)ruleValue).hasNext();
        }

        if (ruleValue instanceof Boolean)
        {
            return ruleValue.equals(true);
        }
        return true;
    }

    private static boolean isZero(Integer count)
    {
        return count == null || count == 0;
    }

    /**
     * Bind the input coordinate to each axis.  The reason the column is a List of columns that the coordinate
     * binds to on the axis, is to support RULE axes.  On a regular axis, the coordinate binds
     * to a column (with a binary search or hashMap lookup), however, on a RULE axis, the act
     * of binding to an axis results in a List<Column>.
     * @param coord The passed in input coordinate to bind (or multi-bind) to each axis.
     */
    private Map<String, List<Column>> bindCoordinateToAxes(Map coord)
    {
        Map<String, List<Column>> coordinates = new HashMap<>();
        for (final Map.Entry<String, Axis> entry : axisList.entrySet())
        {
            final String axisNameLowcase = entry.getKey();
            final Axis axis = entry.getValue();
            final Comparable value = (Comparable) coord.get(axisNameLowcase);

            if (axis.getType() == AxisType.RULE)
            {   // For RULE axis, all possible columns must be added (they are tested later during execution)
                coordinates.put(axisNameLowcase, axis.getRuleColumnsStartingAt((String) coord.get(axis.getName())));
            }
            else
            {   // Find the single column that binds to the input coordinate on a regular axis.
                final Column column = axis.findColumn(value);
                if (column == null)
                {
                    throw new CoordinateNotFoundException("Value '" + value + "' not found on axis '" + axis.getName() + "', NCube '" + name + "'");
                }
                List<Column> cols = new ArrayList<>();
                cols.add(column);
                coordinates.put(axisNameLowcase, cols);
            }
        }

        return coordinates;
    }

    private static String[] getAxisNames(final Map<String, List<Column>> bindings)
    {
        return bindings.keySet().toArray(new String[bindings.size()]);
    }

    private static Map<String, Integer> getCountersPerAxis(final Map<String, List<Column>> bindings)
    {
        final Map<String, Integer> counters = new HashMap<>();

        // Set counters to 1
        for (final String axisName : bindings.keySet())
        {
            counters.put(axisName, 1);
        }
        return counters;
    }

    /**
     * Given a Set of Column IDs, which can be less than the number of axes on this n-cube, this method
     * will fill in the cols and coord parameters (if not null), with the fully specified Column set required
     * to uniquely identify the same cell that the passed in Set<Long> identifies.  This method allows the Set<long>
     * to be underspecified (less entries than number of dimensions on n-cube.  In that case, this method will
     * bind the unmapped axes to their 'Default' column.  In an axis does not have a default column and no ID
     * is passed in that would bind to that axis, then an IllegalArgumentException will be thrown.
     * @param cols Set<Column> to be filled in (a Column for every axis will be added to this Set).  This parameter
     * can be null.
     * @param coord Map<String, Object> will be filled in with the coordinate keys/values that will point to the
     * given cell identified by the Set<Long>.  This parameter can be null.
     * @throws IllegalArgumentException if not enough IDs are passed in, or an axis
     * cannot bind to any of the passed in IDs.
     */
    public void getColumnsAndCoordinateFromIds(final Set<Long> coordinate, Set<Column> cols, Map<String, CellInfo> coord)
    {
        // Ensure that the specified coordinate matches a column on each axis
        final Set<String> axisNamesRef = new CaseInsensitiveSet<>();
        final Set<String> allAxes = new CaseInsensitiveSet<>(axisList.keySet());
        final Set<Column> point = new HashSet<>();

        // Bind all Longs to Columns on an axis.  Allow for additional columns to be specified,
        // but not more than one column ID per axis.  Also, too few can be supplied, if and
        // only if, the axes that are not bound too have a Default column (which will be chosen).
        for (final Axis axis : axisList.values())
        {
            final String axisName = axis.getName();
            for (final Long id : coordinate)
            {
                Column column = axis.getColumnById(id);
                if (column != null)
                {
                    if (axisNamesRef.contains(axisName))
                    {
                        throw new IllegalArgumentException("Cannot have more than one column ID per axis, axis '" + axisName + "', NCube '" + name + "'");
                    }

                    axisNamesRef.add(axisName);
                    point.add(column);

                    getColumnProperties(coord, axis, column);
                }
            }
        }

        // Remove the referenced axes from allAxes set.  This leaves axes to be resolved.
        allAxes.removeAll(axisNamesRef);

        // For the unbound axes, bind them to the Default Column (if the axis has one)
        axisNamesRef.clear();   // use Set again, this time to hold unbound axes
        axisNamesRef.addAll(allAxes);

        // allAxes at this point, is the unbound axis (not referenced by an id in input coordinate)
        for (final String axisName : allAxes)
        {
            Axis axis = getAxis(axisName);
            if (axis.hasDefaultColumn())
            {
                Column defCol = axis.getDefaultColumn();
                axisNamesRef.remove(axisName);
                point.add(defCol);

                getColumnProperties(coord, axis, defCol);
            }
        }

        // Add in all input.keyName references from the Groovy code at the given cell (if it is a CommandCell).
        //  This is highly valuable as it will find, for example, input.vehicle from the Groovy code.
        if (coord != null)
        {
            for (String scopeKey : getRequiredScope())
            {
                if (!coord.containsKey(scopeKey))
                {
                    coord.put(scopeKey, null);
                }
            }
        }

        // Pass back the converted Set<Long> as Set<Column>.  Use their passed in input variable 'cols' (if not null)
        if (cols != null)
        {
            cols.clear();
            cols.addAll(point);
        }

        if (!axisNamesRef.isEmpty())
        {
            throw new IllegalArgumentException("Column IDs missing for the axes: " + axisNamesRef + ", NCube '" + name + "'");
        }
    }

    /**
     * Load properties from map
     */
    private static void getColumnProperties(Map<String, CellInfo> map, Axis axis, Column column) {

        if (map == null)
        {
            return;
        }

        if (axis.getType() != AxisType.RULE)
        {
            Object value = column.getValueThatMatches();
            CellInfo info = new CellInfo(value);
            map.put(axis.getName(), info);
        }
    }

    /**
     * Convert an Object to a Map.  This allows an object to then be passed into n-cube as a coordinate.  Of course
     * the returned map can have additional key/value pairs added to it after calling this method, but before calling
     * getCell().
     * @param o Object any Java object to bind to an NCube.
     * @return Map where the fields of the object are the field names from the class, and the associated values are
     * the values associated to the fields on the object.
     */
    public static Map<String, Object> objectToMap(final Object o)
    {
        if (o == null)
        {
            throw new IllegalArgumentException("null passed into objectToMap.  No possible way to convert null into a Map.");
        }

        try
        {
            final Collection<Field> fields = ReflectionUtils.getDeepDeclaredFields(o.getClass());
            final Iterator<Field> i = fields.iterator();
            final Map<String, Object> newCoord = new CaseInsensitiveMap<>();

            while (i.hasNext())
            {
                final Field field = i.next();
                final String fieldName = field.getName();
                final Object fieldValue = field.get(o);
                if (newCoord.containsKey(fieldName))
                {   // This can happen if field name is same between parent and child class (dumb, but possible)
                    newCoord.put(field.getDeclaringClass().getName() + '.' + fieldName, fieldValue);
                }
                else
                {
                    newCoord.put(fieldName, fieldValue);
                }
            }
            return newCoord;
        }
        catch (Exception e)
        {
            throw new RuntimeException("Failed to access field of passed in object.", e);
        }
    }

    private static String stackToString()
    {
        final Deque<StackEntry> stack = executionStack.get();
        final Iterator<StackEntry> i = stack.descendingIterator();
        final StringBuilder s = new StringBuilder();

        while (i.hasNext())
        {
            final StackEntry key = i.next();
            s.append("-> cell:");
            s.append(key.toString());
            if (i.hasNext())
            {
                s.append('\n');
            }
        }

        return s.toString();
    }

    /**
     * Increment the variable radix number passed in.  The number is represented by a Map, where the keys are the
     * digit names (axis names), and the values are the associated values for the number.
     * @return false if more incrementing can be done, otherwise true.
     */
    private static boolean incrementVariableRadixCount(final Map<String, Integer> counters,
                                                       final Map<String, List<Column>> bindings,
                                                       final String[] axisNames)
    {
        int digit = axisNames.length - 1;
        while (true)
        {
            final int count = counters.get(axisNames[digit]);
            final List<Column> cols = bindings.get(axisNames[digit]);

            if (count >= cols.size())
            {   // Reach max value for given dimension (digit)
                if (digit == 0)
                {   // we have reached the max radix for the most significant digit - we are done
                    return true;
                }
                counters.put(axisNames[digit--], 1);
            }
            else
            {
                counters.put(axisNames[digit], count + 1);  // increment counter
                return false;
            }
        }
    }

    /**
     * @param coordinate passed in coordinate for accessing this n-cube
     * @return Axis the axis that has a Set specified for it rather than a non-Set value.
     * The Set associated to the input coordinate field indicates that the caller is
     * matching more than one value against this axis.
     */
    private Axis getWildcardAxis(final Map<String, Object> coordinate)
    {
        int count = 0;
        Axis wildcardAxis = null;

        for (Map.Entry<String, Object> entry : coordinate.entrySet())
        {
            if (entry.getValue() instanceof Set)
            {
                count++;
                wildcardAxis = axisList.get(entry.getKey().toLowerCase());      // intentional case insensitive match
            }
        }

        if (count == 0)
        {
            throw new IllegalArgumentException("No 'Set' value found within input coordinate, NCube '" + name + "'");
        }

        if (count > 1)
        {
            throw new IllegalArgumentException("More than one 'Set' found as value within input coordinate, NCube '" + name + "'");
        }

        return wildcardAxis;
    }

    /**
     * @param coordinate Map containing Axis names as keys, and Comparable's as
     * values.  The coordinate key matches an axis name, and then the column on the
     * axis is found that best matches the input coordinate value.
     * @return a Set key in the form of Column1,Column2,...Column-n where the Columns
     * are the Columns along the axis that match the value associated to the key (axis
     * name) of the passed in input coordinate. The ordering is the order the axes are
     * stored within in NCube.  The returned Set is the 'key' of NCube's cells Map, which
     * maps a coordinate (Set of column pointers) to the cell value.
     */
    private Set<Column> getCoordinateKey(final Map<String, Object> coordinate)
    {
        final Set<Column> key = new HashSet<>();

        for (final Map.Entry<String, Axis> entry : axisList.entrySet())
        {
            final Axis axis = entry.getValue();
            final Object value = coordinate.get(entry.getKey());
            final Column column = axis.findColumn((Comparable) value);
            if (column == null)
            {
                throw new CoordinateNotFoundException("Value '" + value + "' not found on axis '" + axis.getName() + "', NCube '" + name + "'");
            }
            key.add(column);
        }

        return key;
    }

    /**
     * Ensure that the Map coordinate dimensionality satisfies this nCube.
     * This method verifies that all axes are listed by name in the input coordinate.
     * It should be noted that if the input coordinate contains the axis names with
     * exact case match, this method performs much faster.  It must make a second
     * pass through the axis list when the input coordinate axis names do not match
     * the case of the axis.
     * @param coordinate Map input coordinate
     * @param ignoreDeclaredRequiredScope If this is true, then the requiredScopeKeys ncube
     *                                    metaProperty is ignored
     */
    private Map<String, Object> validateCoordinate(final Map<String, Object> coordinate, boolean ignoreDeclaredRequiredScope)
    {
        if (coordinate == null)
        {
            throw new IllegalArgumentException("'null' passed in for coordinate Map, NCube '" + name + "'");
        }

        // Duplicate input coordinate
        final Map<String, Object> copy = new CaseInsensitiveMap<>(coordinate);

        // Ensure required scope is supplied within the input coordinate
        Set<String> requiredScope = getRequiredScope();

        if (ignoreDeclaredRequiredScope)
        {
            requiredScope.removeAll(getDeclaredScopeKeys());
        }

        for (String scopeKey : requiredScope)
        {
            if (!copy.containsKey(scopeKey))
            {
                throw new IllegalArgumentException("Input coordinate with keys: " + coordinate.keySet() +
                        ", does not contain all of the required scope keys: " + requiredScope +
                        ", required for NCube '" + name + "'");
            }
        }

        return copy;
    }

    /**
     * @param coordinate Map containing Axis names as keys, and Comparable's as
     * values.  The coordinate key matches an axis name, and then the column on the
     * axis is found that best matches the input coordinate value. The input coordinate
     * must contain one Set as a value for one of the axes of the NCube.  If empty,
     * then the Set is treated as '*' (star).  If it has 1 or more elements in
     * it, then for each entry in the Set, a column position value is returned.
     *
     * @return a List of all columns that match the values in the Set, or in the
     * case of an empty Set, all columns on the axis.
     */
    private List<Column> getWildcardColumns(final Axis wildcardAxis, final Map<String, Object> coordinate)
    {
        final List<Column> columns = new ArrayList<>();
        final Set<Comparable> wildcardSet = (Set<Comparable>) coordinate.get(wildcardAxis.getName());

        // To support '*', an empty Set is bound to the axis such that all columns are returned.
        if (wildcardSet.isEmpty())
        {
            columns.addAll(wildcardAxis.getColumns());
        }
        else
        {
            // This loop grabs all the columns from the axis which match the values in the Set
            for (final Comparable value : wildcardSet)
            {
                final Column column = wildcardAxis.findColumn(value);
                if (column == null)
                {
                    throw new CoordinateNotFoundException("Value '" + value + "' not found using Set on axis '" + wildcardAxis.getName() + "', NCube '" + name + "'");
                }

                columns.add(column);
            }
        }

        return columns;
    }

    /**
     * @return T the default value that will be returned when a coordinate specifies
     * a cell that has no entry associated to it.  This is a space-saving technique,
     * as the most common cell value can be set as the defaultCellValue, and then the
     * cells that would have had this value can be left empty.
     */
    public T getDefaultCellValue()
    {
        return defaultCellValue;
    }

    /**
     * Set the default cell value for this n-cube.  This is a space-saving technique,
     * as the most common cell value can be set as the defaultCellValue, and then the
     * cells that would have had this value can be left empty.
     * @param defaultCellValue T the default value that will be returned when a coordinate
     * specifies a cell that has no entry associated to it.
     */
    public void setDefaultCellValue(final T defaultCellValue)
    {
        this.defaultCellValue = defaultCellValue;
    }

    /**
     * Clear all cell values.  All axes and columns remain.
     */
    public void clearCells()
    {
        cells.clear();
    }

    /**
     * Add a column to the n-cube
     * @param axisName String name of the Axis to which the column will be added.
     * @param value Comparable that will be the value for the given column.  Cannot be null.
     * @return Column the added Column.
     */
    public Column addColumn(final String axisName, final Comparable value)
    {
        final Axis axis = getAxis(axisName);
        if (axis == null)
        {
            throw new IllegalArgumentException("Could not add column. Axis name '" + axisName + "' was not found on NCube '" + name + "'");
        }
        Column newCol = axis.addColumn(value);
        clearScopeKeyCaches();
        return newCol;
    }

    /**
     * Delete a column from the named axis.  All cells that reference this
     * column will be deleted.
     * @param axisName String name of Axis contains column to be removed.
     * @param value Comparable value used to identify column
     * @return boolean true if deleted, false otherwise
     */
    public boolean deleteColumn(final String axisName, final Comparable value)
    {
        final Axis axis = getAxis(axisName);
        if (axis == null)
        {
            throw new IllegalArgumentException("Could not delete column. Axis name '" + axisName + "' was not found on NCube '" + name + "'");
        }
        clearScopeKeyCaches();
        final Column column = axis.deleteColumn(value);
        if (column == null)
        {
            return false;
        }

        // Remove all cells that reference the deleted column
        final Iterator<Set<Column>> i = cells.keySet().iterator();

        while (i.hasNext())
        {
            final Set<Column> key = i.next();
            // Locate the uniquely identified column, regardless of axis order
            if (key.contains(column))
            {
                i.remove();
            }
        }
        return true;
    }

    /**
     * Move the column indicated by curPos to the newPos along the axis specified by name.
     * Note this only works for an axis in display order.  This method will through an
     * IllegalStateException if you attempt to call it on an axis in Sorted order.  If the
     * columns indicated by curPos or newPos do not exist, an IllegalArgumentException
     * will be thrown.
     */
    public boolean moveColumn(final String axisName, final int curPos, final int newPos)
    {
        final Axis axis = getAxis(axisName);
        if (axis == null)
        {
            throw new IllegalArgumentException("Could not move column. Axis name '" + axisName + "' was not found on NCube '" + name + "'");
        }

        return axis.moveColumn(curPos, newPos);
    }

    /**
     * Change the value of a Column along an axis.
     * @param id long indicates the column to change
     * @param value Comparable new value to set into the column
     */
    public void updateColumn(long id, Comparable value)
    {
        Axis axis = getAxisFromColumnId(id);
        if (axis == null)
        {
            throw new IllegalArgumentException("No column exists with the id " + id + " within NCube '" + name + "'");
        }
        axis.updateColumn(id, value);
    }

    /**
     * Update all of the columns along an axis at once.  Any cell referencing a column that
     * is deleted, will also be deleted from the internal sparse matrix (Map) of cells.
     * @param newCols Axis used only as a Column holder, such the columns within this
     * Axis are in display order as would come in from a UI, for example.
     * @return Set<Long> column ids, indicating which columns were deleted.
     */
    public Set<Long> updateColumns(final Axis newCols)
    {
        if (newCols == null)
        {
            throw new IllegalArgumentException("Cannot pass in null Axis for updating columns, NCube '" + name + "'");
        }
        final String lowAxisName = newCols.getName().toLowerCase();
        if (!axisList.containsKey(lowAxisName))
        {
            throw new IllegalArgumentException("No axis exists with the name: " + newCols.getName() + ", NCube '" + name + "'");
        }

        final Axis axisToUpdate = axisList.get(newCols.getName().toLowerCase());
        final Set<Long> colsToDel = axisToUpdate.updateColumns(newCols);
        Column testColumn = new Column(1, newCols.getNextColId());
        Iterator<Set<Column>> i = cells.keySet().iterator();

        while (i.hasNext())
        {
            Set<Column> cols = i.next();

            for (Long id : colsToDel)
            {
                testColumn.setId(id);
                if (cols.contains(testColumn))
                {   // If cell referenced deleted column, drop the cell
                    i.remove();
                    break;
                }
            }
        }

        return colsToDel;
    }

    /**
     * Given the passed in Column ID, return the axis that contains the column.
     * @param id Long id of a Column on one of the Axes within this n-cube.
     * @return Axis containing the column id, or null if the id does not match
     * any columns.
     */
    public Axis getAxisFromColumnId(long id)
    {
        for (final Axis axis : axisList.values())
        {
            if (axis.idToCol.containsKey(id))
            {
                return axis;
            }
        }
        return null;
    }

    /**
     * @return int total number of cells that are uniquely set (non default)
     * within this NCube.
     */
    public int getNumCells()
    {
        return cells.size();
    }

    /**
     * @return read-only copy of the n-cube cells.
     */
    public Map<Set<Column>, T> getCellMap()
    {
        return Collections.unmodifiableMap(cells);
    }

    /**
     * Retrieve an axis (by name) from this NCube.
     * @param axisName String name of Axis to fetch.
     * @return Axis instance requested by name, or null
     * if it does not exist.
     */
    public Axis getAxis(final String axisName)
    {
        return axisList.get(axisName.toLowerCase());
    }

    /**
     * Add an Axis to this NCube.
     * All cells will be cleared when axis is added.
     * @param axis Axis to add
     */
    public void addAxis(final Axis axis)
    {
        String axisName = axis.getName().toLowerCase();
        if (axisList.containsKey(axisName))
        {
            throw new IllegalArgumentException("An axis with the name '" + axis.getName()
                    + "' already exists on NCube '" + name + "'");
        }

        cells.clear();
        axisList.put(axisName, axis);
        clearScopeKeyCaches();
    }

    public void renameAxis(final String oldName, final String newName)
    {
        if (StringUtilities.isEmpty(oldName) || StringUtilities.isEmpty(newName))
        {
            throw new IllegalArgumentException("Axis name cannot be empty or blank");
        }
        if (getAxis(newName) != null)
        {
            throw new IllegalArgumentException("There is already an axis named '" + oldName + "' on NCube '" + name + "'");
        }
        final Axis axis = getAxis(oldName);
        if (axis == null)
        {
            throw new IllegalArgumentException("Axis '" + oldName + "' not on NCube '" + name + "'");
        }
        axisList.remove(oldName.toLowerCase());
        axis.setName(newName);
        axisList.put(newName.toLowerCase(), axis);
    }

    /**
     * Remove an axis from an NCube.
     * All cells will be cleared when an axis is deleted.
     * @param axisName String name of axis to remove
     * @return boolean true if removed, false otherwise
     */
    public boolean deleteAxis(final String axisName)
    {
        cells.clear();
        clearScopeKeyCaches();
        return axisList.remove(axisName.toLowerCase()) != null;
    }

    public int getNumDimensions()
    {
        return axisList.size();
    }

    public List<Axis> getAxes()
    {
        return new ArrayList<>(axisList.values());
    }

    /**
     * Get the optional scope keys. These are keys that if supplied, might change the returned value, but if not
     * supplied a value is still returned.  For example, an axis that has a Default column is an optional scope.
     * If not value is supplied for that axis, the Default column is chosen.  However, supplying a value for it
     * *may* change the column selected.
     *
     * @return Set of String scope key names that are optional.
     */
    public Set<String> getOptionalScope()
    {
        if (optionalScopeKeys != null)
        {   // Cube name ==> optional scope keys map
            return new CaseInsensitiveSet<>(optionalScopeKeys); // return sorted, modifiable, case-insensitive copy
        }

        synchronized(name)
        {
            if (optionalScopeKeys != null)
            {   // Check again in case more than one thread was waiting for the cached answer to be built.
                return new CaseInsensitiveSet<>(optionalScopeKeys);  // return sorted, modifiable, case-insensitive copy
            }

            optionalScopeKeys = new CaseInsensitiveSet<>();
            final LinkedList<NCube> stack = new LinkedList<>();
            final Set<String> visited = new HashSet<>();
            stack.addFirst(this);

            while (!stack.isEmpty())
            {
                final NCube<?> cube = stack.removeFirst();
                final String cubeName = cube.getName();
                if (visited.contains(cubeName))
                {
                    continue;
                }
                visited.add(cubeName);

                for (final Axis axis : cube.axisList.values())
                {   // Use original axis name (not .toLowerCase() version)
                    if (axis.hasDefaultColumn() || axis.getType() == AxisType.RULE)
                    {
                        optionalScopeKeys.add(axis.getName());
                    }
                }

                // Snag all input.variable references from CommandCells ('variable' is a potential required scope)
                for (String key : getScopeKeysFromCommandCells(cube.cells))
                {
                    optionalScopeKeys.add(key);
                }

                // Snag all input.variable references from Rule axis conditions ('variable' is a potential required scope)
                for (String key : getScopeKeysFromRuleAxes(cube))
                {
                    optionalScopeKeys.add(key);
                }

                // Add all referenced sub-cubes to the stack (locate n-cube references @cube[:], $cube[:],
                // and NCubeManager.getCube('name').  Each of these n-cubes needs to be checked.
                for (final String ncube : getReferencedCubeNames())
                {
                    NCube refCube = NCubeManager.getCube(appId, ncube);
                    if (refCube == null)
                    {
                        throw new IllegalStateException("Attempting to get required scope, but NCube '" + ncube + "' is not loaded into NCubeManager.  Use NCubeManager.loadCubes() at application start.");
                    }
                    stack.addFirst(refCube);
                }
            }

            optionalScopeKeys.removeAll(getRequiredScope());
            Set<String> sort = new TreeSet<>(optionalScopeKeys);
            optionalScopeKeys.clear();
            optionalScopeKeys.addAll(sort);
            return new CaseInsensitiveSet<>(optionalScopeKeys); // return sorted, modifiable, case-insensitive copy
        }
    }

    /**
     * Determine the required 'scope' needed to access all cells within this
     * NCube.  Effectively, you are determining how many axis names (keys in
     * a Map coordinate) are required to be able to access any cell within this
     * NCube.  Keep in mind, that CommandCells allow this NCube to reference
     * other NCubes and therefore the referenced NCubes must be checked as
     * well.  This code will not get stuck in an infinite loop if one cube
     * has cells that reference another cube, and it has cells that reference
     * back (it has cycle detection).
     * @return Set<String> names of axes that will need to be in an input coordinate
     * in order to use all cells within this NCube.
     */
    public Set<String> getRequiredScope()
    {
        final Set<String> requiredScope = new CaseInsensitiveSet<>();

        for (final Axis axis : axisList.values())
        {   // Use original axis name (not .toLowerCase() version)
            if (!axis.hasDefaultColumn() && !(axis.getType() == AxisType.RULE))
            {
                requiredScope.add(axis.getName());
            }
        }

        requiredScope.addAll(getDeclaredScopeKeys());
        return requiredScope;
    }

    Set<String> getDeclaredScopeKeys()
    {
        if (declaredScopeKeys != null)
        {
            return declaredScopeKeys;
        }
        // Declared scope keys have not yet been set.
        synchronized(name)
        {
            if (declaredScopeKeys != null)
            {   // Double-check (blocked threads should not rebuild...only do for first thread allowed thru)
                return declaredScopeKeys;
            }
            List declaredRequiredScope = (List) extractMetaPropertyValue(getMetaProperty("requiredScopeKeys"));
            declaredScopeKeys = declaredRequiredScope == null ? new CaseInsensitiveSet<>() : new CaseInsensitiveSet(declaredRequiredScope);
        }
        return declaredScopeKeys;
    }

    /**
     * @return a Set of Strings, where each String is the name of a scope key (input.variable, where 'variable'
     * is a required scope) located within the n-cube cells, inside CommandCells.
     */
    private static Set<String> getScopeKeysFromCommandCells(Map<Set<Column>, ?> cubeCells)
    {
        Set<String> scopeKeys = new CaseInsensitiveSet<>();

        for (Object cell : cubeCells.values())
        {
            if (cell instanceof CommandCell)
            {
                CommandCell cmd = (CommandCell) cell;
                cmd.getScopeKeys(scopeKeys);
            }
        }

        return scopeKeys;
    }

    /**
     * Find all occurrences of 'input.variable' within conditions on
     * a Rule axis.  Add 'variable' as required scope key.
     * @param ncube NCube to search
     * @return Set<String> of required scope (coordinate) keys.
     */
    private static Set<String> getScopeKeysFromRuleAxes(NCube<?> ncube)
    {
        Set<String> scopeKeys = new CaseInsensitiveSet<>();

        for (Axis axis : ncube.getAxes())
        {
            if (axis.getType() == AxisType.RULE)
            {
                for (Column column : axis.getColumnsWithoutDefault())
                {
                    CommandCell cmd = (CommandCell) column.getValue();
                    Matcher m = Regexes.inputVar.matcher(cmd.getCmd());
                    while (m.find())
                    {
                        scopeKeys.add(m.group(2));
                    }
                }
            }
        }

        return scopeKeys;
    }

    /**
     * @return Set<String> names of all referenced cubes within this
     * specific NCube.  It is not recursive.
     */
    Set<String> getReferencedCubeNames()
    {
        final Set<String> cubeNames = new LinkedHashSet<>();

        for (final Object cell : cells.values())
        {
            if (cell instanceof CommandCell)
            {
                final CommandCell cmdCell = (CommandCell) cell;
                cmdCell.getCubeNamesFromCommandText(cubeNames);
            }
        }

        for (Axis axis : getAxes())
        {
            if (axis.getType() == AxisType.RULE)
            {
                for (Column column : axis.getColumnsWithoutDefault())
                {
                    CommandCell cmd = (CommandCell) column.getValue();
                    cmd.getCubeNamesFromCommandText(cubeNames);
                }
            }
        }
        return cubeNames;
    }

    /**
     * Use this API to generate an HTML view of this NCube.
     * @param headers String list of axis names to place at top.  If more than one is listed, the first axis encountered that
     * matches one of the passed in headers, will be the axis chosen to be displayed at the top.
     * @return String containing an HTML view of this NCube.
     */
    public String toHtml(String ... headers)
    {
        return new HtmlFormatter(headers).format(this);
    }

    public String toFormattedJson()
    {
        return new JsonFormatter().format(this);
    }

    public String toString()
    {
        return toFormattedJson();
    }

    // ----------------------------
    // Overall cube management APIs
    // ----------------------------

    /**
     * Use this API to create NCubes from a simple JSON format.
     *
     * @param json Simple JSON format
     * @return NCube instance created from the passed in JSON.  It is
     * not added to the static list of NCubes.  If you want that, call
     * addCube() after creating the NCube with this API.
     */
    public static NCube<?> fromSimpleJson(final String json)
    {
        try
        {
            Map<Object, Long> userIdToUniqueId = new CaseInsensitiveMap<>();
            Map<String, Object> jsonNCube = JsonReader.jsonToMaps(json);
            String cubeName = getString(jsonNCube, "ncube");  // new cubes always have ncube as they key in JSON storage
            if (StringUtilities.isEmpty(cubeName))
            {
                throw new IllegalArgumentException("JSON format must have a root 'ncube' field containing the String name of the NCube.");
            }
            NCube ncube = new NCube(cubeName);
            ncube.metaProps = new CaseInsensitiveMap();
            ncube.metaProps.putAll(jsonNCube);
            ncube.metaProps.remove("ncube");
            ncube.metaProps.remove("defaultCellValue");
            ncube.metaProps.remove("defaultCellValueType");
            ncube.metaProps.remove("ruleMode");
            ncube.metaProps.remove("axes");
            ncube.metaProps.remove("cells");
            ncube.metaProps.remove("ruleMode");
            String storedSha1 = (String) ncube.metaProps.get("sha1");
            ncube.metaProps.remove("sha1");
            if (ncube.metaProps.size() < 1)
            {   // No additional props, don't even waste space for meta properties Map.
                ncube.metaProps = null;
            }
            else
            {
                loadMetaProperties(ncube.metaProps);
            }

            String defType = (String) jsonNCube.get("defaultCellValueType");
            ncube.defaultCellValue = CellInfo.parseJsonValue(jsonNCube.get("defaultCellValue"), null, defType, false);

            if (!(jsonNCube.get("axes") instanceof JsonObject))
            {
                throw new IllegalArgumentException("Must specify a list of axes for the ncube, under the key 'axes' as [{axis 1}, {axis 2}, ... {axis n}].");
            }

            JsonObject axes = (JsonObject) jsonNCube.get("axes");
            Object[] items = axes.getArray();

            if (ArrayUtilities.isEmpty(items))
            {
                throw new IllegalArgumentException("Must be at least one axis defined in the JSON format.");
            }
            long idBase = 1;
            // Read axes
            for (Object item : items)
            {
                Map<String, Object> jsonAxis = (Map) item;
                String name = getString(jsonAxis, "name");
                AxisType type = AxisType.valueOf(getString(jsonAxis, "type"));
                boolean hasDefault = getBoolean(jsonAxis, "hasDefault");
                AxisValueType valueType = AxisValueType.valueOf(getString(jsonAxis, "valueType"));
                final int preferredOrder = getLong(jsonAxis, "preferredOrder").intValue();
                Axis axis = new Axis(name, type, valueType, hasDefault, preferredOrder, idBase++);
                ncube.addAxis(axis);
                axis.metaProps = new CaseInsensitiveMap<>();
                axis.metaProps.putAll(jsonAxis);

                axis.metaProps.remove("name");
                axis.metaProps.remove("type");
                axis.metaProps.remove("hasDefault");
                axis.metaProps.remove("valueType");
                axis.metaProps.remove("preferredOrder");
                axis.metaProps.remove("multiMatch");
                axis.metaProps.remove("columns");

                if (axis.metaProps.size() < 1)
                {
                    axis.metaProps = null;
                }
                else
                {
                    loadMetaProperties(axis.metaProps);
                }

                if (!(jsonAxis.get("columns") instanceof JsonObject))
                {
                    throw new IllegalArgumentException("'columns' must be specified, axis '" + name + "', NCube '" + cubeName + "'");
                }
                JsonObject colMap = (JsonObject) jsonAxis.get("columns");

                if (!colMap.isArray())
                {
                    throw new IllegalArgumentException("'columns' must be an array, axis '" + name + "', NCube '" + cubeName + "'");
                }

                // Read columns
                Object[] cols = colMap.getArray();
                for (Object col : cols)
                {
                    Map<String, Object> jsonColumn = (Map) col;
                    Object value = jsonColumn.get("value");
                    String url = (String)jsonColumn.get("url");
                    String colType = (String) jsonColumn.get("type");
                    Object id = jsonColumn.get("id");

                    if (value == null)
                    {
                        if (id == null)
                        {
                            throw new IllegalArgumentException("Missing 'value' field on column or it is null, axis '" + name + "', NCube '" + cubeName + "'");
                        }
                        else
                        {   // Allows you to skip setting both id and value to the same value.
                            value = id;
                        }
                    }

                    boolean cache = false;

                    if (jsonColumn.containsKey("cache"))
                    {
                        if (jsonColumn.get("cache") instanceof Boolean)
                        {
                            cache = (Boolean) jsonColumn.get("cache");
                        }
                        else if (jsonColumn.get("cache") instanceof String)
                        {   // Allow setting it as a String too
                            cache = "true".equalsIgnoreCase((String)jsonColumn.get("cache"));
                        }
                        else
                        {
                            throw new IllegalArgumentException("'cache' parameter must be set to 'true' or 'false', or not used (defaults to 'true')");
                        }
                    }

                    Column colAdded;

                    if (type == AxisType.DISCRETE || type == AxisType.NEAREST)
                    {
                        colAdded = ncube.addColumn(axis.getName(), (Comparable) CellInfo.parseJsonValue(value, null, colType, false));
                    }
                    else if (type == AxisType.RANGE)
                    {
                        Object[] rangeItems = ((JsonObject)value).getArray();
                        if (rangeItems.length != 2)
                        {
                            throw new IllegalArgumentException("Range must have exactly two items, axis '" + name +"', NCube '" + cubeName + "'");
                        }
                        Comparable low = (Comparable) CellInfo.parseJsonValue(rangeItems[0], null, colType, false);
                        Comparable high = (Comparable) CellInfo.parseJsonValue(rangeItems[1], null, colType, false);
                        colAdded = ncube.addColumn(axis.getName(), new Range(low, high));
                    }
                    else if (type == AxisType.SET)
                    {
                        Object[] rangeItems = ((JsonObject)value).getArray();
                        RangeSet rangeSet = new RangeSet();
                        for (Object pt : rangeItems)
                        {
                            if (pt instanceof Object[])
                            {
                                Object[] rangeValues = (Object[]) pt;
                                if (rangeValues.length != 2)
                                {
                                    throw new IllegalArgumentException("Set Ranges must have two values only, range length: " + rangeValues.length + ", axis '" + name + "', NCube '" + cubeName +"'");
                                }
                                Comparable low = (Comparable) CellInfo.parseJsonValue(rangeValues[0], null, colType, false);
                                Comparable high = (Comparable) CellInfo.parseJsonValue(rangeValues[1], null, colType, false);
                                Range range = new Range(low, high);
                                rangeSet.add(range);
                            }
                            else
                            {
                                rangeSet.add((Comparable)CellInfo.parseJsonValue(pt, null, colType, false));
                            }
                        }
                        colAdded = ncube.addColumn(axis.getName(), rangeSet);
                    }
                    else if (type == AxisType.RULE)
                    {
                        Object cmd = CellInfo.parseJsonValue(value, url, colType, cache);
                        if (!(cmd instanceof CommandCell))
                        {
                            throw new IllegalArgumentException("Column values on a RULE axis must be of type CommandCell, axis '" + name + "', NCube '" + cubeName + "'");
                        }
                        colAdded = ncube.addColumn(axis.getName(), (CommandCell)cmd);
                    }
                    else
                    {
                        throw new IllegalArgumentException("Unsupported Axis Type '" + type + "' for simple JSON input, axis '" + name + "', NCube '" + cubeName + "'");
                    }

                    if (id != null)
                    {
                        long sysId = colAdded.getId();
                        userIdToUniqueId.put(id, sysId);
                    }

                    colAdded.metaProps = new CaseInsensitiveMap<>();
                    colAdded.metaProps.putAll(jsonColumn);
                    colAdded.metaProps.remove("id");
                    colAdded.metaProps.remove("value");
                    colAdded.metaProps.remove("type");
                    colAdded.metaProps.remove("url");
                    colAdded.metaProps.remove("cache");

                    if (colAdded.metaProps.size() < 1)
                    {
                        colAdded.metaProps = null;
                    }
                    else
                    {
                        loadMetaProperties(colAdded.metaProps);
                    }
                }
            }

            // Read cells
            if (!(jsonNCube.get("cells") instanceof JsonObject))
            {
                throw new IllegalArgumentException("Must specify the 'cells' portion.  It can be empty but must be specified, NCube '" + cubeName + "'");
            }

            JsonObject cellMap = (JsonObject) jsonNCube.get("cells");

            if (!cellMap.isArray())
            {
                throw new IllegalArgumentException("'cells' must be an []. It can be empty but must be specified, NCube '" + cubeName + "'");
            }

            Object[] cells = cellMap.getArray();
            for (Object cell : cells)
            {
                JsonObject cMap = (JsonObject) cell;
                Object ids = cMap.get("id");
                String type = (String) cMap.get("type");
                String url = (String) cMap.get("url");
                boolean cache = false;

                if (cMap.containsKey("cache"))
                {
                    if (cMap.get("cache") instanceof Boolean)
                    {
                        cache = (Boolean) cMap.get("cache");
                    }
                    else if (cMap.get("cache") instanceof String)
                    {   // Allow setting it as a String too
                        cache = "true".equalsIgnoreCase((String)cMap.get("cache"));
                    }
                    else
                    {
                        throw new IllegalArgumentException("'cache' parameter must be set to 'true' or 'false', or not used (defaults to 'true')");
                    }
                }

                Object v = CellInfo.parseJsonValue(cMap.get("value"), url, type, cache);

                if (ids instanceof JsonObject)
                {   // If specified as ID array, build coordinate that way
                    Set<Long> colIds = new HashSet<>();
                    for (Object id : ((JsonObject)ids).getArray())
                    {
                        if (!userIdToUniqueId.containsKey(id))
                        {
                            throw new IllegalArgumentException("ID specified in cell does not match an ID in the columns, id: " + id);
                        }
                        colIds.add(userIdToUniqueId.get(id));
                    }
                    ncube.setCellById(v, colIds);
                }
                else
                {
                    // TODO: Drop support for specifying columns this way
                    // specified as key-values along each axis
                    if (!(cMap.get("key") instanceof JsonObject))
                    {
                        throw new IllegalArgumentException("'key' must be a JSON object {}, NCube '" + cubeName + "'");
                    }

                    JsonObject<String, Object> keys = (JsonObject<String, Object>) cMap.get("key");
                    for (Map.Entry<String, Object> entry : keys.entrySet())
                    {
                        keys.put(entry.getKey(), CellInfo.parseJsonValue(entry.getValue(), null, null, false));
                    }
                    ncube.setCell(v, keys);
                }
            }

            String calcSha1 = ncube.sha1();
            if (StringUtilities.hasContent(storedSha1))
            {
                if (!calcSha1.equals(storedSha1))
                {
                    // TODO: Add back after demo
//                    throw new IllegalStateException("The json file was edited directly and no longer matches the stored SHA1, n-cube: " + ncube.getName());
                }
            }
            ncube.setMetaProperty("sha1", calcSha1);
            return ncube;
        }
        catch (Exception e)
        {
            throw new RuntimeException("Error reading NCube from passed in JSON", e);
        }
    }

    private static void loadMetaProperties(Map props)
    {
        List<MapEntry> entriesToUpdate = new ArrayList<>();
        for (Map.Entry<String, Object> entry : (Iterable<Map.Entry<String, Object>>) props.entrySet())
        {
            if (entry.getValue() instanceof JsonObject)
            {
                JsonObject map = (JsonObject) entry.getValue();
                Boolean cache = (Boolean) map.get("cache");
                Object value = CellInfo.parseJsonValue(map.get("value"), (String) map.get("url"), (String) map.get("type"), cache == null ? false : cache);
                entriesToUpdate.add(new MapEntry(entry.getKey(), value));
            }
        }

        for (MapEntry entry : entriesToUpdate)
        {
            props.put(entry.getKey(), entry.getValue());
        }
    }

    static String getString(Map obj, String key)
    {
        Object val = obj.get(key);
        if (val instanceof String)
        {
            return (String) val;
        }
        String clazz = val == null ? "null" : val.getClass().getName();
        throw new IllegalArgumentException("Expected 'String' for key '" + key + "' but instead found: " + clazz);
    }

    static Long getLong(Map obj, String key)
    {
        Object val = obj.get(key);
        if (val instanceof Long)
        {
            return (Long) val;
        }
        String clazz = val == null ? "null" : val.getClass().getName();
        throw new IllegalArgumentException("Expected 'Long' for key '" + key + "' but instead found: " + clazz);
    }

    static Boolean getBoolean(Map obj, String key)
    {
        Object val = obj.get(key);
        if (val instanceof Boolean)
        {
            return (Boolean) val;
        }
        if (val == null)
        {
            return false;
        }
        String clazz = val.getClass().getName();
        throw new IllegalArgumentException("Expected 'Boolean' for key '" + key + "' but instead found: " + clazz);
    }

    /**
     * @return List of coordinates that will resolve to each cell within the n-cube.
     */
    public List<NCubeTest> generateNCubeTests()
    {
        List<NCubeTest> coordinates = new ArrayList<>();
        Set<Long> colIds = new HashSet<>();
        int i=1;
        for (Set<Column> pt : cells.keySet())
        {
            colIds.clear();
            for (Column col : pt)
            {
                colIds.add(col.id);
            }
            Map<String, CellInfo> coord = new CaseInsensitiveMap<>();
            Set<Column> cols = new CaseInsensitiveSet<>();
            getColumnsAndCoordinateFromIds(colIds, cols, coord);

            String testName = String.format("test-%03d", i);
            CellInfo[] result = {new CellInfo("exp", "output.return", false, false)};
            coordinates.add(new NCubeTest(testName, convertCoordToList(coord), result));
            i++;
        }

        return coordinates;
    }

    private static StringValuePair<CellInfo>[] convertCoordToList(Map<String, CellInfo> coord)
    {
        int size = coord == null ? 0 : coord.size();
        //List<StringValuePair<CellInfo>> list = new ArrayList<StringValuePair<CellInfo>>(size);
        StringValuePair<CellInfo>[] list = new StringValuePair[size];
        if (size == 0) {
            return list;
        }
        int i=0;
        for (Map.Entry<String, CellInfo> entry : coord.entrySet()) {
            list[i++] = (new StringValuePair(entry.getKey(), entry.getValue()));
        }
        return list;
    }

    /**
     * Create an equivalent n-cube as 'this'.
     */
    public NCube duplicate(String newName)
    {
        NCube copyCube = new NCube(newName);
        copyCube.setDefaultCellValue(defaultCellValue);
        Map<String, Object> metaProperties = getMetaProperties();
        for (Map.Entry<String, Object> entry : metaProperties.entrySet())
        {
            copyCube.setMetaProperty(entry.getKey(), entry.getValue());
        }

        Map<Long, Column> origToNewColumn = new HashMap<>();

        for (Axis axis : axisList.values())
        {
            Axis copyAxis = new Axis(axis.getName(), axis.getType(), axis.getValueType(), axis.hasDefaultColumn(), axis.getColumnOrder(), axis.id);
            metaProperties = axis.getMetaProperties();
            for (Map.Entry<String, Object> entry : metaProperties.entrySet())
            {
                copyAxis.setMetaProperty(entry.getKey(), entry.getValue());
            }
            for (Column column : axis.getColumns())
            {
                Column copyCol = column.isDefault() ? copyAxis.getDefaultColumn() : copyAxis.addColumn(column.getValue());
                metaProperties = column.getMetaProperties();
                for (Map.Entry<String, Object> entry : metaProperties.entrySet())
                {
                    copyCol.setMetaProperty(entry.getKey(), entry.getValue());
                }

                copyCol.setId(column.id);
                origToNewColumn.put(column.id, copyCol);
            }
            copyCube.addAxis(copyAxis);
        }

        for (Map.Entry<Set<Column>, T> entry : cells.entrySet())
        {
            Set<Column> copyKey = new HashSet<>();
            for (Column column : entry.getKey())
            {
                copyKey.add(origToNewColumn.get(column.id));
            }
            copyCube.cells.put(copyKey, entry.getValue());
        }

        return copyCube;
    }

    public boolean equals(Object other)
    {
        if (!(other instanceof NCube))
        {
            return false;
        }

        NCube that = (NCube) other;
        if (!name.equals(that.name))
        {
            return false;
        }

        if (defaultCellValue == null)
        {
            if (that.defaultCellValue != null)
            {
                return false;
            }
        }
        else
        {
            if (!defaultCellValue.equals(that.defaultCellValue))
            {
                return false;
            }
        }

        if (metaProps != null && metaProps.size() == 0)
        {
            metaProps = null;
        }
        if (that.metaProps != null && that.metaProps.size() == 0)
        {
            that.metaProps = null;
        }
        if (!DeepEquals.deepEquals(metaProps, that.metaProps))
        {
            return false;
        }

        if (axisList.size() != that.axisList.size())
        {
            return false;
        }

        Map<Column, Column> idMap = new HashMap<>();

        for (Map.Entry<String, Axis> entry : axisList.entrySet())
        {
            if (!that.axisList.containsKey(entry.getKey()))
            {
                return false;
            }

            Axis thisAxis = entry.getValue();
            Axis thatAxis = (Axis) that.axisList.get(entry.getKey());
            if (!thisAxis.getName().equalsIgnoreCase(thatAxis.getName()))
            {
                return false;
            }

            if (!DeepEquals.deepEquals(thisAxis.metaProps, thatAxis.metaProps))
            {
                return false;
            }

            if (thisAxis.getColumnOrder() != thatAxis.getColumnOrder())
            {
                return false;
            }

            if (thisAxis.getType() != thatAxis.getType())
            {
                return false;
            }

            if (thisAxis.getValueType() != thatAxis.getValueType())
            {
                return false;
            }

            if (thisAxis.getColumns().size() != thatAxis.getColumns().size())
            {
                return false;
            }

            if (thisAxis.hasDefaultColumn() != thatAxis.hasDefaultColumn())
            {
                return false;
            }

            Iterator<Column> iThisCol = thisAxis.getColumns().iterator();
            Iterator<Column> iThatCol = thatAxis.getColumns().iterator();
            while (iThisCol.hasNext())
            {
                Column thisCol = iThisCol.next();
                Column thatCol = iThatCol.next();

                if (thisCol.getValue() == null)
                {
                    if (thatCol.getValue() != null)
                    {
                        return false;
                    }
                }
                else if (!thisCol.getValue().equals(thatCol.getValue()))
                {
                    return false;
                }

                if (!DeepEquals.deepEquals(thisCol.metaProps, thatCol.metaProps))
                {
                    return false;
                }

                idMap.put(thisCol, thatCol);
            }
        }

        if (cells.size() != that.cells.size())
        {
            return false;
        }

        for (Map.Entry<Set<Column>, T> entry : cells.entrySet())
        {
            Set<Column> cellKey = entry.getKey();
            T value = entry.getValue();
            Set<Column> thatCellKey = new HashSet<>();

            for (Column column : cellKey)
            {
                thatCellKey.add(idMap.get(column));
            }

            Object thatCellValue = that.cells.get(thatCellKey);
            if (!DeepEquals.deepEquals(value, thatCellValue))
            {
                return false;
            }
        }

        return true;
    }

    public int hashCode()
    {
        return name.hashCode();
    }

    /**
     * @return SHA1 value for this n-cube.  The value is durable in that Axis order and
     * cell order do not affect the SHA1 value.
     */
    public String sha1()
    {
        byte sep = 0;
        MessageDigest sha1 = EncryptionUtilities.getSHA1Digest();
        sha1.update(name.getBytes());
        sha1.update(sep);
        sha1.update(defaultCellValue == null ? "null".getBytes() : toJson(defaultCellValue).getBytes());
        sha1.update(sep);
        if (metaProps != null && metaProps.size() > 0)
        {
            String storedSha1 = (String) metaProps.remove("sha1");
            sha1.update(toJson(getMetaProperties()).getBytes());
            sha1.update(sep);
            if (StringUtilities.hasContent(storedSha1))
            {
                metaProps.put("sha1", storedSha1);
            }
        }
        sha1.update(sep);
        // Need deterministic ordering (sorted by Axis name will do that)
        Map<String, Axis> sortedAxes = new TreeMap<>(axisList);
        final Map<String, List<Column>> allCoordinates = new LinkedHashMap<>();

        for (Map.Entry<String, Axis> entry : sortedAxes.entrySet())
        {
            Axis axis = entry.getValue();
            allCoordinates.put(axis.getName(), axis.columns);
            sha1.update(axis.getName().getBytes());
            sha1.update(sep);
            sha1.update(String.valueOf(axis.getColumnOrder()).getBytes());
            sha1.update(sep);
            sha1.update(axis.getType().name().getBytes());
            sha1.update(sep);
            sha1.update(axis.getValueType().name().getBytes());
            sha1.update(sep);
            sha1.update(axis.hasDefaultColumn() ? "t".getBytes() : "f".getBytes());
            sha1.update(sep);
            if (!MapUtilities.isEmpty(axis.metaProps))
            {
                sha1.update(toJson(axis.getMetaProperties()).getBytes());
                sha1.update(sep);
            }
            for (Column column : axis.getColumnsWithoutDefault())
            {
                sha1.update(column.getValue().toString().getBytes());
                sha1.update(sep);
                if (!MapUtilities.isEmpty(column.metaProps))
                {
                    sha1.update(toJson(column.getMetaProperties()).getBytes());
                    sha1.update(sep);
                }
            }
        }

        // Need deterministic ordering of cells by walking the n-dim matrix in sorted axis order,
        // accessing each coordinate thru that ordering, rather than their order in the 'cells' Map.
        final Map<String, Integer> counters = getCountersPerAxis(allCoordinates);
        final String[] axisNames = getAxisNames(allCoordinates);
        final Set<Column> idCoord = new HashSet<>();
        boolean done = false;

        while (!done)
        {
            for (final String axisName : axisNames)
            {
                final List<Column> cols = allCoordinates.get(axisName);
                if (cols.size() > 0)
                {
                    final Column boundColumn = cols.get(counters.get(axisName) - 1);
                    idCoord.add(boundColumn);
                }
            }

            if (cells.containsKey(idCoord))
            {
                Object val = cells.get(idCoord);
                if (val != null && val.getClass().isArray())
                {
                    sha1.update(toJson(val).getBytes());
                }
                else
                {
                    sha1.update(val == null ? "null".getBytes() : val.toString().getBytes());
                }

                sha1.update(sep);
            }

            idCoord.clear();
            done = incrementVariableRadixCount(counters, allCoordinates, axisNames);
        }

        return StringUtilities.encode(sha1.digest());
    }

    private static String toJson(Object o)
    {
        if (o == null)
        {
            return "null";
        }
        try
        {
            return JsonWriter.objectToJson(o);
        }
        catch (IOException e)
        {
            throw new IllegalStateException("Unable to convert value to JSON: " + o.toString());
        }
    }

    public static void validateCubeName(String cubeName)
    {
        if (StringUtilities.isEmpty(cubeName))
        {
            throw new IllegalArgumentException("n-cube name cannot be null or empty");
        }

        Matcher m = Regexes.validCubeName.matcher(cubeName);
        if (m.find())
        {
            if (cubeName.equals(m.group(0)))
            {
                return;
            }
        }
        throw new IllegalArgumentException("Invalid n-cube name: '" + cubeName + "'. Name can only contain a-z, A-Z, 0-9, :, ., _, -, #, and |");
    }
}
