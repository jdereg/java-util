package com.cedarsoftware.ncube;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Class used to carry the NCube meta-information
 * to the client.
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

public class NCubeTest
{
    private final String name;
    private final StringValuePair<CellInfo>[] coord;
    private final CellInfo[] expected;

    public NCubeTest(String name, StringValuePair<CellInfo>[] coord, CellInfo[] expected) {
        this.name = name;
        this.coord = coord;
        this.expected = expected;
    }

    public String getName() {
        return name;
    }

    public StringValuePair<CellInfo>[] getCoord() {
        return this.coord;
    }

    public Map<String, Object> createCoord() {
        Map<String, Object> actuals = new LinkedHashMap<>();
        for (StringValuePair item : this.coord) {
            actuals.put(item.getKey(), ((CellInfo)item.getValue()).recreate());
        }
        return actuals;
    }

    public CellInfo[] getAssertions() {
        return this.expected;
    }

    public List<GroovyExpression> createAssertions() {
        List<GroovyExpression> actuals = new ArrayList<>();
        for (CellInfo item : this.expected) {
            actuals.add((GroovyExpression) item.recreate());
        }
        return actuals;
    }
}
