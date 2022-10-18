package org.apache.maven.tools.plugin.extractor.annotations.converter.tag.inline;

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

/**
 * Supports <a href="https://docs.oracle.com/en/java/javase/17/docs/specs/javadoc/doc-comment-spec.html#code">
 * inline code tag</a>.
 */
@Named( "code" )
@Singleton
public class CodeTagConverter
    extends JavadocInlineTagToHtmlConverter
{
    @Override
    public String convert( String text, ConverterContext context )
    {
        return "<code>" + escapeXmlElement( text ) + "</code>";
    }

    
}
