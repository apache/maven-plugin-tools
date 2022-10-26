package org.apache.maven.tools.plugin.generator;

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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import org.apache.maven.plugin.descriptor.DuplicateParameterException;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.plugin.descriptor.Parameter;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugin.descriptor.PluginDescriptorBuilder;
import org.apache.maven.tools.plugin.javadoc.JavadocLinkGenerator;
import org.codehaus.plexus.component.repository.ComponentDependency;
import org.codehaus.plexus.configuration.PlexusConfiguration;
import org.codehaus.plexus.testing.PlexusTest;
import org.codehaus.plexus.util.ReaderFactory;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author <a href="mailto:jason@maven.org">Jason van Zyl </a>
 */
@PlexusTest
public class PluginDescriptorFilesGeneratorTest
    extends AbstractGeneratorTestCase
{
    @Override
    protected void extendPluginDescriptor( PluginDescriptor pluginDescriptor ) throws DuplicateParameterException
    {
        Parameter parameterWithGenerics = new Parameter();
        parameterWithGenerics.setName( "parameterWithGenerics" );
        parameterWithGenerics.setType("java.util.Collection<java.lang.String>");
        parameterWithGenerics.setExpression( "${customParam}" );
        parameterWithGenerics.setDefaultValue( "a,b,c" );
        pluginDescriptor.getMojos().get( 0 ).addParameter( parameterWithGenerics );
    }

    @Override
    protected void validate( File destinationDirectory )
        throws Exception
    {
        PluginDescriptorBuilder pdb = new PluginDescriptorBuilder();

        File pluginDescriptorFile = new File( destinationDirectory, "plugin.xml" );

        String pd = readFile( pluginDescriptorFile );

        PluginDescriptor pluginDescriptor = pdb.build( new StringReader( pd ) );

        assertEquals( 1, pluginDescriptor.getMojos().size() );

        MojoDescriptor mojoDescriptor = pluginDescriptor.getMojos().get( 0 );

        checkMojo( mojoDescriptor );

        // ----------------------------------------------------------------------
        // Dependencies
        // ----------------------------------------------------------------------

        List<ComponentDependency> dependencies = pluginDescriptor.getDependencies();

        checkDependency( "testGroup", "testArtifact", "0.0.0", dependencies.get( 0 ) );

        assertEquals( 1, dependencies.size() );

        ComponentDependency dependency = dependencies.get( 0 );
        assertEquals( "testGroup", dependency.getGroupId() );
        assertEquals( "testArtifact", dependency.getArtifactId() );
        assertEquals( "0.0.0", dependency.getVersion() );
    }

    private String readFile( File pluginDescriptorFile )
        throws IOException
    {
        StringWriter sWriter = new StringWriter();

        try (PrintWriter pWriter = new PrintWriter( sWriter ); //
             BufferedReader reader = new BufferedReader( ReaderFactory.newXmlReader( pluginDescriptorFile ) ))
        {
            String line = null;
            while ( ( line = reader.readLine() ) != null )
            {
                pWriter.println( line );
            }
        }

        return sWriter.toString();
    }

    private void checkMojo( MojoDescriptor mojoDescriptor )
    {
        assertEquals( "test:testGoal", mojoDescriptor.getFullGoalName() );

        assertEquals( "org.apache.maven.tools.plugin.generator.TestMojo", mojoDescriptor.getImplementation() );

        // The following should be defaults
        assertEquals( "per-lookup", mojoDescriptor.getInstantiationStrategy() );

        assertNotNull( mojoDescriptor.isDependencyResolutionRequired() );

        // check the default parameter
        checkParameter( mojoDescriptor.getParameters().get( 0 ) );

        // check another parameter with generics type information
        Parameter parameterWithGenerics = mojoDescriptor.getParameters().get( 2 );
        assertNotNull( parameterWithGenerics );
        assertEquals( "parameterWithGenerics", parameterWithGenerics.getName() );
        assertEquals( "java.util.Collection", parameterWithGenerics.getType() );

        PlexusConfiguration configurations = mojoDescriptor.getMojoConfiguration();
        assertNotNull( configurations );
        PlexusConfiguration configuration = configurations.getChild( "parameterWithGenerics" );
        assertEquals( "java.util.Collection", configuration.getAttribute( "implementation" ) );
        assertEquals( "a,b,c", configuration.getAttribute( "default-value") );
        assertEquals( "${customParam}", configuration.getValue() );
    }

    private void checkParameter( Parameter parameter )
    {
        assertEquals( "dir", parameter.getName() );
        assertEquals( String.class.getName(), parameter.getType() );
        assertTrue( parameter.isRequired() );
        assertEquals( "some.alias", parameter.getAlias() );
    }

    private void checkDependency( String groupId, String artifactId, String version, ComponentDependency dependency )
    {
        assertNotNull( dependency );

        assertEquals( groupId, dependency.getGroupId() );

        assertEquals( artifactId, dependency.getArtifactId() );

        assertEquals( version, dependency.getVersion() );
    }

    @Test
    void testGetJavadocUrlForType() throws URISyntaxException
    {
        URI javadocBaseUri = new URI( "http://localhost/apidocs/" );
        JavadocLinkGenerator linkGenerator = new JavadocLinkGenerator( javadocBaseUri, "1.8" );
        assertEquals( javadocBaseUri.resolve("java/lang/String.html"),
                      PluginDescriptorFilesGenerator.getJavadocUrlForType( linkGenerator, "java.lang.String" ) );
        assertEquals( javadocBaseUri.resolve("java/lang/String.html"),
                      PluginDescriptorFilesGenerator.getJavadocUrlForType( linkGenerator, "java.lang.Collection<java.lang.String>" ) );
        assertEquals( javadocBaseUri.resolve("java/lang/Integer.html"),
                      PluginDescriptorFilesGenerator.getJavadocUrlForType( linkGenerator, "java.lang.Map<java.lang.String,java.lang.Integer>" ) );
        assertEquals( javadocBaseUri.resolve("java/util/function/BiFunction.html"),
                      PluginDescriptorFilesGenerator.getJavadocUrlForType( linkGenerator, "java.util.function.BiFunction<java.lang.String,java.lang.String,java.lang.String>" ) );
    }
}