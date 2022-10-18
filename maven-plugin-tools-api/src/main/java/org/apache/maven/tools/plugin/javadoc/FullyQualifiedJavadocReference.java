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

/**
 * Wraps a fully qualified (and resolved) code reference used in javadoc tags {@code see}, {@code link} and
 * {@code linkplain}. Similar to {@link JavadocReference} but can distinguish between package names and class names. The
 * package name is always set for a resolved reference (except for references to modules). The member is always the
 * normalized form containing only fully qualified type names (without argument names), separated by {@code ,} without
 * any whitespace characters. Also the member type is always resolved to one of {@link MemberType} (in case the
 * reference contains a member part).
 */
public class FullyQualifiedJavadocReference
    extends JavadocReference
{

    /** if false, points to a class/package which is part of the current classloader (and not any of its parents) */
    private final boolean isExternal;

    private final Optional<String> packageName;

    private final Optional<MemberType> memberType;

    /** The type of the member part of the reference. */
    public enum MemberType
    {
        FIELD, METHOD, CONSTRUCTOR
    }

    public FullyQualifiedJavadocReference( String packageName, boolean isExternal )
    {
        this( packageName, Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), isExternal );
    }

    public FullyQualifiedJavadocReference( String packageName, Optional<String> label, boolean isExternal )
    {
        this( packageName, Optional.empty(), Optional.empty(), Optional.empty(), label, isExternal );
    }

    public FullyQualifiedJavadocReference( String packageName, String className, boolean isExternal )
    {
        this( packageName, Optional.of( className ), Optional.empty(), Optional.empty(), Optional.empty(),
              isExternal );
    }

    public FullyQualifiedJavadocReference( String packageName, String className, String member, MemberType memberType,
                                           boolean isExternal )
    {
        this( packageName, Optional.of( className ), Optional.of( member ), Optional.of( memberType ),
              Optional.empty(), isExternal );
    }

    public FullyQualifiedJavadocReference( String packageName, Optional<String> className, Optional<String> member,
                                           Optional<MemberType> memberType, Optional<String> label, boolean isExternal )
    {
        this( Optional.empty(), Optional.of( packageName ), className, member, memberType, label, isExternal );
    }

    public FullyQualifiedJavadocReference( Optional<String> moduleName, Optional<String> packageName,
                                           Optional<String> className, Optional<String> member,
                                           Optional<MemberType> memberType, Optional<String> label,
                                           boolean isExternal )
    {
        super( moduleName, className, member, label );
        this.packageName = packageName;
        this.isExternal = isExternal;
        if ( !moduleName.isPresent() && !packageName.isPresent() )
        {
            throw new IllegalArgumentException( "At least one of module name or package name needs to be set" );
        }
        if ( member.isPresent() )
        {
            if ( !memberType.isPresent() )
            {
                throw new IllegalArgumentException( "When member is set, also the member type needs to be set" );
            }
            if ( member.get().matches( ".*\\s.*" ) )
            {
                throw new IllegalArgumentException( "member must not contain any whitespace characters!" );
            }
        }
        this.memberType = memberType;
    }

    /**
     * 
     * @return {@code true} in case this class/package is part of another classloader
     */
    public boolean isExternal()
    {
        return isExternal;
    }

    /** @return the package name of the referenced class */
    public Optional<String> getPackageName()
    {
        return packageName;
    }

    /**
     * @return the simple class name of the referenced class, may be prefixed by the declaring class names, separated by
     *         '.' (for inner classes)
     */
    public Optional<String> getClassName()
    {
        return getPackageNameClassName();
    }

    /** @return the type of the member. Only empty if no member is set. */
    public Optional<MemberType> getMemberType()
    {
        return memberType;
    }

    public Optional<String> getFullyQualifiedClassName()
    {
        if ( getClassName().isPresent() && getPackageName().isPresent() )
        {
            return Optional.of( getPackageName().get() + "." + getClassName().get() );
        }
        else
        {
            return Optional.empty();
        }
    }

    @Override
    public String toString()
    {
        return "FullyQualifiedJavadocReference [moduleName=" + getModuleName() + ", packageName=" + packageName
            + ", className=" + getClassName() + ", memberType=" + memberType + ", member=" + getMember() + ", label="
            + getLabel() + ", isExternal=" + isExternal + "]";
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + Objects.hash( memberType, packageName, isExternal );
        return result;
    }

    @Override
    public boolean equals( Object obj )
    {
        if ( this == obj )
        {
            return true;
        }
        if ( !super.equals( obj ) )
        {
            return false;
        }
        if ( getClass() != obj.getClass() )
        {
            return false;
        }
        FullyQualifiedJavadocReference other = (FullyQualifiedJavadocReference) obj;
        return Objects.equals( memberType, other.memberType ) && Objects.equals( packageName, other.packageName )
                        && isExternal == other.isExternal;
    }

}
