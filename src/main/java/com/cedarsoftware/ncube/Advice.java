package com.cedarsoftware.ncube;

import java.lang.reflect.Method;
import java.util.Map;

/**
 * Implement this interface and passed a reference of this implementation to n-cube's
 * adviceHandler() API, and n-cube will call the handler before and after each
 * expression or method.
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
public interface Advice
{
    /**
     * What is the name of this advice?  "log", "perf", etc.  Names can be
     * whatever you want.
     */
    String getName();

    /**
     * This method is called before an n-cube expression or controller method is called.  This is only
     * used if the 'advice' handler is set on n-cube.
     * @param method Method being called
     * @param ncube NCube that this advice is being called from
     * @param input Map containing the 'input' coordinate to getCell() type APIs
     * @param output Map containing the 'output' coordinate to getCell() type APIs
     * @return true if you want the method to be called (continue) false if you want execute to not go further (don't
     * call the method).
     */
    boolean before(Method method, NCube ncube, Map input, Map output);

    /**
     * This method is called after a n-cube expression or controller method is called.  This is
     * only used if the 'advice' handler is set on n-cube.
     * @param method Method being called
     * @param ncube NCube that this advice is being called from
     * @param input Map containing the 'input' coordinate to getCell() type APIs
     * @param output Map containing the 'output' coordinate to getCell() type APIs
     * @param returnValue Object return value of the method that was called.
     */
    void after(Method method, NCube ncube, Map input, Map output, Object returnValue, Throwable t);
}
