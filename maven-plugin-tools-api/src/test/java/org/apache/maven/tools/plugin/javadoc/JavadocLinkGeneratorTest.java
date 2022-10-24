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

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;

import org.apache.maven.tools.plugin.javadoc.FullyQualifiedJavadocReference.MemberType;
import org.codehaus.plexus.languages.java.version.JavaVersion;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JavadocLinkGeneratorTest
{

    @ParameterizedTest
    @MethodSource( "org.apache.maven.tools.plugin.javadoc.JavadocSiteTest#jdkNamesAndVersions")
    void testCreateLinkFromFullyQualifiedJavadocReference( String jdkName, JavadocLinkGenerator.JavadocToolVersionRange versionRange, String version )
        throws URISyntaxException
    {
        URI javadocBaseUri = getClass().getResource( "/javadoc/" + jdkName + "/" ).toURI();
        JavadocLinkGenerator linkGenerator =
            new JavadocLinkGenerator( javadocBaseUri, version );
        // invalid link (must not throw exceptions for internal links
        assertEquals( javadocBaseUri.resolve( new URI( null,
                                                       "some/unknown/package/package-summary.html",
                                                       null ) ),
                      linkGenerator.createLink( new FullyQualifiedJavadocReference( "some.unknown.package", false ) ) );
        // valid link to package
        assertEquals( javadocBaseUri.resolve( new URI( null,
                                                       "org/apache/maven/tools/plugin/extractor/annotations/converter/test/package-summary.html",
                                                       null ) ),
                      linkGenerator.createLink( new FullyQualifiedJavadocReference( "org.apache.maven.tools.plugin.extractor.annotations.converter.test", false ) ) );
        // valid link to class
        assertEquals( javadocBaseUri.resolve( new URI( null,
                                                       "org/apache/maven/tools/plugin/extractor/annotations/converter/test/CurrentClass.html",
                                                       null ) ),
                      linkGenerator.createLink( new FullyQualifiedJavadocReference( "org.apache.maven.tools.plugin.extractor.annotations.converter.test",
                                                                                    "CurrentClass", false ) ) );
        // valid link to field
        assertEquals( javadocBaseUri.resolve( new URI( null,
                                                       "org/apache/maven/tools/plugin/extractor/annotations/converter/test/CurrentClass.html",
                                                       "field1" ) ),
                      linkGenerator.createLink( new FullyQualifiedJavadocReference( "org.apache.maven.tools.plugin.extractor.annotations.converter.test",
                                                                                    "CurrentClass", "field1",
                                                                                    MemberType.FIELD, false ) ) );
        // valid link to method
        final String expectedFragment;
        if ( jdkName.equals( "jdk8" ) )
        {
            expectedFragment = "noParamMethod--";
        }
        else
        {
            expectedFragment = "noParamMethod()";
        }
        assertEquals( javadocBaseUri.resolve( new URI( null,
                                                       "org/apache/maven/tools/plugin/extractor/annotations/converter/test/CurrentClass.html",
                                                       expectedFragment ) ),
                      linkGenerator.createLink( new FullyQualifiedJavadocReference( "org.apache.maven.tools.plugin.extractor.annotations.converter.test",
                                                                                    "CurrentClass", "noParamMethod()",
                                                                                    MemberType.METHOD, false ) ) );

    }

    @ParameterizedTest
    @MethodSource( "org.apache.maven.tools.plugin.javadoc.JavadocSiteTest#jdkNamesAndVersions")
    void testCreateLinkFromBinaryName( String jdkName, JavadocLinkGenerator.JavadocToolVersionRange versionRange, String version )
        throws URISyntaxException
    {
        URI javadocBaseUri = getClass().getResource( "/javadoc/" + jdkName + "/" ).toURI();
        JavadocLinkGenerator linkGenerator = new JavadocLinkGenerator( javadocBaseUri, version );
        // invalid link for primitives
        assertThrows( IllegalArgumentException.class, () -> linkGenerator.createLink( "boolean" ) );
        // link for array
        assertEquals( javadocBaseUri.resolve( new URI( null,
                                                       "java/lang/String.html",
                                                       null ) ),
                      linkGenerator.createLink( "java.lang.String[]" ) );

    }

    @Test
    void testGetMatchingJavadocToolVersionRange()
    {
        assertEquals( JavadocLinkGenerator.JavadocToolVersionRange.JDK7_OR_LOWER,  JavadocLinkGenerator.JavadocToolVersionRange.findMatch( JavaVersion.parse("1.5.0") ) );
        assertEquals( JavadocLinkGenerator.JavadocToolVersionRange.JDK7_OR_LOWER,  JavadocLinkGenerator.JavadocToolVersionRange.findMatch( JavaVersion.parse("1.7.0_123") ) );
        assertEquals( JavadocLinkGenerator.JavadocToolVersionRange.JDK10_OR_HIGHER,  JavadocLinkGenerator.JavadocToolVersionRange.findMatch( JavaVersion.parse("10") ) );
        assertEquals( JavadocLinkGenerator.JavadocToolVersionRange.JDK10_OR_HIGHER,  JavadocLinkGenerator.JavadocToolVersionRange.findMatch( JavaVersion.parse("22") ) );
        assertEquals( JavadocLinkGenerator.JavadocToolVersionRange.JDK8_OR_9, JavadocLinkGenerator.JavadocToolVersionRange.findMatch( JavaVersion.parse("1.8.0_345") ) );
        assertEquals( JavadocLinkGenerator.JavadocToolVersionRange.JDK8_OR_9, JavadocLinkGenerator.JavadocToolVersionRange.findMatch( JavaVersion.parse("9.1.1") ) );
    }

    @Test
    void testInaccessibleBaseUri() throws URISyntaxException
    {
        // construction fails as no valid site URL is given
        assertThrows( IllegalArgumentException.class, () -> 
            new JavadocLinkGenerator( Collections.singletonList( new URI( "https://example.com/apidocs/" ) ), null ) );

    }
}
