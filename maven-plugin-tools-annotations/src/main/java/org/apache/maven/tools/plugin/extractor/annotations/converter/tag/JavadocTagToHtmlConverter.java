package org.apache.maven.tools.plugin.extractor.annotations.converter.tag;

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

import java.util.regex.Pattern;

import org.apache.maven.tools.plugin.extractor.annotations.converter.ConverterContext;
import org.apache.maven.tools.plugin.extractor.annotations.converter.tag.block.JavadocBlockTagToHtmlConverter;
import org.apache.maven.tools.plugin.extractor.annotations.converter.tag.inline.JavadocInlineTagToHtmlConverter;

/**
 * Common base class of both {@link JavadocBlockTagToHtmlConverter} and {@link JavadocInlineTagToHtmlConverter}.
 */
public abstract class JavadocTagToHtmlConverter
{

    private static final Pattern LT = Pattern.compile( "<" );
    private static final Pattern GT = Pattern.compile( ">" );

    /**
     * 
     * @param text the value of the tag
     * @param context the content of the tag (may be empty in case there was no content given)
     * @return the converted text which represents the tag with the given value in html
     */
    public abstract String convert( String text, ConverterContext context );

    /** Mostly a copy of {@code org.codehaus.plexus.util.xml.PrettyPrintXMLWriter#escapeXml(String)}. */
    protected static String escapeXmlElement( String text )
    {
        if ( text.indexOf( '<' ) >= 0 )
        {
            text = LT.matcher( text ).replaceAll( "&lt;" );
        }
    
        if ( text.indexOf( '>' ) >= 0 )
        {
            text = GT.matcher( text ).replaceAll( "&gt;" );
        }
        return text;
    }

}
