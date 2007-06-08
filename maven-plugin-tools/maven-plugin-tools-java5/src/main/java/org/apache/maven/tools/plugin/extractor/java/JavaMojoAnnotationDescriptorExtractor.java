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
package org.apache.maven.tools.plugin.extractor.java;

import java.io.File;
import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.Mojo;
import org.apache.maven.plugin.descriptor.InvalidPluginDescriptorException;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugin.descriptor.Requirement;
import org.apache.maven.project.MavenProject;
import org.apache.maven.tools.plugin.Component;
import org.apache.maven.tools.plugin.Execute;
import org.apache.maven.tools.plugin.Goal;
import org.apache.maven.tools.plugin.Parameter;
import org.apache.maven.tools.plugin.extractor.MojoDescriptorExtractor;
import org.apache.maven.tools.plugin.lifecycle.Phase;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.codehaus.plexus.util.DirectoryScanner;

/**
 * Extracts a plugin configuration from the source. Only works for inline literals, not constant variables.
 * 
 * @author Eric Redmond
 * 
 * @plexus.component role="org.apache.maven.tools.plugin.extractor.MojoDescriptorExtractor" role-hint="java5"
 */
public class JavaMojoAnnotationDescriptorExtractor extends AbstractLogEnabled implements MojoDescriptorExtractor
{
    private MojoDescriptor scan( ClassLoader cl, String className ) throws InvalidPluginDescriptorException
    {
        Class<?> c;

        try
        {
            c = cl.loadClass( className );
        }
        catch ( ClassNotFoundException e )
        {
            throw new InvalidPluginDescriptorException( "Error scanning class " + className, e );
        }

        Goal goalAnno = c.getAnnotation( Goal.class );
        if ( goalAnno == null )
        {
            getLogger().debug( "  Not a mojo: " + c.getName() );
            return null;
        }

        MojoDescriptor mojoDescriptor = new MojoDescriptor();

        mojoDescriptor.setRole( Mojo.ROLE );

        mojoDescriptor.setImplementation( c.getName() );

        mojoDescriptor.setLanguage( "java" );

        mojoDescriptor.setInstantiationStrategy( goalAnno.instantiationStrategy() );

        mojoDescriptor.setExecutionStrategy( goalAnno.executionStrategy() );

        mojoDescriptor.setGoal( goalAnno.name() );

        mojoDescriptor.setAggregator( goalAnno.aggregator() );

        mojoDescriptor.setDependencyResolutionRequired( goalAnno.requiresDependencyResolutionScope() );

        mojoDescriptor.setDirectInvocationOnly( goalAnno.requiresDirectInvocation() );

        mojoDescriptor.setProjectRequired( goalAnno.requiresProject() );

        mojoDescriptor.setOnlineRequired( goalAnno.requiresOnline() );

        mojoDescriptor.setInheritedByDefault( goalAnno.inheritByDefault() );

        if ( !Phase.VOID.equals( goalAnno.defaultPhase() ) )
        {
            mojoDescriptor.setPhase( goalAnno.defaultPhase().key() );
        }

        Deprecated deprecatedAnno = c.getAnnotation( Deprecated.class );

        if ( deprecatedAnno != null )
        {
            mojoDescriptor.setDeprecated( "true" );
        }

        Execute executeAnno = c.getAnnotation( Execute.class );

        if ( executeAnno != null )
        {
            String lifecycle = executeAnno.lifecycle();

            mojoDescriptor.setExecuteLifecycle( nullify( lifecycle ) );

            if ( Phase.VOID.equals( executeAnno.phase() ) )
            {
                mojoDescriptor.setExecutePhase( executeAnno.phase().key() );
            }

            String customPhase = executeAnno.customPhase();

            if ( customPhase.length() > 0 )
            {
                if ( !Phase.VOID.equals( executeAnno.phase() ) )
                {
                    getLogger().warn( "Custom phase is overriding \"phase\" field." );
                }
                if ( lifecycle.length() == 0 )
                {
                    getLogger().warn(
                                      "Setting a custom phase without a lifecycle is prone to error. If the phase is not custom, set the \"phase\" field instead." );
                }
                mojoDescriptor.setExecutePhase( executeAnno.customPhase() );
            }

            mojoDescriptor.setExecuteGoal( nullify( executeAnno.goal() ) );
        }

        Class<?> cur = c;
        while ( !Object.class.equals( cur ) )
        {
            attachFieldParameters( cur, mojoDescriptor );

            cur = cur.getSuperclass();
        }

        if ( getLogger().isDebugEnabled() )
        {
            getLogger().debug( "  Component found: " + mojoDescriptor.getHumanReadableKey() );
        }

        return mojoDescriptor;
    }

    private void attachFieldParameters( Class<?> cur, MojoDescriptor mojoDescriptor )
        throws InvalidPluginDescriptorException
    {
        for ( Field f : cur.getDeclaredFields() )
        {
            org.apache.maven.plugin.descriptor.Parameter paramDescriptor =
                new org.apache.maven.plugin.descriptor.Parameter();

            paramDescriptor.setName( f.getName() );

            Parameter paramAnno = f.getAnnotation( Parameter.class );

            if ( paramAnno != null )
            {
                paramDescriptor.setAlias( nullify( paramAnno.alias() ) );

                paramDescriptor.setDefaultValue( nullify( paramAnno.defaultValue() ) );

                paramDescriptor.setEditable( !paramAnno.readonly() );

                paramDescriptor.setExpression( nullify( paramAnno.expression() ) );

                if ( "${reports}".equals( paramDescriptor.getExpression() ) )
                {
                    mojoDescriptor.setRequiresReports( true );
                }

                paramDescriptor.setImplementation( nullify( paramAnno.implementation() ) );

                paramDescriptor.setRequired( paramAnno.required() );

                String property = nullify( paramAnno.property() );

                if ( property != null )
                {
                    paramDescriptor.setName( property );
                }
            }

            Component componentAnno = f.getAnnotation( Component.class );

            if ( componentAnno != null )
            {
                String role = nullify( componentAnno.role() );

                if ( role == null )
                {
                    role = f.getType().getCanonicalName();
                }

                paramDescriptor.setRequirement( new Requirement( role, nullify( componentAnno.roleHint() ) ) );
            }

            if ( paramAnno != null || componentAnno != null )
            {
                paramDescriptor.setType( f.getType().getCanonicalName() );

                mojoDescriptor.addParameter( paramDescriptor );
            }
        }
    }

    private String nullify( String value )
    {
        if ( value == null || value.trim().length() == 0 )
        {
            return null;
        }
        return value;
    }

    public List execute( MavenProject project, PluginDescriptor pluginDescriptor )
        throws InvalidPluginDescriptorException
    {
        List descriptors = new ArrayList();

        File classesDirectory = new File( project.getBuild().getOutputDirectory() );

        DirectoryScanner scanner = new DirectoryScanner();
        scanner.setBasedir( classesDirectory );
        scanner.setIncludes( new String[] { "**/*.class" } );
        scanner.scan();

        List<URL> urls = new ArrayList<URL>();
        for ( Artifact cpe : (Set<Artifact>) project.getArtifacts() ) // CompileClasspathElements() )
        {
            try
            {
                urls.add( cpe.getFile().toURL() ); // URI().toURL() );
            }
            catch ( MalformedURLException e )
            {
                getLogger().warn( "Cannot convert '" + cpe + "' to URL", e );
            }
        }
        try
        {
            urls.add( new File( project.getBuild().getOutputDirectory() ).toURL() );
        }
        catch ( MalformedURLException e )
        {
            getLogger().warn( "Cannot convert '" + project.getBuild().getOutputDirectory() + "' to URL", e );
        }

        if ( getLogger().isDebugEnabled() )
        {
            getLogger().debug( "URLS: \n" + urls.toString().replaceAll( ",", "\n  " ) );
        }
        getLogger().info( "URLS: \n" + urls.toString().replaceAll( ",", "\n  " ) );

        ClassLoader cl = new URLClassLoader( urls.toArray( new URL[urls.size()] ), getClass().getClassLoader() );

        if ( getLogger().isDebugEnabled() )
        {
            getLogger().debug( "Scanning " + scanner.getIncludedFiles().length + " classes" );
        }

        for ( String file : scanner.getIncludedFiles() )
        {
            MojoDescriptor desc = scan( cl, file.substring( 0, file.lastIndexOf( ".class" ) ).replace( '/', '.' ) );
            if ( desc != null )
            {
                desc.setPluginDescriptor( pluginDescriptor );
                descriptors.add( desc );
                getLogger().info( "Found mojo " + desc.getImplementation() );
            }
        }

        Resource resource = new Resource();
        resource.setDirectory( classesDirectory.getAbsolutePath() );
        resource.setIncludes( Collections.EMPTY_LIST );
        resource.setExcludes( Collections.EMPTY_LIST );

        project.addResource( resource );

        return descriptors;
    }
}
