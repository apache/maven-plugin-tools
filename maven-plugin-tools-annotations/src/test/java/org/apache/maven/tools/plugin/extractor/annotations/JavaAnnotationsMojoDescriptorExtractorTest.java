package org.apache.maven.tools.plugin.extractor.annotations;

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

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.plugin.descriptor.InvalidPluginDescriptorException;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.project.MavenProject;
import org.apache.maven.tools.plugin.DefaultPluginToolsRequest;
import org.apache.maven.tools.plugin.extractor.ExtractionException;
import org.apache.maven.tools.plugin.extractor.annotations.scanner.DefaultMojoAnnotationsScanner;
import org.codehaus.plexus.logging.Logger;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

class JavaAnnotationsMojoDescriptorExtractorTest
{

    @Test
    void testExecute() throws InvalidPluginDescriptorException, ExtractionException
    {
        JavaAnnotationsMojoDescriptorExtractor mojoDescriptorExtractor = new JavaAnnotationsMojoDescriptorExtractor();
        DefaultMojoAnnotationsScanner scanner = new DefaultMojoAnnotationsScanner();
        scanner.enableLogging( mock( Logger.class ) );
        mojoDescriptorExtractor.mojoAnnotationsScanner = scanner;
        PluginDescriptor pluginDescriptor = new PluginDescriptor();
        MavenProject mavenProject = new MavenProject();
        File directoryToScan = new File( FooMojo.class.getResource( "" ).getFile() );
        Artifact artifact = new DefaultArtifact("groupId", "artifactId", "1.0.0", null, "jar", "classifier", null);
        mavenProject.setArtifact( artifact );
        mavenProject.getBuild().setOutputDirectory( directoryToScan.toString() );
        List<MojoDescriptor> mojoDescriptors = mojoDescriptorExtractor.execute( new DefaultPluginToolsRequest( mavenProject, pluginDescriptor ) );
        assertEquals( 6, mojoDescriptors.size() );
        Map<String, MojoDescriptor> descriptorsMap = mojoDescriptors.stream().collect( Collectors.toMap( MojoDescriptor::getGoal, Function.<MojoDescriptor>identity() ) );
        assertExecuteMojo( descriptorsMap.get( "execute" ) );
        assertExecute2Mojo( descriptorsMap.get( "execute2" ) );
    }

    private void assertExecuteMojo( MojoDescriptor mojoDescriptor )
    {
        assertEquals( "my-phase-id", mojoDescriptor.getExecutePhase() );
        assertEquals( "compiler", mojoDescriptor.getExecuteGoal() );
        assertEquals( "my-lifecycle", mojoDescriptor.getExecuteLifecycle() );
    }
    
    private void assertExecute2Mojo( MojoDescriptor mojoDescriptor )
    {
        // standard phase overrides custom phase
        assertEquals( LifecyclePhase.GENERATE_RESOURCES.id(), mojoDescriptor.getExecutePhase() );
    }
}
