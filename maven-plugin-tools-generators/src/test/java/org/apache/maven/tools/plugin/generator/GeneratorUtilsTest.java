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

import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.apache.maven.plugin.testing.stubs.MavenProjectStub;
import org.codehaus.plexus.component.repository.ComponentDependency;
import org.codehaus.plexus.util.xml.CompactXMLWriter;
import org.codehaus.plexus.util.xml.XMLWriter;

import java.io.StringWriter;
import java.util.Collections;

/**
 * @author jdcasey
 */
public class GeneratorUtilsTest
    extends AbstractMojoTestCase
{
    public void testShouldWriteDependencies()
        throws Exception
    {
        ComponentDependency dependency = new ComponentDependency();
        dependency.setArtifactId( "testArtifactId" );
        dependency.setGroupId( "testGroupId" );
        dependency.setType( "pom" );
        dependency.setVersion( "0.0.0" );

        PluginDescriptor descriptor = new PluginDescriptor();
        descriptor.setDependencies( Collections.singletonList( dependency ) );

        StringWriter sWriter = new StringWriter();
        XMLWriter writer = new CompactXMLWriter( sWriter );

        GeneratorUtils.writeDependencies( writer, descriptor );

        String output = sWriter.toString();

        String pattern =
            "<dependencies>" + "<dependency>" + "<groupId>testGroupId</groupId>"
                + "<artifactId>testArtifactId</artifactId>" + "<type>pom</type>" + "<version>0.0.0</version>"
                + "</dependency>" + "</dependencies>";

        assertEquals( pattern, output );
    }

    public void testMakeHtmlValid()
    {
        String javadoc = null;
        assertEquals( "", GeneratorUtils.makeHtmlValid( javadoc ) );
        javadoc = "";
        assertEquals( "", GeneratorUtils.makeHtmlValid( javadoc ) );

        // true HTML
        javadoc = "Generates <i>something</i> for the project.";
        assertEquals( "Generates <i>something</i> for the project.", GeneratorUtils.makeHtmlValid( javadoc ) );

        // wrong HTML
        javadoc = "Generates <i>something</i> <b> for the project.";
        assertEquals( "Generates <i>something</i> <b> for the project.</b>", GeneratorUtils.makeHtmlValid( javadoc ) );

        // wrong XHTML
        javadoc = "Line1<br>Line2";
        assertEquals( "Line1<br/>Line2", GeneratorUtils.makeHtmlValid( javadoc ).replaceAll( "\\s", "" ) );

        // special characters
        javadoc = "& &amp; < > \u00A0";
        assertEquals( "&amp; &amp; &lt; &gt; \u00A0", GeneratorUtils.makeHtmlValid( javadoc ) );

        // non ASCII characters
        javadoc = "\u00E4 \u00F6 \u00FC \u00DF";
        assertEquals( javadoc, GeneratorUtils.makeHtmlValid( javadoc ) );

        // non Latin1 characters
        javadoc = "\u0130 \u03A3 \u05D0 \u06DE";
        assertEquals( javadoc, GeneratorUtils.makeHtmlValid( javadoc ) );
    }

    public void testDecodeJavadocTags()
    {
        String javadoc = null;
        assertEquals( "", GeneratorUtils.decodeJavadocTags( javadoc ) );

        javadoc = "";
        assertEquals( "", GeneratorUtils.decodeJavadocTags( javadoc ) );

        javadoc = "{@code text}";
        assertEquals( "<code>text</code>", GeneratorUtils.decodeJavadocTags( javadoc ) );

        javadoc = "{@code <A&B>}";
        assertEquals( "<code>&lt;A&amp;B&gt;</code>", GeneratorUtils.decodeJavadocTags( javadoc ) );

        javadoc = "{@literal text}";
        assertEquals( "text", GeneratorUtils.decodeJavadocTags( javadoc ) );

        javadoc = "{@literal text}  {@literal text}";
        assertEquals( "text  text", GeneratorUtils.decodeJavadocTags( javadoc ) );

        javadoc = "{@literal <A&B>}";
        assertEquals( "&lt;A&amp;B&gt;", GeneratorUtils.decodeJavadocTags( javadoc ) );

        javadoc = "{@link Class}";
        assertEquals( "<code>Class</code>", GeneratorUtils.decodeJavadocTags( javadoc ) );

        javadoc = "{@linkplain Class}";
        assertEquals( "Class", GeneratorUtils.decodeJavadocTags( javadoc ) );

        javadoc = "{@linkplain #field}";
        assertEquals( "field", GeneratorUtils.decodeJavadocTags( javadoc ) );

        javadoc = "{@linkplain Class#field}";
        assertEquals( "Class.field", GeneratorUtils.decodeJavadocTags( javadoc ) );

        javadoc = "{@linkplain #method()}";
        assertEquals( "method()", GeneratorUtils.decodeJavadocTags( javadoc ) );

        javadoc = "{@linkplain #method(Object arg)}";
        assertEquals( "method()", GeneratorUtils.decodeJavadocTags( javadoc ) );

        javadoc = "{@linkplain #method(Object, String)}";
        assertEquals( "method()", GeneratorUtils.decodeJavadocTags( javadoc ) );

        javadoc = "{@linkplain #method(Object, String) label}";
        assertEquals( "label", GeneratorUtils.decodeJavadocTags( javadoc ) );

        javadoc = "{@linkplain Class#method(Object, String)}";
        assertEquals( "Class.method()", GeneratorUtils.decodeJavadocTags( javadoc ) );

        javadoc = "{@linkplain Class#method(Object, String) label}";
        assertEquals( "label", GeneratorUtils.decodeJavadocTags( javadoc ) );
    }

    public void testToText()
        throws Exception
    {
        String javadoc = null;
        assertEquals( "", GeneratorUtils.toText( javadoc ) );
        javadoc = "";
        assertEquals( "", GeneratorUtils.toText( javadoc ) );

        // line breaks
        javadoc = "Line1\nLine2";
        assertEquals( "Line1 Line2", GeneratorUtils.toText( javadoc ) );
        javadoc = "Line1\rLine2";
        assertEquals( "Line1 Line2", GeneratorUtils.toText( javadoc ) );
        javadoc = "Line1\r\nLine2";
        assertEquals( "Line1 Line2", GeneratorUtils.toText( javadoc ) );
        javadoc = "Line1<br>Line2";
        assertEquals( "Line1\nLine2", GeneratorUtils.toText( javadoc ) );

        // true HTML
        javadoc = "Generates <i>something</i> for the project.";
        assertEquals( "Generates something for the project.", GeneratorUtils.toText( javadoc ) );

        // wrong HTML
        javadoc = "Generates <i>something</i> <b> for the project.";
        assertEquals( "Generates something for the project.", GeneratorUtils.toText( javadoc ) );

        // javadoc inline tags
        javadoc = "Generates {@code something} for the project.";
        assertEquals( "Generates something for the project.", GeneratorUtils.toText( javadoc ) );
    }

    public void testIsMavenReport()
        throws Exception
    {
        try
        {
            GeneratorUtils.isMavenReport( null, null );
        }
        catch ( IllegalArgumentException e )
        {
            assertTrue( true );
        }

        String impl = "org.apache.maven.tools.plugin.generator.stubs.MavenReportStub";

        MavenProjectStub stub = new MavenProjectStub();
        stub.setCompileSourceRoots( Collections.singletonList( getBasedir() + "/target/classes" ) );

        assertTrue( GeneratorUtils.isMavenReport( impl, stub ) );

        impl = "org.apache.maven.tools.plugin.util.stubs.MojoStub";
        assertFalse( GeneratorUtils.isMavenReport( impl, stub ) );
    }

}
