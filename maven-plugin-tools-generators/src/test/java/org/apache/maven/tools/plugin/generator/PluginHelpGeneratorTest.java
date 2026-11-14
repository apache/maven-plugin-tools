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
package org.apache.maven.tools.plugin.generator;

import java.io.File;
import java.util.Collections;

import org.apache.maven.project.MavenProject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PluginHelpGeneratorTest {

    @Test
    void failsWithoutMojoAnnotations(@TempDir File destinationDirectory) {
        MavenProject project = new MavenProject();
        project.setArtifacts(Collections.emptySet());

        GeneratorException e = assertThrows(
                GeneratorException.class,
                () -> new PluginHelpGenerator().setMavenProject(project).execute(destinationDirectory));

        assertTrue(e.getMessage().contains("maven-plugin-annotations"));
        assertFalse(new File(destinationDirectory, "HelpMojo.java").exists());
    }
}
