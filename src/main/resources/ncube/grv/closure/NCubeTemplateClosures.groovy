import com.cedarsoftware.ncube.Axis
import com.cedarsoftware.ncube.NCube
import com.cedarsoftware.ncube.NCubeManager
import com.cedarsoftware.ncube.exception.RuleJump
import com.cedarsoftware.ncube.exception.RuleStop

def getCube = { name -> return NCubeManager.getCube(ncube.applicationID, name) }

def getApplicationID = { return ncube.applicationID }

def getCubeNames = { return NCubeManager.getCubeNames(ncube.applicationID) }

def getCubeRecords = { pattern -> return NCubeManager.search(ncube.applicationID, pattern, null, [(NCubeManager.SEARCH_ACTIVE_RECORDS_ONLY): true]) }

def search =
        {  String namePattern, String textPattern, Map options ->
            return NCubeManager.search(ncube.applicationID, namePattern, textPattern, options)
        }

def getColumn =
        { String axisName, Comparable value ->
            Axis axis = ncube.getAxis(axisName)
            if (axis == null)
            {
                throw new IllegalArgumentException('Axis: ' + axisName + ', does not exist on n-cube: ' + ncube.name + ', appId: ' + ncube.applicationID)
            }
            return axis.findColumn(value)
        }

def getColumnExt =
        { String cubeName, String axisName, Comparable value ->
            NCube cube = NCubeManager.getCube(ncube.applicationID, cubeName)
            if (cube == null)
            {
                throw new IllegalArgumentException('n-cube: ' + cubeName + ' does not exist in application: ' + ncube.applicationID)
            }
            Axis axis = cube.getAxis(axisName)
            if (axis == null)
            {
                throw new IllegalArgumentException('Axis: ' + axisName + ', does not exist on n-cube: ' + cubeName + ', appId: ' + ncube.applicationID)
            }
            return axis.findColumn(value)
        }

def getAxis =
        { String axisName ->
            Axis axis = ncube.getAxis(axisName)
            if (axis == null)
            {
                throw new IllegalArgumentException('Axis: ' + axisName + ' does not exist on n-cube: ' + ncube.name + ', appId: ' + ncube.applicationID)
            }
            return axis
        }

def getAxisExt =
        { String cubeName, String axisName ->
            NCube cube = NCubeManager.getCube(ncube.applicationID, cubeName)
            if (cube == null)
            {
                throw new IllegalArgumentException('n-cube: ' + cubeName + ' does not exist in application: ' + ncube.applicationID)
            }
            Axis axis = cube.getAxis(axisName)
            if (axis == null)
            {
                throw new IllegalArgumentException('Axis: ' + axisName + ' does not exist on n-cube: ' + cubeName + ', appId: ' + ncube.applicationID);
            }
            return axis;
        }

def runRuleCube = getRelativeCubeCell =
        { cubeName, coord ->
            input.putAll(coord)
            def cube = NCubeManager.getCube(ncube.applicationID, cubeName)
            if (cube == null)
            {
                throw new IllegalArgumentException('n-cube: ' + cubeName + ' does not exist in application: ' + ncube.applicationID + ', attempting relative reference(@) to cell: ' + coord.toString())
            };
            return cube.getCell(input, output)
        }

def getRelativeCell =
        { coord ->
            input.putAll(coord)
            return ncube.getCell(input, output)
        }

def getFixedCubeCell =
        { name, coord ->
            def cube = NCubeManager.getCube(ncube.applicationID, name)
            if (cube == null)
            {
                throw new IllegalArgumentException('n-cube: ' + name + ' does not exist in application: ' + ncube.applicationID + ', attempting fixed ($) reference to cell: ' + coord.toString())
            };
            return cube.getCell(coord, output)
        }

def getFixedCell = { coord -> return ncube.getCell(coord, output) }

def ruleStop = { throw new RuleStop() }

def jump =
        { coord ->
            input.putAll(coord)
            throw new RuleJump(input)
        }

def now = { return System.nanoTime() }

def elapsedMillis = { long begin, long end -> return (end - begin) / 1000000.0 }
