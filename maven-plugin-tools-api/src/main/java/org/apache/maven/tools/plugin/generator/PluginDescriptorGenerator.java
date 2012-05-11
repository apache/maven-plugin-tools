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

import org.apache.maven.plugin.descriptor.DuplicateMojoDescriptorException;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.plugin.descriptor.Parameter;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugin.descriptor.Requirement;
import org.apache.maven.project.MavenProject;
import org.apache.maven.tools.plugin.ExtendedMojoDescriptor;
import org.apache.maven.tools.plugin.PluginToolsRequest;
import org.apache.maven.tools.plugin.util.PluginUtils;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.xml.PrettyPrintXMLWriter;
import org.codehaus.plexus.util.xml.XMLWriter;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 * @version $Id$
 * @todo add example usage tag that can be shown in the doco
 * @todo need to add validation directives so that systems embedding maven2 can
 * get validation directives to help users in IDEs.
 */
public class PluginDescriptorGenerator
    implements Generator
{

    /**
     * {@inheritDoc}
     */
    public void execute( File destinationDirectory, PluginToolsRequest request )
        throws GeneratorException
    {
        try
        {
            File f = new File( destinationDirectory, "plugin.xml" );
            writeDescriptor( f, request, false );
            MavenProject mavenProject = request.getProject();
            String pluginDescriptionFilePath =
                "META-INF/maven/" + mavenProject.getGroupId() + "/" + mavenProject.getArtifactId()
                    + "/plugin-description.xml";
            f = new File( request.getProject().getBuild().getOutputDirectory(), pluginDescriptionFilePath );
            writeDescriptor( f, request, true );
        }
        catch ( IOException e )
        {
            throw new GeneratorException( e.getMessage(), e );
        }
        catch ( DuplicateMojoDescriptorException e )
        {
            throw new GeneratorException( e.getMessage(), e );
        }
    }

    public void writeDescriptor( File destinationFile, PluginToolsRequest request, boolean cleanDescription )
        throws IOException, DuplicateMojoDescriptorException
    {
        PluginDescriptor pluginDescriptor = request.getPluginDescriptor();

        File tmpPropertiesFile =
            new File( request.getProject().getBuild().getDirectory(), "maven-plugin-help.properties" );

        if ( tmpPropertiesFile.exists() )
        {
            Properties properties = new Properties();
            properties.load( new FileInputStream( tmpPropertiesFile ) );
            //MojoDescriptor mojoDescriptor =
            //    makeHelpDescriptor( pluginDescriptor, properties.getProperty( "helpPackageName" ) );
            //pluginDescriptor.addMojo( mojoDescriptor );
        }
        if ( destinationFile.exists() )
        {
            destinationFile.delete();
        }
        else
        {
            if ( !destinationFile.getParentFile().exists() )
            {
                destinationFile.getParentFile().mkdirs();
            }
        }

        String encoding = "UTF-8";

        Writer writer = null;
        try
        {
            writer = new OutputStreamWriter( new FileOutputStream( destinationFile ), encoding );

            XMLWriter w = new PrettyPrintXMLWriter( writer, encoding, null );

            w.startElement( "plugin" );

            PluginUtils.element( w, "name", pluginDescriptor.getName() );

            PluginUtils.element( w, "description", pluginDescriptor.getDescription() );

            PluginUtils.element( w, "groupId", pluginDescriptor.getGroupId() );

            PluginUtils.element( w, "artifactId", pluginDescriptor.getArtifactId() );

            PluginUtils.element( w, "version", pluginDescriptor.getVersion() );

            PluginUtils.element( w, "goalPrefix", pluginDescriptor.getGoalPrefix() );

            PluginUtils.element( w, "isolatedRealm", "" + pluginDescriptor.isIsolatedRealm() );

            PluginUtils.element( w, "inheritedByDefault", "" + pluginDescriptor.isInheritedByDefault() );

            w.startElement( "mojos" );

            if ( pluginDescriptor.getMojos() != null )
            {
                for ( @SuppressWarnings( "unchecked" ) Iterator<MojoDescriptor> it =
                          pluginDescriptor.getMojos().iterator(); it.hasNext(); )
                {
                    MojoDescriptor descriptor = it.next();
                    processMojoDescriptor( descriptor, w, cleanDescription );
                }
            }

            w.endElement();

            PluginUtils.writeDependencies( w, pluginDescriptor );

            w.endElement();

            writer.flush();

        }
        finally
        {
            IOUtil.close( writer );
        }
    }

    /**
     * Creates a minimalistic mojo descriptor for the generated help goal.
     *
     * @param pluginDescriptor The descriptor of the plugin for which to generate a help goal, must not be
     *                         <code>null</code>.
     * @return The mojo descriptor for the generated help goal, never <code>null</code>.
     */
    private MojoDescriptor makeHelpDescriptor( PluginDescriptor pluginDescriptor, String packageName )
    {
        MojoDescriptor descriptor = new MojoDescriptor();

        descriptor.setPluginDescriptor( pluginDescriptor );

        descriptor.setLanguage( "java" );

        descriptor.setGoal( "help" );

        if ( StringUtils.isEmpty( packageName ) )
        {
            packageName = discoverPackageName( pluginDescriptor );
        }
        if ( StringUtils.isNotEmpty( packageName ) )
        {
            descriptor.setImplementation( packageName + '.' + "HelpMojo" );
        }
        else
        {
            descriptor.setImplementation( "HelpMojo" );
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
    private static String discoverPackageName( PluginDescriptor pluginDescriptor )
    {
        Map packageNames = new HashMap();
        for ( Iterator it = pluginDescriptor.getMojos().iterator(); it.hasNext(); )
        {
            MojoDescriptor descriptor = (MojoDescriptor) it.next();

            String impl = descriptor.getImplementation();
            if ( impl.lastIndexOf( '.' ) != -1 )
            {
                String name = impl.substring( 0, impl.lastIndexOf( '.' ) );
                if ( packageNames.get( name ) != null )
                {
                    int next = ( (Integer) packageNames.get( name ) ).intValue() + 1;
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
            int value = ( (Integer) packageNames.get( key ) ).intValue();
            if ( value > max )
            {
                max = value;
                packageName = key;
            }
        }

        return packageName;
    }

    protected void processMojoDescriptor( MojoDescriptor mojoDescriptor, XMLWriter w )
    {
        processMojoDescriptor( mojoDescriptor, w, false );
    }

    /**
     * @param mojoDescriptor   not null
     * @param w                not null
     * @param cleanDescription will clean html content from description fields
     */
    protected void processMojoDescriptor( MojoDescriptor mojoDescriptor, XMLWriter w, boolean cleanDescription )
    {
        w.startElement( "mojo" );

        // ----------------------------------------------------------------------
        //
        // ----------------------------------------------------------------------

        w.startElement( "goal" );
        w.writeText( mojoDescriptor.getGoal() );
        w.endElement();

        // ----------------------------------------------------------------------
        //
        // ----------------------------------------------------------------------

        String description = mojoDescriptor.getDescription();

        if ( description != null )
        {
            w.startElement( "description" );
            if ( cleanDescription )
            {
                w.writeText( PluginUtils.toText( mojoDescriptor.getDescription() ) );
            }
            else
            {
                w.writeText( mojoDescriptor.getDescription() );
            }
            w.endElement();
        }

        // ----------------------------------------------------------------------
        //
        // ----------------------------------------------------------------------

        if ( mojoDescriptor.isDependencyResolutionRequired() != null )
        {
            PluginUtils.element( w, "requiresDependencyResolution", mojoDescriptor.isDependencyResolutionRequired() );
        }

        // ----------------------------------------------------------------------
        //
        // ----------------------------------------------------------------------

        PluginUtils.element( w, "requiresDirectInvocation", "" + mojoDescriptor.isDirectInvocationOnly() );

        // ----------------------------------------------------------------------
        //
        // ----------------------------------------------------------------------

        PluginUtils.element( w, "requiresProject", "" + mojoDescriptor.isProjectRequired() );

        // ----------------------------------------------------------------------
        //
        // ----------------------------------------------------------------------

        PluginUtils.element( w, "requiresReports", "" + mojoDescriptor.isRequiresReports() );

        // ----------------------------------------------------------------------
        //
        // ----------------------------------------------------------------------

        PluginUtils.element( w, "aggregator", "" + mojoDescriptor.isAggregator() );

        // ----------------------------------------------------------------------
        //
        // ----------------------------------------------------------------------

        PluginUtils.element( w, "requiresOnline", "" + mojoDescriptor.isOnlineRequired() );

        // ----------------------------------------------------------------------
        //
        // ----------------------------------------------------------------------

        PluginUtils.element( w, "inheritedByDefault", "" + mojoDescriptor.isInheritedByDefault() );

        // ----------------------------------------------------------------------
        //
        // ----------------------------------------------------------------------

        if ( mojoDescriptor.getPhase() != null )
        {
            PluginUtils.element( w, "phase", mojoDescriptor.getPhase() );
        }

        // ----------------------------------------------------------------------
        //
        // ----------------------------------------------------------------------

        if ( mojoDescriptor.getExecutePhase() != null )
        {
            PluginUtils.element( w, "executePhase", mojoDescriptor.getExecutePhase() );
        }

        if ( mojoDescriptor.getExecuteGoal() != null )
        {
            PluginUtils.element( w, "executeGoal", mojoDescriptor.getExecuteGoal() );
        }

        if ( mojoDescriptor.getExecuteLifecycle() != null )
        {
            PluginUtils.element( w, "executeLifecycle", mojoDescriptor.getExecuteLifecycle() );
        }

        // ----------------------------------------------------------------------
        //
        // ----------------------------------------------------------------------

        w.startElement( "implementation" );
        w.writeText( mojoDescriptor.getImplementation() );
        w.endElement();

        // ----------------------------------------------------------------------
        //
        // ----------------------------------------------------------------------

        w.startElement( "language" );
        w.writeText( mojoDescriptor.getLanguage() );
        w.endElement();

        // ----------------------------------------------------------------------
        //
        // ----------------------------------------------------------------------

        if ( mojoDescriptor.getComponentConfigurator() != null )
        {
            w.startElement( "configurator" );
            w.writeText( mojoDescriptor.getComponentConfigurator() );
            w.endElement();
        }

        // ----------------------------------------------------------------------
        //
        // ----------------------------------------------------------------------

        if ( mojoDescriptor.getComponentComposer() != null )
        {
            w.startElement( "composer" );
            w.writeText( mojoDescriptor.getComponentComposer() );
            w.endElement();
        }

        // ----------------------------------------------------------------------
        //
        // ----------------------------------------------------------------------

        w.startElement( "instantiationStrategy" );
        w.writeText( mojoDescriptor.getInstantiationStrategy() );
        w.endElement();

        // ----------------------------------------------------------------------
        // Strategy for handling repeated reference to mojo in
        // the calculated (decorated, resolved) execution stack
        // ----------------------------------------------------------------------
        w.startElement( "executionStrategy" );
        w.writeText( mojoDescriptor.getExecutionStrategy() );
        w.endElement();

        // ----------------------------------------------------------------------
        //
        // ----------------------------------------------------------------------

        if ( mojoDescriptor.getDeprecated() != null )
        {
            w.startElement( "deprecated" );

            if ( StringUtils.isEmpty( mojoDescriptor.getDeprecated() ) )
            {
                w.writeText( "No reason given" );
            }
            else
            {
                w.writeText( mojoDescriptor.getDeprecated() );
            }

            w.endElement();
        }

        // ----------------------------------------------------------------------
        // Extended (3.0) descriptor
        // ----------------------------------------------------------------------

        if ( mojoDescriptor instanceof ExtendedMojoDescriptor )
        {
            ExtendedMojoDescriptor extendedMojoDescriptor = (ExtendedMojoDescriptor) mojoDescriptor;
            if ( extendedMojoDescriptor.getDependencyCollectionRequired() != null )
            {
                PluginUtils.element( w, "requiresDependencyCollection",
                                     extendedMojoDescriptor.getDependencyCollectionRequired() );
            }

            PluginUtils.element( w, "threadSafe", String.valueOf( extendedMojoDescriptor.isThreadSafe() ) );
        }

        // ----------------------------------------------------------------------
        // Parameters
        // ----------------------------------------------------------------------

        @SuppressWarnings( "unchecked" ) List<Parameter> parameters = mojoDescriptor.getParameters();

        w.startElement( "parameters" );

        Map<String, Requirement> requirements = new LinkedHashMap<String, Requirement>();

        Set<Parameter> configuration = new LinkedHashSet<Parameter>();

        if ( parameters != null )
        {
            for ( Parameter parameter : parameters )
            {
                String expression = parameter.getExpression();

                if ( StringUtils.isNotEmpty( expression ) && expression.startsWith( "${component." ) )
                {
                    // treat it as a component...a requirement, in other words.

                    // remove "component." plus expression delimiters
                    String role = expression.substring( "${component.".length(), expression.length() - 1 );

                    String roleHint = null;

                    int posRoleHintSeparator = role.indexOf( "#" );
                    if ( posRoleHintSeparator > 0 )
                    {
                        roleHint = role.substring( posRoleHintSeparator + 1 );

                        role = role.substring( 0, posRoleHintSeparator );
                    }

                    // TODO: remove deprecated expression
                    requirements.put( parameter.getName(), new Requirement( role, roleHint ) );
                }
                else if ( parameter.getRequirement() != null )
                {
                    requirements.put( parameter.getName(), parameter.getRequirement() );
                }
                else
                {
                    // treat it as a normal parameter.

                    w.startElement( "parameter" );

                    PluginUtils.element( w, "name", parameter.getName() );

                    if ( parameter.getAlias() != null )
                    {
                        PluginUtils.element( w, "alias", parameter.getAlias() );
                    }

                    PluginUtils.element( w, "type", parameter.getType() );

                    if ( parameter.getDeprecated() != null )
                    {
                        if ( StringUtils.isEmpty( parameter.getDeprecated() ) )
                        {
                            PluginUtils.element( w, "deprecated", "No reason given" );
                        }
                        else
                        {
                            PluginUtils.element( w, "deprecated", parameter.getDeprecated() );
                        }
                    }

                    if ( parameter.getImplementation() != null )
                    {
                        PluginUtils.element( w, "implementation", parameter.getImplementation() );
                    }

                    PluginUtils.element( w, "required", Boolean.toString( parameter.isRequired() ) );

                    PluginUtils.element( w, "editable", Boolean.toString( parameter.isEditable() ) );
                    if ( cleanDescription )
                    {
                        PluginUtils.element( w, "description", PluginUtils.toText( parameter.getDescription() ) );
                    }
                    else
                    {
                        PluginUtils.element( w, "description", parameter.getDescription() );
                    }

                    if ( StringUtils.isNotEmpty( parameter.getDefaultValue() ) || StringUtils.isNotEmpty(
                        parameter.getExpression() ) )
                    {
                        configuration.add( parameter );
                    }

                    w.endElement();
                }

            }
        }

        w.endElement();

        // ----------------------------------------------------------------------
        // Configuration
        // ----------------------------------------------------------------------

        if ( !configuration.isEmpty() )
        {
            w.startElement( "configuration" );

            for ( Parameter parameter : configuration )
            {
                w.startElement( parameter.getName() );

                String type = parameter.getType();
                if ( type != null )
                {
                    w.addAttribute( "implementation", type );
                }

                if ( parameter.getDefaultValue() != null )
                {
                    w.addAttribute( "default-value", parameter.getDefaultValue() );
                }

                if ( parameter.getExpression() != null )
                {
                    w.writeText( parameter.getExpression() );
                }

                w.endElement();
            }

            w.endElement();
        }

        // ----------------------------------------------------------------------
        // Requirements
        // ----------------------------------------------------------------------

        if ( !requirements.isEmpty() )
        {
            w.startElement( "requirements" );

            for ( Map.Entry<String, Requirement> entry : requirements.entrySet() )
            {
                String key = entry.getKey();
                Requirement requirement = entry.getValue();

                w.startElement( "requirement" );

                PluginUtils.element( w, "role", requirement.getRole() );

                if ( requirement.getRoleHint() != null )
                {
                    PluginUtils.element( w, "role-hint", requirement.getRoleHint() );
                }

                PluginUtils.element( w, "field-name", key );

                w.endElement();
            }

            w.endElement();
        }

        w.endElement();
    }
}
