package org.apache.maven.tools.plugin.extractor.javadoc;

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
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.model.Build;
import org.apache.maven.model.Model;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.plugin.descriptor.Parameter;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugin.logging.SystemStreamLog;
import org.apache.maven.project.MavenProject;
import org.apache.maven.tools.plugin.DefaultPluginToolsRequest;
import org.apache.maven.tools.plugin.ExtendedMojoDescriptor;
import org.apache.maven.tools.plugin.PluginToolsRequest;
import org.apache.maven.tools.plugin.generator.Generator;
import org.apache.maven.tools.plugin.generator.PluginDescriptorGenerator;
import org.apache.maven.tools.plugin.util.PluginUtils;
import org.codehaus.plexus.component.repository.ComponentDependency;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.logging.console.ConsoleLogger;
import org.codehaus.plexus.util.FileUtils;

import org.custommonkey.xmlunit.Diff;
import org.custommonkey.xmlunit.XMLUnit;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;

import static org.junit.Assert.*;

/**
 * @author jdcasey
 */
public class JavaMojoDescriptorExtractorTest
{
    private File root;

    @Before
    public void setUp()
    {
        File sourceFile = fileOf( "dir-flag.txt" );
        root = sourceFile.getParentFile();
    }

    private File fileOf( String classpathResource )
    {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        URL resource = cl.getResource( classpathResource );

        File result = null;
        if ( resource != null )
        {
            result = FileUtils.toFile( resource );
        }

        return result;
    }

    private PluginToolsRequest createRequest( String directory )
    {
        Model model = new Model();
        model.setArtifactId( "maven-unitTesting-plugin" );

        MavenProject project = new MavenProject( model );
        project.setBuild( new Build()
        {
            @Override
            public String getOutputDirectory()
            {
                return new File( "target" ).getAbsolutePath();
            }
        } );

        project.setFile( new File( root, "pom.xml" ) );
        project.addCompileSourceRoot( new File( root, directory ).getPath() );

        PluginDescriptor pluginDescriptor = new PluginDescriptor();
        pluginDescriptor.setGoalPrefix( "test" );
        pluginDescriptor.setDependencies( new ArrayList<ComponentDependency>() );

        return new DefaultPluginToolsRequest( project, pluginDescriptor ).setEncoding( "UTF-8" );
    }

    /**
     * generate plugin.xml for a test resources directory content.
     */
    protected PluginDescriptor generate( String directory )
        throws Exception
    {
        JavaJavadocMojoDescriptorExtractor extractor = new JavaJavadocMojoDescriptorExtractor();
        extractor.enableLogging( new ConsoleLogger( Logger.LEVEL_INFO, "test" ) );
        PluginToolsRequest request = createRequest( directory );

        List<MojoDescriptor> mojoDescriptors = extractor.execute( request );

        // to ensure order against plugin-expected.xml
        PluginUtils.sortMojos( mojoDescriptors );

        for ( MojoDescriptor mojoDescriptor : mojoDescriptors )
        {
            // to ensure order against plugin-expected.xml
            PluginUtils.sortMojoParameters( mojoDescriptor.getParameters() );

            request.getPluginDescriptor().addMojo( mojoDescriptor );
        }

        Generator descriptorGenerator = new PluginDescriptorGenerator( new SystemStreamLog() );

        descriptorGenerator.execute( new File( root, directory ), request );

        return request.getPluginDescriptor();
    }

    /**
     * compare mojos from generated plugin.xml against plugin-expected.xml
     */
    protected void checkExpected( String directory )
        throws Exception
    {
        File testDirectory = new File( root, directory );

        XMLUnit.setIgnoreWhitespace( true );
        XMLUnit.setIgnoreComments( true );

        Document expected = XMLUnit.buildControlDocument(
            FileUtils.fileRead( new File( testDirectory, "plugin-expected.xml" ), "UTF-8" ) );
        Document actual =
            XMLUnit.buildTestDocument( FileUtils.fileRead( new File( testDirectory, "plugin.xml" ), "UTF-8" ) );

        Diff diff = XMLUnit.compareXML( expected, actual );

        if ( !diff.identical() )
        {
            fail( "generated plugin.xml is not identical as plugin-expected.xml for " + directory + ": " + diff );
        }
    }

    /**
     * extract plugin descriptor for test resources directory and check against plugin-expected.xml
     */
    protected List<MojoDescriptor> extract( String directory )
        throws Exception
    {
        PluginDescriptor descriptor = generate( directory );

        checkExpected( directory );

        return descriptor.getMojos();
    }

    @Test
    public void testShouldFindTwoMojoDescriptorsInTestSourceDirectory()
        throws Exception
    {
        List<MojoDescriptor> results = extract( "source" );

        assertEquals( "Extracted mojos", 2, results.size() );
    }

    @Test
    public void testShouldPropagateImplementationParameter()
        throws Exception
    {
        List<MojoDescriptor> results = extract( "source2" );

        assertEquals( 1, results.size() );

        MojoDescriptor mojoDescriptor = results.get( 0 );

        List<Parameter> parameters = mojoDescriptor.getParameters();

        assertEquals( 1, parameters.size() );

        Parameter parameter = parameters.get( 0 );

        assertEquals( "Implementation parameter", "source2.sub.MyBla", parameter.getImplementation() );
    }

    @Test
    public void testMaven30Parameters()
        throws Exception
    {
        List<MojoDescriptor> results = extract( "source2" );

        assertEquals( 1, results.size() );

        ExtendedMojoDescriptor mojoDescriptor = (ExtendedMojoDescriptor) results.get( 0 );
        assertTrue( mojoDescriptor.isThreadSafe() );
        assertEquals( "test", mojoDescriptor.getDependencyCollectionRequired() );
    }

    /**
     * Check that the mojo descriptor extractor will ignore any annotations that are found.
     *
     * @throws Exception
     */
    @Test
    public void testAnnotationInPlugin()
        throws Exception
    {
        List<MojoDescriptor> results = extract( "source3" );

        assertNull( results );
    }

    /**
     * Check that the mojo descriptor extractor will successfully parse sources with Java 1.5 language features like
     * generics.
     */
    @Test
    public void testJava15SyntaxParsing()
        throws Exception
    {
        List<MojoDescriptor> results = extract( "java-1.5" );

        assertEquals( 1, results.size() );
    }

    @Test
    public void testSingleTypeImportWithFullyQualifiedClassName()
        throws Exception
    {
        List<MojoDescriptor> results = extract( "MPLUGIN-314" );

        assertEquals( 1, results.size() );
    }

    @Test
    public void testMethodReferenceInEnumConstructor()
        throws Exception
    {
        List<MojoDescriptor> results = extract( "MPLUGIN-320" );

        assertNull( results );
    }

    @Test
    public void testEnumWithRegexPattern()
        throws Exception
    {
        List<MojoDescriptor> results = extract( "MPLUGIN-290" );

        assertNull( results );
    }

}
