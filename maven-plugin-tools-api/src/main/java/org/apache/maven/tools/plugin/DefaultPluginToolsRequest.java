package org.apache.maven.tools.plugin;

import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.project.MavenProject;

/**
 * Default implementation of {@link PluginToolsRequest}, which is used to pass parameters to components
 * used to extract {@link MojoDescriptor} instances from different types of metadata for a given
 * plugin.
 * 
 * @author jdcasey
 */
public class DefaultPluginToolsRequest
    implements PluginToolsRequest
{

    private PluginDescriptor pluginDescriptor;
    private MavenProject project;
    
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

}
