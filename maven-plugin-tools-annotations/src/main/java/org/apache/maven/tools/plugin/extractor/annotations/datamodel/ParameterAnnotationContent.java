package org.apache.maven.tools.plugin.extractor.annotations.datamodel;

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

import org.apache.maven.plugins.annotations.Parameter;
import org.objectweb.asm.Type;

import java.lang.annotation.Annotation;
import java.util.Objects;

/**
 * @author Olivier Lamy
 * @since 3.0
 */
public class ParameterAnnotationContent
    extends AnnotatedField
    implements Parameter
{

    private String name;

    private String alias;

    private String property;

    private String defaultValue;

    private String implementationClassName;

    private boolean required = false;

    private boolean readonly = false;

    private String className;

    public ParameterAnnotationContent( String fieldName, String className )
    {
        super( fieldName );
        this.className = className;
    }

    public ParameterAnnotationContent( String fieldName, String alias, String property, String defaultValue,
                                       Class<?> implementation, boolean required, boolean readonly, String className )
    {
        this( fieldName, className );
        this.alias = alias;
        this.property = property;
        this.defaultValue = defaultValue;
        this.implementationClassName = implementation != null ? implementation.getName() : null;
        this.required = required;
        this.readonly = readonly;
    }

    @Override
    public String name()
    {
        return name;
    }

    public void name( String name )
    {
        this.name = name;
    }

    @Override
    public String alias()
    {
        return alias;
    }

    public void alias( String alias )
    {
        this.alias = alias;
    }

    @Override
    public String property()
    {
        return property;
    }

    public void property( String property )
    {
        this.property = property;
    }

    @Override
    public String defaultValue()
    {
        return defaultValue;
    }

    public void defaultValue( String defaultValue )
    {
        this.defaultValue = defaultValue;
    }

    public void implementation( Type implementation )
    {

        implementationClassName = implementation.getClassName();
        if ( implementationClassName.equals( Object.class.getName() ) )
        {
            // Object is default value for implementation attribute
            this.implementationClassName = null;
        }
    }

    public String getImplementationClassName()
    {
        return implementationClassName;
    }

    @Override
    public Class<?> implementation()
    {
        // needed for implementing @Parameter
        // we don't have access to project class path,
        // so loading class is not possible without build additional classLoader
        // we only operate on classes names
        throw new UnsupportedOperationException(
            "please use getImplementationClassName instead of implementation method" );
    }

    @Override
    public boolean required()
    {
        return required;
    }

    public void required( boolean required )
    {
        this.required = required;
    }

    @Override
    public boolean readonly()
    {
        return readonly;
    }

    public void readonly( boolean readonly )
    {
        this.readonly = readonly;
    }

    @Override
    public Class<? extends Annotation> annotationType()
    {
        return null;
    }

    public String getClassName()
    {
        return className;
    }

    public void setClassName( String className )
    {
        this.className = className;
    }

    @Override
    public String toString()
    {
        final StringBuilder sb = new StringBuilder();
        sb.append( super.toString() );
        sb.append( "ParameterAnnotationContent" );
        sb.append( "{fieldName='" ).append( getFieldName() ).append( '\'' );
        sb.append( ", className='" ).append( getClassName() ).append( '\'' );
        sb.append( ", name='" ).append( name ).append( '\'' );
        sb.append( ", alias='" ).append( alias ).append( '\'' );
        sb.append( ", alias='" ).append( alias ).append( '\'' );
        sb.append( ", property='" ).append( property ).append( '\'' );
        sb.append( ", defaultValue='" ).append( defaultValue ).append( '\'' );
        sb.append( ", implementation='" ).append( implementationClassName ).append( '\'' );
        sb.append( ", required=" ).append( required );
        sb.append( ", readonly=" ).append( readonly );
        sb.append( '}' );
        return sb.toString();
    }

    @Override
    public boolean equals( Object o )
    {
        if ( this == o )
        {
            return true;
        }
        if ( !( o instanceof ParameterAnnotationContent ) )
        {
            return false;
        }

        ParameterAnnotationContent that = (ParameterAnnotationContent) o;

        if ( readonly != that.readonly )
        {
            return false;
        }
        if ( required != that.required )
        {
            return false;
        }

        if ( getFieldName() != null ? !getFieldName().equals( that.getFieldName() ) : that.getFieldName() != null )
        {
            return false;
        }

        if ( getClassName() != null ? !getClassName().equals( that.getClassName() ) : that.getClassName() != null )
        {
            return false;
        }

        if ( !Objects.equals( alias, that.alias ) )
        {
            return false;
        }
        if ( !Objects.equals( defaultValue, that.defaultValue ) )
        {
            return false;
        }
        if ( !Objects.equals( property, that.property ) )
        {
            return false;
        }
        if ( !Objects.equals( implementationClassName, that.implementationClassName ) )
        {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode()
    {
        return Objects.hash( alias, getFieldName(), property, defaultValue, required, readonly,
                             implementationClassName );
    }
}
