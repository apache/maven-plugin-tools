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

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.Prerequisites;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.project.MavenProject;
import org.apache.maven.tools.plugin.ExtendedPluginDescriptor;
import org.codehaus.plexus.util.xml.Xpp3Dom;

/**
 * Plugin requirements history.
 *
 * @author Slawomir Jaranowski
 */
public class RequirementsHistory {
    /**
     * The plugin version.
     */
    private String version;

    /**
     * The minimum version of Maven to run this plugin.
     */
    private String maven;

    /**
     * The minimum version of the JDK to run this plugin.
     */
    private String jdk;

    public String getVersion() {
        return version;
    }

    public String getMaven() {
        return maven;
    }

    public String getJdk() {
        return jdk;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("RequirementsHistory{");
        sb.append("version='").append(version).append('\'');
        sb.append(", maven='").append(maven).append('\'');
        sb.append(", jdk='").append(jdk).append('\'');
        sb.append('}');
        return sb.toString();
    }

    public static RequirementsHistory discoverRequirements(MavenProject project) {
        RequirementsHistory req = new RequirementsHistory();
        req.version = project.getVersion();
        req.jdk = discoverJdkRequirement(project, null);
        req.maven = discoverMavenRequirement(project, null);
        return req;
    }
    /**
     * Tries to determine the Maven requirement from either the plugin descriptor or (if not set) from the
     * Maven prerequisites element in the POM.
     *
     * @param project      not null
     * @param pluginDescriptor the plugin descriptor (can be null)
     * @return the Maven version or null if not specified
     */
    public static String discoverMavenRequirement(MavenProject project, PluginDescriptor pluginDescriptor) {
        if (pluginDescriptor != null && StringUtils.isNotBlank(pluginDescriptor.getRequiredMavenVersion())) {
            return pluginDescriptor.getRequiredMavenVersion();
        }
        return Optional.ofNullable(project.getPrerequisites())
                .map(Prerequisites::getMaven)
                .orElse(null);
    }

    /**
     * Tries to determine the JDK requirement from the following sources (until one is found)
     * <ol>
     * <li>use JDK requirement from plugin descriptor</li>
     * <li>use {@code release} configuration of {@code org.apache.maven.plugins:maven-compiler-plugin}</li>
     * <li>use {@code maven.compiler.release<} property</li>
     * <li>use {@code target} configuration of {@code org.apache.maven.plugins:maven-compiler-plugin}</li>
     * <li>use {@code maven.compiler.target} property</li>
     * </ol>
     *
     * @param project      not null
     * @param pluginDescriptor the plugin descriptor (can be null)
     * @return the JDK version
     */
    public static String discoverJdkRequirement(MavenProject project, PluginDescriptor pluginDescriptor) {
        String jdk = null;
        if (pluginDescriptor instanceof ExtendedPluginDescriptor) {
            ExtendedPluginDescriptor extPluginDescriptor = (ExtendedPluginDescriptor) pluginDescriptor;
            jdk = extPluginDescriptor.getRequiredJavaVersion();
        }
        if (jdk != null) {
            return jdk;
        }
        Plugin compiler = getCompilerPlugin(project.getBuild().getPluginsAsMap());
        if (compiler == null) {
            compiler = getCompilerPlugin(project.getPluginManagement().getPluginsAsMap());
        }

        jdk = getPluginParameter(compiler, "release");
        if (jdk == null) {
            jdk = project.getProperties().getProperty("maven.compiler.release");
        }

        if (jdk == null) {
            jdk = getPluginParameter(compiler, "target");
        }

        if (jdk == null) {
            // default value
            jdk = project.getProperties().getProperty("maven.compiler.target");
        }

        if (jdk == null) {
            String version = (compiler == null) ? null : compiler.getVersion();

            if (version != null) {
                return "Default target for maven-compiler-plugin version " + version;
            }
        } else {
            if (Arrays.asList("1.5", "1.6", "1.7", "1.8").contains(jdk)) {
                jdk = jdk.substring(2);
            }
        }

        return jdk;
    }

    private static Plugin getCompilerPlugin(Map<String, Plugin> pluginsAsMap) {
        return pluginsAsMap.get("org.apache.maven.plugins:maven-compiler-plugin");
    }

    private static String getPluginParameter(Plugin plugin, String parameter) {
        if (plugin != null) {
            Xpp3Dom pluginConf = (Xpp3Dom) plugin.getConfiguration();

            if (pluginConf != null) {
                Xpp3Dom target = pluginConf.getChild(parameter);

                if (target != null) {
                    return target.getValue();
                }
            }
        }

        return null;
    }
}
