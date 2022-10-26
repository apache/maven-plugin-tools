package org.apache.maven.tools.plugin.generator;

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

import java.io.File;
import java.io.InputStream;
import java.util.Locale;

import org.codehaus.plexus.util.ReaderFactory;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.Xpp3DomBuilder;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author <a href="mailto:jason@maven.org">Jason van Zyl </a>
 * @author <a href="mailto:vincent.siveton@gmail.com">Vincent Siveton</a>
 */
public class PluginXdocGeneratorTest
    extends AbstractGeneratorTestCase
{

    // inherits tests from base class

    @Override
    protected void validate( File destinationDirectory )
        throws Exception
    {
        File docFile = new File( destinationDirectory, "testGoal-mojo.xml" );
        Xpp3Dom actual = Xpp3DomBuilder.build( ReaderFactory.newXmlReader( docFile ) );

        InputStream expectedAsStream = getClass().getResourceAsStream( "/expected-testGoal-mojo.xml" );

        Xpp3Dom expected = Xpp3DomBuilder.build( ReaderFactory.newXmlReader( expectedAsStream ) );

        Assertions.assertEquals( expected, actual );

    }

    @Test
    void testGetShortType()
    {
        assertEquals("String", PluginXdocGenerator.getShortType( "java.lang.String" ) );
        assertEquals("List<String>", PluginXdocGenerator.getShortType( "java.util.List<java.lang.String>" ) );
        assertEquals("Map<String,Integer>", PluginXdocGenerator.getShortType( "java.util.Map<java.lang.String,java.lang.Integer>" ) );
        assertEquals("List<...>", PluginXdocGenerator.getShortType( "java.util.List<java.util.List<java.lang.String>>" ) );
    }

    @Test
    void testGetXhtmlWithValidatedLinks()
    {
        File baseDir = new File( this.getClass().getResource( "" ).getFile() );
        PluginXdocGenerator xdocGenerator = new PluginXdocGenerator( null, Locale.ROOT, baseDir, false );
        PluginXdocGenerator xdocGeneratorWithDisabledLinkValidator = new PluginXdocGenerator( null, Locale.ROOT, baseDir, true );
        String externalLink = "test<a href=\"http://example.com/test\">External Link</a>..and a second<a href=\"http://localhost/example\">link</a>end";
        assertEquals( externalLink, xdocGenerator.getXhtmlWithValidatedLinks( externalLink, "test" ) );
        assertEquals( externalLink, xdocGeneratorWithDisabledLinkValidator.getXhtmlWithValidatedLinks( externalLink, "test" ) );
        String validInternalLink = "test<a href=\"PluginXdocGeneratorTest.class\">Internal Link</a>..and a second<a href=\"http://localhost/example\">link</a>end";
        assertEquals( validInternalLink, xdocGenerator.getXhtmlWithValidatedLinks( validInternalLink, "test" ) );
        assertEquals( validInternalLink, xdocGeneratorWithDisabledLinkValidator.getXhtmlWithValidatedLinks( validInternalLink, "test" ) );
        String invalidInternalLink = "test<a href=\"PluginXdocGeneratorTestinvalid.class\">Internal Link</a>..and a second<a href=\"http://localhost/example\">link</a>end";
        String sanitizedInvalidInternalLink = "testInternal Link..and a second<a href=\"http://localhost/example\">link</a>end";
        assertEquals( sanitizedInvalidInternalLink, xdocGenerator.getXhtmlWithValidatedLinks( invalidInternalLink, "test" ) );
        assertEquals( invalidInternalLink, xdocGeneratorWithDisabledLinkValidator.getXhtmlWithValidatedLinks( invalidInternalLink, "test" ) );
    }
}
