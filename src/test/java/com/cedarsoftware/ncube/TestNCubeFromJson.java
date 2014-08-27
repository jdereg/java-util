package com.cedarsoftware.ncube;

import com.cedarsoftware.util.io.JsonReader;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyString;

/**
 * Created by kpartlow on 8/27/2014.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({JsonReader.class})
public class TestNCubeFromJson
{
    //deprecated, so delete test when this is finally gone.
    @Test
    public void testSimpleFetchException() throws IOException
    {
        NCube mock = Mockito.mock(NCube.class);
        BinaryUrlCmd cmd = new BinaryUrlCmd("http://www.cedarsoftware.com", false);

        PowerMockito.mockStatic(JsonReader.class);
        PowerMockito.when(JsonReader.jsonToJava(anyString())).thenThrow(IOException.class);

        try
        {
            NCube.fromJson(null);
        } catch (RuntimeException e) {
            assertEquals("Error reading NCube from passed in JSON", e.getMessage());
        }
    }
}
