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

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.modules.ModuleExportsDirective;
import com.github.javaparser.resolution.TypeSolver;
import com.github.javaparser.resolution.declarations.ResolvedReferenceTypeDeclaration;
import com.github.javaparser.resolution.model.SymbolReference;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ClassLoaderTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JarTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import com.github.javaparser.utils.SourceRoot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Source declarations and type resolution used while extracting Javadocs. */
public final class JavaSourceModel implements Closeable {

    private static final Logger LOGGER = LoggerFactory.getLogger(JavaSourceModel.class);

    private static final Set<String> JAVA_RUNTIME_PACKAGES = javaRuntimePackages();

    private final Charset encoding;
    private final Set<Path> sourceDirectories = new LinkedHashSet<>();
    private final Set<Path> classPathEntries = new LinkedHashSet<>();
    private final Map<String, TypeDeclaration<?>> types = new LinkedHashMap<>();
    private final Map<String, String> modulesByExportedPackage = new LinkedHashMap<>();
    private final Set<String> packages = new LinkedHashSet<>(JAVA_RUNTIME_PACKAGES);
    private final Set<String> internalPackages = new LinkedHashSet<>();

    private ParserConfiguration parserConfiguration;
    private CombinedTypeSolver typeSolver;
    private URLClassLoader classPathLoader;
    private boolean parsed;

    public JavaSourceModel(Charset encoding) {
        this.encoding = encoding;
    }

    public void addSourceDirectory(File directory) throws IOException {
        if (directory != null && directory.isDirectory()) {
            sourceDirectories.add(directory.toPath().toRealPath());
        }
    }

    public void addClassPathEntry(File entry) throws IOException {
        if (entry != null && entry.exists()) {
            classPathEntries.add(entry.toPath().toRealPath());
        }
    }

    public void parse() throws IOException {
        if (parsed) {
            return;
        }
        parsed = true;

        parserConfiguration = new ParserConfiguration()
                .setCharacterEncoding(encoding)
                .setLanguageLevel(ParserConfiguration.LanguageLevel.BLEEDING_EDGE);
        typeSolver = new CombinedTypeSolver();
        for (Path sourceDirectory : sourceDirectories) {
            typeSolver.add(new JavaParserTypeSolver(sourceDirectory, parserConfiguration));
        }

        List<URL> classPathUrls = new ArrayList<>();
        for (Path classPathEntry : classPathEntries) {
            if (Files.isDirectory(classPathEntry)) {
                classPathUrls.add(classPathEntry.toUri().toURL());
                indexClassDirectory(classPathEntry);
            } else if (classPathEntry.getFileName().toString().endsWith(".jar")) {
                classPathUrls.add(classPathEntry.toUri().toURL());
                JarTypeSolver jarTypeSolver = new JarTypeSolver(classPathEntry);
                typeSolver.add(jarTypeSolver);
                jarTypeSolver.getKnownClasses().stream()
                        .map(JavaSourceModel::packageName)
                        .filter(name -> !name.isEmpty())
                        .forEach(packages::add);
            }
        }
        if (!classPathUrls.isEmpty()) {
            classPathLoader = new URLClassLoader(classPathUrls.toArray(new URL[0]), ClassLoader.getSystemClassLoader());
            typeSolver.add(new ClassLoaderTypeSolver(classPathLoader));
        }
        typeSolver.add(new ReflectionTypeSolver(ReflectionTypeSolver.JCL_ONLY));
        parserConfiguration.setSymbolResolver(new JavaSymbolSolver(typeSolver));

        for (Path sourceDirectory : sourceDirectories) {
            SourceRoot sourceRoot = new SourceRoot(sourceDirectory, parserConfiguration);
            for (ParseResult<CompilationUnit> result : sourceRoot.tryToParse()) {
                if (!result.isSuccessful()) {
                    String path = sourcePath(result, sourceDirectory);
                    String problems = result.getProblems().stream()
                            .map(Object::toString)
                            .collect(Collectors.joining(System.lineSeparator()));
                    LOGGER.warn(
                            "Unable to parse {}. Javadoc from this source file will be skipped.{}",
                            path,
                            problems.isEmpty() ? "" : System.lineSeparator() + problems);
                    continue;
                }
                if (result.getResult().isPresent()) {
                    index(result.getResult().get());
                } else {
                    LOGGER.warn(
                            "Parser returned no compilation unit for {}. Javadoc from this source file will be skipped.",
                            sourcePath(result, sourceDirectory));
                }
            }
        }
    }

    private static String sourcePath(ParseResult<CompilationUnit> result, Path sourceDirectory) {
        return result.getSourcePath().map(Path::toString).orElse(sourceDirectory.toString());
    }

    private void indexClassDirectory(Path directory) throws IOException {
        indexClassDirectory(directory, packages);
    }

    private static void indexClassDirectory(Path directory, Set<String> result) throws IOException {
        try (Stream<Path> entries = Files.walk(directory)) {
            entries.filter(Files::isRegularFile)
                    .map(directory::relativize)
                    .filter(path -> path.getFileName().toString().endsWith(".class"))
                    .map(Path::getParent)
                    .filter(path -> path != null)
                    .map(Path::toString)
                    .map(name -> name.replace(File.separatorChar, '.'))
                    .filter(name -> !name.isEmpty())
                    .forEach(result::add);
        }
    }

    private void index(CompilationUnit unit) {
        unit.getPackageDeclaration()
                .map(declaration -> declaration.getName().asString())
                .ifPresent(name -> {
                    packages.add(name);
                    internalPackages.add(name);
                });
        for (TypeDeclaration<?> type : unit.findAll(TypeDeclaration.class)) {
            type.getFullyQualifiedName().ifPresent(name -> types.putIfAbsent(name, type));
        }
        unit.getModule()
                .ifPresent(module -> module.getDirectives().stream()
                        .filter(ModuleExportsDirective.class::isInstance)
                        .map(ModuleExportsDirective.class::cast)
                        .forEach(exports -> modulesByExportedPackage.putIfAbsent(
                                exports.getName().asString(), module.getName().asString())));
    }

    public Collection<TypeDeclaration<?>> getTypes() {
        return Collections.unmodifiableCollection(types.values());
    }

    public Optional<TypeDeclaration<?>> getType(String fullyQualifiedName) {
        return Optional.ofNullable(types.get(fullyQualifiedName));
    }

    public Optional<TypeDeclaration<?>> getType(ResolvedReferenceTypeDeclaration declaration) {
        return getType(declaration.getQualifiedName());
    }

    public Optional<String> getModuleName(String packageName) {
        return Optional.ofNullable(modulesByExportedPackage.get(packageName));
    }

    public boolean hasPackage(String packageName) {
        return packages.contains(packageName);
    }

    public boolean isInternal(ResolvedReferenceTypeDeclaration declaration) {
        return types.containsKey(declaration.getQualifiedName());
    }

    public boolean isInternalPackage(String packageName) {
        return internalPackages.contains(packageName);
    }

    public Optional<ResolvedReferenceTypeDeclaration> resolveType(String fullyQualifiedName) {
        ensureParsed();
        SymbolReference<ResolvedReferenceTypeDeclaration> reference = typeSolver.tryToSolveType(fullyQualifiedName);
        return reference.isSolved() ? Optional.of(reference.getCorrespondingDeclaration()) : Optional.empty();
    }

    public TypeSolver getTypeSolver() {
        ensureParsed();
        return typeSolver;
    }

    public String getLocation(Node node, int fallbackLine) {
        int line = fallbackLine > 0
                ? fallbackLine
                : node.getBegin().map(position -> position.line).orElse(0);
        return node.findCompilationUnit()
                .flatMap(CompilationUnit::getStorage)
                .map(storage -> java.nio.file.Paths.get("")
                                .toAbsolutePath()
                                .toUri()
                                .relativize(storage.getPath().toUri())
                                .toString()
                        + ":" + line)
                .orElse("unknown:" + line);
    }

    private void ensureParsed() {
        if (!parsed) {
            throw new IllegalStateException("Java source model has not been parsed");
        }
    }

    private static String packageName(String className) {
        int separator = className.lastIndexOf('.');
        return separator > 0 ? className.substring(0, separator) : "";
    }

    private static Set<String> javaRuntimePackages() {
        Set<String> result = new LinkedHashSet<>();
        try {
            Class<?> moduleLayerClass = Class.forName("java.lang.ModuleLayer");
            Class<?> moduleClass = Class.forName("java.lang.Module");
            Object bootLayer = moduleLayerClass.getMethod("boot").invoke(null);
            Collection<?> modules =
                    (Collection<?>) moduleLayerClass.getMethod("modules").invoke(bootLayer);
            Method getPackages = moduleClass.getMethod("getPackages");
            for (Object module : modules) {
                for (Object packageName : (Set<?>) getPackages.invoke(module)) {
                    result.add((String) packageName);
                }
            }
        } catch (ClassNotFoundException e) {
            indexBootClassPathPackages(result);
        } catch (ReflectiveOperationException e) {
            LOGGER.warn("Could not index packages from the Java runtime module layer", e);
        }
        return Collections.unmodifiableSet(result);
    }

    private static void indexBootClassPathPackages(Set<String> result) {
        String bootClassPath = System.getProperty("sun.boot.class.path");
        if (bootClassPath == null) {
            return;
        }
        for (String entry : bootClassPath.split(java.util.regex.Pattern.quote(File.pathSeparator))) {
            Path path = new File(entry).toPath();
            try {
                if (Files.isDirectory(path)) {
                    indexClassDirectory(path, result);
                } else if (Files.isRegularFile(path)) {
                    indexJar(path, result);
                }
            } catch (IOException e) {
                LOGGER.debug("Could not index Java runtime packages from {}", path, e);
            }
        }
    }

    private static void indexJar(Path jar, Set<String> result) throws IOException {
        try (JarFile jarFile = new JarFile(jar.toFile())) {
            Enumeration<JarEntry> entries = jarFile.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String name = entry.getName();
                if (!entry.isDirectory() && name.endsWith(".class")) {
                    int separator = name.lastIndexOf('/');
                    if (separator > 0) {
                        result.add(name.substring(0, separator).replace('/', '.'));
                    }
                }
            }
        }
    }

    @Override
    public void close() throws IOException {
        if (classPathLoader != null) {
            classPathLoader.close();
        }
    }
}
