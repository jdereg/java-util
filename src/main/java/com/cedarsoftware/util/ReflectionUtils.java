package com.cedarsoftware.util;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author John DeRegnaucourt (jdereg@gmail.com)
 *         <br/>
 *         Copyright (c) Cedar Software LLC
 *         <br/><br/>
 *         Licensed under the Apache License, Version 2.0 (the "License");
 *         you may not use this file except in compliance with the License.
 *         You may obtain a copy of the License at
 *         <br/><br/>
 *         http://www.apache.org/licenses/LICENSE-2.0
 *         <br/><br/>
 *         Unless required by applicable law or agreed to in writing, software
 *         distributed under the License is distributed on an "AS IS" BASIS,
 *         WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *         See the License for the specific language governing permissions and
 *         limitations under the License.
 */
public class ReflectionUtils
{
    private static final Map<Class, Collection<Field>> _reflectedFields = new ConcurrentHashMap<Class, Collection<Field>>();

    /**
     * Get a Mehod annotation, even if the method is on an object behind a
     * JDK proxy, CGLib proxy, Javassist proxy, etc.  First the method is
     * directly inspected for the annotation, and if not found there, the
     * interfaces are inspected for the class containing the method, which
     * are then checked for the annotation.
     * @param method Method to check for annotation
     * @param annoClass Annotation class
     * @return Annotation if found, null otherwise.
     */
    public static Annotation getMethodAnnotation(Method method, Class annoClass)
    {
        Annotation a = method.getAnnotation(annoClass);
        if (a != null)
        {
            return a;
        }

        Class[] interfaces = method.getDeclaringClass().getInterfaces();
        if (interfaces != null)
        {
            for (Class interFace : interfaces)
            {
                try
                {
                    Method m = interFace.getMethod(method.getName(), method.getParameterTypes());
                    a = m.getAnnotation(annoClass);
                    if (a != null)
                    {
                        return a;
                    }
                }
                catch (Exception ignored) { }
            }
        }
        return null;
    }

    /**
     * Get all non static, non transient, fields of the passed in class, including
     * private fields. Note, the special this$ field is also not returned.  The result
     * is cached in a static ConcurrentHashMap to benefit execution performance.
     * @param c Class instance
     * @return Collection of only the fields in the passed in class
     * that would need further processing (reference fields).  This
     * makes field traversal on a class faster as it does not need to
     * continually process known fields like primitives.
     */
    public static Collection<Field> getDeepDeclaredFields(Class c)
    {
        if (_reflectedFields.containsKey(c))
        {
            return _reflectedFields.get(c);
        }
        Collection<Field> fields = new ArrayList<Field>();
        Class curr = c;

        while (curr != null)
        {
            try
            {
                Field[] local = curr.getDeclaredFields();

                for (Field field : local)
                {
                    if (!field.isAccessible())
                    {
                        try
                        {
                            field.setAccessible(true);
                        }
                        catch (Exception ignored) { }
                    }

                    int modifiers = field.getModifiers();
                    if (!Modifier.isStatic(modifiers) &&
                            !field.getName().startsWith("this$") &&
                            !Modifier.isTransient(modifiers))
                    {   // speed up: do not count static fields, do not go back up to enclosing object in nested case, do not consider transients
                        fields.add(field);
                    }
                }
            }
            catch (ThreadDeath t)
            {
                throw t;
            }
            catch (Throwable ignored)
            { }

            curr = curr.getSuperclass();
        }
        _reflectedFields.put(c, fields);
        return fields;
    }

    /**
     * Return all Fields from a class (including inherited), mapped by
     * String field name to java.lang.reflect.Field.
     * @param c Class whose fields are being fetched.
     * @return Map of all fields on the Class, keyed by String field
     * name to java.lang.reflect.Field.
     */
    public static Map<String, Field> getDeepDeclaredFieldMap(Class c)
    {
        Map<String, Field> fieldMap = new HashMap<String, Field>();
        Collection<Field> fields = getDeepDeclaredFields(c);
        for (Field field : fields)
        {
            String fieldName = field.getName();
            if (fieldMap.containsKey(fieldName))
            {   // Can happen when parent and child class both have private field with same name
                fieldMap.put(field.getDeclaringClass().getName() + '.' + fieldName, field);
            }
            else
            {
                fieldMap.put(fieldName, field);
            }
        }

        return fieldMap;
    }
}
