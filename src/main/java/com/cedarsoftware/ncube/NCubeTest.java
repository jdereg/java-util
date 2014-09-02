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

    // generated items.
    private Map<String, Object> coord;
    private Object expectedResult;

    public NCubeTest(String name, Map<String, Map<String, Object>> coordDescription, Map<String, Object> expectedResultDescription) {
        this.name = name;
        this.coordDescription = coordDescription;
        this.expectedResultDescription = expectedResultDescription;
        coord = buildCoordinate(this.coordDescription);
        expectedResult = buildExpectedResult(this.expectedResultDescription);
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
        return expectedResult;
    }

    public Map<String, Object> getCoordinate() {
        return coord;
    }

    public Object parseValue(Map<String, Object> map) {
        if (map == null) {
            return null;
        }
        Object value = map.get("value");
        String url = (String)map.get("url");
        String type = (String)map.get("type");

        //if (value == null && StringUtilities.isEmpty(url)) {
        //    throw new IllegalArgumentException("Test Items must have either a url or a value");
        //}
        return CellInfo.parseJsonValue(value, url, type, false);
    }
}
