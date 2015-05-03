package com.cedarsoftware.util;

/**
 * Useful System utilities for common tasks
 *
 * @author John DeRegnaucourt (john@cedarsoftware.com)
 *         <br>
 *         Copyright (c) Cedar Software LLC
 *         <br><br>
 *         Licensed under the Apache License, Version 2.0 (the "License");
 *         you may not use this file except in compliance with the License.
 *         You may obtain a copy of the License at
 *         <br><br>
 *         http://www.apache.org/licenses/LICENSE-2.0
 *         <br><br>
 *         Unless required by applicable law or agreed to in writing, software
 *         distributed under the License is distributed on an "AS IS" BASIS,
 *         WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *         See the License for the specific language governing permissions and
 *         limitations under the License.
 */
public final class SystemUtilities
{
    private SystemUtilities() {
    }

    /**
     * Fetch value from environment variable and if not set, then fetch from
     * System properties.  If neither available, return null.
     * @param var String key of variable to return
     */
    public static String getExternalVariable(String var)
    {
        String value = System.getProperty(var);
        if (StringUtilities.isEmpty(value))
        {
            value = System.getenv(var);
        }
        return StringUtilities.isEmpty(value) ? null : value;
    }
}
