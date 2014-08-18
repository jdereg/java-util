package com.cedarsoftware.ncube;

import java.util.Map;

/**
 * Created by kpartlow on 8/12/2014.
 */
public class NCubeTestDto
{
    public String name;
    public Map<String, Object> coords;
    public Object expectedResult;

    public NCubeTestDto(String name, Map<String, Object> coords, Object expectedResult)
    {
        this.name = name;
        this.coords = coords;
        this.expectedResult = expectedResult;
    }
}
