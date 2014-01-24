package com.cedarsoftware.ncube;

import org.junit.Assert;
import org.junit.Test;

/**
 * Created by kpartlow on 1/20/14.
 */
public class TestDynamicProperties extends NCubeTester {

    @Test
    public void testCprStyleProperties() {
        NCube cpr = nCubeManager.getNCubeFromResource("cpr.json");

        Assert.assertEquals("CPR", cpr.getName());

        String ret = getCellAsString(cpr, getCprMap("cdn-base", "AGRI", "SANDBOX"));
        Assert.assertEquals("res://pages", ret);

        ret = getCellAsString(cpr, getCprMap("cdn-base", "AGRI", "DEV"));
        Assert.assertEquals("res://pages", ret);

        ret = getCellAsString(cpr, getCprMap("cdn-layout", "AGRI", "SANDBOX"));
        Assert.assertEquals("AGRI/SANDBOX", ret);

        ret = getCellAsString(cpr, getCprMap("cdn-layout", "AGRI", "DEV"));
        Assert.assertEquals("AGRI/DEV", ret);

        ret = getCellAsString(cpr, getCprMap("cdn-layout", "EQM", "SANDBOX"));
        Assert.assertEquals("EQM/SANDBOX", ret);

        ret = getCellAsString(cpr, getCprMap("cdn-layout", "EQM", "DEV"));
        Assert.assertEquals("EQM/DEV", ret);

        ret = getCellAsString(cpr, getCprMap("cdn-url", "AGRI", "SANDBOX"));
        Assert.assertEquals("res://pages/AGRI/SANDBOX", ret);

        ret = getCellAsString(cpr, getCprMap("cdn-url", "AGRI", "DEV"));
        Assert.assertEquals("res://pages/AGRI/DEV", ret);

        ret = getCellAsString(cpr, getCprMap("cdn-url", "EQM", "SANDBOX"));
        Assert.assertEquals("res://pages/EQM/SANDBOX", ret);

        ret = getCellAsString(cpr, getCprMap("cdn-url", "EQM", "DEV"));
        Assert.assertEquals("res://pages/EQM/DEV", ret);

    }
}
