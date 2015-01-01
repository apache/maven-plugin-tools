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

import org.apache.maven.plugins.annotations.InstantiationStrategy;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;

import java.lang.annotation.Annotation;

/**
 * @author Olivier Lamy
 * @since 3.0
 */
public class MojoAnnotationContent
    extends AnnotatedContent
    implements Mojo
{
    private String name;

    private LifecyclePhase defaultPhase = LifecyclePhase.NONE;

    private ResolutionScope requiresDependencyResolution = ResolutionScope.NONE;

    private ResolutionScope requiresDependencyCollection = ResolutionScope.NONE;

    private InstantiationStrategy instantiationStrategy = InstantiationStrategy.PER_LOOKUP;

    private String executionStrategy = "once-per-session";

    private boolean requiresProject = true;

    private boolean requiresReports = false;

    private boolean aggregator = false;

    private boolean requiresDirectInvocation = false;

    private boolean requiresOnline = false;

    private boolean inheritByDefault = true;

    private String configurator;

    private boolean threadSafe = false;

    public Class<? extends Annotation> annotationType()
    {
        return null;
    }

    public LifecyclePhase defaultPhase()
    {
        return defaultPhase;
    }

    public void defaultPhase( String phase )
    {
        this.defaultPhase = LifecyclePhase.valueOf( phase );
    }

    public ResolutionScope requiresDependencyResolution()
    {
        return requiresDependencyResolution;
    }

    public void requiresDependencyResolution( String requiresDependencyResolution )
    {
        this.requiresDependencyResolution = ResolutionScope.valueOf( requiresDependencyResolution );
    }

    public ResolutionScope requiresDependencyCollection()
    {
        return requiresDependencyCollection;
    }

    public void requiresDependencyCollection( String requiresDependencyCollection )
    {
        this.requiresDependencyCollection = ResolutionScope.valueOf( requiresDependencyCollection );
    }

    public InstantiationStrategy instantiationStrategy()
    {
        return instantiationStrategy;
    }

    public void instantiationStrategy( String instantiationStrategy )
    {
        this.instantiationStrategy = InstantiationStrategy.valueOf( instantiationStrategy );
    }

    public String executionStrategy()
    {
        return executionStrategy;
    }

    public void executionStrategy( String executionStrategy )
    {
        this.executionStrategy = executionStrategy;
    }

    public boolean requiresProject()
    {
        return requiresProject;
    }

    public void requiresProject( boolean requiresProject )
    {
        this.requiresProject = requiresProject;
    }

    public boolean requiresReports()
    {
        return requiresReports;
    }

    public void requiresReports( boolean requiresReports )
    {
        this.requiresReports = requiresReports;
    }

    public boolean aggregator()
    {
        return aggregator;
    }

    public void aggregator( boolean aggregator )
    {
        this.aggregator = aggregator;
    }

    public boolean requiresDirectInvocation()
    {
        return requiresDirectInvocation;
    }

    public void requiresDirectInvocation( boolean requiresDirectInvocation )
    {
        this.requiresDirectInvocation = requiresDirectInvocation;
    }

    public boolean requiresOnline()
    {
        return requiresOnline;
    }

    public void requiresOnline( boolean requiresOnline )
    {
        this.requiresOnline = requiresOnline;
    }

    public boolean inheritByDefault()
    {
        return inheritByDefault;
    }

    public void inheritByDefault( boolean inheritByDefault )
    {
        this.inheritByDefault = inheritByDefault;
    }

    public String configurator()
    {
        return configurator;
    }

    public void configurator( String configurator )
    {
        this.configurator = configurator;
    }

    public boolean threadSafe()
    {
        return threadSafe;
    }

    public void threadSafe( boolean threadSafe )
    {
        this.threadSafe = threadSafe;
    }

    public String name()
    {
        return this.name;
    }

    public void name( String name )
    {
        this.name = name;
    }

    @Override
    public String toString()
    {
        final StringBuilder sb = new StringBuilder();
        sb.append( "MojoAnnotationContent" );
        sb.append( "{name='" ).append( name ).append( '\'' );
        sb.append( ", defaultPhase=" ).append( defaultPhase );
        sb.append( ", requiresDependencyResolution='" ).append( requiresDependencyResolution ).append( '\'' );
        sb.append( ", requiresDependencyCollection='" ).append( requiresDependencyCollection ).append( '\'' );
        sb.append( ", instantiationStrategy='" ).append( instantiationStrategy ).append( '\'' );
        sb.append( ", executionStrategy='" ).append( executionStrategy ).append( '\'' );
        sb.append( ", requiresProject=" ).append( requiresProject );
        sb.append( ", requiresReports=" ).append( requiresReports );
        sb.append( ", aggregator=" ).append( aggregator );
        sb.append( ", requiresDirectInvocation=" ).append( requiresDirectInvocation );
        sb.append( ", requiresOnline=" ).append( requiresOnline );
        sb.append( ", inheritByDefault=" ).append( inheritByDefault );
        sb.append( ", configurator='" ).append( configurator ).append( '\'' );
        sb.append( ", threadSafe=" ).append( threadSafe );
        sb.append( '}' );
        return sb.toString();
    }
}