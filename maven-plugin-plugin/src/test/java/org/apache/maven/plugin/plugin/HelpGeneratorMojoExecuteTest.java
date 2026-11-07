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

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.maven.api.plugin.testing.Basedir;
import org.apache.maven.api.plugin.testing.InjectMojo;
import org.apache.maven.api.plugin.testing.MojoTest;
import org.apache.maven.project.MavenProject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.apache.maven.api.plugin.testing.MojoExtension.setVariableValueToObject;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@MojoTest
class HelpGeneratorMojoExecuteTest {

    @TempDir
    private Path tempDir;

    @Inject
    private MavenProject project;

    /**
     * MPLUGIN-363: every line separator of the generated help mojo is the one of the platform, whatever the
     * separators of the template or of the values written into it are. The goal prefix is written into a comment of
     * the source, so it carries all three separator styles here.
     */
    @Test
    @InjectMojo(goal = "helpmojo", pom = "plugin-config.xml")
    @Basedir("/unit/help-reproducible")
    void helpMojoSourceUsesOnlyThePlatformLineSeparator(HelpGeneratorMojo mojo) throws Exception {
        project.setGroupId("org.apache.maven.its.plugin");
        project.setArtifactId("help");
        project.setVersion("1.0-SNAPSHOT");
        project.setPackaging("maven-plugin");
        setVariableValueToObject(mojo, "goalPrefix", "one\r\ntwo\rthree\nfour");

        Path generated = generate(mojo, "run1");

        String source = new String(Files.readAllBytes(generated), StandardCharsets.UTF_8);
        assertTrue(source.contains("package org.apache.maven.its.plugin.help;"), source);
        assertTrue(
                source.contains("one" + System.lineSeparator() + "two" + System.lineSeparator() + "three"
                        + System.lineSeparator() + "four"),
                source);
        String withoutSeparators = source.replace(System.lineSeparator(), "");
        assertFalse(withoutSeparators.contains("\r"), "stray carriage return in the generated source");
        assertFalse(withoutSeparators.contains("\n"), "stray line feed in the generated source");
    }

    private Path generate(HelpGeneratorMojo mojo, String directoryName) throws Exception {
        File outputDirectory = tempDir.resolve(directoryName).toFile();
        setVariableValueToObject(mojo, "outputDirectory", outputDirectory);
        mojo.execute();
        return outputDirectory.toPath().resolve("org/apache/maven/its/plugin/help/HelpMojo.java");
    }
}
