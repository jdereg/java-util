package com.cedarsoftware.ncube;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by kpartlow on 8/12/2014.
 */
public class NCubeTest
{
    private String name;
    private List<StringValuePair<CellInfo>> coord;
    private List<CellInfo> expected;

    public NCubeTest(String name, List<StringValuePair<CellInfo>> coord, List<CellInfo> expected) {
        this.name = name;
        this.coord = coord;
        this.expected = expected;
    }

    public String getName() {
        return name;
    }

    public List<StringValuePair<CellInfo>> getCoord() {
        return this.coord;
    }

    public Map<String, Object> createCoord() {
        Map<String, Object> actuals = new LinkedHashMap<>();
        for (StringValuePair item : this.coord) {
            actuals.put((String)item.getKey(), ((CellInfo)item.getValue()).recreate());
        }
        return actuals;
    }

    public List<CellInfo> getAssertions() {
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
