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

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import org.apache.maven.tools.plugin.extractor.annotations.converter.tag.inline.CodeTagConverter;
import org.apache.maven.tools.plugin.extractor.annotations.converter.tag.inline.DocRootTagConverter;
import org.apache.maven.tools.plugin.extractor.annotations.converter.tag.inline.JavadocInlineTagToHtmlConverter;
import org.apache.maven.tools.plugin.extractor.annotations.converter.tag.inline.LinkPlainTagToHtmlConverter;
import org.apache.maven.tools.plugin.extractor.annotations.converter.tag.inline.LinkTagToHtmlConverter;
import org.apache.maven.tools.plugin.extractor.annotations.converter.tag.inline.LiteralTagToHtmlConverter;
import org.apache.maven.tools.plugin.extractor.annotations.converter.tag.inline.ValueTagConverter;
import org.apache.maven.tools.plugin.javadoc.FullyQualifiedJavadocReference;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JavadocInlineTagsToXhtmlConverterTest
{
    private final ConverterContext context;
    private final JavadocInlineTagsToXhtmlConverter converter;

    public JavadocInlineTagsToXhtmlConverterTest() {
        URI baseUrl = URI.create( "https://javadoc.example.com/" );
        context = new SimpleConverterContext( "my.package", baseUrl );
        converter = createInlineTagConverter();
    }

    public static JavadocInlineTagsToXhtmlConverter createInlineTagConverter()
    {
        Map<String, JavadocInlineTagToHtmlConverter> converters = new HashMap<>();
        converters.put( "link", new LinkTagToHtmlConverter() );
        converters.put( "linkplain", new LinkPlainTagToHtmlConverter() );
        converters.put( "literal", new LiteralTagToHtmlConverter() );
        converters.put( "code", new CodeTagConverter() );
        converters.put( "value", new ValueTagConverter() );
        converters.put( "docRoot", new DocRootTagConverter() );
        return new JavadocInlineTagsToXhtmlConverter( converters );
    }

    @Test
    void testComplexJavadoc()
    {
        String test = "This is a {@code <>code} and {@link package.Class#member} test {@code code2}\nsome other text";
        assertEquals( "This is a <code>&lt;&gt;code</code> and <a href=\"https://javadoc.example.com/package/Class.html#member\"><code>package.Class.member</code></a> test <code>code2</code> some other text", converter.convert( test, context ) );
    }

    @Test
    void testCode()
    {
        String test = "{@code text}";
        assertEquals( "<code>text</code>", converter.convert( test, context ) );

        test = "{@code <A&B>}";
        assertEquals( "<code>&lt;A&amp;B&gt;</code>", converter.convert( test, context ) );
        
        test = "Something{@code \n<A&B>\n   }";
        assertEquals( "Something<code> &lt;A&amp;B&gt; </code>", converter.convert( test, context ) );
        
        test = "{@code\ntest}";
        assertEquals( "<code>test</code>", converter.convert( test, context ) );
    }

    @Test
    void testLiteral()
    {
        String test = "{@literal text}";
        assertEquals( "text", converter.convert( test, context ) );

        test = "{@literal text}  {@literal text}";
        assertEquals( "text text", converter.convert( test, context ) );

        test = "{@literal <A&B>}";
        assertEquals( "&lt;A&amp;B&gt;", converter.convert( test, context ) );
    }

    @Test
    void testLink()
    {
        String test = "{@link Class}";
        assertEquals( "<a href=\"https://javadoc.example.com/my/package/Class.html\"><code>Class</code></a>",
                      converter.convert( test, context ) );

        test = "{@link MyClass#field1}";
        assertEquals( "<a href=\"https://javadoc.example.com/my/package/MyClass.html#field1\"><code>MyClass.field1</code></a>",
                      converter.convert( test, context ) );
    }

    @Test
    void testLinkplain()
    {
        String test = "{@linkplain Class}";
        assertEquals( "<a href=\"https://javadoc.example.com/my/package/Class.html\">Class</a>",
                      converter.convert( test, context ) );

        test = "{@linkplain #field}";
        assertEquals( "<a href=\"https://javadoc.example.com/my/package/package-summary.html#field\">field</a>",
                      converter.convert( test, context ) );

        test = "{@linkplain Class#field}";
        assertEquals( "<a href=\"https://javadoc.example.com/my/package/Class.html#field\">Class.field</a>",
                      converter.convert( test, context ) );

        test = "{@linkplain #method()}";
        assertEquals( "<a href=\"https://javadoc.example.com/my/package/package-summary.html#method()\">method()</a>",
                      converter.convert( test, context ) );

        test = "{@linkplain #method(Object arg)}";
        assertEquals( "<a href=\"https://javadoc.example.com/my/package/package-summary.html#method(Object)\">method(Object)</a>",
                      converter.convert( test, context ) );

        test = "{@linkplain #method(Object, String)}";
        assertEquals( "<a href=\"https://javadoc.example.com/my/package/package-summary.html#method(Object,String)\">method(Object,String)</a>",
                      converter.convert( test, context ) );

        test = "{@linkplain #method(Object, String) label}";
        assertEquals( "<a href=\"https://javadoc.example.com/my/package/package-summary.html#method(Object,String)\">label</a>",
                      converter.convert( test, context ) );

        test = "{@linkplain Class#method(Object, String)}";
        assertEquals( "<a href=\"https://javadoc.example.com/my/package/Class.html#method(Object,String)\">Class.method(Object,String)</a>",
                      converter.convert( test, context ) );

        test = "{@linkplain Class#method(Object, String) label}";
        assertEquals( "<a href=\"https://javadoc.example.com/my/package/Class.html#method(Object,String)\">label</a>",
                      converter.convert( test, context ) );

    }

    @Test
    void testValue()
    {
        String test = "{@value Class#STATIC_FIELD}";
        assertEquals( "some field value",
                      converter.convert( test, context ) );
    }

    @Test
    void testDocroot()
    {
        String test = "Some <a href=\"{@docRoot}/test.html\">link</a>";
        assertEquals( "Some <a href=\"https://javadoc.example.com/test.html\">link</a>",
                     converter.convert( test, context ) );
    }

    @Test
    void testMultipleTags()
    {
        String test = "Some code {@code myCode} and link {@linkplain Class}. Something";
        assertEquals( "Some code <code>myCode</code> and link <a href=\"https://javadoc.example.com/my/package/Class.html\">Class</a>. Something",
                      converter.convert( test, context ) );
    }

    @Test
    void testExceptionInTag()
    {
        URI baseUrl = URI.create( "https://javadoc.example.com/" );
        ConverterContext context = new SimpleConverterContext( "my.package", baseUrl )
        {

            @Override
            public String getStaticFieldValue( FullyQualifiedJavadocReference reference )
            {
                throw new IllegalArgumentException( "Some exception" );
            }
            
        };
        String test = "{@value Class#STATIC_FIELD}";
        assertEquals( "{@value Class#STATIC_FIELD}<!-- error processing javadoc tag 'value': Some exception -->",
                      converter.convert( test, context ) );
    }

    @Test
    void testUnknownTag()
    {
        String test = "{@unknown text}";
        assertEquals( "{@unknown text}<!-- unsupported tag 'unknown' -->",
                      converter.convert( test, context ) );
    }
}
