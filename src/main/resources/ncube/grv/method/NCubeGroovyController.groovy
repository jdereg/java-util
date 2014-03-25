package ncube.grv.method

import ncube.grv.exp.NCubeGroovyExpression

import java.lang.reflect.Method;

/**
 * Base class for all GroovyExpression and GroovyMethod's within n-cube CommandCells.
 * @see com.cedarsoftware.ncube.GroovyBase
 * @author John DeRegnaucourt
 */
class NCubeGroovyController extends NCubeGroovyExpression
{
    NCubeGroovyController(){}

    def run(Map args) throws Exception
    {
        super.run(args);

        // Invoke the Groovy method named in the input Map at the key 'method'.
        Method methodToRun = getClass().getMethod(input.method, null);
        return methodToRun.invoke(this, null);
    }
}