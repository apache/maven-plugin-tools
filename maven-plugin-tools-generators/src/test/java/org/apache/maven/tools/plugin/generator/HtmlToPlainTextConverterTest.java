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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class HtmlToPlainTextConverterTest
{
    private final Converter converter;

    public HtmlToPlainTextConverterTest()
    {
        converter = new HtmlToPlainTextConverter();
    }

    @Test
    void testConvertWithCodeAndLink()
    {
        String test =
            "This is a <code>code</code> and <a href=\"https://javadoc.example.com/some/javadoc.html\">Link</a>";
        assertEquals( "This is a code and Link <https://javadoc.example.com/some/javadoc.html>",
                      converter.convert( test ) );
    }

    @Test
    void testMultilineJavadocAndWordWrap()
    {
        String test = "Generates a <a href=\"https://jackrabbit.apache.org/jcr/node-type-notation.html\">CND file</a> containing all\n"
            + "used node types and namespaces. It uses the <a href=\"https://s.apache.org/jcr-2.0-spec/3_Repository_Model.html#3.7.11%20Standard%20Application%20Node%20Types\">default namespaces and node types</a>\n"
            + "and in addition some provided ones as source node type and namespace registry.";
        assertEquals( "Generates a CND file <https://jackrabbit.apache.org/jcr/node-type-notation.html> "
            + "containing all used node types and namespaces. It uses the default namespaces"
            + " and node types <https://s.apache.org/jcr-2.0-spec/3_Repository_Model.html#3.7.11%20Standard%20Application%20Node%20Types>"
            + " and in addition some provided ones as source node type and namespace registry.",
                                      converter.convert( test ) );
    }

    @Test
    void testRelativeUrl() {
        String test = "<a href=\"#field\">field</a>";
        assertEquals( "field",
                       converter.convert( test ) );
    }
    
    @Test
    void testNullValue() {
        assertNull( converter.convert( null ) );
    }
    
    @Test
    void testExplicitNewline() {
        String test = "Some \"quotation\" marks and backslashes '\\\\', some <strong>important</strong> javadoc<br> and an\n"
            + "inline link to foo";
        assertEquals( "Some \"quotation\" marks and backslashes '\\\\', some important javadoc\n"
            + "and an inline link to foo",
                      converter.convert( test ) );
    }
}

