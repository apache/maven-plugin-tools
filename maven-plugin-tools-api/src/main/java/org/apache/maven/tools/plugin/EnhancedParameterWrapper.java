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

import org.apache.maven.plugin.descriptor.Parameter;
import org.apache.maven.plugin.descriptor.Requirement;

/**
 * Wrapper around regular {@link Parameter} which adds capability to
 * read/write a type javadoc URL
 */
public class EnhancedParameterWrapper
    extends Parameter
{
    private final Parameter delegate;
    private URI typeJavadocUrl;
    
    public EnhancedParameterWrapper( Parameter delegate )
    {
        super();
        this.delegate = delegate;
    }

    public String getName()
    {
        return delegate.getName();
    }

    public void setName( String name )
    {
        delegate.setName( name );
    }

    public String getType()
    {
        return delegate.getType();
    }

    public void setType( String type )
    {
        delegate.setType( type );
    }

    public boolean isRequired()
    {
        return delegate.isRequired();
    }

    public void setRequired( boolean required )
    {
        delegate.setRequired( required );
    }

    public String getDescription()
    {
        return delegate.getDescription();
    }

    public void setDescription( String description )
    {
        delegate.setDescription( description );
    }

    public String getExpression()
    {
        return delegate.getExpression();
    }

    public void setExpression( String expression )
    {
        delegate.setExpression( expression );
    }

    public String getDeprecated()
    {
        return delegate.getDeprecated();
    }

    public void setDeprecated( String deprecated )
    {
        delegate.setDeprecated( deprecated );
    }

    public int hashCode()
    {
        return delegate.hashCode();
    }

    public boolean equals( Object other )
    {
        return delegate.equals( other );
    }

    public String getAlias()
    {
        return delegate.getAlias();
    }

    public void setAlias( String alias )
    {
        delegate.setAlias( alias );
    }

    public boolean isEditable()
    {
        return delegate.isEditable();
    }

    public void setEditable( boolean editable )
    {
        delegate.setEditable( editable );
    }

    public void setDefaultValue( String defaultValue )
    {
        delegate.setDefaultValue( defaultValue );
    }

    public String getDefaultValue()
    {
        return delegate.getDefaultValue();
    }

    public String toString()
    {
        return delegate.toString();
    }

    public Requirement getRequirement()
    {
        return delegate.getRequirement();
    }

    public void setRequirement( Requirement requirement )
    {
        delegate.setRequirement( requirement );
    }

    public String getImplementation()
    {
        return delegate.getImplementation();
    }

    public void setImplementation( String implementation )
    {
        delegate.setImplementation( implementation );
    }

    public String getSince()
    {
        return delegate.getSince();
    }

    public void setSince( String since )
    {
        delegate.setSince( since );
    }

    public Parameter clone()
    {
        return delegate.clone();
    }

    public URI getTypeJavadocUrl()
    {
        return typeJavadocUrl;
    }

    public void setTypeJavadocUrl( URI typeJavadocUrl )
    {
        this.typeJavadocUrl = typeJavadocUrl;
    }
}
