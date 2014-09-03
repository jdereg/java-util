package com.cedarsoftware.ncube;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Created by kpartlow on 8/12/2014.
 */
public class NCubeTest
{
    private String name;
    private Map<String, Map<String, Object>> coordDescription;
    private Map<String, Object> expectedResultDescription;

    public NCubeTest(String name, Map<String, Map<String, Object>> coordDescription, Map<String, Object> expectedResultDescription) {
        this.name = name;
        this.coordDescription = coordDescription;
        this.expectedResultDescription = expectedResultDescription;
    }

    private Map<String, Object> buildCoordinate(Map<String, Map<String, Object>> descritpion) {
        Map<String, Object> coordinate = new LinkedHashMap<>();
        for (Map.Entry<String, Map<String, Object>> item : coordDescription.entrySet()) {
            coordinate.put(item.getKey(), parseValue(item.getValue()));
        }
        return coordinate;
    }

    private Object buildExpectedResult(Map<String, Object> description) {
        return parseValue(description);
    }

    public String getName() {
        return name;
    }

    public Map<String, Map<String, Object>> getCoordDescription() {
        return coordDescription;
    }

    public Map<String, Object> getExpectedResultDescription() {
        return expectedResultDescription;
    }

    public Object getExpectedResult() {
        return buildExpectedResult(this.expectedResultDescription);
    }

    public Map<String, Object> getCoordinate() {
        return  buildCoordinate(this.coordDescription);
    }

    public Object parseValue(Map<String, Object> map) {
        if (map == null) {
            return null;
        }
        Object value = map.get("value");
        String url = (String)map.get("url");
        String type = (String)map.get("type");

        return CellInfo.parseJsonValue(value, url, type, false);
    }
}
