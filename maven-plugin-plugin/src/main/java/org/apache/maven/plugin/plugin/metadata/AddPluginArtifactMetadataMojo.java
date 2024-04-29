/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.maven.plugin.plugin.metadata;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.rtinfo.RuntimeInformation;
import org.eclipse.aether.util.version.GenericVersionScheme;
import org.eclipse.aether.version.InvalidVersionSpecificationException;
import org.eclipse.aether.version.VersionScheme;

/**
 * Inject any plugin-specific
 * <a href="/ref/current/maven-repository-metadata/repository-metadata.html">artifact metadata</a> to the project's
 * artifact, for subsequent installation and deployment.
 * It is used:
 * <ol>
 * <li>to add the <code>latest</code> metadata (which is plugin-specific) for shipping alongside the plugin's
 *     artifact</li>
 * <li>to define plugin mapping in the group</li>
 * </ol>
 *
 * @see org.apache.maven.artifact.repository.metadata.ArtifactRepositoryMetadata
 * @see org.apache.maven.artifact.repository.metadata.GroupRepositoryMetadata
 *
 * @since 2.0
 */
@Mojo(name = "addPluginArtifactMetadata", defaultPhase = LifecyclePhase.PACKAGE, threadSafe = true)
public class AddPluginArtifactMetadataMojo extends AbstractMojo {
    /**
     * The project artifact, which should have the <code>latest</code> metadata added to it.
     */
    @Component
    private MavenProject project;

    /**
     * The prefix for the plugin goal.
     */
    @Parameter
    private String goalPrefix;

    /**
     * Set this to "true" to skip invoking any goals or reports of the plugin.
     *
     * @since 2.8
     */
    @Parameter(defaultValue = "false", property = "maven.plugin.skip")
    private boolean skip;

    @Component
    private RuntimeInformation runtimeInformation;

    private final VersionScheme versionScheme = new GenericVersionScheme();

    /** {@inheritDoc} */
    @Override
    public void execute() throws MojoExecutionException {
        if (skip) {
            getLog().warn("Execution skipped");
            return;
        }
        // nothing if Maven is 3.9+
        try {
            if (versionScheme
                            .parseVersion("3.9.0")
                            .compareTo(versionScheme.parseVersion(runtimeInformation.getMavenVersion()))
                    < 1) {
                getLog().info("This Mojo is not used in Maven version 3.9.0 and above");
                return;
            }
        } catch (InvalidVersionSpecificationException e) {
            // not happening with generic
            throw new MojoExecutionException(e);
        }

        LegacySupport.execute(project, getGoalPrefix());
    }

    /**
     * @return the goal prefix parameter or the goal prefix from the Plugin artifactId.
     */
    private String getGoalPrefix() {
        if (goalPrefix == null) {
            goalPrefix = PluginDescriptor.getGoalPrefixFromArtifactId(project.getArtifactId());
        }

        return goalPrefix;
    }
}
