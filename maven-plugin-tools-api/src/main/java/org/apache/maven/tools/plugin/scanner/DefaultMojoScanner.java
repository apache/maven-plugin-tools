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
import org.apache.maven.tools.plugin.extractor.GroupKey;
import org.apache.maven.tools.plugin.extractor.MojoDescriptorExtractor;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.logging.console.ConsoleLogger;
import org.codehaus.plexus.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
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
    @Override
    public void populatePluginDescriptor( PluginToolsRequest request )
        throws ExtractionException, InvalidPluginDescriptorException
    {
        Logger logger = getLogger();

        int numMojoDescriptors = 0;

        List<MojoDescriptorExtractor> orderedExtractors = getOrderedExtractors();

        logger.debug( "Using " + orderedExtractors.size() + " mojo extractors." );

        HashMap<String, Integer> groupStats = new HashMap<>();

        for ( MojoDescriptorExtractor extractor : orderedExtractors )
        {
            GroupKey groupKey = extractor.getGroupKey();
            String extractorId = extractor.getName();

            if ( extractor.isDeprecated() )
            {
                if ( groupStats.containsKey( groupKey.getGroup() )
                      && groupStats.get( groupKey.getGroup() ) != 0 )
                {
                    logger.info( extractorId + " deprecated mojo extractor skipped as group '" + groupKey.getGroup()
                        + "' already discovered " + groupStats.get( groupKey.getGroup() )
                        + " mojo descriptor" + ( groupStats.get( groupKey.getGroup() ) > 1 ? "s" : "" ) + "." );
                    continue; // if same group already found descriptors, skip rest of the group
                }
            }

            logger.debug( "Applying " + extractorId + " mojo extractor" );

            List<MojoDescriptor> extractorDescriptors = extractor.execute( request );

            int extractorDescriptorsCount = extractorDescriptors.size();

            logger.info( extractorId + " mojo extractor found " + extractorDescriptors.size()
                             + " mojo descriptor" + ( extractorDescriptors.size() > 1 ? "s" : "" ) + "." );
            numMojoDescriptors += extractorDescriptors.size();

            if ( extractor.isDeprecated() &&  extractorDescriptorsCount > 0 )
            {
                logger.warn( "" );
                logger.warn( "Deprecated extractor extracted descriptors. Upgrade your Mojo definitions." );
                logger.warn( "" );
            }

            if ( groupStats.containsKey( groupKey.getGroup() ) )
            {
                groupStats.put( groupKey.getGroup(),
                    groupStats.get( groupKey.getGroup() ) + extractorDescriptorsCount );
            }
            else
            {
                groupStats.put( groupKey.getGroup(),
                    extractorDescriptorsCount );
            }

            for ( MojoDescriptor descriptor : extractorDescriptors )
            {
                logger.debug( "Adding mojo: " + descriptor + " to plugin descriptor." );

                descriptor.setPluginDescriptor( request.getPluginDescriptor() );

                request.getPluginDescriptor().addMojo( descriptor );
            }
        }

        logger.debug( "Discovered descriptors by groups: " + groupStats );

        if ( numMojoDescriptors == 0 && !request.isSkipErrorNoDescriptorsFound() )
        {
            throw new InvalidPluginDescriptorException(
                "No mojo definitions were found for plugin: " + request.getPluginDescriptor().getPluginLookupKey()
                    + "." );
        }
    }

    private List<MojoDescriptorExtractor> getOrderedExtractors() throws ExtractionException
    {
        Set<String> extractors = activeExtractors;

        if ( extractors == null )
        {
            extractors = new HashSet<>( mojoDescriptorExtractors.keySet() );
        }

        ArrayList<MojoDescriptorExtractor> orderedExtractors = new ArrayList<>();
        for ( String extractorId : extractors )
        {
            MojoDescriptorExtractor extractor = mojoDescriptorExtractors.get( extractorId );

            if ( extractor == null )
            {
                throw new ExtractionException( "No mojo extractor with '" + extractorId + "' id." );
            }

            orderedExtractors.add( extractor );
        }

        Collections.sort( orderedExtractors, new Comparator<MojoDescriptorExtractor>()
        {
            @Override
            public int compare( final MojoDescriptorExtractor o1, final MojoDescriptorExtractor o2 )
            {
                return o1.getGroupKey().compareTo( o2.getGroupKey() );
            }
        } );

        return orderedExtractors;
    }

    @Override
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
