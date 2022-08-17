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

import java.util.Optional;
import java.util.function.UnaryOperator;

import org.apache.maven.tools.plugin.extractor.annotations.converter.ConverterContext;
import org.apache.maven.tools.plugin.extractor.annotations.converter.FullyQualifiedJavadocReference;
import org.apache.maven.tools.plugin.extractor.annotations.converter.JavadocReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility methods for dealing with links generated from Javadoc tags.
 */
public class LinkUtils
{

    private static final Logger LOG = LoggerFactory.getLogger( LinkUtils.class );

    private LinkUtils()
    {
        // only static methods
    }

    public static String createLink( String referenceValue, ConverterContext context )
    {
        return createLink( referenceValue, context, UnaryOperator.identity() );
    }

    public static String createLink( String referenceValue, ConverterContext context,
                                     UnaryOperator<String> labelDecorator )
    {
        StringBuilder link = new StringBuilder();
        try
        {
            JavadocReference reference = JavadocReference.parse( referenceValue );
            FullyQualifiedJavadocReference fqReference = context.resolveReference( reference );
            try
            {
                link.append( "<a href=\"" );
                link.append( context.getUrl( fqReference ).toString() );
                link.append( "\">" );
                String label = getReferenceLabel( fqReference, context );
                label = labelDecorator.apply( label );
                link.append( label );
                link.append( "</a>" );
            }
            catch ( IllegalArgumentException e )
            {
                LOG.warn( "Could not get javadoc URL for reference {} at {} (fully qualified {}): {}", referenceValue,
                          fqReference, context.getLocation(), e.getMessage() );
                String label = getReferenceLabel( fqReference, context );
                link.setLength( 0 );
                link.append( labelDecorator.apply( label ) );
                link.append( "<!-- this link does not have javadoc linked -->" );
            }
        }
        catch ( IllegalArgumentException e )
        {
            LOG.warn( "Unresolvable link in javadoc tag with value {} found in {}: {}", referenceValue,
                      context.getLocation(), e.getMessage() );
            String label = referenceValue;
            label = labelDecorator.apply( label );
            link.setLength( 0 );
            link.append( label );
            link.append( "<!-- this link could not be resolved -->" );
        }

        return link.toString();
    }

    /**
     * @return the undecorated label of the link
     * @see <a href="https://docs.oracle.com/javase/8/docs/technotes/tools/windows/javadoc.html#JSWOR656"> javadoc: How
     *      a Name Appears</a>
     */
    private static String getReferenceLabel( FullyQualifiedJavadocReference fqReference, ConverterContext context )
    {
        if ( fqReference.getLabel().isPresent() )
        {
            return fqReference.getLabel().get();
        }
        else
        {
            Optional<String> packageName;
            Optional<String> moduleName;
            Optional<String> className = fqReference.getClassName();
            if ( Optional.of( context.getPackageName() ).equals( fqReference.getPackageName() )
                && context.getModuleName().equals( fqReference.getModuleName() ) )
            {
                packageName = Optional.empty();
                moduleName = Optional.empty();
                if ( context.isReferencedBy( fqReference ) )
                {
                    className = Optional.empty();
                }
            }
            else
            {
                packageName = fqReference.getPackageName();
                moduleName = fqReference.getModuleName();
            }
            return createLabel( moduleName, packageName, className, fqReference.getMember() );
        }
    }

    private static String createLabel( Optional<String> moduleName, Optional<String> packageName,
                                       Optional<String> className, Optional<String> member )
    {
        StringBuilder sb = new StringBuilder();
        if ( packageName.isPresent() && !"java.lang".equals( packageName.get() ) )
        {
            sb.append( packageName.get() );
        }
        if ( className.isPresent() )
        {
            if ( sb.length() > 0 )
            {
                sb.append( '.' );
            }
            sb.append( className.get() );
        }
        if ( member.isPresent() )
        {
            if ( sb.length() > 0 )
            {
                sb.append( '.' );
            }
            sb.append( member.get() );
        }
        return sb.toString();
    }

}
