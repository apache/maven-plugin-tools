package org.apache.maven.tools.plugin.javadoc;

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

import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.codehaus.plexus.util.StringUtils;

/**
 * Describes a code reference used in javadoc tags {@code see}, {@code link} and {@code linkplain}.
 * The format of the reference given as string is {@code module/package.class#member label}.
 * Members must be separated with a {@code #} to be detected. 
 * Targets either module, package, class or field/method/constructor in class.
 * This class does not know whether the second part part refers to a package, class or both,
 * as they use the same alphabet and separators.
 * @see <a href="https://docs.oracle.com/en/java/javase/17/docs/specs/javadoc/doc-comment-spec.html#link">link tag specification</a>
 */
public class JavadocReference
{
    private final Optional<String> moduleName;

    private final Optional<String> packageNameClassName;

    private final Optional<String> member; // optional, but may appear with both className and packageName being null

    private final Optional<String> label;

    /*
     * Test at https://regex101.com/r/eDzWNx/1
     * Captures several groups: module name (1), package name and/or class name (2), member (3), label (4)
     */
    private static final Pattern REFERENCE_VALUE_PATTERN =
        Pattern.compile( "^\\s*(?:(.+)/)??([^#\\s/]+)?(?:#([^\\s\\(]+(?:\\([^\\)]*\\))?))?(?: +([^\\s/]+)\\s*)?$" );

    private static final int GROUP_INDEX_MODULE = 1;

    private static final int GROUP_INDEX_PACKAGECLASS = 2;

    private static final int GROUP_INDEX_MEMBER = 3;

    private static final int GROUP_INDEX_LABEL = 4;

    /**
     * 
     * @param reference the reference value to parse
     * @return the created {@link JavadocReference}
     * @throws IllegalArgumentException in case the reference has an invalid format
     */
    public static JavadocReference parse( String reference )
    {
        Matcher matcher = REFERENCE_VALUE_PATTERN.matcher( reference );
        if ( !matcher.matches() )
        {
            throw new IllegalArgumentException( "Invalid format of javadoc reference: " + reference );
        }
        final Optional<String> moduleName = getOptionalGroup( matcher, GROUP_INDEX_MODULE );
        final Optional<String> packageNameClassName = getOptionalGroup( matcher, GROUP_INDEX_PACKAGECLASS );
        final Optional<String> member = getOptionalGroup( matcher, GROUP_INDEX_MEMBER );
        final Optional<String> label = getOptionalGroup( matcher, GROUP_INDEX_LABEL );
        return new JavadocReference( moduleName, packageNameClassName, member, label );
    }

    private static Optional<String> getOptionalGroup( Matcher matcher, int index )
    {
        String group = matcher.group( index );
        if ( StringUtils.isNotEmpty( group ) )
        {
            return Optional.of( group );
        }
        else
        {
            return Optional.empty();
        }
    }

    JavadocReference( Optional<String> moduleName, Optional<String> packageNameClassName,
                      Optional<String> member, Optional<String> label )
    {
        this.moduleName = moduleName;
        this.packageNameClassName = packageNameClassName;
        this.member = member;
        this.label = label;
    }

    public Optional<String> getModuleName()
    {
        return moduleName;
    }

    /**
     * 
     * @return a package name, a class name or a package name followed by a class name
     */
    public Optional<String> getPackageNameClassName()
    {
        return packageNameClassName;
    }

    public Optional<String> getMember()
    {
        return member;
    }

    public Optional<String> getLabel()
    {
        return label;
    }

    @Override
    public String toString()
    {
        return "JavadocReference [moduleName=" + moduleName + ", packageNameClassName=" + packageNameClassName
               + ", member=" + member + ", label=" + label + "]";
    }

    @Override
    public int hashCode()
    {
        return Objects.hash( label, member, packageNameClassName, moduleName );
    }

    @Override
    public boolean equals( Object obj )
    {
        if ( this == obj )
        {
            return true;
        }
        if ( obj == null )
        {
            return false;
        }
        if ( getClass() != obj.getClass() )
        {
            return false;
        }
        JavadocReference other = (JavadocReference) obj;
        return Objects.equals( label, other.label ) && Objects.equals( member, other.member )
            && Objects.equals( packageNameClassName, other.packageNameClassName )
            && Objects.equals( moduleName, other.moduleName );
    }

}
