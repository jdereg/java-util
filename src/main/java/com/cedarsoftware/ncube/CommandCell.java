package com.cedarsoftware.ncube;

import java.util.Map;
import java.util.Set;

/**
 * A 'CommandCell' represents an executable cell. NCube ships
 * with support for Groovy CommandCells that allow the NCube author
 * to write cells that contain Groovy expressions, Groovy methods, or
 * Groovy classes.  Javascript executable cells, as well as Java
 * executable cells can be added to NCube.  The CommandCell expects
 * to call the method "Object run(Map args)" on whatever object is assigned
 * to the runnableCode member.
 *
 * Subclasses must implement 'getShortType()' which returns a fixed String
 * so that the NCube JSON reader can figure out what type of CommandCell to
 * instantiate when the CommandCell is specified in NCube's simpleJson format.
 *
 * Subclasses must also implement 'getCubeNamesFromCommandText()' which returns
 * any NCube names the subclass may reference.  For example, if NCubes are referenced
 * in the Groovy code, the Groovy CommandCell subclasses would return any NCube
 * names they reference.  This is required so that when an NCube is loaded,
 * it can find referenced NCubes and load those as well.
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
public interface CommandCell extends Comparable<CommandCell>
{
    //  Items set during construction.
    String getCmd();
    String getUrl();

    //  Return the cached command or the cmd, whichever is available.
    Object getOperableCmd();

    void failOnErrors();

    //  Expands a url and sets it back on the url set during construction?
    void expandUrl(String url, Map ctx);

    // compile etc.
    void prepare(Object data, Map ctx);

    // execute and return
    Object execute(Object data, Map ctx);

    Object fetch(Map ctx);

    void cache(Object data);

    void getCubeNamesFromCommandText(Set<String> cubeNames);
    void getScopeKeys(Set<String> scopeKeys);
}
