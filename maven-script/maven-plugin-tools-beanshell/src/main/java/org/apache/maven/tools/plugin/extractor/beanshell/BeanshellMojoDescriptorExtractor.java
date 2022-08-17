package org.apache.maven.tools.plugin.extractor.beanshell;

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

import javax.inject.Named;
import javax.inject.Singleton;

import static java.nio.charset.StandardCharsets.UTF_8;

import bsh.EvalError;
import bsh.Interpreter;
import org.apache.maven.plugin.descriptor.InvalidPluginDescriptorException;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.tools.plugin.PluginToolsRequest;
import org.apache.maven.tools.plugin.extractor.AbstractScriptedMojoDescriptorExtractor;
import org.apache.maven.tools.plugin.extractor.ExtractionException;
import org.apache.maven.tools.plugin.extractor.GroupKey;

import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Extracts Mojo descriptors from <a href="http://www.beanshell.org/">BeanShell</a> sources.
 *
 * @deprecated Scripting support for mojos is deprecated and is planned tp be removed in maven 4.0
 */
@Deprecated
@Named( BeanshellMojoDescriptorExtractor.NAME )
@Singleton
public class BeanshellMojoDescriptorExtractor
    extends AbstractScriptedMojoDescriptorExtractor
{
    public static final String NAME = "bsh";

    private static final GroupKey GROUP_KEY = new GroupKey( "bsh", 100 );

    @Override
    public String getName()
    {
        return NAME;
    }

    @Override
    public GroupKey getGroupKey()
    {
        return GROUP_KEY;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String getScriptFileExtension( PluginToolsRequest request )
    {
        return ".bsh";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected List<MojoDescriptor> extractMojoDescriptors( Map<String, Set<File>> scriptFilesKeyedByBasedir,
                                                           PluginToolsRequest request )
        throws ExtractionException, InvalidPluginDescriptorException
    {
        List<MojoDescriptor> descriptors = new ArrayList<>();

        for ( Map.Entry<String, Set<File>> entry : scriptFilesKeyedByBasedir.entrySet() )
        {
            String basedir = entry.getKey();
            Set<File> metadataFiles = entry.getValue();

            for ( File scriptFile : metadataFiles )
            {
                String relativePath = null;

                if ( basedir.endsWith( "/" ) )
                {
                    basedir = basedir.substring( 0, basedir.length() - 2 );
                }

                relativePath = scriptFile.getPath().substring( basedir.length() );

                relativePath = relativePath.replace( '\\', '/' );

                MojoDescriptor mojoDescriptor = createMojoDescriptor( basedir, relativePath, request );
                descriptors.add( mojoDescriptor );
            }
        }

        return descriptors;
    }

    /**
     * @param basedir  not null
     * @param resource not null
     * @param request  not null
     * @return a new Mojo descriptor instance
     * @throws InvalidPluginDescriptorException
     *          if any
     */
    private MojoDescriptor createMojoDescriptor( String basedir, String resource, PluginToolsRequest request )
        throws InvalidPluginDescriptorException
    {
        MojoDescriptor mojoDescriptor = new MojoDescriptor();
        mojoDescriptor.setPluginDescriptor( request.getPluginDescriptor() );

        mojoDescriptor.setLanguage( "bsh" );
        mojoDescriptor.setComponentConfigurator( "bsh" );

        mojoDescriptor.setImplementation( resource );

        Interpreter interpreter = new Interpreter();

        try
        {
            interpreter.set( "file", new File( basedir, resource ) );

            interpreter.set( "mojoDescriptor", mojoDescriptor );

            interpreter.set( "encoding", "UTF-8" );

            interpreter.eval( new InputStreamReader( getClass().getResourceAsStream( "/extractor.bsh" ), UTF_8 ) );
        }
        catch ( EvalError evalError )
        {
            throw new InvalidPluginDescriptorException( "Error scanning beanshell script", evalError );
        }

        // FIXME: convert javadocs
        return mojoDescriptor;
    }
}
