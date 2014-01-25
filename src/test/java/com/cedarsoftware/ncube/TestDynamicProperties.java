package com.cedarsoftware.ncube;

import org.junit.Assert;
import org.junit.Test;

/**
 * Created by kpartlow on 1/20/14.
 */
public class TestDynamicProperties extends NCubeTester {

    @Test
    public void testCprStyleProperties() {
        NCube cpr = NCubeManager.getNCubeFromResource("cpr.json");

        Assert.assertEquals("CPR", cpr.getName());

        String ret = getCellAsString(cpr, getCprMap("cdn-base", "Biz1", "SANDBOX"));
        Assert.assertEquals("res://pages", ret);

        ret = getCellAsString(cpr, getCprMap("cdn-base", "Biz1", "DEV"));
        Assert.assertEquals("res://pages", ret);

        ret = getCellAsString(cpr, getCprMap("cdn-layout", "Biz1", "SANDBOX"));
        Assert.assertEquals("Biz1/SANDBOX", ret);

        ret = getCellAsString(cpr, getCprMap("cdn-layout", "Biz1", "DEV"));
        Assert.assertEquals("Biz1/DEV", ret);

        ret = getCellAsString(cpr, getCprMap("cdn-layout", "Biz2", "SANDBOX"));
        Assert.assertEquals("Biz2/SANDBOX", ret);

        ret = getCellAsString(cpr, getCprMap("cdn-layout", "Biz2", "DEV"));
        Assert.assertEquals("Biz2/DEV", ret);

        ret = getCellAsString(cpr, getCprMap("cdn-url", "Biz1", "SANDBOX"));
        Assert.assertEquals("res://pages/Biz1/SANDBOX", ret);

        ret = getCellAsString(cpr, getCprMap("cdn-url", "Biz1", "DEV"));
        Assert.assertEquals("res://pages/Biz1/DEV", ret);

        ret = getCellAsString(cpr, getCprMap("cdn-url", "Biz2", "SANDBOX"));
        Assert.assertEquals("res://pages/Biz2/SANDBOX", ret);

        ret = getCellAsString(cpr, getCprMap("cdn-url", "Biz2", "DEV"));
        Assert.assertEquals("res://pages/Biz2/DEV", ret);

    }
}
