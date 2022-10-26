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

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;

import org.apache.maven.tools.plugin.extractor.ExtractionException;
import org.apache.maven.tools.plugin.extractor.annotations.DeprecatedMojo;
import org.apache.maven.tools.plugin.extractor.annotations.ParametersWithGenericsMojo;
import org.apache.maven.tools.plugin.extractor.annotations.datamodel.ParameterAnnotationContent;
import org.codehaus.plexus.logging.Logger;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

class DefaultMojoAnnotationsScannerTest
{
    private DefaultMojoAnnotationsScanner scanner = new DefaultMojoAnnotationsScanner();

    @Test
    void testSkipModuleInfoClassInArchive() throws Exception
    {
        scanner.scanArchive( new File( "target/test-classes/java9-module.jar" ), null, false );
    }

    @Test
    void testJava8Annotations() throws Exception
    {
        scanner.enableLogging( mock( Logger.class ) );
        scanner.scanArchive( new File( "target/test-classes/java8-annotations.jar" ), null, false );
    }

    @Test
    void scanDeprecatedMojoAnnotatins() throws ExtractionException, IOException
    {
        File directoryToScan = new File( DeprecatedMojo.class.getResource( "" ).getFile() );

        scanner.enableLogging( mock( Logger.class ) );
        Map<String, MojoAnnotatedClass> result = scanner.scanDirectory(
            directoryToScan, Collections.singletonList( "DeprecatedMojo.class" ), null, false );

        assertThat( result ).hasSize( 1 );

        MojoAnnotatedClass annotatedClass = result.get( DeprecatedMojo.class.getName() );
        assertThat( annotatedClass.getClassName() ).isEqualTo( DeprecatedMojo.class.getName() );
        assertThat( annotatedClass.getMojo().getDeprecated() ).isEmpty();

        assertThat( annotatedClass.getParameters() )
            .containsOnlyKeys( "deprecatedParameters", "anotherNotDeprecated" );

        assertThat( annotatedClass.getParameters().get( "deprecatedParameters" ).getDeprecated() ).isEmpty();
        assertThat( annotatedClass.getParameters().get( "deprecatedParameters" ).alias() )
            .isEqualTo( "deprecatedParametersAlias" );

        assertThat( annotatedClass.getParameters().get( "anotherNotDeprecated" ).getDeprecated() ).isNull();
        assertThat( annotatedClass.getParameters().get( "anotherNotDeprecated" ).property() )
            .isEqualTo( "property.anotherNotDeprecated" );
    }

    @Test
    void scanParametersWithGenerics() throws ExtractionException, IOException
    {
        File directoryToScan = new File( ParametersWithGenericsMojo.class.getResource( "" ).getFile() );

        scanner.enableLogging( mock( Logger.class ) );
        Map<String, MojoAnnotatedClass> result = scanner.scanDirectory(
            directoryToScan, Collections.singletonList( "ParametersWithGenericsMojo**.class" ), null, false );

        assertThat( result ).hasSize( 2 ); // mojo and nested class

        MojoAnnotatedClass annotatedClass = result.get( ParametersWithGenericsMojo.class.getName() );
        assertThat( annotatedClass.getClassName() ).isEqualTo( ParametersWithGenericsMojo.class.getName() );

        ParameterAnnotationContent parameter = annotatedClass.getParameters().get( "string" );
        assertNotNull( parameter );
        assertEquals( "java.lang.String", parameter.getClassName() );
        assertThat( parameter.getTypeParameters() ).isEmpty();

        parameter = annotatedClass.getParameters().get( "stringBooleanMap" );
        assertNotNull( parameter );
        assertEquals( "java.util.Map", parameter.getClassName() );
        assertThat( parameter.getTypeParameters() ).containsExactly( "java.lang.String", "java.lang.Boolean" );

        parameter = annotatedClass.getParameters().get( "integerCollection" );
        assertNotNull( parameter );
        assertEquals( "java.util.Collection", parameter.getClassName() );
        assertThat( parameter.getTypeParameters() ).containsExactly( "java.lang.Integer" );

        parameter = annotatedClass.getParameters().get( "nestedStringCollection" );
        assertNotNull( parameter );
        assertEquals( "java.util.Collection", parameter.getClassName() );
        assertThat( parameter.getTypeParameters() ).containsExactly( "java.util.Collection<java.lang.String>" );

        parameter = annotatedClass.getParameters().get( "integerArrayCollection" );
        assertNotNull( parameter );
        assertEquals( "java.util.Collection", parameter.getClassName() );
        assertThat( parameter.getTypeParameters() ).containsExactly( "java.lang.Integer[]" );

        parameter = annotatedClass.getParameters().get( "numberList" );
        assertNotNull( parameter );
        assertEquals( "java.util.List", parameter.getClassName() );
        assertThat( parameter.getTypeParameters() ).containsExactly( "java.lang.Number" );
    }
}
