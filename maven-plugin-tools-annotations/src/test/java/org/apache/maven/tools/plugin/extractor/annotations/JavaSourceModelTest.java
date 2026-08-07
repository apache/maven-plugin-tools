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

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.github.javaparser.resolution.declarations.ResolvedReferenceTypeDeclaration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JavaSourceModelTest {

    @TempDir
    Path sourceDirectory;

    @Test
    void acceptsCommentOnlyAndCurrentSyntax() throws Exception {
        write("CommentOnly.java", "// no declaration\n");
        write(
                "example/Model.java",
                "package example;\n"
                        + "import java.lang.annotation.ElementType;\n"
                        + "import java.lang.annotation.Target;\n"
                        + "import java.util.List;\n"
                        + "@Target(ElementType.TYPE_USE) @interface Nullable {}\n"
                        + "class Model { List<@Nullable String> values; }\n");

        try (JavaSourceModel model = new JavaSourceModel(StandardCharsets.UTF_8)) {
            model.addSourceDirectory(sourceDirectory.toFile());
            model.parse();

            assertTrue(model.getType("example.Model").isPresent());
            assertTrue(model.resolveType("java.lang.String").isPresent());
        }
    }

    @Test
    void indexesNestedTypesAndExportedModules() throws Exception {
        write("module-info.java", "module example.module { exports example; }\n");
        write("example/Outer.java", "package example; public class Outer { public static class Nested {} }\n");

        try (JavaSourceModel model = new JavaSourceModel(StandardCharsets.UTF_8)) {
            model.addSourceDirectory(sourceDirectory.toFile());
            model.parse();

            assertTrue(model.getType("example.Outer.Nested").isPresent());
            assertEquals("example.module", model.getModuleName("example").orElse(null));
            assertTrue(model.hasPackage("example"));
        }
    }

    @Test
    void reportsSourcePathForInvalidJava() throws Exception {
        Path source = write("example/Broken.java", "package example; class Broken {\n");

        try (JavaSourceModel model = new JavaSourceModel(StandardCharsets.UTF_8)) {
            model.addSourceDirectory(sourceDirectory.toFile());
            IOException exception = assertThrows(IOException.class, model::parse);
            assertTrue(exception.getMessage().contains(source.toString()));
        }
    }

    @Test
    void resolvesDirectoryClassesAgainstJarEntriesAndIndexesTheirPackages() throws Exception {
        Path dependencySource = write(
                sourceDirectory.resolve("dependency-sources"),
                "dependency/Parent.java",
                "package dependency; public class Parent {}\n");
        Path dependencyClasses = sourceDirectory.resolve("dependency-classes");
        compile(dependencySource, dependencyClasses);
        Path dependencyJar = jar(dependencyClasses, sourceDirectory.resolve("dependency.jar"));

        Path reactorSource = write(
                sourceDirectory.resolve("reactor-sources"),
                "reactor/Child.java",
                "package reactor; public class Child extends dependency.Parent {}\n");
        Path reactorClasses = sourceDirectory.resolve("reactor-classes");
        compile(reactorSource, reactorClasses, dependencyJar);

        try (JavaSourceModel model = new JavaSourceModel(StandardCharsets.UTF_8)) {
            model.addClassPathEntry(reactorClasses.toFile());
            model.addClassPathEntry(dependencyJar.toFile());
            model.parse();

            ResolvedReferenceTypeDeclaration child =
                    model.resolveType("reactor.Child").orElseThrow(AssertionError::new);
            assertTrue(child.getAncestors().stream()
                    .anyMatch(ancestor -> "dependency.Parent".equals(ancestor.getQualifiedName())));
            assertTrue(model.hasPackage("reactor"));
        }
    }

    @Test
    void resolvesClassesFromTheCompleteJavaClassLibrary() throws Exception {
        try (JavaSourceModel model = new JavaSourceModel(StandardCharsets.UTF_8)) {
            model.parse();

            assertTrue(model.resolveType("org.w3c.dom.Document").isPresent());
            assertTrue(model.resolveType("org.xml.sax.InputSource").isPresent());
        }
    }

    private Path write(String relativePath, String source) throws IOException {
        return write(sourceDirectory, relativePath, source);
    }

    private static Path write(Path directory, String relativePath, String source) throws IOException {
        Path path = directory.resolve(relativePath);
        Files.createDirectories(path.getParent());
        Files.write(path, source.getBytes(StandardCharsets.UTF_8));
        return path;
    }

    private static void compile(Path source, Path outputDirectory, Path... classPath) throws IOException {
        Files.createDirectories(outputDirectory);
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        assertNotNull(compiler);
        List<String> arguments = new ArrayList<>();
        arguments.add("-d");
        arguments.add(outputDirectory.toString());
        if (classPath.length > 0) {
            arguments.add("-classpath");
            arguments.add(Arrays.stream(classPath).map(Path::toString).collect(Collectors.joining(File.pathSeparator)));
        }
        arguments.add(source.toString());
        assertEquals(0, compiler.run(null, null, null, arguments.toArray(new String[0])));
    }

    private static Path jar(Path classesDirectory, Path jarFile) throws IOException {
        List<Path> classFiles;
        try (Stream<Path> files = Files.walk(classesDirectory)) {
            classFiles = files.filter(Files::isRegularFile).collect(Collectors.toList());
        }
        try (JarOutputStream output = new JarOutputStream(Files.newOutputStream(jarFile))) {
            for (Path classFile : classFiles) {
                String name = classesDirectory.relativize(classFile).toString().replace(File.separatorChar, '/');
                output.putNextEntry(new JarEntry(name));
                Files.copy(classFile, output);
                output.closeEntry();
            }
        }
        return jarFile;
    }
}
