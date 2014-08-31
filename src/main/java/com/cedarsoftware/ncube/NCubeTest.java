package com.cedarsoftware.ncube;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Created by kpartlow on 8/12/2014.
 */
public class NCubeTest
{
    private String _name;
    private Map<String, Map<String, Object>> _coordDescription;
    private Map<String, Object> _expectedResultDescription;

    // generated items.
    private Map<String, Object> _coordinate;
    private Object _expectedResult;

    public NCubeTest(String name, Map<String, Map<String, Object>> coordDescription, Map<String, Object> expectedResultDescription) {
        _name = name;
        _coordDescription = coordDescription;
        _expectedResultDescription = expectedResultDescription;
        _coordinate = buildCoordinate(_coordDescription);
        _expectedResult = buildExpectedResult(_expectedResultDescription);
    }

    private Map<String, Object> buildCoordinate(Map<String, Map<String, Object>> descritpion) {
        Map<String, Object> coordinate = new LinkedHashMap<>();
        for (Map.Entry<String, Map<String, Object>> item : _coordDescription.entrySet()) {
            coordinate.put(item.getKey(), parseValue(item.getValue()));
        }
        return coordinate;
    }

    private Object buildExpectedResult(Map<String, Object> description) {
        return parseValue(description);
    }

    public String getName() {
        return _name;
    }

    public Map<String, Map<String, Object>> getCoordDescription() {
        return _coordDescription;
    }

    public Map<String, Object> getExpectedResultDescription() {
        return _expectedResultDescription;
    }

    public Object getExpectedResult() {
        return _expectedResult;
    }

    public Map<String, Object> getCoordinate() {
        return _coordinate;
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
