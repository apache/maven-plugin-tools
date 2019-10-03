package org.apache.maven.tools.plugin.extractor.annotations;

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

import org.apache.maven.plugins.annotations.Execute;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.project.MavenProject;
import org.apache.maven.tools.plugin.extractor.annotations.datamodel.ComponentAnnotationContent;
import org.apache.maven.tools.plugin.extractor.annotations.datamodel.ParameterAnnotationContent;
import org.apache.maven.tools.plugin.extractor.annotations.scanner.MojoAnnotatedClass;
import org.apache.maven.tools.plugin.extractor.annotations.scanner.MojoAnnotationsScanner;
import org.apache.maven.tools.plugin.extractor.annotations.scanner.MojoAnnotationsScannerRequest;
import org.codehaus.plexus.PlexusTestCase;
import org.fest.assertions.Assertions;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

/**
 * @author Olivier Lamy
 */
public class TestAnnotationsReader
    extends PlexusTestCase
{
    public void testReadMojoClass()
        throws Exception
    {
        MojoAnnotationsScanner mojoAnnotationsScanner = (MojoAnnotationsScanner) lookup( MojoAnnotationsScanner.ROLE );

        MojoAnnotationsScannerRequest request = new MojoAnnotationsScannerRequest();
        request.setClassesDirectories( Collections.singletonList( new File( getBasedir(), "target/test-classes" ) ) );
        request.setIncludePatterns( Arrays.asList( "**/FooMojo.class" ) );
        request.setProject( new MavenProject() );

        Map<String, MojoAnnotatedClass> mojoAnnotatedClasses = mojoAnnotationsScanner.scan( request );

        System.out.println( "mojoAnnotatedClasses:" + mojoAnnotatedClasses );

        Assertions.assertThat( mojoAnnotatedClasses ).isNotNull().isNotEmpty().hasSize( 1 );

        MojoAnnotatedClass mojoAnnotatedClass = mojoAnnotatedClasses.values().iterator().next();

        assertEquals( FooMojo.class.getName(), mojoAnnotatedClass.getClassName() );
        assertEquals( AbstractFooMojo.class.getName(), mojoAnnotatedClass.getParentClassName() );

        Mojo mojo = mojoAnnotatedClass.getMojo();

        assertEquals( "foo", mojo.name() );
        assertTrue( mojo.threadSafe() );
        assertFalse( mojo.aggregator() );
        assertEquals( LifecyclePhase.COMPILE, mojo.defaultPhase() );

        Execute execute = mojoAnnotatedClass.getExecute();

        assertEquals( "compiler", execute.goal() );
        assertEquals( "my-lifecycle", execute.lifecycle() );
        assertEquals( LifecyclePhase.PACKAGE, execute.phase() );

        Collection<ComponentAnnotationContent> components = mojoAnnotatedClass.getComponents().values();
        Assertions.assertThat( components ).isNotNull().isNotEmpty().hasSize( 2 );

        Collection<ParameterAnnotationContent> parameters = mojoAnnotatedClass.getParameters().values();
        Assertions.assertThat( parameters ).isNotNull().isNotEmpty().hasSize( 2 ).contains(
            new ParameterAnnotationContent( "bar", null, "thebar", "coolbar", true, false, String.class.getName() ),
            new ParameterAnnotationContent( "beer", null, "thebeer", "coolbeer", false, false,
                                            String.class.getName() ) );
    }
}
