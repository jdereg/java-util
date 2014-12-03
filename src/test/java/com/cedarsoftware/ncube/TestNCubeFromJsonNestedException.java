package com.cedarsoftware.ncube;

import com.cedarsoftware.util.io.JsonReader;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.anyString;

/**
 * Created by kpartlow on 12/3/2014.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({JsonReader.class})
public class TestNCubeFromJsonNestedException
{
    @Test
    public void testNCubeFromJsonWithNestedException() throws Exception
    {
        PowerMockito.mockStatic(JsonReader.class);
        PowerMockito.when(JsonReader.jsonToJava(anyString())).thenThrow(new IllegalArgumentException(new NullPointerException()));

        try {
            NCubeManager.ncubeFromJson(null);
            fail();
        } catch (RuntimeException e) {
            assertEquals(NullPointerException.class, e.getCause().getClass());
        }
    }

}
