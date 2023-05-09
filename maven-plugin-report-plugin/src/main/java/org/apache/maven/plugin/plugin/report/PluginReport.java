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
import java.util.Map;
import java.util.Optional;

import org.apache.maven.doxia.markup.Markup;
import org.apache.maven.doxia.sink.Sink;
import org.apache.maven.doxia.tools.SiteTool;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.Prerequisites;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugin.descriptor.PluginDescriptorBuilder;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Execute;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.plugin.descriptor.EnhancedPluginDescriptorBuilder;
import org.apache.maven.project.MavenProject;
import org.apache.maven.reporting.AbstractMavenReport;
import org.apache.maven.reporting.AbstractMavenReportRenderer;
import org.apache.maven.reporting.MavenReportException;
import org.apache.maven.rtinfo.RuntimeInformation;
import org.apache.maven.tools.plugin.DefaultPluginToolsRequest;
import org.apache.maven.tools.plugin.ExtendedPluginDescriptor;
import org.apache.maven.tools.plugin.PluginToolsRequest;
import org.apache.maven.tools.plugin.generator.GeneratorException;
import org.apache.maven.tools.plugin.generator.GeneratorUtils;
import org.apache.maven.tools.plugin.generator.PluginXdocGenerator;
import org.apache.maven.tools.plugin.util.PluginUtils;
import org.codehaus.plexus.configuration.PlexusConfigurationException;
import org.codehaus.plexus.i18n.I18N;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.xml.XmlStreamReader;
import org.codehaus.plexus.util.xml.Xpp3Dom;

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
     * Report output directory for mojos' documentation.
     *
     * @since 3.7.0
     */
    @Parameter(defaultValue = "${project.build.directory}/generated-site")
    private File generatedSiteDirectory;

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
    protected String getOutputDirectory() {
        // PLUGIN-191: output directory of plugin.html, not *-mojo.xml
        return project.getReporting().getOutputDirectory();
    }

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
     * Generate the mojos documentation, as xdoc files.
     *
     * @param pluginDescriptor not null
     * @param locale           not null
     * @throws MavenReportException if any
     */
    private void generateMojosDocumentation(PluginDescriptor pluginDescriptor, Locale locale)
            throws MavenReportException {
        try {
            File outputDir;
            if (!locale.equals(SiteTool.DEFAULT_LOCALE)) {
                outputDir = new File(new File(generatedSiteDirectory, locale.toString()), "xdoc");
            } else {
                outputDir = new File(generatedSiteDirectory, "xdoc");
            }
            outputDir.mkdirs();

            PluginXdocGenerator generator = new PluginXdocGenerator(
                    getProject(), locale, getReportOutputDirectory(), disableInternalJavadocLinkValidation);
            PluginToolsRequest pluginToolsRequest = new DefaultPluginToolsRequest(getProject(), pluginDescriptor);
            generator.execute(outputDir, pluginToolsRequest);
        } catch (GeneratorException e) {
            throw new MavenReportException("Error writing plugin documentation", e);
        }
    }

    /**
     * Generates an overview page with the list of goals
     * and a link to the goal's page.
     */
    static class PluginOverviewRenderer extends AbstractMavenReportRenderer {
        private final I18N i18n;

        private final Locale locale;

        private final MavenProject project;

        private final List<RequirementsHistory> requirementsHistories;

        private final PluginDescriptor pluginDescriptor;

        private final boolean hasExtensionsToLoad;

        /**
         * @param sink                  not null
         * @param i18n                  not null
         * @param locale                not null
         * @param project               not null
         * @param requirementsHistories not null
         * @param pluginDescriptor      not null
         */
        PluginOverviewRenderer(
                Sink sink,
                I18N i18n,
                Locale locale,
                MavenProject project,
                List<RequirementsHistory> requirementsHistories,
                PluginDescriptor pluginDescriptor,
                boolean hasExtensionsToLoad) {
            super(sink);

            this.i18n = i18n;

            this.locale = locale;

            this.project = project;

            this.requirementsHistories = requirementsHistories;

            this.pluginDescriptor = pluginDescriptor;

            this.hasExtensionsToLoad = hasExtensionsToLoad;
        }

        @Override
        public String getTitle() {
            return getI18nString("title");
        }

        /**
         * @param key The key.
         * @return The translated string.
         */
        protected String getI18nString(String key) {
            return i18n.getString("plugin-report", locale, "report.plugin." + key);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void renderBody() {
            startSection(getTitle());

            if (!(pluginDescriptor.getMojos() != null
                    && pluginDescriptor.getMojos().size() > 0)) {
                paragraph(getI18nString("goals.nogoal"));
                endSection();
                return;
            }

            paragraph(getI18nString("goals.intro"));

            boolean hasMavenReport = false;
            for (MojoDescriptor mojo : pluginDescriptor.getMojos()) {
                if (GeneratorUtils.isMavenReport(mojo.getImplementation(), project)) {
                    hasMavenReport = true;
                }
            }

            startTable();

            String goalColumnName = getI18nString("goals.column.goal");
            String isMavenReport = getI18nString("goals.column.isMavenReport");
            String descriptionColumnName = getI18nString("goals.column.description");
            if (hasMavenReport) {
                tableHeader(new String[] {goalColumnName, isMavenReport, descriptionColumnName});
            } else {
                tableHeader(new String[] {goalColumnName, descriptionColumnName});
            }

            List<MojoDescriptor> mojos = new ArrayList<>();
            mojos.addAll(pluginDescriptor.getMojos());
            PluginUtils.sortMojos(mojos);
            for (MojoDescriptor mojo : mojos) {
                String goalName = mojo.getFullGoalName();

                /*
                 * Added ./ to define a relative path
                 * @see AbstractMavenReportRenderer#getValidHref(java.lang.String)
                 */
                String goalDocumentationLink = "./" + mojo.getGoal() + "-mojo.html";

                String description;
                if (StringUtils.isNotEmpty(mojo.getDeprecated())) {
                    description = "<strong>" + getI18nString("goal.deprecated") + "</strong> " + mojo.getDeprecated();
                } else if (StringUtils.isNotEmpty(mojo.getDescription())) {
                    description = mojo.getDescription();
                } else {
                    description = getI18nString("goal.nodescription");
                }

                sink.tableRow();
                tableCell(createLinkPatternedText(goalName, goalDocumentationLink));
                if (hasMavenReport) {
                    if (GeneratorUtils.isMavenReport(mojo.getImplementation(), project)) {
                        tableCell(getI18nString("isReport"));
                    } else {
                        tableCell(getI18nString("isNotReport"));
                    }
                }
                tableCell(description, true);
                sink.tableRow_();
            }

            endTable();

            startSection(getI18nString("systemrequirements"));

            paragraph(getI18nString("systemrequirements.intro"));

            startTable();

            String maven = discoverMavenRequirement(project, pluginDescriptor);
            sink.tableRow();
            tableCell(getI18nString("systemrequirements.maven"));
            tableCell((maven != null ? maven : getI18nString("systemrequirements.nominimum")));
            sink.tableRow_();

            String jdk = discoverJdkRequirement(project, pluginDescriptor);
            sink.tableRow();
            tableCell(getI18nString("systemrequirements.jdk"));
            tableCell((jdk != null ? jdk : getI18nString("systemrequirements.nominimum")));
            sink.tableRow_();

            endTable();

            endSection();

            renderRequirementsHistories();

            renderUsageSection(hasMavenReport);

            endSection();
        }

        private void renderRequirementsHistories() {
            if (requirementsHistories.isEmpty()) {
                return;
            }

            startSection(getI18nString("systemrequirements.history"));
            paragraph(getI18nString("systemrequirements.history.intro"));

            startTable();
            tableHeader(new String[] {
                getI18nString("systemrequirements.history.version"),
                getI18nString("systemrequirements.history.maven"),
                getI18nString("systemrequirements.history.jdk")
            });

            requirementsHistories.forEach(requirementsHistory -> {
                sink.tableRow();
                tableCell(requirementsHistory.getVersion());
                tableCell(requirementsHistory.getMaven());
                tableCell(requirementsHistory.getJdk());
                sink.tableRow_();
            });
            endTable();

            endSection();
        }

        /**
         * Render the section about the usage of the plugin.
         *
         * @param hasMavenReport If the plugin has a report or not
         */
        private void renderUsageSection(boolean hasMavenReport) {
            startSection(getI18nString("usage"));

            // Configuration
            paragraph(getI18nString("usage.intro"));

            StringBuilder sb = new StringBuilder();
            sb.append("<project>").append(Markup.EOL);
            sb.append("  ...").append(Markup.EOL);
            sb.append("  <build>").append(Markup.EOL);
            sb.append("    <!-- " + getI18nString("usage.pluginManagement") + " -->")
                    .append(Markup.EOL);
            sb.append("    <pluginManagement>").append(Markup.EOL);
            sb.append("      <plugins>").append(Markup.EOL);
            sb.append("        <plugin>").append(Markup.EOL);
            sb.append("          <groupId>")
                    .append(pluginDescriptor.getGroupId())
                    .append("</groupId>")
                    .append(Markup.EOL);
            sb.append("          <artifactId>")
                    .append(pluginDescriptor.getArtifactId())
                    .append("</artifactId>")
                    .append(Markup.EOL);
            sb.append("          <version>")
                    .append(pluginDescriptor.getVersion())
                    .append("</version>")
                    .append(Markup.EOL);
            if (hasExtensionsToLoad) {
                sb.append("          <extensions>true</extensions>").append(Markup.EOL);
            }
            sb.append("        </plugin>").append(Markup.EOL);
            sb.append("        ...").append(Markup.EOL);
            sb.append("      </plugins>").append(Markup.EOL);
            sb.append("    </pluginManagement>").append(Markup.EOL);
            sb.append("    <!-- " + getI18nString("usage.plugins") + " -->").append(Markup.EOL);
            sb.append("    <plugins>").append(Markup.EOL);
            sb.append("      <plugin>").append(Markup.EOL);
            sb.append("        <groupId>")
                    .append(pluginDescriptor.getGroupId())
                    .append("</groupId>")
                    .append(Markup.EOL);
            sb.append("        <artifactId>")
                    .append(pluginDescriptor.getArtifactId())
                    .append("</artifactId>")
                    .append(Markup.EOL);
            sb.append("      </plugin>").append(Markup.EOL);
            sb.append("      ...").append(Markup.EOL);
            sb.append("    </plugins>").append(Markup.EOL);
            sb.append("  </build>").append(Markup.EOL);

            if (hasMavenReport) {
                sb.append("  ...").append(Markup.EOL);
                sb.append("  <!-- " + getI18nString("usage.reporting") + " -->").append(Markup.EOL);
                sb.append("  <reporting>").append(Markup.EOL);
                sb.append("    <plugins>").append(Markup.EOL);
                sb.append("      <plugin>").append(Markup.EOL);
                sb.append("        <groupId>")
                        .append(pluginDescriptor.getGroupId())
                        .append("</groupId>")
                        .append(Markup.EOL);
                sb.append("        <artifactId>")
                        .append(pluginDescriptor.getArtifactId())
                        .append("</artifactId>")
                        .append(Markup.EOL);
                sb.append("        <version>")
                        .append(pluginDescriptor.getVersion())
                        .append("</version>")
                        .append(Markup.EOL);
                sb.append("      </plugin>").append(Markup.EOL);
                sb.append("      ...").append(Markup.EOL);
                sb.append("    </plugins>").append(Markup.EOL);
                sb.append("  </reporting>").append(Markup.EOL);
            }

            sb.append("  ...").append(Markup.EOL);
            sb.append("</project>");

            verbatimText(sb.toString());

            sink.paragraph();
            linkPatternedText(getI18nString("configuration.end"));
            sink.paragraph_();

            endSection();
        }

        /**
         * Tries to determine the Maven requirement from either the plugin descriptor or (if not set) from the
         * Maven prerequisites element in the POM.
         *
         * @param project      not null
         * @param pluginDescriptor the plugin descriptor (not null)
         * @return the Maven version or null if not specified
         */
        private static String discoverMavenRequirement(MavenProject project, PluginDescriptor pluginDescriptor) {
            if (StringUtils.isNotBlank(pluginDescriptor.getRequiredMavenVersion())) {
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
         * @param pluginDescriptor the plugin descriptor (not null)
         * @return the JDK version
         */
        private static String discoverJdkRequirement(MavenProject project, PluginDescriptor pluginDescriptor) {
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
            if (jdk != null) {
                return jdk;
            }

            jdk = project.getProperties().getProperty("maven.compiler.release");
            if (jdk != null) {
                return jdk;
            }

            jdk = getPluginParameter(compiler, "target");
            if (jdk != null) {
                return jdk;
            }

            // default value
            jdk = project.getProperties().getProperty("maven.compiler.target");
            if (jdk != null) {
                return jdk;
            }

            String version = (compiler == null) ? null : compiler.getVersion();

            if (version != null) {
                return "Default target for maven-compiler-plugin version " + version;
            }

            return null;
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
}
