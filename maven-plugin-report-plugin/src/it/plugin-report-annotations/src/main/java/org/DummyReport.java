package org;

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

import org.apache.maven.doxia.sink.Sink;
import org.apache.maven.doxia.siterenderer.Renderer;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Execute;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.reporting.AbstractMavenReport;
import org.apache.maven.reporting.AbstractMavenReportRenderer;
import org.apache.maven.reporting.MavenReportException;

import java.io.File;
import java.util.Locale;
import java.util.ResourceBundle;

/**
 * Dummy Reporting Plugin.
 */
@Mojo( name = "report", requiresReports = true )
@Execute( phase = LifecyclePhase.COMPILE )
public class DummyReport
    extends AbstractMavenReport
{
    /**
     * Report output directory.
     */
    @Parameter( defaultValue = "${project.build.directory}/generated-site/xdoc" )
    private File outputDirectory;

    /**
     * Doxia Site Renderer.
     */
    @Component
    private Renderer siteRenderer;

    /**
     * The Maven Project.
     */
    @Parameter( property = "project", readonly = true, required = true )
    private MavenProject project;


    /**
     * The goal prefix that will appear before the ":".
     *
     * @since 2.4
     */
    @Parameter( property = "goalPrefix" )
    protected String goalPrefix;

    /**
     * Set this to "true" to skip invoking any goals or reports of the plugin.
     *
     * @since 2.8
     */
    @Parameter( defaultValue = "false", property = "maven.plugin.skip" )
    private boolean skip;

    /**
     * Set this to "true" to skip generating the report.
     *
     * @since 2.8
     */
    @Parameter( defaultValue = "false", property = "maven.plugin.report.skip" )
    private boolean skipReport;

    /**
     * {@inheritDoc}
     */
    protected Renderer getSiteRenderer()
    {
        return siteRenderer;
    }

    /**
     * {@inheritDoc}
     */
    protected String getOutputDirectory()
    {
        return outputDirectory.getPath();
    }

    /**
     * {@inheritDoc}
     */
    protected MavenProject getProject()
    {
        return project;
    }

    /**
     * {@inheritDoc}
     */
    public boolean canGenerateReport()
    {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    protected void executeReport( Locale locale )
        throws MavenReportException
    {
        if ( !canGenerateReport() )
        {
            return;
        }
        if ( skip || skipReport )
        {
            getLog().info( "Maven Plugin Plugin Report generation skipped." );
            return;
        }

        // Generate the plugin's documentation
        generatePluginDocumentation( locale );
    }

    /**
     * {@inheritDoc}
     */
    public String getDescription( Locale locale )
    {
        return getBundle( locale ).getString( "report.plugin.description" );
    }

    /**
     * {@inheritDoc}
     */
    public String getName( Locale locale )
    {
        return getBundle( locale ).getString( "report.plugin.name" );
    }

    /**
     * {@inheritDoc}
     */
    public String getOutputName()
    {
        return "plugin-info";
    }

    /**
     * @param pluginDescriptor not null
     * @param locale           not null
     * @throws MavenReportException if any
     */
    private void generatePluginDocumentation( Locale locale )
        throws MavenReportException
    {
        File outputDir = new File( getOutputDirectory() );
        outputDir.mkdirs();
        PluginOverviewRenderer r = new PluginOverviewRenderer( getSink(), locale );
        r.render();
    }

    /**
     * @param locale not null
     * @return the bundle for this report
     */
    protected static ResourceBundle getBundle( Locale locale )
    {
        return ResourceBundle.getBundle( "plugin-report", locale, DummyReport.class.getClassLoader() );
    }

    /**
     * Generates an overview page with the list of goals
     * and a link to the goal's page.
     */
    static class PluginOverviewRenderer
        extends AbstractMavenReportRenderer
    {
        private final Locale locale;

        /**
         * @param project not null
         * @param sink    not null
         * @param locale  not null
         */
        public PluginOverviewRenderer( Sink sink, Locale locale )
        {
            super( sink );

            this.locale = locale;
        }

        /**
         * {@inheritDoc}
         */
        public String getTitle()
        {
            return getBundle( locale ).getString( "report.plugin.title" );
        }

        /**
         * {@inheritDoc}
         */
        public void renderBody()
        {
            startSection( getTitle() );
            paragraph( "This is a report." );
            endSection();
        }
    }
}
