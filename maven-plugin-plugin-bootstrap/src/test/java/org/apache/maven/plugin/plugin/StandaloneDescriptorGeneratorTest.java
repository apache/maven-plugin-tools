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
import java.nio.file.Files;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Build;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginManagement;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StandaloneDescriptorGeneratorTest {

    @Test
    void testFindGoalPrefix() {
        Model model = new Model();
        assertNull(StandaloneDescriptorGenerator.findGoalPrefix(model));

        Build build = new Build();
        model.setBuild(build);
        assertNull(StandaloneDescriptorGenerator.findGoalPrefix(model));

        Plugin plugin = new Plugin();
        plugin.setArtifactId("maven-plugin-plugin");
        build.addPlugin(plugin);
        assertNull(StandaloneDescriptorGenerator.findGoalPrefix(model));

        Xpp3Dom configuration = new Xpp3Dom("configuration");
        Xpp3Dom goalPrefix = new Xpp3Dom("goalPrefix");
        goalPrefix.setValue("custom-prefix");
        configuration.addChild(goalPrefix);
        plugin.setConfiguration(configuration);

        assertEquals("custom-prefix", StandaloneDescriptorGenerator.findGoalPrefix(model));
    }

    @Test
    void testFindGoalPrefixInPluginManagement() {
        Model model = new Model();
        Build build = new Build();
        model.setBuild(build);

        PluginManagement pluginManagement = new PluginManagement();
        build.setPluginManagement(pluginManagement);

        Plugin plugin = new Plugin();
        plugin.setArtifactId("maven-plugin-plugin");
        pluginManagement.addPlugin(plugin);

        Xpp3Dom configuration = new Xpp3Dom("configuration");
        Xpp3Dom goalPrefix = new Xpp3Dom("goalPrefix");
        goalPrefix.setValue("mgmt-prefix");
        configuration.addChild(goalPrefix);
        plugin.setConfiguration(configuration);

        assertEquals("mgmt-prefix", StandaloneDescriptorGenerator.findGoalPrefix(model));
    }

    @Test
    void testFindCompileSourceRootsWalksParentChainAndInterpolatesPaths(@TempDir File tempDir) throws Exception {
        File parentDir = new File(tempDir, "parent");
        parentDir.mkdirs();
        Files.write(
                new File(parentDir, "pom.xml").toPath(),
                ("<project><modelVersion>4.0.0</modelVersion>"
                                + "<groupId>test</groupId><artifactId>parent</artifactId><version>1.0</version>"
                                + "<packaging>pom</packaging>"
                                + "<properties><srcDir>custom-src</srcDir></properties>"
                                + "</project>")
                        .getBytes());

        File childDir = new File(tempDir, "child");
        childDir.mkdirs();
        File childPom = new File(childDir, "pom.xml");
        Files.write(
                childPom.toPath(),
                ("<project><modelVersion>4.0.0</modelVersion>"
                                + "<parent><groupId>test</groupId><artifactId>parent</artifactId>"
                                + "<version>1.0</version><relativePath>../parent/pom.xml</relativePath></parent>"
                                + "<artifactId>child</artifactId>"
                                + "<build><sourceDirectory>${srcDir}</sourceDirectory></build>"
                                + "</project>")
                        .getBytes());

        Model model = StandaloneDescriptorGenerator.buildEffectiveModel(childPom);
        List<File> roots = StandaloneDescriptorGenerator.findCompileSourceRoots(model, childDir);
        assertEquals(Collections.singletonList(new File(childDir, "custom-src").getAbsoluteFile()), roots);
    }

    @Test
    void testBuildScanArtifactsIncludesClasspathEntries() throws Exception {
        String originalClasspath = System.getProperty("java.class.path");
        try {
            File tempDir = Files.createTempDirectory("scan-artifacts").toFile();
            tempDir.deleteOnExit();
            File classpathJar = new File(tempDir, "somelib.jar");
            classpathJar.createNewFile();

            System.setProperty("java.class.path", classpathJar.getAbsolutePath());

            Set<Artifact> scanArtifacts = StandaloneDescriptorGenerator.buildScanArtifacts();

            assertTrue(
                    scanArtifacts.stream()
                            .anyMatch(a -> classpathJar.getAbsoluteFile().equals(a.getFile())),
                    "the real classpath jar should be included for scanning");
        } finally {
            if (originalClasspath != null) {
                System.setProperty("java.class.path", originalClasspath);
            }
        }
    }

    @Test
    void testRun(@TempDir File tempDir) throws Exception {
        File pomFile = new File(tempDir, "pom.xml");
        String pomContent = "<project>\n" + "  <modelVersion>4.0.0</modelVersion>\n"
                + "  <groupId>org.example</groupId>\n"
                + "  <artifactId>test-plugin</artifactId>\n"
                + "  <version>1.0.0</version>\n"
                + "  <packaging>maven-plugin</packaging>\n"
                + "</project>";
        Files.write(pomFile.toPath(), pomContent.getBytes());

        File classesDir = new File(tempDir, "target/classes");
        classesDir.mkdirs();

        StandaloneDescriptorGenerator.run(pomFile);

        File pluginXml = new File(classesDir, "META-INF/maven/plugin.xml");
        assertTrue(pluginXml.exists(), "plugin.xml should be generated by run() in classesDirectory/META-INF/maven");
    }

    @Test
    void generateV4DiIndexFindsNamedBeans(@TempDir File tempDir) throws Exception {
        File classesDir = new File(tempDir, "classes");
        File outputDir = new File(tempDir, "output");
        File packageDir = new File(classesDir, "org/example");
        packageDir.mkdirs();
        outputDir.mkdirs();

        byte[] bytes = DescriptorGeneratorMojo.computeGeneratorClassBytes(
                "org.example", "SomeMojoFactory", "grp:art:1.0:goal", "java.lang.Object");
        Files.write(new File(packageDir, "SomeMojoFactory.class").toPath(), bytes);

        DescriptorGeneratorMojo.generateV4DiIndex(classesDir, outputDir);

        File indexFile = new File(outputDir, "org.apache.maven.api.di.Inject");
        assertTrue(indexFile.exists(), "index file should be created when a @Named v4 bean is present");
        List<String> lines = Files.readAllLines(indexFile.toPath());
        assertEquals(Collections.singletonList("org.example.SomeMojoFactory"), lines);
    }

    @Test
    void generateV4DiIndexDeletesStaleFileWhenNoNamedBeans(@TempDir File tempDir) throws Exception {
        File classesDir = new File(tempDir, "classes");
        File outputDir = new File(tempDir, "output");
        classesDir.mkdirs();
        outputDir.mkdirs();
        File indexFile = new File(outputDir, "org.apache.maven.api.di.Inject");
        Files.write(indexFile.toPath(), "stale".getBytes());

        DescriptorGeneratorMojo.generateV4DiIndex(classesDir, outputDir);

        assertFalse(indexFile.exists(), "stale index file should be removed when no @Named v4 beans remain");
    }
}
