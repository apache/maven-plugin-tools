package org.apache.maven.tools.plugin;

import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.project.MavenProject;

/**
 * Default implementation of {@link PluginToolsRequest}, which is used to pass parameters to components used to extract
 * {@link MojoDescriptor} instances from different types of metadata for a given plugin.
 * 
 * @author jdcasey
 */
public class DefaultPluginToolsRequest
    implements PluginToolsRequest
{

    private PluginDescriptor pluginDescriptor;

    private MavenProject project;

    private String encoding = "ISO-8859-1";

    public DefaultPluginToolsRequest( MavenProject project, PluginDescriptor pluginDescriptor )
    {
        this.project = project;
        this.pluginDescriptor = pluginDescriptor;
    }

    /**
     * {@inheritDoc}
     */
    public PluginDescriptor getPluginDescriptor()
    {
        return pluginDescriptor;
    }

    /**
     * {@inheritDoc}
     */
    public MavenProject getProject()
    {
        return project;
    }

    /**
     * {@inheritDoc}
     */
    public String getEncoding()
    {
        return this.encoding;
    }

    /**
     * {@inheritDoc}
     */
    public PluginToolsRequest setEncoding( String encoding )
    {
        if ( encoding == null )
        {
            throw new IllegalArgumentException( "unspecified source file encoding" );
        }
        this.encoding = encoding;
        
        return this;
    }

}
