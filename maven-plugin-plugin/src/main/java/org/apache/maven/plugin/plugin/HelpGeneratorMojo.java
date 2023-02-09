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

import javax.lang.model.SourceVersion;

import java.io.File;
import java.util.Arrays;
import java.util.stream.Collectors;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.tools.plugin.generator.GeneratorException;
import org.apache.maven.tools.plugin.generator.PluginHelpGenerator;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.velocity.VelocityComponent;

/**
 * Generates a <code>HelpMojo</code> class.
 * Relies at runtime on one output file from {@link DescriptorGeneratorMojo}.
 *
 * @author <a href="mailto:vincent.siveton@gmail.com">Vincent Siveton</a>
 * @since 2.4
 */
@Mojo(
        name = "helpmojo",
        defaultPhase = LifecyclePhase.GENERATE_SOURCES,
        threadSafe = true,
        requiresDependencyResolution = ResolutionScope.COMPILE)
public class HelpGeneratorMojo extends AbstractGeneratorMojo {
    /**
     * The directory where the generated <code>HelpMojo</code> file will be put.
     */
    @Parameter(defaultValue = "${project.build.directory}/generated-sources/plugin")
    protected File outputDirectory;

    /**
     * The name of the package for the generated <code>HelpMojo</code>.
     * <p>
     * By default, the package name will be calculated as <code>groupId + "." + artifactId</code> with additional
     * <ul>
     * <li><code>-</code> (dashes) will be replaced by <code>_</code> (underscores)</li>
     * <li><code>_</code> (underscore) will be added before each number or Java keyword at the beginning of name</li>
     * </ul>
     *
     * @since 2.6
     */
    @Parameter
    private String helpPackageName;

    /**
     * Velocity component.
     */
    @Component
    private VelocityComponent velocity;

    String getHelpPackageName() {
        String packageName = null;
        if (StringUtils.isNotBlank(helpPackageName)) {
            packageName = helpPackageName;
        }

        if (packageName == null) {
            packageName = project.getGroupId() + "." + project.getArtifactId();
            packageName = packageName.replace("-", "_");

            String[] packageItems = packageName.split("\\.");
            packageName =
                    Arrays.stream(packageItems).map(this::prefixSpecialCase).collect(Collectors.joining("."));
        }

        return packageName;
    }

    private String prefixSpecialCase(String name) {
        if (SourceVersion.isKeyword(name) || !Character.isJavaIdentifierStart(name.charAt(0))) {
            name = "_" + name;
        }
        return name;
    }

    @Override
    protected void generate() throws MojoExecutionException {
        PluginHelpGenerator pluginHelpGenerator = new PluginHelpGenerator()
                .setMavenProject(project)
                .setHelpPackageName(getHelpPackageName())
                .setGoalPrefix(goalPrefix)
                .setVelocityComponent(velocity);

        try {
            pluginHelpGenerator.execute(outputDirectory);
        } catch (GeneratorException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }

        if (!project.getCompileSourceRoots().contains(outputDirectory.getAbsolutePath())) {
            project.addCompileSourceRoot(outputDirectory.getAbsolutePath());
        }
    }
}
