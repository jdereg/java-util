package ncube.grv.exp;

import com.cedarsoftware.ncube.Axis;
import com.cedarsoftware.ncube.Column;
import com.cedarsoftware.ncube.NCube;
import com.cedarsoftware.ncube.NCubeManager;
import com.cedarsoftware.ncube.exception.RuleJump;
import com.cedarsoftware.ncube.exception.RuleStop;

import java.util.Map;

/**
 * Base class for all GroovyExpression and GroovyMethod's within n-cube CommandCells.
 * @see com.cedarsoftware.ncube.GroovyBase
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
public class NCubeGroovyExpression
{
    protected Map input;
    protected Map output;
    protected NCube ncube;

    /**
     * @param args a Map that contains the 'input' Map, 'output' Map, 'ncube',
     *  and the 'stack.'
     */
    public void init(Map args)
    {
        input = (Map) args.get("input");
        output = (Map) args.get("output");
        ncube = (NCube) args.get("ncube");
    }

    protected Map getInput()
    {
        return input;
    }

    protected Map getOutput()
    {
        return output;
    }

    protected NCube getNCube()
    {
        return ncube;
    }

    public NCube getCube(String name)
    {
        NCube cube = NCubeManager.getCube(name, ncube.getApplicationID());
        if (cube == null)
        {
            throw new IllegalArgumentException("n-cube: " + name + " not loaded into NCubeManager, make sure to load all n-cubes first.");
        }
        return cube;
    }

    public Object getFixedCell(Map coord)
    {
        return ncube.getCell(coord, output);
    }

    public Object getFixedCubeCell(String cubeName, Map coord)
    {
        return getCube(cubeName).getCell(coord, output);
    }

    public Object getRelativeCell(Map coord)
    {
        input.putAll(coord);
        return ncube.getCell(input, output);
    }

    public Object getRelativeCubeCell(String cubeName, Map coord)
    {
        input.putAll(coord);
        return getCube(cubeName).getCell(input, output);
    }

    /**
     * Restart rule execution.  The Map contains the names of rule axes to rule names.  For any rule axis
     * specified in the map, the rule step counter will be moved (jumped) to the named rule.  More than one
     * rule axis step counter can be moved by including multiple entries in the map.
     * @param coord Map of rule axis names, to rule names.  If the map is empty, it is the same as calling
     * jump() with no args.
     */
    public void jump(Map coord)
    {
        input.putAll(coord);
        throw new RuleJump(input);
    }

    /**
     * Stop rule execution from going any further.
     */
    public void ruleStop()
    {
        throw new RuleStop();
    }

    /**
     * Run another rule cube
     * @param cubeName String name of the other rule cube
     * @param coord Map input coordinate
     * @return is the return Map from the other rule cube
     */
    public Object runRuleCube(String cubeName, Map coord)
    {
        input.putAll(coord);
        return getCube(cubeName).getCell(input, output);
    }

    public Column getColumn(String axisName, Comparable value)
    {
        Axis axis = getAxis(axisName);
        return axis.findColumn(value);
    }

    public Column getColumn(String cubeName, String axisName, Comparable value)
    {
        Axis axis = getAxis(cubeName, axisName);
        return axis.findColumn(value);
    }

    public Axis getAxis(String axisName)
    {
        Axis axis = ncube.getAxis(axisName);
        if (axis == null)
        {
            throw new IllegalArgumentException("Axis '" + axisName + "' does not exist on n-cube: " + ncube.getName());
        }

        return axis;
    }

    public Axis getAxis(String cubeName, String axisName)
    {
        Axis axis = getCube(cubeName).getAxis(axisName);
        if (axis == null)
        {
            throw new IllegalArgumentException("Axis '" + axisName + "' does not exist on n-cube: " + cubeName);
        }

        return axis;
    }

    /**
     * @return long Current time in nano seconds (used to compute how long something takes to execute)
     */
    public long now()
    {
        return System.nanoTime();
    }

    /**
     * Get floating point millisecond value for how much time elapsed.
     * @param begin long value from call to now()
     * @param end long value from call to now()
     * @return double elapsed time in milliseconds.
     */
    public double elapsedMillis(long begin, long end)
    {
        return (end - begin) / 1000000.0;
    }
}