package com.cedarsoftware.ncube;

import com.cedarsoftware.util.ReflectionUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;

import static org.junit.Assert.assertTrue;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.when;


/**
 * Created by ken on 11/26/2014.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({ReflectionUtils.class, Field.class})
public class TestObjectToMapIllegalAccessException
{
    @Test
    public void testObjectToMapIllegalAccessException() throws Exception {
        class dto
        {
            private Date when = new Date();
            String fname = "Albert";
            String lname = "Einstein";
        }
        dto d = new dto();

        Collection c = new ArrayList();
        c.add(d);

        mockStatic(ReflectionUtils.class);
        when(ReflectionUtils.getDeepDeclaredFields(d.getClass())).thenThrow(IllegalAccessException.class);

        try {
            NCube.objectToMap(d);
        } catch (RuntimeException e) {
            assertTrue(e.getMessage().contains("Failed to access field"));
        }

    }
}
