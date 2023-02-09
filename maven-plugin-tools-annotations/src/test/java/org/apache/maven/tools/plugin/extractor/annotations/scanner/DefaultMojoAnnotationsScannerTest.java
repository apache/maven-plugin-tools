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
package org.apache.maven.tools.plugin.extractor.annotations.scanner;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.maven.plugins.annotations.Execute;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.project.MavenProject;
import org.apache.maven.tools.plugin.extractor.ExtractionException;
import org.apache.maven.tools.plugin.extractor.annotations.AbstractFooMojo;
import org.apache.maven.tools.plugin.extractor.annotations.DeprecatedMojo;
import org.apache.maven.tools.plugin.extractor.annotations.FooMojo;
import org.apache.maven.tools.plugin.extractor.annotations.ParametersWithGenericsMojo;
import org.apache.maven.tools.plugin.extractor.annotations.datamodel.ComponentAnnotationContent;
import org.apache.maven.tools.plugin.extractor.annotations.datamodel.ParameterAnnotationContent;
import org.codehaus.plexus.logging.Logger;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.codehaus.plexus.testing.PlexusExtension.getBasedir;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class DefaultMojoAnnotationsScannerTest {
    private DefaultMojoAnnotationsScanner scanner = new DefaultMojoAnnotationsScanner();

    @Test
    void testSkipModuleInfoClassInArchive() throws Exception {
        scanner.scanArchive(new File("target/test-classes/java9-module.jar"), null, false);
    }

    @Test
    void testJava8Annotations() throws Exception {
        scanner.enableLogging(mock(Logger.class));
        scanner.scanArchive(new File("target/test-classes/java8-annotations.jar"), null, false);
    }

    @Test
    void scanDeprecatedMojoAnnotatins() throws ExtractionException, IOException {
        File directoryToScan = new File(DeprecatedMojo.class.getResource("").getFile());

        scanner.enableLogging(mock(Logger.class));
        Map<String, MojoAnnotatedClass> result =
                scanner.scanDirectory(directoryToScan, Collections.singletonList("DeprecatedMojo.class"), null, false);

        assertThat(result).hasSize(1);

        MojoAnnotatedClass annotatedClass = result.get(DeprecatedMojo.class.getName());
        assertThat(annotatedClass.getClassName()).isEqualTo(DeprecatedMojo.class.getName());
        assertThat(annotatedClass.getMojo().getDeprecated()).isEmpty();

        assertThat(annotatedClass.getParameters()).containsOnlyKeys("deprecatedParameters", "anotherNotDeprecated");

        assertThat(annotatedClass.getParameters().get("deprecatedParameters").getDeprecated())
                .isEmpty();
        assertThat(annotatedClass.getParameters().get("deprecatedParameters").alias())
                .isEqualTo("deprecatedParametersAlias");

        assertThat(annotatedClass.getParameters().get("anotherNotDeprecated").getDeprecated())
                .isNull();
        assertThat(annotatedClass.getParameters().get("anotherNotDeprecated").property())
                .isEqualTo("property.anotherNotDeprecated");
    }

    @Test
    void scanParametersWithGenerics() throws ExtractionException, IOException {
        File directoryToScan =
                new File(ParametersWithGenericsMojo.class.getResource("").getFile());

        scanner.enableLogging(mock(Logger.class));
        Map<String, MojoAnnotatedClass> result = scanner.scanDirectory(
                directoryToScan, Collections.singletonList("ParametersWithGenericsMojo**.class"), null, false);

        assertThat(result).hasSize(2); // mojo and nested class

        MojoAnnotatedClass annotatedClass = result.get(ParametersWithGenericsMojo.class.getName());
        assertThat(annotatedClass.getClassName()).isEqualTo(ParametersWithGenericsMojo.class.getName());

        ParameterAnnotationContent parameter = annotatedClass.getParameters().get("string");
        assertNotNull(parameter);
        assertEquals("java.lang.String", parameter.getClassName());
        assertThat(parameter.getTypeParameters()).isEmpty();

        parameter = annotatedClass.getParameters().get("stringBooleanMap");
        assertNotNull(parameter);
        assertEquals("java.util.Map", parameter.getClassName());
        assertThat(parameter.getTypeParameters()).containsExactly("java.lang.String", "java.lang.Boolean");

        parameter = annotatedClass.getParameters().get("integerCollection");
        assertNotNull(parameter);
        assertEquals("java.util.Collection", parameter.getClassName());
        assertThat(parameter.getTypeParameters()).containsExactly("java.lang.Integer");

        parameter = annotatedClass.getParameters().get("nestedStringCollection");
        assertNotNull(parameter);
        assertEquals("java.util.Collection", parameter.getClassName());
        assertThat(parameter.getTypeParameters()).containsExactly("java.util.Collection<java.lang.String>");

        parameter = annotatedClass.getParameters().get("integerArrayCollection");
        assertNotNull(parameter);
        assertEquals("java.util.Collection", parameter.getClassName());
        assertThat(parameter.getTypeParameters()).containsExactly("java.lang.Integer[]");

        parameter = annotatedClass.getParameters().get("numberList");
        assertNotNull(parameter);
        assertEquals("java.util.List", parameter.getClassName());
        assertThat(parameter.getTypeParameters()).containsExactly("java.lang.Number");
    }

    @Test
    void scanFooMojoClass() throws Exception {
        MojoAnnotationsScannerRequest request = new MojoAnnotationsScannerRequest();
        request.setClassesDirectories(Collections.singletonList(new File(getBasedir(), "target/test-classes")));
        request.setIncludePatterns(Arrays.asList("**/FooMojo.class"));
        request.setProject(new MavenProject());

        scanner.enableLogging(mock(Logger.class));
        Map<String, MojoAnnotatedClass> mojoAnnotatedClasses = scanner.scan(request);

        System.out.println("mojoAnnotatedClasses:" + mojoAnnotatedClasses);

        assertThat(mojoAnnotatedClasses).isNotNull().isNotEmpty().hasSize(1);

        MojoAnnotatedClass mojoAnnotatedClass =
                mojoAnnotatedClasses.values().iterator().next();

        assertEquals(FooMojo.class.getName(), mojoAnnotatedClass.getClassName());
        assertEquals(AbstractFooMojo.class.getName(), mojoAnnotatedClass.getParentClassName());

        Mojo mojo = mojoAnnotatedClass.getMojo();

        assertEquals("foo", mojo.name());
        assertTrue(mojo.threadSafe());
        assertFalse(mojo.aggregator());
        assertEquals(LifecyclePhase.COMPILE, mojo.defaultPhase());

        Execute execute = mojoAnnotatedClass.getExecute();

        assertEquals("compiler", execute.goal());
        assertEquals("my-lifecycle", execute.lifecycle());
        assertEquals(LifecyclePhase.PACKAGE, execute.phase());

        Collection<ComponentAnnotationContent> components =
                mojoAnnotatedClass.getComponents().values();
        assertThat(components).isNotNull().isNotEmpty().hasSize(1);

        Collection<ParameterAnnotationContent> parameters =
                mojoAnnotatedClass.getParameters().values();
        assertThat(parameters)
                .isNotNull()
                .isNotEmpty()
                .hasSize(5)
                .containsExactlyInAnyOrder(
                        new ParameterAnnotationContent(
                                "bar",
                                null,
                                "thebar",
                                "coolbar",
                                true,
                                false,
                                String.class.getName(),
                                Collections.emptyList(),
                                false),
                        new ParameterAnnotationContent(
                                "beer",
                                null,
                                "thebeer",
                                "coolbeer",
                                false,
                                false,
                                String.class.getName(),
                                Collections.emptyList(),
                                false),
                        new ParameterAnnotationContent(
                                "paramFromSetter",
                                null,
                                "props.paramFromSetter",
                                null,
                                false,
                                false,
                                String.class.getName(),
                                Collections.emptyList(),
                                true),
                        new ParameterAnnotationContent(
                                "paramFromAdd",
                                null,
                                "props.paramFromAdd",
                                null,
                                false,
                                false,
                                String.class.getName(),
                                Collections.emptyList(),
                                true),
                        new ParameterAnnotationContent(
                                "paramFromSetterDeprecated",
                                null,
                                "props.paramFromSetterDeprecated",
                                null,
                                false,
                                false,
                                List.class.getName(),
                                Collections.singletonList("java.lang.String"),
                                true));
    }
}
