package com.cedarsoftware.util;

/**
 * Useful System utilities for common tasks
 *
 * @author John DeRegnaucourt (jdereg@gmail.com)
 *         <br/>
 *         Copyright (c) John DeRegnaucourt
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
public class SystemUtilities
{
    /**
     * Fetch value from environment variable and if not set, then fetch from
     * System properties.  If neither available, return null.
     * @param var String key of variable to return
     */
    private static String getExternalVariable(String var)
    {
        String variable = System.getenv().get(var);
        if (StringUtilities.isEmpty(variable))
        {
            variable = System.getProperty(var);
        }
        if (StringUtilities.isEmpty(variable))
        {
            variable = null;
        }
        return variable;
    }
}
