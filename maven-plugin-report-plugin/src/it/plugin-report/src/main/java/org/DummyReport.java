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
package org;

import java.io.File;
import java.util.Locale;
import java.util.ResourceBundle;

import org.apache.maven.doxia.sink.Sink;
import org.apache.maven.doxia.siterenderer.Renderer;
import org.apache.maven.project.MavenProject;
import org.apache.maven.reporting.AbstractMavenReport;
import org.apache.maven.reporting.AbstractMavenReportRenderer;
import org.apache.maven.reporting.MavenReportException;

/**
 * Dummy Reporting Plugin.
 *
 * @goal report
 * @requiresReports true
 * @execute phase="compile"
 */
public class DummyReport extends AbstractMavenReport {
    /**
     * Report output directory.
     *
     * @parameter default-value="${project.build.directory}/generated-site/xdoc"
     */
    private File outputDirectory;

    /**
     * Doxia Site Renderer.
     *
     * @component
     */
    private Renderer siteRenderer;

    /**
     * The Maven Project.
     *
     * @parameter expression="${project}"
     * @required
     * @readonly
     */
    private MavenProject project;

    /**
     * The goal prefix that will appear before the ":".
     *
     * @parameter expression="${goalPrefix}"
     * @since 2.4
     */
    protected String goalPrefix;

    /**
     * Set this to "true" to skip invoking any goals or reports of the plugin.
     *
     * @parameter default-value="false" expression="${maven.plugin.skip}"
     * @since 2.8
     */
    private boolean skip;

    /**
     * Set this to "true" to skip generating the report.
     *
     * @parameter default-value="false" expression="${maven.plugin.report.skip}"
     * @since 2.8
     */
    private boolean skipReport;

    /** {@inheritDoc} */
    protected Renderer getSiteRenderer() {
        return siteRenderer;
    }

    /** {@inheritDoc} */
    protected String getOutputDirectory() {
        return outputDirectory.getPath();
    }

    /** {@inheritDoc} */
    protected MavenProject getProject() {
        return project;
    }

    /** {@inheritDoc} */
    public boolean canGenerateReport() {
        if (skip || skipReport) {
            return false;
        }

        return true;
    }

    /** {@inheritDoc} */
    protected void executeReport(Locale locale) throws MavenReportException {
        // Generate the plugin's documentation
        generatePluginDocumentation(locale);
    }

    /** {@inheritDoc} */
    public String getDescription(Locale locale) {
        return getBundle(locale).getString("report.plugin.description");
    }

    /** {@inheritDoc} */
    public String getName(Locale locale) {
        return getBundle(locale).getString("report.plugin.name");
    }

    /** {@inheritDoc} */
    public String getOutputName() {
        return "plugin-info";
    }

    /**
     * @param pluginDescriptor not null
     * @param locale not null
     * @throws MavenReportException if any
     */
    private void generatePluginDocumentation(Locale locale) throws MavenReportException {
        File outputDir = new File(getOutputDirectory());
        outputDir.mkdirs();
        PluginOverviewRenderer r = new PluginOverviewRenderer(getSink(), locale);
        r.render();
    }

    /**
     * @param locale not null
     * @return the bundle for this report
     */
    protected static ResourceBundle getBundle(Locale locale) {
        return ResourceBundle.getBundle("plugin-report", locale, DummyReport.class.getClassLoader());
    }

    /**
     * Generates an overview page with the list of goals
     * and a link to the goal's page.
     */
    static class PluginOverviewRenderer extends AbstractMavenReportRenderer {
        private final Locale locale;

        /**
         * @param sink not null
         * @param locale not null
         */
        PluginOverviewRenderer(Sink sink, Locale locale) {
            super(sink);

            this.locale = locale;
        }

        /** {@inheritDoc} */
        public String getTitle() {
            return getBundle(locale).getString("report.plugin.title");
        }

        /** {@inheritDoc} */
        protected void renderBody() {
            startSection(getTitle());
            paragraph("This is a report.");
            endSection();
        }
    }
}
