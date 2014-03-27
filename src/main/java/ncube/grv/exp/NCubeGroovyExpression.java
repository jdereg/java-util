package ncube.grv.exp;

import com.cedarsoftware.ncube.NCube;
import com.cedarsoftware.ncube.NCubeManager;
import com.cedarsoftware.ncube.exception.RuleStop;

import java.util.Map;

/**
 * Base class for all GroovyExpression and GroovyMethod's within n-cube CommandCells.
 * @see com.cedarsoftware.ncube.GroovyBase
 * @author John DeRegnaucourt
 */
public class NCubeGroovyExpression
{
    protected Map input;
    protected Map output;
    protected Object stack;
    protected NCube ncube;

    public Object run(Map args, String signature) throws Exception
    {
        input = (Map) args.get("input");
        output = (Map) args.get("output");
        stack = args.get("stack");
        ncube = (NCube) args.get("ncube");
        return null;
    }

    public Object getFixedCell(Map coord)
    {
        return ncube.getCell(coord, output);
    }

    public Object getFixedCubeCell(String name, Map coord)
    {
        NCube cube = NCubeManager.getCube(name, ncube.getVersion());
        if (cube == null)
        {
            throw new IllegalArgumentException("NCube '" + name + "' not loaded into NCubeManager, attempting fixed ($) reference to cell: " + coord.toString());
        }
        return cube.getCell(coord, output);
    }

    public Object getRelativeCell(Map coord)
    {
        input.putAll(coord);
        return ncube.getCell(input, output);
    }

    public Object getRelativeCubeCell(String name, Map coord)
    {
        input.putAll(coord);
        NCube cube = NCubeManager.getCube(name, ncube.getVersion());
        if (cube == null)
        {
            throw new IllegalArgumentException("NCube '" + name + "' not loaded into NCubeManager, attempting relative (@) reference to cell: " + coord.toString());
        }
        return cube.getCell(input, output);
    }

    public void ruleStop()
    {
        throw new RuleStop();
    }
}