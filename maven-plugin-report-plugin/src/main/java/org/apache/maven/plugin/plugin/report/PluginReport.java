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
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.apache.maven.doxia.sink.Sink;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugin.descriptor.PluginDescriptorBuilder;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Execute;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.plugin.descriptor.EnhancedPluginDescriptorBuilder;
import org.apache.maven.reporting.AbstractMavenReport;
import org.apache.maven.reporting.MavenReportException;
import org.apache.maven.rtinfo.RuntimeInformation;
import org.codehaus.plexus.configuration.PlexusConfigurationException;
import org.codehaus.plexus.i18n.I18N;
import org.codehaus.plexus.util.xml.XmlStreamReader;

/**
 * Generates the Plugin's documentation report: <code>plugin-info.html</code> plugin overview page,
 * and one <code><i>goal</i>-mojo.html</code> per goal.
 * Relies on one output file from <a href="../maven-plugin-plugin/descriptor-mojo.html">plugin:descriptor</a>.
 *
 * @author <a href="snicoll@apache.org">Stephane Nicoll</a>
 * @author <a href="mailto:vincent.siveton@gmail.com">Vincent Siveton</a>
 * @since 3.7.0
 */
@Mojo(name = "report", threadSafe = true)
@Execute(phase = LifecyclePhase.PROCESS_CLASSES)
public class PluginReport extends AbstractMavenReport {

    /**
     * Set this to "true" to skip generating the report.
     *
     * @since 3.7.0
     */
    @Parameter(defaultValue = "false", property = "maven.plugin.report.skip")
    private boolean skip;

    /**
     * Set this to "true" to generate the usage section for "plugin-info.html" with
     * {@code <extensions>true</extensions>}.
     *
     * @since 3.7.0
     */
    @Parameter(defaultValue = "false", property = "maven.plugin.report.hasExtensionsToLoad")
    private boolean hasExtensionsToLoad;

    /**
     * The Plugin requirements history list.
     * <p>
     * Can be specified as list of <code>requirementsHistory</code>:
     *
     * <pre>
     * &lt;requirementsHistories&gt;
     *   &lt;requirementsHistory&gt;
     *     &lt;version&gt;plugin version&lt;/version&gt;
     *     &lt;maven&gt;maven version&lt;/maven&gt;
     *     &lt;jdk&gt;jdk version&lt;/jdk&gt;
     *   &lt;/requirementsHistory&gt;
     * &lt;/requirementsHistories&gt;
     * </pre>
     *
     * @since 3.7.0
     */
    @Parameter
    private List<RequirementsHistory> requirementsHistories = new ArrayList<>();

    @Component
    private RuntimeInformation rtInfo;

    /**
     * Internationalization component.
     */
    @Component
    private I18N i18n;

    /**
     * Path to enhanced plugin descriptor to generate the report from (must contain some XHTML values)
     *
     * @since 3.7.0
     */
    @Parameter(defaultValue = "${project.build.directory}/plugin-enhanced.xml", required = true, readonly = true)
    private File enhancedPluginXmlFile;

    /**
     * In case the internal javadoc site has not been generated when running this report goal
     * (e.g. when using an aggregator javadoc report) link validation needs to be disabled by setting
     * this value to {@code true}.
     * This might have the drawback that some links being generated in the report might be broken
     * in case not all parameter types and javadoc link references are resolvable through the sites being given to
     * goal {@code plugin:descriptor}.
     *
     * @since 3.7.0
     */
    @Parameter(property = "maven.plugin.report.disableInternalJavadocLinkValidation")
    private boolean disableInternalJavadocLinkValidation;

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean canGenerateReport() {
        return enhancedPluginXmlFile != null && enhancedPluginXmlFile.isFile() && enhancedPluginXmlFile.canRead();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void executeReport(Locale locale) throws MavenReportException {
        if (skip) {
            getLog().info("Maven Plugin Plugin Report generation skipped.");
            return;
        }

        PluginDescriptor pluginDescriptor = extractPluginDescriptor();

        // Generate the mojos' documentation
        generateMojosDocumentation(pluginDescriptor, locale);

        // Write the overview
        PluginOverviewRenderer r = new PluginOverviewRenderer(
                getSink(), i18n, locale, getProject(), requirementsHistories, pluginDescriptor, hasExtensionsToLoad);
        r.render();
    }

    private PluginDescriptor extractPluginDescriptor() throws MavenReportException {
        PluginDescriptorBuilder builder = new EnhancedPluginDescriptorBuilder(rtInfo);

        try (Reader input = new XmlStreamReader(Files.newInputStream(enhancedPluginXmlFile.toPath()))) {
            return builder.build(input);
        } catch (IOException | PlexusConfigurationException e) {
            throw new MavenReportException("Error extracting plugin descriptor from " + enhancedPluginXmlFile, e);
        }
    }

    /**
     * @param locale The locale
     * @param key The key to search for
     * @return The text appropriate for the locale.
     */
    private String getI18nString(Locale locale, String key) {
        return i18n.getString("plugin-report", locale, "report.plugin." + key);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName(Locale locale) {
        return getI18nString(locale, "name");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDescription(Locale locale) {
        return getI18nString(locale, "description");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getOutputName() {
        return "plugin-info";
    }

    /**
     * Generate the mojos' documentation with the {@link #getSinkFactory()}
     *
     * @param pluginDescriptor not null
     * @param locale           not null
     * @throws MavenReportException if any
     * @throws IOException
     */
    private void generateMojosDocumentation(PluginDescriptor pluginDescriptor, Locale locale)
            throws MavenReportException {

        if (pluginDescriptor.getMojos() != null) {
            for (MojoDescriptor descriptor : pluginDescriptor.getMojos()) {
                GoalRenderer renderer;
                try {
                    String filename = descriptor.getGoal() + "-mojo.html";
                    Sink sink = getSinkFactory().createSink(outputDirectory, filename);
                    renderer = new GoalRenderer(
                            sink,
                            i18n,
                            locale,
                            project,
                            descriptor,
                            outputDirectory,
                            disableInternalJavadocLinkValidation,
                            getLog());
                } catch (IOException e) {
                    throw new MavenReportException("Can not generate sink for mojo " + descriptor.getGoal(), e);
                }
                renderer.render();
            }
        }
    }
}
