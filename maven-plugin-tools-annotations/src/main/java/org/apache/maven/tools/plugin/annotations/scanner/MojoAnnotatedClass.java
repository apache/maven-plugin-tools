package org.apache.maven.tools.plugin.annotations.scanner;
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

import org.apache.maven.tools.plugin.annotations.Execute;
import org.apache.maven.tools.plugin.annotations.Mojo;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Olivier Lamy
 */
public class MojoAnnotatedClass
{
    private String className;

    private String parentClassName;

    private Mojo mojo;

    private Execute execute;

    private List<ParameterAnnotationContent> parameters;

    private List<ComponentAnnotationContent> components;

    public MojoAnnotatedClass()
    {
        // no op
    }

    public String getClassName()
    {
        return className;
    }

    public MojoAnnotatedClass setClassName( String className )
    {
        this.className = className;
        return this;
    }

    public Mojo getMojo()
    {
        return mojo;
    }

    public MojoAnnotatedClass setMojo( Mojo mojo )
    {
        this.mojo = mojo;
        return this;
    }

    public Execute getExecute()
    {
        return execute;
    }

    public MojoAnnotatedClass setExecute( Execute execute )
    {
        this.execute = execute;
        return this;
    }

    public List<ParameterAnnotationContent> getParameters()
    {
        if ( this.parameters == null )
        {
            this.parameters = new ArrayList<ParameterAnnotationContent>();
        }
        return parameters;
    }

    public MojoAnnotatedClass setParameters( List<ParameterAnnotationContent> parameters )
    {
        this.parameters = parameters;
        return this;
    }

    public List<ComponentAnnotationContent> getComponents()
    {
        if ( this.components == null )
        {
            this.components = new ArrayList<ComponentAnnotationContent>();
        }
        return components;
    }

    public MojoAnnotatedClass setComponents( List<ComponentAnnotationContent> components )
    {
        this.components = components;
        return this;
    }

    public String getParentClassName()
    {
        return parentClassName;
    }

    public MojoAnnotatedClass setParentClassName( String parentClassName )
    {
        this.parentClassName = parentClassName;
        return this;
    }


    @Override
    public String toString()
    {
        final StringBuilder sb = new StringBuilder();
        sb.append( "MojoAnnotatedClass" );
        sb.append( "{className='" ).append( className ).append( '\'' );
        sb.append( ", parentClassName='" ).append( parentClassName ).append( '\'' );
        sb.append( ", mojo=" ).append( mojo );
        sb.append( ", execute=" ).append( execute );
        sb.append( ", parameters=" ).append( parameters );
        sb.append( ", components=" ).append( components );
        sb.append( '}' );
        return sb.toString();
    }
}
