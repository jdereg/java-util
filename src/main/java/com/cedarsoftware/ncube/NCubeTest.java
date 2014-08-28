package com.cedarsoftware.ncube;

import com.cedarsoftware.util.StringUtilities;

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

    public NCubeTest(String name, Map<String, Map<String, Object>> coordDescription, Map<String, Object> expectedResultDescription) {
        _name = name;
        _coordDescription = coordDescription;
        _expectedResultDescription = expectedResultDescription;
    }

    public String getName() {
        return _name;
    }

    public Map<String, Map<String, Object>> getCoordDescription() {
        return _coordDescription;
    }

    public Map<String, Object> getResultDescription() {
        return _expectedResultDescription;
    }

    public Map<String, Object> getCoordinate() {
        Map<String, Object> coordinate = new LinkedHashMap<>();
        for (Map.Entry<String, Map<String, Object>> item : _coordDescription.entrySet()) {
            coordinate.put(item.getKey(), parseValue(item.getValue()));
        }
        return coordinate;
    }

    public Object getExpectedResult() {
        return parseValue(_expectedResultDescription);
    }

    public Object parseValue(Map<String, Object> map) {
        if (map == null) {
            return null;
        }
        Object value = map.get("value");
        String url = (String)map.get("url");
        String type = (String)map.get("type");

        if (value == null && StringUtilities.isEmpty(url)) {
            throw new IllegalArgumentException("Test Items must have either a url or a type");
        }
        return NCube.parseJsonValue(value, url, type, false);
    }
}
