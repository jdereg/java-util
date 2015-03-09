package com.cedarsoftware.ncube.exception;

/**
 * Execution thrown to stop any further rule execution along a rule axis. When
 * this exception is thrown, no further axis binding takes place, and the
 * execution process ends.  This is true whether a condition column or an
 * n-cube cell throws this exception.  What cells have been process, will be returned,
 * and all changes made (to the output Map) will be kept to this point.
 *
 * This exception only kills of one 'stack frame'.  So if an n-cube calls another
 * n-cube, and the called n-cube throws a rule stop, only the inner n-cubes
 * execution is ended.  The execution is returned to the outer (calling) n-cube
 * and picks up as it normally does on a call (join) to another n-cube.
 *
 * @author John DeRegnaucourt (jdereg@gmail.com)
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
public class RuleStop extends RuntimeException
{
}
