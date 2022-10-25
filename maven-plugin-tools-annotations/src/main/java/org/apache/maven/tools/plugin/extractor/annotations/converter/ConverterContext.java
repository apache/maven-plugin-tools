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
import java.util.Optional;

import org.apache.maven.tools.plugin.extractor.annotations.converter.tag.block.JavadocBlockTagToHtmlConverter;
import org.apache.maven.tools.plugin.javadoc.FullyQualifiedJavadocReference;
import org.apache.maven.tools.plugin.javadoc.JavadocReference;

/**
 * Context which is passed to {@link JavadocBlockTagsToXhtmlConverter}, {@link JavadocInlineTagsToXhtmlConverter},
 * {@link JavadocBlockTagToHtmlConverter} and {@link JavadocBlockTagToHtmlConverter}.
 * It contains metadata about the container class and allows to resolve class or member names 
 * which are not fully qualified as well as creating (deep-) links to javadoc pages.
 */
public interface ConverterContext
{
    /**
     * 
     * @return the module name of the container class
     */
    Optional<String> getModuleName();
    
    /**
     * 
     * @return the package name of the container class
     */
    String getPackageName();

    /**
     * 
     * @param reference
     * @return true in case either the current container class or any of its super classes are referenced
     */
    boolean isReferencedBy( FullyQualifiedJavadocReference reference );

    /**
     * 
     * @return a location text (human readable) indicating where in the container class the conversion is triggered
     * (should be as specific as possible to ease debugging)
     */
    String getLocation();

    /**
     * Resolves a given javadoc reference, according to the rules of 
     * <a href="https://docs.oracle.com/javase/8/docs/technotes/tools/windows/javadoc.html#JSWOR655">
     * Javadoc's search order</a>.
     * @param reference the reference to resolve
     * @return the resolved fully qualified reference
     * @throws IllegalArgumentException in case the reference cannot be resolved
     */
    FullyQualifiedJavadocReference resolveReference( JavadocReference reference );

    /**
     * 
     * @return {@code true} if links to javadoc pages could potentially be generated with
     * {@link #getUrl(FullyQualifiedJavadocReference)}.
     */
    boolean canGetUrl();

    /**
     * Returns a (deep-)link to the javadoc page for the given reference
     * @param reference the reference for which to get the url
     * @return the link
     * @throws IllegalArgumentException in case no javadoc link could be generated for the given reference
     * @throws IllegalStateException in case no javadoc source sites have been configured
     * (i.e. {@link #canGetUrl()} returns {@code false})
     */
    URI getUrl( FullyQualifiedJavadocReference reference );

    /**
     * Returns the value of a referenced static field.
     * @param reference the code reference towards a static field
     * @return the value of the static field given by the {@code reference}
     * @throws IllegalArgumentException in case the reference does not point to a valid static field
     */
    String getStaticFieldValue( FullyQualifiedJavadocReference reference );

    /**
     * Returns the base url to use for internal javadoc links 
     * @return the base url for internal javadoc links (may be {@code null}).
     */
    URI getInternalJavadocSiteBaseUrl();

    /**
     * Stores some attribute in the current context
     * @param <T>
     * @param name
     * @param value
     * @return the old attribute value or null.
     */
    <T> T setAttribute( String name, T value );
    
    /**
     * Retrieves some attribute value from the current context.
     * @param <T>
     * @param name
     * @param clazz
     * @param defaultValue
     * @return the value of the attribute with the given name or {@code null} if it does not exist
     */
    <T> T getAttribute( String name, Class<T> clazz, T defaultValue );
}