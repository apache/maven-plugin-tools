package org.apache.maven.tools.plugin.scanner;

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
import org.apache.maven.tools.plugin.PluginToolsRequest;
import org.apache.maven.tools.plugin.extractor.ExtractionException;
import org.apache.maven.tools.plugin.extractor.MojoDescriptorExtractor;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.logging.console.ConsoleLogger;
import org.codehaus.plexus.util.StringUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author jdcasey
 */
public class DefaultMojoScanner
    extends AbstractLogEnabled
    implements MojoScanner
{

    private Map<String, MojoDescriptorExtractor> mojoDescriptorExtractors;

    /**
     * The names of the active extractors
     */
    private Set<String> activeExtractors;

    /**
     * Default constructor
     *
     * @param extractors not null
     */
    public DefaultMojoScanner( Map<String, MojoDescriptorExtractor> extractors )
    {
        this.mojoDescriptorExtractors = extractors;

        this.enableLogging( new ConsoleLogger( Logger.LEVEL_INFO, "standalone-scanner-logger" ) );
    }

    /**
     * Empty constructor
     */
    public DefaultMojoScanner()
    {
        // nop
    }

    /**
     * {@inheritDoc}
     */
    public void populatePluginDescriptor( PluginToolsRequest request )
        throws ExtractionException, InvalidPluginDescriptorException
    {
        Logger logger = getLogger();
        Set<String> activeExtractorsInternal = getActiveExtractors();

        logger.debug( "Using " + activeExtractorsInternal.size() + " mojo extractors." );

        int numMojoDescriptors = 0;

        for ( String extractorId : activeExtractorsInternal )
        {
            MojoDescriptorExtractor extractor = mojoDescriptorExtractors.get( extractorId );

            if ( extractor == null )
            {
                throw new ExtractionException( "No mojo extractor with '" + extractorId + "' id." );
            }

            logger.debug( "Applying " + extractorId + " mojo extractor" );

            List<MojoDescriptor> extractorDescriptors = extractor.execute( request );

            logger.info( extractorId + " mojo extractor found " + extractorDescriptors.size()
                             + " mojo descriptor" + ( extractorDescriptors.size() > 1 ? "s" : "" ) + "." );
            numMojoDescriptors += extractorDescriptors.size();

            for ( MojoDescriptor descriptor : extractorDescriptors )
            {
                logger.debug( "Adding mojo: " + descriptor + " to plugin descriptor." );

                descriptor.setPluginDescriptor( request.getPluginDescriptor() );

                request.getPluginDescriptor().addMojo( descriptor );
            }
        }

        if ( numMojoDescriptors == 0 && !request.isSkipErrorNoDescriptorsFound() )
        {
            throw new InvalidPluginDescriptorException(
                "No mojo definitions were found for plugin: " + request.getPluginDescriptor().getPluginLookupKey()
                    + "." );
        }
    }

    /**
     * Gets the name of the active extractors.
     *
     * @return A Set containing the names of the active extractors.
     */
    protected Set<String> getActiveExtractors()
    {
        Set<String> result = activeExtractors;

        if ( result == null )
        {
            result = new HashSet<>( mojoDescriptorExtractors.keySet() );
        }

        return result;
    }

    public void setActiveExtractors( Set<String> extractors )
    {
        if ( extractors == null )
        {
            this.activeExtractors = null;
        }
        else
        {
            this.activeExtractors = new HashSet<>();

            for ( String extractor : extractors )
            {
                if ( StringUtils.isNotEmpty( extractor ) )
                {
                    this.activeExtractors.add( extractor );
                }
            }
        }
    }

}
