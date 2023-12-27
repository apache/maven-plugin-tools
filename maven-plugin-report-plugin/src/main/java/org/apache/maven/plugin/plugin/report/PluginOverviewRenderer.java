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

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.doxia.markup.Markup;
import org.apache.maven.doxia.sink.Sink;
import org.apache.maven.doxia.sink.impl.SinkEventAttributeSet;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.Prerequisites;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.project.MavenProject;
import org.apache.maven.tools.plugin.ExtendedPluginDescriptor;
import org.apache.maven.tools.plugin.util.PluginUtils;
import org.codehaus.plexus.i18n.I18N;
import org.codehaus.plexus.util.xml.Xpp3Dom;

/**
 * Generates an overview page with the list of goals
 * and a link to the goal's page.
 */
class PluginOverviewRenderer extends AbstractPluginReportRenderer {

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
        super(sink, locale, i18n, project);

        this.requirementsHistories = requirementsHistories;

        this.pluginDescriptor = pluginDescriptor;

        this.hasExtensionsToLoad = hasExtensionsToLoad;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void renderBody() {
        startSection(getTitle());

        if (!(pluginDescriptor.getMojos() != null && pluginDescriptor.getMojos().size() > 0)) {
            paragraph(getI18nString("goals.nogoal"));
            endSection();
            return;
        }

        boolean hasMavenReport = false;
        for (MojoDescriptor mojo : pluginDescriptor.getMojos()) {
            if (PluginUtils.isMavenReport(mojo.getImplementation(), project)) {
                hasMavenReport = true;
                break;
            }
        }

        paragraph(getI18nString("description"));

        renderGoalsSection(hasMavenReport);

        renderSystemRequirementsSection();

        renderRequirementsHistoriesSection();

        renderUsageSection(hasMavenReport);

        endSection();
    }

    private void renderGoalsSection(boolean hasMavenReport) {
        startSection(getI18nString("goals"));

        paragraph(getI18nString("goals.intro"));

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
            sink.tableRow();

            String goalName = mojo.getFullGoalName();
            /*
             * Added ./ to define a relative path
             * @see AbstractMavenReportRenderer#getValidHref(java.lang.String)
             */
            String goalDocumentationLink = "./" + mojo.getGoal() + "-mojo.html";
            sink.tableCell();
            link(goalDocumentationLink, goalName);
            sink.tableCell_();

            if (hasMavenReport) {
                if (PluginUtils.isMavenReport(mojo.getImplementation(), project)) {
                    tableCell(getI18nString("isReport"));
                } else {
                    tableCell(getI18nString("isNotReport"));
                }
            }

            sink.tableCell();
            if (StringUtils.isNotEmpty(mojo.getDeprecated())) {
                sink.division();
                sink.inline(SinkEventAttributeSet.Semantics.STRONG);
                sink.text(getI18nString("goal.deprecated"));
                sink.text(".");
                sink.inline_();
                sink.text(" ");
                sink.rawText(mojo.getDeprecated());
                sink.division_();
                sink.lineBreak();
            }

            String description;
            if (StringUtils.isNotEmpty(mojo.getDescription())) {
                description = mojo.getDescription();
            } else {
                description = getI18nString("goal.nodescription");
            }
            sink.rawText(description);
            sink.tableCell_();
            sink.tableRow_();
        }

        endTable();

        endSection();
    }

    private void renderSystemRequirementsSection() {
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
    }

    private void renderRequirementsHistoriesSection() {
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

    @Override
    protected String getI18nSection() {
        return "plugin";
    }
}
