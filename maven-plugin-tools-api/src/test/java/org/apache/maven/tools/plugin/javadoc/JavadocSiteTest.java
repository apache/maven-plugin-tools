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
import java.net.URISyntaxException;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.apache.maven.tools.plugin.javadoc.FullyQualifiedJavadocReference.MemberType;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.opentest4j.AssertionFailedError;

/**
 * Tests against the locally available javadoc sites. Doesn't require internet connectivity.
 */
class JavadocSiteTest
{

    static Stream<Arguments> jdkNamesAndVersions()
    {
        return Stream.of( Arguments.of( "jdk17", JavadocLinkGenerator.JavadocToolVersionRange.JDK10_OR_HIGHER, "17" ),
                          Arguments.of( "jdk11", JavadocLinkGenerator.JavadocToolVersionRange.JDK10_OR_HIGHER, "11" ), 
                          Arguments.of( "jdk8", JavadocLinkGenerator.JavadocToolVersionRange.JDK8_OR_9, "8" ) );
    }

    @ParameterizedTest
    @MethodSource( "jdkNamesAndVersions" )
    void testConstructorLink( String jdkName, JavadocLinkGenerator.JavadocToolVersionRange version )
        throws URISyntaxException, IOException
    {
        JavadocSite site = getLocalJavadocSite( jdkName, version );
        assertUrlValid( site.createLink( new FullyQualifiedJavadocReference( "org.apache.maven.tools.plugin.extractor.annotations.converter.test", "CurrentClass",
                                                                             "CurrentClass()", MemberType.CONSTRUCTOR, false ) ) );
    }

    @ParameterizedTest
    @MethodSource( "jdkNamesAndVersions" )
    void testMethodLinks( String jdkName, JavadocLinkGenerator.JavadocToolVersionRange version )
        throws URISyntaxException, IOException
    {
        JavadocSite site = getLocalJavadocSite( jdkName, version );
        // test generics signature
        assertUrlValid( site.createLink( new FullyQualifiedJavadocReference( "org.apache.maven.tools.plugin.extractor.annotations.converter.test", "CurrentClass",
                                                                             "genericsParamMethod(java.util.Collection,java.util.function.BiConsumer)", MemberType.METHOD, false ) ) );
        // test array
        assertUrlValid( site.createLink( new FullyQualifiedJavadocReference( "org.apache.maven.tools.plugin.extractor.annotations.converter.test", "CurrentClass",
                                                                             "arrayParamMethod(int[],java.lang.String[][][])", MemberType.METHOD, false ) ) );
    }

    @ParameterizedTest
    @MethodSource( "jdkNamesAndVersions" )
    void testFieldLink( String jdkName, JavadocLinkGenerator.JavadocToolVersionRange version )
        throws URISyntaxException, IOException
    {
        JavadocSite site = getLocalJavadocSite( jdkName, version );
        assertUrlValid( site.createLink( new FullyQualifiedJavadocReference( "org.apache.maven.tools.plugin.extractor.annotations.converter.test", "CurrentClass",
                                                                             "field1", MemberType.FIELD, false ) ) );
    }

    @ParameterizedTest
    @MethodSource( "jdkNamesAndVersions" )
    void testPackageLink( String jdkName, JavadocLinkGenerator.JavadocToolVersionRange version )
        throws URISyntaxException, IOException
    {
        JavadocSite site = getLocalJavadocSite( jdkName, version );
        assertUrlValid( site.createLink( new FullyQualifiedJavadocReference( "org.apache.maven.tools.plugin.extractor.annotations.converter.test", false ) ) );
    }

    @ParameterizedTest
    @MethodSource( "jdkNamesAndVersions" )
    void testClassLink( String jdkName, JavadocLinkGenerator.JavadocToolVersionRange version )
        throws URISyntaxException, IOException
    {
        JavadocSite site = getLocalJavadocSite( jdkName, version );
        assertUrlValid( site.createLink( new FullyQualifiedJavadocReference( "org.apache.maven.tools.plugin.extractor.annotations.converter.test", "CurrentClass", false ) ) );
    }

    static JavadocSite getLocalJavadocSite( String name, JavadocLinkGenerator.JavadocToolVersionRange version )
        throws IOException, URISyntaxException
    {
        URI javadocBaseUri = JavadocSiteTest.class.getResource( "/javadoc/" + name + "/" ).toURI();
        return new JavadocSite( javadocBaseUri, version, false );
    }

    static void assertUrlValid( final URI url )
    {
        try ( BufferedReader reader = JavadocSite.getReader( url.toURL(), null ) )
        {
            if ( url.getFragment() != null )
            {
                Pattern pattern = JavadocSite.getAnchorPattern( url.getFragment() );
                if ( !reader.lines().anyMatch( pattern.asPredicate() ) )
                {
                    throw new AssertionFailedError( "Although URL " + url + " exists, no line matching the pattern "
                        + pattern + " found in response" );
                }
            }
        }
        catch ( IOException e )
        {
            throw new AssertionFailedError( "Could not find URL " + url, e );
        }
    }
}
