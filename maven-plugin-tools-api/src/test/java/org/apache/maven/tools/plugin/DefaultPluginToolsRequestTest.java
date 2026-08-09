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
package org.apache.maven.tools.plugin;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;

import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.project.MavenProject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DefaultPluginToolsRequestTest {

    private static final Path GENERATED_SOURCES =
            Paths.get("project", "target", "generated-sources", "annotations").toAbsolutePath();

    private PluginToolsRequest newRequest() {
        return new DefaultPluginToolsRequest(new MavenProject(), new PluginDescriptor());
    }

    @Test
    void aConfiguredSourceRootIsExcluded() {
        PluginToolsRequest request = newRequest();
        request.setExcludedScanDirectories(Collections.singleton(GENERATED_SOURCES.toString()));

        assertTrue(request.isExcludedScanDirectory(GENERATED_SOURCES.toFile()));
    }

    @Test
    void aTrailingSeparatorStillExcludes() {
        PluginToolsRequest request = newRequest();
        request.setExcludedScanDirectories(Collections.singleton(
                GENERATED_SOURCES + GENERATED_SOURCES.getFileSystem().getSeparator()));

        assertTrue(request.isExcludedScanDirectory(GENERATED_SOURCES.toFile()));
    }

    @Test
    void otherSourceRootsAreNotExcluded() {
        PluginToolsRequest request = newRequest();
        request.setExcludedScanDirectories(Collections.singleton(GENERATED_SOURCES.toString()));

        assertFalse(request.isExcludedScanDirectory(
                Paths.get("project", "src", "main", "java").toAbsolutePath().toFile()));
    }

    @Test
    void aSiblingOfAnExcludedDirectoryIsNotExcluded() {
        PluginToolsRequest request = newRequest();
        request.setExcludedScanDirectories(Collections.singleton(GENERATED_SOURCES.toString()));

        assertFalse(request.isExcludedScanDirectory(
                Paths.get(GENERATED_SOURCES + "2").toFile()));
    }

    @Test
    void anyOfTheConfiguredDirectoriesExcludes() {
        PluginToolsRequest request = newRequest();
        request.setExcludedScanDirectories(Arrays.asList(
                Paths.get("project", "target", "generated-test-sources", "annotations")
                        .toAbsolutePath()
                        .toString(),
                GENERATED_SOURCES.toString()));

        assertTrue(request.isExcludedScanDirectory(GENERATED_SOURCES.toFile()));
    }

    @Test
    void nothingIsExcludedByDefault() {
        assertFalse(newRequest().isExcludedScanDirectory(GENERATED_SOURCES.toFile()));
    }

    @Test
    void anEmptyEntryExcludesNothing() {
        PluginToolsRequest request = newRequest();
        request.setExcludedScanDirectories(Collections.singleton(""));

        // an empty entry resolves to the working directory, which must not exclude anything
        assertFalse(
                request.isExcludedScanDirectory(Paths.get("").toAbsolutePath().toFile()));
    }

    /**
     * The regression this class was written for: a configured directory built from an interpolated property
     * such as {@code ${project.build.directory}} holds Windows separators on Windows, and a backslash is the
     * escape character of the glob syntax. Left as-is it is swallowed, so the pattern could never match and
     * the exclusion silently did nothing.
     */
    @Test
    void windowsSeparatorsBecomeGlobSeparators() {
        assertEquals(
                "C:/project/target/generated-sources/annotations",
                DefaultPluginToolsRequest.toGlobPattern("C:\\project\\target\\generated-sources\\annotations", true));
    }

    @Test
    void windowsSeparatorsAreTranslatedWithoutDisturbingWildcards() {
        assertEquals(
                "C:/project/target/generated-sources/*",
                DefaultPluginToolsRequest.toGlobPattern("C:\\project\\target\\generated-sources\\*", true));
    }

    @Test
    void aTrailingWindowsSeparatorNoLongerBreaksThePattern() {
        // a glob may not end in an escape character, which used to fail the build with a
        // PatternSyntaxException rather than merely failing to match
        assertEquals("C:/project/target/", DefaultPluginToolsRequest.toGlobPattern("C:\\project\\target\\", true));
    }

    @Test
    void backslashesAreLeftAloneWhereTheyAreNotSeparators() {
        // on such a file system a backslash is a legal name character and a deliberate glob escape
        assertEquals(
                "/project/target/generated-sources/\\*",
                DefaultPluginToolsRequest.toGlobPattern("/project/target/generated-sources/\\*", false));
    }

    @Test
    void globsStillMatch() {
        PluginToolsRequest request = newRequest();
        request.setExcludedScanDirectories(
                Collections.singleton(GENERATED_SOURCES.getParent().resolve("*").toString()));

        assertTrue(request.isExcludedScanDirectory(GENERATED_SOURCES.toFile()));
    }
}
