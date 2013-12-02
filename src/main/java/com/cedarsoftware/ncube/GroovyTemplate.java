package com.cedarsoftware.ncube;

import groovy.text.SimpleTemplateEngine;
import groovy.text.Template;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * Process NCube template cells.  A template cell contains a String that may have both
 * NCube references to other NCubes using the {{ @otherCube([:]) }} format, AND, the
 * template may also contain Groovy template variables.  This class uses the Groovy
 * StandardTemplate which supports ${variable} and <%  %> replaceable tags.  The
 * 'variable' is the name of an NCube input coordinate key.
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
public class GroovyTemplate extends CommandCell
{
    private static final Pattern groovyRefCubeCellPattern = Pattern.compile("[{][{][\\s]*@([^(]+)[(]([^)]*)[)][\\s]*[}][}]");
    private static final Pattern groovyRefCellPattern = Pattern.compile("[{][{][\\s]*@[\\s]*[(]([^)]*)[)][\\s]*[}][}]");
    private Template resolvedTemplate;

    public GroovyTemplate(String cmd)
    {
        super(cmd);
    }

    public Set<String> getCubeNamesFromCommandText(String text)
    {
        Matcher m = groovyRefCubeCellPattern.matcher(text);
        Set<String> cubeNames = new HashSet<String>();
        while (m.find())
        {
            cubeNames.add(m.group(2));  // based on Regex pattern - if pattern changes, this could change
        }

        return cubeNames;
    }

    public Object runFinal(Map args)
    {
        // args.input, args.ncube are ALWAYS set by NCube before the execution gets here.
        try
        {
            if (resolvedTemplate == null)
            {
                SimpleTemplateEngine engine = new SimpleTemplateEngine();
                String template = performNCubeSubstitutions(args);
                resolvedTemplate = engine.createTemplate(template);
            }
            // Do normal Groovy substitutions.
            Map coord = (Map) args.get("input");
            return resolvedTemplate.make(coord).toString();
        }
        catch (Exception e)
        {
            NCube ncube = (NCube) args.get("ncube");
            String errorMsg = "Error setting up Groovy template, NCube '" + ncube.getName() + "'";
            setCompileErrorMsg(errorMsg + ", " + e.getMessage());
            throw new RuntimeException(errorMsg, e);
        }
    }

    private String performNCubeSubstitutions(Map args)
    {
        NCube ncube = (NCube) args.get("ncube");
        String inputStr = getCmd();
        Matcher m = groovyRefCellPattern.matcher(inputStr);
        StringBuilder s = new StringBuilder(getCmd());
        StringBuilder newStr = new StringBuilder();
        int last = 0;

        // 1. Walk through all of {{ @ref() }} and execute each, splicing in the result into the cmd text.
        // 2. Once all the substitutions are performed, the calling code will do the normal Groovy substitutions.
        while (m.find())
        {
            newStr.append(inputStr.substring(last, m.start()));

            String snippet = "@(" + m.group(1) + ")";
            GroovyExpression exp = new GroovyExpression(snippet);
            s.setLength(0);
            exp.buildGroovy(s, snippet, ncube.getName());
            Object ret = exp.run(args);

            newStr.append(ret == null ? "null" : ret.toString());
            last = m.end();
        }

        newStr.append(inputStr.substring(last));
        return newStr.toString();
    }
}
