package ncube.grv.exp

import com.cedarsoftware.ncube.exception.RuleStop

/**
 * Base class for all GroovyExpression and GroovyMethod's within n-cube CommandCells.
 * @see com.cedarsoftware.ncube.GroovyBase
 * @author John DeRegnaucourt
 */
class NCubeGroovyExpression
{
    def input;
    def output;
    def stack;
    def ncube;
    def ncubeMgr;

    def run(Map args, String signature)
    {
        input = args.input;
        output = args.output;
        stack = args.stack;
        ncube = args.ncube;
        ncubeMgr = args.ncubeMgr;
    }

    def getFixedCell(Map coord)
    {
        return ncube.getCell(coord, output);
    }

    def getFixedCubeCell(String name, Map coord)
    {
        def cube = ncubeMgr.getCube(name, ncube.getVersion())
        if (cube == null)
        {
            throw new IllegalArgumentException('NCube "' + name + '" not loaded into NCubeManager, attempting fixed ($) reference to cell: ' + coord.toString());
        }
        return cube.getCell(coord, output);
    }

    def getRelativeCell(Map coord)
    {
        input.putAll(coord);
        return ncube.getCell(input, output);
    }

    def getRelativeCubeCell(String name, Map coord)
    {
        input.putAll(coord);
        def cube = ncubeMgr.getCube(name, ncube.getVersion())
        if (cube == null)
        {
            throw new IllegalArgumentException('NCube "' + name + '" not loaded into NCubeManager, attempting relative (@) reference to cell: ' + coord.toString());
        }
        return cube.getCell(input, output);
    }

    void ruleStop()
    {
        throw new RuleStop();
    }
}