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

import java.lang.annotation.Annotation;
import java.util.List;
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

    private boolean required = false;

    private boolean readonly = false;

    private String className;

    private boolean annotationOnMethod;

    private final List<String> typeParameters;

    public ParameterAnnotationContent( String fieldName, String className, List<String> typeParameters,
                                       boolean annotationOnMethod )
    {
        super( fieldName );
        this.className = className;
        this.typeParameters = typeParameters;
        this.annotationOnMethod = annotationOnMethod;
    }

    public ParameterAnnotationContent( String fieldName, String alias, String property, String defaultValue,
                                       boolean required, boolean readonly, String className,
                                       List<String> typeParameters, boolean annotationOnMethod )
    {
        this( fieldName, className, typeParameters, annotationOnMethod );
        this.alias = alias;
        this.property = property;
        this.defaultValue = defaultValue;
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

    public List<String> getTypeParameters()
    {
        return typeParameters;
    }

    public boolean isAnnotationOnMethod()
    {
        return annotationOnMethod;
    }

    @Override
    public String toString()
    {
        final StringBuilder sb = new StringBuilder();
        sb.append( super.toString() );
        sb.append( "ParameterAnnotationContent" );
        sb.append( "{fieldName='" ).append( getFieldName() ).append( '\'' );
        sb.append( ", className='" ).append( getClassName() ).append( '\'' );
        sb.append( ", typeParameters='" ).append( getTypeParameters() ).append( '\'' );
        sb.append( ", name='" ).append( name ).append( '\'' );
        sb.append( ", alias='" ).append( alias ).append( '\'' );
        sb.append( ", alias='" ).append( alias ).append( '\'' );
        sb.append( ", property='" ).append( property ).append( '\'' );
        sb.append( ", defaultValue='" ).append( defaultValue ).append( '\'' );
        sb.append( ", required=" ).append( required );
        sb.append( ", readonly=" ).append( readonly );
        sb.append( ", methodSource=" ).append( annotationOnMethod );
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

        if ( annotationOnMethod != that.annotationOnMethod )
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

        if ( !Objects.equals( typeParameters, that.typeParameters ) )
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

        return true;
    }

    @Override
    public int hashCode()
    {
        return Objects.hash( alias, getFieldName(), getClassName(), typeParameters, property, defaultValue, required,
                             readonly, annotationOnMethod );
    }
}
