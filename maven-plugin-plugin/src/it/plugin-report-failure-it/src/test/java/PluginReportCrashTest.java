

import org.apache.maven.integrationtests.AbstractMavenIntegrationTestCase;
import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.ResourceExtractor;

import java.io.File;

/**
 * Tests that the PluginDescriptor.getArtifacts() call returns all of the dependencies of the plugin,
 * not just those that made it past the filter excluding Maven's core artifacts.
 */
public class PluginReportCrashTest
    extends AbstractMavenIntegrationTestCase
{
    public void testPluginReport()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/PluginReportCrash" );

        Verifier verifier = new Verifier( testDir.getAbsolutePath() );

        verifier.executeGoal( "org.apache.maven.plugins:maven-plugin-plugin:2.3:report" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();
        
        verifier = new Verifier( testDir.getAbsolutePath() );

        verifier.executeGoal( "org.apache.maven.plugins:maven-plugin-plugin:2.4:report" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();
    }
}
