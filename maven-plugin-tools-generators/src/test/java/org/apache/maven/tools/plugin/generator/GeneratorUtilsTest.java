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

import java.io.StringWriter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.codehaus.plexus.component.repository.ComponentDependency;
import org.codehaus.plexus.util.xml.CompactXMLWriter;
import org.codehaus.plexus.util.xml.XMLWriter;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author jdcasey
 */
class GeneratorUtilsTest {
    @Test
    void testShouldWriteDependencies() throws Exception {
        ComponentDependency dependency = new ComponentDependency();
        dependency.setArtifactId("testArtifactId");
        dependency.setGroupId("testGroupId");
        dependency.setType("pom");
        dependency.setVersion("0.0.0");

        PluginDescriptor descriptor = new PluginDescriptor();
        descriptor.setDependencies(Collections.singletonList(dependency));

        StringWriter sWriter = new StringWriter();
        XMLWriter writer = new CompactXMLWriter(sWriter);

        GeneratorUtils.writeDependencies(writer, descriptor);

        String output = sWriter.toString();

        String pattern = "<dependencies>" + "<dependency>" + "<groupId>testGroupId</groupId>"
                + "<artifactId>testArtifactId</artifactId>" + "<type>pom</type>" + "<version>0.0.0</version>"
                + "</dependency>" + "</dependencies>";

        assertEquals(pattern, output);
    }

    @Test
    void testExcludeProvidedScopeFormComponentDependencies() {

        Artifact a1 = new DefaultArtifact("g", "a1", "1.0", Artifact.SCOPE_COMPILE, "jar", "", null);
        Artifact a2 = new DefaultArtifact("g", "a2", "2.0", Artifact.SCOPE_PROVIDED, "jar", "", null);
        Artifact a3 = new DefaultArtifact("g", "a3", "3.0", Artifact.SCOPE_RUNTIME, "jar", "", null);
        List<Artifact> depList = Arrays.asList(a1, a2, a3);

        List<ComponentDependency> componentDependencies = GeneratorUtils.toComponentDependencies(depList);

        assertEquals(2, componentDependencies.size());

        ComponentDependency componentDependency1 = componentDependencies.get(0);
        assertEquals(a1.getGroupId(), componentDependency1.getGroupId());
        assertEquals(a1.getArtifactId(), componentDependency1.getArtifactId());
        assertEquals(a1.getVersion(), componentDependency1.getVersion());
        assertEquals(a1.getType(), componentDependency1.getType());

        ComponentDependency componentDependency2 = componentDependencies.get(1);
        assertEquals(a3.getGroupId(), componentDependency2.getGroupId());
        assertEquals(a3.getArtifactId(), componentDependency2.getArtifactId());
        assertEquals(a3.getVersion(), componentDependency2.getVersion());
        assertEquals(a3.getType(), componentDependency2.getType());
    }
}
