package com.cedarsoftware.ncube;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class is used to hold Groovy Programs (Groovy Classes).
 * Your class name can optionally be surrounded with ~ as in
 * class ~MyGroovyClass~ in the declaration, indicating you want
 * the class name to be made unique.
 *
 * There are variables available to you to access in your program
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
public class GroovyProgram extends GroovyBase
{
    private static final Pattern classNamePattern = Pattern.compile("class[ \\n\\r\\t]+([a-zA-Z0-9_]+)",Pattern.CASE_INSENSITIVE);
    private static final Pattern startBracePattern = Pattern.compile("\\{");

    public GroovyProgram(String cmd)
    {
        super(cmd);
    }

    public void buildGroovy(StringBuilder groovy, String theirGroovy, String cubeName)
    {
        Matcher m = classNamePattern.matcher(theirGroovy.toString());
        if (!m.find() || m.groupCount() < 1)
        {
            throw new IllegalArgumentException("Must have 'class <className>' in your groovy class definition: " + theirGroovy);
        }
        String className = m.group(1);

        Object[] pieces = startBracePattern.split(theirGroovy.toString(), 2);
        if (pieces.length != 2)
        {
            throw new IllegalArgumentException("Must have 'class <className> { // your code }' in your groovy class definition: " + theirGroovy);
        }

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
        groovy.append(pieces[1]);
    }
}
