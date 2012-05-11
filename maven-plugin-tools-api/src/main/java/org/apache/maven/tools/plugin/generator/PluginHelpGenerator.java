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
import org.apache.maven.plugin.descriptor.Parameter;
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
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
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
     * Line separator
     */
    private static final String LS = System.getProperty( "line.separator" );

    /**
     * Default generated class name
     */
    private static final String HELP_MOJO_CLASS_NAME = "HelpMojo";

    /**
     * Default goal
     */
    private static final String HELP_GOAL = "help";

    private String helpPackageName;

    /**
     * Flag to indicate if the generated help mojo should use Java 5 features
     */
    private boolean useJava5;

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

        MojoDescriptor helpDescriptor = makeHelpDescriptor( pluginDescriptor );

        // Verify that no help goal already exists
        for ( @SuppressWarnings( "unchecked" ) Iterator<MojoDescriptor> it = pluginDescriptor.getMojos().iterator();
              it.hasNext(); )
        {
            MojoDescriptor descriptor = it.next();

            if ( descriptor.getGoal().equals( helpDescriptor.getGoal() ) && !descriptor.getImplementation().equals(
                helpDescriptor.getImplementation() ) )
            {
                if ( getLogger().isWarnEnabled() )
                {
                    getLogger().warn( "\n\nA help goal (" + descriptor.getImplementation()
                                          + ") already exists in this plugin. SKIPPED THE "
                                          + helpDescriptor.getImplementation() + " GENERATION.\n" );
                }

                return;
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

        try
        {
            properties.store( new FileOutputStream( tmpPropertiesFile ), "maven plugin help generation informations" );
        }
        catch ( IOException e )
        {
            throw new GeneratorException( e.getMessage(), e );
        }

        String sourcePath = helpDescriptor.getImplementation().replace( '.', File.separatorChar ) + ".java";
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

    public PluginHelpGenerator setUseJava5( boolean useJava5 )
    {
        this.useJava5 = useJava5;
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
     * Creates a minimalistic mojo descriptor for the generated help goal.
     *
     * @param pluginDescriptor The descriptor of the plugin for which to generate a help goal, must not be
     *                         <code>null</code>.
     * @return The mojo descriptor for the generated help goal, never <code>null</code>.
     */
    private MojoDescriptor makeHelpDescriptor( PluginDescriptor pluginDescriptor )
    {
        MojoDescriptor descriptor = new MojoDescriptor();

        descriptor.setPluginDescriptor( pluginDescriptor );

        descriptor.setLanguage( "java" );

        descriptor.setGoal( HELP_GOAL );

        String packageName = helpPackageName;
        if ( StringUtils.isEmpty( packageName ) )
        {
            packageName = discoverPackageName( pluginDescriptor );
        }
        if ( StringUtils.isNotEmpty( packageName ) )
        {
            descriptor.setImplementation( packageName + '.' + HELP_MOJO_CLASS_NAME );
        }
        else
        {
            descriptor.setImplementation( HELP_MOJO_CLASS_NAME );
        }

        descriptor.setDescription(
            "Display help information on " + pluginDescriptor.getArtifactId() + ".<br/> Call <pre>  mvn "
                + descriptor.getFullGoalName()
                + " -Ddetail=true -Dgoal=&lt;goal-name&gt;</pre> to display parameter details." );

        try
        {
            Parameter param = new Parameter();
            param.setName( "detail" );
            param.setType( "boolean" );
            param.setDescription( "If <code>true</code>, display all settable properties for each goal." );
            param.setDefaultValue( "false" );
            param.setExpression( "${detail}" );
            descriptor.addParameter( param );

            param = new Parameter();
            param.setName( "goal" );
            param.setType( "java.lang.String" );
            param.setDescription(
                "The name of the goal for which to show help." + " If unspecified, all goals will be displayed." );
            param.setExpression( "${goal}" );
            descriptor.addParameter( param );

            param = new Parameter();
            param.setName( "lineLength" );
            param.setType( "int" );
            param.setDescription( "The maximum length of a display line, should be positive." );
            param.setDefaultValue( "80" );
            param.setExpression( "${lineLength}" );
            descriptor.addParameter( param );

            param = new Parameter();
            param.setName( "indentSize" );
            param.setType( "int" );
            param.setDescription( "The number of spaces per indentation level, should be positive." );
            param.setDefaultValue( "2" );
            param.setExpression( "${indentSize}" );
            descriptor.addParameter( param );
        }
        catch ( Exception e )
        {
            throw new RuntimeException( "Failed to setup parameters for help goal", e );
        }

        return descriptor;
    }

    /**
     * Find the best package name, based on the number of hits of actual Mojo classes.
     *
     * @param pluginDescriptor not null
     * @return the best name of the package for the generated mojo
     */
    protected static String discoverPackageName( PluginDescriptor pluginDescriptor )
    {
        Map<String, Integer> packageNames = new HashMap<String, Integer>();
        for ( Iterator it = pluginDescriptor.getMojos().iterator(); it.hasNext(); )
        {
            MojoDescriptor descriptor = (MojoDescriptor) it.next();

            String impl = descriptor.getImplementation();
            if ( StringUtils.equals( descriptor.getGoal(), "help" ) && StringUtils.equals( "HelpMojo", impl ) )
            {
                continue;
            }
            if ( impl.lastIndexOf( '.' ) != -1 )
            {
                String name = impl.substring( 0, impl.lastIndexOf( '.' ) );
                if ( packageNames.get( name ) != null )
                {
                    int next = ( packageNames.get( name ) ).intValue() + 1;
                    packageNames.put( name, new Integer( next ) );
                }
                else
                {
                    packageNames.put( name, new Integer( 1 ) );
                }
            }
            else
            {
                packageNames.put( "", new Integer( 1 ) );
            }
        }

        String packageName = "";
        int max = 0;
        for ( Iterator it = packageNames.keySet().iterator(); it.hasNext(); )
        {
            String key = it.next().toString();
            int value = ( packageNames.get( key ) ).intValue();
            if ( value > max )
            {
                max = value;
                packageName = key;
            }
        }

        return packageName;
    }

}
