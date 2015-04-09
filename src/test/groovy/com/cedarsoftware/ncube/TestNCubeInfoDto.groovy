package com.cedarsoftware.ncube;

import org.junit.Test

import static org.junit.Assert.assertEquals;

/**
 * Created by kpartlow on 4/8/2015.
 */
public class TestNCubeInfoDto
{
    @Test
    public void testGetApplicationID() {
        NCubeInfoDto dto = new NCubeInfoDto();
        dto.app = "APP";
        dto.tenant = "NONE";
        dto.version = "1.2.0";
        dto.status = "RELEASE";
        dto.branch = "HEAD";

        assertEquals(new ApplicationID("NONE", "APP", "1.2.0", "RELEASE", "HEAD"), dto.getApplicationID());
    }

}
