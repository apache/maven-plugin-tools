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

import org.apache.maven.tools.plugin.extractor.annotations.converter.tag.block.JavadocBlockTagToHtmlConverter;
import org.apache.maven.tools.plugin.extractor.annotations.converter.tag.block.SeeTagConverter;
import org.apache.maven.tools.plugin.javadoc.FullyQualifiedJavadocReference;
import org.apache.maven.tools.plugin.javadoc.JavadocReference;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JavadocBlockTagsToXhtmlConverterTest
{
    private final ConverterContext context;
    private final JavadocBlockTagsToXhtmlConverter converter;
    private final JavadocInlineTagsToXhtmlConverter inlineTagsConverter;

    public JavadocBlockTagsToXhtmlConverterTest() {
        URI baseUrl = URI.create( "https://javadoc.example.com/" );
        context = new SimpleConverterContext( "my.package", baseUrl );
        inlineTagsConverter = JavadocInlineTagsToXhtmlConverterTest.createInlineTagConverter();
        Map<String, JavadocBlockTagToHtmlConverter> blockTagConverters = new HashMap<>();
        blockTagConverters.put( "see", new SeeTagConverter() );
        converter =
            new JavadocBlockTagsToXhtmlConverter( inlineTagsConverter, blockTagConverters );
    }

    @Test
    void testSee()
    {
        assertEquals( "<br /><strong>See also:</strong> \"Some reference\"", converter.convert( "see", "\"Some reference\"" , context ) );
        assertEquals( ", <a href=\"example.com\">Example</a>", converter.convert( "see", "<a href=\"example.com\">Example</a>", context ) );
        
        // new context should start again with "See also:"
        ConverterContext context2 = new SimpleConverterContext( "my.package", URI.create( "https://javadoc.example.com/" ) );
        assertEquals( "<br /><strong>See also:</strong> <a href=\"example.com\">Example</a>", converter.convert( "see", "<a href=\"example.com\">Example</a>", context2 ) );
    }

    @Test
    void testExceptionInTag()
    {
        URI baseUrl = URI.create( "https://javadoc.example.com/" );
        ConverterContext context = new SimpleConverterContext( "my.package", baseUrl )
        {
            @Override
            public FullyQualifiedJavadocReference resolveReference( JavadocReference reference )
            {
                throw new IllegalArgumentException( "Some exception" );
            }
        };
        Map<String, JavadocBlockTagToHtmlConverter> blockTagConverters = new HashMap<>();
        blockTagConverters.put( "example", new JavadocBlockTagToHtmlConverter()
            {
                @Override
                public String convert( String text, ConverterContext context )
                {
                    throw new IllegalArgumentException( "Some exception" );
                }
            }
        );
        JavadocBlockTagsToXhtmlConverter converter =
                        new JavadocBlockTagsToXhtmlConverter( inlineTagsConverter, blockTagConverters );
        String test = "Class#field";
        assertEquals( "@example Class#field<!-- error processing javadoc tag 'example': Some exception-->", converter.convert( "example", test, context ) );
    }
}
