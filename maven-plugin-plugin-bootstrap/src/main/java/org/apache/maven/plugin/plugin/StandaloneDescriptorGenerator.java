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
package org.apache.maven.plugin.plugin;

import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.maven.model.Build;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.project.MavenProject;
import org.apache.maven.tools.plugin.DefaultPluginToolsRequest;
import org.apache.maven.tools.plugin.ExtendedMojoDescriptor;
import org.apache.maven.tools.plugin.extractor.annotations.JavaAnnotationsMojoDescriptorExtractor;
import org.apache.maven.tools.plugin.extractor.annotations.converter.JavadocBlockTagsToXhtmlConverter;
import org.apache.maven.tools.plugin.extractor.annotations.converter.JavadocInlineTagsToXhtmlConverter;
import org.apache.maven.tools.plugin.extractor.annotations.scanner.DefaultMojoAnnotationsScanner;
import org.apache.maven.tools.plugin.generator.GeneratorUtils;
import org.apache.maven.tools.plugin.generator.PluginDescriptorFilesGenerator;
import org.apache.maven.tools.plugin.scanner.DefaultMojoScanner;
import org.apache.maven.tools.plugin.scanner.MojoScanner;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.LocalRepositoryManager;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResult;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;

/**
 * Minimal standalone runner to generate the bootstrap plugin descriptor container-free.
 */
public class StandaloneDescriptorGenerator {

    public static void main(String[] args) {
        try {
            File pomFile =
                    (args.length == 0) ? new File("pom.xml").getAbsoluteFile() : new File(args[0]).getAbsoluteFile();
            run(pomFile);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    public static void run(File pomFile) throws Exception {
        Model model = readModel(pomFile);

        Map<String, String> properties = new LinkedHashMap<>();
        Map<String, String> managedVersions = new LinkedHashMap<>();
        gatherPomChainInfo(pomFile, model, properties, managedVersions);

        File baseDir = pomFile.getParentFile();
        File sibling = new File(baseDir, "../maven-plugin-plugin/src/main/java").getAbsoluteFile();
        File sourceDirectory = sibling.isDirectory() ? sibling : new File(baseDir, "src/main/java").getAbsoluteFile();
        File classesDirectory = new File(baseDir, "target/classes").getAbsoluteFile();
        File outputDirectory = new File(classesDirectory, "META-INF/maven").getAbsoluteFile();

        RepositorySystem repoSystem = createMinimalRepositorySystem();
        DefaultRepositorySystemSession repoSession = createRepositorySession(repoSystem);

        MavenProject project = newProject(model, pomFile, properties);

        org.apache.maven.artifact.handler.ArtifactHandler projectHandler =
                new org.apache.maven.artifact.handler.DefaultArtifactHandler("maven-plugin");
        org.apache.maven.artifact.Artifact projectArtifact = new org.apache.maven.artifact.DefaultArtifact(
                project.getGroupId(),
                project.getArtifactId(),
                project.getVersion(),
                null,
                "maven-plugin",
                "",
                projectHandler);
        project.setArtifact(projectArtifact);

        Build build = new Build();
        File targetDirectory =
                classesDirectory.getParentFile() != null ? classesDirectory.getParentFile() : classesDirectory;
        build.setDirectory(targetDirectory.getAbsolutePath());
        build.setOutputDirectory(classesDirectory.getAbsolutePath());
        project.setBuild(build);
        project.addCompileSourceRoot(sourceDirectory.getAbsolutePath());
        File localSrc = new File(baseDir, "src/main/java");
        if (localSrc.isDirectory() && !localSrc.equals(sourceDirectory)) {
            project.addCompileSourceRoot(localSrc.getAbsolutePath());
        }

        project.setArtifacts(populateDependencies(model, project, properties, managedVersions));

        JavadocInlineTagsToXhtmlConverter inlineTagsConverter =
                new JavadocInlineTagsToXhtmlConverter(Collections.emptyMap());
        JavadocBlockTagsToXhtmlConverter blockTagsConverter =
                new JavadocBlockTagsToXhtmlConverter(inlineTagsConverter, Collections.emptyMap());

        JavaAnnotationsMojoDescriptorExtractor extractor = new JavaAnnotationsMojoDescriptorExtractor();
        setField(extractor, "mojoAnnotationsScanner", new DefaultMojoAnnotationsScanner());
        setField(extractor, "javadocInlineTagsToHtmlConverter", inlineTagsConverter);
        setField(extractor, "javadocBlockTagsToHtmlConverter", blockTagsConverter);
        setField(extractor, "repositorySystem", repoSystem);
        setField(extractor, "archiverManager", createArchiverManager());

        MojoScanner mojoScanner = new DefaultMojoScanner(Collections.singletonMap("java-annotations", extractor));

        PluginDescriptor pluginDescriptor = buildPluginDescriptor(project, model, properties);

        DefaultPluginToolsRequest request = new DefaultPluginToolsRequest(project, pluginDescriptor);
        request.setRepoSession(repoSession);
        request.setEncoding("UTF-8");
        request.setSkipErrorNoDescriptorsFound(true);
        request.setDependencies(buildScanArtifacts(project.getArtifacts()));

        mojoScanner.populatePluginDescriptor(request);

        outputDirectory.mkdirs();
        for (MojoDescriptor md : pluginDescriptor.getMojos()) {
            if (md instanceof ExtendedMojoDescriptor && ((ExtendedMojoDescriptor) md).isV4Api()) {
                generateV4Factory(md, classesDirectory);
            }
        }
        generateV4DiIndex(classesDirectory, outputDirectory);

        PluginDescriptorFilesGenerator generator = new PluginDescriptorFilesGenerator();
        generator.execute(outputDirectory, request);

        File generatedFile = new File(outputDirectory, "plugin.xml");
        if (!generatedFile.exists()) {
            throw new IllegalStateException("Descriptor was not generated: " + generatedFile.getAbsolutePath());
        }
    }

    private static void generateV4Factory(MojoDescriptor md, File classesDirectory) throws Exception {
        String mojoClassName = md.getImplementation();
        String packageName = mojoClassName.substring(0, mojoClassName.lastIndexOf('.'));
        String generatorClassName = mojoClassName.substring(mojoClassName.lastIndexOf('.') + 1) + "Factory";
        String mojoName = md.getId();

        byte[] bin = DescriptorGeneratorMojo.computeGeneratorClassBytes(
                packageName, generatorClassName, mojoName, mojoClassName);

        File packageDir = new File(classesDirectory, packageName.replace('.', '/'));
        packageDir.mkdirs();
        File classFile = new File(packageDir, generatorClassName + ".class");
        Files.write(classFile.toPath(), bin);
    }

    static void generateV4DiIndex(File classesDirectory, File outputDirectory) throws Exception {
        Set<String> diBeans = new TreeSet<>();
        try (Stream<Path> paths = Files.walk(classesDirectory.toPath())) {
            List<Path> classFiles = paths.filter(p -> p.getFileName().toString().endsWith(".class"))
                    .collect(Collectors.toList());
            for (Path classFile : classFiles) {
                try (InputStream is = Files.newInputStream(classFile)) {
                    ClassReader reader = new ClassReader(is);
                    reader.accept(
                            new ClassVisitor(Opcodes.ASM9) {
                                private String internalName;

                                @Override
                                public void visit(
                                        int version,
                                        int access,
                                        String name,
                                        String signature,
                                        String superName,
                                        String[] interfaces) {
                                    super.visit(version, access, name, signature, superName, interfaces);
                                    internalName = name;
                                }

                                @Override
                                public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
                                    if ("Lorg/apache/maven/api/di/Named;".equals(descriptor)) {
                                        diBeans.add(internalName.replace('/', '.'));
                                    }
                                    return null;
                                }
                            },
                            ClassReader.SKIP_FRAMES | ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG);
                }
            }
        }

        File indexFile = new File(outputDirectory, "org.apache.maven.api.di.Inject");
        if (diBeans.isEmpty()) {
            Files.deleteIfExists(indexFile.toPath());
        } else {
            StringBuilder content = new StringBuilder();
            for (String bean : diBeans) {
                content.append(bean).append(System.lineSeparator());
            }
            Files.write(indexFile.toPath(), content.toString().getBytes(StandardCharsets.UTF_8));
        }
    }

    private static MavenProject newProject(Model model, File pomFile, Map<String, String> properties) {
        MavenProject project = new MavenProject(model);
        project.setFile(pomFile);
        if (project.getGroupId() == null && model.getParent() != null) {
            project.setGroupId(model.getParent().getGroupId());
        }
        if (project.getVersion() == null && model.getParent() != null) {
            project.setVersion(model.getParent().getVersion());
        }
        properties.put("project.groupId", project.getGroupId());
        properties.put("project.artifactId", project.getArtifactId());
        properties.put("project.version", project.getVersion());
        return project;
    }

    private static String determineGoalPrefix(Model model, MavenProject project, Map<String, String> properties) {
        String goalPrefix = interpolate(findGoalPrefix(model), properties);
        if (goalPrefix == null || goalPrefix.isEmpty()) {
            goalPrefix = AbstractGeneratorMojo.getDefaultGoalPrefix(project);
        }
        return (goalPrefix == null || goalPrefix.isEmpty()) ? "plugin" : goalPrefix;
    }

    private static void setField(Object target, String fieldName, Object value) throws Exception {
        java.lang.reflect.Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static PluginDescriptor buildPluginDescriptor(
            MavenProject project, Model model, Map<String, String> properties) {
        PluginDescriptor pluginDescriptor = new PluginDescriptor();
        pluginDescriptor.setGroupId(project.getGroupId());
        pluginDescriptor.setArtifactId(project.getArtifactId());
        pluginDescriptor.setVersion(project.getVersion());
        pluginDescriptor.setGoalPrefix(determineGoalPrefix(model, project, properties));
        pluginDescriptor.setName(project.getName());
        pluginDescriptor.setDescription(project.getDescription());
        pluginDescriptor.setDependencies(GeneratorUtils.toComponentDependencies(project.getArtifacts()));

        if (project.getPrerequisites() != null) {
            String requiredMavenVersion = interpolate(project.getPrerequisites().getMaven(), properties);
            if (requiredMavenVersion != null) {
                pluginDescriptor.setRequiredMavenVersion(requiredMavenVersion);
            }
        }
        String javaVersion = properties.get("javaVersion");
        if (javaVersion != null) {
            if ("8".equals(javaVersion)) {
                javaVersion = "1.8";
            }
            pluginDescriptor.setRequiredJavaVersion(javaVersion);
        }
        return pluginDescriptor;
    }

    private static Model readModel(File pomFile) throws Exception {
        MavenXpp3Reader pomReader = new MavenXpp3Reader();
        try (InputStream is = Files.newInputStream(pomFile.toPath())) {
            return pomReader.read(is);
        }
    }

    static void gatherPomChainInfo(
            File pomFile, Model model, Map<String, String> properties, Map<String, String> managedVersions) {
        walkParentChain(pomFile, model, m -> {
            putAllIfAbsent(properties, propertiesOf(m));
            addManagedVersions(managedVersions, m);
        });
    }

    private static void addManagedVersions(Map<String, String> managedVersions, Model model) {
        if (model.getDependencyManagement() == null) {
            return;
        }
        for (Dependency managed : model.getDependencyManagement().getDependencies()) {
            managedVersions.putIfAbsent(managed.getGroupId() + ":" + managed.getArtifactId(), managed.getVersion());
        }
    }

    private static void walkParentChain(File pomFile, Model model, java.util.function.Consumer<Model> visitor) {
        visitor.accept(model);

        Set<String> visitedPoms = new HashSet<>();
        visitedPoms.add(canonicalPath(pomFile));

        File currentPomFile = pomFile;
        Parent parent = model.getParent();
        while (parent != null) {
            File parentPomFile = resolveParentPomFile(currentPomFile, parent);
            if (parentPomFile == null || !visitedPoms.add(canonicalPath(parentPomFile))) {
                break;
            }
            Model parentModel;
            try {
                parentModel = readModel(parentPomFile);
            } catch (Exception e) {
                break;
            }
            visitor.accept(parentModel);
            currentPomFile = parentPomFile;
            parent = parentModel.getParent();
        }
    }

    private static String canonicalPath(File file) {
        try {
            return file.getCanonicalPath();
        } catch (java.io.IOException e) {
            return file.getAbsolutePath();
        }
    }

    private static Map<String, String> propertiesOf(Model model) {
        Map<String, String> result = new LinkedHashMap<>();
        if (model.getProperties() != null) {
            for (String name : model.getProperties().stringPropertyNames()) {
                result.put(name, model.getProperties().getProperty(name));
            }
        }
        return result;
    }

    private static void putAllIfAbsent(Map<String, String> target, Map<String, String> source) {
        for (Map.Entry<String, String> entry : source.entrySet()) {
            target.putIfAbsent(entry.getKey(), entry.getValue());
        }
    }

    private static File resolveParentPomFile(File childPomFile, Parent parent) {
        String relativePath = parent.getRelativePath();
        if (relativePath == null || relativePath.isEmpty()) {
            relativePath = "../pom.xml";
        }
        File parentDir = childPomFile.getAbsoluteFile().getParentFile();
        File candidate = new File(parentDir, relativePath);
        if (candidate.isDirectory()) {
            candidate = new File(candidate, "pom.xml");
        }
        return candidate.isFile() ? candidate : null;
    }

    static String interpolate(String value, Map<String, String> properties) {
        if (value == null || !value.contains("${")) {
            return value;
        }
        String result = value;
        for (int pass = 0; pass < 5 && result.contains("${"); pass++) {
            boolean changed = false;
            for (Map.Entry<String, String> entry : properties.entrySet()) {
                if (entry.getValue() == null) {
                    continue;
                }
                String placeholder = "${" + entry.getKey() + "}";
                if (result.contains(placeholder)) {
                    result = result.replace(placeholder, entry.getValue());
                    changed = true;
                }
            }
            if (!changed) {
                break;
            }
        }
        return result;
    }

    static String findGoalPrefix(Model model) {
        if (model.getBuild() == null) {
            return null;
        }
        String prefix = findGoalPrefixInPlugins(model.getBuild().getPlugins());
        if (prefix != null) {
            return prefix;
        }
        if (model.getBuild().getPluginManagement() != null) {
            return findGoalPrefixInPlugins(
                    model.getBuild().getPluginManagement().getPlugins());
        }
        return null;
    }

    private static String findGoalPrefixInPlugins(java.util.List<Plugin> plugins) {
        if (plugins == null) {
            return null;
        }
        for (Plugin plugin : plugins) {
            if ("maven-plugin-plugin".equals(plugin.getArtifactId())) {
                Object configuration = plugin.getConfiguration();
                if (configuration instanceof Xpp3Dom) {
                    Xpp3Dom dom = (Xpp3Dom) configuration;
                    Xpp3Dom goalPrefixNode = dom.getChild("goalPrefix");
                    if (goalPrefixNode != null) {
                        return goalPrefixNode.getValue();
                    }
                }
            }
        }
        return null;
    }

    private static Set<org.apache.maven.artifact.Artifact> populateDependencies(
            Model model, MavenProject project, Map<String, String> properties, Map<String, String> managedVersions) {
        Set<org.apache.maven.artifact.Artifact> artifacts = new HashSet<>();
        for (Dependency dep : model.getDependencies()) {
            String scope = dep.getScope() != null ? dep.getScope() : "compile";
            if ("test".equals(scope) || "provided".equals(scope)) {
                continue;
            }
            String depGroupId = interpolate(dep.getGroupId(), properties);
            String depVersion = interpolate(dep.getVersion(), properties);
            if (depVersion == null) {
                depVersion = interpolate(managedVersions.get(depGroupId + ":" + dep.getArtifactId()), properties);
            }
            if (depVersion == null) {
                depVersion = project.getVersion();
            }
            String depType = dep.getType() != null ? dep.getType() : "jar";
            org.apache.maven.artifact.handler.ArtifactHandler depHandler =
                    new org.apache.maven.artifact.handler.DefaultArtifactHandler(depType);
            org.apache.maven.artifact.Artifact art = new org.apache.maven.artifact.DefaultArtifact(
                    depGroupId, dep.getArtifactId(), depVersion, scope, depType, dep.getClassifier(), depHandler);
            art.setFile(new File("dummy.jar"));
            artifacts.add(art);
        }
        return artifacts;
    }

    static Set<org.apache.maven.artifact.Artifact> buildScanArtifacts(
            Set<org.apache.maven.artifact.Artifact> declaredArtifacts) {
        Set<org.apache.maven.artifact.Artifact> scanArtifacts = new HashSet<>(declaredArtifacts);
        String classpath = System.getProperty("java.class.path", "");
        int index = 0;
        for (String entry : classpath.split(File.pathSeparator)) {
            if (entry.isEmpty()) {
                continue;
            }
            File file = new File(entry);
            if (!file.exists()) {
                continue;
            }
            index++;
            org.apache.maven.artifact.handler.ArtifactHandler handler =
                    new org.apache.maven.artifact.handler.DefaultArtifactHandler("jar");
            org.apache.maven.artifact.Artifact syntheticArt = new org.apache.maven.artifact.DefaultArtifact(
                    "classpath-scan-" + index, file.getName(), "0", "compile", "jar", "", handler);
            syntheticArt.setFile(file);
            scanArtifacts.add(syntheticArt);
        }
        return scanArtifacts;
    }

    private static org.codehaus.plexus.archiver.manager.ArchiverManager createArchiverManager() {
        return new org.codehaus.plexus.archiver.manager.ArchiverManager() {
            @Override
            public org.codehaus.plexus.archiver.Archiver getArchiver(String name)
                    throws org.codehaus.plexus.archiver.manager.NoSuchArchiverException {
                if ("jar".equalsIgnoreCase(name)) {
                    return new org.codehaus.plexus.archiver.jar.JarArchiver();
                }
                throw new org.codehaus.plexus.archiver.manager.NoSuchArchiverException(name);
            }

            @Override
            public org.codehaus.plexus.archiver.Archiver getArchiver(File file)
                    throws org.codehaus.plexus.archiver.manager.NoSuchArchiverException {
                return getArchiver("jar");
            }

            @Override
            public java.util.Collection<String> getAvailableArchivers() {
                return Collections.emptyList();
            }

            @Override
            public org.codehaus.plexus.archiver.UnArchiver getUnArchiver(String name)
                    throws org.codehaus.plexus.archiver.manager.NoSuchArchiverException {
                if ("jar".equalsIgnoreCase(name)) {
                    return new org.codehaus.plexus.archiver.zip.ZipUnArchiver();
                }
                throw new org.codehaus.plexus.archiver.manager.NoSuchArchiverException(name);
            }

            @Override
            public org.codehaus.plexus.archiver.UnArchiver getUnArchiver(File file)
                    throws org.codehaus.plexus.archiver.manager.NoSuchArchiverException {
                return getUnArchiver("jar");
            }

            @Override
            public java.util.Collection<String> getAvailableUnArchivers() {
                return Collections.emptyList();
            }

            @Override
            public org.codehaus.plexus.components.io.resources.PlexusIoResourceCollection getResourceCollection(
                    File file) {
                return null;
            }

            @Override
            public org.codehaus.plexus.components.io.resources.PlexusIoResourceCollection getResourceCollection(
                    String name) {
                return null;
            }

            @Override
            public java.util.Collection<String> getAvailableResourceCollections() {
                return Collections.emptyList();
            }
        };
    }

    private static DefaultRepositorySystemSession createRepositorySession(RepositorySystem repoSystem)
            throws Exception {
        DefaultRepositorySystemSession repoSession =
                org.apache.maven.repository.internal.MavenRepositorySystemUtils.newSession();
        File localRepoFile = Files.createTempDirectory("standalone-repo").toFile();
        localRepoFile.deleteOnExit();
        LocalRepository localRepo = new LocalRepository(localRepoFile);
        LocalRepositoryManager lrm = repoSystem.newLocalRepositoryManager(repoSession, localRepo);
        repoSession.setLocalRepositoryManager(lrm);
        return repoSession;
    }

    private static RepositorySystem createMinimalRepositorySystem() {
        return (RepositorySystem) java.lang.reflect.Proxy.newProxyInstance(
                RepositorySystem.class.getClassLoader(),
                new Class<?>[] {RepositorySystem.class},
                (proxy, method, args) -> {
                    switch (method.getName()) {
                        case "newLocalRepositoryManager":
                            return createNoopLocalRepositoryManager((LocalRepository) args[1]);
                        case "resolveArtifact":
                            throw new org.eclipse.aether.resolution.ArtifactResolutionException(
                                    Collections.singletonList(new ArtifactResult((ArtifactRequest) args[1])));
                        case "shutdown":
                        case "addOnSystemEndedHandler":
                            return null;
                        default:
                            throw new UnsupportedOperationException(method.getName());
                    }
                });
    }

    private static LocalRepositoryManager createNoopLocalRepositoryManager(LocalRepository localRepo) {
        return new LocalRepositoryManager() {
            @Override
            public LocalRepository getRepository() {
                return localRepo;
            }

            @Override
            public String getPathForLocalArtifact(org.eclipse.aether.artifact.Artifact artifact) {
                return pathFor(artifact);
            }

            @Override
            public String getPathForRemoteArtifact(
                    org.eclipse.aether.artifact.Artifact artifact,
                    org.eclipse.aether.repository.RemoteRepository repository,
                    String context) {
                return pathFor(artifact);
            }

            @Override
            public String getPathForLocalMetadata(org.eclipse.aether.metadata.Metadata metadata) {
                return metadata.getGroupId() + "/" + metadata.getArtifactId() + "/" + metadata.getVersion();
            }

            @Override
            public String getPathForRemoteMetadata(
                    org.eclipse.aether.metadata.Metadata metadata,
                    org.eclipse.aether.repository.RemoteRepository repository,
                    String context) {
                return getPathForLocalMetadata(metadata);
            }

            @Override
            public org.eclipse.aether.repository.LocalArtifactResult find(
                    org.eclipse.aether.RepositorySystemSession session,
                    org.eclipse.aether.repository.LocalArtifactRequest request) {
                return new org.eclipse.aether.repository.LocalArtifactResult(request);
            }

            @Override
            public void add(
                    org.eclipse.aether.RepositorySystemSession session,
                    org.eclipse.aether.repository.LocalArtifactRegistration request) {}

            @Override
            public org.eclipse.aether.repository.LocalMetadataResult find(
                    org.eclipse.aether.RepositorySystemSession session,
                    org.eclipse.aether.repository.LocalMetadataRequest request) {
                return new org.eclipse.aether.repository.LocalMetadataResult(request);
            }

            @Override
            public void add(
                    org.eclipse.aether.RepositorySystemSession session,
                    org.eclipse.aether.repository.LocalMetadataRegistration request) {}

            private String pathFor(org.eclipse.aether.artifact.Artifact artifact) {
                String classifier = artifact.getClassifier();
                return artifact.getGroupId().replace('.', '/') + "/" + artifact.getArtifactId() + "/"
                        + artifact.getVersion() + "/" + artifact.getArtifactId() + "-" + artifact.getVersion()
                        + (classifier == null || classifier.isEmpty() ? "" : "-" + classifier) + "."
                        + artifact.getExtension();
            }
        };
    }
}
