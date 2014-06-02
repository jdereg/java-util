package com.cedarsoftware.ncube.util;

import com.cedarsoftware.ncube.CommandCell;
import com.cedarsoftware.ncube.UrlCommandCell;
import com.cedarsoftware.ncube.executor.DefaultExecutor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;

/**
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
public class CdnUrlExecutor extends DefaultExecutor
{
    private HttpServletRequest request;
    private HttpServletResponse response;

    CdnUrlExecutor(HttpServletRequest request, HttpServletResponse response)
    {
        this.request = request;
        this.response = response;
    }

    public Object executeCommand(CommandCell c, Map<String, Object> ctx)
    {
        //if (c instanceof UrlCommandCell)
        //{
            //executeUrlCommand((UrlCommandCell)c, ctx);
        //}
        Object o = super.executeCommand(c, ctx);

        try
        {
            response.getOutputStream().write(o.toString().getBytes());
        } catch (Exception e) {
            // log this.
        }
        return o;
    }

    // TODO: Copy HTTP Response headers from Apache
    protected Object executeUrlCommand(UrlCommandCell c, Map<String, Object> ctx) {

        // default does:
        //super.exec(c, ctx);
        return null;
    }
}

