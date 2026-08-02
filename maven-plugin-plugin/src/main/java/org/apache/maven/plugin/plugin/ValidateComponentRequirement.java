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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.plugin.descriptor.Parameter;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugin.logging.Log;
import org.codehaus.plexus.component.repository.ComponentRequirement;

/**
 * Validate if Mojo has a component requirement - uses a {@code @Component}
 */
class ValidateComponentRequirement {

    public void validate(PluginDescriptor pluginDescriptor, Log logger) {

        if (pluginDescriptor == null || pluginDescriptor.getMojos() == null) {
            return;
        }

        boolean hasComponentRequirement = false;

        for (MojoDescriptor mojo : pluginDescriptor.getMojos()) {

            if (!"java".equalsIgnoreCase(mojo.getLanguage())) {
                // only check java mojos
                continue;
            }

            List<String> fieldsWithRequirements = getFieldsWithRequirements(mojo);
            if (!fieldsWithRequirements.isEmpty()) {
                hasComponentRequirement = true;
                logger.warn(String.format(
                        "Mojo %s uses Plexus Component requirements (@Component annotation) for fields: %s",
                        mojo.getGoal(), fieldsWithRequirements));
            }
        }

        if (hasComponentRequirement) {
            logger.warn("Use JSR 330 annotations to inject dependencies instead.");
        }
    }

    private List<String> getFieldsWithRequirements(MojoDescriptor mojo) {
        List<String> list = new ArrayList<>();
        // we map @Componetnt annotation to Mojo parameter with requirement
        Optional.ofNullable(mojo.getParameters())
                .map(Collection::stream)
                .orElseGet(Stream::empty)
                .filter(parameter -> parameter.getRequirement() != null)
                .map(Parameter::getName)
                .forEach(list::add);

        // check also the requirements field
        Optional.ofNullable(mojo.getRequirements())
                .map(Collection::stream)
                .orElseGet(Stream::empty)
                .map(ComponentRequirement::getFieldName)
                .forEach(list::add);
        return list;
    }
}
