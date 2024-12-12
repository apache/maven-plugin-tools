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

import javax.inject.Named;
import javax.inject.Singleton;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Execute;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.tools.plugin.extractor.ExtractionException;
import org.apache.maven.tools.plugin.extractor.annotations.datamodel.ComponentAnnotationContent;
import org.apache.maven.tools.plugin.extractor.annotations.datamodel.ExecuteAnnotationContent;
import org.apache.maven.tools.plugin.extractor.annotations.datamodel.MojoAnnotationContent;
import org.apache.maven.tools.plugin.extractor.annotations.datamodel.ParameterAnnotationContent;
import org.apache.maven.tools.plugin.extractor.annotations.scanner.visitors.MojoAnnotationVisitor;
import org.apache.maven.tools.plugin.extractor.annotations.scanner.visitors.MojoClassVisitor;
import org.apache.maven.tools.plugin.extractor.annotations.scanner.visitors.MojoFieldVisitor;
import org.apache.maven.tools.plugin.extractor.annotations.scanner.visitors.MojoParameterVisitor;
import org.codehaus.plexus.util.DirectoryScanner;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.reflection.Reflector;
import org.codehaus.plexus.util.reflection.ReflectorException;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Mojo scanner with java annotations.
 *
 * @author Olivier Lamy
 * @since 3.0
 */
@Named
@Singleton
public class DefaultMojoAnnotationsScanner implements MojoAnnotationsScanner {
    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultMojoAnnotationsScanner.class);
    public static final String MVN4_API = "org.apache.maven.api.plugin.annotations.";
    public static final String MOJO_V4 = MVN4_API + "Mojo";
    public static final String EXECUTE_V4 = MVN4_API + "Execute";
    public static final String PARAMETER_V4 = MVN4_API + "Parameter";
    public static final String COMPONENT_V4 = MVN4_API + "Component";

    public static final String MOJO_V3 = Mojo.class.getName();
    public static final String EXECUTE_V3 = Execute.class.getName();
    public static final String PARAMETER_V3 = Parameter.class.getName();
    public static final String COMPONENT_V3 = Component.class.getName();

    // classes with a dash must be ignored
    private static final Pattern SCANNABLE_CLASS = Pattern.compile("[^-]+\\.class");
    private static final String EMPTY = "";

    private Reflector reflector = new Reflector();

    @Override
    public Map<String, MojoAnnotatedClass> scan(MojoAnnotationsScannerRequest request) throws ExtractionException {
        Map<String, MojoAnnotatedClass> mojoAnnotatedClasses = new HashMap<>();

        try {
            for (Artifact dependency : request.getDependencies()) {
                scan(mojoAnnotatedClasses, dependency.getFile(), request.getIncludePatterns(), dependency, true);
                if (request.getMavenApiVersion() == null
                        && dependency.getGroupId().equals("org.apache.maven")
                        && (dependency.getArtifactId().equals("maven-plugin-api")
                                || dependency.getArtifactId().equals("maven-api-core"))) {
                    request.setMavenApiVersion(dependency.getVersion());
                }
            }

            for (File classDirectory : request.getClassesDirectories()) {
                scan(
                        mojoAnnotatedClasses,
                        classDirectory,
                        request.getIncludePatterns(),
                        request.getProject().getArtifact(),
                        false);
            }
        } catch (IOException e) {
            throw new ExtractionException(e.getMessage(), e);
        }

        return mojoAnnotatedClasses;
    }

    protected void scan(
            Map<String, MojoAnnotatedClass> mojoAnnotatedClasses,
            File source,
            List<String> includePatterns,
            Artifact artifact,
            boolean excludeMojo)
            throws IOException, ExtractionException {
        if (source == null || !source.exists()) {
            return;
        }

        Map<String, MojoAnnotatedClass> scanResult;
        if (source.isDirectory()) {
            scanResult = scanDirectory(source, includePatterns, artifact, excludeMojo);
        } else {
            scanResult = scanArchive(source, artifact, excludeMojo);
        }

        mojoAnnotatedClasses.putAll(scanResult);
    }

    /**
     * @param archiveFile
     * @param artifact
     * @param excludeMojo     for dependencies, we exclude Mojo annotations found
     * @return annotated classes found
     * @throws IOException
     * @throws ExtractionException
     */
    protected Map<String, MojoAnnotatedClass> scanArchive(File archiveFile, Artifact artifact, boolean excludeMojo)
            throws IOException, ExtractionException {
        Map<String, MojoAnnotatedClass> mojoAnnotatedClasses = new HashMap<>();

        String zipEntryName = null;
        try (ZipInputStream archiveStream = new ZipInputStream(new FileInputStream(archiveFile))) {
            String archiveFilename = archiveFile.getAbsolutePath();
            for (ZipEntry zipEntry = archiveStream.getNextEntry();
                    zipEntry != null;
                    zipEntry = archiveStream.getNextEntry()) {
                zipEntryName = zipEntry.getName();
                if (!SCANNABLE_CLASS.matcher(zipEntryName).matches()) {
                    continue;
                }
                analyzeClassStream(
                        mojoAnnotatedClasses,
                        archiveStream,
                        artifact,
                        excludeMojo,
                        archiveFilename,
                        zipEntry.getName());
            }
        } catch (IllegalArgumentException e) {
            // In case of a class with newer specs an IllegalArgumentException can be thrown
            LOGGER.error("Failed to analyze " + archiveFile.getAbsolutePath() + "!/" + zipEntryName);

            throw e;
        }

        return mojoAnnotatedClasses;
    }

    /**
     * @param classDirectory
     * @param includePatterns
     * @param artifact
     * @param excludeMojo     for dependencies, we exclude Mojo annotations found
     * @return annotated classes found
     * @throws IOException
     * @throws ExtractionException
     */
    protected Map<String, MojoAnnotatedClass> scanDirectory(
            File classDirectory, List<String> includePatterns, Artifact artifact, boolean excludeMojo)
            throws IOException, ExtractionException {
        Map<String, MojoAnnotatedClass> mojoAnnotatedClasses = new HashMap<>();

        DirectoryScanner scanner = new DirectoryScanner();
        scanner.setBasedir(classDirectory);
        scanner.addDefaultExcludes();
        if (includePatterns != null) {
            scanner.setIncludes(includePatterns.toArray(new String[includePatterns.size()]));
        }
        scanner.scan();
        String[] classFiles = scanner.getIncludedFiles();
        String classDirname = classDirectory.getAbsolutePath();

        for (String classFile : classFiles) {
            if (!SCANNABLE_CLASS.matcher(classFile).matches()) {
                continue;
            }

            try (InputStream is = //
                    new BufferedInputStream(new FileInputStream(new File(classDirectory, classFile)))) {
                analyzeClassStream(mojoAnnotatedClasses, is, artifact, excludeMojo, classDirname, classFile);
            }
        }
        return mojoAnnotatedClasses;
    }

    private void analyzeClassStream(
            Map<String, MojoAnnotatedClass> mojoAnnotatedClasses,
            InputStream is,
            Artifact artifact,
            boolean excludeMojo,
            String source,
            String file)
            throws IOException, ExtractionException {
        MojoClassVisitor mojoClassVisitor = new MojoClassVisitor();
        try {
            ClassReader rdr = new ClassReader(is);
            rdr.accept(mojoClassVisitor, ClassReader.SKIP_FRAMES | ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG);
        } catch (ArrayIndexOutOfBoundsException aiooe) {
            LOGGER.warn(
                    "Error analyzing class " + file + " in " + source + ": ignoring class",
                    LOGGER.isDebugEnabled() ? aiooe : null);
            return;
        } catch (IllegalArgumentException iae) {
            if (iae.getMessage() == null) {
                LOGGER.warn(
                        "Error analyzing class " + file + " in " + source + ": ignoring class",
                        LOGGER.isDebugEnabled() ? iae : null);
                return;
            } else {
                throw iae;
            }
        }

        analyzeVisitors(mojoClassVisitor);

        MojoAnnotatedClass mojoAnnotatedClass = mojoClassVisitor.getMojoAnnotatedClass();

        if (excludeMojo) {
            mojoAnnotatedClass.setMojo(null);
        }

        if (mojoAnnotatedClass != null) // see MPLUGIN-206 we can have intermediate classes without annotations
        {
            if (LOGGER.isDebugEnabled() && mojoAnnotatedClass.hasAnnotations()) {
                LOGGER.debug(
                        "found MojoAnnotatedClass:" + mojoAnnotatedClass.getClassName() + ":" + mojoAnnotatedClass);
            }
            mojoAnnotatedClass.setArtifact(artifact);
            mojoAnnotatedClasses.put(mojoAnnotatedClass.getClassName(), mojoAnnotatedClass);
            mojoAnnotatedClass.setClassVersion(mojoClassVisitor.getVersion());
        }
    }

    protected void populateAnnotationContent(Object content, MojoAnnotationVisitor mojoAnnotationVisitor)
            throws ReflectorException {
        for (Map.Entry<String, Object> entry :
                mojoAnnotationVisitor.getAnnotationValues().entrySet()) {
            reflector.invoke(content, entry.getKey(), new Object[] {entry.getValue()});
        }
    }

    protected void analyzeVisitors(MojoClassVisitor mojoClassVisitor) throws ExtractionException {
        final MojoAnnotatedClass mojoAnnotatedClass = mojoClassVisitor.getMojoAnnotatedClass();

        try {
            // @Mojo annotation
            MojoAnnotationVisitor mojoAnnotationVisitor = mojoClassVisitor.getAnnotationVisitor(MOJO_V3);
            if (mojoAnnotationVisitor == null) {
                mojoAnnotationVisitor = mojoClassVisitor.getAnnotationVisitor(MOJO_V4);
            }
            if (mojoAnnotationVisitor != null) {
                MojoAnnotationContent mojoAnnotationContent = new MojoAnnotationContent();
                populateAnnotationContent(mojoAnnotationContent, mojoAnnotationVisitor);

                if (mojoClassVisitor.getAnnotationVisitor(Deprecated.class) != null) {
                    mojoAnnotationContent.setDeprecated(EMPTY);
                }

                mojoAnnotatedClass.setMojo(mojoAnnotationContent);
            }

            // @Execute annotation
            mojoAnnotationVisitor = mojoClassVisitor.getAnnotationVisitor(EXECUTE_V3);
            if (mojoAnnotationVisitor == null) {
                mojoAnnotationVisitor = mojoClassVisitor.getAnnotationVisitor(EXECUTE_V4);
            }
            if (mojoAnnotationVisitor != null) {
                ExecuteAnnotationContent executeAnnotationContent = new ExecuteAnnotationContent();
                populateAnnotationContent(executeAnnotationContent, mojoAnnotationVisitor);
                mojoAnnotatedClass.setExecute(executeAnnotationContent);
            }

            // @Parameter annotations
            List<MojoParameterVisitor> mojoParameterVisitors =
                    mojoClassVisitor.findParameterVisitors(new HashSet<>(Arrays.asList(PARAMETER_V3, PARAMETER_V4)));
            for (MojoParameterVisitor parameterVisitor : mojoParameterVisitors) {
                ParameterAnnotationContent parameterAnnotationContent = new ParameterAnnotationContent(
                        parameterVisitor.getFieldName(),
                        parameterVisitor.getClassName(),
                        parameterVisitor.getTypeParameters(),
                        parameterVisitor.isAnnotationOnMethod());

                Map<String, MojoAnnotationVisitor> annotationVisitorMap = parameterVisitor.getAnnotationVisitorMap();
                MojoAnnotationVisitor fieldAnnotationVisitor = annotationVisitorMap.get(PARAMETER_V3);
                if (fieldAnnotationVisitor == null) {
                    fieldAnnotationVisitor = annotationVisitorMap.get(PARAMETER_V4);
                }

                if (fieldAnnotationVisitor != null) {
                    populateAnnotationContent(parameterAnnotationContent, fieldAnnotationVisitor);
                }

                if (annotationVisitorMap.containsKey(Deprecated.class.getName())) {
                    parameterAnnotationContent.setDeprecated(EMPTY);
                }

                mojoAnnotatedClass
                        .getParameters()
                        .put(parameterAnnotationContent.getFieldName(), parameterAnnotationContent);
            }

            // @Component annotations
            List<MojoFieldVisitor> mojoFieldVisitors =
                    mojoClassVisitor.findFieldWithAnnotation(new HashSet<>(Arrays.asList(COMPONENT_V3, COMPONENT_V4)));
            for (MojoFieldVisitor mojoFieldVisitor : mojoFieldVisitors) {
                ComponentAnnotationContent componentAnnotationContent =
                        new ComponentAnnotationContent(mojoFieldVisitor.getFieldName());

                Map<String, MojoAnnotationVisitor> annotationVisitorMap = mojoFieldVisitor.getAnnotationVisitorMap();
                MojoAnnotationVisitor annotationVisitor = annotationVisitorMap.get(COMPONENT_V3);
                if (annotationVisitor == null) {
                    annotationVisitor = annotationVisitorMap.get(COMPONENT_V4);
                }

                if (annotationVisitor != null) {
                    for (Map.Entry<String, Object> entry :
                            annotationVisitor.getAnnotationValues().entrySet()) {
                        String methodName = entry.getKey();
                        if ("role".equals(methodName)) {
                            Type type = (Type) entry.getValue();
                            componentAnnotationContent.setRoleClassName(type.getClassName());
                        } else {
                            reflector.invoke(
                                    componentAnnotationContent, entry.getKey(), new Object[] {entry.getValue()});
                        }
                    }

                    if (StringUtils.isEmpty(componentAnnotationContent.getRoleClassName())) {
                        componentAnnotationContent.setRoleClassName(mojoFieldVisitor.getClassName());
                    }
                }
                mojoAnnotatedClass
                        .getComponents()
                        .put(componentAnnotationContent.getFieldName(), componentAnnotationContent);
            }
        } catch (ReflectorException e) {
            throw new ExtractionException(e.getMessage(), e);
        }
    }
}
