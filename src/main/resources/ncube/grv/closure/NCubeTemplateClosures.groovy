import com.cedarsoftware.ncube.*
import com.cedarsoftware.ncube.exception.*
import com.cedarsoftware.ncube.formatters.*
import com.cedarsoftware.ncube.proximity.*
import com.cedarsoftware.ncube.util.*
import ncube.grv.exp.cdn.*
import ncube.grv.method.*
import com.cedarsoftware.util.*
import com.cedarsoftware.util.io.*

NCube getCube(cubeName = ncube.name)
{
    if (cubeName == ncube.name)
    {
        return ncube
    }
    NCube cube = NCubeManager.getCube(ncube.applicationID, cubeName)
    if (cube == null)
    {
        throw new IllegalArgumentException('n-cube: ' + cubeName + ', does not exist in application: ' + ncube.applicationID)
    }
    return cube
}

Axis getAxis(String axisName, String cubeName = ncube.name)
{
    Axis axis = getCube(cubeName).getAxis(axisName)
    if (axis == null)
    {
        throw new IllegalArgumentException('Axis: ' + axisName + ', does not exist on n-cube: ' + cubeName + ', appId: ' + ncube.applicationID)
    }
    return axis
}

Column getColumn(Comparable value, String axisName, String cubeName = ncube.name)
{
    return getAxis(axisName, cubeName).findColumn(value)
}

def getCell(Map coord, String cubeName = ncube.name)
{
    input.putAll(coord)
    return getCube(cubeName).getCell(input, output)
}

def getFixedCell(Map coord, String cubeName = ncube.name)
{
    return getCube(cubeName).getCell(coord, output)
}

def ruleStop()
{
    throw new RuleStop()
}

def jump(Map coord)
{
    input.putAll(coord);
    throw new RuleJump(input)
}
