package org.apache.maven.tools.plugin.extractor.annotations.converter;

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

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.maven.settings.Settings;
import org.codehaus.plexus.languages.java.version.JavaVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Generates links for elements (packages, classes, fields, constructors, methods) in external javadoc sites
 * or an internal (potentially not yet existing) one.
 * The external site must be accessible for it to be considered due to the different fragment formats.
 */
public class JavadocLinkGenerator
{
    /**
     * Javadoc tool version ranges whose generated sites expose different link formats.
     *
     */
    public enum JavadocToolVersionRange
    {
        JDK7_OR_LOWER( null, JavaVersion.parse( "1.7" ) ),
        JDK8_OR_9( JavaVersion.parse( "8" ), JavaVersion.parse( "9" ) ),
        JDK10_OR_HIGHER( JavaVersion.parse( "10" ), null );
        
        // both bounds are inclusive
        private final JavaVersion lowerBound;
        private final JavaVersion upperBound;
        JavadocToolVersionRange( JavaVersion lowerBound, JavaVersion upperBound )
        {
            this.lowerBound = lowerBound;
            this.upperBound = upperBound;
        }

        static JavadocToolVersionRange findMatch( JavaVersion javadocVersion )
        {
            for ( JavadocToolVersionRange range : values() )
            {
                if ( range.lowerBound == null || javadocVersion.isAtLeast( range.lowerBound ) )
                {
                    if ( range.upperBound == null || range.upperBound.isAtLeast( javadocVersion ) )
                    {
                        return range;
                    }
                }
            }
            throw new IllegalArgumentException( "Found no matching javadoc tool version range for " + javadocVersion );
        }
    }

    private static final Logger LOG = LoggerFactory.getLogger( JavadocLinkGenerator.class );
    private final List<JavadocSite> externalJavadocSites;
    private final JavadocSite internalJavadocSite; // may be null

    /**
     * Constructor for an offline internal site only.
     * 
     * @param internalJavadocSiteUrl the url of the javadoc generated website
     * @param internalJavadocVersion the version of javadoc with which the internal site from
     *                               {@code internalJavadocSiteUrl} has been generated
     */
    public JavadocLinkGenerator( URI internalJavadocSiteUrl,
                                 String internalJavadocVersion )
    {
        this( internalJavadocSiteUrl, internalJavadocVersion, Collections.emptyList(), null );
    }

    /**
     * Constructor for online external sites only.
     * 
     * @param externalJavadocSiteUrls
     * @param settings
     */
    public JavadocLinkGenerator( List<URI> externalJavadocSiteUrls, Settings settings )
    {
        this( null, null, externalJavadocSiteUrls, settings );
    }

    /**
     * Constructor for both an internal (offline) and external (online) sites.
     * 
     * @param internalJavadocSiteUrl
     * @param internalJavadocVersion
     * @param externalJavadocSiteUrls
     * @param settings
     */
    public JavadocLinkGenerator( URI internalJavadocSiteUrl,
                                 String internalJavadocVersion,
                                 List<URI> externalJavadocSiteUrls, Settings settings )
    {
        if ( internalJavadocSiteUrl != null )
        {
            // resolve version
            JavaVersion javadocVersion = JavaVersion.parse( internalJavadocVersion );
            internalJavadocSite = new JavadocSite( internalJavadocSiteUrl,
                                                   JavadocToolVersionRange.findMatch( javadocVersion ),
                                                   false );
        }
        else
        {
            internalJavadocSite = null;
        }
        externalJavadocSites = new ArrayList<>( externalJavadocSiteUrls.size() );
        for ( URI siteUrl : externalJavadocSiteUrls  )
        {
            try
            {
                externalJavadocSites.add( new JavadocSite( siteUrl, settings ) );
            }
            catch ( IOException e )
            {
                LOG.warn( "Could not use {} as base URL: {}", siteUrl, e.getMessage(), e );
            }
        }
    }

    /**
     * 
     * @param javadocReference
     * @return the generated link
     * @throws IllegalArgumentException in case no javadoc link could be generated for the given reference
     */
    public URI createLink( FullyQualifiedJavadocReference javadocReference )
    {
        if ( !javadocReference.isExternal() && internalJavadocSite != null )
        {
            return internalJavadocSite.createLink( javadocReference );
        }
        else
        {
            JavadocSite javadocSite = externalJavadocSites.stream()
                .filter( base -> base.hasEntryFor( javadocReference.getModuleName(),
                                                   javadocReference.getPackageName() ) )
                .findFirst().orElseThrow( () -> new IllegalArgumentException( "Found no javadoc site for "
                + javadocReference ) );
            return javadocSite.createLink( javadocReference );
        }
    }

    URI getInternalJavadocSiteBaseUrl()
    {
        if ( internalJavadocSite == null )
        {
            throw new IllegalStateException( "Could not get docroot of internal javadoc as it hasn't been set" );
        }
        return internalJavadocSite.getBaseUri();
    }
}
