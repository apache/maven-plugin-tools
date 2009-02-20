package org.apache.maven.tools.plugin;

import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.ReaderFactory;
import org.codehaus.plexus.util.StringUtils;

/**
 * Default implementation of {@link PluginToolsRequest}, which is used to pass parameters to components used to extract
 * {@link MojoDescriptor} instances from different types of metadata for a given plugin.
 * 
 * @author jdcasey
 * @since 2.5
 */
public class DefaultPluginToolsRequest
    implements PluginToolsRequest
{
    
    private static final String DEFAULT_ENCODING = ReaderFactory.FILE_ENCODING;

    private PluginDescriptor pluginDescriptor;

    private MavenProject project;

    private String encoding = DEFAULT_ENCODING;

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
    public PluginToolsRequest setPluginDescriptor( PluginDescriptor pluginDescriptor )
    {
        this.pluginDescriptor = pluginDescriptor;
        return this;
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
    public PluginToolsRequest setProject( MavenProject project )
    {
        this.project = project;
        return this;
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
        if ( StringUtils.isNotEmpty( encoding ) )
        {
            this.encoding = encoding;
        }
        else
        {
            this.encoding = DEFAULT_ENCODING;
        }

        return this;
    }

}
