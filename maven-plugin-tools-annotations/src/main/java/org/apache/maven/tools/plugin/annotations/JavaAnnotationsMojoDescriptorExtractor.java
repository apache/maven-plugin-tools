package org.apache.maven.tools.plugin.annotations;
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

import com.thoughtworks.qdox.JavaDocBuilder;
import com.thoughtworks.qdox.model.JavaClass;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugin.descriptor.DuplicateParameterException;
import org.apache.maven.plugin.descriptor.InvalidPluginDescriptorException;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugin.descriptor.Requirement;
import org.apache.maven.project.MavenProject;
import org.apache.maven.tools.plugin.DefaultPluginToolsRequest;
import org.apache.maven.tools.plugin.PluginToolsRequest;
import org.apache.maven.tools.plugin.annotations.datamodel.ComponentAnnotationContent;
import org.apache.maven.tools.plugin.annotations.datamodel.ExecuteAnnotationContent;
import org.apache.maven.tools.plugin.annotations.datamodel.MojoAnnotationContent;
import org.apache.maven.tools.plugin.annotations.datamodel.ParameterAnnotationContent;
import org.apache.maven.tools.plugin.annotations.scanner.MojoAnnotatedClass;
import org.apache.maven.tools.plugin.annotations.scanner.MojoAnnotationsScanner;
import org.apache.maven.tools.plugin.annotations.scanner.MojoAnnotationsScannerRequest;
import org.apache.maven.tools.plugin.extractor.ExtractionException;
import org.apache.maven.tools.plugin.extractor.MojoDescriptorExtractor;
import org.codehaus.plexus.logging.AbstractLogEnabled;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * @author Olivier Lamy
 * @since 3.0
 */
public class JavaAnnotationsMojoDescriptorExtractor
    extends AbstractLogEnabled
    implements MojoDescriptorExtractor
{

    /**
     * @requirement
     */
    MojoAnnotationsScanner mojoAnnotationsScanner;

    public List<MojoDescriptor> execute( MavenProject project, PluginDescriptor pluginDescriptor )
        throws ExtractionException, InvalidPluginDescriptorException
    {
        return execute( new DefaultPluginToolsRequest( project, pluginDescriptor ) );
    }

    public List<MojoDescriptor> execute( PluginToolsRequest request )
        throws ExtractionException, InvalidPluginDescriptorException
    {
        try
        {
            MojoAnnotationsScannerRequest mojoAnnotationsScannerRequest = new MojoAnnotationsScannerRequest();

            mojoAnnotationsScannerRequest.setClassesDirectories(
                Arrays.asList( new File( request.getProject().getBuild().getOutputDirectory() ) ) );

            mojoAnnotationsScannerRequest.setDependencies( request.getProject().getCompileClasspathElements() );

            Map<String, MojoAnnotatedClass> mojoAnnotatedClasses =
                mojoAnnotationsScanner.scan( mojoAnnotationsScannerRequest );

            JavaClass[] javaClasses = discoverClasses( request );

            return toMojoDescriptors( mojoAnnotatedClasses, request );
        }
        catch ( DependencyResolutionRequiredException e )
        {
            throw new ExtractionException( e.getMessage(), e );
        }
    }

    protected JavaClass[] discoverClasses( final PluginToolsRequest request )
    {
        JavaDocBuilder builder = new JavaDocBuilder();
        builder.setEncoding( request.getEncoding() );

        MavenProject project = request.getProject();

        for ( String source : (List<String>) project.getCompileSourceRoots() )
        {
            builder.addSourceTree( new File( source ) );
        }

        // TODO be more dynamic
        File generatedPlugin = new File( project.getBasedir(), "target/generated-sources/plugin" );
        if ( !project.getCompileSourceRoots().contains( generatedPlugin.getAbsolutePath() ) )
        {
            builder.addSourceTree( generatedPlugin );
        }

        return builder.getClasses();
    }

    private List<File> toFiles( List<String> directories )
    {
        if ( directories == null )
        {
            return Collections.emptyList();
        }
        List<File> files = new ArrayList<File>( directories.size() );
        for ( String directory : directories )
        {
            files.add( new File( directory ) );
        }
        return files;
    }

    private List<MojoDescriptor> toMojoDescriptors( Map<String, MojoAnnotatedClass> mojoAnnotatedClasses,
                                                    PluginToolsRequest request )
        throws DuplicateParameterException
    {
        List<MojoDescriptor> mojoDescriptors = new ArrayList<MojoDescriptor>( mojoAnnotatedClasses.size() );
        for ( MojoAnnotatedClass mojoAnnotatedClass : mojoAnnotatedClasses.values() )
        {
            MojoDescriptor mojoDescriptor = new MojoDescriptor();

            //mojoDescriptor.setRole( mojoAnnotatedClass.getClassName() );
            //mojoDescriptor.setRoleHint( "default" );
            mojoDescriptor.setImplementation( mojoAnnotatedClass.getClassName() );
            mojoDescriptor.setLanguage( "java" );

            MojoAnnotationContent mojo = mojoAnnotatedClass.getMojo();

            mojoDescriptor.setAggregator( mojo.aggregator() );
            mojoDescriptor.setDependencyResolutionRequired( mojo.requiresDependencyResolution() );
            mojoDescriptor.setDirectInvocationOnly( mojo.requiresDirectInvocation() );
            mojoDescriptor.setDeprecated( mojo.getDeprecated() );

            ExecuteAnnotationContent execute = mojoAnnotatedClass.getExecute();

            if ( execute != null )
            {
                mojoDescriptor.setExecuteGoal( execute.goal() );
                mojoDescriptor.setExecuteLifecycle( execute.lifecycle() );
                mojoDescriptor.setExecutePhase( execute.phase().id() );
            }

            mojoDescriptor.setExecutionStrategy( mojo.executionStrategy() );
            // FIXME olamy wtf ?
            //mojoDescriptor.alwaysExecute(mojo.a)

            mojoDescriptor.setGoal( mojo.name() );
            mojoDescriptor.setOnlineRequired( mojo.requiresOnline() );

            mojoDescriptor.setPhase( mojo.defaultPhase().id() );

            for ( ParameterAnnotationContent parameterAnnotationContent : mojoAnnotatedClass.getParameters() )
            {
                org.apache.maven.plugin.descriptor.Parameter parameter =
                    new org.apache.maven.plugin.descriptor.Parameter();
                parameter.setName( parameterAnnotationContent.getFieldName() );
                parameter.setAlias( parameterAnnotationContent.alias() );
                parameter.setDefaultValue( parameterAnnotationContent.defaultValue() );
                parameter.setDeprecated( parameterAnnotationContent.getDeprecated() );
                parameter.setDescription( parameterAnnotationContent.getDescription() );
                // FIXME olamy wtf ?
                parameter.setEditable( !parameterAnnotationContent.readonly() );
                parameter.setExpression( parameterAnnotationContent.expression() );
                mojoDescriptor.addParameter( parameter );
            }

            for ( ComponentAnnotationContent componentAnnotationContent : mojoAnnotatedClass.getComponents() )
            {
                org.apache.maven.plugin.descriptor.Parameter parameter =
                    new org.apache.maven.plugin.descriptor.Parameter();
                parameter.setName( componentAnnotationContent.getFieldName() );
                parameter.setRequirement(
                    new Requirement( componentAnnotationContent.role(), componentAnnotationContent.roleHint() ) );
                parameter.setEditable( false );
                mojoDescriptor.addParameter( parameter );
            }

            mojoDescriptor.setPluginDescriptor( request.getPluginDescriptor() );

            mojoDescriptors.add( mojoDescriptor );
        }
        return mojoDescriptors;
    }
}
