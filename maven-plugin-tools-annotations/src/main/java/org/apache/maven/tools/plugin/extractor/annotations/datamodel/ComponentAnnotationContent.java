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

import org.apache.maven.plugins.annotations.Component;

import java.lang.annotation.Annotation;

/**
 * @author Olivier Lamy
 * @since 3.0
 */
public class ComponentAnnotationContent
    extends AnnotatedField
    implements Component
{
    private String roleClassName;

    private String hint;

    public ComponentAnnotationContent( String fieldName )
    {
        super( fieldName );
    }

    public ComponentAnnotationContent( String fieldName, String role, String hint )
    {
        this( fieldName );
        this.roleClassName = role;
        this.hint = hint;
    }

    public Class<?> role()
    {
        // not used
        return null;
    }

    public void setRoleClassName( String roleClassName )
    {
        this.roleClassName = roleClassName;
    }

    public String getRoleClassName()
    {
        return roleClassName;
    }

    public String hint()
    {
        return hint == null ? "" : hint;
    }

    public void hint( String hint )
    {
        this.hint = hint;
    }

    public Class<? extends Annotation> annotationType()
    {
        return null;
    }

    @Override
    public String toString()
    {
        final StringBuilder sb = new StringBuilder();
        sb.append( super.toString() );
        sb.append( "ComponentAnnotationContent" );
        sb.append( "{role='" ).append( roleClassName ).append( '\'' );
        sb.append( ", hint='" ).append( hint ).append( '\'' );
        sb.append( '}' );
        return sb.toString();
    }
}
