package org.apache.maven.tools.plugin.extractor.model;

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

import org.apache.maven.plugin.descriptor.DuplicateParameterException;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.plugin.descriptor.Parameter;
import org.apache.maven.tools.plugin.extractor.model.io.xpp3.PluginMetadataXpp3Reader;
import org.codehaus.plexus.component.repository.ComponentRequirement;
import org.codehaus.plexus.util.ReaderFactory;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Parser for plugin metadata.
 */
public class PluginMetadataParser
{
    /**
     * Default implementation path which will be replaced in
     * AbstractScriptedMojoDescriptorExtractor#extractMojoDescriptorsFromMetadata(Map, PluginDescriptor)
     */
    public static final String IMPL_BASE_PLACEHOLDER = "<REPLACE-WITH-MOJO-PATH>";

    /**
     * @param metadataFile the metadata file to be parse
     * @return a set of <code>MojoDescriptor</code>
     * @throws PluginMetadataParseException if any
     */
    public Set<MojoDescriptor> parseMojoDescriptors( File metadataFile )
        throws PluginMetadataParseException
    {
        Set<MojoDescriptor> descriptors = new HashSet<>();

        try ( Reader reader = ReaderFactory.newXmlReader( metadataFile ) )
        {

            PluginMetadataXpp3Reader metadataReader = new PluginMetadataXpp3Reader();

            PluginMetadata pluginMetadata = metadataReader.read( reader );

            List<Mojo> mojos = pluginMetadata.getMojos();

            if ( mojos != null )
            {
                for ( Mojo mojo : mojos )
                {
                    MojoDescriptor descriptor = asDescriptor( metadataFile, mojo );

                    descriptors.add( descriptor );
                }
            }
        }
        catch ( IOException | XmlPullParserException e )
        {
            throw new PluginMetadataParseException( metadataFile, "Cannot parse plugin metadata from file.", e );
        }

        return descriptors;
    }

    /**
     * @param metadataFile not null
     * @param mojo         not null
     * @return a mojo descriptor instance
     * @throws PluginMetadataParseException if any
     */
    private MojoDescriptor asDescriptor( File metadataFile, Mojo mojo )
        throws PluginMetadataParseException
    {
        MojoDescriptor descriptor = new MojoDescriptor();

        if ( mojo.getCall() != null )
        {
            descriptor.setImplementation( IMPL_BASE_PLACEHOLDER + ":" + mojo.getCall() );
        }
        else
        {
            descriptor.setImplementation( IMPL_BASE_PLACEHOLDER );
        }

        descriptor.setGoal( mojo.getGoal() );
        descriptor.setPhase( mojo.getPhase() );
        descriptor.setDependencyResolutionRequired( mojo.getRequiresDependencyResolution() );
        descriptor.setAggregator( mojo.isAggregator() );
        descriptor.setInheritedByDefault( mojo.isInheritByDefault() );
        descriptor.setDirectInvocationOnly( mojo.isRequiresDirectInvocation() );
        descriptor.setOnlineRequired( mojo.isRequiresOnline() );
        descriptor.setProjectRequired( mojo.isRequiresProject() );
        descriptor.setRequiresReports( mojo.isRequiresReports() );
        descriptor.setDescription( mojo.getDescription() );
        descriptor.setDeprecated( mojo.getDeprecation() );
        descriptor.setSince( mojo.getSince() );

        LifecycleExecution le = mojo.getExecution();
        if ( le != null )
        {
            descriptor.setExecuteLifecycle( le.getLifecycle() );
            descriptor.setExecutePhase( le.getPhase() );
            descriptor.setExecuteGoal( le.getGoal() );
        }

        List<org.apache.maven.tools.plugin.extractor.model.Parameter> parameters = mojo.getParameters();

        if ( parameters != null && !parameters.isEmpty() )
        {
            for ( org.apache.maven.tools.plugin.extractor.model.Parameter param : parameters )
            {
                Parameter dParam = new Parameter();
                dParam.setAlias( param.getAlias() );
                dParam.setDeprecated( param.getDeprecation() );
                dParam.setDescription( param.getDescription() );
                dParam.setEditable( !param.isReadonly() );
                dParam.setExpression( param.getExpression() );
                dParam.setDefaultValue( param.getDefaultValue() );
                dParam.setSince( param.getSince() );

                String property = param.getProperty();
                if ( StringUtils.isNotEmpty( property ) )
                {
                    dParam.setName( property );
                }
                else
                {
                    dParam.setName( param.getName() );
                }

                if ( StringUtils.isEmpty( dParam.getName() ) )
                {
                    throw new PluginMetadataParseException( metadataFile, "Mojo: \'" + mojo.getGoal()
                        + "\' has a parameter without either property or name attributes. Please specify one." );
                }

                dParam.setRequired( param.isRequired() );
                dParam.setType( param.getType() );

                try
                {
                    descriptor.addParameter( dParam );
                }
                catch ( DuplicateParameterException e )
                {
                    throw new PluginMetadataParseException( metadataFile,
                                                            "Duplicate parameters detected for mojo: " + mojo.getGoal(),
                                                            e );
                }
            }
        }

        List<Component> components = mojo.getComponents();

        if ( components != null && !components.isEmpty() )
        {
            for ( Component component : components )
            {
                ComponentRequirement cr = new ComponentRequirement();
                cr.setRole( component.getRole() );
                cr.setRoleHint( component.getHint() );

                descriptor.addRequirement( cr );
            }
        }

        return descriptor;
    }
}
