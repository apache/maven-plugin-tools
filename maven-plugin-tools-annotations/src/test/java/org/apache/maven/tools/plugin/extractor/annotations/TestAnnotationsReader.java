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

import javax.inject.Inject;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.maven.plugins.annotations.Execute;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.project.MavenProject;
import org.apache.maven.tools.plugin.extractor.annotations.datamodel.ComponentAnnotationContent;
import org.apache.maven.tools.plugin.extractor.annotations.datamodel.ParameterAnnotationContent;
import org.apache.maven.tools.plugin.extractor.annotations.scanner.MojoAnnotatedClass;
import org.apache.maven.tools.plugin.extractor.annotations.scanner.MojoAnnotationsScanner;
import org.apache.maven.tools.plugin.extractor.annotations.scanner.MojoAnnotationsScannerRequest;
import org.codehaus.plexus.testing.PlexusTest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.codehaus.plexus.testing.PlexusExtension.getBasedir;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Olivier Lamy
 */
@PlexusTest
class TestAnnotationsReader
{

    @Inject
    MojoAnnotationsScanner mojoAnnotationsScanner;

    @Test
    void testReadMojoClass()
        throws Exception
    {
        MojoAnnotationsScannerRequest request = new MojoAnnotationsScannerRequest();
        request.setClassesDirectories( Collections.singletonList( new File( getBasedir(), "target/test-classes" ) ) );
        request.setIncludePatterns( Arrays.asList( "**/FooMojo.class" ) );
        request.setProject( new MavenProject() );

        Map<String, MojoAnnotatedClass> mojoAnnotatedClasses = mojoAnnotationsScanner.scan( request );

        System.out.println( "mojoAnnotatedClasses:" + mojoAnnotatedClasses );

        assertThat( mojoAnnotatedClasses ).isNotNull().isNotEmpty().hasSize( 1 );

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
        assertThat( components ).isNotNull().isNotEmpty().hasSize( 1 );

        Collection<ParameterAnnotationContent> parameters = mojoAnnotatedClass.getParameters().values();
        assertThat( parameters ).isNotNull()
            .isNotEmpty()
            .hasSize( 5 )
            .containsExactlyInAnyOrder(
                new ParameterAnnotationContent( "bar", null, "thebar", "coolbar", true, false,
                                                String.class.getName(), Collections.emptyList(), false ),
                new ParameterAnnotationContent( "beer", null, "thebeer", "coolbeer", false, false,
                                                String.class.getName(), Collections.emptyList(), false ),
                new ParameterAnnotationContent( "paramFromSetter", null, "props.paramFromSetter", null,
                                                false,
                                                false, String.class.getName(), Collections.emptyList(), true ),
                new ParameterAnnotationContent( "paramFromAdd", null, "props.paramFromAdd", null,
                                                false,
                                                false, String.class.getName(), Collections.emptyList(), true ),
                new ParameterAnnotationContent( "paramFromSetterDeprecated", null, "props.paramFromSetterDeprecated", null,
                                                false,
                                                false, List.class.getName(), Collections.singletonList("java.lang.String"), true )
            );
    }
}
