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
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.artifact.handler.DefaultArtifactHandler;
import org.apache.maven.model.Build;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginExecution;
import org.apache.maven.model.Repository;
import org.apache.maven.model.building.DefaultModelBuilderFactory;
import org.apache.maven.model.building.DefaultModelBuildingRequest;
import org.apache.maven.model.building.FileModelSource;
import org.apache.maven.model.building.ModelBuildingRequest;
import org.apache.maven.model.building.ModelSource;
import org.apache.maven.model.resolution.ModelResolver;
import org.apache.maven.model.resolution.UnresolvableModelException;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.project.MavenProject;
import org.apache.maven.tools.plugin.DefaultPluginToolsRequest;
import org.apache.maven.tools.plugin.ExtendedMojoDescriptor;
import org.apache.maven.tools.plugin.generator.GeneratorUtils;
import org.apache.maven.tools.plugin.generator.PluginDescriptorFilesGenerator;
import org.apache.maven.tools.plugin.scanner.MojoScanner;
import org.codehaus.plexus.ContainerConfiguration;
import org.codehaus.plexus.DefaultContainerConfiguration;
import org.codehaus.plexus.DefaultPlexusContainer;
import org.codehaus.plexus.PlexusConstants;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.resolution.ArtifactResolutionException;

/**
 * Minimal standalone runner to generate the bootstrap plugin descriptor.
 */
public class StandaloneDescriptorGenerator {

    public static void main(String[] args) {
        try {
            File pomFile =
                    (args.length == 0) ? new File("pom.xml").getAbsoluteFile() : new File(args[0]).getAbsoluteFile();
            File[] extraSourceRoots = new File[Math.max(0, args.length - 1)];
            for (int i = 1; i < args.length; i++) {
                extraSourceRoots[i - 1] = new File(args[i]).getAbsoluteFile();
            }
            run(pomFile, extraSourceRoots);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    public static void run(File pomFile, File... extraSourceRoots) throws Exception {
        Model model = buildEffectiveModel(pomFile);
        File baseDir = pomFile.getParentFile();

        MavenProject project = newProject(model, pomFile);

        List<File> sourceRoots = findCompileSourceRoots(model, baseDir);
        if (extraSourceRoots != null) {
            Collections.addAll(sourceRoots, extraSourceRoots);
        }

        String outPath = (model.getBuild() != null && model.getBuild().getOutputDirectory() != null)
                ? model.getBuild().getOutputDirectory()
                : "target/classes";
        File outDir = new File(outPath);
        File classesDirectory = (outDir.isAbsolute() ? outDir : new File(baseDir, outPath)).getAbsoluteFile();
        File outputDirectory = new File(classesDirectory, "META-INF/maven").getAbsoluteFile();

        ArtifactHandler projectHandler = new DefaultArtifactHandler("maven-plugin");
        Artifact projectArtifact = new DefaultArtifact(
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
        for (File sourceRoot : sourceRoots) {
            if (sourceRoot.isDirectory()) {
                project.addCompileSourceRoot(sourceRoot.getAbsolutePath());
            }
        }

        project.setArtifacts(populateDependencies(model));

        ContainerConfiguration containerConfiguration = new DefaultContainerConfiguration()
                .setClassPathScanning(PlexusConstants.SCANNING_INDEX)
                .setAutoWiring(true);
        PlexusContainer container = new DefaultPlexusContainer(containerConfiguration);
        try {
            container.addComponent(createMinimalRepositorySystem(), RepositorySystem.class, "");
            MojoScanner mojoScanner = container.lookup(MojoScanner.class);

            PluginDescriptor pluginDescriptor = buildPluginDescriptor(project, model);

            DefaultPluginToolsRequest request = new DefaultPluginToolsRequest(project, pluginDescriptor);
            request.setEncoding("UTF-8");
            request.setSkipErrorNoDescriptorsFound(true);
            request.setDependencies(buildScanArtifacts());

            mojoScanner.populatePluginDescriptor(request);

            outputDirectory.mkdirs();
            for (MojoDescriptor md : pluginDescriptor.getMojos()) {
                if (md instanceof ExtendedMojoDescriptor && ((ExtendedMojoDescriptor) md).isV4Api()) {
                    generateV4Factory(md, classesDirectory);
                }
            }
            DescriptorGeneratorMojo.generateV4DiIndex(classesDirectory, outputDirectory);

            PluginDescriptorFilesGenerator generator = new PluginDescriptorFilesGenerator();
            generator.execute(outputDirectory, request);

            File generatedFile = new File(outputDirectory, "plugin.xml");
            if (!generatedFile.exists()) {
                throw new IllegalStateException("Descriptor was not generated: " + generatedFile.getAbsolutePath());
            }
        } finally {
            container.dispose();
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

    private static MavenProject newProject(Model model, File pomFile) {
        MavenProject project = new MavenProject(model);
        project.setFile(pomFile);
        return project;
    }

    private static String determineGoalPrefix(Model model, MavenProject project) {
        String goalPrefix = findGoalPrefix(model);
        if (goalPrefix == null || goalPrefix.isEmpty()) {
            goalPrefix = AbstractGeneratorMojo.getDefaultGoalPrefix(project);
        }
        return (goalPrefix == null || goalPrefix.isEmpty()) ? "plugin" : goalPrefix;
    }

    private static PluginDescriptor buildPluginDescriptor(MavenProject project, Model model) {
        PluginDescriptor pluginDescriptor = new PluginDescriptor();
        pluginDescriptor.setGroupId(project.getGroupId());
        pluginDescriptor.setArtifactId(project.getArtifactId());
        pluginDescriptor.setVersion(project.getVersion());
        pluginDescriptor.setGoalPrefix(determineGoalPrefix(model, project));
        pluginDescriptor.setName(project.getName());
        pluginDescriptor.setDescription(project.getDescription());
        pluginDescriptor.setDependencies(GeneratorUtils.toComponentDependencies(project.getArtifacts()));

        if (project.getPrerequisites() != null) {
            String requiredMavenVersion = project.getPrerequisites().getMaven();
            if (requiredMavenVersion != null) {
                pluginDescriptor.setRequiredMavenVersion(requiredMavenVersion);
            }
        }
        String javaVersion = model.getProperties().getProperty("javaVersion");
        if (javaVersion != null) {
            if ("8".equals(javaVersion)) {
                javaVersion = "1.8";
            }
            pluginDescriptor.setRequiredJavaVersion(javaVersion);
        }
        return pluginDescriptor;
    }

    /** Builds the fully inherited, interpolated model, the same way Maven itself would. */
    static Model buildEffectiveModel(File pomFile) throws Exception {
        DefaultModelBuildingRequest request = new DefaultModelBuildingRequest();
        request.setPomFile(pomFile);
        request.setValidationLevel(ModelBuildingRequest.VALIDATION_LEVEL_MINIMAL);
        request.setProcessPlugins(false);
        request.setSystemProperties(System.getProperties());
        request.setModelResolver(new LocalRepositoryModelResolver(localRepository()));
        return new DefaultModelBuilderFactory().newInstance().build(request).getEffectiveModel();
    }

    private static File localRepository() {
        String path = System.getProperty("maven.repo.local");
        return path != null ? new File(path) : new File(System.getProperty("user.home"), ".m2/repository");
    }

    /** Resolves parent/imported POMs already present in the local repository; no downloading. */
    private static final class LocalRepositoryModelResolver implements ModelResolver {
        private final File localRepository;

        LocalRepositoryModelResolver(File localRepository) {
            this.localRepository = localRepository;
        }

        @Override
        public ModelSource resolveModel(String groupId, String artifactId, String version)
                throws UnresolvableModelException {
            File pom = new File(
                    localRepository,
                    groupId.replace('.', '/') + '/' + artifactId + '/' + version + '/' + artifactId + '-' + version
                            + ".pom");
            if (!pom.isFile()) {
                throw new UnresolvableModelException("not found in " + localRepository, groupId, artifactId, version);
            }
            return new FileModelSource(pom);
        }

        @Override
        public ModelSource resolveModel(Parent parent) throws UnresolvableModelException {
            return resolveModel(parent.getGroupId(), parent.getArtifactId(), parent.getVersion());
        }

        @Override
        public ModelSource resolveModel(Dependency dependency) throws UnresolvableModelException {
            return resolveModel(dependency.getGroupId(), dependency.getArtifactId(), dependency.getVersion());
        }

        @Override
        public void addRepository(Repository repository) {}

        @Override
        public void addRepository(Repository repository, boolean replace) {}

        @Override
        public ModelResolver newCopy() {
            return this;
        }
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

    private static String findGoalPrefixInPlugins(List<Plugin> plugins) {
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

    static List<File> findCompileSourceRoots(Model model, File baseDir) {
        Set<File> roots = new LinkedHashSet<>();
        String srcPath = (model.getBuild() != null && model.getBuild().getSourceDirectory() != null)
                ? model.getBuild().getSourceDirectory()
                : "src/main/java";
        File defaultSrc = new File(srcPath);
        roots.add((defaultSrc.isAbsolute() ? defaultSrc : new File(baseDir, srcPath)).getAbsoluteFile());

        if (model.getBuild() != null && model.getBuild().getPlugins() != null) {
            for (Plugin plugin : model.getBuild().getPlugins()) {
                if ("maven-compiler-plugin".equals(plugin.getArtifactId())) {
                    collectCompileSourceRoots(plugin.getConfiguration(), baseDir, roots);
                    for (PluginExecution exec : plugin.getExecutions()) {
                        collectCompileSourceRoots(exec.getConfiguration(), baseDir, roots);
                    }
                }
            }
        }
        return new ArrayList<>(roots);
    }

    private static void collectCompileSourceRoots(Object config, File baseDir, Set<File> roots) {
        if (config instanceof Xpp3Dom) {
            Xpp3Dom compileSourceRoots = ((Xpp3Dom) config).getChild("compileSourceRoots");
            if (compileSourceRoots != null) {
                for (Xpp3Dom child : compileSourceRoots.getChildren("compileSourceRoot")) {
                    String val = child.getValue();
                    if (val != null && !val.trim().isEmpty()) {
                        String s = val.trim();
                        File f = new File(s);
                        roots.add((f.isAbsolute() ? f : new File(baseDir, s)).getAbsoluteFile());
                    }
                }
            }
        }
    }

    private static Set<Artifact> populateDependencies(Model model) {
        Set<Artifact> artifacts = new HashSet<>();
        for (Dependency dep : model.getDependencies()) {
            String scope = dep.getScope() != null ? dep.getScope() : "compile";
            if ("test".equals(scope) || "provided".equals(scope)) {
                continue;
            }
            String depType = dep.getType() != null ? dep.getType() : "jar";
            ArtifactHandler depHandler = new DefaultArtifactHandler(depType);
            Artifact art = new DefaultArtifact(
                    dep.getGroupId(),
                    dep.getArtifactId(),
                    dep.getVersion(),
                    scope,
                    depType,
                    dep.getClassifier(),
                    depHandler);
            artifacts.add(art);
        }
        return artifacts;
    }

    static Set<Artifact> buildScanArtifacts() {
        Set<Artifact> scanArtifacts = new HashSet<>();
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
            ArtifactHandler handler = new DefaultArtifactHandler("jar");
            Artifact syntheticArt =
                    new DefaultArtifact("classpath-scan-" + index, file.getName(), "0", "compile", "jar", "", handler);
            syntheticArt.setFile(file);
            scanArtifacts.add(syntheticArt);
        }
        return scanArtifacts;
    }

    private static RepositorySystem createMinimalRepositorySystem() {
        return (RepositorySystem) Proxy.newProxyInstance(
                RepositorySystem.class.getClassLoader(),
                new Class<?>[] {RepositorySystem.class},
                (proxy, method, args) -> {
                    if ("resolveArtifact".equals(method.getName())) {
                        throw new ArtifactResolutionException(Collections.emptyList());
                    }
                    return null;
                });
    }
}
