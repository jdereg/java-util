package com.cedarsoftware.ncube;

import groovy.text.SimpleTemplateEngine;
import groovy.text.Template;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
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
    private static final Pattern scripletPattern = Pattern.compile("<%(.*?)%>");
    private static final Pattern velocityPattern = Pattern.compile("[$][{](.*?)[}]");

    private Template resolvedTemplate;

    public GroovyTemplate(String cmd)
    {
        super(cmd);
    }

    public Set<String> getCubeNamesFromCommandText(final String text)
    {
        // TODO: 1) Return any referenced NCube's or template variables (need API change for latter)
        // TODO: 2) Groovy XML Template & GStringTemplate support
        // TODO: 3) Import statement support in Groovy methods
        // TODO: 4) url: in simpleJsonFormat
        return new LinkedHashSet<String>();
    }

    public Object runFinal(final Map args)
    {
        // args.input, args.output, args.ncube, args.ncubeMgr, and args.stack,
        // are ALWAYS set by NCube before the execution gets here.
        try
        {
            if (resolvedTemplate == null)
            {
                String cmd = getCmd();
                // Expand code : perform <% @()  $() %> and ${ @()   $() } substitutions before passing to template engine.
                cmd = replaceScriptletNCubeRefs(cmd, scripletPattern, "<%", "%>");
                cmd = replaceScriptletNCubeRefs(cmd, velocityPattern, "${", "}");
                cmd = "<% def getRelativeCubeCell = { name, coord -> input.putAll(coord); if (ncubeMgr.getCube(name) == null) { throw new IllegalArgumentException('NCube: ' + name + ' is not loaded, attempting relative (@) reference to cell: ' + coord.toString()); }; return ncubeMgr.getCube(name).getCell(input, output); }; " +
                      "   def getRelativeCell = { coord -> input.putAll(coord); return ncube.getCell(input, output); }; " +
                      "   def getFixedCell = { name, coord -> if (ncubeMgr.getCube(name) == null) { throw new IllegalArgumentException('NCube: ' + name + ' is not loaded, attempting fixed ($) reference to cell: ' + coord.toString()); }; return ncubeMgr.getCube(name).getCell(input, output); }; %>" + cmd;

                // Create Groovy Standard Template
                SimpleTemplateEngine engine = new SimpleTemplateEngine();
                resolvedTemplate = engine.createTemplate(cmd);
            }

            // Do normal Groovy substitutions.
            return resolvedTemplate.make(args).toString();
        }
        catch (Exception e)
        {
            NCube ncube = (NCube) args.get("ncube");
            String errorMsg = "Error setting up Groovy template, NCube '" + ncube.getName() + "'";
            setCompileErrorMsg(errorMsg + ", " + e.getMessage());
            throw new RuntimeException(errorMsg, e);
        }
    }

    private String replaceScriptletNCubeRefs(final String template, final Pattern pattern, final String prefix, final String suffix)
    {
        Matcher m = pattern.matcher(template);
        StringBuilder newStr = new StringBuilder();
        int last = 0;

        while (m.find())
        {
            newStr.append(template.substring(last, m.start()));
            String inner = GroovyBase.expandNCubeShortCuts(m.group(1));
            newStr.append(prefix);
            newStr.append(inner);
            newStr.append(suffix);
            last = m.end();
        }

        newStr.append(template.substring(last));
        return newStr.toString();
    }
}
