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

import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Execute;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.tools.plugin.annotations.datamodel.ComponentAnnotationContent;
import org.apache.maven.tools.plugin.annotations.datamodel.ExecuteAnnotationContent;
import org.apache.maven.tools.plugin.annotations.datamodel.MojoAnnotationContent;
import org.apache.maven.tools.plugin.annotations.datamodel.ParameterAnnotationContent;
import org.apache.maven.tools.plugin.annotations.scanner.visitors.MojoAnnotationVisitor;
import org.apache.maven.tools.plugin.annotations.scanner.visitors.MojoClassVisitor;
import org.apache.maven.tools.plugin.annotations.scanner.visitors.MojoFieldVisitor;
import org.apache.maven.tools.plugin.extractor.ExtractionException;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.codehaus.plexus.util.DirectoryScanner;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.reflection.Reflector;
import org.objectweb.asm.ClassReader;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Olivier Lamy
 * @since 3.0
 */
public class DefaultMojoAnnotationsScanner
    extends AbstractLogEnabled
    implements MojoAnnotationsScanner
{
    private Reflector reflector = new Reflector();

    public Map<String, MojoAnnotatedClass> scan( MojoAnnotationsScannerRequest request )
        throws ExtractionException
    {
        Map<String, MojoAnnotatedClass> mojoAnnotatedClasses = new HashMap<String, MojoAnnotatedClass>();
        try
        {
            for ( File classDirectory : request.getClassesDirectories() )
            {
                if ( classDirectory.exists() && classDirectory.isDirectory() )
                {
                    mojoAnnotatedClasses.putAll( scanDirectory( classDirectory, request.getIncludePatterns() ) );
                }
            }

            //TODO scan dependencies to get super class annotations if exist request.getDependencies()

            return mojoAnnotatedClasses;
        }
        catch ( IOException e )
        {
            throw new ExtractionException( e.getMessage(), e );
        }
    }

    protected Map<String, MojoAnnotatedClass> scanDirectory( File classDirectory, List<String> includePatterns )
        throws IOException, ExtractionException
    {
        Map<String, MojoAnnotatedClass> mojoAnnotatedClasses = new HashMap<String, MojoAnnotatedClass>();
        DirectoryScanner scanner = new DirectoryScanner();
        scanner.setBasedir( classDirectory );
        scanner.addDefaultExcludes();
        if ( includePatterns != null )
        {
            scanner.setIncludes( includePatterns.toArray( new String[includePatterns.size()] ) );
        }
        scanner.scan();
        String[] classFiles = scanner.getIncludedFiles();

        for ( String classFile : classFiles )
        {
            InputStream is = new BufferedInputStream( new FileInputStream( new File( classDirectory, classFile ) ) );
            try
            {

                if ( classFile.endsWith( ".class" ) )
                {
                    MojoClassVisitor mojoClassVisitor = new MojoClassVisitor( getLogger() );
                    ClassReader rdr = new ClassReader( is );
                    rdr.accept( mojoClassVisitor,
                                ClassReader.SKIP_FRAMES | ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG );
                    analyzeVisitors( mojoClassVisitor );
                    if ( isMojoAnnnotatedClassCandidate( mojoClassVisitor.getMojoAnnotatedClass() ) != null )
                    {
                        mojoAnnotatedClasses.put( mojoClassVisitor.getMojoAnnotatedClass().getClassName(),
                                                  mojoClassVisitor.getMojoAnnotatedClass() );
                    }

                }
            }
            finally
            {
                IOUtil.close( is );
            }

        }
        return mojoAnnotatedClasses;
    }

    private MojoAnnotatedClass isMojoAnnnotatedClassCandidate( MojoAnnotatedClass mojoAnnotatedClass )
    {
        if ( mojoAnnotatedClass == null )
        {
            return null;
        }
        if ( !mojoAnnotatedClass.getComponents().isEmpty() || !mojoAnnotatedClass.getParameters().isEmpty()
            || mojoAnnotatedClass.getExecute() != null || mojoAnnotatedClass.getMojo() != null )
        {
            return mojoAnnotatedClass;
        }
        return null;
    }

    protected void analyzeVisitors( MojoClassVisitor mojoClassVisitor )
        throws ExtractionException
    {

        try
        {
            MojoAnnotationVisitor mojoAnnotationVisitor =
                mojoClassVisitor.getAnnotationVisitorMap().get( Mojo.class.getName() );
            if ( mojoAnnotationVisitor != null )
            {
                MojoAnnotationContent mojoAnnotationContent = new MojoAnnotationContent();
                for ( Map.Entry<String, Object> entry : mojoAnnotationVisitor.getAnnotationValues().entrySet() )
                {
                    reflector.invoke( mojoAnnotationContent, entry.getKey(), new Object[]{ entry.getValue() } );
                }
                mojoClassVisitor.getMojoAnnotatedClass().setMojo( mojoAnnotationContent );
            }

            mojoAnnotationVisitor = mojoClassVisitor.getAnnotationVisitorMap().get( Execute.class.getName() );
            if ( mojoAnnotationVisitor != null )
            {
                ExecuteAnnotationContent executeAnnotationContent = new ExecuteAnnotationContent();

                for ( Map.Entry<String, Object> entry : mojoAnnotationVisitor.getAnnotationValues().entrySet() )
                {
                    reflector.invoke( executeAnnotationContent, entry.getKey(), new Object[]{ entry.getValue() } );
                }
                mojoClassVisitor.getMojoAnnotatedClass().setExecute( executeAnnotationContent );
            }

            List<MojoFieldVisitor> mojoFieldVisitors =
                mojoClassVisitor.findFieldWithAnnotationClass( Parameter.class.getName() );

            for ( MojoFieldVisitor mojoFieldVisitor : mojoFieldVisitors )
            {
                ParameterAnnotationContent parameterAnnotationContent =
                    new ParameterAnnotationContent( mojoFieldVisitor.getFieldName() );
                if ( mojoFieldVisitor.getMojoAnnotationVisitor() != null )
                {
                    for ( Map.Entry<String, Object> entry : mojoFieldVisitor.getMojoAnnotationVisitor().getAnnotationValues().entrySet() )
                    {
                        reflector.invoke( parameterAnnotationContent, entry.getKey(),
                                          new Object[]{ entry.getValue() } );
                    }

                }
                mojoClassVisitor.getMojoAnnotatedClass().getParameters().add( parameterAnnotationContent );
            }

            mojoFieldVisitors = mojoClassVisitor.findFieldWithAnnotationClass( Component.class.getName() );

            for ( MojoFieldVisitor mojoFieldVisitor : mojoFieldVisitors )
            {
                ComponentAnnotationContent componentAnnotationContent =
                    new ComponentAnnotationContent( mojoFieldVisitor.getFieldName() );

                if ( mojoFieldVisitor.getMojoAnnotationVisitor() != null )
                {
                    for ( Map.Entry<String, Object> entry : mojoFieldVisitor.getMojoAnnotationVisitor().getAnnotationValues().entrySet() )
                    {
                        reflector.invoke( componentAnnotationContent, entry.getKey(),
                                          new Object[]{ entry.getValue() } );
                    }

                }
                mojoClassVisitor.getMojoAnnotatedClass().getComponents().add( componentAnnotationContent );
            }

        }
        catch ( Exception e )
        {
            throw new ExtractionException( e.getMessage(), e );
        }
    }
}
