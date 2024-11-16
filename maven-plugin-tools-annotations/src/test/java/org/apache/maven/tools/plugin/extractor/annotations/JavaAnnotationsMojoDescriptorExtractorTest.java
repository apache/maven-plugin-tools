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
package org.apache.maven.tools.plugin.extractor.annotations;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.descriptor.InvalidPluginDescriptorException;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.project.MavenProject;
import org.apache.maven.tools.plugin.DefaultPluginToolsRequest;
import org.apache.maven.tools.plugin.extractor.ExtractionException;
import org.apache.maven.tools.plugin.extractor.annotations.scanner.DefaultMojoAnnotationsScanner;
import org.codehaus.plexus.logging.Logger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

class JavaAnnotationsMojoDescriptorExtractorTest {
    @TempDir
    private Path targetDir;

    MojoDescriptor extractDescriptorFromMojoClass(Class<? extends AbstractMojo> mojoClass)
            throws InvalidPluginDescriptorException, ExtractionException, IOException, URISyntaxException {
        // copy class to an empty tmp directory
        Path sourceClass = Paths.get(
                mojoClass.getResource(mojoClass.getSimpleName() + ".class").toURI());
        Files.copy(sourceClass, targetDir.resolve(sourceClass.getFileName()));
        JavaAnnotationsMojoDescriptorExtractor mojoDescriptorExtractor = new JavaAnnotationsMojoDescriptorExtractor();
        DefaultMojoAnnotationsScanner scanner = new DefaultMojoAnnotationsScanner();
        mojoDescriptorExtractor.mojoAnnotationsScanner = scanner;
        PluginDescriptor pluginDescriptor = new PluginDescriptor();
        MavenProject mavenProject = new MavenProject();
        Artifact artifact = new DefaultArtifact("groupId", "artifactId", "1.0.0", null, "jar", "classifier", null);
        mavenProject.setArtifact(artifact);
        mavenProject.getBuild().setOutputDirectory(targetDir.toString());
        List<MojoDescriptor> mojoDescriptors =
                mojoDescriptorExtractor.execute(new DefaultPluginToolsRequest(mavenProject, pluginDescriptor));
        assertEquals(1, mojoDescriptors.size());
        // there should be only one mojo contained in the one class
        return mojoDescriptors.get(0);
    }

    @Test
    void assertFooMojo() throws InvalidPluginDescriptorException, ExtractionException, IOException, URISyntaxException {
        MojoDescriptor mojoDescriptor = extractDescriptorFromMojoClass(FooMojo.class);
        assertEquals("package", mojoDescriptor.getExecutePhase());
        assertEquals("compiler", mojoDescriptor.getExecuteGoal());
        assertEquals("my-lifecycle", mojoDescriptor.getExecuteLifecycle());
    }

    @Test
    void assertExecuteMojo()
            throws InvalidPluginDescriptorException, ExtractionException, IOException, URISyntaxException {
        MojoDescriptor mojoDescriptor = extractDescriptorFromMojoClass(ExecuteMojo.class);
        assertEquals("my-phase-id", mojoDescriptor.getExecutePhase());
        assertEquals("compiler", mojoDescriptor.getExecuteGoal());
        assertEquals("my-lifecycle", mojoDescriptor.getExecuteLifecycle());
    }

    @Test
    void assertExecute2Mojo()
            throws InvalidPluginDescriptorException, ExtractionException, IOException, URISyntaxException {
        // two conflicting phase ids set
        assertThrows(InvalidPluginDescriptorException.class, () -> extractDescriptorFromMojoClass(Execute2Mojo.class));
    }
}
