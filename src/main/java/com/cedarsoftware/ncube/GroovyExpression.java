package com.cedarsoftware.ncube;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;

/**
 * This class is used to hold Groovy Expressions.  This means that
 * the code can start without any class or method signatures.  For
 * example, an expression could look like this:<pre>
 *    1) input.resource.KEY == 10
 *    2) $([BU:'agr',state:'OH') * 1.045
 *    3) return input.state == 'OH' ? 1.0 : 2.0
 *    4) output.result = 'answer computed'; return 1.4
 *
 * </pre>
 * Of course, Java syntax can be used, however, there are many nice
 * short-hands you can use if you know the Groovy language additions.
 * For example, Maps can be accessed like this (the following 3 are
 * equivalent): <pre>
 *    1) input.get('BU')
 *    2) input['BU']
 *    3) input.BU
 * </pre>
 * There are variables available to you to access in your expression
 * supplied by ncube.  <b>input</b> which is the input coordinate Map
 * that was used to get to the the cell containing the expression.
 * <b>output</b> is a Map that your expression can write to.  This allows
 * the program calling into the ncube to get multiple return values, with
 * possible structure to each one (a graph return).  <b>ncube</b> is the
 * current ncube of the cell containing the expression.  <b>ncubeMgr</b>
 * is a reference to the NCubeManager class so that you can access other
 * ncubes. <b>stack</b> which is a List of StackEntry's where element 0 is
 * the StackEntry for the currently executing cell.  Element 1 would be the
 * cell that called into the current cell (if it was during the same execution
 * cycle).  The StackEntry contains the name of the cube as one field, and
 * the coordinate that called into this cell.
 *
 * @author John DeRegnaucourt
 * Copyright (c) 2012-2015, John DeRegnaucourt.  All rights reserved.
 */
public class GroovyExpression extends GroovyBase
{
    private static final Logger LOG = LogManager.getLogger(GroovyExpression.class);

    //  Private constructor only for serialization.
    private GroovyExpression() { }
    public GroovyExpression(String cmd, String url)
    {
        super(cmd, url);
    }

    public String buildGroovy(String theirGroovy)
    {
        Matcher m = Regexes.hasClassDefPattern.matcher(theirGroovy);
        if (m.find())
        {   // If they include a class ... { in their source, then we do not add the 'apartment' around the content.
            return theirGroovy;
        }

        StringBuilder groovyCodeWithoutImportStatements = new StringBuilder();
        Set<String> imports = getImports(theirGroovy, groovyCodeWithoutImportStatements);
        StringBuilder groovy = new StringBuilder("package ncube.grv.exp\n");
        groovy.append("import com.cedarsoftware.ncube.ApplicationID\n");
        groovy.append("import com.cedarsoftware.ncube.Axis\n");
        groovy.append("import com.cedarsoftware.ncube.AxisType\n");
        groovy.append("import com.cedarsoftware.ncube.AxisValueType\n");
        groovy.append("import com.cedarsoftware.ncube.CellInfo\n");
        groovy.append("import com.cedarsoftware.ncube.CellTypes\n");
        groovy.append("import com.cedarsoftware.ncube.Column\n");
        groovy.append("import com.cedarsoftware.ncube.CommandCell\n");
        groovy.append("import com.cedarsoftware.ncube.Delta\n");
        groovy.append("import com.cedarsoftware.ncube.NCube\n");
        groovy.append("import com.cedarsoftware.ncube.NCubeInfoDto\n");
        groovy.append("import com.cedarsoftware.ncube.NCubeManager\n");
        groovy.append("import com.cedarsoftware.ncube.Range\n");
        groovy.append("import com.cedarsoftware.ncube.RangeSet\n");
        groovy.append("import com.cedarsoftware.ncube.RuleMetaKeys\n");
        groovy.append("import com.cedarsoftware.ncube.RuleInfo\n");
        groovy.append("import com.cedarsoftware.ncube.UrlCommandCell\n");
        groovy.append("import com.cedarsoftware.ncube.exception.*\n");
        groovy.append("import com.cedarsoftware.ncube.proximity.*\n");
        groovy.append("import com.cedarsoftware.util.*\n");

        for (String importLine : imports)
        {
            groovy.append(importLine);
            groovy.append('\n');
        }

        String className = "N_" + cmdHash;
        groovy.append("class ");
        groovy.append(className);
        groovy.append(" extends ncube.grv.exp.NCubeGroovyExpression\n{\n\tdef run()\n\t{\n\t");
        groovy.append(groovyCodeWithoutImportStatements);
        groovy.append("\n}\n}");
        return groovy.toString();
    }

    protected String getMethodToExecute(Map args)
    {
        return "run";
    }

    protected Method getRunMethod() throws NoSuchMethodException
    {
        return getRunnableCode().getMethod("run");
    }

    protected Object invokeRunMethod(Method runMethod, Object instance, Map args) throws Throwable
    {
        // If 'around' Advice has been added to n-cube, invoke it before calling Groovy expression's run() method
        NCube ncube = getNCube(args);
        Map input = getInput(args);
        Map output = getOutput(args);
        List<Advice> advices = ncube.getAdvices("run");
        for (Advice advice : advices)
        {
            if (!advice.before(runMethod, ncube, input, output))
            {
                return null;
            }
        }

        Throwable t = null;
        Object ret = null;

        try
        {
            ret = runMethod.invoke(instance);
        }
        catch (ThreadDeath e)
        {
            throw e;
        }
        catch (Throwable e)
        {   // Save exception
            t = e;
        }

        // If 'around' Advice has been added to n-cube, invoke it after calling Groovy expression's run() method
        int len = advices.size();
        for (int i = len - 1; i >= 0; i--)
        {
            Advice advice = advices.get(i);
            try
            {
                advice.after(runMethod, ncube, input, output, ret, t);  // pass exception (t) to advice (or null)
            }
            catch (Exception e)
            {
                LOG.error("An exception occurred calling advice: " + advice.getName() + " on method: " + runMethod.getName(), e);
            }
        }
        if (t == null)
        {
            return ret;
        }
        throw t;
    }
}
