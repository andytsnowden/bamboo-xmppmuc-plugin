package ut.com.ipsoft.plugins.xmppmuc;

import org.junit.Test;
import com.ipsoft.plugins.xmppmuc.api.MyPluginComponent;
import com.ipsoft.plugins.xmppmuc.impl.MyPluginComponentImpl;

import static org.junit.Assert.assertEquals;

public class MyComponentUnitTest
{
    @Test
    public void testMyName()
    {
        MyPluginComponent component = new MyPluginComponentImpl(null);
        assertEquals("names do not match!", "myComponent",component.getName());
    }
}