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
     *                 default encoding.
     * @return This request.
     */
    PluginToolsRequest setEncoding( String encoding );

    /**
     * By default an exception is throw if no mojo descriptor is found. As the maven-plugin is defined in core, the
     * descriptor generator mojo is bound to generate-resources phase.
     * But for annotations, the compiled classes are needed, so skip error
     * @since 3.0
     */
    PluginToolsRequest setSkipErrorNoDescriptorsFound( boolean skipErrorNoDescriptorsFound );

    /**
     * @since 3.0
     * @return
     */
    boolean isSkipErrorNoDescriptorsFound();



}
