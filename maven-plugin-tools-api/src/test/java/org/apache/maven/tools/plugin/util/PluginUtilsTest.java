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
package org.apache.maven.tools.plugin.util;

import java.util.Collections;

import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugin.testing.stubs.MavenProjectStub;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author jdcasey
 */
class PluginUtilsTest {
    @Test
    void testShouldTrimArtifactIdToFindPluginId() {
        assertEquals("artifactId", PluginDescriptor.getGoalPrefixFromArtifactId("maven-artifactId-plugin"));
        assertEquals("artifactId", PluginDescriptor.getGoalPrefixFromArtifactId("maven-plugin-artifactId"));
        assertEquals("artifactId", PluginDescriptor.getGoalPrefixFromArtifactId("artifactId-maven-plugin"));
        assertEquals("artifactId", PluginDescriptor.getGoalPrefixFromArtifactId("artifactId"));
        assertEquals("artifactId", PluginDescriptor.getGoalPrefixFromArtifactId("artifactId-plugin"));
        assertEquals("plugin", PluginDescriptor.getGoalPrefixFromArtifactId("maven-plugin-plugin"));
    }

    @Test
    void testShouldFindTwoScriptsWhenNoExcludesAreGiven() {
        String testScript = "test.txt";

        String basedir = TestUtils.dirname(testScript);

        String includes = "**/*.txt";

        String[] files = PluginUtils.findSources(basedir, includes);
        assertEquals(2, files.length);
    }

    @Test
    void testShouldFindOneScriptsWhenAnExcludeIsGiven() {
        String testScript = "test.txt";

        String basedir = TestUtils.dirname(testScript);

        String includes = "**/*.txt";
        String excludes = "**/*Excludes.txt";

        String[] files = PluginUtils.findSources(basedir, includes, excludes);
        assertEquals(1, files.length);
    }

    @Test
    void testIsMavenReport() throws Exception {
        try {
            PluginUtils.isMavenReport(null, null);
        } catch (IllegalArgumentException e) {
            assertTrue(true);
        }

        String impl = "org.apache.maven.tools.plugin.util.stubs.MavenReportStub";

        MavenProjectStub stub = new MavenProjectStub();
        stub.setCompileSourceRoots(Collections.singletonList(System.getProperty("basedir") + "/target/classes"));

        assertTrue(PluginUtils.isMavenReport(impl, stub));

        impl = "org.apache.maven.tools.plugin.util.stubs.MojoStub";
        assertFalse(PluginUtils.isMavenReport(impl, stub));
    }
}
