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
package org.apache.maven.tools.plugin.extractor.annotations;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.github.javaparser.ast.body.TypeDeclaration;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.descriptor.InvalidParameterException;
import org.apache.maven.plugin.descriptor.InvalidPluginDescriptorException;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.tools.plugin.DefaultPluginToolsRequest;
import org.apache.maven.tools.plugin.extractor.ExtractionException;
import org.apache.maven.tools.plugin.extractor.annotations.converter.JavadocBlockTagsToXhtmlConverter;
import org.apache.maven.tools.plugin.extractor.annotations.converter.JavadocInlineTagsToXhtmlConverter;
import org.apache.maven.tools.plugin.extractor.annotations.scanner.DefaultMojoAnnotationsScanner;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JavaAnnotationsMojoDescriptorExtractorTest {
    @TempDir
    private Path targetDir;

    MojoDescriptor extractDescriptorFromMojoClass(Class<? extends AbstractMojo> mojoClass)
            throws InvalidPluginDescriptorException, ExtractionException, IOException, URISyntaxException {
        // copy class to an empty tmp directory
        String classFile = mojoClass.getName().substring(mojoClass.getName().lastIndexOf('.') + 1) + ".class";
        Path sourceClass = Paths.get(mojoClass.getResource(classFile).toURI());
        Files.copy(sourceClass, targetDir.resolve(sourceClass.getFileName()));
        JavaAnnotationsMojoDescriptorExtractor mojoDescriptorExtractor = new JavaAnnotationsMojoDescriptorExtractor();
        mojoDescriptorExtractor.mojoAnnotationsScanner = new DefaultMojoAnnotationsScanner();
        PluginDescriptor pluginDescriptor = new PluginDescriptor();
        MavenProject mavenProject = new MavenProject();
        Artifact artifact = new DefaultArtifact("groupId", "artifactId", "1.0.0", null, "jar", "classifier", null);
        mavenProject.setArtifact(artifact);
        mavenProject.getBuild().setOutputDirectory(targetDir.toString());
        List<MojoDescriptor> mojoDescriptors =
                mojoDescriptorExtractor.execute(new DefaultPluginToolsRequest(mavenProject, pluginDescriptor));
        assertEquals(1, mojoDescriptors.size());
        // there should be only one mojo contained in the one class
        return mojoDescriptors.get(0);
    }

    @Test
    void discoverClassesIndexesNestedTypesUnderTheirBinaryNameAsWell() throws Exception {
        Path sourceDirectory = Files.createDirectories(targetDir.resolve("sources"));
        Path source = sourceDirectory.resolve("example/Outer.java");
        Files.createDirectories(source.getParent());
        Files.write(
                source,
                "package example; public class Outer { public static class Inner {} }\n"
                        .getBytes(StandardCharsets.UTF_8));

        try (JavaSourceModel sourceModel = new JavaSourceModel(StandardCharsets.UTF_8)) {
            sourceModel.addSourceDirectory(sourceDirectory.toFile());
            sourceModel.parse();

            Map<String, TypeDeclaration<?>> classes =
                    new JavaAnnotationsMojoDescriptorExtractor().discoverClasses(sourceModel);

            assertTrue(classes.containsKey("example.Outer"));
            // the annotation scanner keys classes by their binary name, so nested types must be
            // reachable as "example.Outer$Inner" too, otherwise their Javadoc is silently dropped
            assertTrue(classes.containsKey("example.Outer.Inner"));
            assertTrue(classes.containsKey("example.Outer$Inner"));
            assertEquals(classes.get("example.Outer.Inner"), classes.get("example.Outer$Inner"));
        }
    }

    @Test
    void assertFooMojo() throws Exception {
        MojoDescriptor mojoDescriptor = extractDescriptorFromMojoClass(FooMojo.class);
        assertEquals("package", mojoDescriptor.getExecutePhase());
        assertEquals("compiler", mojoDescriptor.getExecuteGoal());
        assertEquals("my-lifecycle", mojoDescriptor.getExecuteLifecycle());
    }

    @Test
    void assertExecuteMojo() throws Exception {
        MojoDescriptor mojoDescriptor = extractDescriptorFromMojoClass(ExecuteMojo.class);
        assertEquals("my-phase-id", mojoDescriptor.getExecutePhase());
        assertEquals("compiler", mojoDescriptor.getExecuteGoal());
        assertEquals("my-lifecycle", mojoDescriptor.getExecuteLifecycle());
    }

    @Test
    void assertExecute2Mojo() {
        // two conflicting phase ids set
        assertThrows(InvalidPluginDescriptorException.class, () -> extractDescriptorFromMojoClass(Execute2Mojo.class));
    }

    @Test
    void nestedMojoKeepsItsJavadocMetadata() throws Exception {
        MojoDescriptor mojoDescriptor = extractDescriptorFromSources("NestedMojos.java");

        assertEquals("nested", mojoDescriptor.getGoal());
        assertEquals("org.apache.maven.plugin.coreit.NestedMojos$NestedMojo", mojoDescriptor.getImplementation());
        assertEquals("Runs the nested goal.", mojoDescriptor.getDescription());
        assertEquals("1.2", mojoDescriptor.getSince());
        assertEquals("Use a top-level Mojo instead.", mojoDescriptor.getDeprecated());
    }

    @Test
    void commentOnlySourceDoesNotPreventExtraction() throws Exception {
        MojoDescriptor mojoDescriptor = extractDescriptorFromSources("HelloMojo.java", "CommentOnly.java");

        assertEquals("hello", mojoDescriptor.getGoal());
        assertEquals("org.apache.maven.plugin.coreit.HelloMojo", mojoDescriptor.getImplementation());
        assertEquals("Says hello.", mojoDescriptor.getDescription());
    }

    @Test
    void scanNonCollectionParametersWithMultiValueDefault() throws Exception {
        assertThrows(
                InvalidParameterException.class,
                () -> extractDescriptorFromMojoClass(MultiDefaultValueOnNonCollectionTypeMojo.class));
    }

    @Test
    void scanCollectionParametersWithMultiValueDefault() throws Exception {
        MojoDescriptor mojoDescriptor = extractDescriptorFromMojoClass(MultiDefaultValueOnCollectionTypesMojo.class);
        assertEquals(4, mojoDescriptor.getParameters().size());
        mojoDescriptor.getParameters().forEach(parameter -> {
            assertEquals("value1,value2", parameter.getDefaultValue());
        });
    }

    @Test
    void scanScalarAndArrayDefaults() throws Exception {
        MojoDescriptor descriptor = extractDescriptorFromMojoClass(DefaultValuesMojo.class);
        Map<String, String> defaults = new HashMap<>();
        descriptor.getParameters().forEach(parameter -> defaults.put(parameter.getName(), parameter.getDefaultValue()));
        assertEquals(4, defaults.size());
        assertEquals("scalar", defaults.get("scalar"));
        assertEquals("one", defaults.get("single"));
        assertNull(defaults.get("empty"));
        assertNull(defaults.get("absent"));
    }

    @Test
    void rejectBothDefaultAttributes() {
        InvalidParameterException exception = assertThrows(
                InvalidParameterException.class, () -> extractDescriptorFromMojoClass(ConflictingDefaultsMojo.class));
        assertTrue(exception.getMessage().contains("defaultValue and defaultValues"));
    }

    @Test
    void rejectBothDefaultAttributesEvenWhenEmpty() {
        assertThrows(
                InvalidParameterException.class,
                () -> extractDescriptorFromMojoClass(EmptyConflictingDefaultsMojo.class));
    }

    @Mojo(name = "defaults")
    public static class DefaultValuesMojo extends AbstractMojo {
        @Parameter(defaultValue = "scalar")
        private String scalar;

        @Parameter(defaultValues = {"one"})
        private String single;

        @Parameter(defaultValues = {})
        private String empty;

        @Parameter
        private String absent;

        @Override
        public void execute() {}
    }

    @Mojo(name = "conflicting-defaults")
    public static class ConflictingDefaultsMojo extends AbstractMojo {
        @Parameter(
                defaultValue = "one",
                defaultValues = {"two"})
        private String value;

        @Override
        public void execute() {}
    }

    @Mojo(name = "empty-conflicting-defaults")
    public static class EmptyConflictingDefaultsMojo extends AbstractMojo {
        @Parameter(
                defaultValue = "",
                defaultValues = {})
        private String value;

        @Override
        public void execute() {}
    }

    /**
     * Compiles the named sources of {@code src/test/resources/mojo-sources} and extracts the single mojo they
     * contain, with the sources registered as compile source root so that the Javadoc is read.
     */
    private MojoDescriptor extractDescriptorFromSources(String... fileNames) throws Exception {
        Path sourceRoot = targetDir.resolve("sources");
        Path classesDirectory = targetDir.resolve("classes");
        Files.createDirectories(classesDirectory);

        List<String> arguments = new ArrayList<>();
        arguments.add("-d");
        arguments.add(classesDirectory.toString());
        arguments.add("-classpath");
        arguments.add(System.getProperty("java.class.path"));
        for (String fileName : fileNames) {
            Path source = sourceRoot.resolve("org/apache/maven/plugin/coreit").resolve(fileName);
            Files.createDirectories(source.getParent());
            try (InputStream in =
                    getClass().getResourceAsStream("/mojo-sources/org/apache/maven/plugin/coreit/" + fileName)) {
                assertNotNull(in, fileName);
                Files.copy(in, source);
            }
            arguments.add(source.toString());
        }
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        assertNotNull(compiler);
        assertEquals(0, compiler.run(null, null, null, arguments.toArray(new String[0])));

        MavenProject mavenProject = new MavenProject();
        mavenProject.setArtifact(
                new DefaultArtifact("groupId", "artifactId", "1.0.0", null, "jar", "classifier", null));
        mavenProject.getBuild().setOutputDirectory(classesDirectory.toString());
        mavenProject.addCompileSourceRoot(sourceRoot.toString());

        JavaAnnotationsMojoDescriptorExtractor extractor = new JavaAnnotationsMojoDescriptorExtractor();
        extractor.mojoAnnotationsScanner = new DefaultMojoAnnotationsScanner();
        JavadocInlineTagsToXhtmlConverter inlineConverter = new JavadocInlineTagsToXhtmlConverter(new HashMap<>());
        inject(extractor, "javadocInlineTagsToHtmlConverter", inlineConverter);
        inject(
                extractor,
                "javadocBlockTagsToHtmlConverter",
                new JavadocBlockTagsToXhtmlConverter(inlineConverter, new HashMap<>()));
        List<MojoDescriptor> mojoDescriptors =
                extractor.execute(new DefaultPluginToolsRequest(mavenProject, new PluginDescriptor()));
        assertEquals(1, mojoDescriptors.size());
        return mojoDescriptors.get(0);
    }

    private static void inject(Object target, String fieldName, Object value) throws ReflectiveOperationException {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
