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
 * @author <a href="mailto:vincent.siveton@gmail.com">Vincent Siveton</a>
 * @version $Id$
 */
public class PluginHelpGeneratorTest
    extends AbstractGeneratorTestCase
{
    public void testToText()
        throws Exception
    {
        String javadoc = null;
        assertEquals( "", PluginHelpGenerator.toText( javadoc ) );
        javadoc = "";
        assertEquals( "", PluginHelpGenerator.toText( javadoc ) );

        // line breaks
        javadoc = "Line1\nLine2";
        assertEquals( "Line1 Line2", PluginHelpGenerator.toText( javadoc ) );
        javadoc = "Line1\rLine2";
        assertEquals( "Line1 Line2", PluginHelpGenerator.toText( javadoc ) );
        javadoc = "Line1\r\nLine2";
        assertEquals( "Line1 Line2", PluginHelpGenerator.toText( javadoc ) );
        javadoc = "Line1<br>Line2";
        assertEquals( "Line1\nLine2", PluginHelpGenerator.toText( javadoc ) );

        // true HTML
        javadoc = "Generates <i>something</i> for the project.";
        assertEquals( "Generates something for the project.", PluginHelpGenerator.toText( javadoc ) );

        // wrong HTML
        javadoc = "Generates <i>something</i> <b> for the project.";
        assertEquals( "Generates something for the project.", PluginHelpGenerator.toText( javadoc ) );

        // javadoc inline tags
        javadoc = "Generates {@code something} for the project.";
        assertEquals( "Generates something for the project.", PluginHelpGenerator.toText( javadoc ) );
    }
}
