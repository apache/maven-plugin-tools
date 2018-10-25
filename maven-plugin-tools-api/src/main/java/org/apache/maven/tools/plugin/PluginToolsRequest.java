package org.apache.maven.tools.plugin;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.project.MavenProject;

import java.util.List;
import java.util.Set;

/**
 * Request that encapsulates all information relevant to the process of extracting
 * {@link org.apache.maven.plugin.descriptor.MojoDescriptor MojoDescriptor}
 * instances from metadata for a certain type of mojo.
 *
 * @author jdcasey
 * @since 2.5
 */
public interface PluginToolsRequest
{

    /**
     * @return Return the current {@link MavenProject} instance in use.
     */
    MavenProject getProject();

    /**
     * @param project the current {@link MavenProject}
     * @see PluginToolsRequest#getProject()
     * @return This request.
     */
    PluginToolsRequest setProject( MavenProject project );

    /**
     * @return Return the {@link PluginDescriptor} currently being populated as part of the build of the
     * current plugin project.
     */
    PluginDescriptor getPluginDescriptor();

    /**
     * @see PluginToolsRequest#getPluginDescriptor()
     * @param pluginDescriptor the {@link PluginDescriptor}
     * @return This request.
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
     *                 default encoding.
     * @return This request.
     */
    PluginToolsRequest setEncoding( String encoding );

    /**
     * By default an exception is throw if no mojo descriptor is found. As the maven-plugin is defined in core, the
     * descriptor generator mojo is bound to generate-resources phase.
     * But for annotations, the compiled classes are needed, so skip error
     * @param skipErrorNoDescriptorsFound <code>true</code> to skip errors because of not found descriptors
     * @return This request.
     * @since 3.0
     */
    PluginToolsRequest setSkipErrorNoDescriptorsFound( boolean skipErrorNoDescriptorsFound );

    /**
     * @return <code>true</code> if no descriptor found should not cause a failure
     * @since 3.0
     */
    boolean isSkipErrorNoDescriptorsFound();

    /**
     * Returns the list of {@link Artifact} used in class path scanning for annotations
     *
     * @return the dependencies
     * @since 3.0
     */
    Set<Artifact> getDependencies();

    /**
     * @param dependencies the dependencies
     * @return This request.
     * @since 3.0
     */
    PluginToolsRequest setDependencies( Set<Artifact> dependencies );

    /**
     *
     * @return the remote repositories
     * @since 3.0
     */
    List<ArtifactRepository> getRemoteRepos();

    /**
     *
     * @param remoteRepos the remote repositories
     * @return This request.
     * @since 3.0
     */
    PluginToolsRequest setRemoteRepos( List<ArtifactRepository> remoteRepos );

    /**
     *
     * @return the local artifact repository
     * @since 3.0
     */
    ArtifactRepository getLocal();

    /**
     *
     * @param local the local repository
     * @return This request.
     * @since 3.0
     */
    PluginToolsRequest setLocal( ArtifactRepository local );

}
