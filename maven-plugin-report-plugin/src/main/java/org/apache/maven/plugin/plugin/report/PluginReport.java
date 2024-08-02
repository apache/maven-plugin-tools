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
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import org.apache.maven.RepositoryUtils;
import org.apache.maven.artifact.ArtifactUtils;
import org.apache.maven.doxia.sink.Sink;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.building.ModelBuildingRequest;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugin.descriptor.PluginDescriptorBuilder;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Execute;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.plugin.descriptor.EnhancedPluginDescriptorBuilder;
import org.apache.maven.project.DefaultProjectBuildingRequest;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuilder;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.project.ProjectBuildingRequest;
import org.apache.maven.reporting.AbstractMavenReport;
import org.apache.maven.reporting.MavenReportException;
import org.apache.maven.rtinfo.RuntimeInformation;
import org.codehaus.plexus.configuration.PlexusConfigurationException;
import org.codehaus.plexus.i18n.I18N;
import org.codehaus.plexus.util.xml.XmlStreamReader;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.resolution.VersionRangeRequest;
import org.eclipse.aether.resolution.VersionRangeResolutionException;
import org.eclipse.aether.resolution.VersionRangeResult;
import org.eclipse.aether.version.Version;

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

    /**
     * Plugin's version range for automatic detection of requirements history.
     *
     * @since 3.12.0
     */
    @Parameter(defaultValue = "[0,)")
    private String requirementsHistoryDetectionRange;

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

    @Component
    private MavenSession mavenSession;

    @Component
    private RepositorySystem repositorySystem;

    @Component
    private ProjectBuilder projectBuilder;

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean canGenerateReport() {
        if (skip) {
            return false;
        }

        if (!(enhancedPluginXmlFile != null && enhancedPluginXmlFile.isFile() && enhancedPluginXmlFile.canRead())) {
            return false;
        }

        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void executeReport(Locale locale) throws MavenReportException {
        PluginDescriptor pluginDescriptor = extractPluginDescriptor();

        // Generate the mojos' documentation
        generateMojosDocumentation(pluginDescriptor, locale);

        if (requirementsHistories.isEmpty()) {
            // detect requirements history
            String v = null;
            try {
                List<Version> versions = discoverVersions(requirementsHistoryDetectionRange);
                if (versions.isEmpty()) {
                    getLog().info("No plugin history found for range " + requirementsHistoryDetectionRange);
                } else {
                    getLog().info("Detecting plugin requirements history for range "
                            + requirementsHistoryDetectionRange + ": "
                            + versions.size() + " releases, from " + versions.get(0) + " to "
                            + versions.get(versions.size() - 1));
                }

                Collections.reverse(versions);
                for (Version version : versions) {
                    v = version.toString();
                    MavenProject versionProject = buildMavenProject(v);
                    RequirementsHistory requirements = RequirementsHistory.discoverRequirements(versionProject);
                    requirementsHistories.add(requirements);
                    getLog().info("  - " + requirements);
                }
            } catch (VersionRangeResolutionException vrre) {
                throw new MavenReportException(
                        "Cannot resolve past versions " + requirementsHistoryDetectionRange, vrre);
            } catch (ProjectBuildingException pbe) {
                throw new MavenReportException("Cannot resolve MavenProject for version " + v, pbe);
            }
        }

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
                    Sink sink = getSinkFactory().createSink(getReportOutputDirectory(), filename);
                    renderer = new GoalRenderer(
                            sink,
                            i18n,
                            locale,
                            project,
                            descriptor,
                            getReportOutputDirectory(),
                            disableInternalJavadocLinkValidation,
                            getLog());
                } catch (IOException e) {
                    throw new MavenReportException("Cannot generate sink for mojo " + descriptor.getGoal(), e);
                }
                renderer.render();
            }
        }
    }

    private List<Version> discoverVersions(String range) throws VersionRangeResolutionException {
        MavenProject currentProject = mavenSession.getCurrentProject();
        VersionRangeRequest rangeRequest = new VersionRangeRequest();
        rangeRequest.setArtifact(
                new DefaultArtifact(currentProject.getGroupId() + ":" + currentProject.getArtifactId() + ":" + range));
        rangeRequest.setRepositories(
                RepositoryUtils.toRepos(mavenSession.getCurrentProject().getRemoteArtifactRepositories()));
        VersionRangeResult rangeResult =
                repositorySystem.resolveVersionRange(mavenSession.getRepositorySession(), rangeRequest);
        return rangeResult.getVersions().stream()
                .filter(version -> !ArtifactUtils.isSnapshot(version.toString()))
                .collect(Collectors.toList());
    }

    private MavenProject buildMavenProject(String version) throws ProjectBuildingException {
        MavenProject currentProject = mavenSession.getCurrentProject();
        ProjectBuildingRequest buildRequest = new DefaultProjectBuildingRequest();
        buildRequest.setLocalRepository(mavenSession.getLocalRepository());
        buildRequest.setRemoteRepositories(mavenSession.getCurrentProject().getRemoteArtifactRepositories());
        buildRequest.setValidationLevel(ModelBuildingRequest.VALIDATION_LEVEL_MINIMAL);
        buildRequest.setProcessPlugins(false);
        buildRequest.setRepositoryMerging(ProjectBuildingRequest.RepositoryMerging.REQUEST_DOMINANT);
        buildRequest.setSystemProperties(mavenSession.getSystemProperties());
        buildRequest.setUserProperties(mavenSession.getUserProperties());
        buildRequest.setRepositorySession(mavenSession.getRepositorySession());
        return projectBuilder
                .build(
                        RepositoryUtils.toArtifact(new DefaultArtifact(currentProject.getGroupId() + ":"
                                + currentProject.getArtifactId() + ":pom:" + version)),
                        buildRequest)
                .getProject();
    }
}
