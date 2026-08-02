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

import java.util.Collections;

import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.plugin.descriptor.Parameter;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugin.descriptor.Requirement;
import org.apache.maven.plugin.logging.Log;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;

class ValidateComponentRequirementTest {

    @Test
    void warnsForJavaMojoUsingComponentRequirements() throws Exception {
        PluginDescriptor pluginDescriptor = new PluginDescriptor();
        pluginDescriptor.addMojo(aJavaMojoWithRequirement("test-goal", "componentField"));
        Log logger = mock(Log.class);

        new ValidateComponentRequirement().validate(pluginDescriptor, logger);

        InOrder inOrder = inOrder(logger);
        inOrder.verify(logger)
                .warn("Mojo test-goal uses Plexus Component requirements (@Component annotation) for fields: "
                        + "[componentField]");
        inOrder.verify(logger).warn("Use JSR 330 annotations to inject dependencies instead.");
        verifyNoMoreInteractions(logger);
    }

    @Test
    void ignoresNonJavaMojo() throws Exception {
        PluginDescriptor pluginDescriptor = new PluginDescriptor();
        pluginDescriptor.addMojo(aNonJavaMojoWithRequirement("test-goal", "componentField"));
        Log logger = mock(Log.class);

        new ValidateComponentRequirement().validate(pluginDescriptor, logger);

        verifyNoInteractions(logger);
    }

    @Test
    void ignoresJavaMojoWithoutRequirement() throws Exception {
        PluginDescriptor pluginDescriptor = new PluginDescriptor();
        pluginDescriptor.addMojo(aJavaMojoWithoutRequirement("test-goal", "regularField"));
        Log logger = mock(Log.class);

        new ValidateComponentRequirement().validate(pluginDescriptor, logger);

        verifyNoInteractions(logger);
    }

    private static MojoDescriptor aJavaMojoWithRequirement(String goal, String parameterName) throws Exception {
        MojoDescriptor mojo = new MojoDescriptor();
        mojo.setGoal(goal);
        mojo.setLanguage("java");
        mojo.setParameters(Collections.singletonList(aRequiredParameter(parameterName)));
        return mojo;
    }

    private static MojoDescriptor aNonJavaMojoWithRequirement(String goal, String parameterName) throws Exception {
        MojoDescriptor mojo = aJavaMojoWithRequirement(goal, parameterName);
        mojo.setLanguage("bsh");
        return mojo;
    }

    private static MojoDescriptor aJavaMojoWithoutRequirement(String goal, String parameterName) throws Exception {
        MojoDescriptor mojo = new MojoDescriptor();
        mojo.setGoal(goal);
        mojo.setLanguage("java");
        mojo.setParameters(Collections.singletonList(aParameterWithoutRequirement(parameterName)));
        return mojo;
    }

    private static Parameter aRequiredParameter(String name) {
        Parameter parameter = new Parameter();
        parameter.setName(name);
        parameter.setRequirement(new Requirement("org.example.Component", "default"));
        return parameter;
    }

    private static Parameter aParameterWithoutRequirement(String name) {
        Parameter parameter = new Parameter();
        parameter.setName(name);
        return parameter;
    }
}
