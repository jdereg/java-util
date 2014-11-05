import com.cedarsoftware.ncube.NCubeManager
import com.cedarsoftware.ncube.exception.RuleJump
import com.cedarsoftware.ncube.exception.RuleStop

def getRelativeCubeCell =
        { name, coord ->
            input.putAll(coord);
            def cube = NCubeManager.getCube(ncube.getApplicationID(), name)
            if (cube == null)
            {
                throw new IllegalArgumentException('NCube: ' + name + ' is not loaded, attempting relative (@) reference to cell: ' + coord.toString());
            };
            return cube.getCell(input, output);
        };

def getRelativeCell =
        { coord ->
            input.putAll(coord);
            return ncube.getCell(input, output);
        };

def getFixedCubeCell =
        { name, coord ->
            def cube = NCubeManager.getCube(ncube.getApplicationID(), name)
            if (cube == null)
            {
                throw new IllegalArgumentException('NCube: ' + name + ' is not loaded, attempting fixed ($) reference to cell: ' + coord.toString());
            };
            return cube.getCell(input, output)
        };

def getFixedCell =
        { coord ->
            return ncube.getCell(coord, output);
        }

def ruleStop = { throw new RuleStop(); }

def jump = { throw new RuleJump(); }
