package com.cedarsoftware.lang.reflect;

import com.cedarsoftware.test.Asserter;
import com.cedarsoftware.util.ReflectionUtils;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;

/**
 * Created by kpartlow on 1/6/14.
 */
public class TestReflectionUtilities
{
    @Test
    public void testConstructor()
    {
        Asserter.assertClassOnlyHasAPrivateDefaultConstructor(ReflectionUtils.class);
    }

    @Test
    public void testGetDeepDeclaredFieldMap() {
        Map<String, Field> objectMap = ReflectionUtils.getDeepDeclaredFieldMap(Object.class);
        Assert.assertEquals(0, objectMap.size());

        Map<String, Field> integerMap = ReflectionUtils.getDeepDeclaredFieldMap(Integer.class);
        Assert.assertEquals(1, integerMap.size());
        Assert.assertNotNull(integerMap.get("value"));

        Map<String, Field> map2 = ReflectionUtils.getDeepDeclaredFieldMap(Integer.class);
        Assert.assertEquals(1, map2.size());
        Assert.assertNotNull(map2.get("value"));

        Map<String, Field> rf1 = ReflectionUtils.getDeepDeclaredFieldMap(ReflectionTestOne.class);
        Assert.assertEquals(1, rf1.size());
        Assert.assertNotNull(rf1.get("_one"));

        Map<String, Field> rf2 = ReflectionUtils.getDeepDeclaredFieldMap(ReflectionTestThree.class);
        Assert.assertEquals(3, rf2.size());
        Assert.assertNotNull(rf2.get("_one"));
        Assert.assertNotNull(rf2.get(ReflectionTestOne.class.getName() + "._one"));
        Assert.assertNotNull(rf2.get(ReflectionTestTwo.class.getName() +"._one"));
    }

    @Test
    public void test() throws Exception {
        Method m = this.getClass().getMethod("test", new Class[]{});
        Assert.assertNotNull(ReflectionUtils.getMethodAnnotation(m, Test.class));
        Assert.assertNull(ReflectionUtils.getMethodAnnotation(m, String.class));

        Method foo = ReflectionTestThree.class.getMethod("foo", new Class[] {});
        Assert.assertNotNull(ReflectionUtils.getMethodAnnotation(foo, TestMethodAnnotationOne.class));

        Method bar = ReflectionTestThree.class.getMethod("bar", new Class[]{});
        Assert.assertNotNull(ReflectionUtils.getMethodAnnotation(bar, TestMethodAnnotationTwo.class));
    }

    @Test
    public void testGetMethod() throws Exception {
        Assert.assertNotNull(ReflectionUtils.getMethod(this.getClass(), "test"));
        Assert.assertNull(ReflectionUtils.getMethod(String.class, "test"));
        Assert.assertNull(ReflectionUtils.getMethod(null, "test"));
    }

    @java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
    @java.lang.annotation.Target({java.lang.annotation.ElementType.METHOD})
    private @interface TestMethodAnnotationOne {
    }

    @java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
    @java.lang.annotation.Target({java.lang.annotation.ElementType.METHOD})
    private @interface TestMethodAnnotationTwo {
    }

    private interface TestInterfaceOne {
        @TestMethodAnnotationOne
        public void foo();
    }

    private interface TestInterfaceTwo {
        @TestMethodAnnotationTwo
        public void bar();
    }

    private class ReflectionTestOne {
        @SuppressWarnings("unused")
        private int _one;
    }

    private class ReflectionTestTwo extends ReflectionTestOne implements TestInterfaceOne {
        @SuppressWarnings("unused")
        private int _one;

        public void foo() {
        }
    }

    private class ReflectionTestThree extends ReflectionTestTwo implements TestInterfaceTwo {
        @SuppressWarnings("unused")
        private int _one;


        public void bar() {
        }
    }

}
