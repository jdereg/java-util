package com.cedarsoftware.ncube;

/**
 package com.cedarsoftware.ncube;

 /**
 * This class defines allowable n-cube axis types.
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
public enum RuleMetaKeys
{
    RULE_STOP,          // RuleStop forcefully called (within condition or cell)
    NUM_RESOLVED_CELLS, // Number of cells resolved when processing rule cube
    RULES_EXECUTED      // Map of condition names (or IDs if no name for conditions) to associated statements return value
}
