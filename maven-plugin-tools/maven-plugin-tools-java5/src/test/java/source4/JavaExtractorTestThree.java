package source4;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.tools.plugin.Goal;
import org.apache.maven.tools.plugin.Parameter;

/**
 * Tests the implementation argument of the parameter annotation.
 */
@Goal( name = "ideaThree", requiresDependencyResolutionScope = "compile" )
public class JavaExtractorTestThree extends AbstractMojo
{
    /**
     * Desc.
     */
    @Parameter( implementation = "source4.sub.MyBla", required = true )
    private Bla bla;

    public JavaExtractorTestThree()
    {
    }

    public void execute()
    {
        if ( getLog() != null )
        {
            getLog().info( "bla: " + bla );
        }
    }
}
