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

import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugin.descriptor.InvalidPluginDescriptorException;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.project.MavenProject;
import org.apache.maven.tools.plugin.DefaultPluginToolsRequest;
import org.apache.maven.tools.plugin.PluginToolsRequest;
import org.apache.maven.tools.plugin.annotations.datamodel.MojoAnnotationContent;
import org.apache.maven.tools.plugin.annotations.scanner.MojoAnnotatedClass;
import org.apache.maven.tools.plugin.annotations.scanner.MojoAnnotationsScanner;
import org.apache.maven.tools.plugin.annotations.scanner.MojoAnnotationsScannerRequest;
import org.apache.maven.tools.plugin.extractor.ExtractionException;
import org.apache.maven.tools.plugin.extractor.MojoDescriptorExtractor;
import org.codehaus.plexus.logging.AbstractLogEnabled;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
            List<File> classesDirectories = toFiles( request.getProject().getCompileClasspathElements() );
            mojoAnnotationsScannerRequest.setClassesDirectories( classesDirectories );

            List<MojoAnnotatedClass> mojoAnnotatedClasses =
                mojoAnnotationsScanner.scan( mojoAnnotationsScannerRequest );

            return toMojoDescriptors( mojoAnnotatedClasses );
        }
        catch ( DependencyResolutionRequiredException e )
        {
            throw new ExtractionException( e.getMessage(), e );
        }
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

    private List<MojoDescriptor> toMojoDescriptors( List<MojoAnnotatedClass> mojoAnnotatedClasses )
    {
        List<MojoDescriptor> mojoDescriptors = new ArrayList<MojoDescriptor>( mojoAnnotatedClasses.size() );
        for ( MojoAnnotatedClass mojoAnnotatedClass : mojoAnnotatedClasses )
        {
            MojoDescriptor mojoDescriptor = new MojoDescriptor();

            MojoAnnotationContent mojo = mojoAnnotatedClass.getMojo();

            mojoDescriptor.setAggregator( mojo.aggregator() );
            mojoDescriptor.setDependencyResolutionRequired( mojo.requiresDependencyResolution() );
            mojoDescriptor.setDirectInvocationOnly( mojo.requiresDirectInvocation() );
            mojoDescriptor.setDeprecated( mojo.getDeprecated() );
            mojoDescriptors.add( mojoDescriptor );
        }
        return mojoDescriptors;
    }
}
