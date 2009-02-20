package org.apache.maven.tools.plugin;

import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.project.MavenProject;

/**
 * Request that encapsulates all information relevant to the process of extracting {@link MojoDescriptor}
 * instances from metadata for a certain type of mojo.
 * 
 * @author jdcasey
 */
public interface PluginToolsRequest
{
    
    /**
     * Return the current {@link MavenProject} instance in use.
     */
    MavenProject getProject();
    
    /**
     * Return the {@link PluginDescriptor} currently being populated as part of the build of the
     * current plugin project.
     */
    PluginDescriptor getPluginDescriptor();

}
