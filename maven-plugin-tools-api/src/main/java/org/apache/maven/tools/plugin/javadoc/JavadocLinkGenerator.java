package org.apache.maven.tools.plugin.javadoc;

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

import java.io.BufferedReader;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

import org.apache.maven.settings.Settings;
import org.codehaus.plexus.languages.java.version.JavaVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Generates links for elements (packages, classes, fields, constructors, methods) in external
 * and/or an internal (potentially not yet existing) javadoc site.
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
        JDK7_OR_LOWER( null, JavaVersion.parse( "1.8" ) ),
        JDK8_OR_9( JavaVersion.parse( "1.8" ), JavaVersion.parse( "10" ) ),
        JDK10_OR_HIGHER( JavaVersion.parse( "10" ), null );
        
        // upper bound is exclusive, lower bound inclusive (null means unlimited)
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
                if ( ( range.lowerBound == null || javadocVersion.isAtLeast( range.lowerBound ) )
                     && ( range.upperBound == null || javadocVersion.isBefore( range.upperBound ) ) )
                {
                    return range;
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
        if ( externalJavadocSiteUrls != null )
        {
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
        else
        {
            externalJavadocSites = Collections.emptyList();
        }
        if ( internalJavadocSite == null && externalJavadocSites.isEmpty() )
        {
            throw new IllegalArgumentException( "Either internal or at least one accessible external javadoc "
                                                + "URLs must be given!" );
        }
    }

    /**
     * Generates a (deep-)link to a HTML page in any of the sites given to the constructor.
     * The link is not validated (i.e. might point to a non-existing page).
     * Only uses the offline site for references returning {@code false} for
     * {@link FullyQualifiedJavadocReference#isExternal()}.
     * @param javadocReference
     * @return the (deep-) link towards a javadoc page
     * @throws IllegalArgumentException in case no javadoc link could be generated for the given reference
     * @throws IllegalStateException in case no javadoc source sites have been configured
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

    /**
     * Generates a (deep-)link to a HTML page in any of the sites given to the constructor.
     * The link is not validated (i.e. might point to a non-existing page).
     * Preferably resolves from the online sites if they provide the given package.
     * @param binaryName a binary name according to 
     * <a href="https://docs.oracle.com/javase/specs/jls/se8/html/jls-13.html#jls-13.1">JLS 13.1</a>
     * @return the (deep-) link towards a javadoc page
     * @throws IllegalArgumentException in case no javadoc link could be generated for the given name
     */
    public URI createLink( String binaryName )
    {
        Map.Entry<String, String> packageAndClassName = JavadocSite.getPackageAndClassName( binaryName );
        // first check external links, otherwise assume internal link
        JavadocSite javadocSite = externalJavadocSites.stream()
                        .filter( base -> base.hasEntryFor( Optional.empty(),
                                                           Optional.of( packageAndClassName.getKey() ) ) )
                        .findFirst().orElse( null );
        if ( javadocSite == null )
        {
            if ( internalJavadocSite != null )
            {
                javadocSite = internalJavadocSite;
            }
            else
            {
                throw new IllegalArgumentException( "Found no javadoc site for " + binaryName );
            }
        }
        return javadocSite.createLink( packageAndClassName.getKey(), packageAndClassName.getValue() );
    }

    public URI getInternalJavadocSiteBaseUrl()
    {
        if ( internalJavadocSite == null )
        {
            throw new IllegalStateException( "Could not get docroot of internal javadoc as it hasn't been set" );
        }
        return internalJavadocSite.getBaseUri();
    }
    
    /**
     * Checks if a given link is valid. For absolute links uses the underling {@link java.net.HttpURLConnection},
     * otherwise checks for existence of the file on the filesystem.
     * 
     * @param url the url to check
     * @param baseDirectory the base directory to which relative file URLs refer
     * @return {@code true} in case the given link is valid otherwise {@code false}
     */
    public static boolean isLinkValid( URI url, Path baseDirectory )
    {
        if ( url.isAbsolute() )
        {
            try ( BufferedReader reader = JavadocSite.getReader( url.toURL(), null ) )
            {
                if ( url.getFragment() != null )
                {
                    Pattern pattern = JavadocSite.getAnchorPattern( url.getFragment() );
                    if ( reader.lines().noneMatch( pattern.asPredicate() ) )
                    {
                        return false;
                    }
                }
            }
            catch ( IOException e )
            {
                return false;
            }
            return true;
        }
        else
        {
            Path file = baseDirectory.resolve( url.getPath() );
            boolean exists = Files.exists( file );
            if ( !exists )
            {
                LOG.debug( "Could not find file given through '{}' in resolved path '{}'", url, file );
            }
            return exists;
        }
    }
}
