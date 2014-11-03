package com.cedarsoftware.ncube;

import com.cedarsoftware.util.UrlUtilities;
import org.junit.After;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by kpartlow on 1/20/14.
 */
public class NCubeTester
{
    @After
    public void tearDown() throws Exception
    {
        TestNCube.tearDown();
    }

    public Map getCprMap(String prop, String bu, String env) {
        Map map = new HashMap();
        map.put("cprName", prop);
        map.put("env", env);
        map.put("bu", bu);
        return map;
    }


    public Map getPageMap(String prop, String bu, String env, String lang, String page) {
        Map map = new HashMap();
        map.put("cprName", prop);
        map.put("env", env);
        map.put("bu", bu);
        map.put("page", page);

        if (lang != null) {
            map.put("lang", lang);
        }
        return map;
    }

    public String getCellAsString(NCube ncube, Map map) {
        return (String)ncube.getCell(map);
    }

    public String getPageContent(NCube ncube, String prop, String bu, String env, String lang, String page)
    {
        String ret = getCellAsString(ncube, getPageMap(prop, bu, env, lang, page));
        if (ret == null) {
            return null;
        }
        return UrlUtilities.getContentFromUrlAsString(ret);
    }

}
