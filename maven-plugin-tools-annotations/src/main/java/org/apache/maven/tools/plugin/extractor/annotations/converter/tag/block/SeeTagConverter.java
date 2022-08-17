package org.apache.maven.tools.plugin.extractor.annotations.converter.tag.block;

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

import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.maven.tools.plugin.extractor.annotations.converter.ConverterContext;
import org.apache.maven.tools.plugin.extractor.annotations.converter.tag.LinkUtils;

/**
 * Supports <a href="https://docs.oracle.com/en/java/javase/17/docs/specs/javadoc/doc-comment-spec.html#see">block see
 * taglet</a>.
 */
@Named( "see" )
@Singleton
public class SeeTagConverter
    extends JavadocBlockTagToHtmlConverter
{
    private static final String ATTRIBUTE_NAME_IS_FIRST_REFERENCE = 
                    "SeeTagletConverter.isFirstReference";

    @Override
    public String convert( String value, ConverterContext context )
    {
        StringBuilder htmlBuilder = new StringBuilder();
        Boolean isFirstReference = context.getAttribute( ATTRIBUTE_NAME_IS_FIRST_REFERENCE,
                                                         Boolean.class, Boolean.TRUE );
        if ( Boolean.TRUE.equals( isFirstReference ) )
        {
            // headline only once per instance
            htmlBuilder.append( "<br/><strong>See also:</strong>\n" );
            context.setAttribute( ATTRIBUTE_NAME_IS_FIRST_REFERENCE, Boolean.FALSE );
        }
        else
        {
            // multiple links just comma separated, 
            htmlBuilder.append( ", " );
        }
        // is it regular HTML link?
        if ( value.startsWith( "<a href" ) )
        {
            return htmlBuilder.append( value ).toString();
        }
        // is it just a soft string reference?
        if ( value.startsWith( "\"" ) )
        {
            return htmlBuilder.append( value ).toString();
        }
        String link = LinkUtils.createLink( value, context );
        htmlBuilder.append( link );
        return htmlBuilder.toString();
    }

}
