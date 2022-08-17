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

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.maven.tools.plugin.extractor.annotations.converter.tag.JavadocTagToHtmlConverter;
import org.apache.maven.tools.plugin.extractor.annotations.converter.tag.inline.JavadocInlineTagToHtmlConverter;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

/**
 * Replaces inline javadoc taglets by their according XHTML representation.
 */
@Named
@Singleton
public class JavadocInlineTagsToXhtmlConverter
{
    private static final Logger LOG = LoggerFactory.getLogger( JavadocInlineTagsToXhtmlConverter.class );

    private final Map<String, JavadocInlineTagToHtmlConverter> converters;
    
    private static final Pattern INLINE_TAG_PATTERN = Pattern.compile( "\\{@([^\\s]*)(?:\\s([^\\}]*))?\\}" );
    private static final int GROUP_TAG_NAME = 1;
    private static final int GROUP_REFERENCE = 2;

    @Inject
    public JavadocInlineTagsToXhtmlConverter( Map<String, JavadocInlineTagToHtmlConverter> converters )
    {
        this.converters = converters;
    }

    /**
     * Converts the given text containing arbitrarily many inline javadoc tags with their according HTML replacement.
     * @param text
     * @param context
     * @return
     */
    public String convert( String text, ConverterContext context )
    {
        Matcher matcher = INLINE_TAG_PATTERN.matcher( text );
        StringBuffer sb = new StringBuffer();
        while ( matcher.find() )
        {
            String tagName = matcher.group( GROUP_TAG_NAME );
            JavadocTagToHtmlConverter converter = converters.get( tagName );
            String patternReplacement;
            if ( converter == null )
            {
                patternReplacement = matcher.group( 0 ) + "<!-- unsupported tag '" + tagName + "' -->";
                LOG.warn( "Found unsupported javadoc inline tag '{}' in {}", tagName, context.getLocation() );
            }
            else
            {
                try
                {
                    patternReplacement = converter.convert( matcher.group( GROUP_REFERENCE ), context );
                }
                catch ( Throwable t )
                {
                    patternReplacement = matcher.group( 0 ) + "<!-- error processing javadoc tag '" + tagName + "': "
                                         + t.getMessage() + " -->"; // leave original javadoc in place
                    LOG.warn( "Error converting javadoc inline tag '{}' in {}", tagName, context.getLocation(), t );
                }
            }
            matcher.appendReplacement( sb, Matcher.quoteReplacement( patternReplacement ) );
        }
        matcher.appendTail( sb );
        return toXHTML( sb.toString() );
    }

    static String toXHTML( String bodySnippet )
    {
        String html = "<html><head></head><body>" + bodySnippet + "</body>"; // make it a valid HTML document
        final Document document = Jsoup.parse( html );
        document.outputSettings().syntax( Document.OutputSettings.Syntax.xml );
        return document.body().html();
    }
}