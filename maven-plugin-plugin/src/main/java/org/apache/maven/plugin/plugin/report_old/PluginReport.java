package org.apache.maven.plugin.plugin.report_old;

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

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

import org.apache.maven.doxia.sink.Sink;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugin.descriptor.PluginDescriptorBuilder;
import org.apache.maven.plugin.plugin.DescriptorGeneratorMojo;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Execute;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.plugin.descriptor_old.EnhancedPluginDescriptorBuilder;
import org.apache.maven.project.MavenProject;
import org.apache.maven.reporting.AbstractMavenReport;
import org.apache.maven.reporting.AbstractMavenReportRenderer;
import org.apache.maven.reporting.MavenReportException;
import org.apache.maven.rtinfo.RuntimeInformation;
import org.apache.maven.tools.plugin.DefaultPluginToolsRequest;
import org.apache.maven.tools.plugin.PluginToolsRequest;
import org.apache.maven.tools.plugin.generator.GeneratorException;
import org.apache.maven.tools.plugin.generator.GeneratorUtils;
import org.apache.maven.tools.plugin.generator.PluginXdocGenerator;
import org.apache.maven.tools.plugin.util.PluginUtils;
import org.codehaus.plexus.configuration.PlexusConfigurationException;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.xml.XmlStreamReader;
import org.codehaus.plexus.util.xml.Xpp3Dom;

/**
 * Generates the Plugin's documentation report: <code>plugin-info.html</code> plugin overview page,
 * and one <code><i>goal</i>-mojo.html</code> per goal.
 * Relies on one output file from {@link DescriptorGeneratorMojo}.
 *
 * @author <a href="snicoll@apache.org">Stephane Nicoll</a>
 * @author <a href="mailto:vincent.siveton@gmail.com">Vincent Siveton</a>
 * @since 2.0
 * @deprecated use the maven-plugin-report-plugin instead
 */
@Deprecated
@Mojo( name = "report", threadSafe = true )
@Execute( phase = LifecyclePhase.PROCESS_CLASSES )
public class PluginReport
    extends AbstractMavenReport
{
    /**
     * Report output directory for mojos' documentation.
     */
    @Parameter( defaultValue = "${project.build.directory}/generated-site/xdoc" )
    private File outputDirectory;

    /**
     * The file encoding of the source files.
     *
     * @deprecated not used in report, will be removed in the next major version
     *
     * @since 2.7
     */
    @Deprecated
    @Parameter( property = "encoding", defaultValue = "${project.build.sourceEncoding}" )
    private String encoding;

    /**
     * Specify some requirements to execute this plugin.
     * Example:
     * <pre>
     * &lt;requirements&gt;
     *   &lt;maven&gt;2.0&lt;/maven&gt;
     *   &lt;jdk&gt;1.4&lt;/jdk&gt;
     *   &lt;memory&gt;256m&lt;/memory&gt;
     *   &lt;diskSpace&gt;1m&lt;/diskSpace&gt;
     *   &lt;others&gt;
     *     &lt;property&gt;
     *       &lt;name&gt;SVN&lt;/name&gt;
     *       &lt;value&gt;1.4.6&lt;/value&gt;
     *     &lt;/property&gt;
     *   &lt;/others&gt;
     * &lt;/requirements&gt;
     * </pre>
     * <p>
     * If not is specified, Maven requirement is extracted from
     * <code>&lt;project&gt;&lt;prerequisites&gt;&lt;maven&gt;</code>
     * and JDK requirement is extracted from maven-compiler-plugin configuration.
     *
     * @deprecated will be removed in the next major version, please don't use
     */
    @Deprecated
    @Parameter
    private Requirements requirements;

    /**
     * <p>
     * The goal prefix that will appear before the ":".
     * By default, this plugin applies a heuristic to derive a heuristic from
     * the plugin's artifactId.
     * </p>
     * <p>
     * It removes any occurrences of the regular expression <strong>-?maven-?</strong>,
     * and then removes any occurrences of <strong>-?plugin-?</strong>.
     * </p>
     * <p>
     * For example, horsefeature-maven-plugin becomes horsefeature.
     * </p>
     * <p>
     * (There is a special case for maven-plugin-plugin: it is mapped to 'plugin')
     * </p>
     *
     * @deprecated not used in report, will be removed in the next major version
     *
     * @since 2.4
     */
    @Deprecated
    @Parameter( property = "goalPrefix" )
    protected String goalPrefix;

    /**
     * Set this to "true" to skip invoking any goals or reports of the plugin.
     *
     * @deprecated use {@link #skip} parameter instead
     *
     * @since 2.8
     */
    @Deprecated
    @Parameter( defaultValue = "false", property = "maven.plugin.skip" )
    private boolean skipReport;

    /**
     * Set this to "true" to skip generating the report.
     *
     * @since 2.8
     */
    @Parameter( defaultValue = "false", property = "maven.plugin.report.skip" )
    private boolean skip;

    /**
     * Set this to "true" to generate the usage section for "plugin-info.html" with
     * {@code <extensions>true</extensions>}.
     *
     * @since 3.7.0
     */
    @Parameter( defaultValue = "false", property = "maven.plugin.report.hasExtensionsToLoad" )
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
     * @since 3.5.1
     */
    @Component
    private RuntimeInformation rtInfo;

    /**
     * Path to {@code plugin.xml} plugin descriptor to generate the report from.
     *
     * @since 3.5.1
     * @deprecated No longer evaluated, use {@link #enhancedPluginXmlFile}.
     */
    @Parameter( defaultValue = "${project.build.outputDirectory}/META-INF/maven/plugin.xml", required = true,
                readonly = true )
    @Deprecated
    private File pluginXmlFile;

    /**
     * Path to enhanced plugin descriptor to generate the report from (must contain some XHTML values)
     *
     * @since 3.7.0
     */
    @Parameter( defaultValue = "${project.build.directory}/plugin-enhanced.xml", required = true,
                readonly = true )
    private File enhancedPluginXmlFile;

    /**
     * In case the internal javadoc site has not been generated when running this report goal
     * (e.g. when using an aggregator javadoc report) link validation needs to be disabled by setting
     * this value to {@code true}.
     * This might have the drawback that some links being generated in the report might be broken
     * in case not all parameter types and javadoc link references are resolvable through the sites being given to
     * {@link DescriptorGeneratorMojo}.
     * 
     * @since 3.7.0
     */
    @Parameter( property = "maven.plugin.report.disableInternalJavadocLinkValidation" )
    private boolean disableInternalJavadocLinkValidation;

    /**
     * {@inheritDoc}
     */
    @Override
    protected String getOutputDirectory()
    {
        // PLUGIN-191: output directory of plugin.html, not *-mojo.xml
        return project.getReporting().getOutputDirectory();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean canGenerateReport()
    {
        return enhancedPluginXmlFile != null && enhancedPluginXmlFile.isFile() && enhancedPluginXmlFile.canRead();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void executeReport( Locale locale )
        throws MavenReportException
    {
        getLog().warn( "The 'report' goal of the maven-plugin-plugin is deprecated, please use "
                + "the 'report' goal from the maven-plugin-report-plugin instead. This goal will be removed "
                + "in version 4.0.0." );

        if ( skip || skipReport )
        {
            getLog().info( "Maven Plugin Plugin Report generation skipped." );
            return;
        }

        PluginDescriptor pluginDescriptor = extractPluginDescriptor();

        // Generate the mojos' documentation
        generateMojosDocumentation( pluginDescriptor, locale );

        // Write the overview
        PluginOverviewRenderer r =
            new PluginOverviewRenderer( getProject(), requirements, requirementsHistories, getSink(),
                                        pluginDescriptor, locale, hasExtensionsToLoad );
        r.render();
    }

    private PluginDescriptor extractPluginDescriptor()
        throws MavenReportException
    {
        PluginDescriptorBuilder builder = new EnhancedPluginDescriptorBuilder( rtInfo );

        try ( Reader input = new XmlStreamReader( Files.newInputStream( enhancedPluginXmlFile.toPath() ) ) )
        {
            return builder.build( input );
        }
        catch ( IOException | PlexusConfigurationException e )
        {
            throw new MavenReportException( "Error extracting plugin descriptor from " + enhancedPluginXmlFile, e );
        }

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDescription( Locale locale )
    {
        return getBundle( locale ).getString( "report.plugin.description" );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName( Locale locale )
    {
        return getBundle( locale ).getString( "report.plugin.name" );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getOutputName()
    {
        return "plugin-info";
    }

    /**
     * Generate the mojos documentation, as xdoc files.
     *
     * @param pluginDescriptor not null
     * @param locale           not null
     * @throws MavenReportException if any
     */
    private void generateMojosDocumentation( PluginDescriptor pluginDescriptor, Locale locale )
        throws MavenReportException
    {
        try
        {
            File outputDir = outputDirectory;
            outputDir.mkdirs();

            PluginXdocGenerator generator = new PluginXdocGenerator( getProject(), locale, getReportOutputDirectory(),
                                                                     disableInternalJavadocLinkValidation );
            PluginToolsRequest pluginToolsRequest = new DefaultPluginToolsRequest( getProject(), pluginDescriptor );
            generator.execute( outputDir, pluginToolsRequest );
        }
        catch ( GeneratorException e )
        {
            throw new MavenReportException( "Error writing plugin documentation", e );
        }

    }

    /**
     * @param locale not null
     * @return the bundle for this report
     */
    protected static ResourceBundle getBundle( Locale locale )
    {
        return ResourceBundle.getBundle( "plugin-report", locale, PluginReport.class.getClassLoader() );
    }

    /**
     * Generates an overview page with the list of goals
     * and a link to the goal's page.
     */
    static class PluginOverviewRenderer
        extends AbstractMavenReportRenderer
    {
        private final MavenProject project;

        private final Requirements requirements;

        private final List<RequirementsHistory> requirementsHistories;

        private final PluginDescriptor pluginDescriptor;

        private final Locale locale;

        private final boolean hasExtensionsToLoad;

        /**
         * @param project               not null
         * @param requirements          not null
         * @param requirementsHistories not null
         * @param sink                  not null
         * @param pluginDescriptor      not null
         * @param locale                not null
         */
        PluginOverviewRenderer( MavenProject project, Requirements requirements,
                                List<RequirementsHistory> requirementsHistories, Sink sink,
                                PluginDescriptor pluginDescriptor, Locale locale, boolean hasExtensionsToLoad )
        {
            super( sink );

            this.project = project;

            this.requirements = ( requirements == null ? new Requirements() : requirements );

            this.requirementsHistories = requirementsHistories;

            this.pluginDescriptor = pluginDescriptor;

            this.locale = locale;

            this.hasExtensionsToLoad = hasExtensionsToLoad;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String getTitle()
        {
            return getBundle( locale ).getString( "report.plugin.title" );
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void renderBody()
        {
            startSection( getTitle() );

            if ( !( pluginDescriptor.getMojos() != null && pluginDescriptor.getMojos().size() > 0 ) )
            {
                paragraph( getBundle( locale ).getString( "report.plugin.goals.nogoal" ) );
                endSection();
                return;
            }

            paragraph( getBundle( locale ).getString( "report.plugin.goals.intro" ) );

            boolean hasMavenReport = false;
            for ( Iterator<MojoDescriptor> i = pluginDescriptor.getMojos().iterator(); i.hasNext(); )
            {
                MojoDescriptor mojo = i.next();

                if ( GeneratorUtils.isMavenReport( mojo.getImplementation(), project ) )
                {
                    hasMavenReport = true;
                }
            }

            startTable();

            String goalColumnName = getBundle( locale ).getString( "report.plugin.goals.column.goal" );
            String isMavenReport = getBundle( locale ).getString( "report.plugin.goals.column.isMavenReport" );
            String descriptionColumnName = getBundle( locale ).getString( "report.plugin.goals.column.description" );
            if ( hasMavenReport )
            {
                tableHeader( new String[] {goalColumnName, isMavenReport, descriptionColumnName} );
            }
            else
            {
                tableHeader( new String[] {goalColumnName, descriptionColumnName} );
            }

            List<MojoDescriptor> mojos = new ArrayList<>();
            mojos.addAll( pluginDescriptor.getMojos() );
            PluginUtils.sortMojos( mojos );
            for ( MojoDescriptor mojo : mojos )
            {
                String goalName = mojo.getFullGoalName();

                /*
                 * Added ./ to define a relative path
                 * @see AbstractMavenReportRenderer#getValidHref(java.lang.String)
                 */
                String goalDocumentationLink = "./" + mojo.getGoal() + "-mojo.html";

                String description;
                if ( StringUtils.isNotEmpty( mojo.getDeprecated() ) )
                {
                    description =
                        "<strong>" + getBundle( locale ).getString( "report.plugin.goal.deprecated" ) + "</strong> "
                            + mojo.getDeprecated();
                }
                else if ( StringUtils.isNotEmpty( mojo.getDescription() ) )
                {
                    description = mojo.getDescription();
                }
                else
                {
                    description = getBundle( locale ).getString( "report.plugin.goal.nodescription" );
                }

                sink.tableRow();
                tableCell( createLinkPatternedText( goalName, goalDocumentationLink ) );
                if ( hasMavenReport )
                {
                    if ( GeneratorUtils.isMavenReport( mojo.getImplementation(), project ) )
                    {
                        sink.tableCell();
                        sink.text( getBundle( locale ).getString( "report.plugin.isReport" ) );
                        sink.tableCell_();
                    }
                    else
                    {
                        sink.tableCell();
                        sink.text( getBundle( locale ).getString( "report.plugin.isNotReport" ) );
                        sink.tableCell_();
                    }
                }
                tableCell( description, true );
                sink.tableRow_();
            }

            endTable();

            startSection( getBundle( locale ).getString( "report.plugin.systemrequirements" ) );

            paragraph( getBundle( locale ).getString( "report.plugin.systemrequirements.intro" ) );

            startTable();

            String maven = discoverMavenRequirement( project, requirements );
            sink.tableRow();
            tableCell( getBundle( locale ).getString( "report.plugin.systemrequirements.maven" ) );
            tableCell( ( maven != null
                ? maven
                : getBundle( locale ).getString( "report.plugin.systemrequirements.nominimum" ) ) );
            sink.tableRow_();

            String jdk = discoverJdkRequirement( project, requirements );
            sink.tableRow();
            tableCell( getBundle( locale ).getString( "report.plugin.systemrequirements.jdk" ) );
            tableCell(
                ( jdk != null ? jdk : getBundle( locale ).getString( "report.plugin.systemrequirements.nominimum" ) ) );
            sink.tableRow_();

            String memory = requirements.getMemory();
            if ( StringUtils.isNotEmpty( memory ) )
            {
                sink.tableRow();
                tableCell( getBundle( locale ).getString( "report.plugin.systemrequirements.memory" ) );
                tableCell( memory );
                sink.tableRow_();
            }

            String diskSpace = requirements.getDiskSpace();
            if ( StringUtils.isNotEmpty( diskSpace ) )
            {
                sink.tableRow();
                tableCell( getBundle( locale ).getString( "report.plugin.systemrequirements.diskspace" ) );
                tableCell( diskSpace );
                sink.tableRow_();
            }

            if ( requirements.getOthers() != null && requirements.getOthers().size() > 0 )
            {
                for ( Iterator it = requirements.getOthers().keySet().iterator(); it.hasNext(); )
                {
                    String key = it.next().toString();

                    sink.tableRow();
                    tableCell( key );
                    tableCell( ( StringUtils.isNotEmpty( requirements.getOthers().getProperty( key ) )
                        ? requirements.getOthers().getProperty( key )
                        : getBundle( locale ).getString( "report.plugin.systemrequirements.nominimum" ) ) );
                    sink.tableRow_();
                }
            }
            endTable();

            endSection();

            renderRequirementsHistories();

            renderUsageSection( hasMavenReport );

            endSection();
        }

        private void renderRequirementsHistories()
        {
            if ( requirementsHistories.isEmpty() )
            {
                return;
            }

            startSection( getBundle( locale ).getString( "report.plugin.systemrequirements.history" ) );
            paragraph( getBundle( locale ).getString( "report.plugin.systemrequirements.history.intro" ) );

            startTable();
            tableHeader( new String[] {
                getBundle( locale ).getString( "report.plugin.systemrequirements.history.version" ),
                getBundle( locale ).getString( "report.plugin.systemrequirements.history.maven" ),
                getBundle( locale ).getString( "report.plugin.systemrequirements.history.jdk" )
            } );

            requirementsHistories.forEach(
                requirementsHistory ->
                {
                    sink.tableRow();
                    tableCell( requirementsHistory.getVersion() );
                    tableCell( requirementsHistory.getMaven() );
                    tableCell( requirementsHistory.getJdk() );
                    sink.tableRow_();
                } );
            endTable();

            endSection();
        }

        /**
         * Render the section about the usage of the plugin.
         *
         * @param hasMavenReport If the plugin has a report or not
         */
        private void renderUsageSection( boolean hasMavenReport )
        {
            startSection( getBundle( locale ).getString( "report.plugin.usage" ) );

            // Configuration
            sink.paragraph();
            text( getBundle( locale ).getString( "report.plugin.usage.intro" ) );
            sink.paragraph_();

            StringBuilder sb = new StringBuilder();
            sb.append( "<project>" ).append( '\n' );
            sb.append( "  ..." ).append( '\n' );
            sb.append( "  <build>" ).append( '\n' );
            sb.append(
                "    <!-- " + getBundle( locale ).getString( "report.plugin.usage.pluginManagement" ) + " -->" ).append(
                '\n' );
            sb.append( "    <pluginManagement>" ).append( '\n' );
            sb.append( "      <plugins>" ).append( '\n' );
            sb.append( "        <plugin>" ).append( '\n' );
            sb.append( "          <groupId>" ).append( pluginDescriptor.getGroupId() ).append( "</groupId>" ).append(
                '\n' );
            sb.append( "          <artifactId>" ).append( pluginDescriptor.getArtifactId() ).append(
                "</artifactId>" ).append( '\n' );
            sb.append( "          <version>" ).append( pluginDescriptor.getVersion() ).append( "</version>" ).append(
                '\n' );
            if ( hasExtensionsToLoad )
            {
                sb.append( "          <extensions>true</extensions>" ).append(
                    '\n' );
            }
            sb.append( "        </plugin>" ).append( '\n' );
            sb.append( "        ..." ).append( '\n' );
            sb.append( "      </plugins>" ).append( '\n' );
            sb.append( "    </pluginManagement>" ).append( '\n' );
            sb.append( "    <!-- " + getBundle( locale ).getString( "report.plugin.usage.plugins" ) + " -->" ).append(
                '\n' );
            sb.append( "    <plugins>" ).append( '\n' );
            sb.append( "      <plugin>" ).append( '\n' );
            sb.append( "        <groupId>" ).append( pluginDescriptor.getGroupId() ).append( "</groupId>" ).append(
                '\n' );
            sb.append( "        <artifactId>" ).append( pluginDescriptor.getArtifactId() ).append(
                "</artifactId>" ).append( '\n' );
            sb.append( "      </plugin>" ).append( '\n' );
            sb.append( "      ..." ).append( '\n' );
            sb.append( "    </plugins>" ).append( '\n' );
            sb.append( "  </build>" ).append( '\n' );

            if ( hasMavenReport )
            {
                sb.append( "  ..." ).append( '\n' );
                sb.append(
                    "  <!-- " + getBundle( locale ).getString( "report.plugin.usage.reporting" ) + " -->" ).append(
                    '\n' );
                sb.append( "  <reporting>" ).append( '\n' );
                sb.append( "    <plugins>" ).append( '\n' );
                sb.append( "      <plugin>" ).append( '\n' );
                sb.append( "        <groupId>" ).append( pluginDescriptor.getGroupId() ).append( "</groupId>" ).append(
                    '\n' );
                sb.append( "        <artifactId>" ).append( pluginDescriptor.getArtifactId() ).append(
                    "</artifactId>" ).append( '\n' );
                sb.append( "        <version>" ).append( pluginDescriptor.getVersion() ).append( "</version>" ).append(
                    '\n' );
                sb.append( "      </plugin>" ).append( '\n' );
                sb.append( "      ..." ).append( '\n' );
                sb.append( "    </plugins>" ).append( '\n' );
                sb.append( "  </reporting>" ).append( '\n' );
            }

            sb.append( "  ..." ).append( '\n' );
            sb.append( "</project>" ).append( '\n' );

            verbatimText( sb.toString() );

            sink.paragraph();
            linkPatternedText( getBundle( locale ).getString( "report.plugin.configuration.end" ) );
            sink.paragraph_();

            endSection();
        }

        /**
         * Try to lookup on the Maven prerequisites property.
         * If not specified, uses the value defined by the user.
         *
         * @param project      not null
         * @param requirements not null
         * @return the Maven version
         */
        private static String discoverMavenRequirement( MavenProject project, Requirements requirements )
        {
            String maven = requirements.getMaven();
            if ( maven == null )
            {
                maven = ( project.getPrerequisites() != null ? project.getPrerequisites().getMaven() : null );
            }
            if ( maven == null )
            {
                maven = "2.0";
            }

            return maven;
        }

        /**
         * <ol>
         * <li>use configured jdk requirement</li>
         * <li>use <code>target</code> configuration of <code>org.apache.maven.plugins:maven-compiler-plugin</code></li>
         * <li>use <code>target</code> configuration of <code>org.apache.maven.plugins:maven-compiler-plugin</code> in
         * <code>pluginManagement</code></li>
         * <li>use <code>maven.compiler.target</code> property</li>
         * </ol>
         *
         * @param project      not null
         * @param requirements not null
         * @return the JDK version
         */
        private static String discoverJdkRequirement( MavenProject project, Requirements requirements )
        {
            String jdk = requirements.getJdk();

            if ( jdk != null )
            {
                return jdk;
            }

            Plugin compiler = getCompilerPlugin( project.getBuild().getPluginsAsMap() );
            if ( compiler == null )
            {
                compiler = getCompilerPlugin( project.getPluginManagement().getPluginsAsMap() );
            }

            jdk = getPluginParameter( compiler, "release" );
            if ( jdk != null )
            {
                return jdk;
            }

            jdk = project.getProperties().getProperty( "maven.compiler.release" );
            if ( jdk != null )
            {
                return jdk;
            }

            jdk = getPluginParameter( compiler, "target" );
            if ( jdk != null )
            {
                return jdk;
            }

            // default value
            jdk = project.getProperties().getProperty( "maven.compiler.target" );
            if ( jdk != null )
            {
                return jdk;
            }

            // return "1.5" by default?

            String version = ( compiler == null ) ? null : compiler.getVersion();

            if ( version != null )
            {
                return "Default target for maven-compiler-plugin version " + version;
            }

            return "Unknown";
        }

        private static Plugin getCompilerPlugin( Map<String, Plugin> pluginsAsMap )
        {
            return pluginsAsMap.get( "org.apache.maven.plugins:maven-compiler-plugin" );
        }

        private static String getPluginParameter( Plugin plugin, String parameter )
        {
            if ( plugin != null )
            {
                Xpp3Dom pluginConf = (Xpp3Dom) plugin.getConfiguration();

                if ( pluginConf != null )
                {
                    Xpp3Dom target = pluginConf.getChild( parameter );

                    if ( target != null )
                    {
                        return target.getValue();
                    }
                }
            }

            return null;
        }
    }
}
