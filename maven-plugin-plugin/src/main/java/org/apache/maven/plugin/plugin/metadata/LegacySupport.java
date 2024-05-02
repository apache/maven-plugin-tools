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

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.metadata.ArtifactRepositoryMetadata;
import org.apache.maven.artifact.repository.metadata.GroupRepositoryMetadata;
import org.apache.maven.artifact.repository.metadata.Versioning;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;

public class LegacySupport {
    public static void execute(MavenProject project, String goalPrefix) throws MojoExecutionException {
        Artifact projectArtifact = project.getArtifact();
        Versioning versioning = new Versioning();
        versioning.setLatest(projectArtifact.getVersion());
        versioning.updateTimestamp();
        ArtifactRepositoryMetadata metadata = new ArtifactRepositoryMetadata(projectArtifact, versioning);
        projectArtifact.addMetadata(metadata);
        GroupRepositoryMetadata groupMetadata = new GroupRepositoryMetadata(project.getGroupId());
        groupMetadata.addPluginMapping(goalPrefix, project.getArtifactId(), project.getName());
        projectArtifact.addMetadata(groupMetadata);
    }
}
