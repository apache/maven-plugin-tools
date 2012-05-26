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

import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.project.MavenProject;
import org.apache.maven.tools.plugin.PluginToolsRequest;
import org.apache.velocity.VelocityContext;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.logging.console.ConsoleLogger;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.velocity.VelocityComponent;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.List;
import java.util.Properties;

/**
 * Generates an <code>HelpMojo</code> class.
 *
 * @author <a href="mailto:vincent.siveton@gmail.com">Vincent Siveton</a>
 * @version $Id$
 * @since 2.4
 */
public class PluginHelpGenerator
    extends AbstractLogEnabled
    implements Generator
{
    /**
     * Default generated class name
     */
    private static final String HELP_MOJO_CLASS_NAME = "HelpMojo";

    /**
     * Default goal
     */
    private static final String HELP_GOAL = "help";

    private String helpPackageName;

    private VelocityComponent velocityComponent;

    /**
     * Default constructor
     */
    public PluginHelpGenerator()
    {
        this.enableLogging( new ConsoleLogger( Logger.LEVEL_INFO, "PluginHelpGenerator" ) );
    }

    // ----------------------------------------------------------------------
    // Public methods
    // ----------------------------------------------------------------------


    /**
     * {@inheritDoc}
     */
    public void execute( File destinationDirectory, PluginToolsRequest request )
        throws GeneratorException
    {
        PluginDescriptor pluginDescriptor = request.getPluginDescriptor();

        String helpImplementation = getImplementation( pluginDescriptor );

        @SuppressWarnings( "unchecked" )
        List<MojoDescriptor> mojoDescriptors = pluginDescriptor.getMojos();

        if ( mojoDescriptors != null )
        {
            // Verify that no help goal already exists
            for ( MojoDescriptor descriptor : mojoDescriptors )
            {
                if ( HELP_GOAL.equals( descriptor.getGoal() )
                    && !descriptor.getImplementation().equals( helpImplementation ) )
                {
                    if ( getLogger().isWarnEnabled() )
                    {
                        getLogger().warn( "\n\nA help goal (" + descriptor.getImplementation()
                                              + ") already exists in this plugin. SKIPPED THE "
                                              + helpImplementation + " GENERATION.\n" );
                    }

                    return;
                }
            }
        }
        Properties properties = new Properties();
        properties.put( "helpPackageName", helpPackageName == null ? "" : helpPackageName );

        MavenProject mavenProject = request.getProject();

        String propertiesFilePath = "META-INF/maven/" + mavenProject.getGroupId() + "/" + mavenProject.getArtifactId();

        File tmpPropertiesFile =
            new File( request.getProject().getBuild().getDirectory(), "maven-plugin-help.properties" );
        if ( tmpPropertiesFile.exists() )
        {
            tmpPropertiesFile.delete();
        }
        else
        {
            if ( !tmpPropertiesFile.getParentFile().exists() )
            {
                tmpPropertiesFile.getParentFile().mkdirs();
            }
        }
        FileOutputStream fos = null;
        try
        {
            fos = new FileOutputStream( tmpPropertiesFile );
            properties.store( fos, "maven plugin help generation informations" );
        }
        catch ( IOException e )
        {
            throw new GeneratorException( e.getMessage(), e );
        }
        finally
        {
            IOUtil.close( fos );
        }

        String sourcePath = helpImplementation.replace( '.', File.separatorChar ) + ".java";
        File helpClass = new File( destinationDirectory, sourcePath );
        helpClass.getParentFile().mkdirs();

        Writer writer = null;
        try
        {
            writer = new OutputStreamWriter( new FileOutputStream( helpClass ), request.getEncoding() );
            writer.write( getHelpClassSources( propertiesFilePath ) );
            writer.flush();
        }
        catch ( IOException e )
        {
            throw new GeneratorException( e.getMessage(), e );
        }
        finally
        {
            IOUtil.close( writer );
        }
    }

    public PluginHelpGenerator setHelpPackageName( String helpPackageName )
    {
        this.helpPackageName = helpPackageName;
        return this;
    }

    public VelocityComponent getVelocityComponent()
    {
        return velocityComponent;
    }

    public PluginHelpGenerator setVelocityComponent( VelocityComponent velocityComponent )
    {
        this.velocityComponent = velocityComponent;
        return this;
    }

    // ----------------------------------------------------------------------
    // Private methods
    // ----------------------------------------------------------------------

    protected String getHelpClassSources( String propertiesFilePath )
    {
        Properties properties = new Properties();
        VelocityContext context = new VelocityContext( properties );
        if ( this.helpPackageName != null )
        {
            properties.put( "helpPackageName", this.helpPackageName );
        }
        else
        {
            properties.put( "helpPackageName", "" );
        }
        properties.put( "propertiesFilePath", propertiesFilePath + "/plugin-description.xml" );
        // FIXME encoding !

        StringWriter stringWriter = new StringWriter();

        InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream( "help-class-source.vm" );
        InputStreamReader isReader = new InputStreamReader( is );
        velocityComponent.getEngine().evaluate( context, stringWriter, "", isReader );

        return stringWriter.toString();
    }


    /**
     * @param pluginDescriptor The descriptor of the plugin for which to generate a help goal, must not be
     *                         <code>null</code>.
     * @return The implementation.
     */
    private String getImplementation( PluginDescriptor pluginDescriptor )
    {
        String packageName = helpPackageName;
        if ( StringUtils.isEmpty( packageName ) )
        {
            packageName = GeneratorUtils.discoverPackageName( pluginDescriptor );
        }

        return StringUtils.isEmpty( packageName ) ? HELP_MOJO_CLASS_NAME : packageName + '.' + HELP_MOJO_CLASS_NAME;
    }
}
