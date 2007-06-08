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
import org.apache.maven.tools.plugin.extractor.AbstractScriptedMojoDescriptorExtractor;
import org.apache.maven.tools.plugin.extractor.ExtractionException;
import org.codehaus.plexus.util.StringUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class AntMojoDescriptorExtractor
    extends AbstractScriptedMojoDescriptorExtractor
{

    private static final String METADATA_FILE_EXTENSION = ".mojos.xml";

    private static final String SCRIPT_FILE_EXTENSION = ".build.xml";

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

                if ( basedir.endsWith( "/" ) )
                {
                    basedir = basedir.substring( 0, basedir.length() - 2 );
                }

                relativePath = scriptFile.getPath().substring( basedir.length() );

                relativePath = relativePath.replace( '\\', '/' );

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

                        String implementation = relativePath;

                        String dImpl = descriptor.getImplementation();
                        if ( StringUtils.isNotEmpty( dImpl ) )
                        {
                            implementation =
                                relativePath + dImpl.substring( PluginMetadataParser.IMPL_BASE_PLACEHOLDER.length() );
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

    protected String getScriptFileExtension()
    {
        return SCRIPT_FILE_EXTENSION;
    }

    protected String getMetadataFileExtension()
    {
        return METADATA_FILE_EXTENSION;
    }
}
