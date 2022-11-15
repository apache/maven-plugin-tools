package org.apache.maven.tools.plugin;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.util.Objects;

import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.plugin.descriptor.Parameter;

/**
 * Extensions to {@link MojoDescriptor} not supported by Maven 3.2.5.
 * 
 * @author Kristian Rosenvold
 */
public class ExtendedMojoDescriptor
    extends MojoDescriptor
{
    private final boolean containsXhtmlTextValues;
    private boolean v4Api;

    public ExtendedMojoDescriptor()
    {
        this( false );
    }

    /**
     * @param containsXhtmlTextValues
     * @since 3.7.0
     */
    public ExtendedMojoDescriptor( boolean containsXhtmlTextValues )
    {
        this.containsXhtmlTextValues = containsXhtmlTextValues;
    }

    /**
     * Indicates if the methods {@link #getDescription()}, {@link #getDeprecated()}, {@link Parameter#getDescription()}
     * and {@link Parameter#getDeprecated()} return XHTML values.
     * @return {@code true} if aforementioned methods return XHTML values, if {@code false} those values contain
     * javadoc (HTML + custom javadoc tags) (for legacy extractors)
     * @since 3.7.0
     */
    public boolean containsXhtmlTextValues()
    {
        return containsXhtmlTextValues;
    }

    public boolean isV4Api()
    {
        return v4Api;
    }

    public void setV4Api( boolean v4Api )
    {
        this.v4Api = v4Api;
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + Objects.hash( containsXhtmlTextValues );
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
        ExtendedMojoDescriptor other = (ExtendedMojoDescriptor) obj;
        return containsXhtmlTextValues == other.containsXhtmlTextValues;
    }
}
