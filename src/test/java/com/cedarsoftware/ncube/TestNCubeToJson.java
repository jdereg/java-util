package com.cedarsoftware.ncube;

import com.cedarsoftware.util.io.JsonWriter;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.IOException;

import static org.powermock.api.mockito.PowerMockito.when;

/**
 * Created by kpartlow on 8/9/2014.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({JsonWriter.class})
public class TestNCubeToJson
{
    //This one is another "impossible" situation since inside of
    //cube.toJson() calls -> JsonWriter.objectToJson(this) -> which uses a ByteArrayOutputStream()
    //and will never be in a situation where it could throw the IOException.  I guess I'm saying
    //that the wrapper in toJson() probably doesn't need to re-throw the RuntimeException or even
    // better JsonWriter.objectToJson() needs to wrap and catch the IOException since he will not
    // have it thrown (we are using a ByteArrayOutputStream() there (or just catch Exception)
    // in that bit of code and then rethrow RuntimeException.  I'll mock it for now until we can review it.
    @Test(expected = RuntimeException.class)
    public void testNCubeToJson() throws Exception {
        PowerMockito.mockStatic(JsonWriter.class);
        NCube ncube = getTestNCube2D(true);
        when(JsonWriter.objectToJson(ncube)).thenThrow(IOException.class);
        ncube.toJson();
    }

    NCube getTestNCube2D(boolean defCol)
    {
        NCube<Double> ncube = new NCube<Double>("test.Age-Gender");
        Axis axis2 = new Axis("Age", AxisType.RANGE, AxisValueType.LONG, defCol);
        axis2.addColumn(new Range(0, 18));
        axis2.addColumn(new Range(18, 30));
        axis2.addColumn(new Range(30, 40));
        axis2.addColumn(new Range(40, 65));
        axis2.addColumn(new Range(65, 80));
        ncube.addAxis(axis2);

        return ncube;
    }


}
