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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.nodeTypes.NodeWithJavadoc;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.javadoc.Javadoc;
import com.github.javaparser.javadoc.JavadocBlockTag;
import com.github.javaparser.resolution.UnsolvedSymbolException;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.versioning.ComparableVersion;
import org.apache.maven.plugin.descriptor.InvalidParameterException;
import org.apache.maven.plugin.descriptor.InvalidPluginDescriptorException;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugin.descriptor.Requirement;
import org.apache.maven.project.MavenProject;
import org.apache.maven.tools.plugin.ExtendedMojoDescriptor;
import org.apache.maven.tools.plugin.PluginToolsRequest;
import org.apache.maven.tools.plugin.extractor.ExtractionException;
import org.apache.maven.tools.plugin.extractor.GroupKey;
import org.apache.maven.tools.plugin.extractor.MojoDescriptorExtractor;
import org.apache.maven.tools.plugin.extractor.annotations.converter.ConverterContext;
import org.apache.maven.tools.plugin.extractor.annotations.converter.JavaClassConverterContext;
import org.apache.maven.tools.plugin.extractor.annotations.converter.JavadocBlockTagsToXhtmlConverter;
import org.apache.maven.tools.plugin.extractor.annotations.converter.JavadocInlineTagsToXhtmlConverter;
import org.apache.maven.tools.plugin.extractor.annotations.datamodel.AfterAnnotationContent;
import org.apache.maven.tools.plugin.extractor.annotations.datamodel.ComponentAnnotationContent;
import org.apache.maven.tools.plugin.extractor.annotations.datamodel.ExecuteAnnotationContent;
import org.apache.maven.tools.plugin.extractor.annotations.datamodel.MojoAnnotationContent;
import org.apache.maven.tools.plugin.extractor.annotations.datamodel.ParameterAnnotationContent;
import org.apache.maven.tools.plugin.extractor.annotations.scanner.MojoAnnotatedClass;
import org.apache.maven.tools.plugin.extractor.annotations.scanner.MojoAnnotationsScanner;
import org.apache.maven.tools.plugin.extractor.annotations.scanner.MojoAnnotationsScannerRequest;
import org.apache.maven.tools.plugin.javadoc.JavadocLinkGenerator;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.UnArchiver;
import org.codehaus.plexus.archiver.manager.ArchiverManager;
import org.codehaus.plexus.archiver.manager.NoSuchArchiverException;
import org.codehaus.plexus.util.StringUtils;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.ArtifactResult;
import org.objectweb.asm.Opcodes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JavaMojoDescriptorExtractor, a MojoDescriptor extractor to read descriptors from java classes with annotations.
 * Notice that source files are also parsed to get description, since and deprecation information.
 *
 * @author Olivier Lamy
 * @since 3.0
 */
@Named(JavaAnnotationsMojoDescriptorExtractor.NAME)
@Singleton
public class JavaAnnotationsMojoDescriptorExtractor implements MojoDescriptorExtractor {
    private static final Logger LOGGER = LoggerFactory.getLogger(JavaAnnotationsMojoDescriptorExtractor.class);
    public static final String NAME = "java-annotations";

    private static final GroupKey GROUP_KEY = new GroupKey(GroupKey.JAVA_GROUP, 100);

    /**
     *
     * @see <a href="https://docs.oracle.com/javase/specs/jvms/se19/html/jvms-4.html#jvms-4.1">JVMS 4.1</a>
     */
    private static final Map<Integer, String> CLASS_VERSION_TO_JAVA_STRING;

    static {
        CLASS_VERSION_TO_JAVA_STRING = new HashMap<>();
        CLASS_VERSION_TO_JAVA_STRING.put(Opcodes.V1_1, "1.1");
        CLASS_VERSION_TO_JAVA_STRING.put(Opcodes.V1_2, "1.2");
        CLASS_VERSION_TO_JAVA_STRING.put(Opcodes.V1_3, "1.3");
        CLASS_VERSION_TO_JAVA_STRING.put(Opcodes.V1_4, "1.4");
        CLASS_VERSION_TO_JAVA_STRING.put(Opcodes.V1_5, "1.5");
        CLASS_VERSION_TO_JAVA_STRING.put(Opcodes.V1_6, "1.6");
        CLASS_VERSION_TO_JAVA_STRING.put(Opcodes.V1_7, "1.7");
        CLASS_VERSION_TO_JAVA_STRING.put(Opcodes.V1_8, "1.8");
        CLASS_VERSION_TO_JAVA_STRING.put(Opcodes.V9, "9");
        CLASS_VERSION_TO_JAVA_STRING.put(Opcodes.V10, "10");
        CLASS_VERSION_TO_JAVA_STRING.put(Opcodes.V11, "11");
        CLASS_VERSION_TO_JAVA_STRING.put(Opcodes.V12, "12");
        CLASS_VERSION_TO_JAVA_STRING.put(Opcodes.V13, "13");
        CLASS_VERSION_TO_JAVA_STRING.put(Opcodes.V14, "14");
        CLASS_VERSION_TO_JAVA_STRING.put(Opcodes.V15, "15");
        CLASS_VERSION_TO_JAVA_STRING.put(Opcodes.V16, "16");
        CLASS_VERSION_TO_JAVA_STRING.put(Opcodes.V17, "17");
        CLASS_VERSION_TO_JAVA_STRING.put(Opcodes.V18, "18");
        CLASS_VERSION_TO_JAVA_STRING.put(Opcodes.V19, "19");
        CLASS_VERSION_TO_JAVA_STRING.put(Opcodes.V20, "20");
        CLASS_VERSION_TO_JAVA_STRING.put(Opcodes.V21, "21");
        CLASS_VERSION_TO_JAVA_STRING.put(Opcodes.V22, "22");
        CLASS_VERSION_TO_JAVA_STRING.put(Opcodes.V23, "23");
        CLASS_VERSION_TO_JAVA_STRING.put(Opcodes.V24, "24");
        CLASS_VERSION_TO_JAVA_STRING.put(Opcodes.V25, "25");
        CLASS_VERSION_TO_JAVA_STRING.put(Opcodes.V26, "26");
    }

    @Inject
    MojoAnnotationsScanner mojoAnnotationsScanner;

    @Inject
    private RepositorySystem repositorySystem;

    @Inject
    private ArchiverManager archiverManager;

    @Inject
    private JavadocInlineTagsToXhtmlConverter javadocInlineTagsToHtmlConverter;

    @Inject
    private JavadocBlockTagsToXhtmlConverter javadocBlockTagsToHtmlConverter;

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public boolean isDeprecated() {
        return false; // this is the "current way" to write Java Mojos
    }

    @Override
    public GroupKey getGroupKey() {
        return GROUP_KEY;
    }

    /**
     * Compares class file format versions.
     * @see <a href="https://docs.oracle.com/javase/specs/jvms/se19/html/jvms-4.html#jvms-4.1">JVMS 4.1</a>
     *
     */
    @SuppressWarnings("checkstyle:magicnumber")
    static final class ClassVersionComparator implements Comparator<Integer> {
        @Override
        public int compare(Integer classVersion1, Integer classVersion2) {
            // first compare major version (
            int result = Integer.compare(classVersion1 & 0x00FF, classVersion2 & 0x00FF);
            if (result == 0) {
                // compare minor version if major is equal
                result = Integer.compare(classVersion1, classVersion2);
            }
            return result;
        }
    }

    @Override
    public List<MojoDescriptor> execute(PluginToolsRequest request)
            throws ExtractionException, InvalidPluginDescriptorException {
        Map<String, MojoAnnotatedClass> mojoAnnotatedClasses = scanAnnotations(request);

        Optional<Integer> maxClassVersion = mojoAnnotatedClasses.values().stream()
                .map(MojoAnnotatedClass::getClassVersion)
                .max(new ClassVersionComparator());
        if (maxClassVersion.isPresent()) {
            String requiredJavaVersion = CLASS_VERSION_TO_JAVA_STRING.get(maxClassVersion.get());
            if (StringUtils.isBlank(request.getRequiredJavaVersion())
                    || new ComparableVersion(request.getRequiredJavaVersion())
                                    .compareTo(new ComparableVersion(requiredJavaVersion))
                            < 0) {
                request.setRequiredJavaVersion(requiredJavaVersion);
            }
        }
        final JavadocLinkGenerator linkGenerator;
        if (request.getInternalJavadocBaseUrl() != null
                || (request.getExternalJavadocBaseUrls() != null
                        && !request.getExternalJavadocBaseUrls().isEmpty())) {
            linkGenerator = new JavadocLinkGenerator(
                    request.getInternalJavadocBaseUrl(),
                    request.getInternalJavadocVersion(),
                    request.getExternalJavadocBaseUrls(),
                    request.getSettings());
        } else {
            linkGenerator = null;
        }

        // parse() allocates the class loader backing the model, so it must run inside the
        // try-with-resources: a parse failure would otherwise leak it together with every
        // dependency and reactor jar it holds open.
        try (JavaSourceModel sourceModel = scanJavadoc(request, mojoAnnotatedClasses.values())) {
            sourceModel.parse();
            Map<String, TypeDeclaration<?>> javaClassesMap = discoverClasses(sourceModel);
            populateDataFromJavadoc(sourceModel, mojoAnnotatedClasses, javaClassesMap, linkGenerator);
        } catch (IOException e) {
            throw new ExtractionException("Could not parse Java sources: " + e.getMessage(), e);
        }

        return toMojoDescriptors(mojoAnnotatedClasses, request.getPluginDescriptor());
    }

    private Map<String, MojoAnnotatedClass> scanAnnotations(PluginToolsRequest request) throws ExtractionException {
        MojoAnnotationsScannerRequest mojoAnnotationsScannerRequest = new MojoAnnotationsScannerRequest();

        File output = new File(request.getProject().getBuild().getOutputDirectory());
        mojoAnnotationsScannerRequest.setClassesDirectories(Arrays.asList(output));

        mojoAnnotationsScannerRequest.setDependencies(request.getDependencies());

        mojoAnnotationsScannerRequest.setProject(request.getProject());

        Map<String, MojoAnnotatedClass> result = mojoAnnotationsScanner.scan(mojoAnnotationsScannerRequest);
        request.setUsedMavenApiVersion(mojoAnnotationsScannerRequest.getMavenApiVersion());
        return result;
    }

    private JavaSourceModel scanJavadoc(PluginToolsRequest request, Collection<MojoAnnotatedClass> mojoAnnotatedClasses)
            throws ExtractionException, IOException {
        // found artifact from reactors to scan sources
        // we currently only scan sources from reactors
        List<MavenProject> mavenProjects = new ArrayList<>();

        // if we need to scan sources from external artifacts
        Set<Artifact> externalArtifacts = new HashSet<>();

        Charset encoding =
                request.getEncoding() == null ? StandardCharsets.UTF_8 : Charset.forName(request.getEncoding());
        JavaSourceModel sourceModel = new JavaSourceModel(encoding);
        extendJavaSourceModel(request, sourceModel, request.getProject());

        for (MojoAnnotatedClass mojoAnnotatedClass : mojoAnnotatedClasses) {
            if (Objects.equals(
                    mojoAnnotatedClass.getArtifact().getArtifactId(),
                    request.getProject().getArtifact().getArtifactId())) {
                continue;
            }

            if (!isMojoAnnnotatedClassCandidate(mojoAnnotatedClass)) {
                // we don't scan sources for classes without mojo annotations
                continue;
            }

            MavenProject mavenProject =
                    getFromProjectReferences(mojoAnnotatedClass.getArtifact(), request.getProject());

            if (mavenProject != null) {
                mavenProjects.add(mavenProject);
            } else {
                externalArtifacts.add(mojoAnnotatedClass.getArtifact());
            }
        }

        // try to get artifact with sources classifier, extract somewhere then scan for @since, @deprecated
        for (Artifact artifact : externalArtifacts) {
            // parameter for test-sources too ?? olamy I need that for it test only
            if (StringUtils.equalsIgnoreCase("tests", artifact.getClassifier())) {
                extendJavaSourceModelWithSourcesJar(sourceModel, artifact, request, "test-sources");
            } else {
                extendJavaSourceModelWithSourcesJar(sourceModel, artifact, request, "sources");
            }
        }

        for (MavenProject mavenProject : mavenProjects) {
            extendJavaSourceModel(request, sourceModel, mavenProject);
        }

        return sourceModel;
    }

    private boolean isMojoAnnnotatedClassCandidate(MojoAnnotatedClass mojoAnnotatedClass) {
        return mojoAnnotatedClass != null && mojoAnnotatedClass.hasAnnotations();
    }

    /**
     * from sources scan to get @since and @deprecated and description of classes and fields.
     */
    protected void populateDataFromJavadoc(
            JavaSourceModel sourceModel,
            Map<String, MojoAnnotatedClass> mojoAnnotatedClasses,
            Map<String, TypeDeclaration<?>> javaClassesMap,
            JavadocLinkGenerator linkGenerator) {
        for (Map.Entry<String, MojoAnnotatedClass> entry : mojoAnnotatedClasses.entrySet()) {
            TypeDeclaration<?> javaClass = javaClassesMap.get(entry.getKey());
            if (javaClass == null) {
                continue;
            }
            MojoAnnotationContent mojo = entry.getValue().getMojo();
            if (mojo != null) {
                JavaClassConverterContext context = new JavaClassConverterContext(
                        javaClass, sourceModel, mojoAnnotatedClasses, linkGenerator, lineNumber(javaClass));
                mojo.setDescription(getDescriptionFromElement(javaClass, context));
                findInClassHierarchy(sourceModel, javaClass, "since")
                        .ifPresent(tag -> mojo.setSince(getRawValueFromTaglet(tag, context)));
                findInClassHierarchy(sourceModel, javaClass, "deprecated")
                        .ifPresent(tag -> mojo.setDeprecated(getRawValueFromTaglet(tag, context)));
            }

            Map<String, SourceMember> fields = extractFields(sourceModel, javaClass);
            Map<String, SourceMember> methods = extractMethods(sourceModel, javaClass);
            Map<String, ParameterAnnotationContent> parameters =
                    new TreeMap<>(getParametersParentHierarchy(entry.getValue(), mojoAnnotatedClasses));
            for (Map.Entry<String, ParameterAnnotationContent> parameter : parameters.entrySet()) {
                SourceMember member = parameter.getValue().isAnnotationOnMethod()
                        ? methods.get(parameter.getKey())
                        : fields.get(parameter.getKey());
                if (member != null) {
                    populateMemberJavadoc(
                            javaClass, member, parameter.getValue(), sourceModel, mojoAnnotatedClasses, linkGenerator);
                }
            }

            for (Map.Entry<String, ComponentAnnotationContent> component :
                    entry.getValue().getComponents().entrySet()) {
                SourceMember member = fields.get(component.getKey());
                if (member != null) {
                    populateMemberJavadoc(
                            javaClass, member, component.getValue(), sourceModel, mojoAnnotatedClasses, linkGenerator);
                }
            }
        }
    }

    private void populateMemberJavadoc(
            TypeDeclaration<?> mojoClass,
            SourceMember member,
            Object annotation,
            JavaSourceModel sourceModel,
            Map<String, MojoAnnotatedClass> mojoAnnotatedClasses,
            JavadocLinkGenerator linkGenerator) {
        JavaClassConverterContext context = new JavaClassConverterContext(
                mojoClass,
                member.declaringClass,
                member.element,
                sourceModel,
                mojoAnnotatedClasses,
                linkGenerator,
                lineNumber(member.element));
        String description = getDescriptionFromElement(member.javadocElement, context);
        Optional<JavadocBlockTag> deprecated = getTag(member.javadocElement, "deprecated");
        Optional<JavadocBlockTag> since = getTag(member.javadocElement, "since");
        if (annotation instanceof ParameterAnnotationContent) {
            ParameterAnnotationContent parameter = (ParameterAnnotationContent) annotation;
            parameter.setDescription(description);
            deprecated.ifPresent(tag -> parameter.setDeprecated(getRawValueFromTaglet(tag, context)));
            since.ifPresent(tag -> parameter.setSince(getRawValueFromTaglet(tag, context)));
        } else {
            ComponentAnnotationContent component = (ComponentAnnotationContent) annotation;
            component.setDescription(description);
            deprecated.ifPresent(tag -> component.setDeprecated(getRawValueFromTaglet(tag, context)));
            since.ifPresent(tag -> component.setSince(getRawValueFromTaglet(tag, context)));
        }
    }

    String getDescriptionFromElement(NodeWithJavadoc<?> element, JavaClassConverterContext context) {
        Optional<Javadoc> javadoc = element.getJavadoc();
        if (!javadoc.isPresent()) {
            return null;
        }
        StringBuilder description = new StringBuilder(javadocInlineTagsToHtmlConverter.convert(
                javadoc.get().getDescription().toText(), context));
        javadoc.get().getBlockTags().stream()
                .filter(tag -> "see".equals(tag.getTagName()))
                .forEach(tag -> description.append(javadocBlockTagsToHtmlConverter.convert(
                        tag.getTagName(), tag.getContent().toText(), context)));
        return description.toString();
    }

    String getRawValueFromTaglet(JavadocBlockTag tag, ConverterContext context) {
        return javadocInlineTagsToHtmlConverter.convert(tag.getContent().toText(), context);
    }

    private Optional<JavadocBlockTag> findInClassHierarchy(
            JavaSourceModel sourceModel, TypeDeclaration<?> javaClass, String tagName) {
        Optional<JavadocBlockTag> tag = getTag(javaClass, tagName);
        if (tag.isPresent()) {
            return tag;
        }
        return getSuperSourceClass(sourceModel, javaClass)
                .flatMap(parent -> findInClassHierarchy(sourceModel, parent, tagName));
    }

    private static Optional<JavadocBlockTag> getTag(NodeWithJavadoc<?> element, String tagName) {
        return element.getJavadoc()
                .flatMap(javadoc -> javadoc.getBlockTags().stream()
                        .filter(tag -> tagName.equals(tag.getTagName()))
                        .findFirst());
    }

    private Map<String, SourceMember> extractFields(JavaSourceModel sourceModel, TypeDeclaration<?> javaClass) {
        Map<String, SourceMember> result = getSuperSourceClass(sourceModel, javaClass)
                .map(parent -> extractFields(sourceModel, parent))
                .orElseGet(TreeMap::new);
        for (FieldDeclaration field : javaClass.getFields()) {
            field.getVariables()
                    .forEach(variable ->
                            result.put(variable.getNameAsString(), new SourceMember(javaClass, field, field)));
        }
        return result;
    }

    private Map<String, SourceMember> extractMethods(JavaSourceModel sourceModel, TypeDeclaration<?> javaClass) {
        Map<String, SourceMember> result = getSuperSourceClass(sourceModel, javaClass)
                .map(parent -> extractMethods(sourceModel, parent))
                .orElseGet(TreeMap::new);
        for (MethodDeclaration method : javaClass.getMethods()) {
            if (isPublicSetterMethod(method)) {
                result.put(
                        StringUtils.lowercaseFirstLetter(
                                method.getNameAsString().substring(3)),
                        new SourceMember(javaClass, method, method));
            }
        }
        return result;
    }

    private static boolean isPublicSetterMethod(MethodDeclaration method) {
        return method.isPublic()
                && !method.isStatic()
                && method.getNameAsString().length() > 3
                && (method.getNameAsString().startsWith("add")
                        || method.getNameAsString().startsWith("set"))
                && method.getType().isVoidType()
                && method.getParameters().size() == 1;
    }

    private Optional<TypeDeclaration<?>> getSuperSourceClass(
            JavaSourceModel sourceModel, TypeDeclaration<?> javaClass) {
        if (!(javaClass instanceof ClassOrInterfaceDeclaration)
                || ((ClassOrInterfaceDeclaration) javaClass).isInterface()) {
            return Optional.empty();
        }
        Optional<ClassOrInterfaceType> superClass = ((ClassOrInterfaceDeclaration) javaClass)
                .getExtendedTypes().stream().findFirst();
        if (!superClass.isPresent()) {
            return Optional.empty();
        }
        try {
            return superClass
                    .get()
                    .resolve()
                    .asReferenceType()
                    .getTypeDeclaration()
                    .flatMap(sourceModel::getType);
        } catch (UnsolvedSymbolException e) {
            // Sources artifacts need not contain the sources of every superclass. In that case there is no
            // source Javadoc to inherit, just as when no sources artifact is available at all.
            LOGGER.debug("Could not resolve source superclass {}", superClass.get(), e);
            return Optional.empty();
        }
    }

    protected Map<String, TypeDeclaration<?>> discoverClasses(JavaSourceModel sourceModel) {
        Map<String, TypeDeclaration<?>> result = new HashMap<>();
        for (TypeDeclaration<?> type : sourceModel.getTypes()) {
            type.getFullyQualifiedName().ifPresent(name -> {
                result.put(name, type);
                // The annotation scanner keys classes by their binary name ("pkg.Outer$Inner"), while
                // JavaParser reports the canonical one ("pkg.Outer.Inner"). Index both so that mojos
                // declared as nested classes still find their Javadoc.
                binaryName(type).ifPresent(binary -> result.put(binary, type));
            });
        }
        return result;
    }

    private static Optional<String> binaryName(TypeDeclaration<?> type) {
        return type.getFullyQualifiedName().flatMap(fullyQualifiedName -> {
            String canonicalNestedName = type.getNameAsString();
            Node current = type;
            while (current.getParentNode().isPresent()
                    && current.getParentNode().get() instanceof TypeDeclaration) {
                TypeDeclaration<?> parent =
                        (TypeDeclaration<?>) current.getParentNode().get();
                canonicalNestedName = parent.getNameAsString() + "." + canonicalNestedName;
                current = parent;
            }
            if (!canonicalNestedName.contains(".")) {
                // top level type: binary and canonical names are identical
                return Optional.empty();
            }
            String binaryNestedName = canonicalNestedName.replace('.', '$');
            return Optional.of(
                    fullyQualifiedName.substring(0, fullyQualifiedName.length() - canonicalNestedName.length())
                            + binaryNestedName);
        });
    }

    protected void extendJavaSourceModelWithSourcesJar(
            JavaSourceModel sourceModel, Artifact artifact, PluginToolsRequest request, String classifier)
            throws ExtractionException, IOException {
        try {
            org.eclipse.aether.artifact.Artifact sourcesArtifact = new DefaultArtifact(
                    artifact.getGroupId(),
                    artifact.getArtifactId(),
                    classifier,
                    artifact.getArtifactHandler().getExtension(),
                    artifact.getVersion());

            ArtifactRequest resolveRequest =
                    new ArtifactRequest(sourcesArtifact, request.getProject().getRemoteProjectRepositories(), null);
            try {
                ArtifactResult result = repositorySystem.resolveArtifact(request.getRepoSession(), resolveRequest);
                sourcesArtifact = result.getArtifact();
            } catch (ArtifactResolutionException e) {
                String message = "Unable to get sources artifact for " + artifact.getId()
                        + ". Some javadoc tags (@since, @deprecated and comments) won't be used";
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.warn(message, e);
                } else {
                    LOGGER.warn(message);
                }
                return;
            }

            if (sourcesArtifact.getFile() == null || !sourcesArtifact.getFile().exists()) {
                // could not get artifact sources
                return;
            }

            if (sourcesArtifact.getFile().isFile()) {
                // extract sources to target/maven-plugin-plugin-sources/${groupId}/${artifact}/sources
                File extractDirectory = new File(
                        request.getProject().getBuild().getDirectory(),
                        "maven-plugin-plugin-sources/" + sourcesArtifact.getGroupId() + "/"
                                + sourcesArtifact.getArtifactId() + "/" + sourcesArtifact.getVersion()
                                + "/" + sourcesArtifact.getClassifier());
                extractDirectory.mkdirs();

                UnArchiver unArchiver = archiverManager.getUnArchiver("jar");
                unArchiver.setSourceFile(sourcesArtifact.getFile());
                unArchiver.setDestDirectory(extractDirectory);
                unArchiver.extract();

                extendJavaSourceModel(sourceModel, Arrays.asList(extractDirectory), request.getDependencies());
            } else if (sourcesArtifact.getFile().isDirectory()) {
                extendJavaSourceModel(sourceModel, Arrays.asList(sourcesArtifact.getFile()), request.getDependencies());
            }
        } catch (ArchiverException | NoSuchArchiverException e) {
            throw new ExtractionException(e.getMessage(), e);
        }
    }

    private void extendJavaSourceModel(
            PluginToolsRequest request, JavaSourceModel sourceModel, final MavenProject project) throws IOException {
        List<File> sources = new ArrayList<>();

        for (String source : project.getCompileSourceRoots()) {
            File sourceFile = new File(source);

            // Allow users to exclude certain paths such as generated sources from being scanned, in the case that
            // this may be problematic for them (e.g. using obscure unsupported syntax by the parser, comments that
            // cannot be controlled, etc.)
            if (!request.isExcludedScanDirectory(sourceFile)) {
                sources.add(sourceFile);
            }
        }

        // TODO be more dynamic
        File generatedPlugin = new File(project.getBasedir(), "target/generated-sources/plugin");
        if (!project.getCompileSourceRoots().contains(generatedPlugin.getAbsolutePath()) && generatedPlugin.exists()) {
            sources.add(generatedPlugin);
        }

        extendJavaSourceModel(sourceModel, sources, project.getArtifacts());
    }

    private void extendJavaSourceModel(
            JavaSourceModel sourceModel, List<File> sourceDirectories, Set<Artifact> artifacts) throws IOException {
        for (Artifact artifact : artifacts) {
            sourceModel.addClassPathEntry(artifact.getFile());
        }
        for (File source : sourceDirectories) {
            sourceModel.addSourceDirectory(source);
        }
    }

    private static int lineNumber(Node node) {
        return node.getBegin().map(position -> position.line).orElse(0);
    }

    private static final class SourceMember {
        private final TypeDeclaration<?> declaringClass;
        private final Node element;
        private final NodeWithJavadoc<?> javadocElement;

        private SourceMember(TypeDeclaration<?> declaringClass, Node element, NodeWithJavadoc<?> javadocElement) {
            this.declaringClass = declaringClass;
            this.element = element;
            this.javadocElement = javadocElement;
        }
    }

    private List<MojoDescriptor> toMojoDescriptors(
            Map<String, MojoAnnotatedClass> mojoAnnotatedClasses, PluginDescriptor pluginDescriptor)
            throws InvalidPluginDescriptorException {
        List<MojoDescriptor> mojoDescriptors = new ArrayList<>(mojoAnnotatedClasses.size());
        for (MojoAnnotatedClass mojoAnnotatedClass : mojoAnnotatedClasses.values()) {
            // no mojo so skip it
            if (mojoAnnotatedClass.getMojo() == null) {
                continue;
            }

            ExtendedMojoDescriptor mojoDescriptor = new ExtendedMojoDescriptor(true);

            // mojoDescriptor.setRole( mojoAnnotatedClass.getClassName() );
            // mojoDescriptor.setRoleHint( "default" );
            mojoDescriptor.setImplementation(mojoAnnotatedClass.getClassName());
            mojoDescriptor.setLanguage("java");

            mojoDescriptor.setV4Api(mojoAnnotatedClass.isV4Api());

            MojoAnnotationContent mojo = mojoAnnotatedClass.getMojo();

            mojoDescriptor.setDescription(mojo.getDescription());
            mojoDescriptor.setSince(mojo.getSince());
            mojo.setDeprecated(mojo.getDeprecated());

            mojoDescriptor.setProjectRequired(mojo.requiresProject());

            mojoDescriptor.setRequiresReports(mojo.requiresReports());

            mojoDescriptor.setComponentConfigurator(mojo.configurator());

            mojoDescriptor.setInheritedByDefault(mojo.inheritByDefault());

            mojoDescriptor.setInstantiationStrategy(mojo.instantiationStrategy().id());

            mojoDescriptor.setAggregator(mojo.aggregator());
            mojoDescriptor.setDependencyResolutionRequired(
                    mojo.requiresDependencyResolution().id());
            mojoDescriptor.setDependencyCollectionRequired(
                    mojo.requiresDependencyCollection().id());

            mojoDescriptor.setDirectInvocationOnly(mojo.requiresDirectInvocation());
            mojoDescriptor.setDeprecated(mojo.getDeprecated());
            mojoDescriptor.setThreadSafe(mojo.threadSafe());

            MojoAnnotatedClass mojoAnnotatedClassWithExecute =
                    findClassWithExecuteAnnotationInParentHierarchy(mojoAnnotatedClass, mojoAnnotatedClasses);
            if (mojoAnnotatedClassWithExecute != null && mojoAnnotatedClassWithExecute.getExecute() != null) {
                ExecuteAnnotationContent execute = mojoAnnotatedClassWithExecute.getExecute();
                mojoDescriptor.setExecuteGoal(execute.goal());
                mojoDescriptor.setExecuteLifecycle(execute.lifecycle());
                if (execute.phase() != null) {
                    mojoDescriptor.setExecutePhase(execute.phase().id());
                    if (StringUtils.isNotEmpty(execute.customPhase())) {
                        throw new InvalidPluginDescriptorException(
                                "@Execute annotation must only use either 'phase' "
                                        + "or 'customPhase' but not both. Both are used though on "
                                        + mojoAnnotatedClassWithExecute.getClassName(),
                                null);
                    }
                } else if (StringUtils.isNotEmpty(execute.customPhase())) {
                    mojoDescriptor.setExecutePhase(execute.customPhase());
                }
            }

            // @After annotations — collect from class hierarchy
            List<AfterAnnotationContent> afterAnnotations =
                    getAfterAnnotationsFromHierarchy(mojoAnnotatedClass, mojoAnnotatedClasses);
            for (AfterAnnotationContent after : afterAnnotations) {
                mojoDescriptor.addAfterLink(
                        new ExtendedMojoDescriptor.AfterLink(after.phase(), after.type(), after.scope()));
            }

            mojoDescriptor.setExecutionStrategy(mojo.executionStrategy());
            // ???
            // mojoDescriptor.alwaysExecute(mojo.a)

            mojoDescriptor.setGoal(mojo.name());
            mojoDescriptor.setOnlineRequired(mojo.requiresOnline());

            mojoDescriptor.setPhase(mojo.defaultPhase().id());

            // Parameter annotations
            Map<String, ParameterAnnotationContent> parameters =
                    getParametersParentHierarchy(mojoAnnotatedClass, mojoAnnotatedClasses);

            for (ParameterAnnotationContent parameterAnnotationContent : new TreeSet<>(parameters.values())) {
                org.apache.maven.plugin.descriptor.Parameter parameter =
                        new org.apache.maven.plugin.descriptor.Parameter();
                String name = StringUtils.isEmpty(parameterAnnotationContent.name())
                        ? parameterAnnotationContent.getFieldName()
                        : parameterAnnotationContent.name();
                parameter.setName(name);
                parameter.setAlias(parameterAnnotationContent.alias());
                parameter.setDefaultValue(parameterAnnotationContent.defaultValue());
                parameter.setDeprecated(parameterAnnotationContent.getDeprecated());
                parameter.setDescription(parameterAnnotationContent.getDescription());
                parameter.setEditable(!parameterAnnotationContent.readonly());
                String property = parameterAnnotationContent.property();
                if (StringUtils.contains(property, '$')
                        || StringUtils.contains(property, '{')
                        || StringUtils.contains(property, '}')) {
                    throw new InvalidParameterException(
                            "Invalid property for parameter '" + parameter.getName() + "', "
                                    + "forbidden characters ${}: " + property,
                            null);
                }
                parameter.setExpression((property == null || property.isEmpty()) ? "" : "${" + property + "}");
                StringBuilder type = new StringBuilder(parameterAnnotationContent.getClassName());
                if (!parameterAnnotationContent.getTypeParameters().isEmpty()) {
                    type.append(parameterAnnotationContent.getTypeParameters().stream()
                            .collect(Collectors.joining(", ", "<", ">")));
                }
                parameter.setType(type.toString());
                parameter.setSince(parameterAnnotationContent.getSince());
                parameter.setRequired(parameterAnnotationContent.required());

                mojoDescriptor.addParameter(parameter);
            }

            // Component annotations
            Map<String, ComponentAnnotationContent> components =
                    getComponentsParentHierarchy(mojoAnnotatedClass, mojoAnnotatedClasses);

            for (ComponentAnnotationContent componentAnnotationContent : new TreeSet<>(components.values())) {
                org.apache.maven.plugin.descriptor.Parameter parameter =
                        new org.apache.maven.plugin.descriptor.Parameter();
                parameter.setName(componentAnnotationContent.getFieldName());

                parameter.setRequirement(new Requirement(
                        componentAnnotationContent.getRoleClassName(), componentAnnotationContent.hint()));
                parameter.setDeprecated(componentAnnotationContent.getDeprecated());
                parameter.setSince(componentAnnotationContent.getSince());

                // same behaviour as JavaJavadocMojoDescriptorExtractor
                parameter.setEditable(false);

                mojoDescriptor.addParameter(parameter);
            }

            mojoDescriptor.setPluginDescriptor(pluginDescriptor);

            mojoDescriptors.add(mojoDescriptor);
        }
        return mojoDescriptors;
    }

    protected MojoAnnotatedClass findClassWithExecuteAnnotationInParentHierarchy(
            MojoAnnotatedClass mojoAnnotatedClass, Map<String, MojoAnnotatedClass> mojoAnnotatedClasses) {
        if (mojoAnnotatedClass.getExecute() != null) {
            return mojoAnnotatedClass;
        }
        String parentClassName = mojoAnnotatedClass.getParentClassName();
        if (parentClassName == null || parentClassName.isEmpty()) {
            return null;
        }
        MojoAnnotatedClass parent = mojoAnnotatedClasses.get(parentClassName);
        if (parent == null) {
            return null;
        }
        return findClassWithExecuteAnnotationInParentHierarchy(parent, mojoAnnotatedClasses);
    }

    /**
     * Collects all {@code @After} annotations from the class hierarchy, starting from the
     * most-derived class.  Unlike {@code @Execute}, {@code @After} is repeatable, so
     * multiple entries may exist on a single class and across the hierarchy.
     */
    protected List<AfterAnnotationContent> getAfterAnnotationsFromHierarchy(
            MojoAnnotatedClass mojoAnnotatedClass, Map<String, MojoAnnotatedClass> mojoAnnotatedClasses) {
        List<AfterAnnotationContent> result = new ArrayList<>();
        collectAfterAnnotations(mojoAnnotatedClass, mojoAnnotatedClasses, result);
        return result;
    }

    private void collectAfterAnnotations(
            MojoAnnotatedClass mojoAnnotatedClass,
            Map<String, MojoAnnotatedClass> mojoAnnotatedClasses,
            List<AfterAnnotationContent> result) {
        result.addAll(mojoAnnotatedClass.getAfterAnnotations());
        String parentClassName = mojoAnnotatedClass.getParentClassName();
        if (parentClassName != null && !parentClassName.isEmpty()) {
            MojoAnnotatedClass parent = mojoAnnotatedClasses.get(parentClassName);
            if (parent != null) {
                collectAfterAnnotations(parent, mojoAnnotatedClasses, result);
            }
        }
    }

    protected Map<String, ParameterAnnotationContent> getParametersParentHierarchy(
            MojoAnnotatedClass mojoAnnotatedClass, Map<String, MojoAnnotatedClass> mojoAnnotatedClasses) {
        List<ParameterAnnotationContent> parameterAnnotationContents = new ArrayList<>();

        parameterAnnotationContents =
                getParametersParent(mojoAnnotatedClass, parameterAnnotationContents, mojoAnnotatedClasses);

        // move to parent first to build the Map
        Collections.reverse(parameterAnnotationContents);

        Map<String, ParameterAnnotationContent> map = new HashMap<>(parameterAnnotationContents.size());

        for (ParameterAnnotationContent parameterAnnotationContent : parameterAnnotationContents) {
            map.put(parameterAnnotationContent.getFieldName(), parameterAnnotationContent);
        }
        return map;
    }

    protected List<ParameterAnnotationContent> getParametersParent(
            MojoAnnotatedClass mojoAnnotatedClass,
            List<ParameterAnnotationContent> parameterAnnotationContents,
            Map<String, MojoAnnotatedClass> mojoAnnotatedClasses) {
        parameterAnnotationContents.addAll(mojoAnnotatedClass.getParameters().values());
        String parentClassName = mojoAnnotatedClass.getParentClassName();
        if (parentClassName != null) {
            MojoAnnotatedClass parent = mojoAnnotatedClasses.get(parentClassName);
            if (parent != null) {
                return getParametersParent(parent, parameterAnnotationContents, mojoAnnotatedClasses);
            }
        }
        return parameterAnnotationContents;
    }

    protected Map<String, ComponentAnnotationContent> getComponentsParentHierarchy(
            MojoAnnotatedClass mojoAnnotatedClass, Map<String, MojoAnnotatedClass> mojoAnnotatedClasses) {
        List<ComponentAnnotationContent> componentAnnotationContents = new ArrayList<>();

        componentAnnotationContents =
                getComponentParent(mojoAnnotatedClass, componentAnnotationContents, mojoAnnotatedClasses);

        // move to parent first to build the Map
        Collections.reverse(componentAnnotationContents);

        Map<String, ComponentAnnotationContent> map = new HashMap<>(componentAnnotationContents.size());

        for (ComponentAnnotationContent componentAnnotationContent : componentAnnotationContents) {
            map.put(componentAnnotationContent.getFieldName(), componentAnnotationContent);
        }
        return map;
    }

    protected List<ComponentAnnotationContent> getComponentParent(
            MojoAnnotatedClass mojoAnnotatedClass,
            List<ComponentAnnotationContent> componentAnnotationContents,
            Map<String, MojoAnnotatedClass> mojoAnnotatedClasses) {
        componentAnnotationContents.addAll(mojoAnnotatedClass.getComponents().values());
        String parentClassName = mojoAnnotatedClass.getParentClassName();
        if (parentClassName != null) {
            MojoAnnotatedClass parent = mojoAnnotatedClasses.get(parentClassName);
            if (parent != null) {
                return getComponentParent(parent, componentAnnotationContents, mojoAnnotatedClasses);
            }
        }
        return componentAnnotationContents;
    }

    protected MavenProject getFromProjectReferences(Artifact artifact, MavenProject project) {
        if (project.getProjectReferences() == null
                || project.getProjectReferences().isEmpty()) {
            return null;
        }
        Collection<MavenProject> mavenProjects = project.getProjectReferences().values();
        for (MavenProject mavenProject : mavenProjects) {
            if (Objects.equals(mavenProject.getId(), artifact.getId())) {
                return mavenProject;
            }
        }
        return null;
    }
}
