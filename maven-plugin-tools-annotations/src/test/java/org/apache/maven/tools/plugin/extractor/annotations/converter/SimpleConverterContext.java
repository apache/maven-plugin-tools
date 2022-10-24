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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import org.apache.maven.tools.plugin.javadoc.FullyQualifiedJavadocReference;
import org.apache.maven.tools.plugin.javadoc.FullyQualifiedJavadocReference.MemberType;
import org.apache.maven.tools.plugin.javadoc.JavadocLinkGenerator;
import org.apache.maven.tools.plugin.javadoc.JavadocReference;

/**
 * Simple converter not leveraging actual Java classes only for testing purposes.
 * Only generates internal {@link FullyQualifiedJavadocReference}s.
 *
 */
public class SimpleConverterContext
    implements ConverterContext
{

    private final String packageName;

    private final Collection<JavadocReference> unresolvableReferences;

    private final Function<FullyQualifiedJavadocReference, URI> urlSupplier;

    private final Map<String, Object> attributes;

    public SimpleConverterContext( String packageName, URI javadocBaseUrl, JavadocReference... unresolvableReferences )
    {
        this( packageName,
             (ref) -> new JavadocLinkGenerator( javadocBaseUrl, "11" ).createLink( ref ),
             unresolvableReferences );
    }

    public SimpleConverterContext( String packageName, 
                                   Function<FullyQualifiedJavadocReference, URI> urlSupplier,
                                   JavadocReference... unresolvableReferences )
    {
        super();
        this.packageName = packageName;
        this.unresolvableReferences = new ArrayList<>();
        this.urlSupplier = urlSupplier;
        if ( unresolvableReferences != null )
        {
            this.unresolvableReferences.addAll( Arrays.asList( unresolvableReferences ) );
        }
        attributes = new HashMap<>();
    }

    @Override
    public Optional<String> getModuleName()
    {
        return Optional.empty();
    }

    @Override
    public String getPackageName()
    {
        return packageName;
    }

    @Override
    public String getLocation()
    {
        return "customlocation:0";
    }

    @Override
    public FullyQualifiedJavadocReference resolveReference( JavadocReference reference )
    {
        if ( unresolvableReferences.contains( reference ) )
        {
            throw new IllegalArgumentException( "Unresolvable reference" + reference );
        }
        final String packageName;
        final Optional<String> className;
        if ( reference.getPackageNameClassName().isPresent() )
        {
            String packageNameClassName = reference.getPackageNameClassName().get();
            // first character lowercase, assume package name
            if ( Character.isLowerCase( packageNameClassName.charAt( 0 ) ) )
            {
                // find last "."
                int lastDotIndex = packageNameClassName.lastIndexOf( '.' ); // nested classes not supported
                if ( Character.isUpperCase( packageNameClassName.charAt( lastDotIndex + 1 ) ) )
                {
                    packageName = packageNameClassName.substring( 0, lastDotIndex );
                    className = Optional.of( packageNameClassName.substring( lastDotIndex + 1 ) );
                }
                else
                {
                    packageName = packageNameClassName;
                    className = Optional.empty();
                }
            }
            else
            {
                packageName = this.packageName;
                className = Optional.of( packageNameClassName );
            }
        }
        else
        {
            className = Optional.empty();
            packageName = this.packageName;
        }
        Optional<String> normalizedMember = reference.getMember();
        MemberType memberType = null;
        if (reference.getMember().isPresent())
        {
            String member = reference.getMember().get();
            // normalize (i.e. strip argument names and whitespaces)
            int indexOfOpeningParentheses = member.indexOf( '(' );
            int indexOfClosingParentheses = member.indexOf( ')' );
            if (indexOfOpeningParentheses >= 0 && indexOfClosingParentheses >= 0)
            {
                StringBuilder methodArguments = new StringBuilder("(");
                boolean isFirstArgument = true;
                for (String methodArgument : member.substring( indexOfOpeningParentheses + 1, indexOfClosingParentheses ).split("\\s*,\\s*"))
                {
                    String argumentType;
                    int indexOfSpace = methodArgument.indexOf( ' ' );
                    if (indexOfSpace >= 0)
                    {
                        argumentType = methodArgument.substring( 0, indexOfSpace );
                    }
                    else
                    {
                        argumentType = methodArgument;
                    }
                    if (isFirstArgument)
                    {
                        isFirstArgument = false;
                    }
                    else
                    {
                        methodArguments.append(',');
                    }
                    methodArguments.append(argumentType);
                }
                methodArguments.append( ')' );
                normalizedMember = Optional.of( member.substring( 0, indexOfOpeningParentheses ) + methodArguments.toString() );
                memberType = MemberType.METHOD;
            }
            else
            {
                memberType = MemberType.FIELD;
            }
        }
        return new FullyQualifiedJavadocReference( packageName, className, normalizedMember,
                                                   Optional.ofNullable( memberType ), reference.getLabel(), false );
    }

    @Override
    public URI getUrl( FullyQualifiedJavadocReference reference )
    {
        return urlSupplier.apply( reference );
    }

    @Override
    public boolean isReferencedBy( FullyQualifiedJavadocReference reference )
    {
        return false;
    }

    @Override
    public String getStaticFieldValue( FullyQualifiedJavadocReference reference )
    {
        return "some field value";
    }

    @Override
    public URI getInternalJavadocSiteBaseUrl()
    {
        return URI.create( "https://javadoc.example.com" );
    }

    @SuppressWarnings( "unchecked" )
    @Override
    public <T> T setAttribute( String name, T value )
    {
        return (T) attributes.put( name, value );
    }

    @SuppressWarnings( "unchecked" )
    @Override
    public <T> T getAttribute( String name, Class<T> clazz, T defaultValue )
    {
        return (T) attributes.getOrDefault( name, defaultValue );
    }

    @Override
    public boolean canGetUrl()
    {
        return true;
    }
}