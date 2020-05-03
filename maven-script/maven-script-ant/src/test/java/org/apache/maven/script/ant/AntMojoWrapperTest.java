package org.apache.maven.script.ant;

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

import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.hamcrest.CoreMatchers.endsWith;
import static org.hamcrest.CoreMatchers.startsWith;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.Reader;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Build;
import org.apache.maven.model.Model;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugin.descriptor.PluginDescriptorBuilder;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.path.PathTranslator;
import org.apache.tools.ant.BuildEvent;
import org.apache.tools.ant.BuildListener;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.jar.JarArchiver;
import org.codehaus.plexus.component.configurator.ComponentConfigurationException;
import org.codehaus.plexus.component.factory.ComponentInstantiationException;
import org.codehaus.plexus.component.factory.ant.AntScriptInvoker;
import org.codehaus.plexus.component.repository.ComponentRequirement;
import org.codehaus.plexus.configuration.PlexusConfigurationException;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.logging.console.ConsoleLogger;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

public class AntMojoWrapperTest
{
    
    private BuildListener buildListener;

    @Before
    public void setUp() 
    {
        buildListener = mock( BuildListener.class );
    }
    
    @Test
    public void test2xStylePlugin()
        throws PlexusConfigurationException, IOException, ComponentInstantiationException, MojoExecutionException,
        ComponentConfigurationException, ArchiverException, URISyntaxException
    {
        String pluginXml = "META-INF/maven/plugin-2.1.xml";

        List<String> messages = run( pluginXml, true );

        assertPresence( messages, "Unpacked Ant build scripts (in Maven build directory).", false );
        assertPresence( messages, "Maven parameter expression evaluator for Ant properties.", false );
        assertPresence( messages, "Maven standard project-based classpath references.", false );
        assertPresence( messages, "Maven standard plugin-based classpath references.", false );
        assertPresence( messages,
                        "Maven project, session, mojo-execution, or path-translation parameter information is", false );
        assertPresence( messages, "maven-script-ant < 2.1.0, or used maven-plugin-tools-ant < 2.2 during release",
                        false );

        ArgumentCaptor<BuildEvent> buildEvent = ArgumentCaptor.forClass(BuildEvent.class);
        verify( buildListener, atLeastOnce() ).messageLogged( buildEvent.capture() );

        // last message
        assertThat( buildEvent.getValue().getMessage(), startsWith( "plugin classpath is: " ) );
        assertThat( buildEvent.getValue().getMessage(), endsWith( ".test.jar" ) );
    }

    @Test
    public void test20StylePlugin()
        throws PlexusConfigurationException, IOException, ComponentInstantiationException, MojoExecutionException,
        ComponentConfigurationException, ArchiverException, URISyntaxException
    {
        String pluginXml = "META-INF/maven/plugin-2.0.xml";

        List<String> messages = run( pluginXml, false );

        assertPresence( messages, "Unpacked Ant build scripts (in Maven build directory).", true );
        assertPresence( messages, "Maven parameter expression evaluator for Ant properties.", true );
        assertPresence( messages, "Maven standard project-based classpath references.", true );
        assertPresence( messages, "Maven standard plugin-based classpath references.", true );
        assertPresence( messages,
                        "Maven project, session, mojo-execution, or path-translation parameter information is", true );
        assertPresence( messages, "maven-script-ant < 2.1.0, or used maven-plugin-tools-ant < 2.2 during release", true );

        ArgumentCaptor<BuildEvent> buildEvent = ArgumentCaptor.forClass(BuildEvent.class);
        verify( buildListener, atLeastOnce() ).messageLogged( buildEvent.capture() );

        // last message
        assertThat( buildEvent.getValue().getMessage(), startsWith( "plugin classpath is: " ) );
        assertThat( buildEvent.getValue().getMessage(), endsWith( "path-is-missing" ) );
    }

    private void assertPresence( List<String> messages, String test, boolean shouldBePresent )
    {
        for ( String message : messages )
        {
            if ( message.contains( test ) )
            {
                if ( !shouldBePresent )
                {
                    fail( "Test string: '" + test + "' was found in output, but SHOULD NOT BE THERE." );
                }
                return;
            }
        }

        if ( shouldBePresent )
        {
            fail( "Test string: '" + test + "' was NOT found in output, but SHOULD BE THERE." );
        }
    }

    private List<String> run( String pluginXml, boolean includeImplied )
        throws PlexusConfigurationException, IOException, ComponentInstantiationException, MojoExecutionException,
        ComponentConfigurationException, ArchiverException, URISyntaxException
    {
        StackTraceElement stack = new Throwable().getStackTrace()[1];
        System.out.println( "\n\nRunning: " + stack.getMethodName() + "\n\n" );

        URL resource = Thread.currentThread().getContextClassLoader().getResource( pluginXml );

        if ( resource == null )
        {
            fail( "plugin descriptor not found: '" + pluginXml + "'." );
        }

        PluginDescriptor pd;
        try ( Reader reader = new InputStreamReader( resource.openStream() ) )
        {
            pd = new PluginDescriptorBuilder().build( reader, pluginXml );
        }

        Map<String, Object> config = new HashMap<>();
        config.put( "basedir", new File( "." ).getAbsoluteFile() );
        config.put( "messageLevel", "info" );

        MojoDescriptor md = pd.getMojo( "test" );

        AntMojoWrapper wrapper =
            new AntMojoWrapper( new AntScriptInvoker( md, Thread.currentThread().getContextClassLoader() ) );

        wrapper.enableLogging( new ConsoleLogger( Logger.LEVEL_DEBUG, "test" ) );

        Artifact artifact = mock( Artifact.class );
        PathTranslator pt = mock( PathTranslator.class );

        if ( includeImplied )
        {
            File pluginXmlFile = Paths.get( resource.toURI() ).toFile();

            File jarFile = File.createTempFile( "AntMojoWrapperTest.", ".test.jar" );
            jarFile.deleteOnExit();

            JarArchiver archiver = new JarArchiver();
            archiver.enableLogging( new ConsoleLogger( Logger.LEVEL_ERROR, "archiver" ) );
            archiver.setDestFile( jarFile );
            archiver.addFile( pluginXmlFile, pluginXml );
            archiver.createArchive();

            when( artifact.getFile() ).thenReturn( jarFile );

            Model model = new Model();

            Build build = new Build();
            build.setDirectory( "target" );

            model.setBuild( build );

            MavenProject project = new MavenProject( model );
            project.setFile( new File( "pom.xml" ).getAbsoluteFile() );

            pd.setPluginArtifact( artifact );
            pd.setArtifacts( Collections.singletonList( artifact ) );

            config.put( "project", project );
            config.put( "session", new MavenSession( null, null, null, null, null, null, null, null, null, null ) );
            config.put( "mojoExecution", new MojoExecution( md ) );

            ComponentRequirement cr = new ComponentRequirement();
            cr.setRole( PathTranslator.class.getName() );

            wrapper.addComponentRequirement( cr, pt );
        }

        wrapper.setComponentConfiguration( config );

        TestBuildListener tbl = new TestBuildListener();
        
        wrapper.getAntProject().addBuildListener( buildListener );
        
        PrintStream oldOut = System.out;
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try
        {
            System.setOut( new PrintStream( baos ) );

            wrapper.execute();
        }
        finally
        {
            System.setOut( oldOut );
        }

        System.out.println( "\n\n" + stack.getMethodName() + " executed; verifying...\n\n" );

        List<String> messages = new ArrayList<>();
        if ( !tbl.messages.isEmpty() )
        {
            messages.addAll( tbl.messages );
        }
        
        messages.add( new String( baos.toByteArray() ) );
        
        return messages;
    }

    private static final class TestBuildListener
        implements BuildListener
    {
        private List<String> messages = new ArrayList<>();

        public void buildFinished( BuildEvent arg0 )
        {
        }

        public void buildStarted( BuildEvent arg0 )
        {
        }

        public void messageLogged( BuildEvent event )
        {
            messages.add( event.getMessage() );
        }

        public void targetFinished( BuildEvent arg0 )
        {
        }

        public void targetStarted( BuildEvent arg0 )
        {
        }

        public void taskFinished( BuildEvent arg0 )
        {
        }

        public void taskStarted( BuildEvent arg0 )
        {
        }
    }
}
