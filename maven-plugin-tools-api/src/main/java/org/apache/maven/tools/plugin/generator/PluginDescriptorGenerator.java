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
import org.apache.maven.plugin.descriptor.Requirement;
import org.apache.maven.tools.plugin.DefaultPluginToolsRequest;
import org.apache.maven.tools.plugin.ExtendedMojoDescriptor;
import org.apache.maven.tools.plugin.PluginToolsRequest;
import org.apache.maven.tools.plugin.util.PluginUtils;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.xml.PrettyPrintXMLWriter;
import org.codehaus.plexus.util.xml.XMLWriter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @todo add example usage tag that can be shown in the doco
 * @todo need to add validation directives so that systems embedding maven2 can
 * get validation directives to help users in IDEs.
 *
 * @version $Id$
 */
public class PluginDescriptorGenerator
    implements Generator
{
    /** {@inheritDoc} */
    public void execute( File destinationDirectory, PluginDescriptor pluginDescriptor )
        throws IOException
    {
        execute( destinationDirectory, new DefaultPluginToolsRequest( null, pluginDescriptor ) );
    }
    
    /** {@inheritDoc} */
    public void execute( File destinationDirectory, PluginToolsRequest request )
        throws IOException
    {
        PluginDescriptor pluginDescriptor = request.getPluginDescriptor();
        
        String encoding = "UTF-8";

        File f = new File( destinationDirectory, "plugin.xml" );

        if ( !f.getParentFile().exists() )
        {
            f.getParentFile().mkdirs();
        }

        Writer writer = null;
        try
        {
            writer = new OutputStreamWriter( new FileOutputStream( f ), encoding );

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
                for ( Iterator it = pluginDescriptor.getMojos().iterator(); it.hasNext(); )
                {
                    MojoDescriptor descriptor = (MojoDescriptor) it.next();
                    processMojoDescriptor( descriptor, w );
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
     * @param mojoDescriptor not null
     * @param w not null
     */
    protected void processMojoDescriptor( MojoDescriptor mojoDescriptor, XMLWriter w )
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
            w.writeText( mojoDescriptor.getDescription() );
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
            if ( extendedMojoDescriptor.getRequiresDependencyCollection() != null )
            {
                PluginUtils.element( w, "requiresDependencyCollection",
                                     extendedMojoDescriptor.getRequiresDependencyCollection() );
            }

            PluginUtils.element( w, "threadSafe", "" + ( (ExtendedMojoDescriptor) mojoDescriptor ).isThreadSafe() );

        }

        // ----------------------------------------------------------------------
        // Parameters
        // ----------------------------------------------------------------------

        List parameters = mojoDescriptor.getParameters();

        w.startElement( "parameters" );

        Map requirements = new LinkedHashMap();

        Set configuration = new LinkedHashSet();

        if ( parameters != null )
        {
            for ( int j = 0; j < parameters.size(); j++ )
            {
                Parameter parameter = (Parameter) parameters.get( j );

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

                    PluginUtils.element( w, "description", parameter.getDescription() );

                    if ( StringUtils.isNotEmpty( parameter.getDefaultValue() )
                        || StringUtils.isNotEmpty( parameter.getExpression() ) )
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

            for ( Iterator i = configuration.iterator(); i.hasNext(); )
            {
                Parameter parameter = (Parameter) i.next();

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

            for ( Iterator i = requirements.keySet().iterator(); i.hasNext(); )
            {
                String key = (String) i.next();
                Requirement requirement = (Requirement) requirements.get( key );

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
