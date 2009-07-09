import java.util.*;

import org.apache.maven.plugin.AbstractMojo;

/**
 * Test for gleaning of source files with Java 1.5 features
 * 
 * @goal test
 */
public class MyMojo
    extends AbstractMojo
{

    // cf. MPLUGIN-152
    private static final Map<String, String> map1 = Collections.<String, String> emptyMap();

    public void execute()
    {
    }

}
