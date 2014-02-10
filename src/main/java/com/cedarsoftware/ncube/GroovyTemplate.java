package com.cedarsoftware.ncube;

import com.cedarsoftware.util.IOUtilities;
import groovy.text.SimpleTemplateEngine;
import groovy.text.Template;

import java.io.InputStream;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Process NCube template cells.  A template cell contains a String that may have both
 * NCube references to other NCubes within the <% %> or ${ } sections, AND, the
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
public class GroovyTemplate extends UrlCommandCell
{
    private static final Pattern scripletPattern = Pattern.compile("<%(.*?)%>");
    private static final Pattern velocityPattern = Pattern.compile("[$][{](.*?)[}]");
    private Template resolvedTemplate;

    public GroovyTemplate(String cmd)
    {
        super(cmd, true);
    }

    public void getCubeNamesFromCommandText(final Set<String> cubeNames)
    {
        Matcher m = scripletPattern.matcher(getCmd());

        while (m.find())
        {
            GroovyBase.getCubeNamesFromText(cubeNames, m.group(1));
        }

        m = velocityPattern.matcher(getCmd());

        while (m.find())
        {
            GroovyBase.getCubeNamesFromText(cubeNames, m.group(1));
        }
    }

    /**
     * Go through all <%  %> and ${  }.  For each one of these sections,
     * find all 'input.variableName' occurrences and add 'variableName' to
     * the passed in Set.
     * @param scopeKeys Set to add required scope (key) elements to.
     */
    public void getScopeKeys(Set<String> scopeKeys)
    {
        Matcher m = scripletPattern.matcher(getCmd());  // <%  %>

        while (m.find())
        {
            Matcher m1 = inputVar.matcher(m.group(1));
            while (m1.find())
            {
                scopeKeys.add(m1.group(2));
            }
        }

        m = velocityPattern.matcher(getCmd());          // ${   }

        while (m.find())
        {
            Matcher m1 = inputVar.matcher(m.group(1));
            while (m1.find())
            {
                scopeKeys.add(m1.group(2));
            }
        }
    }

    protected void fetch()
    {

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

                InputStream in = GroovyBase.class.getClassLoader().getResourceAsStream("NCubeTemplateClosures");
                String groovyClosures = new String(IOUtilities.inputStreamToBytes(in));

                cmd = "<% " + groovyClosures + " %>" + cmd;

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
