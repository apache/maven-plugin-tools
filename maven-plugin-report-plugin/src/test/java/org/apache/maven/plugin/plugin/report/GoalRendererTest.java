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
package org.apache.maven.plugin.plugin.report;

import java.io.File;
import java.util.Locale;

import org.apache.maven.plugin.logging.SystemStreamLog;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GoalRendererTest {

    @Test
    void getShortType() {
        assertEquals("String", GoalRenderer.getShortType("java.lang.String"));
        assertEquals("List<String>", GoalRenderer.getShortType("java.util.List<java.lang.String>"));
        assertEquals(
                "Map<String,Integer>", GoalRenderer.getShortType("java.util.Map<java.lang.String,java.lang.Integer>"));
        assertEquals("List<...>", GoalRenderer.getShortType("java.util.List<java.util.List<java.lang.String>>"));
    }

    @Test
    void getXhtmlWithValidatedLinks() {
        File baseDir = new File(this.getClass().getResource("").getFile());
        GoalRenderer renderer =
                new GoalRenderer(null, null, Locale.ROOT, null, null, baseDir, false, new SystemStreamLog());
        GoalRenderer rendererWithDisabledLinkValidator =
                new GoalRenderer(null, null, Locale.ROOT, null, null, baseDir, true, new SystemStreamLog());
        String externalLink =
                "test<a href=\"http://example.com/test\">External Link</a>..and a second<a href=\"http://localhost/example\">link</a>end";
        assertEquals(externalLink, renderer.getXhtmlWithValidatedLinks(externalLink, "test"));
        assertEquals(externalLink, renderer.getXhtmlWithValidatedLinks(externalLink, "test"));
        String validInternalLink =
                "test<a href=\"GoalRendererTest.class\">Internal Link</a>..and a second<a href=\"http://localhost/example\">link</a>end";
        assertEquals(validInternalLink, renderer.getXhtmlWithValidatedLinks(validInternalLink, "test"));
        assertEquals(validInternalLink, renderer.getXhtmlWithValidatedLinks(validInternalLink, "test"));
        String invalidInternalLink =
                "test<a href=\"PluginXdocGeneratorTestinvalid.class\">Internal Link</a>..and a second<a href=\"http://localhost/example\">link</a>end";
        String sanitizedInvalidInternalLink =
                "testInternal Link..and a second<a href=\"http://localhost/example\">link</a>end";
        assertEquals(sanitizedInvalidInternalLink, renderer.getXhtmlWithValidatedLinks(invalidInternalLink, "test"));
        assertEquals(
                invalidInternalLink,
                rendererWithDisabledLinkValidator.getXhtmlWithValidatedLinks(invalidInternalLink, "test"));
    }
}
