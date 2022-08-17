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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import java.util.Map;

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

import com.thoughtworks.qdox.model.DocletTag;
import org.apache.maven.tools.plugin.extractor.annotations.converter.tag.block.JavadocBlockTagToHtmlConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Converts a given 
 * <a href="https://docs.oracle.com/en/java/javase/17/docs/specs/javadoc/doc-comment-spec.html#block-tags">
 * javadoc block taglet</a> to HTML.
 * The implementations have a name equal to the supported doclet name.
 * Must be called for each block tag individually.
 */
@Named
@Singleton
public class JavadocBlockTagsToXhtmlConverter
{
    private static final Logger LOG = LoggerFactory.getLogger( JavadocBlockTagsToXhtmlConverter.class );

    private final JavadocInlineTagsToXhtmlConverter inlineTagsConverter;
    private final Map<String, JavadocBlockTagToHtmlConverter> blockTagConverters;

    @Inject
    public JavadocBlockTagsToXhtmlConverter( JavadocInlineTagsToXhtmlConverter inlineTagsConverter,
                                             Map<String, JavadocBlockTagToHtmlConverter> blockTagConverters )
    {
        this.inlineTagsConverter = inlineTagsConverter;
        this.blockTagConverters = blockTagConverters;
    }

    public String convert( DocletTag docletTag, ConverterContext context )
    {
        return convert( docletTag.getName(), docletTag.getValue(), context );
    }

    public String convert( String name, String text, ConverterContext context )
    {
        JavadocBlockTagToHtmlConverter converter = blockTagConverters.get( name );
        if ( converter == null )
        {
            return getOriginalTag( name, text ) + "<!-- unknown block tag '" + name + "' -->";
        }
        else
        {
            try
            {
                String convertedBlockTagValue = converter.convert( text, context );
                return inlineTagsConverter.convert( convertedBlockTagValue, context );
            }
            catch ( Throwable t )
            {
                LOG.warn( "Error converting javadoc block tag '{}' in {}", name, context.getLocation(), t );
                return getOriginalTag( name, text ) + "<!-- error processing javadoc tag '" + name + "': "
                + t.getMessage() + "-->"; // leave original javadoc in place
            }
            
        }
    }

    private static String getOriginalTag( String name, String text )
    {
        return "@" + name + " "  + text;
    }
}
