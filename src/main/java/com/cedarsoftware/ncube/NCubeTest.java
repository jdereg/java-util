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
    private StringValuePair<CellInfo>[] coord;
    private CellInfo[] expected;

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
            actuals.put((String)item.getKey(), ((CellInfo)item.getValue()).recreate());
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
