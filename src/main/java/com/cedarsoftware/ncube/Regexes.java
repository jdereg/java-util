package com.cedarsoftware.ncube;

import java.util.regex.Pattern;

/**
 * Regular Expressions used throughout n-cube implementation.
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
public interface Regexes
{
    Pattern importPattern = Pattern.compile("import[\\s]+[^;]+?;");
    Pattern inputVar = Pattern.compile("([^a-zA-Z0-9_.]|^)input[?]?[.]([a-zA-Z0-9_]+)", Pattern.CASE_INSENSITIVE);

    Pattern scripletPattern = Pattern.compile("<%(.*?)%>");
    Pattern velocityPattern = Pattern.compile("[$][{](.*?)[}]");

    Pattern validTenantName = Pattern.compile("^[0-9A-Za-z-]+$");
    Pattern validVersion = Pattern.compile("^\\d+\\.\\d+\\.\\d+$");
    Pattern validCubeName = Pattern.compile("[" + NCube.validCubeNameChars + "]+");

    Pattern valid2Doubles = Pattern.compile("^\\s*(\\-?\\d+(?:\\.\\d+)?)\\s*,\\s*(\\-?\\d+(?:\\.\\d+)?)\\s*$");
    Pattern valid3Doubles = Pattern.compile("^\\s*(\\-?\\d+(?:\\.\\d+)?)\\s*,\\s*(\\-?\\d+(?:\\.\\d+)?)\\s*,\\s*(\\-?\\d+(?:\\.\\d+)?)\\s*$");

    Pattern groovyAbsRefCubeCellPattern =  Pattern.compile("([^a-zA-Z0-9_]|^)[$][\\s]*([" + NCube.validCubeNameChars + "]+)[\\s]*[(]([^)]+)[)]");
    Pattern groovyAbsRefCubeCellPatternA = Pattern.compile("([^a-zA-Z0-9_]|^)[$][\\s]*([" + NCube.validCubeNameChars + "]+)[\\s]*(\\[.*?:.*?\\])");
    Pattern groovyAbsRefCellPattern =  Pattern.compile("([^a-zA-Z0-9_]|^)[$][\\s]*[(]([^)]+)[)]");
    Pattern groovyAbsRefCellPatternA = Pattern.compile("([^a-zA-Z0-9_]|^)[$][\\s]*(\\[[^\\]]*\\])");
    Pattern groovyRelRefCubeCellPattern =  Pattern.compile("([^a-zA-Z0-9_$]|^)@[\\s]*([" + NCube.validCubeNameChars + "]+)[\\s]*[(]([^)]+)[)]");
    Pattern groovyRelRefCubeCellPatternA = Pattern.compile("([^a-zA-Z0-9_$]|^)@[\\s]*([" + NCube.validCubeNameChars + "]+)[\\s]*(\\[.*?:.*?\\])");
    Pattern groovyRelRefCellPattern =  Pattern.compile("([^a-zA-Z0-9_$]|^)@[\\s]*[(]([^)]+)[)]");
    Pattern groovyRelRefCellPatternA = Pattern.compile("([^a-zA-Z0-9_$]|^)@[\\s]*(\\[.*?:.*?\\])");
    Pattern groovyExplicitCubeRefPattern = Pattern.compile("([^a-zA-Z0-9_$]|^)NCubeManager[.]getCube[\\s]*[(][\\s]*['\"]([" + NCube.validCubeNameChars + "]+)['\"][\\s]*[)]");
    Pattern groovyExplicitRunRulePattern = Pattern.compile("([^a-zA-Z0-9_$]|^)runRuleCube[\\s]*[(][\\s]*['\"]([" + NCube.validCubeNameChars + "]+)['\"].*?[)]");
    Pattern groovyExplicitJumpPattern = Pattern.compile("([^a-zA-Z0-9_$]|^)jump[\\s]*[(][\\s]*['\"]([" + NCube.validCubeNameChars + "]+)['\"].*?[)]");

    Pattern cdnUrlPattern = Pattern.compile("^\\/dyn\\/([^\\/]+)\\/(.*)$");

    Pattern hasClassDefPattern = Pattern.compile("class([a-zA-Z _0-9$\\s\\.]+)\\{");
}
