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

import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.plugin.descriptor.Parameter;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugin.descriptor.Requirement;
import org.apache.maven.project.MavenProject;
import org.apache.maven.tools.plugin.ExtendedMojoDescriptor;
import org.apache.maven.tools.plugin.PluginToolsRequest;
import org.apache.maven.tools.plugin.javadoc.JavadocLinkGenerator;
import org.apache.maven.tools.plugin.util.PluginUtils;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.io.CachingOutputStream;
import org.codehaus.plexus.util.xml.PrettyPrintXMLWriter;
import org.codehaus.plexus.util.xml.XMLWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Serializes
 * <ol>
 * <li>a standard <a href="/ref/current/maven-plugin-api/plugin.html">Maven Plugin Descriptor XML file</a></li>
 * <li>a descriptor containing a limited set of attributes for {@link PluginHelpGenerator}</li>
 * <li>an enhanced descriptor containing HTML values for some elements (instead of plain text as for the other two)
 * for {@link PluginXdocGenerator}</li>
 * </ol>
 * from a given in-memory descriptor. The in-memory descriptor acting as source is supposed to contain XHTML values
 * for description elements.
 *
 */
public class PluginDescriptorFilesGenerator
    implements Generator
{
    private static final Logger LOG = LoggerFactory.getLogger( PluginDescriptorFilesGenerator.class );

    /**
     * The type of the plugin descriptor file
     */
    enum DescriptorType
    {
        STANDARD,
        LIMITED_FOR_HELP_MOJO,
        XHTML
    }

    @Override
    public void execute( File destinationDirectory, PluginToolsRequest request )
        throws GeneratorException
    {
        try
        {
            // write standard plugin.xml descriptor
            File f = new File( destinationDirectory, "plugin.xml" );
            writeDescriptor( f, request, DescriptorType.STANDARD );

            // write plugin-help.xml help-descriptor (containing only a limited set of attributes)
            MavenProject mavenProject = request.getProject();
            f = new File( destinationDirectory,
                          PluginHelpGenerator.getPluginHelpPath( mavenProject ) );
            writeDescriptor( f, request, DescriptorType.LIMITED_FOR_HELP_MOJO );

            // write enhanced plugin-enhanced.xml descriptor (containing some XHTML values)
            f = getEnhancedDescriptorFilePath( mavenProject );
            writeDescriptor( f, request, DescriptorType.XHTML );
        }
        catch ( IOException e )
        {
            throw new GeneratorException( e.getMessage(), e );
        }
    }

    public static File getEnhancedDescriptorFilePath( MavenProject project )
    {
        return new File( project.getBuild().getDirectory(), "plugin-enhanced.xml" );
    }

    private String getVersion()
    {
        Package p = this.getClass().getPackage();
        String version = ( p == null ) ? null : p.getSpecificationVersion();
        return ( version == null ) ? "SNAPSHOT" : version;
    }

    public void writeDescriptor( File destinationFile, PluginToolsRequest request, DescriptorType type )
        throws IOException
    {
        PluginDescriptor pluginDescriptor = request.getPluginDescriptor();

        if ( !destinationFile.getParentFile().exists() )
        {
            destinationFile.getParentFile().mkdirs();
        }

        try ( Writer writer = new OutputStreamWriter( new CachingOutputStream( destinationFile ), UTF_8 ) )
        {
            XMLWriter w = new PrettyPrintXMLWriter( writer, UTF_8.name(), null );
            
            final String additionalInfo;
            switch ( type )
            {
                case LIMITED_FOR_HELP_MOJO:
                    additionalInfo = " (for help'mojo with additional elements)";
                    break;
                case XHTML:
                    additionalInfo = " (enhanced XHTML version with additional elements (used for plugin:report))";
                    break;
                default:
                    additionalInfo = "";
                    break;
            }
            w.writeMarkup( "\n<!-- Generated by maven-plugin-tools " + getVersion() 
                           + additionalInfo + "-->\n\n" );

            w.startElement( "plugin" );

            GeneratorUtils.element( w, "name", pluginDescriptor.getName() );

            GeneratorUtils.element( w, "description", pluginDescriptor.getDescription() );

            GeneratorUtils.element( w, "groupId", pluginDescriptor.getGroupId() );

            GeneratorUtils.element( w, "artifactId", pluginDescriptor.getArtifactId() );

            GeneratorUtils.element( w, "version", pluginDescriptor.getVersion() );

            GeneratorUtils.element( w, "goalPrefix", pluginDescriptor.getGoalPrefix() );

            if ( type != DescriptorType.LIMITED_FOR_HELP_MOJO )
            {
                GeneratorUtils.element( w, "isolatedRealm", String.valueOf( pluginDescriptor.isIsolatedRealm() ) );

                GeneratorUtils.element( w, "inheritedByDefault",
                                        String.valueOf( pluginDescriptor.isInheritedByDefault() ) );
            }

            w.startElement( "mojos" );

            final JavadocLinkGenerator javadocLinkGenerator;
            if ( request.getInternalJavadocBaseUrl() != null 
                 || ( request.getExternalJavadocBaseUrls() != null 
                      && !request.getExternalJavadocBaseUrls().isEmpty() ) )
            {
                javadocLinkGenerator =  new JavadocLinkGenerator( request.getInternalJavadocBaseUrl(),
                                                                  request.getInternalJavadocVersion(),
                                                                  request.getExternalJavadocBaseUrls(),
                                                                  request.getSettings() );
            }
            else
            {
                javadocLinkGenerator = null;
            }
            if ( pluginDescriptor.getMojos() != null )
            {
                List<MojoDescriptor> descriptors = pluginDescriptor.getMojos();

                PluginUtils.sortMojos( descriptors );

                for ( MojoDescriptor descriptor : descriptors )
                {
                    processMojoDescriptor( descriptor, w, type, javadocLinkGenerator );
                }
            }

            w.endElement();

            if ( type != DescriptorType.LIMITED_FOR_HELP_MOJO )
            {
                GeneratorUtils.writeDependencies( w, pluginDescriptor );
            }

            w.endElement();

            writer.flush();

        }
    }

    /**
     * 
     * @param type
     * @param containsXhtmlValue
     * @param text
     * @return the normalized text value (i.e. potentially converted to XHTML)
     */
    private static String getTextValue( DescriptorType type, boolean containsXhtmlValue, String text )
    {
        final String xhtmlText;
        if ( !containsXhtmlValue ) // text comes from legacy extractor
        {
            xhtmlText = GeneratorUtils.makeHtmlValid( text );
        }
        else
        {
            xhtmlText = text;
        }
        if ( type != DescriptorType.XHTML )
        {
            return new HtmlToPlainTextConverter().convert( text );
        }
        else
        {
            return xhtmlText;
        }
    }

    @SuppressWarnings( "deprecation" )
    protected void processMojoDescriptor( MojoDescriptor mojoDescriptor, XMLWriter w, DescriptorType type,
                                          JavadocLinkGenerator javadocLinkGenerator )
    {
        boolean containsXhtmlTextValues = mojoDescriptor instanceof ExtendedMojoDescriptor
                        && ( (ExtendedMojoDescriptor) mojoDescriptor ).containsXhtmlTextValues();
        
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

        if ( StringUtils.isNotEmpty( description ) )
        {
            w.startElement( "description" );
            w.writeText( getTextValue( type, containsXhtmlTextValues, mojoDescriptor.getDescription() ) );
            w.endElement();
        }

        // ----------------------------------------------------------------------
        //
        // ----------------------------------------------------------------------

        if ( StringUtils.isNotEmpty( mojoDescriptor.isDependencyResolutionRequired() ) )
        {
            GeneratorUtils.element( w, "requiresDependencyResolution",
                                    mojoDescriptor.isDependencyResolutionRequired() );
        }

        // ----------------------------------------------------------------------
        //
        // ----------------------------------------------------------------------

        GeneratorUtils.element( w, "requiresDirectInvocation",
                                String.valueOf( mojoDescriptor.isDirectInvocationOnly() ) );

        // ----------------------------------------------------------------------
        //
        // ----------------------------------------------------------------------

        GeneratorUtils.element( w, "requiresProject", String.valueOf( mojoDescriptor.isProjectRequired() ) );

        // ----------------------------------------------------------------------
        //
        // ----------------------------------------------------------------------

        GeneratorUtils.element( w, "requiresReports", String.valueOf( mojoDescriptor.isRequiresReports() ) );

        // ----------------------------------------------------------------------
        //
        // ----------------------------------------------------------------------

        GeneratorUtils.element( w, "aggregator", String.valueOf( mojoDescriptor.isAggregator() ) );

        // ----------------------------------------------------------------------
        //
        // ----------------------------------------------------------------------

        GeneratorUtils.element( w, "requiresOnline", String.valueOf( mojoDescriptor.isOnlineRequired() ) );

        // ----------------------------------------------------------------------
        //
        // ----------------------------------------------------------------------

        GeneratorUtils.element( w, "inheritedByDefault", String.valueOf( mojoDescriptor.isInheritedByDefault() ) );

        // ----------------------------------------------------------------------
        //
        // ----------------------------------------------------------------------

        if ( StringUtils.isNotEmpty( mojoDescriptor.getPhase() ) )
        {
            GeneratorUtils.element( w, "phase", mojoDescriptor.getPhase() );
        }

        // ----------------------------------------------------------------------
        //
        // ----------------------------------------------------------------------

        if ( StringUtils.isNotEmpty( mojoDescriptor.getExecutePhase() ) )
        {
            GeneratorUtils.element( w, "executePhase", mojoDescriptor.getExecutePhase() );
        }

        if ( StringUtils.isNotEmpty( mojoDescriptor.getExecuteGoal() ) )
        {
            GeneratorUtils.element( w, "executeGoal", mojoDescriptor.getExecuteGoal() );
        }

        if ( StringUtils.isNotEmpty( mojoDescriptor.getExecuteLifecycle() ) )
        {
            GeneratorUtils.element( w, "executeLifecycle", mojoDescriptor.getExecuteLifecycle() );
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

        if ( StringUtils.isNotEmpty( mojoDescriptor.getComponentConfigurator() ) )
        {
            w.startElement( "configurator" );
            w.writeText( mojoDescriptor.getComponentConfigurator() );
            w.endElement();
        }

        // ----------------------------------------------------------------------
        //
        // ----------------------------------------------------------------------

        if ( StringUtils.isNotEmpty( mojoDescriptor.getComponentComposer() ) )
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

        if ( mojoDescriptor.getSince() != null )
        {
            w.startElement( "since" );

            if ( StringUtils.isEmpty( mojoDescriptor.getSince() ) )
            {
                w.writeText( "No version given" );
            }
            else
            {
                w.writeText( mojoDescriptor.getSince() );
            }

            w.endElement();
        }

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
                w.writeText( getTextValue( type, containsXhtmlTextValues, mojoDescriptor.getDeprecated() ) );
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
                GeneratorUtils.element( w, "requiresDependencyCollection",
                                        extendedMojoDescriptor.getDependencyCollectionRequired() );
            }

            GeneratorUtils.element( w, "threadSafe", String.valueOf( extendedMojoDescriptor.isThreadSafe() ) );
        }

        // ----------------------------------------------------------------------
        // Parameters
        // ----------------------------------------------------------------------

        List<Parameter> parameters = mojoDescriptor.getParameters();

        w.startElement( "parameters" );

        Map<String, Requirement> requirements = new LinkedHashMap<>();

        Set<Parameter> configuration = new LinkedHashSet<>();

        if ( parameters != null )
        {
            if ( type == DescriptorType.LIMITED_FOR_HELP_MOJO )
            {
                PluginUtils.sortMojoParameters( parameters );
            }

            for ( Parameter parameter : parameters )
            {
                String expression = getExpression( parameter );

                if ( StringUtils.isNotEmpty( expression ) && expression.startsWith( "${component." ) )
                {
                    // treat it as a component...a requirement, in other words.

                    // remove "component." plus expression delimiters
                    String role = expression.substring( "${component.".length(), expression.length() - 1 );

                    String roleHint = null;

                    int posRoleHintSeparator = role.indexOf( '#' );
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
                // don't show readonly parameters in help
                else if ( type != DescriptorType.LIMITED_FOR_HELP_MOJO || parameter.isEditable() )
                {
                    // treat it as a normal parameter.

                    w.startElement( "parameter" );

                    GeneratorUtils.element( w, "name", parameter.getName() );

                    if ( parameter.getAlias() != null )
                    {
                        GeneratorUtils.element( w, "alias", parameter.getAlias() );
                    }

                    writeParameterType( w, type, javadocLinkGenerator, parameter, mojoDescriptor.getGoal() );

                    if ( parameter.getSince() != null )
                    {
                        w.startElement( "since" );

                        if ( StringUtils.isEmpty( parameter.getSince() ) )
                        {
                            w.writeText( "No version given" );
                        }
                        else
                        {
                            w.writeText( parameter.getSince() );
                        }

                        w.endElement();
                    }

                    if ( parameter.getDeprecated() != null )
                    {
                        if ( StringUtils.isEmpty( parameter.getDeprecated() ) )
                        {
                            GeneratorUtils.element( w, "deprecated", "No reason given" );
                        }
                        else
                        {
                            GeneratorUtils.element( w, "deprecated", 
                                                    getTextValue( type, containsXhtmlTextValues,
                                                                  parameter.getDeprecated() ) );
                        }
                    }

                    if ( parameter.getImplementation() != null )
                    {
                        GeneratorUtils.element( w, "implementation", parameter.getImplementation() );
                    }

                    GeneratorUtils.element( w, "required", Boolean.toString( parameter.isRequired() ) );

                    GeneratorUtils.element( w, "editable", Boolean.toString( parameter.isEditable() ) );

                    GeneratorUtils.element( w, "description", getTextValue( type, containsXhtmlTextValues,
                                                                            parameter.getDescription() ) );

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
                if ( type == DescriptorType.LIMITED_FOR_HELP_MOJO && !parameter.isEditable() )
                {
                    // don't show readonly parameters in help
                    continue;
                }

                w.startElement( parameter.getName() );

                // strip type by parameter type (generics) information
                String parameterType = StringUtils.chomp( parameter.getType(), "<" );
                if ( StringUtils.isNotEmpty( parameterType ) )
                {
                    w.addAttribute( "implementation", parameterType );
                }

                if ( parameter.getDefaultValue() != null )
                {
                    w.addAttribute( "default-value", parameter.getDefaultValue() );
                }

                if ( StringUtils.isNotEmpty( parameter.getExpression() ) )
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

        if ( !requirements.isEmpty() && type != DescriptorType.LIMITED_FOR_HELP_MOJO )
        {
            w.startElement( "requirements" );

            for ( Map.Entry<String, Requirement> entry : requirements.entrySet() )
            {
                String key = entry.getKey();
                Requirement requirement = entry.getValue();

                w.startElement( "requirement" );

                GeneratorUtils.element( w, "role", requirement.getRole() );

                if ( StringUtils.isNotEmpty( requirement.getRoleHint() ) )
                {
                    GeneratorUtils.element( w, "role-hint", requirement.getRoleHint() );
                }

                GeneratorUtils.element( w, "field-name", key );

                w.endElement();
            }

            w.endElement();
        }

        w.endElement();
    }

    /**
     * Writes parameter type information and potentially also the related javadoc URL.
     * @param w
     * @param type
     * @param javadocLinkGenerator
     * @param parameter
     * @param goal
     */
    protected void writeParameterType( XMLWriter w, DescriptorType type, JavadocLinkGenerator javadocLinkGenerator,
                                       Parameter parameter, String goal )
    {
        String parameterType = parameter.getType();
        
        if ( type == DescriptorType.STANDARD )
        {
            // strip type by parameter type (generics) information for standard plugin descriptor
            parameterType = StringUtils.chomp( parameterType, "<" );
        }
        GeneratorUtils.element( w, "type", parameterType );

        if ( type == DescriptorType.XHTML && javadocLinkGenerator != null )
        {
            // skip primitives which never has javadoc
            if ( parameter.getType().indexOf( '.' ) == -1 )
            {
                LOG.debug( "Javadoc URLs are not available for primitive types like {}",
                           parameter.getType() );
            }
            else
            {
                try
                {
                    URI javadocUrl = getJavadocUrlForType( javadocLinkGenerator, parameterType );
                    GeneratorUtils.element( w, "typeJavadocUrl", javadocUrl.toString() );
                } 
                catch ( IllegalArgumentException e )
                {
                    LOG.warn( "Could not get javadoc URL for type {} of parameter {} from goal {}: {}",
                              parameter.getType(), parameter.getName(), goal,
                              e.getMessage() );
                }
            }
        }
    }

    static URI getJavadocUrlForType( JavadocLinkGenerator javadocLinkGenerator, String type )
    {
        final String binaryName;
        int startOfParameterType = type.indexOf( "<" );
        if ( startOfParameterType != -1 )
        {
            // parse parameter type
            String mainType = type.substring( 0, startOfParameterType );
            
            // some heuristics here
            String[] parameterTypes = type.substring( startOfParameterType + 1, type.lastIndexOf( ">" ) )
                            .split( ",\\s*" );
            switch ( parameterTypes.length )
            {
                case 1: // if only one parameter type, assume collection, first parameter type is most interesting
                    binaryName = parameterTypes[0];
                    break;
                case 2: // if two parameter types assume map, second parameter type is most interesting
                    binaryName = parameterTypes[1];
                    break;
                default:
                    // all other cases link to main type
                    binaryName = mainType;
            }
        }
        else
        {
            binaryName = type;
        }
        return javadocLinkGenerator.createLink( binaryName );
    }

    /**
     * Get the expression value, eventually surrounding it with <code>${ }</code>.
     *
     * @param parameter the parameter
     * @return the expression value
     */
    private String getExpression( Parameter parameter )
    {
        String expression = parameter.getExpression();
        if ( StringUtils.isNotBlank( expression ) && !expression.contains( "${" ) )
        {
            expression = "${" + expression.trim() + "}";
            parameter.setExpression( expression );
        }
        return expression;
    }
}
