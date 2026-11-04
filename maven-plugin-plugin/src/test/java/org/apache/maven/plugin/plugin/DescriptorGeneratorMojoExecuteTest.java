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

import javax.inject.Inject;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.maven.api.plugin.testing.Basedir;
import org.apache.maven.api.plugin.testing.InjectMojo;
import org.apache.maven.api.plugin.testing.MojoTest;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.DefaultArtifactHandler;
import org.apache.maven.plugin.logging.SystemStreamLog;
import org.apache.maven.project.MavenProject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.apache.maven.api.plugin.testing.MojoExtension.setVariableValueToObject;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Runs {@link DescriptorGeneratorMojo#execute()} against an empty class output directory. Execution that is not
 * stopped early still writes a {@code plugin.xml}, which is how the tests tell a skipped run from a real one.
 */
@MojoTest
class DescriptorGeneratorMojoExecuteTest {

    private static final String SCOPE_WARNING =
            "Some dependencies of Maven Plugins are expected to be in provided scope.";

    @TempDir
    private Path outputDirectory;

    @Inject
    private MavenProject project;

    private final RecordingLog log = new RecordingLog();

    @Test
    @InjectMojo(goal = "descriptor", pom = "plugin-config.xml")
    @Basedir("/unit/descriptor-skip")
    void skipParameterSuppressesDescriptorGeneration(DescriptorGeneratorMojo mojo) throws Exception {
        prepare(mojo, "maven-plugin");

        mojo.execute();

        assertTrue(log.contains("Execution skipped"), log.toString());
        assertFalse(Files.exists(descriptor()), "plugin.xml must not be written");
    }

    @Test
    @InjectMojo(goal = "descriptor", pom = "plugin-config.xml")
    @Basedir("/unit/descriptor-unsupported-packaging")
    void unsupportedPackagingIsSkipped(DescriptorGeneratorMojo mojo) throws Exception {
        prepare(mojo, "jar");

        mojo.execute();

        assertTrue(log.contains("Unsupported packaging type jar, execution skipped"), log.toString());
        assertFalse(Files.exists(descriptor()), "plugin.xml must not be written");
    }

    @Test
    @InjectMojo(goal = "descriptor", pom = "plugin-config.xml")
    @Basedir("/unit/descriptor-packaging-jar")
    void configuredPackagingTypeIsExecuted(DescriptorGeneratorMojo mojo) throws Exception {
        prepare(mojo, "jar");

        mojo.execute();

        assertFalse(log.contains("Unsupported packaging type"), log.toString());
        assertTrue(Files.isRegularFile(descriptor()), "plugin.xml must be written for packagingTypes=jar");
    }

    @Test
    @InjectMojo(goal = "descriptor", pom = "plugin-config.xml")
    @Basedir("/unit/descriptor-scope")
    void mavenDependencyOutsideProvidedScopeIsReported(DescriptorGeneratorMojo mojo) throws Exception {
        prepare(mojo, "maven-plugin");
        project.setArtifacts(artifacts(
                artifact("org.apache.maven", "maven-plugin-api", Artifact.SCOPE_COMPILE),
                artifact("org.apache.maven.plugin-tools", "maven-plugin-annotations", Artifact.SCOPE_COMPILE)));

        mojo.execute();

        assertTrue(log.contains(SCOPE_WARNING), log.toString());
        assertTrue(log.contains(" * org.apache.maven:maven-plugin-api:jar:1.0:compile"), log.toString());
        assertFalse(log.contains("maven-plugin-annotations:jar"), log.toString());
    }

    @Test
    @InjectMojo(goal = "descriptor", pom = "plugin-config.xml")
    @Basedir("/unit/descriptor-scope")
    void providedAndExcludedDependenciesAreNotReported(DescriptorGeneratorMojo mojo) throws Exception {
        prepare(mojo, "maven-plugin");
        project.setArtifacts(artifacts(
                artifact("org.apache.maven", "maven-plugin-api", Artifact.SCOPE_PROVIDED),
                // maven-jxr is on the default exclusion list
                artifact("org.apache.maven", "maven-jxr", Artifact.SCOPE_COMPILE),
                artifact("org.apache.maven.plugin-tools", "maven-plugin-annotations", Artifact.SCOPE_COMPILE)));

        mojo.execute();

        assertFalse(log.contains(SCOPE_WARNING), log.toString());
        assertTrue(Files.isRegularFile(descriptor()), "the run must get past the scope check");
    }

    private void prepare(DescriptorGeneratorMojo mojo, String packaging) throws Exception {
        project.setGroupId("org.apache.maven.its");
        project.setArtifactId("descriptor-execute");
        project.setVersion("1.0-SNAPSHOT");
        project.setPackaging(packaging);
        Path classes = Files.createDirectories(outputDirectory.resolve("classes"));
        project.getBuild().setOutputDirectory(classes.toString());
        setVariableValueToObject(mojo, "classesOutputDirectory", classes.toFile());
        setVariableValueToObject(
                mojo, "outputDirectory", descriptor().getParent().toFile());
        mojo.setLog(log);
    }

    private Path descriptor() {
        return outputDirectory.resolve("META-INF/maven/plugin.xml");
    }

    private static Artifact artifact(String groupId, String artifactId, String scope) {
        return new DefaultArtifact(groupId, artifactId, "1.0", scope, "jar", null, new DefaultArtifactHandler("jar"));
    }

    private static Set<Artifact> artifacts(Artifact... artifacts) {
        return new HashSet<>(Arrays.asList(artifacts));
    }

    /** Collects what the mojo logs at info and warn level. */
    static final class RecordingLog extends SystemStreamLog {
        private final List<String> messages = new ArrayList<>();

        boolean contains(String text) {
            return messages.stream().anyMatch(message -> message.contains(text));
        }

        @Override
        public void info(CharSequence content) {
            messages.add(content.toString());
        }

        @Override
        public void warn(CharSequence content) {
            messages.add(content.toString());
        }

        @Override
        public String toString() {
            return messages.toString();
        }
    }
}
