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

/**
 * @author <a href="mailto:jason@maven.org">Jason van Zyl </a>
 * @author <a href="mailto:vincent.siveton@gmail.com">Vincent Siveton</a>
 * @version $Id$
 */
public class PluginXdocGeneratorTest
    extends AbstractGeneratorTestCase
{
    public void testMakeHtmlValid()
    {
        String javadoc = "";
        assertEquals( "", PluginXdocGenerator.makeHtmlValid( javadoc ) );

        // true HTML
        javadoc = "Generates <i>something</i> for the project.";
        assertEquals( "Generates <i>something</i> for the project.", PluginXdocGenerator.makeHtmlValid( javadoc ) );

        // wrong HTML
        javadoc = "Generates <i>something</i> <b> for the project.";
        assertEquals( "Generates <i>something</i> <b> for the project.</b>", PluginXdocGenerator
            .makeHtmlValid( javadoc ) );
    }

    public void testDecodeJavadocTags()
    {
        String javadoc = null;
        assertEquals( "", PluginXdocGenerator.decodeJavadocTags( javadoc ) );

        javadoc = "";
        assertEquals( "", PluginXdocGenerator.decodeJavadocTags( javadoc ) );

        javadoc = "{@code text}";
        assertEquals( "<code>text</code>", PluginXdocGenerator.decodeJavadocTags( javadoc ) );

        javadoc = "{@code <A&B>}";
        assertEquals( "<code>&lt;A&amp;B&gt;</code>", PluginXdocGenerator.decodeJavadocTags( javadoc ) );

        javadoc = "{@literal text}";
        assertEquals( "text", PluginXdocGenerator.decodeJavadocTags( javadoc ) );

        javadoc = "{@literal text}  {@literal text}";
        assertEquals( "text  text", PluginXdocGenerator.decodeJavadocTags( javadoc ) );

        javadoc = "{@literal <A&B>}";
        assertEquals( "&lt;A&amp;B&gt;", PluginXdocGenerator.decodeJavadocTags( javadoc ) );

        javadoc = "{@link Class}";
        assertEquals( "<code>Class</code>", PluginXdocGenerator.decodeJavadocTags( javadoc ) );

        javadoc = "{@linkplain Class}";
        assertEquals( "Class", PluginXdocGenerator.decodeJavadocTags( javadoc ) );

        javadoc = "{@linkplain #field}";
        assertEquals( "field", PluginXdocGenerator.decodeJavadocTags( javadoc ) );

        javadoc = "{@linkplain Class#field}";
        assertEquals( "Class.field", PluginXdocGenerator.decodeJavadocTags( javadoc ) );

        javadoc = "{@linkplain #method()}";
        assertEquals( "method()", PluginXdocGenerator.decodeJavadocTags( javadoc ) );

        javadoc = "{@linkplain #method(Object arg)}";
        assertEquals( "method()", PluginXdocGenerator.decodeJavadocTags( javadoc ) );

        javadoc = "{@linkplain #method(Object, String)}";
        assertEquals( "method()", PluginXdocGenerator.decodeJavadocTags( javadoc ) );

        javadoc = "{@linkplain #method(Object, String) label}";
        assertEquals( "label", PluginXdocGenerator.decodeJavadocTags( javadoc ) );

        javadoc = "{@linkplain Class#method(Object, String)}";
        assertEquals( "Class.method()", PluginXdocGenerator.decodeJavadocTags( javadoc ) );

        javadoc = "{@linkplain Class#method(Object, String) label}";
        assertEquals( "label", PluginXdocGenerator.decodeJavadocTags( javadoc ) );
    }
}
