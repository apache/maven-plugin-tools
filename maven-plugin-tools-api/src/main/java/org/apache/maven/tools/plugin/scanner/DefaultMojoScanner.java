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
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.project.MavenProject;
import org.apache.maven.tools.plugin.DefaultPluginToolsRequest;
import org.apache.maven.tools.plugin.PluginToolsRequest;
import org.apache.maven.tools.plugin.extractor.ExtractionException;
import org.apache.maven.tools.plugin.extractor.MojoDescriptorExtractor;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.logging.console.ConsoleLogger;

import java.util.HashSet;
import java.util.Iterator;
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

    private Map mojoDescriptorExtractors;

    /**
     * The names of the active extractors
     */
    private Set/* <String> */activeExtractors;

    /**
     * Default constructor
     *
     * @param extractors not null
     */
    public DefaultMojoScanner( Map extractors )
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

    /** {@inheritDoc} */
    public void populatePluginDescriptor( MavenProject project, PluginDescriptor pluginDescriptor )
        throws ExtractionException, InvalidPluginDescriptorException
    {
        populatePluginDescriptor( new DefaultPluginToolsRequest( project, pluginDescriptor ) );
    }
    
    /** {@inheritDoc} */
    public void populatePluginDescriptor( PluginToolsRequest request )
        throws ExtractionException, InvalidPluginDescriptorException
    {
        Logger logger = getLogger();
        Set activeExtractorsInternal = getActiveExtractors();

        logger.debug( "Using " + activeExtractorsInternal.size() + " mojo extractors." );

        int numMojoDescriptors = 0;

        for ( Iterator it = activeExtractorsInternal.iterator(); it.hasNext(); )
        {
            String language = (String) it.next();
            MojoDescriptorExtractor extractor = (MojoDescriptorExtractor) mojoDescriptorExtractors.get( language );

            if ( extractor == null )
            {
                throw new ExtractionException( "No mojo extractor for language: " + language );
            }

            logger.info( "Applying mojo extractor for language: " + language );

            List extractorDescriptors = extractor.execute( request );

            logger.info( "Mojo extractor for language: " + language + " found " + extractorDescriptors.size()
                + " mojo descriptors." );
            numMojoDescriptors += extractorDescriptors.size();

            for ( Iterator descriptorIt = extractorDescriptors.iterator(); descriptorIt.hasNext(); )
            {
                MojoDescriptor descriptor = (MojoDescriptor) descriptorIt.next();

                logger.debug( "Adding mojo: " + descriptor + " to plugin descriptor." );

                descriptor.setPluginDescriptor( request.getPluginDescriptor() );

                request.getPluginDescriptor().addMojo( descriptor );
            }
        }

        if ( numMojoDescriptors == 0 )
        {
            throw new InvalidPluginDescriptorException( "No mojo definitions were found for plugin: "
                + request.getPluginDescriptor().getPluginLookupKey() + "." );
        }
    }

    /**
     * Gets the name of the active extractors.
     *
     * @return A Set containing the names of the active extractors.
     */
    protected Set/* <String> */getActiveExtractors()
    {
        Set/* <String> */result = activeExtractors;

        if ( result == null )
        {
            result = new HashSet/* <String> */( mojoDescriptorExtractors.keySet() );
        }

        return result;
    }

    /** {@inheritDoc} */
    public void setActiveExtractors( Set/* <String> */extractors )
    {
        if ( extractors == null )
        {
            this.activeExtractors = null;
        }
        else
        {
            this.activeExtractors = new HashSet/* <String> */();

            for ( Iterator i = extractors.iterator(); i.hasNext(); )
            {
                String extractor = (String) i.next();

                if ( extractor != null && extractor.length() > 0 )
                {
                    this.activeExtractors.add( extractor );
                }
            }
        }
    }

}
