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
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.io.Writer;

import org.apache.maven.project.MavenProject;
import org.apache.velocity.VelocityContext;
import org.codehaus.plexus.logging.console.ConsoleLogger;
import org.codehaus.plexus.util.io.CachingOutputStream;
import org.codehaus.plexus.velocity.VelocityComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Generates an <code>HelpMojo</code> class from <code>help-class-source.vm</code> template.
 * The generated mojo reads help content from <code>META-INF/maven/${groupId}/${artifactId}/plugin-help.xml</code>
 * resource, which is generated by this {@link PluginDescriptorFilesGenerator}.
 *
 * @author <a href="mailto:vincent.siveton@gmail.com">Vincent Siveton</a>
 * @since 2.4
 */
public class PluginHelpGenerator {
    private static final Logger logger = LoggerFactory.getLogger(PluginHelpGenerator.class);
    /**
     * Default generated class name
     */
    private static final String HELP_MOJO_CLASS_NAME = "HelpMojo";

    private String helpPackageName;
    private String goalPrefix;
    private MavenProject mavenProject;
    private boolean useMaven4Api;
    private VelocityComponent velocityComponent;

    /**
     * Default constructor
     */
    public PluginHelpGenerator() {
        // nop
    }

    // ----------------------------------------------------------------------
    // Public methods
    // ----------------------------------------------------------------------

    public void execute(File destinationDirectory) throws GeneratorException {
        String helpImplementation = getImplementation();

        useMaven4Api = mavenProject.getDependencies().stream()
                .anyMatch(dep ->
                        "org.apache.maven".equals(dep.getGroupId()) && "maven-api-core".equals(dep.getArtifactId()));

        try {
            String sourcePath = helpImplementation.replace('.', File.separatorChar) + ".java";

            File helpClass = new File(destinationDirectory, sourcePath);
            helpClass.getParentFile().mkdirs();

            String helpClassSources = getHelpClassSources(getPluginHelpPath(mavenProject));

            try (Writer w = new OutputStreamWriter(new CachingOutputStream(helpClass), UTF_8)) {
                w.write(helpClassSources);
            }
        } catch (IOException e) {
            throw new GeneratorException(e.getMessage(), e);
        }
    }

    public PluginHelpGenerator setHelpPackageName(String helpPackageName) {
        this.helpPackageName = helpPackageName;
        return this;
    }

    public PluginHelpGenerator setVelocityComponent(VelocityComponent velocityComponent) {
        this.velocityComponent = velocityComponent;
        return this;
    }

    public PluginHelpGenerator setGoalPrefix(String goalPrefix) {
        this.goalPrefix = goalPrefix;
        return this;
    }

    public PluginHelpGenerator setMavenProject(MavenProject mavenProject) {
        this.mavenProject = mavenProject;
        return this;
    }

    // ----------------------------------------------------------------------
    // Private methods
    // ----------------------------------------------------------------------

    private String getHelpClassSources(String pluginHelpPath) throws IOException {
        VelocityContext context = new VelocityContext();

        context.put("helpPackageName", helpPackageName);
        context.put("pluginHelpPath", pluginHelpPath);
        context.put("artifactId", mavenProject.getArtifactId());
        // TODO: evaluate prefix from deserialized plugin
        context.put("goalPrefix", goalPrefix);

        StringWriter stringWriter = new StringWriter();

        // plugin-tools sources are UTF-8 (and even ASCII in this case))
        try (InputStream is = Thread.currentThread()
                        .getContextClassLoader()
                        .getResourceAsStream(useMaven4Api ? "help-class-source-v4.vm" : "help-class-source.vm"); //
                InputStreamReader isReader = new InputStreamReader(is, UTF_8)) {
            // isReader =
            velocityComponent.getEngine().evaluate(context, stringWriter, "", isReader);
        }
        // Apply OS lineSeparator instead of template's lineSeparator to have consistent separators for
        // all source files.
        return stringWriter.toString().replaceAll("(\r\n|\n|\r)", System.lineSeparator());
    }

    /**
     * @return The implementation.
     */
    private String getImplementation() {
        return (helpPackageName == null || helpPackageName.isEmpty())
                ? HELP_MOJO_CLASS_NAME
                : helpPackageName + '.' + HELP_MOJO_CLASS_NAME;
    }

    static String getPluginHelpPath(MavenProject mavenProject) {
        return mavenProject.getGroupId() + "/" + mavenProject.getArtifactId() + "/plugin-help.xml";
    }
}
