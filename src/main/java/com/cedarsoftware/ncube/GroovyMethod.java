package com.cedarsoftware.ncube;

import java.lang.reflect.Method;
import java.util.Map;

/**
 * This class is used to hold Groovy Programs.  The code must start
 * with method declarations.  The outer class wrapper is built for
 * you.  You must at least the following method:<pre>
 *     Object run(Map args)
 *     {
 *         // your code here
 *     }
 *  </pre>
 * You can have additional methods, as well as write classes in
 * between the methods.  The code in the methods can be written as
 * Java, or if you wish to use the excellent Groovy short-cuts, go
 * for it.  Check out the Groovy short-hand notation for Map access
 * which is described below.For example, Maps can be accessed like
 * this (the following 3 are equivalent): <pre>
 *    1) input.get('BU')
 *    2) input['BU']
 *    3) input.BU
 * </pre>
 * There are variables available to you to access in your expression
 * supplied by ncube.  The passed in Map contains the following keys:
 * <b>input</b> which is the input coordinate Map that was used to get to
 * the the cell containing the expression. <b>output</b> is a Map that your
 * expression can write to.  This allows the program calling into the ncube
 * to get multiple return values, with possible structure to each one (a
 * graph return).  <b>ncube</b> is the current ncube of the cell containing
 * the expression.  <b>ncubeMgr</b> is a reference to the NCubeManager class
 * so that you can access other ncubes. <b>stack</b> which is a List of
 * StackEntry's where element 0 is the StackEntry for the currently executing
 * cell.  Element 1 would be the cell that called into the current cell (if it
 * was during the same execution cycle).  The StackEntry contains the name
 * of the cube as one field, and the coordinate that called into this cell.
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
public class GroovyMethod extends GroovyBase
{
    public GroovyMethod(String cmd, String url)
    {
        super(cmd, url, true);
    }

    public GroovyMethod(String cmd, String url, boolean cache)
    {
        super(cmd, url, cache);
    }

    public String buildGroovy(String theirGroovy, String cubeName, String cmdHash)
    {
        return theirGroovy;
    }

    protected String getMethodToExecute(Map args)
    {
        Map input = (Map) args.get("input");
        return (String)input.get("method");
    }

    protected Method getRunMethod() throws NoSuchMethodException
    {
        return getRunnableCode().getMethod("run", String.class);
    }

    protected Object invokeRunMethod(Method runMethod, Object instance, Map args, String cmdHash) throws Exception
    {
        return runMethod.invoke(instance, cmdHash);
    }
}
