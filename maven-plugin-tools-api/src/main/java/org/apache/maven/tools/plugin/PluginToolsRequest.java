package org.apache.maven.tools.plugin;

import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.project.MavenProject;

/**
 * Request that encapsulates all information relevant to the process of extracting {@link MojoDescriptor}
 * instances from metadata for a certain type of mojo.
 * 
 * @author jdcasey
 * @since 2.5
 */
public interface PluginToolsRequest
{
    
    /**
     * Return the current {@link MavenProject} instance in use.
     */
    MavenProject getProject();
    
    /**
     * @see PluginToolsRequest#getProject()
     */
    PluginToolsRequest setProject( MavenProject project );
    
    /**
     * Return the {@link PluginDescriptor} currently being populated as part of the build of the
     * current plugin project.
     */
    PluginDescriptor getPluginDescriptor();

    /**
     * @see PluginToolsRequest#getPluginDescriptor()
     */
    PluginToolsRequest setPluginDescriptor( PluginDescriptor pluginDescriptor );
    
    /**
     * Gets the file encoding of the source files.
     * 
     * @return The file encoding of the source files, never <code>null</code>.
     */
    String getEncoding();

    /**
     * Sets the file encoding of the source files.
     * 
     * @param encoding The file encoding of the source files, may be empty or <code>null</code> to use the platform's
     *            default encoding.
     * @return This request.
     */
    PluginToolsRequest setEncoding( String encoding );

}
