package ncube.grv.exp.cdn;

import com.cedarsoftware.ncube.BinaryUrlCmd;
import com.cedarsoftware.ncube.StringUrlCmd;
import com.cedarsoftware.ncube.UrlCommandCell;
import ncube.grv.exp.NCubeGroovyExpression;

/**
 * Base class for all CdnRouter cubes default cells by type (HTML, css, js, etc.)
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
public class CdnDefaultHandler extends NCubeGroovyExpression
{
    public Object resolve(String extension)
    {
        return resolve(extension, true);
    }

    public Object resolve(final String extension, final boolean isString)
    {
        String axis = "content.name";
        synchronized (ncube.getName().intern())
        {
            String logicalFileName = (String) input.get(axis);
            ncube.addColumn(axis, logicalFileName);
            String url = extension + '/' + logicalFileName + '.' + extension;
            UrlCommandCell exp = isString ? new StringUrlCmd(url, false) : new BinaryUrlCmd(url, false);
            ncube.setCell(exp, input);
            return ncube.getCell(input);
        }
    }
}