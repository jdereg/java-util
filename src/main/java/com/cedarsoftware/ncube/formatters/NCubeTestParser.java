package com.cedarsoftware.ncube.formatters;

import com.cedarsoftware.ncube.NCube;
import com.cedarsoftware.ncube.NCubeTestDto;
import com.cedarsoftware.ncube.UrlCommandCell;
import com.cedarsoftware.util.io.JsonReader;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Created by kpartlow on 8/12/2014.
 */
public class NCubeTestParser
{
    public Map<String, NCubeTestDto> parse(NCube cube, String data) throws IOException
    {
        if (data == null) {
            return null;
        }

        if (cube == null) {
            throw new IllegalArgumentException("You need a valid cube to parse the ");
        }

        Map maps = JsonReader.jsonToMaps(data);

        Map<String, NCubeTestDto> tests = new LinkedHashMap<>();

        Object[] items = (Object[])maps.get("@items");

        for(Object o : items)
        {
            Map map = (Map)o;
            String name = (String)map.get("name");
            Map<String,Object> coords = resolveCoords(cube, (Map)map.get("coords"));
            Object result = parseExpectedResult(cube, (Map)map.get("expectedResult"));

            tests.put(name, new NCubeTestDto(name, coords, result));
        }

        return tests;
    }

    public Map<String,Object> resolveCoords(NCube cube, Map map) {
        Map<String, Object> coords = new LinkedHashMap<>();
        Iterator<Map.Entry<String,Object>> i = map.entrySet().iterator();
        while (i.hasNext()) {
            Map.Entry<String, Object> item = i.next();
            Map typeValue = (Map)item.getValue();

            Object o = NCube.parseJsonValue((String)typeValue.get("type"), typeValue.get("value"));

            Map args = new HashMap();
            args.put("ncube", cube);
            if (o instanceof UrlCommandCell)
            {
                coords.put(item.getKey(), ((UrlCommandCell)o).execute(args));
            } else {
                coords.put(item.getKey(), o);
            }
        }
        return coords;
    }

    public Object parseExpectedResult(NCube cube, Map map) {
        Object o = NCube.parseJsonValue((String)map.get("type"), map.get("value"));

        Map args = new HashMap();
        args.put("ncube", cube);
        return o;
    }

//    public Object execute(String data, String name, String version)
//    {
//        prepare(data, name, version);
//        return executeInternal(data, ctx);
//    }
//
//    public String buildGroovy(String theirGroovy, String cubeName, String cmdHash)
//    {
//        StringBuilder groovyCodeWithoutImportStatements = new StringBuilder();
//        Set<String> imports = getImports(theirGroovy, groovyCodeWithoutImportStatements);
//        StringBuilder groovy = new StringBuilder("package ncube.grv.exp;\n");
//
//        for (String importLine : imports)
//        {
//            groovy.append(importLine);
//            groovy.append('\n');
//        }
//
//        String className = "N_" + cmdHash;
//        groovy.append("class ");
//        groovy.append(className);
//        groovy.append(" extends ncube.grv.exp.NCubeGroovyExpression\n{\n\tdef run()\n\t{\n\t");
//        groovy.append(groovyCodeWithoutImportStatements);
//        groovy.append("\n}\n}");
//        return groovy.toString();
//    }
//
//    /**
//     * Conditionally compile the passed in command.  If it is already compiled, this method
//     * immediately returns.  Insta-check because it is just a ref == null check.
//     */
//    public void prepare(String data, String name, String version)
//    {
//        //  This order is important because data can be null before the url is loaded
//        //  and then be present afterwards.  we'd have two different hashes for the same object.
//        String cmdHash = getCmdHash(data);
//        try
//        {
//            compile(data, name, version, cmdHash);
//        }
//        catch (Exception e)
//        {
//            throw new IllegalArgumentException("Failed to compile Groovy Command '" + data, e);
//        }
//    }
//
//    public String getCmdHash(String command)
//    {
//        return EncryptionUtilities.calculateSHA1Hash(StringUtilities.getBytes(command, "UTF-8"));
//    }
//
//
//    protected void compile(String command, String name, String version, String cmdHash) throws Exception
//    {
//        //version to use?
//        GroovyClassLoader urlLoader = (GroovyClassLoader) NCubeManager.getUrlClassLoader(version);
//
//        if (urlLoader == null)
//        {
//            throw new IllegalStateException("Problem compiling Groovy code. No ClassLoaders set in NCubeManager for version: " + version + ".  Use NCubeManager.addBaseResourceUrls() to set it.  Found executing ncube: " + name);
//        }
//
//        String groovySource = buildGroovy(command, name, cmdHash);
//    }
//
//    public static Set<String> getImports(String text, StringBuilder newGroovy)
//    {
//        Matcher m = Regexes.importPattern.matcher(text);
//        Set<String> importNames = new LinkedHashSet<>();
//        while (m.find())
//        {
//            importNames.add(m.group(0));  // based on Regex pattern - if pattern changes, this could change
//        }
//
//        m.reset();
//        newGroovy.append(m.replaceAll(""));
//        return importNames;
//    }
//
//    protected Object executeInternal(Object data, Map args)
//    {
//        String cubeName = getNCube(args).getName();
//        try
//        {
//            return executeGroovy(args, getCmdHash(data.toString()));
//        }
//        catch(InvocationTargetException e)
//        {
//            Throwable cause = e.getCause();
//            if (cause instanceof CoordinateNotFoundException)
//            {
//                throw (CoordinateNotFoundException) cause;
//            }
//            else if (cause instanceof RuleStop)
//            {
//                throw (RuleStop) cause;
//            }
//            else if (cause instanceof RuleJump)
//            {
//                throw (RuleJump) cause;
//            }
//            throw new RuntimeException("Exception occurred invoking method " + getMethodToExecute(args) + "(), n-cube '" + cubeName + "', input: " + args.get("input"), e) ;
//        }
//        catch (Exception e)
//        {
//            throw new RuntimeException("Error occurred invoking method " + getMethodToExecute(args) + "(), n-cube '" + cubeName + "', input: " + args.get("input"), e);
//        }
//    }
//
//    /**
//     * Fetch constructor (from cache, if cached) and instantiate GroovyExpression
//     */
//    protected Object executeGroovy(final String cmdHash) throws Exception
//    {
//        // Step 1: Construct the object (use default constructor)
//        Constructor c = constructorMap.get(cmdHash);
//        if (c == null)
//        {
//            synchronized (constructorMap)
//            {
//                c = constructorMap.get(cmdHash);
//                if (c == null)
//                {
//                    c = getRunnableCode().getConstructor();
//                    constructorMap.put(cmdHash, c);
//                }
//            }
//        }
//
//        final Object instance = c.newInstance();
//
//        // Step 2: Call the inherited 'init(Map args)' method.  This technique saves the subclasses from having
//        // to implement a duplicate constructor that routes the Map up (Constructors are not inherited).
//        Method initMethod = initMethodMap.get(cmdHash);
//        if (initMethod == null)
//        {
//            synchronized (initMethodMap)
//            {
//                initMethod = initMethodMap.get(cmdHash);
//                if (initMethod == null)
//                {
//                    initMethod = getRunnableCode().getMethod("init", Map.class);
//                    initMethodMap.put(cmdHash, initMethod);
//                }
//            }
//        }
//
//        initMethod.invoke(instance, args);
//
//        // Step 3: Call the run() [for expressions] or run(Signature) [for controllers] method
//        Method runMethod = methodMap.get(cmdHash);
//
//        if (runMethod == null)
//        {
//            synchronized (methodMap)
//            {
//                runMethod = methodMap.get(cmdHash);
//                if (runMethod == null)
//                {
//                    runMethod = getRunMethod();
//                    methodMap.put(cmdHash, runMethod);
//                }
//            }
//        }
//
//        return invokeRunMethod(runMethod, instance, args, cmdHash);
//    }


}
