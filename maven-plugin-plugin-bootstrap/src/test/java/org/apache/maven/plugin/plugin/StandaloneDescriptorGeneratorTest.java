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
import java.nio.file.Files;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.maven.model.Build;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
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

        org.apache.maven.model.Plugin plugin = new org.apache.maven.model.Plugin();
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

        org.apache.maven.model.PluginManagement pluginManagement = new org.apache.maven.model.PluginManagement();
        build.setPluginManagement(pluginManagement);

        org.apache.maven.model.Plugin plugin = new org.apache.maven.model.Plugin();
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
    void testInterpolate() {
        Map<String, String> properties = new HashMap<>();
        properties.put("foo", "bar");
        properties.put("project.version", "1.2.3");

        assertEquals("bar", StandaloneDescriptorGenerator.interpolate("${foo}", properties));
        assertEquals("v1.2.3!", StandaloneDescriptorGenerator.interpolate("v${project.version}!", properties));
        assertEquals("${unknown}", StandaloneDescriptorGenerator.interpolate("${unknown}", properties));
        assertNull(StandaloneDescriptorGenerator.interpolate(null, properties));
        assertEquals("plain", StandaloneDescriptorGenerator.interpolate("plain", properties));
    }

    @Test
    void testGatherPomChainInfoWalksLocalParentChain() throws Exception {
        File tempRoot = Files.createTempDirectory("parent-chain").toFile();
        tempRoot.deleteOnExit();

        File parentDir = new File(tempRoot, "parent");
        parentDir.mkdirs();
        File parentPom = new File(parentDir, "pom.xml");
        Files.write(
                parentPom.toPath(),
                ("<project><modelVersion>4.0.0</modelVersion>"
                                + "<groupId>test</groupId><artifactId>parent</artifactId><version>1.0</version>"
                                + "<packaging>pom</packaging>"
                                + "<properties><foo>bar</foo><shared>from-parent</shared></properties>"
                                + "</project>")
                        .getBytes());

        File childDir = new File(tempRoot, "child");
        childDir.mkdirs();
        File childPom = new File(childDir, "pom.xml");
        Files.write(
                childPom.toPath(),
                ("<project><modelVersion>4.0.0</modelVersion>"
                                + "<parent><groupId>test</groupId><artifactId>parent</artifactId>"
                                + "<version>1.0</version><relativePath>../parent/pom.xml</relativePath></parent>"
                                + "<artifactId>child</artifactId>"
                                + "<properties><baz>qux</baz><shared>from-child</shared></properties>"
                                + "</project>")
                        .getBytes());

        MavenXpp3Reader reader = new MavenXpp3Reader();
        Model childModel;
        try (InputStream is = Files.newInputStream(childPom.toPath())) {
            childModel = reader.read(is);
        }

        Map<String, String> properties = new HashMap<>();
        Map<String, String> managedVersions = new HashMap<>();
        StandaloneDescriptorGenerator.gatherPomChainInfo(childPom, childModel, properties, managedVersions);
        assertEquals("qux", properties.get("baz"));
        assertEquals("bar", properties.get("foo"));
        assertEquals("from-child", properties.get("shared"));
    }

    @Test
    void testBuildScanArtifactsIncludesUnmatchedClasspathEntries() throws Exception {
        String originalClasspath = System.getProperty("java.class.path");
        try {
            File tempDir = Files.createTempDirectory("scan-artifacts").toFile();
            tempDir.deleteOnExit();
            File unmatchedJar = new File(tempDir, "somelib.jar");
            unmatchedJar.createNewFile();

            System.setProperty("java.class.path", unmatchedJar.getAbsolutePath());

            org.apache.maven.artifact.handler.ArtifactHandler handler =
                    new org.apache.maven.artifact.handler.DefaultArtifactHandler("jar");
            org.apache.maven.artifact.Artifact declaredArt = new org.apache.maven.artifact.DefaultArtifact(
                    "some.group", "somelib", "1.0", "compile", "jar", "", handler);
            declaredArt.setFile(new File("dummy.jar"));
            Set<org.apache.maven.artifact.Artifact> declared = new HashSet<>();
            declared.add(declaredArt);

            Set<org.apache.maven.artifact.Artifact> scanArtifacts =
                    StandaloneDescriptorGenerator.buildScanArtifacts(declared);

            assertTrue(
                    scanArtifacts.stream()
                            .anyMatch(a -> unmatchedJar.getAbsoluteFile().equals(a.getFile())),
                    "the real classpath jar should be included for scanning even though it wasn't "
                            + "name-matched to the declared dependency");
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

        StandaloneDescriptorGenerator.generateV4DiIndex(classesDir, outputDir);

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

        StandaloneDescriptorGenerator.generateV4DiIndex(classesDir, outputDir);

        assertFalse(indexFile.exists(), "stale index file should be removed when no @Named v4 beans remain");
    }
}
