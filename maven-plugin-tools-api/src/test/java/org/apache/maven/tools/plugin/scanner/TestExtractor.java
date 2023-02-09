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
package org.apache.maven.tools.plugin.scanner;

import java.util.Collections;
import java.util.List;

import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.project.MavenProject;
import org.apache.maven.tools.plugin.DefaultPluginToolsRequest;
import org.apache.maven.tools.plugin.PluginToolsRequest;
import org.apache.maven.tools.plugin.extractor.GroupKey;
import org.apache.maven.tools.plugin.extractor.MojoDescriptorExtractor;

/**
 * @author jdcasey
 */
public class TestExtractor implements MojoDescriptorExtractor {
    private static final GroupKey GROUP_KEY = new GroupKey("test", 100);

    @Override
    public String getName() {
        return "test";
    }

    @Override
    public boolean isDeprecated() {
        return false;
    }

    @Override
    public GroupKey getGroupKey() {
        return GROUP_KEY;
    }

    public List<MojoDescriptor> execute(MavenProject project, PluginDescriptor pluginDescriptor) {
        return execute(new DefaultPluginToolsRequest(project, pluginDescriptor));
    }

    @Override
    public List<MojoDescriptor> execute(PluginToolsRequest request) {
        MojoDescriptor desc = new MojoDescriptor();
        desc.setPluginDescriptor(request.getPluginDescriptor());
        desc.setGoal("testGoal");

        return Collections.singletonList(desc);
    }
}
