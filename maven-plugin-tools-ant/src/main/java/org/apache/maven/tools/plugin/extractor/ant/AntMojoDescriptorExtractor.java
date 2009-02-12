package org.apache.maven.tools.plugin.extractor.ant;

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

import org.apache.maven.plugin.descriptor.InvalidPluginDescriptorException;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.plugin.descriptor.Parameter;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugin.tools.model.PluginMetadataParseException;
import org.apache.maven.plugin.tools.model.PluginMetadataParser;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.path.PathTranslator;
import org.apache.maven.tools.plugin.extractor.AbstractScriptedMojoDescriptorExtractor;
import org.apache.maven.tools.plugin.extractor.ExtractionException;
import org.codehaus.plexus.component.repository.ComponentRequirement;
import org.codehaus.plexus.util.StringUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Extracts Mojo descriptors from <a href="http://ant.apache.org">Ant</a> sources.
 *
 * @version $Id$
 */
public class AntMojoDescriptorExtractor
    extends AbstractScriptedMojoDescriptorExtractor
{
    /** Default metadata file extension */
    private static final String METADATA_FILE_EXTENSION = ".mojos.xml";

    /** Default Ant build file extension */
    private static final String SCRIPT_FILE_EXTENSION = ".build.xml";
    
    /** {@inheritDoc} */
    protected List extractMojoDescriptorsFromMetadata( Map metadataFilesKeyedByBasedir,
                                                       PluginDescriptor pluginDescriptor )
        throws ExtractionException, InvalidPluginDescriptorException
    {
        List descriptors = new ArrayList();

        PluginMetadataParser parser = new PluginMetadataParser();

        for ( Iterator mapIterator = metadataFilesKeyedByBasedir.entrySet().iterator(); mapIterator.hasNext(); )
        {
            Map.Entry entry = (Map.Entry) mapIterator.next();

            String basedir = (String) entry.getKey();
            Set metadataFiles = (Set) entry.getValue();

            for ( Iterator it = metadataFiles.iterator(); it.hasNext(); )
            {
                File metadataFile = (File) it.next();

                String basename = metadataFile.getName();
                basename = basename.substring( 0, basename.length() - METADATA_FILE_EXTENSION.length() );

                File scriptFile = new File( metadataFile.getParentFile(), basename + SCRIPT_FILE_EXTENSION );

                if ( !scriptFile.exists() )
                {
                    throw new InvalidPluginDescriptorException(
                        "Found orphaned plugin metadata file: " + metadataFile );
                }

                String relativePath = null;

                relativePath = scriptFile.getPath().substring( basedir.length() );
                relativePath = relativePath.replace( '\\', '/' );
                
                if ( relativePath.startsWith( "/" ) )
                {
                    relativePath = relativePath.substring( 1 );
                }

                try
                {
                    Set mojoDescriptors = parser.parseMojoDescriptors( metadataFile );

                    for ( Iterator discoveredMojoIterator = mojoDescriptors.iterator(); discoveredMojoIterator
                        .hasNext(); )
                    {
                        MojoDescriptor descriptor = (MojoDescriptor) discoveredMojoIterator.next();

                        Map paramMap = descriptor.getParameterMap();

                        if ( !paramMap.containsKey( "basedir" ) )
                        {
                            Parameter param = new Parameter();
                            param.setName( "basedir" );
                            param.setAlias( "ant.basedir" );
                            param.setExpression( "${antBasedir}" );
                            param.setDefaultValue( "${basedir}" );
                            param.setType( "java.io.File" );
                            param.setDescription( "The base directory from which to execute the Ant script." );
                            param.setEditable( true );
                            param.setRequired( true );

                            descriptor.addParameter( param );
                        }

                        if ( !paramMap.containsKey( "antMessageLevel" ) )
                        {
                            Parameter param = new Parameter();
                            param.setName( "messageLevel" );
                            param.setAlias( "ant.messageLevel" );
                            param.setExpression( "${antMessageLevel}" );
                            param.setDefaultValue( "info" );
                            param.setType( "java.lang.String" );
                            param.setDescription( "The message-level used to tune the verbosity of Ant logging." );
                            param.setEditable( true );
                            param.setRequired( false );

                            descriptor.addParameter( param );
                        }
                        
                        if ( !paramMap.containsKey( "project" ) )
                        {
                            Parameter param = new Parameter();
                            param.setName( "project" );
                            param.setDefaultValue( "${project}" );
                            param.setType( MavenProject.class.getName() );
                            param.setDescription( "The current MavenProject instance, which contains classpath elements." );
                            param.setEditable( false );
                            param.setRequired( true );

                            descriptor.addParameter( param );
                        }

                        if ( !paramMap.containsKey( "session" ) )
                        {
                            Parameter param = new Parameter();
                            param.setName( "session" );
                            param.setDefaultValue( "${session}" );
                            param.setType( "org.apache.maven.execution.MavenSession" );
                            param.setDescription( "The current MavenSession instance, which is used for plugin-style expression resolution." );
                            param.setEditable( false );
                            param.setRequired( true );

                            descriptor.addParameter( param );
                        }

                        if ( !paramMap.containsKey( "mojoExecution" ) )
                        {
                            Parameter param = new Parameter();
                            param.setName( "mojoExecution" );
                            param.setDefaultValue( "${mojoExecution}" );
                            param.setType( "org.apache.maven.plugin.MojoExecution" );
                            param.setDescription( "The current Maven MojoExecution instance, which contains information about the mojo currently executing." );
                            param.setEditable( false );
                            param.setRequired( true );

                            descriptor.addParameter( param );
                        }
                        
                        List requirements = descriptor.getRequirements();
                        Map reqMap = new HashMap();

                        if ( requirements != null )
                        {
                            for ( Iterator reqIterator = requirements.iterator(); reqIterator.hasNext(); )
                            {
                                ComponentRequirement req = (ComponentRequirement) reqIterator.next();

                                reqMap.put( req.getRole(), req );
                            }
                        }
                        
                        if ( !reqMap.containsKey( PathTranslator.class.getName() ) )
                        {
                            ComponentRequirement req = new ComponentRequirement();
                            req.setRole( PathTranslator.class.getName() );
                            
                            descriptor.addRequirement( req );
                        }

                        String implementation = relativePath;

                        String dImpl = descriptor.getImplementation();
                        if ( StringUtils.isNotEmpty( dImpl ) )
                        {
                            if ( PluginMetadataParser.IMPL_BASE_PLACEHOLDER.equals( dImpl ) )
                            {
                                implementation = relativePath;
                            }
                            else
                            {
                                implementation =
                                    relativePath + dImpl.substring( PluginMetadataParser.IMPL_BASE_PLACEHOLDER.length() );
                            }
                        }

                        descriptor.setImplementation( implementation );

                        descriptor.setLanguage( "ant-mojo" );
                        descriptor.setComponentComposer( "map-oriented" );
                        descriptor.setComponentConfigurator( "map-oriented" );

                        descriptor.setPluginDescriptor( pluginDescriptor );

                        descriptors.add( descriptor );
                    }
                }
                catch ( PluginMetadataParseException e )
                {
                    throw new ExtractionException( "Error extracting mojo descriptor from script: " + metadataFile, e );
                }
            }
        }

        return descriptors;
    }

    /** {@inheritDoc} */
    protected String getScriptFileExtension()
    {
        return SCRIPT_FILE_EXTENSION;
    }

    /** {@inheritDoc} */
    protected String getMetadataFileExtension()
    {
        return METADATA_FILE_EXTENSION;
    }
}
