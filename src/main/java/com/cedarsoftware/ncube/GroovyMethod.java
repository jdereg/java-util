package com.cedarsoftware.ncube;

import com.cedarsoftware.util.UniqueIdGenerator;

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
    public GroovyMethod(String cmd)
    {
        super(cmd);
    }

    public String buildGroovy(String theirGroovy, String cubeName)
    {
        StringBuilder groovy = new StringBuilder();
        String className = "ncGrvMethod" + fixClassName(cubeName) + UniqueIdGenerator.getUniqueId();
        groovy.append("class ");
        groovy.append(className);
        groovy.append("\n{\n");
        groovy.append("  def input;\n");
        groovy.append("  def output;\n");
        groovy.append("  def stack;\n");
        groovy.append("  def ncube;\n");
        groovy.append("  def ncubeMgr;\n  ");
        groovy.append(className);
        groovy.append("(Map args)\n{\n");
        groovy.append("  input=args.input;\n");
        groovy.append("  output=args.output;\n");
        groovy.append("  stack=args.stack;\n");
        groovy.append("  ncube=args.ncube;\n");
        groovy.append("  ncubeMgr=args.ncubeMgr;\n  ");
        groovy.append("}\n\n");
        groovy.append("def getFixedCell(String name, Map coord)\n");
        groovy.append("{\n");
        groovy.append("  if (ncubeMgr.getCube(name) == null)\n");
        groovy.append("  {\n");
        groovy.append("    throw new IllegalArgumentException(\"NCube '\" + ncube + \"' not loaded into NCubeManager.\");\n");
        groovy.append("  }\n");
        groovy.append("  return ncubeMgr.getCube(name).getCell(coord, output);\n");
        groovy.append("}\n\n");
        groovy.append("def getRelativeCell(Map coord)\n");
        groovy.append("{\n");
        groovy.append("  input.putAll(coord);\n");
        groovy.append("  return ncube.getCell(input, output);\n");
        groovy.append("}\n\n");
        groovy.append("def getRelativeCubeCell(String name, Map coord)\n");
        groovy.append("{\n");
        groovy.append("  input.putAll(coord);\n");
        groovy.append("  if (ncubeMgr.getCube(name) == null)\n");
        groovy.append("  {\n");
        groovy.append("    throw new IllegalArgumentException(\"NCube '\" + ncube + \"' not loaded into NCubeManager.\");\n");
        groovy.append("  }\n");
        groovy.append("  return ncubeMgr.getCube(name).getCell(input, output);\n");
        groovy.append("}\n\n");
        groovy.append(theirGroovy);
        groovy.append("\n}");
        System.out.println("groovy.method = " + groovy);
        return groovy.toString();
    }
}
