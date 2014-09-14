package com.cedarsoftware.ncube;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Created by kpartlow on 8/12/2014.
 */
public class NCubeTest
{
    private String name;
    private Map<String, CellInfo> coord;
    private Map<String, CellInfo> expected;

    public NCubeTest(String name, Map<String, CellInfo> coord, Map<String, CellInfo> expected) {
        this.name = name;
        this.coord = coord;
        this.expected = expected;
    }

    /*
    private Map<String, Object> buildCoordinate(Map<String, Map<String, Object>> descritpion) {
        Map<String, Object> coordinate = new LinkedHashMap<>();
        for (Map.Entry<String, Map<String, Object>> item : coordDescription.entrySet()) {
            coordinate.put(item.getKey(), parseValue(item.getValue()));
        }
        return coordinate;
    }

    */

    public String getName() {
        return name;
    }

    public Map<String, CellInfo> getCoord() {
        return this.coord;
    }

    public Map<String, Object> createCoord() {
        Map<String, Object> actuals = new LinkedHashMap<>();
        for (Map.Entry<String, CellInfo> item : this.coord.entrySet()) {
            actuals.put(item.getKey(), item.getValue().recreate());
        }
        return actuals;
    }

    public Map<String, CellInfo> getExpected() {
        return this.expected;
    }

    public Map<String, Object> createExpected() {
        Map<String, Object> actuals = new LinkedHashMap<>();
        for (Map.Entry<String, CellInfo> item : this.expected.entrySet()) {
            actuals.put(item.getKey(), item.getValue().recreate());
        }
        return actuals;
    }


    /*
    public Object parseValue(Map<String, Object> map) {
        if (map == null) {
            return null;
        }
        Object value = map.get("value");
        String url = (String)map.get("url");
        String type = (String)map.get("type");

        return CellInfo.parseJsonValue(value, url, type, false);
    }
    */
}
