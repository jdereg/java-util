package com.cedarsoftware.ncube;

import com.cedarsoftware.ncube.exception.*;
import com.cedarsoftware.ncube.formatters.*;
import com.cedarsoftware.util.*;
import com.cedarsoftware.util.io.*;
import groovy.util.*;

import java.lang.reflect.*;
import java.math.*;
import java.net.*;
import java.security.*;
import java.util.*;
import java.util.regex.*;

/**
 * Implements an n-cube.  This is a hyper (n-dimensional) cube
 * of cells, made up of 'n' number of axes.  Each Axis is composed
 * of Columns that denote discrete nodes along an axis.  Use NCubeManager
 * to manage a list of NCubes.  Documentation on Github.
 *
 * @author John DeRegnaucourt (jdereg@gmail.com)
 *         <br>
 *         Copyright (c) Cedar Software LLC
 *         <br><br>
 *         Licensed under the Apache License, Version 2.0 (the "License");
 *         you may not use this file except in compliance with the License.
 *         You may obtain a copy of the License at
 *         <br><br>
 *         http://www.apache.org/licenses/LICENSE-2.0
 *         <br><br>
 *         Unless required by applicable law or agreed to in writing, software
 *         distributed under the License is distributed on an "AS IS" BASIS,
 *         WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *         See the License for the specific language governing permissions and
 *         limitations under the License.
 */
public class NCube<T>
{
    private String name;
    private String sha1;
    private final Map<String, Axis> axisList = new CaseInsensitiveMap<>();
    final Map<Collection<Column>, T> cells = new LinkedHashMap<>();
    private T defaultCellValue;
    private volatile Set<String> optionalScopeKeys = null;
    private volatile Set<String> declaredScopeKeys = null;
    public static final String validCubeNameChars = "0-9a-zA-Z:._-";
    public static final String RULE_EXEC_INFO = "_rule";
    private final Map<String, Advice> advices = new LinkedHashMap<>();
    private Map<String, Object> metaProps = new CaseInsensitiveMap<>();
    //  Sets up the defaultApplicationId for cubes loaded in from disk.
    private transient ApplicationID appId = ApplicationID.testAppId;
    private static final ThreadLocal<Deque<StackEntry>> executionStack = new ThreadLocal<Deque<StackEntry>>()
    {
        public Deque<StackEntry> initialValue()
        {
            return new ArrayDeque<>();
        }
    };

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
     * Fetch n-cube meta properties (SHA1, HEAD_SHA1, and CHANGE_TYPE are not meta-properties.
     * Use their respective accessor functions to obtain those).
     * @return Map (case insensitive keys) containing meta (additional) properties for the n-cube.
     * Modifications to this Map do not modify the actual meta properties of n-cube.  To do that,
     * you need to use setMetaProperty(), addMetaProperty(), or remoteMetaProperty()
     */
    public Map<String, Object> getMetaProperties()
    {
        return Collections.unmodifiableMap(metaProps);
    }

    /**
     * Fetch the value associated to the passed in Key from the MetaProperties (if any exist).  If
     * none exist, null is returned.
     */
    public Object getMetaProperty(String key)
    {
        return metaProps.get(key);
    }

    /**
     * If a meta property value is fetched from an Axis or a Column, the value should be extracted
     * using this API, so as to allow executable values to be retrieved.
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
        clearSha1();
        return metaProps.put(key, value);
    }

    /**
     * Remove a meta-property entry
     */
    public Object removeMetaProperty(String key)
    {
        Object prop =  metaProps.remove(key);
        clearSha1();
        return prop;
    }

    /**
     * Add a Map of meta properties all at once.
     * @param allAtOnce Map of meta properties to add
     */
    public void addMetaProperties(Map<String, Object> allAtOnce)
    {
        for (Map.Entry<String, Object> entry : allAtOnce.entrySet())
        {
            final String key = entry.getKey();
            metaProps.put(key, entry.getValue());
        }
        clearSha1();
    }

    /**
     * Remove all meta properties associated to this n-cube.
     */
    public void clearMetaProperties()
    {
        metaProps.clear();
        clearSha1();
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
     * This should only be called from NCubeManager when loading the cube from a database
     * It is mainly to prevent an unnecessary sha1 calculation after being loaded from a
     * db that already knows the sha1.
     * @param sha1
     */
    void setSha1(String sha1)
    {
        this.sha1 = sha1;
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
        clearSha1();
        return cells.remove(getCoordinateKey(validateCoordinate(coordinate, true)));
    }

    /**
     * Clear a cell directly from the cell sparse-matrix specified by the passed in Column
     * IDs. After this call, containsCell() for the same coordinate would return false.
     */
    public T removeCellById(final Set<Long> coordinate)
    {
        clearScopeKeyCaches();
        clearSha1();
        Set<Column> cols = getColumnsAndCoordinateFromIds(coordinate, null);
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
    public boolean containsCellById(final Collection<Long> coordinate)
    {
        Set<Column> cols = getColumnsAndCoordinateFromIds(coordinate, null);
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
        clearSha1();
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
        clearSha1();
        Set<Column> cols = getColumnsAndCoordinateFromIds(coordinate, null);
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
    public T getCellByIdNoExecute(final Collection<Long> coordinate)
    {
        Set<Column> cols = getColumnsAndCoordinateFromIds(coordinate, null);
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
        Map<String, Object> input = validateCoordinate(coordinate, false);
        boolean run = true;
        T lastStatementValue = null;
        final List<Binding> bindings = ruleInfo.getAxisBindings();
        final int depth = executionStack.get().size();
        final Set<String> axisNameSet = axisList.keySet();
        final String[] axisNames = axisNameSet.toArray(new String[axisNameSet.size()]);
        final Collection<Axis> axes = axisList.values();

        while (run)
        {
            run = false;
            final Map<String, List<Column>> columnToAxisBindings = bindCoordinateToAxisColumns(input);
            final Map<String, Integer> counters = getCountersPerAxis(axisNameSet);
            final Map<Long, Object> cachedConditionValues = new HashMap<>();
            final Map<String, Integer> conditionsFiredCountPerAxis = new HashMap<>();

            try
            {
                Map<String, Object> ctx = prepareExecutionContext(input, output);
                do
                {
                    final Binding binding = new Binding(name, depth);

                    for (final Axis axis: axes)
                    {
                        final String axisName = axis.getName();
                        final Column boundColumn = columnToAxisBindings.get(axisName).get(counters.get(axisName) - 1);

                        if (axis.getType() == AxisType.RULE)
                        {
                            Object conditionValue;
                            if (!cachedConditionValues.containsKey(boundColumn.id))
                            {   // Has the condition on the Rule axis been run this execution?  If not, run it and cache it.
                                CommandCell cmd = (CommandCell) boundColumn.getValue();

                                // If the cmd == null, then we are looking at a default column on a rule axis.
                                // the conditionValue becomes 'true' for Default column when ruleAxisBindCount = 0
                                final Integer count = conditionsFiredCountPerAxis.get(axisName);
                                conditionValue = cmd == null ? isZero(count) : cmd.execute(ctx);
                                final boolean conditionAnswer = isTrue(conditionValue);
                                cachedConditionValues.put(boundColumn.id, conditionAnswer);

                                if (conditionAnswer)
                                {   // Rule fired
                                    conditionsFiredCountPerAxis.put(axisName, count == null ? 1 : count + 1);
                                    if (!axis.isFireAll())
                                    {   // Only fire one condition on this axis (fireAll is false)
                                        counters.put(axisName, 1);
                                        List<Column> boundCols = new ArrayList<>();
                                        boundCols.add(boundColumn);
                                        columnToAxisBindings.put(axisName, boundCols);
                                    }
                                }
                            }
                            else
                            {   // re-use condition on this rule axis (happens when more than one rule axis on an n-cube)
                                conditionValue = cachedConditionValues.get(boundColumn.id);
                            }

                            // A rule column on a given axis can be accessed more than once (example: A, B, C on
                            // one rule axis, X, Y, Z on another).  This generates coordinate combinations
                            // (AX, AY, AZ, BX, BY, BZ, CX, CY, CZ).  The condition columns must be run only once, on
                            // subsequent access, the cached result of the condition is used.
                            if (isTrue(conditionValue))
                            {
                                binding.bind(axisName, boundColumn);
                            }
                            else
                            {   // Incomplete binding - no need to attempt further bindings on other axes.
                                break;
                            }
                        }
                        else
                        {
                            binding.bind(axisName, boundColumn);
                        }
                    }

                    // Step #2 Execute cell and store return value, associating it to the Axes and Columns it bound to
                    if (binding.getNumBoundAxes() == axisNames.length)
                    {   // Conditions on rule axes that do not evaluate to true, do not generate complete coordinates (intentionally skipped)
                        bindings.add(binding);
                        lastStatementValue = executeAssociatedStatement(input, output, ruleInfo, binding);
                    }

                    // Step #3 increment counters (variable radix increment)
                } while (incrementVariableRadixCount(counters, columnToAxisBindings, axisNames));

                // Verify all rule axes were bound 1 or more times
                ensureAllRuleAxesBound(coordinate, conditionsFiredCountPerAxis);
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

        ruleInfo.setLastExecutedStatementValue(lastStatementValue);
        output.put("return", lastStatementValue);
        return lastStatementValue;
    }

    private T executeAssociatedStatement(Map<String, Object> input, Map<String, Object> output, RuleInfo ruleInfo, Binding binding)
    {
        try
        {
            T statementValue = getCellById(binding.getBoundColsForAxis(), input, output);
            binding.setValue(statementValue);
            return statementValue;
        }
        catch (RuleStop e)
        {   // Statement threw at RuleStop
            binding.setValue("[RuleStop]");
            // Mark that RULE_STOP occurred
            ruleInfo.ruleStopThrown();
            throw e;
        }
        catch(RuleJump e)
        {   // Statement threw at RuleJump
            binding.setValue("[RuleJump]");
            throw e;
        }
        catch (Exception e)
        {
            String msg = e.getMessage();
            if (StringUtilities.isEmpty(msg))
            {
                msg = e.getClass().getName();
            }
            binding.setValue("[" + msg + "]");
            throw e;
        }
    }

    /**
     * Verify that at least one rule on each rule axis fired.  If not, then you have a
     * CoordinateNotFoundException.
     * @param coordinate Input (Map) coordinate for getCell()
     * @param conditionsFiredCountPerAxis Map that tracks AxisName to number of fired-columns bound to axis
     */
    private void ensureAllRuleAxesBound(Map<String, Object> coordinate, Map<String, Integer> conditionsFiredCountPerAxis)
    {
        for (Axis axis : axisList.values())
        {
            if (axis.getType() == AxisType.RULE)
            {
                Integer count = conditionsFiredCountPerAxis.get(axis.getName());
                if (count == null || count < 1)
                {
                    throw new CoordinateNotFoundException("No conditions on the rule axis '" + axis.getName() + "' fired, and there is no default column on this axis, cube: " + name + ", input: " + coordinate);
                }
            }
        }
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
            throw new CoordinateNotFoundException("Coordinate not found in cube: " + name + "\n" + stackToString(), e);
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
        final Map<String, Object> ctx = new HashMap<>();
        ctx.put("input", coord);   // Input coordinate is already a duplicate (CaseInsensitiveMap) at this point
        ctx.put("output", output);
        ctx.put("ncube", this);
        return ctx;
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

    /**
     * Get / Create the RuleInfo Map stored at output[NCube.RULE_EXEC_INFO]
     */
    public static RuleInfo getRuleInfo(Map<String, Object> output)
    {
        final RuleInfo ruleInfo;
        if (output.containsKey(NCube.RULE_EXEC_INFO))
        {   // RULE_EXEC_INFO Map already exists, must be a recursive call.
            return (RuleInfo) output.get(NCube.RULE_EXEC_INFO);
        }
        // RULE_EXEC_INFO Map does not exist, create it.
        ruleInfo = new RuleInfo();
        output.put(NCube.RULE_EXEC_INFO, ruleInfo);
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

        if (ruleValue instanceof Boolean)
        {
            return ruleValue.equals(true);
        }

        if (ruleValue instanceof Number)
        {
            boolean isZero = ruleValue.equals((byte)0) ||
                    ruleValue.equals((short)0) ||
                    ruleValue.equals(0) ||
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
     * @param input The passed in input coordinate to bind (or multi-bind) to each axis.
     */
    private Map<String, List<Column>> bindCoordinateToAxisColumns(Map input)
    {
        Map<String, List<Column>> bindings = new CaseInsensitiveMap<>();
        for (final Map.Entry<String, Axis> entry : axisList.entrySet())
        {
            final String axisName = entry.getKey();
            final Axis axis = entry.getValue();
            final Comparable value = (Comparable) input.get(axisName);

            if (axis.getType() == AxisType.RULE)
            {   // For RULE axis, all possible columns must be added (they are tested later during execution)
                bindings.put(axisName, axis.getRuleColumnsStartingAt((String) input.get(axis.getName())));
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
                bindings.put(axisName, cols);
            }
        }

        return bindings;
    }

    private static String[] getAxisNames(final Map<String, List<Column>> bindings)
    {
        return bindings.keySet().toArray(new String[bindings.size()]);
    }

    private static Map<String, Integer> getCountersPerAxis(final Set<String> axisNames)
    {
        final Map<String, Integer> counters = new CaseInsensitiveMap<>();

        // Set counters to 1
        for (final String axisName : axisNames)
        {
            counters.put(axisName, 1);
        }
        return counters;
    }

    /**
     * Flip Set<Long> (coordinate) to Set<Column>. The challenge is that the Set<Long> is allowed to leave
     * out bindings for Axes that have default columns.  An optional Map<String, CellInfo> can be passed in,
     * which is filled in with the required axes as keys -and- for each axis that is not a rule axis,
     * a column value that would bind to that axis (specified as a CellInfo).
     *
     * @throws IllegalArgumentException if not enough IDs are passed in, or an axis
     * cannot bind to any of the passed in IDs.
     */
    Set<Column> getColumnsAndCoordinateFromIds(final Collection<Long> coordinate, Map<String, CellInfo> coord)
    {
        // Ensure that the specified coordinate matches a column on each axis
        final Set<Axis> axisRef = new HashSet<>();
        final Set<Axis> allAxes = new HashSet<>(axisList.values());
        final Set<Column> point = new HashSet<>();

        // Bind all Longs to Columns on an axis.  Allow for additional columns to be specified,
        // but not more than one column ID per axis.  Also, too few can be supplied, if and
        // only if, the axes that are not bound too have a Default column (which will be chosen).
        for (final Axis axis : allAxes)
        {
            for (final Long id : coordinate)
            {
                final Column column = axis.getColumnById(id);
                if (column != null)
                {
                    if (axisRef.contains(axis))
                    {
                        throw new IllegalArgumentException("Cannot have more than one column ID per axis, axis '" + axis.getName() + "', cube: " + name);
                    }

                    axisRef.add(axis);
                    point.add(column);
                    addCoordinateToColumnEntry(coord, column, axis);
                }
            }
        }

        // Remove the referenced axes from allAxes set.  This leaves axes to be resolved.
        allAxes.removeAll(axisRef);

        // For the unbound axes, bind them to the Default Column (if the axis has one)
        axisRef.clear();   // use Set again, this time to hold unbound axes
        axisRef.addAll(allAxes);

        // allAxes at this point, is the unbound axis (not referenced by an id in input coordinate)
        for (final Axis axis : allAxes)
        {
            if (axis.hasDefaultColumn())
            {
                final Column defCol = axis.getDefaultColumn();
                axisRef.remove(axis);
                point.add(defCol);
                addCoordinateToColumnEntry(coord, defCol, axis);
            }
        }

        // Add in all required scope keys to the [optional] passed in coord
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

        if (!axisRef.isEmpty())
        {
            final StringBuilder s = new StringBuilder();
            for (Axis axis : axisRef)
            {
                s.append(axis.getName());
            }
            throw new IllegalArgumentException("Column IDs missing for the axes: " + s + ", cube: " + name);
        }

        return point;
    }

    /**
     * Associate input variables (axis names) to CellInfo for the given column.
     */
    private static void addCoordinateToColumnEntry(Map<String, CellInfo> coord, Column column, Axis axis)
    {
        if (coord == null)
        {
            return;
        }

        if (axis.getType() != AxisType.RULE)
        {
            coord.put(axis.getName(), new CellInfo(column.getValueThatMatches()));
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
            final String axisName = axisNames[digit];
            final int count = counters.get(axisName);
            final List<Column> cols = bindings.get(axisName);

            if (count >= cols.size())
            {   // Reach max value for given dimension (digit)
                if (digit == 0)
                {   // we have reached the max radix for the most significant digit - we are done
                    return false;
                }
                counters.put(axisNames[digit--], 1);
            }
            else
            {
                counters.put(axisName, count + 1);  // increment counter
                return true;
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
                wildcardAxis = axisList.get(entry.getKey());      // intentional case insensitive match
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
        clearSha1();
    }

    /**
     * Clear all cell values.  All axes and columns remain.
     */
    public void clearCells()
    {
        cells.clear();
        clearScopeKeyCaches();
        clearSha1();
    }

    /**
     * Add a column to the n-cube
     * @param axisName String name of the Axis to which the column will be added.
     * @param value Comparable that will be the value for the given column.  Cannot be null.
     * @return Column the added Column.
     */
    public Column addColumn(final String axisName, final Comparable value)
    {
        return addColumn(axisName, value, null);
    }

    /**
     * Add a column to the n-cube
     * @param axisName String name of the Axis to which the column will be added.
     * @param value Comparable that will be the value for the given column.  Cannot be null.
     * @param colName The optional name of the column, useful for RULE axis columns.
     * @return Column the added Column.
     */
    public Column addColumn(final String axisName, final Comparable value, String colName)
    {
        final Axis axis = getAxis(axisName);
        if (axis == null)
        {
            throw new IllegalArgumentException("Could not add column. Axis name '" + axisName + "' was not found on NCube '" + name + "'");
        }
        Column newCol = axis.addColumn(value, colName);
        clearScopeKeyCaches();
        clearSha1();
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
        clearSha1();
        final Column column = axis.deleteColumn(value);
        if (column == null)
        {
            return false;
        }

        // Remove all cells that reference the deleted column
        final Iterator<Collection<Column>> i = cells.keySet().iterator();

        while (i.hasNext())
        {
            final Collection<Column> key = i.next();
            // Locate the uniquely identified column, regardless of axis order
            if (key.contains(column))
            {
                i.remove();
            }
        }
        return true;
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
        clearScopeKeyCaches();
        clearSha1();
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
        if (!axisList.containsKey(newCols.getName()))
        {
            throw new IllegalArgumentException("No axis exists with the name: " + newCols.getName() + ", NCube '" + name + "'");
        }

        final Axis axisToUpdate = axisList.get(newCols.getName());
        final Set<Long> colsToDel = axisToUpdate.updateColumns(newCols);
        Column testColumn = new Column(1, newCols.getNextColId());
        Iterator<Collection<Column>> i = cells.keySet().iterator();

        while (i.hasNext())
        {
            Collection<Column> cols = i.next();

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

        clearScopeKeyCaches();
        clearSha1();
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
    public Map<Collection<Column>, T> getCellMap()
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
        return axisList.get(axisName);
    }

    /**
     * Add an Axis to this NCube.
     * All cells will be cleared when axis is added.
     * @param axis Axis to add
     */
    public void addAxis(final Axis axis)
    {
        String axisName = axis.getName();
        if (axisList.containsKey(axisName))
        {
            throw new IllegalArgumentException("An axis with the name '" + axisName + "' already exists on NCube '" + name + "'");
        }

        cells.clear();
        axisList.put(axisName, axis);
        clearScopeKeyCaches();
        clearSha1();
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
        axisList.remove(oldName);
        axis.setName(newName);
        axisList.put(newName, axis);
        clearScopeKeyCaches();
        clearSha1();
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
        clearSha1();
        return axisList.remove(axisName) != null;
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
                        throw new IllegalStateException("Attempting to get required scope, but n-cube: " + ncube + " failed to load");
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
    private static Set<String> getScopeKeysFromCommandCells(Map<Collection<Column>, ?> cubeCells)
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
    public Set<String> getReferencedCubeNames()
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
            ncube.metaProps.remove("sha1");
            loadMetaProperties(ncube.metaProps);

            String defType = (String) jsonNCube.get("defaultCellValueType");
            ncube.defaultCellValue = CellInfo.parseJsonValue(jsonNCube.get("defaultCellValue"), null, defType, false);

            if (!(jsonNCube.get("axes") instanceof JsonObject))
            {
                throw new IllegalArgumentException("Must specify a list of axes for the ncube, under the key 'axes' as [{axis 1}, {axis 2}, ... {axis n}], cube: " + cubeName);
            }

            JsonObject axes = (JsonObject) jsonNCube.get("axes");
            Object[] items = axes.getArray();

            if (ArrayUtilities.isEmpty(items))
            {
                throw new IllegalArgumentException("Must be at least one axis defined in the JSON format, cube: " + cubeName);
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
                boolean fireAll = true;
                if (jsonAxis.containsKey("fireAll"))
                {
                    fireAll = getBoolean(jsonAxis, "fireAll");
                }
                Axis axis = new Axis(name, type, valueType, hasDefault, preferredOrder, idBase++, fireAll);
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
                axis.metaProps.remove("fireAll");

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
                    throw new IllegalArgumentException("'columns' must be specified, axis '" + name + "', cube: " + cubeName);
                }
                JsonObject colMap = (JsonObject) jsonAxis.get("columns");

                if (!colMap.isArray())
                {
                    throw new IllegalArgumentException("'columns' must be an array, axis '" + name + "', cube: " + cubeName);
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
                    String colName = (String) jsonColumn.get(Column.NAME);

                    if (value == null)
                    {
                        if (id == null)
                        {
                            throw new IllegalArgumentException("Missing 'value' field on column or it is null, axis '" + name + "', cube: " + cubeName);
                        }
                        else
                        {   // Allows you to skip setting both id and value to the same value.
                            value = id;
                        }
                    }

                    boolean cache = false;

                    if (jsonColumn.containsKey("cache"))
                    {
                        cache = getBoolean(jsonColumn, "cache");
                    }

                    Column colAdded;

                    if (type == AxisType.DISCRETE || type == AxisType.NEAREST)
                    {
                        colAdded = ncube.addColumn(axis.getName(), (Comparable) CellInfo.parseJsonValue(value, null, colType, false), colName);
                    }
                    else if (type == AxisType.RANGE)
                    {
                        Object[] rangeItems = ((JsonObject)value).getArray();
                        if (rangeItems.length != 2)
                        {
                            throw new IllegalArgumentException("Range must have exactly two items, axis '" + name +"', cube: " + cubeName);
                        }
                        Comparable low = (Comparable) CellInfo.parseJsonValue(rangeItems[0], null, colType, false);
                        Comparable high = (Comparable) CellInfo.parseJsonValue(rangeItems[1], null, colType, false);
                        colAdded = ncube.addColumn(axis.getName(), new Range(low, high), colName);
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
                                    throw new IllegalArgumentException("Set Ranges must have two values only, range length: " + rangeValues.length + ", axis '" + name + "', cube: " + cubeName);
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
                        colAdded = ncube.addColumn(axis.getName(), rangeSet, colName);
                    }
                    else if (type == AxisType.RULE)
                    {
                        Object cmd = CellInfo.parseJsonValue(value, url, colType, cache);
                        if (!(cmd instanceof CommandCell))
                        {
                            cmd = new GroovyExpression("false", null, false);
                        }
                        colAdded = ncube.addColumn(axis.getName(), (CommandCell)cmd, colName);
                    }
                    else
                    {
                        throw new IllegalArgumentException("Unsupported Axis Type '" + type + "' for simple JSON input, axis '" + name + "', cube: " + cubeName);
                    }

                    if (id != null)
                    {
                        userIdToUniqueId.put(id, colAdded.id);
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
            if (jsonNCube.get("cells") instanceof JsonObject)
            {   // Allow JSON to have no cells
                JsonObject cellMap = (JsonObject) jsonNCube.get("cells");

                if (!cellMap.isArray())
                {
                    throw new IllegalArgumentException("'cells' must be an []. It can be empty but must be specified, cube: " + cubeName);
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
                        cache = getBoolean(cMap, "cache");
                    }

                    Object v = CellInfo.parseJsonValue(cMap.get("value"), url, type, cache);

                    if (ids instanceof JsonObject)
                    {   // If specified as ID array, build coordinate that way
                        Set<Long> colIds = new HashSet<>();
                        for (Object id : ((JsonObject)ids).getArray())
                        {
                            if (!userIdToUniqueId.containsKey(id))
                            {
                                throw new IllegalArgumentException("ID specified in cell does not match an ID in the columns, id: " + id + ", cube: " + cubeName);
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
            }

            return ncube;
        }
        catch (Exception e)
        {
            if (e instanceof RuntimeException)
            {
                throw (RuntimeException)e;
            }
            throw new RuntimeException("Error reading cube from passed in JSON", e);
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
        if (val instanceof Number)
        {
            return ((Number) val).longValue();
        }
        if (val instanceof String)
        {
            try
            {
                return Long.parseLong((String)val);
            }
            catch(Exception ignored)
            { }
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
        if (val instanceof String)
        {
            return "true".equalsIgnoreCase((String) val);
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
        for (Collection<Column> pt : cells.keySet())
        {
            colIds.clear();
            for (Column col : pt)
            {
                colIds.add(col.id);
            }
            Map<String, CellInfo> coord = new CaseInsensitiveMap<>();
            getColumnsAndCoordinateFromIds(colIds, coord);
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
            Axis copyAxis = new Axis(axis.getName(), axis.getType(), axis.getValueType(), axis.hasDefaultColumn(), axis.getColumnOrder(), axis.id, axis.isFireAll());
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

        for (Map.Entry<Collection<Column>, T> entry : cells.entrySet())
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

        if (this == other)
        {
            return true;
        }

        return sha1().equalsIgnoreCase(((NCube) other).sha1());
    }

    public int hashCode()
    {
        return name.hashCode();
    }

    void clearSha1()
    {
        sha1 = null;
    }

    /**
     * @return SHA1 value for this n-cube.  The value is durable in that Axis order and
     * cell order do not affect the SHA1 value.
     */
    public String sha1()
    {
        // Check if the SHA1 is already calculated.  If so, return it.
        // In order to cache it successfully, all mutable operations on n-cube must clear the SHA1.
        if (StringUtilities.hasContent(sha1))
        {
            return sha1;
        }

        final byte sep = 0;
        MessageDigest sha1Digest = EncryptionUtilities.getSHA1Digest();
        sha1Digest.update(name == null ? "".getBytes() : name.getBytes());
        sha1Digest.update(sep);

        deepSha1(sha1Digest, defaultCellValue, sep);
        deepSha1(sha1Digest, new TreeMap(getMetaProperties()), sep);

        // Need deterministic ordering (sorted by Axis name will do that)
        Map<String, Axis> sortedAxes = new TreeMap<>(axisList);
        final Map<String, List<Column>> allCoordinates = new LinkedHashMap<>();
        sha1Digest.update((byte)'a');       // a=axes
        sha1Digest.update(sep);

        for (Map.Entry<String, Axis> entry : sortedAxes.entrySet())
        {
            Axis axis = entry.getValue();
            allCoordinates.put(axis.getName(), axis.columns);
            sha1Digest.update(axis.getName().toLowerCase().getBytes());
            sha1Digest.update(sep);
            sha1Digest.update(String.valueOf(axis.getColumnOrder()).getBytes());
            sha1Digest.update(sep);
            sha1Digest.update(axis.getType().name().getBytes());
            sha1Digest.update(sep);
            sha1Digest.update(axis.getValueType().name().getBytes());
            sha1Digest.update(sep);
            sha1Digest.update(axis.hasDefaultColumn() ? (byte)'t' : (byte)'f');
            sha1Digest.update(sep);
            if (!axis.isFireAll())
            {   // non-default value, add to SHA1 because it's been changed (backwards sha1 compatible)
                sha1Digest.update((byte)'o');
                sha1Digest.update(sep);
            }
            if (!MapUtilities.isEmpty(axis.metaProps))
            {
                deepSha1(sha1Digest, new TreeMap(axis.metaProps), sep);
            }
            sha1Digest.update(sep);

            for (Column column : axis.getColumnsWithoutDefault())
            {
                sha1Digest.update(column.getValue().toString().getBytes());
                sha1Digest.update(sep);
                if (!MapUtilities.isEmpty(column.metaProps))
                {
                    deepSha1(sha1Digest, column.metaProps, sep);
                }
                sha1Digest.update(sep);
            }
        }

        // Deterministic ordering of cell values with coordinates.
        // 1. Build String SHA-1 of coordinate + SHA-1 of cell contents.
        // 2. Combine and then sort.
        // 3. Build SHA-1 from this.
        sha1Digest.update((byte)'c');  // c = cells
        sha1Digest.update(sep);

        if (getNumCells() > 0)
        {
            List<String> sha1s = new ArrayList<>();
            MessageDigest tempDigest = EncryptionUtilities.getSHA1Digest();
            for (Map.Entry<Collection<Column>, T> entry : cells.entrySet())
            {
                String keySha1 = columnValuesToString(entry.getKey());

                deepSha1(tempDigest, entry.getValue(), sep);
                String valueSha1 = StringUtilities.encode(tempDigest.digest());
                sha1s.add(EncryptionUtilities.calculateSHA1Hash((keySha1 + valueSha1).getBytes()));

                tempDigest.reset();
            }

            Collections.sort(sha1s);
            for (String sha_1 : sha1s)
            {
                sha1Digest.update(sha_1.getBytes());
            }
        }
        sha1 = StringUtilities.encode(sha1Digest.digest());
        return sha1;
    }

    private static String columnValuesToString(Collection<Column> columns)
    {
        List<String> list = new ArrayList<>();
        for (Column column : columns)
        {
            Object value = column.getValue();
            list.add(value == null ? "null" : column.getValue().toString());
        }
        Collections.sort(list);
        StringBuilder s = new StringBuilder();
        for (String str : list)
        {
            s.append(str);
            s.append('|');
        }
        return s.toString();
    }

    private static void deepSha1(MessageDigest md, Object value, byte sep)
    {
        Deque stack = new LinkedList();
        stack.addFirst(value);
        Set visited = new HashSet();

        while (!stack.isEmpty())
        {
            value = stack.removeFirst();
            if (visited.contains(value))
            {
                continue;
            }
            visited.add(value);

            if (value == null)
            {
                md.update("null".getBytes());
                md.update(sep);
            }
            else if (value.getClass().isArray())
            {
                int len = Array.getLength(value);

                md.update("array".getBytes());
                md.update(String.valueOf(len).getBytes());
                md.update(sep);
                for (int i=0; i < len; i++)
                {
                    stack.addFirst(Array.get(value, i));
                }
            }
            else if (value instanceof Collection)
            {
                Collection col = (Collection) value;
                md.update("col".getBytes());
                md.update(String.valueOf(col.size()).getBytes());
                md.update(sep);
                stack.addAll(col);
            }
            else if (value instanceof Map)
            {
                Map map  = (Map) value;
                md.update("map".getBytes());
                md.update(String.valueOf(map.size()).getBytes());
                md.update(sep);

                for (Map.Entry entry : (Iterable<Map.Entry>) map.entrySet())
                {
                    stack.addFirst(entry.getValue());
                    stack.addFirst(entry.getKey());
                }
            }
            else
            {
                if (value instanceof String)
                {
                    md.update(((String)value).getBytes());
                    md.update(sep);
                }
                else if (value instanceof CommandCell)
                {
                    CommandCell cmdCell = (CommandCell) value;
                    md.update(cmdCell.getClass().getName().getBytes());
                    md.update(sep);
                    if (cmdCell.getUrl() != null)
                    {
                        md.update(cmdCell.getUrl().getBytes());
                        md.update(sep);
                    }
                    if (cmdCell.getCmd() != null)
                    {
                        md.update(cmdCell.getCmd().getBytes());
                        md.update(sep);
                    }
                    if (value instanceof UrlCommandCell)
                    {
                        md.update(((UrlCommandCell)cmdCell).isCacheable() ? (byte) 't' : (byte) 'f');
                        md.update(sep);
                    }
                }
                else if (value instanceof ClassLoader)
                {
                    //  TODO:  This code would not longer be hit if we forced sys.classpath
                    //  cubes to actually return a URLClassLoader instead of array of string
                    //  and allowed natural GroovyExpression caching to do it.
                    if (value instanceof URLClassLoader)
                    {
                        URLClassLoader cl = (URLClassLoader) value;
                        for (URL url : cl.getURLs())
                        {
                            md.update(url.toString().getBytes());
                        }
                    }
                    else
                    {
                        md.update(value.getClass().getName().getBytes());
                    }
                    md.update(sep);
                }
                else
                {
                    String strKey = value.toString();
                    if (strKey.contains("@"))
                    {
                        md.update(toJson(value).getBytes());
                    }
                    else
                    {
                        md.update(strKey.getBytes());
                    }
                }
                md.update(sep);
            }
        }
    }

    public List<Delta> getDeltaDescription(NCube<T> old)
    {
        List<Delta> changes = new ArrayList<>();

        if (!name.equalsIgnoreCase(old.name))
        {
            String s = "Name changed from '" + old.name + "' to '" + name + "'";
            changes.add(new Delta(Delta.Location.NCUBE, Delta.Type.UPDATE, s));
        }

        List<Delta> metaChanges = compareMetaProperties(old.getMetaProperties(), getMetaProperties(), Delta.Location.NCUBE_META, "n-cube '" + name + "'");
        changes.addAll(metaChanges);

        CaseInsensitiveSet a1 = new CaseInsensitiveSet(axisList.keySet());
        CaseInsensitiveSet a2 = new CaseInsensitiveSet(old.axisList.keySet());
        a1.removeAll(a2);

        boolean axesChanged = false;
        if (!a1.isEmpty())
        {
            String s = "Added axis: " + a1;
            changes.add(new Delta(Delta.Location.AXIS, Delta.Type.ADD, s));
            axesChanged = true;
        }

        a1 = new CaseInsensitiveSet(axisList.keySet());
        a2.removeAll(a1);
        if (!a2.isEmpty())
        {
            String s = "Removed axis: " + a2;
            changes.add(new Delta(Delta.Location.AXIS, Delta.Type.DELETE, s));
            axesChanged = true;
        }

        for (Axis newAxis : axisList.values())
        {
            Axis oldAxis = old.getAxis(newAxis.getName());
            if (oldAxis == null)
            {
                continue;
            }
            if (!newAxis.areAxisPropsEqual(oldAxis))
            {
                String s = "Axis properties changed from " + oldAxis.getAxisPropString() + " to " + newAxis.getAxisPropString();
                changes.add(new Delta(Delta.Location.AXIS, Delta.Type.UPDATE, s));
            }

            metaChanges = compareMetaProperties(oldAxis.getMetaProperties(), newAxis.getMetaProperties(), Delta.Location.AXIS_META, "axis '" + newAxis.getName() + "'");
            changes.addAll(metaChanges);

            for (Column newCol : newAxis.getColumns())
            {
                Column oldCol = oldAxis.idToCol.get(newCol.id);
                if (oldCol == null)
                {
                    String s = "Column: " + newCol.getValue() + " added to axis: " + newAxis.getName();
                    changes.add(new Delta(Delta.Location.COLUMN, Delta.Type.ADD, s));
                }
                else
                {   // Check Column meta properties
                    metaChanges = compareMetaProperties(oldCol.getMetaProperties(), newCol.getMetaProperties(), Delta.Location.COLUMN_META, "column '" + newAxis.getName() + "'");
                    changes.addAll(metaChanges);

                    if (!DeepEquals.deepEquals(oldCol.getValue(), newCol.getValue()))
                    {
                        String s = "Column value changed from: " + oldCol.getValue() + " to: " + newCol.getValue();
                        changes.add(new Delta(Delta.Location.COLUMN, Delta.Type.UPDATE, s));
                    }
                }
            }

            for (Column oldCol : oldAxis.getColumns())
            {
                Column newCol = newAxis.idToCol.get(oldCol.id);
                if (newCol == null)
                {
                    String s = "Column: " + oldCol.getValue() + " removed";
                    changes.add(new Delta(Delta.Location.COLUMN, Delta.Type.DELETE, s));
                }
            }
        }

        // Different dimensionality, don't compare cells
        if (axesChanged)
        {
            return changes;
        }

        for (Map.Entry<Collection<Column>, T> entry : cells.entrySet())
        {
            Collection<Column> newCellKey = entry.getKey();
            T newCellValue = entry.getValue();

            if (old.cells.containsKey(newCellKey))
            {
                Object oldCellValue = old.cells.get(newCellKey);
                if (!DeepEquals.deepEquals(newCellValue, oldCellValue))
                {
                    Map<String, Object> properCoord = idCoordToProperCoord(newCellKey);
                    String s = "Cell changed at location: " + properCoord + ", from: " +
                            (oldCellValue == null ? null : oldCellValue.toString()) + ", to: " +
                            (newCellValue == null ? null : newCellValue.toString());
                    changes.add(new Delta(Delta.Location.CELL, Delta.Type.UPDATE, s));
                }
            }
            else
            {
                Map<String, Object> properCoord = idCoordToProperCoord(newCellKey);
                String s = "Cell added at location: " + properCoord + ", value: " + (newCellValue == null ? null : newCellValue.toString());
                changes.add(new Delta(Delta.Location.CELL, Delta.Type.ADD, s));
            }
        }

        for (Map.Entry<Collection<Column>, T> entry : old.cells.entrySet())
        {
            Collection<Column> oldCellKey = entry.getKey();
            T oldCellValue = entry.getValue();

            if (!cells.containsKey(oldCellKey))
            {
                boolean allColsStillExist = true;
                for (Column column : oldCellKey)
                {
                    Axis axis = getAxisFromColumnId(column.id);
                    if (axis == null)
                    {
                        allColsStillExist = false;
                        break;
                    }
                }

                // Make sure all columns for this cell still exist before reporting it as removed.  Otherwise, a
                // dropped column would report a ton of removed cells.
                if (allColsStillExist)
                {
                    Map<String, Object> properCoord = idCoordToProperCoord(oldCellKey);
                    String s = "Cell removed at location: " + properCoord + ", value: " + (oldCellValue == null ? null : oldCellValue.toString());
                    changes.add(new Delta(Delta.Location.CELL, Delta.Type.DELETE, s));
                }
            }
        }
        return changes;
    }

    static List<Delta> compareMetaProperties(Map<String, Object> oldMeta, Map<String, Object> newMeta, Delta.Location location, String locName)
    {
        List<Delta> changes = new ArrayList<>();
        Set<String> oldKeys = new CaseInsensitiveSet<>(oldMeta.keySet());
        Set<String> sameKeys = new CaseInsensitiveSet<>(newMeta.keySet());
        sameKeys.retainAll(oldKeys);

        Set<String> addedKeys  = new CaseInsensitiveSet<>(newMeta.keySet());
        addedKeys.removeAll(sameKeys);
        if (!addedKeys.isEmpty())
        {
            StringBuilder s = makeMap(newMeta, addedKeys);
            String entry = addedKeys.size() > 1 ? "meta-entries" : "meta-entry";
            changes.add(new Delta(location, Delta.Type.ADD, locName + " " + entry + " added: " + s));
        }

        Set<String> deletedKeys  = new CaseInsensitiveSet<>(oldMeta.keySet());
        deletedKeys.removeAll(sameKeys);
        if (!deletedKeys.isEmpty())
        {
            StringBuilder s = makeMap(oldMeta, deletedKeys);
            String entry = deletedKeys.size() > 1 ? "meta-entries" : "meta-entry";
            changes.add(new Delta(location, Delta.Type.DELETE, locName + " " + entry + " deleted: " + s));
        }

        int i = 0;
        StringBuilder s = new StringBuilder();
        for (String key : sameKeys)
        {
            if (!DeepEquals.deepEquals(oldMeta.get(key), newMeta.get(key)))
            {
                s.append(key + "->" + oldMeta.get(key) + " ==> " + key + "->" + newMeta.get(key) + ", ");
                i++;
            }
        }
        if (i > 0)
        {
            s.setLength(s.length() - 2);     // remove extra ", " at end
            String entry = i > 1 ? "meta-entries" : "meta-entry";
            changes.add(new Delta(location, Delta.Type.UPDATE, locName + " " + entry + " changed: " + s));
        }

        return changes;
    }

    private static StringBuilder makeMap(Map<String, Object> newMeta, Set<String> addedKeys)
    {
        StringBuilder s = new StringBuilder();
        Iterator<String> i = addedKeys.iterator();
        while (i.hasNext())
        {
            String key = i.next();
            s.append(key);
            s.append("->");
            s.append(newMeta.get(key));
            if (i.hasNext())
            {
                s.append(", ");
            }
        }
        return s;
    }

    private Map<String, Object> idCoordToProperCoord(Collection<Column> idCoord)
    {
        Map<String, Object> properCoord = new CaseInsensitiveMap<>();
        try
        {
            for (Column column : idCoord)
            {
                Axis axis = getAxisFromColumnId(column.id);
                Object value = column.getValueThatMatches();
                if (value == null)
                {
                    value = "default column";
                }
                if (axis.getType() == AxisType.RULE)
                {
                    String ruleName = (String) column.getMetaProperty("name");
                    if (StringUtilities.hasContent(ruleName))
                    {
                        properCoord.put(axis.getName(), "rule: '" + ruleName + "' condition: " + value);
                    }
                    else
                    {
                        properCoord.put(axis.getName(), "condition: " + value);
                    }
                }
                else
                {
                    properCoord.put(axis.getName(), value);
                }
            }
        }
        catch (Exception ignored)
        { }
        return properCoord;
    }

    long getMaxAxisId()
    {
        long max = 0;
        for (Axis axis : axisList.values())
        {
            long axisId = axis.getId();
            if (axisId > max)
            {
                max = axisId;
            }
        }
        return max;
    }

    static String toJson(Object o)
    {
        if (o == null)
        {
            return "null";
        }
        try
        {
            return JsonWriter.objectToJson(o);
        }
        catch (Exception e)
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

    public static NCube createCubeFromBytes(byte[] jsonBytes)
    {
        String json = StringUtilities.createUTF8String(IOUtilities.uncompressBytes(jsonBytes));
        return NCubeManager.ncubeFromJson(json);
    }

    public byte[] getBytesFromCube()
    {
        byte[] jsonBytes = StringUtilities.getBytes(toFormattedJson(), "UTF-8");
        return IOUtilities.compressBytes(jsonBytes);
    }



    public void setName(String name) {
        this.name = name;
        clearSha1();
    }
}
