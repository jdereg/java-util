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
public enum AxisType
{
    DISCRETE,       // Single Comparable
    RANGE,          // Deprecated, use SET
    SET,            // Comparable, Comparable, Range, Comparable, etc.
    NEAREST,        // Choose column closest to passed in value (long, date, big decimal, string, comparable + distance)
    RULE            // Rule axis, where each column is an expression (condition) and when true, the resolved cell is fired.
}
