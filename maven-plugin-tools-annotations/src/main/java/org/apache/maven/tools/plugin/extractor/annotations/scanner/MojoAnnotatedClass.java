package org.apache.maven.tools.plugin.extractor.annotations.scanner;

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

import org.apache.maven.artifact.Artifact;
import org.apache.maven.tools.plugin.extractor.annotations.datamodel.ComponentAnnotationContent;
import org.apache.maven.tools.plugin.extractor.annotations.datamodel.ExecuteAnnotationContent;
import org.apache.maven.tools.plugin.extractor.annotations.datamodel.MojoAnnotationContent;
import org.apache.maven.tools.plugin.extractor.annotations.datamodel.ParameterAnnotationContent;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Olivier Lamy
 * @since 3.0
 */
public class MojoAnnotatedClass
{
    private String className;

    private String parentClassName;

    private MojoAnnotationContent mojo;

    private ExecuteAnnotationContent execute;

    /**
     * key is field name
     */
    private Map<String, ParameterAnnotationContent> parameters;

    /**
     * key is field name
     */
    private Map<String, ComponentAnnotationContent> components;

    /**
     * artifact which contains this annotation
     */
    private Artifact artifact;

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

    public MojoAnnotationContent getMojo()
    {
        return mojo;
    }

    public MojoAnnotatedClass setMojo( MojoAnnotationContent mojo )
    {
        this.mojo = mojo;
        return this;
    }

    public ExecuteAnnotationContent getExecute()
    {
        return execute;
    }

    public MojoAnnotatedClass setExecute( ExecuteAnnotationContent execute )
    {
        this.execute = execute;
        return this;
    }

    public Map<String, ParameterAnnotationContent> getParameters()
    {
        if ( this.parameters == null )
        {
            this.parameters = new HashMap<>();
        }
        return parameters;
    }

    public MojoAnnotatedClass setParameters( Map<String, ParameterAnnotationContent> parameters )
    {
        this.parameters = parameters;
        return this;
    }

    public Map<String, ComponentAnnotationContent> getComponents()
    {
        if ( this.components == null )
        {
            this.components = new HashMap<>();
        }
        return components;
    }

    public MojoAnnotatedClass setComponents( Map<String, ComponentAnnotationContent> components )
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

    public Artifact getArtifact()
    {
        return artifact;
    }

    public void setArtifact( Artifact artifact )
    {
        this.artifact = artifact;
    }

    public boolean hasAnnotations()
    {
        return !( getComponents().isEmpty() && getParameters().isEmpty() && execute == null && mojo == null );
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
