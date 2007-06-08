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
package org.apache.maven.tools.plugin.extractor.java;

import java.io.File;
import java.util.List;

import org.apache.maven.model.Build;
import org.apache.maven.model.Model;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.plugin.descriptor.Parameter;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.project.MavenProject;
import org.apache.maven.tools.plugin.extractor.MojoDescriptorExtractor;
import org.codehaus.plexus.PlexusTestCase;

/**
 * Test the ability to extract from Maven files.
 * 
 * @author eredmond
 */
public class JavaMojoAnnotationDescriptorExtractorTest extends PlexusTestCase
{
    public void testShouldFindTwoMojoDescriptors() throws Exception
    {
        JavaMojoAnnotationDescriptorExtractor extractor =
            (JavaMojoAnnotationDescriptorExtractor) lookup( MojoDescriptorExtractor.ROLE, "java5" );

        File dir = new File( "target/test-classes" );

        Model model = new Model();
        model.setArtifactId( "maven-unitTesting-plugin" );

        MavenProject project = new MavenProject( model );

        Build build = new Build();
        build.setOutputDirectory( dir.getAbsolutePath() );

        project.setBuild( build );
        project.setFile( new File( new File( dir, "gen-pom" ), "pom.xml" ) );
        project.addCompileSourceRoot( dir.getPath() );

        System.out.println( dir.getAbsolutePath() );

        PluginDescriptor pluginDescriptor = new PluginDescriptor();
        pluginDescriptor.setGoalPrefix( "test" );
        List results = extractor.execute( project, pluginDescriptor );
        assertEquals( "Extracted mojos", 3, results.size() );

        for ( int i = 0; i < 2; i++ )
        {
            MojoDescriptor mojoDescriptor = (MojoDescriptor) results.get( i );
            assertEquals( 1, mojoDescriptor.getParameters().size() );
            Parameter parameter = (Parameter) mojoDescriptor.getParameters().get( 0 );
            assertEquals( "project", parameter.getName() );
            assertEquals( "java.lang.String[]", parameter.getType() );
        }

        MojoDescriptor mojoDescriptor = (MojoDescriptor) results.get( 2 );
        assertEquals( 1, mojoDescriptor.getParameters().size() );
        Parameter parameter = (Parameter) mojoDescriptor.getParameters().get( 0 );
        assertEquals( "bla", parameter.getName() );
        assertEquals( "source4.Bla", parameter.getType() );
    }
}
